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
 * The hub card of the Smart Device Hub reference: title, copy, artwork, chevron.
 *
 * Shared with the Tools hub so both surfaces keep one card language.
 */
@Composable
internal fun HubCard(
    title: Copy, copy: Copy, mark: Mark, tint: Color, tag: String, onClick: () -> Unit
) {
    GlassPanel(Modifier.fillMaxWidth().heightIn(min = 122.dp).clip(RoundedCornerShape(20.dp))
        .testTag(tag).clickable(role = Role.Button, onClick = onClick), radius = 20, tint = tint) {
        Column(Modifier.fillMaxWidth().padding(11.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Label(title.localized(), 15, weight = FontWeight.ExtraBold)
            Label(copy.localized(), 10, Mist, maxLines = 3)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Box(Modifier.size(30.dp).clip(RoundedCornerShape(15.dp))
                    .background(Brush.horizontalGradient(listOf(tint.copy(alpha = .30f), tint.copy(alpha = .12f))),
                        RoundedCornerShape(15.dp))
                    .border(1.dp, tint.copy(alpha = .55f), RoundedCornerShape(15.dp))
                    .clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
                    Glyph(Mark.NEXT, Modifier.size(13.dp), tint, true)
                }
                MasterIcon(mark, tint, 46)
            }
        }
    }
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
