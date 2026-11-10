param(
    [ValidatePattern('^emulator-[0-9]+$')][string]$Serial = 'emulator-5554',
    [ValidatePattern('^[0-9]+x[0-9]+$')][string]$Viewport = '1080x2340',
    [string]$Sdk = 'D:\Android\sdk',
    [switch]$Scrolled
)
$ErrorActionPreference = 'Stop'
$adb = Join-Path $Sdk 'platform-tools\adb.exe'
if ((& $adb -s $Serial get-state) -ne 'device') { throw "No connected emulator at $Serial" }
$app = 'com.knoux.ai'
$before = (& $adb -s $Serial shell wm size | Out-String)
if ($LASTEXITCODE -ne 0) { throw 'Cannot read original viewport.' }
$previous = if ($before -match 'Override size:\s*(\d+x\d+)') { $Matches[1] } else { 'reset' }
$routes = 'home','privacy','duplicates','deep-scan','reports','residue','app-manager','storage','battery','ai-optimization'
try {
    & $adb -s $Serial shell wm size $Viewport
    if ($LASTEXITCODE -ne 0) { throw 'Cannot set capture viewport.' }
    & $adb -s $Serial shell wm size
    & $adb -s $Serial shell wm density
    for ($i=0; $i -lt $routes.Length; $i++) {
        if ($Scrolled -and $i -eq 0) { continue }
        $route = $routes[$i]
        $suffix = if ($Scrolled) { '-scrolled' } else { '' }
        $name = '{0:d2}-{1}{2}-runtime.png' -f ($i + 1), $route, $suffix
        $result = & $adb -s $Serial shell am start -W --activity-single-top -n "$app/.MainActivity" --es knoux.preview.route $route --ez knoux.preview.still true
        $code = $LASTEXITCODE
        Write-Output "${route}:"
        $result | Write-Output
        if ($code -ne 0 -or ($result -join "\n") -notmatch 'Status: ok') { throw "Android did not start $route (exit $code)" }
        $expected = 'resource-id="screen-' + $route + '"'
        $ready = $false
        for ($attempt=0; $attempt -lt 3; $attempt++) {
            Start-Sleep -Milliseconds 1000
            $tree = & $adb -s $Serial exec-out uiautomator dump /dev/tty 2>$null | Out-String
            if ($tree.Contains($expected)) { $ready = $true; break }
        }
        if (!$ready) { throw "The active screen did not expose $expected; refusing to record a stale frame." }
        Write-Output "UI hierarchy verified: screen-$route"
        if ($Scrolled) {
            $dims = $Viewport.Split('x')
            $middle = [int]([int]$dims[0] * 0.50)
            $start = [int]([int]$dims[1] * 0.69)
            $end = [int]([int]$dims[1] * 0.38)
            & $adb -s $Serial shell input swipe $middle $start $middle $end 450
            if ($LASTEXITCODE -ne 0) { throw "Could not scroll $route" }
            $tail = if ($route -eq 'storage') { 'open-category-5' } else { 'row-4' }
            $details = & $adb -s $Serial exec-out uiautomator dump /dev/tty 2>$null | Out-String
            if (!$details.Contains('resource-id="' + $tail + '"')) { throw "Last item $tail did not become visible on $route" }
            Write-Output "Last item verified after scroll: $tail"
        }
        Start-Sleep -Milliseconds 700
        $resumed = & $adb -s $Serial shell dumpsys activity activities | Select-String 'mResumedActivity'
        if ("$resumed" -notmatch [regex]::Escape("$app/.MainActivity")) { throw "KNOUX AI not foregrounded for $route" }
        & (Join-Path $PSScriptRoot 'Capture-Preview.ps1') -Serial $Serial -Name $name -Sdk $Sdk
    }
} finally {
    & $adb -s $Serial shell wm size $previous
    & $adb -s $Serial shell am start -W --activity-single-top -n "$app/.MainActivity" --es knoux.preview.route home
}
