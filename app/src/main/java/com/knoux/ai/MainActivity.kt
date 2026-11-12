package com.knoux.ai

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

internal val Disclosure = "All figures are sample data from the design. Nothing on your device has been scanned or changed." ar
    "كل الأرقام أمثلة من التصميم. لم يفحص التطبيق أي بيانات ولم يغيّر أي شيء على جهازك."
internal val NoChange = "No action performed — UI preview only." ar "لم يُنفّذ أي إجراء — معاينة للواجهة فقط."
internal val Close = "Close" ar "إغلاق"

internal data class DebugVisit(val page: Page, val number: Int, val still: Boolean)
internal class PreviewPreferences(private val state: SavedStateHandle) : ViewModel() {
    val arabic = state.getStateFlow("arabic", Locale.getDefault().language == "ar")
    val reduceMotion = state.getStateFlow("reduceMotion", false)
    val authenticated = state.getStateFlow("authenticated", false)
    fun language(arabic: Boolean) { state["arabic"] = arabic }
    fun motion(reduced: Boolean) { state["reduceMotion"] = reduced }
    fun authenticate() { state["authenticated"] = true }
}

class MainActivity : FragmentActivity() {
    private val visits = MutableStateFlow<DebugVisit?>(null)
    private var visitCount = 0
    /** Debug capture flag: freezes decorative motion so screenshots are deterministic. */
    private var captureStill = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        if (savedInstanceState == null) debugVisit(intent)
        val previewRoute = if (BuildConfig.DEBUG) intent?.getStringExtra("knoux.preview.route") else null
        val freezeSplash = if (BuildConfig.DEBUG) intent?.getLongExtra("knoux.preview.splash.time", -1L)?.takeIf { it >= 0L } else null
        // Debug-only capture hooks; release builds ignore both extras.
        val previewArabic = savedInstanceState == null && BuildConfig.DEBUG &&
            intent?.getBooleanExtra("knoux.preview.arabic", false) == true
        val previewReduced = savedInstanceState == null && BuildConfig.DEBUG &&
            intent?.getBooleanExtra("knoux.preview.reduceMotion", false) == true
        // Debug-only capture hook: freezes a specific authentication state so the
        // cinematic gateway can be photographed deterministically.
        val previewAuth = if (savedInstanceState == null && BuildConfig.DEBUG) {
            when (intent?.getStringExtra("knoux.preview.auth")) {
                "welcome" -> AuthStage.WELCOME
                "verify" -> AuthStage.VERIFYING
                "verified" -> AuthStage.VERIFIED
                "unavailable" -> AuthStage.UNAVAILABLE
                else -> null
            }
        } else null
        val startSplash = (savedInstanceState == null && !SplashGate.consumed && previewRoute == null && previewAuth == null) ||
            freezeSplash != null
        setContent {
            val preferences: PreviewPreferences = viewModel()
            LaunchedEffect(Unit) {
                if (previewArabic) preferences.language(true)
                if (previewReduced) preferences.motion(true)
            }
            val arabic by preferences.arabic.collectAsStateWithLifecycle()
            val reduced by preferences.reduceMotion.collectAsStateWithLifecycle()
            val authenticated by preferences.authenticated.collectAsStateWithLifecycle()
            val visit by visits.collectAsStateWithLifecycle()
            val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
            val power = LocalContext.current.getSystemService(PowerManager::class.java)
            // The capture "still" flag is read from the activity, not from the
            // collected visit state, so the motion decision is stable from the
            // very first composition. Deriving it from the collected state would
            // flip the flag mid-transition and leave the outgoing screen frozen
            // on top of the incoming one.
            val motion = lifecycle.isAtLeast(Lifecycle.State.RESUMED) && !reduced && !captureStill &&
                ValueAnimator.areAnimatorsEnabled() && power?.isPowerSaveMode != true
            val splashReduced = reduced || !ValueAnimator.areAnimatorsEnabled() || power?.isPowerSaveMode == true
            var showSplash by remember { mutableStateOf(startSplash) }
            val onbPrefs = LocalContext.current.getSharedPreferences("knoux_prefs", Context.MODE_PRIVATE)
            val onboardingDone = remember { onbPrefs.getBoolean("knoux_onboarding_done", false) }
            var showOnboarding by remember { mutableStateOf(false) }
            // The identity gateway sits between the splash and the application.
            var dashboardPrepared by remember { mutableStateOf(!startSplash || splashReduced) }
            val dashReveal = remember { Animatable(if (startSplash && !splashReduced) 0f else 1f) }
            LaunchedEffect(showSplash) {
                if (!showSplash && dashReveal.value < 1f) {
                    dashReveal.animateTo(1f, tween(520, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                }
            }
            KnouxTheme(arabic, motion && !showSplash) {
                Box(Modifier.fillMaxSize()) {
                    when {
                        // A debug route jump is a capture hook: it opens the requested
                        // screen directly and bypasses the identity gateway.
                        previewRoute != null -> KnouxAppScaffold(visit, preferences::language, preferences::motion, reduced)
                        // Cinematic onboarding on first launch
                        !showSplash && showOnboarding -> OnboardingScreen(
                            onComplete = { showOnboarding = false }
                        )
                        // The cinematic gateway is the first surface after the splash.
                        !showSplash && !authenticated && previewAuth == null -> AuthGateway(
                            reducedMotion = reduced,
                            onAuthenticated = { preferences.authenticate() },
                            onGuest = { preferences.authenticate() },
                            onLanguage = preferences::language,
                            onMotion = preferences::motion,
                            reduced = reduced
                        )
                        // A forced debug state is captured on its own, without the shell.
                        !showSplash && previewAuth != null -> AuthGateway(
                            forcedStage = previewAuth,
                            reducedMotion = reduced,
                            onAuthenticated = { preferences.authenticate() },
                            onGuest = { preferences.authenticate() },
                            onLanguage = preferences::language,
                            onMotion = preferences::motion,
                            reduced = reduced
                        )
                        else -> {
                            if (dashboardPrepared || !showSplash) {
                                // Fluid reveal: scale up from 0.95 + fade in as splash exits
                                val scale = if (splashReduced) 1f else 0.95f + 0.05f * dashReveal.value
                                val alpha = if (splashReduced) 1f else dashReveal.value
                                Box(Modifier.fillMaxSize().graphicsLayer {
                                    scaleX = scale; scaleY = scale
                                    this.alpha = alpha
                                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.52f)
                                }) {
                                    KnouxAppScaffold(visit, preferences::language, preferences::motion, reduced)
                                }
                            }
                            if (showSplash) {
                                val chime = rememberSplashChime(!splashReduced && freezeSplash == null)
                                KnouxSplashRoute(
                                    reduced = splashReduced,
                                    freezeAtMs = freezeSplash,
                                    onSmileCompleted = {
                                        if (this@MainActivity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                                            chime?.play()
                                        }
                                        dashboardPrepared = true
                                    },
                                    // The Enter button appears at the end; tapping it dismisses the splash
                                    onEnter = {
                                        if (!onboardingDone) showOnboarding = true
                                        showSplash = false
                                    }
                                ) {
                                    if (!onboardingDone) showOnboarding = true
                                    showSplash = false
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        debugVisit(intent)
    }

    private fun debugVisit(intent: Intent?) {
        if (BuildConfig.DEBUG) {
            captureStill = intent?.getBooleanExtra("knoux.preview.still", false) == true
            Page.from(intent?.getStringExtra("knoux.preview.route"))?.let {
                visits.value = DebugVisit(it, ++visitCount, captureStill)
            }
        }
    }
}

private val tabs = listOf(Page.HOME to Mark.HOME, Page.CLEAN to Mark.BROOM, Page.TOOLS to Mark.GRID, Page.REPORTS to Mark.BARS)
private fun tabTitle(page: Page) = when (page) {
    Page.HOME -> "Home" ar "الرئيسية"
    Page.CLEAN -> "Clean" ar "تنظيف"
    Page.TOOLS -> "Tools" ar "أدوات"
    Page.REPORTS -> "Reports" ar "التقارير"
    else -> page.title
}
private enum class HeaderSheet { MENU, PRO, SETTINGS, DISCLOSURE }

// ── Notification badge system ─────────────────────────────────────────────
/** Per-tab notification counts. Incremented when the user visits a section. */
internal object SectionNotifier {
    private val _counts = mutableStateOf(mapOf<Page, Int>())
    val counts: Map<Page, Int> get() = _counts.value

    private val _messages = mutableStateOf(mapOf<Page, String>())
    val messages: Map<Page, String> get() = _messages.value

    // Seed with onboarding notices so the user sees something on first open
    init { seed() }

    private fun seed() {
        _counts.value = mapOf(
            Page.CLEAN to 2,
            Page.TOOLS to 1,
            Page.REPORTS to 3
        )
        _messages.value = mapOf(
            Page.CLEAN to "2 items ready to clean",
            Page.TOOLS to "App analysis available",
            Page.REPORTS to "3 new insights"
        )
    }

    fun markSeen(page: Page) {
        _counts.value = _counts.value - page
    }

    fun post(page: Page, count: Int, message: String) {
        _counts.value = _counts.value + (page to count)
        _messages.value = _messages.value + (page to message)
    }
}

// ── Haptic + audio touch feedback ────────────────────────────────────────
internal object TouchFeedback {
    private var vibrator: Vibrator? = null
    private var tapTrack: AudioTrack? = null
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        vibrator = try {
            val vm = context.getSystemService(VibratorManager::class.java)
            vm.defaultVibrator
        } catch (_: Exception) {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        // Build a crisp, short tap sound in memory — no asset file needed
        tapTrack = buildTapSound()
    }

    private fun buildTapSound(): AudioTrack? = runCatching {
        val sampleRate = 44100
        val durationMs = 60
        val samples = sampleRate * durationMs / 1000
        val buf = ShortArray(samples)
        // A single-frequency tick (high C6 ≈ 1047 Hz) with very fast decay
        for (i in buf.indices) {
            val t = i.toDouble() / sampleRate
            val envelope = kotlin.math.exp(-t * 55.0) // extremely fast — sounds like a click
            val sine = kotlin.math.sin(2 * PI * 1046.5 * t) + 0.3 * kotlin.math.sin(2 * PI * 2093.0 * t)
            buf[i] = (0.18f * sine * envelope * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
        }
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
        AudioTrack.Builder().setAudioAttributes(attrs).setAudioFormat(format)
            .setBufferSizeInBytes(buf.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC).build()
            .also { it.write(buf, 0, buf.size) }
    }.getOrNull()

    fun tap(context: Context) {
        try {
            vibrator?.vibrate(VibrationEffect.createOneShot(22, 70))
        } catch (_: Exception) {}
        val am = context.getSystemService(AudioManager::class.java)
        if (am?.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            runCatching { tapTrack?.reloadStaticData(); tapTrack?.play() }
        }
    }

    fun release() {
        runCatching { tapTrack?.stop(); tapTrack?.release() }
        tapTrack = null; vibrator = null; initialized = false
    }
}

private fun NavHostController.switchTab(page: Page) {
    if (page == Page.HOME && popBackStack(Page.HOME.id, inclusive = false)) return
    if (currentDestination?.route == page.id) return
    navigate(page.id) {
        popUpTo(Page.HOME.id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun KnouxAppScaffold(
    visit: DebugVisit?, language: (Boolean) -> Unit, reduceMotion: (Boolean) -> Unit, reduced: Boolean
) {
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()
    val page = Page.from(current?.destination?.route) ?: Page.HOME
    val tabPages = remember { setOf(Page.HOME, Page.CLEAN, Page.TOOLS, Page.REPORTS) }
    val previous = Page.from(nav.previousBackStackEntry?.destination?.route)
    val groupTab = page.reviewTab
    val selected = when {
        page in tabPages -> page
        // A real section-to-section jump (Clean, Tools, Reports) wins. A direct entry
        // from Home still shows the service's own section, matching the design references.
        previous != null && previous in tabPages && previous != Page.HOME -> previous
        groupTab != null -> groupTab
        previous != null -> previous
        else -> Page.HOME
    }
    var headerSheet by remember { mutableStateOf<HeaderSheet?>(null) }
    val back: () -> Unit = { if (!nav.popBackStack()) nav.switchTab(Page.HOME) }
    val open: (Page) -> Unit = { next ->
        if (next in tabs.map { it.first }) nav.switchTab(next)
        else nav.navigate(next.id) { launchSingleTop = true }
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xff190c52), Color(0xff090426), Color(0xff020008))))
        .drawBehind {
            val top = Offset(size.width * .78f, size.height * .01f)
            drawCircle(Brush.radialGradient(listOf(Color(0xff6d24ff).copy(alpha = .42f),
                Color(0xff2c1570).copy(alpha = .22f), Color.Transparent), top, size.width * .95f), size.width * .95f, top)
            val high = Offset(size.width * .14f, size.height * .13f)
            drawCircle(Brush.radialGradient(listOf(Light.magenta.copy(alpha = .16f), Color.Transparent), high, size.width * .48f),
                size.width * .48f, high)
            val low = Offset(size.width * .10f, size.height * .88f)
            drawCircle(Brush.radialGradient(listOf(Cyan.copy(alpha = .16f), Color.Transparent), low, size.width * .70f),
                size.width * .70f, low)
        }.safeDrawingPadding()
        .semantics { testTagsAsResourceId = BuildConfig.DEBUG },
        contentAlignment = Alignment.TopCenter) {
        BoxWithConstraints(Modifier.widthIn(max = 540.dp).fillMaxSize()) {
            val roomy = maxWidth < 350.dp || LocalDensity.current.fontScale > 1.24f
            val navMotion = LocalPreviewMotion.current
            CompositionLocalProvider(LocalRoomy provides roomy, LocalTall provides (maxHeight >= 800.dp && !roomy)) {
                Column(Modifier.fillMaxSize()) {
                    KnouxTopBar(page, { headerSheet = HeaderSheet.MENU }, back, { headerSheet = HeaderSheet.PRO },
                        { headerSheet = HeaderSheet.SETTINGS }, { headerSheet = HeaderSheet.DISCLOSURE })
                    NavHost(navController = nav, startDestination = Page.HOME.id, modifier = Modifier.weight(1f),
                        enterTransition = {
                            if (!navMotion) EnterTransition.None
                            else fadeIn(tween(230)) + scaleIn(tween(290), initialScale = 0.96f) +
                                slideInHorizontally(tween(280)) { it / 20 }
                        },
                        exitTransition = {
                            if (!navMotion) ExitTransition.None
                            else fadeOut(tween(160)) + scaleOut(tween(180), targetScale = 0.97f)
                        },
                        popEnterTransition = {
                            if (!navMotion) EnterTransition.None
                            else fadeIn(tween(230)) + scaleIn(tween(290), initialScale = 0.97f) +
                                slideInHorizontally(tween(280)) { -it / 20 }
                        },
                        popExitTransition = {
                            if (!navMotion) ExitTransition.None
                            else fadeOut(tween(160)) + scaleOut(tween(180), targetScale = 0.95f)
                        }
                    ) {
                        Page.entries.forEach { target ->
                            composable(target.id) {
                                when (target) {
                                    Page.HOME -> HomeScreen(open)
                                    Page.CLEAN -> SmartCleanScreen(open)
                                    Page.TOOLS -> DeviceToolsScreen(open)
                                    Page.REPORTS -> ReportsScreen(open)
                                    Page.PRIVACY -> PrivacySweepScreen(open)
                                    Page.OPTIMIZE -> AiOptimizationScreen(open)
                                    Page.KNOUX_UNIVERSE -> KnouxUniverseScreen()
                                    in Page.detailRoutes -> DetailPreview(target)
                                    else -> ReferenceScreen(target, openReview = open)
                                }
                            }
                        }
                    }
                    KnouxBottomNavigation(selected) { nav.switchTab(it) }
                }
                LaunchedEffect(visit) {
                    if (visit != null) {
                        headerSheet = null
                        nav.popBackStack(Page.HOME.id, inclusive = false)
                        if (visit.page != Page.HOME) open(visit.page)
                    }
                }
                headerSheet?.let { shown ->
                    val title = when (shown) {
                        HeaderSheet.MENU -> "Explore KNOUX AI" ar "استكشف KNOUX AI"
                        HeaderSheet.PRO -> "KNOUX AI Pro" ar "KNOUX AI Pro"
                        HeaderSheet.SETTINGS -> "Preview settings" ar "إعدادات المعاينة"
                        HeaderSheet.DISCLOSURE -> "Design preview" ar "معاينة التصميم"
                    }
                    InfoSheet(title, { headerSheet = null }) {
                        when (shown) {
                            HeaderSheet.DISCLOSURE -> Label(Disclosure.localized(), 14, Mist)
                            HeaderSheet.PRO -> ProSheet()
                            HeaderSheet.MENU -> {
                                val choices = if (BuildConfig.DEBUG) Page.references else tabs.map { it.first }
                                choices.forEach { next ->
                                    AccentButton(Action(next.title, next.hero), Modifier.fillMaxWidth(), primary = false,
                                        tag = "inspect-${next.id}") {
                                        headerSheet = null
                                        if (next != page) open(next)
                                    }
                                }
                                AccentButton(Action("KNOUX AI Pro" ar "KNOUX AI Pro", Mark.CROWN), Modifier.fillMaxWidth(), primary = false) {
                                    headerSheet = HeaderSheet.PRO
                                }
                                AccentButton(Action(Page.KNOUX_UNIVERSE.title, Page.KNOUX_UNIVERSE.hero), Modifier.fillMaxWidth(), primary = false,
                                    tag = "inspect-${Page.KNOUX_UNIVERSE.id}") {
                                    headerSheet = null
                                    open(Page.KNOUX_UNIVERSE)
                                }
                            }
                            HeaderSheet.SETTINGS -> {
                                // App version
                                GlassPanel(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)), radius = 16) {
                                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column {
                                            Label(("KNOUX AI" ar "KNOUX AI").localized(), 16, Chalk, FontWeight.Bold)
                                            Label(("Version 1.0 · Phase 1 Preview" ar "الإصدار 1.0 · معاينة المرحلة الأولى").localized(), 11, Mist.copy(alpha = 0.7f))
                                        }
                                        Box(Modifier.clip(RoundedCornerShape(10.dp))
                                            .background(Brush.horizontalGradient(listOf(Color(0xff2a1c08), Color(0xff1a0d2a))), RoundedCornerShape(10.dp))
                                            .border(1.dp, Gold.copy(alpha = .9f), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)) {
                                            Label("PRO", 10, Gold, FontWeight.Bold)
                                        }
                                    }
                                }
                                // Language
                                Label(("🌐  " + ("Language" ar "اللغة").localized()), 13, Mist, FontWeight.Medium,
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                                val ar = LocalArabic.current
                                AdaptiveActions(
                                    Action("🇬🇧  English" ar "الإنجليزية", Mark.CHECK),
                                    Action("🇸🇦  العربية" ar "العربية", Mark.CHECK),
                                    firstSelected = !ar, secondSelected = ar,
                                    onFirst = { language(false) }, onSecond = { language(true) }
                                )
                                // Motion
                                Label(("⚡  " + ("Animations" ar "الرسوم المتحركة").localized()), 13, Mist, FontWeight.Medium,
                                    modifier = Modifier.fillMaxWidth())
                                AccentButton(
                                    Action(if (reduced) "Enable Animations" ar "تشغيل الرسوم" else "Reduce Motion" ar "تقليل الحركة", Mark.RADAR),
                                    Modifier.fillMaxWidth(), primary = false, tag = "motion-toggle") { reduceMotion(!reduced) }
                                // Disclosure
                                Label(Disclosure.localized(), 11, Mist.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProSheet() {
    val context = LocalContext.current
    var generatedCode by remember { mutableStateOf<String?>(null) }
    val whatsappNumber = "+971503281920"
    val waLink = "https://wa.me/971503281920"

    // Header banner
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
        .background(Brush.linearGradient(listOf(Color(0xff1a0040), Color(0xff002040), Color(0xff001a30))), RoundedCornerShape(18.dp))
        .border(1.dp, Brush.horizontalGradient(listOf(Gold.copy(alpha = .8f), Cyan.copy(alpha = .5f), Gold.copy(alpha = .8f))), RoundedCornerShape(18.dp))
        .padding(20.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Glyph(Mark.CROWN, Modifier.size(28.dp), Gold, true)
                Label(("KNOUX AI Pro" ar "KNOUX AI Pro").localized(), 22, Gold, FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(6.dp))
            Label(("Unlock the full power of AI-driven device care" ar "أطلق القوة الكاملة للعناية الذكية بالجهاز").localized(),
                13, Chalk.copy(alpha = 0.8f), align = TextAlign.Center)
        }
    }

    // Pro features
    val features = listOf(
        Triple(Mark.SPARKLE, "Unlimited AI Deep Scans" ar "فحوصات ذكاء اصطناعي عميقة بلا حدود", Cyan),
        Triple(Mark.SHIELD, "Advanced Privacy Shield" ar "درع الخصوصية المتقدم", Pink),
        Triple(Mark.BOLT, "Priority Performance Boost" ar "تعزيز الأداء بأولوية قصوى", Gold),
        Triple(Mark.CHIP, "AI Optimization Engine" ar "محرك تحسين الذكاء الاصطناعي", Violet),
        Triple(Mark.ROCKET, "Exclusive Pro Features" ar "ميزات Pro حصرية", Mint)
    )
    features.forEach { (icon, title, tint) ->
        GlassPanel(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)), radius = 14, tint = tint) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(icon, tint, diameter = 38)
                Label(title.localized(), 14, Chalk, FontWeight.Medium, modifier = Modifier.weight(1f))
                Glyph(Mark.CHECK, Modifier.size(16.dp), tint, true)
            }
        }
    }

    // Code generator
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
        .background(Brush.linearGradient(listOf(Color(0xff0d0030), Color(0xff001520))), RoundedCornerShape(16.dp))
        .border(1.dp, Violet.copy(alpha = .6f), RoundedCornerShape(16.dp))
        .padding(16.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()) {
            Label(("🎁  " + ("Get a Free Trial Code" ar "احصل على كود تجريبي مجاني").localized()), 14, Chalk, FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(), align = TextAlign.Center)
            if (generatedCode != null) {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(Color(0xff001a10), RoundedCornerShape(12.dp))
                    .border(1.5.dp, Mint.copy(alpha = .8f), RoundedCornerShape(12.dp))
                    .padding(vertical = 12.dp, horizontal = 16.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Label(("Your Trial Code:" ar "كود الكود التجريبي:").localized(), 11, Mist.copy(alpha = 0.7f))
                        Spacer(Modifier.height(4.dp))
                        Label(generatedCode!!, 24, Mint, FontWeight.ExtraBold, align = TextAlign.Center)
                        Spacer(Modifier.height(4.dp))
                        Label(("Send this code to activate Pro" ar "أرسل هذا الكود لتفعيل Pro").localized(), 11, Mist.copy(alpha = 0.6f))
                    }
                }
            }
            AccentButton(
                Action(("✨  Generate Trial Code" ar "✨  توليد كود تجريبي"), Mark.SPARKLE),
                Modifier.fillMaxWidth(), primary = true, tag = "gen-code"
            ) {
                val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
                generatedCode = "KX-" + (1..8).map { chars.random() }.joinToString("")
            }
        }
    }

    // WhatsApp contact
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
        .background(Brush.linearGradient(listOf(Color(0xff003010), Color(0xff001a08))), RoundedCornerShape(16.dp))
        .border(1.5.dp, Color(0xff25D366).copy(alpha = .8f), RoundedCornerShape(16.dp))
        .clickable(role = Role.Button) {
            runCatching {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waLink))
                context.startActivity(intent)
            }
        }
        .padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(24.dp))
                .background(Color(0xff25D366), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center) {
                Glyph(Mark.CHAT, Modifier.size(26.dp), Chalk, true)
            }
            Column(Modifier.weight(1f)) {
                Label(("Contact on WhatsApp" ar "تواصل عبر واتساب").localized(), 15, Chalk, FontWeight.Bold)
                Label(whatsappNumber, 13, Color(0xff25D366), FontWeight.Medium)
                Label(("Tap to activate Pro subscription" ar "اضغط لتفعيل اشتراك Pro").localized(), 11, Mist.copy(alpha = 0.6f))
            }
            Glyph(Mark.NEXT, Modifier.size(20.dp), Color(0xff25D366))
        }
    }
}

@Composable
internal fun IconControl(mark: Mark, label: String, tag: String, onClick: () -> Unit) {
    val mirror = LocalArabic.current && (mark == Mark.BACK || mark == Mark.NEXT)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .92f else 1f, tween(110), label = "icon press")
    val wash by animateColorAsState(if (pressed) Color.White.copy(alpha = .12f) else Color.Transparent,
        tween(120), label = "icon wash")
    val context = LocalContext.current
    Box(Modifier.size(48.dp).testTag(tag).graphicsLayer { scaleX = scale; scaleY = scale }
        .background(wash, RoundedCornerShape(20.dp))
        .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = {
            TouchFeedback.tap(context); onClick()
        })
        .semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Glyph(mark, Modifier.size(22.dp).then(if (mirror) Modifier.rotate(180f) else Modifier))
    }
}

