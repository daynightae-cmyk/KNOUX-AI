package com.knoux.ai.data

import android.content.IntentSender
import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

/** A persisted residue snapshot. Loading it does not make its measurements fresh. */
data class ResidueSnapshot(
    val scanId: String,
    val capturedAtMillis: Long,
    val scopes: Set<AccessScope>,
    val minAgeDays: Int,
    val items: List<ResidueItem>,
    val fileCount: Int,
    val status: DataStatus,
    val note: String?,
) {
    val bytes: Long get() = items.sumOf { it.candidate.bytes }
    val sharedCount: Int get() = items.count { it.confidence != ResidueConfidence.OWNED }
}

data class ResidueHistory(
    val scanId: String,
    val capturedAtMillis: Long,
    val itemCount: Int,
    val bytes: Long,
    val fileCount: Int,
    val status: DataStatus,
)

/** An audit entry. "Kept everything" is recorded just as plainly as a removal. */
data class ResidueAuditEntry(
    val id: String,
    val scanId: String?,
    val action: ResidueAction,
    val itemCount: Int,
    val bytes: Long,
    val createdAtMillis: Long,
    val note: String?,
)

enum class ResidueAction { REVIEWED, DRY_RUN, SKIPPED, ATTEMPTED, VERIFIED }

/** A saved rule for one granted folder. */
data class ResidueProfile(
    val treeUri: String,
    val label: String,
    val minAgeDays: Int,
    val patterns: Set<ResiduePattern>,
    val updatedAtMillis: Long,
)

/**
 * PC03 orchestration.
 *
 * Discovery is delegated to [ResidueRepository]. Execution deliberately reuses the
 * proven PC01 controller, so the OS dialog, the durable claim, the after-state
 * re-query and the Room operation history are the exact code path Smart Clean and
 * Duplicates already proved.
 *
 * The audit trail here records the *decision*, including decisions that removed
 * nothing, and never stores a directory listing or a raw path in its note.
 */
