# Observabilidad: operación local

## Requisitos y configuración

Ejecutar desde la raíz con Docker Desktop activo y Docker Compose 2.40.2 o
compatible con `!override`. El Dockerfile compila y ejecuta con Java 21; no hace
falta seleccionar un JDK de Windows. Se conserva Spring Boot 4.1.1.

- `.env`: configuración local de MySQL, JWT y opciones de la aplicación según
  `.env.example`. No sobrescribirla. Cloudinary requiere credenciales reales
  solo para sus operaciones; las verificaciones usan un servidor simulado.
- `.env.metrics`: `METRICS_USERNAME` y `METRICS_PASSWORD`, exclusivos del acceso
  técnico a métricas. No usar credenciales de usuarios de negocio.
- `scripts/Initialize-ObservabilitySecrets.ps1`: copia esa cuenta a archivos
  individuales y prepara credenciales Grafana en `.observability-secrets` sin
  sobrescribir archivos. Se detiene si las cuentas de métricas no coinciden.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Initialize-ObservabilitySecrets.ps1
$compose = @('-f','docker-compose.yml','-f','docker-compose.baseline.yml','-f','docker-compose.observability.yml')
docker compose @compose config --quiet
docker compose @compose up -d --build
docker compose @compose ps
```

`Bypass` solo afecta a ese proceso. `config --quiet` valida sin imprimir secretos.
El backend usa `dev,observability`, MySQL interno en `mysql:3306` y puerto interno
8080. No iniciar los contenedores históricos preservados: también utilizan 8082.

| Componente | Acceso desde Windows |
| --- | --- |
| Backend | http://127.0.0.1:8082 |
| MySQL | 127.0.0.1:3307 |
| Prometheus 3.15.1 | http://127.0.0.1:9090 |
| Grafana 13.3.0 | http://127.0.0.1:3000 |
| Tempo 3.1.1 | Interno, consultable desde Grafana |
| Receptor de alertas | Interno, webhook local de demostración |

Para entrar a Grafana, abrir localmente `.observability-secrets/grafana_username`
y `grafana_password`. No mostrarlos en consola compartida. Cambiar el archivo no
cambia la contraseña de una cuenta ya inicializada en la base persistente Grafana.
Los secretos Compose son archivos montados, no implican cifrado del disco.

Variables opcionales: `PROMETHEUS_PORT=9090`, `GRAFANA_PORT=3000` y
`TEMPO_RETENTION=24h`. El perfil permite `TRACING_SAMPLING_PROBABILITY` (por defecto
1.0), `TRACING_EXPORT_ENABLED` y `OTLP_TRACES_ENDPOINT`. Compose fija exportación
activa por OTLP HTTP/protobuf a `http://tempo:4318/v1/traces`. Para desactivarla,
usar una sobreescritura Compose local de `TRACING_EXPORT_ENABLED: "false"` y
recrear solo el backend; cambiar `.env` no sustituye el valor fijado por Compose.
Sin el perfil `observability` no se exponen las métricas técnicas.

## Parada, reinicio y persistencia

```powershell
docker compose @compose stop
docker compose @compose up -d
docker compose @compose restart prometheus grafana tempo
```

No usar `down -v`, `volume rm` ni `prune`. MySQL, Prometheus, Tempo, Grafana y el
receptor conservan sus volúmenes. Prometheus retiene siete días, configurados en
`observability/prometheus.yml`; cambiar el valor requiere reiniciarlo. Tempo
retiene un día mediante `TEMPO_RETENTION`; aplicar cambios con `up -d tempo`.
El borrado es periódico, no inmediato. No se ensayó el vencimiento completo.

Para volver al backend anterior preservado, sin tocar MySQL ni volúmenes:

```powershell
docker compose @compose stop backend prometheus grafana tempo alert-receiver
docker compose @compose rm -f backend
docker rename sigsu-backend-step5-preserved sigsu-backend-observability
docker start sigsu-backend-observability
```

Solo usar esa alternativa si existe el contenedor preservado. Para regresar a
Compose, detenerlo, renombrarlo a `sigsu-backend-step5-preserved` y ejecutar
`docker compose @compose up -d`. La ejecución anterior tiene OTLP deshabilitado.

## Comprobaciones esenciales

```powershell
Invoke-RestMethod http://127.0.0.1:8082/actuator/health
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Test-Actuator.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Test-RequestLogs.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Test-ObservabilityStack.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Test-TechnicalDashboard.ps1 -SkipDemo
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Test-Alerting.ps1
```

Esperado: salud UP sin detalles internos, métricas con Basic técnico y rechazo
sin credenciales, endpoints sensibles inaccesibles y seguridad JWT conservada.
Los scripts leen credenciales sin imprimirlas. El del stack genera peticiones de
salud, comprueba incremento del contador y el mismo traceId en respuesta, log y
filas de Tempo consultadas mediante Grafana. Guarda únicamente el estado necesario
para verificar persistencia en `.observability-secrets/validation-state.json`.

Para verificar persistencia, ejecutar el script del stack, reiniciar Prometheus,
Grafana y Tempo y repetirlo con `-VerifyPersistence`, antes de vencer la retención.
Para comprobar tráfico API sin escrituras, ejecutar el script del dashboard sin
`-SkipDemo`: envía seis peticiones inválidas a login. No debe haber otros clientes
API concurrentes en esa comprobación de incremento exacto.

