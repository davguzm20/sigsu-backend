CREATE TABLE areas (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(30) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(150) NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en DATETIME NOT NULL,
    actualizado_en DATETIME NOT NULL,
    CONSTRAINT pk_areas PRIMARY KEY (id),
    CONSTRAINT uq_areas_codigo UNIQUE (codigo),
    CONSTRAINT uq_areas_nombre UNIQUE (nombre)
);

CREATE TABLE usuarios (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    correo VARCHAR(250) NOT NULL,
    password_hash VARCHAR(250) NOT NULL,
    rol VARCHAR(20) NOT NULL,
    area_id INT UNSIGNED NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en DATETIME NOT NULL,
    actualizado_en DATETIME NOT NULL,
    CONSTRAINT pk_usuarios PRIMARY KEY (id),
    CONSTRAINT uq_usuarios_correo UNIQUE (correo),
    CONSTRAINT fk_usuarios_area FOREIGN KEY (area_id) REFERENCES areas(id),
    INDEX idx_usuarios_rol (rol),
    INDEX idx_usuarios_area (area_id)
);

CREATE TABLE categorias (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(30) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(150) NULL,
    area_predeterminada_id INT UNSIGNED NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en DATETIME NOT NULL,
    actualizado_en DATETIME NOT NULL,
    CONSTRAINT pk_categorias PRIMARY KEY (id),
    CONSTRAINT uq_categorias_codigo UNIQUE (codigo),
    CONSTRAINT uq_categorias_nombre UNIQUE (nombre),
    CONSTRAINT fk_categorias_area FOREIGN KEY (area_predeterminada_id) REFERENCES areas(id),
    INDEX idx_categorias_area (area_predeterminada_id)
);

CREATE TABLE prioridades (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(30) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    nivel TINYINT UNSIGNED NOT NULL,
    descripcion VARCHAR(150) NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en DATETIME NOT NULL,
    actualizado_en DATETIME NOT NULL,
    CONSTRAINT pk_prioridades PRIMARY KEY (id),
    CONSTRAINT uq_prioridades_codigo UNIQUE (codigo),
    CONSTRAINT uq_prioridades_nombre UNIQUE (nombre),
    CONSTRAINT uq_prioridades_nivel UNIQUE (nivel),
    CONSTRAINT ck_prioridades_nivel CHECK (nivel > 0)
);

CREATE TABLE solicitudes (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(20) NOT NULL,
    solicitante_id INT UNSIGNED NOT NULL,
    categoria_id INT UNSIGNED NULL,
    prioridad_id INT UNSIGNED NULL,
    estado VARCHAR(20) NOT NULL,
    area_id INT UNSIGNED NULL,
    titulo VARCHAR(150) NOT NULL,
    descripcion TEXT NOT NULL,
    ubicacion VARCHAR(150) NOT NULL,
    creado_en DATETIME NOT NULL,
    actualizado_en DATETIME NOT NULL,
    fecha_limite DATETIME NULL,
    resuelta_en DATETIME NULL,
    cerrada_en DATETIME NULL,
    solucion TEXT NULL,
    CONSTRAINT pk_solicitudes PRIMARY KEY (id),
    CONSTRAINT uq_solicitudes_codigo UNIQUE (codigo),
    CONSTRAINT fk_solicitudes_solicitante FOREIGN KEY (solicitante_id) REFERENCES usuarios(id),
    CONSTRAINT fk_solicitudes_categoria FOREIGN KEY (categoria_id) REFERENCES categorias(id),
    CONSTRAINT fk_solicitudes_prioridad FOREIGN KEY (prioridad_id) REFERENCES prioridades(id),
    CONSTRAINT fk_solicitudes_area FOREIGN KEY (area_id) REFERENCES areas(id),
    CONSTRAINT ck_solicitudes_fecha_limite CHECK (fecha_limite IS NULL OR fecha_limite > creado_en),
    CONSTRAINT ck_solicitudes_resuelta CHECK (resuelta_en IS NULL OR resuelta_en >= creado_en),
    CONSTRAINT ck_solicitudes_cerrada CHECK (cerrada_en IS NULL OR cerrada_en >= creado_en),
    INDEX idx_solicitudes_estado_fecha (estado, creado_en),
    INDEX idx_solicitudes_solicitante_fecha (solicitante_id, creado_en),
    INDEX idx_solicitudes_categoria (categoria_id),
    INDEX idx_solicitudes_prioridad (prioridad_id),
    INDEX idx_solicitudes_area (area_id),
    INDEX idx_solicitudes_fecha_limite (fecha_limite)
);

