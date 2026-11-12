package com.cristinachdr.mitacografo.feature.history

import com.cristinachdr.mitacografo.MainDispatcherRule
import com.cristinachdr.mitacografo.domain.fake.FakeDescansoRepository
import com.cristinachdr.mitacografo.domain.fake.FakeJornadaRepository
import com.cristinachdr.mitacografo.domain.fake.FakeSettingsRepository
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.usecase.ObtenerDescansosPrevios
import com.cristinachdr.mitacografo.domain.usecase.ObtenerHistorial
import com.cristinachdr.mitacografo.domain.usecase.ObtenerInformeReglas
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class HistorialViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val madrid = ZoneId.of("Europe/Madrid")

    private fun jornada(
        id: Long,
        dia: Int,
    ): Jornada {
        val inicio = LocalDateTime.of(2026, 11, dia, 6, 0).atZone(madrid).toInstant()
        return Jornada(id, inicio, madrid, 1_000, fin = inicio.plusSeconds(36_000), zonaFin = madrid, kmFin = 1_500)
    }

    private fun TestScope.observar(vm: HistorialViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
    }

    private fun historial(repo: FakeJornadaRepository): HistorialViewModel {
        val settings = FakeSettingsRepository()
        val reloj = java.time.Clock.systemDefaultZone()
        return HistorialViewModel(
            ObtenerHistorial(repo, settings),
            ObtenerInformeReglas(repo, FakeDescansoRepository(), settings, reloj),
            ObtenerDescansosPrevios(repo, FakeDescansoRepository()),
        )
    }

    @Test
    fun `muestra las jornadas por semanas y se actualiza al borrar`() =
        runTest {
            val repo = FakeJornadaRepository(listOf(jornada(1, 2), jornada(2, 9)))
            val vm = historial(repo)
            observar(vm)

            assertEquals(
                listOf(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 2)),
                vm.uiState.value.semanas
                    .map { it.lunes },
            )

            repo.borrar(2)

            assertEquals(
                listOf(1L),
                vm.uiState.value.semanas
                    .flatMap { it.jornadas }
                    .map { it.id },
            )
        }

    @Test
    fun `sin jornadas se muestra el mensaje de lista vacia`() =
        runTest {
            val vm = historial(FakeJornadaRepository())
            observar(vm)

            assertTrue(vm.uiState.value.vacio)
        }

    @Test
    fun `las flechas van de semana en semana saltando las vacias`() =
        runTest {
            val vm =
                historial(
                    FakeJornadaRepository(listOf(jornada(1, dia = 2), jornada(2, dia = 23), jornada(3, dia = 25))),
                )
            observar(vm)

            // Empieza en la más reciente (la del 23) y no hay ninguna más nueva.
            assertEquals(
                LocalDate.of(2026, 11, 23),
                vm.uiState.value.semana
                    ?.lunes,
            )
            assertFalse(vm.uiState.value.haySiguiente)

            // La del 16 y la del 9 están vacías: salta a la del 2.
            vm.onAnterior()
            assertEquals(
                LocalDate.of(2026, 11, 2),
                vm.uiState.value.semana
                    ?.lunes,
            )
            assertFalse(vm.uiState.value.hayAnterior)

            vm.onSiguiente()
            assertEquals(
                LocalDate.of(2026, 11, 23),
                vm.uiState.value.semana
                    ?.lunes,
            )
        }
}
