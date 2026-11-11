package com.knoux.ai

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

internal val LocalArabic = compositionLocalOf { false }
internal val LocalPreviewMotion = compositionLocalOf { false }
internal val LocalRoomy = compositionLocalOf { false }
internal val LocalTall = compositionLocalOf { false }
internal val DisplayFace = FontFamily(Font(R.font.outfit_extrabold, FontWeight.ExtraBold))
private val panel = listOf(Color(0xff1b0d3c), Color(0xff090a22), Color(0xff0d1433))
private val vivid = listOf(Color(0xffc61bf7), Color(0xff721eea), Color(0xff2d42e8), Color(0xff00c2ff))

@Composable
internal fun Copy.localized() = text(LocalArabic.current)

@Composable
internal fun KnouxTheme(arabic: Boolean, motion: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, secondary = Pink, background = Ink, surface = Color(0xff1a1642), onSurface = Chalk)) {
        CompositionLocalProvider(
            LocalArabic provides arabic, LocalLayoutDirection provides if (arabic) LayoutDirection.Rtl else LayoutDirection.Ltr,
            LocalPreviewMotion provides motion, content = content
        )
    }
}

@Composable
internal fun Label(
    text: String, size: Int = 12, tint: Color = Chalk, weight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier, align: TextAlign? = null, maxLines: Int = Int.MAX_VALUE
) {
    Text(text, modifier, color = tint, fontSize = size.sp, lineHeight = (size * 1.18f).sp, fontWeight = weight,
        textAlign = align, maxLines = maxLines)
}

/**
 * Premium glass material.
 *
 * The surface is built from layers rather than one flat tint: a deep base
 * gradient, a distinct lit top edge, an inner reflection sweep, a directional
 * border that is bright on the light side and quiet on the other, a soft cast
 * shadow underneath and one localised light concentration near the content.
 */
internal fun Modifier.glass(
    radius: Int, active: Boolean = false, ghost: Boolean = false, tab: Boolean = false, tint: Color? = null
): Modifier =
    drawBehind {
        val rad = CornerRadius(radius.dp.toPx())
        val width = size.width.coerceAtLeast(1f)
        val height = size.height.coerceAtLeast(1f)
        val edge = when {
            tab -> listOf(Color(0xff2a1259), Color(0xff47209c), Color(0xff1d0c46))
            active -> vivid
            ghost -> listOf(Color(0xff241a52), Color(0xff14123a), Color(0xff241a4e))
            else -> panel
        }
        // Cast shadow: the panel sits above the background instead of floating flat.
        drawRoundRect(Color.Black.copy(alpha = if (active) .34f else .26f),
            topLeft = Offset(0f, height * .035f), size = size, cornerRadius = rad)
        // Deep base with a vertical depth gradient.
        drawRoundRect(Brush.verticalGradient(listOf(edge[0], edge[1], edge[2], edge[1]),
            startY = 0f, endY = height), cornerRadius = rad)
        // Directional sheen across the upper half.
        drawRoundRect(Brush.linearGradient(listOf(Color.White.copy(alpha = if (active) .20f else .09f), Color.Transparent),
            start = Offset(0f, 0f), end = Offset(width * .9f, height * .8f)), cornerRadius = rad)
        // Distinct top-edge highlight: the polished lip of the glass.
        drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = if (active) .26f else .12f),
            Color.Transparent), startY = 0f, endY = height * .36f), cornerRadius = rad)
        // Localised light concentration where the content starts.
        val flash = tint ?: if (active) Color(0xffd16bff) else Color(0xff9a6bff)
        val flashAlpha = if (active) .60f else .14f
        drawRoundRect(Brush.radialGradient(listOf(flash.copy(alpha = flashAlpha), Color.Transparent),
            Offset(width * .10f, -height * .06f), width * .62f), cornerRadius = rad)
        // Cool bounce light from the opposite side.
        val bounce = if (active && !tab) Light.cyan else tint ?: Light.violet
        drawRoundRect(Brush.radialGradient(listOf(bounce.copy(alpha = if (active) .52f else .16f), Color.Transparent),
            Offset(width * .97f, height * 1.02f), width * .38f), cornerRadius = rad)
        // Border: bright where the light hits, quiet on the shadow side.
        val stroke = when {
            active -> listOf(Color.White.copy(alpha = .95f), Color(0xffd9b4ff), if (tab) Light.magenta else Light.cyan)
            tint != null -> listOf(tint.copy(alpha = .92f), Color(0xff4b4a74), tint.copy(alpha = .48f))
            else -> listOf(Color(0xffd6c8ff), Color(0xff6a5c92), Color(0xffc47ff2), Color(0xff5b5a7c))
        }
        drawRoundRect(Brush.linearGradient(stroke, Offset.Zero, Offset(width, height)), cornerRadius = rad,
            style = Stroke((if (active) 1.7.dp else 1.dp).toPx()))
        // Pinpoints on the rim keep the faces dark while making the glass catch the light.
        val rim = Brush.linearGradient(
            0f to Color.Transparent, .11f to flash.copy(alpha = .89f), .19f to Light.white.copy(alpha = .82f),
            .30f to Color.Transparent, .69f to Color.Transparent, .82f to bounce.copy(alpha = .94f),
            .87f to Light.ice.copy(alpha = .73f), 1f to Color.Transparent,
            start = Offset.Zero, end = Offset(width, height)
        )
        val rimAlpha = if (active) .96f else .85f
        drawRoundRect(rim, cornerRadius = rad, alpha = rimAlpha * .24f, style = Stroke(3.dp.toPx()))
        drawRoundRect(rim, cornerRadius = rad, alpha = rimAlpha, style = Stroke(.95.dp.toPx()))
    }

