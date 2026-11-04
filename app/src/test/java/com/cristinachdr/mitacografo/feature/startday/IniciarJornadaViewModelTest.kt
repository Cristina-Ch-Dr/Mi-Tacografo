package com.cristinachdr.mitacografo.feature.startday

import com.cristinachdr.mitacografo.MainDispatcherRule
import com.cristinachdr.mitacografo.domain.fake.FakeJornadaRepository
import com.cristinachdr.mitacografo.domain.model.ErrorJornada
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.usecase.IniciarJornada
import com.cristinachdr.mitacografo.domain.usecase.ProponerDatosInicio
import com.cristinachdr.mitacografo.ui.ErrorCampo
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId

class IniciarJornadaViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val madrid = ZoneId.of("Europe/Madrid")
    private val ahora = LocalDateTime.of(2026, 11, 2, 6, 0)
    private val reloj = Clock.fixed(ahora.atZone(madrid).toInstant(), madrid)

    private val ayer =
        Jornada(
            id = 1,
            inicio = ahora.minusDays(1).atZone(madrid).toInstant(),
            zonaInicio = madrid,
            kmInicio = 100_000,
            fin = ahora.minusHours(13).atZone(madrid).toInstant(),
            zonaFin = madrid,
            kmFin = 100_480,
            paisFin = "FR",
            ciudadFin = "Lyon",
        )

    private fun viewModel(repo: FakeJornadaRepository) =
        IniciarJornadaViewModel(IniciarJornada(repo, reloj), ProponerDatosInicio(repo), reloj)

    @Test
    fun `se rellena con la hora actual y donde termino la jornada anterior`() {
        val estado = viewModel(FakeJornadaRepository(listOf(ayer))).uiState.value

        assertEquals(ahora, estado.fechaHora)
        assertEquals("100480", estado.km)
        assertEquals("FR", estado.pais)
        assertEquals("Lyon", estado.ciudad)
        assertTrue(estado.puedeGuardar)
    }

    @Test
    fun `con los valores propuestos se inicia en un toque`() =
        runTest {
            val repo = FakeJornadaRepository(listOf(ayer))
            val vm = viewModel(repo)

            vm.onIniciar()

            assertTrue(vm.uiState.value.guardado)
            val abierta = repo.obtenerAbierta()!!
            assertEquals(100_480, abierta.kmInicio)
            assertEquals("FR", abierta.paisInicio)
        }

    @Test
    fun `sin km no se puede iniciar`() {
        val vm = viewModel(FakeJornadaRepository())

        assertEquals(ErrorCampo.OBLIGATORIO, vm.uiState.value.errorKm)
        assertFalse(vm.uiState.value.puedeGuardar)

        vm.onKm("245310")

        assertNull(vm.uiState.value.errorKm)
        assertTrue(vm.uiState.value.puedeGuardar)
    }

    @Test
    fun `una hora futura se avisa mientras se escribe`() {
        val vm = viewModel(FakeJornadaRepository(listOf(ayer)))

        vm.onFechaHora(ahora.plusHours(1))

        assertEquals(setOf(ErrorJornada.INICIO_EN_EL_FUTURO), vm.uiState.value.errores)
        assertFalse(vm.uiState.value.puedeGuardar)
    }

    @Test
    fun `si el dominio rechaza la jornada se muestran sus errores`() =
        runTest {
            // Empieza antes de que terminara la de ayer: solo lo sabe el caso de uso.
            val vm = viewModel(FakeJornadaRepository(listOf(ayer)))
            vm.onFechaHora(ahora.minusHours(14))

            vm.onIniciar()

            assertFalse(vm.uiState.value.guardado)
            assertEquals(setOf(ErrorJornada.INICIO_ANTES_DEL_FIN_ANTERIOR), vm.uiState.value.errores)
        }
}
