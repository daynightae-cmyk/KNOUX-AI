package com.knoux.ai

import com.knoux.ai.data.CandidateCategory
import com.knoux.ai.data.DataProvenance
import com.knoux.ai.data.Eligibility
import com.knoux.ai.data.FileCandidate
import com.knoux.ai.data.OperationKind
import com.knoux.ai.data.ResidueConfidence
import com.knoux.ai.data.ResiduePattern
import com.knoux.ai.data.ResidueRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM proof of PC03's two promises:
 *
 *  - a shared-folder file is never pre-selected on filename evidence alone;
 *  - the number the customer confirms in the dry run is exactly the plan.
 */
class ResidueRulesTest {

    private val now = 1_800_000_000_000L

    private fun candidate(
        name: String,
        bytes: Long = 4_096L,
        modified: Long = now - 10L * ResidueRules.DAY_MILLIS,
        provenance: DataProvenance = DataProvenance.SAF_DOCUMENT,
        eligibility: Eligibility = Eligibility.ELIGIBLE,
        uri: String = "content://test/${provenance.name}/$name",
        physicalKey: String? = null,
    ) = FileCandidate(
        uri = uri,
        displayName = name,
        mimeType = null,
        bytes = bytes,
        lastModifiedMillis = modified,
        category = when (provenance) {
            DataProvenance.APP_OWNED_CACHE -> CandidateCategory.APP_CACHE
            DataProvenance.MEDIASTORE -> CandidateCategory.IMAGE
            DataProvenance.SAF_DOCUMENT -> CandidateCategory.SAF_FILE
        },
        provenance = provenance,
        eligibility = eligibility,
        reason = "test",
        physicalKey = physicalKey,
    )

    private fun item(
        name: String,
        bytes: Long = 4_096L,
        modified: Long = now - 10L * ResidueRules.DAY_MILLIS,
        provenance: DataProvenance = DataProvenance.SAF_DOCUMENT,
        eligibility: Eligibility = Eligibility.ELIGIBLE,
    ) = ResidueRules.detect(
        candidate(name, bytes, modified, provenance, eligibility),
        minAgeDays = 1,
        nowMillis = now,
    )!!

    // ── Classification ───────────────────────────────────────────────────────

    @Test fun osPrefixesAreStrongEvidenceAndGenericNamesAreWeak() {
        assertEquals(ResiduePattern.INTERRUPTED_TRASH, ResidueRules.classify(".trashed-1234_photo.jpg")?.pattern)
        assertEquals(ResidueConfidence.STRONG, ResidueRules.classify(".trashed-1234_photo.jpg")?.confidence)
        assertEquals(ResiduePattern.PENDING_DELETE, ResidueRules.classify(".pending-99_file.bin")?.pattern)
        assertEquals(ResiduePattern.PARTIAL_DOWNLOAD, ResidueRules.classify("movie.mp4.part")?.pattern)
        assertEquals(ResidueConfidence.WEAK, ResidueRules.classify("movie.mp4.part")?.confidence)
        assertEquals(ResiduePattern.TEMP_FILE, ResidueRules.classify("notes.tmp")?.pattern)
        assertEquals(ResiduePattern.EDITOR_BACKUP, ResidueRules.classify("thesis.docx~")?.pattern)
        assertEquals(ResiduePattern.LOG_FILE, ResidueRules.classify("crash.log")?.pattern)
        assertEquals(ResiduePattern.THUMBNAIL, ResidueRules.classify("thumb_42.png", "image/png")?.pattern)
    }

    @Test fun aNormalFileNameIsNotResidue() {
        assertNull(ResidueRules.classify("IMG_20240101_120000.jpg", "image/jpeg"))
        assertNull(ResidueRules.classify("report.pdf", "application/pdf"))
        assertNull(ResidueRules.classify("", null))
    }

    // ── Detect / age rule ────────────────────────────────────────────────────

    @Test fun ownCacheIsResidueRegardlessOfAge() {
        val fresh = ResidueRules.detect(
            candidate("active-cache.bin", provenance = DataProvenance.APP_OWNED_CACHE, modified = now - 60_000L),
            minAgeDays = 30,
            nowMillis = now,
        )!!
        assertEquals(ResidueConfidence.OWNED, fresh.confidence)
        assertEquals(ResiduePattern.CACHE_FILE, fresh.pattern)
        assertEquals(Eligibility.ELIGIBLE, fresh.candidate.eligibility)
    }

