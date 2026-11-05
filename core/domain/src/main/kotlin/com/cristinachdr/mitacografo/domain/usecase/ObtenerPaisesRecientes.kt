package com.cristinachdr.mitacografo.domain.usecase

import com.cristinachdr.mitacografo.domain.port.JornadaRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Caso de uso: los últimos países usados en las jornadas (el más reciente primero, sin repetir).
 *
 * El selector de país los muestra arriba del todo: el conductor suele repetir siempre los mismos.
 */
class ObtenerPaisesRecientes
    @Inject
    constructor(
        private val repository: JornadaRepository,
    ) {
        suspend operator fun invoke(maximo: Int = MAXIMO): List<String> =
            repository
                .observarTodas()
                .first()
                .sortedByDescending { it.inicio }
                .flatMap { listOf(it.paisFin, it.paisInicio) }
                .mapNotNull { it?.trim()?.uppercase()?.ifEmpty { null } }
                .distinct()
                .take(maximo)

        companion object {
            const val MAXIMO = 5
        }
    }
