package com.knoux.ai

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
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

    @Test fun liveSmileCuesOnceAtCompletionThenFinishes() {
        var finished = false
        var cues = 0
        ui.mainClock.autoAdvance = false
        ui.setContent { KnouxTheme(false, true) { KnouxSplashRoute(false, null, { cues++ }) { finished = true } } }
        ui.mainClock.advanceTimeBy(SPLASH_SMILE_END_MS - 50)
        ui.onNodeWithTag("knoux-splash").assertIsDisplayed()
        ui.runOnIdle { assertEquals(0, cues) }
        ui.mainClock.advanceTimeBy(150)
        ui.runOnIdle {
            assertEquals(1, cues)
            check(!finished) { "The smile cue must land before Home" }
        }
        ui.mainClock.advanceTimeBy(700)
        ui.waitForIdle()
        assertEquals(1, cues)
        check(finished) { "Splash did not finish after the full sequence" }
        check(SplashGate.consumed) { "Splash gate was not consumed" }
    }

    @Test fun reducedMotionShowsStaticBrandAndFinishesQuickly() {
        var finished = false
        var cues = 0
        ui.mainClock.autoAdvance = false
        ui.setContent { KnouxTheme(false, false) { KnouxSplashRoute(true, null, { cues++ }) { finished = true } } }
        ui.mainClock.advanceTimeBy(100)
        ui.onNodeWithTag("knoux-splash").assertIsDisplayed()
        ui.mainClock.advanceTimeBy(900)
        ui.waitForIdle()
        check(finished) { "Reduced-motion splash did not finish promptly" }
        assertEquals(0, cues)
    }

    @Test fun frozenDebugFrameStaysVisibleForCapture() {
        var finished = false
        var cues = 0
        ui.mainClock.autoAdvance = false
        ui.setContent { KnouxTheme(false, true) { KnouxSplashRoute(false, 2900L, { cues++ }) { finished = true } } }
        ui.mainClock.advanceTimeBy(5000)
        ui.waitForIdle()
        ui.onNodeWithTag("knoux-splash").assertIsDisplayed()
        check(!finished) { "Frozen capture frame must not auto-finish" }
        assertEquals(0, cues)
    }
}
