package com.cristinachdr.mitacografo.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.domain.model.AjusteSemanas
import com.cristinachdr.mitacografo.domain.port.JornadaRepository
import com.cristinachdr.mitacografo.domain.port.SettingsRepository
import com.cristinachdr.mitacografo.ui.ErrorCampo
import com.cristinachdr.mitacografo.ui.casillasConduccion
import com.cristinachdr.mitacografo.ui.components.BigButton
import com.cristinachdr.mitacografo.ui.components.CampoNumero
import com.cristinachdr.mitacografo.ui.errorMinutos
import com.cristinachdr.mitacografo.ui.formatoFecha
import com.cristinachdr.mitacografo.ui.leerConduccion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class AjusteSemanasUiState(
    /** El lunes de la primera semana con jornadas (o de esta, si aún no hay ninguna). */
    val semana: LocalDate? = null,
    val horasSemana: String = "",
    val minutosSemana: String = "",
    val horasAnterior: String = "",
    val minutosAnterior: String = "",
    val ampliaciones: Int = 0,
    val reducidos: Int = 0,
    /** Hay un ajuste guardado. */
    val guardado: Boolean = false,
    /** El guardado es de otra semana (se añadieron jornadas de antes): ya no se usa. */
    val deOtraSemana: LocalDate? = null,
) {
    private val semanaLeida: Duration? get() = leerConduccion(horasSemana, minutosSemana)
    private val anteriorLeida: Duration? get() = leerConduccion(horasAnterior, minutosAnterior)

    val errorSemana: ErrorCampo? get() = errorConduccion(minutosSemana, semanaLeida)
    val errorAnterior: ErrorCampo? get() = errorConduccion(minutosAnterior, anteriorLeida)

    val puedeGuardar: Boolean get() = semana != null && errorSemana == null && errorAnterior == null

    fun ajuste(): AjusteSemanas? {
        if (!puedeGuardar) return null
        return AjusteSemanas(
            semana = semana ?: return null,
            conduccionSemana = semanaLeida ?: Duration.ZERO,
            conduccionSemanaAnterior = anteriorLeida ?: Duration.ZERO,
            ampliaciones = ampliaciones,
            reducidos = reducidos,
        )
    }
}

/** Minutos de 0 a 59 y como mucho 56 h en la semana (art. 6.2). */
private fun errorConduccion(
    minutos: String,
    valor: Duration?,
): ErrorCampo? =
    errorMinutos(minutos)
        ?: ErrorCampo.MAS_DE_56_HORAS.takeIf { valor != null && valor > AjusteSemanas.MAX_CONDUCCION_SEMANA }

/** Ajustes → «Ajuste de semanas» (MT-59): lo que ya llevaba antes de empezar a usar la app. */
@HiltViewModel
class AjusteSemanasViewModel
    @Inject
    constructor(
        private val settings: SettingsRepository,
        private val jornadas: JornadaRepository,
        private val clock: Clock,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AjusteSemanasUiState())
        val uiState: StateFlow<AjusteSemanasUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                val ajustes = settings.observar().first()
                val primera = jornadas.observarTodas().first().minByOrNull { it.inicio }
                // La semana se mira como el motor de reglas: en la zona de referencia o en la de la primera jornada.
                val zona = ajustes.zonaReferencia ?: primera?.zonaInicio ?: clock.zone
                val dia = primera?.inicio?.atZone(zona)?.toLocalDate() ?: LocalDate.now(clock.withZone(zona))
                val semana = dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val guardado = ajustes.ajusteSemanas
                val (hs, ms) = casillasConduccion(guardado?.conduccionSemana?.takeIf { !it.isZero })
                val (ha, ma) = casillasConduccion(guardado?.conduccionSemanaAnterior?.takeIf { !it.isZero })
                _uiState.value =
                    AjusteSemanasUiState(
                        semana = semana,
                        horasSemana = hs,
                        minutosSemana = ms,
                        horasAnterior = ha,
                        minutosAnterior = ma,
                        ampliaciones = guardado?.ampliaciones ?: 0,
                        reducidos = guardado?.reducidos ?: 0,
                        guardado = guardado != null,
                        deOtraSemana = guardado?.semana?.takeIf { it != semana },
                    )
            }
        }

        fun onHorasSemana(v: String) = _uiState.update { it.copy(horasSemana = v.take(2)) }

        fun onMinutosSemana(v: String) = _uiState.update { it.copy(minutosSemana = v.take(2)) }

        fun onHorasAnterior(v: String) = _uiState.update { it.copy(horasAnterior = v.take(2)) }

        fun onMinutosAnterior(v: String) = _uiState.update { it.copy(minutosAnterior = v.take(2)) }

        fun onAmpliaciones(n: Int) =
            _uiState.update { it.copy(ampliaciones = n.coerceIn(0, AjusteSemanas.MAX_AMPLIACIONES)) }

        fun onReducidos(n: Int) = _uiState.update { it.copy(reducidos = n.coerceIn(0, AjusteSemanas.MAX_REDUCIDOS)) }

        fun onGuardar() {
            val ajuste = _uiState.value.ajuste() ?: return
            viewModelScope.launch {
                settings.actualizar { it.copy(ajusteSemanas = ajuste) }
                _uiState.update { it.copy(guardado = true, deOtraSemana = null) }
            }
        }

        fun onQuitar() {
            viewModelScope.launch {
                settings.actualizar { it.copy(ajusteSemanas = null) }
                _uiState.update {
                    AjusteSemanasUiState(semana = it.semana)
                }
            }
        }
    }

