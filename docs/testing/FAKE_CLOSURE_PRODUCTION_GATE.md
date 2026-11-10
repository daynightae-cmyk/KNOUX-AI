# KNOUX AI — Zero Fake Closure Protocol

## Status vocabulary (never collapse categories)

- `DESIGNED`: reference drawn only.
- `UI_IMPLEMENTED`: actual native widgets and navigation in source.
- `BUILD_VERIFIED`: Gradle build command, exit code and actual artifact hash.
- `RUNTIME_VERIFIED`: installed and launched on named emulator or physical device with timestamp and screenshot/logcat.
- `VISUAL_APPROVED`: screenshots compared, diffs filed, owner approved; a build alone is NOT proof.
- `SERVICE_IMPLEMENTED`: public Android API service connected, not merely a button or fixture.
- `SERVICE_VERIFIED`: realistic happy + denied/partial/error tests, sensor/OS output checked.
- `MERGED`: exact SHA in canonical main with remote tracking evidence.
- `RELEASED`: real signed release artifact and store/deployment evidence.
- `UNVERIFIED`, `BLOCKED`, `DEFERRED`, `UNSUPPORTED` are allowed honest outcomes.

## Phase 1 exit evidence (no exceptions)

- 10/10 actual Compose screen IDs, navigation graph routes, tap/selection state, portrait/RTL/LTR/disabled permission-preview labels.
- 10 emulator screenshots with screenshot paths, emulator model, API level, locale, density, display dimensions.
- Reference-vs-runtime overlay/diff (per screen) with measurable center alignment, spacing, text overflow, icons and responsive behavior. Human review still required.
- Build and unit test command output/exit code (e.g. `./gradlew :app:assembleDebug :app:testDebugUnitTest`); instrumentation tests if available. Check ADB process, ANRs and crash logcat.
- Git `status`, `HEAD` SHA, `origin/main` SHA, PR/CI where relevant; in GitHub repos show whether local commit is pushed or merged.
- Disk free **before and after**, itemized *only project-owned* verified removable outputs with deletion confirmation and SHA/owner/worktree reconciliation.
- One `evidence/FINAL_UI_GATE.md`: claim ledger, 10 artifacts, unresolved differences, and HARD STOP. Empty folders are NOT evidence.

## Service exit gates (Phase 2 only)

1. API supported on selected Android versions and Play policy scope documented.
2. Explicit permission workflow; denial/cancel behavior works.
3. Actual service result from controlled samples with known expected output, not preview data.
4. Database mutations are transactional; app remains offline-capable.
5. Destructive changes require explicit file-by-file selection, OS/user confirmation, action journal, and safe default.
6. Tests on API 26, 30, 33, 35, 36 where device/emulator available and Android 14 partial media access.
7. Screenshots preserve the approved UI, no regressions in RTL or 360dp small screens, dark/landscape/tablet graceful layout.
8. CI + app launch + privacy/security scan + exact Git SHA and remote state.
9. `DONE` only for evidenced outcomes; otherwise `PARTIAL/UNVERIFIED/BLOCKED`.

## Failure scenarios mandatory

- empty library / 0 results; no permission; partial media permission; file removed while scanning; reused URI; low storage; process death; app restart; cancel during hashing; network offline; Supabase unauthorized; user revokes cloud sync; duplicate name in other folders; same-size different bytes; 2-user RLS cross-tenant attempts; loss of Android emulator midway.
- State mutation must happen from actual completed Android operation, never tap alone.
