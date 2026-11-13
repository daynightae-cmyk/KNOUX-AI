package com.knoux.ai

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

/** Two-tone title used by every master screen (white lead, violet accent). */
@Composable
internal fun MasterTitle(lead: Copy, accent: Copy, subtitle: Copy, tag: String) {
    val roomy = LocalRoomy.current
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val styled = buildAnnotatedString {
            withStyle(SpanStyle(color = KnouxColors.textPrimary)) { append(lead.localized()) }
            withStyle(SpanStyle(color = KnouxColors.primary)) {
                append(" ")
                append(accent.localized())
            }
        }
        Text(
            styled,
            Modifier.fillMaxWidth().testTag(tag),
            style = TextStyle(
                fontSize = (if (roomy) 22 else 26).sp,
                lineHeight = (if (roomy) 28 else 33).sp,
                fontFamily = DisplayFace,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
        )
        Label(
            subtitle, 13, KnouxColors.textSecondary,
            modifier = Modifier.fillMaxWidth().padding(top = 3.dp), align = TextAlign.Center,
        )
    }
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

/** Big rounded glass card used for the few main cards of a master screen. */@Composable
internal fun MasterCard(
    modifier: Modifier = Modifier, radius: Int = 22, tint: Color? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Box(modifier.clip(RoundedCornerShape(radius.dp)).glass(radius, tint = tint)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), content = content)
    }
}

/** Circular service icon with one soft bloom. */
@Composable
internal fun MasterIcon(mark: Mark, tint: Color, diameter: Int = 44) {
    Box(
        Modifier.size(diameter.dp).clip(RoundedCornerShape(diameter.dp / 2))
            .background(Brush.radialGradient(listOf(tint.copy(alpha = .45f), KnouxColors.surfaceVariant)))
            .padding(diameter.dp * .22f),
        contentAlignment = Alignment.Center,
    ) {
        Glyph(mark, Modifier.fillMaxSize(), if (tint == KnouxColors.primary) KnouxColors.textPrimary else tint)
    }
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
private data class HubMeasure(val label: Copy, val mark: Mark, val tint: Color, val value: Copy)

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
                Copy.runtime(KnouxFormat.bytes(it.candidates.sumOf { candidate -> candidate.bytes })),
            )
        }
        residue = residueScan?.let {
            HubMeasure(
                Copy(R.string.home_residue), Mark.LAYERS, KnouxColors.primary,
                Copy.runtime(KnouxFormat.bytes(it.bytes)),
            )
        }
        duplicates = duplicateScan?.let {
            HubMeasure(
                Copy(R.string.home_duplicates), Mark.COPY, KnouxColors.primary,
                Copy.runtime(KnouxFormat.bytes(it.redundantBytes)),
            )
        }
        privacy = privacyScan?.let {
            HubMeasure(
                Copy(R.string.home_privacy), Mark.SHIELD, KnouxColors.warning,
                Copy.args(R.string.home_privacy_value, it.attention.size, it.rows.size),
            )
        }
    }
    return listOf(clean, residue, duplicates, privacy).map {
        it ?: HubMeasure(Copy(R.string.state_not_measured), Mark.CHECK, KnouxColors.textSecondary,
            Copy(R.string.state_not_measured))
    }
}

