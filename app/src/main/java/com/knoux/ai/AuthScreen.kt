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
internal enum class AuthStage { WELCOME, VERIFYING, VERIFIED, UNAVAILABLE }

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

/** The shared cosmic backdrop used by every authentication state. */
@Composable
private fun AuthBackdrop(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(
        Brush.verticalGradient(listOf(Color(0xff190c52), Color(0xff090426), Color(0xff020008))))
        .drawBehind {
            val top = Offset(size.width * .78f, size.height * .01f)
            drawCircle(Brush.radialGradient(listOf(Color(0xff6d24ff).copy(alpha = .42f),
                Color(0xff2c1570).copy(alpha = .22f), Color.Transparent), top, size.width * .95f),
                size.width * .95f, top)
            val high = Offset(size.width * .14f, size.height * .13f)
            drawCircle(Brush.radialGradient(listOf(Color(0xffff3df0).copy(alpha = .16f), Color.Transparent), high,
                size.width * .48f), size.width * .48f, high)
            val low = Offset(size.width * .10f, size.height * .88f)
            drawCircle(Brush.radialGradient(listOf(Cyan.copy(alpha = .16f), Color.Transparent), low,
                size.width * .70f), size.width * .70f, low)
        }) {
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
 * The holographic fingerprint: concentric illuminated rings, procedural ridge
 * geometry and a travelling cyan/violet sweep. It is decorative only — it can
 * never grant access on its own.
 */
@Composable
private fun FingerprintOrb(
    diameter: Int, stage: AuthStage, motion: Boolean, modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .96f else 1f, tween(120), label = "fingerprint press")
    val transition = rememberInfiniteTransition(label = "fingerprint")
    val spin = if (motion) transition.animateFloat(0f, 360f,
        infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "ring spin") else null
    val sweep = if (motion && stage == AuthStage.VERIFYING) transition.animateFloat(0f, 1f,
        infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "sweep") else null
    val pulse = if (motion) transition.animateFloat(0f, 1f,
        infiniteRepeatable(tween(3600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "orb pulse") else null

    Box(modifier.size(diameter.dp).graphicsLayer { scaleX = scale; scaleY = scale },
        contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val breath = pulse?.value ?: 1f

            drawCircle(Brush.radialGradient(listOf(Color(0xffff4ade).copy(alpha = .26f * breath),
                Color(0xff6d2dff).copy(alpha = .20f * breath), Color.Transparent), center, radius), radius, center)

            val ringTone = Brush.sweepGradient(listOf(Cyan, Color(0xff3079fa), Pink, Color(0xff8561ff), Chalk, Cyan), center)
            for (factor in listOf(.96f, .86f, .74f)) {
                drawCircle(ringTone, radius * factor, center, style = Stroke(radius * .022f))
                drawCircle(Color.White.copy(alpha = .12f), radius * factor, center, style = Stroke(radius * .05f))
            }
            for (tick in 0..59) {
                val angle = (tick * 6f) * Math.PI.toFloat() / 180f
                val outer = radius * .99f
                val inner = outer - radius * (if (tick % 5 == 0) .045f else .022f)
                drawLine(if (tick % 3 == 0) Cyan.copy(alpha = .55f) else Pink.copy(alpha = .34f),
                    Offset(center.x + kotlin.math.cos(angle) * inner, center.y + kotlin.math.sin(angle) * inner),
                    Offset(center.x + kotlin.math.cos(angle) * outer, center.y + kotlin.math.sin(angle) * outer),
                    radius * .010f)
            }
            spin?.let { rotation ->
                val arcBox = Size(radius * 1.7f, radius * 1.7f)
                val topLeft = Offset(center.x - radius * .85f, center.y - radius * .85f)
                drawArc(Brush.sweepGradient(listOf(Cyan, Color.Transparent, Pink), center), -30f + rotation.value,
                    86f, false, topLeft, arcBox, style = Stroke(radius * .045f, cap = StrokeCap.Round))
                drawArc(Brush.sweepGradient(listOf(Color.Transparent, Pink, Color.Transparent), center),
                    150f - rotation.value * 1.2f, 62f, false, topLeft, arcBox,
                    style = Stroke(radius * .030f, cap = StrokeCap.Round))
            }

            // Procedural fingerprint ridges: concentric arches that spring from a
            // core below the print, the way a real loop pattern curves and
            // re-crosses itself. The outermost ridges break into dashes.
            val coreX = center.x + radius * .02f
            val coreY = center.y + radius * .22f
            val ridgeCount = 9
            val arcStart = 160f
            val arcSweep = 220f
            for (index in 0 until ridgeCount) {
                val t = index / (ridgeCount - 1f)
                val halfWidth = radius * (.11f + t * .41f)
                val halfHeight = radius * (.13f + t * .57f)
                val box = Size(halfWidth * 2f, halfHeight * 2f)
                val topLeft = Offset(coreX - halfWidth, coreY - halfHeight)
                val fade = .95f - t * .20f
                val stroke = Brush.linearGradient(
                    listOf(Cyan.copy(alpha = fade), Color(0xff9a6bff).copy(alpha = fade), Pink.copy(alpha = fade)),
                    Offset(coreX - halfWidth, coreY - halfHeight), Offset(coreX + halfWidth, coreY + halfHeight))
                if (index >= ridgeCount - 3) {
                    // Broken outer ridges, drawn as short segments with gaps.
                    var angle = arcStart
                    var segment = 0
                    while (angle < arcStart + arcSweep) {
                        val span = if (segment % 2 == 0) 40f else 16f
                        drawArc(stroke, angle, span, false, topLeft, box,
                            style = Stroke(radius * .024f, cap = StrokeCap.Round))
                        angle += span + 18f
                        segment++
                    }
                } else {
                    drawArc(stroke, arcStart, arcSweep, false, topLeft, box,
                        style = Stroke(radius * .026f, cap = StrokeCap.Round))
                }
            }
            // The core of the print: a tight whorl closing the innermost ridge.
            val whorlBox = Size(radius * .20f, radius * .24f)
            drawArc(Brush.linearGradient(listOf(Cyan, Color(0xff9a6bff)), Offset(coreX, coreY)),
                arcStart, arcSweep, false, Offset(coreX - radius * .10f, coreY - radius * .12f), whorlBox,
                style = Stroke(radius * .026f, cap = StrokeCap.Round))

            // Pedestal: the print floats over a lit elliptical ring.
            val pedestal = Offset(center.x, center.y + radius * .90f)
            val pedestalRadius = Offset(radius * .74f, radius * .11f)
            drawOval(Brush.radialGradient(listOf(Color(0xffff54ff).copy(alpha = .40f),
                Color(0xff5a28e8).copy(alpha = .18f), Color.Transparent), pedestal, radius * .78f),
                topLeft = Offset(pedestal.x - pedestalRadius.x, pedestal.y - pedestalRadius.y),
                size = Size(pedestalRadius.x * 2f, pedestalRadius.y * 2f))
            drawOval(Brush.horizontalGradient(listOf(Cyan.copy(alpha = .70f), Pink.copy(alpha = .70f),
                Color.Transparent)), topLeft = Offset(pedestal.x - pedestalRadius.x, pedestal.y - pedestalRadius.y),
                size = Size(pedestalRadius.x * 2f, pedestalRadius.y * 2f),
                style = Stroke(radius * .018f))
            drawOval(Brush.horizontalGradient(listOf(Color.Transparent, Pink.copy(alpha = .45f),
                Color.Transparent)),
                topLeft = Offset(pedestal.x - pedestalRadius.x * 1.22f, pedestal.y - pedestalRadius.y * 1.5f),
                size = Size(pedestalRadius.x * 2.44f, pedestalRadius.y * 3f),
                style = Stroke(radius * .010f))

            val sweepValue: Float = sweep?.value ?: 0f
            if (sweepValue > 0f) {
                val band: Float = radius * .26f
                val y: Float = center.y - radius + band * 2f * sweepValue
                val disc = Path().apply {
                    addOval(androidx.compose.ui.geometry.Rect(
                        center.x - radius, center.y - radius, center.x + radius, center.y + radius))
                }
                clipPath(disc) {
                    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Cyan.copy(alpha = .45f), Color.Transparent)),
                        topLeft = Offset(center.x - radius, y - band), size = Size(radius * 2f, band * 2f))
                }
            }

            if (stage == AuthStage.VERIFIED) {
                val w = radius * .12f
                drawLine(Color.White.copy(alpha = .95f), Offset(center.x - radius * .22f, center.y + radius * .02f),
                    Offset(center.x - radius * .05f, center.y + radius * .20f), w, StrokeCap.Round)
                drawLine(Color.White.copy(alpha = .95f), Offset(center.x - radius * .05f, center.y + radius * .20f),
                    Offset(center.x + radius * .26f, center.y - radius * .20f), w, StrokeCap.Round)
                drawLine(Cyan.copy(alpha = .55f), Offset(center.x - radius * .22f, center.y + radius * .02f),
                    Offset(center.x - radius * .05f, center.y + radius * .20f), w * 2.1f, StrokeCap.Round)
                drawLine(Cyan.copy(alpha = .55f), Offset(center.x - radius * .05f, center.y + radius * .20f),
                    Offset(center.x + radius * .26f, center.y - radius * .20f), w * 2.1f, StrokeCap.Round)
            }
        }
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

