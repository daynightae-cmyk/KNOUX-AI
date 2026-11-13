# OWNER P0 FULL-FUNCTIONALITY OVERRIDE: Read docs/pro/PC_PRO_OWNER_FULL_FUNCTIONALITY_OVERRIDE_20261112.md FIRST. It supersedes any preview-only / read-only limitation that would prevent actual user-controlled operations available through authorized Android APIs, user permissions, or permitted optional advanced modes. Build working user journeys (including user-approved trash and permanent delete where supported), not display shells. Do not fake unsupported powers, completion or entitlement.

# KNOUX AI PC — PRO / ELITE FULL-STACK MASTER
Date: 2026-11-12. Status: IMPLEMENTATION CONTRACT, NOT DEPLOYED.

**Canonical Windows repo:** D:\KNOUX-AI (main). Independent phone OpenCode project is OUT OF SCOPE; never merge or synchronize the two.

## 0. Verified baseline and risk
- At authoring: HEAD e5b0994555aa181d00d9f75f292e4ead4b602655, main tracking origin/main. RECHECK current SHA/status and active agents before modifying Kotlin.
- ProSystem.kt and DevAccess.kt exist as unique untracked files; live agent is editing MasterScreens.kt and other visual Kotlin. Protect all tracked/untracked source, UI, artwork and QA.
- ProSection declares 10 sections, but getFeaturesForSection() maps only 6 feature collections (Smart Clean, Privacy, Deep Scan, Reports, App Manager, AI). Duplicates, Residue, Storage and Battery still need complete mapping.
- Current visible 29 AED/month and 249 AED/year are UI strings only, NOT verified Google Play product prices. The KX-... random trial-code button is not genuine activation. The temporary developer override must never ship in a release APK.
- Current Pro features include unsupported Android promises: arbitrary app freeze/clone, backing up other app private data, system-wide acceleration, all-folder access and universal rollback. The catalog remaps these to truthful capabilities or UNSUPPORTED.
- PC data repo already has database/supabase/migrations/001_core.sql, 002_rls.sql and 003_account_erasure.sql; NO verified Pro migration deployment. Reference candidate: database/supabase/drafts/004_pro_entitlements_DRAFT.sql. Never run on production without staging tests and approval.
- Physical Android phone is NOT an authorized target for this PC agent. Emulator only with verified emulator serial. Preserve independently created phone project.
- Existing Pro UI, cinematic artwork, header and service identities are unique work. Integrate functionality underneath; do not build another generic shell.

## 1. The machine-readable source of truth
docs/pro/PC_PRO_FEATURE_CATALOG_20261112.json = 10 sections x 5 features = 50 proposed feature contracts. Each has stable feature_id, defined supported scope, Android mechanism, feasibility and acceptance proof. This is designed data, not implemented services.

Preserve already-known feature IDs: smart_clean_*, prv_*, scan_*, rep_*, app_*, opt_*. Add missing dup_*, residue_*, sto_*, bat_* IDs. Add a catalog parser/validator and deterministic enum-to-catalog mapping. Fail on missing section, duplicated ID, unknown tier, or a paid feature offered despite unsupported status.

Implementation status states:
CATALOGED; IMPLEMENTED_UNVERIFIED; VERIFIED_ON_SUPPORTED_SCOPE; BLOCKED_EXTERNAL; UNSUPPORTED_AS_CLAIMED.
UI status must match observed runtime evidence; no fake VERIFIED from an icon or Pro badge.

## 2. Pricing and entitlement trust model
FREE: truthful core safe Android functionality remains accessible.
PRO: only individually verified premium capabilities; can cover all app or one separately purchased section depending on actual Play SKU terms.
ELITE: future tier, not sellable until separately implemented, priced and tested.
OWNER DEBUG: developer testing only; a bypass never means the account owns an entitlement.

Authorization = authenticated owner + server-verified grant + product or feature scope + status/time window + platform capability + user runtime consent.
Do NOT trust local generated codes, a DataStore isPro boolean, screenshots, a WhatsApp message, an app reinstall, or user-editable clock as a purchase proof.
Price is rendered from real Google Play ProductDetails, NOT hardcoded AED text. Do not invent active SKU IDs.
WhatsApp may remain support only, not the default digital payment escape from Play Billing restrictions.
Do not upsell features that Android cannot actually perform.

## 3. Four layers every Pro feature must use

### A: Android platform / domain
Typed ProFeatureId, ProSectionId, ProductTier, EntitlementState, ProAccessDecision, CapabilityResult and Evidence<T>; repositories must expose StateFlow for real values. Share existing Android adapters with FREE flows, not duplicate scanning to look premium.
Observation provenance: source, scope, capturedAt, permissionState, status, confidence and limitations. Only user-approved operations can mutate files. Android permission denial must degrade visibly, never create success.