CREATE TABLE asignaciones (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    solicitud_id INT UNSIGNED NOT NULL,
    tecnico_id INT UNSIGNED NOT NULL,
    asignado_por_id INT UNSIGNED NOT NULL,
    asignada_en DATETIME NOT NULL,
    finalizada_en DATETIME NULL,
    CONSTRAINT pk_asignaciones PRIMARY KEY (id),
    CONSTRAINT fk_asignaciones_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitudes(id),
    CONSTRAINT fk_asignaciones_tecnico FOREIGN KEY (tecnico_id) REFERENCES usuarios(id),
    CONSTRAINT fk_asignaciones_asignado_por FOREIGN KEY (asignado_por_id) REFERENCES usuarios(id),
    CONSTRAINT ck_asignaciones_finalizada CHECK (finalizada_en IS NULL OR finalizada_en >= asignada_en),
    INDEX idx_asignaciones_solicitud_activa (solicitud_id, finalizada_en),
    INDEX idx_asignaciones_tecnico_activa (tecnico_id, finalizada_en),
    INDEX idx_asignaciones_solicitud_fecha (solicitud_id, asignada_en),
    INDEX idx_asignaciones_asignado_por (asignado_por_id)
);

CREATE TABLE seguimientos (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    solicitud_id INT UNSIGNED NOT NULL,
    creado_por_id INT UNSIGNED NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    contenido TEXT NOT NULL,
    creado_en DATETIME NOT NULL,
    CONSTRAINT pk_seguimientos PRIMARY KEY (id),
    CONSTRAINT fk_seguimientos_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitudes(id),
    CONSTRAINT fk_seguimientos_creado_por FOREIGN KEY (creado_por_id) REFERENCES usuarios(id),
    INDEX idx_seguimientos_solicitud_fecha (solicitud_id, creado_en),
    INDEX idx_seguimientos_creado_por (creado_por_id)
);

CREATE TABLE evidencias (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    solicitud_id INT UNSIGNED NOT NULL,
    seguimiento_id INT UNSIGNED NULL,
    cargado_por_id INT UNSIGNED NOT NULL,
    nombre_archivo VARCHAR(250) NOT NULL,
    tipo_mime VARCHAR(100) NULL,
    tamano_bytes BIGINT UNSIGNED NULL,
    archivo_url VARCHAR(1000) NOT NULL,
    creado_en DATETIME NOT NULL,
    CONSTRAINT pk_evidencias PRIMARY KEY (id),
    CONSTRAINT fk_evidencias_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitudes(id),
    CONSTRAINT fk_evidencias_seguimiento FOREIGN KEY (seguimiento_id) REFERENCES seguimientos(id),
    CONSTRAINT fk_evidencias_cargado_por FOREIGN KEY (cargado_por_id) REFERENCES usuarios(id),
    INDEX idx_evidencias_solicitud (solicitud_id),
    INDEX idx_evidencias_seguimiento (seguimiento_id),
    INDEX idx_evidencias_cargado_por (cargado_por_id)
);

CREATE TABLE historial_estados (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    solicitud_id INT UNSIGNED NOT NULL,
    estado_origen VARCHAR(20) NULL,
    estado_destino VARCHAR(20) NOT NULL,
    cambiado_por_id INT UNSIGNED NOT NULL,
    cambiado_en DATETIME NOT NULL,
    observacion VARCHAR(250) NULL,
    CONSTRAINT pk_historial_estados PRIMARY KEY (id),
    CONSTRAINT fk_historial_estados_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitudes(id),
    CONSTRAINT fk_historial_estados_cambiado_por FOREIGN KEY (cambiado_por_id) REFERENCES usuarios(id),
    INDEX idx_historial_estados_solicitud_fecha (solicitud_id, cambiado_en),
    INDEX idx_historial_estados_cambiado_por (cambiado_por_id)
);
