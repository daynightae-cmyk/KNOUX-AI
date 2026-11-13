package com.knoux.ai.data

import android.content.Intent
import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

/** Pseudo check id for a pass-level audit row (MEASURED / REVIEWED), never a real check. */
private const val AGGREGATE_CHECK = "ALL"

/** A persisted privacy pass. Loading it does not make it a fresh observation. */
data class PrivacyReport(
    val scanId: String,
    val capturedAtMillis: Long,
    val rows: List<PrivacyObservation>,
    val summary: PrivacySummary,
    val status: DataStatus,
    val note: String?,
) {
    val scored: List<PrivacyObservation> get() = PrivacyRules.scored(rows)
    val attention: List<PrivacyObservation> get() = scored.filter { it.status == PrivacyStatus.ATTENTION }
}

/** What was done about a row. Opening an OS screen is not a fix. */
enum class PrivacyAction {
    MEASURED,
    REVIEWED,
    OS_SCREEN_OPENED,
    SETTING_APPLIED,
    SETTING_VERIFIED,
}

data class PrivacyAuditEntry(
    val id: String,
    val scanId: String?,
    /** `null` for a pass-level action such as MEASURED or REVIEWED, which is not one check. */
    val check: PrivacyCheck?,
    val action: PrivacyAction,
    val outcome: String,
    val createdAtMillis: Long,
    val note: String?,
)

/**
 * PC04 orchestration.
 *
 * The sweep itself is read-only. The only state KNOUX changes is its own
 * screenshot-protection flag, and even that is recorded as
 * [PrivacyAction.SETTING_APPLIED] until the real window flag has been re-read and
 * matched. Android screens are recorded as [PrivacyAction.OS_SCREEN_OPENED] with
 * no claim that the customer changed anything there.
 */
