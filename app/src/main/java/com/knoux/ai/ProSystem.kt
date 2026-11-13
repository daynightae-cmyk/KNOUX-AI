package com.knoux.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// Pro System - نظام Pro الموحد لجميع أقسام التطبيق
// كل قسم له ميزات Pro خاصة به

/**
 * Pro Feature Definition
 * تعريف ميزة Pro لكل قسم
 */
internal data class ProFeature(
    val id: String,
    val icon: Mark,
    val title: Copy,
    val description: Copy,
    val color: Color,
    val sectionId: String // القسم التابع له
)

/**
 * Pro Section - كل قسم من أقسام التطبيق العشرة
 */
internal enum class ProSection(val id: String, val title: Copy, val icon: Mark, val themeColor: Color) {
    SMART_CLEAN("smart_clean", "AI Smart Clean" ar "التنظيف الذكي", Mark.SPARKLE, Cyan),
    PRIVACY_SWEEP("privacy_sweep", "Privacy Sweep" ar "مسح الخصوصية", Mark.SHIELD, Pink),
    DUPLICATE_FINDER("duplicate_finder", "Duplicate Files" ar "الملفات المكررة", Mark.COPY, Violet),
    DEEP_SCAN("deep_scan", "Deep Scan" ar "الفحص العميق", Mark.SEARCH, Cyan),
    REPORTS("reports", "Reports & Insights" ar "التقارير", Mark.BARS, Gold),
    APP_RESIDUE("app_residue", "App Residue Cleanup" ar "تنظيف بقايا التطبيقات", Mark.BROOM, Pink),
    APP_MANAGER("app_manager", "App Manager" ar "مدير التطبيقات", Mark.GRID, Violet),
    STORAGE_ANALYZER("storage_analyzer", "Storage Analyzer" ar "محلل التخزين", Mark.CHART, Cyan),
    BATTERY_SAVER("battery_saver", "Battery Saver" ar "موفر البطارية", Mark.BOLT, Gold),
    AI_OPTIMIZATION("ai_optimization", "AI Optimization" ar "التحسين الذكي", Mark.CHIP, Mint);

    companion object {
        fun fromId(id: String) = entries.find { it.id == id }
    }
}

/**
 * Pro Features Database
 * قاعدة بيانات الميزات Pro لكل قسم
 */
internal object ProFeaturesDB {
    // القسم الأول: AI Smart Clean Pro Features
    val smartCleanFeatures = listOf(
        ProFeature(
            "smart_clean_unlimited",
            Mark.ROCKET,
            "Unlimited AI Cleaning Sessions" ar "جلسات تنظيف ذكية بلا حدود",
            "Clean your device as many times as you want with advanced AI algorithms" ar 
            "نظف جهازك بلا حدود باستخدام خوارزميات الذكاء الاصطناعي المتقدمة",
            Cyan,
            ProSection.SMART_CLEAN.id
        ),
        ProFeature(
            "smart_clean_scheduled",
            Mark.CLOCK,
            "Scheduled Auto-Cleaning" ar "التنظيف التلقائي المجدول",
            "Set up automatic cleaning schedules - daily, weekly, or custom" ar
            "إعداد جداول تنظيف تلقائية - يومية أو أسبوعية أو مخصصة",
            Violet,
            ProSection.SMART_CLEAN.id
        ),
        ProFeature(
            "smart_clean_deep_analysis",
            Mark.CHIP,
            "AI Deep Pattern Analysis" ar "تحليل أنماط ذكي عميق",
            "Advanced AI learns your usage patterns and cleans intelligently" ar
            "ذكاء اصطناعي متقدم يتعلم أنماط استخدامك وينظف بذكاء",
            Pink,
            ProSection.SMART_CLEAN.id
        ),
        ProFeature(
            "smart_clean_priority",
            Mark.BOLT,
            "Priority Performance Mode" ar "وضع الأداء ذو الأولوية",
            "Lightning-fast cleaning with priority system resources" ar
            "تنظيف فائق السرعة بموارد النظام ذات الأولوية",
            Gold,
            ProSection.SMART_CLEAN.id
        ),
        ProFeature(
            "smart_clean_rollback",
            Mark.HISTORY,
            "Smart Rollback & Recovery" ar "التراجع والاسترجاع الذكي",
            "Undo any cleaning action with one tap - full safety guarantee" ar
            "تراجع عن أي عملية تنظيف بضغطة واحدة - ضمان أمان كامل",
            Mint,
            ProSection.SMART_CLEAN.id
        )
    )

