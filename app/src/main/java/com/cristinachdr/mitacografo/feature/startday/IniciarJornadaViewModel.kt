package com.cristinachdr.mitacografo.feature.startday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristinachdr.mitacografo.domain.model.ErrorJornada
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.PreferenciasPaises
import com.cristinachdr.mitacografo.domain.model.ResultadoJornada
import com.cristinachdr.mitacografo.domain.model.validarJornada
import com.cristinachdr.mitacografo.domain.usecase.DatosInicio
import com.cristinachdr.mitacografo.domain.usecase.IniciarJornada
import com.cristinachdr.mitacografo.domain.usecase.ObtenerPreferenciasPaises
import com.cristinachdr.mitacografo.domain.usecase.ProponerDatosInicio
import com.cristinachdr.mitacografo.ui.ErrorCampo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class IniciarJornadaUiState(
    val fechaHora: LocalDateTime,
    val zona: ZoneId,
    val km: String = "",
    val pais: String = "",
    /** Qué países salen primero en el selector (recientes y lo elegido en Ajustes). */
    val paises: PreferenciasPaises = PreferenciasPaises(),
    val ciudad: String = "",
    val observaciones: String = "",
    val errorKm: ErrorCampo? = ErrorCampo.OBLIGATORIO,
    val errores: Set<ErrorJornada> = emptySet(),
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    /** Cuando pasa a `true`, la pantalla se cierra. */
    val guardado: Boolean = false,
) {
    val puedeGuardar: Boolean get() = !cargando && !guardando && errorKm == null && errores.isEmpty()
}

@HiltViewModel
class IniciarJornadaViewModel
    @Inject
    constructor(
        private val iniciarJornada: IniciarJornada,
        private val proponerDatosInicio: ProponerDatosInicio,
        private val obtenerPreferenciasPaises: ObtenerPreferenciasPaises,
        private val clock: Clock,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow(
                IniciarJornadaUiState(
                    fechaHora = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES),
                    zona = clock.zone,
                ),
            )
        val uiState: StateFlow<IniciarJornadaUiState> = _uiState.asStateFlow()

        init {
            // Se rellena con lo que dejó la jornada anterior: normalmente basta con pulsar "Iniciar".
            viewModelScope.launch {
                val propuesta = proponerDatosInicio()
                val paises = obtenerPreferenciasPaises()
                actualizar {
                    copy(
                        km = propuesta.kmInicio?.toString().orEmpty(),
                        pais = propuesta.pais.orEmpty(),
                        paises = paises,
                        ciudad = propuesta.ciudad.orEmpty(),
                        cargando = false,
                    )
                }
            }
        }

        fun onFechaHora(valor: LocalDateTime) = actualizar { copy(fechaHora = valor) }

        fun onKm(valor: String) = actualizar { copy(km = valor) }

        fun onPais(valor: String) = actualizar { copy(pais = valor) }

        fun onCiudad(valor: String) = actualizar { copy(ciudad = valor) }

        fun onObservaciones(valor: String) = actualizar { copy(observaciones = valor) }

        fun onIniciar() {
            val estado = _uiState.value
            val km = estado.km.toIntOrNull()
            if (!estado.puedeGuardar || km == null) return

            _uiState.update { it.copy(guardando = true) }
            viewModelScope.launch {
                val datos =
                    DatosInicio(
                        inicio = estado.fechaHora.atZone(estado.zona).toInstant(),
                        zona = estado.zona,
                        kmInicio = km,
                        pais = estado.pais,
                        ciudad = estado.ciudad,
                        observaciones = estado.observaciones,
                    )
                when (val resultado = iniciarJornada(datos)) {
                    is ResultadoJornada.Ok -> {
                        _uiState.update { it.copy(guardado = true) }
                    }

                    is ResultadoJornada.Invalida -> {
                        _uiState.update { it.copy(guardando = false, errores = resultado.errores) }
                    }
                }
            }
        }

        /** Aplica un cambio y vuelve a validar al momento (validación "en vivo"). */
        private fun actualizar(cambio: IniciarJornadaUiState.() -> IniciarJornadaUiState) {
            _uiState.update { it.cambio().validado() }
        }

        private fun IniciarJornadaUiState.validado(): IniciarJornadaUiState {
            val kmNumero = km.toIntOrNull()
            val errorKm =
                when {
                    km.isBlank() -> ErrorCampo.OBLIGATORIO
                    kmNumero == null -> ErrorCampo.NO_ES_NUMERO
                    else -> null
                }
            val borrador =
                kmNumero?.let {
                    Jornada(inicio = fechaHora.atZone(zona).toInstant(), zonaInicio = zona, kmInicio = it)
                }
            val errores = borrador?.let { validarJornada(it, clock.instant()) }.orEmpty()
            return copy(errorKm = errorKm, errores = errores)
        }
    }
