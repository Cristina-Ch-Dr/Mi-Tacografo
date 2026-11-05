package com.cristinachdr.mitacografo.feature.history

import com.cristinachdr.mitacografo.MainDispatcherRule
import com.cristinachdr.mitacografo.domain.fake.FakeJornadaRepository
import com.cristinachdr.mitacografo.domain.fake.FakeSettingsRepository
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.usecase.ObtenerHistorial
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

    @Test
    fun `muestra las jornadas por semanas y se actualiza al borrar`() =
        runTest {
            val repo = FakeJornadaRepository(listOf(jornada(1, 2), jornada(2, 9)))
            val vm = HistorialViewModel(ObtenerHistorial(repo, FakeSettingsRepository()))
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
            val vm = HistorialViewModel(ObtenerHistorial(FakeJornadaRepository(), FakeSettingsRepository()))
            observar(vm)

            assertTrue(vm.uiState.value.vacio)
        }
}
