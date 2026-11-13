package com.knoux.ai

import com.knoux.ai.data.CandidateCategory
import com.knoux.ai.data.CleanupRules
import com.knoux.ai.data.DataProvenance
import com.knoux.ai.data.DuplicateGroup
import com.knoux.ai.data.DuplicateRules
import com.knoux.ai.data.Eligibility
import com.knoux.ai.data.FileCandidate
import com.knoux.ai.data.HashedFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM proof of the guarantees that must hold before any file can be removed:
 * exact-content grouping, one protected original per group, and a deterministic
 * keeper that survives a partial removal.
 */
class DuplicateRulesTest {

    private fun candidate(
        id: String,
        bytes: Long = 4_096L,
        modified: Long = 1_000L,
        eligibility: Eligibility = Eligibility.ELIGIBLE,
        provenance: DataProvenance = DataProvenance.APP_OWNED_CACHE,
        category: CandidateCategory = CandidateCategory.TEMP_FILE,
        physicalKey: String? = null,
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
        physicalKey = physicalKey,
    )

    private fun hash(marker: Char): String = marker.toString().repeat(64)

    @Test
    fun exactHashPairsBecomeOneGroupWithOneProtectedOriginal() {
        val older = candidate("original.bin", modified = 1_000L)
        val newer = candidate("copy.bin", modified = 2_000L)
        val groups = DuplicateRules.group(listOf(HashedFile(older, hash('a')), HashedFile(newer, hash('a'))))

        assertEquals(1, groups.size)
        val group = groups.single()
        assertEquals(hash('a'), group.id)
        assertEquals(older.uri, group.keeper.uri)
        assertEquals(Eligibility.PROTECTED, group.keeper.eligibility)
        assertEquals(listOf(newer.uri), group.duplicates.map { it.uri })
        assertEquals(2, group.memberCount)
        assertEquals(4_096L, group.redundantBytes)
        assertFalse("the keeper must never be selectable", CleanupRules.isSelectable(group.keeper))
        assertTrue(CleanupRules.isSelectable(group.duplicates.single()))
    }

    @Test
    fun aSingleFileOrAnInvalidHashNeverCreatesAGroup() {
        val alone = candidate("alone.bin")
        assertTrue(DuplicateRules.group(listOf(HashedFile(alone, hash('b')))).isEmpty())
        val other = candidate("other.bin")
        assertTrue(
            DuplicateRules.group(
                listOf(HashedFile(alone, "not-a-sha256"), HashedFile(other, "not-a-sha256"))
            ).isEmpty()
        )
    }

    @Test
    fun equalHashWithDifferentSizesIsRejectedDefensively() {
        val a = candidate("a.bin", bytes = 4_096L)
        val b = candidate("b.bin", bytes = 8_192L)
        assertTrue(DuplicateRules.group(listOf(HashedFile(a, hash('c')), HashedFile(b, hash('c')))).isEmpty())
    }

    @Test
    fun selectionNeverContainsTheProtectedOriginal() {
        val a = candidate("a.bin", modified = 10L)
        val b = candidate("b.bin", modified = 20L)
        val c = candidate("c.bin", modified = 30L)
        val group = DuplicateRules.group(
            listOf(HashedFile(a, hash('d')), HashedFile(b, hash('d')), HashedFile(c, hash('d')))
        ).single()

        val selected = DuplicateRules.toggleGroup(group, emptySet(), checked = true)
        assertEquals(setOf(b.uri, c.uri), selected)
        assertTrue(DuplicateRules.selectionKeepsEveryOriginal(listOf(group), selected))
        assertTrue(group.keeper.uri !in DuplicateRules.selectedCandidates(listOf(group), selected).map { it.uri })

        // A forged selection that includes the keeper is still filtered out.
        val forged = selected + group.keeper.uri
        assertTrue(group.keeper.uri !in DuplicateRules.selectedCandidates(listOf(group), forged).map { it.uri })

        assertEquals(emptySet<String>(), DuplicateRules.toggleGroup(group, selected, checked = false))
    }