class ResidueController(
    private val repository: ResidueRepository,
    private val database: KnouxDatabase,
    private val settings: SettingsStore,
    private val permissions: PermissionState,
    private val clean: SmartCleanController,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    val selectedScopes: Flow<Set<AccessScope>> get() = settings.residueScopes
    val minAgeDays: Flow<Int> get() = settings.residueMinAgeDays
    val profiles: Flow<List<ResidueProfile>> get() = database.residueDao().observeProfiles().map { rows ->
        rows.map { it.toDomain() }
    }

    internal fun describeScope(scope: AccessScope): com.knoux.ai.Copy = clean.describeScope(scope)
    fun trashCapability(): FeatureCapability = repository.trashCapability()

    suspend fun setScopes(scopes: Set<AccessScope>) = settings.setResidueScopes(scopes)
    suspend fun setMinAgeDays(days: Int) = settings.setResidueMinAgeDays(days)
    fun newPlanId(): String = UUID.randomUUID().toString()

    /** Granted folders the OS still exposes. A revoked tree is never scanned. */
    fun grantedTrees(): List<android.net.Uri> = permissions.safTrees(requireWrite = true)

    suspend fun scan(
        scopes: Set<AccessScope>,
        ageDays: Int,
        onProgress: (ResidueRepository.Progress) -> Unit = {},
    ): Result<ResidueSnapshot> = try {
        withContext(ioDispatcher) {
            val excluded = database.exclusionDao().uris().toSet()
            val outcome = repository.scan(
                scopes = scopes,
                trees = if (AccessScope.SAF_TREE in scopes) grantedTrees() else emptyList(),
                minAgeDays = ageDays,
                excludedUris = excluded,
                onProgress = onProgress,
            )
            database.withTransaction {
                database.residueDao().upsertScan(
                    ResidueScanEntity(
                        id = outcome.session.id,
                        startedAt = outcome.session.startedAtMillis,
                        finishedAt = outcome.session.finishedAtMillis,
                        status = outcome.status.name,
                        scopes = outcome.session.scopes.joinToString(",") { it.name },
                        minAgeDays = ageDays,
                        fileCount = outcome.filesInspected,
                        itemCount = outcome.items.size,
                        bytes = outcome.items.sumOf { it.candidate.bytes },
                        note = outcome.note,
                    )
                )
                database.residueDao().clearItems(outcome.session.id)
                if (outcome.items.isNotEmpty()) {
                    database.residueDao().insertItems(outcome.items.map { it.toEntity(outcome.session.id) })
                }
            }
            Result.success(
                ResidueSnapshot(
                    scanId = outcome.session.id,
                    capturedAtMillis = outcome.session.finishedAtMillis ?: outcome.session.startedAtMillis,
                    scopes = outcome.session.scopes,
                    minAgeDays = ageDays,
                    items = outcome.items,
                    fileCount = outcome.filesInspected,
                    status = outcome.status,
                    note = outcome.note,
                )
            )
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(failure)
    }

    suspend fun latest(): ResidueSnapshot? = withContext(ioDispatcher) {
        val scan = database.residueDao().latestFinishedScan() ?: return@withContext null
        val rows = database.residueDao().items(scan.id)
        var degraded = scan.note
        val items = rows.mapNotNull { row ->
            val item = row.toDomain()
            if (item == null) {
                degraded = listOfNotNull(degraded, "A stored residue row was unreadable and is not shown")
                    .joinToString("; ")
            }
            item
        }
        ResidueSnapshot(
            scanId = scan.id,
            capturedAtMillis = scan.finishedAt ?: scan.startedAt,
            scopes = scan.scopes.split(',').mapNotNull { name ->
                AccessScope.entries.firstOrNull { it.name == name }
            }.toSet(),
            minAgeDays = scan.minAgeDays,
            items = items,
            fileCount = scan.fileCount,
            status = when {
                items.size != rows.size -> DataStatus.PARTIAL
                enumNamed<DataStatus>(scan.status) == DataStatus.MEASURED -> DataStatus.MEASURED
                else -> DataStatus.PARTIAL
            },
            note = degraded,
        )
    }

    fun observeHistory(): Flow<List<ResidueHistory>> = database.residueDao().observeScans().map { rows ->
        rows.mapNotNull { row ->
            ResidueHistory(
                scanId = row.id,
                capturedAtMillis = row.finishedAt ?: row.startedAt,
                itemCount = row.itemCount,
                bytes = row.bytes,
                fileCount = row.fileCount,
                status = enumNamed<DataStatus>(row.status) ?: DataStatus.PARTIAL,
            )
        }
    }

    fun observeAudit(): Flow<List<ResidueAuditEntry>> = database.residueDao().observeDecisions().map { rows ->
        rows.mapNotNull { row ->
            val action = enumNamed<ResidueAction>(row.action) ?: return@mapNotNull null
            ResidueAuditEntry(
                id = row.id,
                scanId = row.scanId,
                action = action,
                itemCount = row.itemCount,
                bytes = row.bytes,
                createdAtMillis = row.createdAt,
                note = row.note,
            )
        }
    }

    /**
     * Records a decision. Only pattern names and counts are stored, never a path,
     * so an exported audit cannot leak where the customer keeps their files.
     */
    suspend fun recordDecision(
        action: ResidueAction,
        scanId: String?,
        items: List<ResidueItem>,
        note: String? = null,
    ) = withContext(ioDispatcher) {
        database.residueDao().putDecision(
            ResidueDecisionEntity(
                id = UUID.randomUUID().toString(),
                scanId = scanId,
                action = action.name,
                itemCount = items.size,
                bytes = items.sumOf { it.candidate.bytes },
                createdAt = System.currentTimeMillis(),
                note = note ?: items.groupingBy { it.pattern }.eachCount()
                    .entries.sortedByDescending { it.value }
                    .joinToString(", ") { (pattern, count) -> "${pattern.name.lowercase()} x$count" }
                    .takeIf { it.isNotBlank() },
            )
        )
    }

    // ── Execution (delegates to the proven PC01 engine) ──────────────────────

    /** Cache and SAF items are removed directly after the customer's own confirmation. */
    suspend fun executeLocal(plan: OperationPlan, acknowledgedIrreversible: Boolean): OperationResult {
        val result = clean.executeLocal(plan, acknowledgedIrreversible)
        reconcileRemoved(plan.id)
        return result
    }

    suspend fun prepareMedia(plan: OperationPlan, acknowledgedIrreversible: Boolean): IntentSender? =
        clean.prepareMediaOperation(plan, acknowledgedIrreversible)

    suspend fun completeMedia(planId: String, approved: Boolean): OperationResult {
        val result = clean.completeMediaOperation(planId, approved)
        reconcileRemoved(planId)
        return result
    }

    suspend fun recoverInterrupted(exceptPlanId: String? = null) {
        clean.recoverInterruptedOperations(exceptPlanId)
    }

    /**
     * Drops only URIs the operation itself verified as changed.
     *
     * A provider that refused, a cancelled OS dialog and an unverifiable result all
     * leave the row in place, so the screen never loses an item it cannot prove gone.
     */
    suspend fun reconcileRemoved(planId: String): Set<String> = withContext(ioDispatcher) {
        val removed = try {
            clean.verifiedRemovedUris(planId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            emptySet()
        }
        if (removed.isEmpty()) return@withContext emptySet()
        database.withTransaction {
            removed.forEach { uri -> database.residueDao().removeItem(uri) }
            database.residueDao().latestFinishedScan()?.let { scan ->
                database.residueDao().refreshTotals(scan.id)
            }
        }
        removed
    }

    // ── Profiles (customer rules for granted folders) ────────────────────────

    suspend fun saveProfile(treeUri: String, label: String, ageDays: Int, patterns: Set<ResiduePattern>) =
        withContext(ioDispatcher) {
            val existing = database.residueDao().profile(treeUri)
            database.residueDao().putProfile(
                ResidueProfileEntity(
                    treeUri = treeUri,
                    label = label.take(80),
                    minAgeDays = ageDays,
                    patterns = patterns.joinToString(",") { it.name },
                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }

    suspend fun removeProfile(treeUri: String) = withContext(ioDispatcher) {
        database.residueDao().removeProfile(treeUri)
    }

    /**
     * Drops profiles whose grant disappeared. Called after the OS is re-queried, so
     * a revoked folder cannot keep claiming rules that can no longer be applied.
     */
    suspend fun pruneRevokedProfiles(granted: Set<String>): Int = withContext(ioDispatcher) {
        val rows = database.residueDao().profiles()
        val stale = rows.filter { it.treeUri !in granted }
        stale.forEach { database.residueDao().removeProfile(it.treeUri) }
        stale.size
    }

    // ── Mapping ──────────────────────────────────────────────────────────────

    private fun ResidueItem.toEntity(scanId: String): ResidueItemEntity = ResidueItemEntity(
        scanId = scanId,
        uri = candidate.uri,
        displayName = candidate.displayName,
        mimeType = candidate.mimeType,
        bytes = candidate.bytes,
        lastModified = candidate.lastModifiedMillis,
        category = candidate.category.name,
        provenance = candidate.provenance.name,
        eligibility = candidate.eligibility.name,
        pattern = pattern.name,
        confidence = confidence.name,
        matchedOn = matchedOn,
        reason = candidate.reason,
    )

    private fun ResidueItemEntity.toDomain(): ResidueItem? {
        val category = enumNamed<CandidateCategory>(category) ?: return null
        val provenance = enumNamed<DataProvenance>(provenance) ?: return null
        val eligibility = enumNamed<Eligibility>(eligibility) ?: return null
        val pattern = enumNamed<ResiduePattern>(pattern) ?: return null
        val confidence = enumNamed<ResidueConfidence>(confidence) ?: return null
        return ResidueItem(
            candidate = FileCandidate(
                uri = uri,
                displayName = displayName,
                mimeType = mimeType,
                bytes = bytes,
                lastModifiedMillis = lastModified,
                category = category,
                provenance = provenance,
                eligibility = eligibility,
                reason = reason,
            ),
            pattern = pattern,
            confidence = confidence,
            matchedOn = matchedOn,
        )
    }

    private fun ResidueProfileEntity.toDomain(): ResidueProfile = ResidueProfile(
        treeUri = treeUri,
        label = label,
        minAgeDays = minAgeDays,
        patterns = patterns.split(',').mapNotNull { name ->
            ResiduePattern.entries.firstOrNull { it.name == name }
        }.toSet(),
        updatedAtMillis = updatedAt,
    )

    private inline fun <reified T : Enum<T>> enumNamed(name: String): T? =
        enumValues<T>().firstOrNull { it.name == name }
}
