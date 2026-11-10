# Service adapter acceptance tests — Phase 2 onwards only

Every feature requires green tests, genuine device observation, permission denial scenarios and a reproducible failure report before any VERIFIED claim.

| Service | Positive case | Negative / restricted case | Evidence |
|---|---|---|---|
| Media inventory | Consent granted: actual `MediaStore` rows | permission denied, partial photo library, empty collection | ADB + sample URIs redacted + UI state |
| Duplicate finder | two byte-identical accessible files → SHA-256 group | same-size different contents, inaccessible URI, changed file | hashes kept private + test output |
| Trash | explicit selection + OS trash approval | cancel OS prompt; prove original retained | result intent + re-query + audit journal |
| Storage | `StatFs` counts match shell within explainable unit differences | unavailable mount; cannot inspect app-private data | measured totals and scope label |
| App manager | accessible filtered packages, user-granted usage data | restricted package visibility / denied usage access | screenshot and API logs |
| Privacy | inspect user-authorized signals | other-app AppOps/clipboard not available | unsupported states visible |
| Battery | `BatteryManager` level and charging status | unsupported health/cycles/remaining-time unsupported | source + timestamp, never invented savings |
| Thermal | Android >=29 `PowerManager` thermal state | API 26 and unavailable data | runtime feature guard |
| Cloud sync | logged in + opt-in + valid RLS owner inserts | consent off, missing JWT, user B cross-user access | Supabase logs + two-user tests |
| Reports | real measured scans and actions; exported values reconcile | preview/fixture values must never enter reports | offline reconciliation audit |
