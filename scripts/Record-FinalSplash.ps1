param([Parameter(Mandatory)][string]$Qa)
$ErrorActionPreference = 'Stop'
$adb = 'D:\Android\sdk\platform-tools\adb.exe'
$serial = 'emulator-5554'
$app = 'com.knoux.ai'
$dest = Join-Path $Qa 'splash'
$remoteVideo = '/data/local/tmp/knoux-phase1-' + [guid]::NewGuid().ToString('N') + '.mp4'
$proc = [Diagnostics.Process]::new()
$proc.StartInfo.FileName = $adb
foreach ($arg in @('-s',$serial,'shell','screenrecord','--size','540x960','--time-limit','14',$remoteVideo)) {
    $proc.StartInfo.ArgumentList.Add($arg)
}
$proc.StartInfo.UseShellExecute = $false
try {
    if (!$proc.Start()) { throw 'Could not start screenrecord.' }
    Start-Sleep -Milliseconds 700
    & $adb -s $serial shell am start -W -S -n "$app/.MainActivity"
    if ($LASTEXITCODE -ne 0) { throw 'Cold launch failed.' }
    $appPid = (& $adb -s $serial shell pidof $app).Trim()
    if (!$proc.WaitForExit(20000)) { throw 'screenrecord timed out.' }
    $recordExit = $proc.ExitCode
} finally {
    $proc.Dispose()
}
if ($recordExit -eq 0) {
    & $adb -s $serial pull $remoteVideo (Join-Path $dest 'cold-launch.mp4')
    if ($LASTEXITCODE -ne 0) { throw 'Could not pull the emulator recording.' }
} else {
    Write-Output "screenrecord exit $recordExit"
}
$events = & $adb -s $serial logcat -d -v threadtime --pid=$appPid -s 'KnouxSplash:D' '*:S'
$events | Write-Output
$clicks = @($events | Where-Object { $_ -match 'Smile click started' }).Count
Write-Output "Cold-launch SoundPool starts: $clicks"
& 'D:\KNOUX-AI\scripts\Capture-Preview.ps1' -Serial $serial -Name 'cold-launch-home.png' -Destination $dest
foreach ($ms in 480,835,1140,1610,2280,2460,2710,2800,3000) {
    $start = & $adb -s $serial shell am start -W -S -n "$app/.MainActivity" --el knoux.preview.splash.time $ms
    if ($LASTEXITCODE -ne 0 -or "$start" -notmatch 'Status: ok') { throw "Freeze frame launch failed at $ms ms" }
    $start | Write-Output
    Start-Sleep -Milliseconds 1500
    & 'D:\KNOUX-AI\scripts\Capture-Preview.ps1' -Serial $serial -Name "splash-$ms.png" -Destination $dest
}
& $adb -s $serial shell am start -W -S -n "$app/.MainActivity" --es knoux.preview.route home --ez knoux.preview.still true