private fun Modifier.buttonBloom(primary: Boolean): Modifier = drawBehind {
    if (!primary) return@drawBehind
    for (dp in listOf(16, 10, 6)) {
        val delta = dp.dp.toPx()
        drawRoundRect(Brush.horizontalGradient(listOf(Light.magenta.copy(alpha = .18f),
            Color(0xff5a2bf0).copy(alpha = .21f), Light.cyan.copy(alpha = .17f))),
            topLeft = Offset(-delta, -delta * .60f), size = Size(size.width + delta * 2, size.height + delta * 1.20f),
            cornerRadius = CornerRadius(size.height / 2 + delta))
    }
}

@Composable
internal fun GlassPanel(
    modifier: Modifier = Modifier, radius: Int = 14, tint: Color? = null, content: @Composable () -> Unit
) {
    Box(modifier.glass(radius, tint = tint), contentAlignment = Alignment.CenterStart) { content() }
}

/**
 * Icon container: a lit rounded shell with an internal light source, a bright
 * upper edge and the volumetric glyph inside it.
 */
@Composable
internal fun IconBadge(mark: Mark, tint: Color, modifier: Modifier = Modifier, diameter: Int = 36) {
    val shape = RoundedCornerShape((diameter * .30f).dp)
    val box = modifier.size(diameter.dp)
    Box(box.background(
        Brush.verticalGradient(listOf(tint.copy(alpha = .52f), Color(0xff140d33).copy(alpha = .82f))),
        shape
    ).border(.8.dp, Brush.horizontalGradient(listOf(Color.White.copy(alpha = .65f), tint.copy(alpha = .85f), Color.Transparent)), shape),
        contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            bloom(Offset(size.width * .30f, size.height * .18f), size.width * .72f, tint, .95f)
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = .24f), Color.Transparent),
                Offset(size.width * .26f, size.height * .16f), size.width * .42f), size.width * .42f, Offset(size.width * .26f, size.height * .16f))
        }
        VolumetricGlyph(mark, tint, (diameter * .70f).dp)
    }
}

