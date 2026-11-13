package com.knoux.ai.data

import com.knoux.ai.visual.DeepScanStatus
import com.knoux.ai.visual.FacetState
import com.knoux.ai.visual.ModuleResult

/**
 * PC05 — pure decisions of a deep scan, kept out of Android so they can be proven
 * by a JVM test.
 *
 * Every rule here answers one question the UI must never guess:
 *
 *  - did this pass measure the device, or only part of it, or nothing at all?
 *  - is a module's sector full, thin, fogged or deliberately off?
 *  - is a file "large"/"old" by its own recorded facts, not by its name?
 *
 * Totals are de-duplicated by URI because one physical file can be found by more
 * than one module (a large old video), and counting it twice would inflate the
 * number the customer is asked to trust.
 */
internal object DeepScanRules {

    /** Files at or above this size are listed by the large-file module. */
    const val LARGE_FILE_BYTES: Long = 100L * 1024L * 1024L

    /** Files untouched for this long are listed by the old-file module. */
    const val OLD_FILE_MILLIS: Long = 365L * 24L * 60L * 60L * 1000L

    /**
     * The verdict of one pass.
     *
     * A cancelled pass is [DeepScanStatus.CANCELLED] even when what completed was
     * fully measured: the customer stopped it, and saying "complete" would claim a
     * coverage that was never reached.
     */
    internal fun resolveStatus(
        cancelled: Boolean,
        measuredModules: Int,
        partialModules: Int,
        unavailableModules: Int,
    ): DeepScanStatus = when {
        cancelled -> DeepScanStatus.CANCELLED
        measuredModules == 0 && partialModules == 0 -> DeepScanStatus.UNAVAILABLE
        unavailableModules > 0 || partialModules > 0 -> DeepScanStatus.PARTIAL
        else -> DeepScanStatus.COMPLETE
    }

    /** How one module is drawn, from the measurement status it really returned. */
    internal fun facetState(status: DataStatus): FacetState = when (status) {
        DataStatus.MEASURED, DataStatus.VERIFIED -> FacetState.MEASURED
        DataStatus.PARTIAL -> FacetState.THIN
        DataStatus.ESTIMATED -> FacetState.THIN
        // PREVIEW is never produced by the engine; if it ever appears it must be
        // drawn as unknown rather than as a measurement.
        DataStatus.PREVIEW, DataStatus.UNAVAILABLE -> FacetState.FOGGED
    }

    /** A file is large only when its recorded size reaches the threshold. */
    internal fun isLargeFile(bytes: Long): Boolean = bytes >= LARGE_FILE_BYTES

    /**
     * A file is old only when the provider reported a real timestamp older than the
     * cutoff. `0` means "unknown", and an unknown date is not evidence of age.
     */
    internal fun isOldFile(lastModifiedMillis: Long, nowMillis: Long): Boolean {
        if (lastModifiedMillis <= 0L) return false
        val cutoff = nowMillis - OLD_FILE_MILLIS
        return lastModifiedMillis < cutoff
    }

    /**
     * Distinct files across modules, first occurrence wins.
     *
     * The order is the module order, so the review sheet reads in the same order as
     * the sonar column.
     */
    internal fun distinctCandidates(results: List<ModuleResult>): List<FileCandidate> {
        val seen = LinkedHashMap<String, FileCandidate>()
        results.forEach { result ->
            result.candidates.forEach { candidate ->
                seen.putIfAbsent(candidate.uri, candidate)
            }
        }
        return seen.values.toList()
    }

    /**
     * True when a pass found nothing at all but could read at least one scope. That
     * is a real, meaningful result ("your readable scopes are clean") and must not
     * be presented as a failure.
     */
    internal fun isEmptyButMeasured(results: List<ModuleResult>): Boolean =
        results.isNotEmpty() &&
            results.all { it.status == DataStatus.MEASURED || it.status == DataStatus.VERIFIED } &&
            results.all { it.itemCount == 0 }
}