/** Welcome state of the reference. */
@Composable
private fun WelcomeStage(
    motion: Boolean, onUnlock: () -> Unit, onPasskey: () -> Unit, onGoogle: () -> Unit,
    onGuest: () -> Unit, onPrivacy: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(20.dp))
        AuthBrandLockup(previewBadge = true)
        Spacer(Modifier.height(18.dp))
        AuthTitle("Welcome to" ar "مرحبًا بك في", "KNOUX AI" ar "KNOUX AI",
            "Your secure device intelligence starts here" ar "ذكاء جهازك الآمن يبدأ من هنا", splitAccent = true)
        Spacer(Modifier.height(22.dp))
        Box(Modifier.size(250.dp), contentAlignment = Alignment.Center) {
            FingerprintOrb(238, AuthStage.WELCOME, motion, Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, role = Role.Button, onClick = onUnlock).testTag("auth-fingerprint"))
        }
        Spacer(Modifier.height(12.dp))
        Label(("Unlock with fingerprint" ar "افتح ببصمة الإصبع").localized(), 19, weight = FontWeight.ExtraBold)
        Spacer(Modifier.height(20.dp))
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ProviderRow("Use a passkey" ar "استخدم مفتاح مرور", Mark.KEY, Pink, "auth-passkey", onPasskey)
            ProviderRow("Continue with Google" ar "المتابعة عبر Google", Mark.GOOGLE, Cyan, "auth-google", onGoogle)
            ProviderRow("Continue as Guest" ar "المتابعة كزائر", Mark.PERSON, Violet, "auth-guest", onGuest)
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 30.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center) {
            Glyph(Mark.SHIELD, Modifier.size(16.dp), Mist, true)
            Spacer(Modifier.width(8.dp))
            Label(("Privacy Information (Optional)" ar "معلومات الخصوصية (اختياري)").localized(), 13, Mist,
                modifier = Modifier.clickable(role = Role.Button, onClick = onPrivacy)
                    .semantics { contentDescription = "Privacy Information" })
        }
        Spacer(Modifier.height(20.dp))
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
