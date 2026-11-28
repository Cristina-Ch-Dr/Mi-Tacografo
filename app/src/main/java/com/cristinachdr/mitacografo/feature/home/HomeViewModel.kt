package com.cristinachdr.mitacografo.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristinachdr.mitacografo.domain.model.ANTELACION_DESPERTADOR_MAXIMA
import com.cristinachdr.mitacografo.domain.model.ANTELACION_DESPERTADOR_POR_DEFECTO
import com.cristinachdr.mitacografo.domain.model.AvisoConfig
import com.cristinachdr.mitacografo.domain.model.Descanso
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.ResumenViaje
import com.cristinachdr.mitacografo.domain.model.calcularAvisos
import com.cristinachdr.mitacografo.domain.model.cuandoSalir
import com.cristinachdr.mitacografo.domain.model.descansoActual
import com.cristinachdr.mitacografo.domain.model.plazoSiguienteSemanal
import com.cristinachdr.mitacografo.domain.model.resumirViajes
import com.cristinachdr.mitacografo.domain.port.DescansoRepository
import com.cristinachdr.mitacografo.domain.port.JornadaRepository
import com.cristinachdr.mitacografo.domain.port.SettingsRepository
import com.cristinachdr.mitacografo.domain.port.ViajeRepository
import com.cristinachdr.mitacografo.domain.rules.Deuda
import com.cristinachdr.mitacografo.domain.rules.EstadoDeuda
import com.cristinachdr.mitacografo.domain.rules.Intervalo
import com.cristinachdr.mitacografo.domain.rules.ReglaId
import com.cristinachdr.mitacografo.domain.usecase.IniciarPausa
import com.cristinachdr.mitacografo.domain.usecase.ObtenerDescansos
import com.cristinachdr.mitacografo.domain.usecase.ObtenerInformeReglas
import com.cristinachdr.mitacografo.domain.usecase.ObtenerJornadaActual
import com.cristinachdr.mitacografo.domain.usecase.PonerDespertador
import com.cristinachdr.mitacografo.domain.usecase.QuitarDespertador
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
    /** La hora de este estado: cambia cada minuto (para los avisos de arriba y la hora de los formularios). */
    val ahora: Instant = Instant.EPOCH,
    /** Km de las jornadas terminadas que empezaron hoy (MT-56). */
    val kmHoy: Int = 0,
    /** El viaje en curso con sus km, si hay uno (MT-56). */
    val viajeEnCurso: ResumenViaje? = null,
    /** «Cuándo salir» (MT-57): con 9 h y 11 h de descanso. Vacío si hay jornada o ya pasaron las dos salidas. */
    val salidas: List<SalidaUi> = emptyList(),
    /** Cuánto antes de salir suena el despertador. */
    val antelacion: Duration = ANTELACION_DESPERTADOR_POR_DEFECTO,
    /** A qué hora está puesto el despertador (en la zona del móvil); `null` = no hay. */
    val despertador: ZonedDateTime? = null,
)

