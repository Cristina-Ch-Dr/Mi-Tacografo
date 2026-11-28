package com.cristinachdr.mitacografo.feature.home

import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.domain.model.ANTELACIONES_DESPERTADOR
import com.cristinachdr.mitacografo.platform.PermisosAvisos
import com.cristinachdr.mitacografo.ui.FormatoHora
import com.cristinachdr.mitacografo.ui.components.CampoNumero
import com.cristinachdr.mitacografo.ui.formatoDiaSemana
import java.time.Duration
import java.time.ZonedDateTime

/** Las acciones de «Cuándo salir» y el despertador (MT-57). */
data class AccionesDespertador(
    val onPoner: (ZonedDateTime) -> Unit = {},
    val onQuitar: () -> Unit = {},
    val onAntelacion: (Duration) -> Unit = {},
)

/**
 * «Cuándo salir» (MT-57), al terminar la jornada: con 9 h y con 11 h de descanso, a qué hora puede salir y a
 * qué hora despertar, con un ⏰ para poner el despertador. Debajo, cuánto antes despertar.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TarjetaCuandoSalir(
    state: HomeUiState,
    acciones: AccionesDespertador,
    modifier: Modifier = Modifier,
) {
    var otraAntelacion by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.cuando_salir_titulo),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            state.salidas.forEach { salida -> FilaSalida(salida, acciones.onPoner) }
            state.despertador?.let { DespertadorPuesto(it, acciones.onQuitar) }
            HorizontalDivider()
            Text(stringResource(R.string.cuando_salir_antelacion), style = MaterialTheme.typography.bodyLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ANTELACIONES_DESPERTADOR.forEach { opcion ->
                    FilterChip(
                        selected = state.antelacion == opcion,
                        onClick = { acciones.onAntelacion(opcion) },
                        label = { Text(stringResource(R.string.formato_minutos, opcion.toMinutes())) },
                    )
                }
                FilterChip(
                    selected = state.antelacion !in ANTELACIONES_DESPERTADOR,
                    onClick = { otraAntelacion = true },
                    label = {
                        Text(
                            if (state.antelacion in ANTELACIONES_DESPERTADOR) {
                                stringResource(R.string.cuando_salir_otra)
                            } else {
                                stringResource(R.string.formato_minutos, state.antelacion.toMinutes())
                            },
                        )
                    },
                )
            }
        }
    }
    if (otraAntelacion) {
        DialogoAntelacion(
            actual = state.antelacion,
            onAceptar = {
                acciones.onAntelacion(it)
                otraAntelacion = false
            },
            onCancelar = { otraAntelacion = false },
        )
    }
}

/** «Con 9 h de descanso · Salida mié 04:40 · Despertar 03:40» y el ⏰. */
@Composable
private fun FilaSalida(
    salida: SalidaUi,
    onPoner: (ZonedDateTime) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.cuando_salir_descanso, salida.descanso.toHours()),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.cuando_salir_salida, textoDiaYHora(salida.salida)),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                stringResource(R.string.cuando_salir_despertar, textoDiaYHora(salida.despertar)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FilledTonalButton(
            onClick = { onPoner(salida.despertar) },
            enabled = !salida.pasada,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.accion_poner_despertador))
        }
    }
}

/** «⏰ Despertador a las 03:40» con «Quitar», y lo que falta para que suene bien (permisos). */
@Composable
private fun DespertadorPuesto(
    hora: ZonedDateTime,
    onQuitar: () -> Unit,
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.despertador_puesto, textoDiaYHora(hora)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onQuitar) { Text(stringResource(R.string.accion_quitar)) }
        }
        if (!PermisosAvisos.alarmasExactasPermitidas(context)) {
            Aviso(stringResource(R.string.despertador_sin_exactas)) {
                context.startActivity(PermisosAvisos.intentAlarmasExactas(context))
            }
        }
        // Android 14+: sin este permiso no sale la pantalla de alarma encima del bloqueo (sí suena).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            !context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        ) {
            Aviso(stringResource(R.string.despertador_sin_pantalla_completa)) {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                        Uri.parse("package:${context.packageName}"),
                    ),
                )
            }
        }
    }
}

@Composable
private fun Aviso(
    texto: String,
    onArreglar: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        OutlinedButton(onClick = onArreglar, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.accion_activar))
        }
    }
}

/** Otra antelación, en minutos (hasta 240). */
@Composable
private fun DialogoAntelacion(
    actual: Duration,
    onAceptar: (Duration) -> Unit,
    onCancelar: () -> Unit,
) {
    var minutos by remember { mutableStateOf(actual.toMinutes().toString()) }
    val valor = minutos.toLongOrNull()?.takeIf { it in 0..MAXIMO_MINUTOS }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.cuando_salir_antelacion)) },
        text = { CampoNumero(minutos, { minutos = it.take(3) }, stringResource(R.string.campo_minutos)) },
        confirmButton = {
            TextButton(onClick = { valor?.let { onAceptar(Duration.ofMinutes(it)) } }, enabled = valor != null) {
                Text(stringResource(R.string.accion_aceptar))
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text(stringResource(R.string.accion_cancelar)) } },
    )
}

private const val MAXIMO_MINUTOS = 240L

@Composable
private fun textoDiaYHora(hora: ZonedDateTime): String =
    "${hora.format(formatoDiaSemana())} ${hora.format(FormatoHora)}"
