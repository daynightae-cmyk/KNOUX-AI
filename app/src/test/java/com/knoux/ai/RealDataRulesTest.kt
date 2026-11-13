package com.knoux.ai

import com.knoux.ai.data.CandidateCategory
import com.knoux.ai.data.CleanupRules
import com.knoux.ai.data.DataProvenance
import com.knoux.ai.data.DataStatus
import com.knoux.ai.data.Eligibility
import com.knoux.ai.data.FileCandidate
import com.knoux.ai.data.OperationKind
import com.knoux.ai.data.OperationPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * These tests exist to stop the two failure modes the owner explicitly banned:
 * offering an item that cannot be removed, and claiming an operation ran when the
 * customer never confirmed it.
 */
class RealDataRulesTest {

    private fun candidate(
        uri: String = "content://media/external/images/media/1",
        bytes: Long = 1024L,
        eligibility: Eligibility = Eligibility.ELIGIBLE,
        provenance: DataProvenance = DataProvenance.MEDIASTORE,
        modifiedAt: Long = 1_700_000_000_000L
    ) = FileCandidate(
        uri = uri,
        displayName = "photo.jpg",
        mimeType = "image/jpeg",
        bytes = bytes,
        lastModifiedMillis = modifiedAt,
        category = CandidateCategory.IMAGE,
        provenance = provenance,
        eligibility = eligibility,
        reason = "test"
    )

    private fun plan(
        kind: OperationKind,
        confirmed: Boolean,
        vararg items: FileCandidate
    ) = OperationPlan(
        id = "plan-1",
        kind = kind,
        candidates = items.toList(),
        requiresSystemConfirmation = true,
        confirmedByUser = confirmed
    )

    @Test
    fun `unselectable and zero-byte candidates are never selectable`() {
        assertFalse(CleanupRules.isSelectable(candidate(eligibility = Eligibility.PROTECTED)))
        assertFalse(CleanupRules.isSelectable(candidate(eligibility = Eligibility.UNSELECTABLE)))
        assertFalse(CleanupRules.isSelectable(candidate(bytes = 0L)))
        assertTrue(CleanupRules.isSelectable(candidate()))
    }

    @Test
    fun `a plan cannot run before the customer confirms it`() {
        val p = plan(OperationKind.MOVE_TO_TRASH, confirmed = false, candidate())
        assertFalse(CleanupRules.canExecute(p, acknowledgedIrreversible = false))
    }

    @Test
    fun `trash runs after confirmation without an irreversible warning`() {
        val p = plan(OperationKind.MOVE_TO_TRASH, confirmed = true, candidate())
        assertTrue(CleanupRules.canExecute(p, acknowledgedIrreversible = false))
    }

    @Test
    fun `permanent delete additionally requires the irreversible acknowledgement`() {
        val p = plan(OperationKind.PERMANENT_DELETE, confirmed = true, candidate())
        assertFalse(CleanupRules.canExecute(p, acknowledgedIrreversible = false))
        assertTrue(CleanupRules.canExecute(p, acknowledgedIrreversible = true))
    }

    @Test
    fun `an empty plan is never executable`() {
        assertFalse(CleanupRules.canExecute(plan(OperationKind.MOVE_TO_TRASH, confirmed = true), false))
    }

    @Test
    fun `a plan containing one protected item is rejected wholesale`() {
        val p = plan(
            OperationKind.MOVE_TO_TRASH,
            confirmed = true,
            candidate(uri = "a"),
            candidate(uri = "b", eligibility = Eligibility.PROTECTED)
        )
        assertFalse(CleanupRules.canExecute(p, acknowledgedIrreversible = false))
    }

    @Test
    fun `media always needs the OS dialog while SAF only needs it for permanent delete`() {
        assertTrue(CleanupRules.requiresSystemDialog(OperationKind.MOVE_TO_TRASH, DataProvenance.MEDIASTORE))
        assertTrue(CleanupRules.requiresSystemDialog(OperationKind.PERMANENT_DELETE, DataProvenance.MEDIASTORE))
        assertFalse(CleanupRules.requiresSystemDialog(OperationKind.MOVE_TO_TRASH, DataProvenance.APP_OWNED_CACHE))
        assertTrue(CleanupRules.requiresSystemDialog(OperationKind.PERMANENT_DELETE, DataProvenance.SAF_DOCUMENT))
        assertFalse(CleanupRules.requiresSystemDialog(OperationKind.MOVE_TO_TRASH, DataProvenance.SAF_DOCUMENT))
    }

