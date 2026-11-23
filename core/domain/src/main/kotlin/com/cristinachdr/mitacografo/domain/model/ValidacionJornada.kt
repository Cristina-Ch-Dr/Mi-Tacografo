package com.cristinachdr.mitacografo.domain.model

import java.time.Duration
import java.time.Instant
import java.time.ZoneId

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

    /** Hay conducción antes de medianoche del domingo, pero no la total. */
    ANTES_MEDIANOCHE_SIN_TOTAL,

    /** La de antes de medianoche no cabe: es mayor que la total o que las horas a un lado de la medianoche. */
    ANTES_MEDIANOCHE_NO_CUADRA,
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
 * @param zonaReferencia la de Ajustes: en ella se mira la medianoche del domingo (como el motor de reglas).
 * @return los errores encontrados; vacío si la jornada es válida.
 */
fun validarJornada(
    jornada: Jornada,
    ahora: Instant,
    finAnterior: Instant? = null,
    zonaReferencia: ZoneId? = null,
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
            // En curso (MT-53): no puede pasar de lo que llevaba la jornada al apuntarla.
            val hasta = fin ?: jornada.conduccionApuntadaEn
            if (hasta != null && hasta.isAfter(jornada.inicio) &&
                conduccion > Duration.between(jornada.inicio, hasta)
            ) {
                add(ErrorJornada.CONDUCCION_MAYOR_QUE_JORNADA)
            }
        }

        addAll(validarAntesMedianoche(jornada, zonaReferencia))
    }

/** La conducción antes de medianoche del domingo (MT-50): con total, y que quepa a cada lado de la medianoche. */
private fun validarAntesMedianoche(
    jornada: Jornada,
    zonaReferencia: ZoneId?,
): Set<ErrorJornada> {
    val antes = jornada.conduccionAntesMedianoche ?: return emptySet()
    val total = jornada.conduccionTacografo ?: return setOf(ErrorJornada.ANTES_MEDIANOCHE_SIN_TOTAL)
    val fin = jornada.fin ?: return emptySet()
    val cruce = jornada.cruceDeSemana(zonaReferencia) ?: return emptySet()
    val despues = total - antes
    val cuadra =
        !antes.isNegative &&
            !despues.isNegative &&
            antes <= Duration.between(jornada.inicio, cruce) &&
            despues <= Duration.between(cruce, fin)
    return if (cuadra) emptySet() else setOf(ErrorJornada.ANTES_MEDIANOCHE_NO_CUADRA)
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
