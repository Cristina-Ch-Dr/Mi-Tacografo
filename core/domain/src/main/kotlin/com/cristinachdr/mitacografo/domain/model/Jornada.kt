package com.cristinachdr.mitacografo.domain.model

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Una jornada de trabajo del conductor: desde que empieza hasta que termina.
 *
 * Las horas se guardan como [Instant] (UTC) junto a la [ZoneId] del lugar donde
 * ocurrieron. Así las duraciones son siempre reales (cambio de hora, otros países)
 * y cada hora se puede mostrar tal y como la vio el conductor.
 *
 * Las reglas de negocio (una sola jornada abierta, `fin > inicio`, `kmFin ≥ kmInicio`…)
 * las comprueban los casos de uso, no esta clase.
 */
data class Jornada(
    val id: Long = SIN_ID,
    val inicio: Instant,
    val zonaInicio: ZoneId,
    val kmInicio: Int,
    /** País en formato ISO 3166-1 alfa-2, por ejemplo "ES" o "UA". */
    val paisInicio: String? = null,
    val ciudadInicio: String? = null,
    /** `null` mientras la jornada sigue abierta. */
    val fin: Instant? = null,
    val zonaFin: ZoneId? = null,
    val kmFin: Int? = null,
    val paisFin: String? = null,
    val ciudadFin: String? = null,
    /**
     * Conducción que marca el tacógrafo al final. `null` = no se ha apuntado.
     *
     * Con la jornada en curso puede ser la de «hasta ahora» (MT-53): la apuntada a las [conduccionApuntadaEn].
     */
    val conduccionTacografo: Duration? = null,
    /** Solo con la jornada en curso: cuándo se apuntó [conduccionTacografo]. Al finalizarla se borra. */
    val conduccionApuntadaEn: Instant? = null,
    /**
     * Solo si la jornada cruza el domingo a las 24:00 (MT-50, T20): cuánta de [conduccionTacografo]
     * fue antes. Con ella, la conducción se reparte exacta entre las dos semanas. `null` = no se sabe.
     */
    val conduccionAntesMedianoche: Duration? = null,
    val viajeId: Long? = null,
    val observaciones: String? = null,
) {
    val abierta: Boolean get() = fin == null

    /** Kilómetros hechos en la jornada, o `null` si aún no hay km final. */
    val kmRecorridos: Int? get() = kmFin?.let { it - kmInicio }

    /** Tiempo real entre inicio y fin, o `null` si la jornada sigue abierta. */
    val duracion: Duration? get() = fin?.let { duracionEntre(inicio, it) }

    companion object {
        /** Id de una jornada que todavía no se ha guardado. */
        const val SIN_ID = 0L
    }
}

/**
 * Quita lo que ya no tiene sentido antes de guardar: la hora de la conducción «hasta ahora» si la jornada
 * está terminada, y la conducción antes de medianoche si no cruza el domingo a las 24:00.
 */
fun Jornada.sinConduccionSobrante(zonaReferencia: ZoneId?): Jornada =
    copy(
        conduccionApuntadaEn = conduccionApuntadaEn.takeIf { abierta },
        conduccionAntesMedianoche = conduccionAntesMedianoche.takeIf { cruceDeSemana(zonaReferencia) != null },
    )

/**
 * El lunes a las 00:00 (fin de la semana legal, art. 4.i) que cruza la jornada, mirado en [zonaReferencia]
 * o, si no hay, en la zona donde empezó (como el motor de reglas). `null` si no cruza ninguno o sigue abierta.
 */
fun Jornada.cruceDeSemana(zonaReferencia: ZoneId?): Instant? {
    val fin = fin ?: return null
    val zona = zonaReferencia ?: zonaInicio
    val lunes =
        inicio
            .atZone(zona)
            .toLocalDate()
            .with(TemporalAdjusters.next(DayOfWeek.MONDAY))
            .atStartOfDay(zona)
            .toInstant()
    return lunes.takeIf { it < fin }
}
