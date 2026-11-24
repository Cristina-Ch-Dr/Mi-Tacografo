package com.cristinachdr.mitacografo.feature.startday

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.cristinachdr.mitacografo.domain.usecase.PropuestaDescanso
import com.cristinachdr.mitacografo.ui.FormatoHora
import com.cristinachdr.mitacografo.ui.components.BigButton
import com.cristinachdr.mitacografo.ui.components.CampoFechaHora
import com.cristinachdr.mitacografo.ui.components.CampoNumero
import com.cristinachdr.mitacografo.ui.components.CampoPais
import com.cristinachdr.mitacografo.ui.components.CampoRegion
import com.cristinachdr.mitacografo.ui.components.CampoTexto
import com.cristinachdr.mitacografo.ui.components.ListaErrores
import com.cristinachdr.mitacografo.ui.formatoDiaSemana
import com.cristinachdr.mitacografo.ui.textoDuracion
import com.cristinachdr.mitacografo.ui.theme.MiTacografoTheme
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

@Composable
fun IniciarJornadaContent(
    estado: IniciarJornadaUiState,
    onFechaHora: (LocalDateTime) -> Unit,
    onKm: (String) -> Unit,
    onPais: (String) -> Unit,
    onCiudad: (String) -> Unit,
    onObservaciones: (String) -> Unit,
    onIniciar: () -> Unit,
    modifier: Modifier = Modifier,
    enPantallaPropia: Boolean = true,
    onRegion: (String) -> Unit = {},
) {
    // Dentro de Inicio (MT-51) el formulario no se desplaza solo: lo hace la pantalla entera.
    Column(
        modifier =
            if (enPantallaPropia) {
                modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(16.dp)
            } else {
                modifier.fillMaxWidth()
            },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CampoFechaHora(
            etiqueta = stringResource(R.string.campo_hora_inicio),
            valor = estado.fechaHora,
            onCambio = onFechaHora,
        )
        val propuesta = estado.propuestaDescanso
        val duracion = estado.duracionDescanso
        // En Inicio ya pone arriba desde cuándo descansa: aquí solo en la pantalla propia.
        if (enPantallaPropia && propuesta != null && duracion != null) {
            DescansoPrevioCard(propuesta, duracion)
        }
        CampoNumero(
            valor = estado.km,
            onCambio = onKm,
            etiqueta = stringResource(R.string.campo_km_inicio),
            error = estado.errorKm.takeUnless { estado.km.isEmpty() },
        )
        CampoPais(
            valor = estado.pais,
            onCambio = onPais,
            etiqueta = stringResource(R.string.campo_pais),
            preferencias = estado.paises,
        )
        CampoRegion(pais = estado.pais, valor = estado.region, onCambio = onRegion)
        CampoTexto(valor = estado.ciudad, onCambio = onCiudad, etiqueta = stringResource(R.string.campo_ciudad))
        CampoTexto(
            valor = estado.observaciones,
            onCambio = onObservaciones,
            etiqueta = stringResource(R.string.campo_observaciones),
            unaLinea = false,
        )
        ListaErrores(estado.errores)
        BigButton(
            text = stringResource(R.string.accion_iniciar_jornada),
            onClick = onIniciar,
            enabled = estado.puedeGuardar,
            icon = Icons.Filled.PlayArrow,
        )
    }
}

/** «Descanso desde el martes 19:40: 11 h 20 min». Solo informa: el tiempo entre jornadas ya cuenta como descanso. */
@Composable
private fun DescansoPrevioCard(
    propuesta: PropuestaDescanso,
    duracion: Duration,
    modifier: Modifier = Modifier,
) {
    val desde = propuesta.desde.atZone(propuesta.zona)
    OutlinedCard(modifier = modifier.fillMaxWidth(), border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(
                    R.string.inicio_descanso_previo,
                    "${desde.format(formatoDiaSemana())} ${desde.format(FormatoHora)}",
                    textoDuracion(duracion),
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(stringResource(R.string.inicio_descanso_previo_detalle), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun IniciarJornadaPreview() {
    MiTacografoTheme {
        IniciarJornadaContent(
            estado =
                IniciarJornadaUiState(
                    fechaHora = LocalDateTime.of(2026, 11, 4, 6, 0),
                    zona = ZoneId.of("Europe/Madrid"),
                    km = "245310",
                    pais = "ES",
                    ciudad = "Zaragoza",
                    errorKm = null,
                    cargando = false,
                ),
            onFechaHora = {},
            onKm = {},
            onPais = {},
            onCiudad = {},
            onObservaciones = {},
            onIniciar = {},
        )
    }
}
