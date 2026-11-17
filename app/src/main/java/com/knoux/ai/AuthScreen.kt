package com.knoux.ai

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.knoux.ai.visual.InstrumentFog
import com.knoux.ai.visual.PortalSeal
import com.knoux.ai.visual.SealState
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * KNOUX AI — the identity gateway.
 *
 * One seal, one honest decision. The gate offers three real entries:
 *  - the platform biometric prompt, which is genuine (`BiometricPrompt`),
 *  - local mode, which opens the device tools with no account at all,
 *  - an account sign-in that is **not connected in this build** and says so.
 *
 * There is no fourth, pretend entry: no provider, credential field or code can
 * create a session here. The seal only opens on a real success callback, and the
 * unavailable state is neutral fog rather than an error, because a device without
 * an enrolled print has not failed — it simply has nothing to read.
 */
internal enum class AuthStage { WELCOME, VERIFYING, VERIFIED, UNAVAILABLE, OTP }

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
    var email by rememberSaveable { mutableStateOf("") }
    val authPrefs = remember(context) { context.getSharedPreferences("knoux_prefs", Context.MODE_PRIVATE) }
    val returning = remember { authPrefs.getBoolean("knoux_auth_seen", false) }
    fun markSeen() {
        authPrefs.edit().putBoolean("knoux_auth_seen", true).commit()
    }

    // Read once per composition: this is the platform's own answer, not a guess.
    val biometricReady = remember(context) {
        BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    fun launchPrompt(returnStage: AuthStage = AuthStage.WELCOME) {
        val activity = context as? FragmentActivity
        val available = BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
        if (activity == null || !available) {
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
                        else -> returnStage
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

    // The VERIFIED state hands over to the application on its own. A forced debug
    // stage stays on screen so it can be captured deterministically.
    LaunchedEffect(stage, forcedStage) {
        if (stage == AuthStage.VERIFIED && forcedStage == null) {
            markSeen()
            delay(2200)
            onAuthenticated()
        }
    }

    BackHandler(enabled = stage != AuthStage.WELCOME && forcedStage == null) {
        stage = AuthStage.WELCOME
    }

    ApprovedSky {
        when (stage) {
            AuthStage.WELCOME -> if (returning) {
                WelcomeBackStage(
                    motion = motion,
                    biometricReady = biometricReady,
                    email = email,
                    onEmail = { email = it },
                    onUnlock = { launchPrompt() },
                    onPasskey = { sheet = "passkey" },
                    onGoogle = { sheet = "google" },
                    onGuest = { markSeen(); onGuest() },
                    onSettings = { sheet = "settings" },
                    onPro = { sheet = "pro" },
                    onCredentials = { sheet = "credentials" },
                    onRecovery = { sheet = "recovery" },
                    onCreate = { sheet = "create" }
                )
            } else {
                WelcomeStage(
                    motion = motion,
                    biometricReady = biometricReady,
                    email = email,
                    onEmail = { email = it },
                    onUnlock = { launchPrompt() },
                    onPasskey = { sheet = "passkey" },
                    onGoogle = { sheet = "google" },
                    onGuest = { markSeen(); onGuest() },
                    onSettings = { sheet = "settings" },
                    onPro = { sheet = "pro" },
                    onCredentials = { sheet = "credentials" },
                    onRecovery = { sheet = "recovery" },
                    onCreate = { sheet = "create" }
                )
            }
            AuthStage.VERIFYING -> VerifyStage(motion = motion, onCancel = { stage = AuthStage.WELCOME })
            AuthStage.VERIFIED -> VerifiedStage(motion = motion, onPro = { sheet = "pro" })
            AuthStage.OTP -> OtpStage(
                motion = motion,
                email = email,
                biometricReady = biometricReady,
                onBack = { stage = AuthStage.WELCOME },
                onPasskey = { sheet = "passkey" },
                onGoogle = { sheet = "google" },
                onBiometric = { launchPrompt(AuthStage.OTP) }
            )
            AuthStage.UNAVAILABLE -> UnavailableStage(
                motion = motion,
                onBack = { stage = AuthStage.WELCOME },
                onSettings = { sheet = "settings" },
                onGuest = { markSeen(); onGuest() }
            )
        }
    }

    sheet?.let { kind ->
        InfoSheet(when (kind) {
            "passkey" -> (Copy(R.string.k_use_a_passkey))
            "google" -> (Copy(R.string.k_continue_with_google))
            "credentials" -> (Copy(R.string.k_sign_in_is_not_configured))
            "recovery" -> (Copy(R.string.k_password_recovery))
            "settings" -> (Copy(R.string.k_preview_settings))
            "pro" -> (Copy(R.string.k_knoux_ai_pro))
            "create" -> (Copy(R.string.k_create_account))
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
                "pro" -> {
                    Label((Copy(R.string.pro_activation_body)).localized(), 13, Mist)
                    Label((Copy(R.string.pro_activation_build_note)).localized(), 12, Gold.copy(alpha = .92f))
                }
                "create" -> {
                    Label((Copy(R.string.k_account_sheet_body)).localized(), 13, Mist)
                    Label((Copy(R.string.k_email_signin_build_note)).localized(), 12, Mist)
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

// ── Stage shells ─────────────────────────────────────────────────────────────

/** The observatory sky, shared with every route behind the gateway. */
@Composable
private fun AuthSky(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(NativePalette.ink)) {
        NativeAtmosphere(Modifier.fillMaxSize())
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                Brush.radialGradient(
                    listOf(NativePalette.violet.copy(alpha = .16f), Color.Transparent),
                    Offset(size.width / 2f, size.height * .30f),
                    size.width * .95f,
                ),
                radius = size.width * .95f,
                center = Offset(size.width / 2f, size.height * .30f),
            )
        }
        content()
    }
}

/**
 * Brand lockup plus the one control this stage may need. The wordmark keeps LTR
 * order in Arabic, exactly as the brand is drawn everywhere else.
 */
@Composable
private fun AuthHeader(onBack: (() -> Unit)? = null, onSettings: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                Modifier.size(44.dp).clip(CircleShape)
                    .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onBack)
                    .testTag("auth-back"),
                contentAlignment = Alignment.Center,
            ) { Glyph(Mark.BACK, Modifier.size(20.dp).rotate(if (LocalArabic.current) 180f else 0f), Chalk) }
            Spacer(Modifier.width(6.dp))
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            KnouxWordmark(size = if (onBack == null) 26 else 21, withSignature = true, glow = true)
        }
        Spacer(Modifier.weight(1f))
        if (onSettings != null) {
            Box(
                Modifier.size(44.dp).clip(CircleShape)
                    .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onSettings)
                    .testTag("auth-settings"),
                contentAlignment = Alignment.Center,
            ) { Glyph(Mark.GEAR, Modifier.size(21.dp), Chalk.copy(alpha = .90f)) }
        }
    }
}