@Composable
internal fun HeroHeading(page: Page, subtitle: Copy, isScanRunning: Boolean = true, artBeside: Boolean = false) {
    val animate = LocalPreviewMotion.current && isScanRunning
    val inline = artBeside && !LocalRoomy.current
    val title = page.title.localized()
    val split = title.lastIndexOf(' ')
    val first = if (split < 0) "" else title.substring(0, split + 1)
    val last = title.substring(split + 1)
    val fontSize = when {
        inline -> 23
        page == Page.HOME -> 38
        page == Page.DUPLICATES || page == Page.RESIDUE -> 26
        else -> 30
    }
    val font = if (LocalArabic.current) FontFamily.Default else DisplayFace
    val styledTitle = buildAnnotatedString {
        withStyle(SpanStyle(color = Chalk)) { append(first) }
        withStyle(SpanStyle(brush = Brush.verticalGradient(listOf(Color(0xfff896ff), Color(0xffe06bff), Color(0xffc25bff))))) { append(last) }
    }
    val alignment = if (inline) TextAlign.Start else TextAlign.Center
    val headings: @Composable () -> Unit = {
        Text(styledTitle, Modifier.fillMaxWidth().padding(horizontal = 4.dp).testTag("screen-${page.id}"), style = TextStyle(
            fontSize = fontSize.sp, lineHeight = (fontSize * 1.05f).sp, fontFamily = font,
            fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic, textAlign = alignment
        ))
        Label(subtitle.localized(), if (page == Page.HOME) 14 else 12, Mist,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), align = alignment)
    }
    if (inline) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(.61f), content = { headings() })
            NeonHero(page.hero, animate, Modifier.weight(.39f))
        }
    } else {
        NeonHero(page.hero, animate)
        headings()
    }
}

@Composable
internal fun AccentButton(
    action: Action, modifier: Modifier = Modifier, primary: Boolean = true, enabled: Boolean = true,
    tag: String = "", onClick: () -> Unit
) {
    val round = RoundedCornerShape(28.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale = if (pressed) .975f else 1f
    val shimmer = if (primary && enabled && LocalPreviewMotion.current) {
        val transition = rememberInfiniteTransition(label = "cta shimmer")
        transition.animateFloat(0f, 1f, infiniteRepeatable(tween(5200, easing = LinearEasing)), label = "shimmer")
    } else null
    Row(modifier.heightIn(min = 48.dp).then(if (tag.isNotEmpty()) Modifier.testTag(tag) else Modifier)
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .then(if (primary && enabled) Modifier.buttonBloom(true) else Modifier)
        .clip(round)
        .then(if (primary && enabled) Modifier.primarySurface(shimmer) else Modifier.glass(28, false, ghost = true))
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically
    ) {
        val tone = if (enabled) Chalk else Mist.copy(alpha = .63f)
        Glyph(action.icon, Modifier.size(20.dp), tone, primary && enabled)
        Label(action.label.localized(), if (action.label.localized().length > 20) 11 else 13, tone, FontWeight.Bold,
            align = TextAlign.Center, maxLines = 1)
        if (tag == "start-scan" && !LocalRoomy.current) {
            val direction = if (LocalArabic.current) 180f else 0f
            Row(Modifier.rotate(direction), horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                Glyph(Mark.NEXT, Modifier.size(13.dp), tone)
                Glyph(Mark.NEXT, Modifier.size(13.dp), tone)
            }
        }
    }
}

/** The lit surface used by primary calls to action. */
private fun Modifier.primarySurface(shimmer: State<Float>?): Modifier = drawBehind {
    val width = size.width.coerceAtLeast(1f)
    val height = size.height.coerceAtLeast(1f)
    val rad = CornerRadius(height / 2f)
    val travel = shimmer?.value?.let { it * width * 1.6f - width * .3f } ?: width * .35f
    // Body: magenta into violet into cyan, the brand CTA gradient.
    drawRoundRect(Brush.horizontalGradient(listOf(Color(0xffe83dff), Color(0xff9b2bff),
        Color(0xff3a49f0), Color(0xff00c8ff)), 0f, width), cornerRadius = rad)
    // Travelling specular band keeps the button alive without extra glow.
    if (shimmer != null) {
        drawRoundRect(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = .22f), Color.Transparent),
            start = Offset(travel - width * .22f, 0f), end = Offset(travel + width * .22f, height)), cornerRadius = rad)
    }
    // Polished top lip.
    drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = .30f), Color.Transparent),
        startY = 0f, endY = height * .55f), cornerRadius = rad)
    // Inner shadow at the bottom edge for physical depth.
    drawRoundRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0xff2a0a5e).copy(alpha = .35f)),
        startY = height * .45f, endY = height), cornerRadius = rad)
    // Clean bright edge.
    drawRoundRect(Brush.horizontalGradient(listOf(Color.White.copy(alpha = .80f), Color.Transparent,
        Light.cyan.copy(alpha = .65f)), 0f, width), cornerRadius = rad, style = Stroke(1.2.dp.toPx()))
}

