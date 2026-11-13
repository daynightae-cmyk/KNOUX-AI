package com.knoux.ai

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

/**
 * The six master screens rebuilt from the ten approved visual references:
 * Smart Device Hub, Smart Clean, Device Tools, Reports & Insights,
 * Privacy Sweep and AI Optimization.
 *
 * Every figure shown on these screens is sample data from the design and stays
 * visibly labelled as a design preview.
 */

// ---------------------------------------------------------------------------
// Shared master primitives
// ---------------------------------------------------------------------------

/** Two-tone italic title used by every master screen (white lead, magenta accent). */
@Composable
internal fun MasterTitle(lead: Copy, accent: Copy, subtitle: Copy, tag: String) {
    val roomy = LocalRoomy.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val styled = buildAnnotatedString {
            withStyle(SpanStyle(color = Chalk)) { append(lead.localized()) }
            withStyle(SpanStyle(brush = Brush.verticalGradient(listOf(Color(0xffffa3ff), Color(0xffe35bff), Color(0xffb93bff))))) {
                append(" ")
                append(accent.localized())
            }
        }
        Text(styled, Modifier.fillMaxWidth().testTag(tag), style = TextStyle(fontSize = (if (roomy) 24 else 29).sp, lineHeight = (if (roomy) 28 else 33).sp,
            fontFamily = DisplayFace, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center
        ))
        Label(subtitle.localized(), if (roomy) 12 else 13, Mist,
            modifier = Modifier.fillMaxWidth().padding(top = 3.dp), align = TextAlign.Center)
    }
}

/** The compact gold disclosure that marks every master screen as design preview data. */
@Composable
internal fun MasterPreviewPill(label: Copy = (Copy(R.string.k_design_preview))) {
    Box(Modifier.fillMaxWidth().padding(top = 7.dp), contentAlignment = Alignment.Center) {
        DesignPreviewPill(label = label)
    }
}

/** Circular information marker used by the honest-preview notes. */
@Composable
private fun InfoMarker(tint: Color = Mist, modifier: Modifier = Modifier) {
    Canvas(modifier.size(17.dp)) {
        drawCircle(tint.copy(alpha = .18f), radius = size.minDimension / 2f)
        drawCircle(tint.copy(alpha = .85f), radius = size.minDimension / 2f, style = Stroke(1.1.dp.toPx()))
        val w = 1.3.dp.toPx()
        drawLine(tint, Offset(size.width / 2f, size.height * .38f), Offset(size.width / 2f, size.height * .66f), w, StrokeCap.Round)
        drawCircle(tint, w * .62f, Offset(size.width / 2f, size.height * .28f))
    }
}

/** The honest-preview note strip. */
@Composable
internal fun PreviewNote(text: Copy, modifier: Modifier = Modifier) {
    GlassPanel(modifier.fillMaxWidth(), radius = 18, tint = Violet) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            InfoMarker()
            Label(text.localized(), 11, Mist)
        }
    }
}

/** Big rounded glass card used for the featured rows of the master screens. */
@Composable
internal fun MasterCard(modifier: Modifier = Modifier, radius: Int = 22, tint: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassPanel(modifier.fillMaxWidth(), radius = radius, tint = tint) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
    }
}

/**
 * Premium 3D glass-backed service icon matching the reference quality standard.
 *
 * Layer stack (back to front):
 *   1. Deep glass base with vertical dark-to-tint gradient
 *   2. Radial inner glow (bloom) from top-left corner
 *   3. Specular highlight arc on the top edge
 *   4. Lit rim border: white→tint→transparent gradient
 *   5. VolumetricGlyph with its own depth rendering
 *   6. Subtle bottom-edge shadow to anchor the tile
 */
@Composable
internal fun MasterIcon(mark: Mark, tint: Color, diameter: Int = 44) {
    val radius = (diameter * .30f).dp
    val shape = RoundedCornerShape(radius)
    Box(Modifier.size(diameter.dp).clip(shape)
        .background(Brush.verticalGradient(listOf(tint.copy(alpha = .60f), Color(0xff0e0828).copy(alpha = .95f),
                Color(0xff050215).copy(alpha = .98f))), shape)
        .border(1.2.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = .78f), tint.copy(alpha = .90f),
                tint.copy(alpha = .40f), Color.Transparent)), shape),
        contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            // Primary bloom from top-left
            bloom(Offset(size.width * .28f, size.height * .18f), size.width * .90f, tint, 1.0f)
            // Secondary smaller bloom for color depth
            bloom(Offset(size.width * .72f, size.height * .82f), size.width * .50f,
                tint.copy(alpha = .40f), 0.5f)
            // Specular highlight arc on upper rim
            drawArc(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = .55f), Color.White.copy(alpha = .80f),
                    Color.White.copy(alpha = .55f), Color.Transparent)),
                180f, 180f, false,
                Offset(size.width * .08f, size.height * .02f),
                Size(size.width * .84f, size.height * .28f),
                style = Stroke(size.width * .028f, cap = StrokeCap.Round))
        }
        VolumetricGlyph(mark, tint, (diameter * .68f).dp)
    }
}

/** The service row of the Device Tools reference: icon, copy, metric chip, chevron. */
@Composable
private fun ServiceRow(title: Copy, copy: Copy, metric: String, metricNote: Copy, mark: Mark, tint: Color,
    tag: String, onClick: () -> Unit
) {
    val mirror = if (LocalArabic.current) 180f else 0f
    GlassPanel(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
        .testTag(tag).clickable(role = Role.Button, onClick = onClick), radius = 20, tint = tint) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            MasterIcon(mark, tint, 46)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Label(title.localized(), 15, weight = FontWeight.ExtraBold)
                Label(copy.localized(), 10, Mist, maxLines = 2)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NeonPill(metric, Cyan, solid = false)
                    Label(metricNote.localized(), 10, Mist)
                }
            }
            Glyph(Mark.NEXT, Modifier.size(15.dp).rotate(mirror), Mist)
        }
    }
}

