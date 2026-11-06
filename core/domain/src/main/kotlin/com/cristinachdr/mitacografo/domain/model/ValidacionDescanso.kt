package com.cristinachdr.mitacografo.domain.model

import java.time.Instant

/** Motivos por los que no se puede guardar una pausa o un descanso. La UI traduce cada uno. */
enum class ErrorDescanso {
    DESCANSO_NO_EXISTE,
    SIN_JORNADA_ABIERTA,
    YA_HAY_UNO_EN_CURSO,
    SIN_PAUSA_EN_CURSO,
    INICIO_EN_EL_FUTURO,
    FIN_NO_POSTERIOR_AL_INICIO,
    FIN_EN_EL_FUTURO,
    SOLAPA_CON_OTRO_DESCANSO,
}

/**
 * Comprueba un descanso frente a los demás ya guardados.
 *
 * Los descansos no pueden solaparse entre sí. Con una jornada sí (espec. §5.2: puede haber
 * errores al apuntar), así que eso no se comprueba aquí.
 *
 * @param otros los demás descansos (sin el que se está editando).
 */
fun validarDescanso(
    descanso: Descanso,
    otros: List<Descanso>,
    ahora: Instant,
): Set<ErrorDescanso> =
    buildSet {
        val limiteFuturo = ahora.plus(MARGEN_HORA_FUTURA)
        val fin = descanso.fin

        if (descanso.inicio.isAfter(limiteFuturo)) add(ErrorDescanso.INICIO_EN_EL_FUTURO)
        if (fin != null) {
            if (!fin.isAfter(descanso.inicio)) add(ErrorDescanso.FIN_NO_POSTERIOR_AL_INICIO)
            if (fin.isAfter(limiteFuturo)) add(ErrorDescanso.FIN_EN_EL_FUTURO)
        }
        if (descanso.enCurso && otros.any { it.enCurso }) add(ErrorDescanso.YA_HAY_UNO_EN_CURSO)
        if (otros.any { it.solapaCon(descanso, ahora) }) add(ErrorDescanso.SOLAPA_CON_OTRO_DESCANSO)
    }

/** Se solapan si uno empieza antes de que termine el otro. Uno en curso dura hasta [ahora]. */
private fun Descanso.solapaCon(
    otro: Descanso,
    ahora: Instant,
): Boolean = inicio < (otro.fin ?: ahora) && otro.inicio < (fin ?: ahora)

/** Resultado de una operación sobre descansos: o sale bien, o devuelve por qué no. */
sealed interface ResultadoDescanso<out T> {
    data class Ok<T>(
        val valor: T,
    ) : ResultadoDescanso<T>

    data class Invalido(
        val errores: Set<ErrorDescanso>,
    ) : ResultadoDescanso<Nothing> {
        constructor(error: ErrorDescanso) : this(setOf(error))
    }
}
