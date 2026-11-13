package com.knoux.ai.data

/**
 * PC04 — Privacy Sweep rules.
 *
 * Android does not expose another app's permission grants, app-ops history or
 * trackers to an unprivileged app. A privacy screen that invented those numbers
 * would be worse than no screen at all, so this contract separates three things
 * and never lets one become another:
 *
 *  - **Measured** rows come from a real PackageManager / NetworkSecurityPolicy /
 *    NotificationManager / window observation made in this run.
 *  - **Unmeasurable** rows say so explicitly and carry [PrivacyRemedy.NOT_POSSIBLE];
 *    they are never scored and can never be rendered as a pass.
 *  - **Attention** rows describe something KNOUX can actually change (an app
 *    setting) or an OS-owned screen the customer can open. KNOUX never claims it
 *    changed another app, and never records a fix without re-observing the OS.
 *
 * The score is deliberately simple and auditable: 100 minus a fixed penalty per
 * attention row, computed only from measured, scored rows. It is a KNOUX posture
 * indicator, not a security rating, and [PrivacySummary.scoredCount] is exposed so
 * the UI cannot show "100" as if every check had been verified.
 */

/** Everything the sweep examines. Each check appears at most once per pass. */
enum class PrivacyCheck {
    /** Does the manifest even hold the INTERNET permission? */
    NETWORK_EGRESS,

    /** MANAGE_EXTERNAL_STORAGE, the "all files access" special permission. */
    ALL_FILES_ACCESS,

    /** Location, camera, microphone, contacts, calendar, SMS, call log, nearby. */
    SENSITIVE_PERMISSIONS,

    /** The media grant KNOUX itself needs for its cleaning services. */
    MEDIA_ACCESS,

    /** Persisted SAF trees with write access (needed for folder cleanup). */
    SAF_WRITE_GRANTS,

    /** `android:allowBackup` — can the OS copy this app's data off-device? */
    BACKUP_ALLOWED,

    /** Does the platform permit cleartext HTTP for this app? */
    CLEARTEXT_TRAFFIC,

    /** Notification permission/enabled state. */
    NOTIFICATIONS,

    /** KNOUX's own FLAG_SECURE setting (blocks screenshots and recents preview). */
    SCREENSHOT_PROTECTION,

    /** Debug build flag — informational only, never a customer fix. */
    DEBUGGABLE_BUILD,

    /** Whether this Android version exposes the OS privacy dashboard. */
    ANDROID_PRIVACY_DASHBOARD,

    /** Other apps' permissions. Always unmeasurable without privileged access. */
    OTHER_APP_PRIVACY,
}

/** The only three honest outcomes of a privacy check. */
enum class PrivacyStatus { PASS, ATTENTION, UNMEASURABLE }

enum class PrivacySeverity { INFO, LOW, MEDIUM, HIGH }

/** What the customer can actually do about a row, if anything. */
enum class PrivacyRemedy {
    /** A KNOUX-owned setting that changes this app's behaviour. */
    APP_SETTING,

    /** A real OS-owned screen. KNOUX opens it; the customer decides there. */
    OS_SCREEN,

    /** Factual information; nothing to change. */
    NONE,

    /** The platform does not expose this to KNOUX. Never rendered as fixable. */
    NOT_POSSIBLE,
}

/**
 * One observed privacy row.
 *
 * [observed] is the verbatim platform value, not an interpretation. A measured
 * row with a blank observation is invalid and [PrivacyRules.sanitize] downgrades
 * it to unmeasurable rather than displaying an empty "pass".
 */
data class PrivacyObservation(
    val check: PrivacyCheck,
    val status: PrivacyStatus,
    val severity: PrivacySeverity,
    val observed: String,
    val detail: String,
    val remedy: PrivacyRemedy,
)

/** The persisted, auditable result of one sweep. */
data class PrivacySummary(
    /** `null` when no check could be scored — never a fabricated zero. */
    val score: Int?,
    val scoredCount: Int,
    val measuredCount: Int,
    val attentionCount: Int,
    val unmeasurableCount: Int,
    val worst: PrivacySeverity?,
) {
    val label: String
        get() = when {
            score == null -> "Not measured"
            attentionCount == 0 && scoredCount > 0 -> "No attention item in the scored checks"
            else -> "$attentionCount attention item(s) across $scoredCount scored check(s)"
        }
}

object PrivacyRules {

    const val MAX_SCORE = 100

    private const val HIGH_PENALTY = 36
    private const val MEDIUM_PENALTY = 18
    private const val LOW_PENALTY = 8

    /** A measured pass. The caller must have read [observed] from the platform. */
    fun pass(
        check: PrivacyCheck,
        severity: PrivacySeverity,
        observed: String,
        detail: String,
    ): PrivacyObservation = PrivacyObservation(check, PrivacyStatus.PASS, severity, observed, detail, PrivacyRemedy.NONE)

    /** A measured problem with a real remedy. */
    fun attention(
        check: PrivacyCheck,
        severity: PrivacySeverity,
        observed: String,
        detail: String,
        remedy: PrivacyRemedy,
    ): PrivacyObservation {
        require(remedy != PrivacyRemedy.NOT_POSSIBLE) {
            "an attention row must name a real remedy; use unmeasurable() instead"
        }
        return PrivacyObservation(check, PrivacyStatus.ATTENTION, severity, observed, detail, remedy)
    }

    /** Something the platform does not let KNOUX observe. Never scored. */
    fun unmeasurable(check: PrivacyCheck, reason: String): PrivacyObservation =
        PrivacyObservation(
            check = check,
            status = PrivacyStatus.UNMEASURABLE,
            severity = PrivacySeverity.INFO,
            observed = "Not observable by KNOUX",
            detail = reason,
            remedy = PrivacyRemedy.NOT_POSSIBLE,
        )

