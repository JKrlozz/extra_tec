// permisos sobre una actividad
const db = require('./db');
const util = require('./util');

// revisa que el docente sea titular
async function exigirTitular(req, idActividad, q) {
  const rol = req.usuario.rol;
  if (rol !== 'docente' && rol !== 'admin') {
    throw new util.ErrorApi('No tienes permiso para esta acción.', 403);
  }

  // usa la transaccion si viene
  let ejecutar = db.consulta;
  if (q) {
    ejecutar = q;
  }

  const filas = await ejecutar('SELECT id_docente FROM actividades WHERE id = @id AND activa = 1', { id: idActividad });
  if (filas.length === 0) {
    throw new util.ErrorApi('Actividad no encontrada.', 404);
  }

  const docente = filas[0].id_docente;
  if (rol === 'docente' && docente !== req.usuario.id) {
    throw new util.ErrorApi('No eres el docente de esta actividad.', 403);
  }
  // puede regresar null
  return docente;
}

module.exports = { exigirTitular };
