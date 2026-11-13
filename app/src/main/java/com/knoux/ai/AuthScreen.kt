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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.delay

/** The four visible states of the KNOUX identity gateway. */
internal enum class AuthStage { LANDING, WELCOME, VERIFYING, VERIFIED, UNAVAILABLE, OTP }

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
internal fun AuthGateway(forcedStage: AuthStage? = null,
    reducedMotion: Boolean = false,
    onAuthenticated: () -> Unit,
    onGuest: () -> Unit,
    onLanguage: (Boolean) -> Unit,
    onMotion: (Boolean) -> Unit,
    reduced: Boolean
) {
    val context = LocalContext.current
    val arabic = LocalArabic.current
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
            .setTitle(KnouxStrings.get(R.string.auth_biometric_title, arabic))
            .setSubtitle(KnouxStrings.get(R.string.auth_biometric_subtitle, arabic))
            .setNegativeButtonText(KnouxStrings.get(R.string.k_cancel, arabic))
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
            AuthStage.WELCOME -> WelcomeStage(motion = motion,
                onUnlock = { launchPrompt() },
                onPasskey = { sheet = "passkey" },
                onGoogle = { sheet = "google" },
                onGuest = onGuest,
                onPrivacy = { sheet = "privacy" },
                onSettings = { sheet = "settings" },
                onCredentials = { sheet = "credentials" },
                onRecovery = { sheet = "recovery" },
                onSignUp = { sheet = "signup" }
            )
            AuthStage.VERIFYING -> VerifyStage(motion = motion, onCancel = { stage = AuthStage.WELCOME })
            AuthStage.VERIFIED -> VerifiedStage(motion = motion)
            AuthStage.OTP -> OtpStage(motion = motion,
                onBack = { stage = AuthStage.WELCOME },
                onPasskey = { sheet = "passkey" },
                onGoogle = { sheet = "google" }
            )
            AuthStage.UNAVAILABLE -> UnavailableStage(motion = motion,
                onBack = { stage = AuthStage.WELCOME },
                onSettings = { sheet = "settings" },
                onPasskey = { sheet = "passkey" }, onGoogle = { sheet = "google" }, onGuest = onGuest)
        }
    }

    sheet?.let { kind ->
        InfoSheet(when (kind) {
                "passkey" -> (Copy(R.string.k_use_a_passkey))
                "google" -> (Copy(R.string.k_continue_with_google))
                "credentials" -> (Copy(R.string.k_sign_in_is_not_configured))
                "recovery" -> (Copy(R.string.k_password_recovery))
                "signup" -> (Copy(R.string.k_create_an_account))
                "settings" -> (Copy(R.string.k_preview_settings))
                else -> (Copy(R.string.k_privacy_information))
            }, { sheet = null }) {
            when (kind) {
                "passkey" -> {
                    Label((Copy(R.string.k_passkey_sign_in_is_not_configured_in_this_preview_build)).localized(), 14, Mist)
                    Label((Copy(R.string.k_no_credential_provider_key_or_account_session_is_created)).localized(), 13, Mist)
                }
                "google" -> {
                    Label((Copy(R.string.k_google_sign_in_is_not_configured_in_this_preview_build)).localized(), 14, Mist)
                    Label((Copy(R.string.k_no_oauth_client_client_secret_or_account_session_is_crea)).localized(), 13, Mist)
                }
                "credentials" -> {
                    Label(Copy(R.string.k_auth_credentials_note), 14, Mist)
                    Label(Copy(R.string.k_auth_credentials_review), 13, Mist)
                }
                "recovery" -> {
                    Label((Copy(R.string.k_recovery_requires_the_same_identity_provider_and_is_ther)).localized(), 14, Mist)
                    Label((Copy(R.string.k_no_email_is_sent_and_no_reset_link_is_generated_in_this)).localized(), 13, Mist)
                }
                "signup" -> {
                    Label((Copy(R.string.k_account_creation_is_not_connected_in_this_preview_build)).localized(), 14, Mist)
                    Label((Copy(R.string.k_no_account_profile_or_entitlement_is_created_by_this_scr)).localized(), 13, Mist)
                }
                "settings" -> {
                    Label(Copy(R.string.auth_local_note), 13, Mist)
                    val ar = LocalArabic.current
                    AdaptiveActions(Action(Copy(R.string.k_english), Mark.CHECK),
                        Action(Copy(R.string.k_text_26b1b2), Mark.CHECK),
                        firstSelected = !ar, secondSelected = ar,
                        onFirst = { onLanguage(false) }, onSecond = { onLanguage(true) })
                    AccentButton(Action(if (reduced) Copy(R.string.k_enable_decorative_motion) else Copy(R.string.k_reduce_motion),
                        Mark.RADAR), Modifier.fillMaxWidth(), primary = false, tag = "motion-toggle") { onMotion(!reduced) }
                }
                else -> {
                    Label(Copy(R.string.k_auth_permission_note), 13, Mist)
                }
            }
            Label(Copy(R.string.auth_local_note), 12, Mist)
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
            drawCircle(Brush.radialGradient(listOf(Color(0xff6600ff).copy(alpha = 0.35f * (0.7f + pulse * 0.3f)),
                Color(0xff3300aa).copy(alpha = 0.20f),
                Color.Transparent), center, size.width * 0.75f), size.width * 0.75f, center)
            drawCircle(Brush.radialGradient(listOf(Color(0xff00f5ff).copy(alpha = 0.12f * (0.6f + pulse * 0.4f)),
                Color.Transparent), Offset(size.width * 0.8f, size.height * 0.1f), size.width * 0.5f),
                size.width * 0.5f, Offset(size.width * 0.8f, size.height * 0.1f))
            drawCircle(Brush.radialGradient(listOf(Color(0xffff00ee).copy(alpha = 0.10f * (0.6f + pulse * 0.4f)),
                Color.Transparent), Offset(size.width * 0.15f, size.height * 0.85f), size.width * 0.45f),
                size.width * 0.45f, Offset(size.width * 0.15f, size.height * 0.85f))
        }
        content()
    }
}

