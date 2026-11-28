package com.cristinachdr.mitacografo.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Cómo cobra el conductor los viajes (MT-58): por km, por días de dieta o las dos cosas. */
enum class ModoCobro { KM, DIETA, DIETA_Y_KM }

/**
 * Lo que cobra (MT-58). El periodo va del [diaInicio] de un mes al día anterior del mes siguiente.
 * Las cantidades son en euros, con los decimales que haga falta (p. ej. 0,045 € por km).
 */
data class ConfigSalario(
    val diaInicio: Int = DIA_INICIO_POR_DEFECTO,
    val modo: ModoCobro = ModoCobro.DIETA,
    val nomina: BigDecimal = BigDecimal.ZERO,
    val porKm: BigDecimal = BigDecimal.ZERO,
    val porDia: BigDecimal = BigDecimal.ZERO,
) {
    companion object {
        /** Como en su app: del 20 al 19. */
        const val DIA_INICIO_POR_DEFECTO = 20

        /** Hasta el 28, para que exista en todos los meses. */
        val DIAS_INICIO: IntRange = 1..28
    }
}

/** Un gasto de su bolsillo (parking, comida…), que se suma al salario del periodo en el que cae. */
data class Gasto(
    val id: Long = SIN_ID,
    val fecha: LocalDate,
    val descripcion: String? = null,
    val importe: BigDecimal,
) {
    companion object {
        const val SIN_ID = 0L
    }
}

/** Un periodo de salario, con las dos fechas incluidas. */
data class PeriodoSalario(
    val desde: LocalDate,
    val hasta: LocalDate,
) {
    operator fun contains(dia: LocalDate): Boolean = !dia.isBefore(desde) && !dia.isAfter(hasta)
}

/**
 * El periodo en el que cae [hoy], o el que está [desplazamiento] periodos antes (negativo) o después.
 *
 * Del día [diaInicio] al día anterior del mes siguiente: así ningún día cuenta en dos periodos
 * (en su app el día de corte entraba en los dos).
 */
fun periodoSalario(
    hoy: LocalDate,
    diaInicio: Int,
    desplazamiento: Int = 0,
): PeriodoSalario {
    val dia = diaInicio.coerceIn(ConfigSalario.DIAS_INICIO)
    val actual = if (hoy.dayOfMonth >= dia) hoy.withDayOfMonth(dia) else hoy.minusMonths(1).withDayOfMonth(dia)
    val desde = actual.plusMonths(desplazamiento.toLong())
    return PeriodoSalario(desde, desde.plusMonths(1).minusDays(1))
}

/** Un viaje que cuenta en el periodo: de qué día a qué día (las dos incluidas) y sus km. */
data class ViajeSalario(
    val resumen: ResumenViaje,
    val salida: LocalDate,
    val vuelta: LocalDate,
) {
    val dias: Int get() = ChronoUnit.DAYS.between(salida, vuelta).toInt() + 1
    val km: Int get() = resumen.km
}

/** El salario de un periodo, con el desglose (MT-58). Los importes, redondeados a céntimos. */
data class ResultadoSalario(
    val periodo: PeriodoSalario,
    val config: ConfigSalario,
    val viajes: List<ViajeSalario>,
    /** Del más reciente al más antiguo. */
    val gastos: List<Gasto>,
) {
    val km: Int get() = viajes.sumOf { it.km }
    val dias: Int get() = viajes.sumOf { it.dias }

    val importeDietas: BigDecimal
        get() = if (config.modo == ModoCobro.KM) cero() else euros(config.porDia * BigDecimal(dias))

    val importeKm: BigDecimal
        get() = if (config.modo == ModoCobro.DIETA) cero() else euros(config.porKm * BigDecimal(km))

    val totalGastos: BigDecimal get() = euros(gastos.fold(BigDecimal.ZERO) { suma, gasto -> suma + gasto.importe })

    val total: BigDecimal get() = euros(config.nomina + importeDietas + importeKm + totalGastos)
}

/**
 * Calcula el salario del [periodo], como en su app: cuentan los viajes **terminados** que salen y vuelven
 * dentro del periodo (uno que cruza el día de corte no cuenta en ninguno), y los gastos con fecha dentro.
 * El día de salida y de vuelta se miran en la zona donde empezó y terminó el viaje.
 */
fun calcularSalario(
    periodo: PeriodoSalario,
    config: ConfigSalario,
    viajes: List<ResumenViaje>,
    gastos: List<Gasto>,
): ResultadoSalario {
    val delPeriodo =
        viajes
            .mapNotNull { resumen ->
                val viaje = resumen.viaje
                val fin = viaje.fin ?: return@mapNotNull null
                val salida = viaje.inicio.atZone(viaje.zonaInicio).toLocalDate()
                val vuelta = fin.atZone(viaje.zonaFin ?: viaje.zonaInicio).toLocalDate()
                ViajeSalario(resumen, salida, vuelta).takeIf { salida in periodo && vuelta in periodo }
            }.sortedBy { it.resumen.viaje.inicio }
    return ResultadoSalario(
        periodo = periodo,
        config = config,
        viajes = delPeriodo,
        gastos = gastos.filter { it.fecha in periodo }.sortedByDescending { it.fecha },
    )
}

private fun cero(): BigDecimal = BigDecimal.ZERO.setScale(2)

private fun euros(valor: BigDecimal): BigDecimal = valor.setScale(2, RoundingMode.HALF_UP)
