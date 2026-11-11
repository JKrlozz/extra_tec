package itsm.extra_tec.vistas.Comun

// barra de abajo de cada pantalla
enum class Barra_Vista { NINGUNA, ALUMNO, DOCENTE, ADMIN }

// todas las pantallas de la app
// el fragmento se crea en principal_vista
enum class Pantalla_Vista(val barra: Barra_Vista) {
    BIENVENIDA(Barra_Vista.NINGUNA),
    INICIO_SESION(Barra_Vista.NINGUNA),
    REGISTRO(Barra_Vista.NINGUNA),

    INICIO_ALUMNO(Barra_Vista.ALUMNO),
    ACTIVIDADES_ALUMNO(Barra_Vista.ALUMNO),
    HORARIO_ALUMNO(Barra_Vista.ALUMNO),
    CONSTANCIA(Barra_Vista.ALUMNO),

    INICIO_DOCENTE(Barra_Vista.DOCENTE),
    ASISTENCIA(Barra_Vista.DOCENTE),
    ALUMNOS_DOCENTE(Barra_Vista.DOCENTE),
    EVIDENCIA(Barra_Vista.DOCENTE),
    HISTORIAL_ASISTENCIA(Barra_Vista.DOCENTE),
    HISTORIAL_EVIDENCIA(Barra_Vista.DOCENTE),

    INICIO_ADMIN(Barra_Vista.ADMIN),
    GESTIONAR_ACTIVIDADES(Barra_Vista.ADMIN),
    HISTORIAL_ASISTENCIA_ADMIN(Barra_Vista.ADMIN),
    HISTORIAL_EVIDENCIA_ADMIN(Barra_Vista.ADMIN),
    ASISTENCIA_ADMIN(Barra_Vista.ADMIN),
    EVIDENCIA_ADMIN(Barra_Vista.NINGUNA),
    FORMULARIO_ACTIVIDAD(Barra_Vista.NINGUNA),
    GESTIONAR_DOCENTES(Barra_Vista.ADMIN),
    FORMULARIO_DOCENTE(Barra_Vista.NINGUNA),
    GESTIONAR_ALUMNOS(Barra_Vista.ADMIN),
    FORMULARIO_ALUMNO(Barra_Vista.NINGUNA),
    REPORTES(Barra_Vista.ADMIN),
}
