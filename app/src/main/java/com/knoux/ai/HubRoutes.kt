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
    EmptyServiceScreen(
        page = Page.OPTIMIZE,
        open = open,
        body = Copy(R.string.ai_hub_body),
        primary = Action(Copy(R.string.action_open_clean), Mark.BROOM),
        onPrimary = { open(Page.CLEAN) },
    )
}
