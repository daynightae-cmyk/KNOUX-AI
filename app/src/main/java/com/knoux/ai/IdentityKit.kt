package com.knoux.ai

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * The shared geometry of the identity surfaces (storage, large files, app
 * manager, battery, AI, report log).
 *
 * These are not decoration: every value painted here is handed in by a caller
 * that read it from the platform. A null is the honest unknown and is drawn as
 * fog / dash / hollow rim — never as a zero and never as a percentage.
 */

internal object Identity {
    val panelTop = Nx.cardTop
    val panelBottom = Nx.cardBottom
    val line = Nx.border
    val text = Nx.text
    val muted = Nx.muted
    val cyan = Nx.cyan
    val violet = Nx.violet
    val magenta = Nx.magenta
    val mint = Nx.green
    val gold = Nx.amber
    val rose = Nx.rose
    val amber = Nx.amber
}

internal data class IdentityMetric(
    val value: String,
    val label: Copy,
    val tint: Color = Identity.cyan,
)

internal data class IdentityRing(
    /** 0f..1f, or null when the value was never measured. */
    val fraction: Float?,
    val text: String?,
    val label: Copy,
)

internal data class IdentityFooter(
    val text: Copy,
    val mark: Mark = Mark.CHECK,
    val tint: Color = Identity.cyan,
)

/**
 * The summary panel every service shares: one service tile, up to three real
 * metrics, one ring, and (optionally) a single verified line underneath.
 */
@Composable
internal fun IdentityHero(
    world: World,
    mark: Mark,
    title: Copy,
    caption: Copy,
    metrics: List<IdentityMetric>,
    ring: IdentityRing,
    footer: IdentityFooter? = null,
    modifier: Modifier = Modifier,
    tag: String? = null,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.fillMaxWidth().then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(world.deep.copy(alpha = .92f), Identity.panelBottom, Identity.panelTop.copy(alpha = .96f)),
                ),
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(world.primary.copy(alpha = .55f), Identity.line.copy(alpha = .55f), world.secondary.copy(alpha = .35f)),
                ),
                shape,
            )
            .drawBehind {
                // One local light source; it moves with the reading direction so
                // the panel keeps its lit corner next to the title in both locales.
                val anchorX = if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl) size.width * .82f else size.width * .18f
                drawCircle(
                    Brush.radialGradient(
                        listOf(world.primary.copy(alpha = .18f), Color.Transparent),
                        Offset(anchorX, 0f), size.width * .62f,
                    ),
                    size.width * .62f, Offset(anchorX, 0f),
                )
            }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Label(title, 15, Identity.text, FontWeight.Bold, align = TextAlign.Center)
            Label(caption, 11, Identity.muted, align = TextAlign.Center, maxLines = 2)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IdentityTile(mark, world.primary, 52)
            Spacer(Modifier.width(8.dp))
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                metrics.take(3).forEach { metric ->
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        Label(metric.value, 14, metric.tint, FontWeight.Bold, maxLines = 1)
                        Label(metric.label, 10, Identity.muted, align = TextAlign.Center, maxLines = 2)
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            IdentityRingView(ring, world, 72)
        }
        footer?.let { IdentityFooterStrip(it) }
    }
}

@Composable
internal fun IdentityTile(mark: Mark, tint: Color, size: Int = 48) {
    NxTile(mark, tint, size = size, glyph = (size * .52f).toInt().coerceAtLeast(14))
}

