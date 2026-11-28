package com.cristinachdr.mitacografo.domain.usecase

import com.cristinachdr.mitacografo.domain.port.Despertador
import com.cristinachdr.mitacografo.domain.port.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * Caso de uso: poner el despertador (MT-57). Se guarda la hora en Ajustes, para que Inicio la enseñe
 * y para volver a programarlo si se reinicia el móvil.
 *
 * @return `false` si la hora ya ha pasado (no se pone).
 */
class PonerDespertador
    @Inject
    constructor(
        private val settings: SettingsRepository,
        private val despertador: Despertador,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(hora: Instant): Boolean {
            if (!hora.isAfter(clock.instant())) return false
            settings.actualizar { it.copy(despertador = hora) }
            despertador.programar(hora)
            return true
        }
    }

/** Caso de uso: quitar el despertador, o apuntar que ya sonó. */
class QuitarDespertador
    @Inject
    constructor(
        private val settings: SettingsRepository,
        private val despertador: Despertador,
    ) {
        suspend operator fun invoke() {
            despertador.cancelar()
            settings.actualizar { it.copy(despertador = null) }
        }
    }

/**
 * Caso de uso: al reiniciar el móvil Android borra las alarmas. Si el despertador era para más tarde,
 * se vuelve a poner; si su hora ya pasó con el móvil apagado, se olvida.
 */
class ReprogramarDespertador
    @Inject
    constructor(
        private val settings: SettingsRepository,
        private val despertador: Despertador,
        private val clock: Clock,
    ) {
        suspend operator fun invoke() {
            val hora = settings.observar().first().despertador ?: return
            if (hora.isAfter(clock.instant())) {
                despertador.programar(hora)
            } else {
                settings.actualizar { it.copy(despertador = null) }
            }
        }
    }
