Objetivo: inscripción administrada por el administrador, reportes filtrables, pase de lista sin selector redundante y cámara en evidencias.
Rama: feat/inscripcion-admin-reportes-camara · Entrega: ask-on-risk · Pruebas: `cd api && npm test` (node --test), `./gradlew :app:compileDebugKotlin testDebugUnitTest`

## Specs
- S1 (alumno, ya hecho en esta rama): "El alumno no podrá darse de baja por su cuenta". Confirmación antes de inscribirse ya existe en `ActividadesAlumno_Vista.kt`.
- S2 (API + admin): "Modifica la api para lograr eso": "el administrador sea el que pueda dar de baja a un alumno de una actividad". La API debe rechazar que el alumno se dé de baja solo (DELETE de alumno → 403).
- S3 (admin): "al momento de editar los datos de un alumno el administrador tambien pueda editar a que actividad está inscrito".
- S4 (reportes): "mejora los reportes para que se puedan obtener datos mas especificos añadiendo campos para dar mas contexto como especificar el numero de control u otros tipos mas de datos y poder generar reportes mas especificos o especializados, por ejemlo si quisieramos obtener el reporte de un alumno en una fecha especficia se podria añadir mas campos para obtener informacion mas especifica o mas variada, lo mismo con los docentes, el de actividades si se puede quedar general".
- S5 (pase de lista): "al seleccionar pase de lista de laguna actividad no incluir la opcioin de cambiar entre las diferentes actividades que puede impartir un docente ya que eso genera reduncdancia".
- S6 (evidencias): "en el apartado de evidencias añade la funcionalidad para poder tomar fotos en el momento de que se hara la subida de evidencia".

## Tasks
- [x] T1 S1 — inline — 0c6fb68
- [x] T2 S2,S3 — delegado (paralelo) — 0c6fb68 — Risk: item 2 (permisos) e item 3 (se quitó DELETE del alumno)
- [x] T3 S4 — delegado (paralelo) — 871bab2 — Risk: item 6 (SQL sin prueba contra la base)
- [x] T4 S5 — delegado (paralelo) — 9546360
- [x] T5 S6 — delegado (paralelo) — dba43d0
- [x] Publicada y probada contra Azure y en emulador (L3)

Ruta: cuatro unidades independientes con superficies disjuntas, lanzadas juntas (Writer trigger a).

## Log
- L1 (2026-11-19): "Modifica la api para lograr eso, y de paso haz las modificaciones necesarias para lo siguiente: mejora los reportes para que se puedan obtener datos mas especificos añadiendo campos para dar mas contexto como especificar el numero de control u otros tipos mas de datos y poder generar reportes mas especificos o especializados, por ejemlo si quisieramos obtener el reporte de un alumno en una fecha especficia se podria añadir mas campos para obtener informacion mas especifica o mas variada, lo mismo con los docentes, el de actividades si se puede quedar general, al seleccionar pase de lista de laguna actividad no incluir la opcioin de cambiar entre las diferentes actividades que puede impartir un docente ya que eso genera reduncdancia por que para que apareceria si ay de por si elegimos la actividad de la que queremos pasar lista en la ventana anterior, y en el apartado de evidencias añade la funcionalidad para poder tomar fotos en el momento de que se hara la subida de evidencia"
- L0 (petición previa): "El alumno no podrá darse de baja por su cuenta, pero para no modificar la api que el administrador sea el que pueda dar de baja a un alumno de una actividad, tambien del lado del administrador implementa que al momento de editar los datos de un alumno el administrador tambien pueda editar a que actividad está inscrito"
- L2 (2026-11-19): verificación integrada: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest` OK; `npm test` 16/16; columnas de reportes cotejadas con database/azure/schema.sql. api.zip (47 KB, zip real con tar.exe de Windows) generado en api/. Sin probar en emulador ni contra Azure; no se corrió verificador independiente.
- L3 (2026-11-19): API publicada por el usuario. 30/30 pruebas directas contra Azure (permisos, alta/cambio/baja por admin, reportes con filtros y errores 400). Emulador API 34: S1 confirmacion y sin baja OK; S3 cambio/baja/alta desde Editar alumno OK; S4 reportes alumno y docente + PDF OK; S5 sin selector OK; S6 camara -> evidencia guardada en Azure OK (evidencia de prueba id 3 borrada). Corregido: encabezado del PDF se salia con la linea de filtros; aviso del alumno decia "da de baja la actual"; ayuda de filtros en docente hablaba de numero de control. Joseph (id 11) regresado a Futbol matutino. Sin probar: cupo lleno (ninguna actividad llena).
- L4 (2026-11-19): API simplificada (e53d967) publicada por el usuario en Kudu. Pruebas contra Azure: tanda 1 30/30, tanda 2 51/51, tanda 3 10/10 (evidencias). Cubre login, permisos por rol, alumnos/docentes (editar sin cambios, crear y dar de baja uno temporal), semestre, asistencia, evidencias con foto, constancias, inscripcion propia del alumno y por admin, reportes. Estado final: Joseph (id 11) en Futbol matutino, evidencia de prueba borrada. Residuos: alumno temporal 259T0001 dado de baja (activo=0) y folio de constancia 00004 emitido. Sin probar: cupo lleno, rechazo por 80 por ciento (la prueba fallo antes por no estar inscrito), cierre de semestre, limite de intentos de login, scripts npm run bd y migrar.
