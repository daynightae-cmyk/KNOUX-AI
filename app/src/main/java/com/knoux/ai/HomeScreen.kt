package com.knoux.ai

import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.knoux.ai.data.CleanupHistory
import com.knoux.ai.data.OperationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

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

// ---------------------------------------------------------------------------
// Native shell surfaces — Figma 2:2 / 2010:2 measured geometry
// ---------------------------------------------------------------------------

/**
 * The three segments of the hub's compact tab strip (Figma x20 y268, 320x36).
 *
 * A segment deliberately does not filter services away. Every service stays on
 * the screen and reachable by scrolling, because a utility hub that hides
 * functions behind a tab loses them; selecting a segment promotes its group to
 * the top of the grid instead.
 */
internal enum class HubSegment(val labelRes: Int, val tag: String) {
    CLEAN(R.string.k_clean_2, "home-segment-clean"),
    MANAGE(R.string.k_manage, "home-segment-manage"),
    PROTECT(R.string.home_segment_protect, "home-segment-protect"),
}

/** Device capacity as the filesystem actually reports it. Never an estimate. */
internal data class StorageFootprint(val usedBytes: Long, val totalBytes: Long) {
    val freeBytes: Long get() = (totalBytes - usedBytes).coerceAtLeast(0L)

    /** Whole percent used, or null when nothing usable was read. */
    val usedPercent: Int?
        get() = if (totalBytes > 0L) {
            ((usedBytes * 100.0) / totalBytes).roundToInt().coerceIn(0, 100)
        } else null
}

/**
 * Reads the real size of the shared data partition through [StatFs].
 *
 * No permission is required and nothing is written. If Android refuses to report
 * a usable size the result stays null and the card draws an unmeasured dial —
 * the hub never guesses a capacity to fill its own instrument.
 */
@Composable
internal fun readStorageFootprint(): StorageFootprint? {
    val context = LocalContext.current
    var footprint by remember(context) { mutableStateOf<StorageFootprint?>(null) }
    LaunchedEffect(context) {
        footprint = withContext(Dispatchers.IO) {
            runCatching {
                val stats = StatFs(Environment.getDataDirectory().path)
                val total = stats.totalBytes
                require(total > 0L) { "device capacity unreadable" }
                StorageFootprint(total - stats.availableBytes, total)
            }.getOrNull()
        }
    }
    return footprint
}

/**
 * The compact storage overview: Figma x20 y88, 320x108.
 *
 * The ring is the one measured percentage the device can give; the two figures
 * beside it are its operands, so the customer can check the arithmetic. Before a
 * reading exists the ring is drawn as an empty track and both figures are a dash.
 */
@Composable
internal fun NativeStorageOverview(footprint: StorageFootprint?, modifier: Modifier = Modifier) {
    val percent = footprint?.usedPercent
    SurfaceCard(modifier) {
        Row(
            Modifier.fillMaxWidth().height(108.dp).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                if (percent != null) {
                    ProgressDonut(
                        value = percent, diameter = 80, live = false,
                        displayValue = "$percent%",
                    )
                } else {
                    UnmeasuredDial()
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Label(Copy(R.string.home_storage_title), 13, NativePalette.muted, FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NativeFootprintStat(
                        Copy(R.string.home_storage_used),
                        footprint?.let { KnouxFormat.bytes(it.usedBytes) } ?: "—",
                        NativePalette.cyan,
                    )
                    NativeFootprintStat(
                        Copy(R.string.home_storage_free),
                        footprint?.let { KnouxFormat.bytes(it.freeBytes) } ?: "—",
                        NativePalette.text,
                    )
                }
                if (footprint == null) {
                    Label(Copy(R.string.home_storage_unreadable), 12, NativePalette.muted, maxLines = 2)
                }
            }
        }
    }
}

