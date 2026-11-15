package com.knoux.ai

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.unit.sp
import com.knoux.ai.data.CleanupHistory
import com.knoux.ai.data.KnouxData
import java.text.DateFormat
import java.util.Date
import java.util.Locale
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
    // Native header: two lines of plain type on the measured 20dp reading edge,
    // the same edge the hub and every native body use, so a title never sits
    // against the glass or out of line with the cards under it. The gradient
    // lead and the signature flourish stay on the approved cinematic surfaces;
    // a utility screen states its name and gets on with the job.
    if (LocalNativeApp.current) {
        NativeScreenTitle(
            Copy.runtime("${lead.localized()} ${accent.localized()}"),
            subtitle,
            tag,
            Modifier.fillMaxWidth().padding(start = NativeSpace.edge, top = 8.dp, end = NativeSpace.edge),
        )
        return
    }
    val roomy = LocalRoomy.current
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val styled = buildAnnotatedString {
            withStyle(SpanStyle(color = KnouxColors.textPrimary)) { append(lead.localized()) }
            withStyle(
                SpanStyle(
                    brush = Brush.horizontalGradient(
                        listOf(Brand.magenta, Brand.violet, Brand.cyan),
                    ),
                )
            ) {
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
                letterSpacing = (-0.2).sp,
            ),
        )
        // The house flourish: a tapered signature line under every title.
        Box(
            Modifier.padding(top = 6.dp).width(if (roomy) 38.dp else 52.dp).height(2.dp)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(Brand.signature)),
        )
        Label(
            subtitle, 13, KnouxColors.textSecondary,
            modifier = Modifier.fillMaxWidth().padding(top = 5.dp), align = TextAlign.Center,
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
 * The one fixed action pair of a real service.
 *
 * Primary and secondary share a single row: the decision still has one obvious
 * primary, but it no longer stacks two 48dp bars plus a note into a 170dp block
 * that squeezed the measured list on a compact phone.
 */
@Composable
internal fun MasterActions(first: Action, second: Action, onFirst: () -> Unit, onSecond: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SecondaryAction(
            action = first, onClick = onFirst, tag = "review-action",
            modifier = Modifier.weight(1f),
        )
        PrimaryAction(
            action = second, onClick = onSecond, tag = "primary-action",
            modifier = Modifier.weight(1.45f),
        )
    }
}

/**
 * A quiet explanatory strip. Replaces the old "preview data" note.
 *
 * Every caller (hub, reports, Smart Clean status) renders inside the native
 * shell, so it uses the native palette directly.
 */
@Composable
internal fun StateNote(text: Copy, modifier: Modifier = Modifier, bare: Boolean = false) {
    if (bare) {
        // A pinned status under a fixed action pair must not add a third card to
        // the bottom of the screen. One glyph and one muted line say the same.
        Row(
            modifier.fillMaxWidth().padding(start = 2.dp, end = 2.dp, top = 8.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Glyph(Mark.SHIELD, Modifier.size(14.dp), NativePalette.cyan.copy(alpha = .75f))
            Label(text, 11, NativePalette.muted, modifier = Modifier.weight(1f), maxLines = 2)
        }
        return
    }
    SurfaceCard(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Glyph(Mark.SHIELD, Modifier.size(16.dp), NativePalette.cyan)
            Label(text, 12, NativePalette.muted, modifier = Modifier.weight(1f))
        }
    }
}

/**
 * Big rounded card used for the few main cards of a master screen.
 *
 * Inside the utility shell it is the same quiet named surface the hub uses, so a
 * service screen and the home grid cannot drift into two different materials.
 * The cinematic glass is untouched on Splash, Onboarding and authentication.
 */
@Composable
internal fun MasterCard(
    modifier: Modifier = Modifier, radius: Int = 22, tint: Color? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    if (LocalNativeApp.current) {
        val tone = when (tint) {
            null -> null
            Pink, KnouxColors.primary -> NativePalette.blue
            Cyan, KnouxColors.accent -> NativePalette.cyan
            else -> tint
        }
        SurfaceCard(modifier, accent = tone, radius = radius.dp) {
            Column(Modifier.fillMaxWidth().padding(12.dp), content = content)
        }
        return
    }
    Box(modifier.clip(RoundedCornerShape(radius.dp)).glass(radius, tint = tint)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), content = content)
    }
}

