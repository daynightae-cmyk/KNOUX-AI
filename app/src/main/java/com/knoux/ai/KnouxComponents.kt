package com.knoux.ai

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal val LocalArabic = compositionLocalOf { false }
internal val LocalPreviewMotion = compositionLocalOf { false }
/** True only inside the real utility shell, never Splash or login. */
internal val LocalNativeApp = compositionLocalOf { false }
internal val LocalRoomy = compositionLocalOf { false }
internal val LocalTall = compositionLocalOf { false }
/** Brand wordmark only: this face has no Arabic glyphs. */
internal val BrandFace = FontFamily(Font(R.font.outfit_extrabold, FontWeight.ExtraBold))
/** Every heading: renders Arabic and Latin in one texture. */
internal val DisplayFace = KnouxFont
private val panel = listOf(Color(0xff2d1570), Color(0xff0d0935), Color(0xff1c1860))
private val vivid = listOf(Color(0xffee00ff), Color(0xff8800ff), Color(0xff3300ff), Color(0xff00ddff))

@Composable
internal fun Copy.localized() = text(LocalArabic.current)

@Composable
internal fun Label(text: String, size: Int = 12, tint: Color = KnouxColors.textPrimary, weight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier, align: TextAlign? = null, maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val resolved = size.coerceAtLeast(MIN_TEXT_SIZE)
    Text(text, modifier, color = tint, fontSize = resolved.sp, lineHeight = (resolved * 1.3f).sp,
        fontWeight = weightOf(weight), fontFamily = KnouxFont, textAlign = align, maxLines = maxLines,
        overflow = overflow)
}

/**
 * Localized overload so a bilingual string can be passed without repeating
 * `.localized()` at every call site.
 */
@Composable
internal fun Label(text: Copy, size: Int = 12, tint: Color = KnouxColors.textPrimary, weight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier, align: TextAlign? = null, maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) = Label(text.localized(), size, tint, weight, modifier, align, maxLines, overflow)

/**
 * A measurement that has not been taken yet.
 *
 * The character is a real em dash — a screen reader and a UI test both read the
 * same honest token — but at display sizes an em dash is a solid bar, and a bar
 * looks like data. So it is set small and quiet instead of big and bold.
 */
@Composable
internal fun Unmeasured(size: Int = 15, align: TextAlign? = null, modifier: Modifier = Modifier) {
    Label(
        "—", size, KnouxColors.textSecondary.copy(alpha = .70f), FontWeight.SemiBold,
        modifier = modifier, align = align,
    )
}

/**
 * Premium glass material.
 *
 * The surface is built from layers rather than one flat tint: a deep base
 * gradient, a distinct lit top edge, an inner reflection sweep, a directional
 * border that is bright on the light side and quiet on the other, a soft cast
 * shadow underneath and one localised light concentration near the content.
 */
