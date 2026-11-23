package com.cristinachdr.mitacografo.feature.endday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristinachdr.mitacografo.domain.model.ErrorJornada
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.PreferenciasPaises
import com.cristinachdr.mitacografo.domain.model.ResultadoJornada
import com.cristinachdr.mitacografo.domain.model.cruceDeSemana
import com.cristinachdr.mitacografo.domain.model.validarJornada
import com.cristinachdr.mitacografo.domain.port.SettingsRepository
import com.cristinachdr.mitacografo.domain.usecase.ApuntarConduccion
import com.cristinachdr.mitacografo.domain.usecase.DatosFin
import com.cristinachdr.mitacografo.domain.usecase.FinalizarJornada
import com.cristinachdr.mitacografo.domain.usecase.ObtenerJornadaActual
import com.cristinachdr.mitacografo.domain.usecase.ObtenerPreferenciasPaises
import com.cristinachdr.mitacografo.domain.usecase.TerminarPausa
import com.cristinachdr.mitacografo.ui.ErrorCampo
import com.cristinachdr.mitacografo.ui.casillasConduccion
import com.cristinachdr.mitacografo.ui.errorMinutos
import com.cristinachdr.mitacografo.ui.leerConduccion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class FinalizarJornadaUiState(
    val fechaHora: LocalDateTime,
    val zona: ZoneId,
    /** La jornada que se va a cerrar. `null` mientras carga o si no hay ninguna abierta. */
    val jornada: Jornada? = null,
    val km: String = "",
    val conduccionHoras: String = "",
    val conduccionMinutos: String = "",
    val pais: String = "",
    /** Qué países salen primero en el selector (recientes y lo elegido en Ajustes). */
    val paises: PreferenciasPaises = PreferenciasPaises(),
    val ciudad: String = "",
    val observaciones: String = "",
    val errorKm: ErrorCampo? = ErrorCampo.OBLIGATORIO,
    val errorMinutos: ErrorCampo? = null,
    /** Cuándo se apuntó la conducción «hasta ahora» (MT-53), en la zona del móvil; `null` si no se ha apuntado. */
    val conduccionApuntadaEn: ZonedDateTime? = null,
    val apuntando: Boolean = false,
    /** La zona de Ajustes: en ella se mira la medianoche del domingo, como el motor de reglas. */
    val zonaReferencia: ZoneId? = null,
    /** El lunes a las 00:00 que cruza la jornada (MT-50); `null` si no cruza ninguno. */
    val medianoche: ZonedDateTime? = null,
    val antesHoras: String = "",
    val antesMinutos: String = "",
    val errorMinutosAntes: ErrorCampo? = null,
    val errores: Set<ErrorJornada> = emptySet(),
    /** Se calculan en vivo mientras el conductor escribe. */
    val kmRecorridos: Int? = null,
    val duracion: Duration? = null,
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val guardado: Boolean = false,
) {
    val sinJornada: Boolean get() = !cargando && jornada == null

    val puedeGuardar: Boolean
        get() =
            jornada != null &&
                !guardando &&
                errorKm == null &&
                errorMinutos == null &&
                errorMinutosAntes == null &&
                errores.isEmpty()

    /** Se puede apuntar la conducción «hasta ahora» si hay algo escrito y bien. */
    val puedeApuntar: Boolean
        get() = jornada != null && !apuntando && !guardando && errorMinutos == null && conduccion() != null
}

