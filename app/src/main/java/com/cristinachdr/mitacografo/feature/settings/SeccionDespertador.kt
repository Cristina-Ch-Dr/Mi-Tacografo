package com.cristinachdr.mitacografo.feature.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.domain.port.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** El tono del despertador (MT-57), que es de este móvil. */
@HiltViewModel
class DespertadorAjustesViewModel
    @Inject
    constructor(
        private val settings: SettingsRepository,
    ) : ViewModel() {
        /** El tono elegido (`content://…`); `null` = el de alarma del móvil. */
        val tono: StateFlow<String?> =
            settings
                .observar()
                .map { it.tonoDespertador }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        fun onTono(tono: String?) {
            viewModelScope.launch { settings.actualizar { it.copy(tonoDespertador = tono) } }
        }
    }

/**
 * Ajustes → «Despertador»: el tono con el que suena, elegido entre los de alarma del móvil con el selector
 * de Android. Suena con el volumen de alarma, aunque el móvil esté en silencio.
 */
@Composable
fun SeccionDespertador(
    modifier: Modifier = Modifier,
    viewModel: DespertadorAjustesViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val tono by viewModel.tono.collectAsStateWithLifecycle()
    val elegir =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
            if (resultado.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
            viewModel.onTono(tonoElegido(resultado.data)?.toString())
        }
    val porDefecto = stringResource(R.string.despertador_tono_alarma)
    val nombre = remember(tono) { tono?.let { nombreTono(context, it.toUri()) } } ?: porDefecto

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.ajustes_despertador_ayuda),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.despertador_tono_actual, nombre),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
        )
        OutlinedButton(
            onClick = { elegir.launch(selectorTonos(context, tono?.toUri())) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Icon(Icons.Filled.Notifications, contentDescription = null)
            Text(stringResource(R.string.accion_elegir_tono), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

/** El selector de tonos de Android, con los de alarma. */
private fun selectorTonos(
    context: Context,
    actual: Uri?,
): Intent =
    Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, context.getString(R.string.accion_elegir_tono))
        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, actual)

@Suppress("DEPRECATION") // getParcelableExtra con clase es de Android 13
private fun tonoElegido(data: Intent?): Uri? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
    } else {
        data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
    }

private fun nombreTono(
    context: Context,
    tono: Uri,
): String? = runCatching { RingtoneManager.getRingtone(context, tono)?.getTitle(context) }.getOrNull()
