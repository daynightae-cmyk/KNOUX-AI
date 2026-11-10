# Visual QA of ten reference images

Reference PNGs: all `864×1536`. Device mockup marketing frame is EXCLUDED from app UI screenshot comparators. Never show screenshots as a flat app background.

| # | Route | Unique hero | Main acceptance |
|---|---|---|---|
| 01 | Home | neon K orb | 6 entry cards, status score row, giant scan CTA, 4 bottom tabs |
| 02 | Privacy Sweep | shield lock | tracker summary, 5 risks, select all, Review/Improve previews |
| 03 | Duplicate Files Finder | stacked images | counters, 5 groups, Preview/Remove with no real delete |
| 04 | Deep Scan | radar | 5 module progress rows, pause/resume DEMO state, no faked scanning |
| 05 | Reports & Insights | upward bars | 5 insight rows, charts, reports tab selected |
| 06 | App Residue Cleanup | stacked layers | 5 selectable rows, safe confirmation UI only |
| 07 | App Manager | 2x2 app grid | 5 app categories, controls, no real querying in Phase 1 |
| 08 | Storage Analyzer | cylindrical disks | 2-column storage category cards, used bar, 50% ring |
| 09 | Battery Saver | holographic battery | 5 recommendation actions and donut, no fake health |
| 10 | AI Optimization | AI microchip | enable-all, 5 tools, preview-only actions |

Design identity: near-black navy #07051C-ish base, electric violet/magenta/cyan gradients, offwhite text, neon glowing strokes, glassmorphic purple panels, orbital heroes and gradient score rings, generous rounded corners, consistent bottom navigation. Sample hex only; tune against measured references.

Requirements: view each original separately; create color/pixel metrics and screenshot crop specifications; parity across EN/AR RTL, medium/small screens, Android font scaling and reduced-motion user setting; ensure contrast and target sizes. For visual golden image comparison exclude changing clock, OS navigation and preview timestamps; save normalized screenshots at fixed emulator resolution/density. Do not declare 100% exact without objective evidence + user approval.
