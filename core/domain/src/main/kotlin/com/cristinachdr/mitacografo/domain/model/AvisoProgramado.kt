package com.cristinachdr.mitacografo.domain.model

import java.time.Duration
import java.time.Instant

/** Un aviso concreto, ya con su hora de disparo, listo para pasárselo al sistema de alarmas. */
data class AvisoProgramado(
    val jornadaId: Long,
    val disparo: Instant,
    val desdeInicio: Duration,
)

/**
 * Calcula qué avisos hay que programar para una jornada.
 *
 * - Solo para jornadas abiertas (al finalizar se cancelan todos).
 * - Se saltan los avisos desactivados y los que ya han pasado respecto a [ahora]
 *   (por ejemplo, si el conductor apunta la jornada con retraso).
 * - Se devuelven ordenados del primero al último.
 */
fun calcularAvisos(
    jornada: Jornada,
    avisos: List<AvisoConfig>,
    ahora: Instant,
): List<AvisoProgramado> {
    if (!jornada.abierta) return emptyList()
    return avisos
        .asSequence()
        .filter { it.activo }
        .map { AvisoProgramado(jornada.id, jornada.inicio.plus(it.desdeInicio), it.desdeInicio) }
        .filter { it.disparo.isAfter(ahora) }
        .distinctBy { it.disparo }
        .sortedBy { it.disparo }
        .toList()
}
