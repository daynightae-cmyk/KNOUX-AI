package com.knoux.ai

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreviewNavigationTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()

    private fun node(tag: String) = ui.onNodeWithTag(tag)
    private fun click(tag: String) { node(tag).performClick() }
    private fun scrollClick(tag: String) {
        val target = node(tag)
        if (!target.isDisplayed()) target.performScrollTo()
        target.assertIsDisplayed().performClick()
    }
    private fun screen(page: String) { node("screen-$page").assertIsDisplayed(); node("design-preview").assertIsDisplayed() }
    private fun back() { click("back") }

    @Test fun tabsReachAllTenScreensAndDetailBackReturnsToTheIndex() {
        screen("home")
        click("tab-clean"); screen("clean")
        for (page in listOf("deep-scan", "duplicates", "residue")) {
            scrollClick("open-$page")
            screen(page)
            node("tab-clean").assertIsSelected()
            back(); screen("clean")
        }
        click("tab-tools"); screen("tools")
        for (page in listOf("storage", "app-manager", "privacy", "battery", "ai-optimization")) {
            scrollClick("open-$page")
            screen(page)
            node("tab-tools").assertIsSelected()
            back(); screen("tools")
        }
        click("tab-reports"); screen("reports")
        node("tab-reports").assertIsSelected()
        scrollClick("primary-action")
        scrollClick("confirm-preview")
        node("preview-only-result").assertIsDisplayed()
        scrollClick("close-sheet")
        click("tab-home"); screen("home")
    }

    @Test fun scanPauseAndResumeOnlyChangePreviewStateAndSurviveRecreation() {
        scrollClick("start-scan")
        screen("deep-scan")
        node("primary-action").assertIsNotEnabled()
        scrollClick("review-action")
        node("review-action").assertIsNotEnabled()
        node("primary-action").assertIsEnabled()
        ui.activityRule.scenario.recreate()
        node("primary-action").assertIsEnabled()
        scrollClick("primary-action")
        node("primary-action").assertIsNotEnabled()
    }

    @Test fun residueSelectionUpdatesTotalAndConfirmationDoesNotClean() {
        click("tab-clean")
        scrollClick("open-residue")
        node("select-all").performScrollTo().assertIsOn()
        node("row-0").performScrollTo().assertIsOn().performClick().assertIsOff()
        node("select-all").performScrollTo().assertTextContains("568 MB", substring = true)
        node("select-all").performClick().assertIsOn()
        node("select-all").performClick().assertIsOff()
        node("primary-action").assertIsNotEnabled()
        node("row-1").performScrollTo().performClick().assertIsOn()
        scrollClick("primary-action")
        scrollClick("close-sheet")
        node("row-1").assertIsOn()
        scrollClick("primary-action")
        scrollClick("confirm-preview")
        node("preview-only-result").assertTextContains("No cleanup performed", substring = true)
    }

    @Test fun arabicSwitchMirrorsTabOrderAndPreservesNavigation() {
        click("settings")
        click("primary-action")
        scrollClick("close-sheet")
        node("tab-home").assertTextContains("الرئيسية")
        val home = node("tab-home").fetchSemanticsNode().boundsInRoot
        val reports = node("tab-reports").fetchSemanticsNode().boundsInRoot
        check(home.left > reports.left) { "Tab order did not mirror in RTL" }
        click("tab-tools")
        scrollClick("open-battery")
        screen("battery")
        node("screen-battery").assertTextContains("توفير البطارية")
    }

    @Test fun everyRemainingPrimaryActionOpensAReversiblePreview() {
        click("tab-clean")
        scrollClick("open-duplicates")
        scrollClick("primary-action")
        node("sheet-choice-0").performScrollTo().assertIsOn().performClick().assertIsOff()
        scrollClick("confirm-preview")
        node("preview-only-result").assertIsDisplayed()
        scrollClick("close-sheet")
        click("tab-tools")
        for (page in listOf("storage", "app-manager", "privacy", "battery", "ai-optimization")) {
            scrollClick("open-$page")
            if (page == "ai-optimization") {
                node("row-0").performScrollTo().assertIsOn().performClick().assertIsOff()
            } else {
                scrollClick(if (page == "storage") "open-category-0" else "row-0")
                scrollClick("close-sheet")
            }
            scrollClick("review-action")
            scrollClick("close-sheet")
            scrollClick("primary-action")
            scrollClick("confirm-preview")
            node("preview-only-result").assertIsDisplayed()
            scrollClick("close-sheet")
            back()
        }
    }

    @Test fun cleanCenterSelectionChangesThePreviewTotalAndDoesNotClean() {
        scrollClick("open-clean-center")
        screen("clean-center")
        node("detail-selection-total").performScrollTo().assertTextContains("4 of 5 selected", substring = true)
        scrollClick("detail-select-0")
        node("detail-selection-total").performScrollTo().assertTextContains("3 of 5 selected", substring = true)
        scrollClick("primary-action")
        scrollClick("confirm-detail-preview")
        node("detail-preview-result").assertTextContains("No action performed", substring = true)
    }

    @Test fun outerReviewOpensAnInnerPageAndBackReturnsToItsOrigin() {
        scrollClick("open-duplicates")
        scrollClick("review-action")
        scrollClick("open-duplicate-group-review")
        screen("duplicate-group-review")
        node("tab-clean").assertIsSelected()
        node("detail-select-0").assertIsOn()
        scrollClick("detail-select-0")
        node("detail-select-0").assertIsOff()
        back()
        screen("duplicates")
    }
}
