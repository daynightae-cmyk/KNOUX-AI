package com.knoux.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.knoux.ai.data.CleanupHistory
import com.knoux.ai.data.OperationStatus
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.unit.sp
import com.knoux.ai.data.KnouxData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The master surfaces: Smart Device Hub, Device Tools, Reports & Insights.
 *
 * Every figure here is either a stored measurement from a pass the customer
 * actually ran, or an honest "not measured yet". No sample numbers, no
 * estimates, no design-preview badge.
 */

// ---------------------------------------------------------------------------
// Shared master primitives
// ---------------------------------------------------------------------------

/** Two-tone title used by every master screen (white lead, signature accent). */
@Composable
internal fun MasterTitle(lead: Copy, accent: Copy, subtitle: Copy, tag: String) {
    // Reading-start alignment responds to RTL; the rule, title and accent follow
    // the reference header instead of a promotional headline.
    NxHeader(lead, subtitle, accent = accent, tag = tag)
}

/** The measured-state chip a real service shows under its title. */
@Composable
internal fun ServiceStatePill(label: Copy) {
    Box(Modifier.fillMaxWidth().padding(top = 6.dp), contentAlignment = Alignment.Center) {
        StatePill(label, KnouxColors.accent, tag = "service-state")
    }
}

/**
 * The one fixed action bar of a real service: a single primary action and one
 * secondary, so the decision always sits in the same place.
 */
@Composable
internal fun MasterActions(first: Action, second: Action, onFirst: () -> Unit, onSecond: () -> Unit) {
    BottomActionBar(
        primary = second,
        secondary = first,
        enabled = true,
        onPrimary = onSecond,
        onSecondary = onFirst,
        secondaryTag = "review-action",
    )
}

/** A quiet explanatory strip. Replaces the old "preview data" note. */@Composable
internal fun StateNote(text: Copy, modifier: Modifier = Modifier) {
    SurfaceCard(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Glyph(Mark.SHIELD, Modifier.size(16.dp), KnouxColors.accent)
            Label(text, 12, KnouxColors.textSecondary, modifier = Modifier.weight(1f))
        }
    }
}

/** Big rounded card used for the few main cards of a master screen. */@Composable
internal fun MasterCard(
    modifier: Modifier = Modifier, radius: Int = 22, tint: Color? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    NxCard(modifier, radius = radius, glow = tint ?: Nx.violet, padding = 12.dp, content = content)
}

/** Service tile: the visual key the eye uses to recognise a service. */
@Composable
internal fun MasterIcon(mark: Mark, tint: Color, diameter: Int = 44) {
    NxTile(mark, tint, size = diameter, glyph = (diameter * .54f).toInt().coerceAtLeast(14))
}

// ---------------------------------------------------------------------------
// 2 - Smart Clean (Clean)
// ---------------------------------------------------------------------------

// PC01: the screen is backed by measured device data (see SmartCleanReal.kt).
@Composable
internal fun SmartCleanScreen(open: (Page) -> Unit) = RealSmartCleanScreen(open)

// ---------------------------------------------------------------------------
// 1 - Smart Device Hub (Home)
// ---------------------------------------------------------------------------

/** One measured row of the device summary. `null` means "no pass has measured it". */
private data class HubMeasure(
    val label: Copy, val mark: Mark, val tint: Color, val value: Copy, val measured: Boolean,
)

/** One home service card. `valueIndex` points at the measured summary row it reuses. */
private data class HubCardSpec(
    val title: Copy, val body: Copy, val mark: Mark, val tint: Color,
    val tag: String, val page: Page, val valueIndex: Int? = null, val action: Copy? = null,
)

private val hubMeasureLabels = listOf(
    Triple(Copy(R.string.home_clean_potential), Mark.BROOM, KnouxColors.accent),
    Triple(Copy(R.string.home_residue), Mark.LAYERS, KnouxColors.primary),
    Triple(Copy(R.string.home_duplicates), Mark.COPY, KnouxColors.primary),
    Triple(Copy(R.string.home_privacy), Mark.SHIELD, KnouxColors.warning),
)

