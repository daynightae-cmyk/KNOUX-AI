package com.knoux.ai

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * KNOUX visual language, one source.
 *
 * Every utility surface in the application is drawn from these tokens and from
 * the five primitives below, so a screen can never invent a private skin. The
 * geometry and colour come from the approved references: a deep navy canvas,
 * violet-to-cyan light on the edges of every card, luminous icon tiles, a
 * segmented indicator and one lit primary action.
 *
 * Motion is decorative only: it never runs while reduced motion is requested,
 * never carries a value, and never blocks input.
 */
internal object Nx {
    // Canvas
    val ink = Color(0xFF05091E)
    val inkDeep = Color(0xFF030514)
    val aurora = Color(0xFF0B1436)

    // Surfaces
    val cardTop = Color(0xFF101A3C)
    val cardBottom = Color(0xFF080F27)
    val card = Color(0xFF0B1330)
    val raised = Color(0xFF16234A)
    val border = Color(0xFF223258)
    val borderLit = Color(0xFF3A5A96)

    // Identity
    val blue = Color(0xFF2C7BFF)
    val blueDeep = Color(0xFF0B4FC4)
    val cyan = Color(0xFF25D6F2)
    val teal = Color(0xFF1ED9C4)
    val violet = Color(0xFFA06BFF)
    val purple = Color(0xFF7B3FE4)
    val magenta = Color(0xFFE25CF0)
    val pink = Color(0xFFF06AB4)

    // Measured states
    val green = Color(0xFF2ED3A4)
    val amber = Color(0xFFF3B23C)
    val rose = Color(0xFFF24E6A)

    // Type
    val text = Color(0xFFF3F7FF)
    val muted = Color(0xFF9DAFD6)
    val faint = Color(0xFF6B7DA3)
}

/** Press feedback shared by every card and control. */
@Composable
internal fun Modifier.nxPressable(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && enabled) .976f else 1f,
        tween(110, easing = FastOutSlowInEasing),
        label = "nx press",
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            role = Role.Button,
            onClick = onClick,
        )
}

/**
 * The card of the reference: a navy body, a violet light crescent at the far
 * corner, a cyan reflection at the near corner, a hairline top highlight and a
 * thin border that brightens when the card carries a live measurement.
 */
internal fun Modifier.nxCard(
    radius: Int = 20,
    glow: Color = Nx.violet,
    glow2: Color = Nx.cyan,
    selected: Boolean = false,
): Modifier = drawBehind {
    val w = size.width.coerceAtLeast(1f)
    val h = size.height.coerceAtLeast(1f)
    val rad = CornerRadius(radius.dp.toPx())
    drawRoundRect(
        Brush.verticalGradient(listOf(Nx.cardTop, Nx.card, Nx.cardBottom)),
        cornerRadius = rad,
    )
    drawRoundRect(
        Brush.radialGradient(
            listOf(glow.copy(alpha = if (selected) .34f else .20f), Color.Transparent),
            Offset(w * .94f, -h * .06f), w * .82f,
        ),
        cornerRadius = rad,
    )
    drawRoundRect(
        Brush.radialGradient(
            listOf(glow2.copy(alpha = if (selected) .24f else .12f), Color.Transparent),
            Offset(w * .03f, h * 1.06f), w * .70f,
        ),
        cornerRadius = rad,
    )
    // Polished top lip: the only hard highlight on the surface.
    drawLine(
        Brush.horizontalGradient(
            listOf(Color.Transparent, Color.White.copy(alpha = .26f), Color.Transparent),
        ),
        Offset(radius.dp.toPx(), .7f), Offset(w - radius.dp.toPx(), .7f), 1.1f,
    )
    drawRoundRect(
        Brush.linearGradient(
            listOf(
                if (selected) glow2.copy(alpha = .90f) else Nx.border,
                Nx.border.copy(alpha = .50f),
                if (selected) glow.copy(alpha = .90f) else Nx.border,
            ),
            Offset.Zero, Offset(w, h),
        ),
        cornerRadius = rad, style = Stroke((if (selected) 1.4.dp else 1.dp).toPx()),
    )
}

@Composable
internal fun NxCard(
    modifier: Modifier = Modifier,
    radius: Int = 20,
    glow: Color = Nx.violet,
    glow2: Color = Nx.cyan,
    selected: Boolean = false,
    padding: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(radius.dp))
            .nxCard(radius, glow, glow2, selected)
            .padding(padding),
        content = content,
    )
}

