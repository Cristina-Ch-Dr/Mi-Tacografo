package com.cristinachdr.mitacografo.domain.model

import java.time.Duration
import java.time.Instant

/** Motivos por los que no se puede guardar una jornada. La UI traduce cada uno a un mensaje. */
enum class ErrorJornada {
    JORNADA_YA_ABIERTA,
    SIN_JORNADA_ABIERTA,
    JORNADA_NO_EXISTE,
    KM_NEGATIVOS,
    KM_FIN_MENOR_QUE_INICIO,
    INICIO_EN_EL_FUTURO,
    INICIO_ANTES_DEL_FIN_ANTERIOR,
    FIN_NO_POSTERIOR_AL_INICIO,
    FIN_EN_EL_FUTURO,
    CONDUCCION_NEGATIVA,
    CONDUCCION_MAYOR_QUE_JORNADA,
    SOLAPA_CON_OTRA_JORNADA,
    FALTA_EL_FIN,
}

/**
 * Margen para no rechazar una hora "del futuro" por unos minutos:
 * el conductor puede apuntar la hora mirando otro reloj.
 */
val MARGEN_HORA_FUTURA: Duration = Duration.ofMinutes(5)

/**
 * Comprueba las reglas de una jornada por sí sola (sin mirar la base de datos).
 *
 * La usan los casos de uso antes de guardar y la UI para avisar mientras se escribe.
 *
 * @param finAnterior fin de la jornada anterior, para que no se solapen.
 * @return los errores encontrados; vacío si la jornada es válida.
 */
fun validarJornada(
    jornada: Jornada,
    ahora: Instant,
    finAnterior: Instant? = null,
): Set<ErrorJornada> =
    buildSet {
        val limiteFuturo = ahora.plus(MARGEN_HORA_FUTURA)
        val kmFin = jornada.kmFin
        val fin = jornada.fin
        val conduccion = jornada.conduccionTacografo

        if (jornada.kmInicio < 0 || (kmFin != null && kmFin < 0)) add(ErrorJornada.KM_NEGATIVOS)
        if (kmFin != null && kmFin < jornada.kmInicio) add(ErrorJornada.KM_FIN_MENOR_QUE_INICIO)

        if (jornada.inicio.isAfter(limiteFuturo)) add(ErrorJornada.INICIO_EN_EL_FUTURO)
        if (finAnterior != null && jornada.inicio.isBefore(finAnterior)) add(ErrorJornada.INICIO_ANTES_DEL_FIN_ANTERIOR)

        if (fin != null) {
            if (!fin.isAfter(jornada.inicio)) add(ErrorJornada.FIN_NO_POSTERIOR_AL_INICIO)
            if (fin.isAfter(limiteFuturo)) add(ErrorJornada.FIN_EN_EL_FUTURO)
        }

        if (conduccion != null) {
            if (conduccion.isNegative) add(ErrorJornada.CONDUCCION_NEGATIVA)
            if (fin != null && fin.isAfter(jornada.inicio) && conduccion > Duration.between(jornada.inicio, fin)) {
                add(ErrorJornada.CONDUCCION_MAYOR_QUE_JORNADA)
            }
        }
    }

/** Resultado de una operación sobre jornadas: o sale bien, o devuelve por qué no. */
sealed interface ResultadoJornada<out T> {
    data class Ok<T>(
        val valor: T,
    ) : ResultadoJornada<T>

    data class Invalida(
        val errores: Set<ErrorJornada>,
    ) : ResultadoJornada<Nothing> {
        constructor(error: ErrorJornada) : this(setOf(error))
    }
}