/**
 * La sección: semana a la que se refiere, conducción de esa semana y de la anterior, ampliaciones y reducidos ya
 * usados, y un botón para apuntar el último descanso semanal (como un descanso más). El «?» lo explica.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SeccionAjusteSemanas(
    onAnadirDescanso: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AjusteSemanasViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    var ayuda by remember { mutableStateOf(false) }
    val semana = estado.semana ?: return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.ajuste_semana_de, semana.format(formatoFecha())),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { ayuda = true }) {
                Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.ajuste_que_es))
            }
        }
        estado.deOtraSemana?.let {
            Text(
                stringResource(R.string.ajuste_de_otra_semana, it.format(formatoFecha())),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Text(stringResource(R.string.ajuste_conduccion_semana), style = MaterialTheme.typography.titleSmall)
        Casillas(
            estado.horasSemana,
            estado.minutosSemana,
            viewModel::onHorasSemana,
            viewModel::onMinutosSemana,
            estado.errorSemana,
        )
        Text(stringResource(R.string.ajuste_conduccion_anterior), style = MaterialTheme.typography.titleSmall)
        Casillas(
            estado.horasAnterior,
            estado.minutosAnterior,
            viewModel::onHorasAnterior,
            viewModel::onMinutosAnterior,
            estado.errorAnterior,
        )
        Text(stringResource(R.string.ajuste_ampliaciones), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (0..AjusteSemanas.MAX_AMPLIACIONES).forEach { n ->
                FilterChip(
                    selected = estado.ampliaciones == n,
                    onClick = { viewModel.onAmpliaciones(n) },
                    label = { Text("$n") },
                )
            }
        }
        Text(stringResource(R.string.ajuste_reducidos), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (0..AjusteSemanas.MAX_REDUCIDOS).forEach { n ->
                FilterChip(
                    selected = estado.reducidos == n,
                    onClick = { viewModel.onReducidos(n) },
                    label = { Text("$n") },
                )
            }
        }
        OutlinedButton(onClick = onAnadirDescanso, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.ajuste_apuntar_semanal))
        }
        BigButton(
            text = stringResource(if (estado.guardado) R.string.ajuste_guardar_cambios else R.string.ajuste_guardar),
            onClick = viewModel::onGuardar,
            enabled = estado.puedeGuardar,
        )
        if (estado.guardado) {
            TextButton(onClick = viewModel::onQuitar) { Text(stringResource(R.string.ajuste_quitar)) }
        }
    }
    if (ayuda) {
        AlertDialog(
            onDismissRequest = { ayuda = false },
            title = { Text(stringResource(R.string.ajuste_que_es)) },
            text = { Text(stringResource(R.string.ajuste_ayuda)) },
            confirmButton = {
                TextButton(
                    onClick = { ayuda = false },
                ) { Text(stringResource(R.string.accion_aceptar)) }
            },
        )
    }
}

@Composable
private fun Casillas(
    horas: String,
    minutos: String,
    onHoras: (String) -> Unit,
    onMinutos: (String) -> Unit,
    error: ErrorCampo?,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CampoNumero(horas, onHoras, stringResource(R.string.campo_horas), Modifier.weight(1f))
        CampoNumero(minutos, onMinutos, stringResource(R.string.campo_minutos), Modifier.weight(1f), error = error)
    }
}
