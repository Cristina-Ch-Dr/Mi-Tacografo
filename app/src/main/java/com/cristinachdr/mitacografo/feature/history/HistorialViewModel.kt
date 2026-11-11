package com.cristinachdr.mitacografo.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristinachdr.mitacografo.domain.model.SemanaJornadas
import com.cristinachdr.mitacografo.domain.usecase.ObtenerHistorial
import com.cristinachdr.mitacografo.domain.usecase.ObtenerInformeReglas
import com.cristinachdr.mitacografo.ui.reglas.EstadoRegla
import com.cristinachdr.mitacografo.ui.reglas.peor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class HistorialUiState(
    val semanas: List<SemanaJornadas> = emptyList(),
    val cargando: Boolean = true,
    /** El peor estado de las reglas de cada jornada (por id). Sin entrada = aún sin resultados. */
    val estadoJornada: Map<Long, EstadoRegla> = emptyMap(),
    /** El peor estado de las reglas de cada semana, contando las de sus jornadas (por lunes). */
    val estadoSemana: Map<LocalDate, EstadoRegla> = emptyMap(),
) {
    val vacio: Boolean get() = !cargando && semanas.isEmpty()
}

@HiltViewModel
class HistorialViewModel
    @Inject
    constructor(
        obtenerHistorial: ObtenerHistorial,
        obtenerInformeReglas: ObtenerInformeReglas,
    ) : ViewModel() {
        // Al editar o borrar una jornada, Room vuelve a emitir y la lista se actualiza sola (también las reglas).
        val uiState: StateFlow<HistorialUiState> =
            combine(obtenerHistorial(), obtenerInformeReglas()) { semanas, informe ->
                val jornadas = informe.semanas.flatMap { it.jornadas }
                HistorialUiState(
                    semanas = semanas,
                    cargando = false,
                    estadoJornada =
                        jornadas
                            .mapNotNull { j ->
                                j.resultados.peor()?.let { j.jornada.id to it }
                            }.toMap(),
                    estadoSemana =
                        informe.semanas
                            .mapNotNull { s ->
                                (s.resultados + s.jornadas.flatMap { it.resultados }).peor()?.let { s.lunes to it }
                            }.toMap(),
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistorialUiState())
    }
