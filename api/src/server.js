require('dotenv').config();
const path = require('path');
const fs = require('fs');
const express = require('express');
const cors = require('cors');

if (!process.env.JWT_SECRET) {
  console.error('Falta JWT_SECRET en api/.env. Ejecuta instalar.bat para generarlo');
  process.exit(1);
}

const uploads = path.join(__dirname, '..', 'uploads');
fs.mkdirSync(uploads, { recursive: true });

const app = express();
app.use(cors());
app.use(express.json());
app.use('/uploads', express.static(uploads));

app.get('/api/salud', (req, res) => res.json({ ok: true, servicio: 'extra_tec' }));
app.use('/api', require('./routes/auth'));
app.use('/api', require('./routes/personas'));
app.use('/api', require('./routes/actividades'));
app.use('/api', require('./routes/asistencia'));
app.use('/api', require('./routes/constancias'));
app.use('/api', require('./routes/reportes'));

app.use((req, res) => res.status(404).json({ error: 'Ruta no encontrada' }));

app.use((err, req, res, next) => {
  if (err.status) return res.status(err.status).json({ error: err.message });
  if (err.code === 'ER_DUP_ENTRY') return res.status(409).json({ error: 'Ya existe un registro con esos datos' });
  if (err.sqlState === '45000') return res.status(409).json({ error: err.sqlMessage });
  if (err.code === 'ER_NO_REFERENCED_ROW_2') return res.status(400).json({ error: 'Referencia inválida' });
  console.error(err);
  res.status(500).json({ error: 'Error interno del servidor' });
});

const port = Number(process.env.PORT || 3000);
app.listen(port, '0.0.0.0', () => console.log(`API extra_tec escuchando en http://localhost:${port}`));
