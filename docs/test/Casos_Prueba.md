# Casos de prueba

Fecha de ejecución: 14/11/2026. Ambiente: local, JDK 21 y MySQL 8.4 en Testcontainers. Resultado: 96 pruebas ejecutadas, 96 pasaron y 0 fallaron. Evidencia: `target/surefire-reports/`.

## Unitarias

### Máquina de estados (16)

| ID | Título | Objetivo | Datos de entrada | Resultado esperado | Estado (Pasó / Falló) |
|---|---|---|---|---|---|
| UT-SOL-01 | Transición de REGISTRADA a EN_EVALUACION | Verificar que el supervisor puede mover la solicitud de REGISTRADA a EN_EVALUACION. | Origen REGISTRADA; destino EN_EVALUACION; rol SUPERVISOR | La solicitud queda en EN_EVALUACION y se registra una entrada en el historial. | Pasó |
| UT-SOL-02 | Transición de EN_EVALUACION a ASIGNADA | Verificar que el supervisor puede mover la solicitud de EN_EVALUACION a ASIGNADA con una asignación activa. | Origen EN_EVALUACION; destino ASIGNADA; asignación activa | La solicitud queda en ASIGNADA y conserva la asignación activa. | Pasó |
| UT-SOL-03 | Inicio de atención de una solicitud asignada | Verificar que el técnico puede iniciar la atención de una solicitud asignada. | Origen ASIGNADA; destino EN_ATENCION; rol TECNICO | La solicitud queda en EN_ATENCION y conserva la asignación del técnico. | Pasó |
| UT-SOL-04 | Resolución de una solicitud en atención | Verificar que el técnico puede resolver la solicitud con una solución. | Origen EN_ATENCION; destino RESUELTA; solución | La solicitud queda en RESUELTA y se registra la fecha de resolución. | Pasó |
| UT-SOL-05 | Cierre de una solicitud resuelta | Verificar que el supervisor puede cerrar una solicitud resuelta. | Origen RESUELTA; destino CERRADA; rol SUPERVISOR | La solicitud queda en CERRADA y se registra la fecha de cierre. | Pasó |
| UT-SOL-06 | Salto de REGISTRADA a CERRADA | Verificar que el sistema no permite saltar de REGISTRADA a CERRADA. | Origen REGISTRADA; destino CERRADA | El sistema rechaza el salto y conserva el estado original (422). | Pasó |
| UT-SOL-07 | Movimiento de una solicitud CERRADA | Verificar que el sistema no permite mover una solicitud CERRADA. | Origen CERRADA; destino EN_EVALUACION | El sistema rechaza la transición porque CERRADA es el estado final (422). | Pasó |
| UT-SOL-08 | Retroceso de RESUELTA a EN_ATENCION | Verificar que el sistema no permite retroceder de RESUELTA a EN_ATENCION. | Origen RESUELTA; destino EN_ATENCION | El sistema rechaza el retroceso y conserva el estado RESUELTA (422). | Pasó |
| UT-SOL-09 | Salto de EN_EVALUACION a EN_ATENCION | Verificar que el sistema no permite saltar de EN_EVALUACION a EN_ATENCION. | Origen EN_EVALUACION; destino EN_ATENCION | El sistema rechaza el salto y conserva el estado EN_EVALUACION (422). | Pasó |
| UT-SOL-10 | Transición del supervisor ejecutada por un técnico | Verificar que el técnico no puede ejecutar la transición del supervisor. | Origen REGISTRADA; destino EN_EVALUACION; rol TECNICO | El sistema rechaza la operación porque el rol no corresponde y no registra historial (403). | Pasó |
| UT-SOL-11 | Transición del técnico ejecutada por un supervisor | Verificar que el supervisor no puede ejecutar la transición del técnico. | Origen ASIGNADA; destino EN_ATENCION; rol SUPERVISOR | El sistema rechaza la operación porque el rol no corresponde y no registra historial (403). | Pasó |
| UT-SOL-12 | Cierre ejecutado por el administrador | Verificar que el administrador no puede cerrar una solicitud. | Origen RESUELTA; destino CERRADA; rol ADMINISTRADOR | El sistema rechaza el cierre porque el administrador no puede ejecutarlo (403). | Pasó |
| UT-SOL-13 | Resolución sin solución registrada | Verificar que no se puede resolver una solicitud sin registrar la solución. | Origen EN_ATENCION; destino RESUELTA; solución nula | El sistema rechaza la resolución porque falta la solución y conserva el estado (422). | Pasó |
| UT-SOL-14 | Asignación sin asignación activa | Verificar que no se puede asignar una solicitud sin una asignación activa. | Origen EN_EVALUACION; destino ASIGNADA; sin asignación | El sistema rechaza la asignación porque no hay una asignación activa (422). | Pasó |
| UT-SOL-15 | Resolución con solución registrada | Verificar que la solución y la fecha de resolución se guardan al resolver. | Origen EN_ATENCION; destino RESUELTA; solución válida | La solicitud queda en RESUELTA con la solución y la fecha de resolución registradas. | Pasó |
| UT-SOL-16 | Cierre con historial completo | Verificar que el cierre exige una solicitud resuelta y deja el historial completo. | Origen RESUELTA; destino CERRADA; actor supervisor | La solicitud queda en CERRADA con la fecha de cierre y el historial con origen, destino y actor. | Pasó |

