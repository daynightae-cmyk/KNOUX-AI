param(
    [Parameter(Mandatory)][ValidatePattern('^[a-zA-Z0-9-]+$')][string]$Serial,
    [Parameter(Mandatory)][ValidatePattern('^[a-z0-9-]+\.png$')][string]$Name,
    [string]$Sdk = 'D:\Android\sdk',
    [string]$Destination = 'D:\KNOUX-AI\qa-evidence\ui'
)
$ErrorActionPreference = 'Stop'
$adb = Join-Path $Sdk 'platform-tools\adb.exe'
New-Item -ItemType Directory -Path $Destination -Force | Out-Null
$target = Join-Path $Destination $Name
$devicePath = "/sdcard/knoux_capture.png"

# screencap to sdcard (safe binary on device), then pull to host
& $adb -s $Serial shell screencap -p $devicePath
if ($LASTEXITCODE -ne 0) { throw "screencap failed (exit $LASTEXITCODE)" }

& $adb -s $Serial pull $devicePath $target
if ($LASTEXITCODE -ne 0) { throw "adb pull failed (exit $LASTEXITCODE)" }

# Clean up device-side temp file
& $adb -s $Serial shell rm -f $devicePath | Out-Null

# Verify PNG magic bytes
$bytes = [IO.File]::ReadAllBytes($target)
if ($bytes.Length -lt 24) { throw "Capture too small: $($bytes.Length) bytes" }
$magic = ([BitConverter]::ToString($bytes, 0, 8)).Replace('-','')
if ($magic -ne '89504E470D0A1A0A') { throw "Not a PNG (magic=$magic)" }

Write-Host "Captured: $Name ($($bytes.Length) bytes)"
Get-FileHash -Algorithm SHA256 -LiteralPath $target
