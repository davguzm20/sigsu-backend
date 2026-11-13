$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$config=Get-Content (Join-Path $root 'observability/alerting/thresholds.json') -Raw -Encoding UTF8|ConvertFrom-Json
$template=[IO.File]::ReadAllText((Join-Path $root 'observability/alerting/sigsu.template.json'))
$rendered=[regex]::Replace($template,'\$\{(ALERT_[A-Z0-9_]+)\}',[Text.RegularExpressions.MatchEvaluator]{param($match)
    $key=$match.Groups[1].Value
    $property=$config.PSObject.Properties[$key]
    if(!$property){throw "Falta configuración: $key"}
    $value=[Convert]::ToString($property.Value,[Globalization.CultureInfo]::InvariantCulture)
    if($value -notmatch '^([0-9]+(\.[0-9]+)?|[1-9][0-9]*(ms|s|m|h|d))$'){throw "Valor inválido para $key"}
    return $value
})
if($rendered -match '\$\{ALERT_'){throw 'Quedaron variables sin resolver'}
$parsed=$rendered|ConvertFrom-Json
if($parsed.groups[0].rules.Count -ne 4){throw 'Se esperaban cuatro reglas'}
[IO.File]::WriteAllText((Join-Path $root 'observability/grafana/provisioning/alerting/sigsu.yaml'),$rendered,(New-Object Text.UTF8Encoding($false)))
Write-Host 'Generadas cuatro reglas versionadas con umbrales explícitos'
