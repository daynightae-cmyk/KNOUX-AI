package com.knoux.ai

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ServiceWorldsTest {
    @get:Rule val ui = createComposeRule()

    @Test fun tenIndependentServiceStudiosRenderWithInteraction() {
        var current by mutableStateOf(Page.CLEAN_CENTER)
        ui.setContent {
            KnouxTheme(arabic = false, motion = false) {
                Box(Modifier.fillMaxSize().testTag("studio-root")) {
                    ServiceWorld(current, selectedCount = 2, totalCount = 5, selectedMb = 720)
                }
            }
        }

        check(Page.detailRoutes.size == 10) { "Expected ten internal service routes" }

        // PC02 replaced the duplicate studio with a measured screen; the remaining
        // nine services still expose their interactive design-preview studios.
        val previewPages = Page.detailRoutes.filter { it != Page.DUPLICATE_REVIEW }
        check(previewPages.size == 9) { "Expected nine remaining preview studios" }
        previewPages.forEach { page ->
            ui.runOnIdle { current = page }
            ui.onNodeWithTag("service-world-" + page.id).assertExists()
            if (page != Page.RESIDUE_REVIEW) {
                ui.onNodeWithTag("studio-choice-1").performClick()
                ui.onNodeWithTag("studio-choice-1").assertIsSelected()
            } else {
                ui.onNodeWithTag("studio-residue-node-1").performClick()
            }
        }
        ui.runOnIdle { current = Page.DUPLICATE_REVIEW }
        check(ui.onAllNodesWithTag("service-world-" + Page.DUPLICATE_REVIEW.id)
            .fetchSemanticsNodes().isEmpty()) {
            "Duplicate Finder must not present a design-preview studio anymore"
        }
    }
}
