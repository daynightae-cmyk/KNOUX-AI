# KNOUX AI — Android design preview

Kotlin and Jetpack Compose app for the ten KNOUX AI design references. The app's fixed **Design preview** label identifies the figures as sample data. Review, selection and confirmation work inside the UI; they do not scan, clean or modify device data.

From PowerShell in `D:\KNOUX-AI`, using an existing SDK and JDK (17+):

```powershell
$env:ANDROID_HOME = 'D:\Android\sdk'
$env:GRADLE_USER_HOME = 'D:\.gradle'  # existing machine cache
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
& "$env:ANDROID_HOME\platform-tools\adb.exe" devices -l
# Substitute the serial shown for your Android emulator.
& "$env:ANDROID_HOME\platform-tools\adb.exe" -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk
& "$env:ANDROID_HOME\platform-tools\adb.exe" -s emulator-5554 shell am start -W -n com.knoux.ai/.MainActivity
.\gradlew.bat :app:connectedDebugAndroidTest
```

Home opens the sample dashboard. Clean and Tools expose all related screens; Reports opens the report preview. The settings sheet switches English/Arabic and toggles decorative motion. In a debug build, the Home menu also lists all ten reference routes.

For Android screenshot capture use `.\scripts\Capture-Phase1.ps1 -Serial emulator-5554`. It requires the installed debug build, temporarily sets the emulator viewport, captures real Android frames, then restores the previous viewport and Home. Comparisons and limitations are recorded in `PHASE_1_HANDOFF.md`.

The verified kit lives alongside the app. Use `python scripts\Verify-Bundle.py` to check its original assets and input manifest. Materials under `android/integration`, `database` and `prompts/phase2` are future references.
