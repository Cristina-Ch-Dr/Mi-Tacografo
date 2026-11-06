package com.cristinachdr.mitacografo.domain.model

import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** Qué apunta el conductor: una pausa dentro de la jornada o un descanso entre jornadas. */
enum class TipoDescanso {
    /** Dentro de la jornada (el café, la comida…). No cuenta como descanso diario. */
    PAUSA,

    /** Entre dos jornadas: diario, fraccionado o semanal (lo decide el clasificador). */
    DESCANSO,
}

/**
 * Una pausa o un descanso, con sus horas en [Instant] y la zona del lugar, como [Jornada].
 *
 * La clasificación (diario normal, reducido, fraccionado, semanal…) **no se guarda**:
 * la calcula [clasificarDescansos] a partir de las horas, así nunca se queda desfasada.
 */
data class Descanso(
    val id: Long = SIN_ID,
    val inicio: Instant,
    val zonaInicio: ZoneId,
    /** `null` mientras la pausa o el descanso sigue en curso. */
    val fin: Instant? = null,
    val zonaFin: ZoneId? = null,
    val tipo: TipoDescanso,
    /** País donde se descansa (ISO alfa-2). Lo necesita la regla de semanales reducidos fuera de casa. */
    val pais: String? = null,
    val observaciones: String? = null,
) {
    val enCurso: Boolean get() = fin == null

    /** Tiempo real entre inicio y fin, o `null` si sigue en curso. */
    val duracion: Duration? get() = fin?.let { duracionEntre(inicio, it) }

    companion object {
        const val SIN_ID = 0L
    }
}
