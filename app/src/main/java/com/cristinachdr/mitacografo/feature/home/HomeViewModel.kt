package com.cristinachdr.mitacografo.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristinachdr.mitacografo.domain.model.AvisoConfig
import com.cristinachdr.mitacografo.domain.model.Descanso
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.calcularAvisos
import com.cristinachdr.mitacografo.domain.port.DescansoRepository
import com.cristinachdr.mitacografo.domain.port.SettingsRepository
import com.cristinachdr.mitacografo.domain.usecase.IniciarPausa
import com.cristinachdr.mitacografo.domain.usecase.ObtenerJornadaActual
import com.cristinachdr.mitacografo.domain.usecase.TerminarPausa
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    /** `null` si no hay pausa en curso. */
    val pausa: PausaUi? = null,
)

data class PausaUi(
    /** Hora de inicio en la zona donde empezó. */
    val desde: ZonedDateTime,
    val transcurrido: Duration,
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
        settings: SettingsRepository,
        descansos: DescansoRepository,
        private val iniciarPausa: IniciarPausa,
        private val terminarPausa: TerminarPausa,
        private val clock: Clock,
    ) : ViewModel() {
        val uiState: StateFlow<HomeUiState> =
            combine(
                obtenerJornadaActual(),
                settings.observar(),
                descansos.observarEnCurso(),
                cadaMinuto(),
            ) { jornada, ajustes, pausa, ahora ->
                crearEstado(jornada, ajustes.avisos, pausa, ahora)
            }.stateIn(
                scope = viewModelScope,
                // Deja de contar 5 s después de salir de la pantalla (ahorra batería).
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = crearEstado(null, emptyList(), null, clock.instant()),
            )

        /** Botón «Iniciar pausa». Si no se puede (ya hay una), no pasa nada: la pantalla ya lo refleja. */
        fun onIniciarPausa() {
            viewModelScope.launch { iniciarPausa() }
        }

        fun onTerminarPausa() {
            viewModelScope.launch { terminarPausa() }
        }

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
            avisos: List<AvisoConfig>,
            pausa: Descanso?,
            ahora: Instant,
        ): HomeUiState =
            HomeUiState(
                hoy = ahora.atZone(clock.zone).toLocalDate(),
                jornada = jornada?.let { enCurso(it, avisos, ahora) },
                pausa =
                    pausa?.let {
                        PausaUi(
                            desde = it.inicio.atZone(it.zonaInicio),
                            transcurrido = Duration.between(it.inicio, ahora).coerceAtLeast(Duration.ZERO),
                        )
                    },
            )

        private fun enCurso(
            jornada: Jornada,
            avisos: List<AvisoConfig>,
            ahora: Instant,
        ): JornadaEnCursoUi {
            val proximo = calcularAvisos(jornada, avisos, ahora).firstOrNull()
            return JornadaEnCursoUi(
                inicio = jornada.inicio.atZone(jornada.zonaInicio),
                transcurrido = Duration.between(jornada.inicio, ahora).coerceAtLeast(Duration.ZERO),
                kmInicio = jornada.kmInicio,
                proximoAviso = proximo?.let { AvisoUi(it.disparo.atZone(clock.zone), it.desdeInicio) },
            )
        }
    }