@Composable
internal fun SmartHubScreen(open: (Page) -> Unit) {
    val motion = LocalPreviewMotion.current
    val measures = readHubMeasures()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NeonHero(Mark.K, motion, Modifier.padding(top = 4.dp, bottom = 6.dp),
            heightOverride = if (LocalRoomy.current) 88.dp else 96.dp)
        MasterTitle(
            Copy(R.string.k_smart), Copy(R.string.k_device_hub),
            Copy(R.string.home_subtitle), "screen-home",
        )
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SurfaceCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Label(Copy(R.string.home_summary), 15, KnouxColors.textPrimary, FontWeight.Bold)
                    measures.forEach { measure -> MeasureRow(measure) }
                }
            }

            SurfaceCard {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Label(Copy(R.string.k_quick_actions), 15, KnouxColors.textPrimary, FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        QuickAction(Modifier.weight(1f), Copy(R.string.k_deep_scan_2), Mark.RADAR,
                            KnouxColors.accent, "start-scan") { open(Page.SCAN) }
                        QuickAction(Modifier.weight(1f), Copy(R.string.quick_clean), Mark.BROOM,
                            KnouxColors.primary, "quick-clean") { open(Page.CLEAN) }
                        QuickAction(Modifier.weight(1f), Copy(R.string.k_secure_device), Mark.SHIELD,
                            KnouxColors.primary, "quick-privacy") { open(Page.PRIVACY_INSPECTOR) }
                    }
                }
            }

            StateNote(Copy(R.string.home_measured_source))
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun MeasureRow(measure: HubMeasure) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(10.dp))
                .background(measure.tint.copy(alpha = .14f)),
            contentAlignment = Alignment.Center,
        ) { Glyph(measure.mark, Modifier.size(16.dp), measure.tint) }
        Spacer(Modifier.width(10.dp))
        Label(measure.label, 13, KnouxColors.textSecondary, modifier = Modifier.weight(1f))
        Label(measure.value, 14, KnouxColors.textPrimary, FontWeight.SemiBold)
    }
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
        NeonHero(Mark.K, motion, Modifier.padding(top = 4.dp), heightOverride = 88.dp)
        MasterTitle(Copy(R.string.k_device), Copy(R.string.k_tools_2), Copy(R.string.tools_subtitle), "screen-tools")
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ServiceRow(Copy(R.string.k_storage_breakdown), Copy(R.string.tools_storage_body),
                Mark.LAYERS, "open-storage-breakdown", "open-storage") { open(Page.STORAGE_BREAKDOWN) }
            ServiceRow(Copy(R.string.k_app_manager), Copy(R.string.tools_apps_body),
                Mark.GROUP, "open-app-manager", "open-app-manager") { open(Page.APPS_REVIEW) }
            ServiceRow(Copy(R.string.k_privacy_sweep), Copy(R.string.tools_privacy_body),
                Mark.SHIELD, "open-privacy", "open-privacy") { open(Page.PRIVACY_INSPECTOR) }
            ServiceRow(Copy(R.string.k_battery_diagnostics), Copy(R.string.tools_battery_body),
                Mark.BATTERY, "open-battery", "open-battery") { open(Page.BATTERY_DIAGNOSTICS) }
            ServiceRow(Copy(R.string.k_ai_optimization), Copy(R.string.tools_ai_body),
                Mark.CHIP, "open-ai-optimization", "open-ai-optimization") { open(Page.AI_RECOMMENDATIONS) }

            if (showPro) ProLine(onDismiss = { showPro = false })
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun ServiceRow(
    title: Copy, copy: Copy, mark: Mark, tag: String, clickTag: String, onClick: () -> Unit
) {
    SurfaceCard(
        Modifier.heightIn(min = TOUCH_TARGET).testTag(clickTag)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MasterIcon(mark, KnouxColors.accent, 38)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Label(title, 14, KnouxColors.textPrimary, FontWeight.SemiBold, modifier = Modifier.testTag(tag))
                Label(copy, 12, KnouxColors.textSecondary, maxLines = 2)
            }
            Glyph(Mark.NEXT, Modifier.size(16.dp), KnouxColors.textSecondary)
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
    val motion = LocalPreviewMotion.current
    val arabic = LocalArabic.current
    var measured by remember { mutableStateOf<Copy?>(null) }
    // Real Room history only. No fixture, no synthetic trend.
    val history by remember { KnouxData.smartClean.observeHistory() }
        .collectAsState(initial = emptyList())
    LaunchedEffect(Unit) {
        measured = withContext(Dispatchers.IO) {
            runCatching { KnouxData.smartClean.latestScan() }.getOrNull()
        }?.let {
            Copy.args(R.string.reports_last_clean, KnouxFormat.bytes(it.candidates.sumOf { c -> c.bytes }))
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        NeonHero(Mark.BARS, motion, Modifier.padding(top = 4.dp), heightOverride = 88.dp)
        MasterTitle(Copy(R.string.k_reports_2), Copy(R.string.k_insights), Copy(R.string.reports_subtitle), "screen-reports")
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val last = measured
            if (last == null) {
                StateBlock(UiState.EMPTY, Copy(R.string.reports_empty_body))
            } else {
                SurfaceCard {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Label(Copy(R.string.reports_last_clean_title), 15, KnouxColors.textPrimary, FontWeight.Bold)
                        Label(last, 14, KnouxColors.accent, FontWeight.SemiBold)
                    }
                }
            }
            CleanupHistoryChart(history, arabic)
            StateNote(Copy(R.string.reports_honesty))
            Label(Copy(R.string.reports_chart_note), 11, KnouxColors.textSecondary)
            Spacer(Modifier.height(6.dp))
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
        .take(4)
        .reversed()
    if (operations.isEmpty()) {
        StateBlock(UiState.EMPTY, Copy(R.string.reports_chart_empty))
        return
    }
    val maxBytes = operations.maxOf { it.bytes }.coerceAtLeast(1L)
    val formatter = remember(arabic) { DateFormat.getDateInstance(DateFormat.SHORT, Locale.US) }
    SurfaceCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Label(Copy(R.string.reports_chart_title), 15, KnouxColors.textPrimary, FontWeight.Bold)
            Box(Modifier.fillMaxWidth().heightIn(min = 112.dp)) {
                Column(Modifier.matchParentSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    repeat(3) {
                        Box(Modifier.fillMaxWidth().height(1.dp)
                            .background(KnouxColors.textSecondary.copy(alpha = .18f)))
                    }
                }
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 112.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    operations.forEach { operation ->
                        val barHeight = ((operation.bytes.toDouble() / maxBytes) * 78.0)
                            .coerceAtLeast(4.0).dp
                        val date = formatter.format(Date(operation.completedAtMillis))
                        val description = Copy.args(
                            R.string.reports_chart_bar,
                            KnouxFormat.bytes(operation.bytes), date,
                        ).localized()
                        Column(
                            Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            Label(KnouxFormat.bytes(operation.bytes), 11,
                                KnouxColors.textSecondary, maxLines = 1)
                            Spacer(Modifier.height(4.dp))
                            Box(
                                Modifier.fillMaxWidth().height(barHeight)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Brush.verticalGradient(
                                        listOf(KnouxColors.accent, KnouxColors.primary)))
                                    .semantics { contentDescription = description }
                            )
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                operations.forEach { operation ->
                    Label(
                        formatter.format(Date(operation.completedAtMillis)), 11,
                        KnouxColors.textSecondary,
                        modifier = Modifier.weight(1f), align = TextAlign.Center, maxLines = 1,
                    )
                }
            }
        }
    }
}
