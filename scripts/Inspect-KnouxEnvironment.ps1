# READ ONLY: Environment, Git, worktree, disk evidence. Does not assume D: is present.
[CmdletBinding()]
param([string]$ProjectRoot = 'D:\KNOUX-AI')
$ErrorActionPreference = 'Stop'
Write-Host ('KNOUX AI audit @ '+(Get-Date -Format o))
foreach ($disk in @('C','D','K')) {
    $drive = Get-PSDrive -Name $disk -ErrorAction SilentlyContinue
    if ($drive) { '{0}: used={1:N2} GiB free={2:N2} GiB' -f $disk, (($drive.Used)/1GB), (($drive.Free)/1GB) }
    else { Write-Host ('Drive '+$disk+': not accessible') }
}
Write-Host ('ProjectRoot: '+$ProjectRoot)
if (-not (Test-Path -LiteralPath $ProjectRoot -PathType Container)) { Write-Warning 'Project folder absent. No data altered.'; exit 0 }
$git = Get-Command git -ErrorAction SilentlyContinue
if (-not $git) { Write-Warning 'Git unavailable'; exit 0 }
Push-Location -LiteralPath $ProjectRoot
try {
    $repoRoot = & git rev-parse --show-toplevel 2>$null
    if ($LASTEXITCODE -ne 0) { Write-Warning 'Not a Git repository'; exit 0 }
    Write-Host ('Git repo: '+$repoRoot)
    foreach($gitArgs in @(@('status','--porcelain=v1','-uall'), @('branch','-avv'), @('worktree','list','--porcelain'), @('stash','list'), @('reflog','-n','15'), @('rev-parse','HEAD'), @('remote','-v'))) {
        Write-Host ('`n> git '+($gitArgs -join ' ')); & git @gitArgs
    }
} finally { Pop-Location }
