package com.cristinachdr.mitacografo.domain.usecase

import com.cristinachdr.mitacografo.domain.fake.FakeAlarmScheduler
import com.cristinachdr.mitacografo.domain.fake.FakeJornadaRepository
import com.cristinachdr.mitacografo.domain.fake.FakeSettingsRepository
import com.cristinachdr.mitacografo.domain.model.Ajustes
import com.cristinachdr.mitacografo.domain.model.AvisoConfig
import com.cristinachdr.mitacografo.domain.model.Continente
import com.cristinachdr.mitacografo.domain.model.ErrorJornada
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.PreferenciasPaises
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

    /** Avisos "programados" en el sistema (falso) durante el test. */
    private val alarmas = FakeAlarmScheduler()

    private fun gestor(
        repo: FakeJornadaRepository,
        ajustes: Ajustes = Ajustes(),
    ) = GestorAvisos(repo, FakeSettingsRepository(ajustes), alarmas, reloj)

    private fun iniciar(repo: FakeJornadaRepository) = IniciarJornada(repo, gestor(repo), reloj)

    private fun finalizar(repo: FakeJornadaRepository) = FinalizarJornada(repo, gestor(repo), reloj)

    private fun editar(repo: FakeJornadaRepository) = EditarJornada(repo, gestor(repo), reloj)

    private fun abiertaHoy(id: Long = 2) =
        Jornada(id = id, inicio = hora(2, 6), zonaInicio = madrid, kmInicio = 100_480)

    @Nested
    inner class Iniciar {
        @Test
        fun `crea una jornada abierta con los datos limpios`() =
            runTest {
                val repo = FakeJornadaRepository(listOf(ayer))

                val resultado =
                    iniciar(repo)(
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

                val resultado = iniciar(repo)(DatosInicio(hora(2, 19), madrid, 100_480))

                assertEquals(ResultadoJornada.Invalida(ErrorJornada.JORNADA_YA_ABIERTA), resultado)
                assertEquals(2, repo.guardadas.size)
            }

        @Test
        fun `no deja empezar antes de que terminara la anterior`() =
            runTest {
                val repo = FakeJornadaRepository(listOf(ayer))

                val resultado = iniciar(repo)(DatosInicio(hora(1, 16), madrid, 100_480))

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
                    finalizar(repo)(
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

                val resultado = finalizar(repo)(DatosFin(hora(2, 17), madrid, kmFin = 101_000))

                assertEquals(ResultadoJornada.Invalida(ErrorJornada.SIN_JORNADA_ABIERTA), resultado)
            }

        @Test
        fun `con datos incorrectos no guarda nada y devuelve todos los errores`() =
            runTest {
                val repo = FakeJornadaRepository(listOf(abiertaHoy()))

                val resultado =
                    finalizar(repo)(
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

                val resultado = editar(repo)(ayer.copy(kmFin = 100_500, observaciones = " "))

                assertEquals(ResultadoJornada.Ok(ayer.copy(kmFin = 100_500, observaciones = null)), resultado)
                assertEquals(500, repo.obtenerPorId(1)?.kmRecorridos)
            }

        @Test
        fun `no deja reabrir una jornada si ya hay otra abierta`() =
            runTest {
                val repo = FakeJornadaRepository(listOf(ayer, abiertaHoy()))

                val resultado = editar(repo)(ayer.copy(fin = null, zonaFin = null, kmFin = null))

                assertEquals(ResultadoJornada.Invalida(ErrorJornada.JORNADA_YA_ABIERTA), resultado)
            }

        @Test
        fun `no deja que la jornada corregida se solape con otra`() =
            runTest {
                val hoy = abiertaHoy().copy(fin = hora(2, 16), zonaFin = madrid, kmFin = 100_900)
                val repo = FakeJornadaRepository(listOf(ayer, hoy))

                // Ayer terminaba a las 17:00 del día 1; se corrige a las 07:00 del día 2 (hoy empezó a las 06:00).
                val resultado = editar(repo)(ayer.copy(fin = hora(2, 7)))

                assertEquals(ResultadoJornada.Invalida(ErrorJornada.SOLAPA_CON_OTRA_JORNADA), resultado)
                assertEquals(hora(1, 17), repo.obtenerPorId(1)?.fin)
            }

        @Test
        fun `una jornada pegada a la siguiente no se solapa`() =
            runTest {
                val repo = FakeJornadaRepository(listOf(ayer, abiertaHoy()))

                val resultado = editar(repo)(ayer.copy(fin = hora(2, 6)))

                assertTrue(resultado is ResultadoJornada.Ok)
            }

        @Test
        fun `una jornada que no existe no se puede editar`() =
            runTest {
                val resultado = editar(FakeJornadaRepository())(ayer)

                assertEquals(ResultadoJornada.Invalida(ErrorJornada.JORNADA_NO_EXISTE), resultado)
            }
    }

    @Nested
    inner class Borrar {
        @Test
        fun `borra la jornada y quita sus avisos`() =
            runTest {
                val repo = FakeJornadaRepository(listOf(ayer))
                iniciar(repo)(DatosInicio(hora(2, 19), madrid, 100_480))
                val abierta = repo.obtenerAbierta()!!
                assertTrue(alarmas.programados.isNotEmpty())

                val resultado = BorrarJornada(repo, gestor(repo))(abierta.id)

                assertEquals(ResultadoJornada.Ok(Unit), resultado)
                assertEquals(listOf(ayer), repo.guardadas)
                assertTrue(alarmas.programados.isEmpty())
            }

        @Test
        fun `una jornada que no existe no se puede borrar`() =
            runTest {
                val resultado = BorrarJornada(FakeJornadaRepository(), gestor(FakeJornadaRepository()))(7)

                assertEquals(ResultadoJornada.Invalida(ErrorJornada.JORNADA_NO_EXISTE), resultado)
            }
    }

    @Nested
    inner class Avisos {
        @Test
        fun `al iniciar se programan los avisos que aun no han pasado`() =
            runTest {
                // Son las 20:00 y la jornada empezó a las 06:00: ya pasaron los de 12 h 30 y 13 h.
                val repo = FakeJornadaRepository()

                iniciar(repo)(DatosInicio(hora(2, 6), madrid, 0))

                assertEquals(listOf(hora(2, 20, 30), hora(2, 21)), alarmas.programados.map { it.disparo })
            }

        @Test
        fun `al finalizar se cancelan todos`() =
            runTest {
                val repo = FakeJornadaRepository()
                iniciar(repo)(DatosInicio(hora(2, 19), madrid, 0))

                finalizar(repo)(DatosFin(hora(2, 20), madrid, 100))

                assertEquals(emptyList<Any>(), alarmas.programados)
            }

        @Test
        fun `al cambiar la hora de inicio los avisos se mueven`() =
            runTest {
                val repo = FakeJornadaRepository(listOf(abiertaHoy()))
                gestor(repo).reprogramarJornadaAbierta()

                editar(repo)(abiertaHoy().copy(inicio = hora(2, 8)))

                // Desde las 08:00: 20:30, 21:00, 22:30 y 23:00.
                assertEquals(
                    listOf(hora(2, 20, 30), hora(2, 21), hora(2, 22, 30), hora(2, 23)),
                    alarmas.programados.map { it.disparo },
                )
            }

        @Test
        fun `al desactivar un aviso en ajustes deja de estar programado`() =
            runTest {
                val repo = FakeJornadaRepository(listOf(abiertaHoy().copy(inicio = hora(2, 9))))
                val settings = FakeSettingsRepository()
                val gestor = GestorAvisos(repo, settings, alarmas, reloj)
                gestor.reprogramarJornadaAbierta()

                CambiarAjustes(settings, gestor)() { ajustes ->
                    ajustes.copy(avisos = ajustes.avisos.map { it.copy(activo = it != AvisoConfig.POR_DEFECTO[0]) })
                }

                // Sin el de 12 h 30 (21:30): quedan 13 h, 14 h 30 y 15 h.
                assertEquals(
                    listOf(hora(2, 22), hora(2, 23, 30), hora(3, 0)),
                    alarmas.programados.map { it.disparo },
                )
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
        fun `los paises recientes van del mas nuevo al mas antiguo y sin repetir`() =
            runTest {
                val antes =
                    ayer.copy(
                        id = 2,
                        inicio = hora(1, 6).minusSeconds(86_400),
                        paisInicio = "es",
                        paisFin = "FR",
                    )
                val hoy = Jornada(id = 3, inicio = hora(2, 6), zonaInicio = madrid, kmInicio = 0, paisInicio = "DE")
                val repo = FakeJornadaRepository(listOf(antes, ayer.copy(paisInicio = " ", paisFin = "FR"), hoy))

                assertEquals(listOf("DE", "FR", "ES"), ObtenerPaisesRecientes(repo)())
                assertEquals(listOf("DE"), ObtenerPaisesRecientes(repo)(maximo = 1))
            }

        @Test
        fun `las preferencias de pais juntan los recientes y lo elegido en Ajustes`() =
            runTest {
                val repo = FakeJornadaRepository(listOf(ayer))
                val ajustes = Ajustes(paisesHabituales = setOf("DE"), continentes = setOf(Continente.EUROPA))
                val preferencias =
                    ObtenerPreferenciasPaises(ObtenerPaisesRecientes(repo), FakeSettingsRepository(ajustes))()

                assertEquals(PreferenciasPaises(listOf("FR"), setOf("DE"), setOf(Continente.EUROPA)), preferencias)
                assertTrue(preferencias.hayFiltro)
                assertTrue(!PreferenciasPaises(recientes = listOf("FR")).hayFiltro)
            }

        @Test
        fun `la jornada actual se actualiza al iniciar y al finalizar`() =
            runTest {
                val repo = FakeJornadaRepository(listOf(ayer))
                val actual = ObtenerJornadaActual(repo)

                assertNull(actual().first())
                iniciar(repo)(DatosInicio(hora(2, 6), madrid, 100_480))
                assertEquals(hora(2, 6), actual().first()?.inicio)
                finalizar(repo)(DatosFin(hora(2, 16), madrid, 100_900))
                assertNull(actual().first())
            }
    }
}
