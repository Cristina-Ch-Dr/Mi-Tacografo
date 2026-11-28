package com.cristinachdr.mitacografo.domain.usecase

import com.cristinachdr.mitacografo.domain.fake.FakeDespertador
import com.cristinachdr.mitacografo.domain.fake.FakeSettingsRepository
import com.cristinachdr.mitacografo.domain.model.Ajustes
import com.cristinachdr.mitacografo.domain.model.SalidasManuales
import com.cristinachdr.mitacografo.domain.model.cuandoSalir
import com.cristinachdr.mitacografo.domain.model.salidaALas
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** «Cuándo salir» y el despertador (MT-57). Datos inventados. */
class DespertadorTest {
    private val madrid = ZoneId.of("Europe/Madrid")

    private fun hora(
        dia: Int,
        h: Int,
        m: Int = 0,
    ): Instant = LocalDateTime.of(2026, 11, dia, h, m).atZone(madrid).toInstant()

    /** "Ahora": lunes 2/11/2026 a las 20:00. */
    private val reloj = Clock.fixed(hora(2, 20), madrid)

    @Test
    fun `con 9 h y 11 h de descanso, la salida y el despertar con la antelacion`() {
        // Terminó a las 19:40: con 9 h sale a las 04:40 y con 11 h a las 06:40; despierta 45 min antes.
        val salidas = cuandoSalir(hora(2, 19, 40), Duration.ofMinutes(45))

        assertEquals(listOf(Duration.ofHours(9), Duration.ofHours(11)), salidas.map { it.descanso })
        assertEquals(listOf(hora(3, 4, 40), hora(3, 6, 40)), salidas.map { it.salida })
        assertEquals(listOf(hora(3, 3, 55), hora(3, 5, 55)), salidas.map { it.despertar })
    }

    @Test
    fun `poner el despertador lo guarda y lo programa, y quitarlo lo borra`() =
        runTest {
            val settings = FakeSettingsRepository()
            val despertador = FakeDespertador()

            assertTrue(PonerDespertador(settings, despertador, reloj)(hora(3, 4)))
            assertEquals(hora(3, 4), despertador.puesto)
            assertEquals(hora(3, 4), settings.actuales.despertador)

            QuitarDespertador(settings, despertador)()
            assertNull(despertador.puesto)
            assertNull(settings.actuales.despertador)
        }

    @Test
    fun `no se pone para una hora que ya ha pasado`() =
        runTest {
            val despertador = FakeDespertador()

            assertFalse(PonerDespertador(FakeSettingsRepository(), despertador, reloj)(hora(2, 19)))
            assertNull(despertador.puesto)
        }

    @Test
    fun `al reiniciar el movil se vuelve a poner, o se olvida si ya paso`() =
        runTest {
            val pendiente = FakeSettingsRepository(Ajustes(despertador = hora(3, 4)))
            val despertador = FakeDespertador()
            ReprogramarDespertador(pendiente, despertador, reloj)()
            assertEquals(hora(3, 4), despertador.puesto)

            val pasado = FakeSettingsRepository(Ajustes(despertador = hora(2, 6)))
            ReprogramarDespertador(pasado, FakeDespertador(), reloj)()
            assertNull(pasado.actuales.despertador)
        }

    @Test
    fun `cambiar la salida a mano la usa, avisa si no llega a 9 h y mueve el despertador`() =
        runTest {
            // Terminó a las 19:40: con 9 h sale a las 04:40. Tenía el despertador a las 03:40 (60 min antes).
            val fin = hora(2, 19, 40)
            val settings = FakeSettingsRepository(Ajustes(despertador = hora(3, 3, 40)))
            val despertador = FakeDespertador()

            // Prefiere salir a las 04:00: el descanso se queda en 8 h 20.
            CambiarSalida(settings, despertador, reloj)(fin, Duration.ofHours(9), hora(3, 4))

            val salidas = cuandoSalir(fin, settings.actuales.antelacionDespertador, settings.actuales.salidasManuales)
            assertEquals(hora(3, 4), salidas.first().salida)
            assertTrue(salidas.first().manual)
            assertTrue(salidas.first().descansoCorto)
            assertEquals(hora(3, 3), settings.actuales.despertador)
            assertEquals(hora(3, 3), despertador.puesto)

            // ↺: vuelve a la calculada, y el despertador también.
            CambiarSalida(settings, despertador, reloj)(fin, Duration.ofHours(9), null)
            assertNull(settings.actuales.salidasManuales)
            assertEquals(hora(3, 3, 40), settings.actuales.despertador)
        }

    @Test
    fun `una salida a mano solo vale para su jornada, y la hora elegida es la proxima despues del fin`() {
        val fin = hora(2, 19, 40)
        val manuales = SalidasManuales(fin, mapOf(Duration.ofHours(11) to hora(3, 7)))
        assertTrue(cuandoSalir(fin, Duration.ofMinutes(60), manuales).last().manual)
        // Otra jornada terminada más tarde: se vuelve a lo calculado.
        assertFalse(cuandoSalir(hora(3, 18), Duration.ofMinutes(60), manuales).last().manual)

        assertEquals(hora(3, 5), salidaALas(fin, LocalTime.of(5, 0), madrid))
        assertEquals(hora(2, 21), salidaALas(fin, LocalTime.of(21, 0), madrid))
    }
}