@HiltViewModel
class FinalizarJornadaViewModel
    @Inject
    constructor(
        private val finalizarJornada: FinalizarJornada,
        private val obtenerJornadaActual: ObtenerJornadaActual,
        private val obtenerPreferenciasPaises: ObtenerPreferenciasPaises,
        private val terminarPausa: TerminarPausa,
        private val apuntarConduccion: ApuntarConduccion,
        private val settings: SettingsRepository,
        private val clock: Clock,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow(
                FinalizarJornadaUiState(
                    fechaHora = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES),
                    zona = clock.zone,
                ),
            )
        val uiState: StateFlow<FinalizarJornadaUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                val abierta = obtenerJornadaActual().first()
                val paises = obtenerPreferenciasPaises()
                val zonaReferencia = settings.observar().first().zonaReferencia
                // La conducción apuntada durante la jornada (MT-53) sale ya escrita.
                val (horas, minutos) = casillasConduccion(abierta?.conduccionTacografo)
                actualizar {
                    copy(
                        jornada = abierta,
                        zonaReferencia = zonaReferencia,
                        conduccionHoras = horas,
                        conduccionMinutos = minutos,
                        conduccionApuntadaEn = abierta?.conduccionApuntadaEn?.atZone(clock.zone),
                        // Normalmente se termina en el mismo país en el que se empezó.
                        pais = abierta?.paisInicio.orEmpty(),
                        paises = paises,
                        observaciones = abierta?.observaciones.orEmpty(),
                        cargando = false,
                    )
                }
            }
        }

        /** `true` cuando el conductor cambia la hora a mano: desde entonces ya no se mueve sola. */
        private var horaElegida = false

        fun onFechaHora(valor: LocalDateTime) {
            horaElegida = true
            actualizar { copy(fechaHora = valor) }
        }

        /**
         * En Inicio el formulario está siempre a la vista (MT-51), así que la pantalla avisa cada minuto:
         * mientras el conductor no haya cambiado la hora, se queda en la de ahora.
         */
        fun onPasaElTiempo() {
            if (horaElegida) return
            val ahora = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES)
            if (ahora != _uiState.value.fechaHora) actualizar { copy(fechaHora = ahora) }
        }

        fun onKm(valor: String) = actualizar { copy(km = valor) }

        fun onConduccionHoras(valor: String) = actualizar { copy(conduccionHoras = valor.take(2)) }

        fun onConduccionMinutos(valor: String) = actualizar { copy(conduccionMinutos = valor.take(2)) }

        fun onAntesHoras(valor: String) = actualizar { copy(antesHoras = valor.take(2)) }

        fun onAntesMinutos(valor: String) = actualizar { copy(antesMinutos = valor.take(2)) }

        /** «Apuntar la conducción de ahora» (MT-53): la guarda en la jornada en curso sin finalizarla. */
        fun onApuntarConduccion() {
            val estado = _uiState.value
            val conduccion = estado.conduccion()
            if (!estado.puedeApuntar || conduccion == null) return
            _uiState.update { it.copy(apuntando = true) }
            viewModelScope.launch {
                when (val resultado = apuntarConduccion(conduccion)) {
                    is ResultadoJornada.Ok -> {
                        actualizar {
                            copy(
                                apuntando = false,
                                jornada = resultado.valor,
                                conduccionApuntadaEn = resultado.valor.conduccionApuntadaEn?.atZone(clock.zone),
                            )
                        }
                    }

                    is ResultadoJornada.Invalida -> {
                        _uiState.update { it.copy(apuntando = false, errores = resultado.errores) }
                    }
                }
            }
        }

        fun onPais(valor: String) = actualizar { copy(pais = valor) }

        fun onCiudad(valor: String) = actualizar { copy(ciudad = valor) }

        fun onObservaciones(valor: String) = actualizar { copy(observaciones = valor) }

        fun onFinalizar() {
            val estado = _uiState.value
            val km = estado.km.toIntOrNull()
            if (!estado.puedeGuardar || km == null) return

            _uiState.update { it.copy(guardando = true) }
            viewModelScope.launch {
                val datos =
                    DatosFin(
                        fin = estado.fechaHora.atZone(estado.zona).toInstant(),
                        zona = estado.zona,
                        kmFin = km,
                        pais = estado.pais,
                        ciudad = estado.ciudad,
                        conduccionTacografo = estado.conduccion(),
                        conduccionAntesMedianoche = estado.antesMedianoche(),
                        observaciones = estado.observaciones,
                    )
                when (val resultado = finalizarJornada(datos)) {
                    is ResultadoJornada.Ok -> {
                        // Si se quedó una pausa abierta, termina con la jornada (si no había, no hace nada).
                        terminarPausa(hasta = datos.fin, zona = datos.zona)
                        _uiState.update { it.copy(guardado = true) }
                    }

                    is ResultadoJornada.Invalida -> {
                        _uiState.update { it.copy(guardando = false, errores = resultado.errores) }
                    }
                }
            }
        }

        private fun actualizar(cambio: FinalizarJornadaUiState.() -> FinalizarJornadaUiState) {
            _uiState.update { it.cambio().validado() }
        }

        /** Valida los campos y recalcula km recorridos y duración con lo que hay escrito. */
        private fun FinalizarJornadaUiState.validado(): FinalizarJornadaUiState {
            val kmNumero = km.toIntOrNull()
            val errorKm =
                when {
                    km.isBlank() -> ErrorCampo.OBLIGATORIO
                    kmNumero == null -> ErrorCampo.NO_ES_NUMERO
                    else -> null
                }
            val errorMinutos = errorMinutos(conduccionMinutos)
            val errorMinutosAntes = errorMinutos(antesMinutos)

            val abierta =
                jornada ?: return copy(
                    errorKm = errorKm,
                    errorMinutos = errorMinutos,
                    errorMinutosAntes = errorMinutosAntes,
                )
            val fin = fechaHora.atZone(zona).toInstant()
            val sinAntes =
                abierta.copy(
                    fin = fin,
                    zonaFin = zona,
                    kmFin = kmNumero,
                    conduccionTacografo = if (errorMinutos == null) conduccion() else null,
                    conduccionApuntadaEn = null,
                )
            val medianoche = sinAntes.cruceDeSemana(zonaReferencia)
            val conMedianoche = copy(medianoche = medianoche?.atZone(zonaReferencia ?: abierta.zonaInicio))
            val borrador =
                sinAntes.copy(
                    conduccionAntesMedianoche =
                        if (errorMinutosAntes == null) conMedianoche.antesMedianoche() else null,
                )
            return conMedianoche.copy(
                errorKm = errorKm,
                errorMinutos = errorMinutos,
                errorMinutosAntes = errorMinutosAntes.takeIf { medianoche != null },
                errores = validarJornada(borrador, clock.instant(), zonaReferencia = zonaReferencia),
                kmRecorridos = borrador.kmRecorridos,
                duracion = if (fin.isAfter(abierta.inicio)) Duration.between(abierta.inicio, fin) else null,
            )
        }
    }

/** Conducción escrita en horas y minutos. Si las dos casillas están vacías, `null` (no apuntada). */
internal fun FinalizarJornadaUiState.conduccion(): Duration? = leerConduccion(conduccionHoras, conduccionMinutos)

/** La de antes de medianoche del domingo, solo si la jornada la cruza. */
internal fun FinalizarJornadaUiState.antesMedianoche(): Duration? =
    if (medianoche == null) null else leerConduccion(antesHoras, antesMinutos)
