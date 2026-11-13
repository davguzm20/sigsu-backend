param([switch]$SkipDemo)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$dashboard = Get-Content (Join-Path $root 'observability/grafana/dashboards/backend.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$user=[IO.File]::ReadAllText((Join-Path $root '.observability-secrets/grafana_username'))
$password=[IO.File]::ReadAllText((Join-Path $root '.observability-secrets/grafana_password'))
$auth=@{Authorization='Basic '+[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($user+':'+$password))}
function Query($expression) {
    $r=Invoke-RestMethod ('http://127.0.0.1:9090/api/v1/query?query='+[URI]::EscapeDataString($expression)) -TimeoutSec 10
    if($r.status -ne 'success'){throw 'Consulta Prometheus inválida'}
    return $r.data.result
}
function Resolve($expression, $route='/api/.*', $window='5m') {
    return $expression.Replace('$ruta',$route).Replace('$ventana',$window)
}
$live=Invoke-RestMethod -Headers $auth http://127.0.0.1:3000/api/dashboards/uid/sigsu-tecnico
if(!$live.meta.provisioned -or $live.dashboard.panels.Count -ne $dashboard.panels.Count){throw 'Dashboard no aprovisionado correctamente'}
Write-Host ('Dashboard aprovisionado: '+$dashboard.panels.Count+' paneles; UID sigsu-tecnico')
$api='job="sigsu-backend",uri=~"/api/.*"'
$counter='sum(http_server_requests_seconds_count{'+$api+'})'
$buckets=@(Query ('http_server_requests_seconds_bucket{'+$api+',le="+Inf"}'))
if($buckets.Count -eq 0){throw 'No hay buckets HTTP API compatibles'}
Write-Host 'Histogramas clásicos confirmados, incluido bucket +Inf'
$percent=($dashboard.panels|Where-Object id -eq 4).targets[0].expr
$p95=($dashboard.panels|Where-Object id -eq 5).targets[0].expr
$quiet=@(Query ('sum(rate(http_server_requests_seconds_count{'+$api+'}[1m]))'))
if($quiet.Count -eq 1 -and [double]$quiet[0].value[1] -eq 0){
    # Probar denominador cero con 4xx observados, no depender de que falte la serie 5xx.
    $fourPercent=(Resolve $percent '/api/.*' '1m').Replace('5..','4..')
    if(@(Query $fourPercent).Count -ne 0 -or @(Query (Resolve $p95 '/api/.*' '1m')).Count -ne 0){throw 'Sin tráfico se presentó porcentaje o p95'}
    Write-Host 'Sin tráfico: tasa 0 real; porcentaje con denominador cero y p95 sin datos'
}
if(@(Query (Resolve $percent '/api/__sin_series')).Count -ne 0){throw 'Una ruta sin series mostró errores numéricos'}
if(@(Query (Resolve $p95 '/api/__sin_series')).Count -ne 0){throw 'Una ruta sin series mostró p95'}
Write-Host 'Filtro sin series: porcentaje y p95 vacíos, sin ceros de relleno'
$before=$null;$after=$null
if(!$SkipDemo){
    $before=[double](@(Query $counter)[0].value[1])
    Add-Type -AssemblyName System.Net.Http
    $client=New-Object Net.Http.HttpClient
    try{
        for($i=0;$i -lt 6;$i++){
            $request=New-Object Net.Http.HttpRequestMessage('POST','http://127.0.0.1:8082/api/auth/login')
            $request.Content=New-Object Net.Http.StringContent('{}',[Text.Encoding]::UTF8,'application/json')
            $r=$client.SendAsync($request).GetAwaiter().GetResult()
            if([int]$r.StatusCode -ne 400){throw 'La demostración no devolvió el 400 de validación esperado'}
            $r.Dispose();$request.Dispose()
            Start-Sleep -Milliseconds 400
        }
    }finally{$client.Dispose()}
    # Salud no debe sumarse al tráfico API.
    $null=Invoke-RestMethod http://127.0.0.1:8082/actuator/health
    for($retry=0;$retry -lt 30;$retry++){
        $after=[double](@(Query $counter)[0].value[1])
        if($after -ge $before+6){break}
        Start-Sleep -Seconds 2
    }
    if($after-$before -ne 6){throw 'El contador API no aumentó exactamente seis, o incluyó Actuator'}
    Write-Host ('Demostración sin escrituras: API '+$before+' -> '+$after+'; salud Actuator excluida')
}
$checked=0
foreach($panel in $dashboard.panels|Where-Object type -ne 'text'){
    $queries=@()
    foreach($target in $panel.targets){
        $expression=Resolve $target.expr
        $null=Query $expression
        $queries+=@{refId=$target.refId;datasource=@{type='prometheus';uid='sigsu-prometheus'};expr=$expression;instant=$target.instant;range=$target.range;intervalMs=5000;maxDataPoints=300}
        $checked++
    }
    $payload=@{queries=$queries;from=([DateTimeOffset]::UtcNow.AddMinutes(-10).ToUnixTimeMilliseconds()).ToString();to=([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()).ToString()}|ConvertTo-Json -Depth 20
    $result=Invoke-RestMethod -Method Post -Headers $auth -ContentType application/json -Body $payload http://127.0.0.1:3000/api/ds/query -TimeoutSec 15
    foreach($item in $result.results.PSObject.Properties){if($item.Value.error){throw ('Error en panel '+$panel.id)}}
    if(!$panel.fieldConfig.defaults.unit -or !$panel.description){throw 'Panel sin unidad o descripción'}
}
$latency=@(Query (Resolve $p95))
if(!$SkipDemo -and ($latency.Count -ne 1 -or $latency[0].value[1] -match 'NaN|Inf')){throw 'No se pudo calcular p95 tras tráfico e histogramas'}
$cloud=@(Query 'storage_upload_seconds_count{job="sigsu-backend"}')
$five=@(Query 'http_server_requests_seconds_count{job="sigsu-backend",uri=~"/api/.*",status=~"5.."}')
Write-Host ('Consultas verificadas en Prometheus y Grafana: '+$checked)
if($latency.Count){Write-Host ('p95 API observado: '+$latency[0].value[1]+' segundos')}
Write-Host ('Series Cloudinary actuales: '+$cloud.Count+'; series 5xx API actuales: '+$five.Count+' (ausencia no se convierte en cero errores)')
