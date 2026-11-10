# Future capability contracts (Phase 2; no implementation)

Phase 1 renders immutable reference fixtures only. A future live data source should distinguish unavailable, observed, derived, and estimated values. Missing permissions or unsupported data should surface an unavailable state and an explanation. Never use a fixture as a live fallback.

| Order | Module | Future contract and boundary |
|---|---|---|
| 1 | Deep Scan | Emit module results and progress from work actually performed. Propagate cancellation, partial access and errors. |
| 2 | Storage Analyzer | Return summaries with source and scope. Use scoped Android storage APIs, MediaStore and SAF grants as appropriate; distinguish overlap from additive usage. |
| 3 | Duplicate Files | Produce exact groups from accessible streams, with similarity separate. Require explicit selection and Android confirmation for any supported destructive action. |
| 4 | App Manager | List only visible packages and usage where authorized; provide supported system handoff for app settings and removal. |
| 5 | Privacy Sweep | Separate observed permissions, inferred signals and unavailable information; do not infer that trackers were observed from a permission alone. |
| 6 | App Residue | Classify files within authorized roots; preview explicit candidates and consent before any operation. |
| 7 | Battery Saver | Label Android BatteryManager observations and estimates separately; hand off settings where applicable. |
| 8 | AI Optimization | Make scoped recommendations from authorized local observations. State what the system allows and what needs user action. |
| 9 | Reports & History | Aggregate persisted observed outcomes with their provenance; no synthetic cleanup sessions. |
| 10 | Room / optional Supabase | Decide what needs persistence and cloud sync after reviewing real requirements; minimize sensitive data, enforce account boundaries and use RLS if cloud is approved. |

The supplied Android and data examples are future references and are not included in the Gradle modules. Owner approval of visual Phase 1 is required before starting any of these.
