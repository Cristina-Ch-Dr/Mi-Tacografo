package com.cristinachdr.mitacografo.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.domain.model.AvisoConfig
import com.cristinachdr.mitacografo.domain.model.ErrorAviso
import com.cristinachdr.mitacografo.ui.components.CampoNumero
import com.cristinachdr.mitacografo.ui.textoDuracion
import java.time.Duration

/** Ajustes → «Avisos»: activar, cambiar la hora, añadir, quitar y volver a los de por defecto. */
@Composable
fun SeccionAvisos(
    estado: AjustesUiState,
    acciones: AccionesAjustes,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(R.string.ajustes_avisos_ayuda),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        estado.avisos.forEachIndexed { indice, aviso ->
            FilaAviso(
                aviso = aviso,
                onActivo = { acciones.onAviso(indice, it) },
                onEditar = { acciones.onEditarAviso(indice) },
            )
        }
        if (estado.avisos.isEmpty()) {
            Text(stringResource(R.string.ajustes_sin_avisos), style = MaterialTheme.typography.bodyLarge)
        }
        OutlinedButton(
            onClick = acciones.onNuevoAviso,
            enabled = estado.caben,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(stringResource(R.string.ajustes_anadir_aviso), modifier = Modifier.padding(start = 8.dp))
        }
        if (!estado.caben) {
            Text(
                pluralStringResource(R.plurals.ajustes_maximo_avisos, AvisoConfig.MAX_AVISOS, AvisoConfig.MAX_AVISOS),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!estado.avisosPorDefecto) {
            OutlinedButton(onClick = acciones.onRestaurarAvisos, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Text(stringResource(R.string.ajustes_restaurar_avisos), modifier = Modifier.padding(start = 8.dp))
            }
        }
        Text(
            stringResource(R.string.ajustes_avisos_por_defecto),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    estado.editorAviso?.let { EditorAvisoDialogo(it, acciones) }
}

@Composable
private fun FilaAviso(
    aviso: AvisoConfig,
    onActivo: (Boolean) -> Unit,
    onEditar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val explicacion =
        when (aviso.desdeInicio) {
            Duration.ofMinutes(750) -> R.string.aviso_explicacion_1230
            Duration.ofHours(13) -> R.string.aviso_explicacion_13
            Duration.ofMinutes(870) -> R.string.aviso_explicacion_1430
            Duration.ofHours(15) -> R.string.aviso_explicacion_15
            else -> R.string.aviso_explicacion_propio
        }
    val editar = stringResource(R.string.ajustes_cambiar_hora_aviso)
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        // Toda la parte del texto se puede tocar para cambiar la hora (es más fácil que un icono pequeño).
        Row(
            Modifier
                .weight(1f)
                .clickable(onClickLabel = editar, onClick = onEditar)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(textoDuracion(aviso.desdeInicio), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(explicacion),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Filled.Edit, contentDescription = editar, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = aviso.activo, onCheckedChange = onActivo)
    }
}

@Composable
private fun EditorAvisoDialogo(
    editor: EditorAviso,
    acciones: AccionesAjustes,
) {
    val titulo = if (editor.indice == null) R.string.aviso_nuevo_titulo else R.string.aviso_editar_titulo
    AlertDialog(
        onDismissRequest = acciones.onCancelarAviso,
        title = { Text(stringResource(titulo)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.aviso_editor_ayuda), style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CampoNumero(
                        valor = editor.horas,
                        onCambio = acciones.onEditorHoras,
                        etiqueta = stringResource(R.string.campo_horas),
                        modifier = Modifier.weight(1f),
                    )
                    CampoNumero(
                        valor = editor.minutos,
                        onCambio = acciones.onEditorMinutos,
                        etiqueta = stringResource(R.string.campo_minutos),
                        error = editor.errorMinutos,
                        modifier = Modifier.weight(1f),
                    )
                }
                editor.error?.let {
                    Text(
                        stringResource(it.mensaje),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (editor.indice != null) {
                    TextButton(onClick = acciones.onBorrarAviso) {
                        Text(stringResource(R.string.aviso_borrar), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = acciones.onGuardarAviso) { Text(stringResource(R.string.accion_aceptar)) }
        },
        dismissButton = {
            TextButton(onClick = acciones.onCancelarAviso) { Text(stringResource(R.string.accion_cancelar)) }
        },
    )
}

private val ErrorAviso.mensaje: Int
    get() =
        when (this) {
            ErrorAviso.FUERA_DE_RANGO -> R.string.error_aviso_rango
            ErrorAviso.REPETIDO -> R.string.error_aviso_repetido
            ErrorAviso.DEMASIADOS -> R.string.error_aviso_demasiados
        }
