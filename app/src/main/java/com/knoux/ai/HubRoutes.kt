package com.knoux.ai

import androidx.compose.runtime.Composable

/**
 * Two index routes that have no engine of their own in this build.
 *
 * Both used to render sample instrumental art. They now point at the surfaces
 * that do real work and say plainly what has not been measured.
 *
 * They share the unavailable-service composition rather than a bare column: an
 * index route is still a service screen, so it wears its world hero, its fog and
 * the fixed action rail exactly like every other destination.
 */
@Composable
internal fun PrivacySweepScreen(open: (Page) -> Unit) {
    EmptyServiceScreen(
        page = Page.PRIVACY,
        open = open,
        body = Copy(R.string.privacy_hub_body),
        primary = Action(Copy(R.string.privacy_open_inspector), Mark.SHIELD),
        onPrimary = { open(Page.PRIVACY_INSPECTOR) },
    )
}

@Composable
internal fun AiOptimizationScreen(open: (Page) -> Unit) {
    // The index route now opens the real on-device recommendation engine instead
    // of the old "no connected engine" placeholder. Every node it lights traces
    // to a measured pass the customer can open and verify.
    AiRecommendationsScreen(open)
}