/** Circular service icon with one soft bloom, or a flat plate on the native shell. */
@Composable
internal fun MasterIcon(mark: Mark, tint: Color, diameter: Int = 44) {
    if (LocalNativeApp.current) {
        IconBadge(mark, tint, diameter = diameter)
        return
    }
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

/**
 * The hub, rebuilt to the measured Figma geometry.
 *
 * The cinematic hub that used to sit behind a shell flag here was unreachable:
 * the app scaffold always runs the native shell, and the splash, onboarding and
 * authentication surfaces that keep the cinematic identity never render the
 * hub. It was removed rather than carried as dead code; Git history keeps it.
 */
@Composable
internal fun SmartHubScreen(open: (Page) -> Unit) = NativeHubScreen(readHubMeasures(), open)

/** The eight real services the hub offers, in the order the approved hub shows them. */
private fun hubCardSpecs(): List<HubCardSpec> = listOf(
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

/** Which segment owns which service, keyed by the tag the tests hold on to. */
private val hubSegmentOfTag = mapOf(
    "open-clean-center" to HubSegment.CLEAN,
    "hub-duplicates" to HubSegment.CLEAN,
    "hub-residue" to HubSegment.CLEAN,
    "hub-storage-analyzer" to HubSegment.MANAGE,
    "hub-app-manager" to HubSegment.MANAGE,
    "hub-reports" to HubSegment.MANAGE,
    "hub-privacy-sweep" to HubSegment.PROTECT,
    "hub-ai-optimization" to HubSegment.PROTECT,
)

/**
 * The native hub: Figma 2:2 / 2010:2 measured top to bottom.
 *
 * Header type, a 108dp storage overview, one 48dp scan action, the 36dp segment
 * strip, then the service grid at 154x92 with a 12dp gutter, recent activity and
 * quick actions. Selecting a segment promotes its group; it never removes a
 * service from the screen, so everything stays one tap and one scroll away.
 */
@Composable
private fun NativeHubScreen(measures: List<HubMeasure>, open: (Page) -> Unit) {
    val footprint = readStorageFootprint()
    // Real Room journal only, the same source the reports chart reads.
    val history by remember { KnouxData.smartClean.observeHistory() }
        .collectAsState(initial = emptyList())
    var selectedName by rememberSaveable { mutableStateOf(HubSegment.CLEAN.name) }
    val selected = runCatching { HubSegment.valueOf(selectedName) }.getOrDefault(HubSegment.CLEAN)
    val specs = hubCardSpecs()
    val ordered = HubSegment.values().sortedBy { if (it == selected) 0 else 1 }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = NativeSpace.edge),
    ) {
        // Title and instrument share one band: the wordmark-side text keeps the
        // reading edge, the drawn device sits on the open side of the field.
        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            NativeScreenTitle(
                Copy(R.string.home_title), Copy(R.string.home_subtitle), "screen-home",
                Modifier.weight(1f),
            )
            DeviceHero(Modifier.padding(start = 10.dp).size(112.dp).enterRise(0))
        }

        NativeStorageOverview(
            footprint,
            Modifier.fillMaxWidth().padding(top = NativeSpace.block).enterRise(0),
        )

        AccentButton(
            Action(Copy(R.string.k_start_deep_scan), Mark.RADAR),
            Modifier.fillMaxWidth().padding(top = NativeSpace.row).heightIn(min = 48.dp).enterRise(1),
            primary = true,
            tag = "start-scan",
            labelSize = 15,
        ) { open(Page.SCAN) }

        Box(Modifier.fillMaxWidth().padding(top = NativeSpace.row).enterRise(2)) {
            NativeHubSegments(selected) { picked -> selectedName = picked.name }
        }

        // One continuous grid, ordered by the chosen segment.
        //
        // The earlier build printed a group heading above each sub-grid. A group
        // with an odd tile count then left a full-height empty cell, and the
        // heading repeated the segment the customer had just tapped - three
        // stacked voids down the page. The segment bar already states the order,
        // so the grid simply flows: selected services first, the rest after.
        val columns = if (stackForFontScale()) 1 else 2
        val orderedTiles = ordered.flatMap { group -> specs.filter { hubSegmentOfTag[it.tag] == group } }
        Column(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(NativeSpace.gutter),
        ) {
            orderedTiles.chunked(columns).forEachIndexed { rowIndex, row ->
                // Intrinsic height: a tile that grows stretches its neighbour, so
                // a row never shows two different heights.
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min).enterRise(3 + rowIndex),
                    horizontalArrangement = Arrangement.spacedBy(NativeSpace.gutter),
                ) {
                    row.forEach { spec ->
                        NativeHubTile(
                            spec, measures, BrandTone.forTag(spec.tag), open,
                            Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                    // A lone tile in the last row keeps the measured width.
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        NativeRecentActivity(history, Modifier.fillMaxWidth().padding(top = NativeSpace.block).enterRise(6))

        SurfaceCard(Modifier.fillMaxWidth().padding(top = NativeSpace.row).enterRise(7)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Label(Copy(R.string.k_quick_actions), 14, NativePalette.text, FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    QuickAction(Modifier.weight(1f), Copy(R.string.quick_clean), Mark.BROOM,
                        BrandTone.violet, "quick-clean") { open(Page.CLEAN) }
                    QuickAction(Modifier.weight(1f), Copy(R.string.k_save_battery), Mark.BATTERY,
                        BrandTone.cyan, "quick-battery") { open(Page.BATTERY_DIAGNOSTICS) }
                    QuickAction(Modifier.weight(1f), Copy(R.string.k_secure_device), Mark.SHIELD,
                        BrandTone.cyan, "quick-privacy") { open(Page.PRIVACY_INSPECTOR) }
                }
            }
        }

        StateNote(Copy(R.string.home_measured_source), Modifier.padding(top = NativeSpace.row).enterRise(8))
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun NativeHubTile(
    spec: HubCardSpec, measures: List<HubMeasure>, tint: Color, open: (Page) -> Unit,
    modifier: Modifier = Modifier,
) {
    val measure = spec.valueIndex?.let { measures[it] }
    NativeServiceTile(
        title = spec.title, mark = spec.mark, tint = tint, tag = spec.tag,
        value = measure?.takeIf { it.measured }?.value, action = spec.action,
        modifier = modifier, onClick = { open(spec.page) },
    )
}

@Composable
private fun QuickAction(
    modifier: Modifier = Modifier, title: Copy, mark: Mark, tint: Color, tag: String, onClick: () -> Unit
) {
    Column(
        modifier.heightIn(min = TOUCH_TARGET).testTag(tag)
            .brandClickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ServiceIconPlate(mark, tint, size = 38.dp, iconSize = 19.dp)
        Label(title, 11, NativePalette.text, FontWeight.SemiBold, align = TextAlign.Center, maxLines = 2)
    }
}

// ---------------------------------------------------------------------------
// 3 - Device Tools
// ---------------------------------------------------------------------------

@Composable
internal fun DeviceToolsScreen(open: (Page) -> Unit) {
    var showPro by remember { mutableStateOf(true) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        MasterTitle(Copy(R.string.k_device), Copy(R.string.k_tools_2), Copy(R.string.tools_subtitle), "screen-tools")
        Column(
            Modifier.fillMaxWidth().padding(horizontal = NativeSpace.edge, vertical = NativeSpace.row),
            verticalArrangement = Arrangement.spacedBy(NativeSpace.tight),
        ) {
            ServiceRow(Copy(R.string.k_storage_breakdown), Copy(R.string.tools_storage_body),
                Mark.DISK, BrandTone.blue, "open-storage-breakdown", "open-storage") { open(Page.STORAGE_BREAKDOWN) }
            ServiceRow(Copy(R.string.k_app_manager), Copy(R.string.tools_apps_body),
                Mark.GROUP, BrandTone.blue, "open-app-manager", "open-app-manager") { open(Page.APPS_REVIEW) }
            ServiceRow(Copy(R.string.k_privacy_sweep), Copy(R.string.tools_privacy_body),
                Mark.SHIELD, BrandTone.cyan, "open-privacy", "open-privacy") { open(Page.PRIVACY_INSPECTOR) }
            ServiceRow(Copy(R.string.k_battery_diagnostics), Copy(R.string.tools_battery_body),
                Mark.BATTERY, BrandTone.cyan, "open-battery", "open-battery") { open(Page.BATTERY_DIAGNOSTICS) }
            ServiceRow(Copy(R.string.k_ai_optimization), Copy(R.string.tools_ai_body),
                Mark.CHIP, BrandTone.magenta, "open-ai-optimization", "open-ai-optimization") { open(Page.AI_RECOMMENDATIONS) }

            if (showPro) ProLine(onDismiss = { showPro = false })
            Spacer(Modifier.height(18.dp))
        }
    }
}

/**
 * One Device Tools entry: a native row with a single tinted icon plate, the
 * service name, one line on what it does and a direction chevron. The whole row
 * is the control, and its service tone appears only in the plate and one short
 * hairline along the top edge.
 */
@Composable
private fun ServiceRow(
    title: Copy, copy: Copy, mark: Mark, tint: Color, tag: String, clickTag: String, onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    SurfaceCard(
        Modifier.heightIn(min = TOUCH_TARGET).clip(shape).testTag(clickTag)
            .brandClickable(onClick = onClick),
        accent = tint,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ServiceIconPlate(mark, tint, size = 40.dp, iconSize = 20.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Label(title, 14, NativePalette.text, FontWeight.SemiBold, modifier = Modifier.testTag(tag))
                Label(copy, 12, NativePalette.muted, maxLines = 2)
            }
            Glyph(Mark.NEXT, Modifier.size(16.dp).rotate(if (LocalArabic.current) 180f else 0f),
                NativePalette.muted)
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
            Glyph(Mark.CROWN, Modifier.size(16.dp), NativePalette.amber)
            Label(Copy(R.string.pro_line), 12, NativePalette.muted, modifier = Modifier.weight(1f))
            Box(
                Modifier.size(TOUCH_TARGET).testTag("dismiss-pro")
                    .iconLabel(KnouxStrings.get(R.string.action_close, LocalArabic.current))
                    .clickable(role = Role.Button, onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) { Glyph(Mark.CHECK, Modifier.size(14.dp), NativePalette.muted) }
        }
    }
}

// ---------------------------------------------------------------------------
// 4 - Reports & Insights
// ---------------------------------------------------------------------------

@Composable
internal fun ReportsScreen(open: (Page) -> Unit) {
    // Real Room history only. No fixture, no synthetic trend. `null` means the
    // journal has not answered yet, which is not the same as "nothing recorded",
    // so no empty state can flash before the first read.
    val history by remember { KnouxData.smartClean.observeHistory() }
        .collectAsState<List<CleanupHistory>, List<CleanupHistory>?>(initial = null)
    var lastScan by remember { mutableStateOf<Copy?>(null) }
    var scanRead by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        lastScan = withContext(Dispatchers.IO) {
            runCatching { KnouxData.smartClean.latestScan() }.getOrNull()
        }?.let {
            Copy.args(R.string.reports_last_clean, KnouxFormat.bytes(it.candidates.sumOf { c -> c.bytes }))
        }
        scanRead = true
    }
    val records = history
    val completed = remember(records) { records.orEmpty().completedCleanups() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        MasterTitle(Copy(R.string.k_reports_2), Copy(R.string.k_insights), Copy(R.string.reports_subtitle), "screen-reports")
        Column(
            Modifier.fillMaxWidth().padding(horizontal = NativeSpace.edge, vertical = NativeSpace.row),
            verticalArrangement = Arrangement.spacedBy(NativeSpace.row),
        ) {
            val scan = lastScan
            when {
                records == null || !scanRead -> Unit
                // Nothing measured at all: one honest empty state, not a summary
                // of dashes stacked on top of an empty chart.
                scan == null && completed.isEmpty() -> ReportsEmptyPanel { open(Page.SCAN) }
                else -> {
                    ReportsSummary(scan)
                    if (completed.isNotEmpty()) {
                        CleanupHistoryChart(completed)
                        Label(Copy(R.string.reports_chart_note), 12, NativePalette.muted)
                    } else {
                        // A scan was measured but nothing was cleaned yet: the
                        // chart stays empty rather than borrowing the scan figure.
                        StateBlock(UiState.EMPTY, Copy(R.string.reports_chart_empty))
                    }
                }
            }
            StateNote(Copy(R.string.reports_honesty))
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * What the last measured scan found eligible to clean. With no finished scan in
 * Room the value is a dash, never a zero or an estimate. There is deliberately
 * no all-time cleanup count here: the journal flow is a bounded recent window,
 * so any total drawn from it would understate real history.
 */
@Composable
private fun ReportsSummary(lastScan: Copy?) {
    SurfaceCard(accent = NativePalette.cyan) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ServiceIconPlate(Mark.BROOM, NativePalette.cyan, size = 40.dp, iconSize = 20.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Label(Copy(R.string.reports_last_clean_title), 12, NativePalette.muted, FontWeight.SemiBold)
                if (lastScan != null) {
                    Label(lastScan, 18, NativePalette.cyan, FontWeight.Bold, modifier = Modifier.testTag("reports-last-scan"))
                } else {
                    Unmeasured(18, modifier = Modifier.testTag("reports-last-scan"))
                }
            }
        }
    }
}

/**
 * Nothing measured exists yet: no scan and no completed cleanup. One muted
 * glyph marks where the chart will live, but no bar is drawn, because even a
 * faint placeholder bar reads as a value. The one action starts a real pass.
 */
@Composable
private fun ReportsEmptyPanel(onScan: () -> Unit) {
    SurfaceCard(accent = NativePalette.violet) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NativeSpace.row),
        ) {
            ServiceIconPlate(Mark.BARS, NativePalette.violet, size = 52.dp, iconSize = 26.dp)
            Label(Copy(R.string.reports_no_passes_title), 15, NativePalette.text,
                FontWeight.SemiBold, align = TextAlign.Center)
            Label(Copy(R.string.reports_empty_body), 12, NativePalette.muted,
                align = TextAlign.Center)
            BrandHairline(Modifier.padding(horizontal = 18.dp))
            AccentButton(
                Action(Copy(R.string.k_start_deep_scan), Mark.RADAR),
                Modifier.fillMaxWidth(), primary = true, tag = "reports-start-scan",
            ) { onScan() }
        }
    }
}

