package com.cristinachdr.mitacografo.domain.usecase

import com.cristinachdr.mitacografo.domain.fake.FakeAlarmScheduler
import com.cristinachdr.mitacografo.domain.fake.FakeJornadaRepository
import com.cristinachdr.mitacografo.domain.fake.FakeSettingsRepository
import com.cristinachdr.mitacografo.domain.fake.FakeViajeRepository
import com.cristinachdr.mitacografo.domain.model.ErrorViaje
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.ResultadoJornada
import com.cristinachdr.mitacografo.domain.model.ResultadoViaje
import com.cristinachdr.mitacografo.domain.model.Viaje
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

/** Datos inventados: solo horas y km. */
class CasosDeUsoViajeTest {
    private val madrid = ZoneId.of("Europe/Madrid")

    private fun hora(
        dia: Int,
        h: Int,
    ): Instant = LocalDateTime.of(2026, 11, dia, h, 0).atZone(madrid).toInstant()

    /** "Ahora": lunes 9/11/2026 a las 20:00. */
    private val reloj = Clock.fixed(hora(9, 20), madrid)

    private fun jornada(
        id: Long,
        dia: Int,
        km: Int,
        viajeId: Long?,
        pais: Pair<String, String> = "ES" to "FR",
    ) = Jornada(
        id = id,
        inicio = hora(dia, 6),
        zonaInicio = madrid,
        kmInicio = 1_000,
        paisInicio = pais.first,
        fin = hora(dia, 17),
        zonaFin = madrid,
        kmFin = 1_000 + km,
        paisFin = pais.second,
        conduccionTacografo = Duration.ofHours(8),
        viajeId = viajeId,
    )

    private val terminado =
        Viaje(id = 1, nombre = "Ida", inicio = hora(2, 5), zonaInicio = madrid, fin = hora(5, 18), zonaFin = madrid)

    @Test
    fun `los km del viaje son la suma de sus jornadas`() =
        runTest {
            val jornadas =
                FakeJornadaRepository(
                    listOf(
                        jornada(1, 2, 500, viajeId = 1),
                        jornada(2, 3, 600, viajeId = 1, pais = "FR" to "DE"),
                        jornada(3, 4, 450, viajeId = 1, pais = "DE" to "PL"),
                        jornada(4, 6, 300, viajeId = null),
                    ),
                )
            val resumen = ObtenerViajes(FakeViajeRepository(listOf(terminado)), jornadas)().first().single()

            assertEquals(1_550, resumen.km)
            assertEquals(3, resumen.numJornadas)
            assertEquals(Duration.ofHours(24), resumen.conduccion)
            assertEquals(listOf("ES", "FR", "DE", "PL"), resumen.paises)
            assertEquals(listOf(1L, 2L, 3L), resumen.jornadas.map { it.id })
        }

    @Nested
    inner class Iniciar {
        @Test
        fun `empieza ahora con el nombre que se le da`() =
            runTest {
                val viajes = FakeViajeRepository()
                val resultado = IniciarViaje(viajes, FakeJornadaRepository(), reloj)("  Murcia – Kyiv ")

                assertEquals(ResultadoViaje.Ok(1L), resultado)
                assertEquals("Murcia – Kyiv", viajes.guardados.single().nombre)
                assertEquals(reloj.instant(), viajes.guardados.single().inicio)
                assertTrue(viajes.guardados.single().enCurso)
            }

        @Test
        fun `si hay una jornada en curso se apunta en el viaje`() =
            runTest {
                val abierta = Jornada(id = 9, inicio = hora(9, 6), zonaInicio = madrid, kmInicio = 0)
                val jornadas = FakeJornadaRepository(listOf(abierta))
                IniciarViaje(FakeViajeRepository(), jornadas, reloj)("Ida")

                assertEquals(1L, jornadas.obtenerAbierta()?.viajeId)
            }

        @Test
        fun `no se puede tener dos viajes en curso ni uno sin nombre`() =
            runTest {
                val enCurso = Viaje(id = 1, nombre = "Ida", inicio = hora(8, 5), zonaInicio = madrid)
                val viajes = FakeViajeRepository(listOf(enCurso))

                assertEquals(
                    ResultadoViaje.Invalido(setOf(ErrorViaje.SIN_NOMBRE, ErrorViaje.YA_HAY_UNO_EN_CURSO)),
                    IniciarViaje(viajes, FakeJornadaRepository(), reloj)("  "),
                )
            }
    }

    @Test
    fun `finalizar le pone la hora de ahora`() =
        runTest {
            val viajes =
                FakeViajeRepository(listOf(Viaje(id = 1, nombre = "Ida", inicio = hora(8, 5), zonaInicio = madrid)))
            FinalizarViaje(viajes, reloj)()

            assertEquals(reloj.instant(), viajes.guardados.single().fin)
            assertEquals(ResultadoViaje.Invalido(ErrorViaje.SIN_VIAJE_EN_CURSO), FinalizarViaje(viajes, reloj)())
        }

    @Test
    fun `al iniciar jornada con un viaje en curso se apunta sola en el`() =
        runTest {
            val viajes =
                FakeViajeRepository(listOf(Viaje(id = 3, nombre = "Ida", inicio = hora(8, 5), zonaInicio = madrid)))
            val jornadas = FakeJornadaRepository()
            val gestor = GestorAvisos(jornadas, FakeSettingsRepository(), FakeAlarmScheduler(), reloj)
            val resultado = IniciarJornada(jornadas, viajes, gestor, reloj)(DatosInicio(hora(9, 6), madrid, 1_000))

            assertTrue(resultado is ResultadoJornada.Ok)
            assertEquals(3L, jornadas.obtenerAbierta()?.viajeId)
        }

    @Test
    fun `sin viaje en curso la jornada no tiene viaje`() =
        runTest {
            val jornadas = FakeJornadaRepository()
            val gestor = GestorAvisos(jornadas, FakeSettingsRepository(), FakeAlarmScheduler(), reloj)
            IniciarJornada(
                jornadas,
                FakeViajeRepository(listOf(terminado)),
                gestor,
                reloj,
            )(DatosInicio(hora(9, 6), madrid, 1_000))

            assertNull(jornadas.obtenerAbierta()?.viajeId)
        }

    @Test
    fun `corregir un viaje valida las fechas`() =
        runTest {
            val viajes = FakeViajeRepository(listOf(terminado))
            val resultado = GuardarViaje(viajes, reloj)(terminado.copy(fin = hora(1, 5)))

            assertEquals(ResultadoViaje.Invalido(ErrorViaje.FIN_NO_POSTERIOR_AL_INICIO), resultado)
            assertEquals(ResultadoViaje.Ok(1L), GuardarViaje(viajes, reloj)(terminado.copy(nombre = "Ida y vuelta")))
            assertEquals("Ida y vuelta", viajes.guardados.single().nombre)
        }

    @Test
    fun `borrar un viaje deja sus jornadas sin viaje, no las borra`() =
        runTest {
            val jornadas = FakeJornadaRepository(listOf(jornada(1, 2, 500, viajeId = 1)))
            val viajes = FakeViajeRepository(listOf(terminado), jornadas)

            BorrarViaje(viajes)(1)

            assertTrue(viajes.guardados.isEmpty())
            assertNull(jornadas.guardadas.single().viajeId)
        }
}
