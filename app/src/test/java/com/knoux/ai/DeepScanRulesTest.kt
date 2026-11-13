package com.knoux.ai

import com.knoux.ai.data.AccessScope
import com.knoux.ai.data.CandidateCategory
import com.knoux.ai.data.DataProvenance
import com.knoux.ai.data.DataStatus
import com.knoux.ai.data.Eligibility
import com.knoux.ai.data.FileCandidate
import com.knoux.ai.visual.DeepScanStatus
import com.knoux.ai.visual.ModuleResult
import com.knoux.ai.data.DeepScanRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM proof of the deep-scan decision rules that the Abyss Sonar must never guess:
 * status taxonomy, facet mapping, size/age thresholds, de-duplication, and the
 * empty-but-measured distinction.
 */
class DeepScanRulesTest {

    private fun candidate(
        id: String,
        bytes: Long = 4_096L,
        modified: Long = 1_000L,
        eligibility: Eligibility = Eligibility.ELIGIBLE,
        provenance: DataProvenance = DataProvenance.APP_OWNED_CACHE,
        category: CandidateCategory = CandidateCategory.TEMP_FILE,
    ) = FileCandidate(
        uri = "file:///knoux-qa/$id",
        displayName = id,
        mimeType = null,
        bytes = bytes,
        lastModifiedMillis = modified,
        category = category,
        provenance = provenance,
        eligibility = eligibility,
        reason = "JVM fixture",
    )

    private fun module(
        id: String,
        status: DataStatus,
        items: Int = 0,
        bytes: Long = 0,
        candidates: List<FileCandidate> = emptyList(),
    ) = ModuleResult(
        moduleId = id,
        order = 0,
        itemCount = items,
        totalBytes = bytes,
        candidates = candidates,
        status = status,
    )

    @Test
    fun cancelledPassIsCancelledEvenWhenEverythingCompleted() {
        assertEquals(
            DeepScanStatus.CANCELLED,
            DeepScanRules.resolveStatus(cancelled = true, measuredModules = 3, partialModules = 0, unavailableModules = 0),
        )
    }

    @Test
    fun allMeasuredIsComplete() {
        assertEquals(
            DeepScanStatus.COMPLETE,
            DeepScanRules.resolveStatus(cancelled = false, measuredModules = 4, partialModules = 0, unavailableModules = 0),
        )
    }

    @Test
    fun anyPartialIsPartial() {
        assertEquals(
            DeepScanStatus.PARTIAL,
            DeepScanRules.resolveStatus(cancelled = false, measuredModules = 3, partialModules = 1, unavailableModules = 0),
        )
    }

    @Test
    fun anyUnavailableWithoutPartialIsPartial() {
        assertEquals(
            DeepScanStatus.PARTIAL,
            DeepScanRules.resolveStatus(cancelled = false, measuredModules = 2, partialModules = 0, unavailableModules = 1),
        )
    }

    @Test
    fun nothingMeasuredIsUnavailable() {
        assertEquals(
            DeepScanStatus.UNAVAILABLE,
            DeepScanRules.resolveStatus(cancelled = false, measuredModules = 0, partialModules = 0, unavailableModules = 4),
        )
    }

    @Test
    fun facetStateMapsRealStatusesOnly() {
        assertEquals(com.knoux.ai.visual.FacetState.MEASURED, DeepScanRules.facetState(DataStatus.MEASURED))
        assertEquals(com.knoux.ai.visual.FacetState.MEASURED, DeepScanRules.facetState(DataStatus.VERIFIED))
        assertEquals(com.knoux.ai.visual.FacetState.THIN, DeepScanRules.facetState(DataStatus.PARTIAL))
        assertEquals(com.knoux.ai.visual.FacetState.THIN, DeepScanRules.facetState(DataStatus.ESTIMATED))
        assertEquals(com.knoux.ai.visual.FacetState.FOGGED, DeepScanRules.facetState(DataStatus.UNAVAILABLE))
        assertEquals(com.knoux.ai.visual.FacetState.FOGGED, DeepScanRules.facetState(DataStatus.PREVIEW))
    }

    @Test
    fun largeFileThresholdIsInclusive() {
        val threshold = DeepScanRules.LARGE_FILE_BYTES
        assertTrue(DeepScanRules.isLargeFile(threshold))
        assertTrue(DeepScanRules.isLargeFile(threshold + 1))
        assertFalse(DeepScanRules.isLargeFile(threshold - 1))
    }

    @Test
    fun oldFileNeedsARecordedTimestampOlderThanAYear() {
        val oneYearAgo = System.currentTimeMillis() - DeepScanRules.OLD_FILE_MILLIS - 1
        val exactlyOneYearAgo = System.currentTimeMillis() - DeepScanRules.OLD_FILE_MILLIS
        val yesterday = System.currentTimeMillis() - 86_400_000L

        assertTrue("older than a year", DeepScanRules.isOldFile(oneYearAgo, System.currentTimeMillis()))
        assertFalse("exactly a year is not old", DeepScanRules.isOldFile(exactlyOneYearAgo, System.currentTimeMillis()))
        assertFalse("yesterday is not old", DeepScanRules.isOldFile(yesterday, System.currentTimeMillis()))
        assertFalse("zero timestamp is unknown, not old", DeepScanRules.isOldFile(0L, System.currentTimeMillis()))
        assertFalse("negative timestamp is unknown, not old", DeepScanRules.isOldFile(-1L, System.currentTimeMillis()))
    }

    @Test
    fun distinctCandidatesDeduplicatesByUriKeepingFirstOccurrence() {
        val shared = candidate("shared.bin", bytes = 100L, modified = 5_000L)
        val results = listOf(
            module("m1", DataStatus.MEASURED, 1, 100L, listOf(shared)),
            module("m2", DataStatus.MEASURED, 1, 100L, listOf(shared, candidate("m2-only.bin"))),
        )
        val distinct = DeepScanRules.distinctCandidates(results)

        assertEquals(2, distinct.size)
        assertEquals("shared.bin", distinct[0].displayName)
        assertEquals("m2-only.bin", distinct[1].displayName)
        assertEquals(100L + 4096L, distinct.sumOf { it.bytes })
    }

    @Test
    fun emptyButMeasuredIsNotAFailure() {
        val clean = listOf(
            module("app_cache", DataStatus.MEASURED, 0, 0L),
            module("media_images", DataStatus.MEASURED, 0, 0L),
        )
        assertTrue(DeepScanRules.isEmptyButMeasured(clean))

        assertFalse(DeepScanRules.isEmptyButMeasured(emptyList()))
        assertFalse(DeepScanRules.isEmptyButMeasured(listOf(module("x", DataStatus.UNAVAILABLE, 0, 0L))))
        assertFalse(DeepScanRules.isEmptyButMeasured(listOf(module("x", DataStatus.MEASURED, 1, 100L))))
    }

    @Test
    fun deDuplicationCountsDistinctBytesOnlyOnce() {
        val shared = candidate("shared.bin", bytes = 1_000_000L, modified = 5_000L)
        val unique = candidate("unique.bin", bytes = 500L, modified = 5_001L)
        val results = listOf(
            module("large_files", DataStatus.MEASURED, 2, 1_000_500L, listOf(shared, unique)),
            module("old_files", DataStatus.MEASURED, 1, 1_000_000L, listOf(shared)),
        )
        val distinct = DeepScanRules.distinctCandidates(results)

        assertEquals(2, distinct.size)
        assertEquals(1_000_500L, distinct.sumOf { it.bytes })
    }
}