/** Brand lockup used by the welcome and verified states. */
@Composable
private fun AuthBrandLockup() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Glyph(Mark.K, Modifier.size(30.dp))
        Spacer(Modifier.width(4.dp))
        Label("knoux", 20, weight = FontWeight.ExtraBold)
        Label(" ai", 20, Pink, FontWeight.ExtraBold)
    }
}

/** Title treatment of the authentication states. */
@Composable
private fun AuthTitle(lead: Copy, accent: Copy, subtitle: Copy, splitAccent: Boolean = false) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val gradient = Brush.verticalGradient(listOf(Color(0xffffa3ff), Color(0xffe35bff), Color(0xffb93bff)))
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
        Text(styled, Modifier.fillMaxWidth(), style = TextStyle(fontSize = 30.sp, lineHeight = 35.sp, fontFamily = DisplayFace, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center))
        Label(subtitle.localized(), 13, Mist, modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            align = TextAlign.Center)
    }
}

/**
 * Circular fingerprint orb with realistic scan lines — matches reference design.
 * Decorative only — real auth via BiometricPrompt.
 */
@Composable
private fun FingerprintOrb(diameter: Int, stage: AuthStage, motion: Boolean, modifier: Modifier = Modifier
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
            drawCircle(Brush.radialGradient(listOf(Pink.copy(alpha = .30f * (0.6f + breath * 0.4f)),
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
                drawLine(if (tick % 3 == 0) Cyan.copy(alpha = tickAlpha) else Pink.copy(alpha = tickAlpha * 0.7f),
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
                val ridgeColor = Brush.linearGradient(listOf(Cyan.copy(alpha = fade * 0.9f), Color(0xff7788ff).copy(alpha = fade * 0.85f), Pink.copy(alpha = fade * 0.75f)),
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
private fun ProviderRow(title: Copy, mark: Mark, tint: Color, tag: String, onClick: () -> Unit
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
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(52.dp))

            // ── Animated brand K orb ──────────────────────────────────────
            Box(Modifier
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
                    drawCircle(Brush.radialGradient(listOf(Color(0xff9922ff).copy(alpha = .34f * (0.6f + pulse * 0.4f)),
                        Color(0xff3300cc).copy(alpha = .18f),
                        Color.Transparent), Offset(cx, cy), r * 1.6f), r * 1.6f, Offset(cx, cy))
                    drawCircle(Brush.radialGradient(listOf(Cyan.copy(alpha = .16f * (0.5f + pulse * 0.5f)),
                        Color.Transparent), Offset(cx * 1.3f, cy * 0.6f), r * 0.9f), r * 0.9f, Offset(cx * 1.3f, cy * 0.6f))
                    drawCircle(Brush.radialGradient(listOf(Pink.copy(alpha = .12f * (0.5f + pulse * 0.5f)),
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
                        val bloom = Brush.radialGradient(listOf(Color(0xffb040ff).copy(alpha = .36f + pulse * .18f), Color.Transparent),
                            Offset(size.width / 2f, size.height / 2f), size.minDimension * 0.9f)
                        drawCircle(bloom, size.minDimension * 0.9f, Offset(size.width / 2f, size.height / 2f))
                    }
                    Glyph(Mark.K, Modifier.size(72.dp), Chalk, true)
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Main title ───────────────────────────────────────────────
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                    .graphicsLayer { alpha = titleAlpha },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val titleText = buildAnnotatedString {
                    withStyle(SpanStyle(color = Chalk)) {
                        append((Copy(R.string.k_your_device_s)).localized())
                    }
                    withStyle(SpanStyle(brush = Brush.linearGradient(listOf(Cyan, Color(0xff8844ff), Pink),
                        start = Offset(0f, 0f), end = Offset(400f, 0f)
                    ))) {
                        append("\n")
                        append((Copy(R.string.k_ai_guardian)).localized())
                    }
                }
                Text(titleText,
                    Modifier.fillMaxWidth(),
                    style = TextStyle(fontSize = 34.sp, lineHeight = 40.sp,
                        fontFamily = DisplayFace, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center
                    )
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── Tagline ──────────────────────────────────────────────────
            Label((Copy(R.string.k_clean_protect_optimize_analyze)).localized(),
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
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).graphicsLayer { alpha = tagAlpha },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(Copy(R.string.k_ai_powered) to Cyan,
                    Copy(R.string.k_t_10_tools) to Violet,
                    Copy(R.string.k_pro_ready) to Gold
                ).forEachIndexed { i, (label, color) ->
                    if (i > 0) Spacer(Modifier.width(8.dp))
                    NeonPill(label.localized(), color, solid = false)
                }
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(32.dp))

            // ── THE ENTER BUTTON ─────────────────────────────────────────
            Box(Modifier
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
                            drawCircle(Brush.radialGradient(listOf(Color(0xffff36f5).copy(alpha = alpha * (0.7f + pulse * 0.3f)),
                                    Color(0xff6622ff).copy(alpha = alpha * .8f), Color.Transparent),
                                Offset(cx, cy), radius), radius, Offset(cx, cy))
                        }
                    }
                }

                // Button surface
                Box(Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .drawBehind {
                            val w = size.width; val h = size.height
                            val rad = androidx.compose.ui.geometry.CornerRadius(h / 2f)
                            // Brand gradient
                            drawRoundRect(Brush.horizontalGradient(listOf(Color(0xffff36f5), Color(0xffa826ff),
                                    Color(0xff344dff), Color(0xff00d8ff)
                                )), cornerRadius = rad)
                            // Shimmer band
                            if (shimmer != null) {
                                val travel = shimmer.value * w * 1.6f - w * .3f
                                drawRoundRect(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = .30f), Color.Transparent),
                                        start = androidx.compose.ui.geometry.Offset(travel - w * .24f, 0f),
                                        end = androidx.compose.ui.geometry.Offset(travel + w * .24f, h)
                                    ), cornerRadius = rad)
                            }
                            // Top lip highlight
                            drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = .38f), Color.Transparent),
                                    startY = 0f, endY = h * .55f), cornerRadius = rad)
                            // Bright rim
                            drawRoundRect(Brush.horizontalGradient(listOf(Color.White.copy(alpha = .88f), Color.Transparent, Cyan.copy(alpha = .82f))),
                                cornerRadius = rad, style = Stroke(1.3.dp.toPx()))
                        }
                        .clickable(interactionSource = ctaInteraction,
                            indication = null,
                            role = Role.Button
                        ) {
                            TouchFeedback.tap(context)
                            onEnter()
                        }
                        .testTag("landing-enter"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Glyph(Mark.K, Modifier.size(22.dp), Chalk, true)
                        Label((Copy(R.string.k_get_started)).localized(),
                            17, Chalk, FontWeight.ExtraBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy((-5).dp)) {
                            val flip = if (LocalArabic.current) 180f else 0f
                            Glyph(Mark.NEXT, Modifier.size(14.dp).rotate(flip), Chalk)
                            Glyph(Mark.NEXT, Modifier.size(14.dp).rotate(flip), Chalk.copy(alpha = .55f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── Skip / guest link ────────────────────────────────────────
            Box(Modifier
                    .graphicsLayer { alpha = ctaAlpha }
                    .clickable(interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onEnter
                    )
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Label((Copy(R.string.k_explore_without_signing_in)).localized(),
                    13, Mist.copy(alpha = 0.55f)
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

/** Welcome state — full hexagonal cyberpunk identity portal. */
@Composable
private fun WelcomeStage(motion: Boolean, onUnlock: () -> Unit, onPasskey: () -> Unit, onGoogle: () -> Unit,
    onGuest: () -> Unit, onPrivacy: () -> Unit, onSettings: () -> Unit,
    onCredentials: () -> Unit, onRecovery: () -> Unit, onSignUp: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var secret by rememberSaveable { mutableStateOf("") }
    var revealed by rememberSaveable { mutableStateOf(false) }
    var remember by rememberSaveable { mutableStateOf(false) }

    Column(Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Utility row: PRO capsule and the settings gear ──────────────────
        Row(Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(Modifier
                    .heightIn(min = 38.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xff2a1c05), Color(0xff150d24))), RoundedCornerShape(19.dp))
                    .border(1.dp, Gold.copy(alpha = .88f), RoundedCornerShape(19.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Glyph(Mark.CROWN, Modifier.size(15.dp), Gold)
                Label("PRO", 12, Gold, FontWeight.Bold, maxLines = 1)
            }
            Spacer(Modifier.width(4.dp))
            Box(Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .clickable(role = Role.Button, onClick = onSettings)
                    .testTag("auth-settings"),
                contentAlignment = Alignment.Center
            ) { Glyph(Mark.GEAR, Modifier.size(23.dp), Chalk.copy(alpha = .92f)) }
        }

        Spacer(Modifier.height(2.dp))

        // ── Brand lockup with the lit underline ────────────────────────────
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Glyph(Mark.K, Modifier.size(34.dp), Chalk, true)
                    Label("knoux", 28, Chalk, FontWeight.ExtraBold, maxLines = 1)
                    Label(" ai", 28, Color(0xffff5cf0), FontWeight.ExtraBold, maxLines = 1)
                }
                Canvas(Modifier.width(170.dp).height(8.dp)) {
                    val path = Path().apply {
                        moveTo(4f, size.height * .46f)
                        cubicTo(size.width * .30f, size.height * .08f,
                            size.width * .58f, size.height * .92f,
                            size.width - 4f, size.height * .30f
                        )
                    }
                    drawPath(path,
                        Brush.horizontalGradient(listOf(Color(0xffff4ff0), Violet, Color(0xff5a6bff), Cyan)),
                        style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Title ──────────────────────────────────────────────────────────
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            val titleStyle = buildAnnotatedString {
                withStyle(SpanStyle(color = Chalk)) { append((Copy(R.string.k_welcome)).localized()) }
                withStyle(SpanStyle(brush = Brush.horizontalGradient(listOf(Color(0xffff5cf0), Color(0xffd24bff), Color(0xffa06bff)))
                    )
                ) { append((Copy(R.string.k_back)).localized()) }
            }
            Text(titleStyle, Modifier.fillMaxWidth(),
                style = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontFamily = DisplayFace,
                    fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center
                )
            )
            Spacer(Modifier.height(4.dp))
            Label((Copy(R.string.k_sign_in_to_your_knoux_ai_account)).localized(),
                13, Mist.copy(alpha = .86f),
                modifier = Modifier.fillMaxWidth(), align = TextAlign.Center
            )
        }

        Spacer(Modifier.height(10.dp))

        // Quiet reassurance line: no filled banner competing with the form.
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Glyph(Mark.SHIELD, Modifier.size(16.dp), Mint.copy(alpha = .9f))
            Label((Copy(R.string.auth_secure_access_note)).localized(),
                12, Mist.copy(alpha = .9f), FontWeight.Medium,
                modifier = Modifier.weight(1f), maxLines = 2
            )
        }

        Spacer(Modifier.height(8.dp))

        // ── Credentials ────────────────────────────────────────────────────
        GlassField(value = email,
            onValueChange = { email = it },
            mark = Mark.MAIL,
            placeholder = (Copy(R.string.k_email_address)).localized(),
            tag = "auth-email",
            keyboard = KeyboardType.Email
        )

        Spacer(Modifier.height(10.dp))

        GlassField(value = secret,
            onValueChange = { secret = it },
            mark = Mark.LOCK,
            placeholder = (Copy(R.string.k_password)).localized(),
            tag = "auth-password",
            keyboard = KeyboardType.Password,
            obscure = !revealed
        ) {
            Box(Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(role = Role.Button, onClick = { revealed = !revealed })
                    .testTag("auth-reveal"),
                contentAlignment = Alignment.Center
            ) { Glyph(Mark.EYE, Modifier.size(20.dp), Mist.copy(alpha = if (revealed) 1f else .70f)) }
        }

        Spacer(Modifier.height(8.dp))

        // ── Remember me / Forgot password ──────────────────────────────────
        Row(Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Checkbox, onClick = { remember = !remember })
                    .testTag("auth-remember")
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SelectionIndicator(remember, Violet)
                Label((Copy(R.string.k_remember_me)).localized(), 12, Chalk.copy(alpha = .90f), maxLines = 1)
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClick = onRecovery)
                    .testTag("auth-forgot")
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Label((Copy(R.string.k_forgot_password)).localized(), 12, Cyan, FontWeight.SemiBold, maxLines = 1)
            }
        }

        Spacer(Modifier.height(10.dp))

        SignInCta((Copy(R.string.k_sign_in)).localized(), onCredentials)

        Spacer(Modifier.height(10.dp))

        SectionDivider((Copy(R.string.k_or_continue_with)).localized())

        Spacer(Modifier.height(10.dp))

        ReferenceProviderRow(Copy(R.string.k_continue_with_google), Mark.GOOGLE, "auth-google", onGoogle)

        Spacer(Modifier.height(10.dp))

        ReferenceProviderRow(Copy(R.string.k_use_a_passkey_2), Mark.KEY, "auth-passkey", onPasskey)

        Spacer(Modifier.height(2.dp))

        // Genuine biometric capability, kept deliberately small and secondary.
        Box(Modifier
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(24.dp))
                .clickable(role = Role.Button, onClick = onUnlock)
                .testTag("auth-biometrics")
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Label((Copy(R.string.k_sign_in_with_biometrics)).localized(), 13, Chalk.copy(alpha = .92f), FontWeight.Medium)
        }

        Spacer(Modifier.height(6.dp))

        Row(Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Label((Copy(R.string.k_don_t_have_an_account)).localized(), 13, Chalk.copy(alpha = .88f), maxLines = 1)
            Box(Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClick = onSignUp)
                    .testTag("auth-signup")
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Label((Copy(R.string.k_sign_up)).localized(), 13, Cyan, FontWeight.Bold, maxLines = 1)
            }
        }

        Spacer(Modifier.height(2.dp))

        Box(Modifier
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(22.dp))
                .clickable(role = Role.Button, onClick = onGuest)
                .testTag("auth-guest")
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Label((Copy(R.string.k_continue_as_guest)).localized(), 13, Chalk.copy(alpha = .9f),
                    FontWeight.Medium, maxLines = 1)
                Label((Copy(R.string.auth_guest_note)).localized(), 11, Mist.copy(alpha = .78f),
                    align = TextAlign.Center, maxLines = 2)
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

/** Glass credential field: icon, hairline, real editable text, optional trailing control. */
@Composable
private fun GlassField(value: String,
    onValueChange: (String) -> Unit,
    mark: Mark,
    placeholder: String,
    tag: String,
    keyboard: KeyboardType,
    obscure: Boolean = false,
    trailing: (@Composable () -> Unit)? = null
) {
    val shape = RoundedCornerShape(20.dp)
    Row(Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xff2b1658).copy(alpha = .92f), Color(0xff140a36).copy(alpha = .94f), Color(0xff101a44).copy(alpha = .92f))
                ),
                shape
            )
            .border(1.dp,
                Brush.horizontalGradient(listOf(Violet.copy(alpha = .85f), Color(0xff4a7dff).copy(alpha = .70f), Cyan.copy(alpha = .75f))),
                shape
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Glyph(mark, Modifier.size(20.dp), Mist.copy(alpha = .90f))
        Box(Modifier.width(1.dp).height(24.dp).background(Mist.copy(alpha = .28f)))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Label(placeholder, 15, Mist.copy(alpha = .58f), maxLines = 1)
            }
            BasicTextField(value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth().testTag(tag),
                singleLine = true,
                textStyle = TextStyle(color = Chalk, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(Cyan),
                keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = ImeAction.Next),
                visualTransformation = if (obscure) PasswordVisualTransformation() else VisualTransformation.None
            )
        }
        trailing?.invoke()
    }
}