internal fun Modifier.glass(radius: Int, active: Boolean = false, ghost: Boolean = false, tab: Boolean = false, tint: Color? = null
): Modifier =
    drawBehind {
        val rad = CornerRadius(radius.dp.toPx())
        val width = size.width.coerceAtLeast(1f)
        val height = size.height.coerceAtLeast(1f)
        val edge = when {
            tab -> listOf(Color(0xff321670), Color(0xff5124b4), Color(0xff1f0d50))
            active -> vivid
            ghost -> listOf(Color(0xff2c1d66), Color(0xff15113f), Color(0xff2b1c60))
            else -> panel
        }
        // Cast shadow: the panel sits above the background instead of floating flat.
        drawRoundRect(Color.Black.copy(alpha = if (active) .40f else .32f),
            topLeft = Offset(0f, height * .035f), size = size, cornerRadius = rad)
        // Deep base with a vertical depth gradient.
        drawRoundRect(Brush.verticalGradient(listOf(edge[0], edge[1], edge[2], edge[1]),
            startY = 0f, endY = height), cornerRadius = rad)
        // Directional sheen across the upper half.
        drawRoundRect(Brush.linearGradient(listOf(Color.White.copy(alpha = if (active) .18f else .09f), Color.Transparent),
            start = Offset(0f, 0f), end = Offset(width * .9f, height * .8f)), cornerRadius = rad)
        // Distinct top-edge highlight: the polished lip of the glass.
        drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = if (active) .26f else .14f),
            Color.Transparent), startY = 0f, endY = height * .36f), cornerRadius = rad)
        // Localised light concentration where the content starts.
        val flash = tint ?: if (active) Color(0xffff5cff) else Color(0xffad72ff)
        val flashAlpha = if (active) .55f else .16f
        drawRoundRect(Brush.radialGradient(listOf(flash.copy(alpha = flashAlpha), Color.Transparent),
            Offset(width * .10f, -height * .06f), width * .62f), cornerRadius = rad)
        // Cool bounce light from the opposite side.
        val bounce = if (active && !tab) Light.cyan else tint ?: Light.violet
        drawRoundRect(Brush.radialGradient(listOf(bounce.copy(alpha = if (active) .45f else .16f), Color.Transparent),
            Offset(width * .97f, height * 1.02f), width * .38f), cornerRadius = rad)
        // Border: bright where the light hits, quiet on the shadow side.
        val stroke = when {
            active -> listOf(Color.White.copy(alpha = 1f), Color(0xffe2c2ff), if (tab) Light.magenta else Light.cyan)
            tint != null -> listOf(tint.copy(alpha = 1f), Color(0xff6f62a4), tint.copy(alpha = .62f))
            else -> listOf(Color(0xfff0e7ff), Color(0xff8a6fce), Color(0xffe08bff), Color(0xff7264a6))
        }
        drawRoundRect(Brush.linearGradient(stroke, Offset.Zero, Offset(width, height)), cornerRadius = rad,
            style = Stroke((if (active) 1.8.dp else 1.1.dp).toPx()))
        // Pinpoints on the rim keep the faces dark while making the glass catch the light.
        val rim = Brush.linearGradient(0f to Color.Transparent, .11f to flash.copy(alpha = 1f), .19f to Light.white.copy(alpha = .92f),
            .30f to Color.Transparent, .69f to Color.Transparent, .82f to bounce.copy(alpha = 1f),
            .87f to Light.ice.copy(alpha = .82f), 1f to Color.Transparent,
            start = Offset.Zero, end = Offset(width, height)
        )
        val rimAlpha = if (active) 1f else .92f
        drawRoundRect(rim, cornerRadius = rad, alpha = rimAlpha * .18f, style = Stroke(3.dp.toPx()))
        drawRoundRect(rim, cornerRadius = rad, alpha = rimAlpha, style = Stroke(1.05.dp.toPx()))
    }

internal fun Modifier.buttonBloom(primary: Boolean, pulse: State<Float>? = null): Modifier = drawBehind {
    if (!primary) return@drawBehind
    val breath = pulse?.value ?: 0f
    for ((index, dp) in listOf(14, 9, 5).withIndex()) {
        val delta = dp.dp.toPx() * (1f + breath * .10f)
        val base = listOf(.12f, .15f, .13f)[index]
        drawRoundRect(Brush.horizontalGradient(listOf(Light.magenta.copy(alpha = base * (1f + breath * .35f)),
            Color(0xff5a2bf0).copy(alpha = base * 1.08f), Light.cyan.copy(alpha = base * (1f + breath * .30f)))),
            topLeft = Offset(-delta, -delta * .60f), size = Size(size.width + delta * 2, size.height + delta * 1.20f),
            cornerRadius = CornerRadius(size.height / 2 + delta))
    }
}

