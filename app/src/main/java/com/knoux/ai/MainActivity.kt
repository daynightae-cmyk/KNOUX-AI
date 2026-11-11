package com.knoux.ai

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    fun language(arabic: Boolean) { state["arabic"] = arabic }
    fun motion(reduced: Boolean) { state["reduceMotion"] = reduced }
}

class MainActivity : ComponentActivity() {
    private val visits = MutableStateFlow<DebugVisit?>(null)
    private var visitCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        if (savedInstanceState == null) debugVisit(intent)
        val previewRoute = if (BuildConfig.DEBUG) intent?.getStringExtra("knoux.preview.route") else null
        val freezeSplash = if (BuildConfig.DEBUG) intent?.getLongExtra("knoux.preview.splash.time", -1L)?.takeIf { it >= 0L } else null
        val startSplash = (savedInstanceState == null && !SplashGate.consumed && previewRoute == null) || freezeSplash != null
        setContent {
            val preferences: PreviewPreferences = viewModel()
            val arabic by preferences.arabic.collectAsStateWithLifecycle()
            val reduced by preferences.reduceMotion.collectAsStateWithLifecycle()
            val visit by visits.collectAsStateWithLifecycle()
            val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
            val power = LocalContext.current.getSystemService(PowerManager::class.java)
            val motion = lifecycle.isAtLeast(Lifecycle.State.RESUMED) && !reduced && visit?.still != true &&
                ValueAnimator.areAnimatorsEnabled() && power?.isPowerSaveMode != true
            val splashReduced = reduced || !ValueAnimator.areAnimatorsEnabled() || power?.isPowerSaveMode == true
            var showSplash by remember { mutableStateOf(startSplash) }
            var dashboardPrepared by remember { mutableStateOf(!startSplash || splashReduced) }
            KnouxTheme(arabic, motion && !showSplash) {
                Box(Modifier.fillMaxSize()) {
                    if (dashboardPrepared || !showSplash) {
                        KnouxAppScaffold(visit, preferences::language, preferences::motion, reduced)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        debugVisit(intent)
    }

    private fun debugVisit(intent: Intent?) {
        if (BuildConfig.DEBUG) Page.from(intent?.getStringExtra("knoux.preview.route"))?.let {
            visits.value = DebugVisit(it, ++visitCount, intent?.getBooleanExtra("knoux.preview.still", false) == true)
        }
    }
}

private val tabs = listOf(Page.HOME to Mark.HOME, Page.CLEAN to Mark.BROOM, Page.TOOLS to Mark.GRID, Page.REPORTS to Mark.BARS)
private fun tabTitle(page: Page) = when (page) {
    Page.HOME -> "Home" ar "الرئيسية"
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
    val selected = page.reviewTab ?: when (page) {
        Page.HOME, Page.CLEAN, Page.TOOLS, Page.REPORTS -> page
        else -> Page.from(nav.previousBackStackEntry?.destination?.route)
            ?.takeIf { it == Page.CLEAN || it == Page.TOOLS } ?: Page.HOME
    }
    var headerSheet by remember { mutableStateOf<HeaderSheet?>(null) }
    val back: () -> Unit = { if (!nav.popBackStack()) nav.switchTab(Page.HOME) }
    val open: (Page) -> Unit = { next ->
        if (next in tabs.map { it.first }) nav.switchTab(next)
        else nav.navigate(next.id) { launchSingleTop = true }
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xff130a3d), Color(0xff090420), Color(0xff030109))))
        .drawBehind {
            val top = Offset(size.width * .78f, size.height * .01f)
            drawCircle(Brush.radialGradient(listOf(Color(0xff5a22c8).copy(alpha = .32f),
                Color(0xff2c1570).copy(alpha = .16f), Color.Transparent), top, size.width * .95f), size.width * .95f, top)
            val low = Offset(size.width * .10f, size.height * .88f)
            drawCircle(Brush.radialGradient(listOf(Cyan.copy(alpha = .09f), Color.Transparent), low, size.width * .70f),
                size.width * .70f, low)
        }.safeDrawingPadding()
        .semantics { testTagsAsResourceId = BuildConfig.DEBUG },
        contentAlignment = Alignment.TopCenter) {
        BoxWithConstraints(Modifier.widthIn(max = 540.dp).fillMaxSize()) {
            val roomy = maxWidth < 350.dp || LocalDensity.current.fontScale > 1.24f
            CompositionLocalProvider(LocalRoomy provides roomy, LocalTall provides (maxHeight >= 800.dp && !roomy)) {
                Column(Modifier.fillMaxSize()) {
                    KnouxTopBar(page, { headerSheet = HeaderSheet.MENU }, back, { headerSheet = HeaderSheet.PRO },
                        { headerSheet = HeaderSheet.SETTINGS }, { headerSheet = HeaderSheet.DISCLOSURE })
                    NavHost(navController = nav, startDestination = Page.HOME.id, modifier = Modifier.weight(1f),
                        enterTransition = { androidx.compose.animation.EnterTransition.None },
                        exitTransition = { androidx.compose.animation.ExitTransition.None }
                    ) {
                        Page.entries.forEach { target ->
                            composable(target.id) {
                                when (target) {
                                    Page.HOME -> HomeScreen(open)
                                    Page.CLEAN, Page.TOOLS -> ToolIndex(target, open)
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
                                "دي معاينة لشارة Pro. الشراء والاشتراكات غير متاحة.").localized(), 14, Mist)
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
                                    firstEnabled = ar, secondEnabled = !ar, onFirst = { language(false) }, onSecond = { language(true) })
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
    Box(Modifier.size(48.dp).testTag(tag).clip(RoundedCornerShape(20.dp))
        .clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Glyph(mark, Modifier.size(22.dp).then(if (mirror) Modifier.rotate(180f) else Modifier))
    }
}

@Composable
private fun KnouxTopBar(page: Page, menu: () -> Unit, back: () -> Unit, pro: () -> Unit, settings: () -> Unit, disclose: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        if (page == Page.HOME) IconControl(Mark.MENU, ("Menu" ar "القائمة").localized(), "menu", menu)
        else IconControl(Mark.BACK, ("Back" ar "رجوع").localized(), "back", back)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Glyph(Mark.K, Modifier.size(33.dp))
                    if (LocalDensity.current.fontScale <= 1.45f) {
                        Label("knoux", if (LocalRoomy.current) 15 else 23, weight = FontWeight.ExtraBold)
                        Label(" ai", if (LocalRoomy.current) 15 else 23, Pink, FontWeight.ExtraBold)
                    }
                }
            }
        }
        PreviewModeDisclosure(disclose)
        if ((page == Page.HOME || page in Page.detailRoutes) && !LocalRoomy.current) {
            Row(Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(20.dp)).testTag("pro")
                .clickable(role = Role.Button, onClick = pro).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Glyph(Mark.CROWN, Modifier.size(15.dp), Gold)
                Label("Pro", 11, Gold, FontWeight.Bold)
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
            Column(Modifier.weight(1f).heightIn(min = 48.dp).testTag("tab-${tab.id}").clip(RoundedCornerShape(20.dp))
                .then(if (active) Modifier.tabSelection() else Modifier)
                .selectable(active, role = Role.Tab, onClick = { open(tab) }).padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Glyph(icon, Modifier.size(20.dp), if (active) Chalk else Mist, active)
                Label(tabTitle(tab).localized(), 11, if (active) Chalk else Mist, if (active) FontWeight.Bold else FontWeight.Normal, align = TextAlign.Center)
            }
        }
    }
}

