package com.knoux.ai

internal data class Copy(val en: String, val ar: String) {
    fun text(arabic: Boolean) = if (arabic) ar else en
}
internal infix fun String.ar(translation: String) = Copy(this, translation)

internal enum class Mark {
    K, MENU, BACK, NEXT, GEAR, CROWN, HOME, BROOM, GRID, BARS, SHIELD, PICTURES, RADAR,
    LAYERS, DISK, BATTERY, CHIP, SEARCH, FILE, CAMERA, MIC, CLIP, LOCATION, OVERLAY, BELL,
    PERSON, VIDEO, MUSIC, SHARE, CHECK, WARN, CALENDAR, CLOCK, EYE, PLAY, PAUSE, TRASH,
    FOLDER, LINK, CHAT, GAME, BOX, DOWNLOAD, HEART, BOLT, SUN, SYNC, POWER, TEMP, ROCKET,
    GROUP, SPEED, TROPHY, MEMORY
}

internal enum class Page(val id: String, val title: Copy, val hero: Mark) {
    HOME("home", "AI Smart Clean" ar "التنظيف الذكي", Mark.K),
    PRIVACY("privacy", "Privacy Sweep" ar "مراجعة الخصوصية", Mark.SHIELD),
    DUPLICATES("duplicates", "Duplicate Files Finder" ar "اكتشاف الملفات المكررة", Mark.PICTURES),
    SCAN("deep-scan", "Deep Scan" ar "الفحص العميق", Mark.RADAR),
    REPORTS("reports", "Reports & Insights" ar "التقارير والرؤى", Mark.BARS),
    RESIDUE("residue", "App Residue Cleanup" ar "تنظيف بقايا التطبيقات", Mark.LAYERS),
    APPS("app-manager", "App Manager" ar "إدارة التطبيقات", Mark.GRID),
    STORAGE("storage", "Storage Analyzer" ar "تحليل التخزين", Mark.DISK),
    BATTERY("battery", "Battery Saver" ar "توفير البطارية", Mark.BATTERY),
    OPTIMIZE("ai-optimization", "AI Optimization" ar "التحسين الذكي", Mark.CHIP),
    CLEAN("clean", "Clean" ar "التنظيف", Mark.BROOM),
    TOOLS("tools", "Tools" ar "الأدوات", Mark.GRID);

    companion object {
        val references = entries.take(10)
        fun from(id: String?) = entries.find { it.id == id }
    }
}

internal data class Metric(val value: String, val label: Copy, val icon: Mark)
internal data class Fact(val label: Copy, val value: Copy, val icon: Mark)
internal data class Entry(
    val title: Copy, val detail: Copy, val value: String, val icon: Mark,
    val action: Copy? = null, val note: Copy? = null, val mb: Int = 0, val progress: Int? = null
)
internal data class Action(val label: Copy, val icon: Mark)
internal enum class Selection { NONE, TRACKERS, RESIDUE, OPTIMIZATION }
internal data class PreviewScreen(
    val subtitle: Copy, val primary: List<Metric>, val compact: List<Metric>,
    val score: Int, val scoreLabel: Copy, val facts: List<Fact>, val section: Copy?,
    val entries: List<Entry>, val review: Action, val proceed: Action,
    val selection: Selection = Selection.NONE, val grouped: Boolean = false
)
internal enum class PreviewSource { DESIGN_PREVIEW, LIVE_UNAVAILABLE }

private fun m(value: String, label: Copy, icon: Mark) = Metric(value, label, icon)
private fun f(label: Copy, value: String, icon: Mark) = Fact(label, value ar value, icon)
private fun time(label: Copy) = Fact(label, "Today, 10:21 AM" ar "اليوم، ١٠:٢١ ص", Mark.CALENDAR)
private fun a(label: Copy, icon: Mark = Mark.EYE) = Action(label, icon)
private val review = "Review" ar "مراجعة"

