param([string]$Serial='emulator-5554')
$ErrorActionPreference='Stop'
$adb='D:\Android\sdk\platform-tools\adb.exe'
$out='D:\KNOUX-AI\qa-evidence\knoux-ai-visual-finish\videos'
New-Item -ItemType Directory -Force -Path $out | Out-Null

function Get-Tree {
    $tree=(& $adb -s $Serial exec-out uiautomator dump /dev/tty 2>$null | Out-String)
    $start=$tree.IndexOf('<?xml'); $end=$tree.LastIndexOf('>')
    if($start -lt 0 -or $end -le $start){ throw 'No UI hierarchy available' }
    [xml]$doc=$tree.Substring($start,$end-$start+1)
    return $doc
}

function Get-Node([string]$tag) {
    $doc=Get-Tree
    return $doc.SelectSingleNode("//node[@resource-id='$tag']")
}

function Get-TextNode([string]$text) {
    # Sheet buttons render in a Material3 popup window where debug resource-ids
    # are not exposed, so match the visible button text instead.
    $doc=Get-Tree
    return $doc.SelectSingleNode("//node[@text='$text']")
}

function Tap([string]$tag) {
    $node=$null
    foreach($i in 1..3){ $node=Get-Node $tag; if($null -ne $node){ break }; Start-Sleep -Milliseconds 900 }
    if($null -eq $node){ throw "Missing UI target $tag" }
    if($node.bounds -notmatch '\[(\d+),(\d+)\]\[(\d+),(\d+)\]'){ throw "Bad bounds for $tag" }
    $x=([int]$Matches[1]+[int]$Matches[3]) -shr 1
    $y=([int]$Matches[2]+[int]$Matches[4]) -shr 1
    & $adb -s $Serial shell input tap $x $y
    if($LASTEXITCODE -ne 0){ throw "Tap failed for $tag" }
    Write-Output "tapped $tag at $x,$y"
    Start-Sleep -Milliseconds 1400
}

function TapText([string]$text) {
    $node=$null
    foreach($i in 1..4){ $node=Get-TextNode $text; if($null -ne $node){ break }; Start-Sleep -Milliseconds 900 }
    if($null -eq $node){ throw "Missing UI text target $text" }
    if($node.bounds -notmatch '\[(\d+),(\d+)\]\[(\d+),(\d+)\]'){ throw "Bad bounds for $text" }
    $x=([int]$Matches[1]+[int]$Matches[3]) -shr 1
    $y=([int]$Matches[2]+[int]$Matches[4]) -shr 1
    & $adb -s $Serial shell input tap $x $y
    if($LASTEXITCODE -ne 0){ throw "Tap failed for $text" }
    Write-Output "tapped text '$text' at $x,$y"
    Start-Sleep -Milliseconds 1400
}

function KeyBack {
    & $adb -s $Serial shell input keyevent 4
    Start-Sleep -Milliseconds 1400
}

& $adb -s $Serial shell wm size 1080x2340 | Out-Null
& $adb -s $Serial shell settings put global animator_duration_scale 1
& $adb -s $Serial shell am force-stop com.knoux.ai
Start-Sleep -Milliseconds 600
& $adb -s $Serial shell am start -W --activity-single-top -n com.knoux.ai/.MainActivity --es knoux.preview.route home | Out-Null
Start-Sleep -Milliseconds 1600

$remote='/data/local/tmp/knoux-walkthrough-' + [guid]::NewGuid().ToString('N') + '.mp4'
$proc=[Diagnostics.Process]::new(); $proc.StartInfo.FileName=$adb
foreach($a in @('-s',$Serial,'shell','screenrecord','--size','540x960','--time-limit','50',$remote)){ $proc.StartInfo.ArgumentList.Add($a) }
$proc.StartInfo.UseShellExecute=$false
if(!$proc.Start()){ throw 'screenrecord did not start' }
Start-Sleep -Milliseconds 700
try {
    Tap 'open-clean-center'
    Tap 'review-action'
    TapText 'Close'
    Tap 'primary-action'
    TapText 'Confirm preview'
    TapText 'Close'
    KeyBack
    Tap 'tab-home'
} finally {
    if(!$proc.WaitForExit(60000)){ throw 'screenrecord timeout' }
}
$recordExit=$proc.ExitCode; $proc.Dispose()
"screenrecord exit $recordExit"
if($recordExit -eq 0){
    & $adb -s $Serial pull $remote (Join-Path $out 'walkthrough-clean-preview.mp4') | Out-Null
    if($LASTEXITCODE -ne 0){ throw 'pull failed' }
    Get-Item (Join-Path $out 'walkthrough-clean-preview.mp4') | Select-Object FullName,Length,LastWriteTime
}
