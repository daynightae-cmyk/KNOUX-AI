package com.knoux.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The honest destination for a service that has no connected engine yet.
 *
 * It wears the same shell as a measured service - section title, one card that
 * states the state, one footnote and the pinned action pair - so an unavailable
 * tool reads as part of the product instead of a blank page. It deliberately
 * carries no weather, no hero band and no invented figure: the cinematic fog was
 * the identity the owner rejected, and a service with no measurement has nothing
 * to visualise except its own absence.
 */
@Composable
internal fun EmptyServiceScreen(
    page: Page,
    open: (Page) -> Unit,
    body: Copy? = null,
    primary: Action? = null,
    onPrimary: (() -> Unit)? = null,
) {
    val tone = BrandTone.forTag(page.id)
    Column(Modifier.fillMaxSize().testTag("service-unavailable")) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            NativeScreenTitle(
                page.title,
                body ?: Copy(R.string.service_no_engine),
                "screen-${page.id}",
                Modifier.fillMaxWidth()
                    .padding(start = NativeSpace.edge, top = 12.dp, end = NativeSpace.edge),
            )
            Column(
                Modifier.fillMaxWidth()
                    .padding(horizontal = NativeSpace.edge, vertical = NativeSpace.row),
                verticalArrangement = Arrangement.spacedBy(NativeSpace.row),
            ) {
                SurfaceCard(Modifier.enterRise(0), accent = tone) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ServiceIconPlate(page.hero, tone, size = 42.dp, iconSize = 21.dp)
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            StatePill(
                                Copy(R.string.state_not_measured), NativePalette.amber,
                                tag = "state-not-measured",
                            )
                            Label(
                                Copy(R.string.service_unavailable_footnote),
                                12, NativePalette.muted, maxLines = 3,
                            )
                        }
                    }
                }

                SurfaceCard(Modifier.enterRise(1)) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Label(
                            Copy(R.string.technical_status), 13, NativePalette.muted,
                            modifier = Modifier.weight(1f),
                        )
                        Label(
                            Copy(R.string.state_unavailable), 13, NativePalette.text,
                            FontWeight.SemiBold,
                        )
                    }
                }

                StateNote(Copy(R.string.service_unavailable_footnote), Modifier.enterRise(2))
            }
            Spacer(Modifier.height(18.dp))
        }
        PinnedActions(
            first = Action(Copy(R.string.action_back_home), Mark.HOME),
            second = primary ?: Action(Copy(R.string.action_open_clean), Mark.BROOM),
            onFirst = { open(Page.HOME) },
            onSecond = { onPrimary?.invoke() ?: open(Page.CLEAN) },
        )
    }
}
