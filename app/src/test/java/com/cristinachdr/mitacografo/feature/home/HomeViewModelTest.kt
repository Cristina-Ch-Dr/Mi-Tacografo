package com.cristinachdr.mitacografo.feature.home

import com.cristinachdr.mitacografo.MainDispatcherRule
import com.cristinachdr.mitacografo.domain.fake.FakeDescansoRepository
import com.cristinachdr.mitacografo.domain.fake.FakeJornadaRepository
import com.cristinachdr.mitacografo.domain.fake.FakeSettingsRepository
import com.cristinachdr.mitacografo.domain.model.Descanso
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.TipoDescanso
import com.cristinachdr.mitacografo.domain.usecase.GuardarDescanso
import com.cristinachdr.mitacografo.domain.usecase.IniciarPausa
import com.cristinachdr.mitacografo.domain.usecase.ObtenerJornadaActual
import com.cristinachdr.mitacografo.domain.usecase.TerminarPausa
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
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val madrid = ZoneId.of("Europe/Madrid")

    private fun hora(
        h: Int,
        m: Int = 0,
    ): Instant = LocalDateTime.of(2026, 11, 2, h, m).atZone(madrid).toInstant()

    private fun viewModel(
        reloj: Clock,
        vararg jornadas: Jornada,
        descansos: FakeDescansoRepository = FakeDescansoRepository(),
    ): HomeViewModel {
        val repo = FakeJornadaRepository(jornadas.toList())
        return HomeViewModel(
            ObtenerJornadaActual(repo),
            FakeSettingsRepository(),
            descansos,
            IniciarPausa(descansos, repo, GuardarDescanso(descansos, reloj), reloj),
            TerminarPausa(descansos, reloj),
            reloj,
        )
    }

    /** Se suscribe al estado (como haría la pantalla) para que el ViewModel empiece a emitir. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.observar(vm: HomeViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
    }

    @Test
    fun `la fecha de hoy sale del reloj inyectado`() {
        // 23:30 del 4/11 en UTC ya es 5/11 en Kyiv (UTC+2): la fecha depende de la zona.
        val reloj = Clock.fixed(Instant.parse("2026-11-04T23:30:00Z"), ZoneId.of("Europe/Kyiv"))

        assertEquals(LocalDate.of(2026, 11, 5), viewModel(reloj).uiState.value.hoy)
    }

    @Test
    fun `sin jornada abierta no hay jornada en curso`() =
        runTest {
            val vm = viewModel(Clock.fixed(hora(10), madrid))
            observar(vm)

            assertNull(vm.uiState.value.jornada)
        }

    @Test
    fun `con jornada abierta muestra el tiempo transcurrido y el proximo aviso`() =
        runTest {
            val abierta = Jornada(id = 1, inicio = hora(6, 0), zonaInicio = madrid, kmInicio = 245_310)
            val vm = viewModel(Clock.fixed(hora(11, 23), madrid), abierta)
            observar(vm)

            val enCurso = vm.uiState.value.jornada!!
            assertEquals(Duration.ofHours(5).plusMinutes(23), enCurso.transcurrido)
            assertEquals(245_310, enCurso.kmInicio)
            // Primer aviso: 12 h 30 después de las 06:00 → 18:30.
            assertEquals(hora(18, 30), enCurso.proximoAviso?.hora?.toInstant())
        }

    @Test
    fun `cuando ya han pasado todos los avisos no hay proximo`() =
        runTest {
            val abierta = Jornada(id = 1, inicio = hora(6, 0), zonaInicio = madrid, kmInicio = 0)
            val vm = viewModel(Clock.fixed(hora(21, 30), madrid), abierta)
            observar(vm)

            val enCurso = vm.uiState.value.jornada!!
            assertNull(enCurso.proximoAviso)
        }

    @Test
    fun `iniciar pausa la muestra en curso desde ahora`() =
        runTest {
            val abierta = Jornada(id = 1, inicio = hora(6, 0), zonaInicio = madrid, kmInicio = 0)
            val vm = viewModel(Clock.fixed(hora(11, 0), madrid), abierta)
            observar(vm)

            vm.onIniciarPausa()

            assertEquals(
                hora(11, 0),
                vm.uiState.value.pausa
                    ?.desde
                    ?.toInstant(),
            )
            assertEquals(
                Duration.ZERO,
                vm.uiState.value.pausa
                    ?.transcurrido,
            )
        }

    @Test
    fun `terminar pausa le pone la hora de fin y deja de mostrarla`() =
        runTest {
            val abierta = Jornada(id = 1, inicio = hora(6, 0), zonaInicio = madrid, kmInicio = 0)
            val pausa = Descanso(id = 1, inicio = hora(10, 30), zonaInicio = madrid, tipo = TipoDescanso.PAUSA)
            val descansos = FakeDescansoRepository(listOf(pausa))
            val vm = viewModel(Clock.fixed(hora(11, 0), madrid), abierta, descansos = descansos)
            observar(vm)
            assertEquals(
                Duration.ofMinutes(30),
                vm.uiState.value.pausa
                    ?.transcurrido,
            )

            vm.onTerminarPausa()

            assertNull(vm.uiState.value.pausa)
            assertEquals(hora(11, 0), descansos.guardados.single().fin)
        }
}
