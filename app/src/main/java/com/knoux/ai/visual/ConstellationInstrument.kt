package com.knoux.ai.visual

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.knoux.ai.Copy
import com.knoux.ai.InstalledApp
import com.knoux.ai.KnouxFormat
import com.knoux.ai.LocalArabic
import com.knoux.ai.NativePalette
import com.knoux.ai.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Constellation — the signature instrument for the App Manager.
 *
 * The mechanism made visible: the installed apps are a starfield the customer can
 * read at a glance. Two honest orbits — user apps on the inner, bright ring;
 * system apps on the outer, dim ring. Position is a deterministic hash of the
 * package name, so the same sky returns on every launch and a screenshot agrees
 * with the live view. Star size is uniform on purpose: Android does not expose a
 * per-app size KNOUX may read here, so the instrument never invents one.
 *
 * Tapping a star focuses that app and names it in the caption; the matching row
 * below carries the real controls. A gentle twinkle is the only motion, and it
 * stills under reduced motion. At most 120 stars are plotted (the contract's
 * particle ceiling); the caption states the plotted count, never a borrowed total.
 */
@Composable
internal fun ConstellationInstrument(
    apps: List<InstalledApp>,
    focused: String?,
    onFocus: (String?) -> Unit,
    modifier: Modifier = Modifier,
    motion: Boolean = instrumentMotion(),
) {
    val arabic = LocalArabic.current
    val twinkle = rememberMotionPhase(motion, 4200, "constellation twinkle")
    val plotted = remember(apps) { apps.take(120) }
    val stars = remember(plotted) {
        plotted.map { app ->
            val h = app.packageName.hashCode()
            val angle = ((h ushr 8) and 0xFFFF) / 65535f * 2f * PI.toFloat()
            val jitter = ((h ushr 4) and 0xFF) / 255f
            StarApp(
                packageName = app.packageName,
                label = app.label,
                system = app.system,
                angle = angle,
                radiusFrac = if (app.system) 0.40f + jitter * 0.06f else 0.24f + jitter * 0.07f,
                twinkleOffset = (h and 0xFF) / 255f,
            )
        }
    }
    val focusedApp = stars.firstOrNull { it.packageName == focused }

    val description = buildString {
        append(if (arabic) "كوكبة التطبيقات" else "App constellation")
        append(if (arabic) "، " else ", ")
        append(KnouxFormat.number(stars.size))
        append(if (arabic) " تطبيقًا مرسومًا" else " apps plotted")
        if (focusedApp != null) {
            append(if (arabic) "، محدد " else ", focused ")
            append(focusedApp.label)
        }
    }

    Column(modifier.fillMaxWidth().testTag("instrument-constellation").semantics { contentDescription = description }) {
        Canvas(
            Modifier.fillMaxWidth().height(180.dp).pointerInput(stars) {
                detectTapGestures { tap ->
                    val hit = nearestStar(stars, size, tap)
                    onFocus(if (hit != null && hit.packageName != focused) hit.packageName else null)
                }
            },
        ) {
            drawConstellation(stars, focused, twinkle.value, motion)
        }
        val status = if (focusedApp != null) {
            Copy.args(
                R.string.constellation_star_desc,
                focusedApp.label,
                if (focusedApp.system) Copy(R.string.apps_system).text(arabic) else Copy(R.string.apps_user).text(arabic),
            )
        } else {
            Copy(R.string.constellation_hint)
        }
        InstrumentCaption(Copy(R.string.inst_constellation), status, NativePalette.blue, Modifier.padding(top = 8.dp))
        InstrumentCaption(
            Copy.args(R.string.constellation_apps, stars.size), null, NativePalette.muted, Modifier.padding(top = 3.dp),
        )
    }
}

private data class StarApp(
    val packageName: String,
    val label: String,
    val system: Boolean,
    val angle: Float,
    val radiusFrac: Float,
    val twinkleOffset: Float,
)

private fun starOffset(star: StarApp, size: IntSize): Offset {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val base = minOf(size.width, size.height) / 2f
    val r = star.radiusFrac * base * 1.7f
    return Offset(cx + cos(star.angle) * r, cy + sin(star.angle) * r * 0.82f)
}

private fun nearestStar(stars: List<StarApp>, size: IntSize, tap: Offset): StarApp? {
    var best: StarApp? = null
    var bestDist = Float.MAX_VALUE
    val threshold = 22.dp.value * (size.width / 360f).coerceAtLeast(0.6f)
    stars.forEach { star ->
        val pos = starOffset(star, size)
        val d = hypot(pos.x - tap.x, pos.y - tap.y)
        if (d < bestDist) {
            bestDist = d
            best = star
        }
    }
    return if (bestDist <= threshold * 2.2f) best else null
}

private fun DrawScope.drawConstellation(
    stars: List<StarApp>,
    focused: String?,
    twinkle: Float,
    motion: Boolean,
) {
    val w = size.width
    val h = size.height
    if (w < 1f || h < 1f) return
    val cx = w / 2f
    val cy = h / 2f
    val base = minOf(w, h) / 2f

    // Two orbit ellipses: inner user ring, outer system ring.
    listOf(0.275f to NativePalette.cyan.copy(alpha = .18f), 0.43f to NativePalette.violet.copy(alpha = .14f)).forEach { (f, tint) ->
        drawOval(
            tint,
            topLeft = Offset(cx - base * f * 1.7f, cy - base * f * 1.7f * 0.82f),
            size = androidx.compose.ui.geometry.Size(base * f * 3.4f, base * f * 3.4f * 0.82f),
            style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))),
        )
    }
    // The device core.
    drawCircle(
        Brush.radialGradient(listOf(NativePalette.blue.copy(alpha = .30f), Color.Transparent), center = Offset(cx, cy), radius = base * .3f),
        radius = base * .3f, center = Offset(cx, cy),
    )
    drawCircle(NativePalette.cyan.copy(alpha = .8f), 3.dp.toPx(), Offset(cx, cy))

    stars.forEach { star ->
        val pos = starOffset(star, IntSize(w.toInt(), h.toInt()))
        val tw = if (motion) 0.6f + 0.4f * sin(twinkle * 2f * PI.toFloat() + star.twinkleOffset * 2f * PI.toFloat()) else 1f
        val isFocus = star.packageName == focused
        val tone = if (star.system) NativePalette.violet else NativePalette.cyan
        val alpha = (if (star.system) 0.5f else 0.85f) * tw
        val r = if (isFocus) 5.dp.toPx() else if (star.system) 2.2.dp.toPx() else 3.dp.toPx()
        if (isFocus) {
            drawCircle(tone.copy(alpha = .25f), r * 2.6f, pos)
            drawCircle(tone.copy(alpha = .9f), r * 1.7f, pos, style = Stroke(1.4.dp.toPx()))
        }
        drawCircle(tone.copy(alpha = alpha.coerceIn(0f, 1f)), r, pos)
        if (isFocus) drawCircle(Color.White.copy(alpha = .95f), r * .5f, pos)
    }
}
