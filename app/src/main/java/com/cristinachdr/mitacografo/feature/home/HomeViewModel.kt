package com.cristinachdr.mitacografo.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristinachdr.mitacografo.domain.model.AvisoConfig
import com.cristinachdr.mitacografo.domain.model.Descanso
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.calcularAvisos
import com.cristinachdr.mitacografo.domain.model.descansoActual
import com.cristinachdr.mitacografo.domain.port.DescansoRepository
import com.cristinachdr.mitacografo.domain.port.JornadaRepository
import com.cristinachdr.mitacografo.domain.port.SettingsRepository
import com.cristinachdr.mitacografo.domain.port.ViajeRepository
import com.cristinachdr.mitacografo.domain.rules.Deuda
import com.cristinachdr.mitacografo.domain.rules.EstadoDeuda
import com.cristinachdr.mitacografo.domain.rules.Intervalo
import com.cristinachdr.mitacografo.domain.rules.ReglaId
import com.cristinachdr.mitacografo.domain.usecase.IniciarPausa
import com.cristinachdr.mitacografo.domain.usecase.ObtenerInformeReglas
import com.cristinachdr.mitacografo.domain.usecase.ObtenerJornadaActual
import com.cristinachdr.mitacografo.domain.usecase.TerminarPausa
import com.cristinachdr.mitacografo.ui.reglas.EstadoRegla
import com.cristinachdr.mitacografo.ui.reglas.peor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

/** Estado de la pantalla de Inicio. */
data class HomeUiState(
    val hoy: LocalDate,
    /** `null` si no hay jornada en curso. */
    val jornada: JornadaEnCursoUi? = null,
    /** `null` si no hay pausa en curso. */
    val pausa: PausaUi? = null,
    /** Faltan el país de la empresa o el de residencia en Ajustes: la regla R-DS-3 no se puede comprobar. */
    val faltanPaises: Boolean = false,
    /** Sin jornada en curso: desde cuándo descansa (el fin de la última jornada) y cuánto lleva. */
    val descanso: PausaUi? = null,
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
    /** Nombre del viaje en el que se apunta, o `null` si no va en ninguno. */
    val viaje: String? = null,
)

data class AvisoUi(
    /** Hora del aviso en la zona del móvil (la que ve el conductor en su reloj). */
    val hora: ZonedDateTime,
    val desdeInicio: Duration,
)

/**
 * Lo que va en «Esta semana» de Inicio: lo que antes sumaba a mano en la libreta (S, ampliaciones…)
 * y el estado de las reglas.
 */
data class ResumenSemanaUi(
    val lunes: LocalDate,
    val conduccion: Intervalo,
    /** Jornadas de más de 9 h (como mucho 2). */
    val ampliadas: Int,
    /** Diarios reducidos desde el último semanal (como mucho 3); `null` si no se sabe. */
    val reducidos: Int?,
    /** La deuda pendiente que vence antes, si hay. */
    val deuda: Deuda?,
    /** El peor estado de las reglas de la semana y de sus jornadas; `null` si aún no hay resultados. */
    val estado: EstadoRegla?,
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
        viajes: ViajeRepository,
        jornadas: JornadaRepository,
        private val iniciarPausa: IniciarPausa,
        private val terminarPausa: TerminarPausa,
        obtenerInformeReglas: ObtenerInformeReglas,
        private val clock: Clock,
    ) : ViewModel() {
        /** «Esta semana»: `null` si en la semana de hoy aún no hay jornadas. */
        val semana: StateFlow<ResumenSemanaUi?> =
            obtenerInformeReglas()
                .map { informe ->
                    val lunes = LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    informe.semanas.find { it.lunes == lunes }?.let { s ->
                        ResumenSemanaUi(
                            lunes = lunes,
                            conduccion = s.conduccion,
                            ampliadas =
                                s.resultados
                                    .find { it.regla == ReglaId.AMPLIACIONES_SEMANA }
                                    ?.detalle
                                    ?.cuenta ?: 0,
                            reducidos =
                                s.resultados
                                    .find { it.regla == ReglaId.REDUCIDOS_ENTRE_SEMANALES }
                                    ?.detalle
                                    ?.cuenta,
                            deuda =
                                informe.deudas
                                    .filter {
                                        it.estado == EstadoDeuda.PENDIENTE
                                    }.minByOrNull { it.fechaLimite },
                            estado = (s.resultados + s.jornadas.flatMap { it.resultados }).peor(),
                        )
                    }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        val uiState: StateFlow<HomeUiState> =
            combine(
                combine(obtenerJornadaActual(), jornadas.observarTodas()) { actual, todas ->
                    actual to descansoActual(todas)?.let { fin -> fin to todas.maxBy { it.inicio } }
                },
                settings.observar(),
                descansos.observarEnCurso(),
                viajes.observarTodos(),
                cadaMinuto(),
            ) { (jornada, ultima), ajustes, pausa, todosLosViajes, ahora ->
                val viaje = jornada?.viajeId?.let { id -> todosLosViajes.find { it.id == id }?.nombre }
                crearEstado(jornada, ajustes.avisos, pausa, ahora, viaje)
                    .copy(
                        faltanPaises = ajustes.paisEmpresa == null || ajustes.paisResidencia == null,
                        descanso =
                            ultima?.let { (fin, j) ->
                                PausaUi(
                                    desde = fin.atZone(j.zonaFin ?: j.zonaInicio),
                                    transcurrido = Duration.between(fin, ahora).coerceAtLeast(Duration.ZERO),
                                )
                            },
                    )
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
            viaje: String? = null,
        ): HomeUiState =
            HomeUiState(
                hoy = ahora.atZone(clock.zone).toLocalDate(),
                jornada = jornada?.let { enCurso(it, avisos, ahora).copy(viaje = viaje) },
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
