param([string]$Archive = "D:\KNOUX-AI\INBOX\KNOUX_AI_READY_FOR_D.zip")
$ErrorActionPreference='Stop'
$dest='D:\KNOUX-AI'
if (!(Test-Path -LiteralPath $Archive -PathType Leaf)) {throw "Archive not found: $Archive. Save the ZIP to INBOX first."}
if (!(Test-Path -LiteralPath $dest -PathType Container)) {throw "Project folder not found: $dest"}
Add-Type -AssemblyName System.IO.Compression
$zip=[System.IO.Compression.ZipFile]::OpenRead($Archive)
try {
  foreach($entry in $zip.Entries) {
    $rel=$entry.FullName.Replace('/','\')
    if ([string]::IsNullOrWhiteSpace($rel) -or $rel.EndsWith('\')) {continue}
    if ($rel -match '(^|\\)\.\.($|\\)' -or [System.IO.Path]::IsPathRooted($rel)) {throw "Unsafe archive path: $rel"}
    $target=[System.IO.Path]::GetFullPath([System.IO.Path]::Combine($dest,$rel))
    if (!$target.StartsWith($dest+'\',[System.StringComparison]::OrdinalIgnoreCase)){throw "Path escaped root: $rel"}
    $parent=[System.IO.Path]::GetDirectoryName($target)
    [System.IO.Directory]::CreateDirectory($parent)|Out-Null
    if ([System.IO.File]::Exists($target)) {
      $stream=$entry.Open();$ms=New-Object System.IO.MemoryStream
      try {$stream.CopyTo($ms);$raw=$ms.ToArray()}finally{$stream.Dispose();$ms.Dispose()}
      $old=[System.IO.File]::ReadAllBytes($target)
      if (![System.Linq.Enumerable]::SequenceEqual([byte[]]$old,[byte[]]$raw)){throw "Existing DIFFERENT file, preserved without overwrite: $target"}
      continue
    }
    [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry,$target,$false)
  }
} finally {$zip.Dispose()}
$imgs=@(Get-ChildItem -LiteralPath "$dest\design\reference" -File -Filter '*.png')
if ($imgs.Count -ne 10){throw "Expected 10 PNG references; found $($imgs.Count)"}
foreach($i in $imgs){$h=[System.IO.File]::ReadAllBytes($i.FullName);if ($h.Length -lt 24 -or $h[0] -ne 137 -or $h[1] -ne 80 -or $h[2] -ne 78 -or $h[3] -ne 71){throw "Invalid PNG signature $($i.Name)"}}
& python "$dest\scripts\Verify-Bundle.py"
if($LASTEXITCODE -ne 0){throw 'Verification failed'}
Write-Output "IMPORTED & VERIFIED: $dest | 10 PNGs | manifest verified. Ready for Codex UI Phase 1."
