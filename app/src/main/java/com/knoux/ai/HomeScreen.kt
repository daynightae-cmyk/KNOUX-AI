package com.knoux.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private data class HomeTile(val entry: Entry, val page: Page)
private val homeTiles = listOf(
    HomeTile(Entry("Clean" ar "تنظيف", "Remove junk & temporary files" ar "الملفات المؤقتة وغير الضرورية",
        "2.4 GB", Mark.TRASH, note = "removable" ar "قابلة للإزالة"), Page.CLEAN_CENTER),
    HomeTile(Entry("Duplicate Files" ar "الملفات المكررة", "Find and review duplicate items" ar "اكتشف العناصر المكررة وراجعها",
        "1,248", Mark.OVERLAY, note = "duplicates" ar "نسخة مكررة"), Page.DUPLICATES),
    HomeTile(Entry("Storage" ar "التخزين", "Analyze usage & free up space" ar "حلل استخدام مساحة التخزين",
        "128 GB", Mark.DISK, note = "/ 256 GB" ar "من 256 GB", progress = 50), Page.STORAGE),
    HomeTile(Entry("Privacy" ar "الخصوصية", "Detect trackers & protect your data" ar "مراجعة أدوات التتبع ومخاطر الخصوصية",
        "7", Mark.SHIELD, note = "trackers found" ar "أدوات تتبع"), Page.PRIVACY),
    HomeTile(Entry("Battery" ar "البطارية", "Reduce drain & extend battery life" ar "تقليل الاستهلاك وإطالة عمر البطارية",
        "6h 42m", Mark.BATTERY, note = "estimated gain" ar "زيادة تقديرية"), Page.BATTERY),
    HomeTile(Entry(Page.OPTIMIZE.title, "Boost performance & keep your device fast" ar "تحسين الأداء وسرعة الجهاز",
        "94", Mark.CHIP, note = "Score" ar "درجة التحسين"), Page.OPTIMIZE)
)

@Composable
internal fun HomeScreen(open: (Page) -> Unit) {
    val roomy = LocalRoomy.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        HeroHeading(Page.HOME, "The All-in-One AI Cleaner, Optimizer & Device Care" ar "تنظيف ذكي وتحسين الأداء والعناية بالجهاز")
        Column(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(if (LocalTall.current) 8.dp else 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val features = listOf(("Clean" ar "تنظيف") to Mark.BOLT, ("Optimize" ar "تحسين") to Mark.ROCKET,
                ("Protect" ar "حماية") to Mark.SHIELD, ("Manage" ar "إدارة") to Mark.LAYERS)
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
                        CategoryCard(tile.entry, index, Modifier.weight(1f).fillMaxHeight(), tile.page.id, home = true) {
                            open(tile.page)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeMetrics() {
    val labels = listOf(
        "12.6 GB" to ("Storage Saved" ar "مساحة موفرة").localized(),
        "7" to ("Privacy Fixes" ar "إصلاحات الخصوصية").localized(),
        "+18%" to ("Battery Gain" ar "تحسن البطارية").localized()
    )
    val icons = listOf(Mark.DISK, Mark.SHIELD, Mark.BATTERY)
    @Composable fun Metric(i: Int, modifier: Modifier) {
        Column(modifier.padding(vertical = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            VolumetricGlyph(icons[i], if (i == 1) Light.cyan else Light.magenta, 20.dp)
            Label(labels[i].first, 12, weight = FontWeight.Bold, align = TextAlign.Center)
            Label(labels[i].second, 9, Mist, align = TextAlign.Center)
        }
    }
    val roomy = LocalRoomy.current
    GlassPanel(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            ProgressDonut(92, ("Device Score" ar "درجة الجهاز").localized(), diameter = if (roomy) 58 else 68)
            Separator(Modifier.width(1.dp).height(48.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    NeonPill("▲ +6%", Mint, solid = false)
                    Label(("vs last scan" ar "مقارنة بآخر فحص").localized(), 9, Mist)
                }
                if (roomy) {
                    labels.indices.forEach { Metric(it, Modifier.fillMaxWidth()) }
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        labels.indices.forEach { i ->
                            if (i > 0) Separator(Modifier.width(1.dp).height(30.dp))
                            Metric(i, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CategoryCard(entry: Entry, index: Int, modifier: Modifier = Modifier, tag: String, home: Boolean = false, onClick: () -> Unit) {
    val accent = when {
        entry.icon == Mark.CHAT -> Mint
        home -> if (entry.icon in listOf(Mark.OVERLAY, Mark.DISK, Mark.BATTERY)) Cyan else Pink
        index % 2 == 0 -> Pink
        else -> Cyan
    }
    val arrow = Modifier.size(13.dp).then(if (LocalArabic.current) Modifier.rotate(180f) else Modifier)
    GlassPanel(modifier.heightIn(min = if (home && LocalTall.current) 86.dp else if (home) 80.dp else 66.dp).testTag("open-$tag")
        .clip(RoundedCornerShape(14.dp)).clickable(role = Role.Button, onClick = onClick),
        tint = accent.takeIf { home }) {
        Column(Modifier.fillMaxWidth().padding(7.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (home) HomeTileIcon(entry.icon, accent) else IconBadge(entry.icon, accent, diameter = 34)
                Column(Modifier.weight(1f)) {
                    Label(entry.title.localized(), 12, weight = FontWeight.Bold)
                    if (!home) Label(entry.value, 13, Pink, FontWeight.Bold)
                    Label(entry.detail.localized(), 10, Mist)
                }
                Glyph(Mark.NEXT, arrow)
            }
            if (home) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier
                        .background(Brush.horizontalGradient(
                            listOf(accent.copy(alpha = .34f), accent.copy(alpha = .14f)), 0f, 260f),
                            RoundedCornerShape(9.dp))
                        .border(.7.dp, accent.copy(alpha = .55f), RoundedCornerShape(9.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Label(entry.value, 12, accent, FontWeight.Bold)
                        entry.note?.let { Label(it.localized(), 9, Mist) }
                    }
                    entry.progress?.let { Label("$it%", 10, Mist, FontWeight.Bold, Modifier.weight(1f), TextAlign.End) }
                }
                entry.progress?.let { GradientBar(it / 100f, thickness = 4) }
            }
        }
    }
}
