# Navigation QA

Scope: the identity gateway, the four-tab bottom navigation, the six master
screens, the deeper service pages, the back stack, and Arabic / RTL ordering.

## 1. Information architecture

```
Splash
  └── Identity gateway (AuthGateway, four states)
        └── Continue as Guest
              └── Smart Device Hub  (Home tab)
                    ├── Smart Clean        (Clean tab)
                    ├── AI Optimization    (Tools tab)
                    ├── Privacy Sweep      (Tools tab)
                    ├── Storage Analyzer   (Tools tab)
                    ├── App Manager        (Tools tab)
                    └── Reports & Insights (Reports tab)
```

The bottom navigation keeps exactly **four** tabs — Home, Clean, Tools,
Reports — matching the approved references. Privacy Sweep and AI Optimization
are reached from Device Tools and therefore keep the **Tools** tab lit, which
is what the references show.

## 2. Tab behaviour

| Tab | Destination | Screen tag |
|-----|-------------|-----------|
| Home | Smart Device Hub | `screen-home` |
| Clean | Smart Clean | `screen-clean` |
| Tools | Device Tools | `screen-tools` |
| Reports | Reports & Insights | `screen-reports` |

The active tab is drawn as a filled magenta-violet disc behind its icon,
matching the reference navigation. Tab labels read Home / Clean / Tools /
Reports in English and الرئيسية / تنظيف / أدوات / التقارير in Arabic.

## 3. Header behaviour

| Screen | Leading control |
|--------|-----------------|
| Home | Menu control |
| Every other screen | Back arrow (`back` tag) |
| Every screen | One logo lock-up, one **Pro** pill, one gear |

There is no duplicated logo, back arrow, badge or settings control on any
screen.

## 4. Verified navigation paths

### 4.1 The identity gateway

`AuthGatewayTest` (5 tests, all passing):

| Test | Assertion |
|------|-----------|
| `welcomeStageOffersTheThreeDocumentedProviders` | The fingerprint plus all three provider rows are present on the welcome state |
| `passkeyProviderReportsAnHonestSetupPendingState` | The passkey sheet states the provider is not configured |
| `googleProviderReportsAnHonestSetupPendingState` | The Google sheet states the provider is not configured |
| `guestEntryOpensTheApplicationShell` | Guest entry reveals the bottom navigation and the design-preview marker |
| `theGatewayNeverShowsAVerifiedStateWithoutABiometricCallback` | The VERIFIED state is unreachable without a real biometric success |

### 4.2 Tabs reach all screens and back returns to the index

`PreviewNavigationTest.tabsReachAllTenScreensAndDetailBackReturnsToTheIndex`:

* Home → Clean → Deep Scan / Duplicates / Residue, each returning to Clean
  with the **Clean** tab still selected;
* Clean → Tools → Storage / App Manager / Privacy / Battery / AI Optimization,
  each returning to Tools with the **Tools** tab still selected;
* Tools → Reports, with the **Reports** tab selected;
* the Reports primary action opens a review sheet whose confirmation shows
  `preview-only-result` — proof that the action performs no real work.

### 4.3 Deeper pages and the back stack

`PreviewNavigationTest.outerReviewOpensAnInnerPageAndBackReturnsToItsOrigin`
opens Duplicates → its review sheet → the duplicate-group review, asserts the
**Clean** tab is still lit and that the inner selection survives, then returns
and asserts the origin screen is restored.

### 4.4 Actions never perform real work

| Test | Assertion |
|------|-----------|
| `scanPauseAndResumeOnlyChangePreviewStateAndSurviveRecreation` | The scan primary action is disabled while paused and the state survives an activity recreation |
| `residueSelectionUpdatesTotalAndConfirmationDoesNotClean` | Selecting and clearing changes the sample total; confirming reports "No cleanup performed" |
| `cleanCenterSelectionChangesThePreviewTotalAndDoesNotClean` | The clean-centre total tracks the selection and the confirmation reports "No action performed" |
| `everyRemainingPrimaryActionOpensAReversiblePreview` | Every remaining primary action across all service screens opens a reversible preview that reports no action |

### 4.5 Arabic / RTL ordering

`PreviewNavigationTest.arabicSwitchMirrorsTabOrderAndPreservesNavigation`
switches to Arabic through the settings sheet and asserts:

* the Home tab label becomes `الرئيسية`;
* the tab order mirrors — `home.left > reports.left`, i.e. the Home tab moves
  to the right-hand end;
* navigation into a service page still works and renders Arabic copy
  (`توفير البطارية`).

### 4.6 Service worlds and splash

| Test | Assertion |
|------|-----------|
| `ServiceWorldsTest.tenIndependentServiceStudiosRenderWithInteraction` | All ten service studios render and respond to interaction |
| `SplashSequenceTest.liveSmileCuesOnceAtCompletionThenFinishes` | The splash cues once and finishes |
| `SplashSequenceTest.frozenDebugFrameStaysVisibleForCapture` | A frozen splash frame remains capturable |
| `SplashSequenceTest.reducedMotionShowsStaticBrandAndFinishesQuickly` | Reduced motion renders a static brand frame and finishes quickly |

## 5. Back-stack rules

* The back arrow pops the navigation stack; when the stack is empty it falls
  back to the Home tab.
* `switchTab` pops to Home with `saveState = true` and restores per-tab state
  with `restoreState = true`, so switching tabs and returning does not reset
  the scroll position of the previous screen.
* Detail screens opened from a review sheet return to their origin section
  with that section's tab still lit.

## 6. Result

All 16 instrumentation tests pass on `emulator-5554` (see
`BUILD-AND-TESTS.md`).
