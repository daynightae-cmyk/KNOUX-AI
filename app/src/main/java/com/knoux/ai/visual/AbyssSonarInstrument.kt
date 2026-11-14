package com.knoux.ai.visual

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.knoux.ai.Label
import com.knoux.ai.LocalArabic
import com.knoux.ai.LocalPreviewMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Abyss Sonar — the signature instrument for PC05 Deep Scan.
 *
 * Three coupled readings, all from measured state:
 *
 *  - **Depth column** (left): one sector per selected module, in scan order. A
 *    sector fills only with the bytes that module actually returned; a module the
 *    platform refused stays empty and fogged, named in the caption.
 *  - **Descent node**: sits at `completed modules / selected modules` on the
 *    column. When no module has finished there is no fraction, so the node waits
 *    at the surface and the dial sweeps to listen — the instrument never claims a
 *    depth it has not reached.
 *  - **Sonar dial** (right): the core orb grows with the share of modules that
 *    were really measured, and carries the measured byte total as its numeral.
 *    One satellite per module orbits the rim, coloured by that module's state.
 *
 * Visual contract (`docs/design/KNOUX_SERVICE_VISUAL_IDENTITY_20261113.md`):
 * abyss teal, radial geometry, motion only while work is observable, still frame
 * still truthful under reduced motion, and no numeral that was not counted.
 */
