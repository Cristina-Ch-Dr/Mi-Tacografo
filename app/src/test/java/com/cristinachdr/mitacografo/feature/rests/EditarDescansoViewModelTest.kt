package com.cristinachdr.mitacografo.feature.rests

import androidx.lifecycle.SavedStateHandle
import com.cristinachdr.mitacografo.MainDispatcherRule
import com.cristinachdr.mitacografo.domain.fake.FakeDescansoRepository
import com.cristinachdr.mitacografo.domain.fake.FakeJornadaRepository
import com.cristinachdr.mitacografo.domain.fake.FakeSettingsRepository
import com.cristinachdr.mitacografo.domain.model.Descanso
import com.cristinachdr.mitacografo.domain.model.ErrorDescanso
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.TipoDescanso
import com.cristinachdr.mitacografo.domain.usecase.BorrarDescanso
import com.cristinachdr.mitacografo.domain.usecase.GuardarDescanso
import com.cristinachdr.mitacografo.domain.usecase.ObtenerPaisesRecientes
import com.cristinachdr.mitacografo.domain.usecase.ObtenerPreferenciasPaises
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

class EditarDescansoViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val madrid = ZoneId.of("Europe/Madrid")
    private val ahora = LocalDateTime.of(2026, 11, 3, 6, 0)
    private val reloj = Clock.fixed(ahora.atZone(madrid).toInstant(), madrid)

    private val ayer =
        Jornada(
            id = 1,
            inicio = ahora.minusHours(24).atZone(madrid).toInstant(),
            zonaInicio = madrid,
            kmInicio = 100_000,
            fin = ahora.minusHours(11).atZone(madrid).toInstant(),
            zonaFin = madrid,
            kmFin = 100_480,
            paisFin = "FR",
        )

    private fun viewModel(
        descansos: FakeDescansoRepository,
        id: Long = 0,
    ): EditarDescansoViewModel {
        val jornadas = FakeJornadaRepository(listOf(ayer))
        return EditarDescansoViewModel(
            SavedStateHandle(mapOf("id" to id)),
            descansos,
            jornadas,
            GuardarDescanso(descansos, reloj),
            BorrarDescanso(descansos),
            ObtenerPreferenciasPaises(ObtenerPaisesRecientes(jornadas), FakeSettingsRepository()),
            reloj,
        )
    }

    @Test
    fun `uno nuevo se propone desde el fin de la ultima jornada hasta ahora, en su pais`() {
        val estado = viewModel(FakeDescansoRepository()).uiState.value

        assertEquals(ahora.minusHours(11), estado.inicio)
        assertEquals(ahora, estado.fin)
        assertEquals("FR", estado.pais)
        assertEquals(Duration.ofHours(11), estado.duracion)
        assertTrue(estado.puedeGuardar)
    }

    @Test
    fun `guardar uno nuevo lo crea y cierra la pantalla`() {
        val descansos = FakeDescansoRepository()
        val vm = viewModel(descansos)

        vm.onGuardar()

        assertTrue(vm.uiState.value.cerrar)
        assertEquals(TipoDescanso.DESCANSO, descansos.guardados.single().tipo)
        assertEquals(ayer.fin, descansos.guardados.single().inicio)
    }

    @Test
    fun `avisa del solape mientras se escribe y no deja guardar`() {
        val existente =
            Descanso(
                id = 5,
                inicio = ahora.minusHours(12).atZone(madrid).toInstant(),
                zonaInicio = madrid,
                fin = ahora.minusHours(10).atZone(madrid).toInstant(),
                zonaFin = madrid,
                tipo = TipoDescanso.DESCANSO,
            )
        val estado = viewModel(FakeDescansoRepository(listOf(existente))).uiState.value

        assertEquals(setOf(ErrorDescanso.SOLAPA_CON_OTRO_DESCANSO), estado.errores)
        assertFalse(estado.puedeGuardar)
    }

    @Test
    fun `corregir uno existente conserva su id y se puede borrar`() {
        val existente =
            Descanso(
                id = 5,
                inicio = ahora.minusHours(3).atZone(madrid).toInstant(),
                zonaInicio = madrid,
                fin = ahora.minusHours(2).atZone(madrid).toInstant(),
                zonaFin = madrid,
                tipo = TipoDescanso.PAUSA,
            )
        val descansos = FakeDescansoRepository(listOf(existente))
        val vm = viewModel(descansos, id = 5)
        assertEquals(TipoDescanso.PAUSA, vm.uiState.value.tipo)

        vm.onTipo(TipoDescanso.DESCANSO)
        vm.onGuardar()
        assertEquals(existente.copy(tipo = TipoDescanso.DESCANSO), descansos.guardados.single())

        val otraVez = viewModel(descansos, id = 5)
        otraVez.onPedirBorrar()
        otraVez.onConfirmarBorrar()
        assertTrue(descansos.guardados.isEmpty())
    }
}