/** The ghost/primary action pair of the references. */
@Composable
internal fun MasterActions(first: Action, second: Action, onFirst: () -> Unit, onSecond: () -> Unit) {
    if (LocalRoomy.current) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AccentButton(first, Modifier.fillMaxWidth(), primary = false, tag = "review-action", onClick = onFirst)
        AccentButton(second, Modifier.fillMaxWidth(), primary = true, tag = "primary-action", onClick = onSecond)
    } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        AccentButton(first, Modifier.weight(1f), primary = false, tag = "review-action", onClick = onFirst)
        AccentButton(second, Modifier.weight(1f), primary = true, tag = "primary-action", onClick = onSecond)
    }
}

/** Minimal sparkline used by the two report cards of the reference. */
@Composable
private fun TrendSparkline(tint: Color, modifier: Modifier = Modifier) {
    val motion = LocalPreviewMotion.current
    var entered by remember { mutableStateOf(!motion) }
    LaunchedEffect(Unit) { entered = true }
    val phase by animateFloatAsState(if (entered) 1f else 0f,
        animationSpec = tween(if (motion) 900 else 0), label = "spark")
    Canvas(modifier) {
        val values = listOf(.18f, .34f, .28f, .52f, .46f, .74f, .92f)
        val step = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = step * index
            val y = size.height * (1f - value)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, Brush.horizontalGradient(listOf(tint.copy(alpha = .55f), tint)), style = Stroke(1.6.dp.toPx()))
        values.forEachIndexed { index, value ->
            drawCircle(Color.White.copy(alpha = .85f), 2.4.dp.toPx(), Offset(step * index, size.height * (1f - value)))
        }
    }
}

/** The segmented day-range control of the Reports reference. */
@Composable
private fun RangeTabs(selected: Int, onSelect: (Int) -> Unit) {
    val labels = listOf(Copy(R.string.k_t_7_days), Copy(R.string.k_t_30_days), Copy(R.string.k_t_90_days))
    GlassPanel(Modifier.fillMaxWidth(), radius = 22) {
        Row(Modifier.fillMaxWidth().padding(5.dp).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            labels.forEachIndexed { index, title ->
                val active = index == selected
                Box(Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(18.dp))
                    .background(if (active) Brush.horizontalGradient(listOf(Color(0xff9a2bff), Color(0xff5a28e8), Color(0xff2448ff)))
                    else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)))
                    .border(1.dp, if (active) Color.White.copy(alpha = .40f) else Color.White.copy(alpha = .12f), RoundedCornerShape(18.dp))
                    .selectable(active, role = Role.Tab, onClick = { onSelect(index) })
                    .testTag("range-$index"), contentAlignment = Alignment.Center) {
                    Label(title.localized(), 13, if (active) Chalk else Mist,
                        if (active) FontWeight.ExtraBold else FontWeight.Medium, align = TextAlign.Center)
                }
            }
        }
    }
}

/** The weekly activity bar chart of the Reports reference. */
@Composable
private fun ActivityChart(modifier: Modifier = Modifier) {
    val motion = LocalPreviewMotion.current
    var entered by remember { mutableStateOf(!motion) }
    LaunchedEffect(Unit) { entered = true }
    val reveal by animateFloatAsState(if (entered) 1f else 0f,
        animationSpec = tween(if (motion) 950 else 0), label = "chart reveal")
    val values = listOf("0.4 GB" to .25f, "0.7 GB" to .44f, "0.6 GB" to .38f, "0.9 GB" to .56f,
        "1.1 GB" to .69f, "1.3 GB" to .81f, "1.0 GB" to .63f)
    val days = listOf(Copy(R.string.k_mon), Copy(R.string.k_tue), Copy(R.string.k_wed), Copy(R.string.k_thu),
        Copy(R.string.k_fri), Copy(R.string.k_sat), Copy(R.string.k_sun))
    val dayLabels = days.map { it.localized() }
    val measurer = rememberTextMeasurer()
    Canvas(modifier) {
        val axisHeight = 16.dp.toPx()
        val top = 14.dp.toPx()
        val chartHeight = size.height - axisHeight - top
        val grid = listOf("1.6 GB", "1.2 GB", "0.8 GB", "0.4 GB", "0 GB")
        grid.forEachIndexed { index, label ->
            val y = top + chartHeight * (index / (grid.size - 1f))
            drawLine(Color.White.copy(alpha = .18f), Offset(0f, y), Offset(size.width, y),
                .9.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 7f)))
            val measured = measurer.measure(label, style = TextStyle(fontSize = 8.sp, color = Mist))
            drawText(measured, topLeft = Offset(0f, y - measured.size.height / 2f))        }
        val slot = size.width / values.size
        val barWidth = slot * .52f
        values.forEachIndexed { index, pair ->
            val value = pair.second * reveal
            val barHeight = chartHeight * value
            val x = slot * index + (slot - barWidth) / 2f
            val y = top + chartHeight - barHeight
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xffff54ff), Color(0xffb13aff), Color(0xff4f6bff), Cyan)),
                topLeft = Offset(x, y), size = Size(barWidth, barHeight), cornerRadius = CornerRadius(barWidth * .30f))
            drawRoundRect(Color.White.copy(alpha = .35f), topLeft = Offset(x, y),
                size = Size(barWidth, barHeight * .18f), cornerRadius = CornerRadius(barWidth * .30f))
            val label = measurer.measure(pair.first,
                style = TextStyle(fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Chalk))
            drawText(label, topLeft = Offset(x + (barWidth - label.size.width) / 2f,
                max(0f, y - label.size.height - 2.dp.toPx())))
            val day = measurer.measure(dayLabels[index], style = TextStyle(fontSize = 8.5.sp, color = Mist))
            drawText(day, topLeft = Offset(x + (barWidth - day.size.width) / 2f, top + chartHeight + 3.dp.toPx()))
        }
    }
}

