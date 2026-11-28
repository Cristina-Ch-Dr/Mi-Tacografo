package com.cristinachdr.mitacografo.domain.model

import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/** Cuánto antes de salir suena el despertador, si no se elige otra cosa (MT-57). */
val ANTELACION_DESPERTADOR_POR_DEFECTO: Duration = Duration.ofMinutes(60)

/** Las antelaciones que se ofrecen de un toque, como en su app. También se puede escribir otra. */
val ANTELACIONES_DESPERTADOR: List<Duration> = listOf(30L, 45L, 60L, 90L, 120L).map(Duration::ofMinutes)

/** Lo más que se puede adelantar el despertador: más de 4 h antes de salir no tiene sentido. */
val ANTELACION_DESPERTADOR_MAXIMA: Duration = Duration.ofHours(4)

/**
 * Con un descanso de [descanso] desde el fin de la jornada: se puede salir a [salida], despertando a [despertar].
 * Si el conductor cambió la hora de salida a mano (MT-60), [manual] y el descanso real es [descansoReal].
 */
data class SalidaPosible(
    val descanso: Duration,
    val salida: Instant,
    val despertar: Instant,
    val manual: Boolean = false,
    val descansoReal: Duration = descanso,
) {
    /** Con la salida elegida a mano no llega ni a un diario reducido: se enseña en rojo. */
    val descansoCorto: Boolean get() = descansoReal < Duration.ofHours(9)
}

/**
 * Las horas de salida cambiadas a mano (MT-60), solo para la jornada que terminó a las [fin]: al terminar otra,
 * se vuelve a las calculadas, como en su app.
 */
data class SalidasManuales(
    val fin: Instant,
    /** Por descanso (9 h u 11 h), la hora de salida elegida. */
    val salidas: Map<Duration, Instant>,
)

/**
 * «Cuándo salir» (MT-57): al terminar la jornada, a qué hora se puede volver a conducir con un descanso
 * diario reducido (9 h) o normal (11 h), y a qué hora despertar para salir a esa hora.
 */
fun cuandoSalir(
    finJornada: Instant,
    antelacion: Duration,
    manuales: SalidasManuales? = null,
): List<SalidaPosible> =
    DESCANSOS_SALIDA.map { descanso ->
        val elegida = manuales?.takeIf { it.fin == finJornada }?.salidas?.get(descanso)
        val salida = elegida ?: finJornada.plus(descanso)
        SalidaPosible(
            descanso = descanso,
            salida = salida,
            despertar = salida.minus(antelacion),
            manual = elegida != null,
            descansoReal = Duration.between(finJornada, salida),
        )
    }

/** Las dos opciones de «Cuándo salir»: con descanso diario reducido (9 h) o normal (11 h). */
val DESCANSOS_SALIDA: List<Duration> = listOf(Duration.ofHours(9), Duration.ofHours(11))

/**
 * La salida a la [hora] elegida (en [zona]) más próxima después del fin de la jornada: el mismo día si aún no ha
 * pasado, y si no, el siguiente. Como en su app.
 */
fun salidaALas(
    finJornada: Instant,
    hora: LocalTime,
    zona: ZoneId,
): Instant {
    val mismoDia =
        finJornada
            .atZone(zona)
            .toLocalDate()
            .atTime(hora)
            .atZone(zona)
            .toInstant()
    return if (mismoDia.isAfter(finJornada)) mismoDia else mismoDia.atZone(zona).plusDays(1).toInstant()
}

/** El próximo momento en que el reloj marque [hora] (en [zona]) después de [ahora]: para la «Hora propia» del despertador. */
fun proximaVezALas(
    ahora: Instant,
    hora: LocalTime,
    zona: ZoneId,
): Instant = salidaALas(ahora, hora, zona)
