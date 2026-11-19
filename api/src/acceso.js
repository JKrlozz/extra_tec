// permisos sobre una actividad
const { consulta } = require('./db');
const { ErrorApi } = require('./util');

// la actividad debe existir; el docente solo ve las suyas; el alumno no entra
// regresa el id del docente titular (puede ser null)
async function exigirTitular(req, idActividad, q) {
  if (req.usuario.rol !== 'docente' && req.usuario.rol !== 'admin') {
    throw new ErrorApi('No tienes permiso para esta acción.', 403);
  }
  const ejecutar = q || consulta;
  const filas = await ejecutar('SELECT id_docente FROM actividades WHERE id = @id AND activa = 1', { id: idActividad });
  if (filas.length === 0) throw new ErrorApi('Actividad no encontrada.', 404);
  const docente = filas[0].id_docente;
  if (req.usuario.rol === 'docente' && docente !== req.usuario.id) {
    throw new ErrorApi('No eres el docente de esta actividad.', 403);
  }
  return docente;
}

module.exports = { exigirTitular };
