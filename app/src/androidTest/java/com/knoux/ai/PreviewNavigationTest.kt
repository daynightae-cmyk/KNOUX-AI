package com.knoux.ai

import android.content.Context
import android.content.Intent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.knoux.ai.data.KnouxData
import kotlinx.coroutines.runBlocking
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

    /** PC05 is a real measured scan, not the retired design-preview screen. */
    private fun deepScanScreen() {
        showing("deep-scan-modules")
        showing("deep-scan-start")
    }

    /**
     * PC03 turned Residue into a measured screen too, but it has no fixed summary
     * tag: the total may be unavailable until the customer runs a pass, so the
     * list and the total are the honest markers here.
     */
    private fun residueScreen() {
        node("screen-residue").assertExists()
        showing("residue-list")
        showing("residue-total")
    }

    /**
     * PC04 turned Privacy into a measured screen too. The score and the list are
     * the honest markers: the score is a dash until a real pass is run.
     */
    private fun privacyScreen() {
        node("screen-privacy-risk-inspector").assertExists()
        showing("privacy-list")
        showing("privacy-score")
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

    private val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()

    /**
     * The splash composes the dashboard underneath it while it fades out, so the
     * shell can be visible before it is live; interacting then lets the identity
     * gateway replace the screen mid-test. Wait for the splash to leave, then walk
     * the first-run surfaces that actually apply to this device.
     */
    private fun present(tag: String) = ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    /**
     * Walks the first-run surfaces that actually apply to this device. A relaunch
     * with FLAG_ACTIVITY_CLEAR_TASK can land on onboarding or the identity gateway
     * again, so every launch uses the same walk instead of assuming the shell.
     */
    private fun waitForTheShell() {
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

    @Before fun enterTheShell() {
        val intent = Intent(ApplicationProvider.getApplicationContext<Context>(), MainActivity::class.java)
            .putExtra("knoux.preview.english", true)
        scenario = ActivityScenario.launch(intent)
        waitForTheShell()
    }

    @After fun closeShell() {
        if (::scenario.isInitialized) scenario.close()
    }

    /** Regression: the native home scroll must not bury customer services or Quick Actions. */
    @Test fun nativeHomeShowsAllServicesAndReachableQuickActions() {
        screen("home")
        for (tag in listOf(
            "open-clean-center", "hub-ai-optimization", "hub-privacy-sweep",
            "hub-storage-analyzer", "hub-app-manager", "hub-reports",
            "start-scan", "quick-battery", "quick-privacy"
        )) {
            showing(tag)
        }
        node("tab-home").assertIsDisplayed()
        node("menu").assertIsDisplayed()
        node("settings").assertIsDisplayed()
        // The compact disclosure remains an actual action, not hidden by the brand.
        ui.onAllNodesWithTag("design-preview").onFirst().assertIsDisplayed()
    }

    /** P0: compact native header actions must not visually collide at phone width. */
    @Test fun compactHeaderControlsDoNotOverlapOnDevice() {
        val ids = listOf("menu", "design-preview", "pro", "settings")
        val rectangles = ids.map { tag ->
            val bounds = ui.onAllNodesWithTag(tag).onFirst().fetchSemanticsNode().boundsInRoot
            org.junit.Assert.assertTrue("$tag must have real bounds", bounds.width > 0f && bounds.height > 0f)
            tag to bounds
        }
        for (i in rectangles.indices) {
            for (j in i + 1 until rectangles.size) {
                val (leftName, left) = rectangles[i]
                val (rightName, right) = rectangles[j]
                val separated = left.right <= right.left || right.right <= left.left ||
                    left.bottom <= right.top || right.bottom <= left.top
                org.junit.Assert.assertTrue(
                    "Header controls $leftName and $rightName overlap", separated
                )
            }
        }
    }

    @Test fun tabsReachAllTenScreensAndDetailBackReturnsToTheIndex() {
        screen("home")
        click("tab-clean"); screen("clean")
        scrollClick("open-deep-scan")
        deepScanScreen()
        node("tab-clean").assertIsSelected()
        back(); screen("clean")
        // App Residue is a measured screen since PC03, not a design fixture.
        scrollClick("open-residue")
        residueScreen()
        node("tab-clean").assertIsSelected()
        back(); screen("clean")
        // Duplicates is a measured screen since PC02, not a design fixture.
        scrollClick("open-duplicates")
        measuredScreen("duplicates")
        node("tab-clean").assertIsSelected()
        back(); screen("clean")
        click("tab-tools"); screen("tools")
        for ((entry, detail) in toolDetails) {
            scrollClick("open-$entry")
            if (detail == "privacy-risk-inspector") privacyScreen() else detailScreen(detail)
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

    @Test fun realDeepScanEntrySurvivesRecreationWithoutStartingADelete() {
        click("tab-clean")
        scrollClick("open-deep-scan")
        deepScanScreen()
        // The original preview pause/resume test is obsolete: PC05 now has an
        // actual scanner. Recreate the Activity without starting a pass or
        // asking Android to modify personal user data.
        scenario.recreate()
        deepScanScreen()
        node("deep-scan-start").assertExists()
    }

    /**
     * PC03 replaced the residue fixture with a measured screen. The total it shows
     * must be the persisted measurement (or an explicit dash before the first
     * pass), and opening the review with nothing selected must offer no removal.
     */
    @Test fun residueScreenShowsOnlyPersistedDataAndNeverPreselects() {
        click("tab-clean")
        scrollClick("open-residue")
        residueScreen()

        val stored = runBlocking { KnouxData.residue.latest() }
        val expectedTotal = stored?.let { humanBytes(it.bytes) } ?: "\u2014"
        ui.waitUntil(20_000) {
            runCatching {
                node("residue-total").fetchSemanticsNode().config[SemanticsProperties.Text].first().text
            }.getOrNull() == expectedTotal
        }

        if (stored != null && stored.items.isNotEmpty()) {
            // A measured pass never carries a pre-selection: with nothing chosen by
            // hand, the review sheet states that and keeps deletion disabled.
            scrollClick("primary-action")
            node("residue-no-selection").assertIsDisplayed()
            node("residue-action-delete").assertIsNotEnabled()
            scrollClick("close-sheet")
        }

        back()
        screen("clean")
    }

    /**
     * RTL mirroring must not depend on the settings sheet being open.
     *
     * The Arabic surface is reached through the app's own debug capture hook
     * (`--ez knoux.preview.arabic true`), which is the same switch its reference
     * screenshots use. That keeps this case about layout mirroring rather than
     * about which settings row happened to be visible.
     */
    @Test fun arabicSwitchMirrorsTabOrderAndPreservesNavigation() {
        scenario.close()
        val arabic = Intent(Intent.ACTION_MAIN).apply {
            // Resolve through the target package, not the instrumentation context,
            // otherwise the class name lands on the test package.
            setClassName(instrumentation.targetContext.packageName, MainActivity::class.java.name)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra("knoux.preview.arabic", true)
        }
        scenario = ActivityScenario.launch<MainActivity>(arabic)
        waitForTheShell()

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
            if (detail == "privacy-risk-inspector") {
                // PC04 replaced this design fixture with the measured sweep: it has
                // no detail body, selection mask or preview dialog to prove.
                privacyScreen()
                back()
                continue
            }
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
