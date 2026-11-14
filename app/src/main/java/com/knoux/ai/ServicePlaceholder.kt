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
 * It keeps the same composition as a real service — hero, title, one state
 * pill, one explanation and the fixed action rail — so an unavailable tool
 * still reads as part of the product instead of a blank page.
 */
@Composable
internal fun EmptyServiceScreen(
    page: Page,
    open: (Page) -> Unit,
    body: Copy? = null,
    primary: Action? = null,
    onPrimary: (() -> Unit)? = null,
) {
    val motion = LocalPreviewMotion.current
    Column(Modifier.fillMaxSize().testTag("service-unavailable")) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            // Unmeasured scope is fog, not a number and not a bar. The whole
            // service area wears its world's weather behind the content.
            WorldFog(worldOf(page), Modifier.matchParentSize())
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                NeonHero(page.hero, motion, Modifier.padding(top = 2.dp), heightOverride = 84.dp)
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Label(
                        page.title, 24, KnouxColors.textPrimary, FontWeight.Bold,
                        align = TextAlign.Center, modifier = Modifier.testTag("screen-${page.id}"),
                    )
                    StatePill(Copy(R.string.state_not_measured), KnouxColors.warning, tag = "state-not-measured")
                    Label(
                        body ?: Copy(R.string.service_no_engine), 13, KnouxColors.textSecondary,
                        align = TextAlign.Center,
                    )
                    SurfaceCard {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            FactLine(Copy(R.string.technical_status), Copy(R.string.state_unavailable))
                        }
                    }
                    Label(
                        Copy(R.string.service_unavailable_footnote), 12,
                        KnouxColors.textSecondary, align = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(18.dp))
            }
        }
        BottomActionBar(
            primary = primary ?: Action(Copy(R.string.action_open_clean), Mark.BROOM),
            secondary = Action(Copy(R.string.action_back_home), Mark.HOME),
            enabled = true,
            onPrimary = { onPrimary?.invoke() ?: open(Page.CLEAN) },
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