### Asignación (8)

| ID | Título | Objetivo | Datos de entrada | Resultado esperado | Estado (Pasó / Falló) |
|---|---|---|---|---|---|
| UT-ASG-01 | Asignación de una solicitud sin categoría | Verificar que no se puede asignar una solicitud sin categoría. | Solicitud sin categoría; técnico válido | El sistema rechaza la asignación porque la solicitud no tiene categoría (422). | Pasó |
| UT-ASG-02 | Asignación de una solicitud sin área | Verificar que no se puede asignar una solicitud sin área. | Solicitud sin área; técnico válido | El sistema rechaza la asignación porque la solicitud no tiene área (422). | Pasó |
| UT-ASG-03 | Segunda asignación activa | Verificar que no se permite una segunda asignación activa. | Solicitud con asignación activa; segundo técnico | El sistema rechaza la segunda asignación y conserva una sola asignación activa (409). | Pasó |
| UT-ASG-04 | Asignación de una solicitud inexistente | Verificar que una solicitud inexistente responde 404. | `solicitudId=9999` | El sistema responde que la solicitud no existe (404). | Pasó |
| UT-ASG-05 | Asignación de un técnico inexistente | Verificar que un técnico inexistente responde 404. | `tecnicoId=9999` | El sistema responde que el técnico no existe (404). | Pasó |
| UT-ASG-06 | Asignación de un usuario sin rol de técnico | Verificar que solo un usuario con rol de técnico puede ser asignado. | Usuario con rol SOLICITANTE | El sistema rechaza la asignación porque el usuario no tiene el rol de técnico (422). | Pasó |
| UT-ASG-07 | Asignación de un técnico desactivado | Verificar que un técnico desactivado no puede ser asignado. | Técnico con `activo=false` | El sistema rechaza la asignación porque el técnico está desactivado (422). | Pasó |
| UT-ASG-08 | Asignación válida de un técnico | Verificar que una asignación válida se guarda con su fecha y autor. | Solicitud clasificada; técnico activo; supervisor | La asignación se guarda con la fecha de asignación y el supervisor que la realizó. | Pasó |

### Control de acceso (4)

