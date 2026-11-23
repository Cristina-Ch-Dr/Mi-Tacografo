package com.cristinachdr.mitacografo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.ui.ErrorCampo
import com.cristinachdr.mitacografo.ui.formatoDiaSemana
import com.cristinachdr.mitacografo.ui.formatoFecha
import java.time.ZonedDateTime

/**
 * «Conducción antes de medianoche del domingo» (MT-50): solo sale si la jornada cruza el lunes a las 00:00.
 * Es opcional; con ella la conducción se reparte exacta entre las dos semanas, como en la libreta.
 *
 * @param medianoche el lunes a las 00:00 que cruza, en la zona de referencia.
 */
@Composable
fun CampoAntesMedianoche(
    medianoche: ZonedDateTime,
    horas: String,
    minutos: String,
    onHoras: (String) -> Unit,
    onMinutos: (String) -> Unit,
    modifier: Modifier = Modifier,
    errorMinutos: ErrorCampo? = null,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.campo_antes_medianoche), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CampoNumero(
                valor = horas,
                onCambio = onHoras,
                etiqueta = stringResource(R.string.campo_horas),
                modifier = Modifier.weight(1f),
            )
            CampoNumero(
                valor = minutos,
                onCambio = onMinutos,
                etiqueta = stringResource(R.string.campo_minutos),
                error = errorMinutos,
                modifier = Modifier.weight(1f),
            )
        }
        // El domingo que termina: el día antes del lunes a las 00:00.
        val domingo = medianoche.minusDays(1)
        Text(
            stringResource(
                R.string.antes_medianoche_ayuda,
                "${domingo.format(formatoDiaSemana())} ${domingo.format(formatoFecha())}",
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
