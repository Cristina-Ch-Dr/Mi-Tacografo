package com.cristinachdr.mitacografo.domain.usecase

import com.cristinachdr.mitacografo.domain.model.SemanaJornadas
import com.cristinachdr.mitacografo.domain.model.agruparPorSemana
import com.cristinachdr.mitacografo.domain.port.JornadaRepository
import com.cristinachdr.mitacografo.domain.port.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/** Caso de uso: todas las jornadas agrupadas por semana. Se actualiza solo al guardar o borrar. */
class ObtenerHistorial
    @Inject
    constructor(
        private val repository: JornadaRepository,
        private val settings: SettingsRepository,
    ) {
        operator fun invoke(): Flow<List<SemanaJornadas>> =
            combine(repository.observarTodas(), settings.observar()) { jornadas, ajustes ->
                agruparPorSemana(jornadas, ajustes.zonaReferencia)
            }
    }
