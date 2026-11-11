package com.cristinachdr.mitacografo.feature.report

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristinachdr.mitacografo.domain.rules.Deuda
import com.cristinachdr.mitacografo.domain.rules.InformeSemana
import com.cristinachdr.mitacografo.domain.usecase.ObtenerInformeReglas
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class InformeSemanaUiState(
    val lunes: LocalDate,
    /** `null` si esa semana no tiene jornadas (o mientras carga). */
    val semana: InformeSemana? = null,
    /** Las deudas de los reducidos que cuentan en esta semana. */
    val deudas: List<Deuda> = emptyList(),
    /** Zona en la que se enseñan las fechas límite. */
    val zona: ZoneId = ZoneId.systemDefault(),
    val cargando: Boolean = true,
)

/** El informe de reglas de una semana (MT-30). Se recalcula solo si se corrige algo de esa semana. */
@HiltViewModel
class InformeSemanaViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        obtenerInformeReglas: ObtenerInformeReglas,
    ) : ViewModel() {
        /** Viene de la ruta `RutaInforme(lunes)`, como texto ISO («2026-11-09»). */
        private val lunes: LocalDate = LocalDate.parse(checkNotNull(savedStateHandle.get<String>(CLAVE_LUNES)))

        val uiState: StateFlow<InformeSemanaUiState> =
            obtenerInformeReglas()
                .map { informe ->
                    InformeSemanaUiState(
                        lunes = lunes,
                        semana = informe.semanas.find { it.lunes == lunes },
                        deudas = informe.deudas.filter { it.semana == lunes },
                        cargando = false,
                    )
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InformeSemanaUiState(lunes))

        private companion object {
            const val CLAVE_LUNES = "lunes"
        }
    }