@Composable
internal fun GradientBar(value: Float, modifier: Modifier = Modifier, thickness: Int = 7) {
    Canvas(modifier.fillMaxWidth().height(thickness.dp)) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(Color(0xff2a2458), cornerRadius = radius)
        drawRoundRect(Color(0xff8d7bc4), cornerRadius = radius, style = Stroke(.6.dp.toPx()))
        val fill = size.width * value.coerceIn(0f, 1f)
        if (fill > 0f) {
            val colors = listOf(Pink, Color(0xffa63dff), Color(0xff5a63ff), Cyan)
            drawRoundRect(Brush.horizontalGradient(colors, endX = fill), size = Size(fill, size.height), cornerRadius = radius)
            drawRoundRect(Color.White.copy(alpha = .44f), size = Size(fill, size.height), cornerRadius = radius, style = Stroke(.55.dp.toPx()))
        }
    }
}

@Composable
internal fun ProgressDonut(
    value: Int, label: String = "", diameter: Int = 73, split: Boolean = false,
    modifier: Modifier = Modifier, displayValue: String = "$value%"
) {
    Box(modifier.size(diameter.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val width = size.width
            val band = width * .125f
            val inset = width * .087f
            val start = Offset(inset, inset)
            val arcSize = Size(width - inset * 2, width - inset * 2)
            val sweep = if (split) 342f else 348f * value / 100f
            val tone = Brush.sweepGradient(listOf(Color(0xffb52bff), Color(0xff5f2ef7), Light.cyan, if (split) Gold else Light.cyan, Light.cyan, Light.magenta, Color(0xffb52bff)))
            // Dark cavity with an inner light that reads as depth, not a flat ring.
            drawCircle(Brush.radialGradient(listOf(Color(0xff171037), Color(0xff09071f)), center, width * .5f),
                width * .5f - inset * .35f, center)
            drawCircle(Color(0xff37306a), radius = width / 2 - inset, style = Stroke(band))
            drawCircle(Color.White.copy(alpha = .22f), radius = width / 2 - inset - band * .48f, style = Stroke(band * .16f))
            drawArc(tone, -84f, sweep, false, start, arcSize, alpha = .36f, style = Stroke(band * 2f, cap = StrokeCap.Round))
            drawArc(tone, -84f, sweep, false, start, arcSize, style = Stroke(band, cap = StrokeCap.Round))
            // Bright inner filament on the lit part of the ring.
            drawArc(tone, -84f, sweep, false, start, arcSize, alpha = .85f, style = Stroke(band * .22f, cap = StrokeCap.Round))
            // Start and end caps read as machined terminals.
            val capAngle = Math.toRadians((-84f + sweep).toDouble())
            drawCircle(Color.White.copy(alpha = .95f), band * .30f,
                Offset(size.width / 2f + (width / 2 - inset) * cos(capAngle).toFloat(),
                    size.height / 2f + (width / 2 - inset) * sin(capAngle).toFloat()))
        }
        val inset = (diameter * .15f).dp
        Column(Modifier.padding(horizontal = inset), horizontalAlignment = Alignment.CenterHorizontally) {
            val density = LocalDensity.current
            val measurer = rememberTextMeasurer()
            val preferred = (if (diameter < 50) 13 else 21).sp
            val style = LocalTextStyle.current.copy(fontSize = preferred, fontWeight = FontWeight.ExtraBold)
            val available = with(density) { (diameter.dp - inset * 2 - 2.dp).toPx() }
            val fitOnOneLine = density.fontScale <= 1.24f
            val textWidth = measurer.measure(displayValue, style, softWrap = false, maxLines = 1).size.width
            val valueSize = if (fitOnOneLine && textWidth > available) preferred * (available / textWidth) else preferred
            Text(displayValue, color = Chalk, style = style.copy(fontSize = valueSize, lineHeight = valueSize * 1.08f),
                textAlign = TextAlign.Center, softWrap = !fitOnOneLine)
            if (label.isNotEmpty()) {
                val labelSize = if (diameter < 64) 7 else 8
                Text(label, color = Mist, textAlign = TextAlign.Center, maxLines = 1, softWrap = false,
                    style = LocalTextStyle.current.copy(fontSize = labelSize.sp, lineHeight = (labelSize * 1.26f).sp,
                        fontWeight = FontWeight.Medium))
            }
        }
    }
}

