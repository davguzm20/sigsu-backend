-- Semillero de áreas responsables y categorías.
-- Las dos columnas de auditoría son NOT NULL y no llevan DEFAULT, así que van con NOW().

INSERT INTO areas (codigo, nombre, descripcion, activo, creado_en, actualizado_en) VALUES
('SISTEMAS', 'Sistemas', 'Soporte de software, equipos y conectividad', TRUE, NOW(), NOW()),
('MANTENIMIENTO', 'Mantenimiento', 'Electricidad, plomería y obra civil', TRUE, NOW(), NOW()),
('SERVICIOS_GENERALES', 'Servicios generales', 'Mobiliario, aseo y espacios comunes', TRUE, NOW(), NOW()),
('LABORATORIOS', 'Laboratorios', 'Equipos y recursos de laboratorio', TRUE, NOW(), NOW()),
('BIBLIOTECA', 'Biblioteca', 'Servicios bibliotecarios', TRUE, NOW(), NOW());

INSERT INTO categorias (codigo, nombre, descripcion, area_predeterminada_id, activo, creado_en, actualizado_en)
SELECT 'SOFTWARE', 'Software', 'Incidencias de aplicaciones y programas', a.id, TRUE, NOW(), NOW()
FROM areas a WHERE a.codigo = 'SISTEMAS';

INSERT INTO categorias (codigo, nombre, descripcion, area_predeterminada_id, activo, creado_en, actualizado_en)
SELECT 'REDES', 'Redes', 'Conectividad, cableado y accesos de red', a.id, TRUE, NOW(), NOW()
FROM areas a WHERE a.codigo = 'SISTEMAS';

INSERT INTO categorias (codigo, nombre, descripcion, area_predeterminada_id, activo, creado_en, actualizado_en)
SELECT 'EQUIPOS', 'Equipos de cómputo', 'Fallas de computadores e impresoras', a.id, TRUE, NOW(), NOW()
FROM areas a WHERE a.codigo = 'SISTEMAS';

INSERT INTO categorias (codigo, nombre, descripcion, area_predeterminada_id, activo, creado_en, actualizado_en)
SELECT 'ELECTRICIDAD', 'Electricidad', 'Fallas eléctricas, luminarias y tomas', a.id, TRUE, NOW(), NOW()
FROM areas a WHERE a.codigo = 'MANTENIMIENTO';

INSERT INTO categorias (codigo, nombre, descripcion, area_predeterminada_id, activo, creado_en, actualizado_en)
SELECT 'PLOMERIA', 'Plomería', 'Fugas de agua, sanitarios y grifería', a.id, TRUE, NOW(), NOW()
FROM areas a WHERE a.codigo = 'MANTENIMIENTO';

INSERT INTO categorias (codigo, nombre, descripcion, area_predeterminada_id, activo, creado_en, actualizado_en)
SELECT 'MOBILIARIO', 'Mobiliario', 'Pupitres, mesas, sillas y puertas', a.id, TRUE, NOW(), NOW()
FROM areas a WHERE a.codigo = 'SERVICIOS_GENERALES';

INSERT INTO categorias (codigo, nombre, descripcion, area_predeterminada_id, activo, creado_en, actualizado_en)
SELECT 'LIMPIEZA', 'Limpieza', 'Aseo de aulas y espacios comunes', a.id, TRUE, NOW(), NOW()
FROM areas a WHERE a.codigo = 'SERVICIOS_GENERALES';

INSERT INTO categorias (codigo, nombre, descripcion, area_predeterminada_id, activo, creado_en, actualizado_en)
SELECT 'EQUIPOS_LABORATORIO', 'Equipos de laboratorio', 'Fallas de equipos y recursos de laboratorio', a.id, TRUE, NOW(), NOW()
FROM areas a WHERE a.codigo = 'LABORATORIOS';

INSERT INTO categorias (codigo, nombre, descripcion, area_predeterminada_id, activo, creado_en, actualizado_en)
SELECT 'SERVICIOS_BIBLIOTECA', 'Servicios de biblioteca', 'Préstamos, catálogo y puestos de estudio', a.id, TRUE, NOW(), NOW()
FROM areas a WHERE a.codigo = 'BIBLIOTECA';
