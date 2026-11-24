package com.cristinachdr.mitacografo.feature.home

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class AvisoInicioTest {
    private val madrid = ZoneId.of("Europe/Madrid")
    private val ahora = ZonedDateTime.of(2026, 11, 20, 19, 0, 0, 0, madrid)

    private fun conJornada(horas: Long) =
        HomeUiState(
            hoy = LocalDate.of(2026, 11, 20),
            ahora = ahora.toInstant(),
            jornada =
                JornadaEnCursoUi(
                    inicio = ahora.minusHours(horas),
                    transcurrido = Duration.ofHours(horas),
                    kmInicio = 0,
                    proximoAviso = null,
                ),
        )

    private fun descansando(horas: Long) =
        HomeUiState(
            hoy = LocalDate.of(2026, 11, 20),
            ahora = ahora.toInstant(),
            descanso = PausaUi(ahora.minusHours(horas), Duration.ofHours(horas)),
        )

    @Test
    fun `sin nada que avisar`() {
        assertEquals(emptyList<AvisoInicio>(), avisosInicio(conJornada(8), ahora.plusDays(3)))
    }

    @Test
    fun `a las 13 h y a las 15 h de jornada`() {
        assertEquals(listOf(AvisoInicio.Jornada13h), avisosInicio(conJornada(13), null))
        assertEquals(listOf(AvisoInicio.Jornada15h), avisosInicio(conJornada(15), null))
    }

    @Test
    fun `el semanal, si queda menos de un dia o ya ha pasado el plazo`() {
        val pronto = ahora.plusHours(20)
        val pasado = ahora.minusHours(2)

        assertEquals(listOf(AvisoInicio.SemanalPronto(pronto)), avisosInicio(conJornada(8), pronto))
        assertEquals(listOf(AvisoInicio.SemanalVencido(pasado)), avisosInicio(conJornada(8), pasado))
        // Con 6 h o menos, el aviso más serio (MT-56).
        val muyPronto = ahora.plusHours(5)
        assertEquals(listOf(AvisoInicio.SemanalMuyPronto(muyPronto)), avisosInicio(conJornada(8), muyPronto))
    }

    @Test
    fun `no avisa del semanal si ya lleva 24 h descansando`() {
        val pasado = ahora.minusHours(2)

        assertEquals(emptyList<AvisoInicio>(), avisosInicio(descansando(30), pasado))
        assertEquals(listOf(AvisoInicio.SemanalVencido(pasado)), avisosInicio(descansando(10), pasado))
    }
}
