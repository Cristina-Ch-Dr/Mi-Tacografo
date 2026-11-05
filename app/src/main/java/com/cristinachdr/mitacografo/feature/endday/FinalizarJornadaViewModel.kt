package com.cristinachdr.mitacografo.feature.endday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristinachdr.mitacografo.domain.model.ErrorJornada
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.PreferenciasPaises
import com.cristinachdr.mitacografo.domain.model.ResultadoJornada
import com.cristinachdr.mitacografo.domain.model.validarJornada
import com.cristinachdr.mitacografo.domain.usecase.DatosFin
import com.cristinachdr.mitacografo.domain.usecase.FinalizarJornada
import com.cristinachdr.mitacografo.domain.usecase.ObtenerJornadaActual
import com.cristinachdr.mitacografo.domain.usecase.ObtenerPreferenciasPaises
import com.cristinachdr.mitacografo.ui.ErrorCampo
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
            jornada != null && !guardando && errorKm == null && errorMinutos == null && errores.isEmpty()
}

@HiltViewModel
class FinalizarJornadaViewModel
    @Inject
    constructor(
        private val finalizarJornada: FinalizarJornada,
        private val obtenerJornadaActual: ObtenerJornadaActual,
        private val obtenerPreferenciasPaises: ObtenerPreferenciasPaises,
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
                actualizar {
                    copy(
                        jornada = abierta,
                        // Normalmente se termina en el mismo país en el que se empezó.
                        pais = abierta?.paisInicio.orEmpty(),
                        paises = paises,
                        observaciones = abierta?.observaciones.orEmpty(),
                        cargando = false,
                    )
                }
            }
        }

        fun onFechaHora(valor: LocalDateTime) = actualizar { copy(fechaHora = valor) }

        fun onKm(valor: String) = actualizar { copy(km = valor) }

        fun onConduccionHoras(valor: String) = actualizar { copy(conduccionHoras = valor.take(2)) }

        fun onConduccionMinutos(valor: String) = actualizar { copy(conduccionMinutos = valor.take(2)) }

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
                        observaciones = estado.observaciones,
                    )
                when (val resultado = finalizarJornada(datos)) {
                    is ResultadoJornada.Ok -> {
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
            val minutos = conduccionMinutos.toIntOrNull()
            val errorMinutos = if (minutos != null && minutos > 59) ErrorCampo.MINUTOS_INVALIDOS else null

            val abierta = jornada ?: return copy(errorKm = errorKm, errorMinutos = errorMinutos)
            val fin = fechaHora.atZone(zona).toInstant()
            val borrador =
                abierta.copy(
                    fin = fin,
                    zonaFin = zona,
                    kmFin = kmNumero,
                    conduccionTacografo = if (errorMinutos == null) conduccion() else null,
                )
            return copy(
                errorKm = errorKm,
                errorMinutos = errorMinutos,
                errores = validarJornada(borrador, clock.instant()),
                kmRecorridos = borrador.kmRecorridos,
                duracion = if (fin.isAfter(abierta.inicio)) Duration.between(abierta.inicio, fin) else null,
            )
        }
    }

/** Conducción escrita en horas y minutos. Si las dos casillas están vacías, `null` (no apuntada). */
internal fun FinalizarJornadaUiState.conduccion(): Duration? {
    if (conduccionHoras.isBlank() && conduccionMinutos.isBlank()) return null
    val horas = conduccionHoras.toLongOrNull() ?: 0
    val minutos = conduccionMinutos.toLongOrNull() ?: 0
    return Duration.ofHours(horas).plusMinutes(minutos)
}
