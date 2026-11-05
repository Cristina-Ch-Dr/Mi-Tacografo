package com.cristinachdr.mitacografo.feature.editday

import androidx.lifecycle.SavedStateHandle
import com.cristinachdr.mitacografo.MainDispatcherRule
import com.cristinachdr.mitacografo.domain.fake.FakeAlarmScheduler
import com.cristinachdr.mitacografo.domain.fake.FakeJornadaRepository
import com.cristinachdr.mitacografo.domain.fake.FakeSettingsRepository
import com.cristinachdr.mitacografo.domain.model.ErrorJornada
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.usecase.BorrarJornada
import com.cristinachdr.mitacografo.domain.usecase.EditarJornada
import com.cristinachdr.mitacografo.domain.usecase.GestorAvisos
import com.cristinachdr.mitacografo.domain.usecase.ObtenerPaisesRecientes
import com.cristinachdr.mitacografo.domain.usecase.ObtenerPreferenciasPaises
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

class EditarJornadaViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val madrid = ZoneId.of("Europe/Madrid")
    private val inicio = LocalDateTime.of(2026, 11, 2, 6, 0)
    private val reloj = Clock.fixed(LocalDateTime.of(2026, 11, 3, 20, 0).atZone(madrid).toInstant(), madrid)

    private val lunes =
        Jornada(
            id = 1,
            inicio = inicio.atZone(madrid).toInstant(),
            zonaInicio = madrid,
            kmInicio = 245_310,
            paisInicio = "ES",
            fin = inicio.plusHours(11).atZone(madrid).toInstant(),
            zonaFin = madrid,
            kmFin = 245_872,
            paisFin = "FR",
            conduccionTacografo = Duration.ofMinutes(525),
        )

    private val martes =
        Jornada(
            id = 2,
            inicio = inicio.plusDays(1).atZone(madrid).toInstant(),
            zonaInicio = madrid,
            kmInicio = 245_872,
        )

    private fun viewModel(
        repo: FakeJornadaRepository,
        id: Long,
    ): EditarJornadaViewModel {
        val gestor = GestorAvisos(repo, FakeSettingsRepository(), FakeAlarmScheduler(), reloj)
        return EditarJornadaViewModel(
            SavedStateHandle(mapOf("id" to id)),
            repo,
            EditarJornada(repo, gestor, reloj),
            BorrarJornada(repo, gestor),
            ObtenerPreferenciasPaises(ObtenerPaisesRecientes(repo), FakeSettingsRepository()),
            reloj,
        )
    }

    @Test
    fun `carga los datos de la jornada en el formulario`() {
        val estado = viewModel(FakeJornadaRepository(listOf(lunes)), 1).uiState.value

        assertEquals(inicio, estado.inicio)
        assertEquals("245310", estado.kmInicio)
        assertEquals(inicio.plusHours(11), estado.fin)
        assertEquals("245872", estado.kmFin)
        assertEquals("FR", estado.paisFin)
        assertEquals("8" to "45", estado.conduccionHoras to estado.conduccionMinutos)
        assertEquals(562, estado.kmRecorridos)
        assertTrue(estado.terminada)
        assertTrue(estado.puedeGuardar)
    }

    @Test
    fun `corregir los km y la conduccion se guarda`() =
        runTest {
            val repo = FakeJornadaRepository(listOf(lunes))
            val vm = viewModel(repo, 1)

            vm.onKmFin("245900")
            vm.onConduccionHoras("9")
            vm.onConduccionMinutos("")
            assertEquals(590, vm.uiState.value.kmRecorridos)
            vm.onGuardar()

            assertTrue(vm.uiState.value.terminado)
            val guardada = repo.obtenerPorId(1)!!
            assertEquals(245_900, guardada.kmFin)
            assertEquals(Duration.ofHours(9), guardada.conduccionTacografo)
        }

    @Test
    fun `borrar las casillas de conduccion la deja sin apuntar`() =
        runTest {
            val repo = FakeJornadaRepository(listOf(lunes))
            val vm = viewModel(repo, 1)

            vm.onConduccionHoras("")
            vm.onConduccionMinutos("")
            vm.onGuardar()

            assertNull(repo.obtenerPorId(1)!!.conduccionTacografo)
        }

    @Test
    fun `los errores se ven mientras se escribe`() {
        val vm = viewModel(FakeJornadaRepository(listOf(lunes)), 1)

        vm.onKmFin("245000")
        assertEquals(setOf(ErrorJornada.KM_FIN_MENOR_QUE_INICIO), vm.uiState.value.errores)

        vm.onKmFin("")
        assertEquals(ErrorCampo.OBLIGATORIO, vm.uiState.value.errorKmFin)
        assertFalse(vm.uiState.value.puedeGuardar)
    }

    @Test
    fun `si se solapa con otra jornada no se guarda y se dice por que`() =
        runTest {
            val repo = FakeJornadaRepository(listOf(lunes, martes))
            val vm = viewModel(repo, 1)

            vm.onFin(inicio.plusDays(1).plusHours(1)) // termina a las 07:00 del martes, y el martes empezó a las 06:00
            vm.onGuardar()

            assertEquals(setOf(ErrorJornada.SOLAPA_CON_OTRA_JORNADA), vm.uiState.value.errores)
            assertFalse(vm.uiState.value.terminado)
            assertEquals(lunes, repo.obtenerPorId(1))
        }

    @Test
    fun `una jornada en curso solo deja cambiar el inicio`() =
        runTest {
            val repo = FakeJornadaRepository(listOf(lunes, martes))
            val vm = viewModel(repo, 2)

            assertFalse(vm.uiState.value.terminada)
            vm.onKmInicio("245880")
            vm.onGuardar()

            assertEquals(245_880, repo.obtenerPorId(2)!!.kmInicio)
            assertNull(repo.obtenerPorId(2)!!.fin)
        }

    @Test
    fun `borrar pide confirmacion y despues la borra`() =
        runTest {
            val repo = FakeJornadaRepository(listOf(lunes, martes))
            val vm = viewModel(repo, 1)

            vm.onPedirBorrar()
            assertTrue(vm.uiState.value.confirmandoBorrado)
            vm.onCancelarBorrar()
            assertEquals(2, repo.guardadas.size)

            vm.onPedirBorrar()
            vm.onConfirmarBorrar()

            assertTrue(vm.uiState.value.terminado)
            assertEquals(listOf(martes), repo.guardadas)
        }

    @Test
    fun `una jornada que ya no existe lo dice`() {
        val estado = viewModel(FakeJornadaRepository(), 9).uiState.value

        assertTrue(estado.noExiste)
    }
}
