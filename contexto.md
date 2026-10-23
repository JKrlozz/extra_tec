# Contexto del proyecto – extra_tec

**Materia:** DB de Aplicaciones Móviles (Séptimo semestre)
**Creado:** 2026-10-09

## Objetivo
_Pendiente de definir._

## Decisiones y avances
- 2026-10-09: Se crea este documento para guardar el contexto que se vaya desarrollando.

- 2026-10-09: Rediseño institucional de las 21 pantallas (app Android, vistas XML + Material 3).
  - Paleta: azul marino `#1B396A`/`#12284B`, dorado `#C9A227`, fondo `#F3F5F9` (`res/values/colors.xml`).
  - Estilos reutilizables `Itsm.*` en `res/values/styles.xml` (encabezado, tarjetas, campos, botones, barra de navegación).
  - Tema fijo claro (`Theme.Material3.Light`), también en `values-night`.
  - Todos los IDs se conservaron; `MainActivity.kt` solo añadió el manejo de insets (`setContentView`).
  - Pendiente: compilar y revisar en emulador (no había JAVA_HOME en la sesión).

- 2026-10-12: Base de datos MySQL 8.0 (carpeta `database/`).
  - `schema.sql`: crea la BD `extra_tec` con 12 tablas: `carreras`, `periodos`, `usuarios` (rol alumno/docente/admin), `alumnos`, `docentes`, `actividades`, `horarios`, `inscripciones`, `sesiones`, `asistencias`, `evidencias`, `constancias`.
  - Vistas `v_actividades` (cupo inscritos/total, horario, docente) y `v_asistencia_alumno` (% de asistencia para la constancia).
  - Trigger `trg_inscripcion_cupo`: bloquea inscripciones cuando el cupo está lleno.
  - `seed.sql`: datos de prueba iguales a los de las pantallas (usuarios con contraseña demo `itsm1234`, 5 actividades, inscripciones y asistencia de ejemplo).
  - Cada pantalla se mapea así: login/registro → `usuarios`+`alumnos`; actividades/horario → `v_actividades`+`horarios`+`inscripciones`; pase de lista → `sesiones`+`asistencias`; evidencia → `evidencias`; constancia → `v_asistencia_alumno`+`constancias`; reportes → consultas sobre las anteriores.
  - Pendiente: ejecutar los scripts en MySQL (servicio `MySQL80` ya corre en el equipo) y verificar.

- 2026-10-12: API REST en Node.js (carpeta `api/`, Express + mysql2 + JWT + bcrypt + multer + pdfkit). La app Android no se conecta directo a MySQL (expondría credenciales); consume esta API por HTTP.
  - Dependencias instaladas y sintaxis verificada. **Aún no probada contra la BD** (falta crear la BD y el `.env`).
  - Auth: JWT de 12 h; el login valida correo + contraseña + rol. Rutas protegidas por rol (`alumno`/`docente`/`admin`).
  - Endpoints (prefijo `/api`):
    - Públicos: `POST /login`, `POST /registro`, `GET /carreras`, `GET /salud`.
    - Admin: CRUD `/alumnos`, `/docentes`, `/actividades` (las bajas son lógicas para no perder historial), `GET /reportes?tipo=actividad|alumno|docente&desde&hasta`.
    - Alumno: `GET /actividades`, `POST /inscripciones`, `DELETE /inscripciones/:idActividad`, `GET /mi-horario`, `GET /constancias`, `GET /constancias/:idActividad/pdf`.
    - Docente: `GET /mis-actividades`, `GET /actividades/:id/alumnos`, `GET|POST /asistencia`, `GET|POST /evidencias` (foto con `multipart`, solo JPG/PNG/WebP ≤ 8 MB).
  - Reglas: el docente solo opera sus actividades; la constancia exige ≥ 80 % de asistencia (`ASISTENCIA_MINIMA`) y conserva su folio al volver a descargarla; el cupo lo valida el trigger de MySQL.
  - `database/usuario_app.sql`: usuario MySQL `extra_tec_app` solo con SELECT/INSERT/UPDATE/DELETE (no usar root en el `.env`).

- 2026-10-15: BD instalada, API en marcha y app Android conectada (rama `feat/conectar-api-bd`).
  - `database/instalar.ps1` crea la BD, carga el seed, crea el usuario `extra_tec_app` y genera `api/.env` (pide la clave de root; no la guarda).
  - App: `Api.kt` (HttpURLConnection + org.json, sin dependencias nuevas), `Archivos.kt` (PDF a Descargas), `MainActivity.kt` reescrita para usar la API; layouts con listas dinámicas (`item_tarjeta.xml`) y `id` en los formularios.
  - Probado en el emulador Pixel_8a con la alumna María: login, actividades, inscripción/baja, constancia y descarga de PDF. Docente y admin verificados solo contra la API (falta probar sus pantallas en el emulador).
  - Red: desde el emulador `10.0.2.2:3000` no conectó (probable Firewall de Windows). Se resolvió con `adb reverse tcp:3000 tcp:3000` y poniendo `http://localhost:3000` en "Configurar servidor" del login. Hay que repetir el `adb reverse` cada vez que se reinicie el emulador. En teléfono real: usar la IP del PC y permitir el puerto 3000 en el firewall.
- 2026-10-19: Reestructura a MVVM (rama `refactor/mvvm`), una Activity y un Fragment por pantalla, con ViewBinding.
  - `vistas/<Rol>` (Comun, Alumno, Docente, Administrador): clases `*_Vista` en `java/itsm/extra_tec/vistas` y layouts `*_vista.xml` en `src/main/vistas/<Rol>/res/layout`. Los layouts van en minúsculas porque Android no admite mayúsculas en nombres de recursos.
  - `ModeloVista`: un ViewModel `*_ModeloVista` por pantalla, más `Sesion_ModeloVista` compartido, con LiveData y eventos de un solo uso.
  - `Modelo`: `*_Modelo` con el acceso a la API, la sesión guardada, los PDF y las entidades.
  - `Principal_Vista` es la única Activity: aloja el fragmento actual y las barras de navegación. `Pantalla_Vista` define cada pantalla y su barra.
  - Comentarios sin decoraciones en Kotlin, API, SQL y scripts.
  - Compila; falta probarlo en emulador.
## Cómo levantar todo
1. Crear BD: ejecutar `database/schema.sql` y luego `database/seed.sql` (con root).
2. Editar la clave en `database/usuario_app.sql` y ejecutarlo.
3. `cd api`, copiar `.env.example` a `.env` y poner `DB_PASSWORD` y un `JWT_SECRET` largo.
4. `npm run demo-passwords` (cambia las contraseñas de prueba a bcrypt; clave `itsm1234`).
5. `npm start` → `http://localhost:3000/api/salud`.

## Siguiente paso
- Ejecutar los pasos anteriores y probar los endpoints.
- Conectar la app Android a la API (Retrofit/HttpURLConnection; el emulador accede al PC en `http://10.0.2.2:3000`; requiere permiso `INTERNET` y `usesCleartextTraffic` en desarrollo).

## Notas
-
