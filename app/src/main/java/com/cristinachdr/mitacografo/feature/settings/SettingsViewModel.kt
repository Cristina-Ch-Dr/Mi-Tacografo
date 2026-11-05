package com.cristinachdr.mitacografo.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristinachdr.mitacografo.domain.model.Ajustes
import com.cristinachdr.mitacografo.domain.model.AvisoConfig
import com.cristinachdr.mitacografo.domain.model.Continente
import com.cristinachdr.mitacografo.domain.model.ErrorAviso
import com.cristinachdr.mitacografo.domain.model.PreferenciasPaises
import com.cristinachdr.mitacografo.domain.model.conAviso
import com.cristinachdr.mitacografo.domain.model.errorAviso
import com.cristinachdr.mitacografo.domain.model.sinAviso
import com.cristinachdr.mitacografo.domain.port.SettingsRepository
import com.cristinachdr.mitacografo.domain.usecase.CambiarAjustes
import com.cristinachdr.mitacografo.domain.usecase.ObtenerPaisesRecientes
import com.cristinachdr.mitacografo.platform.ProbadorAvisos
import com.cristinachdr.mitacografo.ui.ErrorCampo
import com.cristinachdr.mitacografo.ui.casillasConduccion
import com.cristinachdr.mitacografo.ui.errorMinutos
import com.cristinachdr.mitacografo.ui.leerConduccion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import javax.inject.Inject

/**
 * Diálogo para poner la hora de un aviso.
 *
 * @param indice posición del aviso que se cambia, o `null` si se está añadiendo uno.
 */
data class EditorAviso(
    val indice: Int?,
    val horas: String = "",
    val minutos: String = "",
    val error: ErrorAviso? = null,
) {
    val errorMinutos: ErrorCampo? get() = errorMinutos(minutos)
}

data class AjustesUiState(
    val avisos: List<AvisoConfig> = AvisoConfig.POR_DEFECTO,
    /** Diálogo abierto para cambiar o añadir un aviso; `null` si no hay ninguno. */
    val editorAviso: EditorAviso? = null,
    val paisEmpresa: String = "",
    val paisResidencia: String = "",
    /** Los últimos países usados, para ponerlos arriba en el selector. */
    val paisesRecientes: List<String> = emptyList(),
    val paisesHabituales: Set<String> = emptySet(),
    val continentes: Set<Continente> = emptySet(),
    val nombreConductor: String = "",
    val cargando: Boolean = true,
    /** `true` justo después de pulsar «Probar aviso», para mostrar «Llegará en 1 minuto». */
    val pruebaProgramada: Boolean = false,
) {
    val avisosPorDefecto: Boolean get() = avisos == AvisoConfig.POR_DEFECTO
    val caben: Boolean get() = avisos.size < AvisoConfig.MAX_AVISOS

    /** Para los selectores de país de esta pantalla: se actualiza al momento al cambiar los filtros. */
    val preferenciasPaises: PreferenciasPaises
        get() = PreferenciasPaises(paisesRecientes, paisesHabituales, continentes)
}

