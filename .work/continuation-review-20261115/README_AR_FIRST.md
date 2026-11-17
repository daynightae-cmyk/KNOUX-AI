# تشغيل المهمة الجديدة — KNOUX AI على D: فقط

**ارفع ملف ZIP ده لوكيل OpenCode الجديد، أو فكّه في مكان خارجي غير المستودع.**

- 00_MASTER_EXECUTION_PROMPT.md: البرومبت التنفيذي الوحيد؛ لا تضف عليه برومبت Pro الخاص بـ K:.
- 01_APPROVED_REFERENCE_IMAGES/: عشر PNG أصلية مرجعية 941×1672، كل واحدة لشاشة مختلفة.
- 02_PREVIOUS_AGENT_RUNTIME_SHOTS/: لقطات من RAR الوكيل السابق (720×1600 غالبًا). صور تاريخية للمقارنة فقط.
- 03_SOURCE_REVIEW_NOTES.md: حقائق المصدر وقيود التحقق؛ لا تعتبرها Git snapshot حي.
- 04_SHA256_MANIFEST.json: تحقق سلامة الملفات داخل ZIP.

**رسالة البداية الجاهزة:**

Read `00_MASTER_EXECUTION_PROMPT.md` completely; review all ten approved PNGs in `01_APPROVED_REFERENCE_IMAGES/` and the prior runtime captures in `02_PREVIOUS_AGENT_RUNTIME_SHOTS/`. Continue existing uncommitted Kotlin/Compose work ONLY at `D:\KNOUX-AI`; do not overwrite parallel agents or touch `K:\KNOUX-AI-Pro`. The Pro crown is verified entitlement, not a separate product theme. Implement real screen-by-screen visual parity in the native shell and verify on Samsung with actual runtime screenshots. Execute, do not stop at planning. No fake metrics, no source restart, no destructive Git, and no byte-level DataStore modifications.

**ملاحظة:** ملف محادثة الوكيل JSON الخام غير مشمول في هذه الحزمة عمدًا لتجنّب إعادة نشر مخرجات طويلة قد تحتوي بيانات داخلية وصورًا مضمّنة. ويمكن إرفاقه بشكل خاص للوكيل بعد تنقيح الأسرار. هذه الحزمة تحتوي خلاصة مراجعة مقيدة بالأدلة. الصور المرجعية أصلية وغير معدلة.
