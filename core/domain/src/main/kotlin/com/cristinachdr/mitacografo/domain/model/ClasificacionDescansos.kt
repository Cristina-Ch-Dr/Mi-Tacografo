package com.cristinachdr.mitacografo.domain.model

import java.time.Duration
import java.time.Instant

/**
 * Qué es cada descanso según el Reglamento (CE) 561/2006, art. 4 y 8 (espec. §7.4 y §7.5).
 *
 * Solo mira la duración y el orden. Si se cumplen las reglas (3 reducidos como mucho,
 * semanal cada 6 días, compensaciones…) lo comprueba el motor de reglas (E4).
 */
enum class ClaseDescanso {
    /** Pausa dentro de la jornada. */
    PAUSA,

    /** Sin hora de fin: aún no se sabe qué será. */
    EN_CURSO,

    /** Menos de 3 h (o una primera parte que no llegó a completarse): no cuenta como descanso diario. */
    INSUFICIENTE,

    /** De 3 h a menos de 9 h: puede ser la primera parte de un fraccionado 3 + 9. */
    FRACCIONADO_PRIMERA_PARTE,

    /** La segunda parte (9 h o más) de un fraccionado 3 + 9: los dos juntos cuentan como un diario normal. */
    FRACCIONADO_COMPLETO,

    /** De 9 h a menos de 11 h. */
    DIARIO_REDUCIDO,

    /** De 11 h a menos de 24 h. */
    DIARIO_NORMAL,

    /** De 24 h a menos de 45 h. Deja una deuda que hay que compensar. */
    SEMANAL_REDUCIDO,

    /** 45 h o más. */
    SEMANAL_NORMAL,
}

/** Un descanso con su clase y, si es un semanal reducido, lo que le falta hasta 45 h. */
data class DescansoClasificado(
    val descanso: Descanso,
    val clase: ClaseDescanso,
    val deuda: Duration = Duration.ZERO,
)

private val MINIMO_PRIMERA_PARTE: Duration = Duration.ofHours(3)
private val MINIMO_DIARIO: Duration = Duration.ofHours(9)
private val DIARIO_NORMAL: Duration = Duration.ofHours(11)
private val MINIMO_SEMANAL: Duration = Duration.ofHours(24)
private val SEMANAL_NORMAL: Duration = Duration.ofHours(45)

/** Las dos partes de un fraccionado tienen que caber en las 24 h que siguen al último descanso diario. */
private val PERIODO_DIARIO: Duration = Duration.ofHours(24)

/**
 * Clasifica todos los descansos de una vez, porque un fraccionado depende del anterior y del siguiente.
 *
 * Fraccionado 3 + 9: una parte de 3 h a menos de 9 h y, después, otra de 9 h o más, en ese orden.
 * La segunda tiene que terminar dentro de las 24 h que empiezan al acabar el último descanso
 * diario o semanal. Si no hay uno anterior registrado, se cuenta desde el inicio de la primera parte.
 * Las pausas de en medio no cuentan. ⚠️ Revisar con la guía oficial antes del motor de reglas (E4).
 *
 * @return los descansos en orden cronológico, cada uno con su clase.
 */
fun clasificarDescansos(descansos: List<Descanso>): List<DescansoClasificado> {
    val ordenados = descansos.sortedBy { it.inicio }
    val clases = arrayOfNulls<ClaseDescanso>(ordenados.size)
    // Fin del último descanso de 9 h o más ya clasificado: empieza el periodo de 24 h siguiente.
    var finUltimoDiario: Instant? = null
    // Primera parte que espera a su segunda parte (índice en `ordenados`).
    var primeraParte: Int? = null

    ordenados.forEachIndexed { i, descanso ->
        val duracion = descanso.duracion
        val clase =
            when {
                descanso.tipo == TipoDescanso.PAUSA -> {
                    ClaseDescanso.PAUSA
                }

                duracion == null -> {
                    ClaseDescanso.EN_CURSO
                }

                duracion < MINIMO_PRIMERA_PARTE -> {
                    ClaseDescanso.INSUFICIENTE
                }

                duracion < MINIMO_DIARIO -> {
                    // Una primera parte anterior que no llegó a completarse ya no cuenta.
                    primeraParte?.let { clases[it] = ClaseDescanso.INSUFICIENTE }
                    primeraParte = i
                    ClaseDescanso.FRACCIONADO_PRIMERA_PARTE
                }

                else -> {
                    val completa =
                        primeraParte?.let { indice ->
                            val desde = finUltimoDiario ?: ordenados[indice].inicio
                            duracion < DIARIO_NORMAL && !descanso.fin!!.isAfter(desde.plus(PERIODO_DIARIO))
                        } ?: false
                    if (!completa) primeraParte?.let { clases[it] = ClaseDescanso.INSUFICIENTE }
                    primeraParte = null
                    finUltimoDiario = descanso.fin
                    if (completa) ClaseDescanso.FRACCIONADO_COMPLETO else porDuracion(duracion)
                }
            }
        clases[i] = clase
    }

    return ordenados.mapIndexed { i, descanso ->
        val clase = clases[i]!!
        val deuda =
            if (clase == ClaseDescanso.SEMANAL_REDUCIDO) SEMANAL_NORMAL.minus(descanso.duracion) else Duration.ZERO
        DescansoClasificado(descanso, clase, deuda)
    }
}

/** Clase de un descanso de 9 h o más que no es la segunda parte de un fraccionado. */
private fun porDuracion(duracion: Duration): ClaseDescanso =
    when {
        duracion < DIARIO_NORMAL -> ClaseDescanso.DIARIO_REDUCIDO
        duracion < MINIMO_SEMANAL -> ClaseDescanso.DIARIO_NORMAL
        duracion < SEMANAL_NORMAL -> ClaseDescanso.SEMANAL_REDUCIDO
        else -> ClaseDescanso.SEMANAL_NORMAL
    }