/** Gradient sign-in pill with the icon/divider/label arrangement of reference 08. */
@Composable
private fun SignInCta(label: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .972f else 1f, tween(120), label = "signin scale")
    val pulse = if (LocalPreviewMotion.current) {
        val spin = rememberInfiniteTransition(label = "signin pulse")
        spin.animateFloat(0f, 1f,
            infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "signin glow"
        )
    } else null

    Row(Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .buttonBloom(true, pulse)
            .clip(RoundedCornerShape(30.dp))
            .primarySurface(null, pulse)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .testTag("auth-signin")
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Glyph(Mark.NEXT, Modifier.size(21.dp).rotate(if (LocalArabic.current) 180f else 0f), Chalk, true)
        Box(Modifier
                .padding(horizontal = 14.dp)
                .width(1.dp)
                .height(22.dp)
                .background(Chalk.copy(alpha = .60f))
        )
        Label(label, 18, Chalk, FontWeight.ExtraBold, maxLines = 1)
    }
}

/** Thin "or continue with" rule pair. */
@Composable
private fun SectionDivider(label: String) {
    Row(Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, Mist.copy(alpha = .38f)))
            )
        )
        Label(label, 12, Mist.copy(alpha = .78f), maxLines = 1)
        Box(Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(Mist.copy(alpha = .38f), Color.Transparent))
            )
        )
    }
}