    internal fun getFeaturesForSection(sectionId: String): List<ProFeature> {
        return when (sectionId) {
            ProSection.SMART_CLEAN.id -> smartCleanFeatures
            ProSection.PRIVACY_SWEEP.id -> privacyFeatures
            ProSection.DEEP_SCAN.id -> deepScanFeatures
            ProSection.REPORTS.id -> reportsFeatures
            ProSection.APP_MANAGER.id -> appManagerFeatures
            ProSection.AI_OPTIMIZATION.id -> aiOptFeatures
            else -> emptyList()
        }
    }

    // ─── Privacy Sweep Pro ───────────────────────────────────────────────────
    val privacyFeatures = listOf(
        ProFeature("prv_realtime", Mark.RADAR,
            "Real-Time Threat Monitor" ar "مراقبة التهديدات الفورية",
            "Live alerts when apps access your camera, mic or location in background" ar
            "تنبيهات فورية عند وصول التطبيقات للكاميرا والميك والموقع في الخلفية",
            Pink, ProSection.PRIVACY_SWEEP.id),
        ProFeature("prv_vault", Mark.SHIELD,
            "Encrypted Privacy Vault" ar "خزنة الخصوصية المشفرة",
            "Lock sensitive photos, notes and files behind biometric authentication" ar
            "احمِ الصور والملاحظات الحساسة خلف مصادقة بيومترية",
            Violet, ProSection.PRIVACY_SWEEP.id),
        ProFeature("prv_tracker", Mark.EYE,
            "Deep Tracker Blocker" ar "حاجب المتتبعين العميق",
            "Identify and silence 500+ known ad-trackers and analytics SDKs" ar
            "كشف وإسكات أكثر من ٥٠٠ متتبع إعلاني معروف",
            Cyan, ProSection.PRIVACY_SWEEP.id),
        ProFeature("prv_report", Mark.BARS,
            "Full Privacy Audit Report" ar "تقرير مراجعة الخصوصية الكامل",
            "Downloadable PDF report with every permission, risk and recommendation" ar
            "تقرير PDF قابل للتنزيل بكل إذن وخطر وتوصية",
            Gold, ProSection.PRIVACY_SWEEP.id),
        ProFeature("prv_history", Mark.HISTORY,
            "Permission History Timeline" ar "الجدول الزمني لسجل الأذونات",
            "30-day history showing exactly when each app accessed your data" ar
            "سجل ٣٠ يوماً يُظهر متى وصل كل تطبيق لبياناتك",
            Pink, ProSection.PRIVACY_SWEEP.id)
    )

    // ─── Deep Scan Pro ───────────────────────────────────────────────────────
    val deepScanFeatures = listOf(
        ProFeature("scan_malware", Mark.SEARCH,
            "AI Malware & Spyware Scanner" ar "ماسح البرمجيات الخبيثة بالذكاء الاصطناعي",
            "Detect hidden malware, spyware and adware across your entire storage" ar
            "كشف البرمجيات الخبيثة والتجسسية الخفية في كل التخزين",
            Cyan, ProSection.DEEP_SCAN.id),
        ProFeature("scan_hidden", Mark.EYE,
            "Hidden File Deep Inspector" ar "فاحص الملفات المخفية العميق",
            "Reveal and examine every hidden file, folder and system cache" ar
            "اكشف وافحص كل ملف ومجلد مخفي وذاكرة نظام",
            Violet, ProSection.DEEP_SCAN.id),
        ProFeature("scan_schedule", Mark.CLOCK,
            "Scheduled Background Scans" ar "الفحوصات المجدولة في الخلفية",
            "Automatic deep scans run silently at night — wake up to clean results" ar
            "فحوصات عميقة تلقائية تعمل ليلاً وتستيقظ على نتائج نظيفة",
            Pink, ProSection.DEEP_SCAN.id),
        ProFeature("scan_custom", Mark.FOLDER,
            "Custom Scan Profiles" ar "ملفات الفحص المخصصة",
            "Build your own scan profiles targeting specific folders and file types" ar
            "أنشئ ملفات فحص خاصة بك تستهدف مجلدات وأنواع ملفات محددة",
            Gold, ProSection.DEEP_SCAN.id),
        ProFeature("scan_forensic", Mark.CHIP,
            "Forensic-Grade Analysis" ar "تحليل بمستوى الطب الجنائي الرقمي",
            "Examine file metadata, creation timestamps and modification logs" ar
            "فحص بيانات الملف والطوابع الزمنية وسجلات التعديل",
            Cyan, ProSection.DEEP_SCAN.id)
    )

