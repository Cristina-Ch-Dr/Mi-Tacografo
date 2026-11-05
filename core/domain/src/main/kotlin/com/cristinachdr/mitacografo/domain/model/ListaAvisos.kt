package com.cristinachdr.mitacografo.domain.model

import java.time.Duration

/** Por qué no se puede guardar un aviso. */
enum class ErrorAviso {
    /** Menos de [AvisoConfig.MINIMO] o más de [AvisoConfig.MAXIMO] desde el inicio. */
    FUERA_DE_RANGO,

    /** Ya hay otro aviso a esa misma hora. */
    REPETIDO,

    /** Ya hay [AvisoConfig.MAX_AVISOS] avisos. */
    DEMASIADOS,
}

/**
 * ¿Se puede poner un aviso a [desdeInicio]?
 *
 * @param editando posición del aviso que se está cambiando, o `null` si es uno nuevo.
 *   El propio aviso no cuenta como repetido (se puede guardar sin cambiar la hora).
 */
fun List<AvisoConfig>.errorAviso(
    desdeInicio: Duration,
    editando: Int? = null,
): ErrorAviso? =
    when {
        desdeInicio < AvisoConfig.MINIMO || desdeInicio > AvisoConfig.MAXIMO -> ErrorAviso.FUERA_DE_RANGO
        filterIndexed { i, it -> i != editando && it.desdeInicio == desdeInicio }.isNotEmpty() -> ErrorAviso.REPETIDO
        editando == null && size >= AvisoConfig.MAX_AVISOS -> ErrorAviso.DEMASIADOS
        else -> null
    }

/**
 * La lista con el aviso añadido ([editando] `null`) o cambiado de hora, ordenada de menor a mayor.
 * Un aviso nuevo empieza activado; uno cambiado conserva si estaba activado o no.
 * Hay que comprobar antes [errorAviso].
 */
fun List<AvisoConfig>.conAviso(
    desdeInicio: Duration,
    editando: Int? = null,
): List<AvisoConfig> {
    val nueva =
        if (editando == null) {
            this + AvisoConfig(desdeInicio)
        } else {
            mapIndexed { i, it -> if (i == editando) it.copy(desdeInicio = desdeInicio) else it }
        }
    return nueva.sortedBy { it.desdeInicio }
}

/** La lista sin el aviso de la posición [indice]. */
fun List<AvisoConfig>.sinAviso(indice: Int): List<AvisoConfig> = filterIndexed { i, _ -> i != indice }