    @Test fun sharedResidueIsRejectedWhenTooNewOrUndated() {
        assertNull(
            ResidueRules.detect(
                candidate("download.part", modified = now - 2L * ResidueRules.DAY_MILLIS),
                minAgeDays = 7,
                nowMillis = now,
            )
        )
        assertNull(
            ResidueRules.detect(
                candidate("undated.tmp", modified = 0L),
                minAgeDays = 0,
                nowMillis = now,
            )
        )
        // The same file becomes eligible once it is old enough.
        val old = ResidueRules.detect(
            candidate("download.part", modified = now - 8L * ResidueRules.DAY_MILLIS),
            minAgeDays = 7,
            nowMillis = now,
        )
        assertEquals(ResiduePattern.PARTIAL_DOWNLOAD, old?.pattern)
    }

    @Test fun zeroByteCacheFileIsListedButNeverSelectable() {
        val empty = ResidueRules.detect(
            candidate("empty.log", bytes = 0L, provenance = DataProvenance.APP_OWNED_CACHE),
            nowMillis = now,
        )!!
        assertEquals(Eligibility.UNSELECTABLE, empty.candidate.eligibility)
        assertFalse(ResidueRules.canSelect(empty))
        assertFalse(ResidueRules.isAutoSafe(empty))
    }

    // ── Selection guarantees ─────────────────────────────────────────────────

    @Test fun safeSelectionContainsOwnCacheOnlyEvenWhenSharedEvidenceIsStrong() {
        val owned = item("thumb_1.png", provenance = DataProvenance.APP_OWNED_CACHE)
        val strong = item(".trashed-77_photo.jpg")
        val weak = item("resume.docx~")
        val media = item("clip.mp4.part", provenance = DataProvenance.MEDIASTORE)

        val safe = ResidueRules.safeSelection(listOf(owned, strong, weak, media))
        assertEquals(setOf(owned.candidate.uri), safe)
        assertTrue(ResidueRules.canSelect(strong))
        assertTrue(ResidueRules.canSelect(media))
    }

    @Test fun toggleNeverSelectsAProviderProtectedItem() {
        val protectedItem = item("report.tmp", eligibility = Eligibility.UNSELECTABLE)
        assertEquals(emptySet<String>(), ResidueRules.toggle(listOf(protectedItem), emptySet(), protectedItem.candidate.uri))
        val allowed = item("report.tmp")
        assertEquals(
            setOf(allowed.candidate.uri),
            ResidueRules.toggle(listOf(allowed), emptySet(), allowed.candidate.uri),
        )
        assertEquals(
            emptySet<String>(),
            ResidueRules.toggle(listOf(allowed), setOf(allowed.candidate.uri), allowed.candidate.uri),
        )
    }

    // ── Dry run ──────────────────────────────────────────────────────────────

    @Test fun dryRunReportsExactlyTheSelectedSelection() {
        val a = item("a.tmp", bytes = 1_000L)
        val b = item("b.log", bytes = 2_500L)
        val c = item("c.bak", bytes = 9_999L)
        val run = ResidueRules.dryRun(listOf(a, b, c), setOf(a.candidate.uri, b.candidate.uri))
        assertEquals(2, run.items.size)
        assertEquals(3_500L, run.bytes)
        assertEquals(0, run.ownedCount)
        assertEquals(2, run.sharedCount)
        assertEquals(2, run.weakCount)
        assertTrue(run.canExecute)
        assertTrue(run.needsAndroidDialog)
        assertTrue(run.warnings.any { it.contains("shared-folder") })
    }

    @Test fun dryRunWithoutSelectionIsNotExecutable() {
        val a = item("a.tmp")
        val run = ResidueRules.dryRun(listOf(a), emptySet())
        assertFalse(run.canExecute)
        assertEquals(0, run.items.size)
        assertFalse(run.needsAndroidDialog)
        assertTrue(run.warnings.any { it.contains("Nothing is selected") })
    }

