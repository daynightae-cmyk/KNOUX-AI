package com.knoux.ai

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.util.Locale

/**
 * Verifies the cinematic identity gateway.
 *
 * The gateway is a real state machine: the VERIFIED state is only reachable
 * from a successful biometric callback, and the provider buttons report an
 * honest "provider not configured" state instead of fabricating a session.
 *
 * The test pins the English surface and the first-run flag so its assertions do
 * not depend on the owner's device language or on whatever onboarding state an
 * earlier session left behind. The owner's onboarding value is restored after
 * every case.
 */
@RunWith(AndroidJUnit4::class)
class AuthGatewayTest {
    init {
        Locale.setDefault(Locale.ENGLISH)
    }

    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private var previousOnboarding = false

    private val onboardingRule = object : ExternalResource() {
        override fun before() {
            val prefs = context.getSharedPreferences("knoux_prefs", Context.MODE_PRIVATE)
            previousOnboarding = prefs.getBoolean("knoux_onboarding_done", false)
            prefs.edit().putBoolean("knoux_onboarding_done", true).commit()
            SplashGate.consumed = true
        }

        override fun after() {
            context.getSharedPreferences("knoux_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("knoux_onboarding_done", previousOnboarding).commit()
        }
    }

    private val ui = createEmptyComposeRule()
    private lateinit var scenario: ActivityScenario<MainActivity>

    @get:Rule val rules: RuleChain = RuleChain.outerRule(onboardingRule).around(ui)

    private fun node(tag: String) = ui.onNodeWithTag(tag)

    /** The welcome portal is a scroll container; reveal a control before using it. */
    private fun reveal(tag: String) {
        val target = node(tag)
        runCatching { target.performScrollTo() }
        target.assertIsDisplayed()
    }

    private fun sheetMentions(text: String) =
        ui.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()

    @Before fun openGateway() {
        val intent = Intent(ApplicationProvider.getApplicationContext<Context>(), MainActivity::class.java)
            .putExtra("knoux.preview.english", true)
        scenario = ActivityScenario.launch(intent)
        ui.waitUntil(20_000) {
            ui.onAllNodesWithTag("auth-biometrics").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @After fun closeGateway() {
        if (::scenario.isInitialized) scenario.close()
    }

    @Test fun welcomeStageOffersTheThreeDocumentedProviders() {
        reveal("auth-biometrics")
        reveal("auth-passkey")
        reveal("auth-google")
        reveal("auth-guest")
    }

    @Test fun passkeyProviderReportsAnHonestSetupPendingState() {
        reveal("auth-passkey")
        node("auth-passkey").performClick()
        ui.waitUntil(5_000) { sheetMentions("not configured") }
        assertTrue("Passkey sheet must state that no provider is configured", sheetMentions("not configured"))
    }

    @Test fun googleProviderReportsAnHonestSetupPendingState() {
        reveal("auth-google")
        node("auth-google").performClick()
        ui.waitUntil(5_000) { sheetMentions("not configured") }
        assertTrue("Google sheet must state that no provider is configured", sheetMentions("not configured"))
    }

    @Test fun guestEntryOpensTheApplicationShell() {
        reveal("auth-guest")
        node("auth-guest").performClick()
        ui.waitUntil(20_000) {
            ui.onAllNodesWithTag("tab-home").fetchSemanticsNodes().isNotEmpty()
        }
        node("tab-home").assertIsDisplayed()
        // Release hygiene: no design-preview marker may survive in the shell.
        assertEquals(0, ui.onAllNodesWithTag("design-preview").fetchSemanticsNodes().size)
    }

    @Test fun theGatewayNeverShowsAVerifiedStateWithoutABiometricCallback() {
        // Without a real biometric success the VERIFIED state must not appear.
        assertEquals(0, ui.onAllNodesWithTag("auth-verified-orb").fetchSemanticsNodes().size)
    }
}