| ID | Título | Objetivo | Datos de entrada | Resultado esperado | Estado (Pasó / Falló) |
|---|---|---|---|---|---|
| UT-ACC-01 | Consulta del solicitante dueño | Verificar que el solicitante dueño puede consultar su solicitud. | Solicitud del solicitante autenticado | El solicitante dueño recibe su solicitud. | Pasó |
| UT-ACC-02 | Consulta de una solicitud ajena por el solicitante | Verificar que un solicitante no puede consultar una solicitud ajena. | Solicitud de otro solicitante | El sistema rechaza la consulta porque la solicitud pertenece a otro solicitante (403). | Pasó |
| UT-ACC-03 | Consulta del técnico asignado | Verificar que el técnico asignado puede consultar la solicitud. | Solicitud con asignación activa al técnico | El técnico asignado recibe la solicitud. | Pasó |
| UT-ACC-04 | Consulta de una solicitud no asignada por el técnico | Verificar que un técnico no asignado no puede consultar la solicitud. | Solicitud asignada a otro técnico | El sistema rechaza la consulta porque el técnico no está asignado a la solicitud (403). | Pasó |

### Inicio de sesión (5)

| ID | Título | Objetivo | Datos de entrada | Resultado esperado | Estado (Pasó / Falló) |
|---|---|---|---|---|---|
| UT-AUTH-01 | Inicio de sesión con correo no registrado | Verificar que un correo no registrado no puede iniciar sesión. | Correo inexistente | El sistema rechaza el acceso con el mensaje de credenciales incorrectas (401). | Pasó |
| UT-AUTH-02 | Inicio de sesión con contraseña incorrecta | Verificar que una contraseña incorrecta no permite iniciar sesión. | Correo válido y contraseña incorrecta | El sistema rechaza el acceso con el mismo mensaje de credenciales incorrectas (401). | Pasó |
| UT-AUTH-03 | Inicio de sesión de un usuario desactivado | Verificar que un usuario desactivado no puede iniciar sesión. | Usuario con `activo=false` | El sistema rechaza el acceso porque el usuario está desactivado (401). | Pasó |
| UT-AUTH-04 | Inicio de sesión con credenciales válidas | Verificar que un usuario activo inicia sesión con credenciales válidas. | Credenciales válidas | El sistema devuelve el token, su tipo, la expiración y los datos del usuario (200). | Pasó |
| UT-AUTH-05 | Login fallido sin emisión de token | Verificar que un login fallido no emite token. | Credenciales inválidas | El sistema no genera ningún token cuando el login falla. | Pasó |

### Guardas del administrador (8)

| ID | Título | Objetivo | Datos de entrada | Resultado esperado | Estado (Pasó / Falló) |
|---|---|---|---|---|---|
| UT-USR-01 | Registro de un usuario con correo duplicado | Verificar que no se registra un usuario con un correo duplicado. | Correo ya registrado | El sistema rechaza el alta porque el correo ya está registrado (409). | Pasó |
| UT-USR-02 | Desactivación de la cuenta propia | Verificar que el administrador no puede desactivar su propia cuenta. | Administrador autenticado sobre sí mismo | El sistema rechaza la desactivación de la cuenta propia (422). | Pasó |
| UT-USR-03 | Eliminación de la cuenta propia | Verificar que el administrador no puede eliminar su propia cuenta. | Administrador autenticado sobre sí mismo | El sistema rechaza la eliminación de la cuenta propia (422). | Pasó |
| UT-USR-04 | Retiro del rol de administrador propio | Verificar que el administrador no puede quitarse su propio rol. | Cambio de rol sobre sí mismo | El sistema rechaza quitarse el rol de administrador (422). | Pasó |
| UT-USR-05 | Cambio del rol de otro usuario | Verificar que el administrador puede cambiar el rol de otro usuario. | Administrador y otro usuario | El rol del otro usuario se actualiza sin errores. | Pasó |
| UT-USR-06 | Actualización de un usuario inexistente | Verificar que actualizar un usuario inexistente responde 404. | `userId=9999` | El sistema responde que el usuario no existe (404). | Pasó |
| UT-USR-07 | Actualización con un correo de otro usuario | Verificar que no se puede editar un usuario con un correo de otro. | Correo de otro usuario | El sistema rechaza la edición porque el correo pertenece a otro usuario (409). | Pasó |
| UT-USR-08 | Listado de técnicos activos por área | Verificar que el listado de técnicos por área solo incluye activos. | Área con dos técnicos activos y uno inactivo | El listado incluye solo los técnicos activos del área. | Pasó |

