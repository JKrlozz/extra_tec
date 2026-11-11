# Normalización de la base de datos

La app usa una base SQLite local (`extra_tec.db`) que se crea con
`app/src/main/java/itsm/extra_tec/Modelo/BaseDatos_Esquema.kt`. La copia de
referencia está en `database/schema.sqlite.sql`. Los archivos de MySQL
(`schema.sql`, `seed.sql`, `usuario_app.sql`, `instalar.ps1`) son históricos y
ya no los usa la app.

La versión actual del esquema es la **5**. Al pasar de una versión anterior la
base se vuelve a crear con los datos de prueba, porque todavía no hay datos
reales.

## Criterios

1. **1FN:** cada columna guarda un solo valor y cada tabla tiene llave primaria.
2. **2FN:** ningún dato depende solo de una parte de una llave compuesta.
3. **3FN:** ningún dato depende de otro dato que no sea la llave. Los textos que
   se repiten van en catálogos y los datos calculados se sacan con vistas.

## Cambios de la versión 4

| Cambio | Motivo |
| --- | --- |
| Catálogo nuevo `especialidades`; `docentes.especialidad` pasa a `docentes.id_especialidad` | La especialidad era texto libre repetido ("Deportes", "deportes"). Ahora se guarda una vez y se elige del catálogo; si el administrador escribe una nueva, se agrega. |
| `evidencia_fotos.ruta_foto` pasa a `evidencia_fotos.archivo` | Se guardaba la ruta completa del teléfono, que depende de dónde está instalada la app. Ahora solo se guarda el nombre del archivo y la ruta se arma al leer. También sirve para la migración a Azure. |
| `CHECK` de longitud en todos los textos | SQLite no respeta `VARCHAR(n)`. Los límites son los mismos que valida la app (`Modelo/Limites.kt`). |
| `CHECK` de formato en fechas (`AAAA-MM-DD`), horas (`HH:MM`), correo y teléfono (10 dígitos) | Evita datos con formato incorrecto aunque se inserten fuera de la app. |
| `lugares`, `especialidades` y `usuarios.correo` con `COLLATE NOCASE` | "Gimnasio" y "gimnasio" o dos correos que solo cambian en mayúsculas ya no se duplican. |
| Índice único `idx_periodo_activo` | Solo puede haber un periodo activo. |
| Trigger `trg_asistencia_inscrito` | Una asistencia solo puede ser de un alumno inscrito en la actividad de esa sesión. |
| Triggers `trg_alumno_rol` y `trg_docente_rol` | `usuarios.rol` debe coincidir con la tabla `alumnos` o `docentes`. |
| Versión 5: `trg_alumno_rol` y `trg_alumno_correo` exigen que el correo del alumno sea su número de control con `@itsm.edu.mx`; `semestre` va de 1 a 8 | Regla de la escuela para cuentas de alumno. |
| Trigger `trg_fotos_maximo` | Máximo 10 fotografías por evidencia. |
| `cupo` entre 1 y 100 | Límite real de un grupo. |

## Revisión por tabla

| Tabla | Llave | Atributos | Notas |
| --- | --- | --- | --- |
| `carreras` | `id` | `nombre` | Catálogo. |
| `periodos` | `id` | `inicio`, `fin`, `activo` | El nombre ("Ago/Dic 2026") se calcula en `v_periodos`. |
| `dias_semana` | `id` | `nombre`, `orden` | Catálogo con el orden de la semana. |
| `lugares` | `id` | `nombre` | Catálogo. |
| `especialidades` | `id` | `nombre` | Catálogo nuevo. |
| `usuarios` | `id` | `nombres`, `apellido_paterno`, `apellido_materno`, `correo`, `contrasena_hash`, `rol`, `activo`, `creado_en` | El nombre completo se arma al consultar. |
| `alumnos` | `id_usuario` | `matricula`, `id_carrera`, `semestre` | El nombre de la carrera sale por `JOIN`. |
| `docentes` | `id_usuario` | `id_especialidad`, `telefono` | La especialidad sale por `JOIN`. |
| `actividades` | `id` | `nombre`, `descripcion`, `cupo`, `id_docente`, `id_periodo`, `activa` | Docente, periodo e inscritos salen de `v_actividades`. |
| `horarios` | `id` | `id_actividad`, `id_dia`, `hora_inicio`, `hora_fin`, `id_lugar` | `UNIQUE (id_actividad, id_dia, hora_inicio)`; la app rechaza horarios traslapados. |
| `inscripciones` | `id` | `id_alumno`, `id_actividad`, `fecha`, `estado` | Baja lógica; un alumno solo tiene una inscripción activa. |
| `sesiones` | `id` | `id_actividad`, `fecha` | Una sesión por actividad y día. |
| `asistencias` | `id` | `id_sesion`, `id_alumno`, `presente` | Un registro por alumno y sesión. |
| `evidencias` | `id` | `id_actividad`, `id_docente`, `fecha`, `estado`, `descripcion`, `creada_en`, `actualizada_en` | `id_docente` se conserva como autor histórico: el titular de la actividad puede cambiar después. |
| `evidencia_fotos` | `id` | `id_evidencia`, `archivo`, `orden` | De 1 a 10 fotos por evidencia. |
| `constancias` | `id` | `id_inscripcion`, `folio`, `emitida_en` | El porcentaje se calcula en `v_asistencia_alumno`; el folio se guarda porque identifica el documento emitido. |

## Datos que no se guardan

| Dato | Dónde se calcula |
| --- | --- |
| Nombre del periodo | `v_periodos` |
| Docente, periodo e inscritos de una actividad | `v_actividades` |
| Sesiones, asistencias y porcentaje | `v_asistencia_alumno` |
| Nombre completo | `nombreCompleto()` en Kotlin o `||` en SQL |
| Ruta completa de una foto | `Evidencias_Modelo` con la carpeta de la app |

## Límites de los datos

Los mismos números están en la base (`CHECK`), en `Modelo/Limites.kt` y en el
`maxLength` de los campos de los XML.

| Campo | Límite |
| --- | --- |
| Nombre(s) | 2 a 50 letras |
| Apellidos | 2 a 40 letras (el materno es opcional) |
| Correo | hasta 100, debe terminar en `@itsm.edu.mx`; el del alumno es `numerodecontrol@itsm.edu.mx` (por ejemplo `193t0183@itsm.edu.mx`) |
| Contraseña | 8 a 30, sin espacios |
| Número de control | `000T0000` |
| Semestre | 1 a 8 |
| Teléfono | 10 dígitos (opcional) |
| Especialidad | hasta 50 (opcional) |
| Nombre de actividad | 3 a 50 |
| Descripción de actividad | hasta 200 |
| Cupo | 1 a 100 |
| Lugar | 2 a 60 |
| Horarios por actividad | 1 a 7 |
| Descripción de evidencia | hasta 300 |
| Fotos por evidencia | hasta 10, de 8 MB cada una |
| Fecha de evidencia y asistencia | no puede ser futura |

## Para la migración a Azure SQL

El esquema ya no guarda rutas del teléfono y todos los textos tienen longitud
máxima, así que cada columna `TEXT` puede pasar a `NVARCHAR(n)` con el mismo
número. Lo que hay que reescribir en T-SQL: `AUTOINCREMENT` (`IDENTITY`),
`GLOB` (`LIKE` con rangos), `datetime('now','localtime')` (`SYSDATETIME()`),
`strftime` de `v_periodos`, `COLLATE NOCASE` (la intercalación de Azure ya
ignora mayúsculas) y los triggers con `RAISE` (`THROW`).
