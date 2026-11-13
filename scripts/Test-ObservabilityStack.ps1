param([switch]$VerifyPersistence)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$statePath = Join-Path $root '.observability-secrets/validation-state.json'
$user = [IO.File]::ReadAllText((Join-Path $root '.observability-secrets/grafana_username'))
$password = [IO.File]::ReadAllText((Join-Path $root '.observability-secrets/grafana_password'))
$auth = @{ Authorization = 'Basic ' + [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($user + ':' + $password)) }
$query = 'sum(http_server_requests_seconds_count{job="sigsu-backend",uri="/actuator/health",method="GET",status="200"})'
function PromQuery($expression, $at = $null) {
    $url = 'http://127.0.0.1:9090/api/v1/query?query=' + [URI]::EscapeDataString($expression)
    if ($at) { $url += '&time=' + $at }
    $result = Invoke-RestMethod $url -TimeoutSec 10
    if ($result.status -ne 'success') { throw 'Falló la consulta Prometheus' }
    return $result.data.result
}
function Grafana($path, $body = $null) {
    $arguments = @{ Uri=('http://127.0.0.1:3000' + $path); Headers=$auth; TimeoutSec=15 }
    if ($body) {
        $arguments.Method = 'POST'
        $arguments.ContentType = 'application/json'
        $arguments.Body = ($body | ConvertTo-Json -Depth 20 -Compress)
    }
    return Invoke-RestMethod @arguments
}
function Retry($description, [scriptblock]$check) {
    for ($i=0; $i -lt 30; $i++) {
        try { $result = & $check; if ($result) { return $result } } catch { }
        Start-Sleep -Seconds 2
    }
    throw "No se verificó: $description"
}
function TraceFromGrafana($id) {
    $result = Grafana '/api/ds/query' @{
        queries = @(@{ refId='A'; datasource=@{type='tempo';uid='sigsu-tempo'}; queryType='traceId'; query=$id })
        from = ([DateTimeOffset]::UtcNow.AddHours(-1).ToUnixTimeMilliseconds()).ToString()
        to = ([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()).ToString()
    }
    if ($result.results.A.error) { return $null }
    # Comprobar filas reales, no solo un ID mencionado en metadatos de la consulta.
    foreach ($frame in $result.results.A.frames) {
        for ($column=0; $column -lt $frame.schema.fields.Count; $column++) {
            if ($frame.schema.fields[$column].name -ieq 'traceID' -and
                @($frame.data.values[$column] | Where-Object { $_ -eq $id }).Count -gt 0) {
                return $result
            }
        }
    }
    return $null
}
$null = Retry 'target UP' {
    $targets = (Invoke-RestMethod http://127.0.0.1:9090/api/v1/targets -TimeoutSec 5).data.activeTargets
    $target = $targets | Where-Object { $_.labels.job -eq 'sigsu-backend' }
    if ($target.health -eq 'up' -and !$target.lastError) { return $true }
}
Write-Host 'Prometheus: target sigsu-backend UP, sin error de scrape'
$null = Retry 'Grafana disponible y protegido' {
    try {
        $null = Invoke-WebRequest -UseBasicParsing http://127.0.0.1:3000/api/datasources -TimeoutSec 5
        throw 'Grafana permitió acceso sin autenticar'
    } catch {
        if ($_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq 401) { return $true }
        throw
    }
}
Write-Host 'Grafana: API de fuentes rechaza acceso anónimo con 401'
foreach ($uid in @('sigsu-prometheus', 'sigsu-tempo')) {
    $null = Retry "fuente $uid saludable" {
        $health = Grafana "/api/datasources/uid/$uid/health"
        if ($health.status -eq 'OK') { return $true }
    }
    Write-Host "Grafana: fuente $uid OK"
}
$up = @(PromQuery 'up{job="sigsu-backend"}')
if ($up.Count -ne 1 -or [double]$up[0].value[1] -ne 1) { throw 'up no devolvió 1' }
if ($VerifyPersistence) {
    $state = Get-Content $statePath -Raw | ConvertFrom-Json
    $historic = @(PromQuery $query $state.metricTime)
    if ($historic.Count -ne 1 -or [double]$historic[0].value[1] -ne [double]$state.metricValue) {
        throw 'No se recuperó la muestra histórica anterior al reinicio'
    }
    $null = Retry 'traza anterior al reinicio vía Grafana' { TraceFromGrafana $state.traceId }
    Write-Host ('Persistencia verificada: muestra histórica=' + $state.metricValue + '; traza=' + $state.traceId)
    return
}
$null = Invoke-RestMethod http://127.0.0.1:8082/actuator/health -TimeoutSec 10
$baseline = Retry 'métrica HTTP inicial' { $sample = @(PromQuery $query); if ($sample.Count -eq 1) { return $sample[0] } }
$before = [double]$baseline.value[1]
for ($i=0; $i -lt 5; $i++) {
    $response = Invoke-WebRequest -UseBasicParsing http://127.0.0.1:8082/actuator/health -TimeoutSec 10
    if ($response.StatusCode -ne 200) { throw 'Falló la petición de salud' }
    $traceId = [string]$response.Headers['X-Trace-Id']
}
$after = Retry 'incremento de cinco peticiones HTTP' {
    $sample = @(PromQuery $query)
    if ($sample.Count -eq 1 -and [double]$sample[0].value[1] -ge ($before + 5)) { return $sample[0] }
}
Write-Host ('Métrica HTTP salud: ' + $before + ' -> ' + $after.value[1])
$null = Retry 'traza vía consulta de fuente Tempo en Grafana' { TraceFromGrafana $traceId }
$logs = @(docker logs sigsu-backend-observability 2>&1 | ForEach-Object {
    try { $_.ToString() | ConvertFrom-Json -ErrorAction Stop } catch { }
})
if ($LASTEXITCODE -ne 0) { throw 'No se pudieron leer los logs del backend' }
$summary = @($logs | Where-Object { $_.event -eq 'http.request' -and $_.traceId -eq $traceId })
if ($summary.Count -ne 1 -or $summary[0].status -ne 200) { throw 'No coincide el resumen con la traza' }
Write-Host ('Traza encontrada por Grafana y correlacionada con respuesta y log: ' + $traceId)
$state = @{ traceId=$traceId; metricTime=$after.value[0]; metricValue=$after.value[1]; checkedAt=[DateTime]::UtcNow.ToString('o') }
[IO.File]::WriteAllText($statePath, ($state | ConvertTo-Json), (New-Object Text.UTF8Encoding($false)))
Write-Host 'Estado local guardado para comprobar persistencia tras reiniciar'
