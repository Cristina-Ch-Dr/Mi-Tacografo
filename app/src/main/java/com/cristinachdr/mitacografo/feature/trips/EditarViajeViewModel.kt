package com.cristinachdr.mitacografo.feature.trips

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristinachdr.mitacografo.domain.model.ErrorViaje
import com.cristinachdr.mitacografo.domain.model.ResultadoViaje
import com.cristinachdr.mitacografo.domain.model.ResumenViaje
import com.cristinachdr.mitacografo.domain.model.Viaje
import com.cristinachdr.mitacografo.domain.model.validarViaje
import com.cristinachdr.mitacografo.domain.port.ViajeRepository
import com.cristinachdr.mitacografo.domain.usecase.BorrarViaje
import com.cristinachdr.mitacografo.domain.usecase.GuardarViaje
import com.cristinachdr.mitacografo.domain.usecase.ObtenerViajes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject

data class EditarViajeUiState(
    /** El viaje con sus jornadas, tal y como está guardado. `null` mientras carga o si ya no existe. */
    val resumen: ResumenViaje? = null,
    val nombre: String = "",
    val inicio: LocalDateTime? = null,
    val fin: LocalDateTime? = null,
    val observaciones: String = "",
    val errores: Set<ErrorViaje> = emptySet(),
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val confirmandoBorrado: Boolean = false,
    /** Cuando pasa a `true` (guardado o borrado), la pantalla se cierra. */
    val cerrar: Boolean = false,
) {
    val noExiste: Boolean get() = !cargando && resumen == null
    val puedeGuardar: Boolean get() = resumen != null && !guardando && errores.isEmpty()
}

/** Detalle de un viaje: sus jornadas y km, y corregir el nombre, las fechas o borrarlo. */
@HiltViewModel
class EditarViajeViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        obtenerViajes: ObtenerViajes,
        private val repository: ViajeRepository,
        private val guardarViaje: GuardarViaje,
        private val borrarViaje: BorrarViaje,
        private val clock: Clock,
    ) : ViewModel() {
        private val id: Long = checkNotNull(savedStateHandle[CLAVE_ID])

        private val _uiState = MutableStateFlow(EditarViajeUiState())
        val uiState: StateFlow<EditarViajeUiState> = _uiState.asStateFlow()

        private var otros: List<Viaje> = emptyList()

        init {
            viewModelScope.launch {
                otros = repository.obtenerTodos().filter { it.id != id }
                var primeraVez = true
                // Las jornadas del viaje se siguen actualizando (si se corrige una desde aquí, cambian los km).
                obtenerViajes.uno(id).collect { resumen ->
                    _uiState.update { estado ->
                        val base = estado.copy(resumen = resumen, cargando = false)
                        if (primeraVez && resumen != null) {
                            primeraVez = false
                            base.desde(resumen.viaje).validado()
                        } else {
                            base
                        }
                    }
                }
            }
        }

        fun onNombre(valor: String) = actualizar { copy(nombre = valor) }

        fun onInicio(valor: LocalDateTime) = actualizar { copy(inicio = valor) }

        fun onFin(valor: LocalDateTime) = actualizar { copy(fin = valor) }

        fun onObservaciones(valor: String) = actualizar { copy(observaciones = valor) }

        fun onGuardar() {
            val estado = _uiState.value
            val viaje = estado.borrador() ?: return
            if (!estado.puedeGuardar) return
            _uiState.update { it.copy(guardando = true) }
            viewModelScope.launch {
                when (val resultado = guardarViaje(viaje)) {
                    is ResultadoViaje.Ok -> {
                        _uiState.update { it.copy(cerrar = true) }
                    }

                    is ResultadoViaje.Invalido -> {
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
                borrarViaje(id)
                _uiState.update { it.copy(cerrar = true) }
            }
        }

        private fun EditarViajeUiState.desde(viaje: Viaje): EditarViajeUiState =
            copy(
                nombre = viaje.nombre,
                inicio = viaje.inicio.atZone(viaje.zonaInicio).toLocalDateTime(),
                fin = viaje.fin?.atZone(viaje.zonaFin ?: viaje.zonaInicio)?.toLocalDateTime(),
                observaciones = viaje.observaciones.orEmpty(),
            )

        private fun actualizar(cambio: EditarViajeUiState.() -> EditarViajeUiState) =
            _uiState.update { it.cambio().validado() }

        private fun EditarViajeUiState.validado(): EditarViajeUiState =
            copy(errores = borrador()?.let { validarViaje(it, otros, clock.instant()) }.orEmpty())

        private companion object {
            const val CLAVE_ID = "id"
        }
    }

/** El viaje con lo que hay escrito, o `null` si aún no ha cargado. */
internal fun EditarViajeUiState.borrador(): Viaje? {
    val original = resumen?.viaje ?: return null
    val inicio = inicio ?: return null
    return original.copy(
        nombre = nombre,
        inicio = inicio.atZone(original.zonaInicio).toInstant(),
        fin = fin?.atZone(original.zonaFin ?: original.zonaInicio)?.toInstant(),
        observaciones = observaciones,
    )
}
