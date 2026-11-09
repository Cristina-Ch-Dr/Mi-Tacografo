package com.cristinachdr.mitacografo.feature.trips

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.ui.FormatoHora
import com.cristinachdr.mitacografo.ui.components.BigButton
import com.cristinachdr.mitacografo.ui.components.CampoFechaHora
import com.cristinachdr.mitacografo.ui.components.CampoTexto
import com.cristinachdr.mitacografo.ui.components.ListaErroresViaje
import com.cristinachdr.mitacografo.ui.formatoDiaSemana
import com.cristinachdr.mitacografo.ui.formatoFecha
import com.cristinachdr.mitacografo.ui.textoDuracion
import com.cristinachdr.mitacografo.ui.textoKm
import java.time.LocalDateTime

@Composable
fun EditarViajeScreen(
    onJornada: (Long) -> Unit,
    onCerrar: () -> Unit,
    viewModel: EditarViajeViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val alCerrar by rememberUpdatedState(onCerrar)
    LaunchedEffect(estado.cerrar) { if (estado.cerrar) alCerrar() }

    EditarViajeContent(
        estado = estado,
        acciones =
            AccionesViaje(
                onNombre = viewModel::onNombre,
                onInicio = viewModel::onInicio,
                onFin = viewModel::onFin,
                onObservaciones = viewModel::onObservaciones,
                onGuardar = viewModel::onGuardar,
                onPedirBorrar = viewModel::onPedirBorrar,
                onCancelarBorrar = viewModel::onCancelarBorrar,
                onConfirmarBorrar = viewModel::onConfirmarBorrar,
                onJornada = onJornada,
            ),
    )
}

data class AccionesViaje(
    val onNombre: (String) -> Unit = {},
    val onInicio: (LocalDateTime) -> Unit = {},
    val onFin: (LocalDateTime) -> Unit = {},
    val onObservaciones: (String) -> Unit = {},
    val onGuardar: () -> Unit = {},
    val onPedirBorrar: () -> Unit = {},
    val onCancelarBorrar: () -> Unit = {},
    val onConfirmarBorrar: () -> Unit = {},
    val onJornada: (Long) -> Unit = {},
)

@Composable
fun EditarViajeContent(
    estado: EditarViajeUiState,
    acciones: AccionesViaje,
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
            Text(stringResource(R.string.error_viaje_no_existe), style = MaterialTheme.typography.headlineSmall)
            return@Column
        }
        val resumen = estado.resumen ?: return@Column
        val inicio = estado.inicio ?: return@Column

        TarjetaViaje(resumen, onClick = {}, destacada = resumen.viaje.enCurso)
        if (!resumen.conduccion.isZero) {
            Text(
                stringResource(R.string.resumen_conduccion) + ": " + textoDuracion(resumen.conduccion),
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Text(
            stringResource(R.string.viaje_jornadas),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        if (resumen.jornadas.isEmpty()) {
            Text(stringResource(R.string.viaje_sin_jornadas), style = MaterialTheme.typography.bodyLarge)
        }
        resumen.jornadas.forEach { jornada -> FilaJornadaViaje(jornada, onClick = { acciones.onJornada(jornada.id) }) }

        HorizontalDivider()
        CampoTexto(estado.nombre, acciones.onNombre, stringResource(R.string.viaje_nombre))
        CampoFechaHora(stringResource(R.string.campo_hora_inicio), inicio, acciones.onInicio)
        estado.fin?.let { CampoFechaHora(stringResource(R.string.campo_hora_fin), it, acciones.onFin) }
        CampoTexto(
            valor = estado.observaciones,
            onCambio = acciones.onObservaciones,
            etiqueta = stringResource(R.string.campo_observaciones),
            unaLinea = false,
        )
        ListaErroresViaje(estado.errores)
        BigButton(
            text = stringResource(R.string.accion_guardar_cambios),
            onClick = acciones.onGuardar,
            enabled = estado.puedeGuardar,
            icon = Icons.Filled.Done,
        )
        OutlinedButton(
            onClick = acciones.onPedirBorrar,
            enabled = !estado.guardando,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Delete, contentDescription = null)
            Text(stringResource(R.string.accion_borrar_viaje), modifier = Modifier.padding(start = 8.dp))
        }
    }

    if (estado.confirmandoBorrado) {
        AlertDialog(
            onDismissRequest = acciones.onCancelarBorrar,
            title = { Text(stringResource(R.string.borrar_viaje_titulo)) },
            text = { Text(stringResource(R.string.borrar_viaje_texto)) },
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

/** Una jornada del viaje: día, horas y km. Al tocarla se abre para corregirla. */
@Composable
private fun FilaJornadaViaje(
    jornada: Jornada,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val inicio = jornada.inicio.atZone(jornada.zonaInicio)
    val fin = jornada.fin?.atZone(jornada.zonaFin ?: jornada.zonaInicio)
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(
                        R.string.historial_dia,
                        inicio.format(formatoDiaSemana()),
                        inicio.format(formatoFecha()),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (fin == null) {
                        stringResource(R.string.historial_en_curso, inicio.format(FormatoHora))
                    } else {
                        stringResource(R.string.historial_horas, inicio.format(FormatoHora), fin.format(FormatoHora))
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            jornada.kmRecorridos?.let {
                Text("${textoKm(it)} km", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
