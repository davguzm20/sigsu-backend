$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http
$root = Split-Path $PSScriptRoot -Parent
$credentials = @{}
Get-Content (Join-Path $root '.env.metrics') | ForEach-Object {
    if ($_ -match '^(METRICS_USERNAME|METRICS_PASSWORD)=(.*)$') {
        $credentials[$matches[1]] = $matches[2]
    }
}
if (!$credentials.METRICS_USERNAME -or !$credentials.METRICS_PASSWORD) {
    throw 'Faltan las credenciales locales en .env.metrics'
}
$client = New-Object System.Net.Http.HttpClient
function Query($path, $expected, $authorization = $null, $method = 'GET') {
    $request = New-Object System.Net.Http.HttpRequestMessage($method, "http://127.0.0.1:8082$path")
    if ($authorization) { $request.Headers.Add('Authorization', $authorization) }
    if ($method -eq 'POST') {
        $request.Content = New-Object System.Net.Http.StringContent('{}', [Text.Encoding]::UTF8, 'application/json')
    }
    $response = $client.SendAsync($request).GetAwaiter().GetResult()
    $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    if ([int]$response.StatusCode -ne $expected) {
        throw "Respuesta inesperada en $method $path : $([int]$response.StatusCode)"
    }
    Write-Host "$method $path : HTTP $expected"
    $result = @{ Body = $body; ContentType = [string]$response.Content.Headers.ContentType }
    $response.Dispose()
    $request.Dispose()
    return $result
}
try {
    $health = (Query '/actuator/health' 200).Body | ConvertFrom-Json
    if ($health.status -ne 'UP' -or $health.components -or $health.details) {
        throw 'Salud no disponible o con detalles internos'
    }
    $basic = 'Basic ' + [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes(
        $credentials.METRICS_USERNAME + ':' + $credentials.METRICS_PASSWORD))
    $wrong = 'Basic ' + [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes(
        $credentials.METRICS_USERNAME + ':' + [Guid]::NewGuid().ToString()))
    $null = Query '/actuator/prometheus' 401
    $null = Query '/actuator/prometheus' 401 $wrong
    $metrics = Query '/actuator/prometheus' 200 $basic
    if ($metrics.ContentType -notmatch 'text/plain|application/openmetrics-text' -or
        $metrics.Body -notmatch '# HELP' -or $metrics.Body -notmatch '# TYPE' -or
        $metrics.Body -notmatch 'jvm_memory_used_bytes') {
        throw 'No se obtuvo el formato o las métricas JVM esperados'
    }
    Write-Host ('Formato de métricas: ' + $metrics.ContentType + '; HELP, TYPE y JVM verificados')
    foreach ($path in @('/actuator', '/actuator/env', '/actuator/configprops', '/actuator/beans',
            '/actuator/loggers', '/actuator/heapdump', '/actuator/shutdown')) {
        $null = Query $path 403 $basic
    }
    $null = Query '/actuator/shutdown' 403 $basic 'POST'
    $null = Query '/api/usuarios/roles' 401
    $null = Query '/api/usuarios/roles' 401 'Bearer invalid'
    $null = Query '/api/usuarios/roles' 401 $basic
    $null = Query '/api/auth/login' 400 $null 'POST'
    $null = Query '/v3/api-docs' 200
} finally {
    $client.Dispose()
    $credentials.Clear()
}
