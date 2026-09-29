$ErrorActionPreference = 'Stop'
$workflowRoot = Split-Path -Parent $PSScriptRoot
$workflowEnv = Join-Path $workflowRoot '.env'
if (Test-Path -LiteralPath $workflowEnv) {
    Write-Output '.env already exists; left unchanged.'
    exit 0
}
function New-WorkflowSecret {
    $bytes = New-Object byte[] 48
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $generator.GetBytes($bytes) } finally { $generator.Dispose() }
    return [Convert]::ToBase64String($bytes)
}
$content = [IO.File]::ReadAllText((Join-Path $workflowRoot '.env.example'))
$content = $content.Replace('replace-with-a-random-database-password', (New-WorkflowSecret))
$content = $content.Replace('replace-with-at-least-32-random-bytes', (New-WorkflowSecret))
$stream = [IO.File]::Open($workflowEnv, [IO.FileMode]::CreateNew)
$writer = New-Object IO.StreamWriter($stream, (New-Object Text.UTF8Encoding($false)))
try { $writer.Write($content) } finally { $writer.Dispose() }
Write-Output 'Created .env with random secrets. Keep this file private.'
