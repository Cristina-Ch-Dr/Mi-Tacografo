package com.cristinachdr.mitacografo.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

class DuracionesTest {
    private val madrid = ZoneId.of("Europe/Madrid")
    private val kyiv = ZoneId.of("Europe/Kyiv")

    @Test
    fun `T18 - jornada con cambio de hora de octubre dura las horas reales`() {
        // 25/10/2026: a las 03:00 los relojes vuelven a las 02:00 en España.
        val inicio = LocalDateTime.of(2026, 10, 24, 22, 0).atZone(madrid).toInstant()
        val fin = LocalDateTime.of(2026, 10, 25, 6, 0).atZone(madrid).toInstant()

        // En el reloj pasan 8 h, pero en realidad son 9 h.
        assertEquals(Duration.ofHours(9), duracionEntre(inicio, fin))
    }

    @Test
    fun `T19 - jornada que empieza en Madrid y acaba en Kyiv`() {
        val inicio = LocalDateTime.of(2026, 11, 2, 8, 0).atZone(madrid).toInstant()
        // Las 18:00 en Kyiv son las 17:00 en Madrid (UTC+2 frente a UTC+1).
        val fin = LocalDateTime.of(2026, 11, 2, 18, 0).atZone(kyiv).toInstant()

        assertEquals(Duration.ofHours(9), duracionEntre(inicio, fin))
    }

    @Test
    fun `fin anterior al inicio lanza error`() {
        val inicio = LocalDateTime.of(2026, 11, 2, 8, 0).atZone(madrid).toInstant()
        assertThrows<IllegalArgumentException> {
            duracionEntre(inicio, inicio.minusSeconds(60))
        }
    }
}
