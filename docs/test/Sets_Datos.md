# Sets de datos

| Set | Escenario | Datos asignados |
|---|---|---|
| SD-01 | ESC-01 | Credenciales válidas de supervisor; contraseña incorrecta; usuario inactivo; token ausente y token alterado. |
| SD-02 | ESC-02 | Título `Falla de red en el laboratorio 3`; descripción `No hay conexión en los equipos del laboratorio`; ubicación `Laboratorio 3`; título de 151 caracteres; título vacío. |
| SD-03 | ESC-02 | `evidencia.png` válido de 120 KB; `evidencia.pdf` válido; `evidencia.txt`; `falso.png` con contenido de texto; archivo de 6 MB. |
| SD-04 | ESC-03 | `categoriaId` de REDES; `prioridadId` de ALTA; `areaId` de SISTEMAS; `fechaLimite` una semana después; categoría inexistente. |
| SD-05 | ESC-04 | `solicitudId` de `SOL-2026-000002`; `tecnicoId` de `tecnico@sigsu.local`; técnico inexistente; técnico desactivado; segunda asignación activa. |
| SD-06 | ESC-05 | Estado de destino; observación; solución; solicitud sin solución; salto de estado no permitido. |
| SD-07 | ESC-04 | Solicitudes de `solicitante` y de `solicitante2`; asignaciones de `tecnico` y `tecnico2`. |
| SD-08 | ESC-07 | `desde=2026-11-01`; `hasta=2026-11-30`; `categoriaId` de REDES; `prioridadId` de ALTA; sin filtros. |
| SD-09 | ESC-09 | Solicitud en `EN_EVALUACION` creada hace 25 horas; prioridad ALTA; `fechaLimite` a 8 horas del registro. |
| SD-10 | ESC-10 | `solicitante` con tres solicitudes: la actual y dos anteriores. |
| SD-11 | ESC-11 | Solicitud `CERRADA`; `puntuacion=5`; `comentario=Atención rápida`; edición dentro de los 7 días del cierre. |
| SD-12 | ESC-01 | `JWT_SECRET` de prueba en Base64; expiración de 3600000 ms; token con expiración negativa; token firmado con otra clave; cadena sin formato JWT; usuario inactivo. |
| SD-13 | ESC-08 | Área con código `SISTEMAS`; área con nombre `Sistemas`; prioridad con nivel 3; `id=9999`; correo de otro usuario. |
| SD-14 | ESC-02 | `evidencia.png` válido; `falso.png`; `evidencia.pdf`; cuenta de Cloudinary de prueba o WireMock. |
| SD-15 | ESC-08 | Dos técnicos activos en SISTEMAS y uno inactivo; usuario inexistente `9999`; correo duplicado. |