@Composable
internal fun NeonPill(label: String, tint: Color = Cyan, solid: Boolean = true) {
    val shape = RoundedCornerShape(16.dp)
    val aqua = tint == Cyan && solid
    val outlined = tint == Cyan && !solid
    val fill = when {
        aqua -> listOf(Color(0xff70f6ff), Color(0xff07bddd))
        outlined -> listOf(Color(0xff103246), Color(0xff0a1c2c))
        else -> listOf(Color(0xff7a2fd0), Color(0xff3d1673))
    }
    Box(Modifier.background(Brush.verticalGradient(fill), shape)
        .border(.7.dp, tint.copy(alpha = .72f), shape).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Label(label, 10, if (aqua) Color(0xff07182b) else if (outlined) Cyan else Chalk, FontWeight.Bold, align = TextAlign.Center)
    }
}

@Composable
internal fun Separator(modifier: Modifier = Modifier) {
    Box(modifier.background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xffa692d6), Color.Transparent))))
}

@Composable
internal fun SelectionIndicator(checked: Boolean, tint: Color = Cyan) {
    val shape = RoundedCornerShape(5.dp)
    val check = if (tint == Cyan) Ink else Chalk
    Box(Modifier.size(17.dp).background(if (checked) tint else Color.Transparent, shape).border(1.dp, tint, shape), contentAlignment = Alignment.Center) {
        if (checked) Canvas(Modifier.size(12.dp)) {
            drawLine(check, Offset(size.width*.18f,size.height*.53f), Offset(size.width*.43f,size.height*.76f), 1.7.dp.toPx(), StrokeCap.Round)
            drawLine(check, Offset(size.width*.43f,size.height*.76f), Offset(size.width*.84f,size.height*.23f), 1.7.dp.toPx(), StrokeCap.Round)
        }
    }
}

@Composable
internal fun SelectChip(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.heightIn(min = 48.dp).testTag("select-all").clip(RoundedCornerShape(22.dp))
        .toggleable(checked, role = Role.Checkbox, onValueChange = onChange).padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.glass(18, ghost = true).padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            SelectionIndicator(checked)
            Label(label, 11, weight = FontWeight.Medium)
        }
    }
}

@Composable
internal fun MetricPairPanel(values: List<Metric>, privacy: Boolean = false) {
    val roomy = LocalRoomy.current
    @Composable fun MetricCell(m: Metric, index: Int, modifier: Modifier) {
        val tint = if (index == 1 && m.icon in listOf(Mark.SHIELD, Mark.HEART, Mark.BATTERY, Mark.FOLDER)) Cyan else Pink
        Row(modifier.then(if (privacy && index == 1) Modifier.border(1.dp, Cyan.copy(alpha = .72f), RoundedCornerShape(22.dp)) else Modifier)
            .padding(horizontal = 5.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally)) {
            IconBadge(m.icon, tint, diameter = if (roomy) 30 else 39)
            Column {
                Label(m.value, if (roomy || m.value.length > 6) 20 else 26, if (m.icon == Mark.HEART) Cyan else Chalk, FontWeight.ExtraBold)
                Label(m.label.localized(), 12, Mist)
            }
        }
    }
    if (roomy) Column(Modifier.fillMaxWidth().padding(5.dp)) {
        values.forEachIndexed { i, m -> MetricCell(m, i, Modifier.fillMaxWidth()) }
    } else Row(Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
        values.forEachIndexed { i, m ->
            if (i > 0) Separator(Modifier.width(1.dp).height(35.dp))
            MetricCell(m, i, Modifier.weight(1f))
        }
    }
}