@Composable
private fun KnouxTopBar(page: Page, menu: () -> Unit, back: () -> Unit, pro: () -> Unit, settings: () -> Unit, disclose: () -> Unit) {
    var logoTapCount by remember { mutableIntStateOf(0) }
    var showDevAccess by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) { TouchFeedback.init(context) }
    
    LaunchedEffect(logoTapCount) {
        if (logoTapCount >= 5) {
            showDevAccess = true
            logoTapCount = 0
        } else if (logoTapCount > 0) {
            kotlinx.coroutines.delay(2000)
            logoTapCount = 0
        }
    }
    
    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp).glass(20, tab = false)
        .height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (page == Page.HOME) IconControl(Mark.MENU, ("Menu" ar "القائمة").localized(), "menu", menu)
        else IconControl(Mark.BACK, ("Back" ar "رجوع").localized(), "back", back)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { logoTapCount++ }
                ) {
                    // K-mark logo
                    Glyph(Mark.K, Modifier.size(26.dp))
                    if (LocalDensity.current.fontScale <= 1.45f) {
                        // "knoux" + "ai" on the SAME line, no wrapping
                        val wordmark = when {
                            LocalRoomy.current -> 13
                            LocalArabic.current -> 17
                            else -> 19
                        }
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            Label("knoux", wordmark, Chalk, FontWeight.ExtraBold, maxLines = 1)
                            Label(" ai", wordmark, Pink, FontWeight.ExtraBold, maxLines = 1)
                        }
                    }
                    // Dev mode badge — inline after name, styled like auth chip
                    if (DevAccessManager.isDevModeActive) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.horizontalGradient(listOf(Color(0xff2a1800), Color(0xff1a1430))),
                                    RoundedCornerShape(10.dp)
                                )
                                .border(1.dp, Brush.horizontalGradient(listOf(Gold.copy(alpha = .9f), Cyan.copy(alpha = .5f))), RoundedCornerShape(10.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Glyph(Mark.KEY, Modifier.size(10.dp), Gold)
                                Label("DEV", 9, Gold, FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        PreviewModeDisclosure(disclose)
        if (!LocalRoomy.current) {
            Box(Modifier.heightIn(min = 48.dp).testTag("pro")
                .clickable(role = Role.Button, onClick = {
                    TouchFeedback.tap(context); pro()
                }).semantics { contentDescription = "KNOUX AI Pro" },
                contentAlignment = Alignment.Center) {
                Row(Modifier.clip(RoundedCornerShape(20.dp))
                    .border(1.dp, Gold.copy(alpha = .85f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Glyph(Mark.CROWN, Modifier.size(13.dp), Gold)
                    Label("Pro", 10, Gold, FontWeight.Bold)
                }
            }
        }
        IconControl(Mark.GEAR, ("Settings" ar "الإعدادات").localized(), "settings", settings)
    }

    // Dev Access Dialog - triggered by 5 quick taps on the logo
    if (showDevAccess) {
        DevAccessDialog(
            onDismiss = { showDevAccess = false },
            onAuthenticated = { showDevAccess = false }
        )
    }
}

@Composable
private fun KnouxBottomNavigation(selected: Page, open: (Page) -> Unit) {
    val context = LocalContext.current
    val notifCounts = SectionNotifier.counts
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp).glass(25).selectableGroup().padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        tabs.forEach { (tab, icon) ->
            val active = tab == selected
            val badge = notifCounts[tab] ?: 0
            val fraction by animateFloatAsState(if (active) 1f else 0f,
                animationSpec = tween(260, easing = androidx.compose.animation.core.FastOutSlowInEasing), label = "tab selection")
            val tone by animateColorAsState(if (active) Chalk else Mist, tween(200), label = "tab tone")
            val iconScale by animateFloatAsState(if (active) 1.05f else 1f, tween(230), label = "tab icon")
            Column(Modifier.weight(1f).heightIn(min = 48.dp).testTag("tab-${tab.id}").clip(RoundedCornerShape(20.dp))
                .selectable(active, role = Role.Tab, onClick = {
                    TouchFeedback.tap(context)
                    SectionNotifier.markSeen(tab)
                    open(tab)
                }),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(contentAlignment = Alignment.TopEnd) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(19.dp)).tabSelection(fraction),
                        contentAlignment = Alignment.Center) {
                        Glyph(icon, Modifier.size(20.dp).graphicsLayer { scaleX = iconScale; scaleY = iconScale }, tone, active)
                    }
                    // Notification badge
                    if (badge > 0 && !active) {
                        val badgeAnim by animateFloatAsState(1f, tween(300), label = "badge pop")
                        Box(
                            Modifier
                                .offset(x = 2.dp, y = (-2).dp)
                                .size(16.dp)
                                .graphicsLayer { scaleX = badgeAnim; scaleY = badgeAnim }
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.radialGradient(listOf(Color(0xffff36f5), Color(0xff9922ff))),
                                    RoundedCornerShape(8.dp)
                                )
                                .border(.8.dp, Color.White.copy(alpha = .6f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Label(if (badge > 9) "9+" else badge.toString(), 8, Chalk, FontWeight.Bold, align = TextAlign.Center)
                        }
                    }
                }
                Label(tabTitle(tab).localized(), 11, tone, if (active) FontWeight.Bold else FontWeight.Normal, align = TextAlign.Center)
            }
        }
    }
}

