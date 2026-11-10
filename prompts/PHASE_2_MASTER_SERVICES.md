# KNOUX AI — PHASE 2 PER-SERVICE END-TO-END EXECUTION (NOT AUTHORIZED UNTIL OWNER ACCEPTS UI)

**READ ME FIRST:** Never execute this document during the Phase 1 UI run. This file is a prepared future engineering program. The owner must explicitly approve all ten UI screenshots before starting the next service. Scope is one service at a time, maintaining approved visual identity and canonical `D:\KNOUX-AI` main.

You are Codex, Principal Android Engineer + Product QA + Privacy/Play Compliance Lead. First inspect actual working directory, Git repo `HEAD`, `origin/main`, local/remote branches, worktrees, stashes, CI and untracked files. Check for other active agents. Protect all unmerged unique work before any maintenance or cleanup. Never `git reset --hard`, `git clean`, force-push, discard unknown files or delete external projects. Never claim physical D: access if it is not mounted.

## Per-service recurring process (repeat for each service after owner go-ahead)

1. **REALITY:** check approved UI screen diff against saved reference; identify current API/service implementation and tests. Record exact commits, pending unmerged work and device restrictions.
2. **FEASIBILITY:** map allowed Android APIs with SDK guards, permissions, Google Play policy and user disclosures. Make unsupported behaviors explicit with an honest disabled state. No unauthorized Shizuku/Accessibility/root/device admin workarounds.
3. **DATA:** create typed domain contracts, API adapter, local-only Room migrations and repository; precise attribution of `sample/preview/measured`, scope, timestamp and method. Respect preview/real isolation.
4. **ACTIONS:** connect actual scan/useful information, show pause/cancel/resume, errors and empty results. For any action touching user data, use review → explicit selection → consent → perform → re-read → journal. No silent or irreversible bulk deletion.
5. **SYNC:** implement optional cloud only after owner enables it. Supabase must authenticate via user JWT, client only uses publishable key, RLS verifies owner, consent is revocable; never upload raw content, paths, hashes or package names.
6. **QA:** unit tests of calculations, fakes and permissions; Android instrumentation on API 26/30/33/35/36 where possible; physical phone smoke tests for OEM differences; RTL and accessibility; launch screenshot diffs vs approved UI.
7. **DELIVER:** commit reviewable code, integrate safely with canonical main when possible, CI and screenshot evidence, exact SHA of local and remote main, user consent and permissions audit, measured metrics reconciliation, free disk before/after. Report status VERIFIED / PARTIAL / BLOCKED / UNSUPPORTED with actual evidence.
8. **STOP** after the single service is verified and approved. Do not begin next service automatically.

## Ordered service implementation

1. Storage Analyzer (low-risk, real StatFs + accessible media totals)
2. Duplicate Files Finder (exact matches + opt-in user selection; no auto deletion)
3. App Residue Cleanup (only user-granted accessible locations, no app sandbox bypass)
4. Privacy Sweep (honest exposure signals, not global invisible trackers)
5. App Manager (filtered visibility and optional Usage Access)
6. Battery Saver (measure public indicators, no fake promises)
7. Deep Scan (coordinate verified modules; robust progress)
8. Reports & Insights (history derived from actual journals)
9. AI Optimization (explainable advisory engine, no fake hibernation)
10. Home / Smart Clean integration (genuine cross-module summaries + launch routing)

## Database integration order

- Local Room before cloud. Build migrations from current existing schema; do not apply illustrative `database/local/001_illustrative_room_schema.sql` as a replacement if migration already exists.
- Opt-in Supabase: create owned separate project or use specifically approved existing one; apply migrations in order; ensure auth setup and forced RLS; run 2-user isolation tests and account deletion tests. Only after consent build validated Kotlin client. No placeholder secrets in production.
- Schema migrations and Supabase Kotlin examples in this kit need compilation/SQL execution in staging before claiming VERIFIED. Project uses API 36 target for new Play phone apps, with minSdk chosen after actual business requirements.

## Real vs mock guard

UI fixtures have `PreviewDataSource` and visible `Preview` indicator, NEVER plugged into repositories marked `Measured`. Real source must give provenance, permissions, confidence, item count and timestamp. A successful tap or an animated progress ring is NOT evidence of a scan or cleanup.