/**
 * Los cambios se guardan al momento (no hay botón «Guardar»).
 *
 * Los textos se guardan en el estado local y se escriben en orden con un [Mutex]:
 * si el conductor escribe rápido, la última letra es la que queda guardada.
 */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val settings: SettingsRepository,
        private val cambiarAjustes: CambiarAjustes,
        private val probadorAvisos: ProbadorAvisos,
        private val obtenerPaisesRecientes: ObtenerPaisesRecientes,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AjustesUiState())
        val uiState: StateFlow<AjustesUiState> = _uiState.asStateFlow()

        private val escritura = Mutex()

        init {
            viewModelScope.launch {
                val ajustes = settings.observar().first()
                val recientes = obtenerPaisesRecientes()
                _uiState.update {
                    it.copy(
                        avisos = ajustes.avisos,
                        paisEmpresa = ajustes.paisEmpresa.orEmpty(),
                        paisResidencia = ajustes.paisResidencia.orEmpty(),
                        paisesRecientes = recientes,
                        paisesHabituales = ajustes.paisesHabituales,
                        continentes = ajustes.continentes,
                        nombreConductor = ajustes.nombreConductor.orEmpty(),
                        cargando = false,
                    )
                }
            }
        }

        fun onAviso(
            indice: Int,
            activo: Boolean,
        ) {
            val avisos = _uiState.value.avisos.toMutableList()
            avisos[indice] = avisos[indice].copy(activo = activo)
            _uiState.update { it.copy(avisos = avisos) }
            guardar { it.copy(avisos = avisos) }
        }

        fun onEditarAviso(indice: Int) {
            val (horas, minutos) = casillasConduccion(_uiState.value.avisos[indice].desdeInicio)
            _uiState.update { it.copy(editorAviso = EditorAviso(indice, horas, minutos)) }
        }

        fun onNuevoAviso() {
            if (!_uiState.value.caben) return
            _uiState.update { it.copy(editorAviso = EditorAviso(indice = null)) }
        }

        fun onEditorHoras(valor: String) = editor { it.copy(horas = valor, error = null) }

        fun onEditorMinutos(valor: String) = editor { it.copy(minutos = valor, error = null) }

        fun onCancelarAviso() = _uiState.update { it.copy(editorAviso = null) }

        fun onGuardarAviso() {
            val estado = _uiState.value
            val editor = estado.editorAviso ?: return
            if (editor.errorMinutos != null) return
            val desdeInicio = leerConduccion(editor.horas, editor.minutos) ?: Duration.ZERO
            val error = estado.avisos.errorAviso(desdeInicio, editor.indice)
            if (error != null) {
                _uiState.update { it.copy(editorAviso = editor.copy(error = error)) }
                return
            }
            cambiarAvisos(estado.avisos.conAviso(desdeInicio, editor.indice))
        }

        fun onBorrarAviso() {
            val estado = _uiState.value
            val indice = estado.editorAviso?.indice ?: return
            cambiarAvisos(estado.avisos.sinAviso(indice))
        }

        fun onRestaurarAvisos() = cambiarAvisos(AvisoConfig.POR_DEFECTO)

        private fun cambiarAvisos(avisos: List<AvisoConfig>) {
            _uiState.update { it.copy(avisos = avisos, editorAviso = null) }
            guardar { it.copy(avisos = avisos) }
        }

        private fun editor(cambio: (EditorAviso) -> EditorAviso) =
            _uiState.update { estado -> estado.copy(editorAviso = estado.editorAviso?.let(cambio)) }

        fun onPaisEmpresa(valor: String) {
            _uiState.update { it.copy(paisEmpresa = valor) }
            guardar { it.copy(paisEmpresa = valor.trim().uppercase()) }
        }

        fun onPaisResidencia(valor: String) {
            _uiState.update { it.copy(paisResidencia = valor) }
            guardar { it.copy(paisResidencia = valor.trim().uppercase()) }
        }

        fun onContinente(
            continente: Continente,
            elegido: Boolean,
        ) {
            val continentes = _uiState.value.continentes.let { if (elegido) it + continente else it - continente }
            _uiState.update { it.copy(continentes = continentes) }
            guardar { it.copy(continentes = continentes) }
        }

        fun onAnadirPaisHabitual(codigo: String) {
            if (codigo.isBlank()) return
            val paises = _uiState.value.paisesHabituales + codigo.trim().uppercase()
            _uiState.update { it.copy(paisesHabituales = paises) }
            guardar { it.copy(paisesHabituales = paises) }
        }

        fun onQuitarPaisHabitual(codigo: String) {
            val paises = _uiState.value.paisesHabituales - codigo
            _uiState.update { it.copy(paisesHabituales = paises) }
            guardar { it.copy(paisesHabituales = paises) }
        }

        fun onNombreConductor(valor: String) {
            _uiState.update { it.copy(nombreConductor = valor) }
            guardar { it.copy(nombreConductor = valor) }
        }

        fun onProbarAviso() {
            probadorAvisos.programarPrueba(Duration.ofMinutes(1))
            _uiState.update { it.copy(pruebaProgramada = true) }
        }

        private fun guardar(cambio: (Ajustes) -> Ajustes) {
            viewModelScope.launch { escritura.withLock { cambiarAjustes(cambio) } }
        }
    }