/** A card whose whole body is one touch target. */
@Composable
internal fun NxActionCard(
    modifier: Modifier = Modifier,
    radius: Int = 20,
    glow: Color = Nx.violet,
    glow2: Color = Nx.cyan,
    selected: Boolean = false,
    enabled: Boolean = true,
    tag: String = "",
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxWidth().then(if (tag.isNotEmpty()) Modifier.testTag(tag) else Modifier)
            .clip(RoundedCornerShape(radius.dp))
            .nxPressable(enabled = enabled, onClick = onClick)
            .nxCard(radius, glow, glow2, selected)
            .padding(14.dp),
        content = content,
    )
}

/**
 * The service tile: a squircle lit from the top-left, a bright rim and the
 * service glyph in white. This is the object the eye uses to recognise a
 * service, so its hue belongs to the service, not to the screen.
 */
@Composable
internal fun NxTile(
    mark: Mark,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Int = 44,
    glyph: Int = (size * .52f).toInt().coerceAtLeast(16),
    glow: Boolean = true,
) {
    val shape = RoundedCornerShape((size * .30f).dp)
    Box(
        modifier.size(size.dp).clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        tint.copy(alpha = .92f),
                        tint.copy(alpha = .48f),
                        Color(0xFF0A1130),
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size * 1.15f, size * 1.25f),
                ),
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = .62f), tint.copy(alpha = .80f), Color.Transparent),
                ),
                shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (glow) {
            Canvas(Modifier.matchParentSize()) {
                val unit = this.size.width
                val centre = Offset(unit * .30f, this.size.height * .22f)
                drawCircle(
                    Brush.radialGradient(listOf(tint.copy(alpha = .55f), Color.Transparent), centre, unit * .72f),
                    unit * .72f,
                    centre,
                )
            }
        }
        Glyph(mark, Modifier.size(glyph.dp), Nx.text, glow)
    }
}

/** One headline per screen: a lit rule, the title, then a quiet subtitle. */
@Composable
internal fun NxHeader(
    title: Copy,
    subtitle: Copy? = null,
    modifier: Modifier = Modifier,
    accent: Copy? = null,
    tag: String = "",
) {
    val motion = LocalPreviewMotion.current
    val breath = if (motion) {
        val transition = rememberInfiniteTransition(label = "header rule")
        transition.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(2900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "header breath",
        )
    } else null
    Column(
        modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().then(if (tag.isNotEmpty()) Modifier.testTag(tag) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // The lit rule sits on the reading-start edge in both directions.
            Box(
                Modifier.width(3.dp).height(24.dp).clip(RoundedCornerShape(50))
                    .background(
                        Brush.verticalGradient(
                            listOf(Nx.magenta, Nx.violet, Nx.cyan),
                        ),
                    )
                    .graphicsLayer { alpha = .72f + .28f * (breath?.value ?: 0f) },
            )
            // One styled line so a long localized title wraps instead of being
            // clipped at the screen edge; the accent keeps its own colour.
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Nx.text)) { append(title.localized()) }
                    accent?.let {
                        append(" ")
                        withStyle(SpanStyle(color = Nx.cyan)) { append(it.localized()) }
                    }
                },
                modifier = Modifier.weight(1f),
                style = TextStyle(
                    fontFamily = KnouxFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp,
                    lineHeight = 26.sp,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        subtitle?.let {
            Label(it.localized(), 12, Nx.muted, modifier = Modifier.padding(start = 11.dp, end = 11.dp), maxLines = 3)
        }
    }
}