### Evidencias (5)

| ID | Título | Objetivo | Datos de entrada | Resultado esperado | Estado (Pasó / Falló) |
|---|---|---|---|---|---|
| UT-EVI-01 | Carga de un archivo nulo o vacío | Verificar que un archivo nulo o vacío se rechaza. | `null` o archivo de 0 bytes | El sistema rechaza el archivo nulo o vacío (422). | Pasó |
| UT-EVI-02 | Carga de un archivo con extensión no permitida | Verificar que una extensión no permitida se rechaza. | `evidencia.txt` | El sistema rechaza la extensión no permitida (422). | Pasó |
| UT-EVI-03 | Carga de un archivo con tipo MIME incoherente | Verificar que un tipo MIME incoherente se rechaza. | `.pdf` con `text/plain` | El sistema rechaza el archivo porque el tipo MIME no corresponde (422). | Pasó |
| UT-EVI-04 | Carga de un archivo con extensión en mayúsculas | Verificar que una extensión en mayúsculas se acepta. | `FOTO.PNG` | El sistema acepta la extensión en mayúsculas y procesa el archivo. | Pasó |
| UT-EVI-05 | Carga de una evidencia válida | Verificar que una evidencia válida se guarda con sus datos. | PNG o PDF válido | La evidencia se guarda con su URL, nombre, tipo y tamaño. | Pasó |

### Dashboard (5)

| ID | Título | Objetivo | Datos de entrada | Resultado esperado | Estado (Pasó / Falló) |
|---|---|---|---|---|---|
| UT-DSH-01 | Dashboard sin solicitudes | Verificar que el dashboard devuelve cero cuando no hay solicitudes. | Lista vacía | Todos los indicadores quedan en cero, sin división por cero. | Pasó |
| UT-DSH-02 | Conteo de pendientes y atendidas | Verificar que pendientes y atendidas se cuentan según el estado. | Solicitudes en distintos estados | Pendientes excluye CERRADA y atendidas cuenta EN_ATENCION, RESUELTA y CERRADA. | Pasó |
| UT-DSH-03 | Conteo de solicitudes vencidas | Verificar que solo las solicitudes abiertas con plazo vencido cuentan como vencidas. | Fecha pasada y abierta; pasada y cerrada; sin fecha | Solo cuenta la solicitud abierta con plazo vencido; no la cerrada ni la que no tiene fecha. | Pasó |
| UT-DSH-04 | Cálculo de porcentaje resuelto y tiempo promedio | Verificar el porcentaje resuelto y el tiempo promedio de atención. | Dos resueltas de cuatro; una en 10 h y otra en 20 h | El porcentaje resuelto es 50.0 % y el tiempo promedio es 15.0 h, con un decimal. | Pasó |
| UT-DSH-05 | Agrupación por categoría, prioridad y responsable | Verificar los desgloses por categoría, prioridad y responsable. | Solicitudes con categoría, prioridad y responsable | Los conteos por categoría, prioridad y responsable son correctos y están ordenados de mayor a menor. | Pasó |

### JWT (5)