/** Two-weight headline: the lead stays chalk, the second word takes the spectrum. */
@Composable
private fun AuthTitle(lead: Copy, accent: Copy, subtitle: Copy, splitAccent: Boolean = false) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        val gradient = Brush.verticalGradient(listOf(Color(0xffffa3ff), Color(0xffd24bff), Color(0xff9a6bff)))
        val leadText = lead.localized().trimEnd()
        val accentText = accent.localized().trim()
        val styled = buildAnnotatedString {
            withStyle(SpanStyle(color = Chalk)) { append(leadText) }
            if (accentText.isNotEmpty()) {
                if (splitAccent) append("\n") else append(" ")
                withStyle(SpanStyle(brush = gradient)) { append(accentText) }
            }
        }
        Text(styled, Modifier.fillMaxWidth(),
            style = TextStyle(fontSize = 32.sp, lineHeight = 37.sp, fontFamily = DisplayFace,
                fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center
            )
        )
        Label(subtitle.localized(), 13, Mist.copy(alpha = .84f),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp), align = TextAlign.Center)
    }
}

// ── 01 · Welcome ─────────────────────────────────────────────────────────────

@Composable
private fun WelcomeStage(
    motion: Boolean,
    biometricReady: Boolean,
    email: String,
    onEmail: (String) -> Unit,
    onUnlock: () -> Unit,
    onPasskey: () -> Unit,
    onGoogle: () -> Unit,
    onGuest: () -> Unit,
    onSettings: () -> Unit,
    onPro: () -> Unit,
    onCredentials: () -> Unit,
    onRecovery: () -> Unit,
    onCreate: () -> Unit
) {
    var secret by rememberSaveable { mutableStateOf("") }
    var fullName by rememberSaveable { mutableStateOf("") }
    var revealed by rememberSaveable { mutableStateOf(false) }
    var mode by rememberSaveable { mutableStateOf(0) }
    var remember by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(6.dp))
        ApprovedAuthHeader(onPro = onPro, onSettings = onSettings)
        Spacer(Modifier.height(6.dp))

        // The approved credential seal carries a lock, not the brand K.
        PortalSeal(
            SealState.IDLE, 124.dp, motion,
            tag = "auth-seal", face = Mark.LOCK, faceScale = .31f
        )
        Spacer(Modifier.height(4.dp))

        ApprovedHeadline(
            lead = (Copy(R.string.k_welcome_lead)).localized(),
            accent = "KNOUX AI",
            size = 28
        )
        ApprovedSubtitle((Copy(R.string.k_secure_access_device_intelligence)).localized())

        Spacer(Modifier.height(10.dp))

        val privacy = (Copy(R.string.k_private_by_design)).localized()
        InfoBanner(
            Mark.SHIELD,
            privacy.substringBefore(" — "),
            privacy.substringAfter(" — ", "")
        )

        Spacer(Modifier.height(10.dp))

        ApprovedPanel {
            AuthTabs(mode) { mode = it }
            Spacer(Modifier.height(10.dp))

            if (mode == 1) {
                AuthField(
                    fullName, { fullName = it }, Mark.PERSON,
                    (Copy(R.string.k_full_name)).localized(), "auth-name", KeyboardType.Text
                )
                Spacer(Modifier.height(8.dp))
            }

            AuthField(
                email, onEmail, Mark.MAIL,
                (Copy(R.string.k_email_address)).localized(), "auth-email", KeyboardType.Email
            )
            Spacer(Modifier.height(8.dp))

            AuthField(
                secret, { secret = it }, Mark.LOCK,
                (Copy(R.string.k_password)).localized(), "auth-password", KeyboardType.Password,
                obscure = !revealed
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button) { revealed = !revealed }
                        .testTag("auth-reveal"),
                    contentAlignment = Alignment.Center
                ) {
                    Glyph(Mark.EYE, Modifier.size(18.dp), Mist.copy(alpha = if (revealed) 1f else .68f))
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button) { remember = !remember }
                        .testTag("auth-keep"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CheckMark(remember)
                    Spacer(Modifier.width(8.dp))
                    Label(
                        (Copy(R.string.k_keep_me_signed_in)).localized(), 12,
                        Mist.copy(alpha = .88f), maxLines = 1
                    )
                }
                Text(
                    (Copy(R.string.k_forgot_password)).localized(),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button, onClick = onRecovery)
                        .testTag("auth-forgot")
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                    color = AwViolet,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = KnouxFont,
                    textDecoration = TextDecoration.Underline,
                    maxLines = 1
                )
            }

            Spacer(Modifier.height(6.dp))

            val action = if (mode == 1) {
                (Copy(R.string.k_create_account)).localized()
            } else {
                (Copy(R.string.k_sign_in)).localized()
            }
            ApprovedCta(action, if (mode == 1) onCreate else onCredentials, "auth-signin")

            Spacer(Modifier.height(12.dp))
            DividerLabel(
                (Copy(R.string.k_or_continue_with)).localized()
                    .replaceFirstChar { it.uppercase() }
            )
            Spacer(Modifier.height(9.dp))

            ProviderTiles(onGoogle = onGoogle, onPasskey = onPasskey)
            Spacer(Modifier.height(9.dp))

            GlassActionRow(
                Mark.FINGERPRINT, AwCyan,
                (Copy(R.string.k_sign_in_with_biometrics)).localized(),
                "auth-biometrics", onUnlock, enabled = biometricReady
            )
            if (!biometricReady) {
                Spacer(Modifier.height(5.dp))
                Label(
                    (Copy(R.string.auth_biometric_absent)).localized(), 10,
                    Mist.copy(alpha = .70f), align = TextAlign.Center
                )
            }

            Spacer(Modifier.height(7.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 46.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(role = Role.Button, onClick = onGuest)
                    .testTag("auth-guest"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Glyph(Mark.PERSON, Modifier.size(18.dp), AwMagenta, true)
                Spacer(Modifier.width(8.dp))
                Label(
                    (Copy(R.string.k_continue_as_guest)).localized(), 13,
                    AwMagenta, FontWeight.SemiBold, maxLines = 1
                )
            }
        }

        Spacer(Modifier.height(11.dp))
        InfoFootnote((Copy(R.string.k_biometric_production_note)).localized())
        Spacer(Modifier.height(16.dp))
    }
}

