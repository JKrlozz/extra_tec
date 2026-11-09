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
  - Triggers: `trg_inscripcion_cupo` bloquea inscripciones cuando el cupo está lleno; `trg_inscripcion_unica_alta` y `trg_inscripcion_unica_cambio` impiden que un alumno tenga dos actividades activas.
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
  - Los formularios de alumno (registro público y panel del administrador) usan una lista de semestre de Primero a Noveno, ya no piden "opcional" en el apellido materno y muestran "Número de control" en lugar de "Matrícula".
  - La misma palabra "número de control" se usa en la lista de alumnos del administrador, la lista de alumnos del docente, el pase de lista y el texto del PDF de la constancia. El campo de la base y los nombres en el código siguen siendo `matricula`.
  - Un alumno solo puede estar inscrito en una actividad a la vez. Se valida en tres capas: la API responde 409 en `POST /inscripciones` si ya tiene otra, los triggers de MySQL lo impiden a nivel de tabla y la app desactiva los botones de las demás actividades con el texto "Ya tienes una actividad". Para cambiar debe darse de baja primero.
  - El número de control tiene el formato `000T0000` (tres dígitos, `T` y cuatro dígitos, por ejemplo `232T0182`). Se valida con `CHECK` en `alumnos`, con `requireMatricula` en la API y con `NumeroControl` en la app; se guarda siempre en mayúsculas. Los campos muestran la ayuda "Formato 000T0000, por ejemplo 232T0182" y limitan a 8 caracteres.
  - Pantallas de inicio sin saludo: el alumno y el docente muestran solo el nombre de la cuenta (el alumno agrega "Alumno, correo" y el docente su rol como subtítulo) y el administrador muestra "Panel de Administrador" con su correo.

- 2026-11-06: La app funciona de forma local con SQLite, sin servidor ni red.
  - La base vive en el teléfono (`extra_tec.db`) y se crea con `Modelo/BaseDatos_Esquema.kt` la primera vez que se abre la app. El esquema de referencia está en `database/schema.sqlite.sql`.
  - `Modelo/BaseDatos.kt` usa `SQLiteOpenHelper` sin dependencias nuevas: un solo hilo para las consultas, transacciones para las escrituras y mensajes de error entendibles. `onUpgrade` migra de la versión 1 a la 2 conservando las evidencias anteriores y, para otros cambios, vuelve a crear la base porque los datos son locales.
  - Las contraseñas se guardan con PBKDF2 (`Modelo/Clave.kt`), nunca en claro. Los datos de prueba usan la contraseña `itsm1234`.
  - Se conservan las 13 tablas (incluida `evidencia_fotos`), la 3FN, las vistas `v_periodos`, `v_actividades` y `v_asistencia_alumno`, y los triggers de cupo e inscripción única. El horario resumido se arma en Kotlin para respetar el orden de los días.
  - Las fotografías de evidencia se guardan en el almacenamiento interno de la app (`filesDir/evidencias/`); la tabla `evidencias` guarda el día, el estado y la descripción, y `evidencia_fotos` guarda de una a diez fotografías por evidencia. Las constancias y los reportes se generan como PDF en el propio teléfono.
  - La evidencia admite los estados `realizada`, `suspendida` y `no_realizada`, una fecha elegida con el selector del teléfono y varias fotografías. Para `realizada` se exige al menos una foto; cada foto puede pesar hasta 8 MB.
  - Se pueden registrar varias evidencias el mismo día: cada una conserva su fecha de actividad y la hora de registro. El historial de evidencias de cada clase permite editar, eliminar y subir una nueva evidencia, y muestra la fecha, la hora, el estado, el número de fotos y si fue editada.
  - Cada tarjeta de clase del docente tiene cuatro acciones: pase de lista, ver alumnos, historial de asistencias e historial de evidencias. El historial de asistencias muestra cada sesión con el número de presentes y el detalle por alumno.
  - La sesión ya no usa token ni servidor: `Sesion_Modelo` guarda el usuario en las preferencias locales y `Sesion_Actual` lo comparte con los modelos.
  - Se eliminaron `Api_Modelo`, `Descubrimiento_Modelo`, su prueba y los controles de "servidor" del inicio de sesión. La carpeta `api/`, `instalar.bat` e `iniciar-api.bat` se movieron a `servidor_legado/` y quedan solo como referencia histórica.
  - El APK de depuración se copia a la raíz del proyecto como `extra_tec.apk` para instalarlo rápido en un teléfono.
  - Se agregó `NumeroControlTest` para validar el formato `000T0000`.