| ID | Título | Objetivo | Datos de entrada | Resultado esperado | Estado (Pasó / Falló) |
|---|---|---|---|---|---|
| UT-JWT-01 | Generación de un token con claims | Verificar que el token se firma e incluye los claims esperados. | Usuario activo con id y correo | El token incluye el correo, el id, la fecha de emisión y la expiración. | Pasó |
| UT-JWT-02 | Extracción del correo desde el token | Verificar que el correo se extrae correctamente del token. | Token válido | El sistema devuelve el correo del usuario. | Pasó |
| UT-JWT-03 | Rechazo de un token expirado | Verificar que un token expirado se rechaza. | Token con expiración pasada | El sistema rechaza el token porque está expirado. | Pasó |
| UT-JWT-04 | Rechazo de un token con firma incorrecta | Verificar que un token con otra firma se rechaza. | Token firmado con otra clave | El sistema rechaza el token porque la firma no corresponde. | Pasó |
| UT-JWT-05 | Rechazo de una cadena sin formato JWT | Verificar que una cadena sin formato JWT se rechaza. | Cadena sin formato JWT | El sistema rechaza la cadena porque no tiene formato JWT. | Pasó |

### Filtro JWT (3)

| ID | Título | Objetivo | Datos de entrada | Resultado esperado | Estado (Pasó / Falló) |
|---|---|---|---|---|---|
| UT-FIL-01 | Autenticación con un token válido | Verificar que un token válido autentica al usuario con su rol. | `Bearer` con token válido | El sistema autentica al usuario y deja su rol en el contexto. | Pasó |
| UT-FIL-02 | Token inválido sin autenticación | Verificar que un token inválido no autentica y la cadena continúa. | `Bearer` con token alterado | El sistema deja el contexto vacío y continúa la cadena de filtros. | Pasó |
| UT-FIL-03 | Usuario inactivo sin autenticación | Verificar que un usuario inactivo no se autentica. | Token válido de usuario con `activo=false` | El sistema no autentica al usuario porque está inactivo. | Pasó |

### Catálogos (4)

| ID | Título | Objetivo | Datos de entrada | Resultado esperado | Estado (Pasó / Falló) |
|---|---|---|---|---|---|
| UT-CAT-01 | Registro de un área con código duplicado | Verificar que no se crea un área con un código duplicado. | Área con código existente | El sistema rechaza el área porque el código ya existe (409). | Pasó |
| UT-CAT-02 | Registro de un área con nombre duplicado | Verificar que no se crea un área con un nombre duplicado. | Área con nombre existente | El sistema rechaza el área porque el nombre ya existe (409). | Pasó |
| UT-CAT-03 | Registro de una prioridad con nivel duplicado | Verificar que no se crea una prioridad con un nivel duplicado. | Prioridad con nivel existente | El sistema rechaza la prioridad porque el nivel ya existe (409). | Pasó |
| UT-CAT-04 | Actualización de un catálogo inexistente | Verificar que actualizar un catálogo inexistente responde 404. | `id=9999` en área, categoría o prioridad | El sistema responde que el catálogo no existe (404). | Pasó |

### Código de solicitud (1)

| ID | Título | Objetivo | Datos de entrada | Resultado esperado | Estado (Pasó / Falló) |
|---|---|---|---|---|---|
| UT-SOL-17 | Consecutivo anual del código de solicitud | Verificar que el código de solicitud avanza con el consecutivo del año. | Sin solicitudes del año; luego una `SOL-2026-000001` | La primera alta genera el consecutivo 000001 y la segunda el 000002. | Pasó |

## Integración

