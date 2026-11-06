package com.cristinachdr.mitacografo.feature.rests

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristinachdr.mitacografo.domain.model.Descanso
import com.cristinachdr.mitacografo.domain.model.ErrorDescanso
import com.cristinachdr.mitacografo.domain.model.PreferenciasPaises
import com.cristinachdr.mitacografo.domain.model.ResultadoDescanso
import com.cristinachdr.mitacografo.domain.model.TipoDescanso
import com.cristinachdr.mitacografo.domain.model.validarDescanso
import com.cristinachdr.mitacografo.domain.port.DescansoRepository
import com.cristinachdr.mitacografo.domain.port.JornadaRepository
import com.cristinachdr.mitacografo.domain.usecase.BorrarDescanso
import com.cristinachdr.mitacografo.domain.usecase.GuardarDescanso
import com.cristinachdr.mitacografo.domain.usecase.ObtenerPreferenciasPaises
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class EditarDescansoUiState(
    /** El descanso tal y como está guardado; `null` si es uno nuevo (o mientras carga). */
    val original: Descanso? = null,
    val nuevo: Boolean = true,
    val tipo: TipoDescanso = TipoDescanso.DESCANSO,
    val inicio: LocalDateTime? = null,
    val zonaInicio: ZoneId? = null,
    /** `false` = sigue en curso (sin hora de fin). Uno nuevo siempre está terminado. */
    val terminado: Boolean = true,
    val fin: LocalDateTime? = null,
    val zonaFin: ZoneId? = null,
    val pais: String = "",
    val observaciones: String = "",
    val errores: Set<ErrorDescanso> = emptySet(),
    val duracion: Duration? = null,
    val paises: PreferenciasPaises = PreferenciasPaises(),
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val confirmandoBorrado: Boolean = false,
    /** Cuando pasa a `true` (guardado o borrado), la pantalla se cierra. */
    val cerrar: Boolean = false,
) {
    val noExiste: Boolean get() = !cargando && !nuevo && original == null
    val puedeGuardar: Boolean get() = !cargando && !guardando && inicio != null && errores.isEmpty() && !noExiste
}

/**
 * Alta y corrección de una pausa o un descanso (MT-22).
 *
 * Uno nuevo se propone desde que terminó la última jornada hasta ahora, en su país de fin:
 * normalmente solo hay que ajustar las horas. Las horas se editan en la zona donde ocurrieron.
 */