/** Provider row: brand glyph, hairline divider, centred label — reference 08 geometry. */
@Composable
private fun ReferenceProviderRow(title: Copy, mark: Mark, tag: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Row(Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xff2a1656).copy(alpha = .88f), Color(0xff150a38).copy(alpha = .92f), Color(0xff171a4a).copy(alpha = .88f))
                ),
                shape
            )
            .border(1.dp,
                Brush.horizontalGradient(listOf(Violet.copy(alpha = .60f), Cyan.copy(alpha = .50f))),
                shape
            )
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(tag)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (mark == Mark.GOOGLE) GoogleG(22) else Glyph(mark, Modifier.size(22.dp), Chalk.copy(alpha = .92f), true)
        Box(Modifier
                .padding(horizontal = 12.dp)
                .width(1.dp)
                .height(22.dp)
                .background(Mist.copy(alpha = .35f))
        )
        Label(title.localized(), 16, Chalk, FontWeight.Bold, maxLines = 1)
    }
}

/** Four-colour Google "G" drawn from arcs plus the signature bar. */
@Composable
private fun GoogleG(size: Int) {
    Canvas(Modifier.size(size.dp)) {
        val stroke = this.size.minDimension * .24f
        val inset = stroke / 2f
        val rect = Size(this.size.width - stroke, this.size.height - stroke)
        val topLeft = Offset(inset, inset)
        val blue = Color(0xff4285F4)
        val green = Color(0xff34A853)
        val yellow = Color(0xffFBBC05)
        val red = Color(0xffEA4335)
        drawArc(red, 190f, 60f, false, topLeft, rect, style = Stroke(stroke))
        drawArc(yellow, 130f, 58f, false, topLeft, rect, style = Stroke(stroke))
        drawArc(green, 68f, 60f, false, topLeft, rect, style = Stroke(stroke))
        drawArc(blue, -14f, 78f, false, topLeft, rect, style = Stroke(stroke))
        // The bar that turns the ring into a G.
        drawRect(blue,
            topLeft = Offset(this.size.width * .50f, this.size.height * .42f),
            size = Size(this.size.width * .50f, stroke)
        )
    }
}


