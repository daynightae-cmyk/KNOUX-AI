package com.knoux.ai

internal data class DetailDesign(
    val subtitle: Copy,
    val heading: Copy,
    val summary: Copy,
    val metrics: List<Metric>,
    val listHeading: Copy,
    val rows: List<Entry>,
    val review: Action,
    val proceed: Action,
    val selected: Int? = null,
    val score: Int? = null,
    val scoreLabel: Copy? = null
)

private fun items(page: Page) = requireNotNull(PreviewFixtures.screen(page, PreviewSource.DESIGN_PREVIEW)).entries
private fun metric(value: String, label: Copy, icon: Mark) = Metric(value, label, icon)
private fun action(title: Copy, icon: Mark = Mark.EYE) = Action(title, icon)

internal val detailDesigns = mapOf(
    Page.CLEAN_CENTER to DetailDesign(
        "AI-powered cleanup and storage recovery." ar "معاينة التنظيف واستعادة مساحة التخزين.",
        "Sample scan complete" ar "اكتمل الفحص التجريبي",
        "128,430 example files · 14.2 seconds" ar "١٢٨٬٤٣٠ ملف تجريبي · ١٤٫٢ ثانية",
        listOf(metric("5,342", "Junk items" ar "عناصر غير ضرورية", Mark.FOLDER),
            metric("23.6 GB", "Total scanned" ar "حجم الفحص" , Mark.DISK),
            metric("92%", "Device score" ar "درجة الجهاز", Mark.SPEED)),
        "Select Items to Clean" ar "اختيار العناصر للتنظيف",
        listOf(
            Entry("Cache Files" ar "ملفات التخزين المؤقت", "App cache and temporary data" ar "بيانات التطبيقات المؤقتة", "1.24 GB", Mark.DISK, mb = 1240),
            Entry("Temporary Files" ar "الملفات المؤقتة", "System and app temporary files" ar "ملفات النظام والتطبيقات المؤقتة", "982 MB", Mark.FILE, mb = 982),
            Entry("APK Leftovers" ar "بقايا APK", "Unused installation packages" ar "حزم تثبيت غير مستخدمة", "426 MB", Mark.BOX, mb = 426),
            Entry("Empty Folders" ar "مجلدات فارغة", "Folders with no useful files" ar "مجلدات من دون ملفات مفيدة", "148 MB", Mark.FOLDER, mb = 148),
            Entry("Recycle Items" ar "عناصر سلة المهملات", "Files in the sample trash" ar "ملفات في سلة المثال", "70 MB", Mark.TRASH, mb = 70)
        ), action("Preview Items" ar "معاينة العناصر"), action("Clean Selected" ar "تنظيف المحدد", Mark.BROOM),
        selected = 0b10111, score = 92, scoreLabel = "Junk found" ar "ملفات غير ضرورية"
    ),
    Page.DUPLICATE_REVIEW to DetailDesign(
        "Review groups and keep the best copies." ar "راجع المجموعات واحتفظ بأفضل نسخة.",
        "Duplicate Groups" ar "مجموعات التكرار",
        "The original in each sample group stays selected to keep." ar "النسخة الأصلية في كل مجموعة محفوظة.",
        listOf(metric("1,248", "duplicates found" ar "ملفات مكررة", Mark.FILE),
            metric("8.7 GB", "recoverable" ar "قابلة للاسترداد", Mark.DISK),
            metric("5", "groups" ar "مجموعات", Mark.LAYERS)),
        "Duplicate Groups (5)" ar "مجموعات التكرار (٥)",
        items(Page.DUPLICATES).mapIndexed { i, entry -> entry.copy(mb = listOf(1600, 3100, 2400, 1100, 521)[i]) },
        action("Keep Best Copies" ar "الاحتفاظ بأفضل نسخة", Mark.CROWN),
        action("Remove Selected" ar "إزالة المحدد", Mark.TRASH), selected = 0b00011
    ),
    Page.PRIVACY_INSPECTOR to DetailDesign(
        "Explore sample trackers, permissions, and risks." ar "استكشف أمثلة أدوات التتبع والأذونات والمخاطر.",
        "Privacy Score" ar "درجة الخصوصية", "▲ +6% vs last sample scan" ar "▲ +٦٪ مقارنة بآخر معاينة",
        listOf(metric("24", "Protected apps" ar "تطبيقات محمية", Mark.LAYERS),
            metric("5", "Needs review" ar "تحتاج مراجعة", Mark.WARN),
            metric("7", "Trackers found" ar "أدوات تتبع", Mark.SEARCH),
            metric("0", "Blocked" ar "تم حجبها", Mark.SHIELD)),
        "Detected Privacy Risks" ar "مخاطر الخصوصية المكتشفة",
        listOf(
            Entry("Instagram" ar "إنستغرام", "Meta Ads · Analytics · Profiling" ar "إعلانات ميتا · تحليلات · ملفات تعريف", "4 permissions", Mark.CAMERA, note = "High" ar "مرتفع"),
            Entry("TikTok" ar "تيك توك", "Ads · Analytics · Behavior · ID" ar "إعلانات · تحليلات · سلوك · معرّف", "5 permissions", Mark.VIDEO, note = "High" ar "مرتفع"),
            Entry("Facebook" ar "فيسبوك", "Meta Ads · Measurement" ar "إعلانات ميتا · قياس", "3 permissions", Mark.GROUP, note = "Medium" ar "متوسط"),
            Entry("Google Maps" ar "خرائط Google", "Location · Analytics" ar "الموقع · التحليلات", "2 permissions", Mark.LOCATION, note = "Medium" ar "متوسط"),
            Entry("Dropbox" ar "دروب بوكس", "Analytics" ar "تحليلات", "2 permissions", Mark.BOX, note = "Low" ar "منخفض")
        ), action("Review Permissions" ar "مراجعة الأذونات"), action("Fix Selected Risks" ar "إصلاح المخاطر المحددة", Mark.SHIELD),
        selected = 0b01011, score = 86, scoreLabel = "Privacy Score" ar "درجة الخصوصية"
    ),
    Page.SCAN_SUMMARY to DetailDesign(
        "Explore the outcome of the sample deep scan." ar "استكشف نتائج نموذج الفحص العميق.",
        "Sample Scan Complete" ar "اكتمل نموذج الفحص", "7 issues · 5.6 GB recoverable" ar "٧ مشاكل · 5.6 GB قابلة للاسترداد",
        listOf(metric("2.4 GB", "Junk" ar "ملفات مؤقتة", Mark.TRASH), metric("1,248", "Duplicates" ar "التكرار", Mark.DISK),
            metric("7", "Privacy Alerts" ar "تنبيهات خصوصية", Mark.SHIELD), metric("4", "Apps" ar "تطبيقات", Mark.LAYERS),
            metric("+18%", "Battery" ar "البطارية", Mark.BATTERY)),
        "Key Findings" ar "النتائج المهمة", listOf(
            Entry("Junk Files" ar "ملفات غير ضرورية", "Unnecessary files in the sample" ar "ملفات غير ضرورية في المثال", "2.4 GB", Mark.TRASH, note = "High" ar "مرتفع"),
            Entry("Duplicate Files" ar "الملفات المكررة", "Duplicate media items" ar "عناصر وسائط مكررة", "1,248", Mark.DISK, note = "Medium" ar "متوسط"),
            Entry("Privacy Risks" ar "مخاطر الخصوصية", "Trackers and permissions" ar "أدوات تتبع وأذونات", "7", Mark.SHIELD, note = "Medium" ar "متوسط"),
            Entry("App Optimization" ar "تحسين التطبيقات", "Apps with high sample usage" ar "تطبيقات عالية الاستهلاك", "4", Mark.LAYERS),
            Entry("Battery Issues" ar "مشاكل البطارية", "High sample background usage" ar "استهلاك خلفية مرتفع في المثال", "+18%", Mark.BATTERY)
        ), action("Review Manually" ar "مراجعة يدوية"), action("Fix All Safe Issues" ar "إصلاح المشاكل الآمنة", Mark.CHECK),
        score = 92, scoreLabel = "Scan Score" ar "درجة الفحص"
    ),
    Page.REPORT_DETAIL to DetailDesign(
        "Cleanup, storage, privacy, battery, and performance trends." ar "اتجاهات التنظيف والتخزين والخصوصية والبطارية والأداء.",
        "Full Optimization Report" ar "تقرير التحسين الكامل", "Sample comprehensive scan · 10:21 AM" ar "نموذج فحص شامل · ١٠:٢١ ص",
        listOf(metric("12.6 GB", "Total saved" ar "إجمالي التوفير", Mark.DISK), metric("31", "Cleanups" ar "عمليات تنظيف", Mark.BROOM),
            metric("7", "Privacy fixes" ar "إصلاحات الخصوصية", Mark.SHIELD), metric("+18%", "Battery gain" ar "تحسن البطارية", Mark.BATTERY)),
        "Performance Trends" ar "اتجاهات الأداء", items(Page.REPORTS),
        action("Share Report Preview" ar "معاينة مشاركة التقرير", Mark.SHARE), action("Export PDF Preview" ar "معاينة تصدير PDF", Mark.FILE),
        score = 92, scoreLabel = "Device Score" ar "درجة الجهاز"
    ),
    Page.RESIDUE_REVIEW to DetailDesign(
        "Inspect sample folders, cache, and installer remnants." ar "افحص أمثلة المجلدات والذاكرة المؤقتة وبقايا التثبيت.",
        "Cleanup Safety Check" ar "فحص أمان التنظيف", "836 MB safe to review · 28 MB needs review" ar "836 MB في المثال · 28 MB تحتاج مراجعة",
        emptyList(), "All Items" ar "كل العناصر",
        items(Page.RESIDUE).mapIndexed { index, entry ->
            val paths = listOf("Android/media/com.whatsapp/", "Android/data/com.gamezone/cache/", "Pictures/OldCamera/",
                "Android/tmp/com.editor.temp/", "Download/app_installer/")
            val path = "/storage/emulated/0/${paths[index]}"
            if (index == 3) entry.copy(detail = path ar path, value = "28 MB", mb = 28) else entry.copy(detail = path ar path)
        }, action("Preview Paths" ar "معاينة المسارات"), action("Delete Selected Remnants" ar "حذف البقايا المحددة", Mark.TRASH),
        selected = 0b10111, score = 91, scoreLabel = "Cleanup Safe" ar "أمان التنظيف"
    ),
    Page.APPS_REVIEW to DetailDesign(
        "Social apps · detailed sample controls and suggestions." ar "تطبيقات التواصل · أدوات وتوصيات تجريبية.",
        "Social Apps" ar "تطبيقات التواصل", "3.12 GB · 5 sample apps" ar "3.12 GB · ٥ تطبيقات في المثال",
        listOf(metric("3.12 GB", "Storage" ar "التخزين", Mark.DISK), metric("+12%", "Battery" ar "البطارية", Mark.BATTERY),
            metric("24", "Permissions" ar "الأذونات", Mark.SHIELD), metric("2", "Startup" ar "بدء التشغيل", Mark.POWER),
            metric("1", "Updates" ar "تحديثات", Mark.SYNC)),
        "Apps in This Group" ar "تطبيقات المجموعة", listOf(
            Entry("Instagram" ar "إنستغرام", "+5% · 12 permissions · Starts at boot" ar "+٥٪ · ١٢ إذن · يبدأ تلقائيًا", "1.28 GB", Mark.CAMERA),
            Entry("Facebook" ar "فيسبوك", "+4% · 8 permissions" ar "+٤٪ · ٨ أذونات", "744 MB", Mark.GROUP),
            Entry("WhatsApp" ar "واتساب", "+2% · 6 permissions · Starts at boot" ar "+٢٪ · ٦ أذونات · يبدأ تلقائيًا", "612 MB", Mark.CHAT),
            Entry("TikTok" ar "تيك توك", "+3% · 7 permissions" ar "+٣٪ · ٧ أذونات", "372 MB", Mark.MUSIC),
            Entry("X (Twitter)" ar "إكس (تويتر)", "+1% · 5 permissions" ar "+١٪ · ٥ أذونات", "128 MB", Mark.GROUP)
        ), action("Manage Selected" ar "إدارة المحدد", Mark.GEAR), action("Optimize Apps" ar "تحسين التطبيقات", Mark.ROCKET),
        selected = 0b00111
    ),
    Page.STORAGE_BREAKDOWN to DetailDesign(
        "Explore categories, file types, and sample cleanup suggestions." ar "استكشف الفئات وأنواع الملفات واقتراحات التنظيف.",
        "128 GB / 256 GB used" ar "128 GB مستخدم من 256 GB", "Sample storage at 50% capacity" ar "مساحة التخزين في المثال ٥٠٪",
        listOf(metric("+12.6 GB", "Recent growth" ar "الزيادة الأخيرة", Mark.BARS),
            metric("10:21 AM", "Last scan" ar "آخر فحص", Mark.CALENDAR), metric("24.7 GB", "Potential cleanup" ar "مساحة قابلة للتحرير", Mark.SHIELD)),
        "All Categories" ar "كل الفئات", items(Page.STORAGE),
        action("Open Largest Files" ar "معاينة أكبر الملفات"), action("Free Space Preview" ar "معاينة تحرير المساحة", Mark.BROOM)
    ),
    Page.BATTERY_DIAGNOSTICS to DetailDesign(
        "Battery health, charging, and app drain in the design preview." ar "حالة البطارية والشحن والاستهلاك في المعاينة.",
        "Healthy Battery" ar "البطارية بحالة جيدة", "Sample battery in good condition." ar "بطارية المثال في حالة جيدة.",
        listOf(metric("6h 42m", "Estimated gain" ar "تحسن تقديري", Mark.BOLT),
            metric("5", "Drain apps" ar "تطبيقات مستهلكة", Mark.LAYERS), metric("287", "Charging cycles" ar "دورات الشحن", Mark.SYNC),
            metric("91%", "Health" ar "الحالة", Mark.HEART)),
        "Optimization Suggestions" ar "اقتراحات التحسين", items(Page.BATTERY),
        action("Battery Tips" ar "نصائح البطارية"), action("Apply Safe Fixes" ar "تطبيق إصلاحات آمنة", Mark.BOLT),
        score = 91, scoreLabel = "Battery Health" ar "حالة البطارية"
    ),
    Page.AI_RECOMMENDATIONS to DetailDesign(
        "Explore a personalized sample optimization plan." ar "استكشف مثال لخطة تحسين مخصصة.",
        "Estimated Result" ar "النتيجة التقديرية", "Sample score before and after" ar "درجة الأداء قبل وبعد في المثال",
        listOf(metric("+2.4 GB", "Memory recovery" ar "استعادة الذاكرة", Mark.DISK),
            metric("+18–24%", "Battery life" ar "عمر البطارية", Mark.BATTERY), metric("+36%", "Responsiveness" ar "الاستجابة", Mark.SPEED)),
        "Personalized Optimization Plan" ar "خطة التحسين المخصصة",
        listOf(items(Page.OPTIMIZE)[0].copy(title = "Memory Recovery" ar "استعادة الذاكرة", value = "+2.4 GB"),
            items(Page.OPTIMIZE)[2].copy(title = "Background App Management" ar "إدارة تطبيقات الخلفية", value = "−12 apps"),
            items(Page.OPTIMIZE)[3], items(Page.OPTIMIZE)[4].copy(title = "Thermal Optimization" ar "التحسين الحراري")),
        action("Customize Actions" ar "تخصيص الإجراءات", Mark.GEAR), action("Apply Recommended" ar "تطبيق التوصيات", Mark.ROCKET),
        selected = 0b1111
    )
)