@HiltViewModel
class EditarDescansoViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: DescansoRepository,
        private val jornadas: JornadaRepository,
        private val guardarDescanso: GuardarDescanso,
        private val borrarDescanso: BorrarDescanso,
        private val obtenerPreferenciasPaises: ObtenerPreferenciasPaises,
        private val clock: Clock,
    ) : ViewModel() {
        /** El id viene de la ruta `RutaDescanso(id)`; 0 = nuevo. */
        private val id: Long = savedStateHandle[CLAVE_ID] ?: Descanso.SIN_ID

        private val _uiState = MutableStateFlow(EditarDescansoUiState(nuevo = id == Descanso.SIN_ID))
        val uiState: StateFlow<EditarDescansoUiState> = _uiState.asStateFlow()

        /** Los demás descansos, para avisar de solapes mientras se escribe. */
        private var otros: List<Descanso> = emptyList()

        init {
            viewModelScope.launch {
                val paises = obtenerPreferenciasPaises()
                otros = repository.obtenerTodos().filter { it.id != id }
                val estado = if (id == Descanso.SIN_ID) propuestaNueva() else existente(repository.obtenerPorId(id))
                _uiState.update { estado.copy(paises = paises, cargando = false).validado() }
            }
        }

        fun onTipo(valor: TipoDescanso) = actualizar { copy(tipo = valor) }

        fun onInicio(valor: LocalDateTime) = actualizar { copy(inicio = valor) }

        fun onTerminado(valor: Boolean) =
            actualizar { copy(terminado = valor, fin = fin ?: ahora(), zonaFin = zonaFin ?: clock.zone) }

        fun onFin(valor: LocalDateTime) = actualizar { copy(fin = valor) }

        fun onPais(valor: String) = actualizar { copy(pais = valor) }

        fun onObservaciones(valor: String) = actualizar { copy(observaciones = valor) }

        fun onGuardar() {
            val estado = _uiState.value
            val descanso = estado.borrador() ?: return
            if (!estado.puedeGuardar) return

            _uiState.update { it.copy(guardando = true) }
            viewModelScope.launch {
                when (val resultado = guardarDescanso(descanso)) {
                    is ResultadoDescanso.Ok -> {
                        _uiState.update { it.copy(cerrar = true) }
                    }

                    is ResultadoDescanso.Invalido -> {
                        _uiState.update {
                            it.copy(
                                guardando = false,
                                errores = resultado.errores,
                            )
                        }
                    }
                }
            }
        }

        fun onPedirBorrar() = _uiState.update { it.copy(confirmandoBorrado = true) }

        fun onCancelarBorrar() = _uiState.update { it.copy(confirmandoBorrado = false) }

        fun onConfirmarBorrar() {
            _uiState.update { it.copy(confirmandoBorrado = false, guardando = true) }
            viewModelScope.launch {
                when (val resultado = borrarDescanso(id)) {
                    is ResultadoDescanso.Ok -> {
                        _uiState.update { it.copy(cerrar = true) }
                    }

                    is ResultadoDescanso.Invalido -> {
                        _uiState.update {
                            it.copy(
                                guardando = false,
                                errores = resultado.errores,
                            )
                        }
                    }
                }
            }
        }

        private fun ahora(): LocalDateTime = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES)

        private suspend fun propuestaNueva(): EditarDescansoUiState {
            val ultima = jornadas.obtenerUltimaFinalizada()
            val zona = clock.zone
            val ahora = ahora()
            // Desde el fin de la última jornada, si fue antes de ahora; si no, 9 h hacia atrás.
            val desde =
                ultima
                    ?.fin
                    ?.atZone(zona)
                    ?.toLocalDateTime()
                    ?.truncatedTo(ChronoUnit.MINUTES)
                    ?.takeIf { it < ahora }
                    ?: ahora.minusHours(9)
            return EditarDescansoUiState(
                nuevo = true,
                inicio = desde,
                zonaInicio = zona,
                fin = ahora,
                zonaFin = zona,
                pais = ultima?.paisFin.orEmpty(),
            )
        }

        private fun existente(descanso: Descanso?): EditarDescansoUiState {
            if (descanso == null) return EditarDescansoUiState(nuevo = false)
            val zonaFin = descanso.zonaFin ?: descanso.zonaInicio
            return EditarDescansoUiState(
                original = descanso,
                nuevo = false,
                tipo = descanso.tipo,
                inicio = descanso.inicio.atZone(descanso.zonaInicio).toLocalDateTime(),
                zonaInicio = descanso.zonaInicio,
                terminado = !descanso.enCurso,
                fin = descanso.fin?.atZone(zonaFin)?.toLocalDateTime(),
                zonaFin = zonaFin,
                pais = descanso.pais.orEmpty(),
                observaciones = descanso.observaciones.orEmpty(),
            )
        }

        private fun actualizar(cambio: EditarDescansoUiState.() -> EditarDescansoUiState) {
            _uiState.update { it.cambio().validado() }
        }

        private fun EditarDescansoUiState.validado(): EditarDescansoUiState {
            val borrador = borrador()
            return copy(
                errores = borrador?.let { validarDescanso(it, otros, clock.instant()) }.orEmpty(),
                duracion =
                    borrador?.let { b ->
                        b.fin?.takeIf { it.isAfter(b.inicio) }?.let { Duration.between(b.inicio, it) }
                    },
            )
        }

        private companion object {
            const val CLAVE_ID = "id"
        }
    }

/** El descanso con lo que hay escrito, o `null` si aún falta algo. */
internal fun EditarDescansoUiState.borrador(): Descanso? {
    val inicio = inicio ?: return null
    val zonaInicio = zonaInicio ?: return null
    val zonaFin = zonaFin ?: zonaInicio
    val base =
        original ?: Descanso(inicio = inicio.atZone(zonaInicio).toInstant(), zonaInicio = zonaInicio, tipo = tipo)
    return base.copy(
        inicio = inicio.atZone(zonaInicio).toInstant(),
        tipo = tipo,
        fin = if (terminado) fin?.atZone(zonaFin)?.toInstant() else null,
        zonaFin = if (terminado) zonaFin else null,
        pais = pais,
        observaciones = observaciones,
    )
}
