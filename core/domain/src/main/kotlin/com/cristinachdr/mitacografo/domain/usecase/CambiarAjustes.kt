package com.cristinachdr.mitacografo.domain.usecase

import com.cristinachdr.mitacografo.domain.model.Ajustes
import com.cristinachdr.mitacografo.domain.port.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Caso de uso: cambiar los ajustes.
 *
 * Si cambian los avisos con una jornada en curso, se reprograman al momento
 * (por ejemplo, al desactivar el de 12 h 30 ya no debe sonar).
 */
class CambiarAjustes
    @Inject
    constructor(
        private val settings: SettingsRepository,
        private val avisos: GestorAvisos,
    ) {
        suspend operator fun invoke(cambio: (Ajustes) -> Ajustes) {
            val antes = settings.observar().first().avisos
            settings.actualizar(cambio)
            if (settings.observar().first().avisos != antes) avisos.reprogramarJornadaAbierta()
        }
    }