/**
 * The approved returning surface: same materials, no account tabs, one honest
 * entry (biometrics) plus the local guest entry.
 */
@Composable
private fun WelcomeBackStage(
    motion: Boolean,
    biometricReady: Boolean,
    email: String,
    onEmail: (String) -> Unit,
    onUnlock: () -> Unit,
    onPasskey: () -> Unit,
    onGoogle: () -> Unit,
    onGuest: () -> Unit,
    onSettings: () -> Unit,
    onPro: () -> Unit,
    onCredentials: () -> Unit,
    onRecovery: () -> Unit,
    onCreate: () -> Unit
) {
    var secret by rememberSaveable { mutableStateOf("") }
    var revealed by rememberSaveable { mutableStateOf(false) }
    var remember by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(6.dp))
        ApprovedAuthHeader(onPro = onPro, onSettings = onSettings, showWordmark = false)
        Spacer(Modifier.height(4.dp))

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            BrandLockupRow(size = 30, tag = "auth-welcome-back-brand")
        }

        Spacer(Modifier.height(8.dp))

        ApprovedHeadline(
            lead = (Copy(R.string.k_welcome_back_lead)).localized(),
            accent = (Copy(R.string.k_welcome_back_accent)).localized(),
            size = 31
        )
        ApprovedSubtitle((Copy(R.string.k_sign_in_to_your_knoux_ai_account)).localized())

        Spacer(Modifier.height(10.dp))

        val secure = (Copy(R.string.k_secure_access_caps)).localized()
        InfoBanner(
            Mark.SHIELD,
            secure.substringBefore(" — "),
            secure.substringAfter(" — ", "")
        )

        Spacer(Modifier.height(10.dp))

        ApprovedPanel {
            AuthField(
                email, onEmail, Mark.MAIL,
                (Copy(R.string.k_email_address)).localized(), "auth-email", KeyboardType.Email
            )
            Spacer(Modifier.height(8.dp))

            AuthField(
                secret, { secret = it }, Mark.LOCK,
                (Copy(R.string.k_password)).localized(), "auth-password", KeyboardType.Password,
                obscure = !revealed
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button) { revealed = !revealed }
                        .testTag("auth-reveal"),
                    contentAlignment = Alignment.Center
                ) {
                    Glyph(
                        if (revealed) Mark.EYE else Mark.EYE,
                        Modifier.size(18.dp), Mist.copy(alpha = if (revealed) 1f else .68f)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button) { remember = !remember }
                        .testTag("auth-remember"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CheckMark(remember)
                    Spacer(Modifier.width(8.dp))
                    Label(
                        (Copy(R.string.k_remember_me)).localized(), 12,
                        Mist.copy(alpha = .88f), maxLines = 1
                    )
                }
                Text(
                    (Copy(R.string.k_forgot_password)).localized(),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button, onClick = onRecovery)
                        .testTag("auth-forgot")
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                    color = AwCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = KnouxFont,
                    textDecoration = TextDecoration.Underline,
                    maxLines = 1
                )
            }

            Spacer(Modifier.height(6.dp))
            ApprovedCta(
                (Copy(R.string.k_sign_in)).localized(),
                onCredentials,
                "auth-signin"
            )
            Spacer(Modifier.height(12.dp))
            DividerLabel((Copy(R.string.k_or_continue_with)).localized())
            Spacer(Modifier.height(9.dp))

            GlassActionRow(
                Mark.GOOGLE, AwBlue,
                (Copy(R.string.k_continue_with_google)).localized(),
                "auth-google", onGoogle
            )
            Spacer(Modifier.height(7.dp))
            GlassActionRow(
                Mark.KEY, AwViolet,
                (Copy(R.string.k_use_a_passkey_2)).localized(),
                "auth-passkey", onPasskey
            )
            Spacer(Modifier.height(7.dp))
            GlassActionRow(
                Mark.FINGERPRINT, AwCyan,
                (Copy(R.string.k_sign_in_with_biometrics)).localized(),
                "auth-biometrics", onUnlock, enabled = biometricReady
            )
            if (!biometricReady) {
                Spacer(Modifier.height(5.dp))
                Label(
                    (Copy(R.string.auth_biometric_absent)).localized(), 10,
                    Mist.copy(alpha = .70f), align = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Label(
                (Copy(R.string.k_don_t_have_an_account)).localized(), 12,
                Mist.copy(alpha = .82f), maxLines = 1
            )
            AccentTextLink(
                (Copy(R.string.k_sign_up)).localized(), AwCyan, onCreate, "auth-signup"
            )
        }

        Box(
            Modifier
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(22.dp))
                .clickable(role = Role.Button, onClick = onGuest)
                .testTag("auth-guest")
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Glyph(Mark.PERSON, Modifier.size(16.dp), AwMagenta)
                Spacer(Modifier.width(7.dp))
                Label(
                    (Copy(R.string.k_continue_as_guest)).localized(), 12,
                    AwMagenta.copy(alpha = .92f), FontWeight.Medium, maxLines = 1
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

/** The approved square checkbox: violet when on, hairline when off. */
@Composable
private fun CheckMark(checked: Boolean) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        Modifier
            .size(21.dp)
            .clip(shape)
            .background(
                if (checked) Brush.linearGradient(listOf(AwViolet, AwBlue)) else Brush.linearGradient(
                    listOf(Color.White.copy(alpha = .10f), Color.White.copy(alpha = .06f))
                ),
                shape,
            )
            .border(1.2.dp, if (checked) AwViolet else AwViolet.copy(alpha = .55f), shape),
        contentAlignment = Alignment.Center
    ) {
        if (checked) Glyph(Mark.CHECK, Modifier.size(13.dp), Chalk, false)
    }
}