    @Test fun cacheOnlyDryRunNeedsNoAndroidDialog() {
        val cache = item("cache.bin", provenance = DataProvenance.APP_OWNED_CACHE)
        val run = ResidueRules.dryRun(listOf(cache), setOf(cache.candidate.uri), OperationKind.PERMANENT_DELETE)
        assertEquals(1, run.ownedCount)
        assertEquals(0, run.sharedCount)
        assertFalse(run.needsAndroidDialog)
    }

    @Test fun sharedMediaAlwaysNeedsTheOsDialogForBothActions() {
        val media = item("clip.part", provenance = DataProvenance.MEDIASTORE)
        val selected = setOf(media.candidate.uri)
        assertTrue(ResidueRules.dryRun(listOf(media), selected, OperationKind.MOVE_TO_TRASH).needsAndroidDialog)
        assertTrue(ResidueRules.dryRun(listOf(media), selected, OperationKind.PERMANENT_DELETE).needsAndroidDialog)
    }

    @Test fun trashIsOfferedOnlyForSharedMedia() {
        val media = item("clip.part", provenance = DataProvenance.MEDIASTORE)
        val cache = item("cache.bin", provenance = DataProvenance.APP_OWNED_CACHE)
        val saf = item("notes.tmp")
        assertTrue(ResidueRules.canTrash(listOf(media), setOf(media.candidate.uri)))
        assertFalse(ResidueRules.canTrash(listOf(cache), setOf(cache.candidate.uri)))
        assertFalse(ResidueRules.canTrash(listOf(saf), setOf(saf.candidate.uri)))
        assertFalse(
            ResidueRules.canTrash(
                listOf(media, saf),
                setOf(media.candidate.uri, saf.candidate.uri),
            )
        )
    }

    // ── Reconciliation ───────────────────────────────────────────────────────

    @Test fun afterVerifiedRemovalDropsOnlyProvenUris() {
        val gone = item("gone.tmp")
        val kept = item("kept.tmp")
        val remaining = ResidueRules.afterVerifiedRemoval(listOf(gone, kept), setOf(gone.candidate.uri))
        assertEquals(listOf(kept.candidate.uri), remaining.map { it.candidate.uri })
    }

    @Test fun summarySeparatesOwnedFromSharedBytes() {
        val cache = item("a.bin", bytes = 1_024L, provenance = DataProvenance.APP_OWNED_CACHE)
        val shared = item("b.tmp", bytes = 2_048L)
        val summary = ResidueRules.summarize(listOf(cache, shared))
        assertEquals(2, summary.count)
        assertEquals(3_072L, summary.bytes)
        assertEquals(1, summary.ownedCount)
        assertEquals(1_024L, summary.ownedBytes)
        assertEquals(2_048L, summary.sharedBytes)
        assertEquals(1, summary.patterns[ResiduePattern.CACHE_FILE])
        assertEquals(1, summary.patterns[ResiduePattern.TEMP_FILE])
    }

    @Test fun theSamePhysicalFileIsCountedOnceAndMediaStoreWins() {
        val safAlias = item("photo.jpg~")
        val mediaAlias = ResidueRules.detect(
            candidate(
                "photo.jpg~",
                provenance = DataProvenance.MEDIASTORE,
                uri = "content://media/external/images/media/7",
                physicalKey = "emulated/0:/DCIM/photo.jpg~",
            ),
            nowMillis = now,
        )!!
        val safWithKey = ResidueRules.detect(
            candidate(
                "photo.jpg~",
                provenance = DataProvenance.SAF_DOCUMENT,
                uri = "content://com.android.externalstorage.documents/tree/x/document/y",
                physicalKey = "emulated/0:/DCIM/photo.jpg~",
            ),
            nowMillis = now,
        )!!
        val distinct = ResidueRules.distinctPhysicalItems(listOf(safWithKey, mediaAlias))
        assertEquals(1, distinct.size)
        assertEquals(DataProvenance.MEDIASTORE, distinct.single().candidate.provenance)
        assertEquals(2, ResidueRules.distinctPhysicalItems(listOf(safAlias, mediaAlias)).size)
    }

    @Test fun defaultAgeRuleIsOfferedAndSharedScopesStayExplicit() {
        assertTrue(ResidueRules.DEFAULT_MIN_AGE_DAYS in ResidueRules.AGE_CHOICES)
        assertEquals(0, ResidueRules.AGE_CHOICES.first())
    }
}
