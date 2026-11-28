package com.cristinachdr.mitacografo.domain.usecase

import com.cristinachdr.mitacografo.domain.model.SalidasManuales
import com.cristinachdr.mitacografo.domain.model.cuandoSalir
import com.cristinachdr.mitacografo.domain.port.Despertador
import com.cristinachdr.mitacografo.domain.port.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Duration
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

/**
 * Caso de uso: cambiar a mano la hora de salida de una opción de «Cuándo salir» (MT-60), o volver a la
 * calculada con `null`. Si el despertador estaba puesto para esa opción, se mueve con ella.
 */
class CambiarSalida
    @Inject
    constructor(
        private val settings: SettingsRepository,
        private val despertador: Despertador,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(
            finJornada: Instant,
            descanso: Duration,
            salida: Instant?,
        ) {
            if (salida != null && !salida.isAfter(finJornada)) return
            var reprogramar: Instant? = null
            settings.actualizar { ajustes ->
                val antes = cuandoSalir(finJornada, ajustes.antelacionDespertador, ajustes.salidasManuales)
                val actuales =
                    ajustes.salidasManuales
                        ?.takeIf { it.fin == finJornada }
                        ?.salidas
                        .orEmpty()
                val nuevas = if (salida == null) actuales - descanso else actuales + (descanso to salida)
                val manuales = SalidasManuales(finJornada, nuevas).takeIf { nuevas.isNotEmpty() }
                val despues = cuandoSalir(finJornada, ajustes.antelacionDespertador, manuales)
                val anterior = antes.first { it.descanso == descanso }.despertar
                val nuevo = despues.first { it.descanso == descanso }.despertar
                val mover = ajustes.despertador == anterior && nuevo.isAfter(clock.instant())
                if (mover) reprogramar = nuevo
                ajustes.copy(salidasManuales = manuales, despertador = if (mover) nuevo else ajustes.despertador)
            }
            reprogramar?.let { despertador.programar(it) }
        }
    }
