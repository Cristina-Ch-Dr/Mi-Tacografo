package com.cristinachdr.mitacografo.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristinachdr.mitacografo.domain.model.AvisoConfig
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.calcularAvisos
import com.cristinachdr.mitacografo.domain.usecase.ObtenerJornadaActual
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject

/** Estado de la pantalla de Inicio. */
data class HomeUiState(
    val hoy: LocalDate,
    /** `null` si no hay jornada en curso. */
    val jornada: JornadaEnCursoUi? = null,
)

data class JornadaEnCursoUi(
    /** Hora de inicio en la zona donde empezó. */
    val inicio: ZonedDateTime,
    val transcurrido: Duration,
    val kmInicio: Int,
    /** `null` si ya han pasado todos los avisos. */
    val proximoAviso: AvisoUi?,
)

data class AvisoUi(
    /** Hora del aviso en la zona del móvil (la que ve el conductor en su reloj). */
    val hora: ZonedDateTime,
    val desdeInicio: Duration,
)

private const val UN_MINUTO_MS = 60_000L

/**
 * Combina la jornada en curso (que viene de Room y cambia al iniciar o finalizar)
 * con un "tic" cada minuto, para que el tiempo transcurrido se actualice solo.
 */
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        obtenerJornadaActual: ObtenerJornadaActual,
        private val clock: Clock,
    ) : ViewModel() {
        val uiState: StateFlow<HomeUiState> =
            combine(obtenerJornadaActual(), cadaMinuto(), ::crearEstado)
                .stateIn(
                    scope = viewModelScope,
                    // Deja de contar 5 s después de salir de la pantalla (ahorra batería).
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = crearEstado(null, clock.instant()),
                )

        /** Emite la hora actual ahora y luego al empezar cada minuto. */
        private fun cadaMinuto(): Flow<Instant> =
            flow {
                while (true) {
                    val ahora = clock.instant()
                    emit(ahora)
                    delay(UN_MINUTO_MS - ahora.toEpochMilli() % UN_MINUTO_MS)
                }
            }

        private fun crearEstado(
            jornada: Jornada?,
            ahora: Instant,
        ): HomeUiState =
            HomeUiState(
                hoy = ahora.atZone(clock.zone).toLocalDate(),
                jornada = jornada?.let { enCurso(it, ahora) },
            )

        private fun enCurso(
            jornada: Jornada,
            ahora: Instant,
        ): JornadaEnCursoUi {
            // Hasta MT-13 (Ajustes) se usan los avisos por defecto: 12:30, 13:00, 14:30 y 15:00.
            val proximo = calcularAvisos(jornada, AvisoConfig.POR_DEFECTO, ahora).firstOrNull()
            return JornadaEnCursoUi(
                inicio = jornada.inicio.atZone(jornada.zonaInicio),
                transcurrido = Duration.between(jornada.inicio, ahora).coerceAtLeast(Duration.ZERO),
                kmInicio = jornada.kmInicio,
                proximoAviso = proximo?.let { AvisoUi(it.disparo.atZone(clock.zone), it.desdeInicio) },
            )
        }
    }