    // ─── Reports & Insights Pro ──────────────────────────────────────────────
    val reportsFeatures = listOf(
        ProFeature("rep_export", Mark.DOWNLOAD,
            "Export Full PDF Report" ar "تصدير تقرير PDF كامل",
            "Professional-grade PDF report with charts, graphs and AI recommendations" ar
            "تقرير PDF احترافي بالرسوم البيانية وتوصيات الذكاء الاصطناعي",
            Gold, ProSection.REPORTS.id),
        ProFeature("rep_history", Mark.HISTORY,
            "90-Day History & Trends" ar "السجل والاتجاهات لـ ٩٠ يوماً",
            "Track your device health, storage and battery trends over 90 days" ar
            "تتبع صحة جهازك والتخزين واتجاهات البطارية لـ ٩٠ يوماً",
            Cyan, ProSection.REPORTS.id),
        ProFeature("rep_ai_insights", Mark.SPARKLE,
            "AI Predictive Insights" ar "رؤى الذكاء الاصطناعي التنبؤية",
            "AI predicts future storage shortfalls and performance drops before they happen" ar
            "يتنبأ الذكاء الاصطناعي بنقص التخزين وانخفاض الأداء قبل حدوثه",
            Mint, ProSection.REPORTS.id),
        ProFeature("rep_compare", Mark.CHART,
            "Device Comparison Benchmarks" ar "معايير مقارنة الجهاز",
            "Compare your device performance against similar models worldwide" ar
            "قارن أداء جهازك بالنماذج المماثلة حول العالم",
            Pink, ProSection.REPORTS.id),
        ProFeature("rep_schedule", Mark.CALENDAR,
            "Automated Weekly Reports" ar "التقارير الأسبوعية التلقائية",
            "Receive automated weekly summary reports delivered as notifications" ar
            "احصل على تقارير ملخص أسبوعية تلقائية كإشعارات",
            Violet, ProSection.REPORTS.id)
    )

    // ─── App Manager Pro ─────────────────────────────────────────────────────
    val appManagerFeatures = listOf(
        ProFeature("app_batch", Mark.LAYERS,
            "Batch App Operations" ar "العمليات الجماعية على التطبيقات",
            "Freeze, uninstall or backup multiple apps simultaneously with one tap" ar
            "جمّد أو احذف أو احتفظ بنسخة احتياطية لتطبيقات متعددة بضغطة واحدة",
            Cyan, ProSection.APP_MANAGER.id),
        ProFeature("app_freeze", Mark.POWER,
            "App Hibernation & Freeze" ar "تعليق التطبيقات وتجميدها",
            "Freeze background apps to stop battery drain without uninstalling" ar
            "جمّد تطبيقات الخلفية لإيقاف استنزاف البطارية دون حذف",
            Violet, ProSection.APP_MANAGER.id),
        ProFeature("app_backup", Mark.DOWNLOAD,
            "Full App Backup & Restore" ar "النسخ الاحتياطي الكامل والاستعادة",
            "Backup apps with their data to external storage before uninstalling" ar
            "احتفظ بنسخة احتياطية من التطبيقات مع بياناتها قبل الحذف",
            Gold, ProSection.APP_MANAGER.id),
        ProFeature("app_usage", Mark.BARS,
            "Deep Usage Analytics" ar "تحليلات الاستخدام العميقة",
            "Track exactly how much battery, RAM and data each app consumes" ar
            "تتبع مقدار البطارية والذاكرة والبيانات التي يستهلكها كل تطبيق",
            Pink, ProSection.APP_MANAGER.id),
        ProFeature("app_clone", Mark.COPY,
            "App Clone & Multi-Account" ar "استنساخ التطبيقات ومتعدد الحسابات",
            "Run two instances of any social app with separate accounts" ar
            "شغّل نسختين من أي تطبيق اجتماعي بحسابات منفصلة",
            Mint, ProSection.APP_MANAGER.id)
    )

