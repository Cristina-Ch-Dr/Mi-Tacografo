package com.cristinachdr.mitacografo.feature.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristinachdr.mitacografo.domain.model.ErrorViaje
import com.cristinachdr.mitacografo.domain.model.ResultadoViaje
import com.cristinachdr.mitacografo.domain.model.ResumenViaje
import com.cristinachdr.mitacografo.domain.usecase.FinalizarViaje
import com.cristinachdr.mitacografo.domain.usecase.IniciarViaje
import com.cristinachdr.mitacografo.domain.usecase.ObtenerViajes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class ViajesUiState(
    val enCurso: ResumenViaje? = null,
    /** Los terminados, del más reciente al más antiguo. */
    val anteriores: List<ResumenViaje> = emptyList(),
    val cargando: Boolean = true,
    /** Diálogo «Iniciar viaje» abierto, con el nombre que se está escribiendo. */
    val nombreNuevo: String? = null,
    val confirmandoFin: Boolean = false,
    val errores: Set<ErrorViaje> = emptySet(),
)

/** Pestaña Viajes (MT-18): el viaje en curso con sus km, y los anteriores. */
@HiltViewModel
class ViajesViewModel
    @Inject
    constructor(
        obtenerViajes: ObtenerViajes,
        private val iniciarViaje: IniciarViaje,
        private val finalizarViaje: FinalizarViaje,
        private val clock: Clock,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ViajesUiState())
        val uiState: StateFlow<ViajesUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                obtenerViajes().collect { viajes ->
                    _uiState.update { estado ->
                        estado.copy(
                            enCurso = viajes.firstOrNull { it.viaje.enCurso },
                            anteriores = viajes.filterNot { it.viaje.enCurso },
                            cargando = false,
                        )
                    }
                }
            }
        }

        /** Abre el diálogo con un nombre propuesto; casi siempre basta con cambiarlo o aceptarlo. */
        fun onPedirIniciar(nombrePropuesto: (LocalDate) -> String) =
            _uiState.update { it.copy(nombreNuevo = nombrePropuesto(LocalDate.now(clock)), errores = emptySet()) }

        fun onNombreNuevo(valor: String) = _uiState.update { it.copy(nombreNuevo = valor, errores = emptySet()) }

        fun onCancelarIniciar() = _uiState.update { it.copy(nombreNuevo = null, errores = emptySet()) }

        fun onIniciar() {
            val nombre = _uiState.value.nombreNuevo ?: return
            viewModelScope.launch {
                when (val resultado = iniciarViaje(nombre)) {
                    is ResultadoViaje.Ok -> _uiState.update { it.copy(nombreNuevo = null) }
                    is ResultadoViaje.Invalido -> _uiState.update { it.copy(errores = resultado.errores) }
                }
            }
        }

        fun onPedirFinalizar() = _uiState.update { it.copy(confirmandoFin = true) }

        fun onCancelarFinalizar() = _uiState.update { it.copy(confirmandoFin = false) }

        fun onFinalizar() {
            _uiState.update { it.copy(confirmandoFin = false) }
            viewModelScope.launch { finalizarViaje() }
        }
    }