@Composable
private fun IdentityRingView(ring: IdentityRing, world: World, size: Int) {
    Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 6.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawCircle(
                Brush.radialGradient(
                    listOf(world.primary.copy(alpha = .16f), Color.Transparent),
                    center, this.size.minDimension * .52f,
                ),
                this.size.minDimension * .52f,
                center,
            )
            drawArc(
                Color.White.copy(alpha = .07f), 0f, 360f, false,
                topLeft = Offset(inset, inset), size = arcSize,
                style = Stroke(stroke),
            )
            if (ring.fraction != null) {
                val sweep = (ring.fraction.coerceIn(0f, 1f) * 360f).coerceAtLeast(0.6f)
                drawArc(
                    Brush.sweepGradient(listOf(world.primary, world.secondary, world.primary)),
                    -90f, sweep, false,
                    topLeft = Offset(inset, inset), size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            } else {
                // Hollow, dashed rim: the scope exists but was never measured.
                val dashes = 16
                repeat(dashes) { index ->
                    if (index % 2 == 0) {
                        drawArc(
                            Identity.muted.copy(alpha = .55f),
                            -90f + index * (360f / dashes), 360f / dashes * .55f, false,
                            topLeft = Offset(inset, inset), size = arcSize,
                            style = Stroke(2.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                }
            }
        }
        Column(
            Modifier.padding(horizontal = (size * .17f).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            if (ring.text != null) {
                Label(ring.text, 15, Identity.text, FontWeight.Bold, maxLines = 1)
            } else {
                Unmeasured(15)
            }
            Label(ring.label, 10, Identity.muted, align = TextAlign.Center, maxLines = 2)
        }
    }
}

@Composable
private fun IdentityFooterStrip(footer: IdentityFooter) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(50))
            .background(footer.tint.copy(alpha = .10f))
            .border(1.dp, footer.tint.copy(alpha = .30f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Glyph(footer.mark, Modifier.size(13.dp), footer.tint)
        Spacer(Modifier.width(6.dp))
        Label(footer.text, 11, footer.tint, FontWeight.Medium, align = TextAlign.Center, maxLines = 2)
    }
}

/** A section label with the identity accent rail and an optional counter. */
@Composable
internal fun IdentitySection(
    title: Copy,
    tint: Color = Identity.violet,
    count: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.width(3.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(tint))
        Label(title, 14, Identity.text, FontWeight.Bold, maxLines = 1)
        if (count != null) NxBadge(Copy.runtime(count), tint)
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
    }
}

/**
 * One task row: service tile, one title, at most one subtitle, one measured
 * value, and a control at the far edge.
 */
@Composable
internal fun IdentityRow(
    mark: Mark,
    tint: Color,
    title: Copy,
    subtitle: Copy? = null,
    value: String? = null,
    valueNote: Copy? = null,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    tag: String? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(Identity.panelTop, Identity.panelBottom.copy(alpha = .55f)),
                ),
            )
            .border(1.dp, Identity.line.copy(alpha = .65f), shape)
            .then(
                if (onClick != null && enabled) Modifier.clickable(enabled = true, role = Role.Button, onClick = onClick)
                else Modifier,
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        IdentityTile(mark, if (enabled) tint else Identity.muted, 40)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Label(title, 13, if (enabled) Identity.text else Identity.muted, FontWeight.SemiBold, maxLines = 1)
                if (valueNote != null) {
                    Box(
                        Modifier.clip(RoundedCornerShape(50)).background(tint.copy(alpha = .14f))
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    ) { Label(valueNote, 9, tint, FontWeight.Medium, maxLines = 1) }
                }
            }
            subtitle?.let { Label(it, 10, Identity.muted, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        }
        if (value != null) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
                Label(value, 14, if (enabled) tint else Identity.muted, FontWeight.Bold, maxLines = 1)
            }
        }
        if (checked != null && onCheckedChange != null) {
            IdentityCheck(checked, enabled = enabled, onChange = onCheckedChange)
        } else {
            trailing?.invoke()
        }
    }
}

@Composable
internal fun IdentityCheck(checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier.size(40.dp)
            .semantics { contentDescription = if (checked) "selected" else "not selected" }
            .clickable(enabled = enabled, role = Role.Checkbox) { onChange(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(24.dp).clip(shape)
                .background(if (checked) Identity.cyan else Color.Transparent)
                .border(1.4.dp, if (checked) Identity.cyan else Identity.cyan.copy(alpha = .55f), shape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Glyph(Mark.CHECK, Modifier.size(15.dp), Color(0xFF041018), glow = false)
        }
    }
}

@Composable
internal fun IdentityChip(
    label: Copy,
    selected: Boolean,
    tint: Color = Identity.cyan,
    count: String? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val fill = if (selected) {
        Modifier.background(Brush.horizontalGradient(listOf(tint, tint.copy(alpha = .75f))))
    } else {
        Modifier.background(Identity.panelTop.copy(alpha = .8f))
    }
    Row(
        Modifier.heightIn(min = 34.dp).clip(shape).then(fill)
            .border(1.dp, if (selected) Color.Transparent else Identity.line, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Label(label, 11, if (selected) Color(0xFF03101C) else Identity.text, FontWeight.SemiBold, maxLines = 1)
        if (count != null) {
            Label(count, 10, if (selected) Color(0xFF03101C).copy(alpha = .75f) else Identity.muted, FontWeight.Bold)
        }
    }
}

internal data class IdentityBar(
    val label: Copy,
    val value: String,
    val fraction: Float,
    val tint: Color,
    val measured: Boolean = true,
)

@Composable
internal fun DistributionBars(entries: List<IdentityBar>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        entries.forEach { entry ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Label(entry.label, 10, Identity.muted, modifier = Modifier.width(66.dp), maxLines = 1)
                Box(
                    Modifier.weight(1f).height(9.dp).clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = .06f)),
                ) {
                    if (entry.measured) {
                        Box(
                            Modifier.fillMaxWidth(entry.fraction.coerceIn(0.02f, 1f)).height(9.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Brush.horizontalGradient(listOf(entry.tint, entry.tint.copy(alpha = .55f)))),
                        )
                    } else {
                        // Unmeasured share: a hollow, dotted track, never a fill.
                        Box(
                            Modifier.fillMaxWidth(entry.fraction.coerceIn(0.02f, 1f)).height(9.dp)
                                .border(1.dp, Identity.muted.copy(alpha = .45f), RoundedCornerShape(50)),
                        )
                    }
                }
                Label(entry.value, 10, if (entry.measured) entry.tint else Identity.muted, FontWeight.Bold, modifier = Modifier.width(52.dp), align = TextAlign.End, maxLines = 1)
            }
        }
    }
}