    // ─── AI Optimization Pro ─────────────────────────────────────────────────
    val aiOptFeatures = listOf(
        ProFeature("opt_gaming", Mark.GAME,
            "Gaming Boost Mode" ar "وضع تعزيز الألعاب",
            "Allocate maximum CPU/GPU/RAM for gaming — block interruptions" ar
            "خصّص أقصى CPU/GPU/RAM للألعاب وأوقف كل المقاطعات",
            Cyan, ProSection.AI_OPTIMIZATION.id),
        ProFeature("opt_ai_learn", Mark.CHIP,
            "Adaptive AI Engine" ar "محرك الذكاء الاصطناعي التكيفي",
            "AI learns your daily routine and auto-optimizes at the right moments" ar
            "يتعلم الذكاء الاصطناعي روتينك اليومي ويحسّن تلقائياً في اللحظات المناسبة",
            Mint, ProSection.AI_OPTIMIZATION.id),
        ProFeature("opt_thermal", Mark.TEMP,
            "Thermal Management Pro" ar "إدارة الحرارة الاحترافية",
            "Advanced cooling profiles to prevent throttling and overheating" ar
            "ملفات تبريد متقدمة لمنع التقليل والسخونة الزائدة",
            Pink, ProSection.AI_OPTIMIZATION.id),
        ProFeature("opt_network", Mark.SPEED,
            "Network Speed Optimizer" ar "محسّن سرعة الشبكة",
            "Prioritize bandwidth for active apps and boost download speeds" ar
            "أعطِ الأولوية للبيانات للتطبيقات النشطة وعزّز سرعات التنزيل",
            Gold, ProSection.AI_OPTIMIZATION.id),
        ProFeature("opt_scene", Mark.SPARKLE,
            "Smart Scene Detection" ar "اكتشاف المشاهد الذكية",
            "Auto-switch to Work, Gaming, Sleep or Power modes based on context" ar
            "تبديل تلقائي لأوضاع العمل والألعاب والنوم أو الطاقة بحسب السياق",
            Violet, ProSection.AI_OPTIMIZATION.id)
    )
} // end ProFeaturesDB

