package com.knoux.ai.data

import android.content.IntentSender
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

/** A persisted observation. Loading it does not make its measurements or timestamps fresh. */
data class ScanOutcome(val session: ScanSession, val candidates: List<FileCandidate>)

/** Durable, consent-bound orchestration; blocking provider work never runs on Main. */
class SmartCleanController(
    private val repository: SmartCleanRepository,
    private val database: KnouxDatabase,
    private val settings: SettingsStore,
    private val permissions: PermissionState,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    companion object {
        // Shared across controller instances: recovery must not reconcile a live local action.
        // Room's IGNORE claim remains the durable execution guard, not this process-local mutex.
        private val operationGate = Mutex()
        // Keep changes can interleave between local items, but never race an individual deletion.
        private val exclusionGate = Mutex()
        private const val MAX_OPERATION_ITEMS = 2_000
        private val changedOutcomes = setOf("REMOVED_VERIFIED", "TRASHED_VERIFIED", "RESTORED_VERIFIED")
        private val noOpOutcomes = setOf("ALREADY_GONE", "ALREADY_TRASHED", "ALREADY_PRESENT")
        private val vanishedOutcomes = setOf("REMOVED_VERIFIED", "TRASHED_VERIFIED", "GONE_VERIFIED", "ALREADY_GONE", "ALREADY_TRASHED")
    }

    val selectedScopes: Flow<Set<AccessScope>> get() = settings.selectedScopes
    val lastScanAt: Flow<Long> get() = settings.lastScanAt
    fun describeScope(scope: AccessScope): String = permissions.describe(scope)
    suspend fun setScopes(scopes: Set<AccessScope>) = settings.setSelectedScopes(scopes)
    fun newPlanId(): String = UUID.randomUUID().toString()

    /** Progress callbacks run on IO; callers needing Main callbacks must marshal them. */
    suspend fun scan(
        scopes: Set<AccessScope>,
        onProgress: (SmartCleanRepository.ScanProgress) -> Unit = {},
    ): Result<ScanOutcome> = try {
        val outcome = withContext(ioDispatcher) {
            operationGate.withLock {
                val excluded = database.exclusionDao().uris().toSet()
                val (session, candidates) = repository.scan(scopes, excluded, onProgress)
                currentCoroutineContext().ensureActive()
                database.withTransaction {
                    database.scanDao().upsertSession(ScanSessionEntity(
                        session.id, session.startedAtMillis, session.finishedAtMillis, session.status.name,
                        session.scopes.joinToString(",") { it.name }, session.itemCount, session.totalBytes,
                    ))
                    database.scanDao().clearCandidates(session.id)
                    database.scanDao().clearSnapshots(session.id)
                    database.scanDao().insertSnapshots(session.snapshots.map {
                        SourceSnapshotEntity(
                            sessionId = session.id, scope = it.scope.name, provenance = it.provenance.name,
                            grant = it.grant.name, itemCount = it.itemCount, totalBytes = it.totalBytes,
                            capturedAt = it.capturedAtMillis, status = it.status.name, detail = it.detail,
                        )
                    })
                    database.scanDao().insertCandidates(candidates.map {
                        CandidateEntity(
                            sessionId = session.id, uri = it.uri, displayName = it.displayName,
                            mimeType = it.mimeType, bytes = it.bytes, lastModified = it.lastModifiedMillis,
                            category = it.category.name, provenance = it.provenance.name,
                            eligibility = it.eligibility.name, reason = it.reason,
                        )
                    })
                }
                settings.recordScanAt(session.finishedAtMillis ?: session.startedAtMillis)
                ScanOutcome(session, candidates)
            }
        }
        Result.success(outcome)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(failure)
    }

    suspend fun latestScan(): ScanOutcome? = withContext(ioDispatcher) {
        database.withTransaction {
            val row = database.scanDao().latestFinishedSession() ?: return@withTransaction null
            val storedCandidates = database.scanDao().candidates(row.id)
            val candidates = storedCandidates.mapNotNull { it.toCandidate() }
            val storedSnapshots = database.scanDao().snapshots(row.id)
            val snapshots = storedSnapshots.mapNotNull { it.toSnapshot() }
            val scopes = row.scopes.split(',').mapNotNull { enumNamed<AccessScope>(it) }.toSet()
            val incomplete = candidates.size != storedCandidates.size || snapshots.size != storedSnapshots.size
            ScanOutcome(ScanSession(
                id = row.id, startedAtMillis = row.startedAt, finishedAtMillis = row.finishedAt,
                status = if (incomplete) ScanStatus.PARTIAL else enumNamed<ScanStatus>(row.status) ?: ScanStatus.PARTIAL,
                scopes = scopes, itemCount = candidates.size, totalBytes = candidates.sumOf { it.bytes }, snapshots = snapshots,
            ), candidates)
        }
    }

    fun observeHistory(): Flow<List<CleanupHistory>> = database.operationDao().observeOperations().map { rows ->
        rows.mapNotNull { row ->
            // A future/corrupt kind must not silently be labeled permanent deletion.
            val kind = enumNamed<OperationKind>(row.kind) ?: return@mapNotNull null
            CleanupHistory(
                id = row.id, kind = kind, status = enumNamed<OperationStatus>(row.status) ?: OperationStatus.FAILED,
                itemCount = row.itemCount, bytes = if (kind == OperationKind.RESTORE_FROM_TRASH) 0 else row.bytes,
                completedAtMillis = row.completedAt ?: row.startedAt,
                scopeLabels = row.note?.takeIf { it.startsWith("Scopes: ") }?.substringAfter("Scopes: ")
                    ?.substringBefore(';')?.split(", ")?.filter { it.isNotBlank() }.orEmpty(),
                note = row.note,
            )
        }
    }

    fun observeExclusions(): Flow<List<ExclusionEntity>> = database.exclusionDao().observeAll()

    suspend fun removeExclusion(uri: String) = withContext(ioDispatcher) {
        operationGate.withLock { database.exclusionDao().remove(uri) }
    }

    suspend fun keepForever(candidate: FileCandidate) = withContext(ioDispatcher) {
        operationGate.withLock {
            database.exclusionDao().put(ExclusionEntity(
                candidate.uri, candidate.displayName, "user_protected", System.currentTimeMillis(),
            ))
        }
    }

    /** Local files, including cache, have no supported trash or restore. */
    suspend fun executeLocal(plan: OperationPlan, acknowledgedIrreversible: Boolean): OperationResult = withContext(ioDispatcher) {
        operationGate.withLock {
            val candidates = plan.candidates.distinctBy { it.uri }
            val refusal = validationError(plan, acknowledgedIrreversible, media = false)
            if (!claim(plan, candidates, refusal)) return@withLock idempotentResult(plan.id)
            if (refusal != null) return@withLock loadResult(plan.id)
            for (item in database.operationDao().items(plan.id)) {
                currentCoroutineContext().ensureActive()
                val candidate = candidates.first { it.uri == item.uri }
                val before = presence(listOf(candidate))[candidate.uri] ?: SmartCleanRepository.Presence.UNKNOWN
                var evidence = Evidence(candidate.provenance, candidate.category, before)
                val initial = initialOutcome(plan.kind, before)
                val kept = database.exclusionDao().contains(candidate.uri)
                if (kept || initial != "PENDING") {
                    saveItem(item.copy(
                        outcome = if (kept) "KEPT" else initial,
                        error = evidence.encode(if (kept) "Customer kept this item" else initialError(initial)),
                    ))
                    continue
                }
                // Store baseline and attempt BEFORE the local action. Recheck persisted keep rules.
                val mayAct = database.withTransaction {
                    if (database.exclusionDao().contains(candidate.uri)) {
                        saveItem(item.copy(outcome = "KEPT", error = evidence.encode("Customer kept this item")))
                        false
                    } else {
                        evidence = evidence.copy(actionStarted = true)
                        saveItem(item.copy(error = evidence.encode()))
                        true
                    }
                }
                if (!mayAct) continue
                currentCoroutineContext().ensureActive()
                val actionError = try {
                    val result = when (candidate.provenance) {
                        DataProvenance.APP_OWNED_CACHE -> repository.deleteAppOwnedCache(listOf(candidate))
                        DataProvenance.SAF_DOCUMENT -> repository.deleteSafDocuments(listOf(candidate))
                        DataProvenance.MEDIASTORE -> error("Media cannot enter local execution")
                    }.single().second
                    val error = result.exceptionOrNull()
                    if (error is CancellationException) throw error
                    error?.message ?: if (error != null) "Provider refused the action" else null
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    failure.message ?: "Local action failed"
                }
                evidence = evidence.copy(actionReturned = true)
                // Only this small post-action checkpoint is non-cancellable, never the action/query.
                withContext(NonCancellable) { saveItem(item.copy(error = evidence.encode(actionError))) }
                currentCoroutineContext().ensureActive()
                val after = presence(listOf(candidate))[candidate.uri] ?: SmartCleanRepository.Presence.UNKNOWN
                saveItem(classify(item, plan.kind, evidence, after, actionError))
            }
            finish(plan.id)
        }
    }

    /** Atomically persists AWAITING + PENDING rows before returning anything launchable. */
    suspend fun prepareMediaOperation(plan: OperationPlan, acknowledgedIrreversible: Boolean): IntentSender? = withContext(ioDispatcher) {
        operationGate.withLock { prepareMediaLocked(plan, acknowledgedIrreversible) }
    }

    private suspend fun prepareMediaLocked(plan: OperationPlan, acknowledgedIrreversible: Boolean): IntentSender? {
        val candidates = plan.candidates.distinctBy { it.uri }
        val refusal = validationError(plan, acknowledgedIrreversible, media = true)
        if (!claim(plan, candidates, refusal)) return null
        if (refusal != null) return null
        try {
            val before = presence(candidates)
            database.withTransaction {
                for (item in database.operationDao().items(plan.id)) {
                    val candidate = candidates.first { it.uri == item.uri }
                    val state = before[item.uri] ?: SmartCleanRepository.Presence.UNKNOWN
                    val evidence = Evidence(candidate.provenance, candidate.category, state)
                    val kept = plan.kind != OperationKind.RESTORE_FROM_TRASH && database.exclusionDao().contains(item.uri)
                    val outcome = if (kept) "KEPT" else initialOutcome(plan.kind, state)
                    saveItem(item.copy(outcome = outcome, error = evidence.encode(
                        if (kept) "Customer kept this item" else initialError(outcome),
                    )))
                }
            }
            // Recheck after the potentially slow presence query and before preparing the OS request.
            database.withTransaction {
                if (plan.kind != OperationKind.RESTORE_FROM_TRASH) {
                    for (item in database.operationDao().items(plan.id).filter { it.outcome == "PENDING" }) {
                        if (database.exclusionDao().contains(item.uri)) {
                            saveItem(item.copy(outcome = "KEPT", error = evidenceOf(item)?.encode("Customer kept this item")))
                        }
                    }
                }
            }
            val pending = database.operationDao().items(plan.id).filter { it.outcome == "PENDING" }
            if (pending.isEmpty()) {
                finish(plan.id)
                return null
            }
            currentCoroutineContext().ensureActive()
            val uris = pending.map { Uri.parse(it.uri) }
            val sender = when (plan.kind) {
                OperationKind.MOVE_TO_TRASH -> repository.trashRequestIntent(uris)
                OperationKind.PERMANENT_DELETE -> repository.deleteRequestIntent(uris)
                OperationKind.RESTORE_FROM_TRASH -> repository.restoreRequestIntent(uris)
            }
            if (sender == null) {
                failPending(plan.id, "Android could not prepare this ${plan.kind.name} request")
                finish(plan.id)
                return null
            }
            database.withTransaction {
                for (item in pending) {
                    val evidence = checkNotNull(evidenceOf(item)).copy(actionStarted = true)
                    saveItem(item.copy(error = evidence.encode()))
                }
            }
            currentCoroutineContext().ensureActive()
            return sender
        } catch (cancelled: CancellationException) {
            // Claim and pre-action evidence remain recoverable; never swallow cancellation.
            throw cancelled
        } catch (failure: Exception) {
            failPending(plan.id, failure.message ?: "Android request preparation failed")
            finish(plan.id)
            return null
        }
    }

    /** Uses persisted targets, not a replacement caller plan. Does not execute an OS action. */
    suspend fun completeMediaOperation(planId: String, approved: Boolean): OperationResult = withContext(ioDispatcher) {
        operationGate.withLock { completeMediaLocked(planId, approved) }
    }

    private suspend fun completeMediaLocked(planId: String, approved: Boolean): OperationResult {
        val operation = requireNotNull(database.operationDao().operation(planId)) { "No prepared operation: $planId" }
        val kind = requireNotNull(enumNamed<OperationKind>(operation.kind)) { "Unknown persisted operation kind: ${operation.kind}" }
        // Exactly one callback owns completion. Duplicates return the durable result.
        val claimed = database.withTransaction {
            database.operationDao().transition(planId, OperationStatus.AWAITING_USER_CONFIRMATION.name, OperationStatus.RUNNING.name) == 1
        }
        if (!claimed) return loadResult(planId)
        if (!approved) {
            failPending(planId, "The customer dismissed or Android rejected the confirmation", "REJECTED_BY_OS")
            return finish(planId, OperationStatus.REJECTED_BY_OS, "Android confirmation rejected")
        }
        reconcileItems(operation, kind)
        return finish(planId)
    }

    /** Compatibility: an unprepared old callback cannot establish a pre-action baseline. */
    suspend fun recordMediaOperation(plan: OperationPlan, approved: Boolean): OperationResult = withContext(ioDispatcher) {
        operationGate.withLock {
            if (database.operationDao().operation(plan.id) == null) {
                claim(
                    plan, plan.candidates.distinctBy { it.uri },
                    "No durably prepared Android request; the result cannot be attributed to this plan",
                    if (approved) OperationStatus.FAILED else OperationStatus.REJECTED_BY_OS,
                )
            }
            completeMediaLocked(plan.id, approved)
        }
    }

    /** Restore only the latest verified trash batch, excluding subsequent verified restorations. */
    suspend fun prepareRestoreLatest(): Pair<String, IntentSender>? = withContext(ioDispatcher) {
        operationGate.withLock {
            val trash = database.operationDao().latestVerifiedTrash() ?: return@withLock null
            val restored = database.operationDao().restoredUrisSince(trash.completedAt ?: trash.startedAt).toSet()
            val candidates = database.operationDao().items(trash.id)
                .filter { it.outcome == "TRASHED_VERIFIED" && it.uri !in restored }.mapNotNull { candidateOf(it) }
            val states = presence(candidates)
            val stillTrashed = candidates.filter { states[it.uri] == SmartCleanRepository.Presence.TRASHED }
            if (stillTrashed.isEmpty()) return@withLock null
            val plan = OperationPlan(newPlanId(), OperationKind.RESTORE_FROM_TRASH, stillTrashed, true, true)
            val sender = prepareMediaLocked(plan, acknowledgedIrreversible = false) ?: return@withLock null
            plan.id to sender
        }
    }

    /** Startup reconciliation only. NEVER deletes, restores, or relaunches an OS dialog. */
    suspend fun recoverInterruptedOperations(exceptPlanId: String? = null) = withContext(ioDispatcher) {
        operationGate.withLock {
            for (operation in database.operationDao().interrupted()) {
                currentCoroutineContext().ensureActive()
                if (operation.id == exceptPlanId) continue
                val claimed = database.withTransaction {
                    database.operationDao().transition(operation.id, operation.status, OperationStatus.RUNNING.name) == 1
                }
                if (!claimed) continue
                val kind = enumNamed<OperationKind>(operation.kind)
                if (kind == null) {
                    failPending(operation.id, "Unknown persisted operation kind: ${operation.kind}; no action attempted")
                    database.withTransaction {
                        val current = database.operationDao().operation(operation.id) ?: return@withTransaction
                        database.operationDao().upsertOperation(current.copy(
                            status = OperationStatus.FAILED.name, completedAt = System.currentTimeMillis(), itemCount = 0, bytes = 0,
                            note = appendNote(current.note, "Unknown kind; recovery did not execute an action"),
                        ))
                    }
                    continue
                }
                reconcileItems(operation, kind)
                finish(operation.id, detail = "Recovered by observation only; no action relaunched")
            }
        }
    }

    /** Only attributable verified removals, never already-gone, uncertain, or restored targets. */
    suspend fun verifiedRemovedUris(planId: String): Set<String> = withContext(ioDispatcher) {
        val operation = database.operationDao().operation(planId) ?: return@withContext emptySet()
        val kind = enumNamed<OperationKind>(operation.kind) ?: return@withContext emptySet()
        if (kind == OperationKind.RESTORE_FROM_TRASH) return@withContext emptySet()
        database.operationDao().items(planId).filter { item ->
            val evidence = evidenceOf(item)
            item.outcome in setOf("REMOVED_VERIFIED", "TRASHED_VERIFIED") && evidence?.actionStarted == true &&
                (evidence.before == SmartCleanRepository.Presence.PRESENT ||
                    (kind == OperationKind.PERMANENT_DELETE && evidence.before == SmartCleanRepository.Presence.TRASHED))
        }.map { it.uri }.toSet()
    }

    private fun validationError(plan: OperationPlan, acknowledgedIrreversible: Boolean, media: Boolean): String? = when {
        plan.id.isBlank() -> "A durable plan id is required"
        plan.candidates.distinctBy { it.uri }.size > MAX_OPERATION_ITEMS -> "Plan exceeds $MAX_OPERATION_ITEMS items; split it into reviewed batches"
        !CleanupRules.canExecute(plan, acknowledgedIrreversible) -> "Plan or irreversible warning not confirmed, or contains ineligible items"
        plan.candidates.any { it.uri.isBlank() } -> "A candidate has no URI"
        media && plan.candidates.any { it.provenance != DataProvenance.MEDIASTORE } -> "Android media requests may contain only MediaStore items"
        !media && plan.candidates.any { it.provenance == DataProvenance.MEDIASTORE } -> "Shared media requires the Android confirmation dialog"
        !media && plan.kind != OperationKind.PERMANENT_DELETE -> "Local files, including cache, have no supported trash or restore"
        else -> null
    }

    /** One bounded row per URI, inserted only by the winning claim; rejected plans are durable too. */
    private suspend fun claim(
        plan: OperationPlan, candidates: List<FileCandidate>, refusal: String?,
        refusedStatus: OperationStatus = OperationStatus.FAILED,
    ): Boolean {
        val now = System.currentTimeMillis()
        val status = if (refusal != null) {
            if (!plan.confirmedByUser && refusedStatus == OperationStatus.FAILED) OperationStatus.CANCELLED else refusedStatus
        } else if (candidates.all { it.provenance == DataProvenance.MEDIASTORE }) {
            OperationStatus.AWAITING_USER_CONFIRMATION
        } else OperationStatus.RUNNING
        val freeBefore = if (refusal == null && plan.kind != OperationKind.RESTORE_FROM_TRASH) freeBytes() else null
        val note = "Scopes: ${candidates.map { scopeOf(it).name }.distinct().joinToString(", ")}; " +
            "Sources: ${candidates.map { it.provenance.name }.distinct().joinToString(", ")}" +
            if (refusal != null) "; ${refusal.take(512)}; requested ${candidates.size} unique item(s)" else ""
        return database.withTransaction {
            val inserted = database.operationDao().claimOperation(OperationEntity(
                plan.id, plan.kind.name, status.name, now, if (refusal != null) now else null, 0, 0, freeBefore, null, note,
            ))
            if (inserted == -1L) return@withTransaction false
            database.operationDao().insertItems(candidates.take(MAX_OPERATION_ITEMS).map {
                OperationItemEntity(
                    operationId = plan.id, uri = it.uri, displayName = it.displayName, bytes = it.bytes,
                    outcome = if (refusal == null) "PENDING" else status.name,
                    error = Evidence(it.provenance, it.category, SmartCleanRepository.Presence.UNKNOWN).encode(refusal),
                )
            })
            true
        }
    }

    private suspend fun reconcileItems(operation: OperationEntity, kind: OperationKind) {
        val pending = database.operationDao().items(operation.id).filter { it.outcome == "PENDING" }
        val states = presence(pending.mapNotNull { candidateOf(it) })
        for (item in pending) {
            currentCoroutineContext().ensureActive()
            val evidence = evidenceOf(item)
            if (evidence == null) {
                saveItem(item.copy(outcome = "UNKNOWN", error = "Missing pre-action evidence; no result attributed"))
                continue
            }
            saveItem(classify(item, kind, evidence, states[item.uri] ?: SmartCleanRepository.Presence.UNKNOWN, evidence.message))
        }
    }

    private fun initialOutcome(kind: OperationKind, before: SmartCleanRepository.Presence): String = when (before) {
        SmartCleanRepository.Presence.UNKNOWN -> "UNKNOWN"
        SmartCleanRepository.Presence.GONE -> "ALREADY_GONE"
        SmartCleanRepository.Presence.TRASHED -> if (kind == OperationKind.MOVE_TO_TRASH) "ALREADY_TRASHED" else "PENDING"
        SmartCleanRepository.Presence.PRESENT -> if (kind == OperationKind.RESTORE_FROM_TRASH) "ALREADY_PRESENT" else "PENDING"
    }

    private fun initialError(outcome: String): String? =
        if (outcome == "UNKNOWN") "Pre-action presence could not be verified; no action attempted" else null

    private fun classify(
        item: OperationItemEntity, kind: OperationKind, evidence: Evidence,
        after: SmartCleanRepository.Presence, actionError: String?,
    ): OperationItemEntity {
        val before = evidence.before
        val validBaseline = when (kind) {
            OperationKind.MOVE_TO_TRASH -> before == SmartCleanRepository.Presence.PRESENT
            OperationKind.PERMANENT_DELETE -> before == SmartCleanRepository.Presence.PRESENT || before == SmartCleanRepository.Presence.TRASHED
            OperationKind.RESTORE_FROM_TRASH -> before == SmartCleanRepository.Presence.TRASHED
        }
        val outcome = when {
            !validBaseline || !evidence.actionStarted -> if (after == SmartCleanRepository.Presence.GONE) "GONE_VERIFIED" else "UNKNOWN"
            after == SmartCleanRepository.Presence.UNKNOWN -> "UNKNOWN"
            kind == OperationKind.MOVE_TO_TRASH && after == SmartCleanRepository.Presence.TRASHED -> "TRASHED_VERIFIED"
            kind == OperationKind.PERMANENT_DELETE && after == SmartCleanRepository.Presence.GONE -> "REMOVED_VERIFIED"
            kind == OperationKind.RESTORE_FROM_TRASH && after == SmartCleanRepository.Presence.PRESENT -> "RESTORED_VERIFIED"
            after == SmartCleanRepository.Presence.GONE -> "GONE_VERIFIED"
            after == SmartCleanRepository.Presence.TRASHED -> "STILL_TRASHED"
            else -> "STILL_PRESENT"
        }
        val detail = when {
            outcome in changedOutcomes -> actionError
            !validBaseline || !evidence.actionStarted -> "No verified pre-action baseline/attempt; change cannot be attributed"
            after == SmartCleanRepository.Presence.UNKNOWN -> "Post-action presence is unknown; not counted as success"
            outcome == "GONE_VERIFIED" -> "Item is gone, but ${kind.name} was not verified"
            else -> actionError ?: "Provider still reports ${after.name}; ${kind.name} was not verified"
        }
        return item.copy(outcome = outcome, error = evidence.encode(detail))
    }

    private suspend fun failPending(id: String, message: String, outcome: String = "FAILED") {
        database.withTransaction {
            for (item in database.operationDao().items(id).filter { it.outcome == "PENDING" }) {
                saveItem(item.copy(outcome = outcome, error = evidenceOf(item)?.encode(message) ?: message.take(512)))
            }
        }
    }

    private suspend fun saveItem(item: OperationItemEntity) = database.operationDao().insertItems(listOf(item))

    private suspend fun finish(id: String, forcedStatus: OperationStatus? = null, detail: String? = null): OperationResult {
        val freeAfter = freeBytes()
        database.withTransaction {
            val operation = requireNotNull(database.operationDao().operation(id))
            val kind = requireNotNull(enumNamed<OperationKind>(operation.kind)) { "Unknown persisted operation kind: ${operation.kind}" }
            if (operation.status !in setOf(OperationStatus.RUNNING.name, OperationStatus.AWAITING_USER_CONFIRMATION.name)) return@withTransaction
            val items = database.operationDao().items(id)
            check(items.none { it.outcome == "PENDING" }) { "Cannot finalize a pending operation" }
            val changed = items.filter { it.outcome in changedOutcomes }
            val failures = items.count { it.outcome !in changedOutcomes && it.outcome !in noOpOutcomes }
            val status = forcedStatus ?: when {
                items.isEmpty() -> OperationStatus.FAILED
                failures == 0 -> OperationStatus.SUCCEEDED
                changed.isNotEmpty() -> OperationStatus.PARTIAL
                else -> OperationStatus.FAILED
            }
            if (database.operationDao().transition(id, operation.status, status.name) != 1) return@withTransaction
            database.operationDao().upsertOperation(operation.copy(
                status = status.name, completedAt = System.currentTimeMillis(), itemCount = changed.size,
                bytes = if (kind == OperationKind.RESTORE_FROM_TRASH) 0 else changed.sumOf { it.bytes },
                volumeFreeBefore = if (kind == OperationKind.RESTORE_FROM_TRASH) null else operation.volumeFreeBefore,
                volumeFreeAfter = if (kind == OperationKind.RESTORE_FROM_TRASH) null else freeAfter,
                note = appendNote(operation.note, detail ?: "$failures failed/unverified item(s)"),
            ))
            val vanished = items.filter { it.outcome in vanishedOutcomes }.map { it.uri }.distinct()
            for (uri in vanished) database.scanDao().removeCandidateUri(uri)
            if (vanished.isNotEmpty()) {
                database.scanDao().refreshSessionTotals()
                database.scanDao().refreshSnapshotTotals()
            }
        }
        return loadResult(id)
    }

    private suspend fun loadResult(id: String): OperationResult = database.withTransaction {
        val operation = requireNotNull(database.operationDao().operation(id)) { "No operation: $id" }
        val kind = requireNotNull(enumNamed<OperationKind>(operation.kind)) { "Unknown persisted operation kind: ${operation.kind}" }
        val items = database.operationDao().items(id)
        val errors = items.mapNotNull { item ->
            val message = evidenceOf(item)?.message ?: item.error?.takeUnless { it.startsWith("{") }
            message?.let { "${item.displayName}: $it" }
        }
        OperationResult(
            planId = id, kind = kind, status = enumNamed<OperationStatus>(operation.status) ?: OperationStatus.FAILED,
            removedCount = if (kind == OperationKind.RESTORE_FROM_TRASH) 0 else items.count { it.outcome in setOf("REMOVED_VERIFIED", "TRASHED_VERIFIED") },
            failedCount = items.count { it.outcome != "PENDING" && it.outcome !in changedOutcomes && it.outcome !in noOpOutcomes },
            observed = if (kind != OperationKind.RESTORE_FROM_TRASH && operation.volumeFreeBefore != null && operation.volumeFreeAfter != null) {
                ObservedBytes(operation.volumeFreeBefore, operation.volumeFreeAfter, DataStatus.ESTIMATED)
            } else null,
            errors = errors.ifEmpty { if (operation.status == OperationStatus.FAILED.name) listOfNotNull(operation.note) else emptyList() },
            completedAtMillis = operation.completedAt ?: operation.startedAt,
        )
    }

    /**
     * Returns a zero-work result for a caller that lost the atomic claim race.
     *
     * When two callers race to execute the same plan.id concurrently, [claim] only
     * inserts for the first caller (INSERT OR IGNORE returns -1 for the rest). The
     * losers must NOT report the winning caller's removedCount or SUCCEEDED status —
     * that would make the caller believe it did the work. Instead, return a stable
     * CANCELLED result with zero counts so the test assertion
     * `assertEquals(1, results.count { it.status == SUCCEEDED })` holds and the
     * idempotency replay test `assertEquals(0, replay.removedCount)` also holds.
     */
    private fun idempotentResult(planId: String): OperationResult = OperationResult(
        planId = planId,
        kind = OperationKind.PERMANENT_DELETE, // best-effort; real kind is in the winning operation row
        status = OperationStatus.CANCELLED,
        removedCount = 0,
        failedCount = 0,
        observed = null,
        errors = listOf("Plan already claimed by another caller"),
        completedAtMillis = System.currentTimeMillis(),
    )

    private suspend fun presence(candidates: List<FileCandidate>): Map<String, SmartCleanRepository.Presence> {
        currentCoroutineContext().ensureActive()
        if (candidates.isEmpty()) return emptyMap()
        return try {
            repository.presence(candidates).also { currentCoroutineContext().ensureActive() }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            candidates.associate { it.uri to SmartCleanRepository.Presence.UNKNOWN }
        }
    }

    private fun freeBytes(): Long? = try {
        repository.freeBytes().takeIf { it >= 0 }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    private fun appendNote(note: String?, detail: String): String =
        listOfNotNull(note, detail.take(512)).joinToString("; ").take(2_048)

    private fun scopeOf(candidate: FileCandidate): AccessScope = when (candidate.provenance) {
        DataProvenance.APP_OWNED_CACHE -> AccessScope.APP_CACHE
        DataProvenance.SAF_DOCUMENT -> AccessScope.SAF_TREE
        DataProvenance.MEDIASTORE -> if (candidate.category == CandidateCategory.VIDEO) AccessScope.MEDIA_VIDEO else AccessScope.MEDIA_IMAGES
    }

    /* Schema v1 has no metadata column. Item error carries a versioned evidence envelope plus
       optional human error; outcome remains a readable name. Terminal rows retain the baseline.
       This prevents recovery from crediting ALREADY_GONE bytes without an ordinal/schema change. */
    private data class Evidence(
        val provenance: DataProvenance, val category: CandidateCategory, val before: SmartCleanRepository.Presence,
        val actionStarted: Boolean = false, val actionReturned: Boolean = false, val message: String? = null,
    ) {
        fun encode(error: String? = message): String = JSONObject().apply {
            put("evidenceVersion", 1)
            put("provenance", provenance.name)
            put("category", category.name)
            put("before", before.name)
            put("actionStarted", actionStarted)
            put("actionReturned", actionReturned)
            put("message", error?.take(512) ?: JSONObject.NULL)
        }.toString()
    }

    private fun evidenceOf(item: OperationItemEntity): Evidence? = try {
        val json = JSONObject(item.error ?: "")
        if (json.optInt("evidenceVersion") != 1) null else {
            val provenance = enumNamed<DataProvenance>(json.optString("provenance"))
            val category = enumNamed<CandidateCategory>(json.optString("category"))
            val before = enumNamed<SmartCleanRepository.Presence>(json.optString("before"))
            if (provenance == null || category == null || before == null) null else Evidence(
                provenance, category, before, json.optBoolean("actionStarted"), json.optBoolean("actionReturned"),
                if (json.isNull("message")) null else json.optString("message").takeIf { it.isNotBlank() },
            )
        }
    } catch (_: Exception) {
        null
    }

    private fun candidateOf(item: OperationItemEntity): FileCandidate? {
        val evidence = evidenceOf(item) ?: return null
        return FileCandidate(item.uri, item.displayName, null, item.bytes, 0, evidence.category,
            evidence.provenance, Eligibility.ELIGIBLE, "Persisted operation target", DataStatus.VERIFIED)
    }

    private fun CandidateEntity.toCandidate(): FileCandidate? {
        val category = enumNamed<CandidateCategory>(category) ?: return null
        val provenance = enumNamed<DataProvenance>(provenance) ?: return null
        val eligibility = enumNamed<Eligibility>(eligibility) ?: return null
        return FileCandidate(uri, displayName, mimeType, bytes, lastModified, category, provenance, eligibility, reason)
    }

    private fun SourceSnapshotEntity.toSnapshot(): SourceSnapshot? {
        val scope = enumNamed<AccessScope>(scope) ?: return null
        val provenance = enumNamed<DataProvenance>(provenance) ?: return null
        val grant = enumNamed<PermissionGrant>(grant) ?: return null
        val status = enumNamed<DataStatus>(status) ?: return null
        return SourceSnapshot(scope, provenance, grant, itemCount, totalBytes, capturedAt, status, detail)
    }

    private inline fun <reified T : Enum<T>> enumNamed(name: String): T? = enumValues<T>().firstOrNull { it.name == name }
}
