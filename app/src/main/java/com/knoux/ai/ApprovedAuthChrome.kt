package com.knoux.ai

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knoux.ai.visual.InstrumentFog

// ─────────────────────────────────────────────────────────────────────────────
// The owner's approved identity vocabulary (references 2026-11-16).
//
// These surfaces are a skin over real controls, never a mock: every gradient
// button below is a clickable role=Button, every field is a real BasicTextField,
// and the biometric row still opens the platform prompt. Nothing here reports a
// success state on its own.
// ─────────────────────────────────────────────────────────────────────────────

/** Near-black field the approved screens sit on. */
internal val AwInk = Color(0xFF07030F)
internal val AwViolet = Color(0xFF8938FF)
internal val AwMagenta = Color(0xFFE62BFF)
internal val AwCyan = Color(0xFF00DEFF)
internal val AwBlue = Color(0xFF1968FF)
internal val AwGold = Color(0xFFF5B942)

/** The single call-to-action gradient: magenta → violet → blue → cyan. */
internal val AwCtaBrush = Brush.horizontalGradient(
    listOf(AwMagenta, Color(0xFFC22BFF), AwViolet, AwBlue, AwCyan)
)

private val AwPanelBrush = Brush.verticalGradient(
    listOf(Color(0xFF1C1044), Color(0xFF100933), Color(0xFF0B0B2A))
)

private val AwPanelBorder = Brush.linearGradient(
    listOf(AwViolet.copy(alpha = .85f), AwBlue.copy(alpha = .55f), AwCyan.copy(alpha = .75f))
)

private val AwFieldBrush = Brush.horizontalGradient(
    listOf(Color(0xFF241352), Color(0xFF140A33), Color(0xFF0D1336))
)

private val AwFieldBorder = Brush.horizontalGradient(
    listOf(AwViolet.copy(alpha = .72f), AwBlue.copy(alpha = .48f), AwCyan.copy(alpha = .58f))
)

/** The founder signature face (Great Vibes, OFL — see docs/design/fonts). */
internal val ScriptFace = FontFamily(Font(R.font.great_vibes, FontWeight.Normal))

/**
 * The room every approved identity screen stands in: deep ink, the observatory
 * atmosphere, and four broad ribbons that echo the reference artwork without
 * encoding any state.
 */
@Composable
internal fun ApprovedSky(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(AwInk)) {
        NativeAtmosphere(Modifier.fillMaxSize())
        Canvas(Modifier.fillMaxSize()) { drawRibbons() }
        content()
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRibbons() {
    val w = size.width
    val h = size.height
    if (w < 1f || h < 1f) return

    // Top-left magenta sweep.
    val topLeft = Path().apply {
        moveTo(-w * .18f, -h * .02f)
        cubicTo(w * .10f, h * .10f, w * .26f, h * .00f, w * .58f, h * .055f)
    }
    drawPath(
        topLeft,
        Brush.horizontalGradient(
            listOf(AwMagenta.copy(alpha = .30f), AwViolet.copy(alpha = .20f), Color.Transparent),
            startX = 0f, endX = w * .60f,
        ),
        style = Stroke(w * .11f, cap = StrokeCap.Round),
    )
    drawPath(
        topLeft,
        Brush.horizontalGradient(
            listOf(Color(0xFFFF9BFF).copy(alpha = .55f), AwViolet.copy(alpha = .25f), Color.Transparent),
            startX = 0f, endX = w * .58f,
        ),
        style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round),
    )

    // Top-right cyan sweep.
    val topRight = Path().apply {
        moveTo(w * 1.18f, -h * .01f)
        cubicTo(w * .88f, h * .12f, w * .74f, h * .02f, w * .48f, h * .10f)
    }
    drawPath(
        topRight,
        Brush.horizontalGradient(
            listOf(Color.Transparent, AwBlue.copy(alpha = .18f), AwCyan.copy(alpha = .26f)),
            startX = w * .45f, endX = w,
        ),
        style = Stroke(w * .09f, cap = StrokeCap.Round),
    )
    drawPath(
        topRight,
        Brush.horizontalGradient(
            listOf(Color.Transparent, AwCyan.copy(alpha = .50f)),
            startX = w * .50f, endX = w,
        ),
        style = Stroke(1.4.dp.toPx(), cap = StrokeCap.Round),
    )

    // Bottom corners: quieter, so the CTA stays the brightest thing on screen.
    val bottom = Path().apply {
        moveTo(-w * .05f, h * .97f)
        cubicTo(w * .22f, h * .90f, w * .34f, h * 1.02f, w * .62f, h * .955f)
        cubicTo(w * .78f, h * .92f, w * .92f, h * .955f, w * 1.05f, h * .925f)
    }
    drawPath(
        bottom,
        Brush.horizontalGradient(
            listOf(AwCyan.copy(alpha = .16f), AwViolet.copy(alpha = .16f), AwMagenta.copy(alpha = .22f)),
            startX = 0f, endX = w,
        ),
        style = Stroke(w * .10f, cap = StrokeCap.Round),
    )
    drawPath(
        bottom,
        Brush.horizontalGradient(
            listOf(AwCyan.copy(alpha = .34f), Color(0xFFB04BFF).copy(alpha = .30f), AwMagenta.copy(alpha = .42f)),
            startX = 0f, endX = w,
        ),
        style = Stroke(1.3.dp.toPx(), cap = StrokeCap.Round),
    )
}