/**
 * Pro Gate Dialog
 * البوابة التي تظهر عند محاولة الوصول لميزة Pro
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProGateDialog(
    section: ProSection,
    onDismiss: () -> Unit,
    onActivate: () -> Unit
) {
    // إذا كان وضع المطور مفعل، لا تظهر البوابة
    if (DevAccessManager.isDevModeActive) {
        LaunchedEffect(Unit) {
            onActivate()
            onDismiss()
        }
        return
    }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var generatedCode by remember { mutableStateOf<String?>(null) }
    
    val whatsappNumber = "+971503281920"
    val waLink = "https://wa.me/971503281920?text=${Uri.encode("مرحباً، أريد تفعيل ${section.title.localized()}")}"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xff0a0520),
        contentColor = Chalk,
        tonalElevation = 0.dp
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with animated lock icon
            ProGateHeader(section)

            // Section-specific Pro Features
            val features = ProFeaturesDB.getFeaturesForSection(section.id)
            features.forEach { feature ->
                ProFeatureCard(feature)
            }

            // Pricing & Activation
            ProPricingCard(section)

            // Code Generator
            ProCodeGenerator(
                sectionId = section.id,
                generatedCode = generatedCode,
                onGenerateCode = { code -> generatedCode = code }
            )

            // WhatsApp Activation Button
            ProWhatsAppButton(
                whatsappNumber = whatsappNumber,
                waLink = waLink,
                sectionName = section.title.localized(),
                context = context
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ProGateHeader(section: ProSection) {
    val infiniteTransition = rememberInfiniteTransition(label = "pro-gate-anim")
    val lockRotation by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "lock-swing"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "glow-pulse"
    )

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        section.themeColor.copy(alpha = 0.15f),
                        Color(0xff1a0040),
                        Color(0xff002040)
                    )
                ),
                RoundedCornerShape(20.dp)
            )
            .border(
                2.dp,
                Brush.horizontalGradient(
                    listOf(
                        section.themeColor.copy(alpha = glowPulse * 0.8f),
                        Gold.copy(alpha = glowPulse * 0.5f),
                        section.themeColor.copy(alpha = glowPulse * 0.8f)
                    )
                ),
                RoundedCornerShape(20.dp)
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Animated Lock Icon
            Box(
                Modifier
                    .size(64.dp)
                    .rotate(lockRotation)
            ) {
                Glyph(Mark.LOCK, Modifier.size(64.dp), section.themeColor, true)
            }

            // Section Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Glyph(section.icon, Modifier.size(24.dp), section.themeColor, true)
                Label(
                    section.title.localized(),
                    20,
                    Chalk,
                    FontWeight.ExtraBold
                )
                Glyph(Mark.CROWN, Modifier.size(22.dp), Gold, true)
            }

            // Pro Badge
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Gold.copy(alpha = 0.3f), section.themeColor.copy(alpha = 0.3f))
                        ),
                        RoundedCornerShape(12.dp)
                    )
                    .border(1.dp, Gold.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Label(
                    ("🔒  PRO FEATURE" ar "🔒  ميزة PRO").localized(),
                    12,
                    Gold,
                    FontWeight.Bold
                )
            }

            Label(
                ("Unlock premium features with Pro subscription" ar "افتح الميزات المميزة مع اشتراك Pro").localized(),
                13,
                Mist.copy(alpha = 0.7f),
                align = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProFeatureCard(feature: ProFeature) {
    GlassPanel(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        radius = 16,
        tint = feature.color
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Feature Icon
            IconBadge(feature.icon, feature.color, diameter = 48)

            // Feature Info
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Label(
                    feature.title.localized(),
                    15,
                    Chalk,
                    FontWeight.Bold
                )
                Label(
                    feature.description.localized(),
                    12,
                    Mist.copy(alpha = 0.8f)
                )
            }

            // Check mark
            Glyph(Mark.CHECK, Modifier.size(20.dp), feature.color, true)
        }
    }
}

@Composable
private fun ProPricingCard(section: ProSection) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xff1a003a),
                        Color(0xff0d0028),
                        Color(0xff001a30)
                    )
                ),
                RoundedCornerShape(18.dp)
            )
            .border(
                2.dp,
                Brush.horizontalGradient(
                    listOf(
                        Gold.copy(alpha = 0.6f),
                        section.themeColor.copy(alpha = 0.4f),
                        Gold.copy(alpha = 0.6f)
                    )
                ),
                RoundedCornerShape(18.dp)
            )
            .padding(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Label(
                ("💎  Premium Pricing" ar "💎  أسعار مميزة").localized(),
                16,
                Gold,
                FontWeight.ExtraBold
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Monthly
                PriceOption(
                    period = "Monthly" ar "شهرياً",
                    price = "29 AED",
                    color = Cyan
                )

                Box(
                    Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(Mist.copy(alpha = 0.2f))
                )

                // Yearly
                PriceOption(
                    period = "Yearly" ar "سنوياً",
                    price = "249 AED",
                    color = Gold,
                    badge = "-30%"
                )
            }

            Label(
                ("✨ All Pro features included • Cancel anytime" ar "✨ جميع ميزات Pro متضمنة • إلغاء في أي وقت").localized(),
                11,
                Mist.copy(alpha = 0.6f),
                align = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PriceOption(period: Copy, price: String, color: Color, badge: String? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (badge != null) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Label(badge, 10, color, FontWeight.Bold)
            }
        }
        Label(period.localized(), 12, Mist)
        Label(price, 22, color, FontWeight.ExtraBold)
    }
}

@Composable
private fun ProCodeGenerator(
    sectionId: String,
    generatedCode: String?,
    onGenerateCode: (String) -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xff0d0030), Color(0xff001520))
                ),
                RoundedCornerShape(16.dp)
            )
            .border(1.dp, Violet.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Label(
                ("🎁  Get a Free Trial Code" ar "🎁  احصل على كود تجريبي مجاني").localized(),
                14,
                Chalk,
                FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                align = TextAlign.Center
            )

            if (generatedCode != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xff001a10), RoundedCornerShape(12.dp))
                        .border(1.5.dp, Mint.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Label(
                            ("Your Trial Code:" ar "كود التجربة:").localized(),
                            11,
                            Mist.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Label(
                            generatedCode,
                            20,
                            Mint,
                            FontWeight.ExtraBold,
                            align = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Label(
                            ("Send this code to activate Pro" ar "أرسل هذا الكود لتفعيل Pro").localized(),
                            11,
                            Mist.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            AccentButton(
                Action(("✨  Generate Trial Code" ar "✨  توليد كود تجريبي"), Mark.SPARKLE),
                Modifier.fillMaxWidth(),
                primary = true,
                tag = "gen-code-$sectionId"
            ) {
                val sectionPrefix = sectionId.take(3).uppercase()
                val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
                val randomPart = (1..8).map { chars.random() }.joinToString("")
                onGenerateCode("KX-$sectionPrefix-$randomPart")
            }
        }
    }
}

@Composable
private fun ProWhatsAppButton(
    whatsappNumber: String,
    waLink: String,
    sectionName: String,
    context: Context
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xff003010), Color(0xff001a08))
                ),
                RoundedCornerShape(16.dp)
            )
            .border(2.dp, Color(0xff25D366).copy(alpha = 0.8f), RoundedCornerShape(16.dp))
            .clickable(role = Role.Button) {
                runCatching {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waLink))
                    context.startActivity(intent)
                }
            }
            .padding(18.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xff25D366), RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center
            ) {
                Glyph(Mark.CHAT, Modifier.size(28.dp), Chalk, true)
            }
            Column(Modifier.weight(1f)) {
                Label(
                    ("Activate via WhatsApp" ar "تفعيل عبر واتساب").localized(),
                    16,
                    Chalk,
                    FontWeight.Bold
                )
                Label(whatsappNumber, 13, Color(0xff25D366), FontWeight.Medium)
                Label(
                    ("Tap to contact and activate $sectionName Pro" ar "اضغط للتواصل وتفعيل $sectionName Pro").localized(),
                    11,
                    Mist.copy(alpha = 0.6f)
                )
            }
            Glyph(Mark.RIGHT, Modifier.size(24.dp), Color(0xff25D366), true)
        }
    }
}

/**
 * ProSectionBanner
 * شريط Pro موحد يظهر في أعلى كل شاشة
 */