@Composable
private fun OtpStage(motion: Boolean, onBack: () -> Unit, onPasskey: () -> Unit, onGoogle: () -> Unit
) {
    var code by rememberSaveable { mutableStateOf("") }
    var seconds by rememberSaveable { mutableStateOf(59) }
    var notice by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(seconds) {
        if (seconds > 0) {
            delay(1000)
            seconds -= 1
        }
    }

    val active = code.length.coerceAtMost(5)
    val resendNotice = (Copy(R.string.k_code_resend_is_not_connected_to_a_provider_in_this_previ)).localized()
    val verifyNotice = Copy(R.string.k_auth_verification_note).localized()

    Column(Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(Modifier
                    .heightIn(min = 38.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xff2a1c05), Color(0xff150d24))), RoundedCornerShape(19.dp))
                    .border(1.dp, Gold.copy(alpha = .88f), RoundedCornerShape(19.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Glyph(Mark.CROWN, Modifier.size(15.dp), Gold)
                Label("PRO", 12, Gold, FontWeight.Bold, maxLines = 1)
            }
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Glyph(Mark.K, Modifier.size(30.dp), Chalk, true)
                    Label("knoux", 25, Chalk, FontWeight.ExtraBold, maxLines = 1)
                    Label(" ai", 25, Color(0xffff5cf0), FontWeight.ExtraBold, maxLines = 1)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Shield focal with the envelope cue and its verification check.
        Box(Modifier.fillMaxWidth().aspectRatio(1.85f), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2f, size.height * .48f)
                val r = size.minDimension * .46f
                drawCircle(Brush.radialGradient(listOf(Violet.copy(alpha = .34f), Cyan.copy(alpha = .16f), Color.Transparent),
                        c, r * 1.7f
                    ), r * 1.7f, c
                )
                drawCircle(Cyan.copy(alpha = .40f), r * 1.30f, c, style = Stroke(1.dp.toPx()))
                // Pedestal disc under the shield.
                val py = c.y + r * 1.05f
                drawOval(Brush.radialGradient(listOf(Cyan.copy(alpha = .55f), Violet.copy(alpha = .24f), Color.Transparent),
                        Offset(c.x, py), r * 1.20f
                    ),
                    topLeft = Offset(c.x - r * 1.20f, py - r * .20f),
                    size = Size(r * 2.40f, r * .40f)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Glyph(Mark.SHIELD, Modifier.size(96.dp), Cyan, true)
            }
            Box(Modifier.size(96.dp), contentAlignment = Alignment.BottomEnd) {
                Box(Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Brush.verticalGradient(listOf(Cyan, Color(0xff16b6d8))), RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center
                ) { Glyph(Mark.CHECK, Modifier.size(17.dp), Ink, true) }
            }
        }

        Spacer(Modifier.height(6.dp))

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            val titleStyle = buildAnnotatedString {
                withStyle(SpanStyle(color = Chalk)) { append((Copy(R.string.k_verify_your)).localized()) }
                withStyle(SpanStyle(brush = Brush.horizontalGradient(listOf(Color(0xffff5cf0), Color(0xffd24bff), Color(0xffa06bff)))
                    )
                ) { append((Copy(R.string.k_identity)).localized()) }
            }
            Text(titleStyle, Modifier.fillMaxWidth(),
                style = TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontFamily = DisplayFace,
                    fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center
                )
            )
            Spacer(Modifier.height(4.dp))
            Label(// Reference 12 says "sent to your email". No mail provider is
                // connected, so claiming a delivery would be false; the layout is
                // identical but the copy states only what is true.
                (Copy(R.string.k_enter_your_6_digit_verification_code)).localized(),
                13, Mist.copy(alpha = .86f),
                modifier = Modifier.fillMaxWidth(), align = TextAlign.Center
            )
        }

        Spacer(Modifier.height(16.dp))

        OtpSlots(code = code, active = active, onCode = { value ->
            code = value.filter { it.isDigit() }.take(6)
            notice = null
        })

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Glyph(Mark.CLOCK, Modifier.size(15.dp), Cyan.copy(alpha = .40f).let { Cyan })
            Spacer(Modifier.width(6.dp))
            Label((Copy(R.string.k_resend_code_in)).localized(), 12, Mist.copy(alpha = .82f), maxLines = 1
            )
            Label(// Locale.ROOT is required: the device locale is Arabic, and a
                // locale-sensitive %d renders Arabic-Indic numerals (٠٠:٥٩), whose
                // zero is a bare dot and is unreadable at this size.
                String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60), 12,
                if (seconds > 0) Cyan else Mist.copy(alpha = .55f), FontWeight.Bold, maxLines = 1
            )
        }

        Spacer(Modifier.height(2.dp))

        Box(Modifier
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(22.dp))
                .clickable(role = Role.Button, onClick = {
                    seconds = 59
                    notice = resendNotice
                })
                .testTag("otp-resend")
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Label((Copy(R.string.k_resend_code)).localized(), 12,
                Cyan.copy(alpha = if (seconds == 0) 1f else .55f), FontWeight.SemiBold, maxLines = 1
            )
        }

        if (notice != null) {
            Spacer(Modifier.height(6.dp))
            Row(Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xff2a1c05).copy(alpha = .70f), RoundedCornerShape(14.dp))
                    .border(1.dp, Gold.copy(alpha = .45f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Glyph(Mark.WARN, Modifier.size(14.dp), Gold)
                Label(notice ?: "", 11, Gold.copy(alpha = .92f), modifier = Modifier.weight(1f), maxLines = 3)
            }
        }

        Spacer(Modifier.height(12.dp))

        VerifyCta((Copy(R.string.k_verify)).localized()) {
            // No OTP provider is configured, so nothing is verified and no session
            // is created. The user is told the truth instead of a fake success.
            notice = verifyNotice
        }

        Spacer(Modifier.height(14.dp))

        SectionDivider((Copy(R.string.k_or)).localized())

        Spacer(Modifier.height(10.dp))

        ReferenceProviderRow(Copy(R.string.k_use_a_passkey_2), Mark.KEY, "otp-passkey", onPasskey)

        Spacer(Modifier.height(10.dp))

        ReferenceProviderRow(Copy(R.string.k_verify_with_google), Mark.GOOGLE, "otp-google", onGoogle)

        Spacer(Modifier.height(10.dp))

        Box(Modifier
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(24.dp))
                .clickable(role = Role.Button, onClick = onBack)
                .testTag("otp-back")
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Glyph(Mark.BACK, Modifier.size(15.dp).rotate(if (LocalArabic.current) 180f else 0f), Mist.copy(alpha = .85f))
                Label((Copy(R.string.k_back_to_sign_in)).localized(), 13, Mist.copy(alpha = .85f), FontWeight.Medium, maxLines = 1)
            }
        }

        Spacer(Modifier.height(18.dp))
    }
}

