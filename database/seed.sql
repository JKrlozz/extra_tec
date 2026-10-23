-- Datos de prueba de las pantallas. Contraseña demo: itsm1234
-- SHA2 es solo de prueba; la API guarda bcrypt.
USE extra_tec;

INSERT INTO carreras (nombre) VALUES ('Ing. en Sistemas'),('Ing. Industrial'),('Ing. en Gestión');
INSERT INTO periodos (nombre, inicio, fin, activo) VALUES ('Ene/Jun 2025','2025-01-20','2025-06-30',1);

INSERT INTO usuarios (nombre, correo, contrasena_hash, rol) VALUES
 ('Administrador',   'admin@itsm.edu.mx',      SHA2('itsm1234',256),'admin'),
 ('Carlos Pérez',    'cperez@itsm.edu.mx',     SHA2('itsm1234',256),'docente'),
 ('Ana López',       'alopez@itsm.edu.mx',     SHA2('itsm1234',256),'docente'),
 ('Luis Gómez',      'lgomez@itsm.edu.mx',     SHA2('itsm1234',256),'docente'),
 ('María Hernández', 'mhernandez@itsm.edu.mx', SHA2('itsm1234',256),'alumno'),
 ('José Martínez',   'jmartinez@itsm.edu.mx',  SHA2('itsm1234',256),'alumno'),
 ('Diana Sánchez',   'dsanchez@itsm.edu.mx',   SHA2('itsm1234',256),'alumno'),
 ('Pedro García',    'pgarcia@itsm.edu.mx',    SHA2('itsm1234',256),'alumno');

INSERT INTO docentes (id_usuario, especialidad, telefono)
SELECT id, 'Deportes', '2351000000' FROM usuarios WHERE rol = 'docente';

INSERT INTO alumnos (id_usuario, matricula, id_carrera, semestre)
SELECT u.id, m.mat, 1, 7
FROM usuarios u
JOIN (SELECT 'mhernandez@itsm.edu.mx' AS c, '2024021' AS mat
      UNION ALL SELECT 'jmartinez@itsm.edu.mx', '2024022'
      UNION ALL SELECT 'dsanchez@itsm.edu.mx',  '2024023'
      UNION ALL SELECT 'pgarcia@itsm.edu.mx',   '2024024') m ON m.c = u.correo;

INSERT INTO actividades (nombre, cupo, id_docente, id_periodo)
SELECT t.n, t.c, u.id, 1
FROM (SELECT 'Fútbol' AS n, 25 AS c, 'cperez@itsm.edu.mx' AS d
      UNION ALL SELECT 'Ajedrez', 20, 'alopez@itsm.edu.mx'
      UNION ALL SELECT 'Voleibol', 25, 'lgomez@itsm.edu.mx'
      UNION ALL SELECT 'Baloncesto', 25, 'cperez@itsm.edu.mx'
      UNION ALL SELECT 'Banda de guerra', 30, 'lgomez@itsm.edu.mx') t
JOIN usuarios u ON u.correo = t.d;

INSERT INTO horarios (id_actividad, dia_semana, hora_inicio, hora_fin)
SELECT a.id, h.d, h.i, h.f
FROM actividades a
JOIN (SELECT 'Fútbol' AS n, 'Lunes' AS d, '16:00' AS i, '18:00' AS f
      UNION ALL SELECT 'Fútbol', 'Miércoles', '16:00', '18:00'
      UNION ALL SELECT 'Ajedrez', 'Martes', '15:00', '17:00'
      UNION ALL SELECT 'Voleibol', 'Jueves', '17:00', '19:00'
      UNION ALL SELECT 'Baloncesto', 'Viernes', '17:00', '19:00'
      UNION ALL SELECT 'Banda de guerra', 'Lunes', '18:00', '20:00') h ON h.n = a.nombre;

-- Todos en Fútbol; María además en Banda de guerra
INSERT INTO inscripciones (id_alumno, id_actividad)
SELECT al.id_usuario, a.id FROM alumnos al JOIN actividades a ON a.nombre = 'Fútbol';
INSERT INTO inscripciones (id_alumno, id_actividad)
SELECT u.id, a.id FROM usuarios u JOIN actividades a ON a.nombre = 'Banda de guerra'
WHERE u.correo = 'mhernandez@itsm.edu.mx';

-- 2 sesiones de Fútbol; Pedro faltó a la segunda
INSERT INTO sesiones (id_actividad, fecha)
SELECT id, '2025-03-03' FROM actividades WHERE nombre = 'Fútbol';
INSERT INTO sesiones (id_actividad, fecha)
SELECT id, '2025-03-05' FROM actividades WHERE nombre = 'Fútbol';
INSERT INTO asistencias (id_sesion, id_alumno, presente)
SELECT s.id, al.id_usuario, NOT (al.matricula = '2024024' AND s.fecha = '2025-03-05')
FROM sesiones s JOIN alumnos al;
