package com.cristinachdr.mitacografo.feature.home

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HomeViewModelTest {
    @Test
    fun `la fecha de hoy sale del reloj inyectado`() {
        // 23:30 del 4/11 en UTC ya es 5/11 en Kyiv (UTC+2): la fecha depende de la zona.
        val reloj = Clock.fixed(Instant.parse("2026-11-04T23:30:00Z"), ZoneId.of("Europe/Kyiv"))

        val viewModel = HomeViewModel(reloj)

        assertEquals(LocalDate.of(2026, 11, 5), viewModel.uiState.hoy)
    }
}
