package com.cristinachdr.mitacografo.feature.home

import java.time.Duration
import java.time.ZonedDateTime

/** Lo que sale arriba de Inicio, en un recuadro de color, como en la app que se hizo el conductor (MT-51). */
sealed interface AvisoInicio {
    /** Grave (rojo): ya se ha pasado. Si no, es para que se prepare (naranja). */
    val grave: Boolean

    /** Más de 13 h desde el inicio de la jornada: el descanso de hoy ya no llega a 11 h dentro de las 24 h. */
    data object Jornada13h : AvisoInicio {
        override val grave = false
    }

    /** Más de 15 h: ni siquiera cabe un diario reducido de 9 h. */
    data object Jornada15h : AvisoInicio {
        override val grave = true
    }

    /** Queda menos de un día para que tenga que empezar el semanal. */
    data class SemanalPronto(
        val plazo: ZonedDateTime,
    ) : AvisoInicio {
        override val grave = false
    }

    data class SemanalVencido(
        val plazo: ZonedDateTime,
    ) : AvisoInicio {
        override val grave = true
    }
}

private val TRECE_HORAS: Duration = Duration.ofHours(13)
private val QUINCE_HORAS: Duration = Duration.ofHours(15)
private val UN_DIA: Duration = Duration.ofHours(24)

/**
 * Los avisos de arriba de Inicio.
 *
 * Las 13 h y 15 h se cuentan desde el inicio de la jornada, como en la libreta (el reglamento cuenta desde el
 * fin del descanso anterior, que casi siempre es lo mismo). El plazo del semanal no se avisa si ahora mismo
 * ya lleva 24 h o más descansando: ese descanso ya es el semanal.
 */
fun avisosInicio(
    estado: HomeUiState,
    plazoSemanal: ZonedDateTime?,
): List<AvisoInicio> {
    val avisos = mutableListOf<AvisoInicio>()
    val jornada = estado.jornada
    when {
        jornada == null -> Unit
        jornada.transcurrido >= QUINCE_HORAS -> avisos += AvisoInicio.Jornada15h
        jornada.transcurrido >= TRECE_HORAS -> avisos += AvisoInicio.Jornada13h
    }
    val enSemanal = jornada == null && (estado.descanso?.transcurrido ?: Duration.ZERO) >= UN_DIA
    if (plazoSemanal != null && !enSemanal) {
        val falta = Duration.between(estado.ahora, plazoSemanal.toInstant())
        when {
            falta.isNegative || falta.isZero -> avisos += AvisoInicio.SemanalVencido(plazoSemanal)
            falta <= UN_DIA -> avisos += AvisoInicio.SemanalPronto(plazoSemanal)
        }
    }
    return avisos
}
