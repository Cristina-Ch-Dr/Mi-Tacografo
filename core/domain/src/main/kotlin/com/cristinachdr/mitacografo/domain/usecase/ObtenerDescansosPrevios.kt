package com.cristinachdr.mitacografo.domain.usecase

import com.cristinachdr.mitacografo.domain.model.DescansoPrevio
import com.cristinachdr.mitacografo.domain.model.clasificarDescansos
import com.cristinachdr.mitacografo.domain.model.completarDescansos
import com.cristinachdr.mitacografo.domain.model.descansoAntesDeCadaJornada
import com.cristinachdr.mitacografo.domain.port.DescansoRepository
import com.cristinachdr.mitacografo.domain.port.JornadaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/** Caso de uso: lo que descansó antes de cada jornada (por id), apuntado o calculado entre jornadas (MT-46). */
class ObtenerDescansosPrevios
    @Inject
    constructor(
        private val jornadas: JornadaRepository,
        private val descansos: DescansoRepository,
    ) {
        operator fun invoke(): Flow<Map<Long, DescansoPrevio>> =
            combine(jornadas.observarTodas(), descansos.observarTodos()) { j, d ->
                descansoAntesDeCadaJornada(j, clasificarDescansos(completarDescansos(j, d)))
            }
    }