// ── Header lockup + Pro chip ─────────────────────────────────────────────────

/** The gold Pro capsule, crown first, exactly as the approved header draws it. */
@Composable
internal fun ProPill(onClick: (() -> Unit)? = null, tag: String = "pro-pill") {
    val shape = RoundedCornerShape(22.dp)
    val arabic = LocalArabic.current
    val description = remember(arabic) {
        KnouxStrings.get(R.string.k_pro_chip_description, arabic)
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            Modifier
                .heightIn(min = 44.dp)
                .clip(shape)
                .background(
                    Brush.horizontalGradient(listOf(Color(0xFF2A1B05), Color(0xFF171006))),
                    shape,
                )
                .border(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(AwGold, Color(0xFFFFE6A3), AwGold.copy(alpha = .55f))
                    ),
                    shape,
                )
                .then(
                    if (onClick != null) Modifier
                        .clickable(role = Role.Button, onClick = onClick)
                        .testTag(tag)
                        .semantics {
                            contentDescription = description
                        }
                    else Modifier
                )
                .padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Glyph(Mark.CROWN, Modifier.size(15.dp), AwGold, true)
            Text(
                "PRO",
                color = AwGold,
                fontSize = 12.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = KnouxFont,
            )
        }
    }
}

/** The official lockup, optionally flanked by the reference hairlines. */
@Composable
internal fun BrandLockupRow(size: Int = 24, dashes: Boolean = false, tag: String? = null) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = if (tag != null) Modifier.testTag(tag) else Modifier,
        ) {
            if (dashes) DashRule()
            KnouxWordmark(size = size, withSignature = true, glow = true)
            if (dashes) DashRule()
        }
    }
}

@Composable
private fun DashRule() {
    Box(
        Modifier
            .width(30.dp)
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, Mist.copy(alpha = .55f), Color.Transparent)
                )
            )
    )
}

/** Header used by the approved welcome and returning surfaces. */
@Composable
internal fun ApprovedAuthHeader(
    onPro: () -> Unit,
    onSettings: () -> Unit,
    showWordmark: Boolean = true,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showWordmark) {
            BrandLockupRow(size = 21, tag = "auth-wordmark")
        }
        Spacer(Modifier.weight(1f))
        ProPill(onClick = onPro)
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onSettings)
                .testTag("auth-settings"),
            contentAlignment = Alignment.Center,
        ) {
            Glyph(Mark.GEAR, Modifier.size(22.dp), Chalk.copy(alpha = .92f), false)
        }
    }
}

/** Centred lockup for the verify surface, which carries no header controls. */
@Composable
internal fun CenteredBrand(size: Int = 26) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        BrandLockupRow(size = size, dashes = true, tag = "auth-centered-brand")
    }
}

// ── Glass cards ─────────────────────────────────────────────────────────────

/** The big translucent panel that holds the approved credential forms. */
@Composable
internal fun ApprovedPanel(
    modifier: Modifier = Modifier,
    radius: Int = 26,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius.dp)
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AwPanelBrush, shape)
            .border(1.dp, AwPanelBorder, shape)
    ) {
        Column(Modifier.fillMaxWidth().padding(11.dp), content = content)
    }
}

