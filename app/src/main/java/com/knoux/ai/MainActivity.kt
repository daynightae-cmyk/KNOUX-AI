package com.knoux.ai

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
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
                                KnouxSplashRoute(splashReduced, freezeSplash, onSmileCompleted = {
                                    if (this@MainActivity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                                        chime?.play()
                                    }
                                    dashboardPrepared = true
                                }) { showSplash = false }
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
                        { headerSheet = HeaderSheet.SETTINGS })
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
                            HeaderSheet.PRO -> Label(("This is a preview of the Pro badge. Purchases and subscriptions are unavailable." ar
                                "هذه معاينة لشارة Pro. الشراء والاشتراكات غير متاحة.").localized(), 14, Mist)
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
                            }
                            HeaderSheet.SETTINGS -> {
                                Label(Disclosure.localized(), 13, Mist)
                                val ar = LocalArabic.current
                                AdaptiveActions(Action("English" ar "الإنجليزية", Mark.CHECK), Action("العربية" ar "العربية", Mark.CHECK),
                                    firstSelected = !ar, secondSelected = ar,
                                    onFirst = { language(false) }, onSecond = { language(true) })
                                AccentButton(Action(if (reduced) "Enable decorative motion" ar "تشغيل الحركة" else "Reduce motion" ar "تقليل الحركة", Mark.RADAR),
                                    Modifier.fillMaxWidth(), primary = false, tag = "motion-toggle") { reduceMotion(!reduced) }
                            }
                        }
                    }
                }
            }
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
    Box(Modifier.size(48.dp).testTag(tag).graphicsLayer { scaleX = scale; scaleY = scale }
        .background(wash, RoundedCornerShape(20.dp))
        .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
        .semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Glyph(mark, Modifier.size(22.dp).then(if (mirror) Modifier.rotate(180f) else Modifier))
    }
}

@Composable
private fun KnouxTopBar(page: Page, menu: () -> Unit, back: () -> Unit, pro: () -> Unit, settings: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp).glass(20, tab = false)
        .heightIn(min = 56.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (page == Page.HOME) IconControl(Mark.MENU, ("Menu" ar "القائمة").localized(), "menu", menu)
        else IconControl(Mark.BACK, ("Back" ar "رجوع").localized(), "back", back)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Glyph(Mark.K, Modifier.size(31.dp))
                    if (LocalDensity.current.fontScale <= 1.45f) {
                        val wordmark = when {
                            LocalRoomy.current -> 14
                            LocalArabic.current -> 18
                            else -> 21
                        }
                        Label("knoux", wordmark, weight = FontWeight.ExtraBold)
                        Label(" ai", wordmark, Pink, FontWeight.ExtraBold)
                    }
                }
            }
        }
        if (!LocalRoomy.current) {
            Box(Modifier.heightIn(min = 48.dp).testTag("pro")
                .clickable(role = Role.Button, onClick = pro).semantics { contentDescription = "KNOUX AI Pro" },
                contentAlignment = Alignment.Center) {
                Row(Modifier.clip(RoundedCornerShape(18.dp))
                    .border(1.dp, Gold.copy(alpha = .85f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Glyph(Mark.CROWN, Modifier.size(14.dp), Gold)
                    Label("Pro", 11, Gold, FontWeight.Bold)
                }
            }
        }
        IconControl(Mark.GEAR, ("Settings" ar "الإعدادات").localized(), "settings", settings)
    }
}

@Composable
private fun KnouxBottomNavigation(selected: Page, open: (Page) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp).glass(25).selectableGroup().padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        tabs.forEach { (tab, icon) ->
            val active = tab == selected
            val fraction by animateFloatAsState(if (active) 1f else 0f,
                animationSpec = tween(260, easing = androidx.compose.animation.core.FastOutSlowInEasing), label = "tab selection")
            val tone by animateColorAsState(if (active) Chalk else Mist, tween(200), label = "tab tone")
            val iconScale by animateFloatAsState(if (active) 1.05f else 1f, tween(230), label = "tab icon")
            Column(Modifier.weight(1f).heightIn(min = 48.dp).testTag("tab-${tab.id}").clip(RoundedCornerShape(20.dp))
                .selectable(active, role = Role.Tab, onClick = { open(tab) }),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(19.dp)).tabSelection(fraction),
                    contentAlignment = Alignment.Center) {
                    Glyph(icon, Modifier.size(20.dp).graphicsLayer { scaleX = iconScale; scaleY = iconScale }, tone, active)
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
