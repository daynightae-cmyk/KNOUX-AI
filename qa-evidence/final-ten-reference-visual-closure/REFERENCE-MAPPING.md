# Reference Mapping — Ten Approved References

This document maps every one of the ten approved visual references to the
screen that implements it in the KNOUX AI Android application.

The ten references were recovered from the session attachment store and are
preserved byte-for-byte in `references/` (JPEG, as originally uploaded). The
original uploads are **not** modified, converted, or deleted.

| # | Reference file | Reference screen | Implementation | Route / state | Runtime capture |
|---|----------------|------------------|----------------|---------------|-----------------|
| 01 | `references/ref-01.jpg` | Device Tools | `DeviceToolsScreen` | `tools` | `runtime/03-device-tools.png` |
| 02 | `references/ref-02.jpg` | Reports & Insights | `ReportsScreen` | `reports` | `runtime/04-reports-insights.png` |
| 03 | `references/ref-03.jpg` | AI Optimization | `AiOptimizationScreen` | `ai-optimization` | `runtime/06-ai-optimization.png` |
| 04 | `references/ref-04.jpg` | Privacy Sweep | `PrivacySweepScreen` | `privacy` | `runtime/05-privacy-sweep.png` |
| 05 | `references/ref-05.jpg` | Smart Clean | `SmartCleanScreen` | `clean` | `runtime/02-smart-clean.png` |
| 06 | `references/ref-06.jpg` | Smart Device Hub | `SmartHubScreen` | `home` | `runtime/01-smart-device-hub.png` |
| 07 | `references/ref-07.jpg` | Biometric unavailable | `AuthGateway` — UNAVAILABLE | auth state | `runtime/08-auth-biometric-unavailable.png` |
| 08 | `references/ref-08.jpg` | Identity verified | `AuthGateway` — VERIFIED | auth state | `runtime/09-auth-identity-verified.png` |
| 09 | `references/ref-09.jpg` | Verify your identity | `AuthGateway` — VERIFYING | auth state | `runtime/10-auth-verify-identity.png` |
| 10 | `references/ref-10.jpg` | Welcome to KNOUX AI | `AuthGateway` — WELCOME | auth state | `runtime/07-auth-welcome.png` |

## Notes on interpretation

* Reference 06 (Smart Device Hub) is the application's home surface. The
  reference shows a six-card service grid plus a Quick Actions row; both are
  implemented in `MasterScreens.kt` (`SmartHubScreen`).
* References 01–05 are the four bottom-navigation destinations plus the two
  deeper service pages reached from **Device Tools**. The bottom navigation keeps
  exactly four tabs (Home, Clean, Tools, Reports) as in the references;
  Privacy Sweep and AI Optimization are reached from Device Tools and therefore
  highlight the **Tools** tab, which is what the references show.
* References 07–10 are four states of one screen, not four screens. They share a
  single implementation (`AuthGateway` in `AuthScreen.kt`) driven by
  `AuthStage`.
* Reference 08 shows the "Identity verified" state with a KNOUX wordmark and a
  fingerprint carrying a cyan check mark; reference 10 shows the wordmark with a
  gold PREVIEW badge. Both are rendered by the same lock-up composable with the
  badge toggled per state.

## Structural elements carried across the set

| Reference element | Implementation |
|--------------------|----------------|
| Compact glass capsule header, 56 dp, single logo lock-up | `KnouxTopBar` in `MainActivity.kt` |
| Gold outlined **Pro** pill (crown + label) | `KnouxTopBar` |
| Four-tab bottom navigation with a lit gradient disc on the active icon | `KnouxBottomNavigation` in `MainActivity.kt` |
| Holographic hero artwork (particle globe, orbital arcs, pedestal) | `NeonHero` in `NeonArt.kt` |
| Volumetric service icons with lit rim | `MasterIcon`, `IconBadge`, `VolumetricGlyph` |
| Gold **DESIGN PREVIEW** disclosure | `DesignPreviewPill` in `KnouxComponents.kt` |
| Honest-preview footnote strip | `PreviewNote` in `MasterScreens.kt` |