/**
 * A small real series (battery level samples, daily bytes). Two points are the
 * minimum: one point is a reading, not a trend, and drawing a line for it would
 * invent a history.
 */
@Composable
internal fun IdentitySparkline(
    points: List<Float>,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.fillMaxWidth().height(64.dp)) {
        if (points.size < 2) return@Canvas
        val minValue = points.min()
        val maxValue = points.max()
        val span = max(0.01f, maxValue - minValue)
        val stepX = size.width / (points.size - 1)
        fun y(value: Float): Float = size.height - ((value - minValue) / span) * (size.height * .82f) - size.height * .08f
        val line = Path()
        points.forEachIndexed { index, value ->
            val x = index * stepX
            val py = y(value)
            if (index == 0) line.moveTo(x, py) else line.lineTo(x, py)
        }
        val area = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(tint.copy(alpha = .28f), Color.Transparent)))
        drawPath(line, tint, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        points.forEachIndexed { index, value ->
            if (index == points.lastIndex) {
                drawCircle(tint.copy(alpha = .30f), 6.dp.toPx(), Offset(index * stepX, y(value)))
                drawCircle(tint, 3.dp.toPx(), Offset(index * stepX, y(value)))
            }
        }
    }
}

/** Before/after is only painted when both numbers were measured. */
@Composable
internal fun IdentityCompare(
    before: String,
    after: String,
    beforeFraction: Float?,
    afterFraction: Float?,
    tint: Color,
    beforeLabel: Copy,
    afterLabel: Copy,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CompareBar(before, beforeLabel, beforeFraction, Identity.muted, Modifier.weight(1f))
        Glyph(Mark.BACK, Modifier.size(14.dp), Identity.muted)
        CompareBar(after, afterLabel, afterFraction, tint, Modifier.weight(1f))
    }
}

@Composable
private fun CompareBar(value: String, label: Copy, fraction: Float?, tint: Color, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Label(label, 9, Identity.muted, maxLines = 1)
            Label(value, 12, tint, FontWeight.Bold, maxLines = 1)
        }
        Box(Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = .06f))) {
            if (fraction != null) {
                Box(
                    Modifier.fillMaxWidth(fraction.coerceIn(.02f, 1f)).height(7.dp)
                        .clip(RoundedCornerShape(50)).background(tint.copy(alpha = .8f)),
                )
            } else {
                Box(
                    Modifier.fillMaxWidth(.25f).height(7.dp)
                        .border(1.dp, Identity.muted.copy(alpha = .4f), RoundedCornerShape(50)),
                )
            }
        }
    }
}

/** A quiet, tinted honesty strip. */
@Composable
internal fun IdentityNotice(text: Copy, tint: Color = Identity.cyan, mark: Mark = Mark.SHIELD) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(tint.copy(alpha = .07f))
            .border(1.dp, tint.copy(alpha = .20f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Glyph(mark, Modifier.size(15.dp), tint)
        Label(text, 10, Identity.muted, modifier = Modifier.weight(1f))
    }
}