@Composable
private fun readHubMeasures(): List<HubMeasure> {
    var clean by remember { mutableStateOf<HubMeasure?>(null) }
    var residue by remember { mutableStateOf<HubMeasure?>(null) }
    var duplicates by remember { mutableStateOf<HubMeasure?>(null) }
    var privacy by remember { mutableStateOf<HubMeasure?>(null) }

    LaunchedEffect(Unit) {
        val cleanScan = withContext(Dispatchers.IO) {
            runCatching { KnouxData.smartClean.latestScan() }.getOrNull()
        }
        val residueScan = withContext(Dispatchers.IO) {
            runCatching { KnouxData.residue.latest() }.getOrNull()
        }
        val duplicateScan = withContext(Dispatchers.IO) {
            runCatching { KnouxData.duplicates.latest() }.getOrNull()
        }
        val privacyScan = withContext(Dispatchers.IO) {
            runCatching { KnouxData.privacy.latest() }.getOrNull()
        }
        clean = cleanScan?.let {
            HubMeasure(
                Copy(R.string.home_clean_potential), Mark.BROOM, KnouxColors.accent,
                Copy.runtime(KnouxFormat.bytes(it.candidates.sumOf { candidate -> candidate.bytes })), true,
            )
        }
        residue = residueScan?.let {
            HubMeasure(
                Copy(R.string.home_residue), Mark.LAYERS, KnouxColors.primary,
                Copy.runtime(KnouxFormat.bytes(it.bytes)), true,
            )
        }
        duplicates = duplicateScan?.let {
            HubMeasure(
                Copy(R.string.home_duplicates), Mark.COPY, KnouxColors.primary,
                Copy.runtime(KnouxFormat.bytes(it.redundantBytes)), true,
            )
        }
        privacy = privacyScan?.let {
            HubMeasure(
                Copy(R.string.home_privacy), Mark.SHIELD, KnouxColors.warning,
                Copy.args(R.string.home_privacy_value, it.attention.size, it.rows.size), true,
            )
        }
    }
    return listOf(clean, residue, duplicates, privacy).mapIndexed { index, measure ->
        val (label, mark, tint) = hubMeasureLabels[index]
        measure ?: HubMeasure(label, mark, tint, Copy.runtime("—"), false)
    }
}

