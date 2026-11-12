package com.knoux.ai

import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.delay

/** The four visible states of the KNOUX identity gateway. */
internal enum class AuthStage { LANDING, WELCOME, VERIFYING, VERIFIED, UNAVAILABLE }

/**
 * Cinematic identity gateway.
 *
 * The biometric path is genuine: a real [BiometricPrompt] is launched and the
 * VERIFIED state is only entered from its success callback. When the device
 * cannot offer a usable biometric, the honest UNAVAILABLE state is shown
 * instead. Passkey and Google remain provider-configuration placeholders and
 * never fabricate a session.
 */
@Composable
internal fun AuthGateway(
    forcedStage: AuthStage? = null,
    reducedMotion: Boolean = false,
    onAuthenticated: () -> Unit,
    onGuest: () -> Unit,
    onLanguage: (Boolean) -> Unit,
    onMotion: (Boolean) -> Unit,
    reduced: Boolean
) {
    val context = LocalContext.current
    val motion = LocalPreviewMotion.current && !reducedMotion
    var stage by remember { mutableStateOf(forcedStage ?: AuthStage.WELCOME) }
    var sheet by remember { mutableStateOf<String?>(null) }

    fun available(): Boolean =
        BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS

    fun launchPrompt() {
        val activity = context as? FragmentActivity
        if (activity == null || !available()) {
            stage = AuthStage.UNAVAILABLE
            return
        }
        stage = AuthStage.VERIFYING
        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    stage = AuthStage.VERIFIED
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    stage = when (errorCode) {
                        BiometricPrompt.ERROR_HW_UNAVAILABLE,
                        BiometricPrompt.ERROR_HW_NOT_PRESENT,
                        BiometricPrompt.ERROR_NO_BIOMETRICS,
                        BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> AuthStage.UNAVAILABLE
                        else -> AuthStage.WELCOME
                    }
                }
            })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Verify your identity")
            .setSubtitle("Unlock KNOUX AI with your fingerprint")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
        runCatching { prompt.authenticate(info) }.onFailure { stage = AuthStage.UNAVAILABLE }
    }

    // The VERIFIED state hands over to the application on its own. A forced
    // debug stage stays on screen so it can be captured deterministically.
    LaunchedEffect(stage, forcedStage) {
        if (stage == AuthStage.VERIFIED && forcedStage == null) {
            delay(1500)
            onAuthenticated()
        }
    }

    BackHandler(enabled = stage != AuthStage.WELCOME && forcedStage == null) {
        stage = AuthStage.WELCOME
    }

    AuthBackdrop {
        when (stage) {
            AuthStage.LANDING -> LandingStage(motion = motion, onEnter = { stage = AuthStage.WELCOME })
            AuthStage.WELCOME -> WelcomeStage(motion = motion, onUnlock = { launchPrompt() },
                onPasskey = { sheet = "passkey" }, onGoogle = { sheet = "google" }, onGuest = onGuest,
                onPrivacy = { sheet = "privacy" })
            AuthStage.VERIFYING -> VerifyStage(motion = motion, onCancel = { stage = AuthStage.WELCOME })
            AuthStage.VERIFIED -> VerifiedStage(motion = motion)
            AuthStage.UNAVAILABLE -> UnavailableStage(motion = motion,
                onBack = { stage = AuthStage.WELCOME },
                onSettings = { sheet = "settings" },
                onPasskey = { sheet = "passkey" }, onGoogle = { sheet = "google" }, onGuest = onGuest)
        }
    }

    sheet?.let { kind ->
        InfoSheet(
            when (kind) {
                "passkey" -> ("Use a passkey" ar "استخدم مفتاح مرور")
                "google" -> ("Continue with Google" ar "المتابعة عبر Google")
                "settings" -> ("Preview settings" ar "إعدادات المعاينة")
                else -> ("Privacy information" ar "معلومات الخصوصية")
            }, { sheet = null }) {
            when (kind) {
                "passkey" -> {
                    Label(("Passkey sign-in is not configured in this preview build."
                        ar "تسجيل الدخول بمفتاح المرور غير مُهيّأ في نسخة المعاينة هذه.").localized(), 14, Mist)
                    Label(("No credential provider, key or account session is created here."
                        ar "لا يتم هنا إنشاء أي مزوّد بيانات أو مفتاح أو جلسة حساب.").localized(), 13, Mist)
                }
                "google" -> {
                    Label(("Google sign-in is not configured in this preview build."
                        ar "تسجيل الدخول عبر Google غير مُهيّأ في نسخة المعاينة هذه.").localized(), 14, Mist)
                    Label(("No OAuth client, client secret or account session is created here."
                        ar "لا يتم هنا إنشاء أي عميل OAuth أو سر أو جلسة حساب.").localized(), 13, Mist)
                }
                "settings" -> {
                    Label(Disclosure.localized(), 13, Mist)
                    val ar = LocalArabic.current
                    AdaptiveActions(Action("English" ar "الإنجليزية", Mark.CHECK),
                        Action("العربية" ar "العربية", Mark.CHECK),
                        firstSelected = !ar, secondSelected = ar,
                        onFirst = { onLanguage(false) }, onSecond = { onLanguage(true) })
                    AccentButton(Action(if (reduced) "Enable decorative motion" ar "تشغيل الحركة" else "Reduce motion" ar "تقليل الحركة",
                        Mark.RADAR), Modifier.fillMaxWidth(), primary = false, tag = "motion-toggle") { onMotion(!reduced) }
                }
                else -> {
                    Label(("KNOUX AI can read device information only after you grant permission. "
                        + "This preview performs no device scan and sends no sample data anywhere."
                        ar "يمكن لـ KNOUX AI قراءة معلومات الجهاز فقط بعد منحك الإذن. "
                        + "لا تُجري هذه المعاينة أي فحص للجهاز ولا ترسل أي بيانات نموذجية.").localized(), 13, Mist)
                }
            }
            Label(Disclosure.localized(), 12, Mist)
        }
    }
}