    @Test
    fun `only measured and verified statuses count as real data`() {
        assertTrue(CleanupRules.countsAsMeasured(DataStatus.MEASURED))
        assertTrue(CleanupRules.countsAsMeasured(DataStatus.VERIFIED))
        assertFalse(CleanupRules.countsAsMeasured(DataStatus.PREVIEW))
        assertFalse(CleanupRules.countsAsMeasured(DataStatus.ESTIMATED))
        assertFalse(CleanupRules.countsAsMeasured(DataStatus.UNAVAILABLE))
        assertFalse(CleanupRules.countsAsMeasured(DataStatus.PARTIAL))
    }

    @Test
    fun `plan totals use Long and do not overflow on large libraries`() {
        val big = 4_000_000_000L
        val p = plan(
            OperationKind.MOVE_TO_TRASH,
            confirmed = true,
            candidate(uri = "a", bytes = big),
            candidate(uri = "b", bytes = big),
            candidate(uri = "c", bytes = big)
        )
        assertTrue(p.totalBytes == 12_000_000_000L)
    }

    @Test
    fun `ANY age retains unknown-date files including negative dates`() {
        val items = listOf(
            candidate(uri = "unknown-zero", modifiedAt = 0L),
            candidate(uri = "unknown-negative", modifiedAt = -1L),
            candidate(uri = "known", modifiedAt = System.currentTimeMillis())
        )
        val state = CleanState(candidates = items, ageFilter = AgeFilter.ANY)
        assertEquals(items.map { it.uri }.toSet(), state.visible.map { it.uri }.toSet())
    }

    @Test
    fun `specific age filters reject unknown dates but retain genuinely old files`() {
        val now = System.currentTimeMillis()
        val items = listOf(
            candidate(uri = "unknown-zero", modifiedAt = 0L),
            candidate(uri = "unknown-negative", modifiedAt = -1L),
            candidate(uri = "recent", modifiedAt = now),
            candidate(uri = "future", modifiedAt = now + 86_400_000L),
            candidate(uri = "old", modifiedAt = now - 90L * 86_400_000L)
        )
        listOf(AgeFilter.WEEK, AgeFilter.MONTH).forEach { filter ->
            assertEquals(listOf("old"), CleanState(candidates = items, ageFilter = filter).visible.map { it.uri })
        }
    }

    @Test
    fun `trash requires a nonempty selection consisting entirely of media`() {
        val media = candidate(uri = "media-a")
        val moreMedia = candidate(uri = "media-b")
        val cache = candidate(uri = "file-cache", provenance = DataProvenance.APP_OWNED_CACHE)
        val saf = candidate(uri = "saf-document", provenance = DataProvenance.SAF_DOCUMENT)
        val items = listOf(media, moreMedia, cache, saf)
        fun supports(vararg selected: FileCandidate) = CleanState(
            candidates = items,
            selected = selected.map { it.uri }.toSet()
        ).trashSupported

        assertFalse(supports())
        assertTrue(supports(media))
        assertTrue(supports(media, moreMedia))
        assertFalse(supports(cache))
        assertFalse(supports(saf))
        assertFalse(supports(media, cache))
        assertFalse(supports(media, saf))
        assertFalse(supports(media, cache, saf))
        // Unselected cache/SAF rows must not disable a purely media selection.
        assertTrue(supports(moreMedia))
    }

    @Test
    fun `restore requires confirmed media only and never an irreversible warning`() {
        val media = candidate()
        assertTrue(CleanupRules.canExecute(plan(OperationKind.RESTORE_FROM_TRASH, true, media), false))
        assertTrue(CleanupRules.requiresSystemDialog(OperationKind.RESTORE_FROM_TRASH, DataProvenance.MEDIASTORE))
        assertFalse(CleanupRules.requiresIrreversibleWarning(OperationKind.RESTORE_FROM_TRASH))
        assertFalse(CleanupRules.canExecute(plan(OperationKind.RESTORE_FROM_TRASH, false, media), false))
        assertFalse(CleanupRules.canExecute(plan(OperationKind.RESTORE_FROM_TRASH, true), false))
        listOf(DataProvenance.APP_OWNED_CACHE, DataProvenance.SAF_DOCUMENT).forEach { source ->
            val nonmedia = candidate(uri = "nonmedia-$source", provenance = source)
            assertFalse(CleanupRules.canExecute(plan(OperationKind.RESTORE_FROM_TRASH, true, nonmedia), false))
            assertFalse(CleanupRules.canExecute(plan(OperationKind.RESTORE_FROM_TRASH, true, media, nonmedia), true))
        }
    }
}
