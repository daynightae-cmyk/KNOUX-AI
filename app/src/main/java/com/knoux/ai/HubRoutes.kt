package com.knoux.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Two index routes that have no engine of their own in this build.
 *
 * Both used to render sample instrumental art. They now point at the surfaces
 * that do real work and say plainly what has not been measured.
 */
@Composable
internal fun PrivacySweepScreen(open: (Page) -> Unit) {
    HubRoute(
        page = Page.PRIVACY,
        body = Copy(R.string.privacy_hub_body),
        primary = Action(Copy(R.string.privacy_open_inspector), Mark.SHIELD),
        onPrimary = { open(Page.PRIVACY_INSPECTOR) },
    )
}

@Composable
internal fun AiOptimizationScreen(open: (Page) -> Unit) {
    HubRoute(
        page = Page.OPTIMIZE,
        body = Copy(R.string.ai_hub_body),
        primary = Action(Copy(R.string.action_open_clean), Mark.BROOM),
        onPrimary = { open(Page.CLEAN) },
    )
}

@Composable
private fun HubRoute(page: Page, body: Copy, primary: Action, onPrimary: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ServiceStatePill(Copy(R.string.state_not_measured))
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Label(page.title, 20, KnouxColors.textPrimary, FontWeight.Bold)
            Label(body, 14, KnouxColors.textSecondary)
            StateNote(Copy(R.string.service_no_engine))
            Spacer(Modifier.height(4.dp))
            PrimaryAction(primary, onClick = onPrimary)
        }
    }
}
