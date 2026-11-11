param(
    [ValidatePattern('^emulator-[0-9]+$')][string]$Serial = 'emulator-5554',
    [ValidatePattern('^[0-9]+x[0-9]+$')][string]$Viewport = '1080x2340',
    [string]$Sdk = 'D:\Android\sdk',
    [switch]$Scrolled,
    [switch]$Interior,
    [string]$Destination = (Join-Path $PSScriptRoot '..\qa-evidence\ui')
)
$ErrorActionPreference = 'Stop'
$adb = Join-Path $Sdk 'platform-tools\adb.exe'
if ((& $adb -s $Serial get-state) -ne 'device') { throw "No connected emulator at $Serial" }
$app = 'com.knoux.ai'
$before = (& $adb -s $Serial shell wm size | Out-String)
if ($LASTEXITCODE -ne 0) { throw 'Cannot read original viewport.' }
$previous = if ($before -match 'Override size:\s*(\d+x\d+)') { $Matches[1] } else { 'reset' }
$routes = if ($Interior) {
    'clean-center','duplicate-group-review','privacy-risk-inspector','deep-scan-summary',
    'performance-report-detail','residue-item-review','installed-apps-review','storage-breakdown',
    'battery-diagnostics','ai-recommendations'
} else {
    'home','privacy','duplicates','deep-scan','reports','residue','app-manager','storage','battery','ai-optimization'
}
if ($Interior -and $Scrolled) { throw 'Use -Interior for the ten entry-state captures.' }
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
        for ($attempt=0; $attempt -lt 5; $attempt++) {
            Start-Sleep -Milliseconds 1000
            $tree = & $adb -s $Serial exec-out uiautomator dump /dev/tty 2>$null | Out-String
            if ($LASTEXITCODE -eq 0 -and $tree.Contains($expected)) { $ready = $true; break }
        }
        if (!$ready) { throw "The active screen did not expose $expected; refusing to record a stale frame." }
        Write-Output "UI hierarchy verified: screen-$route"
        if ($Scrolled) {
            $scrollBounds = 'scrollable="true"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
            if ($tree -notmatch $scrollBounds) { throw "Could not read the scroll area for $route" }
            $middle = ([int]$Matches[1] + [int]$Matches[3]) -shr 1
            $top = [int]$Matches[2]
            $height = [int]$Matches[4] - $top
            $start = $top + [int]($height * .82)
            $end = $top + [int]($height * .32)
            $tail = if ($route -eq 'storage') { 'open-category-5' } else { 'row-4' }
            # The software-rendered emulator needs a beat to settle the fling before
            # uiautomator sees the new hierarchy; retry the same swipe when it races.
            $details = ''
            foreach ($attempt in 1..3) {
                & $adb -s $Serial shell input swipe $middle $start $middle $end 450
                if ($LASTEXITCODE -ne 0) { throw "Could not scroll $route" }
                Start-Sleep -Milliseconds 900
                $details = & $adb -s $Serial exec-out uiautomator dump /dev/tty 2>$null | Out-String
                if ($details.Contains('resource-id="' + $tail + '"')) { break }
                if ($attempt -lt 3) { Start-Sleep -Milliseconds 700 }
            }
            if (!$details.Contains('resource-id="' + $tail + '"')) { throw "Last item $tail did not become visible on $route" }
            Write-Output "Last item verified after scroll: $tail"
        }
        Start-Sleep -Milliseconds 700
        $resumed = & $adb -s $Serial shell dumpsys activity activities | Select-String 'mResumedActivity'
        if ("$resumed" -notmatch [regex]::Escape("$app/.MainActivity")) { throw "KNOUX AI not foregrounded for $route" }
        $captureScript = Join-Path (Split-Path $MyInvocation.MyCommand.Path -Parent) 'Capture-Preview.ps1'
        & $captureScript -Serial $Serial -Name $name -Sdk $Sdk -Destination $Destination
    }
} finally {
    & $adb -s $Serial shell wm size $previous
    & $adb -s $Serial shell am start -W --activity-single-top -n "$app/.MainActivity" --es knoux.preview.route home
}
