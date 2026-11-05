package com.cristinachdr.mitacografo.domain.usecase

import com.cristinachdr.mitacografo.domain.model.PreferenciasPaises
import com.cristinachdr.mitacografo.domain.port.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Caso de uso: lo que necesita el selector de país (recientes + habituales y continentes de Ajustes). */
class ObtenerPreferenciasPaises
    @Inject
    constructor(
        private val recientes: ObtenerPaisesRecientes,
        private val settings: SettingsRepository,
    ) {
        suspend operator fun invoke(): PreferenciasPaises {
            val ajustes = settings.observar().first()
            return PreferenciasPaises(
                recientes = recientes(),
                habituales = ajustes.paisesHabituales,
                continentes = ajustes.continentes,
            )
        }
    }
