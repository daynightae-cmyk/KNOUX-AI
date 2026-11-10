package com.knoux.ai

import org.junit.Assert.*
import org.junit.Test

class PreviewFixturesTest {
    @Test fun liveUnavailableCannotExposeReferenceMetrics() {
        Page.references.forEach { assertNull(PreviewFixtures.screen(it, PreviewSource.LIVE_UNAVAILABLE)) }
    }

    @Test fun removingOneResidueSelectionChangesTheDisplayTotal() {
        val rows = checkNotNull(PreviewFixtures.screen(Page.RESIDUE, PreviewSource.DESIGN_PREVIEW)).entries
        assertEquals(836, PreviewFixtures.selectedMb(rows, rows.indices.toSet()))
        assertEquals(568, PreviewFixtures.selectedMb(rows, rows.indices.toSet() - 0))
        assertEquals(0, PreviewFixtures.selectedMb(rows, emptySet()))
    }

    @Test fun debugRouteParserAcceptsOnlyKnownIds() {
        assertEquals(Page.DUPLICATES, Page.from("duplicates"))
        assertNull(Page.from("clean-device"))
        assertNull(Page.from(null))
    }
}
