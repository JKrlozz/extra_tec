package itsm.extra_tec.Modelo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NumeroControlTest {

    @Test
    fun aceptaElFormato000T0000() {
        assertTrue(NumeroControl.esValido("232T0182"))
        assertTrue(NumeroControl.esValido("219t0383"))
    }

    @Test
    fun rechazaFormatosIncorrectos() {
        assertFalse(NumeroControl.esValido("2320182"))
        assertFalse(NumeroControl.esValido("232T018"))
        assertFalse(NumeroControl.esValido("23T0182"))
        assertFalse(NumeroControl.esValido("232X0182"))
        assertFalse(NumeroControl.esValido(""))
    }

    @Test
    fun normalizaAMayusculas() {
        assertEquals("232T0182", NumeroControl.normalizar(" 232t0182 "))
    }
}
