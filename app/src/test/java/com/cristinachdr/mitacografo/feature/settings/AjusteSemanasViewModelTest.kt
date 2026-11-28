package com.cristinachdr.mitacografo.feature.settings

import com.cristinachdr.mitacografo.MainDispatcherRule
import com.cristinachdr.mitacografo.domain.fake.FakeJornadaRepository
import com.cristinachdr.mitacografo.domain.fake.FakeSettingsRepository
import com.cristinachdr.mitacografo.domain.model.AjusteSemanas
import com.cristinachdr.mitacografo.domain.model.Ajustes
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.ui.ErrorCampo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Ajustes → «Ajuste de semanas» (MT-59). Datos inventados. */
class AjusteSemanasViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val madrid = ZoneId.of("Europe/Madrid")
    private val reloj = Clock.fixed(LocalDateTime.of(2026, 11, 20, 12, 0).atZone(madrid).toInstant(), madrid)

    /** Empezó a usar la app el jueves 5 de noviembre: la semana del lunes 2. */
    private val primera =
        Jornada(
            id = 1,
            inicio = LocalDateTime.of(2026, 11, 5, 6, 0).atZone(madrid).toInstant(),
            zonaInicio = madrid,
            kmInicio = 0,
        )

    @Test
    fun `propone la primera semana con jornadas y guarda lo que llevaba`() {
        val settings = FakeSettingsRepository()
        val vm = AjusteSemanasViewModel(settings, FakeJornadaRepository(listOf(primera)), reloj)
        assertEquals(LocalDate.of(2026, 11, 2), vm.uiState.value.semana)

        vm.onHorasSemana("27")
        vm.onHorasAnterior("50")
        vm.onMinutosAnterior("30")
        vm.onAmpliaciones(1)
        vm.onReducidos(2)
        vm.onGuardar()

        assertEquals(
            AjusteSemanas(
                semana = LocalDate.of(2026, 11, 2),
                conduccionSemana = Duration.ofHours(27),
                conduccionSemanaAnterior = Duration.ofMinutes(50 * 60 + 30),
                ampliaciones = 1,
                reducidos = 2,
            ),
            settings.actuales.ajusteSemanas,
        )

        vm.onQuitar()
        assertNull(settings.actuales.ajusteSemanas)
    }

    @Test
    fun `mas de 56 h en una semana no se puede guardar`() {
        val vm = AjusteSemanasViewModel(FakeSettingsRepository(), FakeJornadaRepository(listOf(primera)), reloj)

        vm.onHorasSemana("57")

        assertEquals(ErrorCampo.MAS_DE_56_HORAS, vm.uiState.value.errorSemana)
        assertFalse(vm.uiState.value.puedeGuardar)
    }

    @Test
    fun `avisa si el ajuste guardado es de otra semana`() {
        val antiguo = AjusteSemanas(semana = LocalDate.of(2026, 11, 9), conduccionSemana = Duration.ofHours(10))
        val vm =
            AjusteSemanasViewModel(
                FakeSettingsRepository(Ajustes(ajusteSemanas = antiguo)),
                FakeJornadaRepository(listOf(primera)),
                reloj,
            )

        assertEquals(LocalDate.of(2026, 11, 9), vm.uiState.value.deOtraSemana)
        assertEquals("10", vm.uiState.value.horasSemana)
    }
}
