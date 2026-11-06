package com.cristinachdr.mitacografo.feature.startday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristinachdr.mitacografo.domain.model.Descanso
import com.cristinachdr.mitacografo.domain.model.ErrorJornada
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.PreferenciasPaises
import com.cristinachdr.mitacografo.domain.model.ResultadoJornada
import com.cristinachdr.mitacografo.domain.model.TipoDescanso
import com.cristinachdr.mitacografo.domain.model.validarJornada
import com.cristinachdr.mitacografo.domain.usecase.DatosInicio
import com.cristinachdr.mitacografo.domain.usecase.GuardarDescanso
import com.cristinachdr.mitacografo.domain.usecase.IniciarJornada
import com.cristinachdr.mitacografo.domain.usecase.ObtenerPreferenciasPaises
import com.cristinachdr.mitacografo.domain.usecase.ProponerDatosInicio
import com.cristinachdr.mitacografo.domain.usecase.ProponerDescansoPrevio
import com.cristinachdr.mitacografo.domain.usecase.PropuestaDescanso
import com.cristinachdr.mitacografo.ui.ErrorCampo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.Instant
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
    /** «¿Descansaste desde…?»: desde el fin de la jornada anterior, si no hay descanso apuntado. */
    val propuestaDescanso: PropuestaDescanso? = null,
    /** El conductor ha dicho que sí: al iniciar, se apunta el descanso hasta la hora de inicio. */
    val apuntarDescanso: Boolean = false,
    val errorKm: ErrorCampo? = ErrorCampo.OBLIGATORIO,
    val errores: Set<ErrorJornada> = emptySet(),
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    /** Cuando pasa a `true`, la pantalla se cierra. */
    val guardado: Boolean = false,
) {
    val puedeGuardar: Boolean get() = !cargando && !guardando && errorKm == null && errores.isEmpty()

    /** Lo que duraría el descanso propuesto, o `null` si la hora de inicio no es posterior. */
    val duracionDescanso: Duration?
        get() {
            val propuesta = propuestaDescanso ?: return null
            val hasta = fechaHora.atZone(zona).toInstant()
            return Duration.between(propuesta.desde, hasta).takeIf { !it.isNegative && !it.isZero }
        }
}

@HiltViewModel
class IniciarJornadaViewModel
    @Inject
    constructor(
        private val iniciarJornada: IniciarJornada,
        private val proponerDatosInicio: ProponerDatosInicio,
        private val obtenerPreferenciasPaises: ObtenerPreferenciasPaises,
        private val proponerDescansoPrevio: ProponerDescansoPrevio,
        private val guardarDescanso: GuardarDescanso,
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
                val descanso = proponerDescansoPrevio()
                actualizar {
                    copy(
                        km = propuesta.kmInicio?.toString().orEmpty(),
                        pais = propuesta.pais.orEmpty(),
                        paises = paises,
                        ciudad = propuesta.ciudad.orEmpty(),
                        propuestaDescanso = descanso,
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

        fun onApuntarDescanso(valor: Boolean) = actualizar { copy(apuntarDescanso = valor) }

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
                        estado.descansoAApuntar(datos.inicio)?.let { guardarDescanso(it) }
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

/**
 * El descanso que se apunta al iniciar, si el conductor dijo que sí: desde el fin de la jornada
 * anterior hasta [inicio], en el país donde terminó. Solo se crea si lo confirma (espec. §8.5).
 */
internal fun IniciarJornadaUiState.descansoAApuntar(inicio: Instant): Descanso? {
    val propuesta = propuestaDescanso ?: return null
    if (!apuntarDescanso || !inicio.isAfter(propuesta.desde)) return null
    return Descanso(
        inicio = propuesta.desde,
        zonaInicio = propuesta.zona,
        fin = inicio,
        zonaFin = zona,
        tipo = TipoDescanso.DESCANSO,
        pais = propuesta.pais,
    )
}
