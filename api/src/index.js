// API de ITSM Extraescolares
try { process.loadEnvFile(); } catch (_) { /* en Azure las variables ya existen */ }
const express = require('express');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const multer = require('multer');
const { consulta, mensajeSql } = require('./db');
const { exigirSesion } = require('./auth');
const { ErrorApi, ruta } = require('./util');
const { archivarVencido } = require('./semestre');

function crearApp() {
  const app = express();
  app.disable('x-powered-by');
  // Azure pone un proxy delante
  app.set('trust proxy', 1);
  app.use(helmet());
  app.use(express.json({ limit: '200kb' }));

  const api = express.Router();

  // despierta la base gratuita y confirma que responde
  api.get('/salud', ruta(async (req, res) => {
    await consulta('SELECT 1 AS ok');
    res.json({ ok: true });
  }));

  // limita intentos de adivinar contraseñas
  const limiteLogin = rateLimit({
    windowMs: 15 * 60 * 1000,
    limit: 30,
    standardHeaders: true,
    legacyHeaders: false,
    message: { error: 'Demasiados intentos. Intenta de nuevo en unos minutos.' },
  });
  api.use('/login', limiteLogin);
  api.use('/registro', rateLimit({ windowMs: 60 * 60 * 1000, limit: 20, message: { error: 'Demasiados registros desde este dispositivo.' } }));
  api.use(require('./routes/sesion'));

  api.use(exigirSesion);
  // si termino el semestre, sus registros se archivan solos
  api.use(ruta(async (req, res, next) => { await archivarVencido(); next(); }));
  api.use(require('./routes/actividades'));
  api.use(require('./routes/asistencia'));
  api.use(require('./routes/evidencias'));
  api.use(require('./routes/personas'));
  api.use(require('./routes/reportes'));
  api.use(require('./routes/semestre'));

  app.use('/api', api);
  app.use((req, res) => res.status(404).json({ error: 'Ruta no encontrada.' }));

  // todos los errores salen como { error: mensaje }
  // eslint-disable-next-line no-unused-vars
  app.use((e, req, res, next) => {
    if (e instanceof ErrorApi) return res.status(e.estado).json({ error: e.message });
    if (e instanceof multer.MulterError) {
      const texto = e.code === 'LIMIT_FILE_SIZE' ? 'Una fotografía supera 8 MB.' : 'Puedes guardar hasta 10 fotografías por evidencia.';
      return res.status(400).json({ error: texto });
    }
    if (e && e.type === 'entity.parse.failed') return res.status(400).json({ error: 'Los datos enviados no son válidos.' });
    if (e && e.type === 'entity.too.large') return res.status(413).json({ error: 'Los datos enviados son demasiado grandes.' });
    if (e && ['ESOCKET', 'ELOGIN', 'ETIMEOUT', 'ECONNCLOSED', 'ENOTOPEN', 'ECONNRESET'].includes(e.code)) {
      return res.status(503).json({ error: 'No se pudo conectar con la base de datos. Intenta de nuevo en un minuto.' });
    }
    const claro = mensajeSql(e);
    if (claro) return res.status(409).json({ error: claro });
    console.error(e);
    return res.status(500).json({ error: 'No se pudo completar la operación.' });
  });
  return app;
}

if (require.main === module) {
  const puerto = process.env.PORT || process.env.WEBSITES_PORT || 3000;
  crearApp().listen(puerto, () => console.log(`API lista en el puerto ${puerto}`));
}

module.exports = { crearApp };