@Composable
private fun RowScope.NativeFootprintStat(label: Copy, value: String, tint: Color) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Label(label, 12, NativePalette.muted)
        Label(value, 15, tint, FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** An empty ring: the shape of a measurement that has not happened. */
@Composable
private fun UnmeasuredDial() {
    Canvas(Modifier.size(74.dp)) {
        drawCircle(
            color = NativePalette.border,
            radius = (minOf(size.width, size.height) - 6.dp.toPx()) / 2f,
            center = Offset(size.width / 2f, size.height / 2f),
            style = Stroke(width = 6.dp.toPx()),
        )
    }
    Label("—", 17, NativePalette.muted, FontWeight.Bold, align = TextAlign.Center)
}

/**
 * The segmented control, drawn 36dp tall as measured.
 *
 * The touch band around each label is still 48dp: the pill keeps the Figma
 * geometry while the finger keeps the platform minimum.
 */
@Composable
internal fun NativeHubSegments(selected: HubSegment, onSelect: (HubSegment) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(TOUCH_TARGET).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(NativeSpace.gutter),
    ) {
        HubSegment.values().forEach { segment ->
            val active = segment == selected
            val pill = RoundedCornerShape(10.dp)
            Box(
                Modifier.weight(1f).fillMaxHeight()
                    .testTag(segment.tag)
                    .selectable(selected = active, role = Role.Tab, onClick = { onSelect(segment) }),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.fillMaxWidth().height(36.dp).clip(pill)
                        .background(if (active) NativePalette.violet.copy(alpha = .22f) else NativePalette.card)
                        .border(
                            1.dp,
                            if (active) NativePalette.violet.copy(alpha = .60f) else NativePalette.border,
                            pill,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Label(
                        Copy(segment.labelRes), 13,
                        if (active) NativePalette.text else NativePalette.muted,
                        if (active) FontWeight.Bold else FontWeight.Normal,
                        align = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * One service tile: Figma 154x92 in a two-column grid with a 12dp gutter.
 *
 * Flat surface, one icon plate, the service name and its last measured value.
 * The whole tile is the control, so there is exactly one thing to tap. An
 * unmeasured service shows a dash rather than a number borrowed from a fixture.
 */
@Composable
internal fun NativeServiceTile(
    title: Copy, mark: Mark, tint: Color, tag: String,
    value: Copy?, action: Copy?, onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.fillMaxWidth().height(NativeSpace.tile).clip(shape)
            .background(NativePalette.card).border(1.dp, NativePalette.border, shape)
            .testTag(tag).clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val plate = RoundedCornerShape(8.dp)
            Box(
                Modifier.size(24.dp).clip(plate)
                    .background(tint.copy(alpha = .16f))
                    .border(1.dp, tint.copy(alpha = .34f), plate),
                contentAlignment = Alignment.Center,
            ) { Glyph(mark, Modifier.size(14.dp), tint) }
            Label(
                title, 13, NativePalette.text, FontWeight.SemiBold,
                modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Label(
                value ?: Copy.runtime("—"), 15,
                if (value != null) tint else NativePalette.muted, FontWeight.Bold,
                modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            action?.let { Label(it, 12, NativePalette.muted, maxLines = 1) }
        }
    }
}

/**
 * The small recent-activity block, Figma x20 y628.
 *
 * It lists real finished operations from the Room journal only. With no
 * operation recorded it says so; it never fills itself with sample entries.
 */
@Composable
internal fun NativeRecentActivity(history: List<CleanupHistory>, modifier: Modifier = Modifier) {
    val arabic = LocalArabic.current
    val operations = history
        .filter { it.status == OperationStatus.SUCCEEDED || it.status == OperationStatus.PARTIAL }
        .filter { it.bytes > 0L }
        .take(3)
    SurfaceCard(modifier) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Label(Copy(R.string.home_recent_title), 13, NativePalette.muted, FontWeight.SemiBold)
            if (operations.isEmpty()) {
                Label(Copy(R.string.k_nothing_measured_yet), 13, NativePalette.text)
            } else {
                val formatter = remember(arabic) {
                    DateFormat.getDateInstance(DateFormat.SHORT, if (arabic) Locale("ar", "AE") else Locale.US)
                }
                operations.forEach { operation ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(Modifier.size(5.dp).clip(RoundedCornerShape(50)).background(NativePalette.cyan))
                        Label(
                            Copy.args(
                                R.string.reports_chart_bar,
                                KnouxFormat.bytes(operation.bytes),
                                formatter.format(Date(operation.completedAtMillis)),
                            ),
                            12, NativePalette.text,
                            modifier = Modifier.weight(1f), maxLines = 2,
                        )
                    }
                }
            }
        }
    }
}
