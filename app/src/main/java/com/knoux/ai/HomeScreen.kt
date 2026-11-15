package com.knoux.ai

import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.Canvas
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
 * Every figure on it is read from the device or from the Room journal of a pass
 * the customer actually ran; anything not yet measured is drawn as a dash.
 */
@Composable
internal fun HomeScreen(open: (Page) -> Unit) {
    SmartHubScreen(open)
}

/**
 * Above this font scale the measured two-column geometry can no longer hold a
 * service name and its value, so tiles and figures stack into one column. This
 * is the same threshold the shell uses for its compact mode.
 */
private const val STACKING_FONT_SCALE = 1.24f

/** True when the customer's font scale needs single-column layouts. */
@Composable
internal fun stackForFontScale(): Boolean = LocalDensity.current.fontScale > STACKING_FONT_SCALE

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
 *
 * 108dp is the measured minimum, not a clamp: the unreadable note and large font
 * scales grow the card instead of clipping a measured figure.
 */
@Composable
internal fun NativeStorageOverview(footprint: StorageFootprint?, modifier: Modifier = Modifier) {
    val percent = footprint?.usedPercent
    val stacked = stackForFontScale()
    val used = footprint?.let { KnouxFormat.bytes(it.usedBytes) } ?: "—"
    val free = footprint?.let { KnouxFormat.bytes(it.freeBytes) } ?: "—"
    SurfaceCard(modifier, accent = NativePalette.cyan) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 108.dp).padding(horizontal = 14.dp, vertical = 14.dp),
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
                if (stacked) {
                    NativeFootprintStat(Copy(R.string.home_storage_used), used, NativePalette.cyan)
                    NativeFootprintStat(Copy(R.string.home_storage_free), free, NativePalette.text)
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NativeFootprintStat(
                            Copy(R.string.home_storage_used), used, NativePalette.cyan, Modifier.weight(1f),
                        )
                        NativeFootprintStat(
                            Copy(R.string.home_storage_free), free, NativePalette.text, Modifier.weight(1f),
                        )
                    }
                }
                // The same arithmetic as the ring, drawn as one measured bar:
                // used bytes against free bytes. It is not a prediction and it
                // disappears entirely when the partition cannot be read.
                if (percent != null) {
                    Row(
                        Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50))
                            .background(NativePalette.raised)
                    ) {
                        Box(
                            Modifier.fillMaxWidth(percent / 100f).fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(NativePalette.blue, NativePalette.violet),
                                    )
                                )
                        )
                    }
                }
                if (footprint == null) {
                    Label(Copy(R.string.home_storage_unreadable), 12, NativePalette.muted, maxLines = 3)
                }
            }
        }
    }
}

@Composable
private fun NativeFootprintStat(label: Copy, value: String, tint: Color, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
            val fill by animateColorAsState(
                if (active) NativePalette.violet.copy(alpha = .24f) else NativePalette.card,
                tween(180), label = "segment fill",
            )
            val stroke by animateColorAsState(
                if (active) NativePalette.violet.copy(alpha = .70f) else NativePalette.border,
                tween(180), label = "segment stroke",
            )
            Box(
                Modifier.weight(1f).fillMaxHeight()
                    .testTag(segment.tag)
                    .selectable(selected = active, role = Role.Tab, onClick = { onSelect(segment) }),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.fillMaxWidth().height(36.dp).clip(pill)
                        .background(fill)
                        .border(1.dp, stroke, pill),
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
 * The tile carries its service's tone in exactly three places - the icon plate,
 * a short top hairline and the measured figure - so the grid reads as a family
 * without eight different cards. The whole tile is the control. An unmeasured
 * service shows a dash rather than a number borrowed from a fixture.
 */
@Composable
internal fun NativeServiceTile(
    title: Copy, mark: Mark, tint: Color, tag: String,
    value: Copy?, action: Copy?, modifier: Modifier = Modifier, onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.fillMaxWidth().heightIn(min = 104.dp).clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF121B3B), Color(0xFF0B1128))))
            .drawWithContent {
                drawContent()
                drawLine(
                    Brush.horizontalGradient(
                        listOf(tint.copy(alpha = .55f), tint.copy(alpha = .06f), Color.Transparent),
                    ),
                    Offset(0f, 1.dp.toPx()),
                    Offset(size.width * .68f, 1.dp.toPx()),
                    strokeWidth = 1.2.dp.toPx(),
                )
            }
            .border(1.dp, NativePalette.border.copy(alpha = .78f), shape)
            .testTag(tag).brandClickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ServiceIconPlate(mark, tint, size = 36.dp, iconSize = 18.dp)
            Label(
                title, 13, NativePalette.text, FontWeight.SemiBold,
                modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Glyph(
                Mark.NEXT, Modifier.size(13.dp).rotate(if (LocalArabic.current) 180f else 0f),
                NativePalette.muted.copy(alpha = .55f),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Label(
                value ?: Copy.runtime("—"), 15,
                if (value != null) tint else NativePalette.muted.copy(alpha = .72f),
                if (value != null) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            action?.let {
                val pill = RoundedCornerShape(50)
                Box(
                    Modifier.clip(pill).background(tint.copy(alpha = .12f))
                        .border(1.dp, tint.copy(alpha = .28f), pill)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Label(it, 10, tint, FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
    }
}

/**
 * Operations that actually freed measured bytes. Home's recent list and the
 * reports chart both read this one rule, so the two screens can never disagree
 * about which cleanups count.
 */
internal fun List<CleanupHistory>.completedCleanups(): List<CleanupHistory> = filter {
    (it.status == OperationStatus.SUCCEEDED || it.status == OperationStatus.PARTIAL) && it.bytes > 0L
}

/**
 * The small recent-activity block, Figma x20 y628.
 *
 * It lists real finished operations from the Room journal only. With no
 * operation recorded it says so; it never fills itself with sample entries.
 */
@Composable
internal fun NativeRecentActivity(history: List<CleanupHistory>, modifier: Modifier = Modifier) {
    val operations = history.completedCleanups().take(3)
    SurfaceCard(modifier) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Label(Copy(R.string.home_recent_title), 13, NativePalette.muted, FontWeight.SemiBold)
            if (operations.isEmpty()) {
                Label(Copy(R.string.k_nothing_measured_yet), 13, NativePalette.text)
            } else {
                // The same digit convention as the reports chart: measured
                // figures and their dates stay Latin and isolated in both
                // languages, so one operation reads identically on both screens.
                val formatter = remember { DateFormat.getDateInstance(DateFormat.SHORT, Locale.US) }
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
