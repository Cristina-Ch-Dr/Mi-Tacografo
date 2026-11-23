package com.cristinachdr.mitacografo.feature.rests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristinachdr.mitacografo.domain.model.DescansoClasificado
import com.cristinachdr.mitacografo.domain.model.ErrorDescanso
import com.cristinachdr.mitacografo.domain.model.ResultadoDescanso
import com.cristinachdr.mitacografo.domain.usecase.ApuntarPausaTresHoras
import com.cristinachdr.mitacografo.domain.usecase.ObtenerDescansos
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** El diálogo «Pausa de 3 horas» (MT-54), mientras está abierto. */
data class PausaTresHorasUi(
    /** Cuándo terminó la pausa, en la zona del móvil. Se propone la hora de ahora. */
    val fin: LocalDateTime,
    val errores: Set<ErrorDescanso> = emptySet(),
    val guardando: Boolean = false,
) {
    /** Desde cuándo se apunta: las 3 h anteriores. */
    val inicio: LocalDateTime get() = fin.minus(ApuntarPausaTresHoras.DURACION)
}

data class DescansosUiState(
    /** Del más reciente al más antiguo, cada uno con su clase. */
    val descansos: List<DescansoClasificado> = emptyList(),
    val cargando: Boolean = true,
    /** `null` si el diálogo de la pausa de 3 h está cerrado. */
    val pausaTresHoras: PausaTresHorasUi? = null,
) {
    val vacio: Boolean get() = !cargando && descansos.isEmpty()
}

@HiltViewModel
class DescansosViewModel
    @Inject
    constructor(
        obtenerDescansos: ObtenerDescansos,
        private val apuntarPausaTresHoras: ApuntarPausaTresHoras,
        private val clock: Clock,
    ) : ViewModel() {
        private val pausa = MutableStateFlow<PausaTresHorasUi?>(null)

        val uiState: StateFlow<DescansosUiState> =
            combine(obtenerDescansos(), pausa) { descansos, pausa ->
                DescansosUiState(descansos = descansos, cargando = false, pausaTresHoras = pausa)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DescansosUiState())

        /** Abre el diálogo proponiendo que la pausa terminó ahora. */
        fun onPedirPausaTresHoras() {
            pausa.value = PausaTresHorasUi(fin = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES))
        }

        fun onFinPausa(valor: LocalDateTime) = pausa.update { it?.copy(fin = valor, errores = emptySet()) }

        fun onCancelarPausa() {
            pausa.value = null
        }

        /** Apunta las 3 h anteriores a la hora elegida. Si sale bien, se cierra el diálogo y aparece en la lista. */
        fun onApuntarPausa() {
            val actual = pausa.value ?: return
            if (actual.guardando) return
            pausa.value = actual.copy(guardando = true)
            viewModelScope.launch {
                val fin = actual.fin.atZone(clock.zone).toInstant()
                when (val resultado = apuntarPausaTresHoras(fin, clock.zone)) {
                    is ResultadoDescanso.Ok -> {
                        pausa.value = null
                    }

                    is ResultadoDescanso.Invalido -> {
                        pausa.update {
                            it?.copy(
                                guardando = false,
                                errores = resultado.errores,
                            )
                        }
                    }
                }
            }
        }
    }