/**
 * Six real, focusable digit slots backed by one wide input so platform OTP
 * autofill and the IME keep working. No digit is ever pre-filled.
 */
@Composable
private fun OtpSlots(code: String, active: Int, onCode: (String) -> Unit) {
    val arabic = LocalArabic.current
    Box(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(6) { index ->
                val filled = index < code.length
                val focused = index == active && code.length < 6
                val shape = RoundedCornerShape(14.dp)
                Box(Modifier
                        .weight(1f)
                        .aspectRatio(0.78f)
                        .clip(shape)
                        .background(Brush.verticalGradient(listOf(Color(0xff2c1560).copy(alpha = if (filled) .95f else .70f),
                                    Color(0xff120735).copy(alpha = .92f)
                                )
                            ),
                            shape
                        )
                        .border(if (focused) 1.6.dp else 1.dp,
                            Brush.linearGradient(if (focused) listOf(Cyan, Color(0xff7a5bff))
                                else listOf(Violet.copy(alpha = .85f), Cyan.copy(alpha = .62f))
                            ),
                            shape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (filled) code[index].toString() else "",
                        style = TextStyle(color = Chalk, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
        // The real input sits above the decoration with transparent ink.
        BasicTextField(value = code,
            onValueChange = onCode,
            modifier = Modifier
                .fillMaxWidth()
                .matchParentSize()
                .testTag("otp-input")
                .semantics {
                    contentDescription = KnouxStrings.get(
                        R.string.auth_otp_description, arabic,
                    )
                },
            singleLine = true,
            textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
            cursorBrush = SolidColor(Color.Transparent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
        )
    }
}

/** Verify pill — same material as the sign-in call to action. */
@Composable
private fun VerifyCta(label: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .972f else 1f, tween(120), label = "verify scale")
    val pulse = if (LocalPreviewMotion.current) {
        val spin = rememberInfiniteTransition(label = "verify pulse")
        spin.animateFloat(0f, 1f,
            infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "verify glow"
        )
    } else null

    Row(Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .buttonBloom(true, pulse)
            .clip(RoundedCornerShape(30.dp))
            .primarySurface(null, pulse)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .testTag("otp-verify")
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Glyph(Mark.CHECK, Modifier.size(19.dp), Chalk, true)
        Box(Modifier
                .padding(horizontal = 14.dp)
                .width(1.dp)
                .height(22.dp)
                .background(Chalk.copy(alpha = .60f))
        )
        Label(label, 18, Chalk, FontWeight.ExtraBold, maxLines = 1)
    }
}

/** Biometric verification state of the reference. */
@Composable
private fun VerifyStage(motion: Boolean, onCancel: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(10.dp))
        AuthTopBar(onBack = onCancel)
        Spacer(Modifier.height(26.dp))
        AuthTitle(Copy(R.string.k_verify_your_2), Copy(R.string.k_identity),
            Copy(R.string.k_waiting_for_biometric_verification))
        Spacer(Modifier.height(18.dp))
        Box(Modifier.size(268.dp), contentAlignment = Alignment.Center) {
            FingerprintOrb(258, AuthStage.VERIFYING, motion, Modifier.testTag("auth-verify-orb"))
        }
        Spacer(Modifier.height(24.dp))
        AccentButton(Action(Copy(R.string.k_cancel), Mark.BACK), Modifier.fillMaxWidth(0.62f).padding(horizontal = 18.dp),
            primary = false, tag = "auth-cancel", onClick = onCancel)
        Spacer(Modifier.height(24.dp))
    }
}

/** Verified state of the reference. */
@Composable
private fun VerifiedStage(motion: Boolean) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp))
        AuthBrandLockup()
        Spacer(Modifier.height(30.dp))
        Box(Modifier.size(262.dp), contentAlignment = Alignment.Center) {
            FingerprintOrb(252, AuthStage.VERIFIED, motion, Modifier.testTag("auth-verified-orb"))
        }
        Spacer(Modifier.height(26.dp))
        AuthTitle(Copy(R.string.k_identity_2), Copy(R.string.k_verified),
            Copy(R.string.k_welcome_back_to_knoux_ai))
        Spacer(Modifier.weight(1f))
        Label((Copy(R.string.k_opening_your_dashboard)).localized(), 15, Mist,
            modifier = Modifier.padding(bottom = 42.dp), align = TextAlign.Center)
    }
}

/** Biometric unavailable state of the reference. */
@Composable
private fun UnavailableStage(motion: Boolean, onBack: () -> Unit, onSettings: () -> Unit,
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
        AuthTitle(Copy(R.string.k_biometric), Copy(R.string.k_unavailable),
            Copy(R.string.k_choose_another_sign_in_method_to_continue))
        Spacer(Modifier.height(22.dp))
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ProviderRow(Copy(R.string.k_use_a_passkey), Mark.KEY, Pink, "auth-passkey", onPasskey)
            ProviderRow(Copy(R.string.k_continue_with_google), Mark.GOOGLE, Cyan, "auth-google", onGoogle)
            ProviderRow(Copy(R.string.k_continue_as_guest), Mark.PERSON, Violet, "auth-guest", onGuest)
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Compact glass bar used by the verifying and unavailable states. */
@Composable
private fun AuthTopBar(onBack: () -> Unit, onSettings: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp).glass(20, tab = false)
        .heightIn(min = 56.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        IconControl(Mark.BACK, (Copy(R.string.k_back_2)).localized(), "back", onBack)
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
        IconControl(Mark.GEAR, (Copy(R.string.k_settings)).localized(), "settings", onSettings)
    }
}