/** The segmented risk donut of the Privacy reference. */
@Composable
private fun RiskDonut(modifier: Modifier = Modifier) {
    val motion = LocalPreviewMotion.current
    var entered by remember { mutableStateOf(!motion) }
    LaunchedEffect(Unit) { entered = true }
    val reveal by animateFloatAsState(if (entered) 1f else 0f,
        animationSpec = tween(if (motion) 900 else 0), label = "risk reveal")
    val segments = listOf(.13f to Color(0xffff4d8d), .25f to Gold, .38f to Cyan, .24f to Color(0xff9f9ad6))
    Canvas(modifier) {
        val band = size.minDimension * .17f
        val inset = size.minDimension * .07f
        val topLeft = Offset(inset, inset)
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        var angle = -90f
        val frontier = -90f + 360f * reveal
        segments.forEach { (share, color) ->
            val sweep = 360f * share
            val lit = (frontier - angle).coerceIn(0f, sweep)
            if (lit > .5f) {
                drawArc(color.copy(alpha = .30f), angle + 1.2f, lit - 2.4f, false, topLeft, arcSize, style = Stroke(band * 1.5f))
                drawArc(color, angle + 1.2f, lit - 2.4f, false, topLeft, arcSize, style = Stroke(band))
                drawArc(Color.White.copy(alpha = .45f), angle + 1.8f, lit - 3.6f, false, topLeft, arcSize, style = Stroke(band * .16f))
            }
            angle += sweep
        }
        drawCircle(Color.White.copy(alpha = .08f), radius = size.minDimension / 2f - inset - band * .6f,
            style = Stroke(1.dp.toPx()))
    }
}

/** A glowing opportunity sphere used by the AI Optimization reference. */
@Composable
private fun OpportunitySphere(modifier: Modifier = Modifier, title: Copy, mark: Mark, tint: Color, selected: Boolean,
    tag: String, onToggle: () -> Unit
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        val scale by animateFloatAsState(if (selected) 1f else .82f, tween(220), label = "sphere scale")
        Box(Modifier.size(74.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(37.dp))
            .background(Brush.radialGradient(listOf(tint.copy(alpha = .55f), Color(0xff1b0f52).copy(alpha = .85f))))
            .border(if (selected) 1.6.dp else 1.dp,
                Brush.verticalGradient(listOf(Color.White.copy(alpha = .85f), tint, Color.Transparent)),
                RoundedCornerShape(37.dp))
            .toggleable(selected, role = Role.Checkbox, onValueChange = { onToggle() })
            .testTag(tag), contentAlignment = Alignment.Center) {
            Canvas(Modifier.matchParentSize()) {
                bloom(Offset(size.width * .34f, size.height * .26f), size.width * .70f, tint, .85f)
            }
            VolumetricGlyph(mark, if (selected) Chalk else Mist, 30.dp)
        }
        Spacer(Modifier.height(6.dp))
        Label(title.localized(), 13, if (selected) Chalk else Mist,
            if (selected) FontWeight.ExtraBold else FontWeight.Medium, align = TextAlign.Center)
    }
}

// ---------------------------------------------------------------------------
// 1 - Smart Device Hub (Home)
// ---------------------------------------------------------------------------

private data class HubCardSpec(val title: Copy, val copy: Copy, val mark: Mark, val tint: Color, val tag: String, val page: Page
)