    @Test
    fun unselectableCopyIsListedButCannotEnterAPlan() {
        val a = candidate("a.bin", modified = 10L)
        val locked = candidate(
            "locked.bin", modified = 20L, eligibility = Eligibility.UNSELECTABLE,
            provenance = DataProvenance.SAF_DOCUMENT, category = CandidateCategory.SAF_FILE,
        )
        val group = DuplicateRules.group(listOf(HashedFile(a, hash('e')), HashedFile(locked, hash('e')))).single()
        assertEquals(1, group.duplicates.size)
        assertTrue(DuplicateRules.selectableUris(group).isEmpty())
        assertTrue(DuplicateRules.toggleGroup(group, emptySet(), checked = true).isEmpty())
        assertTrue(DuplicateRules.selectedCandidates(listOf(group), setOf(locked.uri)).isEmpty())
    }

    @Test
    fun measuredBytesOnlyCountSelectedVerifiableCopies() {
        val a = candidate("a.bin", bytes = 4_096L, modified = 10L)
        val b = candidate("b.bin", bytes = 4_096L, modified = 20L)
        val c = candidate("c.bin", bytes = 4_096L, modified = 30L)
        val groups = DuplicateRules.group(
            listOf(HashedFile(a, hash('f')), HashedFile(b, hash('f')), HashedFile(c, hash('f')))
        )
        assertEquals(8_192L, groups.sumOf { it.redundantBytes })
        assertEquals(4_096L, DuplicateRules.selectedBytes(groups, setOf(b.uri)))
        assertEquals(8_192L, DuplicateRules.selectedBytes(groups, setOf(b.uri, c.uri)))
    }

    @Test
    fun verifiedRemovalDropsSingletonGroupAndPromotesTheOldestSurvivor() {
        val a = candidate("a.bin", modified = 10L)
        val b = candidate("b.bin", modified = 20L)
        val c = candidate("c.bin", modified = 30L)
        val group = DuplicateRules.group(
            listOf(HashedFile(a, hash('1')), HashedFile(b, hash('1')), HashedFile(c, hash('1')))
        ).single()

        val afterOriginalVanished = DuplicateRules.afterVerifiedRemoval(listOf(group), setOf(a.uri)).single()
        assertEquals(b.uri, afterOriginalVanished.keeper.uri)
        assertEquals(Eligibility.PROTECTED, afterOriginalVanished.keeper.eligibility)
        assertEquals(listOf(c.uri), afterOriginalVanished.duplicates.map { it.uri })

        assertTrue(
            "one surviving copy is not a duplicate group",
            DuplicateRules.afterVerifiedRemoval(listOf(afterOriginalVanished), setOf(c.uri)).isEmpty()
        )
    }

    @Test
    fun equalTimestampsChooseTheLowerUriSoTheKeeperNeverFlips() {
        val first = candidate("a.bin", modified = 500L)
        val second = candidate("b.bin", modified = 500L)
        val groups = DuplicateRules.group(listOf(HashedFile(second, hash('2')), HashedFile(first, hash('2'))))
        assertEquals(first.uri, groups.single().keeper.uri)
    }

    @Test
    fun trashRequiresEverySelectedCopyToBeSharedMedia() {
        val mediaA = candidate("a.jpg", provenance = DataProvenance.MEDIASTORE, category = CandidateCategory.IMAGE)
        val mediaB = candidate("b.jpg", provenance = DataProvenance.MEDIASTORE, category = CandidateCategory.IMAGE)
        val cache = candidate("c.tmp")
        assertTrue(DuplicateRules.canTrash(listOf(mediaA, mediaB)))
        assertFalse(DuplicateRules.canTrash(listOf(mediaA, cache)))
        assertFalse(DuplicateRules.canTrash(emptyList()))
        assertNull(DuplicateRules.selectedCandidates(emptyList(), emptySet()).firstOrNull())
    }