@Composable
internal fun AbyssSonarInstrument(
    frame: InstrumentFrame,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    val systemMotion = LocalPreviewMotion.current
    val arabic = LocalArabic.current
    val animated = frame.motion && systemMotion

    val teal = Color(0xff00D4AA)
    val deepTeal = Color(0xff00A88A)
    val violet = Color(0xff8B5CF6)
    val fog = Color(0xff6E6A92)
    val alert = Color(0xffFF6B6B)
    val chalk = Color(0xffE8E6F0)
    val mist = Color(0xffA8A4C8)
    val ink = Color(0xff07051A)

    val breath = rememberIfAnimated(animated, periodMillis = 5200)
    val sweep = rememberIfAnimated(animated, periodMillis = 2800)
    val pulse = rememberIfAnimated(animated, periodMillis = 2100)

    val dialScale by animateFloatAsState(
        targetValue = when (frame.phase) {
            InstrumentPhase.FAILED -> 0.82f
            InstrumentPhase.PAUSED -> 0.9f
            InstrumentPhase.PARTIAL -> 0.95f
            else -> 1f
        },
        animationSpec = tween(420),
        label = "dialScale",
    )

    val totalWeight = frame.facets.sumOf { it.weight }.coerceAtLeast(1L)
    val measuredFacets = frame.facets.filter {
        it.state == FacetState.MEASURED || it.state == FacetState.THIN
    }
    val coreCoverage = if (frame.facets.isEmpty()) 0f else {
        measuredFacets.size.toFloat() / frame.facets.size
    }
    val depth = frame.progress?.coerceIn(0f, 1f)

    val a11y = description ?: describe(frame, arabic)

    Box(
        modifier
            .fillMaxWidth()
            .height(392.dp)
            .testTag("instrument-abyss-sonar")
            .semantics { contentDescription = a11y },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val columnX = w * 0.17f
            val columnTop = h * 0.10f
            val columnBottom = h * 0.90f
            val columnHeight = columnBottom - columnTop
            val columnWidth = 5.dp.toPx()

            val dialCx = w * 0.63f
            val dialCy = h * 0.47f
            val dialR = min(w * 0.29f, h * 0.38f)

            // ── Depth spine ──────────────────────────────────────────────────
            drawLine(
                color = mist.copy(alpha = 0.16f),
                start = Offset(columnX, columnTop),
                end = Offset(columnX, columnBottom),
                strokeWidth = columnWidth,
            )

            val sectorHeight = if (frame.facets.isEmpty()) columnHeight
            else columnHeight / frame.facets.size

            frame.facets.forEachIndexed { index, facet ->
                val top = columnBottom - (index + 1) * sectorHeight
                val share = (facet.weight.toFloat() / totalWeight).coerceIn(0f, 1f)
                val fillColor = when (facet.state) {
                    FacetState.MEASURED -> teal
                    FacetState.THIN -> deepTeal
                    FacetState.FOGGED -> fog
                    FacetState.BLOCKED -> fog.copy(alpha = 0.35f)
                    FacetState.REMOVED -> violet
                }
                // Sector body: real share of the measured bytes.
                if (share > 0f) {
                    val segment = (sectorHeight - 6.dp.toPx()).coerceAtLeast(2.dp.toPx())
                    drawLine(
                        color = fillColor.copy(alpha = 0.85f),
                        start = Offset(columnX, top + segment),
                        end = Offset(columnX, top + segment * (1f - share)),
                        strokeWidth = columnWidth,
                    )
                }
                // Sector tick: marks the boundary even when the sector is empty.
                drawLine(
                    color = fillColor.copy(alpha = if (facet.state == FacetState.FOGGED) 0.35f else 0.7f),
                    start = Offset(columnX - 9.dp.toPx(), top + sectorHeight / 2f),
                    end = Offset(columnX + 9.dp.toPx(), top + sectorHeight / 2f),
                    strokeWidth = if (facet.state == FacetState.MEASURED) 2.dp.toPx() else 1.dp.toPx(),
                )
                if (facet.state == FacetState.FOGGED) {
                    // Fog is drawn as a broken outline: unknown, not zero.
                    val dash = 3.dp.toPx()
                    var y = top + 4.dp.toPx()
                    while (y < top + sectorHeight - 4.dp.toPx()) {
                        drawLine(
                            color = fog.copy(alpha = 0.38f),
                            start = Offset(columnX - 3.dp.toPx(), y),
                            end = Offset(columnX + 3.dp.toPx(), y),
                            strokeWidth = 1.dp.toPx(),
                        )
                        y += dash * 2.4f
                    }
                }
            }

            // ── Descent node ─────────────────────────────────────────────────
            val surfacedFraction: Float? = depth
            val nodeY = if (surfacedFraction == null) {
                columnTop
            } else {
                columnTop + columnHeight * (1f - surfacedFraction)
            }
            if (surfacedFraction != null) {
                drawLine(
                    color = teal.copy(alpha = 0.35f),
                    start = Offset(columnX, nodeY),
                    end = Offset(dialCx - dialR * 0.92f, dialCy),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            val nodeGlow = if (animated) 0.5f + 0.5f * (1f - breath.value) else 0.7f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(teal.copy(alpha = 0.45f * nodeGlow), Color.Transparent),
                    center = Offset(columnX, nodeY),
                    radius = 18.dp.toPx(),
                ),
                radius = 18.dp.toPx(),
                center = Offset(columnX, nodeY),
            )
            drawCircle(color = teal, radius = 5.dp.toPx(), center = Offset(columnX, nodeY))
            drawCircle(
                color = ink,
                radius = 2.dp.toPx(),
                center = Offset(columnX, nodeY),
            )

            // ── Sonar dial ───────────────────────────────────────────────────
            val dialAlphaBase = when (frame.phase) {
                InstrumentPhase.IDLE -> 0.30f
                InstrumentPhase.ARMED, InstrumentPhase.RUNNING -> 0.55f
                else -> 0.42f
            }
            listOf(0.42f, 0.68f, 1f).forEach { ring ->
                drawCircle(
                    color = teal.copy(alpha = dialAlphaBase * (1f - ring * 0.45f)),
                    radius = dialR * ring,
                    center = Offset(dialCx, dialCy),
                    style = Stroke(width = if (ring == 1f) 1.6.dp.toPx() else 1.dp.toPx()),
                )
            }

            // Sweep: only while a pass is genuinely running.
            if (animated && (frame.phase == InstrumentPhase.RUNNING || frame.phase == InstrumentPhase.ARMED)) {
                val angle = sweep.value * 360f
                rotate(degrees = angle, pivot = Offset(dialCx, dialCy)) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(Color.Transparent, teal.copy(alpha = 0.45f), Color.Transparent),
                            center = Offset(dialCx, dialCy),
                        ),
                        startAngle = 0f,
                        sweepAngle = 70f,
                        useCenter = true,
                        topLeft = Offset(dialCx - dialR, dialCy - dialR),
                        size = Size(dialR * 2f, dialR * 2f),
                    )
                }
                drawCircle(
                    color = teal.copy(alpha = 0.6f),
                    radius = 5.dp.toPx(),
                    center = Offset(
                        dialCx + dialR * cos(angle * PI.toFloat() / 180f),
                        dialCy + dialR * sin(angle * PI.toFloat() / 180f),
                    ),
                )
            }

            // Core orb: radius follows the fraction of modules really measured.
            val orbRadius = dialR * (0.30f + 0.34f * coreCoverage) * dialScale
            val orbPulse = if (animated && frame.phase == InstrumentPhase.RUNNING) {
                1f + 0.04f * pulse.value
            } else 1f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        teal.copy(alpha = 0.95f),
                        deepTeal.copy(alpha = 0.55f),
                        violet.copy(alpha = 0.28f),
                        Color.Transparent,
                    ),
                    center = Offset(dialCx, dialCy),
                    radius = orbRadius * 1.9f * orbPulse,
                ),
                radius = orbRadius * 1.9f * orbPulse,
                center = Offset(dialCx, dialCy),
            )
            drawCircle(
                color = teal.copy(alpha = 0.85f),
                radius = orbRadius * orbPulse,
                center = Offset(dialCx, dialCy),
                style = Stroke(width = 2.dp.toPx()),
            )

            // Facet satellites: one per module, angle fixed by scan order.
            val count = frame.facets.size
            frame.facets.forEachIndexed { index, facet ->
                val angle = (-210f + index * (240f / max(1, count - 1))) * PI.toFloat() / 180f
                val orbit = dialR * 1.16f
                val cx = dialCx + orbit * cos(angle)
                val cy = dialCy + orbit * sin(angle)
                val color = when (facet.state) {
                    FacetState.MEASURED -> teal
                    FacetState.THIN -> deepTeal
                    FacetState.FOGGED -> fog
                    FacetState.BLOCKED -> fog.copy(alpha = 0.35f)
                    FacetState.REMOVED -> violet
                }
                val dot = when (facet.state) {
                    FacetState.MEASURED -> 6.dp.toPx()
                    FacetState.THIN -> 5.dp.toPx()
                    else -> 3.5.dp.toPx()
                }
                if (facet.state == FacetState.MEASURED) {
                    drawCircle(
                        color = color.copy(alpha = 0.22f),
                        radius = dot * 2.4f,
                        center = Offset(cx, cy),
                    )
                }
                drawCircle(color = color, radius = dot, center = Offset(cx, cy))
                if (facet.breakdown.removable > 0) {
                    drawCircle(
                        color = chalk.copy(alpha = 0.75f),
                        radius = 1.6.dp.toPx(),
                        center = Offset(cx, cy),
                    )
                }
            }

            // Failure keeps one honest mark: a broken spine, not a fake result.
            if (frame.phase == InstrumentPhase.FAILED) {
                drawLine(
                    color = alert.copy(alpha = 0.7f),
                    start = Offset(columnX - 12.dp.toPx(), nodeY),
                    end = Offset(columnX + 12.dp.toPx(), nodeY),
                    strokeWidth = 2.dp.toPx(),
                )
            }
        }

        // ── Numeral + caption, centred on the dial ──────────────────────────
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .semantics { contentDescription = a11y },
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.50f)
                    // The dial sits at 63% of the canvas, not its centre; a readout
                    // on the page centre would rest on the ring's edge.
                    .absoluteOffset(x = maxWidth * 0.13f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                frame.heroValue?.let { bytes ->
                    Label(
                        text = formatBytes(bytes),
                        size = 26,
                        tint = chalk,
                        weight = FontWeight.ExtraBold,
                        align = TextAlign.Center,
                    )
                }
                Label(
                    text = frame.caption.localizedFor(arabic),
                    size = 11,
                    tint = mist,
                    weight = FontWeight.Medium,
                    align = TextAlign.Center,
                )
            }

            // Facet strip: the static, reduced-motion-safe reading of the same data.
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                frame.facets.forEach { facet ->
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                when (facet.state) {
                                    FacetState.MEASURED -> teal
                                    FacetState.THIN -> deepTeal
                                    FacetState.FOGGED -> fog
                                    FacetState.BLOCKED -> fog.copy(alpha = 0.35f)
                                    FacetState.REMOVED -> violet
                                }
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberIfAnimated(animated: Boolean, periodMillis: Int): State<Float> {
    if (!animated) {
        return rememberPaused()
    }
    val transition = rememberInfiniteTransition(label = "abyss sonar")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMillis),
            repeatMode = RepeatMode.Restart,
        ),
        label = "abyss tick",
    )
}

