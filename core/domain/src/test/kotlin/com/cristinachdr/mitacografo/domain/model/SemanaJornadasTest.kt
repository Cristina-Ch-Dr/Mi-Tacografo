package com.cristinachdr.mitacografo.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class SemanaJornadasTest {
    private val madrid = ZoneId.of("Europe/Madrid")
    private val kyiv = ZoneId.of("Europe/Kyiv")

    private fun jornada(
        id: Long,
        dia: Int,
        hora: Int = 6,
        zona: ZoneId = madrid,
        km: Int? = 500,
        conduccion: Duration? = Duration.ofHours(9),
    ): Jornada {
        val inicio = LocalDateTime.of(2026, 11, dia, hora, 0).atZone(zona).toInstant()
        return Jornada(
            id = id,
            inicio = inicio,
            zonaInicio = zona,
            kmInicio = 1_000,
            fin = km?.let { inicio.plusSeconds(11 * 3600) },
            zonaFin = km?.let { zona },
            kmFin = km?.let { 1_000 + it },
            conduccionTacografo = conduccion,
        )
    }

    @Test
    fun `agrupa de lunes a domingo, de la semana mas reciente a la mas antigua`() {
        // 2/11/2026 es lunes y 8/11 domingo; el 9/11 empieza otra semana.
        val semanas = agruparPorSemana(listOf(jornada(1, 2), jornada(2, 8), jornada(3, 9), jornada(4, 1)))

        assertEquals(
            listOf(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 2), LocalDate.of(2026, 10, 26)),
            semanas.map { it.lunes },
        )
        assertEquals(listOf(2L, 1L), semanas[1].jornadas.map { it.id })
        assertEquals(LocalDate.of(2026, 11, 8), semanas[1].domingo)
    }

    @Test
    fun `suma km y conduccion de la semana`() {
        val semana = agruparPorSemana(listOf(jornada(1, 2, km = 400), jornada(2, 3, km = 600))).single()

        assertEquals(1_000, semana.km)
        assertEquals(Duration.ofHours(18), semana.conduccion)
        assertTrue(semana.conduccionCompleta)
    }

    @Test
    fun `si falta la conduccion de una jornada terminada la suma es incompleta`() {
        val semana = agruparPorSemana(listOf(jornada(1, 2), jornada(2, 3, conduccion = null))).single()

        assertEquals(Duration.ofHours(9), semana.conduccion)
        assertFalse(semana.conduccionCompleta)
    }

    @Test
    fun `la jornada en curso no cuenta como conduccion sin apuntar`() {
        val semana = agruparPorSemana(listOf(jornada(1, 2), jornada(2, 3, km = null, conduccion = null))).single()

        assertTrue(semana.conduccionCompleta)
        assertEquals(500, semana.km)
    }

    @Test
    fun `el dia se mira en la zona de inicio o en la de referencia`() {
        // Domingo 8/11 a las 23:30 en Madrid = lunes 9/11 a las 00:30 en Kyiv.
        val domingoNoche = jornada(1, 8, hora = 23, zona = madrid).let { it.copy(inicio = it.inicio.plusSeconds(1800)) }

        assertEquals(LocalDate.of(2026, 11, 2), agruparPorSemana(listOf(domingoNoche)).single().lunes)
        assertEquals(
            LocalDate.of(2026, 11, 9),
            agruparPorSemana(listOf(domingoNoche), zonaReferencia = kyiv).single().lunes,
        )
    }
}
