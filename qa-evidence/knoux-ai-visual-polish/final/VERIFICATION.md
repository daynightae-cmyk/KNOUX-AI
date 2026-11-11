# KNOUX AI visual polish

## Reality

- Windows workspace `D:\KNOUX-AI` on `main` at starting commit `b87a3297efdebe0f6ea85af9fc70a20dd4fccfae`. No Git remote was configured.
- The twenty existing outer and interior screens and their original [runtime comparisons](../../knoux-ai-visual-finish/VISUAL_COMPARISONS/INDEX.md) are in the baseline. The latest agent left four modified Kotlin files, the new `PremiumArt.kt`, reference imports, and working captures.

## Implemented

- Finished the shared Compose hero, icon, glass, CTA, and selected-tab treatments. Fixed the closed ribbon geometry, globe lighting, icon coordinate alignment, centered donut shading, and the button sizing that could crowd sticky actions.
- The splash draws the three K planes, brand name, and smooth magenta-to-cyan smile in sequence. Light ribbons and orbit accents move during entry, followed by a flare and the existing one-shot click.
- Hero drift and CTA shimmer follow the app's motion gate. The built-in route names, UI previews, and prior evidence are preserved.

## Verification

| Check | Evidence |
| --- | --- |
| `gradlew.bat :app:assembleDebug --offline --console=plain` | Exit 0 in 57 seconds. [Build log](build.log) |
| `adb -s emulator-5554 install -r <APK>` | Success on `kx_preview`, Android SDK built for x86_64, Android 8.0 / API 26, SwiftShader. Physical 1080×1920, logical 1080×2340, 420 dpi. |
| `adb shell am start -W -S -n com.knoux.ai/.MainActivity` | Cold launch reported 1,916 ms total time. The real capture reached Home. [Video](splash/cold-launch.mp4) |
| `adb logcat --pid=<pid> -s KnouxSplash:D` | One `Smile click started` event. [Runtime log](splash.log) |
| `adb exec-out uiautomator dump /dev/tty` and `scripts/Capture-Preview.ps1` | [Home](after/home.png), [Smart Clean Center](after/clean-center.png), [Installed Apps Review](after/installed-apps-review.png), and [Storage Breakdown](after/storage-breakdown.png) verified. A temporary ADB transport error recovered with `adb start-server`. [Route log](layout.log) |
| `git diff --cached --check` | Exit 0 before delivery. |

The APK is 13,269,818 bytes. [SHA-256](APK.sha256)

Free space on D: went from 3,641,155,584 to 3,566,911,488 bytes across build and capture before staging the delivery evidence, a decrease of 74,244,096 bytes. No cleanup was performed.

## Owner review

[Home comparison](../comparisons/home.png) · [Smart Clean Center comparison](../comparisons/clean-center.png) · [Splash comparison](../comparisons/splash.png) · [Splash frames](../comparisons/splash-sequence.png)

Visual parity is partial. The reference has stronger and finer localized highlights, denser artwork, and several tighter content layouts. The Clean Center score wraps the unit on this device. The screen recording has 52 encoded frames over 13.92 seconds on the software-rendered emulator. A physical-device motion and acoustic check and a full suite rerun on this APK remain unverified. Phase 1 stays in design preview pending owner review.
