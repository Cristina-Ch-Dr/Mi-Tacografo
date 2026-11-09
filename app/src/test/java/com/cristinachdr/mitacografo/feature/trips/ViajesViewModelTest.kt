package com.cristinachdr.mitacografo.feature.trips

import androidx.lifecycle.SavedStateHandle
import com.cristinachdr.mitacografo.MainDispatcherRule
import com.cristinachdr.mitacografo.domain.fake.FakeJornadaRepository
import com.cristinachdr.mitacografo.domain.fake.FakeViajeRepository
import com.cristinachdr.mitacografo.domain.model.ErrorViaje
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.Viaje
import com.cristinachdr.mitacografo.domain.usecase.BorrarViaje
import com.cristinachdr.mitacografo.domain.usecase.FinalizarViaje
import com.cristinachdr.mitacografo.domain.usecase.GuardarViaje
import com.cristinachdr.mitacografo.domain.usecase.IniciarViaje
import com.cristinachdr.mitacografo.domain.usecase.ObtenerViajes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class ViajesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val madrid = ZoneId.of("Europe/Madrid")
    private val ahora = LocalDateTime.of(2026, 11, 9, 20, 0)
    private val reloj = Clock.fixed(ahora.atZone(madrid).toInstant(), madrid)

    private fun instante(
        dia: Int,
        h: Int,
    ) = LocalDateTime.of(2026, 11, dia, h, 0).atZone(madrid).toInstant()

    private val ida =
        Viaje(
            id = 1,
            nombre = "Ida",
            inicio = instante(2, 5),
            zonaInicio = madrid,
            fin = instante(5, 18),
            zonaFin = madrid,
        )

    private val jornada =
        Jornada(
            id = 1,
            inicio = instante(3, 6),
            zonaInicio = madrid,
            kmInicio = 1_000,
            fin = instante(3, 17),
            zonaFin = madrid,
            kmFin = 1_600,
            viajeId = 1,
        )

    private fun lista(
        viajes: FakeViajeRepository,
        jornadas: FakeJornadaRepository = FakeJornadaRepository(listOf(jornada)),
    ) = ViajesViewModel(
        ObtenerViajes(viajes, jornadas),
        IniciarViaje(viajes, jornadas, reloj),
        FinalizarViaje(viajes, reloj),
        reloj,
    )

    @Test
    fun `separa el viaje en curso de los anteriores, con sus km`() {
        val enCurso = Viaje(id = 2, nombre = "Vuelta", inicio = instante(8, 5), zonaInicio = madrid)
        val estado = lista(FakeViajeRepository(listOf(ida, enCurso))).uiState.value

        assertEquals(2L, estado.enCurso?.viaje?.id)
        assertEquals(listOf(1L), estado.anteriores.map { it.viaje.id })
        assertEquals(600, estado.anteriores.single().km)
    }

    @Test
    fun `iniciar un viaje propone un nombre con la fecha de hoy`() {
        val viajes = FakeViajeRepository()
        val vm = lista(viajes)

        vm.onPedirIniciar { hoy -> "Viaje del $hoy" }
        assertEquals("Viaje del ${LocalDate.of(2026, 11, 9)}", vm.uiState.value.nombreNuevo)

        vm.onNombreNuevo("Murcia – Kyiv")
        vm.onIniciar()
        assertNull(vm.uiState.value.nombreNuevo)
        assertEquals(
            "Murcia – Kyiv",
            vm.uiState.value.enCurso
                ?.viaje
                ?.nombre,
        )
    }

    @Test
    fun `sin nombre no se inicia y se dice por que`() {
        val vm = lista(FakeViajeRepository())
        vm.onPedirIniciar { "" }
        vm.onIniciar()

        assertEquals(setOf(ErrorViaje.SIN_NOMBRE), vm.uiState.value.errores)
        assertNull(vm.uiState.value.enCurso)
    }

    @Test
    fun `finalizar pide confirmacion y termina el viaje`() {
        val viajes =
            FakeViajeRepository(listOf(Viaje(id = 2, nombre = "Vuelta", inicio = instante(8, 5), zonaInicio = madrid)))
        val vm = lista(viajes)
        vm.onPedirFinalizar()
        assertTrue(vm.uiState.value.confirmandoFin)

        vm.onFinalizar()
        assertFalse(vm.uiState.value.confirmandoFin)
        assertNull(vm.uiState.value.enCurso)
        assertEquals(reloj.instant(), viajes.guardados.single().fin)
    }

    private fun detalle(
        viajes: FakeViajeRepository,
        jornadas: FakeJornadaRepository,
    ) = EditarViajeViewModel(
        SavedStateHandle(mapOf("id" to 1L)),
        ObtenerViajes(viajes, jornadas),
        viajes,
        GuardarViaje(viajes, reloj),
        BorrarViaje(viajes),
        reloj,
    )

    @Test
    fun `el detalle muestra sus jornadas y guarda el nombre corregido`() {
        val jornadas = FakeJornadaRepository(listOf(jornada))
        val viajes = FakeViajeRepository(listOf(ida), jornadas)
        val vm = detalle(viajes, jornadas)
        assertEquals(
            listOf(1L),
            vm.uiState.value.resumen
                ?.jornadas
                ?.map { it.id },
        )
        assertEquals("Ida", vm.uiState.value.nombre)

        vm.onNombre("Ida a Kyiv")
        vm.onGuardar()

        assertTrue(vm.uiState.value.cerrar)
        assertEquals("Ida a Kyiv", viajes.guardados.single().nombre)
    }

    @Test
    fun `borrar el viaje deja la jornada sin viaje`() {
        val jornadas = FakeJornadaRepository(listOf(jornada))
        val viajes = FakeViajeRepository(listOf(ida), jornadas)
        val vm = detalle(viajes, jornadas)

        vm.onPedirBorrar()
        vm.onConfirmarBorrar()

        assertTrue(viajes.guardados.isEmpty())
        assertNull(jornadas.guardadas.single().viajeId)
        assertTrue(vm.uiState.value.cerrar)
    }
}
