# Normalización de la base de datos

El esquema `database/schema.sql` (MySQL) y su equivalente local
`database/schema.sqlite.sql` quedaron en tercera forma normal (3FN). Este
documento revisa cada tabla, indica qué se cambió y por qué. La app usa el
esquema SQLite; el de MySQL queda como referencia histórica.

## Criterios aplicados

1. **Primera forma normal (1FN):** cada columna guarda un solo valor atómico y
   cada tabla tiene llave primaria.
2. **Segunda forma normal (2FN):** ningún atributo depende solo de una parte de
   una llave compuesta. Las tablas de relación usan una llave propia y sus
   columnas dependen de la llave completa.
3. **Tercera forma normal (3FN):** ningún atributo depende de otro atributo que
   no sea la llave. Los datos derivados se calculan en vistas o columnas
   generadas, y los textos repetidos se guardan una sola vez en catálogos.

## Revisión por tabla

### `carreras`

| Columna | Dependencia |
| --- | --- |
| `id` | llave primaria |
| `nombre` | depende de `id` |

Sin cambios de fondo. `nombre` es único y atómico.

### `periodos`

| Columna | Dependencia |
| --- | --- |
| `id` | llave primaria |
| `inicio`, `fin` | dependen de `id` |
| `activo` | depende de `id` |

**Cambio:** se eliminó `nombre`, porque se deducía de `inicio` y `fin`
(por ejemplo `Ene/Jun 2025`). Guardarlo repetía una dependencia transitiva. El
nombre legible ahora lo calcula la vista `v_periodos` con `ELT` y `YEAR`.

### `dias_semana` (nueva)

| Columna | Dependencia |
| --- | --- |
| `id` | llave primaria |
| `nombre` | depende de `id` |
| `orden` | depende de `id` |

**Cambio:** el día de la semana ya no se repite como texto en cada horario.
El catálogo conserva además el orden de la semana, que antes se perdía.

### `lugares` (nueva)

| Columna | Dependencia |
| --- | --- |
| `id` | llave primaria |
| `nombre` | depende de `id` |

**Cambio:** `horarios.lugar` era texto libre y se repetía en cada bloque. Ahora
es una llave foránea a un catálogo. La API registra un lugar nuevo cuando el
administrador escribe uno que no existe.

### `usuarios`

| Columna | Dependencia |
| --- | --- |
| `id` | llave primaria |
| `nombres` | depende de `id` |
| `apellido_paterno` | depende de `id` |
| `apellido_materno` | depende de `id` |
| `correo` | depende de `id`, único |
| `contrasena_hash` | depende de `id` |
| `rol` | depende de `id` |
| `activo`, `creado_en` | dependen de `id` |
| `nombre_completo` | columna generada en MySQL; en SQLite se compone al consultar |

**Cambio:** `nombre` guardaba el nombre completo en un solo campo, lo que
rompía la 1FN. Se dividió en `nombres`, `apellido_paterno` y
`apellido_materno`. En MySQL `nombre_completo` es una columna
`GENERATED ALWAYS AS ... STORED`; en SQLite no se guarda y se compone con
`TRIM` y `||` en las consultas. Por eso no introduce una dependencia transitiva
real.

### `alumnos`

| Columna | Dependencia |
| --- | --- |
| `id_usuario` | llave primaria y llave foránea a `usuarios` |
| `matricula` | depende de `id_usuario`, única |
| `id_carrera` | depende de `id_usuario`, llave foránea a `carreras` |
| `semestre` | depende de `id_usuario` |

**Cambio:** el nombre de la carrera no se duplica; se obtiene con `JOIN`.
`matricula` es `CHAR(8)` con `CHECK (matricula REGEXP '^[0-9]{3}T[0-9]{4}$')`
para exigir el formato `000T0000` (por ejemplo `232T0182`), y `semestre` tiene
`CHECK (semestre BETWEEN 1 AND 9)` porque los planes van de primero a noveno.

### `docentes`

| Columna | Dependencia |
| --- | --- |
| `id_usuario` | llave primaria y llave foránea a `usuarios` |
| `especialidad`, `telefono` | dependen de `id_usuario` |

Sin cambios de fondo. Los datos personales viven en `usuarios`.

### `actividades`

| Columna | Dependencia |
| --- | --- |
| `id` | llave primaria |
| `nombre`, `descripcion`, `cupo`, `activa` | dependen de `id` |
| `id_docente` | depende de `id`, llave foránea a `docentes` |
| `id_periodo` | depende de `id`, llave foránea a `periodos` |

**Cambio:** se eliminó el nombre del docente y el nombre del periodo repetidos;
ahora se resuelven con `JOIN` en `v_actividades`.

### `horarios`

