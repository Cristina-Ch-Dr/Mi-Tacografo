package com.cristinachdr.mitacografo.feature.settings

import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.platform.Notificaciones

/**
 * Sonido de los avisos (MT-45): cuál hay puesto, escucharlo y abrir los ajustes de Android para cambiarlo.
 *
 * El sonido es del canal de notificaciones: la app pone el suyo al crearlo, pero después lo puede
 * cambiar el conductor en los ajustes de Android. Por eso se vuelve a leer del canal cada vez que se
 * vuelve a esta pantalla (al regresar de los ajustes de Android ya sale el nuevo).
 */
@Composable
fun SeccionSonido(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var sonido by remember { mutableStateOf(Notificaciones.sonidoActual(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { sonido = Notificaciones.sonidoActual(context) }

    val nombre =
        when (val actual = sonido) {
            null -> stringResource(R.string.sonido_ninguno)
            Notificaciones.sonido(context) -> stringResource(R.string.sonido_propio)
            else -> remember(actual) { nombreSonido(context, actual) } ?: stringResource(R.string.sonido_otro)
        }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.ajustes_sonido_ayuda),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.ajustes_sonido_actual, nombre),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
        )
        OutlinedButton(
            onClick = { sonido?.let { reproducir(context, it) } },
            enabled = sonido != null,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Text(stringResource(R.string.accion_escuchar_sonido), modifier = Modifier.padding(start = 8.dp))
        }
        OutlinedButton(
            onClick = { context.startActivity(Notificaciones.intentAjustesCanal(context)) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Icon(Icons.Filled.Notifications, contentDescription = null)
            Text(stringResource(R.string.accion_cambiar_sonido), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

/** El nombre que Android da a un tono del móvil («Chime», «Pixie Dust»…), o `null` si no lo sabe. */
private fun nombreSonido(
    context: Context,
    sonido: Uri,
): String? =
    try {
        RingtoneManager.getRingtone(context, sonido)?.getTitle(context)
    } catch (e: SecurityException) {
        // Un tono guardado donde la app no puede leer: se muestra «otro sonido».
        null
    }

/** Suena con el volumen de las notificaciones (el del aviso de verdad), no con el del timbre. */
private fun reproducir(
    context: Context,
    sonido: Uri,
) {
    RingtoneManager.getRingtone(context, sonido)?.apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            audioAttributes =
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
        }
        play()
    }
}
