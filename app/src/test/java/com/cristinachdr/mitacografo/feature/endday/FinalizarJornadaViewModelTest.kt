package com.cristinachdr.mitacografo.feature.endday

import com.cristinachdr.mitacografo.MainDispatcherRule
import com.cristinachdr.mitacografo.domain.fake.FakeAlarmScheduler
import com.cristinachdr.mitacografo.domain.fake.FakeDescansoRepository
import com.cristinachdr.mitacografo.domain.fake.FakeJornadaRepository
import com.cristinachdr.mitacografo.domain.fake.FakeSettingsRepository
import com.cristinachdr.mitacografo.domain.model.Descanso
import com.cristinachdr.mitacografo.domain.model.ErrorJornada
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.TipoDescanso
import com.cristinachdr.mitacografo.domain.usecase.ApuntarConduccion
import com.cristinachdr.mitacografo.domain.usecase.FinalizarJornada
import com.cristinachdr.mitacografo.domain.usecase.GestorAvisos
import com.cristinachdr.mitacografo.domain.usecase.ObtenerJornadaActual
import com.cristinachdr.mitacografo.domain.usecase.ObtenerPaisesRecientes
import com.cristinachdr.mitacografo.domain.usecase.ObtenerPreferenciasPaises
import com.cristinachdr.mitacografo.domain.usecase.TerminarPausa
import com.cristinachdr.mitacografo.ui.ErrorCampo
import kotlinx.coroutines.test.runTest
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

class FinalizarJornadaViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val madrid = ZoneId.of("Europe/Madrid")
    private val inicio = LocalDateTime.of(2026, 11, 2, 6, 0)
    private val ahora = inicio.plusHours(11)
    private val reloj = Clock.fixed(ahora.atZone(madrid).toInstant(), madrid)

    private val abierta =
        Jornada(
            id = 1,
            inicio = inicio.atZone(madrid).toInstant(),
            zonaInicio = madrid,
            kmInicio = 245_310,
            paisInicio = "ES",
        )

    private fun viewModel(
        repo: FakeJornadaRepository,
        descansos: FakeDescansoRepository = FakeDescansoRepository(),
    ) = FinalizarJornadaViewModel(
        FinalizarJornada(
            repo,
            GestorAvisos(repo, FakeSettingsRepository(), FakeAlarmScheduler(), reloj),
            FakeSettingsRepository(),
            reloj,
        ),
        ObtenerJornadaActual(repo),
        ObtenerPreferenciasPaises(ObtenerPaisesRecientes(repo), FakeSettingsRepository()),
        TerminarPausa(descansos, reloj),
        ApuntarConduccion(repo, reloj),
        FakeSettingsRepository(),
        reloj,
    )

    @Test
    fun `carga la jornada abierta y propone la hora actual y el mismo pais`() {
        val estado = viewModel(FakeJornadaRepository(listOf(abierta))).uiState.value

        assertEquals(abierta, estado.jornada)
        assertEquals(ahora, estado.fechaHora)
        assertEquals("ES", estado.pais)
        assertEquals(Duration.ofHours(11), estado.duracion)
    }

    @Test
    fun `los km recorridos se calculan mientras se escribe`() {
        val vm = viewModel(FakeJornadaRepository(listOf(abierta)))

        vm.onKm("245872")

        assertEquals(562, vm.uiState.value.kmRecorridos)
        assertTrue(vm.uiState.value.puedeGuardar)
    }

    @Test
    fun `finaliza la jornada con la conduccion del tacografo`() =
        runTest {
            val repo = FakeJornadaRepository(listOf(abierta))
            val vm = viewModel(repo)
            vm.onKm("245872")
            vm.onConduccionHoras("8")
            vm.onConduccionMinutos("45")

            vm.onFinalizar()

            assertTrue(vm.uiState.value.guardado)
            val finalizada = repo.obtenerPorId(1)!!
            assertNull(repo.obtenerAbierta())
            assertEquals(Duration.ofHours(8).plusMinutes(45), finalizada.conduccionTacografo)
            assertEquals(562, finalizada.kmRecorridos)
        }

    @Test
    fun `sin conduccion apuntada se guarda como no apuntada`() =
        runTest {
            val repo = FakeJornadaRepository(listOf(abierta))
            val vm = viewModel(repo)
            vm.onKm("245872")

            vm.onFinalizar()

            assertNull(repo.obtenerPorId(1)!!.conduccionTacografo)
        }

    @Test
    fun `avisa si los km finales son menores o la conduccion supera la jornada`() {
        val vm = viewModel(FakeJornadaRepository(listOf(abierta)))

        vm.onKm("245000")
        vm.onConduccionHoras("12")

        assertEquals(
            setOf(ErrorJornada.KM_FIN_MENOR_QUE_INICIO, ErrorJornada.CONDUCCION_MAYOR_QUE_JORNADA),
            vm.uiState.value.errores,
        )
        assertFalse(vm.uiState.value.puedeGuardar)
    }

    @Test
    fun `los minutos tienen que estar entre 0 y 59`() {
        val vm = viewModel(FakeJornadaRepository(listOf(abierta)))
        vm.onKm("245872")

        vm.onConduccionMinutos("75")

        assertEquals(ErrorCampo.MINUTOS_INVALIDOS, vm.uiState.value.errorMinutos)
        assertFalse(vm.uiState.value.puedeGuardar)
    }

    @Test
    fun `sin jornada abierta se indica y no se puede guardar`() {
        val estado = viewModel(FakeJornadaRepository()).uiState.value

        assertTrue(estado.sinJornada)
        assertFalse(estado.puedeGuardar)
    }

    @Test
    fun `una pausa que se quedo abierta termina con la jornada`() =
        runTest {
            val descansos =
                FakeDescansoRepository(
                    listOf(
                        Descanso(
                            id = 1,
                            inicio = ahora.minusMinutes(30).atZone(madrid).toInstant(),
                            zonaInicio = madrid,
                            tipo = TipoDescanso.PAUSA,
                        ),
                    ),
                )
            val vm = viewModel(FakeJornadaRepository(listOf(abierta)), descansos)
            vm.onKm("245872")

            vm.onFinalizar()

            assertEquals(ahora.atZone(madrid).toInstant(), descansos.guardados.single().fin)
        }

    @Test
    fun `apunta la conduccion de ahora sin finalizar la jornada`() =
        runTest {
            val repo = FakeJornadaRepository(listOf(abierta))
            val vm = viewModel(repo)
            vm.onConduccionHoras("6")
            vm.onConduccionMinutos("10")

            vm.onApuntarConduccion()

            val enCurso = repo.obtenerAbierta()!!
            assertEquals(Duration.ofHours(6).plusMinutes(10), enCurso.conduccionTacografo)
            assertEquals(reloj.instant(), enCurso.conduccionApuntadaEn)
            assertEquals(
                ahora,
                vm.uiState.value.conduccionApuntadaEn
                    ?.toLocalDateTime(),
            )
            assertFalse(vm.uiState.value.guardado)
        }

    @Test
    fun `la conduccion apuntada durante la jornada sale ya escrita al finalizar`() {
        val apuntada =
            abierta.copy(
                conduccionTacografo = Duration.ofHours(6).plusMinutes(10),
                conduccionApuntadaEn = reloj.instant().minusSeconds(3_600),
            )
        val estado = viewModel(FakeJornadaRepository(listOf(apuntada))).uiState.value

        assertEquals("6", estado.conduccionHoras)
        assertEquals("10", estado.conduccionMinutos)
    }

    @Test
    fun `sin cruzar el domingo no pide la conduccion antes de medianoche`() {
        assertNull(viewModel(FakeJornadaRepository(listOf(abierta))).uiState.value.medianoche)
    }

    @Test
    fun `si la jornada cruza el domingo pide la conduccion antes de medianoche y la guarda`() =
        runTest {
            // Empezó el domingo 1 a las 20:00 y termina el lunes 2 a las 17:00 (la hora del reloj).
            val domingo = abierta.copy(inicio = LocalDateTime.of(2026, 11, 1, 20, 0).atZone(madrid).toInstant())
            val repo = FakeJornadaRepository(listOf(domingo))
            val vm = viewModel(repo)

            assertEquals(
                LocalDateTime.of(2026, 11, 2, 0, 0),
                vm.uiState.value.medianoche
                    ?.toLocalDateTime(),
            )

            vm.onKm("245872")
            vm.onConduccionHoras("9")
            vm.onAntesHoras("5")
            // Solo hubo 4 h antes de medianoche.
            assertEquals(setOf(ErrorJornada.ANTES_MEDIANOCHE_NO_CUADRA), vm.uiState.value.errores)

            vm.onAntesHoras("3")
            vm.onAntesMinutos("52")
            vm.onFinalizar()

            assertEquals(Duration.ofHours(3).plusMinutes(52), repo.obtenerPorId(1)!!.conduccionAntesMedianoche)
        }
}
