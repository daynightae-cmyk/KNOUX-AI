package com.knoux.ai

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * The Smart Device Hub — the application's home surface.
 *
 * Six service entries sit above a quick-action row; every figure shown on the
 * linked screens is sample data and is labelled as such.
 */
@Composable
internal fun HomeScreen(open: (Page) -> Unit) {
    SmartHubScreen(open)
}

/**
 * One service card of the hub.
 *
 * A card carries exactly three things: the world crest, the service name and one
 * measured value with its action. The longer description that used to live here
 * is gone on purpose — the reference direction is a visual surface where text is
 * a caption, and a truncated sentence is worse than no sentence.
 */
@Composable
internal fun HubCard(
    title: Copy, mark: Mark, tint: Color, tag: String, onClick: () -> Unit,
    value: Copy? = null, action: Copy? = null,
) {
    val world = worldOf(mark)
    val accent = world.primary
    // One touch target per service (not a nested arrow button).
    GlassPanel(
        Modifier.fillMaxWidth().heightIn(min = 104.dp).clip(RoundedCornerShape(20.dp))
            .testTag(tag)
            .clickable(role = Role.Button, onClick = onClick)
            .drawBehind {
                val width = size.width
                val height = size.height
                // A diagonal wash of the service hue: the card belongs to a world.
                drawRect(
                    Brush.linearGradient(
                        listOf(accent.copy(alpha = .20f), accent.copy(alpha = .03f), Color.Transparent),
                        start = Offset(0f, 0f), end = Offset(width * .95f, height * 1.1f),
                    )
                )
                // A lit rail on the reading-start edge.
                val railX = if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl) width - 2.dp.toPx() else 0f
                drawRect(
                    Brush.verticalGradient(
                        listOf(accent.copy(alpha = .95f), world.secondary.copy(alpha = .45f), Color.Transparent),
                    ),
                    topLeft = Offset(railX, height * .12f),
                    size = Size(2.dp.toPx(), height * .76f),
                )
                // A hairline of the world colour along the floor of the card.
                drawLine(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, accent.copy(alpha = .55f), world.secondary.copy(alpha = .30f), Color.Transparent),
                    ),
                    Offset(width * .06f, height - 1f), Offset(width * .94f, height - 1f),
                    strokeWidth = 1f,
                )
            },
        radius = 20, tint = accent,
    ) {
        Box(Modifier.fillMaxWidth()) {
            // The world's sigil as a quiet watermark; text always wins.
            Canvas(
                Modifier.align(Alignment.TopEnd).size(72.dp).offset(x = 26.dp, y = (-16).dp),
            ) { drawSigil(world, accent, alpha = .20f) }
            Column(
                Modifier.fillMaxWidth().heightIn(min = 98.dp).padding(horizontal = 11.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    WorldCrest(world, 36)
                    Label(
                        title.localized(), 14, KnouxColors.textPrimary, FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f), align = TextAlign.Start, maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
                if (value != null || action != null) {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (value != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Box(
                                    Modifier.size(5.dp).clip(RoundedCornerShape(50))
                                        .background(accent),
                                )
                                Label(
                                    value.localized(), 14, KnouxColors.textPrimary, FontWeight.Bold,
                                    maxLines = 1,
                                )
                            }
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                        action?.let { HubActionChip(it, accent, world.secondary) }
                    }
                }
            }
        }
    }
}

/** Non-interactive chip: the whole card is the touch target. */
@Composable
private fun HubActionChip(text: Copy, tint: Color, second: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(listOf(tint.copy(alpha = .22f), second.copy(alpha = .12f))))
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(tint.copy(alpha = .75f), second.copy(alpha = .45f))),
                RoundedCornerShape(50),
            )
            .padding(horizontal = 9.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) { Label(text.localized(), 12, Light.white, FontWeight.Bold, maxLines = 1) }
}

/**
 * The compact service card shared by the storage and hub surfaces.
 */
@Composable
internal fun CategoryCard(
    entry: Entry, index: Int, modifier: Modifier = Modifier, tag: String,
    floatPhase: State<Float>? = null, onClick: () -> Unit
) {
    val accent = if (index % 2 == 0) Pink else Cyan
    GlassPanel(modifier.heightIn(min = 66.dp).testTag("open-$tag")
        .clip(RoundedCornerShape(14.dp)).clickable(role = Role.Button, onClick = onClick),
        tint = accent) {
        Column(Modifier.fillMaxWidth().padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconBadge(entry.icon, accent, diameter = 34)
                Column(Modifier.weight(1f)) {
                    Label(entry.title.localized(), 12, weight = FontWeight.Bold)
                    Label(entry.detail.localized(), 10, Mist)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier
                    .background(Brush.horizontalGradient(
                        listOf(accent.copy(alpha = .46f), accent.copy(alpha = .20f)), 0f, 260f),
                        RoundedCornerShape(9.dp))
                    .border(1.dp, accent.copy(alpha = .82f), RoundedCornerShape(9.dp))
                    .padding(horizontal = 6.dp, vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Label(entry.value, 12, accent, FontWeight.Bold)
                    entry.note?.let { Label(it.localized(), 9, Mist) }
                }
                entry.progress?.let { Label("$it%", 10, Chalk, FontWeight.Bold, Modifier.weight(1f), TextAlign.End) }
            }
            entry.progress?.let { GradientBar(it / 100f, thickness = 3, live = true) }
        }
    }
}