- 2026-11-06: Historiales para el administrador, calendario del alumno y formularios más claros.
  - Las tarjetas de actividades muestran filas separadas para Horario, Lugar, Docente y Cupo, además de una barra de ocupación. Ya no mezclan todos los datos en una sola línea.
  - El administrador abre el historial de evidencias y el de asistencias desde cada actividad. Puede editar, eliminar y crear evidencias, y editar las asistencias de cualquier sesión. El docente conserva las mismas acciones para sus clases.
  - El historial de asistencias muestra cada sesión con presentes, total y detalle por alumno, y agrega la acción Editar para corregir una fecha anterior.
  - El alumno ve su horario como un calendario semanal: un bloque por día con las horas, la actividad y el lugar, y el día actual resaltado.
  - Las horas de las actividades se eligen con el reloj de Material (esfera o teclado) en lugar de escribir el formato. Las fechas de los reportes se eligen con el calendario del teléfono y se pueden limpiar; la fecha de la evidencia muestra un formato legible.
  - Las acciones peligrosas (eliminar, darse de baja) se distinguen con el color de alerta y las acciones principales con el color institucional.

- 2026-11-06: Pase de lista con selección de actividad y fecha, detalle de asistencias con Expander y estilos sin bordes.
  - El docente elige la actividad y la fecha antes de marcar la lista, igual que en el formulario de evidencias.
  - El historial de asistencias muestra un resumen con barra de avance y el detalle por alumno dentro de la misma tarjeta. El botón Ver detalle ahora se llama Expander.
  - Cada alumno del pase de lista muestra su nombre y su número de control en líneas separadas.
  - Se quitaron los bordes de tarjetas, campos de texto y botones.

- 2026-11-08: Constancia oficial, reportes visuales, Personas y colores del Tec del Bienestar.
  - La constancia se genera en PDF con el formato oficial: membrete, texto con datos del alumno, nivel de desempeño, firmas y folio.
  - Los reportes se muestran como tarjetas con cada dato en su renglón. El PDF usa el mismo contenido con encabezado, tarjetas y número de página.
  - El apartado Personas reúne alumnos y docentes en una sola pantalla con contadores, pestañas, búsqueda y tarjetas detalladas.
  - El botón Volver se movió a una barra inferior con estilo de píldora y las pantallas cambian con una transición suave.
  - La paleta cambia al guinda y dorado del Tec del Bienestar. Los textos que antes se separaban con comas ahora usan filas de etiqueta y valor.
  - El catálogo de carreras se actualiza a las diez carreras oficiales. La versión 3 de la base migra los nombres anteriores sin perder alumnos.

## Cómo levantar todo
1. Abrir el proyecto en Android Studio y compilar la app, o usar el APK ya generado `extra_tec.apk` de la raíz.
2. Instalar en el emulador o en el teléfono. No hace falta encender MySQL, la API ni configurar la red.
3. Iniciar sesión con una cuenta de prueba (contraseña `itsm1234`) o registrarse como alumno.

## Siguiente paso
- Probar en el emulador los historiales del administrador, la edición de asistencias y el calendario del alumno.
- Revisar el commit de la migración a SQLite y de estas mejoras antes de subirlo.

## Notas
- La base local se borra al desinstalar la app o al limpiar sus datos; al abrirla de nuevo se vuelve a crear con los datos de prueba.
- Si se cambia el esquema, subir la versión de `BaseDatos`. La versión 2 migra las evidencias de la versión 1 y la versión 3 actualiza el catálogo de carreras; otros cambios vuelven a crear las tablas.