/** Full-screen hexagonal grid backdrop for every authentication state. */
@Composable
private fun AuthBackdrop(content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition(label = "hex-bg")
    val hexPulse = if (LocalPreviewMotion.current) transition.animateFloat(0f, 1f,
        infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "hex-pulse") else null
    val hexShift = if (LocalPreviewMotion.current) transition.animateFloat(0f, 1f,
        infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "hex-shift") else null

    Box(Modifier.fillMaxSize().background(Color(0xff02000a))) {
        // Hex grid canvas covering full screen
        Canvas(Modifier.fillMaxSize()) {
            val pulse = hexPulse?.value ?: 0.5f
            val shift = hexShift?.value ?: 0f
            val hexR = size.width * 0.075f          // hex circumradius
            val hexW = hexR * 1.732f                // width = √3 * r
            val hexH = hexR * 2f
            val cols = (size.width / hexW).toInt() + 3
            val rows = (size.height / (hexH * 0.75f)).toInt() + 3

            // Animated offset
            val animOffsetX = (shift * hexW) % hexW
            val animOffsetY = (shift * hexH * 0.5f) % (hexH * 0.75f)

            for (row in -1..rows) {
                for (col in -1..cols) {
                    val offsetX = if (row % 2 == 0) 0f else hexW * 0.5f
                    val cx = col * hexW + offsetX - animOffsetX
                    val cy = row * hexH * 0.75f - animOffsetY

                    // Distance from screen center for glow effect
                    val dx = cx - size.width * 0.5f
                    val dy = cy - size.height * 0.4f
                    val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                    val maxDist = size.width * 0.9f
                    val proximity = (1f - (dist / maxDist).coerceIn(0f, 1f))

                    // Base alpha for all hexagons
                    val baseAlpha = 0.06f + proximity * 0.18f * (0.7f + pulse * 0.3f)

                    // Draw hex border
                    val hexPath = Path().apply {
                        for (i in 0..5) {
                            val a = (60f * i - 30f) * Math.PI.toFloat() / 180f
                            val px = cx + hexR * kotlin.math.cos(a)
                            val py = cy + hexR * kotlin.math.sin(a)
                            if (i == 0) moveTo(px, py) else lineTo(px, py)
                        }
                        close()
                    }

                    // Color varies by position creating a cyan-to-violet gradient field
                    val hue = (col + row * 0.5f) / (cols + rows * 0.5f)
                    val hexColor = when {
                        hue < 0.33f -> Color(0xff00f5ff).copy(alpha = baseAlpha)
                        hue < 0.66f -> Color(0xffb040ff).copy(alpha = baseAlpha * 0.9f)
                        else -> Color(0xffff00ee).copy(alpha = baseAlpha * 0.7f)
                    }

                    drawPath(hexPath, hexColor, style = Stroke(1.2f))

                    // Highlight brightest hexagons near center
                    if (proximity > 0.6f) {
                        val fillAlpha = (proximity - 0.6f) * 0.15f * (0.5f + pulse * 0.5f)
                        drawPath(hexPath, hexColor.copy(alpha = fillAlpha))
                    }
                }
            }

            // Central radial glow overlay
            val center = Offset(size.width * 0.5f, size.height * 0.38f)
            drawCircle(Brush.radialGradient(listOf(
                Color(0xff6600ff).copy(alpha = 0.35f * (0.7f + pulse * 0.3f)),
                Color(0xff3300aa).copy(alpha = 0.20f),
                Color.Transparent), center, size.width * 0.75f), size.width * 0.75f, center)
            drawCircle(Brush.radialGradient(listOf(
                Color(0xff00f5ff).copy(alpha = 0.12f * (0.6f + pulse * 0.4f)),
                Color.Transparent), Offset(size.width * 0.8f, size.height * 0.1f), size.width * 0.5f),
                size.width * 0.5f, Offset(size.width * 0.8f, size.height * 0.1f))
            drawCircle(Brush.radialGradient(listOf(
                Color(0xffff00ee).copy(alpha = 0.10f * (0.6f + pulse * 0.4f)),
                Color.Transparent), Offset(size.width * 0.15f, size.height * 0.85f), size.width * 0.45f),
                size.width * 0.45f, Offset(size.width * 0.15f, size.height * 0.85f))
        }
        content()
    }
}

/** Brand lockup used by the welcome and verified states. */
@Composable
private fun AuthBrandLockup(previewBadge: Boolean) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Glyph(Mark.K, Modifier.size(30.dp))
        Spacer(Modifier.width(4.dp))
        Label("knoux", 20, weight = FontWeight.ExtraBold)
        Label(" ai", 20, Pink, FontWeight.ExtraBold)
        if (previewBadge) {
            Spacer(Modifier.width(12.dp))
            Box(Modifier.width(1.dp).height(22.dp).background(Color.White.copy(alpha = .28f)))
            Spacer(Modifier.width(12.dp))
            Row(Modifier.heightIn(min = 28.dp).clip(RoundedCornerShape(14.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xff2a1c08), Color(0xff150d24))), RoundedCornerShape(14.dp))
                .border(1.dp, Gold.copy(alpha = .85f), RoundedCornerShape(14.dp))
                .padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(7.dp).background(Gold, RoundedCornerShape(4.dp)))
                Label("PREVIEW", 10, Gold, FontWeight.Bold)
            }
        }
    }
}

