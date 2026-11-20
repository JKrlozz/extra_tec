// prepara la base para archivar semestres
// no borra datos y se repite
// uso npm run migrar
try {
  process.loadEnvFile();
} catch (e) {
  // variables ya definidas
}
const { obtener } = require('../src/db');

const pasos = [
  ['periodos.cerrado_en',
    "IF COL_LENGTH('periodos', 'cerrado_en') IS NULL ALTER TABLE periodos ADD cerrado_en DATETIME2(0) NULL"],
  ['actividades.archivada_en',
    "IF COL_LENGTH('actividades', 'archivada_en') IS NULL ALTER TABLE actividades ADD archivada_en DATETIME2(0) NULL"],
  ['inscripciones.estado admite archivada',
    `IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('inscripciones') AND name = 'estado' AND max_length < 9)
        OR EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'ck_inscripciones_estado' AND definition NOT LIKE '%archivada%')
     BEGIN
       -- estos dependen de la columna
       IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_inscripcion_activa' AND object_id = OBJECT_ID('inscripciones'))
         DROP INDEX ux_inscripcion_activa ON inscripciones;
       ALTER TABLE inscripciones DROP CONSTRAINT ck_inscripciones_estado;
       ALTER TABLE inscripciones DROP CONSTRAINT df_inscripciones_estado;
       ALTER TABLE inscripciones ALTER COLUMN estado VARCHAR(9) NOT NULL;
       ALTER TABLE inscripciones ADD CONSTRAINT df_inscripciones_estado DEFAULT 'activa' FOR estado;
       ALTER TABLE inscripciones ADD CONSTRAINT ck_inscripciones_estado CHECK (estado IN ('activa', 'baja', 'archivada'));
       CREATE UNIQUE INDEX ux_inscripcion_activa ON inscripciones (id_alumno) WHERE estado = 'activa';
     END`],
  ['vista v_asistencia_alumno',
    `CREATE OR ALTER VIEW v_asistencia_alumno AS
     SELECT i.id AS id_inscripcion, i.id_alumno, i.id_actividad,
            COUNT(s.id) AS sesiones,
            COALESCE(SUM(CAST(asi.presente AS INT)), 0) AS asistencias,
            ROUND(100.0 * COALESCE(SUM(CAST(asi.presente AS INT)), 0) / NULLIF(COUNT(s.id), 0), 2) AS porcentaje
     FROM inscripciones i
     LEFT JOIN sesiones s      ON s.id_actividad = i.id_actividad
     LEFT JOIN asistencias asi ON asi.id_sesion = s.id AND asi.id_alumno = i.id_alumno
     WHERE i.estado IN ('activa', 'archivada')
     GROUP BY i.id, i.id_alumno, i.id_actividad`],
];

async function principal() {
  const pool = await obtener();
  for (let i = 0; i < pasos.length; i++) {
    const nombre = pasos[i][0];
    const sentencia = pasos[i][1];
    await pool.request().batch(sentencia);
    console.log(`Listo: ${nombre}`);
  }
  await pool.close();
  console.log('Migración terminada.');
}

principal().catch(function (e) {
  console.error('Falló:', e.message);
  process.exit(1);
});
