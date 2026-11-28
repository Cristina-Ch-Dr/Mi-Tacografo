package com.cristinachdr.mitacografo.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** El salario (MT-58). Datos inventados. */
class SalarioTest {
    private val madrid = ZoneId.of("Europe/Madrid")

    private fun hora(
        mes: Int,
        dia: Int,
        h: Int = 6,
    ): Instant = LocalDateTime.of(2026, mes, dia, h, 0).atZone(madrid).toInstant()

    private fun viaje(
        id: Long,
        desde: Instant,
        hasta: Instant?,
        km: Int,
    ): ResumenViaje {
        val viaje =
            Viaje(
                id = id,
                nombre = "V$id",
                inicio = desde,
                zonaInicio = madrid,
                fin = hasta,
                zonaFin = hasta?.let { madrid },
            )
        val jornada =
            Jornada(
                id = id,
                inicio = desde,
                zonaInicio = madrid,
                kmInicio = 0,
                fin = desde.plusSeconds(3600),
                zonaFin = madrid,
                kmFin = km,
                viajeId = id,
            )
        return ResumenViaje(viaje, listOf(jornada))
    }

    @Test
    fun `el periodo va del dia de inicio al anterior del mes siguiente`() {
        // El 28 de noviembre, con el 20: del 20 nov al 19 dic. El 5 de diciembre también.
        assertEquals(
            PeriodoSalario(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 12, 19)),
            periodoSalario(LocalDate.of(2026, 11, 28), 20),
        )
        assertEquals(
            PeriodoSalario(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 12, 19)),
            periodoSalario(LocalDate.of(2026, 12, 5), 20),
        )
        // Con la flecha hacia atrás, el anterior.
        assertEquals(
            PeriodoSalario(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 11, 19)),
            periodoSalario(LocalDate.of(2026, 11, 28), 20, -1),
        )
        // El día 1: el mes natural.
        assertEquals(
            PeriodoSalario(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)),
            periodoSalario(LocalDate.of(2026, 11, 28), 1),
        )
    }

    @Test
    fun `cuentan los viajes terminados que salen y vuelven dentro, con sus dias y km`() {
        val periodo = PeriodoSalario(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 12, 19))
        val viajes =
            listOf(
                // Del 21 al 25 nov: 5 días, 2.000 km.
                viaje(1, hora(11, 21), hora(11, 25, 18), 2_000),
                // Cruza el día de corte (del 18 al 22 nov): no cuenta en ninguno.
                viaje(2, hora(11, 18), hora(11, 22), 900),
                // Aún en curso: no cuenta.
                viaje(3, hora(11, 27), null, 500),
            )
        val config =
            ConfigSalario(
                modo = ModoCobro.DIETA_Y_KM,
                nomina = BigDecimal("1200"),
                porKm = BigDecimal("0.045"),
                porDia = BigDecimal("60"),
            )
        val gastos =
            listOf(
                Gasto(1, LocalDate.of(2026, 11, 22), "Parking", BigDecimal("12.50")),
                // Del periodo anterior: no cuenta.
                Gasto(2, LocalDate.of(2026, 11, 19), "Comida", BigDecimal("9.00")),
            )

        val salario = calcularSalario(periodo, config, viajes, gastos)

        assertEquals(listOf(1L), salario.viajes.map { it.resumen.viaje.id })
        assertEquals(5, salario.dias)
        assertEquals(2_000, salario.km)
        assertEquals(BigDecimal("300.00"), salario.importeDietas) // 5 × 60
        assertEquals(BigDecimal("90.00"), salario.importeKm) // 2.000 × 0,045
        assertEquals(BigDecimal("12.50"), salario.totalGastos)
        assertEquals(BigDecimal("1602.50"), salario.total)
    }

    @Test
    fun `segun el modo, solo dietas o solo km`() {
        val periodo = PeriodoSalario(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 12, 19))
        val viajes = listOf(viaje(1, hora(11, 21), hora(11, 22), 1_000))
        val tarifas = ConfigSalario(porKm = BigDecimal("0.10"), porDia = BigDecimal("50"))

        val soloDieta = calcularSalario(periodo, tarifas.copy(modo = ModoCobro.DIETA), viajes, emptyList())
        assertEquals(BigDecimal("100.00"), soloDieta.total)
        assertEquals(BigDecimal("0.00"), soloDieta.importeKm)

        val soloKm = calcularSalario(periodo, tarifas.copy(modo = ModoCobro.KM), viajes, emptyList())
        assertEquals(BigDecimal("100.00"), soloKm.total)
        assertEquals(BigDecimal("0.00"), soloKm.importeDietas)
    }
}
