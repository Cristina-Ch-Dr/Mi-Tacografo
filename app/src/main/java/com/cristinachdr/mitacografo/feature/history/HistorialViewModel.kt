package com.cristinachdr.mitacografo.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristinachdr.mitacografo.domain.model.DescansoPrevio
import com.cristinachdr.mitacografo.domain.model.SemanaJornadas
import com.cristinachdr.mitacografo.domain.usecase.ObtenerDescansosPrevios
import com.cristinachdr.mitacografo.domain.usecase.ObtenerHistorial
import com.cristinachdr.mitacografo.domain.usecase.ObtenerInformeReglas
import com.cristinachdr.mitacografo.ui.reglas.EstadoRegla
import com.cristinachdr.mitacografo.ui.reglas.peor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
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
    /** Lo que descansó antes de cada jornada (por id), apuntado o calculado (MT-46). */
    val descansoPrevio: Map<Long, DescansoPrevio> = emptyMap(),
    /** La semana que se ve (su lunes); `null` = la más reciente. */
    val elegida: LocalDate? = null,
) {
    val vacio: Boolean get() = !cargando && semanas.isEmpty()

    /** Posición de la semana que se ve en [semanas] (0 = la más reciente). */
    private val indice: Int get() = semanas.indexOfFirst { it.lunes == elegida }.coerceAtLeast(0)

    /** La semana que se ve: una cada vez, como en el Resumen (MT-48). */
    val semana: SemanaJornadas? get() = semanas.getOrNull(indice)

    /** Hay una semana más antigua con jornadas. */
    val hayAnterior: Boolean get() = indice < semanas.lastIndex

    /** Hay una semana más reciente con jornadas. */
    val haySiguiente: Boolean get() = indice > 0

    internal fun lunesAnterior(): LocalDate? = semanas.getOrNull(indice + 1)?.lunes

    internal fun lunesSiguiente(): LocalDate? = semanas.getOrNull(indice - 1)?.lunes
}

@HiltViewModel
class HistorialViewModel
    @Inject
    constructor(
        obtenerHistorial: ObtenerHistorial,
        obtenerInformeReglas: ObtenerInformeReglas,
        obtenerDescansosPrevios: ObtenerDescansosPrevios,
    ) : ViewModel() {
        private val elegida = MutableStateFlow<LocalDate?>(null)

        // Al editar o borrar una jornada, Room vuelve a emitir y la lista se actualiza sola (también las reglas).
        val uiState: StateFlow<HistorialUiState> =
            combine(
                obtenerHistorial(),
                obtenerInformeReglas(),
                obtenerDescansosPrevios(),
                elegida,
            ) { semanas, informe, previos, lunes ->
                val jornadas = informe.semanas.flatMap { it.jornadas }
                HistorialUiState(
                    semanas = semanas,
                    cargando = false,
                    descansoPrevio = previos,
                    elegida = lunes,
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

        /** Flecha ‹: la semana anterior que tenga jornadas (se saltan las vacías, como las vacaciones). */
        fun onAnterior() {
            uiState.value.lunesAnterior()?.let { elegida.value = it }
        }

        /** Flecha ›: la semana siguiente que tenga jornadas. */
        fun onSiguiente() {
            uiState.value.lunesSiguiente()?.let { elegida.value = it }
        }
    }
