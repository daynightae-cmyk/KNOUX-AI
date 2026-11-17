# KNOUX visual QA capture — one cold start per route, 12 s settle.
# The first Compose frame on this handset needs ~7-12 s (measured in logcat:
# "Skipped 297 frames", Davey 5036 ms), so a 3 s wait captures a black window.
param(
    [string]$Serial = $env:KNOUX_TEST_DEVICE_SERIAL,
    [string]$Out = 'D:\Temp\opencode\knoux-visual-20261115',
    [int]$SettleSeconds = 12
)
$ErrorActionPreference = 'Stop'
if (-not $Serial) { throw 'Set KNOUX_TEST_DEVICE_SERIAL or pass -Serial.' }
$adb = 'D:\Android\sdk\platform-tools\adb.exe'
New-Item -ItemType Directory -Force -Path $Out | Out-Null

$apk = 'D:\KNOUX-AI\app\build\outputs\apk\debug\app-debug.apk'
& $adb -s $Serial install -r -t $apk | Out-Null
if ($LASTEXITCODE -ne 0) { throw "install failed (exit $LASTEXITCODE)" }

$routes = @(
    @('home', 'home'),
    @('clean', 'clean'),
    @('clean-center', 'clean-center'),
    @('privacy-risk-inspector', 'privacy'),
    @('deep-scan-summary', 'deep-scan'),
    @('storage-breakdown', 'storage'),
    @('app-manager', 'apps'),
    @('battery-diagnostics', 'battery'),
    @('large-files', 'large-files'),
    @('ai-recommendations', 'ai-recommendations'),
    @('performance-report-detail', 'reports'),
    @('tools', 'tools'),
    @('residue-item-review', 'residue'),
    @('duplicate-group-review', 'duplicates')
)

foreach ($item in $routes) {
    $route = $item[0]; $name = $item[1]
    & $adb -s $Serial shell am force-stop com.knoux.ai | Out-Null
    & $adb -s $Serial shell am start -n com.knoux.ai/.MainActivity `
        -e knoux.preview.route $route --ez knoux.preview.english true --ez knoux.preview.reduceMotion false | Out-Null
    Start-Sleep -Seconds $SettleSeconds
    & $adb -s $Serial shell screencap -p "/sdcard/qa-$name.png" | Out-Null
    & $adb -s $Serial pull "/sdcard/qa-$name.png" "$Out\en-$name.png" | Out-Null
    Write-Output ("en-{0}`t{1}" -f $name, (Get-Item "$Out\en-$name.png").Length)
}

foreach ($item in @(@('home', 'home'), @('clean', 'clean'))) {
    $route = $item[0]; $name = $item[1]
    & $adb -s $Serial shell am force-stop com.knoux.ai | Out-Null
    & $adb -s $Serial shell am start -n com.knoux.ai/.MainActivity `
        -e knoux.preview.route $route --ez knoux.preview.english false --ez knoux.preview.reduceMotion false | Out-Null
    Start-Sleep -Seconds $SettleSeconds
    & $adb -s $Serial shell screencap -p "/sdcard/qa-ar-$name.png" | Out-Null
    & $adb -s $Serial pull "/sdcard/qa-ar-$name.png" "$Out\ar-$name.png" | Out-Null
    Write-Output ("ar-{0}`t{1}" -f $name, (Get-Item "$Out\ar-$name.png").Length)
}
