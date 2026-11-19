// semestre en curso: fechas, vigencia y archivo al terminar
const { consulta, transaccion } = require('./db');
const { ErrorApi, hoy, AHORA_SQL } = require('./util');

const CAMPOS = `id, CONVERT(VARCHAR(10), inicio, 23) AS inicio, CONVERT(VARCHAR(10), fin, 23) AS fin`;

// 2026-08-17 -> 17/08/2026
const legible = (iso) => iso.split('-').reverse().join('/');

async function periodoActivo(q = consulta) {
  const filas = await q(`SELECT ${CAMPOS} FROM periodos WHERE activo = 1`);
  return filas[0] || null;
}

// los registros nuevos solo se hacen con el semestre en curso
// idActividad es opcional: revisa que la actividad sea de ese semestre
async function exigirVigente(q = consulta, idActividad = null) {
  const periodo = await periodoActivo(q);
  if (!periodo) throw new ErrorApi('No hay un semestre activo. El administrador debe iniciar uno.');
  if (hoy() < periodo.inicio) throw new ErrorApi(`El semestre inicia el ${legible(periodo.inicio)}.`);
  if (idActividad !== null) {
    const act = await q('SELECT id_periodo FROM actividades WHERE id = @id', { id: idActividad });
    if (act.length === 0 || act[0].id_periodo !== periodo.id) {
      throw new ErrorApi('Esa actividad pertenece a un semestre archivado.');
    }
  }
  return periodo;
}

// lo registrado queda guardado pero fuera de las listas
// las inscripciones quedan como archivadas y las actividades se desactivan
async function archivar(q, idPeriodo) {
  await q(
    `UPDATE inscripciones SET estado = 'archivada'
      WHERE estado = 'activa' AND id_actividad IN (SELECT id FROM actividades WHERE id_periodo = @p)`,
    { p: idPeriodo },
  );
  await q(
    `UPDATE actividades SET activa = 0, archivada_en = ${AHORA_SQL} WHERE id_periodo = @p AND activa = 1`,
    { p: idPeriodo },
  );
  await q(`UPDATE periodos SET activo = 0, cerrado_en = ${AHORA_SQL} WHERE id = @p`, { p: idPeriodo });
}

// cierra el semestre activo (a mano)
async function cerrarActivo() {
  return transaccion(async (q) => {
    const p = await q('SELECT id FROM periodos WITH (UPDLOCK, HOLDLOCK) WHERE activo = 1');
    if (p.length === 0) throw new ErrorApi('No hay un semestre activo.');
    await archivar(q, p[0].id);
  });
}

// si la fecha final ya paso, archiva solo
let ultimaRevision = 0;
async function archivarVencido(forzar = false) {
  const ahora = Date.now();
  if (!forzar && ahora - ultimaRevision < 30000) return;
  ultimaRevision = ahora;
  await transaccion(async (q) => {
    const p = await q(
      'SELECT id FROM periodos WITH (UPDLOCK, HOLDLOCK) WHERE activo = 1 AND fin < @hoy',
      { hoy: hoy() },
    );
    if (p.length > 0) await archivar(q, p[0].id);
  });
}

// semestre nuevo con las actividades del ultimo semestre archivado y sin alumnos
async function iniciar(inicio, fin) {
  return transaccion(async (q) => {
    const activo = await q('SELECT id FROM periodos WITH (UPDLOCK, HOLDLOCK) WHERE activo = 1');
    if (activo.length > 0) throw new ErrorApi('Ya hay un semestre activo.');
    const ultimo = await q('SELECT TOP 1 id FROM periodos ORDER BY id DESC');
    const nuevo = (await q(
      'INSERT INTO periodos (inicio, fin, activo) OUTPUT INSERTED.id VALUES (@inicio, @fin, 1)', { inicio, fin },
    ))[0].id;
    let copiadas = 0;
    if (ultimo.length > 0) {
      const anteriores = await q(
        `SELECT a.id, a.nombre, a.descripcion, a.cupo,
                CASE WHEN u.activo = 1 THEN a.id_docente ELSE NULL END AS id_docente
           FROM actividades a LEFT JOIN usuarios u ON u.id = a.id_docente
          WHERE a.id_periodo = @p AND a.archivada_en IS NOT NULL`,
        { p: ultimo[0].id },
      );
      for (const a of anteriores) {
        const copia = (await q(
          `INSERT INTO actividades (nombre, descripcion, cupo, id_docente, id_periodo)
           OUTPUT INSERTED.id VALUES (@nombre, @descripcion, @cupo, @docente, @periodo)`,
          { nombre: a.nombre, descripcion: a.descripcion, cupo: a.cupo, docente: a.id_docente, periodo: nuevo },
        ))[0].id;
        await q(
          `INSERT INTO horarios (id_actividad, id_dia, hora_inicio, hora_fin, id_lugar)
           SELECT @nueva, id_dia, hora_inicio, hora_fin, id_lugar FROM horarios WHERE id_actividad = @vieja`,
          { nueva: copia, vieja: a.id },
        );
        copiadas++;
      }
    }
    return copiadas;
  });
}

module.exports = { legible, periodoActivo, exigirVigente, archivarVencido, cerrarActivo, iniciar, CAMPOS };
