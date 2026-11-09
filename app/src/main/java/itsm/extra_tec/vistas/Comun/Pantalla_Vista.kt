package itsm.extra_tec.vistas.Comun

import androidx.fragment.app.Fragment
import itsm.extra_tec.vistas.Administrador.FormularioActividad_Vista
import itsm.extra_tec.vistas.Administrador.FormularioAlumno_Vista
import itsm.extra_tec.vistas.Administrador.FormularioDocente_Vista
import itsm.extra_tec.vistas.Administrador.GestionarActividades_Vista
import itsm.extra_tec.vistas.Administrador.InicioAdmin_Vista
import itsm.extra_tec.vistas.Administrador.Personas_Vista
import itsm.extra_tec.vistas.Administrador.Reportes_Vista
import itsm.extra_tec.vistas.Alumno.ActividadesAlumno_Vista
import itsm.extra_tec.vistas.Alumno.Constancia_Vista
import itsm.extra_tec.vistas.Alumno.HorarioAlumno_Vista
import itsm.extra_tec.vistas.Alumno.InicioAlumno_Vista
import itsm.extra_tec.vistas.Docente.AlumnosDocente_Vista
import itsm.extra_tec.vistas.Docente.Asistencia_Vista
import itsm.extra_tec.vistas.Docente.Evidencia_Vista
import itsm.extra_tec.vistas.Docente.HistorialAsistencia_Vista
import itsm.extra_tec.vistas.Docente.HistorialEvidencia_Vista
import itsm.extra_tec.vistas.Docente.InicioDocente_Vista

// Barra de navegacion de cada pantalla
enum class Barra_Vista { NINGUNA, ALUMNO, DOCENTE, ADMIN }

enum class Pantalla_Vista(val barra: Barra_Vista, val crear: () -> Fragment) {
    BIENVENIDA(Barra_Vista.NINGUNA, ::Bienvenida_Vista),
    INICIO_SESION(Barra_Vista.NINGUNA, ::InicioSesion_Vista),
    REGISTRO(Barra_Vista.NINGUNA, ::Registro_Vista),

    INICIO_ALUMNO(Barra_Vista.ALUMNO, ::InicioAlumno_Vista),
    ACTIVIDADES_ALUMNO(Barra_Vista.ALUMNO, ::ActividadesAlumno_Vista),
    HORARIO_ALUMNO(Barra_Vista.ALUMNO, ::HorarioAlumno_Vista),
    CONSTANCIA(Barra_Vista.ALUMNO, ::Constancia_Vista),

    INICIO_DOCENTE(Barra_Vista.DOCENTE, ::InicioDocente_Vista),
    ASISTENCIA(Barra_Vista.DOCENTE, ::Asistencia_Vista),
    ALUMNOS_DOCENTE(Barra_Vista.DOCENTE, ::AlumnosDocente_Vista),
    EVIDENCIA(Barra_Vista.DOCENTE, ::Evidencia_Vista),
    HISTORIAL_ASISTENCIA(Barra_Vista.DOCENTE, ::HistorialAsistencia_Vista),
    HISTORIAL_EVIDENCIA(Barra_Vista.DOCENTE, ::HistorialEvidencia_Vista),

    INICIO_ADMIN(Barra_Vista.ADMIN, ::InicioAdmin_Vista),
    GESTIONAR_ACTIVIDADES(Barra_Vista.ADMIN, ::GestionarActividades_Vista),
    HISTORIAL_ASISTENCIA_ADMIN(Barra_Vista.ADMIN, ::HistorialAsistencia_Vista),
    HISTORIAL_EVIDENCIA_ADMIN(Barra_Vista.ADMIN, ::HistorialEvidencia_Vista),
    ASISTENCIA_ADMIN(Barra_Vista.ADMIN, ::Asistencia_Vista),
    EVIDENCIA_ADMIN(Barra_Vista.NINGUNA, ::Evidencia_Vista),
    FORMULARIO_ACTIVIDAD(Barra_Vista.NINGUNA, ::FormularioActividad_Vista),
    GESTIONAR_DOCENTES(Barra_Vista.ADMIN, ::Personas_Vista),
    FORMULARIO_DOCENTE(Barra_Vista.NINGUNA, ::FormularioDocente_Vista),
    GESTIONAR_ALUMNOS(Barra_Vista.ADMIN, ::Personas_Vista),
    FORMULARIO_ALUMNO(Barra_Vista.NINGUNA, ::FormularioAlumno_Vista),
    REPORTES(Barra_Vista.ADMIN, ::Reportes_Vista),
}
