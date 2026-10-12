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

## Notas
-
