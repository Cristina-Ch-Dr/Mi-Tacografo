package com.cristinachdr.mitacografo.domain.model

import java.time.Duration
import java.time.Instant

/**
 * Duración real entre dos instantes.
 *
 * Se calcula con [Instant] (UTC), así que los cambios de hora y las zonas horarias
 * no la alteran: una jornada de 8 h reales siempre mide 8 h.
 */
fun duracionEntre(inicio: Instant, fin: Instant): Duration {
    require(!fin.isBefore(inicio)) { "El fin ($fin) no puede ser anterior al inicio ($inicio)" }
    return Duration.between(inicio, fin)
}