@Composable
internal fun MiniMetricStrip(values: List<Metric>, vertical: Boolean = false) {
    val roomy = LocalRoomy.current
    values.chunked(if (roomy) 2 else values.size.coerceAtLeast(1)).forEach { group ->
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            group.forEachIndexed { i, m ->
                if (i > 0) Separator(Modifier.width(1.dp).height(30.dp))
                val tint = if (i % 2 == 0) Pink else Cyan
                if (vertical) {
                    Column(Modifier.weight(1f).padding(horizontal = 2.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Glyph(m.icon, Modifier.size(18.dp), tint, true)
                        Label(m.label.localized(), 10, Mist, align = TextAlign.Center)
                        Label(m.value, 11, weight = FontWeight.Bold, align = TextAlign.Center)
                    }
                } else Row(Modifier.weight(1f).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Glyph(m.icon, Modifier.size(if (roomy) 22.dp else 18.dp), tint, true)
                    Column {
                        Label(m.value, if (m.value.length > 8) 10 else 12, Chalk, FontWeight.Bold)
                        Label(m.label.localized(), 9, Mist)
                    }
                }
            }
        }
    }
}

@Composable
internal fun StatusSummaryPanel(screen: PreviewScreen, page: Page) {
    val roomy = LocalRoomy.current
    @Composable fun Facts(modifier: Modifier) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            screen.facts.forEach { fact ->
                val tint = when (fact.icon) {
                    Mark.WARN, Mark.TROPHY -> Gold
                    Mark.CALENDAR -> Pink
                    else -> Cyan
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Glyph(fact.icon, Modifier.size(16.dp), tint, true)
                    Text(buildAnnotatedString {
                        append(fact.label.localized())
                        append(": ")
                        withStyle(SpanStyle(color = if (fact.icon == Mark.CALENDAR) Chalk else tint, fontWeight = FontWeight.SemiBold)) {
                            append(fact.value.localized())
                        }
                    }, style = TextStyle(fontSize = 11.sp, lineHeight = 13.sp, color = Mist))
                }
            }
        }
    }
    GlassPanel(Modifier.fillMaxWidth()) {
        if (roomy) Column(Modifier.padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ProgressDonut(screen.score, split = page == Page.STORAGE)
            Label(screen.scoreLabel.localized(), 11, Mist)
            Facts(Modifier.padding(top = 8.dp).fillMaxWidth())
        } else Row(Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Box(Modifier.weight(.37f), contentAlignment = Alignment.Center) {
                ProgressDonut(screen.score, screen.scoreLabel.localized(), split = page == Page.STORAGE)
            }
            Separator(Modifier.width(1.dp).height(57.dp))
            Facts(Modifier.weight(.63f))
        }
    }
}

@Composable
internal fun PreviewModeDisclosure(onClick: () -> Unit) {
    val label = ("Design preview" ar "معاينة التصميم").localized()
    Box(Modifier.heightIn(min = 48.dp).testTag("design-preview").clip(RoundedCornerShape(18.dp))
        .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
        Label(label, 10, Cyan, FontWeight.Bold, Modifier.border(.8.dp, Cyan.copy(alpha = .75f), RoundedCornerShape(16.dp))
            .background(Color(0xff172052), RoundedCornerShape(16.dp)).padding(horizontal = 8.dp, vertical = 4.dp), TextAlign.Center)
    }
}

@Composable
internal fun AdaptiveActions(first: Action, second: Action, firstEnabled: Boolean = true, secondEnabled: Boolean = true,
    onFirst: () -> Unit, onSecond: () -> Unit
) {
    if (LocalRoomy.current || LocalDensity.current.fontScale > 1.25f) Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        AccentButton(first, Modifier.fillMaxWidth(), false, firstEnabled, "review-action", onFirst)
        AccentButton(second, Modifier.fillMaxWidth(), true, secondEnabled, "primary-action", onSecond)
    } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AccentButton(first, Modifier.weight(.82f), false, firstEnabled, "review-action", onFirst)
        AccentButton(second, Modifier.weight(1.18f), true, secondEnabled, "primary-action", onSecond)
    }
}