/** A ring gauge: measured sweep, luminous cap, honest empty state. */
@Composable
internal fun NxRing(
    value: Int,
    display: String,
    label: String,
    modifier: Modifier = Modifier,
    diameter: Int = 96,
    tint: Color = Nx.cyan,
    tint2: Color = Nx.violet,
    live: Boolean = false,
    indeterminate: Boolean = false,
) {
    val motion = LocalPreviewMotion.current
    val target = value.coerceIn(0, 100).toFloat()
    val shown by animateFloatAsState(
        target,
        tween(if (motion) 900 else 0, easing = FastOutSlowInEasing),
        label = "ring sweep",
    )
    val spin = if (indeterminate && motion) {
        val transition = rememberInfiniteTransition(label = "ring spin")
        transition.animateFloat(
            0f, 360f,
            infiniteRepeatable(tween(1500, easing = androidx.compose.animation.core.LinearEasing)),
            label = "ring spin",
        )
    } else null
    Box(modifier.size(diameter.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = size.width * .105f
            val inset = stroke / 2f + size.width * .045f
            val box = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            // Recessed track.
            drawArc(Nx.inkDeep.copy(alpha = .92f), -90f, 360f, false, topLeft, box,
                style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(Nx.border.copy(alpha = .85f), -90f, 360f, false, topLeft, box,
                style = Stroke(stroke * .96f, cap = StrokeCap.Round))
            if (indeterminate) {
                drawArc(
                    Brush.sweepGradient(listOf(Color.Transparent, tint2, tint, Nx.text, Color.Transparent)),
                    spin?.value ?: 0f, 88f, false, topLeft, box,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            } else if (shown > 0f) {
                val sweep = shown / 100f * 360f
                // Outer bloom, then the body, then the specular filament.
                drawArc(
                    Brush.sweepGradient(listOf(tint2, tint, tint2)),
                    -90f, sweep, false, topLeft, box,
                    alpha = .38f, style = Stroke(stroke * 1.9f, cap = StrokeCap.Round),
                )
                drawArc(
                    Brush.sweepGradient(listOf(tint2, tint, tint2)),
                    -90f, sweep, false, topLeft, box,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
                drawArc(
                    Brush.horizontalGradient(listOf(tint, Nx.text)),
                    -90f, sweep, false, topLeft, box,
                    alpha = .85f, style = Stroke(stroke * .28f, cap = StrokeCap.Round),
                )
            }
        }
        Column(
            Modifier.padding(horizontal = (diameter * .17f).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                display,
                style = TextStyle(
                    fontFamily = KnouxFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = (diameter * .215f).sp,
                    lineHeight = (diameter * .235f).sp,
                    color = Nx.text,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
            )
            if (label.isNotEmpty()) {
                Label(label, if (diameter < 84) 9 else 10, Nx.muted, align = TextAlign.Center, maxLines = 2)
            }
        }
    }
}

/** The lit primary action of a screen: blue to cyan, with a real press state. */
@Composable
internal fun NxPrimary(
    label: Copy,
    modifier: Modifier = Modifier,
    icon: Mark? = null,
    enabled: Boolean = true,
    tag: String = "",
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) .975f else 1f, tween(110), label = "primary press")
    Row(
        modifier.heightIn(min = 50.dp)
            .then(if (tag.isNotEmpty()) Modifier.testTag(tag) else Modifier)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .drawBehind {
                val rad = CornerRadius(size.height / 2f)
                if (enabled) {
                    // A soft cast light instead of a flat drop shadow.
                    drawRoundRect(
                        Brush.verticalGradient(
                            listOf(Nx.cyan.copy(alpha = .22f), Color.Transparent),
                        ),
                        topLeft = Offset(-2f, size.height * .55f),
                        size = Size(size.width + 4f, size.height * .60f),
                        cornerRadius = CornerRadius(size.height * .8f),
                    )
                    drawRoundRect(
                        Brush.horizontalGradient(listOf(Nx.blueDeep, Nx.blue, Nx.cyan)),
                        cornerRadius = rad,
                    )
                    drawRoundRect(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = .30f), Color.Transparent),
                            startY = 0f, endY = size.height * .62f,
                        ),
                        cornerRadius = rad,
                    )
                    drawRoundRect(
                        Color.White.copy(alpha = .55f),
                        cornerRadius = rad,
                        style = Stroke(1.dp.toPx()),
                    )
                    if (selected) {
                        drawRoundRect(
                            Nx.text.copy(alpha = .92f),
                            cornerRadius = rad,
                            style = Stroke(2.dp.toPx()),
                        )
                    }
                } else {
                    drawRoundRect(Nx.raised, cornerRadius = rad)
                    drawRoundRect(Nx.border, cornerRadius = rad, style = Stroke(1.dp.toPx()))
                }
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tone = if (enabled) Color.White else Nx.muted
        if (icon != null) {
            Glyph(icon, Modifier.size(19.dp), tone)
            Spacer(Modifier.width(8.dp))
        }
        Label(label.localized(), 14, tone, FontWeight.Bold, align = TextAlign.Center, maxLines = 2)
    }
}

