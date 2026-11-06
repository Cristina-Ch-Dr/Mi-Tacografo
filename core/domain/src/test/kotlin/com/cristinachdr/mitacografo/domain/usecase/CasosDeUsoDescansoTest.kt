package com.cristinachdr.mitacografo.domain.usecase

import com.cristinachdr.mitacografo.domain.fake.FakeDescansoRepository
import com.cristinachdr.mitacografo.domain.fake.FakeJornadaRepository
import com.cristinachdr.mitacografo.domain.model.ClaseDescanso
import com.cristinachdr.mitacografo.domain.model.Descanso
import com.cristinachdr.mitacografo.domain.model.ErrorDescanso
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.ResultadoDescanso
import com.cristinachdr.mitacografo.domain.model.TipoDescanso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class CasosDeUsoDescansoTest {
    private val madrid = ZoneId.of("Europe/Madrid")

    private fun hora(
        dia: Int,
        h: Int,
        m: Int = 0,
    ): Instant = LocalDateTime.of(2026, 11, dia, h, m).atZone(madrid).toInstant()

    /** "Ahora": lunes 2/11/2026 a las 20:00 en Madrid. */
    private val reloj = Clock.fixed(hora(2, 20), madrid)

    private fun descanso(
        id: Long,
        inicio: Instant,
        fin: Instant?,
        tipo: TipoDescanso = TipoDescanso.DESCANSO,
    ) = Descanso(id = id, inicio = inicio, zonaInicio = madrid, fin = fin, zonaFin = fin?.let { madrid }, tipo = tipo)

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
        )

    private val hoyEnCurso =
        Jornada(id = 2, inicio = hora(2, 8), zonaInicio = madrid, kmInicio = 100_480, paisInicio = "FR")

    @Nested
    inner class Guardar {
        @Test
        fun `guarda un descanso nuevo con el país en mayúsculas y sin espacios de más`() =
            runTest {
                val repo = FakeDescansoRepository()
                val resultado =
                    GuardarDescanso(
                        repo,
                        reloj,
                    )(descanso(0, hora(1, 17), hora(2, 6)).copy(pais = " fr ", observaciones = " "))

                assertTrue(resultado is ResultadoDescanso.Ok)
                assertEquals("FR", repo.guardados.single().pais)
                assertNull(repo.guardados.single().observaciones)
            }

        @Test
        fun `no se puede solapar con otro descanso`() =
            runTest {
                val repo = FakeDescansoRepository(listOf(descanso(1, hora(1, 17), hora(2, 6))))
                val resultado = GuardarDescanso(repo, reloj)(descanso(0, hora(2, 5), hora(2, 7)))

                assertEquals(ResultadoDescanso.Invalido(ErrorDescanso.SOLAPA_CON_OTRO_DESCANSO), resultado)
            }

        @Test
        fun `al corregir uno, no choca consigo mismo`() =
            runTest {
                val repo = FakeDescansoRepository(listOf(descanso(1, hora(1, 17), hora(2, 6))))
                val resultado = GuardarDescanso(repo, reloj)(descanso(1, hora(1, 18), hora(2, 6)))

                assertEquals(ResultadoDescanso.Ok(1L), resultado)
                assertEquals(hora(1, 18), repo.guardados.single().inicio)
            }

        @Test
        fun `el fin tiene que ser posterior al inicio y no estar en el futuro`() =
            runTest {
                val resultado = GuardarDescanso(FakeDescansoRepository(), reloj)(descanso(0, hora(2, 21), hora(2, 21)))

                assertEquals(
                    ResultadoDescanso.Invalido(
                        setOf(
                            ErrorDescanso.INICIO_EN_EL_FUTURO,
                            ErrorDescanso.FIN_NO_POSTERIOR_AL_INICIO,
                            ErrorDescanso.FIN_EN_EL_FUTURO,
                        ),
                    ),
                    resultado,
                )
            }

        @Test
        fun `corregir uno que ya no existe falla`() =
            runTest {
                val resultado = GuardarDescanso(FakeDescansoRepository(), reloj)(descanso(7, hora(1, 17), hora(2, 6)))
                assertEquals(ResultadoDescanso.Invalido(ErrorDescanso.DESCANSO_NO_EXISTE), resultado)
            }
    }

    @Nested
    inner class Pausas {
        private fun iniciar(
            descansos: FakeDescansoRepository,
            jornadas: FakeJornadaRepository,
        ) = IniciarPausa(descansos, jornadas, GuardarDescanso(descansos, reloj), reloj)

        @Test
        fun `iniciar pausa la empieza ahora, en el país de la jornada`() =
            runTest {
                val descansos = FakeDescansoRepository()
                iniciar(descansos, FakeJornadaRepository(listOf(hoyEnCurso)))()

                val pausa = descansos.guardados.single()
                assertEquals(TipoDescanso.PAUSA, pausa.tipo)
                assertEquals(reloj.instant(), pausa.inicio)
                assertEquals("FR", pausa.pais)
                assertTrue(pausa.enCurso)
            }

        @Test
        fun `sin jornada en curso no hay pausa`() =
            runTest {
                val resultado = iniciar(FakeDescansoRepository(), FakeJornadaRepository(listOf(ayer)))()
                assertEquals(ResultadoDescanso.Invalido(ErrorDescanso.SIN_JORNADA_ABIERTA), resultado)
            }

        @Test
        fun `no se pueden tener dos pausas a la vez`() =
            runTest {
                val descansos = FakeDescansoRepository(listOf(descanso(1, hora(2, 19), null, TipoDescanso.PAUSA)))
                val resultado = iniciar(descansos, FakeJornadaRepository(listOf(hoyEnCurso)))()
                assertEquals(ResultadoDescanso.Invalido(ErrorDescanso.YA_HAY_UNO_EN_CURSO), resultado)
            }

        @Test
        fun `terminar pausa le pone la hora de ahora`() =
            runTest {
                val descansos = FakeDescansoRepository(listOf(descanso(1, hora(2, 19, 15), null, TipoDescanso.PAUSA)))
                TerminarPausa(descansos, reloj)()

                assertEquals(reloj.instant(), descansos.guardados.single().fin)
                assertEquals(madrid, descansos.guardados.single().zonaFin)
            }

        @Test
        fun `al finalizar la jornada antes de que empezara la pausa, la pausa se borra`() =
            runTest {
                val descansos = FakeDescansoRepository(listOf(descanso(1, hora(2, 19, 15), null, TipoDescanso.PAUSA)))
                TerminarPausa(descansos, reloj)(hasta = hora(2, 19))

                assertTrue(descansos.guardados.isEmpty())
            }

        @Test
        fun `terminar sin pausa en curso falla`() =
            runTest {
                assertEquals(
                    ResultadoDescanso.Invalido(ErrorDescanso.SIN_PAUSA_EN_CURSO),
                    TerminarPausa(FakeDescansoRepository(), reloj)(),
                )
            }
    }

    @Nested
    inner class Propuesta {
        @Test
        fun `propone un descanso desde el fin de la última jornada, en su país`() =
            runTest {
                val propuesta = ProponerDescansoPrevio(FakeJornadaRepository(listOf(ayer)), FakeDescansoRepository())()

                assertEquals(PropuestaDescanso(desde = hora(1, 17), zona = madrid, pais = "FR"), propuesta)
            }

        @Test
        fun `si ya hay un descanso apuntado después, no propone nada`() =
            runTest {
                val descansos = FakeDescansoRepository(listOf(descanso(1, hora(1, 17), hora(2, 5))))
                assertNull(ProponerDescansoPrevio(FakeJornadaRepository(listOf(ayer)), descansos)())
            }

        @Test
        fun `una pausa no cuenta como descanso apuntado`() =
            runTest {
                val descansos =
                    FakeDescansoRepository(listOf(descanso(1, hora(1, 12), hora(1, 13), TipoDescanso.PAUSA)))
                assertEquals(
                    hora(1, 17),
                    ProponerDescansoPrevio(FakeJornadaRepository(listOf(ayer)), descansos)()?.desde,
                )
            }

        @Test
        fun `sin jornadas anteriores o con una en curso no propone nada`() =
            runTest {
                assertNull(ProponerDescansoPrevio(FakeJornadaRepository(), FakeDescansoRepository())())
                assertNull(
                    ProponerDescansoPrevio(FakeJornadaRepository(listOf(ayer, hoyEnCurso)), FakeDescansoRepository())(),
                )
            }
    }

    @Test
    fun `la lista sale con su clase, del más reciente al más antiguo`() =
        runTest {
            val descansos =
                FakeDescansoRepository(
                    listOf(
                        descanso(1, hora(1, 17), hora(2, 5)),
                        descanso(2, hora(2, 12), hora(2, 12, 30), TipoDescanso.PAUSA),
                    ),
                )
            val lista = ObtenerDescansos(descansos)().first()

            assertEquals(listOf(2L, 1L), lista.map { it.descanso.id })
            assertEquals(listOf(ClaseDescanso.PAUSA, ClaseDescanso.DIARIO_NORMAL), lista.map { it.clase })
        }
}
