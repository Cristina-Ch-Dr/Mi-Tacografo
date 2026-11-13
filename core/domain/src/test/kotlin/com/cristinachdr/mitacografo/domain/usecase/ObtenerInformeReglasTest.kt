package com.cristinachdr.mitacografo.domain.usecase

import com.cristinachdr.mitacografo.domain.fake.FakeDescansoRepository
import com.cristinachdr.mitacografo.domain.fake.FakeJornadaRepository
import com.cristinachdr.mitacografo.domain.fake.FakeSettingsRepository
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.rules.ReglaId
import com.cristinachdr.mitacografo.domain.rules.Resultado
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

class ObtenerInformeReglasTest {
    private val madrid = ZoneId.of("Europe/Madrid")
    private val reloj = Clock.fixed(LocalDateTime.of(2026, 11, 20, 12, 0).atZone(madrid).toInstant(), madrid)

    private fun hora(
        dia: Int,
        h: Int,
    ) = LocalDateTime.of(2026, 11, dia, h, 0).atZone(madrid).toInstant()

    private val antigua =
        Jornada(
            id = 1,
            inicio = hora(3, 6),
            zonaInicio = madrid,
            kmInicio = 0,
            fin = hora(3, 18),
            zonaFin = madrid,
            kmFin = 600,
            conduccionTacografo = Duration.ofHours(9),
        )

    @Test
    fun `T24 - al corregir una jornada de hace dos semanas se recalcula su semana`() =
        runTest {
            val jornadas = FakeJornadaRepository(listOf(antigua))
            val obtener = ObtenerInformeReglas(jornadas, FakeDescansoRepository(), FakeSettingsRepository(), reloj)

            suspend fun diaria() =
                obtener()
                    .first()
                    .semanas
                    .single()
                    .jornadas
                    .single()
                    .resultado(ReglaId.CONDUCCION_DIARIA)

            assertTrue(diaria() is Resultado.Cumple)

            jornadas.guardar(antigua.copy(conduccionTacografo = Duration.ofHours(10).plusMinutes(30)))
            assertTrue(diaria() is Resultado.Excedido)
        }
}
