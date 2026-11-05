package com.cristinachdr.mitacografo.domain.usecase

import com.cristinachdr.mitacografo.domain.model.CopiaInvalidaException
import com.cristinachdr.mitacografo.domain.model.CopiaSeguridad
import com.cristinachdr.mitacografo.domain.model.ErrorCopia
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.port.BackupPort
import com.cristinachdr.mitacografo.domain.port.JornadaRepository
import com.cristinachdr.mitacografo.domain.port.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.Clock
import javax.inject.Inject

/** Caso de uso: preparar el texto del archivo de copia con todas las jornadas y los ajustes. */
class ExportarCopia
    @Inject
    constructor(
        private val repository: JornadaRepository,
        private val settings: SettingsRepository,
        private val backup: BackupPort,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(): String {
            val copia =
                CopiaSeguridad(
                    creada = clock.instant(),
                    jornadas = repository.observarTodas().first(),
                    ajustes = settings.observar().first(),
                )
            return backup.codificar(copia)
        }
    }

/** Resultado de leer un archivo: la copia (para enseñar el resumen) o por qué no vale. */
sealed interface ResultadoLectura {
    data class Ok(
        val copia: CopiaSeguridad,
    ) : ResultadoLectura

    data class Invalida(
        val error: ErrorCopia,
    ) : ResultadoLectura
}

/**
 * Caso de uso: leer un archivo de copia **sin tocar nada**.
 *
 * La pantalla enseña el resumen («120 jornadas, del … al …») y solo restaura si el conductor confirma.
 */
class LeerCopia
    @Inject
    constructor(
        private val backup: BackupPort,
    ) {
        operator fun invoke(texto: String): ResultadoLectura =
            try {
                val copia = backup.decodificar(texto)
                val correcta = datosCorrectos(copia.jornadas)
                if (correcta) ResultadoLectura.Ok(copia) else ResultadoLectura.Invalida(ErrorCopia.DATOS_INCORRECTOS)
            } catch (e: CopiaInvalidaException) {
                ResultadoLectura.Invalida(e.error)
            }

        /** Lo mínimo para que la app funcione después: ids únicos y como mucho una jornada abierta. */
        private fun datosCorrectos(jornadas: List<Jornada>): Boolean =
            jornadas.all { it.id > Jornada.SIN_ID } &&
                jornadas.map { it.id }.toSet().size == jornadas.size &&
                jornadas.count { it.abierta } <= 1 &&
                jornadas.all { jornada -> jornada.fin?.isAfter(jornada.inicio) ?: true }
    }

/**
 * Caso de uso: **reemplazar** todos los datos por los de la copia (jornadas y ajustes).
 *
 * Las jornadas se cambian de una vez (transacción): si algo falla a mitad, se queda todo como estaba.
 * Después se reprograman los avisos por si la copia trae una jornada en curso.
 */
class RestaurarCopia
    @Inject
    constructor(
        private val repository: JornadaRepository,
        private val settings: SettingsRepository,
        private val avisos: GestorAvisos,
    ) {
        suspend operator fun invoke(copia: CopiaSeguridad) {
            // Los avisos de la jornada en curso de ahora dejarían de tener sentido.
            repository.obtenerAbierta()?.let { avisos.cancelar(it.id) }
            repository.reemplazarTodas(copia.jornadas)
            settings.actualizar { copia.ajustes }
            avisos.reprogramarJornadaAbierta()
        }
    }
