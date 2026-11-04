# Encerra somente os processos de teste registrados, preservando o banco e as credenciais.
$ErrorActionPreference = 'Stop'
$rootPath = Split-Path -Parent $PSScriptRoot
$statePath = Join-Path $rootPath '.tools/sandbox-processes.json'
if (!(Test-Path -LiteralPath $statePath)) { Write-Output 'Nenhum ambiente registrado.'; exit 0 }
$entries = Get-Content -Raw -LiteralPath $statePath | ConvertFrom-Json
foreach ($entry in ($entries | Where-Object name -ne 'postgres')) {
    $process = Get-Process -Id $entry.id -ErrorAction SilentlyContinue
    if ($process -and $process.ProcessName -eq $entry.name -and
        $process.StartTime.ToUniversalTime().Ticks.ToString() -eq $entry.started) {
        Stop-Process -Id $process.Id
    }
}
$entry = $entries | Where-Object name -eq 'postgres' | Select-Object -First 1
if ($entry) {
    $process = Get-Process -Id $entry.id -ErrorAction SilentlyContinue
    if ($process -and $process.ProcessName -eq $entry.name -and
        $process.StartTime.ToUniversalTime().Ticks.ToString() -eq $entry.started) {
        & (Join-Path $rootPath '.tools/postgres-test/pgsql/bin/pg_ctl.exe') -D (Join-Path $rootPath '.tools/postgres-test/data') -m fast -w stop
        if ($LASTEXITCODE -ne 0) { throw 'Não foi possível encerrar o PostgreSQL de testes.' }
    }
}
Write-Output 'Ambiente de testes encerrado; dados locais preservados.'
