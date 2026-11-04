package com.cristinachdr.mitacografo.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class AvisoProgramadoTest {
    private val madrid = ZoneId.of("Europe/Madrid")
    private val inicio: Instant = LocalDateTime.of(2026, 11, 2, 6, 0).atZone(madrid).toInstant()
    private val jornada = Jornada(id = 7, inicio = inicio, zonaInicio = madrid, kmInicio = 0)

    private fun horaMadrid(
        hora: Int,
        minuto: Int,
    ): Instant = LocalDateTime.of(2026, 11, 2, hora, minuto).atZone(madrid).toInstant()

    @Test
    fun `con los avisos por defecto salta a las 12h30, 13h, 14h30 y 15h del inicio`() {
        val avisos = calcularAvisos(jornada, AvisoConfig.POR_DEFECTO, ahora = inicio)

        assertEquals(
            listOf(horaMadrid(18, 30), horaMadrid(19, 0), horaMadrid(20, 30), horaMadrid(21, 0)),
            avisos.map { it.disparo },
        )
        assertTrue(avisos.all { it.jornadaId == 7L })
    }

    @Test
    fun `los avisos desactivados no se programan`() {
        val config = AvisoConfig.POR_DEFECTO.mapIndexed { i, aviso -> aviso.copy(activo = i % 2 == 0) }

        val avisos = calcularAvisos(jornada, config, ahora = inicio)

        assertEquals(listOf(horaMadrid(18, 30), horaMadrid(20, 30)), avisos.map { it.disparo })
    }

    @Test
    fun `si la jornada se apunta con retraso, los avisos ya pasados se saltan`() {
        // Son las 19:15: ya han pasado los avisos de 12h30 y 13h.
        val avisos = calcularAvisos(jornada, AvisoConfig.POR_DEFECTO, ahora = horaMadrid(19, 15))

        assertEquals(listOf(horaMadrid(20, 30), horaMadrid(21, 0)), avisos.map { it.disparo })
    }

    @Test
    fun `una jornada finalizada no tiene avisos`() {
        val finalizada = jornada.copy(fin = horaMadrid(16, 0), zonaFin = madrid)

        assertEquals(emptyList<AvisoProgramado>(), calcularAvisos(finalizada, AvisoConfig.POR_DEFECTO, inicio))
    }

    @Test
    fun `los avisos salen ordenados y sin repetir`() {
        val config =
            listOf(
                AvisoConfig(Duration.ofHours(15)),
                AvisoConfig(Duration.ofHours(13)),
                AvisoConfig(Duration.ofHours(13)),
            )

        val avisos = calcularAvisos(jornada, config, ahora = inicio)

        assertEquals(listOf(Duration.ofHours(13), Duration.ofHours(15)), avisos.map { it.desdeInicio })
    }

    @Test
    fun `un cambio de hora no desplaza los avisos`() {
        // Empieza a las 20:00 del 24/10; a las 03:00 del 25/10 el reloj vuelve a las 02:00.
        val antesDelCambio = LocalDateTime.of(2026, 10, 24, 20, 0).atZone(madrid).toInstant()
        val nocturna = jornada.copy(inicio = antesDelCambio)

        val primero = calcularAvisos(nocturna, AvisoConfig.POR_DEFECTO, antesDelCambio).first()

        // 12h30 reales después: en el reloj del móvil se ven las 07:30, no las 08:30.
        assertEquals(LocalDateTime.of(2026, 10, 25, 7, 30), LocalDateTime.ofInstant(primero.disparo, madrid))
    }
}
