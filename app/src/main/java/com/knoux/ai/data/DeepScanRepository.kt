package com.knoux.ai.data

import com.knoux.ai.R

import com.knoux.ai.Copy
import com.knoux.ai.visual.DeepScanModule
import com.knoux.ai.visual.DeepScanOutcome
import com.knoux.ai.visual.DeepScanProgress
import com.knoux.ai.visual.DeepScanStatus
import com.knoux.ai.visual.ModuleResult
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * PC05 — real multi-module deep scan.
 *
 * This engine owns no discovery, hashing or deletion logic of its own. It asks
 * the already-proven engines exactly once per concern:
 *
 *  - [SmartCleanRepository.scan] enumerates only scopes the customer granted and
 *    returns each file with its provider-reported eligibility and provenance.
 *  - [DuplicateRepository.scan] proves content equality with a full-stream
 *    SHA-256 inside its own budgets, and protects one keeper per group.
 *
 * What this class adds is the *deep scan* view: one measured module at a time,
 * a status per module, and a totals model in which a module that could not be
 * read is reported as unavailable instead of contributing a fabricated zero.
 *
 * Cancellation is cooperative and non-destructive: [cancel] sets a flag the loop
 * observes between modules, so a cancelled pass still persists the modules that
 * genuinely finished.
 */
