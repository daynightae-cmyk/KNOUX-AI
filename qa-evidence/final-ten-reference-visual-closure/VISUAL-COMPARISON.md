# Visual Comparison — Ten Approved References vs. Runtime Capture

Every reference was captured on a real device (emulator `knoux_qa26`,
API 26, 1080 × 2400, 420 dpi) with the **same** build that produced the
evidence APK. The external handset frame is not present in either image, so
no masking was required.

References: `references/ref-01.jpg` … `references/ref-10.jpg`
Captures:   `runtime/01-smart-device-hub.png` … `runtime/10-auth-verify-identity.png`

Arabic / RTL captures: `arabic-rtl/`

## Per-screen comparison

| # | Reference | Capture | Result | Notes |
|---|-----------|---------|--------|-------|
| 01 | Device Tools | `03-device-tools.png` | **MATCH** | Featured Storage Analyzer card with lit rim and 50 % bar, five service rows (App Manager 41, Privacy Sweep 7 trackers, Battery +18 %, AI Optimization 94, Storage Breakdown 12.6 GB), four satellite tiles orbiting the hero, Tools tab lit. |
| 02 | Reports & Insights | `04-reports-insights.png` | **MATCH** | Segmented 7/30/90-day control, weekly activity bar chart with a 1.6 → 0 GB top-down axis, Storage Trend and Privacy Review sparkline cards, Historical Activity row, gold DESIGN PREVIEW chips on every data card. |
| 03 | AI Optimization | `06-ai-optimization.png` | **MATCH** | Four metric cards (94/100, +18 %, 1.6 GB free, Normal), "Optimization Opportunities" divider with three glowing spheres (Performance / Privacy / Battery), primary action and preview note. |
| 04 | Privacy Sweep | `05-privacy-sweep.png` | **MATCH** | Four risk cards (Camera / Microphone / Background Trackers / Clipboard) with High / Medium / Low pills, segmented risk donut at 38 % with the four-colour legend, "Sample data only — … Not a real device scan." footnote, Tools tab lit. |
| 05 | Smart Clean | `02-smart-clean.png` | **MATCH** | Cleanup Potential donut (2.86 GB across 4 categories), four selectable categories with size chips and selection indicators, ghost + primary action pair, preview note. |
| 06 | Smart Device Hub | `01-smart-device-hub.png` | **MATCH** | Particle-globe hero with the brand K, six-card service grid with chevron buttons and volumetric icons, Quick Actions row (Deep Scan / Save Battery / Secure Device), Home tab lit. |
| 07 | Biometric unavailable | `08-auth-biometric-unavailable.png` | **MATCH** | Gold warning badge on the print, "Biometric unavailable", three provider rows, compact glass header with back arrow / wordmark / Pro / gear. |
| 08 | Identity verified | `09-auth-identity-verified.png` | **MATCH** | Cyan check mark drawn over the print, "Identity verified", "Opening your dashboard…", wordmark without the PREVIEW badge. |
| 09 | Verify your identity | `10-auth-verify-identity.png` | **MATCH** | Scanning print with travelling light sweep, "Waiting for biometric verification…", Cancel action, compact glass header. |
| 10 | Welcome to KNOUX AI | `07-auth-welcome.png` | **MATCH** | Centred wordmark with divider and gold PREVIEW badge, two-line "Welcome to / KNOUX AI", holographic loop-pattern print over a lit pedestal ring, three provider rows (four-colour Google mark), underlined privacy link. |

## Structural checks performed on every capture

| Check | Result |
|-------|--------|
| Exactly one logo lock-up per screen | PASS |
| Exactly one **Pro** badge per screen | PASS |
| No duplicated back arrow | PASS — Home shows the menu control, every other screen shows a single back arrow |
| No duplicated settings control | PASS |
| Header height | PASS — one 56 dp glass capsule, 31 dp logo |
| Touch targets | PASS — all interactive controls ≥ 48 dp |
| Clipped or overlapping text | PASS — no clipping at 420 dpi, and none at the Arabic pass |
| Design-preview labelling | PASS — every screen carries a gold DESIGN PREVIEW pill; every data card additionally carries its own chip; every screen carries a plain-language preview note |
| No fabricated device results | PASS — no screen claims a completed scan, cleanup, privacy fix or battery optimisation |

## Arabic / RTL pass

`arabic-rtl/` holds Arabic captures of the same seven surfaces (six master
screens plus the welcome gate).

* Layout direction mirrors correctly: the bottom navigation reads
  Reports · Tools · Clean · Home from left to right, the header mirrors its
  controls, and chevrons point left.
* The wordmark is LTR-locked so the brand mark is never mirrored.
* Numerals stay Western (`1.6 GB Free`, `94 / 100`, `+18 %`) and are placed
  correctly for right-to-left reading.
* No text is clipped, truncated or overlapped in any Arabic capture.

## Known, unavoidable differences

1. **Fingerprint artwork.** The reference print is a rendered bitmap with
   film-grain, chromatic aberration and a background of particle ribbons. The
   application draws its print procedurally on a Compose `Canvas` (concentric
   arch ridges, broken outer segments, a lit pedestal ring), so the geometry
   reads equivalently but the grain and the background ribbons are not
   reproduced.
2. **Google provider mark.** Reproduced as the classic four-segment ring
   (red / yellow / green / blue with the horizontal bar) rather than the
   official raster asset, which cannot be shipped in this repository.
3. **Font.** The reference uses a heavy geometric display face. The
   application uses its bundled display family, which is metrically similar
   but not identical.
4. **Biometric success path.** The emulator has no enrolled fingerprint, so
   the VERIFYING and VERIFIED captures were taken through the debug-only
   `knoux.preview.auth` extra. See `AUTH-AND-HEADER-QA.md`.
