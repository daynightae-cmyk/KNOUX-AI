package com.knoux.ai

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val LocalArabic = compositionLocalOf { false }
internal val LocalPreviewMotion = compositionLocalOf { false }
internal val LocalRoomy = compositionLocalOf { false }
internal val LocalTall = compositionLocalOf { false }
internal val DisplayFace = FontFamily(Font(R.font.outfit_extrabold, FontWeight.ExtraBold))
private val panel = listOf(Color(0xff201943), Color(0xff10122e), Color(0xff171c39))
private val vivid = listOf(Color(0xffb53bfa), Color(0xff551bd4), Color(0xff1930dd), Color(0xff05bcea))

@Composable
internal fun Copy.localized() = text(LocalArabic.current)

@Composable
internal fun KnouxTheme(arabic: Boolean, motion: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, secondary = Pink, background = Ink, surface = Color(0xff151332), onSurface = Chalk)) {
        CompositionLocalProvider(
            LocalArabic provides arabic, LocalLayoutDirection provides if (arabic) LayoutDirection.Rtl else LayoutDirection.Ltr,
            LocalPreviewMotion provides motion, content = content
        )
    }
}

@Composable
internal fun Label(
    text: String, size: Int = 12, tint: Color = Chalk, weight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier, align: TextAlign? = null
) {
    Text(text, modifier, color = tint, fontSize = size.sp, lineHeight = (size * 1.18f).sp, fontWeight = weight, textAlign = align)
}

internal fun Modifier.glass(radius: Int, active: Boolean = false, ghost: Boolean = false, tab: Boolean = false): Modifier =
    drawBehind {
        val rad = CornerRadius(radius.dp.toPx())
        val edge = when {
            tab -> listOf(Color(0xff30185b), Color(0xff461ba5), Color(0xff281358))
            active -> vivid
            ghost -> listOf(Color(0xff2d245a), Color(0xff1b1940), Color(0xff302654))
            else -> panel
        }
        drawRoundRect(Brush.linearGradient(edge, Offset.Zero, Offset(size.width, size.height)), cornerRadius = rad)
        val flash = if (active) Color(0xffa652fa) else Color(0xff7752f5)
        drawRoundRect(Brush.radialGradient(listOf(flash.copy(alpha = if (active) .62f else .32f), Color.Transparent), Offset(size.width * .30f, 0f), size.width.coerceAtLeast(1f) * .59f), cornerRadius = rad)
        drawRoundRect(Brush.radialGradient(listOf((if (active && !tab) Cyan else Violet).copy(alpha = if (active) .55f else .25f), Color.Transparent), Offset(size.width * .56f, size.height * 1.07f), size.width.coerceAtLeast(1f) * .40f), cornerRadius = rad)
        val stroke = if (active) listOf(Chalk, Color(0xffa97aff), if (tab) Violet else Color(0xff7cedff)) else
            listOf(Color(0xffa893d1), Color(0xff5b5280), Color(0xffab7ce0), Color(0xff55547a))
        drawRoundRect(Brush.linearGradient(stroke, Offset.Zero, Offset(size.width, size.height)), cornerRadius = rad, style = Stroke((if (active) 1.3.dp else .7.dp).toPx()))
    }

private fun Modifier.buttonBloom(): Modifier = drawBehind {
    for (dp in listOf(14, 9, 5)) {
        val delta = dp.dp.toPx()
        drawRoundRect(Brush.horizontalGradient(listOf(Violet.copy(alpha = .07f), Color(0xff5231f7).copy(alpha = .08f), Cyan.copy(alpha = .08f))),
            topLeft = Offset(-delta, -delta * .55f), size = Size(size.width + delta * 2, size.height + delta * 1.10f),
            cornerRadius = CornerRadius(size.height / 2 + delta))
    }
}

@Composable
internal fun GlassPanel(modifier: Modifier = Modifier, radius: Int = 14, content: @Composable () -> Unit) {
    Box(modifier.glass(radius), contentAlignment = Alignment.CenterStart) { content() }
}

@Composable
internal fun IconBadge(mark: Mark, tint: Color, modifier: Modifier = Modifier, diameter: Int = 36) {
    val shape = RoundedCornerShape((diameter * .30f).dp)
    Box(modifier.size(diameter.dp).background(
        Brush.radialGradient(listOf(tint.copy(alpha = .46f), tint.copy(alpha = .16f), Color.Transparent)), shape
    ).border(.6.dp, tint.copy(alpha = .38f), shape), contentAlignment = Alignment.Center) { Glyph(mark, Modifier.size((diameter * .72).dp), tint, true) }
}

@Composable
internal fun HeroHeading(page: Page, subtitle: Copy, isScanRunning: Boolean = true) {
    NeonHero(page.hero, LocalPreviewMotion.current && isScanRunning)
    val title = page.title.localized()
    val split = title.lastIndexOf(' ')
    val first = if (split < 0) "" else title.substring(0, split + 1)
    val last = title.substring(split + 1)
    val fontSize = when {
        page == Page.HOME -> 35
        page == Page.DUPLICATES || page == Page.RESIDUE -> 26
        else -> 30
    }
    val font = if (LocalArabic.current) FontFamily.Default else DisplayFace
    Text(buildAnnotatedString {
        withStyle(SpanStyle(color = Chalk)) { append(first) }
        withStyle(SpanStyle(brush = Brush.verticalGradient(listOf(Pink, Color(0xffc86cf2), Color(0xffa84ae8))))) { append(last) }
    }, Modifier.fillMaxWidth().padding(horizontal = 4.dp).testTag("screen-${page.id}"), style = TextStyle(
        fontSize = fontSize.sp, lineHeight = (fontSize * 1.05f).sp, fontFamily = font,
        fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center
    ))
    Label(subtitle.localized(), if (page == Page.HOME) 14 else 12, Mist,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp), align = TextAlign.Center)
}

