package com.cristinachdr.mitacografo.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Duration

class ListaAvisosTest {
    private val porDefecto = AvisoConfig.POR_DEFECTO

    private fun horas(
        h: Long,
        min: Long = 0,
    ) = Duration.ofHours(h).plusMinutes(min)

    @Test
    fun `los de por defecto salen de restar el descanso a 24 h`() {
        assertEquals(
            listOf(horas(12, 30), horas(24 - 11), horas(14, 30), horas(24 - 9)),
            porDefecto.map { it.desdeInicio },
        )
    }

    @Test
    fun `se puede poner entre 1 h y 24 h, ambas incluidas`() {
        assertNull(porDefecto.errorAviso(horas(1)))
        assertNull(porDefecto.errorAviso(horas(24)))
        assertEquals(ErrorAviso.FUERA_DE_RANGO, porDefecto.errorAviso(horas(0, 59)))
        assertEquals(ErrorAviso.FUERA_DE_RANGO, porDefecto.errorAviso(horas(24, 1)))
    }

    @Test
    fun `no se puede repetir la hora de otro aviso`() {
        assertEquals(ErrorAviso.REPETIDO, porDefecto.errorAviso(horas(13)))
        // Cambiar el de 12 h 30 a 13 h choca con el de 13 h…
        assertEquals(ErrorAviso.REPETIDO, porDefecto.errorAviso(horas(13), editando = 0))
        // …pero guardarlo con su misma hora no.
        assertNull(porDefecto.errorAviso(horas(12, 30), editando = 0))
    }

    @Test
    fun `caben como mucho 8 avisos, pero se pueden cambiar`() {
        val ocho = (1L..8L).map { AvisoConfig(horas(it)) }

        assertEquals(ErrorAviso.DEMASIADOS, ocho.errorAviso(horas(20)))
        assertNull(ocho.errorAviso(horas(20), editando = 7))
    }

    @Test
    fun `un aviso nuevo se coloca en su sitio y empieza activado`() {
        val avisos = porDefecto.conAviso(horas(14))

        assertEquals(
            listOf(horas(12, 30), horas(13), horas(14), horas(14, 30), horas(15)),
            avisos.map { it.desdeInicio },
        )
        assertEquals(true, avisos[2].activo)
    }

    @Test
    fun `cambiar la hora conserva si estaba activado y reordena`() {
        val desactivado = porDefecto.mapIndexed { i, it -> if (i == 0) it.copy(activo = false) else it }

        val avisos = desactivado.conAviso(horas(16), editando = 0)

        assertEquals(listOf(horas(13), horas(14, 30), horas(15), horas(16)), avisos.map { it.desdeInicio })
        assertEquals(AvisoConfig(horas(16), activo = false), avisos.last())
    }

    @Test
    fun `quitar un aviso`() {
        assertEquals(listOf(horas(12, 30), horas(14, 30), horas(15)), porDefecto.sinAviso(1).map { it.desdeInicio })
    }
}
