package com.cristinachdr.mitacografo.domain.usecase

import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.port.JornadaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Caso de uso: la jornada en curso, que se actualiza sola al iniciar o finalizar. */
class ObtenerJornadaActual
    @Inject
    constructor(
        private val repository: JornadaRepository,
    ) {
        operator fun invoke(): Flow<Jornada?> = repository.observarAbierta()
    }
