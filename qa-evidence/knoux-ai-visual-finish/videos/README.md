# videos/ — runtime captures (final build)

| File | What it proves | Notes |
|---|---|---|
| `walkthrough-clean-preview.mp4` | Full interaction: Home → Clean Center → "Review before clean" → **Preview Items** sheet → Close → primary "Clean selected" → **Clean Selected** sheet → **Confirm preview** → **Preview finished — No action performed — UI preview only.** → Close → back → tab-home to Home | 540×960 screenrecord, 49.9 s (recording ran to its 50 s cap; interaction occupies the first ~45 s). No audio track (Android screenrecord). Sheet buttons are located by visible text: the Material3 `ModalBottomSheet` popup window does not expose Compose debug resource-ids (main-window tags do) |
| `../splash/cold-launch.mp4` | Cold launch splash: bloom → K build → wordmark write-on → smile → tip flare → fade to Home | 540×960, 14.35 s; click verified exactly once via logcat (`../splash/click-events.log`); no audio track on this emulator |
| `walkthrough-frame-1-home.png` | Home before the flow (all six service cards visible) | frame @ ~10 s |
| `walkthrough-frame-2-clean-center-preview-sheet.png` | Clean Center with the **Preview Items** sheet open | frame @ ~22 s |
| `walkthrough-frame-3-clean-selected-confirm.png` | **Clean Selected** sheet with the Confirm preview CTA | frame @ ~36.5 s |
| `walkthrough-frame-4-preview-finished-noop.png` | **Preview finished — No action performed** result sheet (honest no-op) | frame @ ~38.5 s |
| `walkthrough-frame-5-back-home.png` | Back on Home after back navigation | frame @ ~44 s |

SwiftShader caveat: motion smoothness is not claimed; time-based animation can step
between rendered frames on this software-rendered emulator. Real-device capture is the
recommended next check (see `../HANDOFF_TO_PERSONALIZATION.md`).
