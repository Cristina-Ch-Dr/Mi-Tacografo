package com.cristinachdr.mitacografo.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

class JornadaTest {
    private val madrid = ZoneId.of("Europe/Madrid")
    private val kyiv = ZoneId.of("Europe/Kyiv")

    private fun jornadaAbierta(kmInicio: Int = 100_000) =
        Jornada(
            inicio = LocalDateTime.of(2026, 11, 2, 6, 0).atZone(madrid).toInstant(),
            zonaInicio = madrid,
            kmInicio = kmInicio,
        )

    @Test
    fun `una jornada sin fin esta abierta y aun no tiene km ni duracion`() {
        val jornada = jornadaAbierta()

        assertTrue(jornada.abierta)
        assertNull(jornada.kmRecorridos)
        assertNull(jornada.duracion)
    }

    @Test
    fun `kmRecorridos es la diferencia entre km final e inicial`() {
        val jornada = jornadaAbierta(kmInicio = 245_310).copy(kmFin = 245_872)

        assertEquals(562, jornada.kmRecorridos)
    }

    @Test
    fun `una jornada sin moverse tiene 0 km`() {
        val jornada = jornadaAbierta(kmInicio = 245_310).copy(kmFin = 245_310)

        assertEquals(0, jornada.kmRecorridos)
    }

    @Test
    fun `T18 - la duracion de la jornada es real aunque haya cambio de hora`() {
        val jornada =
            Jornada(
                inicio = LocalDateTime.of(2026, 10, 24, 22, 0).atZone(madrid).toInstant(),
                zonaInicio = madrid,
                kmInicio = 0,
                fin = LocalDateTime.of(2026, 10, 25, 6, 0).atZone(madrid).toInstant(),
                zonaFin = madrid,
            )

        assertFalse(jornada.abierta)
        assertEquals(Duration.ofHours(9), jornada.duracion)
    }

    @Test
    fun `T19 - la duracion es real aunque empiece en Madrid y acabe en Kyiv`() {
        val jornada =
            jornadaAbierta().copy(
                fin = LocalDateTime.of(2026, 11, 2, 18, 0).atZone(kyiv).toInstant(),
                zonaFin = kyiv,
            )

        // 06:00 en Madrid = 05:00 UTC; 18:00 en Kyiv = 16:00 UTC.
        assertEquals(Duration.ofHours(11), jornada.duracion)
    }
}
