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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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
            Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                StorageGauge(percent)
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

/**
 * The hub's capacity instrument — a 270° observatory gauge.
 *
 * One quiet track with machined ticks, one measured arc in the brand's own
 * violet→blue→cyan order, and a lit terminal where the reading stops. The arc
 * is the real used-percent of the data partition and nothing else; before a
 * reading exists the track is drawn dashed and the centre keeps the honest
 * em dash. The reveal is one finite sweep when the figure first arrives —
 * never a looping animation, and never a value the filesystem did not give.
 */
@Composable
private fun StorageGauge(percent: Int?) {
    val motion = LocalPreviewMotion.current
    var entered by remember { mutableStateOf(!motion) }
    LaunchedEffect(percent) { if (percent != null) entered = true }
    val shown by androidx.compose.animation.core.animateFloatAsState(
        if (entered && percent != null) percent / 100f else 0f,
        tween(if (motion) 900 else 0, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "capacity sweep",
    )
    Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val band = w * .085f
            val inset = w * .10f
            val radius = w / 2f - inset
            val startAngle = 135f
            val totalSweep = 270f
            val arcTopLeft = Offset(inset, inset)
            val arcSize = androidx.compose.ui.geometry.Size(w - inset * 2, w - inset * 2)

            // Inner cavity: a dark well with one faint floor light, so the
            // gauge reads as an instrument face rather than a flat ring.
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFF101A3E), Color(0xFF0A0F26)),
                    center = center, radius = radius,
                ),
                radius = radius - band * .7f,
            )
            drawCircle(
                Brush.radialGradient(
                    listOf(NativePalette.cyan.copy(alpha = .07f), Color.Transparent),
                    center = Offset(w * .5f, w * .72f), radius = radius * .9f,
                ),
                radius = radius - band * .7f,
            )

            // Machined ticks: 28 marks, every seventh longer. They are chrome,
            // not data — the same in both measured and unmeasured states.
            for (tick in 0..27) {
                val angle = Math.toRadians((startAngle + totalSweep * tick / 27f).toDouble())
                val major = tick % 7 == 0
                val outer = radius + band * .62f
                val inner = outer - (if (major) w * .045f else w * .024f)
                drawLine(
                    (if (major) NativePalette.muted.copy(alpha = .5f) else NativePalette.border)
                        .copy(alpha = if (major) .55f else .8f),
                    Offset(w / 2f + cos(angle).toFloat() * inner, w / 2f + sin(angle).toFloat() * inner),
                    Offset(w / 2f + cos(angle).toFloat() * outer, w / 2f + sin(angle).toFloat() * outer),
                    strokeWidth = if (major) 1.6.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            if (percent == null) {
                // Unmeasured: a dashed track. No arc, no number, no colour
                // that could be mistaken for a reading.
                drawArc(
                    NativePalette.border.copy(alpha = .8f),
                    startAngle, totalSweep, false, arcTopLeft, arcSize,
                    style = Stroke(band, cap = StrokeCap.Butt, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))),
                )
                return@Canvas
            }

            // Quiet full track under the measured arc.
            drawArc(
                NativePalette.raised, startAngle, totalSweep, false, arcTopLeft, arcSize,
                style = Stroke(band, cap = StrokeCap.Round),
            )
            val sweep = totalSweep * shown
            if (sweep > .5f) {
                // Halo under the lit part, then the arc itself in the logo order.
                drawArc(
                    Brush.sweepGradient(
                        listOf(NativePalette.violet, NativePalette.blue, NativePalette.cyan),
                        center,
                    ),
                    startAngle, sweep, false, arcTopLeft, arcSize,
                    alpha = .22f, style = Stroke(band * 2.1f, cap = StrokeCap.Round),
                )
                drawArc(
                    Brush.sweepGradient(
                        listOf(NativePalette.violet, NativePalette.blue, NativePalette.cyan),
                        center,
                    ),
                    startAngle, sweep, false, arcTopLeft, arcSize,
                    style = Stroke(band, cap = StrokeCap.Round),
                )
                // Lit terminal: the exact point where the reading stops.
                val capAngle = Math.toRadians((startAngle + sweep).toDouble())
                val capCenter = Offset(
                    w / 2f + cos(capAngle).toFloat() * radius,
                    w / 2f + sin(capAngle).toFloat() * radius,
                )
                drawCircle(NativePalette.cyan.copy(alpha = .30f), band * .95f, capCenter)
                drawCircle(Color.White.copy(alpha = .95f), band * .34f, capCenter)
            }
        }
        if (percent != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Label(
                    KnouxFormat.isolate("$percent%"), 18, NativePalette.text, FontWeight.ExtraBold,
                    align = TextAlign.Center,
                )
            }
        } else {
            Label("—", 17, NativePalette.muted, FontWeight.Bold, align = TextAlign.Center)
        }
    }
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
                        .border(1.dp, stroke, pill)
                        .drawBehind {
                            // The chosen segment carries the brand line under
                            // its label — one gradient beat, not a filled tab.
                            if (active) {
                                drawLine(
                                    Brush.horizontalGradient(
                                        listOf(
                                            NativePalette.violet.copy(alpha = .2f),
                                            NativePalette.blue,
                                            NativePalette.cyan,
                                            NativePalette.violet.copy(alpha = .2f),
                                        )
                                    ),
                                    Offset(size.width * .16f, size.height - 1.5.dp.toPx()),
                                    Offset(size.width * .84f, size.height - 1.5.dp.toPx()),
                                    strokeWidth = 2.dp.toPx(),
                                    cap = StrokeCap.Round,
                                )
                            }
                        },
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
            .drawBehind {
                // One quiet corner light in the service's own tone: the tile
                // belongs to a family, and the tone says which member it is
                // without painting the whole card.
                drawRect(
                    Brush.radialGradient(
                        listOf(tint.copy(alpha = .085f), Color.Transparent),
                        center = Offset(size.width * .06f, 0f),
                        radius = size.width * .85f,
                    )
                )
            }
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
                title, 12, NativePalette.text, FontWeight.SemiBold,
                modifier = Modifier.weight(1f), maxLines = 3, overflow = TextOverflow.Ellipsis,
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
                operations.forEachIndexed { index, operation ->
                    Row(
                        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // The journal as a timeline: each real operation is a
                        // lit node on one rail, newest at the reading edge.
                        Column(
                            Modifier.width(12.dp).fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Spacer(Modifier.height(5.dp))
                            Box(
                                Modifier.size(8.dp).clip(RoundedCornerShape(50))
                                    .background(
                                        Brush.radialGradient(
                                            listOf(NativePalette.cyan, NativePalette.blue),
                                        )
                                    )
                            )
                            if (index < operations.lastIndex) {
                                Box(
                                    Modifier.width(1.dp).weight(1f)
                                        .background(NativePalette.border.copy(alpha = .7f))
                                )
                            }
                        }
                        Label(
                            Copy.args(
                                R.string.reports_chart_bar,
                                KnouxFormat.bytes(operation.bytes),
                                formatter.format(Date(operation.completedAtMillis)),
                            ),
                            12, NativePalette.text,
                            modifier = Modifier.weight(1f).padding(vertical = 3.dp), maxLines = 2,
                        )
                    }
                }
            }
        }
    }
}
