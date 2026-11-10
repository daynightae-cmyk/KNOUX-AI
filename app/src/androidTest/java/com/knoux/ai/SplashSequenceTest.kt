package com.knoux.ai

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the cinematic brand signature with a controlled animation clock:
 * full choreography completes to Home, reduced motion is a brief static brand
 * transition, and the debug freeze frame stays put for deterministic capture.
 */
@RunWith(AndroidJUnit4::class)
class SplashSequenceTest {
    @get:Rule val ui = createComposeRule()

    @Test fun fullSignatureDrawsEverySceneThenFinishes() {
        var finished = false
        ui.mainClock.autoAdvance = false
        ui.setContent { KnouxTheme(false, true) { KnouxSplashRoute(false, null) { finished = true } } }
        ui.mainClock.advanceTimeBy(100)
        ui.onNodeWithTag("knoux-splash").assertIsDisplayed()
        ui.mainClock.advanceTimeBy(3600)
        ui.waitForIdle()
        check(finished) { "Splash did not finish after the full sequence" }
        check(SplashGate.consumed) { "Splash gate was not consumed" }
    }

    @Test fun reducedMotionShowsStaticBrandAndFinishesQuickly() {
        var finished = false
        ui.mainClock.autoAdvance = false
        ui.setContent { KnouxTheme(false, false) { KnouxSplashRoute(true, null) { finished = true } } }
        ui.mainClock.advanceTimeBy(100)
        ui.onNodeWithTag("knoux-splash").assertIsDisplayed()
        ui.mainClock.advanceTimeBy(900)
        ui.waitForIdle()
        check(finished) { "Reduced-motion splash did not finish promptly" }
    }

    @Test fun frozenDebugFrameStaysVisibleForCapture() {
        var finished = false
        ui.mainClock.autoAdvance = false
        ui.setContent { KnouxTheme(false, true) { KnouxSplashRoute(false, 2400L) { finished = true } } }
        ui.mainClock.advanceTimeBy(5000)
        ui.waitForIdle()
        ui.onNodeWithTag("knoux-splash").assertIsDisplayed()
        check(!finished) { "Frozen capture frame must not auto-finish" }
    }
}