/** Shield-plus-copy banner: "Private by design — …". */
@Composable
internal fun InfoBanner(icon: Mark, lead: String, rest: String) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF2B1558).copy(alpha = .94f), Color(0xFF161046).copy(alpha = .92f), Color(0xFF0D1B3E).copy(alpha = .90f))
                ),
                shape,
            )
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(AwViolet.copy(alpha = .85f), AwBlue.copy(alpha = .55f), AwCyan.copy(alpha = .55f))
                ),
                shape,
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(listOf(Color(0xFF6B32D8), Color(0xFF2B4BC8))),
                    RoundedCornerShape(14.dp),
                )
                .border(1.dp, Color.White.copy(alpha = .22f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Glyph(icon, Modifier.size(23.dp), Chalk, true)
        }
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = Chalk, fontWeight = FontWeight.Bold)) { append(lead) }
                if (rest.isNotEmpty()) {
                    append(" ")
                    withStyle(SpanStyle(color = Mist.copy(alpha = .88f), fontWeight = FontWeight.Normal)) {
                        append(rest)
                    }
                }
            },
            modifier = Modifier.weight(1f),
            style = TextStyle(
                fontFamily = KnouxFont,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = Mist,
            ),
        )
    }
}

/** Segmented Sign In / Create Account control. */
@Composable
internal fun AuthTabs(selected: Int, onSelect: (Int) -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = .055f), shape)
            .border(1.dp, AwViolet.copy(alpha = .30f), shape)
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SegmentTab(0, selected, onSelect, KnouxStrings.get(R.string.k_sign_in, LocalArabic.current), "auth-tab-signin")
        SegmentTab(1, selected, onSelect, KnouxStrings.get(R.string.k_create_account, LocalArabic.current), "auth-tab-create")
    }
}

@Composable
private fun RowScope.SegmentTab(index: Int, selected: Int, onSelect: (Int) -> Unit, label: String, tag: String) {
    val shape = RoundedCornerShape(23.dp)
    val active = selected == index
    Box(
        Modifier
            .weight(1f)
            .height(46.dp)
            .clip(shape)
            .then(
                if (active) Modifier
                    .background(
                        Brush.horizontalGradient(listOf(AwViolet, Color(0xFF6A5CFF), AwBlue)),
                        shape,
                    )
                else Modifier
            )
            .clickable(role = Role.Button) { onSelect(index) }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (active) Chalk else Chalk.copy(alpha = .68f),
            fontSize = 14.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
            fontFamily = KnouxFont,
            maxLines = 1,
        )
    }
}

// ── Fields, actions, dividers ───────────────────────────────────────────────

/** Approved credential field: icon, hairline, real text, optional trailing eye. */
@Composable
internal fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    mark: Mark,
    placeholder: String,
    tag: String,
    keyboard: KeyboardType,
    obscure: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(AwFieldBrush, shape)
            .border(1.dp, AwFieldBorder, shape)
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Glyph(mark, Modifier.size(19.dp), Mist.copy(alpha = .90f), false)
        Box(Modifier.width(1.dp).height(22.dp).background(Mist.copy(alpha = .22f)))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Label(placeholder, 13, Mist.copy(alpha = .58f), maxLines = 1)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth().testTag(tag),
                singleLine = true,
                textStyle = TextStyle(color = Chalk, fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = KnouxFont),
                cursorBrush = SolidColor(AwCyan),
                keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = ImeAction.Next),
                visualTransformation = if (obscure) PasswordVisualTransformation() else VisualTransformation.None,
            )
        }
        trailing?.invoke()
    }
}

/** Full-width gradient CTA: label and a real trailing arrow. */
@Composable
internal fun ApprovedCta(
    label: String,
    onClick: () -> Unit,
    tag: String,
    leading: Mark? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .975f else 1f, tween(120), label = "$tag scale")
    val flip = if (LocalArabic.current) 180f else 0f
    val pulse: State<Float>? = if (LocalPreviewMotion.current) {
        val transition = rememberInfiniteTransition(label = "$tag pulse")
        transition.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "$tag glow",
        )
    } else null
    val shape = RoundedCornerShape(30.dp)

    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 55.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .buttonBloom(true, pulse)
            .clip(shape)
            .background(AwCtaBrush, shape)
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(Color.White.copy(alpha = .42f), Color.White.copy(alpha = .12f), Color.White.copy(alpha = .40f))
                ),
                shape,
            )
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .testTag(tag)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Glyph(leading, Modifier.size(20.dp), Chalk, true)
            Spacer(Modifier.width(10.dp))
        }
        Text(
            label,
            color = Chalk,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = KnouxFont,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.width(9.dp))
        Glyph(
            Mark.NEXT,
            Modifier.size(18.dp).graphicsLayer { rotationZ = flip },
            Chalk,
            true,
        )
    }
}

