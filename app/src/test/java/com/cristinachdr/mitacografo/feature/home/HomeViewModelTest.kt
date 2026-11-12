package com.cristinachdr.mitacografo.feature.home

import com.cristinachdr.mitacografo.MainDispatcherRule
import com.cristinachdr.mitacografo.domain.fake.FakeDescansoRepository
import com.cristinachdr.mitacografo.domain.fake.FakeJornadaRepository
import com.cristinachdr.mitacografo.domain.fake.FakeSettingsRepository
import com.cristinachdr.mitacografo.domain.fake.FakeViajeRepository
import com.cristinachdr.mitacografo.domain.model.Ajustes
import com.cristinachdr.mitacografo.domain.model.Descanso
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.TipoDescanso
import com.cristinachdr.mitacografo.domain.usecase.GuardarDescanso
import com.cristinachdr.mitacografo.domain.usecase.IniciarPausa
import com.cristinachdr.mitacografo.domain.usecase.ObtenerInformeReglas
import com.cristinachdr.mitacografo.domain.usecase.ObtenerJornadaActual
import com.cristinachdr.mitacografo.domain.usecase.TerminarPausa
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
        ajustes: Ajustes = Ajustes(),
    ): HomeViewModel {
        val repo = FakeJornadaRepository(jornadas.toList())
        val settings = FakeSettingsRepository(ajustes)
        return HomeViewModel(
            ObtenerJornadaActual(repo),
            settings,
            descansos,
            FakeViajeRepository(),
            repo,
            IniciarPausa(descansos, repo, GuardarDescanso(descansos, reloj), reloj),
            TerminarPausa(descansos, reloj),
            ObtenerInformeReglas(repo, descansos, settings, reloj),
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

    @Test
    fun `avisa si faltan el pais de la empresa o el de residencia`() =
        runTest {
            val sinPaises = viewModel(Clock.fixed(hora(11, 0), madrid))
            observar(sinPaises)
            assertTrue(sinPaises.uiState.value.faltanPaises)

            val conPaises =
                viewModel(
                    Clock.fixed(hora(11, 0), madrid),
                    ajustes = Ajustes(paisEmpresa = "ES", paisResidencia = "UA"),
                )
            observar(conPaises)
            assertFalse(conPaises.uiState.value.faltanPaises)
        }

    @Test
    fun `esta semana muestra la conduccion y las ampliadas`() =
        runTest {
            val lunes =
                Jornada(
                    id = 1,
                    inicio = hora(6, 0),
                    zonaInicio = madrid,
                    kmInicio = 0,
                    fin = hora(17, 0),
                    zonaFin = madrid,
                    kmFin = 500,
                    conduccionTacografo = Duration.ofHours(9).plusMinutes(30),
                )
            val vm = viewModel(Clock.fixed(hora(20, 0), madrid), lunes)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.semana.collect {} }

            val semana = vm.semana.value!!
            // Es la primera semana con datos: lo de antes de las 06:00 del lunes no se sabe.
            assertEquals(Duration.ofHours(9).plusMinutes(30), semana.conduccion.min)
            assertEquals(1, semana.ampliadas)
        }

    @Test
    fun `sin jornada en curso cuenta el descanso desde la ultima`() =
        runTest {
            val ayer =
                Jornada(
                    id = 1,
                    inicio = hora(6, 0).minus(Duration.ofDays(1)),
                    zonaInicio = madrid,
                    kmInicio = 0,
                    fin = hora(18, 0).minus(Duration.ofDays(1)),
                    zonaFin = madrid,
                    kmFin = 500,
                )
            val vm = viewModel(Clock.fixed(hora(6, 0), madrid), ayer)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

            assertEquals(
                Duration.ofHours(12),
                vm.uiState.value.descanso
                    ?.transcurrido,
            )
        }
}
