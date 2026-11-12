param(
    [string]$Serial = "1334825549012211",
    [string]$Apk = "D:\KNOUX-AI\app\build\outputs\apk\debug\app-debug.apk",
    [string]$Out = "D:\KNOUX-AI\qa-evidence\final-visual-gate\runtime"
)

$ErrorActionPreference = "Stop"
$adb = "D:\Android\sdk\platform-tools\adb.exe"
New-Item -ItemType Directory -Force -Path $Out | Out-Null

Write-Host "== device =="
& $adb -s $Serial shell wm size
& $adb -s $Serial shell wm density
& $adb -s $Serial shell getprop ro.build.version.sdk

Write-Host "== install =="
& $adb -s $Serial install -r $Apk

# Keep the panel awake and past the keyguard so captures show the app, not the lock screen.
& $adb -s $Serial shell svc power stayon true | Out-Null
& $adb -s $Serial shell input keyevent 224 | Out-Null
& $adb -s $Serial shell wm dismiss-keyguard | Out-Null
# Force portrait: the references are portrait phone screens, and a rotated panel
# would silently invalidate every comparison.
& $adb -s $Serial shell settings put system accelerometer_rotation 0 | Out-Null
& $adb -s $Serial shell settings put system user_rotation 0 | Out-Null
Start-Sleep -Seconds 2

function Capture([string]$name, [string[]]$extras) {
    $args = @("-s", $Serial, "shell", "am", "start", "-S", "-f", "0x10000000", "-n", "com.knoux.ai/.MainActivity") + $extras
    & $adb @args | Out-Null
    Start-Sleep -Seconds 4
    & $adb -s $Serial shell screencap -p /sdcard/knoux_cap.png | Out-Null
    & $adb -s $Serial pull /sdcard/knoux_cap.png (Join-Path $Out "$name.png") | Out-Null
    & $adb -s $Serial shell rm /sdcard/knoux_cap.png | Out-Null
    & $adb -s $Serial shell settings put global stay_on_while_plugged_in 3 | Out-Null
    Write-Host "captured $name"
}

# Onboarding chapters, Arabic (references 01, 02, 03)
Capture "VG02_CH01_AR" @("--es", "knoux.preview.onboarding", "0", "--ez", "knoux.preview.arabic", "true")
Capture "VG02_CH02_AR" @("--es", "knoux.preview.onboarding", "1", "--ez", "knoux.preview.arabic", "true")
Capture "VG02_CH03_AR" @("--es", "knoux.preview.onboarding", "2", "--ez", "knoux.preview.arabic", "true")

# Onboarding chapter 1, English (reference 04 is a locale equivalent, not a chapter)
Capture "VG02_CH01_EN" @("--es", "knoux.preview.onboarding", "0", "--ez", "knoux.preview.english", "true")

# Login primary (reference 08)
Capture "VG03_LOGIN_EN" @("--es", "knoux.preview.auth", "welcome", "--ez", "knoux.preview.english", "true")
Capture "VG03_LOGIN_AR" @("--es", "knoux.preview.auth", "welcome", "--ez", "knoux.preview.arabic", "true")

# OTP primary empty state (reference 12)
Capture "VG04_OTP_EMPTY_EN" @("--es", "knoux.preview.auth", "otp", "--ez", "knoux.preview.english", "true")
Capture "VG04_OTP_EMPTY_AR" @("--es", "knoux.preview.auth", "otp", "--ez", "knoux.preview.arabic", "true")

Write-Host "== done =="
Get-ChildItem $Out -Filter "*.png" | Select-Object Name, Length
