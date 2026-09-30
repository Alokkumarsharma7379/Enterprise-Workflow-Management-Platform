param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
$workflowRoot = Split-Path -Parent $PSScriptRoot
$toolDirectory = Join-Path $workflowRoot '.tools'
New-Item -ItemType Directory -Path $toolDirectory -Force | Out-Null

if (-not (Test-Path -LiteralPath (Join-Path $workflowRoot '.env'))) {
    & (Join-Path $PSScriptRoot 'setup-env.ps1')
}
. (Join-Path $PSScriptRoot 'load-env.ps1')

if (-not $env:JAVA_HOME) {
    $javaCommand = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($javaCommand) {
        $env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent $javaCommand.Source)
    } else {
        $javaCandidate = Get-ChildItem -Path "$toolDirectory/java/*/bin/java.exe", "$env:ProgramFiles/JetBrains/*/jbr/bin/java.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($javaCandidate) { $env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent $javaCandidate.FullName) }
    }
}
if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath "$env:JAVA_HOME/bin/javac.exe")) {
    throw 'Install a Java 21 JDK and set JAVA_HOME before starting the application.'
}
$env:Path = "$env:JAVA_HOME/bin;" + $env:Path
$nodeCommand = Get-Command node.exe -ErrorAction SilentlyContinue
if (-not $nodeCommand) {
    $nodeCandidate = Get-ChildItem -Path "$toolDirectory/node/*/node.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $nodeCandidate) { throw 'Install Node.js 22.12+ or provide the local Node tool directory.' }
    $env:Path = $nodeCandidate.DirectoryName + ';' + $env:Path
    $nodeCommand = Get-Command node.exe
}

& (Join-Path $PSScriptRoot 'stop-local.ps1')

# Reuse only this workspace's existing isolated database, if present.
# Otherwise use the PostgreSQL connection configured in .env.
$data = Join-Path $toolDirectory 'pgdata'
if (Test-Path -LiteralPath (Join-Path $data 'PG_VERSION')) {
    $pg = Get-ChildItem -Path "$env:ProgramFiles/PostgreSQL/*/bin/pg_ctl.exe" -ErrorAction SilentlyContinue | Select-Object -Last 1
    if (-not $pg) { throw 'The workspace database exists, but pg_ctl.exe was not found.' }
    & $pg.FullName -D $data status *> $null
    if ($LASTEXITCODE -ne 0) {
        $arguments = @('-D', ('"' + $data + '"'), '-l', ('"' + (Join-Path $toolDirectory 'postgres.log') + '"'), '-o', '"-p 55432 -h 127.0.0.1"', 'start')
        $pgProcess = Start-Process -FilePath $pg.FullName -ArgumentList $arguments -WindowStyle Hidden -PassThru
        if (-not $pgProcess.WaitForExit(30000) -or $pgProcess.ExitCode -ne 0) { throw 'Could not start the workspace PostgreSQL instance; inspect .tools/postgres.log.' }
    }
    $env:DB_URL = 'jdbc:postgresql://127.0.0.1:55432/workflow'
    $env:DB_USERNAME = 'workflow'
}
$env:FRONTEND_ORIGIN = 'http://localhost:5173'
$env:COOKIE_SECURE = 'false'
$env:PORT = '8080'
$env:DEBUG = 'false'

Push-Location (Join-Path $workflowRoot 'backend')
try {
    if (-not $SkipBuild) {
        & .\mvnw.cmd -B '-DskipTests' package
        if ($LASTEXITCODE -ne 0) { throw 'Backend build failed.' }
    }
} finally { Pop-Location }
Push-Location (Join-Path $workflowRoot 'frontend')
try {
    if (-not (Test-Path -LiteralPath 'node_modules')) {
        & npm.cmd ci
        if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency installation failed.' }
    }
} finally { Pop-Location }

foreach ($port in @(8080, 5173)) {
    if (Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue) { throw "Port $port is already in use by another process." }
}
$runtime = Join-Path $toolDirectory 'runtime'
New-Item -ItemType Directory -Path $runtime -Force | Out-Null
$runtimeJar = Join-Path $runtime 'workflow.jar'
Copy-Item -LiteralPath (Join-Path $workflowRoot 'backend/target/workflow-1.0.0.jar') -Destination $runtimeJar -Force
$backend = Start-Process -FilePath "$env:JAVA_HOME/bin/java.exe" -ArgumentList @('-jar', ('"' + $runtimeJar + '"')) -WorkingDirectory $workflowRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $toolDirectory 'backend.log') -RedirectStandardError (Join-Path $toolDirectory 'backend-error.log')
Set-Content -LiteralPath (Join-Path $toolDirectory 'backend.pid') -Value $backend.Id
$vite = Join-Path $workflowRoot 'frontend/node_modules/vite/bin/vite.js'
$frontend = Start-Process -FilePath $nodeCommand.Source -ArgumentList @(('"' + $vite + '"'), '--host', '127.0.0.1', '--port', '5173', '--strictPort') -WorkingDirectory (Join-Path $workflowRoot 'frontend') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $toolDirectory 'frontend.log') -RedirectStandardError (Join-Path $toolDirectory 'frontend-error.log')
Set-Content -LiteralPath (Join-Path $toolDirectory 'frontend.pid') -Value $frontend.Id
Write-Output 'Started local development services. Open http://localhost:5173 after the backend finishes starting.'
Write-Output 'Logs: .tools/backend.log and .tools/frontend.log. Stop with scripts/stop-local.ps1.'