@Composable
internal fun GlassPanel(modifier: Modifier = Modifier, radius: Int = 14, tint: Color? = null, content: @Composable () -> Unit
) {
    // Scoped substitution: actual utility screens borrow the Figma quiet navy
    // panel, while the approved Splash/Onboarding/Auth cinematic glass is intact.
    val native = LocalNativeApp.current
    val shape = RoundedCornerShape(radius.dp)
    val surface = if (native) {
        modifier.clip(shape).nxCard(radius, glow = tint ?: Nx.violet)
    } else modifier.glass(radius, tint = tint)
    Box(surface, contentAlignment = Alignment.CenterStart) { content() }
}

/**
 * Icon container: a lit rounded shell with an internal light source, a bright
 * upper edge and the volumetric glyph inside it.
 */
@Composable
internal fun IconBadge(mark: Mark, tint: Color, modifier: Modifier = Modifier, diameter: Int = 36) {
    NxTile(mark, tint, modifier, size = diameter, glyph = (diameter * .54f).toInt().coerceAtLeast(14))
}

@Composable
internal fun HeroHeading(page: Page, subtitle: Copy, isScanRunning: Boolean = true, artBeside: Boolean = false) {
    NxHeader(page.title, subtitle, tag = "screen-${page.id}")
}

@Composable
internal fun AccentButton(action: Action, modifier: Modifier = Modifier, primary: Boolean = true, enabled: Boolean = true,
    tag: String = "", selected: Boolean? = null, labelSize: Int = 13, onClick: () -> Unit
) {
    // The utility shell draws every action from the reference kit; the approved
    // Splash / Auth / Onboarding cinematics keep their own signature material.
    if (LocalNativeApp.current) {
        if (primary) {
            NxPrimary(action.label, modifier, action.icon, enabled, tag, selected == true, onClick)
        } else {
            NxSecondary(action.label, modifier, action.icon, enabled, tag, selected == true, onClick)
        }
        return
    }
    val round = RoundedCornerShape(28.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .972f else 1f, tween(120), label = "cta press")
    val active = enabled && LocalPreviewMotion.current
    val pulse = if (active && primary) {
        val transition = rememberInfiniteTransition(label = "cta breath")
        transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing),
            RepeatMode.Reverse), label = "cta glow")
    } else null
    val shimmer = if (active && primary) {
        val transition = rememberInfiniteTransition(label = "cta shimmer")
        transition.animateFloat(0f, 1f, infiniteRepeatable(tween(3600, easing = LinearEasing)), label = "shimmer")
    } else null
    val surface = when {
        primary && enabled -> Modifier.primarySurface(shimmer, pulse)
        enabled -> Modifier.glass(28, active = selected == true, ghost = true)
        else -> Modifier.disabledSurface()
    }
    val click = if (selected != null) {
        Modifier.selectable(selected, interaction, indication = null, enabled = enabled, role = Role.RadioButton, onClick = onClick)
    } else {
        Modifier.clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
    }
    Row(modifier.heightIn(min = 48.dp).then(if (tag.isNotEmpty()) Modifier.testTag(tag) else Modifier)
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .then(if (primary && enabled) Modifier.buttonBloom(true, pulse) else Modifier)
        .clip(round)
        .then(surface)
        .then(if (selected == true) Modifier.border(1.4.dp,
            Brush.horizontalGradient(listOf(Light.ice, Light.cyan, Light.pink)), round) else Modifier)
        .then(click)
        .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically
    ) {
        val tone = if (enabled) Chalk else Mist.copy(alpha = .64f)
        Glyph(action.icon, Modifier.size(20.dp), tone, primary && enabled)
        Label(action.label.localized(), if (action.label.localized().length > 20 && labelSize <= 13) 11 else labelSize,
            tone, FontWeight.Bold, align = TextAlign.Center, maxLines = 2)
        if (tag == "start-scan" && !LocalRoomy.current) {
            val direction = if (LocalArabic.current) 180f else 0f
            Row(Modifier.rotate(direction).then(if (pulse != null) Modifier.graphicsLayer {
                translationX = pulse.value * 2.2.dp.toPx()
            } else Modifier), horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                Glyph(Mark.NEXT, Modifier.size(13.dp), tone)
                Glyph(Mark.NEXT, Modifier.size(13.dp), tone)
            }
        }
    }
}

