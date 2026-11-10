param(
    [Parameter(Mandatory)][ValidatePattern('^[a-zA-Z0-9-]+$')][string]$Serial,
    [Parameter(Mandatory)][ValidatePattern('^[a-z0-9-]+\.png$')][string]$Name,
    [string]$Sdk = 'D:\Android\sdk'
)
$ErrorActionPreference = 'Stop'
$adb = Join-Path $Sdk 'platform-tools\adb.exe'
$directory = Join-Path $PSScriptRoot '..\qa-evidence\ui'
New-Item -ItemType Directory -Path $directory -Force | Out-Null
$target = Join-Path $directory $Name
$process = [Diagnostics.Process]::new()
$process.StartInfo.FileName = $adb
foreach ($argument in @('-s', $Serial, 'exec-out', 'screencap', '-p')) {
    $process.StartInfo.ArgumentList.Add($argument)
}
$process.StartInfo.UseShellExecute = $false
$process.StartInfo.RedirectStandardOutput = $true
$buffer = [IO.MemoryStream]::new()
try {
    if (!$process.Start()) { throw 'Could not start adb.' }
    $process.StandardOutput.BaseStream.CopyTo($buffer)
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) { throw "adb screencap exited $($process.ExitCode)" }
    $bytes = $buffer.ToArray()
    if ($bytes.Length -lt 24 -or [Convert]::ToHexString($bytes, 0, 8) -ne '89504E470D0A1A0A') {
        throw 'adb did not return a PNG.'
    }
    [IO.File]::WriteAllBytes($target, $bytes)
} finally {
    $process.Dispose()
    $buffer.Dispose()
}
Get-FileHash -Algorithm SHA256 -LiteralPath $target
