# KNOUX identity-gateway QA capture.
#
# One cold start per auth state, 18 s settle. The first Compose frame on the
# reference handset needs ~7-12 s (logcat: "Skipped 297 frames"), so a short wait
# captures a black window.
#
# TalkBack is enabled on the owner's phone. Its touch-exploration focus ring is
# painted over the app and would be mistaken for a UI defect in a screenshot, so
# this script parks the accessibility service for the duration of the run and
# restores the exact previous value in `finally`. It never touches any other
# device setting and never writes the device identifier anywhere.
param(
    [string]$Serial = $env:KNOUX_TEST_DEVICE_SERIAL,
    [string]$Out = 'D:\Temp\opencode\knoux-auth-20261115',
    [int]$SettleSeconds = 18,
    [string]$States = 'welcome,verify,verified,unavailable,otp'
)
$ErrorActionPreference = 'Stop'
$adb = 'D:\Android\sdk\platform-tools\adb.exe'
if (-not $Serial) {
    # Discover the one authorised device instead of storing its identifier in the
    # repository. Fails closed when zero or several devices are attached.
    $found = @(& $adb devices | Select-String -Pattern "`tdevice$" | ForEach-Object { ($_.Line -split "`t")[0] })
    if ($found.Count -ne 1) { throw "Set KNOUX_TEST_DEVICE_SERIAL or connect exactly one device (found $($found.Count))." }
    $Serial = $found[0]
}
New-Item -ItemType Directory -Force -Path $Out | Out-Null

$apk = 'D:\KNOUX-AI\app\build\outputs\apk\debug\app-debug.apk'
& $adb -s $Serial install -r -t $apk
if ($LASTEXITCODE -ne 0) { throw "install failed (exit $LASTEXITCODE)" }

$talkback = (& $adb -s $Serial shell settings get secure enabled_accessibility_services | Out-String).Trim()
$touch = (& $adb -s $Serial shell settings get secure touch_exploration_enabled | Out-String).Trim()

try {
    & $adb -s $Serial shell settings put secure enabled_accessibility_services null | Out-Null
    & $adb -s $Serial shell settings put secure touch_exploration_enabled 0 | Out-Null
    Start-Sleep -Seconds 3

    foreach ($locale in @('en', 'ar')) {
        foreach ($state in ($States -split ',')) {
            $english = if ($locale -eq 'en') { 'true' } else { 'false' }
            & $adb -s $Serial shell am force-stop com.knoux.ai | Out-Null
            # The handset sleeps between cold starts; a sleeping screen captures as a
            # solid black PNG, so wake it and drop the keyguard before every launch.
            & $adb -s $Serial shell input keyevent KEYCODE_WAKEUP | Out-Null
            & $adb -s $Serial shell wm dismiss-keyguard | Out-Null
            & $adb -s $Serial shell am start -n com.knoux.ai/.MainActivity `
                -e knoux.preview.auth $state `
                --ez knoux.preview.english $english `
                --ez knoux.preview.reduceMotion false | Out-Null
            Start-Sleep -Seconds $SettleSeconds
            & $adb -s $Serial shell input keyevent KEYCODE_WAKEUP | Out-Null
            & $adb -s $Serial shell screencap -p "/sdcard/qa-auth-$locale-$state.png" | Out-Null
            & $adb -s $Serial pull "/sdcard/qa-auth-$locale-$state.png" "$Out\$locale-auth-$state.png" | Out-Null
            Write-Output ("{0}-auth-{1}`t{2} bytes" -f $locale, $state, (Get-Item "$Out\$locale-auth-$state.png").Length)
        }
    }
} finally {
    if ($talkback -and $talkback -ne 'null') {
        & $adb -s $Serial shell settings put secure enabled_accessibility_services $talkback | Out-Null
        & $adb -s $Serial shell settings put secure touch_exploration_enabled $touch | Out-Null
    }
}
