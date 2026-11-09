package com.cristinachdr.mitacografo.domain.model

import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * Un viaje: de varios días, con un nombre que pone el conductor («Murcia – Kyiv»).
 *
 * Mientras hay uno en curso, cada jornada que empieza se apunta en él sola.
 * Los km no se guardan: son la suma de los de sus jornadas ([ResumenViaje]).
 */
data class Viaje(
    val id: Long = SIN_ID,
    val nombre: String,
    val inicio: Instant,
    val zonaInicio: ZoneId,
    /** `null` mientras el viaje sigue en curso. */
    val fin: Instant? = null,
    val zonaFin: ZoneId? = null,
    val observaciones: String? = null,
) {
    val enCurso: Boolean get() = fin == null

    companion object {
        const val SIN_ID = 0L
    }
}

/** Un viaje con sus jornadas (en orden cronológico) y lo que suman. */
data class ResumenViaje(
    val viaje: Viaje,
    val jornadas: List<Jornada>,
) {
    /** Km de las jornadas terminadas del viaje. */
    val km: Int get() = jornadas.sumOf { it.kmRecorridos ?: 0 }

    val conduccion: Duration get() = jornadas.mapNotNull { it.conduccionTacografo }.fold(Duration.ZERO, Duration::plus)

    val numJornadas: Int get() = jornadas.size

    /** Países por los que ha pasado, en el orden en que aparecen. */
    val paises: List<String> get() = jornadas.flatMap { listOfNotNull(it.paisInicio, it.paisFin) }.distinct()
}

/** Junta cada viaje con sus jornadas. @return del más reciente al más antiguo. */
fun resumirViajes(
    viajes: List<Viaje>,
    jornadas: List<Jornada>,
): List<ResumenViaje> {
    val porViaje = jornadas.filter { it.viajeId != null }.groupBy { it.viajeId }
    return viajes
        .sortedByDescending { it.inicio }
        .map { viaje -> ResumenViaje(viaje, porViaje[viaje.id].orEmpty().sortedBy { it.inicio }) }
}

/** Motivos por los que no se puede guardar un viaje. */
enum class ErrorViaje {
    VIAJE_NO_EXISTE,
    YA_HAY_UNO_EN_CURSO,
    SIN_VIAJE_EN_CURSO,
    SIN_NOMBRE,
    INICIO_EN_EL_FUTURO,
    FIN_NO_POSTERIOR_AL_INICIO,
    FIN_EN_EL_FUTURO,
}

/** Reglas de un viaje frente a los demás: con nombre, fechas con sentido y solo uno en curso. */
fun validarViaje(
    viaje: Viaje,
    otros: List<Viaje>,
    ahora: Instant,
): Set<ErrorViaje> =
    buildSet {
        val limiteFuturo = ahora.plus(MARGEN_HORA_FUTURA)
        val fin = viaje.fin
        if (viaje.nombre.isBlank()) add(ErrorViaje.SIN_NOMBRE)
        if (viaje.inicio.isAfter(limiteFuturo)) add(ErrorViaje.INICIO_EN_EL_FUTURO)
        if (fin != null) {
            if (!fin.isAfter(viaje.inicio)) add(ErrorViaje.FIN_NO_POSTERIOR_AL_INICIO)
            if (fin.isAfter(limiteFuturo)) add(ErrorViaje.FIN_EN_EL_FUTURO)
        }
        if (viaje.enCurso && otros.any { it.enCurso }) add(ErrorViaje.YA_HAY_UNO_EN_CURSO)
    }

sealed interface ResultadoViaje<out T> {
    data class Ok<T>(
        val valor: T,
    ) : ResultadoViaje<T>

    data class Invalido(
        val errores: Set<ErrorViaje>,
    ) : ResultadoViaje<Nothing> {
        constructor(error: ErrorViaje) : this(setOf(error))
    }
}
