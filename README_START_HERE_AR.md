# KNOUX AI — COMPLETE CODEX DELIVERY BUNDLE

**تاريخ التجميع:** 10 نوفمبر 2026  | **الهدف على كمبيوتر المالك:** `D:\KNOUX-AI`

## اقرأ ده الأول

الحزمة بتسلم مرجع العشر صور الحقيقي، البرومبت التنفيذي الأصلي كاملًا (41,419 byte)، وخطة الخدمات المستقبلية، ومراجع APIs وسياسات Google Play، ومخطط قواعد البيانات المحلي والسحابي واختبارات تسليم وملفات Kotlin مرجعية قابلة للدمج. **دي ليست APK ولا مشروع Android مبني ومختبر حتى الآن**. لا توجد بيانات تسجيل دخول أو مفاتيح حقيقية مضمنة. لم نصل إلى D: فعليًا ولم نغير ملفات الكمبيوتر أو Supabase.

### سياسة التنفيذ

1. ابدأ بتشغيل `MASTER_PROMPT_CODEX_PHASE_1_UI.md` في Codex بعد فك ZIP على كمبيوتر يملك صلاحية `D:`. يُبنى UI **فقط** من الصور العشر أولًا. بيانات الصور Preview اصطناعية معلنة.
2. بعد Runtime screenshots ومقارنة مظهر الشاشات العشر، توقف تمامًا لطلب موافقة المالك.
3. بعد الموافقة فقط: اتبع `prompts/PHASE_2_MASTER_SERVICES.md` ثم prompts خدمة بخدمة. خدمة فعلية كاملة + اختبارات + توثيق قبل الانتقال.
4. السكربتات المعطاة لا تحذف أي بيانات افتراضيًا. مسح البناء والكاش الخاص بالمشروع لا يحصل إلا بعد إثبات أنه ناتج قابل للإعادة وداخل المسار المسموح. لا حذف لعمل Git فريد أو حذف مشاريع أخرى.
5. **Supabase اختياري**. مصدر الحقيقة للتليفون Room + Android APIs. ما فيش خدمة سحابية تقدر تمنح تطبيق عادي صلاحيات Android المحظورة.

### خريطة الملفات

- `design/reference/`: الصور العشر **مستقلة، 864×1536** مع إطار الموبايل الخارجي. تم اكتشاف أن نسخ الحزمة السابقة كانت ملفات **JPEG تحمل امتداد .png خطأً**؛ لذلك احتفظنا بالبايتات الأصلية بامتداد `.jpg` داخل `design/original_jpeg/` وأنتجنا PNG حقيقية من نفس البكسلات داخل `design/reference/` (التحويل لا يستعيد تفاصيل JPEG المفقودة).
- `MASTER_PROMPT_CODEX_PHASE_1_UI.md`: البرومبت الكامل السابق كما هو.
- `prompts/PHASE_2_MASTER_SERVICES.md`: تعليمات العمل لاحقًا خدمة بخدمة.
- `prompts/phase2/`: ملفات Mission منفصلة لكل خدمة.
- `database/local/`: تصميم SQLite / Room للبيانات التي تبقى على الجهاز.
- `database/supabase/migrations/`: SQL اختياري للمزامنة باشتراك صريح وRLS.
- `database/supabase/tests/`: فحص بنيوي للـRLS وخطة عزل مستخدمين.
- `android/integration/kotlin/`: أمثلة منطق حقيقي للتعامل مع Android API + Supabase Kotlin.
- `android/integration/manifest/`: صلاحيات **مرجعية فقط وليست للتطبيق المباشر**.
- `docs/policies/`: توافق Android وGoogle Play ومصفوفة الصلاحيات.
- `docs/testing/`: Golden screenshots، حالات السيناريو، وFake Closure Gate.
- `docs/architecture/`: data flows، الخصوصية، والمخططات.
- `docs/research/`: مقارنة الوكلاء، الأدلة، وتاريخ التحقق.
- `scripts/`: فحص Windows / Git / SHA / ملفات ZIP / المساحة ومرشحات التنظيف.
- `provenance/`: ما يمكن إثباته الآن وما يحتاج تحققًا لاحقًا.

### بداية التشغيل المقترحة في Codex

> Read and execute `MASTER_PROMPT_CODEX_PHASE_1_UI.md` completely, with all original images in `design/reference/`. Work against the real Windows `D:\KNOUX-AI` only if that disk exists and is accessible. Stop strictly after ten real Compose screens, navigation, emulator screenshots, screenshot comparisons, and safe project-local cleanup. The Phase 2 assets and SQL are reference-only during this run. Do not silently execute them.

## اختلافات مهمة في التصميم

- الصور التسويقية فيها Mockup إطار تليفون: لازم ما يتنسخش داخل الـUI.
- قيم 12.6 GB / 8.7 GB / +18% / 91% / 72% وغيرها مش بيانات حقيقية ولا نسبة تقدم مقاسة.
- `Deep Scan` بيعرض `Modules scanned: 5 of 5` في نفس اللحظة مع `72%`: ده تناقض بصري يجب إبقاؤه فقط في ملف لقطة مرجعية / Preview معاد تمييزه.
- أهداف `Android/data`, `Android/obb`, كاش برامج تانية، Privacy trackers على مستوى النظام، فرض hibernation/kill apps، وتحسين بطارية بنسب مضمونة: تحتاج حدود واضحة أو قد تكون غير متاحة لتطبيق عادي.

**مهم: أغراض الحزمة تنفيذية وتعليمية. صلاحية Play / API وتوافق أجهزة السوق لا يُثبتان إلا بالاختبارات والمراجعة النهائية وقت النشر.**