@Composable
internal fun SmartHubScreen(open: (Page) -> Unit) {
    val roomy = LocalRoomy.current
    val motion = LocalPreviewMotion.current
    // Native dashboard: content scrolls INSIDE the persistent top/bottom app shell.
    // The previous fixed viewport clipped Quick Actions on real 360dp phones.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally) {
            // Hero section - now reduced height from Pass 3
            NeonHero(Mark.K, motion, Modifier.padding(top = 4.dp, bottom = 6.dp),
                heightOverride = if (roomy) 96.dp else 112.dp)
            MasterTitle(Copy(R.string.k_smart), Copy(R.string.k_device_hub),
                Copy(R.string.k_device_intelligence_optimization_and_cleanup_with_a_clea),
                "screen-home")

            // Natural height prevents service rows and Quick Actions being cropped.
            Column(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)) {
                val cards = listOf(HubCardSpec(Copy(R.string.k_smart_clean),
                        Copy(R.string.k_remove_junk_duplicates_and_app_residue),
                        Mark.TRASH, Pink, "open-clean-center", Page.CLEAN_CENTER),
                    HubCardSpec(Copy(R.string.k_ai_optimization),
                        Copy(R.string.k_boost_performance_tune_system_and_save_battery),
                        Mark.CHIP, Cyan, "hub-ai-optimization", Page.AI_RECOMMENDATIONS),
                    HubCardSpec(Copy(R.string.k_privacy_sweep),
                        Copy(R.string.k_review_trackers_risky_permissions_and_exposure),
                        Mark.SHIELD, Pink, "hub-privacy-sweep", Page.PRIVACY_INSPECTOR),
                    HubCardSpec(Copy(R.string.k_storage_analyzer),
                        Copy(R.string.k_see_what_s_using_space_and_find_what_s_reclaimable),
                        Mark.DISK, Cyan, "hub-storage-analyzer", Page.STORAGE_BREAKDOWN),
                    HubCardSpec(Copy(R.string.k_app_manager),
                        Copy(R.string.k_manage_installed_apps_usage_and_heavy_weights),
                        Mark.GROUP, Pink, "hub-app-manager", Page.APPS_REVIEW),
                    HubCardSpec(Copy(R.string.k_reports_insights),
                        Copy(R.string.k_track_cleanups_optimizations_and_device_health_trends),
                        Mark.BARS, Cyan, "hub-reports", Page.REPORTS)
                )
                // Service grid - 3x2 on normal screens, vertical on roomy
                if (roomy) {
                    cards.forEach { spec ->
                        HubCard(spec.title, spec.copy, spec.mark, spec.tint, spec.tag) { open(spec.page) }
                    }
                } else {
                    cards.chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            pair.forEach { spec ->
                                Box(Modifier.weight(1f)) {
                                    HubCard(spec.title, spec.copy, spec.mark, spec.tint, spec.tag) { open(spec.page) }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Quick Actions row
                Label((Copy(R.string.k_quick_actions)).localized(), 15, weight = FontWeight.ExtraBold,
                    modifier = Modifier.fillMaxWidth().padding(start = 2.dp, top = 2.dp, bottom = 3.dp))
                MasterCard(radius = 18, tint = Violet) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        QuickAction(Modifier.weight(1f), Copy(R.string.k_deep_scan_2), Mark.RADAR, Cyan, "start-scan") { open(Page.SCAN_SUMMARY) }
                        Separator(Modifier.width(1.dp).height(34.dp))
                        QuickAction(Modifier.weight(1f), Copy(R.string.k_save_battery), Mark.BATTERY, Pink, "quick-battery") { open(Page.BATTERY_DIAGNOSTICS) }
                        Separator(Modifier.width(1.dp).height(34.dp))
                        QuickAction(Modifier.weight(1f), Copy(R.string.k_secure_device), Mark.SHIELD, Gold, "quick-privacy") { open(Page.PRIVACY_INSPECTOR) }
                    }
                }

                // Disclosure note - compact
                PreviewNote((Copy(R.string.k_preview_demo_sample_only_these_are_example_values_and_do)))
            }
        }
    }
}

@Composable
private fun QuickAction(modifier: Modifier = Modifier, title: Copy, mark: Mark, tint: Color, tag: String, onClick: () -> Unit
) {
    Column(modifier.clickable(role = Role.Button, onClick = onClick).testTag(tag)
        .padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Glyph(mark, Modifier.size(22.dp), tint, true)
        Label(title.localized(), 11, weight = FontWeight.Bold, align = TextAlign.Center)
    }
}

// ---------------------------------------------------------------------------
// 2 - Smart Clean (Clean)
// ---------------------------------------------------------------------------

// PC01: the screen is now backed by measured device data (see SmartCleanReal.kt).
// The approved hero, title, donut card, category rows and action pair are unchanged.
@Composable
internal fun SmartCleanScreen(open: (Page) -> Unit) = RealSmartCleanScreen(open)

// ---------------------------------------------------------------------------
// 3 - Device Tools (Tools)
// ---------------------------------------------------------------------------