    /**
     * Enforces the invariants in one place so a mistake in a caller cannot become
     * a false claim on screen:
     *
     *  - an empty observation is downgraded to unmeasurable;
     *  - an attention row without a real remedy is downgraded to unmeasurable;
     *  - an unmeasurable row can never carry APP_SETTING/OS_SCREEN/NONE;
     *  - a duplicated check keeps its most conservative row (attention beats a
     *    pass), so a repeated check cannot hide a problem;
     *  - rows are returned in enum order, which is the screen's reading order.
     */
    fun sanitize(rows: List<PrivacyObservation>): List<PrivacyObservation> {
        val normalized = rows.map { row ->
            when {
                row.observed.isBlank() -> unmeasurable(row.check, "The platform returned no value for this check")
                row.status == PrivacyStatus.ATTENTION && row.remedy == PrivacyRemedy.NOT_POSSIBLE ->
                    unmeasurable(row.check, "An attention row must name an action; this one did not")
                row.status == PrivacyStatus.UNMEASURABLE && row.remedy != PrivacyRemedy.NOT_POSSIBLE ->
                    unmeasurable(row.check, row.detail)
                row.status != PrivacyStatus.UNMEASURABLE && row.remedy == PrivacyRemedy.NOT_POSSIBLE ->
                    unmeasurable(row.check, "A measured row cannot be marked as impossible to act on")
                else -> row
            }
        }
        return normalized.groupBy { it.check }.map { (_, group) ->
            group.maxWithOrNull(
                compareBy<PrivacyObservation>({ statusWeight(it.status) }, { severityRank(it.severity) })
            )!!
        }.sortedBy { it.check.ordinal }
    }

    private fun statusWeight(status: PrivacyStatus): Int = when (status) {
        PrivacyStatus.UNMEASURABLE -> 0
        PrivacyStatus.PASS -> 1
        PrivacyStatus.ATTENTION -> 2
    }

    /** Rows that affect the score: measured, non-informational checks. */
    fun scored(rows: List<PrivacyObservation>): List<PrivacyObservation> =
        sanitize(rows).filter { it.status != PrivacyStatus.UNMEASURABLE && it.severity != PrivacySeverity.INFO }

    /** `null` when nothing scorable was measured. */
    fun score(rows: List<PrivacyObservation>): Int? {
        val table = scored(rows)
        if (table.isEmpty()) return null
        val penalty = table.sumOf { row ->
            if (row.status != PrivacyStatus.ATTENTION) 0 else when (row.severity) {
                PrivacySeverity.HIGH -> HIGH_PENALTY
                PrivacySeverity.MEDIUM -> MEDIUM_PENALTY
                PrivacySeverity.LOW -> LOW_PENALTY
                PrivacySeverity.INFO -> 0
            }
        }
        return (MAX_SCORE - penalty).coerceIn(0, MAX_SCORE)
    }

    fun summarize(rows: List<PrivacyObservation>): PrivacySummary {
        val clean = sanitize(rows)
        val table = scored(clean)
        return PrivacySummary(
            score = score(clean),
            scoredCount = table.size,
            measuredCount = clean.count { it.status != PrivacyStatus.UNMEASURABLE },
            attentionCount = table.count { it.status == PrivacyStatus.ATTENTION },
            unmeasurableCount = clean.count { it.status == PrivacyStatus.UNMEASURABLE },
            worst = table.filter { it.status == PrivacyStatus.ATTENTION }
                .maxByOrNull { severityRank(it.severity) }?.severity,
        )
    }

    fun severityRank(severity: PrivacySeverity): Int = when (severity) {
        PrivacySeverity.INFO -> 0
        PrivacySeverity.LOW -> 1
        PrivacySeverity.MEDIUM -> 2
        PrivacySeverity.HIGH -> 3
    }

    /**
     * Structural validation used by tests and by the repository before persisting.
     * Returns human-readable violations; an empty list means the pass is coherent.
     */
    fun validate(rows: List<PrivacyObservation>): List<String> {
        val problems = mutableListOf<String>()
        val duplicates = rows.groupingBy { it.check }.eachCount().filterValues { it > 1 }.keys
        if (duplicates.isNotEmpty()) problems += "duplicate checks: ${duplicates.joinToString()}"
        rows.forEach { row ->
            if (row.observed.isBlank()) problems += "${row.check}: blank observation"
            if (row.status == PrivacyStatus.ATTENTION && row.remedy == PrivacyRemedy.NOT_POSSIBLE) {
                problems += "${row.check}: attention without a remedy"
            }
            if (row.status == PrivacyStatus.UNMEASURABLE && row.remedy != PrivacyRemedy.NOT_POSSIBLE) {
                problems += "${row.check}: unmeasurable marked as fixable"
            }
            if (row.status == PrivacyStatus.UNMEASURABLE && row.severity != PrivacySeverity.INFO) {
                problems += "${row.check}: unmeasurable carries a non-informational severity"
            }
        }
        return problems
    }

    /** True only when the customer can act from inside KNOUX on this row. */
    fun isAppOwned(row: PrivacyObservation): Boolean =
        row.remedy == PrivacyRemedy.APP_SETTING && row.status == PrivacyStatus.ATTENTION

    /** True when the row is honest about being outside our reach. */
    fun isUnmeasurable(row: PrivacyObservation): Boolean = row.status == PrivacyStatus.UNMEASURABLE
}