@Composable
internal fun AccentButton(
    action: Action, modifier: Modifier = Modifier, primary: Boolean = true, enabled: Boolean = true,
    tag: String = "", onClick: () -> Unit
) {
    val round = RoundedCornerShape(28.dp)
    Row(modifier.heightIn(min = 48.dp).then(if (tag.isNotEmpty()) Modifier.testTag(tag) else Modifier)
        .then(if (primary && enabled) Modifier.buttonBloom() else Modifier)
        .clip(round).glass(28, primary && enabled, ghost = true).clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically
    ) {
        val tone = if (enabled) Chalk else Mist.copy(alpha = .63f)
        Glyph(action.icon, Modifier.size(20.dp), tone, primary && enabled)
        Label(action.label.localized(), 13, tone, FontWeight.Bold, align = TextAlign.Center)
        if (tag == "start-scan" && !LocalRoomy.current) {
            val direction = if (LocalArabic.current) 180f else 0f
            Row(Modifier.rotate(direction), horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                Glyph(Mark.NEXT, Modifier.size(13.dp), tone)
                Glyph(Mark.NEXT, Modifier.size(13.dp), tone)
            }
        }
    }
}

@Composable
internal fun GradientBar(value: Float, modifier: Modifier = Modifier, thickness: Int = 7) {
    Canvas(modifier.fillMaxWidth().height(thickness.dp)) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(Color(0xff292652), cornerRadius = radius)
        drawRoundRect(Color(0xff68588d), cornerRadius = radius, style = Stroke(.6.dp.toPx()))
        val fill = size.width * value.coerceIn(0f, 1f)
        if (fill > 0f) {
            val colors = listOf(Pink, Color(0xff963dff), Color(0xff575dff), Cyan)
            drawRoundRect(Brush.horizontalGradient(colors, endX = fill), size = Size(fill, size.height), cornerRadius = radius)
            drawRoundRect(Color.White.copy(alpha = .30f), size = Size(fill, size.height), cornerRadius = radius, style = Stroke(.55.dp.toPx()))
        }
    }
}

@Composable
internal fun ProgressDonut(value: Int, label: String = "", diameter: Int = 73, split: Boolean = false, modifier: Modifier = Modifier) {
    Box(modifier.size(diameter.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val width = size.width
            val band = width * .125f
            val inset = width * .087f
            val start = Offset(inset, inset)
            val arcSize = Size(width - inset * 2, width - inset * 2)
            val sweep = if (split) 342f else 348f * value / 100f
            val tone = Brush.sweepGradient(listOf(Color(0xff9b35ff), Color(0xff5535f1), Cyan, if (split) Gold else Cyan, Cyan, Pink, Color(0xff9b35ff)))
            drawCircle(Color(0xff383360), radius = width / 2 - inset, style = Stroke(band))
            drawArc(tone, -84f, sweep, false, start, arcSize, alpha = .20f, style = Stroke(band * 1.8f, cap = StrokeCap.Round))
            drawArc(tone, -84f, sweep, false, start, arcSize, style = Stroke(band, cap = StrokeCap.Round))
        }
        Column(Modifier.padding(horizontal = (diameter * .15f).dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Label("$value%", if (diameter < 50) 13 else 21, weight = FontWeight.ExtraBold, align = TextAlign.Center)
            if (label.isNotEmpty()) Label(label, 9, Mist, align = TextAlign.Center)
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
        else -> listOf(Color(0xff6235a9), Color(0xff341958))
    }
    Box(Modifier.background(Brush.verticalGradient(fill), shape)
        .border(.6.dp, tint.copy(alpha = .64f), shape).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Label(label, 10, if (aqua) Color(0xff07182b) else if (outlined) Cyan else Chalk, FontWeight.Bold, align = TextAlign.Center)
    }
}

@Composable
internal fun Separator(modifier: Modifier = Modifier) {
    Box(modifier.background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xff9282bb), Color.Transparent))))
}

@Composable
internal fun SelectionIndicator(checked: Boolean) {
    val shape = RoundedCornerShape(5.dp)
    Box(Modifier.size(17.dp).background(if (checked) Cyan else Color.Transparent, shape).border(1.dp, Cyan, shape), contentAlignment = Alignment.Center) {
        if (checked) Canvas(Modifier.size(12.dp)) {
            drawLine(Ink, Offset(size.width*.18f,size.height*.53f), Offset(size.width*.43f,size.height*.76f), 1.7.dp.toPx(), StrokeCap.Round)
            drawLine(Ink, Offset(size.width*.43f,size.height*.76f), Offset(size.width*.84f,size.height*.23f), 1.7.dp.toPx(), StrokeCap.Round)
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
        Label(label, 10, Cyan, FontWeight.Bold, Modifier.border(.8.dp, Cyan.copy(alpha = .60f), RoundedCornerShape(16.dp))
            .background(Color(0xff131b3e), RoundedCornerShape(16.dp)).padding(horizontal = 8.dp, vertical = 4.dp), TextAlign.Center)
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
        AccentButton(first, Modifier.weight(1f), false, firstEnabled, "review-action", onFirst)
        AccentButton(second, Modifier.weight(1f), true, secondEnabled, "primary-action", onSecond)
    }
}
