package com.knoux.ai

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Release hygiene: a service without a connected engine must say so.
 *
 * The retired design-preview studios filled these routes with sample rows and a
 * "PREVIEW" badge. That is exactly what the owner removed, so this test proves
 * the opposite now: an honest "not measured" state, no sample numbers, and a
 * primary action that only ever opens a service that really exists.
 */
@RunWith(AndroidJUnit4::class)
class UnavailableServiceTest {
    @get:Rule val ui = createComposeRule()

    private fun compose(page: Page, opened: (Page) -> Unit = {}) {
        ui.setContent {
            KnouxTheme(arabic = false, motion = false) {
                Box(Modifier.fillMaxSize().testTag("root")) { EmptyServiceScreen(page, opened) }
            }
        }
    }

    @Test fun anUnmeasuredServiceStatesTheTruthAndNeverFakesProgress() {
        compose(Page.STORAGE)

        ui.onNodeWithTag("service-unavailable").assertIsDisplayed()
        ui.onNodeWithTag("state-not-measured").assertIsDisplayed()

        check(ui.onAllNodesWithTag("design-preview").fetchSemanticsNodes().isEmpty()) {
            "A release build must not show a design-preview badge"
        }
        check(ui.onAllNodesWithTag("service-world-" + Page.STORAGE_BREAKDOWN.id)
            .fetchSemanticsNodes().isEmpty()) {
            "A service without an engine must not render a sample studio"
        }
    }

    @Test fun thePrimaryActionOpensARealServiceAndTheSecondaryGoesHome() {
        var opened: Page? = null
        compose(Page.BATTERY) { opened = it }

        ui.onNodeWithTag("primary-action").performClick()
        ui.runOnIdle { check(opened == Page.CLEAN) { "The primary action must open Smart Clean" } }

        ui.onNodeWithTag("secondary-action").performClick()
        ui.runOnIdle { check(opened == Page.HOME) { "The secondary action must return Home" } }
    }

    @Test fun theArabicSurfaceIsFullyArabic() {
        ui.setContent {
            KnouxTheme(arabic = true, motion = false) { EmptyServiceScreen(Page.STORAGE, {}) }
        }
        ui.onNodeWithText(KnouxStrings.get(R.string.state_not_measured, true)).assertIsDisplayed()
        check(ui.onAllNodesWithText(KnouxStrings.get(R.string.state_not_measured, false))
            .fetchSemanticsNodes().isEmpty()) {
            "English leaked into the Arabic surface"
        }
        check(ui.onAllNodesWithText(KnouxStrings.get(R.string.state_empty, false))
            .fetchSemanticsNodes().isEmpty()) {
            "English leaked into the Arabic surface"
        }
    }
}