/** One icon-sized provider conduit: 62 dp tall, brand glyph centred. */
@Composable
private fun RowScope.ProviderTile(mark: Mark, tint: Color, tag: String, description: String,
    enabled: Boolean = true, onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(Modifier
            .weight(1f)
            .height(66.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(tint.copy(alpha = if (enabled) .20f else .05f),
                Color(0xff0a0d24).copy(alpha = .94f))), shape)
            .border(1.dp, Brush.linearGradient(listOf(tint.copy(alpha = if (enabled) .68f else .26f),
                Color.White.copy(alpha = .10f))), shape)
            .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
            .testTag(tag)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        if (mark == Mark.GOOGLE) GoogleG(26)
        else Glyph(mark, Modifier.size(27.dp), if (enabled) tint else InstrumentFog, enabled)
    }
}

/** The quiet card that tells the truth about what this build can and cannot do. */
@Composable
private fun SecurityNote() {
    Row(Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = .035f), RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = .07f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Glyph(Mark.SHIELD, Modifier.size(18.dp), NativePalette.green)
        Column(Modifier.weight(1f)) {
            Label((Copy(R.string.auth_secure_access_note)).localized(), 11, Chalk.copy(alpha = .88f),
                FontWeight.Medium, maxLines = 2)
            Label((Copy(R.string.auth_signin_not_connected_short)).localized(), 10, Mist.copy(alpha = .70f),
                maxLines = 2)
        }
    }
}