/** Thin rule pair with a small label between, e.g. "Or continue with". */
@Composable
internal fun DividerLabel(label: String, tint: Color = Mist.copy(alpha = .70f)) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.weight(1f).height(1.dp).background(
                Brush.horizontalGradient(listOf(Color.Transparent, Mist.copy(alpha = .35f)))
            )
        )
        Label(label, 12, tint, maxLines = 1)
        Box(
            Modifier.weight(1f).height(1.dp).background(
                Brush.horizontalGradient(listOf(Mist.copy(alpha = .35f), Color.Transparent))
            )
        )
    }
}

/** Full-width glass action row: provider / password / fingerprint. */
@Composable
internal fun GlassActionRow(
    mark: Mark,
    tint: Color,
    label: String,
    tag: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    trailing: Boolean = true,
) {
    val shape = RoundedCornerShape(26.dp)
    val flip = if (LocalArabic.current) 180f else 0f
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF241245).copy(alpha = .92f), Color(0xFF130A2C).copy(alpha = .94f), Color(0xFF0D1332).copy(alpha = .92f))
                ),
                shape,
            )
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(tint.copy(alpha = if (enabled) .62f else .22f), Color.White.copy(alpha = .10f), AwViolet.copy(alpha = .38f))
                ),
                shape,
            )
            .clickable(role = Role.Button, enabled = enabled, onClick = onClick)
            .testTag(tag)
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        if (mark == Mark.GOOGLE) GoogleG(21) else Glyph(mark, Modifier.size(21.dp), if (enabled) tint else InstrumentFog, enabled)
        Box(Modifier.width(1.dp).height(20.dp).background(Color.White.copy(alpha = .22f)))
        Label(label, 13, Chalk.copy(alpha = if (enabled) .92f else .45f), FontWeight.SemiBold, Modifier.weight(1f), maxLines = 1)
        if (trailing) {
            Glyph(
                Mark.NEXT,
                Modifier.size(17.dp).graphicsLayer { rotationZ = flip },
                Mist.copy(alpha = .78f),
            )
        }
    }
}

/** Two equal provider tiles (Google / Passkey) as in the approved welcome. */
@Composable
internal fun ProviderTiles(
    onGoogle: () -> Unit,
    onPasskey: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ProviderTile(Mark.GOOGLE, AwBlue, (Copy(R.string.k_google)).localized(), "auth-google", onGoogle)
        ProviderTile(Mark.KEY, AwViolet, (Copy(R.string.k_passkey)).localized(), "auth-passkey", onPasskey)
    }
}

@Composable
private fun RowScope.ProviderTile(mark: Mark, tint: Color, label: String, tag: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .weight(1f)
            .heightIn(min = 50.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(listOf(tint.copy(alpha = .16f), Color(0xFF0D0A24).copy(alpha = .94f))),
                shape,
            )
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(tint.copy(alpha = .55f), Color.White.copy(alpha = .10f))),
                shape,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (mark == Mark.GOOGLE) GoogleG(20) else Glyph(mark, Modifier.size(19.dp), tint, true)
        Spacer(Modifier.width(9.dp))
        Label(label, 13, Chalk.copy(alpha = .92f), FontWeight.SemiBold, maxLines = 1)
    }
}

/** Quiet inline footnote, with the blue information dot of the reference. */
@Composable
internal fun InfoFootnote(text: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Canvas(Modifier.size(15.dp).padding(top = 1.dp)) {
            drawCircle(AwCyan.copy(alpha = .90f), size.minDimension / 2f)
            drawCircle(AwInk, size.minDimension * .055f, Offset(size.width / 2f, size.height * .30f))
            drawCircle(AwInk, size.minDimension * .055f, Offset(size.width / 2f, size.height * .64f))
        }
        Label(text, 12, Mist.copy(alpha = .78f), modifier = Modifier.weight(1f))
    }
}