@Composable
internal fun DeviceToolsScreen(open: (Page) -> Unit) {
    val motion = LocalPreviewMotion.current
    var showProGate by remember { mutableStateOf(false) }
    // Tools is an index screen (directory) — controlled scroll is acceptable here
    // but we still tighten the hero and reduce excess padding
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(176.dp), contentAlignment = Alignment.Center) {
            NeonHero(Mark.K, motion, heightOverride = 172.dp)
            SatelliteTile(Mark.DISK, Cyan, Modifier.align(Alignment.TopStart).padding(start = 22.dp, top = 12.dp))
            SatelliteTile(Mark.GROUP, Pink, Modifier.align(Alignment.TopEnd).padding(end = 22.dp, top = 6.dp))
            SatelliteTile(Mark.SHIELD, Pink, Modifier.align(Alignment.CenterStart).padding(start = 12.dp, bottom = 22.dp))
            SatelliteTile(Mark.BATTERY, Cyan, Modifier.align(Alignment.CenterEnd).padding(end = 12.dp, bottom = 26.dp))
        }
        MasterTitle(Copy(R.string.k_device), Copy(R.string.k_tools_2),
            Copy(R.string.k_your_device_intelligence_workspace), "screen-tools")
        MasterPreviewPill()
        // ── Pro Banner ──────────────────────────────────────────────────────
        ProSectionBanner(section = ProSection.APP_MANAGER,
            tagline = (Copy(R.string.k_batch_operations_app_freeze_backup_restore_usage_analyti)),
            onClick = { showProGate = true }
        )
        Column(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)) {
            GlassPanel(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                .testTag("open-storage").clickable(role = Role.Button) { open(Page.STORAGE_BREAKDOWN) }, radius = 22, tint = Pink) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Glyph(Mark.CHECK, Modifier.size(12.dp), Cyan, true)
                        Label((Copy(R.string.k_featured)).localized(), 10, Cyan, FontWeight.ExtraBold)
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Label((Copy(R.string.k_storage_analyzer_2)).localized(), 19, weight = FontWeight.ExtraBold)
                            Label((Copy(R.string.k_see_where_every_byte_goes_what_is_reclaimable_and_which)).localized(), 11, Mist)
                        }
                        MasterIcon(Mark.DISK, Pink, 48)
                    }
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Label("128 GB", 17, weight = FontWeight.ExtraBold)
                        Label((Copy(R.string.k_used_of_256_gb)).localized(), 11, Mist,
                            modifier = Modifier.padding(bottom = 2.dp))
                    }
                    GradientBar(.5f, thickness = 7, live = motion)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Label("50%", 14, Cyan, FontWeight.ExtraBold)
                        Spacer(Modifier.weight(1f))
                        Glyph(Mark.NEXT, Modifier.size(14.dp), Mist)
                    }
                }
            }
            ServiceRow(Copy(R.string.k_app_manager),
                Copy(R.string.k_manage_installed_apps_usage_heavy_weights_and_rarely_use),
                "41", Copy(R.string.k_apps_installed), Mark.GROUP, Pink, "open-app-manager") { open(Page.APPS_REVIEW) }
            ServiceRow(Copy(R.string.k_privacy_sweep),
                Copy(R.string.k_review_trackers_risky_permissions_and_privacy_exposure),
                "7", Copy(R.string.k_trackers_found_2), Mark.SHIELD, Pink, "open-privacy") { open(Page.PRIVACY_INSPECTOR) }
            ServiceRow(Copy(R.string.k_battery_diagnostics),
                Copy(R.string.k_check_battery_health_usage_patterns_and_background_drain),
                "+18%", Copy(R.string.k_estimated_battery_gain),
                Mark.BATTERY, Cyan, "open-battery") { open(Page.BATTERY_DIAGNOSTICS) }
            ServiceRow(Copy(R.string.k_ai_optimization),
                Copy(R.string.k_boost_performance_tune_system_and_optimize_battery_usage),
                "94", Copy(R.string.k_performance_score), Mark.CHIP, Pink, "open-ai-optimization") { open(Page.AI_RECOMMENDATIONS) }
            ServiceRow(Copy(R.string.k_storage_breakdown),
                Copy(R.string.k_a_clear_view_of_storage_by_category_photos_videos_apps_f),
                "12.6 GB", Copy(R.string.k_reclaimable_space),
                Mark.LAYERS, Cyan, "open-storage-breakdown") { open(Page.STORAGE_BREAKDOWN) }
        }
    }
    if (showProGate) {
        ProGateDialog(section = ProSection.APP_MANAGER,
            onDismiss = { showProGate = false },
            onActivate = { showProGate = false }
        )
    }
}

@Composable
private fun SatelliteTile(mark: Mark, tint: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
        .background(Brush.verticalGradient(listOf(tint.copy(alpha = .40f), Color(0xff150c36).copy(alpha = .9f))),
            RoundedCornerShape(14.dp))
        .border(1.dp, Brush.horizontalGradient(listOf(Color.White.copy(alpha = .7f), tint, Color.Transparent)),
            RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
        VolumetricGlyph(mark, tint, 26.dp)
    }
}

// ---------------------------------------------------------------------------
// 4 - Reports & Insights (Reports)
// ---------------------------------------------------------------------------

