param([string]$Serial = $env:KNOUX_TEST_DEVICE_SERIAL)
$adb     = "D:\Android\sdk\platform-tools\adb.exe"
if ([string]::IsNullOrWhiteSpace($Serial)) { throw "Specify -Serial explicitly or set KNOUX_TEST_DEVICE_SERIAL locally; do not hardcode device identifiers." }
$pkg     = "com.knoux.ai"
$runner  = "androidx.test.runner.AndroidJUnitRunner"
$class   = "com.knoux.ai.PreviewNavigationTest"
$method  = "tabsReachAllTenScreensAndDetailBackReturnsToTheIndex"

Write-Host "=== Device state ===" -ForegroundColor Cyan
& $adb -s $Serial get-state

Write-Host "=== Clearing logcat ===" -ForegroundColor Cyan
& $adb -s $Serial logcat -c

Write-Host "=== Running single test ===" -ForegroundColor Cyan
$result = & $adb -s $Serial shell am instrument -w -r `
    -e class "${class}#${method}" `
    "${pkg}.test/${runner}" 2>&1

Write-Host "=== am instrument output ===" -ForegroundColor Cyan
$result | ForEach-Object { Write-Output $_ }

Write-Host "=== Logcat (AndroidRuntime + TestRunner) ===" -ForegroundColor Cyan
& $adb -s $Serial logcat -d -v time AndroidRuntime:E TestRunner:I "*:S" 2>&1

Write-Host "=== Done ===" -ForegroundColor Green