/** The quiet action: same geometry, glass instead of light. */
@Composable
internal fun NxSecondary(
    label: Copy,
    modifier: Modifier = Modifier,
    icon: Mark? = null,
    enabled: Boolean = true,
    tag: String = "",
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) .975f else 1f, tween(110), label = "secondary press")
    Row(
        modifier.heightIn(min = 50.dp)
            .then(if (tag.isNotEmpty()) Modifier.testTag(tag) else Modifier)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Nx.cardTop, Nx.cardBottom)))
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(Nx.borderLit.copy(alpha = .55f), Nx.border, Nx.cyan.copy(alpha = .35f))),
                shape,
            )
            .then(
                if (selected) Modifier.border(2.dp, Nx.cyan.copy(alpha = .85f), shape) else Modifier,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tone = if (enabled) Nx.text else Nx.muted
        if (icon != null) {
            Glyph(icon, Modifier.size(18.dp), if (enabled) Nx.cyan else Nx.muted)
            Spacer(Modifier.width(8.dp))
        }
        Label(label.localized(), 13, tone, FontWeight.SemiBold, align = TextAlign.Center, maxLines = 2)
    }
}

/** The selection box of the reference: a rounded square that fills and glows. */
@Composable
internal fun NxCheck(
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Nx.cyan,
    tag: String = "",
    onChange: (Boolean) -> Unit,
) {
    val fill by animateColorAsState(if (checked) tint else Color.Transparent, tween(160), label = "check fill")
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier.size(TOUCH_TARGET)
            .then(if (tag.isNotEmpty()) Modifier.testTag(tag) else Modifier)
            .toggleable(checked, enabled = enabled, role = Role.Checkbox, onValueChange = onChange),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(26.dp).clip(shape)
                .drawBehind {
                    if (checked) {
                        drawRoundRect(
                            Brush.radialGradient(
                                listOf(tint.copy(alpha = .55f), Color.Transparent),
                                center, size.width * .85f,
                            ),
                            cornerRadius = CornerRadius(8.dp.toPx()),
                        )
                    }
                }
                .background(fill, shape)
                .border(
                    if (checked) 1.4.dp else 1.2.dp,
                    if (checked) Color.White.copy(alpha = .90f) else if (enabled) Nx.borderLit else Nx.border,
                    shape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Canvas(Modifier.size(15.dp)) {
                    val p = Path().apply {
                        moveTo(size.width * .16f, size.height * .53f)
                        lineTo(size.width * .42f, size.height * .78f)
                        lineTo(size.width * .86f, size.height * .22f)
                    }
                    drawPath(p, Nx.inkDeep, style = Stroke(2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
    }
}

/** iOS-shaped switch used by the confirmation cards. */
@Composable
internal fun NxSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = Nx.cyan,
    tag: String = "",
    onChange: (Boolean) -> Unit,
) {
    val shift by animateFloatAsState(if (checked) 1f else 0f, tween(200), label = "switch")
    val track by animateColorAsState(if (checked) tint else Nx.raised, tween(200), label = "switch track")
    Box(
        modifier.heightIn(min = TOUCH_TARGET)
            .then(if (tag.isNotEmpty()) Modifier.testTag(tag) else Modifier)
            .toggleable(checked, role = Role.Switch, onValueChange = onChange),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier.width(50.dp).height(28.dp).clip(RoundedCornerShape(50))
                .background(track)
                .border(1.dp, if (checked) Color.White.copy(alpha = .45f) else Nx.border, RoundedCornerShape(50)),
        )
        Box(
            Modifier
                .width(22.dp).height(22.dp)
                .graphicsLayer { translationX = (4 + shift * 20) * density }
                .clip(RoundedCornerShape(50))
                .background(Nx.text),
        )
    }
}

/** One filter chip. Selected chips carry the identity gradient. */
@Composable
internal fun NxChip(
    label: Copy,
    selected: Boolean,
    modifier: Modifier = Modifier,
    icon: Mark? = null,
    tag: String = "",
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    val fill by animateColorAsState(
        if (selected) Nx.blue.copy(alpha = .22f) else Nx.card,
        tween(180), label = "chip fill",
    )
    Row(
        modifier.heightIn(min = 44.dp)
            .then(if (tag.isNotEmpty()) Modifier.testTag(tag) else Modifier)
            .clip(shape)
            .background(fill)
            .border(
                1.dp,
                if (selected) Brush.horizontalGradient(listOf(Nx.cyan, Nx.blue))
                else Brush.horizontalGradient(listOf(Nx.border, Nx.border)),
                shape,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Glyph(icon, Modifier.size(16.dp), if (selected) Nx.cyan else Nx.muted)
        Label(label, 12, if (selected) Nx.text else Nx.muted,
            if (selected) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
    }
}

/** A labelled measurement inside a summary card. */
@Composable
internal fun NxMeasure(
    label: Copy,
    value: String?,
    modifier: Modifier = Modifier,
    tint: Color = Nx.text,
    align: TextAlign = TextAlign.Center,
) {
    Column(modifier, horizontalAlignment = if (align == TextAlign.Start) Alignment.Start else Alignment.CenterHorizontally) {
        Label(label, 11, Nx.muted, maxLines = 2, align = align)
        Spacer(Modifier.height(3.dp))
        if (value != null) {
            Label(value, 18, tint, FontWeight.Bold, maxLines = 1, align = align)
        } else {
            Unmeasured(15, align = align)
        }
    }
}

/** The hairline between two measurements. */
@Composable
internal fun NxDivider(modifier: Modifier = Modifier, height: Dp = 40.dp) {
    Box(
        modifier.height(height).width(1.dp)
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Nx.borderLit.copy(alpha = .55f), Color.Transparent),
                ),
            ),
    )
}

