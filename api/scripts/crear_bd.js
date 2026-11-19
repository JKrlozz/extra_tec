// Crea las tablas en Azure SQL y carga los datos de prueba.
// Uso: npm run bd        (borra todo y lo crea de nuevo)
try { process.loadEnvFile(); } catch (_) { /* variables ya definidas */ }
const fs = require('fs');
const path = require('path');
const { sql, obtener, transaccion } = require('../src/db');
const { generarHash } = require('../src/auth');

const CLAVE_DEMO = 'itsm1234';

async function esquema(pool) {
  const archivo = path.join(__dirname, '..', '..', 'database', 'azure', 'schema.sql');
  const lotes = fs.readFileSync(archivo, 'utf8').split(/^\s*GO\s*$/im).map((l) => l.trim()).filter(Boolean);
  for (const lote of lotes) {
    await pool.request().batch(lote);
  }
}

async function datos() {
  await transaccion(async (q) => {
    const id = async (tabla, columna, valor) => (await q(`SELECT id FROM ${tabla} WHERE ${columna} = @v`, { v: valor }))[0].id;

    const carreras = [
      'Ingeniería en Sistemas Computacionales', 'Ingeniería Industrial', 'Ingeniería en Gestión Empresarial',
      'Ingeniería Bioquímica', 'Ingeniería Electromecánica', 'Ingeniería Civil',
      'Ingeniería en Tecnologías de la Información y Comunicaciones', 'Ingeniería Ambiental',
      'Ingeniería Petrolera', 'Licenciatura en Gastronomía',
    ];
    for (const n of carreras) await q('INSERT INTO carreras (nombre) VALUES (@n)', { n });

    await q("INSERT INTO periodos (inicio, fin, activo) VALUES ('2026-08-17', '2026-12-18', 1)");

    const dias = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];
    for (let i = 0; i < dias.length; i++) {
      await q('INSERT INTO dias_semana (id, nombre, orden) VALUES (@id, @n, @id)', { id: i + 1, n: dias[i] });
    }

    const lugares = [
      'Cancha del Tecnológico', 'Domo del Tecnológico', 'Campo de fútbol del Tecnológico',
      'Auditorio Deportivo COMUDE', 'Unidad Deportiva',
    ];
    for (const n of lugares) await q('INSERT INTO lugares (nombre) VALUES (@n)', { n });
    for (const n of ['Deportes', 'Ajedrez', 'Música']) await q('INSERT INTO especialidades (nombre) VALUES (@n)', { n });

    const hash = generarHash(CLAVE_DEMO);
    const usuario = (nombres, paterno, materno, correo, rol) => q(
      `INSERT INTO usuarios (nombres, apellido_paterno, apellido_materno, correo, contrasena_hash, rol)
       VALUES (@nombres, @paterno, @materno, @correo, @hash, @rol)`,
      { nombres, paterno, materno, correo, hash, rol },
    );
    await usuario('Administrador', 'ITSM', null, 'admin@itsm.edu.mx', 'admin');

    // docentes de actividades extraescolares: nombres, paterno, materno, correo, especialidad, telefono
    const docentes = [
      ['Jasciel', 'Jiménez', 'Cortes', 'jjimenez@itsm.edu.mx', 'Deportes', '2351000001'],
      ['Ricardo Ángel', 'Hidalgo', 'Ponce', 'rhidalgo@itsm.edu.mx', 'Deportes', '2351000002'],
      ['César David', 'Rodríguez', 'Francisco', 'cdrodriguez@itsm.edu.mx', 'Deportes', '2351000003'],
      ['Cristóbal Alí', 'Méndez', 'Cortijo', 'cmendez@itsm.edu.mx', 'Deportes', '2351000004'],
      ['Emiliano', 'Sesma', null, 'esesma@itsm.edu.mx', 'Música', '2351000005'],
      ['Zair', 'Rodríguez', null, 'zrodriguez@itsm.edu.mx', 'Ajedrez', '2351000006'],
    ];
    for (const [nombres, paterno, materno, correo, esp, tel] of docentes) {
      await usuario(nombres, paterno, materno, correo, 'docente');
      await q('INSERT INTO docentes (id_usuario, id_especialidad, telefono) VALUES (@u, @e, @t)',
        { u: await id('usuarios', 'correo', correo), e: await id('especialidades', 'nombre', esp), t: tel });
    }

    // alumnos: nombres, paterno, materno, matricula, carrera (id), semestre, actividad
    const alumnos = [
      ['Milton David', 'Ramírez', 'Romero', '231T0412', 1, 7, 'Fútbol matutino'],
      ['Joselin', 'Suárez', 'Mayo', '241T0233', 2, 5, 'Baloncesto matutino'],
      ['Daniela Elizabeth', 'Quiroz', 'López', '251T0318', 3, 3, 'Baloncesto matutino'],
      ['Joseph Rodolfo', 'de los Santos', 'Landa', '231T0127', 1, 7, 'Fútbol matutino'],
      ['Yatziri', 'Portilla', 'Lozada', '261T0094', 7, 1, 'Voleibol matutino'],
      ['José Manuel', 'Viveros', 'Barradas', '241T0356', 5, 5, 'Banda de guerra'],
      ['Ángel', 'Martínez', 'Jiménez', '251T0271', 1, 3, 'Fútbol vespertino'],
      ['Saúl', 'Martinez', 'Monzon', '231T0589', 1, 7, 'Fútbol matutino'],
      ['Luis Armando', 'Lobato', 'Valencia', '241T0142', 6, 5, 'Fútbol vespertino'],
      ['Rafael', 'Ortega', 'Guerra', '251T0465', 4, 3, 'Fútbol matutino'],
      ['Jared Henares', 'Pardo', 'Fernández', '231T0203', 1, 7, 'Fútbol vespertino'],
      ['Ana Keren', 'Córdova', 'Riviello', '261T0178', 8, 1, 'Voleibol matutino'],
      ['Noé de Jesús', 'Aguirre', 'Hernández', '241T0527', 2, 5, 'Voleibol vespertino'],
      ['Irvign Alberto', 'Pérez', 'Hernández', '251T0390', 7, 3, 'Baloncesto vespertino'],
      ['Jesús Antonio', 'Pérez', 'López', '231T0316', 1, 7, 'Baloncesto vespertino'],
      ['Héctor', 'Uribe', 'Herrera', '241T0481', 5, 5, 'Ajedrez'],
      ['Josué', 'Quiñónez', 'Reyes', '261T0059', 9, 1, 'Banda de guerra'],
    ];
    for (const [nombres, paterno, materno, matricula, carrera, semestre] of alumnos) {
      const correo = `${matricula.toLowerCase()}@itsm.edu.mx`;
      await usuario(nombres, paterno, materno, correo, 'alumno');
      await q('INSERT INTO alumnos (id_usuario, matricula, id_carrera, semestre) VALUES (@u, @m, @c, @s)',
        { u: await id('usuarios', 'correo', correo), m: matricula, c: carrera, s: semestre });
    }

    // actividad: nombre, cupo, docente, horarios [dia, inicio, fin], lugar
    const MAT = ['07:00', '09:00'];
    const VES = ['16:00', '18:00'];
    const actividades = [
      ['Fútbol matutino', 25, 'jjimenez@itsm.edu.mx', [['Lunes', ...MAT], ['Miércoles', ...MAT]], 'Campo de fútbol del Tecnológico'],
      ['Fútbol vespertino', 25, 'jjimenez@itsm.edu.mx', [['Martes', ...VES], ['Jueves', ...VES]], 'Campo de fútbol del Tecnológico'],
      ['Baloncesto matutino', 25, 'rhidalgo@itsm.edu.mx', [['Martes', ...MAT], ['Jueves', ...MAT]], 'Cancha del Tecnológico'],
      ['Baloncesto vespertino', 25, 'rhidalgo@itsm.edu.mx', [['Lunes', ...VES], ['Miércoles', ...VES]], 'Cancha del Tecnológico'],
      ['Voleibol matutino', 25, 'cdrodriguez@itsm.edu.mx', [['Lunes', ...MAT], ['Miércoles', ...MAT]], 'Domo del Tecnológico'],
      ['Voleibol vespertino', 25, 'cmendez@itsm.edu.mx', [['Martes', ...VES], ['Jueves', ...VES]], 'Domo del Tecnológico'],
      ['Banda de guerra', 30, 'esesma@itsm.edu.mx', [['Lunes', '17:00', '19:00'], ['Miércoles', '17:00', '19:00']], 'Unidad Deportiva'],
      ['Ajedrez', 20, 'zrodriguez@itsm.edu.mx', [['Viernes', ...VES]], 'Auditorio Deportivo COMUDE'],
    ];
    for (const [nombre, cupo, docente, horarios, lugar] of actividades) {
      const idAct = (await q(
        'INSERT INTO actividades (nombre, cupo, id_docente, id_periodo) OUTPUT INSERTED.id VALUES (@n, @c, @d, 1)',
        { n: nombre, c: cupo, d: await id('usuarios', 'correo', docente) },
      ))[0].id;
      for (const [dia, inicio, fin] of horarios) {
        await q(
          'INSERT INTO horarios (id_actividad, id_dia, hora_inicio, hora_fin, id_lugar) VALUES (@a, @d, @i, @f, @l)',
          { a: idAct, d: await id('dias_semana', 'nombre', dia), i: inicio, f: fin, l: await id('lugares', 'nombre', lugar) },
        );
      }
    }

    // cada alumno en su actividad
    for (const [, , , matricula, , , actividad] of alumnos) {
      await q(
        `INSERT INTO inscripciones (id_alumno, id_actividad)
         SELECT a.id_usuario, act.id FROM alumnos a, actividades act WHERE a.matricula = @m AND act.nombre = @n`,
        { m: matricula, n: actividad },
      );
    }

    // tres sesiones por actividad (septiembre de 2026), con algunas faltas
    const FALTAS = { '231T0589': [2], '231T0203': [2, 3], '241T0233': [3], '251T0271': [2] };
    const DIAS = { Lunes: 7, Martes: 8, Miércoles: 9, Jueves: 10, Viernes: 11 };
    for (const [nombre, , , horarios] of actividades) {
      const idAct = await id('actividades', 'nombre', nombre);
      const dia1 = DIAS[horarios[0][0]];
      // dos dias por semana: lunes, miercoles y lunes siguiente; un dia: tres semanas seguidas
      const dias = horarios[1] ? [dia1, DIAS[horarios[1][0]], dia1 + 7] : [dia1, dia1 + 7, dia1 + 14];
      const fechas = dias.map((d) => `2026-09-${String(d).padStart(2, '0')}`);
      for (let i = 0; i < fechas.length; i++) {
        const sesion = (await q(
          'INSERT INTO sesiones (id_actividad, fecha) OUTPUT INSERTED.id VALUES (@a, @f)', { a: idAct, f: fechas[i] },
        ))[0].id;
        for (const [, , , matricula, , , actividad] of alumnos) {
          if (actividad !== nombre) continue;
          const presente = (FALTAS[matricula] || []).includes(i + 1) ? 0 : 1;
          await q(
            `INSERT INTO asistencias (id_sesion, id_alumno, presente)
             SELECT @s, id_usuario, @p FROM alumnos WHERE matricula = @m`,
            { s: sesion, p: presente, m: matricula },
          );
        }
      }
    }
  });
}

(async () => {
  const pool = await obtener();
  console.log('Conectado. Creando tablas...');
  await esquema(pool);
  console.log('Cargando datos de prueba...');
  await datos();
  const [r] = (await pool.request().query('SELECT COUNT(*) AS n FROM usuarios')).recordset;
  console.log(`Listo: ${r.n} usuarios. Contraseña de prueba: ${CLAVE_DEMO}`);
  await pool.close();
})().catch((e) => {
  console.error('Falló:', e.message);
  process.exit(1);
});