@Composable
internal fun ReportsScreen(open: (Page) -> Unit) {
    val motion = LocalPreviewMotion.current
    var range by rememberSaveable { mutableIntStateOf(0) }
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }
    var showProGate by remember { mutableStateOf(false) }
    val entries = PreviewFixtures.screen(Page.REPORTS, PreviewSource.DESIGN_PREVIEW)?.entries.orEmpty()
    Column(Modifier.fillMaxSize()) {
        NeonHero(Mark.BARS, motion, Modifier.padding(top = 4.dp, bottom = 6.dp))
        MasterTitle(Copy(R.string.k_reports_2), Copy(R.string.k_insights),
            Copy(R.string.k_see_device_trends_and_previous_actions), "screen-reports")
        MasterPreviewPill()
        // ── Pro Banner ──────────────────────────────────────────────────────
        ProSectionBanner(section = ProSection.REPORTS,
            tagline = (Copy(R.string.k_pdf_export_90_day_history_ai_predictions_weekly_reports)),
            onClick = { showProGate = true }
        )
        Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 11.dp, vertical = 7.dp)) {
            // The cards overflow on short screens (360x812dp), which used to clip
            // the whole action row out of the window. Scroll the evidence and pin
            // the actions so they are always reachable.
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
            RangeTabs(range) { range = it }
            MasterCard(radius = 20, tint = Violet) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    MasterIcon(Mark.BARS, Pink, 32)
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Label((Copy(R.string.k_device_activity_7_days)).localized(), 14, weight = FontWeight.ExtraBold)
                        Label((Copy(R.string.k_daily_optimization_activity_example)).localized(), 10, Mist)
                    }
                    PreviewChip((Copy(R.string.k_design_preview)), Gold, "preview-chip-chart")
                }
                ActivityChart(Modifier.fillMaxWidth().height(130.dp).padding(top = 5.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                MasterCard(Modifier.weight(1f), radius = 18, tint = Cyan) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MasterIcon(Mark.DISK, Pink, 30)
                        Column(Modifier.weight(1f)) {
                            Label((Copy(R.string.k_storage_trend)).localized(), 12, weight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Label("128 GB", 13, Cyan, FontWeight.ExtraBold)
                                Label("/ 256 GB", 9, Mist)
                            }
                        }
                    }
                    TrendSparkline(Cyan, Modifier.fillMaxWidth().height(32.dp).padding(top = 3.dp))
                    PreviewChip((Copy(R.string.k_design_preview)), Gold, "preview-chip-storage")
                }
                MasterCard(Modifier.weight(1f), radius = 18, tint = Violet) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MasterIcon(Mark.SHIELD, Cyan, 30)
                        Column(Modifier.weight(1f)) {
                            Label((Copy(R.string.k_privacy_review)).localized(), 12, weight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Label("7", 13, Cyan, FontWeight.ExtraBold)
                                Label((Copy(R.string.k_items)).localized(), 9, Mist)
                            }
                        }
                    }
                    TrendSparkline(Violet, Modifier.fillMaxWidth().height(32.dp).padding(top = 3.dp))
                    PreviewChip((Copy(R.string.k_design_preview)), Gold, "preview-chip-privacy")
                }
            }
            GlassPanel(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .testTag("open-report-history").clickable(role = Role.Button) { open(Page.REPORT_DETAIL) },
                radius = 18, tint = Violet) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    MasterIcon(Mark.CALENDAR, Pink, 34)
                    Column(Modifier.weight(1f)) {
                        Label((Copy(R.string.k_historical_activity_example)).localized(), 13, weight = FontWeight.Bold)
                        Label((Copy(R.string.k_scan_analyze_optimize_review)).localized(), 10, Mist)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        listOf(12, 20, 28).forEach { h ->
                            Box(Modifier.width(4.dp).height(h.dp).clip(RoundedCornerShape(2.dp))
                                .background(Brush.verticalGradient(listOf(Pink, Cyan))))
                        }
                    }
                    Glyph(Mark.NEXT, Modifier.size(14.dp), Mist)
                }
            }
            }
            MasterActions(Action(Copy(R.string.k_view_history), Mark.CALENDAR),
                Action(Copy(R.string.k_view_report_details), Mark.BARS),
                onFirst = { sheet = "review" }, onSecond = { sheet = "proceed" }
            )
        }
    }
    sheet?.let { mode ->
        InfoSheet(if (mode == "review") (Copy(R.string.k_view_history)) else (Copy(R.string.k_view_report_details)),
            { sheet = null }) {
            when (mode) {
                "review" -> {
                    AccentButton(Action(Page.REPORT_DETAIL.title, Page.REPORT_DETAIL.hero), Modifier.fillMaxWidth(),
                        tag = "open-performance-report-detail") {
                        sheet = null
                        open(Page.REPORT_DETAIL)
                    }
                    entries.forEach { Label("${it.title.localized()} · ${it.value}", 13, Mist) }
                }
                "proceed" -> {
                    entries.forEach { Label("${it.title.localized()} · ${it.value}", 13, Mist) }
                    AccentButton(Action(Copy(R.string.k_confirm_preview), Mark.CHECK), Modifier.fillMaxWidth(),
                        tag = "confirm-preview") { sheet = "ack" }
                }
                else -> Label(NoChange.localized(), 16, Cyan, FontWeight.Bold, Modifier.testTag("preview-only-result"))
            }
            Label(Disclosure.localized(), 12, Mist)
        }
    }
    if (showProGate) {
        ProGateDialog(section = ProSection.REPORTS,
            onDismiss = { showProGate = false },
            onActivate = { showProGate = false }
        )
    }
}

