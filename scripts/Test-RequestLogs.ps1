$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http
$canary = 'private-check-' + [Guid]::NewGuid().ToString('N')
$client = New-Object System.Net.Http.HttpClient
$checks = @(
    @{ Method='GET'; Path='/actuator/health'; Status=200 },
    @{ Method='POST'; Path='/api/auth/login'; Status=400 },
    @{ Method='GET'; Path=('/api/private/' + $canary + '?password=' + $canary); Status=401 }
)
$responses = @()
try {
    foreach ($check in $checks) {
        $request = New-Object System.Net.Http.HttpRequestMessage($check.Method, ('http://127.0.0.1:8082' + $check.Path))
        $request.Headers.Add('Cookie', ('private=' + $canary))
        $request.Headers.Add('X-Trace-Id', $canary)
        if ($check.Method -eq 'POST') {
            $request.Content = New-Object System.Net.Http.StringContent(('{"contrasena":"' + $canary + '"}'), [Text.Encoding]::UTF8, 'application/json')
        }
        $response = $client.SendAsync($request).GetAwaiter().GetResult()
        if ([int]$response.StatusCode -ne $check.Status) { throw 'Estado HTTP inesperado' }
        $id = @($response.Headers.GetValues('X-Trace-Id'))[0]
        if ($id -notmatch '^[0-9a-f]{32}$') { throw 'Falta identificador de traza real' }
        $responses += @{ TraceId=$id; Status=$check.Status }
        $response.Dispose()
        $request.Dispose()
    }
    # Dar tiempo a que finalice el filtro tras enviar la respuesta.
    Start-Sleep -Milliseconds 300
    $raw = @(docker logs sigsu-backend-observability 2>&1)
    if ($LASTEXITCODE -ne 0) { throw 'No se pudieron leer los logs Docker' }
    $events = @($raw | Where-Object { $_.ToString().Trim() } | ForEach-Object {
        $_.ToString() | ConvertFrom-Json -ErrorAction Stop
    })
    if (($raw -join "`n").Contains($canary)) { throw 'Se encontró un valor sensible de prueba en los logs' }
    foreach ($response in $responses) {
        $summary = @($events | Where-Object { $_.event -eq 'http.request' -and $_.traceId -eq $response.TraceId })
        if ($summary.Count -ne 1 -or $summary[0].status -ne $response.Status -or
            $summary[0].durationMs -lt 0 -or $summary[0].spanId -notmatch '^[0-9a-f]{16}$' -or
            !$summary[0].service -or !$summary[0].'@timestamp' -or !$summary[0].level) {
            throw 'Resumen ausente, duplicado o sin correlación válida'
        }
        Write-Host ('HTTP ' + $response.Status + ': un resumen JSON; traceId coincide; spanId y duración presentes')
    }
    Write-Host ('JSON válido en ' + $events.Count + ' registros; valores sensibles de prueba ausentes')
} finally {
    $client.Dispose()
}
