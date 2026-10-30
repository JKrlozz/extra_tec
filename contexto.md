# Contexto del proyecto: extra_tec

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
  - Cada pantalla se mapea así: login/registro usa `usuarios`+`alumnos`; actividades/horario usa `v_actividades`+`horarios`+`inscripciones`; pase de lista usa `sesiones`+`asistencias`; evidencia usa `evidencias`; constancia usa `v_asistencia_alumno`+`constancias`; reportes son consultas sobre las anteriores.
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

- 2026-10-30: Base de datos en 3FN y pantallas adaptadas (rama actual).
  - `usuarios.nombre` se dividió en `nombres`, `apellido_paterno` y `apellido_materno`; `nombre_completo` es una columna generada para mostrar y buscar. Los formularios de alumno, docente y registro público ya piden las tres partes.
  - Catálogos nuevos `dias_semana` (con orden) y `lugares`. `horarios` usa `id_dia` e `id_lugar` en vez del `ENUM` y del texto libre.
  - Se quitaron datos derivados: `periodos.nombre` se calcula en `v_periodos` y `constancias.porcentaje` en `v_asistencia_alumno`.
  - `evidencias.id_docente` se conserva como autor histórico de la subida.
  - La API devuelve el nombre completo y sus partes; el editor de actividades usa filas dinámicas con día, hora de inicio, hora de fin y lugar, y rechaza horarios traslapados.
  - Navegación: `Principal_Vista` guarda un historial; el botón Volver y el botón físico Atrás regresan a la pantalla anterior, y las barras inferiores reinician el historial.
  - Se quitaron símbolos decorativos de títulos y etiquetas en toda la app.
  - `database/NORMALIZACION.md` documenta la revisión 3FN tabla por tabla.
  - Verificado con una instancia temporal de MySQL y pruebas de la API: esquema, seed, login, altas y cambios con nombres separados, horarios, inscripciones, cupo, asistencia, evidencias, constancias y reportes.
  - Probado en el emulador: login con nombre completo, actividades, horario con día y lugar, botón Volver, botón físico Atrás, editor de actividad con horarios dinámicos y registro con nombres separados.
  - Aplicado en la base real `extra_tec` con `instalar.bat` y verificado contra ella: API (login, catálogos, actividades, horarios, inscripciones, cupo, asistencia, constancias, PDF y reportes) y pantallas del emulador.
  - Registro de alumno: el semestre ahora es una lista de Primero a Noveno, el apellido materno ya no dice "opcional" y la matrícula se muestra como "Número de control".

## Cómo levantar todo
1. Ejecutar `instalar.bat`: crea la base, carga los datos de prueba, genera `api/.env` e instala las dependencias (pide la contraseña de root de MySQL).
2. Ejecutar `iniciar-api.bat` y comprobar `http://localhost:3000/api/salud`.
3. Para usar un celular en la misma red: ejecutar `api/permitir-red.ps1` como administrador, una sola vez.

## Siguiente paso
- Probar en el emulador el flujo del docente (pase de lista y evidencias) contra la base real.
- Revisar el commit del refactor 3FN antes de subirlo.

## Notas
-