| ID | Título | Objetivo | Datos de entrada | Resultado esperado | Estado (Pasó / Falló) |
|---|---|---|---|---|---|
| INT-01 | Listado del solicitante | Verificar que el solicitante solo ve sus propias solicitudes. | Solicitante autenticado con dos solicitudes propias y una de otro solicitante. | El listado devuelve solo las solicitudes del solicitante autenticado. | Pasó |
| INT-02 | Listado del técnico | Verificar que el técnico solo ve las solicitudes asignadas activas. | Técnico con dos asignaciones activas y una solicitud sin asignar. | El listado devuelve solo las solicitudes con asignación activa al técnico. | Pasó |
| INT-03 | Código único | Verificar el consecutivo y la restricción de unicidad del código. | Dos altas seguidas y un intento de insertar un código duplicado. | Dos altas seguidas generan códigos distintos y el índice rechaza un duplicado manual. | Pasó |
| INT-04 | Borrado de categoría en uso | Verificar la integridad referencial al borrar un catálogo. | Categoría asociada a una solicitud; `DELETE /api/categorias/{id}`. | El sistema rechaza el borrado porque la categoría está en uso (409). | Pasó |
| INT-05 | Rango de fechas | Verificar que el extremo superior del rango incluye todo el día. | Solicitudes creadas al inicio y al final del día; `desde` y `hasta` de ese día. | El rango incluye las solicitudes del extremo superior durante todo el día. | Pasó |
| INT-06 | Listado con asignaciones | Verificar que el listado no duplica filas por las asignaciones. | Solicitud con una asignación activa y una finalizada. | El listado no duplica filas aunque la solicitud tenga asignaciones. | Pasó |
| INT-07 | Filtro sin asignar | Verificar el filtro de solicitudes sin asignación activa. | Una solicitud sin asignación y otra con asignación activa; `sinAsignar=true`. | El filtro devuelve solo las solicitudes sin asignación activa. | Pasó |
| INT-08 | Dashboard con datos reales | Verificar los nueve indicadores con datos persistidos. | Solicitudes en distintos estados, categorías, prioridades y responsables. | El dashboard devuelve los nueve indicadores con sus valores. | Pasó |
| INT-09 | Subida válida a Cloudinary | Verificar la subida de una evidencia válida. | `POST /api/solicitudes/{id}/evidencias`; PNG válido; almacenamiento simulado. | La subida devuelve una URL dentro de `sigsu/evidencias`. | Pasó |
| INT-10 | PNG falso en Cloudinary | Verificar el rechazo de un archivo que no es una imagen. | `POST /api/solicitudes/{id}/evidencias`; `falso.png` con texto; almacenamiento simulado. | El sistema rechaza el PNG falso y no guarda la evidencia (502). | Pasó |
| INT-11 | Migraciones Flyway | Verificar que las migraciones se aplican sobre una base vacía. | Base MySQL 8.4 vacía; migraciones `V1` a `V3`. | Flyway aplica V1 a V3 sobre una base vacía y Hibernate valida el esquema. | Pasó |
| INT-12 | Filtro JWT con seguridad real | Verificar el filtro JWT con la cadena de seguridad completa. | `GET /api/auth/perfil` con token válido y con token inválido. | El token válido autentica la petición y el inválido responde 401. | Pasó |

## API