// ── 02 · Verifying ───────────────────────────────────────────────────────────

@Composable
private fun VerifyStage(motion: Boolean, onCancel: () -> Unit) {
    Column(Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        AuthHeader(onBack = onCancel)
        Column(Modifier.weight(1f).fillMaxWidth().padding(bottom = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PortalSeal(SealState.SCANNING, 176.dp, motion, tag = "auth-verify-orb")
            Spacer(Modifier.height(26.dp))
            AuthTitle(Copy(R.string.k_verify_your), Copy(R.string.k_identity),
                Copy(R.string.k_waiting_for_biometric_verification))
        }
        AccentButton(Action(Copy(R.string.k_cancel), Mark.BACK), Modifier.fillMaxWidth(),
            primary = false, tag = "auth-cancel") { onCancel() }
        Spacer(Modifier.height(24.dp))
    }
}

// ── 03 · Verified ────────────────────────────────────────────────────────────

@Composable
private fun VerifiedStage(motion: Boolean, onPro: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(6.dp))

        Row(
            Modifier.fillMaxWidth().heightIn(min = 52.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandLockupRow(size = 21, tag = "auth-verified-brand")
            Spacer(Modifier.weight(1f))
            ProPill(onClick = onPro)
        }

        Column(
            Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PortalSeal(SealState.VERIFIED, 182.dp, motion, tag = "auth-verified-orb")

            Spacer(Modifier.height(14.dp))

            ApprovedHeadline(
                lead = (Copy(R.string.k_identity_2)).localized(),
                accent = (Copy(R.string.k_verified)).localized(),
                size = 33
            )
            ApprovedSubtitle((Copy(R.string.k_welcome_back_to_knoux_ai)).localized(), 14)

            Spacer(Modifier.height(16.dp))

            FingerprintMedallion(124, motion)

            Spacer(Modifier.height(10.dp))

            Label(
                (Copy(R.string.k_authentication_successful)).localized(), 13,
                Mist.copy(alpha = .88f), align = TextAlign.Center
            )
        }

        StepDots(active = 2)
        Spacer(Modifier.height(14.dp))
    }
}

