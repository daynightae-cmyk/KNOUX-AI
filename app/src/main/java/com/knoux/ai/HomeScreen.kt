package com.knoux.ai

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private data class HomeTile(val entry: Entry, val page: Page?)
private val homeTiles = listOf(
    HomeTile(Entry("Temporary Files" ar "الملفات المؤقتة", "Remove cached & unnecessary files" ar "الذاكرة المؤقتة والملفات غير الضرورية", "2.4 GB", Mark.TRASH, "Clean" ar "تنظيف", "removable" ar "قابلة للإزالة"), null),
    HomeTile(Entry("Duplicate Files" ar "الملفات المكررة", "Find duplicate photos, videos and documents" ar "صور وفيديو ومستندات مكررة", "1,248", Mark.OVERLAY, "Review" ar "مراجعة", "duplicates found" ar "نسخة مكررة"), Page.DUPLICATES),
    HomeTile(Entry(Page.STORAGE.title, "Analyze storage usage and large files" ar "استخدام التخزين والملفات الكبيرة", "128 GB", Mark.DISK, note = "/ 256 GB used" ar "من 256 GB", progress = 50), Page.STORAGE),
    HomeTile(Entry("App Residue" ar "بقايا التطبيقات", "Leftover files from uninstalled apps" ar "ملفات متبقية من التطبيقات المحذوفة", "836 MB", Mark.LAYERS, "836 MB" ar "836 MB"), Page.RESIDUE),
    HomeTile(Entry(Page.PRIVACY.title, "Review trackers and privacy risks" ar "أدوات التتبع ومخاطر الخصوصية", "7", Mark.SHIELD, "Review" ar "مراجعة", "trackers found" ar "أدوات تتبع"), Page.PRIVACY),
    HomeTile(Entry(Page.OPTIMIZE.title, "Intelligent performance and battery tuning" ar "ضبط الأداء والبطارية بذكاء", "", Mark.CHIP, "Optimize" ar "تحسين", "Boost device health" ar "تحسين حالة الجهاز"), Page.OPTIMIZE)
)

@Composable
internal fun HomeScreen(open: (Page) -> Unit) {
    var temp by remember { mutableStateOf(false) }
    val roomy = LocalRoomy.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        HeroHeading(Page.HOME, "Intelligent Cleaning & Device Optimization" ar "تنظيف ذكي وتحسين أداء الجهاز")
        Column(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(if (LocalTall.current) 10.dp else 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val features = listOf(("Clean" ar "تنظيف") to Mark.BOLT, ("Optimize" ar "تحسين") to Mark.ROCKET,
                ("Protect" ar "حماية") to Mark.SHIELD, ("More Space" ar "مساحة أكبر") to Mark.LAYERS)
            features.chunked(if (roomy) 2 else 4).forEach { group ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    group.forEachIndexed { i, (title, icon) ->
                        if (i > 0) Separator(Modifier.width(1.dp).height(19.dp))
                        Row(Modifier.weight(1f).padding(horizontal = 3.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)) {
                            Glyph(icon, Modifier.size(16.dp), if (i < 2) Pink else Cyan, true)
                            Label(title.localized(), 10, Mist, align = TextAlign.Center)
                        }
                    }
                }
            }
            AccentButton(Action("Start Deep Scan" ar "ابدأ الفحص العميق", Mark.SEARCH),
                Modifier.fillMaxWidth(if (roomy) 1f else .74f), tag = "start-scan") { open(Page.SCAN) }
            HomeMetrics()
            homeTiles.chunked(if (roomy) 1 else 2).forEachIndexed { row, tiles ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    tiles.forEachIndexed { col, tile ->
                        val index = if (roomy) row else row * 2 + col
                        CategoryCard(tile.entry, index, Modifier.weight(1f).fillMaxHeight(), tile.page?.id ?: "temp", home = true) {
                            if (tile.page == null) temp = true else open(tile.page)
                        }
                    }
                }
            }
        }
    }
    if (temp) InfoSheet(homeTiles.first().entry.title, { temp = false }) {
        Label(("2.4 GB in the sample list" ar "2.4 GB في بيانات المثال").localized(), 16, Pink, FontWeight.Bold)
        Label(Disclosure.localized(), 13, Mist)
        AccentButton(Action("Explore cleanup tools" ar "استكشف أدوات التنظيف", Mark.BROOM), Modifier.fillMaxWidth(), tag = "temp-tools") {
            temp = false
            open(Page.CLEAN)
        }
    }
}

