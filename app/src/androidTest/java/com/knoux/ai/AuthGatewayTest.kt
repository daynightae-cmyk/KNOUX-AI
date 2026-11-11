package com.knoux.ai

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the cinematic identity gateway.
 *
 * The gateway is a real state machine: the VERIFIED state is only reachable
 * from a successful biometric callback, and the provider buttons report an
 * honest "provider not configured" state instead of fabricating a session.
 */
@RunWith(AndroidJUnit4::class)
class AuthGatewayTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()

    private fun node(tag: String) = ui.onNodeWithTag(tag)

    private fun sheetMentions(text: String) =
        ui.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()

    @Before fun openGateway() {
        ui.waitUntil(20_000) {
            ui.onAllNodesWithTag("auth-fingerprint").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun welcomeStageOffersTheThreeDocumentedProviders() {
        node("auth-fingerprint").assertIsDisplayed()
        node("auth-passkey").assertIsDisplayed()
        node("auth-google").assertIsDisplayed()
        node("auth-guest").assertIsDisplayed()
    }

    @Test fun passkeyProviderReportsAnHonestSetupPendingState() {
        node("auth-passkey").performClick()
        ui.waitForIdle()
        assertTrue("Passkey sheet must state that no provider is configured", sheetMentions("not configured"))
    }

    @Test fun googleProviderReportsAnHonestSetupPendingState() {
        node("auth-google").performClick()
        ui.waitForIdle()
        assertTrue("Google sheet must state that no provider is configured", sheetMentions("not configured"))
    }

    @Test fun guestEntryOpensTheApplicationShell() {
        node("auth-guest").performClick()
        ui.waitUntil(20_000) {
            ui.onAllNodesWithTag("tab-home").fetchSemanticsNodes().isNotEmpty()
        }
        node("tab-home").assertIsDisplayed()
        node("design-preview").assertIsDisplayed()
    }

    @Test fun theGatewayNeverShowsAVerifiedStateWithoutABiometricCallback() {
        // Without a real biometric success the VERIFIED state must not appear.
        assertEquals(0, ui.onAllNodesWithTag("auth-verified-orb").fetchSemanticsNodes().size)
    }
}