/** Title treatment of the authentication states. */
@Composable
private fun AuthTitle(lead: Copy, accent: Copy, subtitle: Copy, splitAccent: Boolean = false) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val gradient = Brush.verticalGradient(
            listOf(Color(0xffffa3ff), Color(0xffe35bff), Color(0xffb93bff)))
        val styled = buildAnnotatedString {
            withStyle(SpanStyle(color = Chalk)) { append(lead.localized()) }
            if (splitAccent) {
                withStyle(SpanStyle(brush = gradient)) { append("\n"); append(accent.localized()) }
            } else {
                withStyle(SpanStyle(brush = gradient)) {
                    append(" ")
                    append(accent.localized())
                }
            }
        }
        Text(styled, Modifier.fillMaxWidth(), style = TextStyle(
            fontSize = 30.sp, lineHeight = 35.sp, fontFamily = DisplayFace, fontWeight = FontWeight.ExtraBold,
            fontStyle = FontStyle.Italic, textAlign = TextAlign.Center))
        Label(subtitle.localized(), 13, Mist, modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            align = TextAlign.Center)
    }
}

/**
 * Circular fingerprint orb with realistic scan lines — matches reference design.
 * Decorative only — real auth via BiometricPrompt.
 */
@Composable
private fun FingerprintOrb(
    diameter: Int, stage: AuthStage, motion: Boolean, modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .95f else 1f, tween(120), label = "orb press")
    val transition = rememberInfiniteTransition(label = "orb")
    val spin = if (motion) transition.animateFloat(0f, 360f,
        infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "ring spin") else null
    val pulse = if (motion) transition.animateFloat(0f, 1f,
        infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse") else null
    val scanSweep = if (motion && stage == AuthStage.VERIFYING) transition.animateFloat(0f, 1f,
        infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "scan") else null

    Box(modifier.size(diameter.dp).graphicsLayer { scaleX = scale; scaleY = scale },
        contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val cx = size.width / 2f
            val cy = size.height / 2f
            val breath = pulse?.value ?: 0.6f

            // Outer glow atmosphere
            drawCircle(Brush.radialGradient(listOf(
                Pink.copy(alpha = .30f * (0.6f + breath * 0.4f)),
                Color(0xff6600ff).copy(alpha = .18f),
                Color.Transparent), Offset(cx, cy), r * 1.3f), r * 1.3f, Offset(cx, cy))

            // Outer glowing rings (cyan + pink spectrum)
            val ringTone = Brush.sweepGradient(listOf(Cyan, Color(0xff5599ff), Pink, Color(0xff9955ff), Cyan), Offset(cx, cy))
            for (ringFactor in listOf(0.98f, 0.90f, 0.80f)) {
                drawCircle(ringTone, r * ringFactor, Offset(cx, cy), style = Stroke(r * 0.020f))
                drawCircle(Color.White.copy(alpha = .08f), r * ringFactor, Offset(cx, cy), style = Stroke(r * 0.048f))
            }

            // Rotating arc highlight (cyan sweep)
            spin?.let { rotation ->
                val arcSize = Size(r * 1.76f, r * 1.76f)
                val arcTL = Offset(cx - r * 0.88f, cy - r * 0.88f)
                drawArc(Brush.sweepGradient(listOf(Cyan, Color.Transparent, Pink, Color.Transparent, Cyan), Offset(cx, cy)),
                    -25f + rotation.value, 70f, false, arcTL, arcSize, style = Stroke(r * 0.038f, cap = StrokeCap.Round))
            }

            // Circular tick marks
            for (tick in 0..71) {
                val angle = (tick * 5f) * Math.PI.toFloat() / 180f
                val outer = r * 0.99f
                val inner = outer - r * (if (tick % 6 == 0) 0.055f else 0.022f)
                val tickAlpha = if (tick % 6 == 0) 0.85f else 0.35f
                drawLine(
                    if (tick % 3 == 0) Cyan.copy(alpha = tickAlpha) else Pink.copy(alpha = tickAlpha * 0.7f),
                    Offset(cx + kotlin.math.cos(angle) * inner, cy + kotlin.math.sin(angle) * inner),
                    Offset(cx + kotlin.math.cos(angle) * outer, cy + kotlin.math.sin(angle) * outer),
                    if (tick % 6 == 0) r * 0.018f else r * 0.011f
                )
            }

            // Fingerprint ridges - realistic concentric arcs
            val ridgeCore = Offset(cx, cy + r * 0.18f)
            val ridgeCount = 11
            for (i in 0 until ridgeCount) {
                val t = i / (ridgeCount - 1f)
                val arcR = r * (0.08f + t * 0.62f)
                val fade = (1f - t * 0.25f).coerceAtLeast(0.5f)
                val ridgeColor = Brush.linearGradient(
                    listOf(Cyan.copy(alpha = fade * 0.9f), Color(0xff7788ff).copy(alpha = fade * 0.85f), Pink.copy(alpha = fade * 0.75f)),
                    Offset(ridgeCore.x - arcR, ridgeCore.y - arcR), Offset(ridgeCore.x + arcR, ridgeCore.y + arcR))

                // Outer ridges are broken (like real fingerprints)
                if (i >= ridgeCount - 3) {
                    var segAngle = 150f
                    while (segAngle < 370f) {
                        val span = if ((segAngle.toInt() / 40) % 2 == 0) 35f else 0f
                        if (span > 0f) {
                            drawArc(ridgeColor, segAngle, span, false,
                                Offset(ridgeCore.x - arcR, ridgeCore.y - arcR), Size(arcR * 2f, arcR * 2f),
                                style = Stroke(r * 0.022f, cap = StrokeCap.Round))
                        }
                        segAngle += 50f
                    }
                } else {
                    drawArc(ridgeColor, 150f, 240f, false,
                        Offset(ridgeCore.x - arcR, ridgeCore.y - arcR), Size(arcR * 2f, arcR * 2f),
                        style = Stroke(r * 0.024f, cap = StrokeCap.Round))
                }
            }

            // Central core whorl
            val whorlR = r * 0.12f
            drawArc(Brush.linearGradient(listOf(Cyan, Color(0xff8866ff)), ridgeCore),
                150f, 240f, false, Offset(ridgeCore.x - whorlR, ridgeCore.y - whorlR), Size(whorlR * 2f, whorlR * 2f),
                style = Stroke(r * 0.024f, cap = StrokeCap.Round))

            // Platform pedestal glow below orb
            val pedestalY = cy + r * 0.88f
            val pedestalRx = r * 0.70f
            val pedestalRy = r * 0.10f
            drawOval(Brush.radialGradient(listOf(Pink.copy(alpha = .38f + breath * 0.15f),
                Color(0xff6622dd).copy(alpha = .16f), Color.Transparent), Offset(cx, pedestalY), r * 0.75f),
                topLeft = Offset(cx - pedestalRx, pedestalY - pedestalRy),
                size = Size(pedestalRx * 2f, pedestalRy * 2f))
            drawOval(Brush.horizontalGradient(listOf(Cyan.copy(alpha = .68f), Pink.copy(alpha = .58f), Color.Transparent)),
                topLeft = Offset(cx - pedestalRx, pedestalY - pedestalRy),
                size = Size(pedestalRx * 2f, pedestalRy * 2f), style = Stroke(r * 0.016f))

            // Scanning line overlay (Verifying)
            val scanSweepValue = scanSweep?.value
            if (scanSweepValue != null) {
                val sweepY = cy - r + (r * 2f * scanSweepValue)
                val sweepH = r * 0.20f
                val disc = Path().apply {
                    addOval(androidx.compose.ui.geometry.Rect(cx - r, cy - r, cx + r, cy + r))
                }
                clipPath(disc) {
                    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Cyan.copy(alpha = .50f), Color.Transparent)),
                        topLeft = Offset(cx - r, sweepY - sweepH), size = Size(r * 2f, sweepH * 2f))
                }
            }

            // Verified checkmark
            if (stage == AuthStage.VERIFIED) {
                val w = r * 0.11f
                drawLine(Color.White.copy(alpha = .95f), Offset(cx - r * .20f, cy + r * .02f),
                    Offset(cx - r * .04f, cy + r * .18f), w, StrokeCap.Round)
                drawLine(Color.White.copy(alpha = .95f), Offset(cx - r * .04f, cy + r * .18f),
                    Offset(cx + r * .24f, cy - r * .18f), w, StrokeCap.Round)
                drawLine(Cyan.copy(alpha = .48f), Offset(cx - r * .20f, cy + r * .02f),
                    Offset(cx - r * .04f, cy + r * .18f), w * 2.3f, StrokeCap.Round)
                drawLine(Cyan.copy(alpha = .48f), Offset(cx - r * .04f, cy + r * .18f),
                    Offset(cx + r * .24f, cy - r * .18f), w * 2.3f, StrokeCap.Round)
            }
        }

        // Unavailable warning badge
        if (stage == AuthStage.UNAVAILABLE) {
            Box(Modifier.size(46.dp).align(Alignment.BottomEnd).clip(RoundedCornerShape(23.dp))
                .background(Brush.verticalGradient(listOf(Color(0xff5a3a00), Color(0xff241300))), RoundedCornerShape(23.dp))
                .border(1.6.dp, Gold.copy(alpha = .95f), RoundedCornerShape(23.dp)),
                contentAlignment = Alignment.Center) {
                Glyph(Mark.WARN, Modifier.size(24.dp), Gold, true)
            }
        }
    }
}