### B: Local persistence
Inspect current Room or SQL before adding anything. Introduce versioned migrations for local:
- entitlement verification cache (signed/verified metadata, grant, scope, validity, last checked; non-authoritative)
- Pro feature usage journal and idempotency keys
- scheduled review rules and last actual run
- operation ledger (candidate, selected, OS approval, attempted, per-URI outcome, requery, actually verified bytes, recovery)
- recommendations and evidence references.
Use DataStore for settings/consent/UI only, never license authority. Make process-death behavior predictable and avoid destructive migrations.

### C: Billing and secure server
Google Play builds normally use the Google Play Billing Library; actual current policy applies to any alternative billing programme. Implement real ProductDetails and offer presentation, purchase, pending, failure, cancel, restore, renewal and refund paths.
Purchase token goes only to authenticated secure backend over TLS. Backend verifies via Google Play Developer API (subscriptionV2 state), checks ownership/product/expiration/linked tokens, updates entitlement transactionally and acknowledges as required.
RTDN requires authenticated Pub/Sub ingress; an RTDN event is a change notification, not proof. Backend always re-queries Google, handles grace, hold, expiry, revoke/refund/replay and account removal. Store token digests not plaintext in ordinary reporting; any needed verification token storage must be server-secure and encrypted.
No service-role, Play service account or billing signing key in APK or git.
Trial codes must be securely generated, hashed and redeemed on backend, rate-limited, expiring, one-time/account-bound, and replay-resistant. Random KX-... created by an Android button grants nothing.
Guest mode does not confer a paid authenticated identity.

### D: Supabase data model / RLS
Pro draft (NOT deployed): ka_pro_plans, ka_pro_entitlements, ka_pro_purchase_events, ka_pro_trial_codes, ka_pro_trial_requests, ka_pro_access_audit.
Signed-in clients may read own entitlement and own trial request only. Users cannot INSERT/UPDATE/DELETE a Pro grant. Server alone changes purchase and entitlement states. Test with two actual JWTs, malicious owner_id, changed expiration, duplicate RTDN, race in one-time redemption and refund.
Reconcile current 001–003 schema with draft, review erasure cascading, run staging migrations and policy tests. Don't modify any live Supabase project without confirming the exact authorized target.
Cloud billing data must never include raw device filenames, URIs, content hashes, media or package inventories.

## 4. Ten Pro sections — real scope

**01 Smart Clean Pro (START HERE):** unlimited eligible *review* sessions within resource bounds, WorkManager read-only scheduling, local explainable pattern analysis, own job queue priority only, OS-supported recovery journal. No global CPU boost, silent cleanup or guaranteed undo.

**02 Duplicate Finder Pro:** ContentResolver/SAF/MediaStore stream-hashed exact duplicates, KEEP_ONE invariant, optional opt-in similar photo suggestions separated from exact, batch user review, OS Trash consent and verified outcome journaling.

**03 App Residue Pro:** app-owned cache plus user-selected readable folder residue candidates only, conservative classifier, dry-run, explicit selection and SAF/MediaStore authorization. NEVER other apps private caches or Android/data.

**04 Privacy Sweep Pro:** evidence from accessible PackageManager/permissions, optional user-imported AES-GCM/Keystore vault, local evidence reports, history of KNOUX observations only. No false system-wide camera/mic log, tracker blocking or retrospective 30-day permissions history.

**05 Deep Scan Pro:** combined VERIFIED adapters, user-scoped profiles, read-only WorkManager schedule, metadata report. No privileged hidden file acquisition or malware detection certification without signatures and documented test corpus.

**06 Reports Pro:** 7/30/90-day records backed by actual Room journal, locally exported sanitized PDFs, evidence-based forecast, own-device baseline comparisons and opt-in weekly notices. No made-up global leaderboard.

**07 App Manager Pro:** PackageManager scoped visibility, optional UsageStats special permission, system-controlled uninstall/details intents. Arbitrary freeze, all-app private backup and arbitrary app cloning are UNSUPPORTED and must not be sold.

**08 Storage Analyzer Pro:** StatFs volume totals separate from MediaStore/SAF accessible category subtotals, large file review, historical snapshots, scope-aware what-if and local user export. No hidden app cache size claims.

**09 Battery Saver Pro:** real ACTION_BATTERY_CHANGED, BatteryManager and permitted PowerManager thermal signals; history, charge patterns, user-selected alerts and Settings handoff. No fake CPU tuning, OEM cycle count or saved-hours guarantee.

**10 AI Optimization Pro:** deterministic local rule recommendations over verified observations, consented adaptive suggestions, guarded OS settings and offline-first behavior. Optional remote AI only with verified provider/backend/consent and typed validation. No invented performance increases or system-wide networking priority.

The five concrete feature IDs and acceptance methods for EACH are in the JSON catalog; do not omit them.

## 5. Execution — foundation, first service, then automatic progression

