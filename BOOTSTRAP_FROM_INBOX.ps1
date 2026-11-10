# KNOUX AI — Safe, non-overwriting ZIP import into D:\KNOUX-AI
param([string]$Archive = 'D:\KNOUX-AI\INBOX\KNOUX_AI_READY_FOR_D.zip')
$ErrorActionPreference = 'Stop'
$root = 'D:\KNOUX-AI'
if (!(Test-Path -LiteralPath $Archive -PathType Leaf)) { throw "Missing ZIP: $Archive. Download it from the ChatGPT handoff and save it to INBOX." }
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($Archive)
try {
 foreach ($entry in $zip.Entries) {
  if ([string]::IsNullOrEmpty($entry.Name)) { continue }
  $rel = $entry.FullName.Replace('/', '\')
  $target = [System.IO.Path]::GetFullPath([System.IO.Path]::Combine($root, $rel))
  if (!$target.StartsWith($root + '\', [StringComparison]::OrdinalIgnoreCase)) { throw "Unsafe ZIP entry: $rel" }
  $parent = [System.IO.Path]::GetDirectoryName($target)
  [System.IO.Directory]::CreateDirectory($parent) | Out-Null
  if ([System.IO.File]::Exists($target)) {
   $existing = (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash
   $s = $entry.Open()
   try {
    $sha = [Security.Cryptography.SHA256]::Create()
    try { $computed = [Convert]::ToHexString($sha.ComputeHash($s)) } finally { $sha.Dispose() }
   } finally { $s.Dispose() }
   if ($existing -ne $computed) { throw "Preserved differing existing file; no overwrite: $target" }
   continue
  }
  [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $target, $false)
 }
} finally { $zip.Dispose() }
& python "$root\scripts\Verify-Bundle.py"
if ($LASTEXITCODE -ne 0) { throw "Integrity verification failed." }
Write-Output "IMPORT PASS: 10 reference PNGs and archived input checksums verified. Phase 1 files ready on D:."
