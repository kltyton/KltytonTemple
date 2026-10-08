$repoRoot = Split-Path -Parent $PSScriptRoot
& python (Join-Path $repoRoot 'temple.py') --root $repoRoot matrix --json
exit $LASTEXITCODE
