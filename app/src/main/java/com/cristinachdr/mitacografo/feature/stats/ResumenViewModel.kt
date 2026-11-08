package com.cristinachdr.mitacografo.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristinachdr.mitacografo.domain.model.ResumenPeriodo
import com.cristinachdr.mitacografo.domain.usecase.ObtenerResumen
import com.cristinachdr.mitacografo.domain.usecase.PrepararInforme
import com.cristinachdr.mitacografo.platform.ArchivosInforme
import com.cristinachdr.mitacografo.platform.GeneradorPdf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject

/** Qué periodo se ve: una semana o dos seguidas (la bisemana de los límites de 90 h). */
data class Periodo(
    /** Lunes de la primera semana. */
    val desde: LocalDate,
    val semanas: Int,
)

enum class MensajeResumen { PDF_GUARDADO, PDF_ERROR }

data class ResumenUiState(
    val periodo: Periodo? = null,
    /** Lunes de la semana de hoy: no se puede ir más allá. */
    val lunesActual: LocalDate? = null,
    val resumen: ResumenPeriodo? = null,
    val exportando: Boolean = false,
    val mensaje: MensajeResumen? = null,
    /** PDF listo para compartir (`content://`). La pantalla abre el menú de compartir y lo limpia. */
    val compartir: String? = null,
) {
    val puedeAvanzar: Boolean
        get() {
            val periodo = periodo ?: return false
            val actual = lunesActual ?: return false
            return periodo.desde.plusWeeks(periodo.semanas.toLong() - 1) < actual
        }
}

/**
 * Pantalla Resumen (MT-32): la semana o la bisemana con sus totales, y exportar a PDF (MT-33B, sin reglas).
 *
 * La bisemana son la semana elegida y la anterior: las que cuentan juntas para las 90 h de conducción.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ResumenViewModel
    @Inject
    constructor(
        private val obtenerResumen: ObtenerResumen,
        private val prepararInforme: PrepararInforme,
        private val generadorPdf: GeneradorPdf,
        private val archivos: ArchivosInforme,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ResumenUiState())
        val uiState: StateFlow<ResumenUiState> = _uiState.asStateFlow()

        private val periodo = MutableStateFlow<Periodo?>(null)

        init {
            viewModelScope.launch {
                val lunes = obtenerResumen.lunesActual()
                _uiState.update { it.copy(lunesActual = lunes) }
                periodo.value = Periodo(lunes, 1)
            }
            viewModelScope.launch {
                periodo
                    .filterNotNull()
                    .flatMapLatest { p ->
                        _uiState.update { it.copy(periodo = p) }
                        obtenerResumen(p.desde, p.semanas)
                    }.collect { resumen -> _uiState.update { it.copy(resumen = resumen) } }
            }
        }

        /** Cambia entre una semana y dos. Con dos, la última sigue siendo la que se estaba viendo. */
        fun onSemanas(semanas: Int) =
            periodo.update { p ->
                if (p == null || p.semanas == semanas) return@update p
                val ultima = p.desde.plusWeeks(p.semanas.toLong() - 1)
                Periodo(ultima.minusWeeks(semanas.toLong() - 1), semanas)
            }

        fun onAnterior() = periodo.update { it?.copy(desde = it.desde.minusWeeks(1)) }

        fun onSiguiente() {
            if (_uiState.value.puedeAvanzar) periodo.update { it?.copy(desde = it.desde.plusWeeks(1)) }
        }

        /** Nombre propuesto al guardar: «mi-tacografo-2026-11-02_2026-11-08.pdf». */
        fun nombreArchivo(): String {
            val p = _uiState.value.resumen
            return if (p == null) "mi-tacografo.pdf" else "mi-tacografo-${p.desde}_${p.hasta}.pdf"
        }

        fun onGuardarEn(destino: String) = exportar { bytes -> archivos.guardar(destino, bytes) }

        fun onCompartir() =
            exportar { bytes ->
                val uri = archivos.paraCompartir(nombreArchivo(), bytes)
                _uiState.update { it.copy(compartir = uri) }
            }

        fun onCompartido() = _uiState.update { it.copy(compartir = null) }

        fun onMensajeVisto() = _uiState.update { it.copy(mensaje = null) }

        private fun exportar(destino: suspend (ByteArray) -> Unit) {
            val p = periodo.value ?: return
            if (_uiState.value.exportando) return
            _uiState.update { it.copy(exportando = true, mensaje = null) }
            viewModelScope.launch {
                val mensaje =
                    try {
                        destino(generadorPdf.generar(prepararInforme(p.desde, p.semanas)))
                        if (_uiState.value.compartir == null) MensajeResumen.PDF_GUARDADO else null
                    } catch (e: IOException) {
                        MensajeResumen.PDF_ERROR
                    } catch (e: IllegalStateException) {
                        // PdfDocument o FileProvider mal configurados: mejor un mensaje que cerrar la app.
                        MensajeResumen.PDF_ERROR
                    }
                _uiState.update { it.copy(exportando = false, mensaje = mensaje) }
            }
        }
    }