@Composable
internal fun SmartHubScreen(open: (Page) -> Unit) {
    val measures = readHubMeasures()
    val cards = listOf(
        HubCardSpec(Copy(R.string.k_smart_clean), Copy(R.string.home_card_clean_body),
            Mark.TRASH, KnouxColors.primary, "open-clean-center", Page.CLEAN, 0, Copy(R.string.k_review)),
        HubCardSpec(Copy(R.string.k_duplicate_files), Copy(R.string.home_card_duplicates_body),
            Mark.COPY, KnouxColors.accent, "hub-duplicates", Page.DUPLICATES, 2, Copy(R.string.k_review)),
        HubCardSpec(Copy(R.string.k_app_residue_cleanup), Copy(R.string.home_card_residue_body),
            Mark.LAYERS, KnouxColors.primary, "hub-residue", Page.RESIDUE, 1, Copy(R.string.k_review)),
        HubCardSpec(Copy(R.string.k_storage_breakdown), Copy(R.string.home_card_storage_body),
            Mark.DISK, KnouxColors.accent, "hub-storage-analyzer", Page.STORAGE_BREAKDOWN, action = Copy(R.string.k_open)),
        HubCardSpec(Copy(R.string.k_privacy_sweep), Copy(R.string.home_card_privacy_body),
            Mark.SHIELD, KnouxColors.warning, "hub-privacy-sweep", Page.PRIVACY_INSPECTOR, 3, Copy(R.string.k_review)),
        HubCardSpec(Copy(R.string.k_ai_optimization), Copy(R.string.home_card_ai_body),
            Mark.CHIP, KnouxColors.accent, "hub-ai-optimization", Page.AI_RECOMMENDATIONS, action = Copy(R.string.k_optimize)),
        HubCardSpec(Copy(R.string.k_app_manager), Copy(R.string.home_card_apps_body),
            Mark.GROUP, KnouxColors.primary, "hub-app-manager", Page.APPS_REVIEW, action = Copy(R.string.k_open)),
        HubCardSpec(Copy(R.string.k_reports_insights), Copy(R.string.home_card_reports_body),
            Mark.BARS, KnouxColors.accent, "hub-reports", Page.REPORTS, action = Copy(R.string.k_open)),
    )
    Column(
        Modifier.fillMaxSize().background(NativePalette.ink).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MasterTitle(Copy(R.string.k_smart), Copy(R.string.k_device_hub),
                Copy(R.string.home_subtitle), "screen-home")
            NativeHomeSummary(measures)
            NativeScanAction("start-scan") { open(Page.SCAN) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                NativeHubSegment(Copy(R.string.k_smart_clean), Modifier.weight(1f), true) { open(Page.CLEAN) }
                NativeHubSegment(Copy(R.string.k_device), Modifier.weight(1f), false) { open(Page.TOOLS) }
                NativeHubSegment(Copy(R.string.k_reports_2), Modifier.weight(1f), false) { open(Page.REPORTS) }
            }
            val singleColumn = LocalRoomy.current || LocalDensity.current.fontScale > 1.24f
            if (singleColumn) {
                cards.forEach { spec -> HubCardFor(spec, measures, open) }
            } else {
                cards.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { spec ->
                            Box(Modifier.weight(1f)) { HubCardFor(spec, measures, open) }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickAction(Modifier.weight(1f), Copy(R.string.quick_clean), Mark.BROOM,
                    NativePalette.blue, "quick-clean") { open(Page.CLEAN) }
                QuickAction(Modifier.weight(1f), Copy(R.string.k_save_battery), Mark.BATTERY,
                    NativePalette.cyan, "quick-battery") { open(Page.BATTERY_DIAGNOSTICS) }
                QuickAction(Modifier.weight(1f), Copy(R.string.k_secure_device), Mark.SHIELD,
                    NativePalette.blue, "quick-privacy") { open(Page.PRIVACY_INSPECTOR) }
            }
            StateNote(Copy(R.string.home_measured_source))
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Real main reading: the measured cleanable volume, then two live observations. */
@Composable
private fun NativeHomeSummary(measures: List<HubMeasure>) {
    val leading = measures.first()
    NxCard(radius = 20, glow = Nx.violet, glow2 = Nx.cyan) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NxTile(Mark.RADAR, Nx.violet, size = 54, glyph = 26)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Label(Copy(R.string.home_clean_potential), 12, Nx.muted, maxLines = 2)
                if (leading.measured) {
                    Label(leading.value.localized(), 28, Nx.cyan, FontWeight.Bold, maxLines = 1)
                } else {
                    Unmeasured(20)
                }
            }
            NxBadge(
                if (leading.measured) Copy(R.string.state_success) else Copy(R.string.state_not_measured),
                if (leading.measured) Nx.green else Nx.faint,
            )
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(1.dp)
            .background(Brush.horizontalGradient(listOf(Color.Transparent, Nx.borderLit.copy(alpha = .55f), Color.Transparent))))
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            measures.drop(1).take(2).forEach { metric ->
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                        .background(Nx.inkDeep.copy(alpha = .55f))
                        .border(1.dp, Nx.border.copy(alpha = .75f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Glyph(metric.mark, Modifier.size(15.dp), metric.tint)
                        Label(metric.label, 11, Nx.muted, maxLines = 2)
                    }
                    if (metric.measured) {
                        Label(metric.value, 15, Nx.text, FontWeight.Bold, maxLines = 1)
                    } else Unmeasured(13)
                }
            }
        }
    }
}

/** Real Deep Scan route; no ornamental inert button. */
@Composable
private fun NativeScanAction(tag: String, onClick: () -> Unit) {
    NxPrimary(Copy(R.string.k_start_deep_scan), Modifier.fillMaxWidth(), Mark.RADAR, tag = tag, onClick = onClick)
}

@Composable
private fun NativeHubSegment(title: Copy, modifier: Modifier, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(21.dp)
    Box(
        modifier.heightIn(min = 48.dp).clip(shape)
            .background(
                if (selected) Brush.horizontalGradient(listOf(Nx.blueDeep, Nx.blue, Nx.cyan))
                else Brush.verticalGradient(listOf(Nx.cardTop, Nx.cardBottom)),
            )
            .border(
                1.dp,
                if (selected) Color.White.copy(alpha = .42f) else Nx.border,
                shape,
            )
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Label(title, 12, if (selected) Color.White else Nx.muted,
            FontWeight.Bold, align = TextAlign.Center, maxLines = 1)
    }
}