internal class DeepScanRepository(
    private val permissions: PermissionState,
    private val scanner: SmartCleanRepository,
    private val duplicates: DuplicateRepository,
) {

    companion object {
        /** A file at or above this size is listed by the "large files" module. */
        const val LARGE_FILE_BYTES: Long = DeepScanRules.LARGE_FILE_BYTES

        /** Files untouched for this long are listed by the "old files" module. */
        const val OLD_FILE_MILLIS: Long = DeepScanRules.OLD_FILE_MILLIS

        private val READABLE = setOf(PermissionGrant.GRANTED, PermissionGrant.PARTIAL_SELECTED)
    }

    /**
     * Modules in sector order. Order is meaningful: the sonar column reads top to
     * bottom, so cheap, permission-light modules come first.
     */
    val modules: List<DeepScanModule> = listOf(
        DeepScanModule(
            id = "app_cache",
            order = 0,
            title = Copy(R.string.k_knoux_cache_temp),
            shortTitle = Copy(R.string.k_cache),
            description = Copy(R.string.k_knoux_s_own_cache_code_cache_and_external_cache),
            mark = com.knoux.ai.Mark.BROOM,
            scopes = setOf(AccessScope.APP_CACHE),
            defaultEnabled = true,
        ),
        DeepScanModule(
            id = "media_images",
            order = 1,
            title = Copy(R.string.k_photos_images),
            shortTitle = Copy(R.string.k_photos),
            description = Copy(R.string.k_images_visible_through_mediastore),
            mark = com.knoux.ai.Mark.PICTURES,
            scopes = setOf(AccessScope.MEDIA_IMAGES),
            defaultEnabled = true,
        ),
        DeepScanModule(
            id = "media_video",
            order = 2,
            title = Copy(R.string.k_videos),
            shortTitle = Copy(R.string.k_video),
            description = Copy(R.string.k_videos_visible_through_mediastore),
            mark = com.knoux.ai.Mark.VIDEO,
            scopes = setOf(AccessScope.MEDIA_VIDEO),
            defaultEnabled = true,
        ),
        DeepScanModule(
            id = "documents",
            order = 3,
            title = Copy(R.string.k_granted_folders),
            shortTitle = Copy(R.string.k_folders),
            description = Copy(R.string.k_files_inside_folders_you_granted_to_knoux),
            mark = com.knoux.ai.Mark.FOLDER,
            scopes = setOf(AccessScope.SAF_TREE),
            defaultEnabled = true,
        ),
        DeepScanModule(
            id = "large_files",
            order = 4,
            title = Copy(R.string.k_large_files_100_mb),
            shortTitle = Copy(R.string.k_large),
            description = Copy(R.string.k_single_files_at_or_above_100_mb_in_the_readable_scopes),
            mark = com.knoux.ai.Mark.DISK,
            scopes = setOf(AccessScope.MEDIA_IMAGES, AccessScope.MEDIA_VIDEO, AccessScope.SAF_TREE),
            defaultEnabled = true,
        ),
        DeepScanModule(
            id = "old_files",
            order = 5,
            title = Copy(R.string.k_untouched_for_a_year),
            shortTitle = Copy(R.string.k_old),
            description = Copy(R.string.k_files_whose_own_timestamp_is_older_than_a_year),
            mark = com.knoux.ai.Mark.CALENDAR,
            scopes = setOf(AccessScope.MEDIA_IMAGES, AccessScope.MEDIA_VIDEO, AccessScope.SAF_TREE),
            defaultEnabled = false,
        ),
        DeepScanModule(
            id = "duplicates",
            order = 6,
            title = Copy(R.string.k_byte_identical_duplicates),
            shortTitle = Copy(R.string.k_duplicates),
            description = Copy(R.string.k_full_sha_256_of_every_same_size_file_one_keeper_is_alway),
            mark = com.knoux.ai.Mark.COPY,
            scopes = setOf(AccessScope.MEDIA_IMAGES, AccessScope.MEDIA_VIDEO, AccessScope.SAF_TREE),
            defaultEnabled = false,
        ),
    )

    private val _progress = MutableStateFlow<DeepScanProgress?>(null)

    /** Live progress of the running pass, or null when idle. */
    val progress: StateFlow<DeepScanProgress?> = _progress.asStateFlow()

    private val cancelled = AtomicBoolean(false)

    /** Stops the pass between modules without discarding finished work. */
    fun cancel() {
        cancelled.set(true)
    }

    /** Modules enabled for a fresh install: cheap and permission-free only. */
    val defaultSelection: Set<String>
        get() = modules.filter { it.defaultEnabled }.map { it.id }.toSet()

    /**
     * Runs one deep scan over [enabledModuleIds].
     *
     * Returns a [DeepScanOutcome] for every non-throwing path, including
     * cancellation and "nothing could be read". [onProgress] is invoked on the
     * caller's dispatcher with real counters only.
     */
    suspend fun scan(
        enabledModuleIds: Set<String>,
        excludedUris: Set<String> = emptySet(),
        onProgress: (DeepScanProgress) -> Unit = {},
    ): DeepScanOutcome {
        cancelled.set(false)
        val startedAt = System.currentTimeMillis()
        val sessionId = UUID.randomUUID().toString()
        val selected = modules.filter { it.id in enabledModuleIds }.sortedBy { it.order }
        val results = mutableListOf<ModuleResult>()
        val scopesScanned = linkedSetOf<AccessScope>()
        val issues = linkedSetOf<String>()

        if (selected.isEmpty()) {
            _progress.value = null
            return DeepScanOutcome(
                sessionId = sessionId,
                startedAtMillis = startedAt,
                finishedAtMillis = System.currentTimeMillis(),
                moduleResults = emptyList(),
                scopesScanned = emptySet(),
                status = DeepScanStatus.UNAVAILABLE,
                note = "No module was selected, so nothing was measured",
            )
        }

        fun publish(label: String, currentModuleId: String? = null) {
            val distinct = results.flatMap { it.candidates }.distinctBy { it.uri }
            val update = DeepScanProgress(
                currentModuleId = currentModuleId,
                modulesCompleted = results.size,
                modulesTotal = selected.size,
                itemsFound = distinct.size,
                bytesFound = distinct.sumOf { it.bytes },
                label = label,
                completedBytes = results.associate { it.moduleId to it.totalBytes },
                completedItems = results.associate { it.moduleId to it.itemCount },
                completedStatus = results.associate { it.moduleId to it.status },
            )
            _progress.value = update
            onProgress(update)
        }

        val outcome = try {
            // One real enumeration per readable scope, shared by every module.
            val baselineModules = selected.filter { it.id != "duplicates" }
            val baselineScopes = baselineModules.flatMap { it.scopes }.toSet()
            var pool: List<FileCandidate> = emptyList()
            var snapshots: List<SourceSnapshot> = emptyList()

            if (baselineScopes.isNotEmpty()) {
                publish("Reading granted scopes")
                val (session, candidates) = scanner.scan(
                    scopes = baselineScopes,
                    excludedUris = excludedUris,
                    onProgress = { p -> publish(p.label) },
                )
                pool = candidates
                snapshots = session.snapshots
                scopesScanned += session.scopes
            }

            for (module in selected) {
                currentCoroutineContext().ensureActive()
                if (cancelled.get()) {
                    issues += "Stopped by the customer after ${results.size} module(s)"
                    break
                }

                publish("Measuring ${module.title.text(false)}", currentModuleId = module.id)
                val result = when (module.id) {
                    "duplicates" -> duplicateModule(module, excludedUris)
                    else -> poolModule(module, pool, snapshots)
                }
                results += result

                publish("Measured ${module.title.text(false)}")
            }

            val finishedAt = System.currentTimeMillis()
            val measured = results.count { it.status == DataStatus.MEASURED || it.status == DataStatus.VERIFIED }
            val partial = results.count { it.status == DataStatus.PARTIAL }
            val unavailable = results.count { it.status == DataStatus.UNAVAILABLE }

            val status = DeepScanRules.resolveStatus(
                cancelled = cancelled.get(),
                measuredModules = measured,
                partialModules = partial,
                unavailableModules = unavailable,
            )
            DeepScanOutcome(
                sessionId = sessionId,
                startedAtMillis = startedAt,
                finishedAtMillis = finishedAt,
                moduleResults = results.toList(),
                scopesScanned = scopesScanned,
                status = status,
                note = buildList {
                    addAll(issues)
                    if (unavailable > 0) add("$unavailable module(s) returned no measurable data")
                }.takeIf { it.isNotEmpty() }?.joinToString("; "),
            )
        } catch (failure: kotlinx.coroutines.CancellationException) {
            throw failure
        } catch (failure: Exception) {
            val finishedAt = System.currentTimeMillis()
            DeepScanOutcome(
                sessionId = sessionId,
                startedAtMillis = startedAt,
                finishedAtMillis = finishedAt,
                moduleResults = results.toList(),
                scopesScanned = scopesScanned,
                status = DeepScanStatus.FAILED,
                note = "Scan engine error: ${failure.javaClass.simpleName}: ${failure.message}",
            )
        } finally {
            _progress.value = null
        }
        return outcome
    }

    // ── Module builders ─────────────────────────────────────────────────────

    private fun poolModule(
        module: DeepScanModule,
        pool: List<FileCandidate>,
        snapshots: List<SourceSnapshot>,
    ): ModuleResult {
        val relevant = snapshots.filter { it.scope in module.scopes }
        val grantedScopes = relevant.filter { it.grant in READABLE || it.grant == PermissionGrant.NOT_REQUIRED }
        val findings = when (module.id) {
            "app_cache" -> pool.filter { it.provenance == DataProvenance.APP_OWNED_CACHE }
            "media_images" -> pool.filter {
                it.provenance == DataProvenance.MEDIASTORE && it.category == CandidateCategory.IMAGE
            }
            "media_video" -> pool.filter {
                it.provenance == DataProvenance.MEDIASTORE && it.category == CandidateCategory.VIDEO
            }
            "documents" -> pool.filter { it.provenance == DataProvenance.SAF_DOCUMENT }
            "large_files" -> pool.filter { DeepScanRules.isLargeFile(it.bytes) }
            "old_files" -> {
                val now = System.currentTimeMillis()
                pool.filter { DeepScanRules.isOldFile(it.lastModifiedMillis, now) }
            }
            else -> emptyList()
        }

        val status = when {
            relevant.isEmpty() -> DataStatus.UNAVAILABLE
            grantedScopes.isEmpty() -> DataStatus.UNAVAILABLE
            relevant.any { it.status == DataStatus.UNAVAILABLE } -> DataStatus.PARTIAL
            relevant.any { it.status == DataStatus.PARTIAL } -> DataStatus.PARTIAL
            else -> DataStatus.MEASURED
        }
        val detail = when {
            relevant.isEmpty() -> "This module needs a scope that was not part of the readable set"
            grantedScopes.isEmpty() -> "Not granted: ${module.scopes.joinToString { it.name }}"
            status == DataStatus.PARTIAL -> relevant.firstOrNull { it.detail != null }?.detail
                ?: "Only part of this scope could be read"
            else -> null
        }
        return ModuleResult(
            moduleId = module.id,
            order = module.order,
            itemCount = findings.size,
            totalBytes = findings.sumOf { it.bytes },
            candidates = findings,
            status = status,
            detail = detail,
        )
    }

    private suspend fun duplicateModule(
        module: DeepScanModule,
        excludedUris: Set<String>,
    ): ModuleResult {
        val readableScopes = buildSet {
            if (permissions.mediaGrant(AccessScope.MEDIA_IMAGES) in READABLE) add(AccessScope.MEDIA_IMAGES)
            if (permissions.mediaGrant(AccessScope.MEDIA_VIDEO) in READABLE) add(AccessScope.MEDIA_VIDEO)
            if (permissions.safTrees(requireWrite = false).isNotEmpty()) add(AccessScope.SAF_TREE)
        }
        if (readableScopes.isEmpty()) {
            return ModuleResult(
                moduleId = module.id,
                order = module.order,
                itemCount = 0,
                totalBytes = 0L,
                candidates = emptyList(),
                status = DataStatus.UNAVAILABLE,
                detail = "No readable scope for content hashing",
            )
        }
        val outcome = duplicates.scan(readableScopes, excludedUris) { p -> publishDuplicateProgress(p) }
        // Keepers are excluded by construction: only the redundant copies of each
        // group are offered, so a group can never delete the file it calls original.
        val redundant = outcome.groups.flatMap { group ->
            group.duplicates.map { copy ->
                copy.copy(
                    reason = "Byte-identical to the protected copy \"${group.keeper.displayName}\" " +
                        "(SHA-256 ${group.contentHash.take(12)}…)",
                )
            }
        }
        val detail = buildString {
            append(outcome.groups.size).append(" group(s)")
            append(" · ").append(outcome.hashedCount).append(" file(s) hashed")
            if (outcome.status == DataStatus.PARTIAL) append(" · partial read")
            outcome.note?.let { append(" · ").append(it) }
        }
        return ModuleResult(
            moduleId = module.id,
            order = module.order,
            itemCount = redundant.size,
            totalBytes = redundant.sumOf { it.bytes },
            candidates = redundant,
            status = outcome.status,
            detail = detail,
        )
    }

    private fun publishDuplicateProgress(progress: DuplicateRepository.Progress) {
        // Hashing is real work but not a module boundary; report the counters
        // without pretending the module fraction advanced.
        val update = _progress.value ?: return
        val next = update.copy(
            itemsFound = progress.filesInspected,
            bytesFound = progress.bytesHashed,
            label = progress.label,
        )
        _progress.value = next
    }
}