/** The three provider rows of the reference. */
@Composable
private fun ProviderRow(
    title: Copy, mark: Mark, tint: Color, tag: String, onClick: () -> Unit
) {
    GlassPanel(Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(20.dp))
        .testTag(tag).clickable(role = Role.Button, onClick = onClick), radius = 20, tint = tint) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(15.dp))
                .background(Brush.verticalGradient(listOf(tint.copy(alpha = .40f), Color(0xff150c36).copy(alpha = .9f))),
                    RoundedCornerShape(15.dp))
                .border(1.dp, Brush.horizontalGradient(listOf(Color.White.copy(alpha = .6f), tint, Color.Transparent)),
                    RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                VolumetricGlyph(mark, tint, 26.dp)
            }
            Separator(Modifier.width(1.dp).height(38.dp))
            Label(title.localized(), 17, weight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
    }
}

/**
 * Landing stage — the first surface the user sees after the splash.
 * A cinematic brand moment with an animated K mark, taglines that reveal
 * letter by letter, and a single glowing CTA button to proceed to sign-in.
 * Nothing here signs in or performs any device action.
 */
@Composable
private fun LandingStage(motion: Boolean, onEnter: () -> Unit) {
    // Entry animations
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(120)
        entered = true
    }

    val logoScale by animateFloatAsState(if (entered) 1f else 0.55f,
        tween(680, easing = FastOutSlowInEasing), label = "logo-scale")
    val logoAlpha by animateFloatAsState(if (entered) 1f else 0f,
        tween(500), label = "logo-alpha")
    val titleAlpha by animateFloatAsState(if (entered) 1f else 0f,
        tween(600, delayMillis = 320), label = "title-alpha")
    val tagAlpha by animateFloatAsState(if (entered) 1f else 0f,
        tween(500, delayMillis = 540), label = "tag-alpha")
    val ctaAlpha by animateFloatAsState(if (entered) 1f else 0f,
        tween(600, delayMillis = 780), label = "cta-alpha")
    val ctaOffset by animateFloatAsState(if (entered) 0f else 32f,
        tween(600, delayMillis = 780, easing = FastOutSlowInEasing), label = "cta-offset")

    val transition = rememberInfiniteTransition(label = "landing")
    val floatY = if (motion) transition.animateFloat(-10f, 10f,
        infiniteRepeatable(tween(3400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float") else null
    val orbPulse = if (motion) transition.animateFloat(0f, 1f,
        infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "orb-pulse") else null
    val shimmer = if (motion) transition.animateFloat(0f, 1f,
        infiniteRepeatable(tween(2800, easing = LinearEasing)), label = "shimmer") else null

    // CTA press state
    val ctaInteraction = remember { MutableInteractionSource() }
    val ctaPressed by ctaInteraction.collectIsPressedAsState()
    val ctaScale by animateFloatAsState(if (ctaPressed) 0.96f else 1f, tween(100), label = "cta-press")
    val context = LocalContext.current

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(52.dp))

            // ── Animated brand K orb ──────────────────────────────────────
            Box(
                Modifier
                    .size(220.dp)
                    .graphicsLayer {
                        scaleX = logoScale; scaleY = logoScale
                        alpha = logoAlpha
                        translationY = floatY?.value ?: 0f
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val cx = size.width / 2f; val cy = size.height / 2f
                    val r = size.minDimension / 2f
                    val pulse = orbPulse?.value ?: 0.5f

                    // Multi-layer atmospheric glow
                    drawCircle(Brush.radialGradient(listOf(
                        Color(0xff9922ff).copy(alpha = .34f * (0.6f + pulse * 0.4f)),
                        Color(0xff3300cc).copy(alpha = .18f),
                        Color.Transparent), Offset(cx, cy), r * 1.6f), r * 1.6f, Offset(cx, cy))
                    drawCircle(Brush.radialGradient(listOf(
                        Cyan.copy(alpha = .16f * (0.5f + pulse * 0.5f)),
                        Color.Transparent), Offset(cx * 1.3f, cy * 0.6f), r * 0.9f), r * 0.9f, Offset(cx * 1.3f, cy * 0.6f))
                    drawCircle(Brush.radialGradient(listOf(
                        Pink.copy(alpha = .12f * (0.5f + pulse * 0.5f)),
                        Color.Transparent), Offset(cx * 0.7f, cy * 1.4f), r * 0.8f), r * 0.8f, Offset(cx * 0.7f, cy * 1.4f))

                    // Three concentric rings (like the reference auth orb)
                    val ringBrush = Brush.sweepGradient(listOf(Color(0xff00f5ff), Color(0xffb040ff), Color(0xffff00ee), Color(0xff00f5ff)), Offset(cx, cy))
                    for (rScale in listOf(0.96f, 0.84f, 0.70f)) {
                        drawCircle(ringBrush, r * rScale, Offset(cx, cy), style = Stroke(r * 0.018f))
                        drawCircle(Color.White.copy(alpha = 0.06f), r * rScale, Offset(cx, cy), style = Stroke(r * 0.038f))
                    }

                    // Rotating shimmer arc
                    if (shimmer != null) {
                        val angle = shimmer.value * 360f
                        val arcSize = Size(r * 1.78f, r * 1.78f)
                        drawArc(Brush.sweepGradient(listOf(Color.Transparent, Cyan.copy(alpha = .65f), Color.Transparent), Offset(cx, cy)),
                            angle - 20f, 60f, false,
                            Offset(cx - r * 0.89f, cy - r * 0.89f), arcSize,
                            style = Stroke(r * 0.032f, cap = StrokeCap.Round))
                    }

                    // Platform glow below
                    val py = cy + r * 0.88f
                    drawOval(Brush.radialGradient(listOf(Pink.copy(alpha = 0.38f + pulse * 0.12f),
                        Color(0xff6600ff).copy(alpha = 0.12f), Color.Transparent), Offset(cx, py), r * 0.8f),
                        topLeft = Offset(cx - r * 0.80f, py - r * 0.10f), size = Size(r * 1.60f, r * 0.20f))
                    drawOval(Brush.horizontalGradient(listOf(Color.Transparent, Cyan.copy(alpha = 0.6f), Pink.copy(alpha = 0.5f), Color.Transparent)),
                        topLeft = Offset(cx - r * 0.60f, py - r * 0.06f),
                        size = Size(r * 1.20f, r * 0.12f), style = Stroke(r * 0.012f))
                }

                // K mark centred in the orb with glow
                Box(Modifier.size(90.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.matchParentSize()) {
                        val pulse = orbPulse?.value ?: 0.5f
                        val bloom = Brush.radialGradient(listOf(
                            Color(0xffb040ff).copy(alpha = .36f + pulse * .18f), Color.Transparent),
                            Offset(size.width / 2f, size.height / 2f), size.minDimension * 0.9f)
                        drawCircle(bloom, size.minDimension * 0.9f, Offset(size.width / 2f, size.height / 2f))
                    }
                    Glyph(Mark.K, Modifier.size(72.dp), Chalk, true)
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Main title ───────────────────────────────────────────────
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                    .graphicsLayer { alpha = titleAlpha },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val titleText = buildAnnotatedString {
                    withStyle(SpanStyle(color = Chalk)) {
                        append(("Your Device's" ar "الحارس الذكي").localized())
                    }
                    withStyle(SpanStyle(brush = Brush.linearGradient(
                        listOf(Cyan, Color(0xff8844ff), Pink),
                        start = Offset(0f, 0f), end = Offset(400f, 0f)
                    ))) {
                        append("\n")
                        append(("AI Guardian" ar "لجهازك").localized())
                    }
                }
                Text(
                    titleText,
                    Modifier.fillMaxWidth(),
                    style = TextStyle(
                        fontSize = 34.sp, lineHeight = 40.sp,
                        fontFamily = DisplayFace, fontWeight = FontWeight.ExtraBold,
                        fontStyle = FontStyle.Italic, textAlign = TextAlign.Center
                    )
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── Tagline ──────────────────────────────────────────────────
            Label(
                ("Clean · Protect · Optimize · Analyze" ar
                    "تنظيف · حماية · تحسين · تحليل").localized(),
                13,
                Mist.copy(alpha = 0.72f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .graphicsLayer { alpha = tagAlpha },
                align = TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            // ── Feature pills ────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).graphicsLayer { alpha = tagAlpha },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "AI Powered" ar "بالذكاء الاصطناعي" to Cyan,
                    "10 Tools" ar "١٠ أداة" to Violet,
                    "Pro Ready" ar "Pro جاهز" to Gold
                ).forEachIndexed { i, (label, color) ->
                    if (i > 0) Spacer(Modifier.width(8.dp))
                    NeonPill(label.localized(), color, solid = false)
                }
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(32.dp))

            // ── THE ENTER BUTTON ─────────────────────────────────────────
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .graphicsLayer {
                        alpha = ctaAlpha
                        translationY = ctaOffset
                        scaleX = ctaScale; scaleY = ctaScale
                    }
            ) {
                // Outer bloom halo
                if (motion) {
                    Canvas(Modifier.fillMaxWidth().height(80.dp)) {
                        val pulse = orbPulse?.value ?: 0.5f
                        val cx = size.width / 2f; val cy = size.height / 2f
                        for ((radius, alpha) in listOf(size.width * .56f to .18f, size.width * .40f to .28f, size.width * .26f to .18f)) {
                            drawCircle(Brush.radialGradient(
                                listOf(Color(0xffff36f5).copy(alpha = alpha * (0.7f + pulse * 0.3f)),
                                    Color(0xff6622ff).copy(alpha = alpha * .8f), Color.Transparent),
                                Offset(cx, cy), radius), radius, Offset(cx, cy))
                        }
                    }
                }

                // Button surface
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .drawBehind {
                            val w = size.width; val h = size.height
                            val rad = androidx.compose.ui.geometry.CornerRadius(h / 2f)
                            // Brand gradient
                            drawRoundRect(
                                Brush.horizontalGradient(listOf(
                                    Color(0xffff36f5), Color(0xffa826ff),
                                    Color(0xff344dff), Color(0xff00d8ff)
                                )), cornerRadius = rad)
                            // Shimmer band
                            if (shimmer != null) {
                                val travel = shimmer.value * w * 1.6f - w * .3f
                                drawRoundRect(
                                    Brush.linearGradient(
                                        listOf(Color.Transparent, Color.White.copy(alpha = .30f), Color.Transparent),
                                        start = androidx.compose.ui.geometry.Offset(travel - w * .24f, 0f),
                                        end = androidx.compose.ui.geometry.Offset(travel + w * .24f, h)
                                    ), cornerRadius = rad)
                            }
                            // Top lip highlight
                            drawRoundRect(
                                Brush.verticalGradient(listOf(Color.White.copy(alpha = .38f), Color.Transparent),
                                    startY = 0f, endY = h * .55f), cornerRadius = rad)
                            // Bright rim
                            drawRoundRect(
                                Brush.horizontalGradient(listOf(Color.White.copy(alpha = .88f), Color.Transparent, Cyan.copy(alpha = .82f))),
                                cornerRadius = rad, style = Stroke(1.3.dp.toPx()))
                        }
                        .clickable(
                            interactionSource = ctaInteraction,
                            indication = null,
                            role = Role.Button
                        ) {
                            TouchFeedback.tap(context)
                            onEnter()
                        }
                        .testTag("landing-enter"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Glyph(Mark.K, Modifier.size(22.dp), Chalk, true)
                        Label(
                            ("Get Started" ar "ابدأ الآن").localized(),
                            17, Chalk, FontWeight.ExtraBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy((-5).dp)) {
                            Glyph(Mark.NEXT, Modifier.size(14.dp), Chalk)
                            Glyph(Mark.NEXT, Modifier.size(14.dp), Chalk.copy(alpha = .55f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── Skip / guest link ────────────────────────────────────────
            Box(
                Modifier
                    .graphicsLayer { alpha = ctaAlpha }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onEnter
                    )
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Label(
                    ("Explore without signing in →" ar "→ تصفح بدون تسجيل دخول").localized(),
                    13, Mist.copy(alpha = 0.55f)
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

/** Welcome state — full hexagonal cyberpunk identity portal. */
@Composable
private fun WelcomeStage(
    motion: Boolean, onUnlock: () -> Unit, onPasskey: () -> Unit, onGoogle: () -> Unit,
    onGuest: () -> Unit, onPrivacy: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "welcome")
    val floatY = if (motion) transition.animateFloat(-7f, 7f,
        infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float") else null
    val ringPulse = if (motion) transition.animateFloat(0f, 1f,
        infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "ring") else null
    val glowSpin = if (motion) transition.animateFloat(0f, 360f,
        infiniteRepeatable(tween(10000, easing = LinearEasing)), label = "spin") else null

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))

        // Top brand bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Glyph(Mark.K, Modifier.size(30.dp))
                Label("knoux", 20, Chalk, FontWeight.ExtraBold)
                Label(" ai", 20, Pink, FontWeight.ExtraBold)
            }
            Box(Modifier.clip(RoundedCornerShape(14.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xff1a1000), Color(0xff0f0820))), RoundedCornerShape(14.dp))
                .border(1.dp, Gold.copy(alpha = .9f), RoundedCornerShape(14.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.size(6.dp).background(Gold, RoundedCornerShape(3.dp)))
                    Label("Preview Build", 10, Gold, FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Central HEX IRIS ORB — large, floating, with hex halo
        Box(
            Modifier.size(240.dp).graphicsLayer { translationY = floatY?.value ?: 0f },
            contentAlignment = Alignment.Center
        ) {
            // Hex halo rings behind the orb
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val r = size.minDimension / 2f
                val pulse = ringPulse?.value ?: 0.5f
                val spinDeg = glowSpin?.value ?: 0f

                // Draw 3 large hexagons as halos
                for (haloIdx in 0..2) {
                    val haloR = r * (0.88f + haloIdx * 0.07f)
                    val rotOffset = spinDeg * (if (haloIdx % 2 == 0) 1f else -1f) * 0.4f
                    val hexPath = Path().apply {
                        for (i in 0..5) {
                            val a = ((60f * i + rotOffset) * Math.PI / 180f).toFloat()
                            val px = cx + haloR * kotlin.math.cos(a)
                            val py = cy + haloR * kotlin.math.sin(a)
                            if (i == 0) moveTo(px, py) else lineTo(px, py)
                        }
                        close()
                    }
                    val haloAlpha = (0.5f - haloIdx * 0.12f) * (0.6f + pulse * 0.4f)
                    val haloColor = when (haloIdx) {
                        0 -> Cyan.copy(alpha = haloAlpha)
                        1 -> Color(0xffb040ff).copy(alpha = haloAlpha)
                        else -> Pink.copy(alpha = haloAlpha * 0.7f)
                    }
                    drawPath(hexPath, haloColor, style = Stroke(r * 0.018f))
                    // Glow blur simulation
                    drawPath(hexPath, haloColor.copy(alpha = haloAlpha * 0.3f), style = Stroke(r * 0.045f))
                }

                // Sweeping arc highlight
                val arcSize = Size(r * 1.72f, r * 1.72f)
                val arcTL = Offset(cx - r * 0.86f, cy - r * 0.86f)
                drawArc(Brush.sweepGradient(listOf(Color.Transparent, Cyan.copy(alpha = 0.6f), Color.Transparent), Offset(cx, cy)),
                    spinDeg - 30f, 80f, false, arcTL, arcSize, style = Stroke(r * 0.030f, cap = StrokeCap.Round))

                // Platform glow below orb
                val py = cy + r * 0.88f
                drawOval(Brush.radialGradient(listOf(Pink.copy(alpha = 0.4f + pulse * 0.15f),
                    Color(0xff6600ff).copy(alpha = 0.15f), Color.Transparent), Offset(cx, py), r * 0.85f),
                    topLeft = Offset(cx - r * 0.85f, py - r * 0.11f),
                    size = Size(r * 1.70f, r * 0.22f))
                drawOval(Brush.horizontalGradient(listOf(Color.Transparent, Cyan.copy(alpha = 0.65f + pulse * 0.2f),
                    Pink.copy(alpha = 0.5f), Color.Transparent)),
                    topLeft = Offset(cx - r * 0.65f, py - r * 0.07f),
                    size = Size(r * 1.30f, r * 0.14f), style = Stroke(r * 0.011f))
            }
            FingerprintOrb(210, AuthStage.WELCOME, motion,
                Modifier
                    .clickable(interactionSource = remember { MutableInteractionSource() },
                        indication = null, role = Role.Button, onClick = onUnlock)
                    .testTag("auth-fingerprint"))
        }

        Spacer(Modifier.height(16.dp))

        // Title
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            val titleStyle = buildAnnotatedString {
                withStyle(SpanStyle(color = Chalk)) { append("Welcome to ") }
                withStyle(SpanStyle(brush = Brush.linearGradient(
                    listOf(Cyan, Color(0xffb040ff), Pink),
                    start = Offset(0f, 0f), end = Offset(300f, 0f)))) { append("KNOUX AI") }
            }
            Text(titleStyle, Modifier.fillMaxWidth(), style = TextStyle(
                fontSize = 26.sp, fontFamily = DisplayFace, fontWeight = FontWeight.ExtraBold,
                fontStyle = FontStyle.Italic, textAlign = TextAlign.Center))
            Spacer(Modifier.height(6.dp))
            Label(("Secure access to your device intelligence." ar "وصول آمن لذكاء جهازك.").localized(),
                13, Mist.copy(alpha = 0.8f), modifier = Modifier.fillMaxWidth(), align = TextAlign.Center)
        }

        Spacer(Modifier.height(14.dp))

        // Privacy info banner
        GlassPanel(Modifier.fillMaxWidth().padding(horizontal = 18.dp).clip(RoundedCornerShape(16.dp)), radius = 16, tint = Cyan) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                    .background(Brush.verticalGradient(listOf(Cyan.copy(alpha = .35f), Color(0xff001820).copy(alpha = .9f))), RoundedCornerShape(12.dp))
                    .border(1.dp, Cyan.copy(alpha = .5f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center) {
                    Glyph(Mark.SHIELD, Modifier.size(22.dp), Cyan, true)
                }
                Column(Modifier.weight(1f)) {
                    Label(("Private by design" ar "خصوصية بالتصميم").localized(), 13, Chalk, FontWeight.Bold)
                    Label(("On-device tools, transparent access, no hidden scanning." ar "أدوات محلية، وصول شفاف، لا مسح خفي.").localized(),
                        11, Mist.copy(alpha = 0.75f))
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Scan label
        Row(Modifier.padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(8.dp).background(Cyan, RoundedCornerShape(4.dp))
                .graphicsLayer { alpha = 0.5f + (ringPulse?.value ?: 0.5f) * 0.5f })
            Label(("Tap iris to unlock with your fingerprint" ar "اضغط القزحية لفتح ببصمتك").localized(),
                14, Chalk, FontWeight.Bold)
        }

        Spacer(Modifier.height(18.dp))

        // Auth buttons
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ProviderRow("Use a passkey" ar "استخدم مفتاح مرور", Mark.KEY, Pink, "auth-passkey", onPasskey)
            ProviderRow("Continue with Google" ar "المتابعة عبر Google", Mark.GOOGLE, Cyan, "auth-google", onGoogle)
            ProviderRow("Continue as Guest" ar "المتابعة كزائر", Mark.PERSON, Violet, "auth-guest", onGuest)
        }

        Spacer(Modifier.height(16.dp))

        // Privacy footer
        Row(Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Glyph(Mark.WARN, Modifier.size(13.dp), Mist.copy(alpha = 0.5f))
            Label(("Biometric, passkey, and Google sign-in become fully active after production key setup." ar
                "تصبح طرق الدخول فعّالة بعد إعداد مفاتيح الإنتاج.").localized(),
                11, Mist.copy(alpha = 0.55f), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Biometric verification state of the reference. */
@Composable
private fun VerifyStage(motion: Boolean, onCancel: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(10.dp))
        AuthTopBar(onBack = onCancel)
        Spacer(Modifier.height(26.dp))
        AuthTitle("Verify your" ar "تحقق من", "identity" ar "هويتك",
            "Waiting for biometric verification…" ar "في انتظار التحقق بالبصمة…")
        Spacer(Modifier.height(18.dp))
        Box(Modifier.size(268.dp), contentAlignment = Alignment.Center) {
            FingerprintOrb(258, AuthStage.VERIFYING, motion, Modifier.testTag("auth-verify-orb"))
        }
        Spacer(Modifier.height(24.dp))
        AccentButton(Action("Cancel" ar "إلغاء", Mark.BACK), Modifier.fillMaxWidth(0.62f).padding(horizontal = 18.dp),
            primary = false, tag = "auth-cancel", onClick = onCancel)
        Spacer(Modifier.height(24.dp))
    }
}

/** Verified state of the reference. */
@Composable
private fun VerifiedStage(motion: Boolean) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp))
        AuthBrandLockup(previewBadge = false)
        Spacer(Modifier.height(30.dp))
        Box(Modifier.size(262.dp), contentAlignment = Alignment.Center) {
            FingerprintOrb(252, AuthStage.VERIFIED, motion, Modifier.testTag("auth-verified-orb"))
        }
        Spacer(Modifier.height(26.dp))
        AuthTitle("Identity" ar "تم التحقق من", "verified" ar "الهوية",
            "Welcome back to KNOUX AI" ar "مرحبًا بعودتك إلى KNOUX AI")
        Spacer(Modifier.weight(1f))
        Label(("Opening your dashboard…" ar "جارٍ فتح لوحة التحكم…").localized(), 15, Mist,
            modifier = Modifier.padding(bottom = 42.dp), align = TextAlign.Center)
    }
}

/** Biometric unavailable state of the reference. */
@Composable
private fun UnavailableStage(
    motion: Boolean, onBack: () -> Unit, onSettings: () -> Unit,
    onPasskey: () -> Unit, onGoogle: () -> Unit, onGuest: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(10.dp))
        AuthTopBar(onBack = onBack, onSettings = onSettings)
        Spacer(Modifier.height(18.dp))
        Box(Modifier.size(228.dp), contentAlignment = Alignment.Center) {
            FingerprintOrb(216, AuthStage.UNAVAILABLE, motion, Modifier.testTag("auth-unavailable-orb"))
        }
        Spacer(Modifier.height(14.dp))
        AuthTitle("Biometric" ar "البصمة", "unavailable" ar "غير متاحة",
            "Choose another sign-in method to continue." ar "اختر طريقة تسجيل دخول أخرى للمتابعة.")
        Spacer(Modifier.height(22.dp))
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ProviderRow("Use a passkey" ar "استخدم مفتاح مرور", Mark.KEY, Pink, "auth-passkey", onPasskey)
            ProviderRow("Continue with Google" ar "المتابعة عبر Google", Mark.GOOGLE, Cyan, "auth-google", onGoogle)
            ProviderRow("Continue as Guest" ar "المتابعة كزائر", Mark.PERSON, Violet, "auth-guest", onGuest)
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Compact glass bar used by the verifying and unavailable states. */
@Composable
private fun AuthTopBar(onBack: () -> Unit, onSettings: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp).glass(20, tab = false)
        .heightIn(min = 56.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        IconControl(Mark.BACK, ("Back" ar "رجوع").localized(), "back", onBack)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Glyph(Mark.K, Modifier.size(30.dp))
                    if (LocalDensity.current.fontScale <= 1.45f) {
                        val wordmark = if (LocalRoomy.current) 14 else if (LocalArabic.current) 18 else 20
                        Label("knoux", wordmark, weight = FontWeight.ExtraBold)
                        Label(" ai", wordmark, Pink, FontWeight.ExtraBold)
                    }
                }
            }
        }
        Row(Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(18.dp))
            .border(1.dp, Gold.copy(alpha = .85f), RoundedCornerShape(18.dp))
            .padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Glyph(Mark.CROWN, Modifier.size(15.dp), Gold)
            Label("Pro", 11, Gold, FontWeight.Bold)
        }
        IconControl(Mark.GEAR, ("Settings" ar "الإعدادات").localized(), "settings", onSettings)
    }
}