class PrivacyController(
    private val repository: PrivacyRepository,
    private val database: KnouxDatabase,
    private val settings: SettingsStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    val secureWindow: Flow<Boolean> get() = settings.privacySecureWindow

    fun newScanId(): String = UUID.randomUUID().toString()

    suspend fun measure(windowSecure: Boolean): Result<PrivacyReport> = try {
        withContext(ioDispatcher) {
            val observation = repository.observe(windowSecure)
            val summary = PrivacyRules.summarize(observation.rows)
            val scanId = newScanId()
            val startedAt = System.currentTimeMillis()
            val status = if (observation.note == null) DataStatus.MEASURED else DataStatus.PARTIAL
            database.withTransaction {
                database.privacyDao().upsertScan(
                    PrivacyScanEntity(
                        id = scanId,
                        startedAt = startedAt,
                        finishedAt = System.currentTimeMillis(),
                        status = status.name,
                        score = summary.score,
                        scoredCount = summary.scoredCount,
                        measuredCount = summary.measuredCount,
                        attentionCount = summary.attentionCount,
                        unmeasurableCount = summary.unmeasurableCount,
                        note = observation.note,
                    )
                )
                database.privacyDao().clearFindings(scanId)
                database.privacyDao().insertFindings(
                    observation.rows.map { row ->
                        PrivacyFindingEntity(
                            scanId = scanId,
                            checkId = row.check.name,
                            status = row.status.name,
                            severity = row.severity.name,
                            observed = row.observed,
                            detail = row.detail,
                            remedy = row.remedy.name,
                            recordedAt = startedAt,
                        )
                    }
                )
                database.privacyDao().putAction(
                    PrivacyActionEntity(
                        id = UUID.randomUUID().toString(),
                        scanId = scanId,
                        checkId = AGGREGATE_CHECK,
                        action = PrivacyAction.MEASURED.name,
                        outcome = "RECORDED",
                        createdAt = startedAt,
                        note = "${observation.rows.size} checks on device; score ${summary.score ?: "unavailable"}",
                    )
                )
            }
            Result.success(
                PrivacyReport(
                    scanId = scanId,
                    capturedAtMillis = startedAt,
                    rows = observation.rows,
                    summary = summary,
                    status = status,
                    note = observation.note,
                )
            )
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(failure)
    }

    suspend fun latest(): PrivacyReport? = withContext(ioDispatcher) {
        val scan = database.privacyDao().latestFinishedScan() ?: return@withContext null
        val stored = database.privacyDao().findings(scan.id)
        val rows = stored.mapNotNull { it.toObservation() }
        val degraded = listOfNotNull(
            scan.note,
            "A stored privacy row was unreadable and is not shown".takeIf { rows.size != stored.size },
        ).joinToString(" ").takeIf { it.isNotBlank() }
        PrivacyReport(
            scanId = scan.id,
            capturedAtMillis = scan.finishedAt ?: scan.startedAt,
            rows = rows,
            summary = PrivacyRules.summarize(rows),
            status = if (degraded == null && enumNamed<DataStatus>(scan.status) == DataStatus.MEASURED) {
                DataStatus.MEASURED
            } else {
                DataStatus.PARTIAL
            },
            note = degraded,
        )
    }

    fun observeAudit(): Flow<List<PrivacyAuditEntry>> =
        database.privacyDao().observeActions().map { rows ->
            rows.mapNotNull { row ->
                val check = when (row.checkId) {
                    AGGREGATE_CHECK -> null
                    else -> enumNamed<PrivacyCheck>(row.checkId) ?: return@mapNotNull null
                }
                val action = enumNamed<PrivacyAction>(row.action) ?: return@mapNotNull null
                PrivacyAuditEntry(
                    id = row.id,
                    scanId = row.scanId,
                    check = check,
                    action = action,
                    outcome = row.outcome,
                    createdAtMillis = row.createdAt,
                    note = row.note,
                )
            }
        }

    /** The live OS screen for a check, or `null` when the platform has none. */
    fun osScreenIntent(check: PrivacyCheck): Intent? = repository.osScreenIntent(check)

    /**
     * Records that an Android screen was launched. It deliberately records no
     * change: KNOUX cannot read what the customer did there until the next sweep,
     * and the next sweep is a new measurement, not a confirmation.
     */
    suspend fun recordOsScreenOpened(check: PrivacyCheck, resolved: Boolean, scanId: String?) =
        withContext(ioDispatcher) {
            database.privacyDao().putAction(
                PrivacyActionEntity(
                    id = UUID.randomUUID().toString(),
                    scanId = scanId,
                    checkId = check.name,
                    action = PrivacyAction.OS_SCREEN_OPENED.name,
                    outcome = if (resolved) "OPENED" else "UNAVAILABLE",
                    createdAt = System.currentTimeMillis(),
                    note = if (resolved) {
                        "Android-owned screen launched; no change claimed by KNOUX"
                    } else {
                        "No Android screen resolved on this device"
                    },
                )
            )
        }

    suspend fun recordReviewed(scanId: String, attention: Int) = withContext(ioDispatcher) {
        database.privacyDao().putAction(
            PrivacyActionEntity(
                id = UUID.randomUUID().toString(),
                scanId = scanId,
                checkId = AGGREGATE_CHECK,
                action = PrivacyAction.REVIEWED.name,
                outcome = "RECORDED",
                createdAt = System.currentTimeMillis(),
                note = "$attention attention item(s) reviewed",
            )
        )
    }

    /** Persists the one setting KNOUX owns. The window flag is applied by the UI. */
    suspend fun setSecureWindowPreference(enabled: Boolean) = withContext(ioDispatcher) {
        settings.setPrivacySecureWindow(enabled)
        database.privacyDao().putAction(
            PrivacyActionEntity(
                id = UUID.randomUUID().toString(),
                scanId = latestScanId(),
                checkId = PrivacyCheck.SCREENSHOT_PROTECTION.name,
                action = PrivacyAction.SETTING_APPLIED.name,
                outcome = "REQUESTED",
                createdAt = System.currentTimeMillis(),
                note = "screenshot protection ${if (enabled) "on" else "off"} requested",
            )
        )
    }

    /**
     * Compares the requested setting with the flag the OS actually holds now.
     * Only a match is recorded as VERIFIED; a mismatch stays applied-but-unverified.
     */
    suspend fun verifySecureWindow(
        requested: Boolean,
        actualSecure: Boolean,
        scanId: String?,
    ): Boolean = withContext(ioDispatcher) {
        val match = requested == actualSecure
        database.privacyDao().putAction(
            PrivacyActionEntity(
                id = UUID.randomUUID().toString(),
                scanId = scanId ?: latestScanId(),
                checkId = PrivacyCheck.SCREENSHOT_PROTECTION.name,
                action = if (match) PrivacyAction.SETTING_VERIFIED.name else PrivacyAction.SETTING_APPLIED.name,
                outcome = if (match) "VERIFIED" else "MISMATCH",
                createdAt = System.currentTimeMillis(),
                note = "window FLAG_SECURE=${if (actualSecure) "set" else "clear"}; requested=${if (requested) "set" else "clear"}",
            )
        )
        match
    }

    private suspend fun latestScanId(): String? = database.privacyDao().latestFinishedScan()?.id

    private fun PrivacyFindingEntity.toObservation(): PrivacyObservation? {
        val check = enumNamed<PrivacyCheck>(checkId) ?: return null
        val status = enumNamed<PrivacyStatus>(status) ?: return null
        val severity = enumNamed<PrivacySeverity>(severity) ?: return null
        val remedy = enumNamed<PrivacyRemedy>(remedy) ?: return null
        return PrivacyObservation(check, status, severity, observed, detail, remedy)
    }

    private inline fun <reified T : Enum<T>> enumNamed(name: String): T? =
        enumValues<T>().firstOrNull { it.name == name }
}
