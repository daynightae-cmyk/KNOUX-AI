# Phase 1 UI components and asset sources

- The screens use native Compose text, semantic controls, scroll, a Navigation Compose graph, and a persistent four-tab bar. Android draws the actual status and navigation bars.
- `NeonArt.kt` defines original Canvas geometry for all ten heroes, the K mark, glyph paths, particle waves, rings and gradient layers. Decorative motion runs only on the foreground screen while motion is allowed. The APK does not contain the marketing PNGs.
- `KnouxComponents.kt` owns glass surfaces, metric panels, donut/gradient bars and native control semantics. `ReferenceScreen.kt` renders tool-specific panel/row variations, charts and preview decisions. `HomeScreen.kt` owns the Home dashboard.
- Display Latin text uses Outfit ExtraBold, a static instance (weight 800) of the [Outfit variable font in Google Fonts](https://github.com/google/fonts/tree/main/ofl/outfit), under [OFL 1.1](../licenses/Outfit-OFL.txt). Body text and Arabic display use Android system font families. The exact reference typeface is unidentified.
- Values are fixture data and are disclosed in the fixed header. Pause/resume affects decorative radar motion; row selection and confirmation are local preview state. No feature reads or mutates device data.