internal object PreviewFixtures {
    fun screen(page: Page, source: PreviewSource): PreviewScreen? =
        if (source == PreviewSource.DESIGN_PREVIEW) screens[page] else null

    fun selectedMb(entries: List<Entry>, selected: Set<Int>): Int =
        entries.withIndex().sumOf { (index, item) -> if (index in selected) item.mb else 0 }

    private val screens = mapOf(
        Page.PRIVACY to PreviewScreen(
            "Review trackers, risky permissions, and privacy exposure indicators." ar "راجع أدوات التتبع والأذونات ومؤشرات التعرّض لمخاطر الخصوصية.",
            listOf(m("7", "trackers found" ar "أدوات تتبع", Mark.PERSON), m("86 /100", "Privacy Score" ar "درجة الخصوصية", Mark.SHIELD)),
            listOf(m("2", "Camera Access" ar "الكاميرا", Mark.CAMERA), m("1", "Microphone Access" ar "الميكروفون", Mark.MIC),
                m("7", "Background Trackers" ar "تتبع بالخلفية", Mark.LAYERS), m("2", "Clipboard Watchers" ar "مراقبة الحافظة", Mark.CLIP)),
            86, "Privacy Score" ar "درجة الخصوصية",
            listOf(f("Protected apps" ar "تطبيقات محمية", "24", Mark.CHECK), f("Needs review" ar "تحتاج مراجعة", "5", Mark.WARN), time("Last sweep" ar "آخر مراجعة")),
            "Privacy Items (5)" ar "بنود الخصوصية (٥)",
            listOf(
                Entry("Ad SDK Trackers" ar "أدوات تتبع الإعلانات", "Third-party ad SDKs that track your activity across apps." ar "أدوات إعلانية خارجية تتتبع نشاطك عبر التطبيقات.", "7 found", Mark.BELL, review),
                Entry("Background Location" ar "الموقع في الخلفية", "Apps that can access your location in the background." ar "تطبيقات يمكنها معرفة موقعك في الخلفية.", "1 app", Mark.LOCATION, review),
                Entry("Clipboard Access" ar "الوصول إلى الحافظة", "Apps that can read your clipboard content." ar "تطبيقات يمكنها قراءة محتوى الحافظة.", "2 apps", Mark.CLIP, review),
                Entry("Overlay Permission" ar "الظهور فوق التطبيقات", "Apps that can appear over other apps." ar "تطبيقات يمكنها الظهور فوق تطبيقات أخرى.", "1 app", Mark.OVERLAY, review),
                Entry("Notification Listeners" ar "قراءة الإشعارات", "Apps that can read and process notifications." ar "تطبيقات يمكنها قراءة الإشعارات ومعالجتها.", "2 apps", Mark.BELL, review)),
            a("Review Risks" ar "مراجعة المخاطر"), a("Improve Privacy" ar "تحسين الخصوصية", Mark.SHIELD), Selection.TRACKERS
        ),
        Page.DUPLICATES to PreviewScreen(
            "Detect and review duplicate photos, videos, audio, and documents." ar "راجع الصور والفيديوهات والصوت والمستندات المكررة.",
            listOf(m("1,248", "duplicates found" ar "ملفات مكررة", Mark.FILE), m("8.7 GB", "recoverable" ar "قابلة للاسترداد", Mark.DISK)),
            listOf(m("612", "Similar Photos" ar "صور متشابهة", Mark.PICTURES), m("238", "Repeated Videos" ar "فيديو مكرر", Mark.VIDEO),
                m("316", "Copy Documents" ar "نسخ مستندات", Mark.FILE), m("82", "Duplicate Audio" ar "صوت مكرر", Mark.MUSIC)),
            96, "Match Confidence" ar "ثقة التطابق",
            listOf(Fact("Auto-grouped items" ar "تجميع تلقائي", "218 groups" ar "٢١٨ مجموعة", Mark.CHECK), Fact("Needs review" ar "تحتاج مراجعة", "37 items" ar "٣٧ عنصر", Mark.WARN), time("Last scan" ar "آخر فحص")),
            "Duplicate Groups (5)" ar "مجموعات التكرار (٥)",
            listOf(
                Entry("Similar Selfies" ar "صور شخصية متشابهة", "Duplicates and near-identical selfies." ar "صور شخصية مكررة أو متقاربة.", "1.6 GB", Mark.PERSON, note = "189 items" ar "١٨٩ عنصر"),
                Entry("Burst Photos" ar "صور متتابعة", "Multiple shots of the same moment." ar "لقطات متعددة لنفس اللحظة.", "3.1 GB", Mark.PICTURES, note = "423 items" ar "٤٢٣ عنصر"),
                Entry("Duplicate Videos" ar "فيديوهات مكررة", "Repeated videos and saved copies." ar "فيديوهات مكررة ونسخ محفوظة.", "2.4 GB", Mark.VIDEO, note = "238 items" ar "٢٣٨ عنصر"),
                Entry("Repeated Documents" ar "مستندات مكررة", "Duplicate PDF, DOC and XLS files." ar "نسخ مكررة من المستندات.", "1.1 GB", Mark.FILE, note = "316 items" ar "٣١٦ عنصر"),
                Entry("Shared Media Copies" ar "نسخ وسائط مشتركة", "Duplicates from chats and shared apps." ar "نسخ من المحادثات وتطبيقات المشاركة.", "521 MB", Mark.SHARE, note = "82 items" ar "٨٢ عنصر")),
            a("Preview Groups" ar "معاينة المجموعات"), a("Review & Remove" ar "مراجعة وإزالة", Mark.TRASH)
        ),
        Page.SCAN to PreviewScreen(
            "Preview a scan for junk, duplicates, privacy issues, and system health." ar "عاين فحص الملفات المؤقتة والتكرار والخصوصية وحالة النظام.",
            emptyList(),
            listOf(m("2.4 GB", "Temporary Files" ar "ملفات مؤقتة", Mark.DISK), m("1,248", "Duplicate Media" ar "وسائط مكررة", Mark.PICTURES),
                m("7", "Privacy Alerts" ar "تنبيهات خصوصية", Mark.SHIELD), m("95%", "App Health" ar "حالة التطبيقات", Mark.LAYERS)),
            72, "Overall Scan" ar "إجمالي الفحص",
            listOf(f("Modules scanned" ar "الوحدات", "5 of 5", Mark.LAYERS), Fact("Findings so far" ar "العناصر", "1,656 items" ar "١٬٦٥٦ عنصر", Mark.SEARCH), Fact("Estimated time" ar "الوقت التقديري", "~ 1 min 24 sec" ar "حوالي دقيقة و٢٤ ثانية", Mark.CLOCK)),
            "Scan Modules (5)" ar "وحدات الفحص (٥)",
            listOf(
                Entry("Scan Storage" ar "فحص التخزين", "Temporary and residual files" ar "الملفات المؤقتة والمتبقية", "84%", Mark.DISK, progress = 84),
                Entry("Review Media" ar "مراجعة الوسائط", "Duplicate photos and videos" ar "الصور والفيديوهات المكررة", "67%", Mark.PICTURES, progress = 67),
                Entry("Inspect Privacy" ar "فحص الخصوصية", "Trackers, permissions and risks" ar "التتبع والأذونات والمخاطر", "73%", Mark.SHIELD, progress = 73),
                Entry("Analyze Apps" ar "تحليل التطبيقات", "App health and resource use" ar "حالة التطبيقات واستخدام الموارد", "91%", Mark.LAYERS, progress = 91),
                Entry("Battery Diagnosis" ar "تشخيص البطارية", "Battery health and usage patterns" ar "حالة البطارية وأنماط الاستخدام", "58%", Mark.BATTERY, progress = 58)),
            a("Pause Scan" ar "إيقاف المعاينة", Mark.PAUSE), a("Continue Scan" ar "استئناف المعاينة", Mark.PLAY)
        ),
        Page.REPORTS to PreviewScreen(
            "Track cleanups, storage gains, optimization history, and device health trends." ar "عاين سجل التنظيف ومساحة التخزين والتحسين ومؤشرات الجهاز.",
            listOf(m("12.6 GB", "total saved" ar "إجمالي التوفير", Mark.DISK), m("31", "cleanups completed" ar "عمليات تنظيف", Mark.BROOM)),
            listOf(m("4.8 GB", "Weekly Savings" ar "توفير أسبوعي", Mark.CALENDAR), m("7", "Privacy Fixes" ar "إصلاحات الخصوصية", Mark.SHIELD),
                m("+18%", "Battery Gains" ar "تحسن البطارية", Mark.BATTERY), m("1,248", "Duplicate Reduction" ar "تقليل التكرار", Mark.LAYERS)),
            92, "Device Score" ar "درجة الجهاز",
            listOf(Fact("Best improvement" ar "أفضل تحسن", "Storage Cleanup (6.4 GB saved)" ar "تنظيف التخزين (6.4 GB)", Mark.TROPHY),
                f("Current device score" ar "درجة الجهاز", "92%  ▲ +6%", Mark.SPEED), time("Last report" ar "آخر تقرير")),
            null,
            listOf(
                Entry("Cleanup History" ar "سجل التنظيف", "Past cleanups, space saved, and items removed." ar "جلسات التنظيف والمساحة والعناصر.", "31", Mark.BROOM, note = "sessions" ar "جلسة"),
                Entry("Storage Trend" ar "اتجاه التخزين", "Track storage usage and savings over time." ar "استخدام التخزين وتغيره مع الوقت.", "12.6 GB", Mark.DISK, note = "total saved" ar "إجمالي التوفير"),
                Entry("Privacy Improvements" ar "تحسينات الخصوصية", "Trackers, cleared data, and reduced risks." ar "التتبع والبيانات وتقليل المخاطر.", "7", Mark.SHIELD, note = "fixes applied" ar "إصلاحات"),
                Entry("Battery Sessions" ar "جلسات البطارية", "Optimization sessions and performance gains." ar "جلسات التحسين ومؤشرات الأداء.", "+18%", Mark.BATTERY, note = "battery gain" ar "تحسن البطارية"),
                Entry("Smart Recommendations" ar "توصيات ذكية", "Free up ~2.1 GB · Optimize battery usage" ar "وفّر مساحة وحسّن استخدام البطارية.", "3", Mark.CHIP, note = "new" ar "جديدة")),
            a("View History" ar "عرض السجل"), a("Generate Report" ar "إنشاء تقرير", Mark.FILE)
        ),
        Page.RESIDUE to PreviewScreen(
            "Find leftover folders, empty data, and remnants from removed apps." ar "عاين المجلدات الفارغة وبقايا التطبيقات المحذوفة.",
            listOf(m("836 MB", "residual data" ar "بيانات متبقية", Mark.DISK), m("42", "orphan folders" ar "مجلدات متبقية", Mark.FOLDER)),
            listOf(m("8", "Uninstalled Apps" ar "تطبيقات محذوفة", Mark.TRASH), m("42", "Empty Directories" ar "مجلدات فارغة", Mark.FOLDER),
                m("516 MB", "Old Cache" ar "ذاكرة مؤقتة قديمة", Mark.LAYERS), m("17", "Broken Shortcuts" ar "اختصارات معطلة", Mark.LINK)),
            91, "Cleanup Safe" ar "أمان التنظيف",
            listOf(f("Safe to clean" ar "تنظيف آمن", "836 MB", Mark.CHECK), f("Needs review" ar "تحتاج مراجعة", "28 MB", Mark.WARN), time("Last scan" ar "آخر فحص")),
            "Items Found (5)" ar "العناصر (٥)",
            listOf(
                Entry("WhatsApp Leftovers" ar "بقايا واتساب", "Residual folders, logs, and unused data." ar "مجلدات وسجلات وبيانات غير مستخدمة.", "268 MB", Mark.CHAT, mb = 268),
                Entry("Old Game Cache" ar "ذاكرة الألعاب القديمة", "Obsolete cache from uninstalled games." ar "بيانات مؤقتة من ألعاب محذوفة.", "184 MB", Mark.GAME, mb = 184),
                Entry("Empty Media Folders" ar "مجلدات وسائط فارغة", "Media directories without files." ar "مجلدات وسائط بلا ملفات.", "96 MB", Mark.PICTURES, mb = 96),
                Entry("Broken Temp Data" ar "بيانات مؤقتة تالفة", "Incomplete temporary data and sessions." ar "بيانات مؤقتة وجلسات غير مكتملة.", "172 MB", Mark.FILE, mb = 172),
                Entry("Archived Installer Remnants" ar "بقايا ملفات التثبيت", "Leftover APK or OBB files." ar "ملفات APK أو OBB متبقية.", "116 MB", Mark.BOX, mb = 116)),
            a("Preview Residue" ar "معاينة البقايا"), a("Clean Remnants" ar "تنظيف البقايا", Mark.BROOM), Selection.RESIDUE, true
        ),
        Page.APPS to PreviewScreen(
            "Review installed apps, space use, startup impact, and update status." ar "عاين التطبيقات واستخدام المساحة وبدء التشغيل والتحديثات.",
            listOf(m("143", "installed apps" ar "تطبيقات مثبّتة", Mark.LAYERS), m("18.4 GB", "app storage" ar "مساحة التطبيقات", Mark.DISK)),
            listOf(m("27", "Rarely Used" ar "قليلة الاستخدام", Mark.CALENDAR), m("16", "Large Apps (≥ 500 MB)" ar "تطبيقات كبيرة", Mark.DISK),
                m("9", "Startup Apps" ar "بدء التشغيل", Mark.POWER), m("12", "Updates Available" ar "تحديثات متاحة", Mark.SYNC)),
            87, "App Health" ar "حالة التطبيقات",
            listOf(f("Apps to review" ar "تحتاج مراجعة", "27", Mark.WARN), f("Safe suggestions" ar "اقتراحات آمنة", "18", Mark.CHECK), time("Last checked" ar "آخر مراجعة")),
            null,
            listOf(
                Entry("Streaming Apps" ar "تطبيقات البث", "Video, music, and live streaming apps." ar "تطبيقات الفيديو والموسيقى والبث.", "5.86 GB", Mark.VIDEO, "Optimize" ar "تحسين", "Last used: Today" ar "آخر استخدام: اليوم"),
                Entry("Social Apps" ar "تطبيقات التواصل", "Social networking and community apps." ar "شبكات التواصل والمجتمعات.", "3.12 GB", Mark.GROUP, review, "Last used: Today" ar "آخر استخدام: اليوم"),
                Entry("Game Collection" ar "الألعاب", "Installed games and game resources." ar "ألعاب مثبّتة وملفاتها.", "6.27 GB", Mark.GAME, "Manage" ar "إدارة", "Last used: Yesterday" ar "آخر استخدام: أمس"),
                Entry("Utility Tools" ar "الأدوات المساعدة", "Productivity, file, and system tools." ar "أدوات الإنتاجية والملفات والنظام.", "2.43 GB", Mark.GRID, "Clean" ar "تنظيف", "Last used: Today" ar "آخر استخدام: اليوم"),
                Entry("Messaging Apps" ar "تطبيقات الرسائل", "Chat, calls, and communication apps." ar "المحادثات والمكالمات والتواصل.", "744 MB", Mark.CHAT, "Open" ar "فتح", "Last used: Today" ar "آخر استخدام: اليوم")),
            a("Review Apps" ar "مراجعة التطبيقات"), a("Manage Apps" ar "إدارة التطبيقات", Mark.GRID), grouped = true
        ),
        Page.STORAGE to PreviewScreen(
            "Understand storage usage, large files, and space distribution." ar "عاين استخدام التخزين والملفات الكبيرة وتوزيع المساحة.",
            emptyList(),
            listOf(m("48.6 GB", "Apps" ar "تطبيقات", Mark.GRID), m("36.8 GB", "Media" ar "وسائط", Mark.PICTURES), m("8.4 GB", "Documents" ar "مستندات", Mark.FILE),
                m("22.2 GB", "System" ar "النظام", Mark.GEAR), m("128 GB", "Free Space" ar "مساحة فارغة", Mark.LAYERS)),
            50, "Used (128 GB)" ar "مستخدم (128 GB)",
            listOf(Fact("Largest category" ar "أكبر فئة", "Apps & Games — 48.6 GB" ar "تطبيقات وألعاب — 48.6 GB", Mark.TROPHY),
                f("Unused space trend" ar "تغير المساحة الفارغة", "+12.6 GB", Mark.BARS), time("Last updated" ar "آخر تحديث")),
            "Storage Categories" ar "فئات التخزين",
            listOf(
                Entry("Apps & Games" ar "تطبيقات وألعاب", "Installed apps, games, and app data." ar "التطبيقات والألعاب وبياناتها.", "48.6 GB", Mark.GRID),
                Entry("Photos & Videos" ar "صور وفيديوهات", "Photos, videos, and screen recordings." ar "الصور والفيديو وتسجيلات الشاشة.", "36.8 GB", Mark.PICTURES),
                Entry("Downloads" ar "التنزيلات", "Downloaded files and installers." ar "الملفات والتنزيلات والتثبيت.", "6.2 GB", Mark.DOWNLOAD),
                Entry("Documents" ar "المستندات", "PDFs, Office files, and documents." ar "ملفات PDF وأوفيس ومستندات أخرى.", "8.4 GB", Mark.FILE),
                Entry("WhatsApp Media" ar "وسائط واتساب", "Photos, videos, audio, and shared files." ar "صور وفيديو وصوت وملفات مشتركة.", "4.1 GB", Mark.CHAT),
                Entry("System Files" ar "ملفات النظام", "Android system, cache, and essential files." ar "نظام Android والملفات الأساسية.", "22.2 GB", Mark.GEAR)),
            a("View Breakdown" ar "عرض التفاصيل"), a("Manage Space" ar "إدارة المساحة", Mark.LAYERS), grouped = true
        ),
        Page.BATTERY to PreviewScreen(
            "Reduce drain, control background activity, and extend battery life." ar "عاين توصيات تقليل الاستهلاك ونشاط الخلفية وإطالة عمر البطارية.",
            listOf(m("6h 42m", "estimated gain" ar "تحسن تقديري", Mark.CLOCK), m("91%", "Battery health" ar "حالة البطارية", Mark.HEART)),
            listOf(m("5", "Drain Sources" ar "مصادر الاستهلاك", Mark.BOLT), m("18", "Background Apps" ar "تطبيقات بالخلفية", Mark.LAYERS),
                m("−22%", "Brightness Impact" ar "تأثير السطوع", Mark.SUN), m("287", "Charging Cycles" ar "دورات الشحن", Mark.SYNC)),
            91, "Battery Health" ar "حالة البطارية",
            listOf(f("Critical drain apps" ar "تطبيقات عالية الاستهلاك", "3", Mark.WARN), Fact("Safe actions" ar "إجراءات آمنة", "5 available" ar "٥ متاحة", Mark.CHECK), time("Last analysis" ar "آخر تحليل")),
            "Optimization & Actions" ar "التحسين والإجراءات",
            listOf(
                Entry("Screen Brightness" ar "سطوع الشاشة", "High brightness increases battery drain." ar "السطوع العالي يزيد الاستهلاك.", "−22%", Mark.SUN, "Auto Optimize" ar "تحسين تلقائي"),
                Entry("Background Sync" ar "مزامنة الخلفية", "Limit non-essential sync to save power." ar "تقليل المزامنة غير الضرورية.", "−14%", Mark.SYNC, "Restrict Sync" ar "تقييد المزامنة"),
                Entry("App Wakeups" ar "تنشيط التطبيقات", "Stop frequent background wakeups." ar "تقليل تنشيط التطبيقات في الخلفية.", "−18%", Mark.LAYERS, "Limit Wakeups" ar "تقليل التنشيط"),
                Entry("GPS Usage" ar "استخدام الموقع", "High-accuracy GPS can drain battery." ar "الدقة العالية قد تستهلك البطارية.", "−11%", Mark.LOCATION, "Use While Active" ar "أثناء الاستخدام"),
                Entry("Charging Health" ar "صحة الشحن", "Optimize charging to prolong battery life." ar "توصية لتحسين الشحن وعمر البطارية.", "+7%", Mark.BATTERY, "Smart Charging" ar "شحن ذكي")),
            a("View Details" ar "عرض التفاصيل"), a("Save Battery" ar "توفير البطارية", Mark.BOLT), grouped = true
        ),
        Page.OPTIMIZE to PreviewScreen(
            "Boost responsiveness, tune performance, and optimize battery usage intelligently." ar "عاين تحسين الاستجابة وضبط الأداء واستهلاك البطارية بذكاء.",
            listOf(m("94", "Performance Score" ar "درجة الأداء", Mark.SPEED), m("+18%", "Battery gain" ar "تحسن البطارية", Mark.BATTERY)),
            listOf(m("24% (−36%)", "CPU Load" ar "حمل المعالج", Mark.CHIP), m("1.6 GB", "Memory Freed" ar "ذاكرة متاحة", Mark.DISK),
                m("12", "App Hibernation" ar "إسبات التطبيقات", Mark.LAYERS), m("Normal", "Thermal Status" ar "الحالة الحرارية", Mark.TEMP)),
            94, "Optimized" ar "درجة التحسين",
            listOf(f("Recommended actions" ar "إجراءات مقترحة", "5", Mark.CHECK), f("Applied safely" ar "تطبيق آمن", "5", Mark.SHIELD), time("Last tune-up" ar "آخر ضبط")),
            "Optimization Tools (5)" ar "أدوات التحسين (٥)",
            listOf(
                Entry("Memory Boost" ar "تعزيز الذاكرة", "Free up RAM and improve responsiveness." ar "توصية لتوفير الذاكرة وتحسين الاستجابة.", "+1.6 GB", Mark.MEMORY, note = "freed" ar "متاحة"),
                Entry("Battery Balancing" ar "موازنة البطارية", "Intelligently manage power consumption." ar "إدارة استهلاك الطاقة بذكاء.", "+18%", Mark.BATTERY, note = "battery life" ar "عمر البطارية"),
                Entry("Smart Hibernation" ar "إسبات ذكي", "Put inactive apps to sleep in the preview." ar "معاينة إسبات التطبيقات غير النشطة.", "12 apps", Mark.LAYERS, note = "hibernated" ar "في الإسبات"),
                Entry("Startup Control" ar "التحكم في بدء التشغيل", "Review auto-start apps and boot time." ar "مراجعة تطبيقات البدء ووقت التشغيل.", "−4.2 s", Mark.POWER, note = "boot time" ar "وقت التشغيل"),
                Entry("Thermal Guard" ar "الحماية الحرارية", "Review peak temperature recommendations." ar "معاينة توصيات الحرارة القصوى.", "−6°C", Mark.TEMP, note = "peak temp" ar "الحرارة القصوى")),
            a("See Recommendations" ar "عرض التوصيات"), a("Optimize Now" ar "حسّن الآن", Mark.ROCKET), Selection.OPTIMIZATION
        )
    )
}
