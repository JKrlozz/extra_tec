-- Datos de prueba
-- Las claves de prueba usan SHA2
USE extra_tec;

INSERT INTO carreras (nombre) VALUES
  ('Ingeniería en Sistemas Computacionales'),
  ('Ingeniería Industrial'),
  ('Ingeniería en Gestión Empresarial'),
  ('Ingeniería Bioquímica'),
  ('Ingeniería Electromecánica'),
  ('Ingeniería Civil'),
  ('Ingeniería en Tecnologías de la Información y Comunicaciones'),
  ('Ingeniería Ambiental'),
  ('Ingeniería Petrolera'),
  ('Licenciatura en Gastronomía');

INSERT INTO periodos (inicio, fin, activo) VALUES ('2025-01-20','2025-06-30',1);

INSERT INTO dias_semana (id, nombre, orden) VALUES
  (1,'Lunes',1),(2,'Martes',2),(3,'Miércoles',3),(4,'Jueves',4),(5,'Viernes',5),(6,'Sábado',6);

INSERT INTO lugares (nombre) VALUES
  ('ITSM'),
  ('Cancha de fútbol'),
  ('Cancha de básquetbol'),
  ('Gimnasio'),
  ('Salón de usos múltiples'),
  ('Patio central');

INSERT INTO usuarios (nombres, apellido_paterno, apellido_materno, correo, contrasena_hash, rol) VALUES
  ('Administrador',   'ITSM',     NULL,        'admin@itsm.edu.mx',      SHA2('itsm1234',256),'admin'),
  ('Carlos',          'Pérez',    'Domínguez', 'cperez@itsm.edu.mx',     SHA2('itsm1234',256),'docente'),
  ('Ana',             'López',    'Hernández', 'alopez@itsm.edu.mx',     SHA2('itsm1234',256),'docente'),
  ('Luis',            'Gómez',    'Ramos',     'lgomez@itsm.edu.mx',     SHA2('itsm1234',256),'docente'),
  ('María',           'Hernández', 'Cruz',     'mhernandez@itsm.edu.mx', SHA2('itsm1234',256),'alumno'),
  ('José',            'Martínez', 'Luna',      'jmartinez@itsm.edu.mx',  SHA2('itsm1234',256),'alumno'),
  ('Diana',           'Sánchez',  'Rojas',     'dsanchez@itsm.edu.mx',   SHA2('itsm1234',256),'alumno'),
  ('Pedro',           'García',   'Mendoza',   'pgarcia@itsm.edu.mx',    SHA2('itsm1234',256),'alumno');

INSERT INTO docentes (id_usuario, especialidad, telefono)
SELECT id, 'Deportes', '2351000000' FROM usuarios WHERE rol = 'docente';

INSERT INTO alumnos (id_usuario, matricula, id_carrera, semestre)
SELECT u.id, m.mat, 1, 7
FROM usuarios u
JOIN (SELECT 'mhernandez@itsm.edu.mx' AS c, '232T0182' AS mat
      UNION ALL SELECT 'jmartinez@itsm.edu.mx', '219T0382'
      UNION ALL SELECT 'dsanchez@itsm.edu.mx',  '232T0183'
      UNION ALL SELECT 'pgarcia@itsm.edu.mx',   '219T0383') m ON m.c = u.correo;

INSERT INTO actividades (nombre, cupo, id_docente, id_periodo)
SELECT t.n, t.c, u.id, 1
FROM (SELECT 'Fútbol' AS n, 25 AS c, 'cperez@itsm.edu.mx' AS d
      UNION ALL SELECT 'Ajedrez', 20, 'alopez@itsm.edu.mx'
      UNION ALL SELECT 'Voleibol', 25, 'lgomez@itsm.edu.mx'
      UNION ALL SELECT 'Baloncesto', 25, 'cperez@itsm.edu.mx'
      UNION ALL SELECT 'Banda de guerra', 30, 'lgomez@itsm.edu.mx') t
JOIN usuarios u ON u.correo = t.d;

-- El dia y el lugar se guardan como claves
INSERT INTO horarios (id_actividad, id_dia, hora_inicio, hora_fin, id_lugar)
SELECT a.id, d.id, h.i, h.f, l.id
FROM actividades a
JOIN (SELECT 'Fútbol' AS n, 'Lunes' AS d, '16:00' AS i, '18:00' AS f, 'Cancha de fútbol' AS l
      UNION ALL SELECT 'Fútbol', 'Miércoles', '16:00', '18:00', 'Cancha de fútbol'
      UNION ALL SELECT 'Ajedrez', 'Martes', '15:00', '17:00', 'Salón de usos múltiples'
      UNION ALL SELECT 'Voleibol', 'Jueves', '17:00', '19:00', 'Gimnasio'
      UNION ALL SELECT 'Baloncesto', 'Viernes', '17:00', '19:00', 'Cancha de básquetbol'
      UNION ALL SELECT 'Banda de guerra', 'Lunes', '18:00', '20:00', 'Patio central') h ON h.n = a.nombre
JOIN dias_semana d ON d.nombre = h.d
JOIN lugares l ON l.nombre = h.l;

-- Cada alumno queda inscrito en una sola actividad
INSERT INTO inscripciones (id_alumno, id_actividad)
SELECT al.id_usuario, a.id FROM alumnos al JOIN actividades a ON a.nombre = 'Fútbol';

-- Dos sesiones de futbol y una falta
INSERT INTO sesiones (id_actividad, fecha)
SELECT id, '2025-03-03' FROM actividades WHERE nombre = 'Fútbol';
INSERT INTO sesiones (id_actividad, fecha)
SELECT id, '2025-03-05' FROM actividades WHERE nombre = 'Fútbol';
INSERT INTO asistencias (id_sesion, id_alumno, presente)
SELECT s.id, al.id_usuario, NOT (al.matricula = '219T0383' AND s.fecha = '2025-03-05')
FROM sesiones s JOIN alumnos al;
