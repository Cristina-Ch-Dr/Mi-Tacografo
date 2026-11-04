package com.cristinachdr.mitacografo.domain.model

import java.time.Duration
import java.time.Instant
import java.time.ZoneId

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
    /** Conducción que marca el tacógrafo al final. `null` = no se ha apuntado. */
    val conduccionTacografo: Duration? = null,
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
