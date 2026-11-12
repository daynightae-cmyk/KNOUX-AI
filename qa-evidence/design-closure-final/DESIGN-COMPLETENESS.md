# DESIGN COMPLETENESS — Phase 1 UI

**Scope:** Kotlin/Compose preview only. Every number is a design fixture. The app does not
scan, delete, clean, change permissions, alter battery settings, or modify any file.

## 1. Destinations

### Referenced screens (10)

| # | Screen | Route | Reference | Implemented content | Stateful preview |
|---|---|---|---|---|---|
| 01 | Home / AI Smart Clean | `home` | `01_AI_Smart_Clean_Home.png` | brand hero, four capability chips, Start Deep Scan, score panel, six service tiles | tiles + CTA navigate; disclosure sheet |
| 02 | Cinematic splash | start-up | `02_Splash.png` | vector K mark, wordmark, orbit/particle scene, smile cue, fade to dashboard | 3.1 s choreography; reduced-motion path; freeze hook |
| 03 | Privacy Sweep | `privacy` | `03_Privacy_Sweep.png` | privacy score, 4 mini metrics, summary facts, risky-permission rows | row detail sheet, review/proceed sheets, selection |
| 04 | Duplicate Files Finder | `duplicates` | `04_Duplicate_Files_Finder.png` | 1,248 / 8.7 GB metrics, scan stats, five duplicate groups | group selection, review sheet, proceed/confirm/result |
| 05 | Deep Scan | `deep-scan` | `05_Deep_Scan.png` | 72% scan ring, bytes/counts, six scan modules with progress | running / paused / finished preview; recreation-safe |
| 06 | Reports & Insights | `reports` | `06_Reports_Insights.png` | 31 fixes / 12.6 GB summary, device score ring, 3 chart blocks, recommendations | range chips, generate-report sheet, export sheet |
| 07 | App Residue Cleanup | `residue` | `07_App_Residue_Cleanup.png` | 836 MB / 42 apps metrics, facts, five residue groups with checkboxes | select all/row, total recompute, confirm, result |
| 08 | Storage Analyzer | `storage` | `08_Storage_Analyzer.png` | 128/256 GB used, five category bars, largest-category panel, six categories | category detail sheet, breakdown + manage-space routes |
| 09 | Battery Saver | `battery` | `09_Battery_Saver.png` | 6 h 42 m gain, 91% health, facts, six optimization rows with toggles | toggle preview, detail sheet, save-battery confirm |
| 10 | AI Optimization | `ai-optimization` | `10_AI_Optimization.png` | score 94 / +18%, four mini metrics, five optimization tools | row selection, customize/optimize sheets |

### Internal service studios (10)

| # | Screen | Route | Reference | Distinctive studio module |
|---|---|---|---|---|
| 11 | Smart Clean Center | `clean-center` | `01_Smart_Clean_Center.png` | **Clean Reactor** — 2,718 MB recovery lanes, Junk/Temporary/Downloads choices |
| 12 | Duplicate Group Review | `duplicate-group-review` | `02_Duplicate_Group_Review.png` | photo/video contact-sheet pair with Keep Best / Remove Selected |
| 13 | Privacy Risk Inspector | `privacy-risk-inspector` | `03_Privacy_Risk_Inspector.png` | **Privacy Inspector** — risk dial and signal lanes (Trackers/Permissions/Exposure) |
| 14 | Deep Scan Summary | `deep-scan-summary` | `04_Deep_Scan_Summary.png` | **Scan Orbit** timeline with scan modules and 8.4 GB reclaim preview |
| 15 | Performance Report Detail | `performance-report-detail` | `05_Performance_Report_Detail.png` | five-bar weekly optimizer and 92% score card |
| 16 | Residue Item Review | `residue-item-review` | `06_Residue_Item_Review.png` | **Residue Map** node graph plus cleanup safety check |
| 17 | Installed Apps Review | `installed-apps-review` | `07_Installed_Apps_Review.png` | **App Constellation** with Social Apps / Media / Utilities lanes |
| 18 | Storage Breakdown | `storage-breakdown` | `08_Storage_Breakdown.png` | **Storage Orbit** — 128 GB used ring and 45 GB Apps segment |
| 19 | Battery Diagnostics | `battery-diagnostics` | `09_Battery_Diagnostics.png` | **Battery Pulse** — 91% gauge, drain ranking, thermal note |
| 20 | AI Recommendations | `ai-recommendations` | `10_AI_Recommendations.png` | **Model Constellation** — 94/98 scores, five recommendation rows |

### Supporting navigation indexes

| Screen | Route | Role |
|---|---|---|
| Clean index | `clean` | list of Deep Scan, Duplicates, Residue, Smart Clean Center |
| Tools index | `tools` | list of Storage, App Manager, Privacy, Battery, AI Optimization |
| Reports tab | `reports` | reports destination and report-detail entry |

They are part of the functional navigation and are exercised by instrumentation. Clean and
Tools are captured as supporting screenshots in `runtime/support/`.

## 2. Interaction completeness

- All ten referenced screens are reachable from the bottom tabs; Home tiles jump directly to
  the matching service.
- All ten interior studios are reachable through the "Review" action of their parent service,
  and Back returns to the exact parent scroll position.
- Selection is reversible and truthful: selected totals are recomputed, but the confirm sheet
  always ends in **"No action performed - UI preview only."**
- Deep Scan exposes running, paused and finished preview states. The primary action is
  disabled while running, enabled after pause, and survives activity recreation.
- Every sheet has a visible Close action and a back/scrim dismissal path.
- Arabic mirrors the tab order and all rows; numeric tokens keep their own LTR bidi order.
- No action labels are truncated: action buttons accept two lines and share equal width.

## 3. Truthfulness and design-fixture labelling

Visible on every screen:

- A top-bar **Design preview / معاينة التصميم** pill that opens the disclosure sheet.
- A persistent preview disclosure line near the actions: *"All figures are sample data from
  the design. Nothing on your device has been scanned or changed."*
- Result sheets state **"No action performed - UI preview only."**
- Interior studios carry a **DESIGN PREVIEW · No device action is performed** caption.

## 4. Deliberate design extensions

The ten interior studios are an owner-directed extension beyond the supplied interiors. They
are intentionally distinctive and were kept from the previous uncommitted work. They add a
compact interactive module above the detail rows, which reduces the number of reference rows
visible above the fold. Rows remain scrollable and every reference row is present. This is the
main acknowledged visual delta and is recorded in `VISUAL-PARITY.md`.

## 5. Completeness verdict

| Area | Status |
|---|---|
| 10 referenced destinations | Complete and runtime-captured |
| 10 interior studios | Complete and runtime-captured |
| Splash | Complete; live, frozen frames and reduced motion captured |
| English LTR | Complete, 20/20 route captures |
| Arabic RTL | Complete, 20/20 route captures |
| Dialogs/sheets | Settings, disclosure, confirm and result captured |
| Compact/tall viewport | Captured at 360 × 640 dp equivalent |
| Large font | Captured at font scale 1.3 |
| Real device services | **Not in scope; not implemented** |
