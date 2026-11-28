package com.cristinachdr.mitacografo.feature.salary

import com.cristinachdr.mitacografo.MainDispatcherRule
import com.cristinachdr.mitacografo.domain.fake.FakeGastoRepository
import com.cristinachdr.mitacografo.domain.fake.FakeJornadaRepository
import com.cristinachdr.mitacografo.domain.fake.FakeSettingsRepository
import com.cristinachdr.mitacografo.domain.fake.FakeViajeRepository
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.ModoCobro
import com.cristinachdr.mitacografo.domain.model.Viaje
import com.cristinachdr.mitacografo.domain.usecase.AnadirGasto
import com.cristinachdr.mitacografo.domain.usecase.BorrarGasto
import com.cristinachdr.mitacografo.domain.usecase.CambiarConfigSalario
import com.cristinachdr.mitacografo.domain.usecase.ObtenerSalario
import com.cristinachdr.mitacografo.domain.usecase.ObtenerViajes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Pantalla Salario (MT-58). Datos inventados. */
class SalarioViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val madrid = ZoneId.of("Europe/Madrid")

    /** "Hoy": 28 de noviembre de 2026. Con el día 20, el periodo es del 20 nov al 19 dic. */
    private val reloj = Clock.fixed(LocalDateTime.of(2026, 11, 28, 12, 0).atZone(madrid).toInstant(), madrid)

    private fun instante(
        dia: Int,
        h: Int,
    ) = LocalDateTime.of(2026, 11, dia, h, 0).atZone(madrid).toInstant()

    private val viaje =
        Viaje(
            id = 1,
            nombre = "Ida",
            inicio = instante(21, 6),
            zonaInicio = madrid,
            fin = instante(24, 18),
            zonaFin = madrid,
        )

    private val jornada =
        Jornada(
            id = 1,
            inicio = instante(21, 6),
            zonaInicio = madrid,
            kmInicio = 1_000,
            fin = instante(21, 17),
            zonaFin = madrid,
            kmFin = 2_000,
            viajeId = 1,
        )

    private val gastos = FakeGastoRepository()

    private fun viewModel(): SalarioViewModel {
        val jornadas = FakeJornadaRepository(listOf(jornada))
        val settings = FakeSettingsRepository()
        return SalarioViewModel(
            ObtenerSalario(ObtenerViajes(FakeViajeRepository(listOf(viaje)), jornadas), gastos, settings, reloj),
            CambiarConfigSalario(settings),
            AnadirGasto(gastos),
            BorrarGasto(gastos),
            reloj,
        )
    }

    @Test
    fun `calcula el periodo de hoy con lo que cobra y los gastos`() {
        val vm = viewModel()
        assertEquals(
            LocalDate.of(2026, 11, 20),
            vm.uiState.value.resultado
                ?.periodo
                ?.desde,
        )
        assertFalse(vm.uiState.value.puedeAvanzar)

        vm.onModo(ModoCobro.DIETA_Y_KM)
        vm.onNomina("1200")
        vm.onPorDia("60")
        vm.onPorKm("0,045")
        // 4 días × 60 = 240; 1.000 km × 0,045 = 45; + 1.200 de nómina.
        assertEquals(
            BigDecimal("1485.00"),
            vm.uiState.value.resultado
                ?.total,
        )

        vm.onImporteGasto("12,50")
        vm.onDescripcionGasto("Parking")
        vm.onAnadirGasto()
        assertEquals(
            BigDecimal("1497.50"),
            vm.uiState.value.resultado
                ?.total,
        )
        assertEquals("", vm.uiState.value.gasto.importe)

        vm.onBorrarGasto(gastos.guardados.single().id)
        assertEquals(
            BigDecimal("1485.00"),
            vm.uiState.value.resultado
                ?.total,
        )
    }

    @Test
    fun `el periodo anterior no tiene el viaje, y se puede volver`() {
        val vm = viewModel()
        vm.onAnterior()
        assertEquals(
            LocalDate.of(2026, 10, 20),
            vm.uiState.value.resultado
                ?.periodo
                ?.desde,
        )
        assertTrue(
            vm.uiState.value.resultado!!
                .viajes
                .isEmpty(),
        )
        assertTrue(vm.uiState.value.puedeAvanzar)

        vm.onSiguiente()
        assertEquals(
            1,
            vm.uiState.value.resultado!!
                .viajes.size,
        )
    }

    @Test
    fun `los importes se leen con coma o punto`() {
        assertEquals(BigDecimal("0.045"), leerImporte("0,045"))
        assertEquals(BigDecimal("12.5"), leerImporte(" 12.5 "))
        assertNull(leerImporte("abc"))
        assertEquals("12,50", soloImporte("12.5,0"))
        assertEquals("0,045", textoImporte(BigDecimal("0.0450")))
        assertEquals("", textoImporte(BigDecimal.ZERO))
    }
}