/** Small state badge: measured, unavailable, attention. */
@Composable
internal fun NxBadge(text: Copy, tint: Color, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = .16f))
            .border(1.dp, tint.copy(alpha = .50f), RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(Modifier.size(5.dp).clip(RoundedCornerShape(50)).background(tint))
        Label(text.localized(), 10, tint, FontWeight.SemiBold, maxLines = 1)
    }
}

/** A section label with its own glyph, used above a list of rows. */
@Composable
internal fun NxSectionLabel(title: Copy, icon: Mark = Mark.BARS, tint: Color = Nx.violet) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Glyph(icon, Modifier.size(16.dp), tint)
        Label(title.localized(), 14, Nx.text, FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
    }
}

/** The horizontal composition of the reference summary cards. */
@Composable
internal fun NxSummaryRow(
    tile: Mark,
    tint: Color,
    modifier: Modifier = Modifier,
    ring: (@Composable () -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NxTile(tile, tint, size = 52)
        content()
        ring?.invoke()
    }
}

/** Vertical gradient progress with a travelling highlight while work is live. */
@Composable
internal fun NxBar(
    value: Float,
    modifier: Modifier = Modifier,
    thickness: Int = 7,
    live: Boolean = false,
    tint: Color = Nx.cyan,
    tint2: Color = Nx.violet,
) {
    val motion = LocalPreviewMotion.current
    val shown by animateFloatAsState(
        value.coerceIn(0f, 1f),
        tween(if (motion) 700 else 0, easing = FastOutSlowInEasing),
        label = "bar",
    )
    val sweep = if (live && motion) {
        val transition = rememberInfiniteTransition(label = "bar sweep")
        transition.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(2200, easing = androidx.compose.animation.core.LinearEasing)),
            label = "bar sweep",
        )
    } else null
    Canvas(modifier.fillMaxWidth().height(thickness.dp)) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(Nx.inkDeep.copy(alpha = .9f), cornerRadius = radius)
        drawRoundRect(Nx.border.copy(alpha = .8f), cornerRadius = radius, style = Stroke(.8.dp.toPx()))
        val fill = size.width * shown
        if (fill > 0f) {
            val brush = Brush.horizontalGradient(listOf(tint2, tint, Nx.text))
            drawRoundRect(brush, size = Size(fill, size.height), cornerRadius = radius, alpha = .35f)
            drawRoundRect(Brush.horizontalGradient(listOf(tint2, tint)), size = Size(fill, size.height), cornerRadius = radius)
            if (sweep != null) {
                val travel = sweep.value * fill
                drawRoundRect(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = .45f), Color.Transparent),
                        startX = travel - size.height, endX = travel + size.height,
                    ),
                    size = Size(fill, size.height), cornerRadius = radius,
                )
            }
        }
    }
}
