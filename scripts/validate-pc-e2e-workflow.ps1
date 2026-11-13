param([string]$Repo='D:\KNOUX-AI')
$ErrorActionPreference='Stop'
function Assert-Truth([bool]$Condition,[string]$Why){if(-not $Condition){throw $Why}}
try {
  Assert-Truth (Test-Path -LiteralPath $Repo) 'REPOSITORY_NOT_FOUND'
  $m=Join-Path $Repo 'docs\workflows\PC_E2E_TASK_MANIFEST_20261112.json'
  $c=Join-Path $Repo 'docs\pro\PC_PRO_FEATURE_CATALOG_20261112.json'
  $doc=Join-Path $Repo 'docs\workflows\KNOUX_PC_AUTONOMOUS_E2E_WORKFLOW_20261112.md'
  $o=Join-Path $Repo 'docs\pro\PC_PRO_OWNER_FULL_FUNCTIONALITY_OVERRIDE_20261112.md'
  $g=Join-Path $Repo 'AGENTS.md'
  foreach($p in @($m,$c,$doc,$o,$g)){Assert-Truth (Test-Path -LiteralPath $p) ('MISSING_FILE_'+$p)}
  $manifest=Get-Content -LiteralPath $m -Raw -Encoding UTF8|ConvertFrom-Json
  $cat=Get-Content -LiteralPath $c -Raw -Encoding UTF8|ConvertFrom-Json
  $tasks=@($manifest.tasks);$ids=@($tasks|ForEach-Object {$_.task_id})
  Assert-Truth ($tasks.Count -eq 14 -and @($ids|Sort-Object -Unique).Count -eq 14) 'INVALID_TASK_GRAPH_COUNT'
  foreach($task in $tasks){foreach($d in @($task.depends_on)){Assert-Truth ($ids -contains $d) ('MISSING_DEPENDENCY_'+$task.task_id+'_'+$d)}}
  Assert-Truth ($ids -contains $manifest.current_task_id) 'INVALID_CURRENT_TASK'
  Assert-Truth (@($cat.sections).Count -eq 10) 'INVALID_SECTION_COUNT'
  $features=@($cat.sections|ForEach-Object {$_.features})
  $featureIDs=@($features|ForEach-Object {$_.feature_id})
  Assert-Truth ($features.Count -eq 50 -and @($featureIDs|Sort-Object -Unique).Count -eq 50) 'INVALID_FEATURE_CATALOG'
  Assert-Truth ((Get-Content -LiteralPath $g -Raw -Encoding UTF8).Contains('LATEST OWNER OVERRIDE')) 'OUTDATED_AGENT_POLICY'
  Assert-Truth ((Get-Content -LiteralPath $o -Raw -Encoding UTF8).Contains('Smart Clean')) 'MISSING_OWNER_FUNCTIONALITY_OVERRIDE'
  $head=(git -C $Repo rev-parse HEAD 2>&1).Trim()
  Assert-Truth ($LASTEXITCODE -eq 0) 'GIT_HEAD_NOT_READABLE'
  $branch=(git -C $Repo branch --show-current 2>&1).Trim()
  $status=@(git -C $Repo status --short --branch 2>&1)
  $state=[ordered]@{workflow_contract='VALID';repo=$Repo;head=$head;branch=$branch;tasks=$tasks.Count;catalog_sections=@($cat.sections).Count;catalog_features=$features.Count;next_phase=$manifest.current_task_id;services_verified_by_this_script=0;runtime_test='NOT_RUN';source_modified_by_script=$false;working_tree_rows=$status.Count}
  $state|ConvertTo-Json -Depth 6
  exit 0
}catch{Write-Error ('WORKFLOW_VALIDATION_FAILED: '+$_.Exception.Message);exit 1}
