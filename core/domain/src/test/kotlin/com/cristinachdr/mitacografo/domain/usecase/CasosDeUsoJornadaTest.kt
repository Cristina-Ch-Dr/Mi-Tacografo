package com.cristinachdr.mitacografo.domain.usecase

import com.cristinachdr.mitacografo.domain.fake.FakeJornadaRepository
import com.cristinachdr.mitacografo.domain.model.ErrorJornada
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.ResultadoJornada
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class CasosDeUsoJornadaTest {
    private val madrid = ZoneId.of("Europe/Madrid")

    private fun hora(
        dia: Int,
        h: Int,
        m: Int = 0,
    ): Instant = LocalDateTime.of(2026, 11, dia, h, m).atZone(madrid).toInstant()

    /** "Ahora" en todos los tests: lunes 2/11/2026 a las 20:00 en Madrid. */
    private val reloj = Clock.fixed(hora(2, 20), madrid)

    private val ayer =
        Jornada(
            id = 1,
            inicio = hora(1, 6),
            zonaInicio = madrid,
            kmInicio = 100_000,
            fin = hora(1, 17),
            zonaFin = madrid,
            kmFin = 100_480,
            paisFin = "FR",
            ciudadFin = "Lyon",
        )

    private fun abiertaHoy(id: Long = 2) =
        Jornada(id = id, inicio = hora(2, 6), zonaInicio = madrid, kmInicio = 100_480)

    @Nested
    inner class Iniciar {
        @Test
        fun `crea una jornada abierta con los datos limpios`() =
            runTest {
                val repo = FakeJornadaRepository(listOf(ayer))

                val resultado =
                    IniciarJornada(repo, reloj)(
                        DatosInicio(hora(2, 6), madrid, 100_480, pais = " fr ", ciudad = "  ", observaciones = "Carga"),
                    )

                val id = (resultado as ResultadoJornada.Ok).valor
                val creada = repo.obtenerPorId(id)!!
                assertTrue(creada.abierta)
                assertEquals("FR", creada.paisInicio)
                assertNull(creada.ciudadInicio)
                assertEquals("Carga", creada.observaciones)
            }

        @Test
        fun `no deja iniciar si ya hay una abierta`() =
            runTest {
                val repo = FakeJornadaRepository(listOf(ayer, abiertaHoy()))

                val resultado = IniciarJornada(repo, reloj)(DatosInicio(hora(2, 19), madrid, 100_480))

                assertEquals(ResultadoJornada.Invalida(ErrorJornada.JORNADA_YA_ABIERTA), resultado)
                assertEquals(2, repo.guardadas.size)
            }

        @Test
        fun `no deja empezar antes de que terminara la anterior`() =
            runTest {
                val repo = FakeJornadaRepository(listOf(ayer))

                val resultado = IniciarJornada(repo, reloj)(DatosInicio(hora(1, 16), madrid, 100_480))

                assertEquals(ResultadoJornada.Invalida(ErrorJornada.INICIO_ANTES_DEL_FIN_ANTERIOR), resultado)
            }
    }

    @Nested
    inner class Finalizar {
        @Test
        fun `cierra la jornada abierta`() =
            runTest {
                val repo = FakeJornadaRepository(listOf(ayer, abiertaHoy()))

                val resultado =
                    FinalizarJornada(repo, reloj)(
                        DatosFin(
                            hora(2, 17),
                            madrid,
                            kmFin = 101_000,
                            pais = "es",
                            conduccionTacografo = Duration.ofHours(8),
                        ),
                    )

                val finalizada = (resultado as ResultadoJornada.Ok).valor
                assertEquals(520, finalizada.kmRecorridos)
                assertEquals("ES", finalizada.paisFin)
                assertNull(repo.obtenerAbierta())
                assertEquals(finalizada, repo.obtenerPorId(2))
            }

        @Test
        fun `sin jornada abierta no hay nada que finalizar`() =
            runTest {
                val repo = FakeJornadaRepository(listOf(ayer))

                val resultado = FinalizarJornada(repo, reloj)(DatosFin(hora(2, 17), madrid, kmFin = 101_000))

                assertEquals(ResultadoJornada.Invalida(ErrorJornada.SIN_JORNADA_ABIERTA), resultado)
            }

        @Test
        fun `con datos incorrectos no guarda nada y devuelve todos los errores`() =
            runTest {
                val repo = FakeJornadaRepository(listOf(abiertaHoy()))

                val resultado =
                    FinalizarJornada(repo, reloj)(
                        DatosFin(hora(2, 7), madrid, kmFin = 1, conduccionTacografo = Duration.ofHours(2)),
                    )

                assertEquals(
                    ResultadoJornada.Invalida(
                        setOf(ErrorJornada.KM_FIN_MENOR_QUE_INICIO, ErrorJornada.CONDUCCION_MAYOR_QUE_JORNADA),
                    ),
                    resultado,
                )
                assertTrue(repo.obtenerAbierta()!!.abierta)
            }
    }

    @Nested
    inner class Editar {
        @Test
        fun `corrige una jornada antigua`() =
            runTest {
                val repo = FakeJornadaRepository(listOf(ayer))

                val resultado = EditarJornada(repo, reloj)(ayer.copy(kmFin = 100_500, observaciones = " "))

                assertEquals(ResultadoJornada.Ok(ayer.copy(kmFin = 100_500, observaciones = null)), resultado)
                assertEquals(500, repo.obtenerPorId(1)?.kmRecorridos)
            }

        @Test
        fun `no deja reabrir una jornada si ya hay otra abierta`() =
            runTest {
                val repo = FakeJornadaRepository(listOf(ayer, abiertaHoy()))

                val resultado = EditarJornada(repo, reloj)(ayer.copy(fin = null, zonaFin = null, kmFin = null))

                assertEquals(ResultadoJornada.Invalida(ErrorJornada.JORNADA_YA_ABIERTA), resultado)
            }

        @Test
        fun `una jornada que no existe no se puede editar`() =
            runTest {
                val resultado = EditarJornada(FakeJornadaRepository(), reloj)(ayer)

                assertEquals(ResultadoJornada.Invalida(ErrorJornada.JORNADA_NO_EXISTE), resultado)
            }
    }

    @Nested
    inner class Consultas {
        @Test
        fun `la propuesta de inicio es donde termino la jornada anterior`() =
            runTest {
                val propuesta = ProponerDatosInicio(FakeJornadaRepository(listOf(ayer)))()

                assertEquals(PropuestaInicio(kmInicio = 100_480, pais = "FR", ciudad = "Lyon"), propuesta)
            }

        @Test
        fun `sin jornadas anteriores no hay propuesta`() =
            runTest {
                assertEquals(PropuestaInicio(), ProponerDatosInicio(FakeJornadaRepository())())
            }

        @Test
        fun `la jornada actual se actualiza al iniciar y al finalizar`() =
            runTest {
                val repo = FakeJornadaRepository(listOf(ayer))
                val actual = ObtenerJornadaActual(repo)

                assertNull(actual().first())
                IniciarJornada(repo, reloj)(DatosInicio(hora(2, 6), madrid, 100_480))
                assertEquals(hora(2, 6), actual().first()?.inicio)
                FinalizarJornada(repo, reloj)(DatosFin(hora(2, 16), madrid, 100_900))
                assertNull(actual().first())
            }
    }
}
