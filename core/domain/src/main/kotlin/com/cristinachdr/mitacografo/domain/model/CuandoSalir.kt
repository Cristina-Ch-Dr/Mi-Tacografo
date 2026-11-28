package com.cristinachdr.mitacografo.domain.model

import java.time.Duration
import java.time.Instant

/** Cuánto antes de salir suena el despertador, si no se elige otra cosa (MT-57). */
val ANTELACION_DESPERTADOR_POR_DEFECTO: Duration = Duration.ofMinutes(60)

/** Las antelaciones que se ofrecen de un toque, como en su app. También se puede escribir otra. */
val ANTELACIONES_DESPERTADOR: List<Duration> = listOf(30L, 45L, 60L, 90L, 120L).map(Duration::ofMinutes)

/** Lo más que se puede adelantar el despertador: más de 4 h antes de salir no tiene sentido. */
val ANTELACION_DESPERTADOR_MAXIMA: Duration = Duration.ofHours(4)

/** Con un descanso de [descanso] desde el fin de la jornada: se puede salir a [salida], despertando a [despertar]. */
data class SalidaPosible(
    val descanso: Duration,
    val salida: Instant,
    val despertar: Instant,
)

/**
 * «Cuándo salir» (MT-57): al terminar la jornada, a qué hora se puede volver a conducir con un descanso
 * diario reducido (9 h) o normal (11 h), y a qué hora despertar para salir a esa hora.
 */
fun cuandoSalir(
    finJornada: Instant,
    antelacion: Duration,
): List<SalidaPosible> =
    listOf(Duration.ofHours(9), Duration.ofHours(11)).map { descanso ->
        val salida = finJornada.plus(descanso)
        SalidaPosible(descanso, salida, salida.minus(antelacion))
    }
