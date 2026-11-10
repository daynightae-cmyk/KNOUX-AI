# READ ONLY: only names likely to be reproducible build outputs, never delete automatically.
[CmdletBinding()]
param([string]$ProjectRoot = 'D:\KNOUX-AI', [string]$Report = '')
$ErrorActionPreference = 'Stop'
$resolved = Resolve-Path -LiteralPath $ProjectRoot -ErrorAction Stop
$root = $resolved.ProviderPath.TrimEnd('\')
if ($root -match '^[A-Za-z]:$' -or $root.Length -lt 8) { throw 'Refusing to scan drive root.' }
$projectMarker = Join-Path $root 'settings.gradle.kts'
if (-not (Test-Path $projectMarker) -and -not (Test-Path (Join-Path $root 'settings.gradle'))) { throw 'No Android project marker: audit only after confirmed project creation.' }
Write-Host 'Inventory only, NO DELETIONS.'
$candidates = @()
Get-ChildItem -LiteralPath $root -Force -Directory -Recurse -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -in @('build','.gradle') -and $_.FullName.StartsWith($root+'\',[StringComparison]::OrdinalIgnoreCase) } |
    ForEach-Object {
       $folder=$_.FullName
       $size=(Get-ChildItem -LiteralPath $folder -File -Recurse -Force -ErrorAction SilentlyContinue | Measure-Object -Property Length -Sum).Sum
       $candidates += [pscustomobject]@{Path=$folder;Bytes=[long]$size;Disposition='REVIEW ONLY: check active tasks, Git uniqueness, generated-only contents'}
    }
$candidates | Sort-Object -Property Bytes -Descending | Format-Table -AutoSize
if ($Report) { $candidates | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $Report -Encoding utf8 }
Write-Warning 'No automatic removal. Project build caches are not verified disposable until processes / unique-work / regeneration checks pass.'