/**
 * The latest completed cleanups, at most four, oldest first along the reading
 * direction (a Row mirrors under RTL, so an Arabic chart starts on the right).
 * A bar's height is that cleanup's measured bytes over the largest of the four
 * and nothing else: no slope, no interpolation, no trend. A tiny value keeps a
 * visible stub so "small" never reads as "nothing recorded". Figures and dates
 * stay Latin and LTR-isolated in both languages. Under large text the columns
 * become rows, so no value or date is ever clipped.
 */
@Composable
private fun CleanupHistoryChart(completed: List<CleanupHistory>) {
    val operations = completed.take(4).reversed()
    val maxBytes = operations.maxOf { it.bytes }.coerceAtLeast(1L)
    val formatter = remember { DateFormat.getDateInstance(DateFormat.SHORT, Locale.US) }
    val stacked = stackForFontScale()
    val plot = 88.dp
    // One finite reveal when the chart first appears; the bars never animate
    // again and never invent a value. Motion off means the final frame.
    val motion = LocalPreviewMotion.current
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val reveal by animateFloatAsState(
        if (!motion || entered) 1f else 0f,
        tween(520), label = "chart reveal",
    )
    SurfaceCard(accent = NativePalette.blue) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(NativeSpace.row)) {
            Label(Copy(R.string.reports_chart_title), 15, NativePalette.text, FontWeight.SemiBold)
            if (stacked) {
                operations.asReversed().forEach { operation ->
                    val ratio = (operation.bytes.toDouble() / maxBytes).toFloat().coerceIn(.04f, 1f)
                    Column(
                        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Label(formatter.format(Date(operation.completedAtMillis)), 12, NativePalette.muted)
                        Label(KnouxFormat.bytes(operation.bytes), 13, NativePalette.text, FontWeight.SemiBold)
                        Box(
                            Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50))
                                .background(NativePalette.raised)
                        ) {
                            Box(
                                Modifier.fillMaxWidth(ratio * reveal).fillMaxHeight().clip(RoundedCornerShape(50))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(NativePalette.blue, NativePalette.violet),
                                        )
                                    )
                            )
                        }
                    }
                }
            } else {
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(NativeSpace.tight),
                    ) {
                        operations.forEach { operation ->
                            val ratio = (operation.bytes.toDouble() / maxBytes).toFloat().coerceIn(0f, 1f)
                            val description = Copy.args(
                                R.string.reports_chart_bar,
                                KnouxFormat.bytes(operation.bytes),
                                formatter.format(Date(operation.completedAtMillis)),
                            ).localized()
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Label(KnouxFormat.bytes(operation.bytes), 11, NativePalette.text,
                                    FontWeight.SemiBold, maxLines = 1)
                                Spacer(Modifier.height(6.dp))
                                Box(
                                    Modifier.width(32.dp).height((plot * ratio).coerceAtLeast(6.dp))
                                        .graphicsLayer {
                                            scaleY = reveal
                                            transformOrigin = TransformOrigin(.5f, 1f)
                                        }
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(NativePalette.cyan, NativePalette.blue, NativePalette.violet),
                                            )
                                        )
                                        .semantics { contentDescription = description }
                                )
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(NativePalette.border))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NativeSpace.tight)) {
                    operations.forEach { operation ->
                        Label(
                            formatter.format(Date(operation.completedAtMillis)), 11, NativePalette.muted,
                            modifier = Modifier.weight(1f), align = TextAlign.Center, maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