| ID | Título | Objetivo | Datos de entrada | Resultado esperado | Estado (Pasó / Falló) |
|---|---|---|---|---|---|
| API-01 | Petición protegida sin token | Verificar que un endpoint protegido rechaza una petición sin token. | `GET /api/solicitudes`; cabecera `Authorization` ausente. | El sistema rechaza la petición porque no hay token (401). | Pasó |
| API-02 | Petición con token malformado | Verificar que un token malformado no autentica. | `GET /api/solicitudes`; `Authorization: Bearer token-invalido`. | El sistema rechaza la petición porque el token está malformado (401). | Pasó |
| API-03 | Petición con token de otra firma | Verificar que un token firmado con otra clave no autentica. | `GET /api/solicitudes`; token firmado con otra clave. | El sistema rechaza la petición porque la firma no corresponde (401). | Pasó |
| API-04 | Petición con usuario desactivado | Verificar que un usuario desactivado no autentica. | `GET /api/solicitudes`; token de un usuario con `activo=false`. | El sistema rechaza la petición porque el usuario está desactivado (401). | Pasó |
| API-05 | Alta de usuario por un técnico | Verificar que solo el administrador puede crear usuarios. | `POST /api/usuarios`; rol `TECNICO`; cuerpo de alta válido. | El sistema rechaza el alta porque el rol no tiene permiso (403). | Pasó |
| API-06 | Clasificación por un solicitante | Verificar que solo el supervisor puede clasificar. | `PUT /api/solicitudes/{id}/clasificacion`; rol `SOLICITANTE`; categoría, prioridad y área. | El sistema rechaza la clasificación porque el rol no tiene permiso (403). | Pasó |
| API-07 | Registro de solicitud por un técnico | Verificar que solo el solicitante puede registrar solicitudes. | `POST /api/solicitudes`; rol `TECNICO`; cuerpo válido. | El sistema rechaza el registro porque el rol no tiene permiso (403). | Pasó |
| API-08 | Creación de categoría por un supervisor | Verificar que solo el administrador puede crear catálogos. | `POST /api/categorias`; rol `SUPERVISOR`; código y nombre. | El sistema rechaza la creación del catálogo porque el rol no tiene permiso (403). | Pasó |
| API-09 | Dashboard consultado por un solicitante | Verificar que el dashboard es solo para supervisor y administrador. | `GET /api/dashboard`; rol `SOLICITANTE`. | El sistema rechaza la consulta del dashboard porque el rol no tiene permiso (403). | Pasó |
| API-10 | Asignación por un solicitante | Verificar que solo el supervisor puede asignar técnicos. | `POST /api/solicitudes/{id}/asignaciones`; rol `SOLICITANTE`; técnico válido. | El sistema rechaza la asignación porque el rol no tiene permiso (403). | Pasó |
| API-11 | Eliminación de usuario por un supervisor | Verificar que solo el administrador puede eliminar usuarios. | `DELETE /api/usuarios/{id}`; rol `SUPERVISOR`. | El sistema rechaza la eliminación porque el rol no tiene permiso (403). | Pasó |
| API-12 | Listado de usuarios por un supervisor | Verificar que solo el administrador puede listar usuarios. | `GET /api/usuarios`; rol `SUPERVISOR`. | El sistema rechaza el listado porque el rol no tiene permiso (403). | Pasó |
| API-13 | Inicio de sesión público | Verificar que el login devuelve el token y la expiración. | `POST /api/auth/login`; correo y contraseña válidos. | El sistema devuelve el token, su tipo y la expiración (200). | Pasó |
| API-14 | Detalle de una solicitud inexistente | Verificar el 404 y el formato de error. | `GET /api/solicitudes/9999` y `GET /api/solicitudes/abc`; token válido. | El sistema responde que la solicitud no existe (404) o que el identificador no es válido (400), con el mismo formato de error. | Pasó |
| API-15 | Registro con cuerpo inválido | Verificar el rechazo de un cuerpo inválido. | `POST /api/solicitudes`; cuerpo vacío o mal formado; token de solicitante. | El sistema rechaza el cuerpo porque no es válido (400). | Pasó |
| API-16 | Registro con título de 151 caracteres | Verificar el límite del título. | `POST /api/solicitudes`; título de 151 caracteres; token de solicitante. | El sistema rechaza el título porque supera el límite y detalla el campo (400). | Pasó |
| API-17 | Alta de usuario con contraseña de 7 caracteres | Verificar el mínimo de la contraseña. | `POST /api/usuarios`; contraseña de 7 caracteres; token de administrador. | El sistema rechaza la contraseña porque no cumple el mínimo (400). | Pasó |
| API-18 | Listado con estado inexistente | Verificar el rechazo de un estado no válido. | `GET /api/solicitudes?estado=INVENTADO`; token válido. | El sistema rechaza el estado porque no existe (400). | Pasó |
| API-19 | Evidencia con extensión .txt | Verificar el rechazo de una extensión no permitida. | `POST /api/solicitudes/{id}/evidencias`; archivo `evidencia.txt`; token de solicitante. | El sistema rechaza la evidencia porque la extensión no está permitida (422). | Pasó |
| API-20 | Documento OpenAPI | Verificar que la documentación lista todas las operaciones. | `GET /v3/api-docs`; sin token. | El documento OpenAPI lista las 36 operaciones con su descripción. | Pasó |