/**
 * The three figures the home can actually measure, in the compact instrument
 * strip of the approved reference. A dash means the pass has not run.
 */
@Composable
private fun HubMeasureStrip(measures: List<HubMeasure>) {
    SurfaceCard {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            measures.take(3).forEachIndexed { index, measure ->
                if (index > 0) {
                    Box(
                        Modifier.width(1.dp).height(52.dp).background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, KnouxColors.border, Color.Transparent),
                            )
                        )
                    )
                }
                val world = worldOf(measure.mark)
                Column(
                    Modifier.weight(1f).padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    WorldCrest(world, 28, alpha = if (measure.measured) 1f else .5f)
                    if (measure.measured) {
                        Label(
                            measure.value.localized(), 17,
                            KnouxColors.textPrimary, FontWeight.ExtraBold,
                            align = TextAlign.Center, maxLines = 1,
                        )
                    } else {
                        // A dash at display weight reads as a bar. Keep the same
                        // word, drop the weight, so "unknown" stays quiet.
                        Unmeasured(15, align = TextAlign.Center)
                    }
                    Label(
                        measure.label.localized(), 11, KnouxColors.textSecondary,
                        align = TextAlign.Center, maxLines = 2,
                    )
                }
            }
        }
    }
}

@Composable
private fun HubCardFor(spec: HubCardSpec, measures: List<HubMeasure>, open: (Page) -> Unit) {
    val measure = spec.valueIndex?.let { measures[it] }
    HubCard(
        title = spec.title,
        mark = spec.mark,
        tint = spec.tint,
        tag = spec.tag,
        value = measure?.takeIf { it.measured }?.value,
        action = spec.action,
        onClick = { open(spec.page) },
    )
}

@Composable
private fun QuickAction(
    modifier: Modifier = Modifier, title: Copy, mark: Mark, tint: Color, tag: String, onClick: () -> Unit
) {
    Column(
        modifier.heightIn(min = TOUCH_TARGET).clickable(role = Role.Button, onClick = onClick).testTag(tag)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Glyph(mark, Modifier.size(22.dp), tint)
        Label(title, 12, KnouxColors.textPrimary, FontWeight.SemiBold, align = TextAlign.Center, maxLines = 2)
    }
}

// ---------------------------------------------------------------------------
// 3 - Device Tools
// ---------------------------------------------------------------------------

@Composable
internal fun DeviceToolsScreen(open: (Page) -> Unit) {
    val motion = LocalPreviewMotion.current
    var showPro by remember { mutableStateOf(true) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        MasterTitle(Copy(R.string.k_device), Copy(R.string.k_tools_2), Copy(R.string.tools_subtitle), "screen-tools")
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ServiceRow(Copy(R.string.k_storage_breakdown), Copy(R.string.tools_storage_body),
                Mark.DISK, "open-storage-breakdown", "open-storage") { open(Page.STORAGE_BREAKDOWN) }
            ServiceRow(Copy(R.string.k_app_manager), Copy(R.string.tools_apps_body),
                Mark.GROUP, "open-app-manager", "open-app-manager") { open(Page.APPS_REVIEW) }
            ServiceRow(Copy(R.string.k_privacy_sweep), Copy(R.string.tools_privacy_body),
                Mark.SHIELD, "open-privacy", "open-privacy") { open(Page.PRIVACY_INSPECTOR) }
            ServiceRow(Copy(R.string.k_battery_diagnostics), Copy(R.string.tools_battery_body),
                Mark.BATTERY, "open-battery", "open-battery") { open(Page.BATTERY_DIAGNOSTICS) }
            ServiceRow(Copy(R.string.k_ai_optimization), Copy(R.string.tools_ai_body),
                Mark.CHIP, "open-ai-optimization", "open-ai-optimization") { open(Page.AI_RECOMMENDATIONS) }

            if (showPro) ProLine(onDismiss = { showPro = false })
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun ServiceRow(
    title: Copy, copy: Copy, mark: Mark, tag: String, clickTag: String, onClick: () -> Unit
) {
    val world = worldOf(mark)
    SurfaceCard(
        Modifier.heightIn(min = TOUCH_TARGET).testTag(clickTag)
            .clickable(role = Role.Button, onClick = onClick)
            .drawBehind {
                val width = size.width
                val height = size.height
                drawRect(
                    Brush.linearGradient(
                        listOf(world.primary.copy(alpha = .16f), Color.Transparent),
                        start = Offset(0f, 0f), end = Offset(width * .8f, height),
                    )
                )
                val railX = if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl) width - 2.dp.toPx() else 0f
                drawRect(
                    Brush.verticalGradient(
                        listOf(world.primary, world.secondary.copy(alpha = .5f), Color.Transparent),
                    ),
                    topLeft = Offset(railX, height * .18f),
                    size = Size(2.dp.toPx(), height * .64f),
                )
            }
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            NxTile(mark, world.primary, size = 44, glyph = 22)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Label(title, 14, KnouxColors.textPrimary, FontWeight.SemiBold, modifier = Modifier.testTag(tag))
                Label(copy, 12, KnouxColors.textSecondary, maxLines = 2)
            }
            Glyph(Mark.NEXT, Modifier.size(16.dp).rotate(if (LocalArabic.current) 180f else 0f),
                world.primary.copy(alpha = .8f))
        }
    }
}

