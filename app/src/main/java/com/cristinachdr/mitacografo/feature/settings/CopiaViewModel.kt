package com.cristinachdr.mitacografo.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristinachdr.mitacografo.domain.model.CopiaSeguridad
import com.cristinachdr.mitacografo.domain.model.ErrorCopia
import com.cristinachdr.mitacografo.domain.port.JornadaRepository
import com.cristinachdr.mitacografo.domain.usecase.ExportarCopia
import com.cristinachdr.mitacografo.domain.usecase.LeerCopia
import com.cristinachdr.mitacografo.domain.usecase.RestaurarCopia
import com.cristinachdr.mitacografo.domain.usecase.ResultadoLectura
import com.cristinachdr.mitacografo.platform.ArchivosCopia
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Lo que se le dice al conductor después de guardar o restaurar. */
sealed interface MensajeCopia {
    data object Guardada : MensajeCopia

    data object Restaurada : MensajeCopia

    data object NoSePudoGuardar : MensajeCopia

    data object NoSePudoLeer : MensajeCopia

    data class NoValida(
        val error: ErrorCopia,
    ) : MensajeCopia
}

data class CopiaUiState(
    val trabajando: Boolean = false,
    /** Copia leída que espera a que el conductor confirme («¿Restaurar esta copia?»). */
    val porRestaurar: CopiaSeguridad? = null,
    /** Jornadas que hay ahora en el móvil y que se borrarán al restaurar. */
    val jornadasActuales: Int = 0,
    val mensaje: MensajeCopia? = null,
)

/**
 * Copia de seguridad en Ajustes: guardar todo en un archivo y restaurarlo.
 *
 * Restaurar va en dos pasos: primero se lee el archivo y se enseña el resumen; solo si el
 * conductor confirma se reemplazan los datos. Así un archivo equivocado no borra nada.
 */
@HiltViewModel
class CopiaViewModel
    @Inject
    constructor(
        private val exportarCopia: ExportarCopia,
        private val leerCopia: LeerCopia,
        private val restaurarCopia: RestaurarCopia,
        private val repository: JornadaRepository,
        private val archivos: ArchivosCopia,
        private val clock: Clock,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CopiaUiState())
        val uiState: StateFlow<CopiaUiState> = _uiState.asStateFlow()

        /** Nombre que se propone al guardar, con la fecha: "mi-tacografo-2026-11-05.json". */
        fun nombreArchivo(): String = "mi-tacografo-${LocalDate.now(clock)}.json"

        /** Se llama con el archivo que eligió el conductor en la pantalla de Android. */
        fun onGuardarEn(destino: String) =
            trabajar {
                val mensaje =
                    try {
                        archivos.escribir(destino, exportarCopia())
                        MensajeCopia.Guardada
                    } catch (_: IOException) {
                        MensajeCopia.NoSePudoGuardar
                    }
                _uiState.update { it.copy(mensaje = mensaje) }
            }

        fun onAbrir(origen: String) =
            trabajar {
                val texto =
                    try {
                        archivos.leer(origen)
                    } catch (_: IOException) {
                        _uiState.update { it.copy(mensaje = MensajeCopia.NoSePudoLeer) }
                        return@trabajar
                    }
                when (val resultado = leerCopia(texto)) {
                    is ResultadoLectura.Ok -> {
                        val actuales = repository.observarTodas().first().size
                        _uiState.update {
                            it.copy(
                                porRestaurar = resultado.copia,
                                jornadasActuales = actuales,
                                mensaje = null,
                            )
                        }
                    }

                    is ResultadoLectura.Invalida -> {
                        _uiState.update { it.copy(mensaje = MensajeCopia.NoValida(resultado.error)) }
                    }
                }
            }

        fun onConfirmarRestaurar() {
            val copia = _uiState.value.porRestaurar ?: return
            _uiState.update { it.copy(porRestaurar = null) }
            trabajar {
                restaurarCopia(copia)
                _uiState.update { it.copy(mensaje = MensajeCopia.Restaurada) }
            }
        }

        fun onCancelarRestaurar() = _uiState.update { it.copy(porRestaurar = null) }

        private fun trabajar(accion: suspend () -> Unit) {
            if (_uiState.value.trabajando) return
            _uiState.update { it.copy(trabajando = true, mensaje = null) }
            viewModelScope.launch {
                try {
                    accion()
                } finally {
                    _uiState.update { it.copy(trabajando = false) }
                }
            }
        }
    }
