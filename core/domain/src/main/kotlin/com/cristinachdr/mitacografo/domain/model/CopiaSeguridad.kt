package com.cristinachdr.mitacografo.domain.model

import java.time.Instant

/**
 * Todo lo que guarda la app, en un solo archivo: jornadas, descansos, viajes y ajustes.
 *
 * Sirve para cambiar de móvil o recuperar los datos si se borra la app.
 */
data class CopiaSeguridad(
    val creada: Instant,
    val jornadas: List<Jornada>,
    val ajustes: Ajustes,
    /** Pausas y descansos. Las copias anteriores a MT-20 no los traen: se leen como lista vacía. */
    val descansos: List<Descanso> = emptyList(),
    /** Viajes. Las copias anteriores a MT-18 no los traen: se leen como lista vacía. */
    val viajes: List<Viaje> = emptyList(),
) {
    /** La primera y la última jornada de la copia, para el resumen antes de restaurar. */
    val desde: Instant? get() = jornadas.minOfOrNull { it.inicio }
    val hasta: Instant? get() = jornadas.maxOfOrNull { it.inicio }

    companion object {
        /**
         * Versión del formato del archivo. Si un día cambia, se sube este número y se añade
         * la conversión desde la versión anterior: así una copia antigua se sigue pudiendo restaurar.
         */
        const val VERSION_FORMATO = 1
    }
}

/** Por qué no se puede restaurar un archivo. La UI traduce cada uno a un mensaje. */
enum class ErrorCopia {
    /** No es un archivo de Mi Tacógrafo (otro JSON, una foto…) o está roto. */
    NO_ES_UNA_COPIA,

    /** Lo hizo una versión más nueva de la app: hay que actualizar la app primero. */
    VERSION_MAS_NUEVA,

    /** El archivo se lee, pero sus datos no tienen sentido (dos jornadas abiertas, ids repetidos…). */
    DATOS_INCORRECTOS,
}

class CopiaInvalidaException(
    val error: ErrorCopia,
    causa: Throwable? = null,
) : Exception(error.name, causa)