@Composable
private fun HomeMetrics() {
    val labels = listOf(
        ("Optimization Score" ar "درجة التحسين").localized() to ("↑ Excellent" ar "↑ ممتاز").localized(),
        "12.6 GB" to ("Storage Saved" ar "مساحة موفرة").localized(),
        ("Scan Progress" ar "تقدم الفحص").localized() to ("Analyzing device…" ar "تحليل الجهاز…").localized()
    )
    @Composable fun Metric(i: Int, modifier: Modifier) {
        Row(modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            when (i) {
                0 -> ProgressDonut(93, diameter = 40)
                1 -> IconBadge(Mark.DISK, Pink, diameter = 30)
                else -> ProgressDonut(72, diameter = 40)
            }
            Column {
                Label(labels[i].first, if (i == 1) 15 else 10, weight = FontWeight.Bold)
                Label(labels[i].second, 9, if (i == 0) Mint else Mist)
            }
        }
    }
    GlassPanel(Modifier.fillMaxWidth()) {
        if (LocalRoomy.current) Column(Modifier.fillMaxWidth()) { labels.indices.forEach { Metric(it, Modifier.fillMaxWidth()) } }
        else Row(Modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = if (LocalTall.current) 6.dp else 3.dp), verticalAlignment = Alignment.CenterVertically) {
            labels.indices.forEach { i ->
                if (i > 0) Separator(Modifier.width(1.dp).height(33.dp))
                Metric(i, Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun CategoryCard(entry: Entry, index: Int, modifier: Modifier = Modifier, tag: String, home: Boolean = false, onClick: () -> Unit) {
    val accent = when {
        entry.icon == Mark.CHAT -> Mint
        home -> if (index in listOf(1, 2, 4)) Cyan else Pink
        index % 2 == 0 -> Pink
        else -> Cyan
    }
    val arrow = Modifier.size(13.dp).then(if (LocalArabic.current) Modifier.rotate(180f) else Modifier)
    GlassPanel(modifier.heightIn(min = if (home && LocalTall.current) 93.dp else if (home) 83.dp else 66.dp).testTag("open-$tag")
        .clip(RoundedCornerShape(14.dp)).clickable(role = Role.Button, onClick = onClick)) {
        Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconBadge(entry.icon, accent, diameter = 34)
                Column(Modifier.weight(1f)) {
                    Label(entry.title.localized(), 12, weight = FontWeight.Bold)
                    if (!home) Label(entry.value, 13, Pink, FontWeight.Bold)
                    Label(entry.detail.localized(), 10, Mist)
                }
                Glyph(Mark.NEXT, arrow)
            }
            if (home) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Column(Modifier.weight(1f)) {
                        if (entry.value.isNotEmpty() && entry.icon != Mark.LAYERS) Label(entry.value, 12, Pink, FontWeight.Bold)
                        val detail = if (entry.icon == Mark.LAYERS) ("Residual data cleanup" ar "تنظيف البقايا").localized() else entry.note?.localized()
                        detail?.let { Label(it, 9, Mist) }
                    }
                    entry.action?.let { NeonPill(it.localized()) }
                    entry.progress?.let { Label("$it%", 10, Mist, FontWeight.Bold) }
                }
                entry.progress?.let { GradientBar(it / 100f, thickness = 4) }
            }
        }
    }
}
