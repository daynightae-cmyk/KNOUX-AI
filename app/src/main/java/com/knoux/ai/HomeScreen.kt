package com.knoux.ai

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
 * The hub card of the Smart Device Hub reference: icon, title, copy, chevron and
 * one measured value with a quiet action chip. Shared with the Tools hub so both
 * surfaces keep one card language.
 *
 * A card without a real measurement shows an em dash, never a fabricated number
 * and never the same "not measured yet" sentence repeated down the whole grid.
 */
@Composable
internal fun HubCard(
    title: Copy, copy: Copy, mark: Mark, tint: Color, tag: String, onClick: () -> Unit,
    value: Copy? = null, action: Copy? = null,
) {
    // One touch target per service (not a nested arrow button).
    GlassPanel(Modifier.fillMaxWidth().heightIn(min = 118.dp).clip(RoundedCornerShape(18.dp))
        .testTag(tag).clickable(role = Role.Button, onClick = onClick), radius = 18, tint = tint) {
        Column(Modifier.fillMaxWidth().heightIn(min = 112.dp).padding(horizontal = 11.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MasterIcon(mark, tint, 36)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Label(title.localized(), 13, weight = FontWeight.ExtraBold,
                        align = TextAlign.Start, maxLines = 2)
                    Label(copy.localized(), 12, Mist, align = TextAlign.Start, maxLines = 2)
                }
                Glyph(Mark.NEXT, Modifier.size(12.dp), Mist.copy(alpha = .7f))
            }
            if (value != null || action != null) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (value != null) {
                        Label(value.localized(), 12, tint, FontWeight.Bold,
                            Modifier.weight(1f), TextAlign.Start, 1)
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    action?.let { HubActionChip(it, tint) }
                }
            }
        }
    }
}

/** Non-interactive chip: the whole card is the touch target. */
@Composable
private fun HubActionChip(text: Copy, tint: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(9.dp))
            .background(tint.copy(alpha = .16f))
            .border(1.dp, tint.copy(alpha = .55f), RoundedCornerShape(9.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) { Label(text.localized(), 12, Chalk, FontWeight.Bold, maxLines = 1) }
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
