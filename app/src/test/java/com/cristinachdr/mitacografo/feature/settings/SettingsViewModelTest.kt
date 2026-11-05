package com.cristinachdr.mitacografo.feature.settings

import com.cristinachdr.mitacografo.MainDispatcherRule
import com.cristinachdr.mitacografo.domain.fake.FakeAlarmScheduler
import com.cristinachdr.mitacografo.domain.fake.FakeJornadaRepository
import com.cristinachdr.mitacografo.domain.fake.FakeSettingsRepository
import com.cristinachdr.mitacografo.domain.model.Ajustes
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.usecase.CambiarAjustes
import com.cristinachdr.mitacografo.domain.usecase.GestorAvisos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val madrid = ZoneId.of("Europe/Madrid")
    private val ahora = LocalDateTime.of(2026, 11, 2, 10, 0).atZone(madrid).toInstant()
    private val reloj = Clock.fixed(ahora, madrid)

    private val abierta = Jornada(id = 1, inicio = ahora.minusSeconds(3600), zonaInicio = madrid, kmInicio = 0)

    private val settings = FakeSettingsRepository(Ajustes(paisEmpresa = "ES"))
    private val alarmas = FakeAlarmScheduler()
    private var pruebas = mutableListOf<Duration>()

    private fun viewModel(): SettingsViewModel {
        val gestor = GestorAvisos(FakeJornadaRepository(listOf(abierta)), settings, alarmas, reloj)
        return SettingsViewModel(settings, CambiarAjustes(settings, gestor)) { pruebas += it }
    }

    @Test
    fun `carga los ajustes guardados`() {
        val estado = viewModel().uiState.value

        assertEquals("ES", estado.paisEmpresa)
        assertEquals(4, estado.avisos.size)
    }

    @Test
    fun `los paises se guardan en mayusculas y un texto vacio se borra`() {
        val vm = viewModel()

        vm.onPaisResidencia("ua")
        vm.onPaisEmpresa("")

        assertEquals("UA", settings.actuales.paisResidencia)
        assertNull(settings.actuales.paisEmpresa?.ifBlank { null })
    }

    @Test
    fun `desactivar un aviso lo quita de la jornada en curso`() {
        val vm = viewModel()

        vm.onAviso(indice = 0, activo = false)

        assertEquals(false, settings.actuales.avisos[0].activo)
        // La jornada empezó a las 09:00: quedan 13 h, 14 h 30 y 15 h (sin el de 12 h 30).
        assertEquals(
            listOf(Duration.ofHours(13), Duration.ofMinutes(870), Duration.ofHours(15)),
            alarmas.programados.map { it.desdeInicio },
        )
    }

    @Test
    fun `probar aviso programa uno dentro de un minuto`() {
        val vm = viewModel()

        vm.onProbarAviso()

        assertEquals(listOf(Duration.ofMinutes(1)), pruebas)
        assertTrue(vm.uiState.value.pruebaProgramada)
    }
}