// ── 04 · Unavailable ─────────────────────────────────────────────────────────

@Composable
private fun UnavailableStage(motion: Boolean, onBack: () -> Unit, onSettings: () -> Unit,
    onGuest: () -> Unit
) {
    Column(Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        AuthHeader(onBack = onBack)
        Column(Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PortalSeal(SealState.UNAVAILABLE, 132.dp, motion, tag = "auth-unavailable-orb")
            Spacer(Modifier.height(20.dp))
            AuthTitle(Copy(R.string.k_biometric), Copy(R.string.k_unavailable),
                Copy(R.string.auth_unavailable_body))
            Spacer(Modifier.height(18.dp))
            AccentButton(Action(Copy(R.string.auth_local_entry), Mark.NEXT), Modifier.fillMaxWidth(),
                primary = true, tag = "auth-guest") { onGuest() }
            Spacer(Modifier.height(10.dp))
            AccentButton(Action(Copy(R.string.k_settings), Mark.GEAR), Modifier.fillMaxWidth(),
                primary = false, tag = "auth-open-settings") { onSettings() }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ── 05 · Verification code (provider not connected) ──────────────────────────

@Composable
private fun OtpStage(
    motion: Boolean,
    email: String,
    biometricReady: Boolean,
    onBack: () -> Unit,
    onPasskey: () -> Unit,
    onGoogle: () -> Unit,
    onBiometric: () -> Unit
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
    val timer = String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60)

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(6.dp))
        CenteredBrand(22)
        Spacer(Modifier.height(4.dp))

        PortalSeal(
            SealState.IDLE, 110.dp, motion,
            tag = "otp-seal", face = Mark.LOCK, faceScale = .32f
        )

        Spacer(Modifier.height(2.dp))

        ApprovedHeadline(
            lead = (Copy(R.string.k_verify_your)).localized().trim(),
            accent = (Copy(R.string.k_identity)).localized().trim(),
            size = 29
        )
        ApprovedSubtitle((Copy(R.string.k_we_sent_code_to_email)).localized())

        Spacer(Modifier.height(9.dp))
        EmailChip(email)
        Spacer(Modifier.height(11.dp))

        DividerLabel((Copy(R.string.k_enter_verification_code)).localized(), Mist.copy(alpha = .88f))
        Spacer(Modifier.height(11.dp))

        OtpSlots(code = code, active = active, onCode = { value ->
            code = value.filter { it.isDigit() }.take(6)
            notice = null
        })

        Spacer(Modifier.height(7.dp))

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Row(
                Modifier
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(role = Role.Button, onClick = {
                        seconds = 59
                        notice = resendNotice
                    })
                    .testTag("otp-resend")
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Glyph(Mark.SYNC, Modifier.size(15.dp), AwCyan, true)
                Spacer(Modifier.width(6.dp))
                Label(
                    (Copy(R.string.k_resend_code)).localized(), 12,
                    AwCyan.copy(alpha = if (seconds == 0) 1f else .72f),
                    FontWeight.SemiBold, maxLines = 1
                )
            }
            Spacer(Modifier.width(9.dp))
            Box(Modifier.width(1.dp).height(16.dp).background(Mist.copy(alpha = .28f)))
            Spacer(Modifier.width(9.dp))
            Label(
                timer, 12,
                if (seconds > 0) Mist.copy(alpha = .82f) else AwCyan,
                FontWeight.Bold, maxLines = 1
            )
        }

        if (notice != null) {
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xff2a1c05).copy(alpha = .70f), RoundedCornerShape(14.dp))
                    .border(1.dp, AwGold.copy(alpha = .45f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Glyph(Mark.WARN, Modifier.size(14.dp), AwGold)
                Label(notice ?: "", 11, AwGold.copy(alpha = .92f), modifier = Modifier.weight(1f), maxLines = 3)
            }
        }

        Spacer(Modifier.height(10.dp))

        ApprovedCta(
            (Copy(R.string.k_verify)).localized(),
            { notice = verifyNotice },
            "otp-verify"
        )

        Spacer(Modifier.height(12.dp))
        DividerLabel((Copy(R.string.k_or)).localized())
        Spacer(Modifier.height(8.dp))

        GlassActionRow(
            Mark.FINGERPRINT, AwCyan,
            (Copy(R.string.k_use_fingerprint)).localized(),
            "otp-fingerprint", onBiometric, enabled = biometricReady
        )
        Spacer(Modifier.height(7.dp))
        GlassActionRow(
            Mark.LOCK, AwViolet,
            (Copy(R.string.k_use_password)).localized(),
            "otp-password", onBack
        )
        Spacer(Modifier.height(7.dp))
        GlassActionRow(
            Mark.GOOGLE, AwBlue,
            (Copy(R.string.k_continue_with_google)).localized(),
            "otp-google", onGoogle
        )

        if (!biometricReady) {
            Spacer(Modifier.height(5.dp))
            Label(
                (Copy(R.string.auth_biometric_absent)).localized(), 10,
                Mist.copy(alpha = .70f), align = TextAlign.Center
            )
        }

        Spacer(Modifier.height(13.dp))

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Glyph(Mark.SHIELD, Modifier.size(16.dp), AwCyan, true)
            Spacer(Modifier.width(7.dp))
            Label(
                (Copy(R.string.k_identity_protected)).localized(), 12,
                Mist.copy(alpha = .80f), align = TextAlign.Center, maxLines = 2
            )
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
                        .height(56.dp)
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
            .clickable(interactionSource = interaction, indication = null,
                role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
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

// ── Shared field / action materials ──────────────────────────────────────────

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
    val shape = RoundedCornerShape(18.dp)
    Row(Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xff241352).copy(alpha = .90f), Color(0xff120a30).copy(alpha = .94f),
                Color(0xff0d1538).copy(alpha = .90f))), shape)
            .border(1.dp,
                Brush.horizontalGradient(listOf(Violet.copy(alpha = .72f), Color(0xff4a7dff).copy(alpha = .55f),
                    Cyan.copy(alpha = .62f))), shape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Glyph(mark, Modifier.size(19.dp), Mist.copy(alpha = .88f))
        Box(Modifier.width(1.dp).height(22.dp).background(Mist.copy(alpha = .24f)))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Label(placeholder, 14, Mist.copy(alpha = .56f), maxLines = 1)
            }
            BasicTextField(value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth().testTag(tag),
                singleLine = true,
                textStyle = TextStyle(color = Chalk, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(Cyan),
                keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = ImeAction.Next),
                visualTransformation = if (obscure) PasswordVisualTransformation() else VisualTransformation.None
            )
        }
        trailing?.invoke()
    }
}

