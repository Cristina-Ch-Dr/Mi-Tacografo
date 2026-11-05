package com.cristinachdr.mitacografo.feature.settings

import com.cristinachdr.mitacografo.MainDispatcherRule
import com.cristinachdr.mitacografo.domain.fake.FakeAlarmScheduler
import com.cristinachdr.mitacografo.domain.fake.FakeJornadaRepository
import com.cristinachdr.mitacografo.domain.fake.FakeSettingsRepository
import com.cristinachdr.mitacografo.domain.model.Ajustes
import com.cristinachdr.mitacografo.domain.model.AvisoConfig
import com.cristinachdr.mitacografo.domain.model.Continente
import com.cristinachdr.mitacografo.domain.model.ErrorAviso
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.usecase.CambiarAjustes
import com.cristinachdr.mitacografo.domain.usecase.GestorAvisos
import com.cristinachdr.mitacografo.domain.usecase.ObtenerPaisesRecientes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    private val abierta =
        Jornada(id = 1, inicio = ahora.minusSeconds(3600), zonaInicio = madrid, kmInicio = 0, paisInicio = "UA")

    private var settings = FakeSettingsRepository(Ajustes(paisEmpresa = "ES"))
    private val alarmas = FakeAlarmScheduler()
    private var pruebas = mutableListOf<Duration>()

    private fun viewModel(): SettingsViewModel {
        val repo = FakeJornadaRepository(listOf(abierta))
        val gestor = GestorAvisos(repo, settings, alarmas, reloj)
        return SettingsViewModel(
            settings,
            CambiarAjustes(settings, gestor),
            { pruebas += it },
            ObtenerPaisesRecientes(repo),
        )
    }

    @Test
    fun `carga los ajustes guardados`() {
        val estado = viewModel().uiState.value

        assertEquals("ES", estado.paisEmpresa)
        assertEquals(4, estado.avisos.size)
        assertEquals(listOf("UA"), estado.paisesRecientes)
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
    fun `elegir continentes y paises habituales se guarda y cambia el selector`() {
        val vm = viewModel()

        vm.onContinente(Continente.EUROPA, elegido = true)
        vm.onContinente(Continente.ASIA, elegido = true)
        vm.onContinente(Continente.ASIA, elegido = false)
        vm.onAnadirPaisHabitual("ma")
        vm.onAnadirPaisHabitual("DE")
        vm.onQuitarPaisHabitual("DE")
        vm.onAnadirPaisHabitual("")

        assertEquals(setOf(Continente.EUROPA), settings.actuales.continentes)
        assertEquals(setOf("MA"), settings.actuales.paisesHabituales)
        val preferencias = vm.uiState.value.preferenciasPaises
        assertEquals(listOf("UA"), preferencias.recientes)
        assertEquals(setOf("MA"), preferencias.habituales)
        assertEquals(setOf(Continente.EUROPA), preferencias.continentes)
    }

    @Test
    fun `probar aviso programa uno dentro de un minuto`() {
        val vm = viewModel()

        vm.onProbarAviso()

        assertEquals(listOf(Duration.ofMinutes(1)), pruebas)
        assertTrue(vm.uiState.value.pruebaProgramada)
    }

    @Test
    fun `cambiar la hora de un aviso lo guarda ordenado y reprograma la jornada`() {
        val vm = viewModel()

        vm.onEditarAviso(0)
        assertEquals(EditorAviso(indice = 0, horas = "12", minutos = "30"), vm.uiState.value.editorAviso)
        vm.onEditorMinutos("0")
        vm.onEditorHoras("16")
        vm.onGuardarAviso()

        val esperado = listOf(Duration.ofHours(13), Duration.ofMinutes(870), Duration.ofHours(15), Duration.ofHours(16))
        assertEquals(esperado, settings.actuales.avisos.map { it.desdeInicio })
        assertEquals(esperado, alarmas.programados.map { it.desdeInicio })
        assertNull(vm.uiState.value.editorAviso)
    }

    @Test
    fun `una hora repetida o fuera de rango no se guarda`() {
        val vm = viewModel()

        vm.onNuevoAviso()
        vm.onEditorHoras("13")
        vm.onGuardarAviso()
        assertEquals(
            ErrorAviso.REPETIDO,
            vm.uiState.value.editorAviso
                ?.error,
        )

        vm.onEditorHoras("25")
        assertNull(
            vm.uiState.value.editorAviso
                ?.error,
        ) // al escribir se quita el error
        vm.onGuardarAviso()
        assertEquals(
            ErrorAviso.FUERA_DE_RANGO,
            vm.uiState.value.editorAviso
                ?.error,
        )

        vm.onEditorHoras("")
        vm.onGuardarAviso()
        assertEquals(
            ErrorAviso.FUERA_DE_RANGO,
            vm.uiState.value.editorAviso
                ?.error,
        )

        assertEquals(AvisoConfig.POR_DEFECTO, settings.actuales.avisos)
    }

    @Test
    fun `anadir, borrar y restaurar los de por defecto`() {
        val vm = viewModel()

        vm.onNuevoAviso()
        vm.onEditorHoras("11")
        vm.onGuardarAviso()
        assertEquals(
            Duration.ofHours(11),
            settings.actuales.avisos
                .first()
                .desdeInicio,
        )
        assertFalse(vm.uiState.value.avisosPorDefecto)

        vm.onEditarAviso(1) // el de 12 h 30
        vm.onBorrarAviso()
        assertEquals(
            listOf(Duration.ofHours(11), Duration.ofHours(13), Duration.ofMinutes(870), Duration.ofHours(15)),
            settings.actuales.avisos.map { it.desdeInicio },
        )

        vm.onRestaurarAvisos()
        assertEquals(AvisoConfig.POR_DEFECTO, settings.actuales.avisos)
        assertTrue(vm.uiState.value.avisosPorDefecto)
    }

    @Test
    fun `con 8 avisos no deja anadir mas`() {
        settings = FakeSettingsRepository(Ajustes(avisos = (1L..8L).map { AvisoConfig(Duration.ofHours(it)) }))
        val vm = viewModel()

        vm.onNuevoAviso()

        assertFalse(vm.uiState.value.caben)
        assertNull(vm.uiState.value.editorAviso)
    }

    @Test
    fun `cancelar no cambia nada`() {
        val vm = viewModel()

        vm.onEditarAviso(0)
        vm.onEditorHoras("20")
        vm.onCancelarAviso()

        assertNull(vm.uiState.value.editorAviso)
        assertEquals(AvisoConfig.POR_DEFECTO, settings.actuales.avisos)
    }
}
