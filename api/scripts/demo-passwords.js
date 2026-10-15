// seed.sql guarda SHA2 de prueba; este script los cambia por bcrypt (contraseña demo: itsm1234)
const bcrypt = require('bcryptjs');
const db = require('../src/db');

(async () => {
  const hash = await bcrypt.hash('itsm1234', 10);
  const [r] = await db.query('UPDATE usuarios SET contrasena_hash = ?', [hash]);
  console.log(`Contraseñas actualizadas a bcrypt en ${r.affectedRows} usuarios.`);
  await db.end();
})().catch((e) => {
  console.error(e.message);
  process.exit(1);
});
