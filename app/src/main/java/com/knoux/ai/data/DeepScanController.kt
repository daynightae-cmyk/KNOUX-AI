package com.knoux.ai.data

import android.content.IntentSender
import androidx.room.withTransaction
import com.knoux.ai.Copy
import com.knoux.ai.ar
import com.knoux.ai.visual.DeepScanHistory
import com.knoux.ai.visual.DeepScanModule
import com.knoux.ai.visual.DeepScanOutcome
import com.knoux.ai.visual.DeepScanProgress
import com.knoux.ai.visual.DeepScanStatus
import com.knoux.ai.visual.Facet
import com.knoux.ai.visual.FacetBreakdown
import com.knoux.ai.visual.FacetState
import com.knoux.ai.visual.InstrumentFrame
import com.knoux.ai.visual.InstrumentPhase
import com.knoux.ai.visual.ModuleResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * PC05 orchestration.
 *
 * Discovery and hashing belong to the proven engines; this controller owns the
 * pass lifecycle, the durable evidence and the exact mapping from measured state
 * to the Abyss Sonar frame. Nothing here animates a value that was not counted:
 * depth is `completedModules / selectedModules`, and every facet's weight is the
 * byte total the module really returned.
 */
internal class DeepScanController(
    private val repository: DeepScanRepository,
    private val database: KnouxDatabase,
    private val settings: SettingsStore,
    private val clean: SmartCleanController,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    companion object {
        /** Outcomes that mean the file was re-read and confirmed gone. */
        private val VANISHED = setOf("REMOVED_VERIFIED", "TRASHED_VERIFIED", "GONE_VERIFIED",
            "ALREADY_GONE", "ALREADY_TRASHED")
    }

    val modules: List<DeepScanModule> get() = repository.modules

    private val scanGate = Mutex()

    private val _selected = MutableStateFlow(repository.defaultSelection)

    /** Modules the customer selected, persisted so a later pass keeps the choice. */
    val selectedModules: StateFlow<Set<String>> = _selected.asStateFlow()

    private val _lastOutcome = MutableStateFlow<DeepScanOutcome?>(null)

    /** The most recent measured pass, in memory or restored from Room. */
    val lastOutcome: StateFlow<DeepScanOutcome?> = _lastOutcome.asStateFlow()

    val progress: StateFlow<DeepScanProgress?> get() = repository.progress

    private val _frame = MutableStateFlow(idleFrame(repository.defaultSelection, null))

    /** The single source the Abyss Sonar renders. */
    val instrumentFrame: StateFlow<InstrumentFrame> = _frame.asStateFlow()

    /**
     * Restores persisted selection and the last finished pass, so returning to the
     * screen shows evidence instead of an empty instrument.
     */
    suspend fun hydrate() = withContext(ioDispatcher) {
        val storedSelection = settings.deepScanModules.first()
        if (storedSelection.isNotEmpty()) {
            _selected.value = storedSelection.filter { id -> modules.any { it.id == id } }.toSet()
        }
        val restored = readLatestOutcome()
        if (restored != null) {
            _lastOutcome.value = restored
            _frame.value = frameFor(restored, _selected.value, motion = true)
        } else {
            _frame.value = idleFrame(_selected.value, null)
        }
    }

    suspend fun toggleModule(moduleId: String, enabled: Boolean) = withContext(ioDispatcher) {
        if (modules.none { it.id == moduleId }) return@withContext
        val next = _selected.value.toMutableSet()
        if (enabled) next += moduleId else next -= moduleId
        _selected.value = next
        settings.setDeepScanModules(next)
        if (_frame.value.phase != InstrumentPhase.RUNNING) {
            _frame.value = idleFrame(next, _lastOutcome.value)
        }
    }

    suspend fun selectAll(enabled: Boolean) = withContext(ioDispatcher) {
        val next = if (enabled) modules.map { it.id }.toSet() else emptySet()
        _selected.value = next
        settings.setDeepScanModules(next)
        if (_frame.value.phase != InstrumentPhase.RUNNING) {
            _frame.value = idleFrame(next, _lastOutcome.value)
        }
    }

    /**
     * Runs one pass. A cancelled or failed pass still returns its measured part;
     * only a genuine engine crash becomes [Result.failure].
     */
    suspend fun scan(motion: Boolean = true): Result<DeepScanOutcome> = try {
        val outcome = scanGate.withLock {
            withContext(ioDispatcher) {
                val selection = _selected.value
                if (selection.isEmpty()) {
                    _frame.value = idleFrame(selection, _lastOutcome.value).copy(
                        phase = InstrumentPhase.FAILED,
                        caption = "No module selected" ar "لم تُحدد وحدة",
                    )
                    return@withContext null
                }
                _frame.value = InstrumentFrame(
                    phase = InstrumentPhase.ARMED,
                    progress = null,
                    heroValue = null,
                    facets = modules.map { module ->
                        facet(module, null, selection)
                    },
                    motion = motion,
                    caption = "Reading access" ar "قراءة صلاحيات الوصول",
                    lastResultAgeMillis = _lastOutcome.value?.let { ageOf(it.finishedAtOrStartedMillis) },
                )
                val excluded = database.exclusionDao().uris().toSet()
                val measured = repository.scan(selection, excluded) { p ->
                    _frame.value = frameFor(p, selection, motion)
                }
                persist(measured)
                _lastOutcome.value = measured
                _frame.value = frameFor(measured, selection, motion)
                measured
            }
        }
        if (outcome == null) Result.failure(IllegalStateException("No deep scan module was selected"))
        else Result.success(outcome)
    } catch (cancelled: CancellationException) {
        _frame.value = _frame.value.copy(
            phase = InstrumentPhase.PAUSED,
            caption = "Stopped" ar "متوقف",
        )
        throw cancelled
    } catch (failure: Exception) {
        _frame.value = _frame.value.copy(
            phase = InstrumentPhase.FAILED,
            caption = "Not measured" ar "لم يُقس",
        )
        Result.failure(failure)
    }

    /** Stops the running pass; finished modules stay measured and persisted. */
    fun cancel() = repository.cancel()

    suspend fun latestOutcome(): DeepScanOutcome? = withContext(ioDispatcher) {
        _lastOutcome.value ?: readLatestOutcome()
    }

    fun observeHistory(): Flow<List<DeepScanHistory>> = database.deepScanDao().observeSessions().map { rows ->
        rows.mapNotNull { row ->
            val status = runCatching { DeepScanStatus.valueOf(row.status) }.getOrNull() ?: return@mapNotNull null
            DeepScanHistory(
                sessionId = row.id,
                capturedAtMillis = row.finishedAt ?: row.startedAt,
                status = status,
                moduleCount = row.moduleCount,
                moduleMeasured = row.moduleMeasured,
                moduleUnavailable = row.moduleUnavailable,
                itemCount = row.itemCount,
                totalBytes = row.totalBytes,
                note = row.note,
            )
        }
    }

    // ── Execution: delegation to the proven PC01 path ───────────────────────

    /** Freezes and validates a plan, returning the OS consent request when needed. */
    suspend fun prepareRemoval(plan: OperationPlan, acknowledgedIrreversible: Boolean): IntentSender? =
        clean.prepareMediaOperation(plan, acknowledgedIrreversible)

    /** Executes app-owned and SAF removals directly, with Room history and re-reads. */
    suspend fun executeLocal(plan: OperationPlan, acknowledgedIrreversible: Boolean): OperationResult =
        clean.executeLocal(plan, acknowledgedIrreversible)

    /** Completes an OS-mediated media request that the customer approved or rejected. */
    suspend fun completeMedia(planId: String, approved: Boolean): OperationResult =
        clean.completeMediaOperation(planId, approved)

    fun newPlanId(): String = clean.newPlanId()

    /**
     * Drops findings the platform re-read as gone, then recomputes the affected
     * module and session totals from the surviving evidence row by row.
     */
    suspend fun reconcileRemoval(planId: String) = withContext(ioDispatcher) {
        val items = database.operationDao().items(planId)
        val gone = items.filter { it.outcome in VANISHED }.map { it.uri }.toSet()
        if (gone.isEmpty()) return@withContext
        val latest = database.deepScanDao().latestFinishedSession()?.id ?: return@withContext
        database.withTransaction {
            gone.forEach { uri -> database.deepScanDao().removeFinding(uri) }
            database.deepScanDao().refreshModuleTotals(latest)
            database.deepScanDao().refreshSessionTotals(latest)
        }
        val refreshed = readLatestOutcome()
        if (refreshed != null) {
            _lastOutcome.value = refreshed
            if (_frame.value.phase != InstrumentPhase.RUNNING) {
                _frame.value = frameFor(refreshed, _selected.value, _frame.value.motion)
            }
        }
    }

    // ── Persistence ─────────────────────────────────────────────────────────

    private suspend fun persist(outcome: DeepScanOutcome) {
        val distinct = outcome.distinctCandidates
        val measured = outcome.moduleResults.count {
            it.status == DataStatus.MEASURED || it.status == DataStatus.VERIFIED || it.status == DataStatus.PARTIAL
        }
        val unavailable = outcome.moduleResults.count { it.status == DataStatus.UNAVAILABLE }
        database.withTransaction {
            database.deepScanDao().upsertSession(
                DeepScanSessionEntity(
                    id = outcome.sessionId,
                    startedAt = outcome.startedAtMillis,
                    finishedAt = outcome.finishedAtMillis,
                    status = outcome.status.name,
                    scopes = outcome.scopesScanned.joinToString(",") { it.name },
                    moduleCount = outcome.moduleResults.size,
                    moduleMeasured = measured,
                    moduleUnavailable = unavailable,
                    itemCount = distinct.size,
                    totalBytes = distinct.sumOf { it.bytes },
                    note = outcome.note,
                )
            )
            if (outcome.moduleResults.isNotEmpty()) {
                database.deepScanDao().insertModules(
                    outcome.moduleResults.map { result ->
                        DeepScanModuleEntity(
                            scanId = outcome.sessionId,
                            moduleId = result.moduleId,
                            status = result.status.name,
                            itemCount = result.itemCount,
                            totalBytes = result.totalBytes,
                            detail = result.detail,
                            recordedAt = outcome.finishedAtOrStartedMillis,
                        )
                    }
                )
            }
            val findings = outcome.moduleResults.flatMap { result ->
                result.candidates.map { candidate ->
                    DeepScanFindingEntity(
                        scanId = outcome.sessionId,
                        moduleId = result.moduleId,
                        uri = candidate.uri,
                        displayName = candidate.displayName,
                        mimeType = candidate.mimeType,
                        bytes = candidate.bytes,
                        lastModified = candidate.lastModifiedMillis,
                        category = candidate.category.name,
                        provenance = candidate.provenance.name,
                        eligibility = candidate.eligibility.name,
                        reason = candidate.reason,
                        status = candidate.status.name,
                        physicalKey = candidate.physicalKey,
                    )
                }
            }.distinctBy { it.moduleId to it.uri }
            if (findings.isNotEmpty()) database.deepScanDao().insertFindings(findings)
        }
    }

    private suspend fun readLatestOutcome(): DeepScanOutcome? {
        val session = database.deepScanDao().latestFinishedSession() ?: return null
        val moduleRows = database.deepScanDao().modules(session.id)
        val findingRows = database.deepScanDao().findings(session.id)
        val byModule = findingRows.groupBy { it.moduleId }
        var degraded = session.note
        val results = moduleRows.mapNotNull { row ->
            val module = modules.firstOrNull { it.id == row.moduleId }
            if (module == null) {
                degraded = listOfNotNull(degraded, "A stored module is unknown to this build").joinToString("; ")
                return@mapNotNull null
            }
            val candidates = byModule[row.moduleId].orEmpty().mapNotNull { it.toCandidate() }
            ModuleResult(
                moduleId = row.moduleId,
                order = module.order,
                itemCount = candidates.size,
                totalBytes = candidates.sumOf { it.bytes },
                candidates = candidates,
                status = deepEnum<DataStatus>(row.status) ?: DataStatus.PARTIAL,
                detail = row.detail,
            )
        }
        return DeepScanOutcome(
            sessionId = session.id,
            startedAtMillis = session.startedAt,
            finishedAtMillis = session.finishedAt,
            moduleResults = results,
            scopesScanned = session.scopes.split(',').mapNotNull { name ->
                AccessScope.entries.firstOrNull { it.name == name }
            }.toSet(),
            status = runCatching { DeepScanStatus.valueOf(session.status) }
                .getOrElse { DeepScanStatus.PARTIAL },
            note = degraded,
        )
    }

    // ── Frame mapping ───────────────────────────────────────────────────────

    private fun idleFrame(selection: Set<String>, last: DeepScanOutcome?): InstrumentFrame {
        val age = last?.let { ageOf(it.finishedAtOrStartedMillis) }
        val caption: Copy = when {
            age != null -> "Last scan kept" ar "آخر فحص محفوظ"
            else -> "Ready to dive" ar "جاهز للغوص"
        }
        return InstrumentFrame(
            phase = InstrumentPhase.IDLE,
            progress = null,
            heroValue = last?.totalBytes,
            facets = modules.map { facet(it, null, selection) },
            motion = true,
            caption = caption,
            lastResultAgeMillis = age,
        )
    }

    private fun frameFor(progress: DeepScanProgress, selection: Set<String>, motion: Boolean): InstrumentFrame {
        val fraction = if (progress.modulesTotal > 0) {
            progress.modulesCompleted.toFloat() / progress.modulesTotal
        } else null
        val runningModule = modules.firstOrNull { it.id == progress.currentModuleId }
        val caption: Copy = when {
            fraction == null || progress.modulesCompleted == 0 -> "Listening" ar "إصغاء"
            runningModule != null -> "Measuring" ar "قياس"
            else -> "Deepening" ar "تعميق"
        }
        return InstrumentFrame(
            phase = InstrumentPhase.RUNNING,
            // Null until the first module really finishes: the sweep then means
            // "work in progress, depth unknown", not a fabricated percentage.
            progress = fraction?.takeIf { it > 0f },
            heroValue = progress.bytesFound.takeIf { progress.modulesCompleted > 0 },
            facets = modules.map { module ->
                if (module.id !in selection) return@map facet(module, null, selection)
                val status = progress.completedStatus[module.id]
                val bytes = progress.completedBytes[module.id] ?: 0L
                val items = progress.completedItems[module.id] ?: 0
                facet(
                    module = module,
                    result = status?.let { ModuleResult(module.id, module.order, items, bytes, emptyList(), it) },
                    selection = selection,
                )
            },
            motion = motion,
            caption = caption,
            lastResultAgeMillis = null,
        )
    }

    private fun frameFor(outcome: DeepScanOutcome, selection: Set<String>, motion: Boolean): InstrumentFrame {
        val phase = when (outcome.status) {
            DeepScanStatus.COMPLETE -> InstrumentPhase.RESULT
            DeepScanStatus.PARTIAL -> InstrumentPhase.PARTIAL
            DeepScanStatus.UNAVAILABLE, DeepScanStatus.FAILED -> InstrumentPhase.FAILED
            DeepScanStatus.CANCELLED -> InstrumentPhase.PAUSED
        }
        val caption: Copy = when (outcome.status) {
            DeepScanStatus.COMPLETE -> "Scan complete" ar "اكتمل الفحص"
            DeepScanStatus.PARTIAL -> "Partial read" ar "قراءة جزئية"
            DeepScanStatus.UNAVAILABLE -> "Nothing measured" ar "لم يُقس شيء"
            DeepScanStatus.CANCELLED -> "Stopped" ar "متوقف"
            DeepScanStatus.FAILED -> "Scan failed" ar "فشل الفحص"
        }
        val byId = outcome.moduleResults.associateBy { it.moduleId }
        return InstrumentFrame(
            phase = phase,
            progress = if (outcome.status == DeepScanStatus.CANCELLED) null else 1f,
            heroValue = if (outcome.status == DeepScanStatus.UNAVAILABLE) null else outcome.totalBytes,
            facets = modules.map { module -> facet(module, byId[module.id], selection) },
            motion = motion,
            caption = caption,
            lastResultAgeMillis = ageOf(outcome.finishedAtOrStartedMillis),
        )
    }

    private fun facet(module: DeepScanModule, result: ModuleResult?, selection: Set<String>): Facet {
        if (module.id !in selection) {
            return Facet(
                id = module.id,
                weight = 0L,
                state = FacetState.BLOCKED,
                label = module.shortTitle,
                note = "Not selected",
            )
        }
        if (result == null) {
            return Facet(
                id = module.id,
                weight = 0L,
                state = FacetState.FOGGED,
                label = module.shortTitle,
                note = "Not measured yet",
            )
        }
        val state = when (result.status) {
            DataStatus.MEASURED, DataStatus.VERIFIED -> FacetState.MEASURED
            DataStatus.PARTIAL -> FacetState.THIN
            else -> FacetState.FOGGED
        }
        val removable = result.candidates.count { it.eligibility == Eligibility.ELIGIBLE }
        val unselectable = result.candidates.count { it.eligibility == Eligibility.UNSELECTABLE }
        val protected = result.candidates.count { it.eligibility == Eligibility.PROTECTED }
        return Facet(
            id = module.id,
            weight = result.totalBytes,
            state = state,
            label = module.shortTitle,
            breakdown = FacetBreakdown(removable, protected, unselectable),
            provenance = result.candidates.map { it.provenance.name }.distinct()
                .takeIf { it.isNotEmpty() }?.joinToString(" + "),
            note = result.detail,
        )
    }

    private fun ageOf(capturedAtMillis: Long): Long =
        (System.currentTimeMillis() - capturedAtMillis).coerceAtLeast(0L)
}

private fun DeepScanFindingEntity.toCandidate(): FileCandidate? {
    val category = deepEnum<CandidateCategory>(category) ?: return null
    val provenance = deepEnum<DataProvenance>(provenance) ?: return null
    val eligibility = deepEnum<Eligibility>(eligibility) ?: return null
    val status = deepEnum<DataStatus>(status) ?: DataStatus.PARTIAL
    return FileCandidate(
        uri = uri,
        displayName = displayName,
        mimeType = mimeType,
        bytes = bytes,
        lastModifiedMillis = lastModified,
        category = category,
        provenance = provenance,
        eligibility = eligibility,
        reason = reason,
        status = status,
        physicalKey = physicalKey,
    )
}

private inline fun <reified T : Enum<T>> deepEnum(name: String): T? =
    enumValues<T>().firstOrNull { it.name == name }
