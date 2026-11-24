package com.cristinachdr.mitacografo.domain.model

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Las jornadas de una semana legal (de lunes a domingo), como en la libreta.
 *
 * @param lunes primer día de la semana.
 * @param jornadas de la más reciente a la más antigua.
 */
data class SemanaJornadas(
    val lunes: LocalDate,
    val jornadas: List<Jornada>,
) {
    val domingo: LocalDate get() = lunes.plusDays(6)

    /** Km de las jornadas ya terminadas. */
    val km: Int get() = jornadas.sumOf { it.kmRecorridos ?: 0 }

    /** Suma de la conducción apuntada del tacógrafo. */
    val conduccion: Duration get() = jornadas.mapNotNull { it.conduccionTacografo }.fold(Duration.ZERO, Duration::plus)

    /** `false` si alguna jornada terminada no tiene la conducción apuntada: la suma se queda corta. */
    val conduccionCompleta: Boolean get() = jornadas.filterNot { it.abierta }.all { it.conduccionTacografo != null }
}

/**
 * Agrupa las jornadas por la semana en la que **empiezan**, de la más reciente a la más antigua.
 *
 * El día se mira en [zonaReferencia] (la de Ajustes) o, si no hay, en la zona donde empezó cada
 * jornada: así una jornada que empieza el domingo a las 23:00 en España cuenta en esa semana.
 */
fun agruparPorSemana(
    jornadas: List<Jornada>,
    zonaReferencia: ZoneId? = null,
): List<SemanaJornadas> =
    jornadas
        .sortedByDescending { it.inicio }
        .groupBy { jornada ->
            jornada.inicio
                .atZone(zonaReferencia ?: jornada.zonaInicio)
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        }.map { (lunes, deLaSemana) -> SemanaJornadas(lunes, deLaSemana) }
        .sortedByDescending { it.lunes }

/** Jornadas seguidas del mismo viaje (o fuera de viaje, con [viajeId] `null`), para las cabeceras del Historial. */
data class TramoViaje(
    val viajeId: Long?,
    val jornadas: List<Jornada>,
)

/**
 * Las jornadas de la semana partidas en tramos de un mismo viaje (MT-56), en el mismo orden.
 * Si ninguna va en un viaje, no hay tramos: no hace falta ninguna cabecera.
 */
fun SemanaJornadas.porViaje(): List<TramoViaje> {
    if (jornadas.all { it.viajeId == null }) return emptyList()
    val tramos = mutableListOf<TramoViaje>()
    jornadas.forEach { jornada ->
        val ultimo = tramos.lastOrNull()
        if (ultimo != null && ultimo.viajeId == jornada.viajeId) {
            tramos[tramos.lastIndex] = ultimo.copy(jornadas = ultimo.jornadas + jornada)
        } else {
            tramos += TramoViaje(jornada.viajeId, listOf(jornada))
        }
    }
    return tramos
}