@Composable
private fun rememberPaused(): State<Float> = remember { mutableStateOf(0.5f) }

private fun com.knoux.ai.Copy.localizedFor(arabic: Boolean): String = text(arabic)

private fun describe(frame: InstrumentFrame, arabic: Boolean): String {
    val phase = when (frame.phase) {
        InstrumentPhase.IDLE -> if (arabic) "سكون" else "idle"
        InstrumentPhase.ARMED -> if (arabic) "قراءة الوصول" else "reading access"
        InstrumentPhase.RUNNING -> if (arabic) "فحص جارٍ" else "scanning"
        InstrumentPhase.PAUSED -> if (arabic) "متوقف" else "stopped"
        InstrumentPhase.RESULT -> if (arabic) "اكتمل" else "complete"
        InstrumentPhase.PARTIAL -> if (arabic) "جزئي" else "partial"
        InstrumentPhase.FAILED -> if (arabic) "لم يُقس" else "not measured"
        InstrumentPhase.UNDO -> if (arabic) "مراجعة" else "review"
    }
    val measured = frame.facets.count { it.state == FacetState.MEASURED || it.state == FacetState.THIN }
    val unread = frame.facets.count { it.state == FacetState.FOGGED }
    val hero = frame.heroValue?.let { formatBytes(it) }
    return if (arabic) {
        buildString {
            append("سونار العمق، ")
            append(phase)
            if (hero != null) append("، $hero")
            append("، $measured وحدة مقاسة")
            if (unread > 0) append("، $unread غير مقاسة")
        }
    } else {
        buildString {
            append("Abyss Sonar, ")
            append(phase)
            if (hero != null) append(", $hero")
            append(", $measured measured module(s)")
            if (unread > 0) append(", $unread unmeasured")
        }
    }
}

/** Byte formatting used by the instrument only; never applied to an unmeasured value. */
internal fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000_000L -> "%.2f TB".format(bytes / 1_000_000_000_000.0)
    bytes >= 1_000_000_000L -> "%.2f GB".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000L -> "%.2f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000L -> "%.1f KB".format(bytes / 1_000.0)
    else -> "$bytes B"
}