/** One dismissible Pro line at the bottom of the content, not on every screen. */
@Composable
private fun ProLine(onDismiss: () -> Unit) {
    SurfaceCard(variant = true) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Glyph(Mark.CROWN, Modifier.size(16.dp), KnouxColors.pro)
            Label(Copy(R.string.pro_line), 12, KnouxColors.textSecondary, modifier = Modifier.weight(1f))
            Box(
                Modifier.size(TOUCH_TARGET).testTag("dismiss-pro")
                    .iconLabel(KnouxStrings.get(R.string.action_close, LocalArabic.current))
                    .clickable(role = Role.Button, onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) { Glyph(Mark.CHECK, Modifier.size(14.dp), KnouxColors.textSecondary) }
        }
    }
}

// ---------------------------------------------------------------------------
// 4 - Reports & Insights
// ---------------------------------------------------------------------------

@Composable
internal fun ReportsScreen(open: (Page) -> Unit) {
    val arabic = LocalArabic.current
    var measured by remember { mutableStateOf<Copy?>(null) }
    // This flow reads real Room operations, never design fixture statistics.
    val history by remember { KnouxData.smartClean.observeHistory() }
        .collectAsState(initial = emptyList())
    LaunchedEffect(Unit) {
        measured = withContext(Dispatchers.IO) {
            runCatching { KnouxData.smartClean.latestScan() }.getOrNull()
        }?.let {
            Copy.runtime(KnouxFormat.bytes(it.candidates.sumOf { c -> c.bytes }))
        }
    }
    val completed = history.filter {
        (it.status == OperationStatus.SUCCEEDED || it.status == OperationStatus.PARTIAL) && it.bytes > 0L
    }
    Column(
        Modifier.fillMaxSize().background(NativePalette.ink).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MasterTitle(Copy(R.string.k_reports_2), Copy(R.string.k_insights),
                Copy(R.string.reports_subtitle), "screen-reports")
            NxCard(radius = 20) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NxTile(Mark.BARS, Nx.violet, size = 50, glyph = 24)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Label(Copy(R.string.reports_last_clean_title), 12, Nx.muted, maxLines = 2)
                        if (measured != null) {
                            Label(measured!!, 24, Nx.cyan, FontWeight.Bold)
                        } else {
                            Unmeasured(18)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    NxBadge(Copy(R.string.state_success), Nx.green)
                    Label(completed.size.toString(), 13, Nx.text, FontWeight.Bold)
                    Label(Copy(R.string.reports_chart_title), 12, Nx.muted, maxLines = 2)
                }
            }
            if (completed.isEmpty()) {
                ReportsEmptyPanel { open(Page.SCAN) }
            } else {
                CleanupHistoryChart(history, arabic)
            }
            StateNote(Copy(R.string.reports_honesty))
            Label(Copy(R.string.reports_chart_note), 12, NativePalette.muted)
            Spacer(Modifier.height(18.dp))
        }
    }
}

