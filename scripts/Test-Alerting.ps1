param([switch]$EndToEnd)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$compose=@('-f','docker-compose.yml','-f','docker-compose.baseline.yml','-f','docker-compose.observability.yml')
$u=[IO.File]::ReadAllText((Join-Path $root '.observability-secrets/grafana_username'))
$p=[IO.File]::ReadAllText((Join-Path $root '.observability-secrets/grafana_password'))
$headers=@{Authorization='Basic '+[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($u+':'+$p))}
function Grafana($path){return (Invoke-RestMethod -Headers $headers ('http://127.0.0.1:3000'+$path) -TimeoutSec 10)}
function Query($expression){
    $r=Invoke-RestMethod ('http://127.0.0.1:9090/api/v1/query?query='+[URI]::EscapeDataString($expression)) -TimeoutSec 10
    if($r.status -ne 'success'){throw 'Consulta Prometheus inválida'}
    return $r.data.result
}
function Evaluations {
    return @((Grafana '/api/prometheus/grafana/api/v1/rules').data.groups|ForEach-Object {$_.rules}|Where-Object {$_.labels.service -eq 'sigsu-backend'})
}
function Messages {
    $code="import json,pathlib; p=pathlib.Path('/data/notifications.jsonl'); print(json.dumps([json.loads(x) for x in p.read_text().splitlines()] if p.exists() else []))"
    $raw=@(docker compose @compose exec -T alert-receiver python -c $code)
    if($LASTEXITCODE -ne 0){throw 'No se pudieron leer las recepciones locales'}
    $parsed=($raw -join "`n")|ConvertFrom-Json
    return $parsed
}
function RestoreBackend {
    docker compose @compose start backend
    if($LASTEXITCODE -ne 0){throw 'No se pudo restaurar el backend'}
    for($i=0;$i -lt 30;$i++){
        try{if((Invoke-RestMethod http://127.0.0.1:8082/actuator/health -TimeoutSec 3).status -eq 'UP'){return}}catch{}
        Start-Sleep -Seconds 2
    }
    throw 'El backend no recuperó salud UP'
}
$rules=@()
for($i=0;$i -lt 20;$i++){
    try{$rules=@(Grafana '/api/v1/provisioning/alert-rules');if($rules.Count -eq 4){break}}catch{}
    Start-Sleep -Seconds 2
}
if($rules.Count -ne 4){throw 'Se esperaban cuatro reglas'}
foreach($rule in $rules){
    if($rule.provenance -ne 'file' -or $rule.execErrState -ne 'Error'){throw 'Regla sin aprovisionamiento o sin manejo de errores'}
    $expr=($rule.data|Where-Object refId -eq 'A').model.expr
    if($expr -match '\$\{'){throw 'Variables no resueltas en una consulta'}
    $samples=@(Query $expr)
    Write-Host ($rule.uid+': consulta válida; series='+$samples.Count+'; for='+$rule.for+'; NoData='+$rule.noDataState+'; Error='+$rule.execErrState)
}
$contacts=@(Grafana '/api/v1/provisioning/contact-points')
$contact=$contacts|Where-Object uid -eq 'sigsu-local-webhook'
if(!$contact -or $contact.provenance -ne 'file' -or $contact.disableResolveMessage){throw 'Contacto local no configurado para resolución'}
$policy=Grafana '/api/v1/provisioning/policies'
if($policy.receiver -ne $contact.name){throw 'Política no dirigida al receptor local'}
$null=docker compose @compose exec -T alert-receiver python -c "import urllib.request; assert urllib.request.urlopen('http://127.0.0.1:8080/health').status==200"
if($LASTEXITCODE -ne 0){throw 'Receptor local no saludable'}
for($i=0;$i -lt 10;$i++){
    $evaluations=@(Evaluations)
    if($evaluations.Count -eq 4 -and !@($evaluations|Where-Object {$_.health -eq 'error' -or $_.lastError}).Count){break}
    Start-Sleep -Seconds 2
}
if($evaluations.Count -ne 4 -or @($evaluations|Where-Object {$_.health -eq 'error' -or $_.lastError}).Count){throw 'Persisten errores de evaluación'}
$evaluations|ForEach-Object{Write-Host ($_.labels.rule_uid+': estado='+$_.state+'; evaluación='+$_.health)}
Write-Host 'Contacto y política locales aprovisionados; resoluciones habilitadas'
if(!$EndToEnd){return}
$scrape=$rules|Where-Object uid -eq 'sigsu-scrape'
if($scrape.for -ne '1m'){throw 'Esta demostración requiere mantener for=1m en la regla de scrape'}
$up=@(Query 'up{job="sigsu-backend"}')
if($up.Count -ne 1 -or [double]$up[0].value[1] -ne 1){throw 'El backend debe empezar con up=1'}
$startedAt=[DateTime]::UtcNow
$stopped=$false;$restored=$false;$firing=$null;$resolved=$null
try{
    $stopped=$true
    docker compose @compose stop backend
    if($LASTEXITCODE -ne 0){throw 'No se pudo detener solo el backend'}
    $stoppedAt=[DateTime]::UtcNow
    Write-Host 'Solo backend detenido; Prometheus y Grafana siguen activos'
    $lastState=''
    for($i=0;$i -lt 40;$i++){
        $ruleState=@(Evaluations)|Where-Object {$_.labels.rule_uid -eq 'sigsu-scrape'}
        if($ruleState.state -ne $lastState){
            $lastState=$ruleState.state
            $seconds=[Math]::Round(([DateTime]::UtcNow-$stoppedAt).TotalSeconds,1)
            Write-Host ('t+'+$seconds+'s: '+$lastState)
        }
        $firing=@(Messages|Where-Object {([DateTimeOffset]::Parse($_.receivedAtUtc)).UtcDateTime -ge $startedAt -and $_.status -eq 'firing' -and @($_.alerts|Where-Object {$_.labels.rule_uid -eq 'sigsu-scrape' -and $_.status -eq 'firing'}).Count})|Select-Object -First 1
        if($firing){break}
        Start-Sleep -Seconds 5
    }
    if(!$firing){throw 'No se recibió la notificación firing'}
    $up=@(Query 'up{job="sigsu-backend"}')
    if([double]$up[0].value[1] -ne 0){throw 'No se verificó la condición real up=0'}
    $firingDelay=[Math]::Round((([DateTimeOffset]::Parse($firing.receivedAtUtc)).UtcDateTime-$stoppedAt).TotalSeconds,1)
    if($firingDelay -lt 60){throw 'La activación ocurrió antes de la permanencia mínima'}
    Write-Host ('FIRING recibido por webhook local después de '+$firingDelay+'s')
    $restoreAt=[DateTime]::UtcNow
    RestoreBackend
    $restored=$true
    Write-Host 'Backend restaurado, salud UP; esperando resolución real'
    for($i=0;$i -lt 30;$i++){
        $resolved=@(Messages|Where-Object {([DateTimeOffset]::Parse($_.receivedAtUtc)).UtcDateTime -ge $restoreAt -and $_.status -eq 'resolved' -and @($_.alerts|Where-Object {$_.labels.rule_uid -eq 'sigsu-scrape' -and $_.status -eq 'resolved'}).Count})|Select-Object -First 1
        if($resolved){break}
        Start-Sleep -Seconds 3
    }
    if(!$resolved){throw 'No se recibió la notificación resolved'}
    $up=@(Query 'up{job="sigsu-backend"}')
    if([double]$up[0].value[1] -ne 1){throw 'No se recuperó up=1'}
    $resolutionDelay=[Math]::Round((([DateTimeOffset]::Parse($resolved.receivedAtUtc)).UtcDateTime-$restoreAt).TotalSeconds,1)
    Write-Host ('RESOLVED recibido '+$resolutionDelay+'s después de iniciar recuperación; up=1')
}finally{
    if($stopped -and !$restored){RestoreBackend}
}
