package com.knoux.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * The honest destination for a service that has no connected engine yet.
 *
 * The old design-preview screens filled this space with sample rows and a
 * "preview" badge. This screen states the truth instead: nothing has been
 * measured, nothing was scanned, and nothing on the device changed.
 */
@Composable
internal fun EmptyServiceScreen(page: Page, open: (Page) -> Unit) {
    Column(
        Modifier.fillMaxSize().testTag("service-unavailable").padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            SurfaceCard(Modifier.size(72.dp).wrapContentSize(), variant = true) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Glyph(page.hero, Modifier.size(36.dp), KnouxColors.accent)
                }
            }
            Label(page.title, 22, KnouxColors.textPrimary, FontWeight.Bold, align = TextAlign.Center)
            StatePill(Copy(R.string.state_not_measured), KnouxColors.warning, tag = "state-not-measured")
            Label(
                Copy(R.string.service_no_engine), 14, KnouxColors.textSecondary, align = TextAlign.Center
            )
            SurfaceCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FactLine(Copy(R.string.state_not_measured), Copy(R.string.state_unavailable))
                }
            }
        }
        BottomActionBar(
            primary = Action(Copy(R.string.action_open_clean), Mark.BROOM),
            secondary = Action(Copy(R.string.action_back_home), Mark.HOME),
            enabled = true,
            onPrimary = { open(Page.CLEAN) },
            onSecondary = { open(Page.HOME) },
        )
    }
}

@Composable
private fun FactLine(label: Copy, value: Copy) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Label(label, 13, KnouxColors.textSecondary, modifier = Modifier.weight(1f))
        Label(value, 13, KnouxColors.textPrimary, FontWeight.SemiBold)
    }
}