/**
 * No pass has ever been recorded. Three faint, unlabelled bars hold the shape of
 * the chart without inventing a single value, and the one action starts a real
 * measured pass.
 */
@Composable
private fun ReportsEmptyPanel(onScan: () -> Unit) {
    SurfaceCard {
        Column(
            Modifier.fillMaxWidth().padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // An empty chart stays empty. Outlined sample bars can be mistaken
            // for observations, so no graph is painted until Room has records.
            Glyph(Mark.BARS, Modifier.size(32.dp), NativePalette.muted)
            Label(Copy(R.string.reports_no_passes_title), 15, NativePalette.text,
                FontWeight.Bold, align = TextAlign.Center)
            Label(Copy(R.string.reports_empty_body), 12, NativePalette.muted,
                align = TextAlign.Center)
            NativeScanAction("reports-start-scan", onScan)
        }
    }
}

/**
 * Four bars at most, three grid lines and a value above every bar. The time
 * axis follows the reading direction, so an Arabic chart starts on the right;
 * the measured numbers stay isolated and never mirror.
 */
@Composable
private fun CleanupHistoryChart(history: List<CleanupHistory>, arabic: Boolean) {
    val operations = history
        .filter { it.status == OperationStatus.SUCCEEDED || it.status == OperationStatus.PARTIAL }
        .filter { it.bytes > 0L }
        .take(7)
        .reversed()
    if (operations.isEmpty()) {
        StateBlock(UiState.EMPTY, Copy(R.string.reports_chart_empty))
        return
    }
    val maxBytes = operations.maxOf { it.bytes }.coerceAtLeast(1L)
    val formatter = remember(arabic) { DateFormat.getDateInstance(DateFormat.SHORT, Locale.US) }
    SurfaceCard {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Label(Copy(R.string.reports_chart_title), 15, NativePalette.text, FontWeight.Bold)
            androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(220.dp)) {
                val left = 16.dp.toPx()
                val right = size.width - 16.dp.toPx()
                val top = 22.dp.toPx()
                val bottom = size.height - 22.dp.toPx()
                val chartHeight = bottom - top
                repeat(4) { idx ->
                    val y = top + chartHeight * idx / 3f
                    drawLine(NativePalette.border.copy(alpha = .85f),
                        Offset(left, y), Offset(right, y), 1.dp.toPx())
                }
                val points = operations.mapIndexed { index, item ->
                    val x = if (operations.size == 1) (left + right) / 2f
                        else left + (right - left) * index / (operations.size - 1).toFloat()
                    val y = bottom - (item.bytes.toDouble() / maxBytes).toFloat()
                        .coerceIn(0f, 1f) * chartHeight
                    Offset(x, y)
                }
                if (points.size > 1) {
                    val area = androidx.compose.ui.graphics.Path().apply {
                        moveTo(points.first().x, bottom)
                        points.forEach { lineTo(it.x, it.y) }
                        lineTo(points.last().x, bottom)
                        close()
                    }
                    drawPath(area, Brush.verticalGradient(
                        listOf(NativePalette.blue.copy(alpha = .33f),
                            NativePalette.blue.copy(alpha = .02f)),
                        startY = top, endY = bottom))
                    val line = androidx.compose.ui.graphics.Path().apply {
                        moveTo(points.first().x, points.first().y)
                        points.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(line, NativePalette.cyan,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 3.dp.toPx()))
                }
                points.forEach { point ->
                    drawCircle(NativePalette.blue, 6.dp.toPx(), point)
                    drawCircle(NativePalette.text, 2.5.dp.toPx(), point)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                operations.forEach { operation ->
                    Column(Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Label(KnouxFormat.bytes(operation.bytes), 11, NativePalette.cyan,
                            FontWeight.Bold, maxLines = 1)
                        Label(formatter.format(Date(operation.completedAtMillis)), 11,
                            NativePalette.muted, maxLines = 1)
                    }
                }
            }
        }
    }
}

