package com.cristinachdr.mitacografo.domain.model

import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

/** Km de un día del periodo (0 si no se trabajó). */
data class KmDia(
    val dia: LocalDate,
    val km: Int,
)

/**
 * Resumen de una semana legal (lunes a domingo) o de una bisemana: lo que se suma en la libreta.
 *
 * Solo cuenta lo que **empieza** dentro del periodo. No dice si se cumplen las reglas:
 * eso lo añadirá el motor del reglamento (E4) encima de este resumen.
 *
 * @param jornadas y [descansos] en orden cronológico.
 */
data class ResumenPeriodo(
    val desde: LocalDate,
    val semanas: Int,
    val jornadas: List<Jornada>,
    val descansos: List<DescansoClasificado>,
    val kmPorDia: List<KmDia>,
    /** Cada semana por separado (en una bisemana, dos). */
    val porSemana: List<ResumenPeriodo> = emptyList(),
) {
    val hasta: LocalDate get() = desde.plusWeeks(semanas.toLong()).minusDays(1)

    val numJornadas: Int get() = jornadas.size

    /** Km de las jornadas terminadas. */
    val km: Int get() = jornadas.sumOf { it.kmRecorridos ?: 0 }

    /** Tiempo entre inicio y fin de las jornadas terminadas. */
    val tiempoJornadas: Duration get() = jornadas.mapNotNull { it.duracion }.fold(Duration.ZERO, Duration::plus)

    /** Conducción según el tacógrafo (solo la apuntada). */
    val conduccion: Duration get() = jornadas.mapNotNull { it.conduccionTacografo }.fold(Duration.ZERO, Duration::plus)

    /** `false` si a alguna jornada terminada le falta la conducción: entonces [conduccion] se queda corta. */
    val conduccionCompleta: Boolean get() = jornadas.filterNot { it.abierta }.all { it.conduccionTacografo != null }

    /** Tiempo de descanso diario y semanal (apuntado o calculado entre jornadas); las pausas no cuentan. */
    val tiempoDescanso: Duration
        get() =
            descansos
                .filter { it.descanso.tipo == TipoDescanso.DESCANSO }
                .mapNotNull { it.descanso.duracion }
                .fold(Duration.ZERO, Duration::plus)

    /** Lo que falta hasta 45 h en los semanales reducidos del periodo (habrá que compensarlo). */
    val deudaSemanal: Duration get() = descansos.map { it.deuda }.fold(Duration.ZERO, Duration::plus)

    fun cuantos(clase: ClaseDescanso): Int = descansos.count { it.clase == clase }
}

/**
 * Construye el resumen de [semanas] semanas desde el lunes [desde].
 *
 * Los descansos se clasifican con **todos** los que hay (un fraccionado depende del anterior)
 * y después se quedan los del periodo. El día de cada registro se mira en [zonaReferencia]
 * (la de Ajustes) o, si no hay, en la zona donde empezó, como en el historial.
 */
fun resumirPeriodo(
    jornadas: List<Jornada>,
    descansos: List<Descanso>,
    desde: LocalDate,
    semanas: Int,
    zonaReferencia: ZoneId? = null,
): ResumenPeriodo {
    require(semanas >= 1) { "Al menos una semana" }
    val clasificados = clasificarDescansos(descansos)
    return resumir(jornadas.sortedBy { it.inicio }, clasificados, desde, semanas, zonaReferencia, conSemanas = true)
}

private fun resumir(
    jornadas: List<Jornada>,
    clasificados: List<DescansoClasificado>,
    desde: LocalDate,
    semanas: Int,
    zonaReferencia: ZoneId?,
    conSemanas: Boolean,
): ResumenPeriodo {
    val hasta = desde.plusWeeks(semanas.toLong())

    fun dentro(dia: LocalDate) = !dia.isBefore(desde) && dia.isBefore(hasta)

    val delPeriodo = jornadas.filter { dentro(it.dia(zonaReferencia)) }
    val descansos =
        clasificados.filter {
            dentro(
                it.descanso.inicio
                    .atZone(zonaReferencia ?: it.descanso.zonaInicio)
                    .toLocalDate(),
            )
        }
    val kmPorDia =
        (0 until semanas * DIAS_SEMANA).map { i ->
            val dia = desde.plusDays(i.toLong())
            KmDia(dia, delPeriodo.filter { it.dia(zonaReferencia) == dia }.sumOf { it.kmRecorridos ?: 0 })
        }
    val porSemana =
        if (conSemanas) {
            (0 until semanas).map { i ->
                resumir(jornadas, clasificados, desde.plusWeeks(i.toLong()), 1, zonaReferencia, conSemanas = false)
            }
        } else {
            emptyList()
        }
    return ResumenPeriodo(desde, semanas, delPeriodo, descansos, kmPorDia, porSemana)
}

private const val DIAS_SEMANA = 7

private fun Jornada.dia(zonaReferencia: ZoneId?): LocalDate = inicio.atZone(zonaReferencia ?: zonaInicio).toLocalDate()