/** Small gold preview marker used inside cards (never the canonical design-preview tag). */
@Composable
private fun PreviewChip(label: Copy, tint: Color, tag: String) {
    Box(Modifier.heightIn(min = 26.dp).testTag(tag).clip(RoundedCornerShape(13.dp))
        .background(Brush.horizontalGradient(listOf(Color(0xff24163f), Color(0xff160d2c))), RoundedCornerShape(13.dp))
        .border(1.dp, tint.copy(alpha = .8f), RoundedCornerShape(13.dp)).padding(horizontal = 9.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center) {
        Label(label.localized(), 9, tint, FontWeight.Bold, align = TextAlign.Center)
    }
}

// ---------------------------------------------------------------------------
// 5 - Privacy Sweep (Privacy)
// ---------------------------------------------------------------------------

private data class RiskSpec(val title: Copy, val count: Copy, val risk: Copy, val tint: Color, val mark: Mark
)

@Composable
internal fun PrivacySweepScreen(open: (Page) -> Unit) {
    val motion = LocalPreviewMotion.current
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }
    var entryIndex by rememberSaveable { mutableIntStateOf(0) }
    var showProGate by remember { mutableStateOf(false) }
    val data = PreviewFixtures.screen(Page.PRIVACY, PreviewSource.DESIGN_PREVIEW)
    val risks = listOf(RiskSpec(Copy(R.string.k_camera_access), Copy(R.string.k_t_2_apps), Copy(R.string.k_medium_risk), Gold, Mark.CAMERA),
        RiskSpec(Copy(R.string.k_microphone_access), Copy(R.string.k_t_1_app), Copy(R.string.k_high_risk),
            Color(0xffff5d8f), Mark.MIC),
        RiskSpec(Copy(R.string.k_background_trackers), Copy(R.string.k_t_7_trackers),
            Copy(R.string.k_medium_risk), Gold, Mark.LAYERS),
        RiskSpec(Copy(R.string.k_clipboard_watchers), Copy(R.string.k_t_2_apps), Copy(R.string.k_low_risk), Cyan, Mark.CLIP)
    )
    val legend = listOf(Triple(Copy(R.string.k_high_risk), "1 (13%)", Color(0xffff5d8f)),
        Triple(Copy(R.string.k_medium_risk), "2 (25%)", Gold),
        Triple(Copy(R.string.k_low_risk), "3 (38%)", Cyan),
        Triple(Copy(R.string.k_minimal), "2 (25%)", Color(0xff9f9ad6))
    )
    Column(Modifier.fillMaxSize()) {
        NeonHero(Mark.SHIELD, motion, Modifier.padding(top = 4.dp, bottom = 6.dp))
        MasterTitle(Copy(R.string.k_privacy_2), Copy(R.string.k_sweep),
            Copy(R.string.k_understand_permissions_trackers_and_exposure),
            "screen-privacy")
        MasterPreviewPill((Copy(R.string.k_design_preview_sample_data)))
        // ── Pro Banner ──────────────────────────────────────────────────────
        ProSectionBanner(section = ProSection.PRIVACY_SWEEP,
            tagline = (Copy(R.string.k_real_time_threat_detection_encrypted_vault_tracker_block)),
            onClick = { showProGate = true }
        )
        Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 11.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)) {
            risks.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    pair.forEach { spec ->
                        val index = risks.indexOf(spec)
                        Box(Modifier.weight(1f)) {
                            RiskCard(spec, "row-$index") { entryIndex = index; sheet = "detail" }
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            MasterCard(radius = 20, tint = Violet) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
                        RiskDonut(Modifier.fillMaxSize())
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Label((Copy(R.string.k_privacy_risk)).localized(), 9, Mist, align = TextAlign.Center)
                            Label("38%", 26, weight = FontWeight.ExtraBold)
                            NeonPill("Medium", Gold, solid = false)
                        }
                    }
                    Separator(Modifier.width(1.dp).height(90.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Label((Copy(R.string.k_sample_risk_breakdown)).localized(), 12, weight = FontWeight.ExtraBold)
                        Label((Copy(R.string.k_design_preview_values)).localized(), 10, Mist)
                        legend.forEach { (name, count, color) ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
                                Label(name.localized(), 11, modifier = Modifier.weight(1f))
                                Label(count, 11, weight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            PreviewNote((Copy(R.string.k_sample_data_only_design_preview_values_to_illustrate_the)))
            Spacer(Modifier.weight(1f))
            MasterActions(Action(Copy(R.string.k_review_risks), Mark.SHIELD),
                Action(Copy(R.string.k_privacy_recommendations), Mark.CHECK),
                onFirst = { sheet = "review" }, onSecond = { sheet = "proceed" }
            )
        }
    }
    sheet?.let { mode ->
        val entries = data?.entries.orEmpty()
        InfoSheet(when (mode) {
                "detail" -> risks.getOrNull(entryIndex)?.title ?: (Copy(R.string.k_privacy_item))
                "review" -> (Copy(R.string.k_review_risks))
                else -> (Copy(R.string.k_privacy_recommendations))
            }, { sheet = null }) {
            when (mode) {
                "detail" -> {
                    risks.getOrNull(entryIndex)?.let {
                        Label(it.count.localized(), 18, Pink, FontWeight.Bold)
                        Label(it.risk.localized(), 13, it.tint, FontWeight.Bold)
                        Label((Copy(R.string.k_sample_entry_no_permission_was_read_on_this_device)).localized(), 12, Mist)
                    }
                    entries.getOrNull(entryIndex)?.let {
                        Label(it.detail.localized(), 13, Mist)
                        Label(it.value, 16, Pink, FontWeight.Bold)
                    }
                }
                "review" -> {
                    AccentButton(Action(Page.PRIVACY_INSPECTOR.title, Page.PRIVACY_INSPECTOR.hero),
                        Modifier.fillMaxWidth(), tag = "open-privacy-risk-inspector") {
                        sheet = null
                        open(Page.PRIVACY_INSPECTOR)
                    }
                    entries.forEach { Label("${it.title.localized()} · ${it.value}", 13, Mist) }
                }
                "proceed" -> {
                    entries.forEach { Label("${it.title.localized()} · ${it.value}", 13, Mist) }
                    AccentButton(Action(Copy(R.string.k_confirm_preview), Mark.CHECK), Modifier.fillMaxWidth(),
                        tag = "confirm-preview") { sheet = "ack" }
                }
                else -> Label(NoChange.localized(), 16, Cyan, FontWeight.Bold, Modifier.testTag("preview-only-result"))
            }
            Label(Disclosure.localized(), 12, Mist)
        }
    }
    if (showProGate) {
        ProGateDialog(section = ProSection.PRIVACY_SWEEP,
            onDismiss = { showProGate = false },
            onActivate = { showProGate = false }
        )
    }
}

@Composable
private fun RiskCard(spec: RiskSpec, tag: String, onClick: () -> Unit) {
    GlassPanel(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
        .testTag(tag).clickable(role = Role.Button, onClick = onClick), radius = 20, tint = spec.tint) {
        Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MasterIcon(spec.mark, spec.tint, 44)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Label(spec.title.localized(), 14, weight = FontWeight.ExtraBold)
                Label(spec.count.localized(), 16, weight = FontWeight.ExtraBold)
                RiskPill(spec.risk, spec.tint)
            }
        }
    }
}

@Composable
private fun RiskPill(label: Copy, tint: Color) {
    Box(Modifier.heightIn(min = 24.dp).clip(RoundedCornerShape(12.dp))
        .background(tint.copy(alpha = .16f)).border(1.dp, tint.copy(alpha = .8f), RoundedCornerShape(12.dp))
        .padding(horizontal = 9.dp, vertical = 3.dp), contentAlignment = Alignment.Center) {
        Label(label.localized(), 10, tint, FontWeight.Bold, align = TextAlign.Center)
    }
}

