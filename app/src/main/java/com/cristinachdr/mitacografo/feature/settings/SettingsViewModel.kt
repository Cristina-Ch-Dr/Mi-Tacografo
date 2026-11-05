package com.cristinachdr.mitacografo.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristinachdr.mitacografo.domain.model.Ajustes
import com.cristinachdr.mitacografo.domain.model.AvisoConfig
import com.cristinachdr.mitacografo.domain.model.Continente
import com.cristinachdr.mitacografo.domain.model.PreferenciasPaises
import com.cristinachdr.mitacografo.domain.port.SettingsRepository
import com.cristinachdr.mitacografo.domain.usecase.CambiarAjustes
import com.cristinachdr.mitacografo.domain.usecase.ObtenerPaisesRecientes
import com.cristinachdr.mitacografo.platform.ProbadorAvisos
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

data class AjustesUiState(
    val avisos: List<AvisoConfig> = AvisoConfig.POR_DEFECTO,
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
