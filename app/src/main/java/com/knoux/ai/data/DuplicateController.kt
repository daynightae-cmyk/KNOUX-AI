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

/** A persisted duplicate snapshot. Loading it does not make its measurements fresh. */
data class DuplicateSnapshot(
    val scanId: String,
    val capturedAtMillis: Long,
    val scopes: Set<AccessScope>,
    val groups: List<DuplicateGroup>,
    val fileCount: Int,
    val hashedCount: Int,
    val bytesHashed: Long,
    val status: DataStatus,
    val note: String?,
) {
    val redundantBytes: Long get() = groups.sumOf { it.redundantBytes }
    val duplicateCount: Int get() = groups.sumOf { it.duplicates.size }
}

data class DuplicateHistory(
    val scanId: String,
    val capturedAtMillis: Long,
    val groupCount: Int,
    val redundantBytes: Long,
    val fileCount: Int,
    val hashedCount: Int,
    val status: DataStatus,
)

/**
 * PC02 orchestration.
 *
 * Discovery is delegated to [DuplicateRepository]; execution deliberately reuses
 * the proven PC01 controller so the OS confirmation, claim, verification and Room
 * history are the same code path Smart Clean already proved. After a verified
 * removal the duplicate snapshot is reconciled from persisted evidence only —
 * never from a UI assumption.
 */
