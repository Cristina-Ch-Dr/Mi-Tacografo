package com.cristinachdr.mitacografo.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.ui.FormatoHora
import com.cristinachdr.mitacografo.ui.formatoFecha
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

private enum class Paso { NINGUNO, FECHA, HORA }

/**
 * Muestra una fecha y hora en grande. Al tocarla se elige primero el día y luego la hora.
 *
 * En el caso normal ("ahora") no hace falta tocarla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoFechaHora(
    etiqueta: String,
    valor: LocalDateTime,
    onCambio: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    var paso by remember { mutableStateOf(Paso.NINGUNO) }
    var fechaElegida by remember { mutableStateOf(valor.toLocalDate()) }

    OutlinedCard(
        onClick = {
            fechaElegida = valor.toLocalDate()
            paso = Paso.FECHA
        },
        modifier = modifier.fillMaxWidth().heightIn(min = 72.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(etiqueta, style = MaterialTheme.typography.labelMedium)
                Text(
                    text = "${valor.format(FormatoHora)} · ${valor.format(formatoFecha())}",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.accion_cambiar))
        }
    }

    when (paso) {
        Paso.NINGUNO -> {
            Unit
        }

        Paso.FECHA -> {
            // El DatePicker trabaja con milisegundos a medianoche UTC.
            val estado =
                rememberDatePickerState(
                    initialSelectedDateMillis = fechaElegida.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
                )
            DatePickerDialog(
                onDismissRequest = { paso = Paso.NINGUNO },
                confirmButton = {
                    TextButton(onClick = {
                        estado.selectedDateMillis?.let { fechaElegida = it.comoFechaUtc() }
                        paso = Paso.HORA
                    }) { Text(stringResource(R.string.accion_aceptar)) }
                },
                dismissButton = {
                    TextButton(onClick = { paso = Paso.NINGUNO }) { Text(stringResource(R.string.accion_cancelar)) }
                },
            ) { DatePicker(state = estado) }
        }

        Paso.HORA -> {
            DialogoHora(
                inicial = valor.toLocalTime(),
                onAceptar = {
                    onCambio(LocalDateTime.of(fechaElegida, it))
                    paso = Paso.NINGUNO
                },
                onCancelar = { paso = Paso.NINGUNO },
            )
        }
    }
}

/** Elegir una hora (en formato de 24 h), con «Aceptar» y «Cancelar». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoHora(
    inicial: LocalTime,
    onAceptar: (LocalTime) -> Unit,
    onCancelar: () -> Unit,
) {
    val estado = rememberTimePickerState(inicial.hour, inicial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onCancelar,
        confirmButton = {
            TextButton(onClick = { onAceptar(LocalTime.of(estado.hour, estado.minute)) }) {
                Text(stringResource(R.string.accion_aceptar))
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text(stringResource(R.string.accion_cancelar)) } },
        text = { TimePicker(state = estado) },
    )
}

private fun Long.comoFechaUtc(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
