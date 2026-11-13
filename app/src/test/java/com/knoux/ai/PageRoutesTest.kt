package com.knoux.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The retired reference layer used to ship fixture metrics. Routes must now be
 * the only thing left: an id parses to exactly one page, and every deep route
 * belongs to a real tab.
 */
class PageRoutesTest {
    @Test
    fun debugRouteParserAcceptsOnlyKnownIds() {
        assertEquals(Page.DUPLICATES, Page.from("duplicates"))
        assertNull(Page.from("clean-device"))
        assertNull(Page.from(null))
    }

    @Test
    fun everyPageIdRoundTrips() {
        Page.entries.forEach { page ->
            assertEquals(page, Page.from(page.id))
        }
    }

    @Test
    fun pageIdsAreUnique() {
        val ids = Page.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun referenceRoutesAreTheTenMasterScreens() {
        assertEquals(10, Page.references.size)
        assertTrue(Page.references.all { it in Page.entries })
    }

    @Test
    fun everyDetailRouteBelongsToATab() {
        Page.detailRoutes.forEach { detail ->
            assertTrue("$detail has no tab", detail.reviewTab != null)
        }
    }
}
