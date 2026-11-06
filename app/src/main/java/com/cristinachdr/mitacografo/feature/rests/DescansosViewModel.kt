package com.cristinachdr.mitacografo.feature.rests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristinachdr.mitacografo.domain.model.DescansoClasificado
import com.cristinachdr.mitacografo.domain.usecase.ObtenerDescansos
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DescansosUiState(
    /** Del más reciente al más antiguo, cada uno con su clase. */
    val descansos: List<DescansoClasificado> = emptyList(),
    val cargando: Boolean = true,
) {
    val vacio: Boolean get() = !cargando && descansos.isEmpty()
}

@HiltViewModel
class DescansosViewModel
    @Inject
    constructor(
        obtenerDescansos: ObtenerDescansos,
    ) : ViewModel() {
        val uiState: StateFlow<DescansosUiState> =
            obtenerDescansos()
                .map { DescansosUiState(descansos = it, cargando = false) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DescansosUiState())
    }
