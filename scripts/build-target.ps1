param([Parameter(Mandatory = $true)][string] $Target)
$repoRoot = Split-Path -Parent $PSScriptRoot
& python (Join-Path $repoRoot 'temple.py') --root $repoRoot build --target $Target
exit $LASTEXITCODE
