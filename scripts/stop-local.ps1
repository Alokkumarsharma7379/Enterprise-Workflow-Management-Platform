param([switch]$StopDatabase)
$ErrorActionPreference = 'Stop'
$workflowRoot = Split-Path -Parent $PSScriptRoot
foreach ($service in @('backend', 'frontend')) {
    $pidFile = Join-Path $workflowRoot ".tools/$service.pid"
    if (Test-Path -LiteralPath $pidFile) {
        $serviceId = [int](Get-Content -LiteralPath $pidFile)
        $process = Get-CimInstance Win32_Process -Filter "ProcessId = $serviceId"
        if ($process -and $process.CommandLine -and $process.CommandLine.Contains($workflowRoot)) {
            Stop-Process -Id $serviceId
            Write-Output "Stopped local $service."
        }
    }
}
if ($StopDatabase) {
    $data = Join-Path $workflowRoot '.tools/pgdata'
    $pg = Get-ChildItem -Path "$env:ProgramFiles/PostgreSQL/*/bin/pg_ctl.exe" -ErrorAction SilentlyContinue | Select-Object -Last 1
    if ($pg -and (Test-Path -LiteralPath (Join-Path $data 'PG_VERSION'))) {
        & $pg.FullName -D $data stop -m fast
    }
}