/** A clear disabled material: still a button, never a washed-out ghost. */
private fun Modifier.disabledSurface(): Modifier = drawBehind {
    val rad = CornerRadius(size.height / 2f)
    drawRoundRect(Color(0xff17102f), cornerRadius = rad)
    drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = .08f), Color.Transparent),
        startY = 0f, endY = size.height * .55f), cornerRadius = rad)
    drawRoundRect(Brush.horizontalGradient(listOf(Color(0xff5a4a86), Color(0xff342a5e), Color(0xff4d4378)),
        startX = 0f, endX = size.width), cornerRadius = rad, style = Stroke(1.1.dp.toPx()))
}

/** The lit surface used by primary calls to action. */
internal fun Modifier.primarySurface(shimmer: State<Float>?, glow: State<Float>?): Modifier = drawBehind {
    val width = size.width.coerceAtLeast(1f)
    val height = size.height.coerceAtLeast(1f)
    val rad = CornerRadius(height / 2f)
    val breath = glow?.value ?: 0f
    val travel = shimmer?.value?.let { it * width * 1.6f - width * .3f } ?: width * .35f
    // Body: magenta into violet into cyan, the brand CTA gradient.
    drawRoundRect(Brush.horizontalGradient(listOf(Color(0xffff36f5), Color(0xffa826ff),
        Color(0xff344dff), Color(0xff00d8ff)), 0f, width), cornerRadius = rad)
    // Travelling specular band keeps the button alive without extra glow.
    if (shimmer != null) {
        drawRoundRect(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = .30f), Color.Transparent),
            start = Offset(travel - width * .24f, 0f), end = Offset(travel + width * .24f, height)), cornerRadius = rad)
    }
    // Lit left shoulder that breathes with the call to action.
    drawRoundRect(Brush.radialGradient(listOf(Color(0xffff9dff).copy(alpha = .18f + breath * .16f), Color.Transparent),
        Offset(width * .12f, height * .10f), width * .46f), cornerRadius = rad)
    // Polished top lip.
    drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = .38f), Color.Transparent),
        startY = 0f, endY = height * .55f), cornerRadius = rad)
    // Inner shadow at the bottom edge for physical depth.
    drawRoundRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0xff24085a).copy(alpha = .40f)),
        startY = height * .45f, endY = height), cornerRadius = rad)
    // Clean bright edge.
    drawRoundRect(Brush.horizontalGradient(listOf(Color.White.copy(alpha = .92f), Color.Transparent,
        Light.cyan.copy(alpha = .82f)), 0f, width), cornerRadius = rad, style = Stroke(1.3.dp.toPx()))
    // Second inner filament so the rim reads as polished metal, not a flat outline.
    drawRoundRect(Brush.horizontalGradient(listOf(Light.ice.copy(alpha = .55f), Color.Transparent,
        Color.Transparent), startX = 0f, endX = width), cornerRadius = rad, style = Stroke(.55.dp.toPx()))
}

@Composable
internal fun GradientBar(value: Float, modifier: Modifier = Modifier, thickness: Int = 7, live: Boolean = false) {
    NxBar(value, modifier, thickness, live)
}

@Composable
internal fun ProgressDonut(value: Int, label: String = "", diameter: Int = 73, split: Boolean = false,
    modifier: Modifier = Modifier, displayValue: String = "$value%", live: Boolean = false
) {
    NxRing(
        value = value,
        display = displayValue,
        label = label,
        modifier = modifier,
        diameter = diameter,
        live = live,
    )
}

