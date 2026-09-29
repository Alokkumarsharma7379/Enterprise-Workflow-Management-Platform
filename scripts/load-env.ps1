# Dot-source this script to load .env into the current PowerShell process.
$workflowRoot = Split-Path -Parent $PSScriptRoot
Get-Content -LiteralPath (Join-Path $workflowRoot '.env') | ForEach-Object {
    if ($_ -match '^([A-Z][A-Z0-9_]*)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process')
    }
}
