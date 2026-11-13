package com.knoux.ai

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class PreviewNavigationTest {
    init {
        // The app follows the device language. The assertions below are written
        // against the English surface, so pin the default instead of depending on
        // the owner's phone settings.
        Locale.setDefault(Locale.ENGLISH)
    }

    @get:Rule val ui = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>

    private fun node(tag: String) = ui.onNodeWithTag(tag)

    private fun click(tag: String) { node(tag).performClick() }

    private fun scrollClick(tag: String) {
        val target = node(tag)
        if (!target.isDisplayed()) target.performScrollTo()
        target.assertIsDisplayed().performClick()
    }

    /** Brings a node into view when it sits inside a scroll container. */
    private fun showing(tag: String) {
        // Some pages legitimately host two disclosure pills (the hero plus the
        // service workspace); the first one is the page-level marker.
        val target = ui.onAllNodesWithTag(tag).onFirst()
        if (!target.isDisplayed()) target.performScrollTo()
        target.assertIsDisplayed()
    }

    /** A design reference page: its heading plus the sample-data disclosure pill. */
    private fun screen(page: String) {
        showing("screen-$page")
        showing("design-preview")
    }

    /**
     * PC02 turned Duplicates into a measured screen backed by real Room data. It
     * deliberately carries no design-preview fixture, so the live list and the
     * measured redundancy figure prove the screen instead.
     */
    private fun measuredScreen(page: String) {
        node("screen-$page").assertExists()
        showing("duplicate-list")
        showing("duplicate-redundant")
    }

    /** Detail routes render a service workspace plus a scrollable detail body. */
    private fun detailScreen(page: String) {
        showing("screen-$page")
        showing("detail-scroll-$page")
    }

    /** The five Device Tools entries open their measured detail workspaces. */
    private val toolDetails = listOf(
        "storage" to "storage-breakdown",
        "app-manager" to "installed-apps-review",
        "privacy" to "privacy-risk-inspector",
        "battery" to "battery-diagnostics",
        "ai-optimization" to "ai-recommendations",
    )

    private fun back() { click("back") }

    /**
     * The splash composes the dashboard underneath it while it fades out, so the
     * shell can be visible before it is live; interacting then lets the identity
     * gateway replace the screen mid-test. Wait for the splash to leave, then walk
     * the first-run surfaces that actually apply to this device.
     */
    @Before fun enterTheShell() {
        val intent = Intent(ApplicationProvider.getApplicationContext<Context>(), MainActivity::class.java)
            .putExtra("knoux.preview.english", true)
        scenario = ActivityScenario.launch(intent)

        fun present(tag: String) = ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

        ui.waitUntil(30_000) { !present("knoux-splash") }
        ui.waitUntil(20_000) {
            present("onboarding-skip") || present("auth-guest") || present("tab-home")
        }
        if (present("onboarding-skip")) ui.onNodeWithTag("onboarding-skip").performClick()
        ui.waitUntil(20_000) { present("auth-guest") || present("tab-home") }
        if (present("auth-guest")) {
            // The guest row is the last item of a scrollable sign-in surface.
            ui.onNodeWithTag("auth-guest").performScrollTo().performClick()
        }
        ui.waitUntil(20_000) { present("tab-home") }
    }

    @After fun closeShell() {
        if (::scenario.isInitialized) scenario.close()
    }

    @Test fun tabsReachAllTenScreensAndDetailBackReturnsToTheIndex() {
        screen("home")
        click("tab-clean"); screen("clean")
        for (page in listOf("deep-scan", "residue")) {
            scrollClick("open-$page")
            screen(page)
            node("tab-clean").assertIsSelected()
            back(); screen("clean")
        }
        // Duplicates is a measured screen since PC02, not a design fixture.
        scrollClick("open-duplicates")
        measuredScreen("duplicates")
        node("tab-clean").assertIsSelected()
        back(); screen("clean")
        click("tab-tools"); screen("tools")
        for ((entry, detail) in toolDetails) {
            scrollClick("open-$entry")
            detailScreen(detail)
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
        click("tab-clean")
        scrollClick("open-deep-scan")
        screen("deep-scan")
        node("review-action").assertIsEnabled()
        node("primary-action").assertIsNotEnabled()
        scrollClick("review-action")
        node("scan-state").assertTextContains("Preview paused")
        node("review-action").assertIsNotEnabled()
        node("primary-action").assertIsEnabled()
        scenario.recreate()
        node("primary-action").assertIsEnabled()
        scrollClick("primary-action")
        node("review-action").assertIsEnabled()
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
        // The settings sheet's second action is Arabic; the second action of the
        // language row mirrors every tab and the whole layout direction.
        scrollClick("primary-action")
        scrollClick("close-sheet")
        node("tab-home").assertTextContains("الرئيسية")
        val home = node("tab-home").fetchSemanticsNode().boundsInRoot
        val reports = node("tab-reports").fetchSemanticsNode().boundsInRoot
        check(home.left > reports.left) { "Tab order did not mirror in RTL" }
        click("tab-tools")
        scrollClick("open-battery")
        detailScreen("battery-diagnostics")
        node("screen-battery-diagnostics").assertTextContains("تشخيص البطارية")
    }

    @Test fun everyRemainingPrimaryActionOpensAReversiblePreview() {
        click("tab-tools")
        for ((entry, detail) in toolDetails) {
            scrollClick("open-$entry")
            detailScreen(detail)
            // Pages that ship a sample selection start with one row selected; a
            // page without a selection mask has no selectors at all. Either way
            // the primary action must only ever open the preview dialog.
            if (ui.onAllNodesWithTag("detail-select-0").fetchSemanticsNodes().isNotEmpty()) {
                scrollClick("detail-select-0")
            }
            scrollClick("primary-action")
            scrollClick("confirm-detail-preview")
            node("detail-preview-result").assertTextContains("No action performed", substring = true)
            scrollClick("close-sheet")
            back()
        }
    }

    @Test fun cleanCenterSelectionChangesThePreviewTotalAndDoesNotClean() {
        scrollClick("open-clean-center")
        detailScreen("clean-center")
        node("detail-selection-total").performScrollTo().assertTextContains("4 of 5 selected", substring = true)
        scrollClick("detail-select-0")
        node("detail-selection-total").performScrollTo().assertTextContains("3 of 5 selected", substring = true)
        scrollClick("primary-action")
        scrollClick("confirm-detail-preview")
        node("detail-preview-result").assertTextContains("No action performed", substring = true)
    }

    @Test fun outerReviewOpensTheMeasuredDuplicateScreenAndBackReturnsToItsOrigin() {
        // PC02 replaced the duplicate design preview with the real measured screen.
        // This still proves a detail route opens under the Clean tab and back returns.
        click("tab-clean")
        scrollClick("open-duplicates")
        measuredScreen("duplicates")
        node("tab-clean").assertIsSelected()
        check(ui.onAllNodesWithTag("service-world-duplicate-group-review").fetchSemanticsNodes().isEmpty()) {
            "Duplicate Finder must not render the old design-preview studio"
        }
        back()
        screen("clean")
    }
}