class DuplicateController(
    private val repository: DuplicateRepository,
    private val database: KnouxDatabase,
    private val settings: SettingsStore,
    private val clean: SmartCleanController,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    val selectedScopes: Flow<Set<AccessScope>> get() = settings.selectedScopes

    fun describeScope(scope: AccessScope): String = clean.describeScope(scope)

    suspend fun setScopes(scopes: Set<AccessScope>) = settings.setSelectedScopes(scopes)

    fun newPlanId(): String = UUID.randomUUID().toString()

    suspend fun scan(
        scopes: Set<AccessScope>,
        onProgress: (DuplicateRepository.Progress) -> Unit = {},
    ): Result<DuplicateSnapshot> = try {
        val snapshot = withContext(ioDispatcher) {
            val excluded = database.exclusionDao().uris().toSet()
            val outcome = repository.scan(scopes, excluded, onProgress)
            withContext(ioDispatcher) {
                database.withTransaction {
                    database.duplicateDao().upsertScan(
                        DuplicateScanEntity(
                            id = outcome.session.id,
                            startedAt = outcome.session.startedAtMillis,
                            finishedAt = outcome.session.finishedAtMillis,
                            status = outcome.status.name,
                            scopes = outcome.session.scopes.joinToString(",") { it.name },
                            fileCount = outcome.fileCount,
                            hashedCount = outcome.hashedCount,
                            bytesHashed = outcome.bytesHashed,
                            groupCount = outcome.groups.size,
                            redundantBytes = outcome.groups.sumOf { it.redundantBytes },
                            statusNote = outcome.note,
                        )
                    )
                    database.duplicateDao().clearGroups(outcome.session.id)
                    database.duplicateDao().clearMembers(outcome.session.id)
                    val groups = outcome.groups.map { it.toEntity(outcome.session.id) }
                    if (groups.isNotEmpty()) {
                        database.duplicateDao().insertGroups(groups.map { it.first })
                        database.duplicateDao().insertMembers(groups.flatMap { it.second })
                    }
                }
            }
            DuplicateSnapshot(
                scanId = outcome.session.id,
                capturedAtMillis = outcome.session.finishedAtMillis ?: outcome.session.startedAtMillis,
                scopes = outcome.session.scopes,
                groups = outcome.groups,
                fileCount = outcome.fileCount,
                hashedCount = outcome.hashedCount,
                bytesHashed = outcome.bytesHashed,
                status = outcome.status,
                note = outcome.note,
            )
        }
        Result.success(snapshot)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(failure)
    }

    suspend fun latest(): DuplicateSnapshot? = withContext(ioDispatcher) {
        val scan = database.duplicateDao().latestFinishedScan() ?: return@withContext null
        val groupRows = database.duplicateDao().groups(scan.id)
        val membersByGroup = database.duplicateDao().members(scan.id).groupBy { it.groupId }
        var degraded = scan.statusNote
        val groups = groupRows.mapNotNull { row ->
            val domain = row.toDomain(membersByGroup[row.id].orEmpty())
            if (domain == null) {
                degraded = listOfNotNull(degraded, "A stored duplicate group was unreadable and is not shown").joinToString("; ")
            }
            domain
        }
        DuplicateSnapshot(
            scanId = scan.id,
            capturedAtMillis = scan.finishedAt ?: scan.startedAt,
            scopes = scan.scopes.split(',').mapNotNull { name ->
                AccessScope.entries.firstOrNull { it.name == name }
            }.toSet(),
            groups = groups,
            fileCount = scan.fileCount,
            hashedCount = scan.hashedCount,
            bytesHashed = scan.bytesHashed,
            status = if (groups.size != groupRows.size) DataStatus.PARTIAL
            else enumNamed<DataStatus>(scan.status)?.let { if (it == DataStatus.MEASURED) DataStatus.MEASURED else DataStatus.PARTIAL }
                ?: DataStatus.PARTIAL,
            note = degraded,
        )
    }

    fun observeHistory(): Flow<List<DuplicateHistory>> = database.duplicateDao().observeScans().map { rows ->
        rows.map { row ->
            DuplicateHistory(
                scanId = row.id,
                capturedAtMillis = row.finishedAt ?: row.startedAt,
                groupCount = row.groupCount,
                redundantBytes = row.redundantBytes,
                fileCount = row.fileCount,
                hashedCount = row.hashedCount,
                status = enumNamed<DataStatus>(row.status) ?: DataStatus.PARTIAL,
            )
        }
    }

    /** Local copies are permanently deleted directly; shared media needs the OS dialog. */
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

    /**
     * Closes plans left behind by a process death.
     *
     * A row that was never verified is closed with an honest status instead of
     * being retired as a success; [reconcileRemoved] only ever applies removals the
     * provider itself confirmed.
     */
    suspend fun recoverInterrupted(exceptPlanId: String? = null) {
        clean.recoverInterruptedOperations(exceptPlanId)
    }

    /**
     * Drops only URIs the operation itself verified as changed.
     *
     * If the preserved original vanished outside KNOUX, the oldest surviving copy
     * becomes the new keeper; a group left with one member is no longer a group.
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
            val scan = database.duplicateDao().latestFinishedScan() ?: return@withTransaction
            val groupRows = database.duplicateDao().groups(scan.id)
            val members = database.duplicateDao().members(scan.id)
            val domain = groupRows.mapNotNull { it.toDomain(members.filter { member -> member.groupId == it.id }) }
            if (domain.isEmpty()) return@withTransaction
            val rebuilt = DuplicateRules.afterVerifiedRemoval(domain, removed)
            database.duplicateDao().clearGroups(scan.id)
            database.duplicateDao().clearMembers(scan.id)
            val entities = rebuilt.map { it.toEntity(scan.id) }
            if (entities.isNotEmpty()) {
                database.duplicateDao().insertGroups(entities.map { it.first })
                database.duplicateDao().insertMembers(entities.flatMap { it.second })
            }
            val removedFromGroups = members.count { it.uri in removed }
            database.duplicateDao().updateSummary(
                scanId = scan.id,
                fileCount = (scan.fileCount - removedFromGroups).coerceAtLeast(0),
                groupCount = rebuilt.size,
                redundantBytes = rebuilt.sumOf { it.redundantBytes },
            )
        }
        removed
    }

    // ── Entity mapping (never fabricates a missing enum) ─────────────────────

    private fun DuplicateGroup.toEntity(scanId: String): Pair<DuplicateGroupEntity, List<DuplicateMemberEntity>> {
        val groupRow = DuplicateGroupEntity(
            id = id,
            scanId = scanId,
            contentHash = contentHash,
            bytes = bytes,
            keeperUri = keeper.uri,
            keeperName = keeper.displayName,
            keeperModified = keeper.lastModifiedMillis,
            memberCount = memberCount,
            redundantBytes = redundantBytes,
        )
        val rows = (listOf(keeper to true) + duplicates.map { it to false }).map { (candidate, isKeeper) ->
            DuplicateMemberEntity(
                scanId = scanId,
                groupId = id,
                uri = candidate.uri,
                displayName = candidate.displayName,
                mimeType = candidate.mimeType,
                bytes = candidate.bytes,
                lastModified = candidate.lastModifiedMillis,
                category = candidate.category.name,
                provenance = candidate.provenance.name,
                eligibility = candidate.eligibility.name,
                reason = candidate.reason,
                isKeeper = isKeeper,
            )
        }
        return groupRow to rows
    }

    private fun DuplicateGroupEntity.toDomain(members: List<DuplicateMemberEntity>): DuplicateGroup? {
        val parsed = members.mapNotNull { it.toCandidate() }
        if (parsed.size != members.size) return null
        if (parsed.size < 2) return null
        val keeper = parsed.firstOrNull { it.uri == keeperUri } ?: return null
        val duplicates = parsed.filter { it.uri != keeper.uri }
        if (duplicates.isEmpty()) return null
        return DuplicateGroup(
            id = id,
            contentHash = contentHash,
            bytes = bytes,
            keeper = DuplicateRules.asKeeper(keeper),
            duplicates = duplicates.map { candidate ->
                if (candidate.eligibility == Eligibility.PROTECTED) candidate.copy(eligibility = Eligibility.UNSELECTABLE)
                else candidate
            },
        )
    }

    private fun DuplicateMemberEntity.toCandidate(): FileCandidate? {
        val category = enumNamed<CandidateCategory>(category) ?: return null
        val provenance = enumNamed<DataProvenance>(provenance) ?: return null
        val eligibility = enumNamed<Eligibility>(eligibility) ?: return null
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
        )
    }

    private inline fun <reified T : Enum<T>> enumNamed(name: String): T? =
        enumValues<T>().firstOrNull { it.name == name }
}