/** Una opción de «Cuándo salir», con las horas en la zona del móvil (la del reloj del conductor). */
data class SalidaUi(
    val descanso: Duration,
    val salida: ZonedDateTime,
    val despertar: ZonedDateTime,
    /** La hora de despertar ya pasó: no se puede poner el despertador. */
    val pasada: Boolean,
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
    /** Cuándo apuntó la conducción «hasta ahora» (MT-53), en la zona del móvil; `null` si no lo ha hecho. */
    val conduccionApuntadaEn: ZonedDateTime? = null,
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
    /** La conducción de la semana anterior, para las 90 h de dos semanas (cero si no hay jornadas). */
    val conduccionAnterior: Intervalo = Intervalo.exacto(Duration.ZERO),
    /** Jornadas de más de 9 h (como mucho 2). */
    val ampliadas: Int,
    /** Diarios reducidos desde el último semanal (como mucho 3); `null` si no se sabe. */
    val reducidos: Int?,
    /** La deuda pendiente que vence antes, si hay. */
    val deuda: Deuda?,
    /** El peor estado de las reglas de la semana y de sus jornadas; `null` si aún no hay resultados. */
    val estado: EstadoRegla?,
    /** Hasta cuándo puede empezar el siguiente semanal (en la zona del móvil); `null` si no se sabe. */
    val plazoSemanal: ZonedDateTime? = null,
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
        descansos: DescansoRepository,
        viajes: ViajeRepository,
        jornadas: JornadaRepository,
        private val iniciarPausa: IniciarPausa,
        private val terminarPausa: TerminarPausa,
        private val settings: SettingsRepository,
        private val ponerDespertador: PonerDespertador,
        private val quitarDespertador: QuitarDespertador,
        obtenerInformeReglas: ObtenerInformeReglas,
        obtenerDescansos: ObtenerDescansos,
        private val clock: Clock,
    ) : ViewModel() {
        /**
         * «Lo que te queda» (MT-51): lo que antes sumaba a mano en la libreta. `null` si aún no hay ni jornadas
         * en esta semana o la anterior ni ningún semanal.
         */
        val semana: StateFlow<ResumenSemanaUi?> =
            combine(obtenerInformeReglas(), obtenerDescansos()) { informe, clasificados ->
                val lunes = LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val esta = informe.semanas.find { it.lunes == lunes }
                val anterior = informe.semanas.find { it.lunes == lunes.minusWeeks(1) }
                val plazo = plazoSiguienteSemanal(clasificados)
                if (esta == null && anterior == null && plazo == null) return@combine null
                val cero = Intervalo.exacto(Duration.ZERO)
                ResumenSemanaUi(
                    lunes = lunes,
                    conduccion = esta?.conduccion ?: cero,
                    conduccionAnterior = anterior?.conduccion ?: cero,
                    ampliadas =
                        esta
                            ?.resultados
                            ?.find { it.regla == ReglaId.AMPLIACIONES_SEMANA }
                            ?.detalle
                            ?.cuenta ?: 0,
                    reducidos =
                        esta
                            ?.resultados
                            ?.find { it.regla == ReglaId.REDUCIDOS_ENTRE_SEMANALES }
                            ?.detalle
                            ?.cuenta,
                    deuda =
                        informe.deudas
                            .filter { it.estado == EstadoDeuda.PENDIENTE }
                            .minByOrNull { it.fechaLimite },
                    estado = esta?.let { s -> (s.resultados + s.jornadas.flatMap { it.resultados }).peor() },
                    plazoSemanal = plazo?.atZone(clock.zone),
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        val uiState: StateFlow<HomeUiState> =
            combine(
                combine(obtenerJornadaActual(), jornadas.observarTodas()) { actual, todas ->
                    Triple(actual, descansoActual(todas)?.let { fin -> fin to todas.maxBy { it.inicio } }, todas)
                },
                settings.observar(),
                descansos.observarEnCurso(),
                viajes.observarTodos(),
                cadaMinuto(),
            ) { (jornada, ultima, todas), ajustes, pausa, todosLosViajes, ahora ->
                val viaje = jornada?.viajeId?.let { id -> todosLosViajes.find { it.id == id }?.nombre }
                crearEstado(jornada, ajustes.avisos, pausa, ahora, viaje)
                    .copy(
                        kmHoy = kmDeHoy(todas, ahora),
                        viajeEnCurso =
                            todosLosViajes
                                .find { it.enCurso }
                                ?.let { enCurso -> resumirViajes(listOf(enCurso), todas).single() },
                        salidas =
                            if (jornada ==
                                null
                            ) {
                                salidas(ultima?.first, ajustes.antelacionDespertador, ahora)
                            } else {
                                emptyList()
                            },
                        antelacion = ajustes.antelacionDespertador,
                        despertador = ajustes.despertador?.atZone(clock.zone),
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

        /** ⏰ de «Cuándo salir»: pone el despertador a esa hora. */
        fun onPonerDespertador(hora: ZonedDateTime) {
            viewModelScope.launch { ponerDespertador(hora.toInstant()) }
        }

        fun onQuitarDespertador() {
            viewModelScope.launch { quitarDespertador() }
        }

        /** Cuánto antes despertar (30, 45, 60, 90, 120 min o la que escriba). */
        fun onAntelacion(antelacion: Duration) {
            if (antelacion.isNegative || antelacion > ANTELACION_DESPERTADOR_MAXIMA) return
            viewModelScope.launch { settings.actualizar { it.copy(antelacionDespertador = antelacion) } }
        }

        /** Las salidas posibles mientras quede alguna por llegar (MT-57). */
        private fun salidas(
            fin: Instant?,
            antelacion: Duration,
            ahora: Instant,
        ): List<SalidaUi> {
            fin ?: return emptyList()
            val posibles = cuandoSalir(fin, antelacion)
            if (posibles.none { it.salida.isAfter(ahora) }) return emptyList()
            return posibles.map {
                SalidaUi(
                    descanso = it.descanso,
                    salida = it.salida.atZone(clock.zone),
                    despertar = it.despertar.atZone(clock.zone),
                    pasada = !it.despertar.isAfter(ahora),
                )
            }
        }

        /** Km de las jornadas terminadas que empezaron hoy (en la zona del móvil), como en su app. */
        private fun kmDeHoy(
            todas: List<Jornada>,
            ahora: Instant,
        ): Int {
            val hoy = ahora.atZone(clock.zone).toLocalDate()
            return todas.filter { it.inicio.atZone(clock.zone).toLocalDate() == hoy }.sumOf { it.kmRecorridos ?: 0 }
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
                ahora = ahora,
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
                conduccionApuntadaEn = jornada.conduccionApuntadaEn?.atZone(clock.zone),
            )
        }
    }
