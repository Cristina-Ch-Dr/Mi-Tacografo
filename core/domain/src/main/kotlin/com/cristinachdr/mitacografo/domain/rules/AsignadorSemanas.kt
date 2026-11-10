package com.cristinachdr.mitacografo.domain.rules

import com.cristinachdr.mitacografo.domain.model.Jornada
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * La conducción de una jornada que le toca a una semana, con lo que la hace incierta.
 *
 * @param motivo por qué no es exacta (`null` si lo es).
 */
internal data class Parte(
    val jornada: Jornada,
    val conduccion: Intervalo,
    val motivo: MotivoNoVerificado?,
)

/**
 * Reparte la conducción de cada jornada entre las semanas legales (lunes 00:00 – domingo 24:00, art. 4.i)
 * en la zona de referencia (D-1). Si no hay, en la zona donde empezó cada jornada.
 *
 * - Jornada dentro de una semana: toda su conducción va a esa semana.
 * - Jornada que cruza el domingo a las 24:00 (T20): no se sabe cuánto se condujo a cada lado. A cada semana
 *   le toca como poco lo que no cabe en la otra y como mucho lo que cabe en ella.
 * - Sin la conducción del tacógrafo: entre 0 y lo que duró la jornada (no se puede conducir más).
 * - En curso: entre 0 y lo que lleva hasta [ahora].
 *
 * @return por cada lunes, las partes de las jornadas que le tocan.
 */
internal fun repartirPorSemanas(
    jornadas: List<Jornada>,
    zonaReferencia: ZoneId?,
    ahora: Instant,
): Map<LocalDate, List<Parte>> {
    val porSemana = mutableMapOf<LocalDate, MutableList<Parte>>()
    jornadas.forEach { jornada ->
        val zona = zonaReferencia ?: jornada.zonaInicio
        val fin = jornada.fin ?: maxOf(ahora, jornada.inicio)
        val total = conduccionPosible(jornada, ahora)
        val motivoTotal = if (total.exacto) null else MotivoNoVerificado.SIN_CONDUCCION_TACOGRAFO

        // Trozos de la jornada en cada semana, cortando en cada lunes a las 00:00.
        val trozos = mutableListOf<Pair<LocalDate, Duration>>()
        var desde = jornada.inicio
        while (desde < fin) {
            val lunes = lunesDe(desde, zona)
            val siguienteLunes = lunes.plusWeeks(1).atStartOfDay(zona).toInstant()
            val hasta = minOf(fin, siguienteLunes)
            trozos += lunes to Duration.between(desde, hasta)
            desde = hasta
        }
        if (trozos.isEmpty()) trozos += lunesDe(jornada.inicio, zona) to Duration.ZERO

        val duracion = trozos.fold(Duration.ZERO) { acc, (_, d) -> acc + d }
        trozos.forEach { (lunes, enEstaSemana) ->
            val fuera = duracion - enEstaSemana
            val parte =
                if (trozos.size == 1) {
                    Parte(jornada, total, motivoTotal)
                } else {
                    // Como poco: lo que no cupo fuera de esta semana. Como mucho: lo que cabe en ella.
                    val min = (total.min - fuera).coerceAtLeast(Duration.ZERO)
                    val max = minOf(total.max, enEstaSemana)
                    val intervalo = Intervalo(minOf(min, max), max)
                    Parte(
                        jornada,
                        intervalo,
                        motivoTotal ?: MotivoNoVerificado.JORNADA_CRUZA_SEMANA.takeUnless { intervalo.exacto },
                    )
                }
            porSemana.getOrPut(lunes) { mutableListOf() } += parte
        }
    }
    return porSemana
}

/** La conducción de toda la jornada: la del tacógrafo o, si no está, entre 0 y lo que duró. */
internal fun conduccionPosible(
    jornada: Jornada,
    ahora: Instant,
): Intervalo {
    jornada.conduccionTacografo?.takeIf { !jornada.abierta }?.let { return Intervalo.exacto(it) }
    val fin = jornada.fin ?: maxOf(ahora, jornada.inicio)
    return Intervalo(Duration.ZERO, Duration.between(jornada.inicio, fin))
}

/** El lunes de la semana legal en la que cae [instante], mirado en [zona]. */
internal fun lunesDe(
    instante: Instant,
    zona: ZoneId,
): LocalDate = instante.atZone(zona).toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
