package com.cristinachdr.mitacografo.feature.rests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.domain.model.TipoDescanso
import com.cristinachdr.mitacografo.ui.components.BigButton
import com.cristinachdr.mitacografo.ui.components.CampoFechaHora
import com.cristinachdr.mitacografo.ui.components.CampoPais
import com.cristinachdr.mitacografo.ui.components.CampoTexto
import com.cristinachdr.mitacografo.ui.components.ListaErroresDescanso
import com.cristinachdr.mitacografo.ui.textoDuracion
import com.cristinachdr.mitacografo.ui.theme.MiTacografoTheme
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

@Composable
fun EditarDescansoScreen(
    onCerrar: () -> Unit,
    viewModel: EditarDescansoViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    val alCerrar by rememberUpdatedState(onCerrar)
    LaunchedEffect(estado.cerrar) { if (estado.cerrar) alCerrar() }

    EditarDescansoContent(
        estado = estado,
        acciones =
            AccionesDescanso(
                onTipo = viewModel::onTipo,
                onInicio = viewModel::onInicio,
                onTerminado = viewModel::onTerminado,
                onFin = viewModel::onFin,
                onPais = viewModel::onPais,
                onObservaciones = viewModel::onObservaciones,
                onGuardar = viewModel::onGuardar,
                onPedirBorrar = viewModel::onPedirBorrar,
                onCancelarBorrar = viewModel::onCancelarBorrar,
                onConfirmarBorrar = viewModel::onConfirmarBorrar,
            ),
    )
}

data class AccionesDescanso(
    val onTipo: (TipoDescanso) -> Unit = {},
    val onInicio: (LocalDateTime) -> Unit = {},
    val onTerminado: (Boolean) -> Unit = {},
    val onFin: (LocalDateTime) -> Unit = {},
    val onPais: (String) -> Unit = {},
    val onObservaciones: (String) -> Unit = {},
    val onGuardar: () -> Unit = {},
    val onPedirBorrar: () -> Unit = {},
    val onCancelarBorrar: () -> Unit = {},
    val onConfirmarBorrar: () -> Unit = {},
)

@Composable
fun EditarDescansoContent(
    estado: EditarDescansoUiState,
    acciones: AccionesDescanso,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (estado.noExiste) {
            Text(stringResource(R.string.error_descanso_no_existe), style = MaterialTheme.typography.headlineSmall)
            return@Column
        }
        val inicio = estado.inicio ?: return@Column

        Text(stringResource(R.string.descanso_tipo), style = MaterialTheme.typography.titleMedium)
        Column(Modifier.selectableGroup()) {
            OpcionTipo(TipoDescanso.DESCANSO, R.string.tipo_descanso, estado.tipo, acciones.onTipo)
            OpcionTipo(TipoDescanso.PAUSA, R.string.tipo_pausa, estado.tipo, acciones.onTipo)
        }

        CampoFechaHora(stringResource(R.string.campo_hora_inicio), inicio, acciones.onInicio)

        if (!estado.nuevo && estado.original?.enCurso == true) {
            // Uno en curso normalmente se termina desde Inicio, pero aquí también se puede.
            FilaInterruptor(stringResource(R.string.descanso_terminado), estado.terminado, acciones.onTerminado)
        }
        val fin = estado.fin
        if (estado.terminado && fin != null) {
            CampoFechaHora(stringResource(R.string.campo_hora_fin), fin, acciones.onFin)
        } else {
            Text(
                stringResource(R.string.descanso_sigue_en_curso),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        estado.duracion?.let {
            Text(
                stringResource(R.string.descanso_duracion, textoDuracion(it)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        CampoPais(
            valor = estado.pais,
            onCambio = acciones.onPais,
            etiqueta = stringResource(R.string.campo_pais),
            preferencias = estado.paises,
        )
        HorizontalDivider()
        CampoTexto(
            valor = estado.observaciones,
            onCambio = acciones.onObservaciones,
            etiqueta = stringResource(R.string.campo_observaciones),
            unaLinea = false,
        )
        ListaErroresDescanso(estado.errores)
        BigButton(
            text = stringResource(R.string.accion_guardar_cambios),
            onClick = acciones.onGuardar,
            enabled = estado.puedeGuardar,
            icon = Icons.Filled.Done,
        )
        if (!estado.nuevo) {
            OutlinedButton(
                onClick = acciones.onPedirBorrar,
                enabled = !estado.guardando,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null)
                Text(stringResource(R.string.accion_borrar_descanso), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }

    if (estado.confirmandoBorrado) {
        AlertDialog(
            onDismissRequest = acciones.onCancelarBorrar,
            title = { Text(stringResource(R.string.borrar_descanso_titulo)) },
            text = { Text(stringResource(R.string.borrar_texto)) },
            confirmButton = {
                TextButton(onClick = acciones.onConfirmarBorrar) {
                    Text(stringResource(R.string.accion_borrar), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = acciones.onCancelarBorrar) { Text(stringResource(R.string.accion_cancelar)) }
            },
        )
    }
}

/** Una opción grande (toda la fila se puede tocar, también con guantes). */
@Composable
private fun OpcionTipo(
    tipo: TipoDescanso,
    texto: Int,
    elegido: TipoDescanso,
    onTipo: (TipoDescanso) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .selectable(selected = tipo == elegido, onClick = { onTipo(tipo) }, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = tipo == elegido, onClick = null)
        Text(
            stringResource(texto),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun FilaInterruptor(
    texto: String,
    activo: Boolean,
    onCambio: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .toggleable(value = activo, onValueChange = onCambio, role = Role.Switch),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(texto, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = activo, onCheckedChange = null)
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun EditarDescansoPreview() {
    val madrid = ZoneId.of("Europe/Madrid")
    val inicio = LocalDateTime.of(2026, 11, 4, 19, 0)
    MiTacografoTheme {
        EditarDescansoContent(
            estado =
                EditarDescansoUiState(
                    inicio = inicio,
                    zonaInicio = madrid,
                    fin = inicio.plusHours(11),
                    zonaFin = madrid,
                    pais = "FR",
                    duracion = Duration.ofHours(11),
                    cargando = false,
                ),
            acciones = AccionesDescanso(),
        )
    }
}
