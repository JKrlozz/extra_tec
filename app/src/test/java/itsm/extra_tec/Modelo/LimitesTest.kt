package itsm.extra_tec.Modelo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LimitesTest {

    @Test
    fun nombresValidos() {
        assertNull(Limites.revisarPersona("María José", "Hernández", "", "maria@itsm.edu.mx"))
        assertNull(Limites.revisarPersona("Ana", "O'Neil", "De la Cruz", "ana.o@itsm.edu.mx"))
    }

    @Test
    fun nombresInvalidos() {
        assertNotNull(Limites.revisarNombre("Nombre(s)", "", Limites.NOMBRES_MAX, true))
        assertNotNull(Limites.revisarNombre("Nombre(s)", "A", Limites.NOMBRES_MAX, true))
        assertNotNull(Limites.revisarNombre("Nombre(s)", "Ana3", Limites.NOMBRES_MAX, true))
        assertNotNull(Limites.revisarNombre("Nombre(s)", "a".repeat(51), Limites.NOMBRES_MAX, true))
        assertNull(Limites.revisarNombre("Apellido materno", "", Limites.APELLIDO_MAX, false))
    }

    @Test
    fun correo() {
        assertNull(Limites.revisarCorreo("cperez@itsm.edu.mx"))
        assertNotNull(Limites.revisarCorreo("cperez"))
        assertNotNull(Limites.revisarCorreo("cperez@itsm"))
        assertNotNull(Limites.revisarCorreo("a".repeat(95) + "@x.com"))
        assertNotNull(Limites.revisarCorreo("cperez@gmail.com"))
    }

    @Test
    fun correoAlumno() {
        assertNull(Limites.revisarCorreoAlumno("193t0183@itsm.edu.mx", "193T0183"))
        assertNull(Limites.revisarCorreoAlumno("193T0183@ITSM.EDU.MX", "193t0183"))
        assertNotNull(Limites.revisarCorreoAlumno("maria@itsm.edu.mx", "193T0183"))
        assertNotNull(Limites.revisarCorreoAlumno("193t0184@itsm.edu.mx", "193T0183"))
        assertNotNull(Limites.revisarCorreoAlumno("193t0183@gmail.com", "193T0183"))
    }

    @Test
    fun clave() {
        assertNull(Limites.revisarClave("itsm1234", false))
        assertNull(Limites.revisarClave("", true))
        assertNotNull(Limites.revisarClave("", false))
        assertNotNull(Limites.revisarClave("corta", false))
        assertNotNull(Limites.revisarClave("con espacio", false))
        assertNotNull(Limites.revisarClave("a".repeat(31), false))
    }

    @Test
    fun telefono() {
        assertNull(Limites.revisarTelefono(""))
        assertNull(Limites.revisarTelefono("2351000001"))
        assertNotNull(Limites.revisarTelefono("235100"))
        assertNotNull(Limites.revisarTelefono("23510000aa"))
    }

    @Test
    fun actividad() {
        assertNull(Limites.revisarActividad("Fútbol", "", 25))
        assertNotNull(Limites.revisarActividad("Fu", "", 25))
        assertNotNull(Limites.revisarActividad("Fútbol", "", 0))
        assertNotNull(Limites.revisarActividad("Fútbol", "", 101))
        assertNotNull(Limites.revisarActividad("Fútbol", "", null))
        assertNotNull(Limites.revisarActividad("Fútbol", "x".repeat(201), 10))
    }

    @Test
    fun fecha() {
        assertNull(Limites.revisarFecha("2026-01-15"))
        assertNull(Limites.revisarFecha(Limites.hoy()))
        assertNotNull(Limites.revisarFecha("15/01/2026"))
        assertNotNull(Limites.revisarFecha("2999-01-01"))
    }

    @Test
    fun descripcionEvidencia() {
        assertNull(Limites.revisarDescripcionEvidencia("Se jugó un partido"))
        assertNotNull(Limites.revisarDescripcionEvidencia("x".repeat(301)))
    }

    @Test
    fun nombreCompletoSinMaterno() {
        assertEquals("Ana López", nombreCompleto("Ana", "López", ""))
        assertEquals("Ana López Cruz", nombreCompleto("Ana", "López", "Cruz"))
    }
}