Suite de seguridad, correlación, Cloudinary simulado y JWT, sin exportación remota:

```powershell
docker build --target build -t sigsu-backend:verification .
docker run --rm --network sigsu-backend_default --env-file .env --env-file .env.metrics -e MYSQL_HOST=mysql -e MYSQL_PORT=3306 -e SPRING_PROFILES_ACTIVE=dev,observability -e TRACING_EXPORT_ENABLED=false sigsu-backend:verification ./mvnw -B test
```

Estas pruebas no realizan subidas reales. Los spans del fallo de dependencia se
capturan en memoria; no deben presentarse como trazas consultadas en Tempo.

## Dashboard y alertas

Dashboard técnico: http://127.0.0.1:3000/d/sigsu-tecnico. Está separado del
dashboard de negocio. Incluye recolección, tasa HTTP, 4xx/5xx, p95, JVM, GC, Hikari
y almacenamiento por resultado. Las consultas API excluyen Actuator y UNKNOWN;
permiten rutas parametrizadas. Sin observaciones suficientes, porcentajes y p95
quedan sin datos. Cloudinary tampoco muestra datos hasta que haya operaciones.
`up=1` indica acceso al endpoint de métricas, no salud funcional completa.

Reglas: http://127.0.0.1:3000/alerting/list. Contacto:
http://127.0.0.1:3000/alerting/notifications. Todas se evalúan cada 10 segundos.

| Regla | Condición inicial y ventana | Permanencia |
| --- | --- | --- |
| Recolección | `up=0`, último scrape | 1 minuto |
| 5xx | Más de 5 % y al menos 100 peticiones API en 5 minutos | 2 minutos |
| p95 | Más de 1 segundo y al menos 50 observaciones API en 5 minutos | 2 minutos |
| Cloudinary | Al menos 3 fallos en 10 minutos | 1 minuto |

Los mínimos evitan interpretar muestras pequeñas como degradación; los umbrales
requieren calibración con uso real, no son un SLO. Ventana es el historial
consultado; permanencia es cuánto debe mantenerse la condición. La política suma
5 segundos de espera inicial y 10 segundos de intervalo de agrupación.
Recolección usa NoData y Error; las reglas de actividad usan OK sin datos y Error
ante fallos de evaluación. OK por inactividad no demuestra ausencia de fallos.

Editar `observability/alerting/thresholds.json` o `sigsu.template.json`, ejecutar
`scripts/Build-AlertRules.ps1` y reiniciar Grafana. El YAML generado es JSON válido;
se conserva porque Grafana no resuelve las variables anidadas de esas consultas.
El webhook interno `http://alert-receiver:8080/alerts` guarda recepciones sanitizadas
en `/data/notifications.jsonl`, dentro de su volumen. No avisa a una persona externa.

`scripts/Test-Alerting.ps1 -EndToEnd` detiene brevemente solo el backend, comprueba
recepción firing, lo restaura y comprueba resolved; requiere una ventana local
sin uso. La variante normal no detiene servicios. Solo recolección fue probada
E2E; las otras tres reglas se validaron por configuración, consulta y evaluación.

## Identificar y depurar incidentes

1. Registrar hora, ruta, estado HTTP e impacto observado. Revisar contenedores y
   el target `sigsu-backend` en Prometheus. `up=0` puede indicar proceso, red o
   autenticación; consultar `lastError` antes de atribuir una causa.
2. Comprobar la condición, Pending/Firing y recepción en el webhook. Una
   interrupción que no llegó al proceso no tiene traza de petición del backend.
3. Si hubo respuesta, tomar `X-Trace-Id`. En Grafana Explore, elegir Tempo y Trace
   ID; esperar el envío OTLP. No hay enlaces automáticos desde métricas a trazas.
4. Buscar el resumen y error del mismo ID; comparar duración servidor, operación
   y cliente sin sumar tiempos anidados. No registrar cuerpos, tokens ni URLs
   individuales para ampliar el diagnóstico.
5. Corregir la causa comprobada; verificar salud, `up=1`, respuesta y resolución.

```powershell
$response = Invoke-WebRequest -UseBasicParsing http://127.0.0.1:8082/actuator/health
$traceId = $response.Headers['X-Trace-Id']
docker logs sigsu-backend-observability 2>&1 | ForEach-Object {
    try { $_.ToString() | ConvertFrom-Json -ErrorAction Stop } catch { }
} | Where-Object traceId -eq $traceId | Select-Object '@timestamp',level,message,event,status,durationMs,traceId,spanId
```

La prueba `ExternalSignalsTests` permite investigar un HTTP 503 simulado que
produce 502: valida causa segura `http_error`, un solo ERROR y resumen correlacionado,
relación SERVER/almacenamiento/CLIENT, duración y recuperación posterior 200.
No agrega endpoints a la aplicación normal. Los fallos técnicos JWT se registran
sin token ni datos personales; los tokens inválidos siguen rechazándose.

La instalación es local, sin alta disponibilidad ni copias externas. Basic no
cifra el tráfico; acceso remoto requiere TLS, gestión de secretos y un canal humano
autorizado. Tempo puede emitir `no jobs found` con cola de mantenimiento vacía;
comprobar ingestión y consulta antes de interpretarlo como caída.
