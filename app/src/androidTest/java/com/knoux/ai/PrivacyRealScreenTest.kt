package com.knoux.ai

import android.view.WindowManager
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.ai.data.KnouxData
import com.knoux.ai.data.PrivacyAction
import com.knoux.ai.data.PrivacyCheck
import com.knoux.ai.data.PrivacyStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.util.UUID

/**
 * PC04 end-to-end UI proof on the authorized phone.
 *
 * The screen is opened through the real navigation, measured against this build's
 * real package state, and the one KNOUX-owned setting is verified against the live
 * window flag. No personal data, clipboard or other app is touched.
 */
@RunWith(AndroidJUnit4::class)
class PrivacyRealScreenTest {
    private val ui = createAndroidComposeRule<MainActivity>()

    @get:Rule val rules: RuleChain = RuleChain
        .outerRule(object : ExternalResource() {
            override fun before() { SplashGate.consumed = true }
        })
        .around(ui)

    private var originalSecure = false
    private var originalScanId: String? = null

    private fun <T> io(block: suspend () -> T): T = runBlocking(Dispatchers.IO) { block() }
    private fun exists(tag: String) = ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    private fun node(tag: String): SemanticsNodeInteraction = ui.onAllNodesWithTag(tag).onLast()

    private fun click(tag: String) {
        val target = node(tag)
        runCatching { target.performScrollTo() }
        target.assertIsDisplayed().performClick()
    }

    private fun text(tag: String): String =
        node(tag).fetchSemanticsNode().config[SemanticsProperties.Text].joinToString(" ") { it.text }

    private fun reveal(tag: String) {
        ui.waitUntil(20_000) {
            runCatching { ui.onNodeWithTag("privacy-list").performScrollToNode(hasTestTag(tag)) }.isSuccess
        }
        node(tag).assertIsDisplayed()
    }

    private fun windowSecure(): Boolean =
        (ui.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0

    private fun waitForWindowFlag(expected: Boolean, timeoutMs: Long = 10_000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline && windowSecure() != expected) {
            Thread.sleep(100)
        }
        assertEquals("the live window FLAG_SECURE state", expected, windowSecure())
    }

    @Before fun clearTheSettingAndOpenTheRealScreen() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        KnouxData.init(context)
        originalSecure = io { KnouxData.settings.privacySecureWindow.first() }
        originalScanId = io { KnouxData.privacy.latest()?.scanId }
        io { KnouxData.settings.setPrivacySecureWindow(false) }
        ui.runOnUiThread { ui.activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }

        ui.waitUntil(30_000) { !exists("knoux-splash") }
        ui.waitUntil(20_000) { exists("onboarding-skip") || exists("auth-guest") || exists("tab-home") }
        if (exists("onboarding-skip")) click("onboarding-skip")
        ui.waitUntil(20_000) { exists("auth-guest") || exists("tab-home") }
        if (exists("auth-guest")) click("auth-guest")
        ui.waitUntil(20_000) { exists("tab-home") }
        click("tab-home")
        ui.waitUntil(10_000) { exists("hub-privacy-sweep") }
        click("hub-privacy-sweep")
        ui.waitUntil(10_000) { exists("screen-privacy-risk-inspector") }
        node("screen-privacy-risk-inspector").assertIsDisplayed()
    }

    @After fun restoreTheSettingAndTheWindow() {
        runCatching { io { KnouxData.settings.setPrivacySecureWindow(originalSecure) } }
        runCatching {
            ui.runOnUiThread {
                if (originalSecure) ui.activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else ui.activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    @Test fun theScreenShowsThisBuildsRealStateAndNeverClaimsOtherAppsAreReadable() {
        click("privacy-measure")
        ui.waitUntil(120_000) {
            io {
                val latest = KnouxData.privacy.latest()
                latest != null && latest.scanId != originalScanId
            }
        }
        val report = io { KnouxData.privacy.latest()!! }
        assertTrue("the pass must contain a real window observation", report.rows.isNotEmpty())
        assertNotEquals(originalScanId, report.scanId)

        // The screen must display the persisted score, not a fixture.
        ui.waitUntil(20_000) { exists("privacy-score") }
        reveal("privacy-score")
        assertEquals(report.summary.score.toString(), text("privacy-score"))

        // The measured network fact belongs to this build, whatever it is.
        val network = report.rows.single { it.check == PrivacyCheck.NETWORK_EGRESS }
        assertTrue(network.observed.isNotBlank())
        assertEquals(
            PrivacyStatus.ATTENTION,
            report.rows.single { it.check == PrivacyCheck.SCREENSHOT_PROTECTION }.status,
        )

        // Other apps are present and explicitly unmeasurable, never a fake count.
        reveal("privacy-check-other-app-privacy")
        assertTrue(
            "the screen must say other apps' data is not observable",
            ui.onAllNodesWithText("does not expose other apps", substring = true)
                .fetchSemanticsNodes().isNotEmpty(),
        )
        assertEquals(
            PrivacyStatus.UNMEASURABLE,
            report.rows.single { it.check == PrivacyCheck.OTHER_APP_PRIVACY }.status,
        )

        // The one app-owned action is visible and off by default in this test.
        reveal("privacy-toggle-secure")
        node("privacy-toggle-secure").assertIsOff()
        assertFalse(windowSecure())
    }

    @Test fun screenshotProtectionAppliesARealFlagRaisesTheStoredScoreAndIsVerified() {
        // A baseline pass with the flag off.
        click("privacy-measure")
        ui.waitUntil(120_000) {
            io {
                val latest = KnouxData.privacy.latest()
                latest != null && latest.scanId != originalScanId
            }
        }
        val before = io { KnouxData.privacy.latest()!! }
        assertFalse(windowSecure())
        val auditsBefore = io { KnouxData.privacy.observeAudit().first().map { it.id }.toSet() }

        reveal("privacy-toggle-secure")
        node("privacy-toggle-secure").assertIsOff()
        node("privacy-toggle-secure").performClick()

        // The OS flag, not the preference, is the proof.
        waitForWindowFlag(true)
        ui.waitUntil(30_000) {
            io {
                KnouxData.privacy.observeAudit().first().any { entry ->
                    entry.id !in auditsBefore &&
                        entry.check == PrivacyCheck.SCREENSHOT_PROTECTION &&
                        entry.action == PrivacyAction.SETTING_VERIFIED &&
                        entry.outcome == "VERIFIED"
                }
            }
        }

        // The verified flag triggers a fresh measured pass, so the stored score moves.
        ui.waitUntil(60_000) {
            io {
                val latest = KnouxData.privacy.latest()
                latest != null && latest.scanId != before.scanId
            }
        }
        val after = io { KnouxData.privacy.latest()!! }
        assertEquals(
            PrivacyStatus.PASS,
            after.rows.single { it.check == PrivacyCheck.SCREENSHOT_PROTECTION }.status,
        )
        assertEquals(
            "the verified flag must raise the stored score by exactly the low penalty",
            before.summary.score!! + 8,
            after.summary.score,
        )

        reveal("privacy-score")
        assertEquals(after.summary.score.toString(), text("privacy-score"))
    }
}