/** Selected tab: a filled, lit pill with a soft bloom under it. */
private fun Modifier.tabSelection(): Modifier = drawBehind {
    val rad = CornerRadius(size.height * .42f)
    for (dp in listOf(9, 5)) {
        val delta = dp.dp.toPx()
        drawRoundRect(Brush.horizontalGradient(listOf(Light.violet.copy(alpha = .16f), Light.cyan.copy(alpha = .12f))),
            topLeft = Offset(-delta * .5f, -delta * .35f), size = Size(size.width + delta, size.height + delta * .7f),
            cornerRadius = CornerRadius(size.height * .42f + delta * .5f))
    }
    drawRoundRect(Brush.linearGradient(listOf(Color(0xff7a2be8), Color(0xff4d23c4), Color(0xff1f3fb8)),
        Offset.Zero, Offset(size.width, size.height)), cornerRadius = rad)
    drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = .28f), Color.Transparent)),
        cornerRadius = rad)
    drawRoundRect(Brush.horizontalGradient(listOf(Color.White.copy(alpha = .55f), Light.cyan.copy(alpha = .45f), Color.Transparent)),
        cornerRadius = rad, style = Stroke(1.1.dp.toPx()))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InfoSheet(title: Copy, dismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = dismiss, sheetState = state,
        containerColor = Color(0xff1b1742), contentColor = Chalk) {
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