@Composable
internal fun NeonPill(label: String, tint: Color = Cyan, solid: Boolean = true) {
    val shape = RoundedCornerShape(16.dp)
    val lightTint = tint == Cyan || tint == Mint || tint == Gold
    val fill = when {
        solid && lightTint -> listOf(lerp(tint, Color.White, .24f), tint)
        solid -> listOf(tint.copy(alpha = .96f), lerp(tint, Ink, .52f))
        else -> listOf(lerp(tint, Ink, .70f), lerp(tint, Ink, .88f))
    }
    val text = when {
        solid && lightTint -> Color(0xff071227)
        solid -> Chalk
        else -> tint
    }
    Box(Modifier.background(Brush.verticalGradient(fill), shape)
        .border(if (solid) .8.dp else 1.dp,
            Brush.horizontalGradient(listOf(tint.copy(alpha = 1f), lerp(tint, Color.White, .55f),
                tint.copy(alpha = .62f))), shape)
        .padding(horizontal = 8.dp, vertical = 4.dp)) {
        Label(label, 10, text, FontWeight.Bold, align = TextAlign.Center)
    }
}

@Composable
internal fun Separator(modifier: Modifier = Modifier) {
    Box(modifier.background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xffa692d6), Color.Transparent))))
}

@Composable
internal fun SelectionIndicator(checked: Boolean, tint: Color = Cyan) {
    val accent = if (tint == Cyan) Nx.cyan else tint
    val shape = RoundedCornerShape(7.dp)
    val fill by animateColorAsState(if (checked) accent else Color.Transparent,
        animationSpec = tween(if (LocalPreviewMotion.current) 180 else 0), label = "check fill")
    val checkScale by animateFloatAsState(if (checked) 1f else .55f,
        animationSpec = tween(if (LocalPreviewMotion.current) 190 else 0), label = "check pop")
    Box(Modifier.size(21.dp)
        .graphicsLayer { scaleX = if (checked) 1.04f else 1f; scaleY = if (checked) 1.04f else 1f }
        .background(fill, shape)
        .border(if (checked) 1.4.dp else 1.dp,
            if (checked) Color.White.copy(alpha = .86f) else accent.copy(alpha = .70f), shape),
        contentAlignment = Alignment.Center) {
        if (checked) Canvas(Modifier.size(13.dp).graphicsLayer { scaleX = checkScale; scaleY = checkScale }) {
            drawLine(Nx.inkDeep, Offset(size.width*.18f,size.height*.53f), Offset(size.width*.43f,size.height*.76f), 2f.dp.toPx(), StrokeCap.Round)
            drawLine(Nx.inkDeep, Offset(size.width*.43f,size.height*.76f), Offset(size.width*.84f,size.height*.23f), 2f.dp.toPx(), StrokeCap.Round)
        }
    }
}

@Composable
internal fun SelectChip(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.heightIn(min = 48.dp).testTag("select-all").clip(RoundedCornerShape(22.dp))
        .toggleable(checked, role = Role.Checkbox, onValueChange = onChange).padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically) {
        val surface = if (LocalNativeApp.current) Modifier.nxCard(14, selected = checked)
        else Modifier.glass(18, ghost = true)
        Row(surface.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            SelectionIndicator(checked)
            Label(label, 12, if (LocalNativeApp.current) Nx.text else KnouxColors.textPrimary, FontWeight.Medium)
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
internal fun AdaptiveActions(first: Action, second: Action, firstEnabled: Boolean = true, secondEnabled: Boolean = true,
    onFirst: () -> Unit, onSecond: () -> Unit, firstSelected: Boolean? = null, secondSelected: Boolean? = null
) {
    if (LocalRoomy.current || LocalDensity.current.fontScale > 1.25f) Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        AccentButton(first, Modifier.fillMaxWidth(), false, firstEnabled, "review-action", firstSelected, onClick = onFirst)
        AccentButton(second, Modifier.fillMaxWidth(), true, secondEnabled, "primary-action", secondSelected, onClick = onSecond)
    } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AccentButton(first, Modifier.weight(1f), false, firstEnabled, "review-action", firstSelected, onClick = onFirst)
        AccentButton(second, Modifier.weight(1f), true, secondEnabled, "primary-action", secondSelected, onClick = onSecond)
    }
}