// ---------------------------------------------------------------------------
// 6 - AI Optimization (AI Optimization)
// ---------------------------------------------------------------------------

@Composable
internal fun AiOptimizationScreen(open: (Page) -> Unit) {
    val motion = LocalPreviewMotion.current
    var opportunities by rememberSaveable { mutableIntStateOf(0b111) }
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }
    var showProGate by remember { mutableStateOf(false) }
    val data = PreviewFixtures.screen(Page.OPTIMIZE, PreviewSource.DESIGN_PREVIEW)
    Column(Modifier.fillMaxSize()) {
        NeonHero(Mark.CHIP, motion, Modifier.padding(top = 4.dp, bottom = 6.dp))
        MasterTitle(Copy(R.string.k_ai), Copy(R.string.k_optimization_2),
            Copy(R.string.k_explore_performance_opportunities_with_knoux_ai),
            "screen-ai-optimization")
        MasterPreviewPill()
        // ── Pro Banner ──────────────────────────────────────────────────────
        ProSectionBanner(section = ProSection.AI_OPTIMIZATION,
            tagline = (Copy(R.string.k_gaming_boost_adaptive_ai_thermal_management_smart_scenes)),
            onClick = { showProGate = true }
        )
        Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 11.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)) {
            val metrics = listOf(Triple(Copy(R.string.k_performance_analysis), "94 /100", Mark.SPEED),
                Triple(Copy(R.string.k_battery_efficiency), "+18%", Mark.BATTERY),
                Triple(Copy(R.string.k_memory_pressure), "1.6 GB Free", Mark.MEMORY),
                Triple(Copy(R.string.k_thermal_observations), "Normal", Mark.TEMP)
            )
            metrics.chunked(2).forEachIndexed { row, pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    pair.forEachIndexed { columnInRow, spec ->
                        val index = row * 2 + columnInRow
                        Box(Modifier.weight(1f)) {
                            AiMetricCard(spec.first, spec.second, spec.third,
                                if (index == 0) "review-action" else "metric-$index") { sheet = "review" }
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Separator(Modifier.weight(.16f).height(1.dp))
                Label((Copy(R.string.k_optimization_opportunities)).localized(), 15, weight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(.68f), align = TextAlign.Center)
                Separator(Modifier.weight(.16f).height(1.dp))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                OpportunitySphere(Modifier.weight(1f), Copy(R.string.k_performance), Mark.BARS, Cyan,
                    opportunities and 1 != 0, "row-0") { opportunities = opportunities xor 1 }
                OpportunitySphere(Modifier.weight(1f), Copy(R.string.k_privacy), Mark.SHIELD, Pink,
                    opportunities and 2 != 0, "row-1") { opportunities = opportunities xor 2 }
                OpportunitySphere(Modifier.weight(1f), Copy(R.string.k_battery), Mark.BATTERY, Cyan,
                    opportunities and 4 != 0, "row-2") { opportunities = opportunities xor 4 }
            }
            Spacer(Modifier.weight(1f))
            AccentButton(Action(Copy(R.string.k_see_recommendations), Mark.SPARKLE),
                Modifier.fillMaxWidth(), primary = true, tag = "primary-action") { sheet = "proceed" }
            PreviewNote((Copy(R.string.k_design_preview_example_metrics_and_opportunities_for_vis)))
        }
    }
    sheet?.let { mode ->
        val entries = data?.entries.orEmpty()
        InfoSheet(if (mode == "review") (Copy(R.string.k_review_opportunities)) else (Copy(R.string.k_see_recommendations)),
            { sheet = null }) {
            when (mode) {
                "review" -> {
                    AccentButton(Action(Page.AI_RECOMMENDATIONS.title, Page.AI_RECOMMENDATIONS.hero),
                        Modifier.fillMaxWidth(), tag = "open-ai-recommendations") {
                        sheet = null
                        open(Page.AI_RECOMMENDATIONS)
                    }
                    entries.forEach { Label("${it.title.localized()} · ${it.value}", 13, Mist) }
                }
                "proceed" -> {
                    entries.forEach { Label("${it.title.localized()} · ${it.value}", 13, Mist) }
                    AccentButton(Action(Copy(R.string.k_confirm_preview), Mark.CHECK), Modifier.fillMaxWidth(),
                        tag = "confirm-preview") { sheet = "ack" }
                }
                else -> Label(NoChange.localized(), 16, Cyan, FontWeight.Bold, Modifier.testTag("preview-only-result"))
            }
            Label(Disclosure.localized(), 12, Mist)
        }
    }
    if (showProGate) {
        ProGateDialog(section = ProSection.AI_OPTIMIZATION,
            onDismiss = { showProGate = false },
            onActivate = { showProGate = false }
        )
    }
}

@Composable
private fun AiMetricCard(title: Copy, value: String, mark: Mark, tag: String, onReview: () -> Unit) {
    GlassPanel(Modifier.fillMaxWidth().heightIn(min = 122.dp).clip(RoundedCornerShape(20.dp))
        .testTag(tag).clickable(role = Role.Button, onClick = onReview), radius = 20, tint = Violet) {
        Column(Modifier.fillMaxWidth().padding(11.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Label(title.localized(), 14, weight = FontWeight.ExtraBold)
            Label((Copy(R.string.k_sample_metric)).localized(), 9, Mist)
            Spacer(Modifier.height(2.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MasterIcon(mark, if (mark == Mark.BATTERY) Cyan else Pink, 34)
                Label(value, 16, weight = FontWeight.ExtraBold)
            }
        }
    }
}