@Composable
internal fun ProSectionBanner(
    section: ProSection,
    tagline: Copy,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "banner-pulse")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "border-pulse"
    )

    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 11.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        section.themeColor.copy(alpha = 0.12f),
                        Color(0xff1a0040),
                        Color(0xff001a30)
                    )
                ),
                RoundedCornerShape(16.dp)
            )
            .border(
                1.5.dp,
                Brush.horizontalGradient(
                    listOf(
                        Gold.copy(alpha = borderAlpha),
                        section.themeColor.copy(alpha = borderAlpha * 0.6f),
                        Gold.copy(alpha = borderAlpha)
                    )
                ),
                RoundedCornerShape(16.dp)
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Crown icon with section color glow
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Gold.copy(alpha = 0.3f),
                                section.themeColor.copy(alpha = 0.1f)
                            )
                        ),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Glyph(Mark.CROWN, Modifier.size(24.dp), Gold, true)
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Label(
                        ("Unlock ${section.title.localized()} Pro" ar "افتح ${section.title.localized()} Pro").localized(),
                        13,
                        Gold,
                        FontWeight.ExtraBold
                    )
                    // PRO pill
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Gold.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .border(1.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Label("PRO", 8, Gold, FontWeight.ExtraBold)
                    }
                }
                Label(
                    tagline.localized(),
                    10,
                    Mist.copy(alpha = 0.65f)
                )
            }

            Glyph(Mark.RIGHT, Modifier.size(18.dp), Gold.copy(alpha = 0.8f), false)
        }
    }
}