/** Three-step dots. `active` is the step the user is really on. */
@Composable
internal fun StepDots(active: Int, tint: Color = AwMagenta) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            val on = index == active
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (on) 9.dp else 6.dp)
                    .then(
                        if (on) Modifier.background(
                            Brush.radialGradient(listOf(tint, tint.copy(alpha = .35f))),
                            CircleShape,
                        ) else Modifier.background(Color.White.copy(alpha = .22f), CircleShape)
                    )
                    .testTag("auth-dot-$index")
            )
        }
    }
}

/** The fingerprint medallion with the green success badge of the reference. */
@Composable
internal fun FingerprintMedallion(size: Int = 128, motion: Boolean = LocalPreviewMotion.current) {
    val breath = if (motion) {
        val transition = rememberInfiniteTransition(label = "fingerprint breath")
        transition.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "fingerprint glow",
        ).value
    } else .45f

    Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            val r = this.size.minDimension * .37f
            drawCircle(
                Brush.radialGradient(
                    listOf(AwCyan.copy(alpha = .16f + .10f * breath), AwViolet.copy(alpha = .16f), Color.Transparent),
                    c, r * 2.1f,
                ),
                radius = r * 2.1f, center = c,
            )
            drawCircle(
                Brush.radialGradient(
                    0f to Color(0xFF2A1A63).copy(alpha = .95f),
                    .62f to Color(0xFF160C3C).copy(alpha = .96f),
                    1f to Color(0xFF07030F),
                    center = Offset(c.x - r * .28f, c.y - r * .32f),
                    radius = r * 1.35f,
                ),
                radius = r, center = c,
            )
            drawCircle(
                Brush.sweepGradient(listOf(AwCyan, AwBlue, AwViolet, AwMagenta, AwCyan), c),
                radius = r, center = c, style = Stroke(1.6.dp.toPx()),
            )
            drawCircle(Color.White.copy(alpha = .28f), radius = r * .78f, center = c, style = Stroke(1.dp.toPx()))
            // Mixed light: cyan shoulder on the right, violet on the left.
            drawCircle(
                Brush.radialGradient(
                    listOf(AwCyan.copy(alpha = .30f), Color.Transparent),
                    Offset(c.x + r * .55f, c.y - r * .35f), r * .75f,
                ),
                radius = r * .75f, center = Offset(c.x + r * .55f, c.y - r * .35f),
            )
        }
        Glyph(Mark.FINGERPRINT, Modifier.size((size * .40f).dp), Chalk.copy(alpha = .94f), true)
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 8.dp)
                .size((size * .30f).dp)
                .clip(CircleShape)
                .background(Color(0xFF19D9A0), CircleShape)
                .border(3.dp, AwInk, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Glyph(Mark.CHECK, Modifier.size((size * .16f).dp), AwInk, false)
        }
    }
}

/** Masked address inside the verify chip. Never invents an address. */
internal fun maskEmail(raw: String): String {
    val at = raw.indexOf('@')
    if (at <= 0) return raw.trim()
    val name = raw.substring(0, at)
    val domain = raw.substring(at)
    val visible = name.take(2)
    return visible + "***" + domain
}

@Composable
internal fun EmailChip(email: String) {
    if (email.isBlank()) return
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier
            .clip(shape)
            .background(
                Brush.horizontalGradient(listOf(Color(0xFF2A1552).copy(alpha = .95f), Color(0xFF171344).copy(alpha = .92f))),
                shape,
            )
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(AwViolet.copy(alpha = .80f), AwBlue.copy(alpha = .50f), AwCyan.copy(alpha = .60f))),
                shape,
            )
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Glyph(Mark.MAIL, Modifier.size(17.dp), AwViolet, false)
        Text(
            maskEmail(email),
            color = Chalk.copy(alpha = .94f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = KnouxFont,
            maxLines = 1,
        )
    }
}

// ── Founder plate (approved onboarding reference) ───────────────────────────

