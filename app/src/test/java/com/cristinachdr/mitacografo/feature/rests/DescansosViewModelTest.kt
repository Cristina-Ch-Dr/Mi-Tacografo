package com.cristinachdr.mitacografo.feature.rests

import com.cristinachdr.mitacografo.MainDispatcherRule
import com.cristinachdr.mitacografo.domain.fake.FakeDescansoRepository
import com.cristinachdr.mitacografo.domain.fake.FakeJornadaRepository
import com.cristinachdr.mitacografo.domain.model.ClaseDescanso
import com.cristinachdr.mitacografo.domain.model.Descanso
import com.cristinachdr.mitacografo.domain.model.ErrorDescanso
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.TipoDescanso
import com.cristinachdr.mitacografo.domain.usecase.ApuntarPausaTresHoras
import com.cristinachdr.mitacografo.domain.usecase.GuardarDescanso
import com.cristinachdr.mitacografo.domain.usecase.ObtenerDescansos
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId

/** La pestaña Descanso: el diálogo «Pausa de 3 horas» (MT-54). Datos inventados. */
@OptIn(ExperimentalCoroutinesApi::class)
class DescansosViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val madrid = ZoneId.of("Europe/Madrid")
    private val ahora = LocalDateTime.of(2026, 11, 2, 15, 7)
    private val reloj = Clock.fixed(ahora.atZone(madrid).toInstant(), madrid)

    private val enCurso =
        Jornada(
            id = 1,
            inicio = LocalDateTime.of(2026, 11, 2, 6, 0).atZone(madrid).toInstant(),
            zonaInicio = madrid,
            kmInicio = 1_000,
            paisInicio = "ES",
        )

    private fun viewModel(descansos: FakeDescansoRepository): DescansosViewModel {
        val jornadas = FakeJornadaRepository(listOf(enCurso))
        return DescansosViewModel(
            ObtenerDescansos(descansos, jornadas),
            ApuntarPausaTresHoras(jornadas, GuardarDescanso(descansos, reloj)),
            reloj,
        )
    }

    private fun TestScope.observar(vm: DescansosViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
    }

    @Test
    fun `propone que la pausa termino ahora y apunta las 3 horas anteriores`() =
        runTest {
            val descansos = FakeDescansoRepository()
            val vm = viewModel(descansos)
            observar(vm)

            vm.onPedirPausaTresHoras()
            assertEquals(
                ahora.withSecond(0),
                vm.uiState.value.pausaTresHoras
                    ?.fin,
            )
            vm.onFinPausa(LocalDateTime.of(2026, 11, 2, 14, 0))
            assertEquals(
                LocalDateTime.of(2026, 11, 2, 11, 0),
                vm.uiState.value.pausaTresHoras
                    ?.inicio,
            )

            vm.onApuntarPausa()

            assertNull(vm.uiState.value.pausaTresHoras)
            val apuntado = descansos.guardados.single()
            assertEquals(LocalDateTime.of(2026, 11, 2, 11, 0), apuntado.inicio.atZone(madrid).toLocalDateTime())
            assertEquals(
                ClaseDescanso.FRACCIONADO_PRIMERA_PARTE,
                vm.uiState.value.descansos
                    .single()
                    .clase,
            )
        }

    @Test
    fun `si pisa otro descanso, lo dice y no cierra el dialogo`() =
        runTest {
            val pausa =
                Descanso(
                    id = 1,
                    inicio = LocalDateTime.of(2026, 11, 2, 12, 0).atZone(madrid).toInstant(),
                    zonaInicio = madrid,
                    fin = LocalDateTime.of(2026, 11, 2, 12, 30).atZone(madrid).toInstant(),
                    zonaFin = madrid,
                    tipo = TipoDescanso.PAUSA,
                )
            val vm = viewModel(FakeDescansoRepository(listOf(pausa)))
            observar(vm)

            vm.onPedirPausaTresHoras()
            vm.onApuntarPausa()

            assertEquals(
                setOf(ErrorDescanso.SOLAPA_CON_OTRO_DESCANSO),
                vm.uiState.value.pausaTresHoras
                    ?.errores,
            )
        }
}