/** Honest empty: a named absence, not a zero. */
@Composable
internal fun IdentityEmpty(mark: Mark, title: Copy, body: Copy, tint: Color = Identity.muted, action: (@Composable () -> Unit)? = null) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(Identity.panelTop.copy(alpha = .55f))
            .border(1.dp, Identity.line, RoundedCornerShape(18.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Glyph(mark, Modifier.size(28.dp), tint)
        Label(title, 14, Identity.text, FontWeight.Bold, align = TextAlign.Center)
        Label(body, 11, Identity.muted, align = TextAlign.Center)
        action?.invoke()
    }
}

/** Loading placeholder that does not pretend to be a result. */
@Composable
internal fun IdentityLoading(label: Copy) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Glyph(Mark.SYNC, Modifier.size(16.dp), Identity.cyan)
        Spacer(Modifier.width(8.dp))
        Label(label, 11, Identity.muted)
    }
}

/** Page heading used above every identity surface; one family with the rest. */
@Composable
internal fun IdentityHeader(title: Copy, caption: Copy, tag: String, state: Copy? = null, stateTint: Color = Identity.cyan) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        NxHeader(title, caption, tag = tag)
        if (state != null) {
            NxBadge(state, stateTint, Modifier.padding(start = 11.dp))
        }
    }
}

/** One day/section of the activity log, with its own rail. */
@Composable
internal fun IdentityTimelineGroup(
    title: Copy,
    count: String?,
    tint: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(Identity.panelTop.copy(alpha = .8f))
                .border(1.dp, Identity.line.copy(alpha = .7f), RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier.size(26.dp).clip(RoundedCornerShape(8.dp))
                    .background(tint.copy(alpha = .16f)),
                contentAlignment = Alignment.Center,
            ) { Glyph(Mark.CALENDAR, Modifier.size(14.dp), tint) }
            Label(title, 12, Identity.text, FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
            if (count != null) Label(count, 10, Identity.muted, maxLines = 1)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier.fillMaxWidth().drawBehind {
                val railX = if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl) {
                    size.width - 11.dp.toPx()
                } else 11.dp.toPx()
                drawLine(tint.copy(alpha = .22f), Offset(railX, 0f), Offset(railX, size.height), 1.2.dp.toPx())
                drawCircle(tint, 3.2.dp.toPx(), Offset(railX, 8.dp.toPx()))
            },
        ) {
            Column(
                Modifier.fillMaxWidth().padding(start = 22.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                content = content,
            )
        }
    }
}

/** One measured operation inside the timeline: colour is its real status. */
@Composable
internal fun IdentityTimelineItem(
    mark: Mark,
    tint: Color,
    title: Copy,
    detail: Copy,
    value: String?,
    status: Copy,
    statusTint: Color,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.horizontalGradient(listOf(Identity.panelTop, Identity.panelBottom.copy(alpha = .5f))))
            .border(1.dp, Identity.line.copy(alpha = .55f), shape)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IdentityTile(mark, tint, 36)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Label(title, 12, Identity.text, FontWeight.SemiBold, maxLines = 1)
            Label(detail, 10, Identity.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(statusTint.copy(alpha = .12f))
                    .border(1.dp, statusTint.copy(alpha = .35f), RoundedCornerShape(50))
                    .padding(horizontal = 7.dp, vertical = 1.dp),
            ) { Label(status, 9, statusTint, FontWeight.Medium, maxLines = 1) }
        }
        if (value != null) {
            Label(value, 12, tint, FontWeight.Bold, maxLines = 1)
        }
    }
}

/** One full-bleed measured badge used for confidence/coverage. */
@Composable
internal fun IdentityCoverage(value: Int?, label: Copy, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier.size(8.dp).clip(RoundedCornerShape(50))
                .background(if (value != null) tint else Identity.muted.copy(alpha = .5f)),
        )
        Label(label, 10, if (value != null) tint else Identity.muted, FontWeight.SemiBold, maxLines = 1)
    }
}

/** The one fixed decision surface shared by these screens. */
@Composable
internal fun IdentityActionBar(
    primary: Action,
    secondary: Action?,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit = {},
    enabled: Boolean = true,
    primaryTag: String = "identity-primary",
    secondaryTag: String = "identity-secondary",
) {
    Column(
        Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (secondary != null) {
                AccentButton(secondary, Modifier.weight(.46f), primary = false, tag = secondaryTag, labelSize = 12, onClick = onSecondary)
            }
            AccentButton(primary, Modifier.weight(if (secondary != null) .54f else 1f), primary = true, enabled = enabled, tag = primaryTag, labelSize = 13, onClick = onPrimary)
        }
    }
}