### PASS 00 — foundation / Pro security
- Re-audit HEAD, worktree, origin/main, stash/reflog, concurrent writers and all untracked work, including ProSystem.kt / DevAccess.kt.
- Resolve missing four Pro mappings without erasing existing feature content or changing live files owned by another worker.
- Create typed catalog/entitlement/capability interfaces; deny access on unknown or expired grant.
- Remove release-variant reusable developer passwords, five-tap bypass, client code minting and unverified paywalled claims. Debug-only feature access clearly marked local TEST and cannot write paid server entitlements.
- Tests: 10 sections/50 unique IDs, no unknown tiers, no cross-user license access, missing SKU, trial replay/expiry, debug-vs-release, RESTORE/PENDING/OFFLINE/REVOKED.
- Do not claim production payment readiness if Play Console and server are unconfigured; mark CONFIGURATION_REQUIRED.
- Preserve approved premium glass UI, small inset-safe header, no false scan success.

### PASS 01 — Smart Clean Pro COMPLETE VERTICAL SLICE
Mandatory existing IDs:
smart_clean_unlimited, smart_clean_scheduled, smart_clean_deep_analysis, smart_clean_priority, smart_clean_rollback.

Implement in this order:
1. Real read-only candidates for the app's own cache and user-granted accessible MediaStore/SAF content, with permission rationale, explicit scope and reliable Long bytes. Reuse existing Storage/Duplicate/Residue adapters where present; implement minimal shared primitives if absent.
2. Domain types SmartCleanCandidate, CleanupPlan, SmartCleanSnapshot, ProGateDecision, OperationOutcome and scoped error states with evidence timestamps.
3. Transactional Room scan history, per-item candidate audit, action journal, durable schedule and recovery metadata. Never use fixtures as real measured values.
4. Existing SmartCleanScreen + ProGate integration: verified metrics, free/pro feature cards, clear preview vs real labels, category list, item selection, user review, consent and explicit action. Keep art, glass colors, responsive fixed header/footer, RTL/LTR and accessibility.
5. WorkManager scheduling for read-only review only. Periodic work is inexact (minimum 15 minutes); support cancel/reschedule and permission revocation. Never schedule an automatic destructive cleanup.
6. Cleanup execution ONLY for user-selected test media with OS-mediated Trash request where supported, or user-granted SAF; re-query and journal actual result. Never run destructive tests on the owner's personal phone or real personal files.
7. At least: access denied, partial grant, same URI twice, changed item, double press, cancel OS dialog, partial success, restart, retry, no internet, expired paid rights, out-of-date cache, RTL and short viewport.
8. Build, unit test, lint and Android instrumentation on a VERIFIED EMULATOR serial. Capture actual screenshots and SHA; old build results do not verify latest changes.

Strict PASS01 gate: tested local read-only discovery + safe selected action where platform permits + Room persisted true results + checked paid gate + working UI + screenshot navigation + no regression. A missing Play account/backend is a documented external blocker, NOT permission to fake subscription completion.

### PASS 02..10 — remaining sections
After verified independent Smart Clean scope, automatically continue:
Duplicates → App Residue → Privacy → Deep Scan → Reports → App Manager → Storage → Battery → AI Optimization.
For each: inspect existing code; implement Android adapter/domain/Room/UI/Pro entitlement/test; record evidence; integrate only if safe; move on. Block unsupported claims clearly and continue independent work. NO A/B/C questions.

### PASS 11 — commercial/server release gate
Implement and verify real Play products, server token verification, RTDN, RLS, verified trial grants, refunds, cancellation, privacy & terms, account deletion and release build checks only after the owner authorizes the real Play Console/Cloud/Supabase targets and provides required external setup through secure configuration.

## 6. Evidence contract and no fake closure
Record in qa-evidence/pro-end-to-end/PRO_EXECUTION_LEDGER.md:
- actual current HEAD and repo scope; all unique work
- individual feature status, exact changed files, and source/permission mapping
- build/test command + exit code + counts, evidence screenshots and APK SHA
- staging DB/RLS outcomes or NOT RUN and precise blockers
- RUNTIME screenshot vs approved identity; no cropped CTA, RTL regression or duplicate header
- server purchase token flow/Play test only when actual Play configured
- action lifecycle and evidence; no simulation passed off as real
- next section and dependency-safe remaining tasks.

A Kiro message saying DONE/COMPLETE, a compiled APK, a WhatsApp conversation, a locally generated activation code, or premium pricing card is NOT proof of an active paid full-stack product.
Do NOT commit or push while concurrent writers own affected files; inspect integrity, protect unique work and coordinate one active writer per shared file.
Never use reset --hard, clean, force-push, unreviewed git checkout/restore, delete or destructive cleanup.

## 7. Official technical references
Google Play payments: https://support.google.com/googleplay/android-developer/answer/9858738
Google Play Billing: https://developer.android.com/google/play/billing/integrate
Subscription state lifecycle: https://developer.android.com/google/play/billing/lifecycle/subscriptions
RTDN: https://developer.android.com/google/play/billing/rtdn-reference
Android WorkManager: https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work
Android package visibility: https://support.google.com/googleplay/android-developer/answer/10158779
Supabase Row Level Security: https://supabase.com/docs/guides/database/postgres/row-level-security

Next action: Kiro reads this master + JSON + DRAFT SQL, then the independent kickoff in prompts/pro/PC_PRO_PASS01_EXECUTE_20261112.md. No merging with the mobile OpenCode version.