/**
 * Selected tab: a lit gradient disc behind the icon, matching the reference
 * navigation where the active icon sits in a filled magenta-violet circle.
 */
private fun Modifier.tabSelection(fraction: Float): Modifier = drawBehind {
    if (fraction <= .01f) return@drawBehind
    val radius = size.minDimension / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    drawCircle(Brush.radialGradient(listOf(Color(0xffff54ff).copy(alpha = .34f * fraction),
        Color(0xff5a28e8).copy(alpha = .30f * fraction), Color.Transparent), center, radius * 1.5f),
        radius * 1.5f, center)
    drawCircle(Brush.radialGradient(listOf(Color(0xff9a2bff), Color(0xff5a28e8), Color(0xff2448ff)), center, radius),
        radius, center)
    drawCircle(Color.White.copy(alpha = .30f * fraction), radius, center, style = Stroke(1.1.dp.toPx()))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InfoSheet(title: Copy, dismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = dismiss, sheetState = state,
        containerColor = Color(0xff1d1650), contentColor = Chalk) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Label(title.localized(), 23, weight = FontWeight.Bold)
            content()
            AccentButton(Action(Close, Mark.CHECK), Modifier.fillMaxWidth(), primary = false, tag = "close-sheet", onClick = dismiss)
            Spacer(Modifier.height(7.dp))
        }
    }
}

@Composable
private fun ToolIndex(page: Page, open: (Page) -> Unit) {
    val choices = if (page == Page.CLEAN) listOf(Page.CLEAN_CENTER, Page.SCAN, Page.DUPLICATES, Page.RESIDUE)
    else listOf(Page.STORAGE, Page.APPS, Page.PRIVACY, Page.BATTERY, Page.OPTIMIZE)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 11.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)) {
        HeroHeading(page, "Explore the KNOUX AI interface." ar "استكشف واجهة KNOUX AI.")
        choices.forEach { target ->
            AccentButton(Action(target.title, target.hero), Modifier.fillMaxWidth(), primary = false, tag = "open-${target.id}") { open(target) }
        }
    }
}
@Preview(widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PhonePreview() {
    KnouxTheme(false, false) { KnouxAppScaffold(null, {}, {}, false) }
}
