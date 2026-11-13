$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$directory = Join-Path $root '.observability-secrets'
$null = New-Item -ItemType Directory -Path $directory -Force
$values = @{}
Get-Content (Join-Path $root '.env.metrics') | ForEach-Object {
    if ($_ -match '^(METRICS_USERNAME|METRICS_PASSWORD)=(.*)$') { $values[$matches[1]] = $matches[2] }
}
if (!$values.METRICS_USERNAME -or !$values.METRICS_PASSWORD) { throw 'Faltan credenciales en .env.metrics' }
$encoding = New-Object Text.UTF8Encoding($false)
function Write-NewSecret($name, $value) {
    $path = Join-Path $directory $name
    if (!(Test-Path -LiteralPath $path)) {
        [IO.File]::WriteAllText($path, $value, $encoding)
        Write-Host "Archivo local creado: $name (valor oculto)"
    } else {
        Write-Host "Archivo local conservado: $name"
    }
}
foreach ($mapping in @(@('metrics_username', 'METRICS_USERNAME'), @('metrics_password', 'METRICS_PASSWORD'))) {
    $path = Join-Path $directory $mapping[0]
    if ((Test-Path $path) -and [IO.File]::ReadAllText($path) -cne $values[$mapping[1]]) {
        throw 'Las credenciales locales no coinciden con .env.metrics; no se sobrescribieron'
    }
    Write-NewSecret $mapping[0] $values[$mapping[1]]
}
Write-NewSecret 'grafana_username' 'admin'
$bytes = New-Object byte[] 32
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
Write-NewSecret 'grafana_password' ([Convert]::ToBase64String($bytes))
$values.Clear()
