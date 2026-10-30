# Normalización de la base de datos

El esquema `database/schema.sql` quedó en tercera forma normal (3FN). Este
documento revisa cada tabla, indica qué se cambió y por qué.

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
| `nombre_completo` | columna generada a partir de las tres partes |

**Cambio:** `nombre` guardaba el nombre completo en un solo campo, lo que
rompía la 1FN. Se dividió en `nombres`, `apellido_paterno` y
`apellido_materno`. `nombre_completo` es una columna `GENERATED ALWAYS AS ...
STORED`: la calcula MySQL, no se captura ni se actualiza, y sirve para buscar y
mostrar. Por eso no introduce una dependencia transitiva real.

### `alumnos`

| Columna | Dependencia |
| --- | --- |
| `id_usuario` | llave primaria y llave foránea a `usuarios` |
| `matricula` | depende de `id_usuario`, única |
| `id_carrera` | depende de `id_usuario`, llave foránea a `carreras` |
| `semestre` | depende de `id_usuario` |

**Cambio:** el nombre de la carrera no se duplica; se obtiene con `JOIN`.

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
| `descripcion`, `ruta_foto`, `subida_en` | dependen de `id` |

**Decisión:** `id_docente` se conserva aunque la actividad ya tenga un titular.
No es un dato derivable: registra quién subió la foto en ese momento, y el
titular puede cambiar después. Es un hecho histórico, no una repetición.

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
| Nombre completo de la persona | `usuarios.nombre_completo` (columna generada) |

## Reglas de negocio que quedan en la API

- Solo puede haber un periodo activo a la vez; la API toma el más reciente
  (`ORDER BY inicio DESC LIMIT 1`).
- El cupo se valida con el trigger `trg_inscripcion_cupo` y también al
  reactivar una inscripción dada de baja.
- Un docente solo consulta y modifica sus propias actividades.
- La constancia exige el porcentaje mínimo configurado en `ASISTENCIA_MINIMA`.