/** Gradient call to action: arrow, hairline, label. */
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
            .heightIn(min = 56.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .buttonBloom(true, pulse)
            .clip(RoundedCornerShape(28.dp))
            .primarySurface(null, pulse)
            .clickable(interactionSource = interaction, indication = null,
                role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
            .testTag("auth-signin")
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Glyph(Mark.NEXT, Modifier.size(20.dp).rotate(if (LocalArabic.current) 180f else 0f), Chalk, true)
        Box(Modifier
                .padding(horizontal = 14.dp)
                .width(1.dp)
                .height(22.dp)
                .background(Chalk.copy(alpha = .60f))
        )
        Label(label, 17, Chalk, FontWeight.ExtraBold, maxLines = 1)
    }
}

/** Thin "or continue with" rule pair. */
@Composable
private fun SectionDivider(label: String) {
    Row(Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent,
            Mist.copy(alpha = .34f)))))
        Label(label, 12, Mist.copy(alpha = .74f), maxLines = 1)
        Box(Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(Mist.copy(alpha = .34f),
            Color.Transparent))))
    }
}

/** Four-colour Google "G" drawn from arcs plus the signature bar. */
@Composable
internal fun GoogleG(size: Int) {
    Canvas(Modifier.size(size.dp)) {
        val stroke = this.size.minDimension * .24f
        val inset = stroke / 2f
        val rect = Size(this.size.width - stroke, this.size.height - stroke)
        val topLeft = Offset(inset, inset)
        val blue = Color(0xff4285F4)
        val green = Color(0xff34A853)
        val yellow = Color(0xffFBBC05)
        val red = Color(0xffEA4335)
        drawArc(red, 190f, 60f, false, topLeft, rect, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
        drawArc(yellow, 130f, 58f, false, topLeft, rect, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
        drawArc(green, 68f, 60f, false, topLeft, rect, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
        drawArc(blue, -14f, 78f, false, topLeft, rect, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
        // The bar that turns the ring into a G.
        drawRect(blue,
            topLeft = Offset(this.size.width * .50f, this.size.height * .42f),
            size = Size(this.size.width * .50f, stroke)
        )
    }
}