    @Test
    fun progressSummaryAgreesWithTheRealGrouping() {
        fun pair(name: String, bytes: Long, marker: Char): List<HashedFile> {
            val first = candidate("$name-1.bin", bytes = bytes, modified = 1L)
            val second = candidate("$name-2.bin", bytes = bytes, modified = 2L)
            return listOf(HashedFile(first, hash(marker)), HashedFile(second, hash(marker)))
        }
        val third = candidate("triple-3.bin", bytes = 4_096L, modified = 3L)
        val single = candidate("single.bin", bytes = 9_999L)
        val invalid = candidate("invalid.bin", bytes = 7_777L)
        val hashed = pair("small", 2_048L, '7') + pair("large", 100_000L, '8') +
            listOf(
                HashedFile(candidate("triple-1.bin", bytes = 4_096L, modified = 1L), hash('9')),
                HashedFile(candidate("triple-2.bin", bytes = 4_096L, modified = 2L), hash('9')),
                HashedFile(third, hash('9')),
                HashedFile(single, hash('a')),
                HashedFile(invalid, "not-a-sha256"),
            )
        val groups = DuplicateRules.group(hashed)
        val summary = DuplicateRules.summarize(hashed)
        assertEquals(groups.size, summary.groups)
        assertEquals(groups.sumOf { it.redundantBytes }, summary.redundantBytes)
        assertEquals(3, summary.groups)
        assertEquals(2_048L + 100_000L + 8_192L, summary.redundantBytes)
    }

    @Test
    fun groupsAreOrderedByRecoverableSpace() {
        fun pair(name: String, bytes: Long, marker: Char): List<HashedFile> {
            val first = candidate("$name-1.bin", bytes = bytes, modified = 1L)
            val second = candidate("$name-2.bin", bytes = bytes, modified = 2L)
            return listOf(HashedFile(first, hash(marker)), HashedFile(second, hash(marker)))
        }
        val groups = DuplicateRules.group(
            pair("small", 2_048L, '3') + pair("large", 100_000L, '4') +
                pair("large-two", 100_000L, '5') + pair("large-three", 100_000L, '6')
        )
        assertEquals(4, groups.size)
        assertEquals(100_000L, groups.first().redundantBytes)
        assertEquals(2_048L, groups.last().redundantBytes)
        assertTrue(groups.all { it.keeper.eligibility == Eligibility.PROTECTED })
    }

    @Test
    fun mediaAndSafAliasesOfOneFileAreNeverTwoCopies() {
        val key = "ext:primary:dcim/camera/photo.jpg"
        val media = candidate(
            "media-photo.jpg", provenance = DataProvenance.MEDIASTORE, physicalKey = key,
        )
        val saf = candidate(
            "saf-photo.jpg", provenance = DataProvenance.SAF_DOCUMENT, physicalKey = key,
        )

        val deduped = DuplicateRules.distinctPhysicalFiles(listOf(saf, media))
        assertEquals("the two URIs are one physical file", 1, deduped.size)
        assertEquals(
            "the OS-mediated MediaStore row is the representative",
            DataProvenance.MEDIASTORE,
            deduped.single().provenance,
        )

        // Even if an alias reaches grouping, one file can never be a copy of itself.
        val grouped = DuplicateRules.group(
            listOf(HashedFile(media, hash('7')), HashedFile(saf, hash('7'))),
        )
        assertTrue("one physical file must not form a duplicate group", grouped.isEmpty())
    }

    @Test
    fun distinctPhysicalFilesWithTheSameContentStillFormARealGroup() {
        val first = candidate("one.bin", physicalKey = "ext:primary:pictures/one.bin")
        val second = candidate("two.bin", physicalKey = "ext:primary:pictures/two.bin")
        assertEquals(2, DuplicateRules.distinctPhysicalFiles(listOf(first, second)).size)
        val groups = DuplicateRules.group(
            listOf(HashedFile(first, hash('8')), HashedFile(second, hash('8'))),
        )
        assertEquals(1, groups.size)
        assertEquals(2, groups.single().memberCount)
    }
}
