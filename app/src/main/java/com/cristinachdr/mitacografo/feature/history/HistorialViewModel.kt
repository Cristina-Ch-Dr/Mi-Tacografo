package com.cristinachdr.mitacografo.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristinachdr.mitacografo.domain.model.SemanaJornadas
import com.cristinachdr.mitacografo.domain.usecase.ObtenerHistorial
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HistorialUiState(
    val semanas: List<SemanaJornadas> = emptyList(),
    val cargando: Boolean = true,
) {
    val vacio: Boolean get() = !cargando && semanas.isEmpty()
}

@HiltViewModel
class HistorialViewModel
    @Inject
    constructor(
        obtenerHistorial: ObtenerHistorial,
    ) : ViewModel() {
        // Al editar o borrar una jornada, Room vuelve a emitir y la lista se actualiza sola.
        val uiState: StateFlow<HistorialUiState> =
            obtenerHistorial()
                .map { HistorialUiState(semanas = it, cargando = false) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistorialUiState())
    }