@Composable
internal fun FounderPlate() {
    val shape = RoundedCornerShape(24.dp)
    val motion = LocalPreviewMotion.current
    val sheen = if (motion) {
        val transition = rememberInfiniteTransition(label = "founder plate")
        transition.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(6200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "founder sheen",
        ).value
    } else .35f

    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF251047), Color(0xFF140830), Color(0xFF0C0622))
                ),
                shape,
            )
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(
                Brush.radialGradient(
                    listOf(AwViolet.copy(alpha = .22f + .06f * sheen), Color.Transparent),
                    Offset(size.width * .16f, size.height * .10f), size.width * .55f,
                ),
                radius = size.width * .55f,
                center = Offset(size.width * .16f, size.height * .10f),
            )
            drawCircle(
                Brush.radialGradient(
                    listOf(AwMagenta.copy(alpha = .12f + .05f * sheen), Color.Transparent),
                    Offset(size.width * .92f, size.height * .95f), size.width * .50f,
                ),
                radius = size.width * .50f,
                center = Offset(size.width * .92f, size.height * .95f),
            )
            drawRoundRect(
                Brush.horizontalGradient(
                    listOf(AwGold.copy(alpha = .14f), Color.Transparent, AwCyan.copy(alpha = .10f)),
                ),
                cornerRadius = CornerRadius(24.dp.toPx()),
                alpha = .9f,
            )
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 15.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier.weight(1f).height(1.dp).background(
                        Brush.horizontalGradient(listOf(Color.Transparent, AwGold.copy(alpha = .70f)))
                    )
                )
                Text(
                    (Copy(R.string.k_founded_by)).localized(),
                    color = AwGold.copy(alpha = .95f),
                    fontSize = 10.sp,
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = KnouxFont,
                    maxLines = 1,
                )
                Box(
                    Modifier.weight(1f).height(1.dp).background(
                        Brush.horizontalGradient(listOf(AwGold.copy(alpha = .70f), Color.Transparent))
                    )
                )
            }

            Spacer(Modifier.height(2.dp))

            Text(
                (Copy(R.string.k_founder_name_script)).localized(),
                color = Chalk,
                fontSize = 34.sp,
                lineHeight = 40.sp,
                fontFamily = ScriptFace,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )

            Canvas(Modifier.width(150.dp).height(9.dp)) {
                val path = Path().apply {
                    moveTo(4f, size.height * .42f)
                    cubicTo(
                        size.width * .34f, size.height * .02f,
                        size.width * .58f, size.height * .95f,
                        size.width - 4f, size.height * .34f,
                    )
                }
                drawPath(
                    path,
                    Brush.horizontalGradient(listOf(AwMagenta, AwViolet, AwCyan)),
                    style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round),
                )
            }

            Spacer(Modifier.height(2.dp))

            Text(
                (Copy(R.string.k_founder_role)).localized(),
                color = Mist.copy(alpha = .86f),
                fontSize = 10.sp,
                letterSpacing = 2.2.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = KnouxFont,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(AwGold.copy(alpha = .10f), AwViolet.copy(alpha = .55f), AwCyan.copy(alpha = .45f), AwGold.copy(alpha = .10f))
                )
            )
    )
}

/** Outlined pill used by the returning surface's "Sign Up" accent. */
@Composable
internal fun AccentTextLink(label: String, tint: Color, onClick: () -> Unit, tag: String) {
    Text(
        label,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(tag)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        color = tint,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = KnouxFont,
        textDecoration = TextDecoration.Underline,
        maxLines = 1,
    )
}

/** Italic display headline with the second word on the brand spectrum. */
@Composable
internal fun ApprovedHeadline(
    lead: String,
    accent: String,
    size: Int = 31,
    align: TextAlign = TextAlign.Center,
) {
    val gradient = Brush.horizontalGradient(listOf(Color(0xFFFF9BFF), AwMagenta, Color(0xFFB26BFF)))
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = Chalk)) { append(lead) }
            if (accent.isNotBlank()) {
                append(" ")
                withStyle(SpanStyle(brush = gradient)) { append(accent) }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        style = TextStyle(
            fontFamily = if (LocalArabic.current) KnouxFont else BrandFace,
            fontSize = size.sp,
            lineHeight = (size * 1.16f).sp,
            fontStyle = if (LocalArabic.current) FontStyle.Normal else FontStyle.Italic,
            fontWeight = FontWeight.ExtraBold,
            textAlign = align,
        ),
    )
}

/** Small italic caption under a headline. */
@Composable
internal fun ApprovedSubtitle(text: String, size: Int = 13) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
        color = Mist.copy(alpha = .86f),
        fontSize = size.sp,
        lineHeight = (size * 1.35f).sp,
        fontFamily = KnouxFont,
        textAlign = TextAlign.Center,
    )
}
