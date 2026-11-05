package com.cristinachdr.mitacografo.ui.components

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.platform.PermisosAvisos
import com.cristinachdr.mitacografo.ui.theme.EstadoCumple

private data class EstadoPermisos(
    val notificaciones: Boolean,
    val alarmasExactas: Boolean,
    val bateria: Boolean,
) {
    /** Sin estos dos, los avisos no llegan o llegan tarde. La batería es solo recomendable. */
    val faltaAlgoImportante: Boolean get() = !notificaciones || !alarmasExactas
}

private fun leerEstado(context: Context) =
    EstadoPermisos(
        notificaciones = PermisosAvisos.notificacionesPermitidas(context),
        alarmasExactas = PermisosAvisos.alarmasExactasPermitidas(context),
        bateria = PermisosAvisos.bateriaSinRestricciones(context),
    )

/**
 * Comprueba lo que Android necesita para que los avisos lleguen y ofrece un botón para cada cosa.
 *
 * El estado se vuelve a leer cada vez que la pantalla vuelve a primer plano
 * (el usuario va a los ajustes del sistema, activa el permiso y regresa).
 *
 * @param soloSiFalta en Inicio solo aparece si falta algo importante; en Ajustes se ve siempre.
 */
@Composable
fun EstadoPermisosAvisos(
    soloSiFalta: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var estado by remember { mutableStateOf(leerEstado(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { estado = leerEstado(context) }

    // Si el usuario deniega el permiso, la siguiente vez se le lleva a los ajustes de la app.
    var notificacionesDenegadas by rememberSaveable { mutableStateOf(false) }
    val pedirNotificaciones =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
            notificacionesDenegadas = !concedido
            estado = leerEstado(context)
        }

    if (soloSiFalta && !estado.faltaAlgoImportante) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (estado.faltaAlgoImportante) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
            ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (estado.faltaAlgoImportante) {
                Text(stringResource(R.string.permisos_titulo_falta), style = MaterialTheme.typography.titleMedium)
            }
            FilaPermiso(
                texto = stringResource(R.string.permiso_notificaciones),
                concedido = estado.notificaciones,
                onActivar = {
                    if (PermisosAvisos.hayQuePedirNotificaciones && !notificacionesDenegadas) {
                        pedirNotificaciones.launch(PermisosAvisos.PERMISO_NOTIFICACIONES)
                    } else {
                        context.startActivity(PermisosAvisos.intentAjustesApp(context))
                    }
                },
            )
            FilaPermiso(
                texto = stringResource(R.string.permiso_alarmas_exactas),
                concedido = estado.alarmasExactas,
                onActivar = { context.startActivity(PermisosAvisos.intentAlarmasExactas(context)) },
            )
            if (!soloSiFalta) {
                FilaPermiso(
                    texto = stringResource(R.string.permiso_bateria),
                    concedido = estado.bateria,
                    onActivar = { context.startActivity(PermisosAvisos.intentBateria()) },
                )
                if (!estado.bateria) {
                    Text(
                        stringResource(R.string.permiso_bateria_ayuda),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaPermiso(
    texto: String,
    concedido: Boolean,
    onActivar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (concedido) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = null,
            tint = if (concedido) EstadoCumple else MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.width(12.dp))
        Text(texto, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (!concedido) {
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(onClick = onActivar) { Text(stringResource(R.string.accion_activar)) }
        }
    }
}
