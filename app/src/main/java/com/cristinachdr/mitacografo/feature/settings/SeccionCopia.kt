package com.cristinachdr.mitacografo.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.domain.model.CopiaSeguridad
import com.cristinachdr.mitacografo.domain.model.ErrorCopia
import com.cristinachdr.mitacografo.ui.formatoFecha
import java.time.Instant
import java.time.ZoneId

/** Ajustes → «Copia de seguridad»: guardar todo en un archivo y restaurarlo en este u otro móvil. */
@Composable
fun SeccionCopia(
    modifier: Modifier = Modifier,
    viewModel: CopiaViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    // Android abre su pantalla para elegir dónde guardar (Descargas, Drive…) o qué archivo abrir.
    val guardar =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            uri?.let { viewModel.onGuardarEn(it.toString()) }
        }
    val abrir =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { viewModel.onAbrir(it.toString()) }
        }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.copia_ayuda),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = { guardar.launch(viewModel.nombreArchivo()) },
            enabled = !estado.trabajando,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Share, contentDescription = null)
            Text(stringResource(R.string.copia_guardar), modifier = Modifier.padding(start = 8.dp))
        }
        OutlinedButton(
            // Algunos gestores de archivos no marcan los .json como JSON: se deja elegir cualquiera
            // y, si no es una copia, se dice.
            onClick = { abrir.launch(arrayOf("application/json", "application/octet-stream", "text/*")) },
            enabled = !estado.trabajando,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Text(stringResource(R.string.copia_restaurar), modifier = Modifier.padding(start = 8.dp))
        }
        estado.mensaje?.let { MensajeCopiaTexto(it) }
    }

    estado.porRestaurar?.let { copia ->
        ConfirmarRestaurar(
            copia = copia,
            jornadasActuales = estado.jornadasActuales,
            onConfirmar = viewModel::onConfirmarRestaurar,
            onCancelar = viewModel::onCancelarRestaurar,
        )
    }
}

@Composable
private fun MensajeCopiaTexto(
    mensaje: MensajeCopia,
    modifier: Modifier = Modifier,
) {
    val (texto, esError) =
        when (mensaje) {
            MensajeCopia.Guardada -> {
                R.string.copia_guardada to false
            }

            MensajeCopia.Restaurada -> {
                R.string.copia_restaurada to false
            }

            MensajeCopia.NoSePudoGuardar -> {
                R.string.copia_error_guardar to true
            }

            MensajeCopia.NoSePudoLeer -> {
                R.string.copia_error_leer to true
            }

            is MensajeCopia.NoValida -> {
                when (mensaje.error) {
                    ErrorCopia.NO_ES_UNA_COPIA -> R.string.copia_error_no_es_copia
                    ErrorCopia.VERSION_MAS_NUEVA -> R.string.copia_error_version
                    ErrorCopia.DATOS_INCORRECTOS -> R.string.copia_error_datos
                } to true
            }
        }
    Text(
        stringResource(texto),
        style = MaterialTheme.typography.bodyLarge,
        color = if (esError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}

@Composable
private fun ConfirmarRestaurar(
    copia: CopiaSeguridad,
    jornadasActuales: Int,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
) {
    val formato = formatoFecha()
    val zona = ZoneId.systemDefault()

    fun fecha(instante: Instant?) = instante?.atZone(zona)?.format(formato).orEmpty()

    val numero = copia.jornadas.size
    val contenido =
        buildString {
            append(stringResource(R.string.copia_resumen_creada, fecha(copia.creada)))
            append('\n')
            append(pluralStringResource(R.plurals.copia_resumen_jornadas, numero, numero))
            if (numero >
                0
            ) {
                append(
                    ' ',
                ).append(stringResource(R.string.copia_resumen_rango, fecha(copia.desde), fecha(copia.hasta)))
            }
            append("\n\n")
            append(
                if (jornadasActuales == 0) {
                    stringResource(R.string.copia_aviso_sin_jornadas)
                } else {
                    pluralStringResource(R.plurals.copia_aviso_reemplazar, jornadasActuales, jornadasActuales)
                },
            )
        }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.copia_confirmar_titulo)) },
        text = { Text(contenido) },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text(stringResource(R.string.copia_confirmar), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text(stringResource(R.string.accion_cancelar)) } },
    )
}