| Columna | Dependencia |
| --- | --- |
| `id` | llave primaria |
| `id_actividad` | llave foránea a `actividades` |
| `id_dia` | llave foránea a `dias_semana` |
| `hora_inicio`, `hora_fin` | dependen de la fila |
| `id_lugar` | llave foránea a `lugares` |

**Cambio:** `dia_semana` era un `ENUM` repetido y `lugar` era texto libre. Ahora
son llaves foráneas. `UNIQUE (id_actividad, id_dia, hora_inicio)` evita
duplicados y `CHECK (hora_fin > hora_inicio)` valida el rango. La API además
rechaza bloques traslapados del mismo día.

### `inscripciones`

| Columna | Dependencia |
| --- | --- |
| `id` | llave primaria |
| `id_alumno` | llave foránea a `alumnos` |
| `id_actividad` | llave foránea a `actividades` |
| `fecha`, `estado` | dependen de `id` |

**Cambio:** `UNIQUE (id_alumno, id_actividad)` evita inscribir dos veces al
mismo alumno. La baja es lógica (`estado = 'baja'`) para conservar historial.
Además, los disparadores `trg_inscripcion_unica_alta` y
`trg_inscripcion_unica_cambio` impiden que un alumno tenga dos inscripciones
activas al mismo tiempo: solo puede pertenecer a una actividad y debe darse de
baja antes de elegir otra.

### `sesiones`

| Columna | Dependencia |
| --- | --- |
| `id` | llave primaria |
| `id_actividad` | llave foránea a `actividades` |
| `fecha` | depende de `id` |

**Cambio:** `UNIQUE (id_actividad, fecha)` evita dos sesiones el mismo día.

### `asistencias`

| Columna | Dependencia |
| --- | --- |
| `id` | llave primaria |
| `id_sesion` | llave foránea a `sesiones` |
| `id_alumno` | llave foránea a `alumnos` |
| `presente` | depende de la combinación sesión y alumno |

**Cambio:** `UNIQUE (id_sesion, id_alumno)` permite un solo registro por alumno
y sesión.

### `evidencias`

| Columna | Dependencia |
| --- | --- |
| `id` | llave primaria |
| `id_actividad` | llave foránea a `actividades` |
| `id_docente` | llave foránea a `docentes` |
| `fecha`, `estado`, `descripcion`, `creada_en`, `actualizada_en` | dependen de `id` |

**Decisión:** `id_docente` se conserva aunque la actividad ya tenga un titular.
No es un dato derivable: registra quién subió la evidencia en ese momento, y el
titular puede cambiar después. Es un hecho histórico, no una repetición. Cuando
la evidencia la registra el administrador, se guarda el docente titular de la
actividad, porque la columna solo puede referenciar a `docentes`; si la actividad
no tiene titular, queda `NULL`. Así el reporte por docente sigue contando la
evidencia sin inventar un usuario que no existe en `docentes`.

**Cambio:** una evidencia representa un día de una actividad. `fecha` guarda el
día de la actividad y `creada_en` la hora de registro; así se pueden registrar
varias evidencias el mismo día. `estado` solo admite `realizada`, `suspendida`
o `no_realizada`.

### `evidencia_fotos`

| Columna | Dependencia |
| --- | --- |
| `id` | llave primaria |
| `id_evidencia` | llave foránea a `evidencias` |
| `ruta_foto`, `orden` | dependen de la combinación evidencia y fotografía |

**Cambio:** se separó la fotografía de la evidencia para permitir de una a diez
fotos por registro sin repetir los datos de la evidencia. `orden` conserva el
orden en que se eligieron las fotos.

### `constancias`

| Columna | Dependencia |
| --- | --- |
| `id` | llave primaria |
| `id_inscripcion` | llave foránea a `inscripciones`, única |
| `folio` | depende de `id`, único |
| `emitida_en` | depende de `id` |

**Cambio:** se eliminó `porcentaje`, porque se calcula con
`v_asistencia_alumno`. `folio` sí se guarda: es el identificador del documento
emitido y no debe cambiar aunque cambie la fórmula que lo genera.

## Datos derivados que no se almacenan

| Dato | Dónde se calcula |
| --- | --- |
| Nombre del periodo | `v_periodos.nombre` |
| Docente, inscritos y horario resumido | `v_actividades` |
| Sesiones, asistencias y porcentaje | `v_asistencia_alumno` |
| Nombre completo de la persona | `usuarios.nombre_completo` en MySQL; composición en Kotlin/SQL en SQLite |

## Reglas de negocio que quedan en la app

- Solo puede haber un periodo activo a la vez; la app toma el más reciente
  (`ORDER BY inicio DESC LIMIT 1`).
- El cupo se valida con el trigger `trg_inscripcion_cupo` y también al
  reactivar una inscripción dada de baja.
- Un docente solo consulta y modifica sus propias actividades.
- La constancia exige el porcentaje mínimo de 80 % de asistencia.
