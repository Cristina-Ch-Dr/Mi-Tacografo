package com.cristinachdr.mitacografo.domain.usecase

import com.cristinachdr.mitacografo.domain.fake.FakeDescansoRepository
import com.cristinachdr.mitacografo.domain.fake.FakeJornadaRepository
import com.cristinachdr.mitacografo.domain.fake.FakeSettingsRepository
import com.cristinachdr.mitacografo.domain.model.Ajustes
import com.cristinachdr.mitacografo.domain.model.Jornada
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class ObtenerResumenTest {
    private val madrid = ZoneId.of("Europe/Madrid")

    /** "Ahora": jueves 5/11/2026 a las 20:00. */
    private val reloj = Clock.fixed(LocalDateTime.of(2026, 11, 5, 20, 0).atZone(madrid).toInstant(), madrid)

    private val martes =
        Jornada(
            id = 1,
            inicio = LocalDateTime.of(2026, 11, 3, 6, 0).atZone(madrid).toInstant(),
            zonaInicio = madrid,
            kmInicio = 1_000,
            fin = LocalDateTime.of(2026, 11, 3, 17, 0).atZone(madrid).toInstant(),
            zonaFin = madrid,
            kmFin = 1_600,
        )

    private val settings = FakeSettingsRepository(Ajustes(nombreConductor = "Conductor de prueba"))

    private fun obtener(repo: FakeJornadaRepository = FakeJornadaRepository(listOf(martes))) =
        ObtenerResumen(repo, FakeDescansoRepository(), settings, reloj)

    @Test
    fun `la semana actual empieza el lunes`() =
        runTest {
            assertEquals(LocalDate.of(2026, 11, 2), obtener().lunesActual())
        }

    @Test
    fun `el resumen de la semana suma sus jornadas`() =
        runTest {
            val resumen = obtener()(LocalDate.of(2026, 11, 2), 1).first()
            assertEquals(600, resumen.km)
        }

    @Test
    fun `el informe lleva el nombre del conductor y la hora de generacion`() =
        runTest {
            val informe = PrepararInforme(obtener(), settings, reloj)(LocalDate.of(2026, 11, 2), 2)

            assertEquals("Conductor de prueba", informe.conductor)
            assertEquals(reloj.instant(), informe.generado)
            assertEquals(LocalDate.of(2026, 11, 15), informe.resumen.hasta)
        }
}
