package com.cristinachdr.mitacografo.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.ui.FormatoFecha
import com.cristinachdr.mitacografo.ui.FormatoHora
import com.cristinachdr.mitacografo.ui.components.BigButton
import com.cristinachdr.mitacografo.ui.textoDuracion
import com.cristinachdr.mitacografo.ui.textoKm
import com.cristinachdr.mitacografo.ui.theme.MiTacografoTheme
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Versión "con estado": obtiene el ViewModel de Hilt. */
@Composable
fun HomeScreen(
    onIniciarJornada: () -> Unit,
    onFinalizarJornada: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(state = estado, onIniciarJornada = onIniciarJornada, onFinalizarJornada = onFinalizarJornada)
}

/** Versión "sin estado": solo pinta. Se puede previsualizar y testear sin Hilt. */
@Composable
fun HomeContent(
    state: HomeUiState,
    onIniciarJornada: () -> Unit,
    onFinalizarJornada: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = state.hoy.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))

        val jornada = state.jornada
        if (jornada == null) {
            Text(
                text = stringResource(R.string.inicio_sin_jornada),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            BigButton(
                text = stringResource(R.string.accion_iniciar_jornada),
                onClick = onIniciarJornada,
                icon = Icons.Filled.PlayArrow,
            )
        } else {
            JornadaEnCursoCard(jornada)
            Spacer(Modifier.height(32.dp))
            BigButton(
                text = stringResource(R.string.accion_finalizar_jornada),
                onClick = onFinalizarJornada,
                icon = Icons.Filled.Done,
            )
        }
    }
}

@Composable
private fun JornadaEnCursoCard(
    jornada: JornadaEnCursoUi,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.inicio_jornada_en_curso), style = MaterialTheme.typography.titleMedium)
            Text(
                text = textoDuracion(jornada.transcurrido),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text =
                    stringResource(
                        R.string.inicio_desde,
                        jornada.inicio.format(FormatoHora),
                        jornada.inicio.format(FormatoFecha),
                    ),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.inicio_km_inicio, textoKm(jornada.kmInicio)),
                style = MaterialTheme.typography.bodyLarge,
            )
            val aviso = jornada.proximoAviso
            Text(
                text =
                    if (aviso == null) {
                        stringResource(R.string.inicio_sin_avisos)
                    } else {
                        stringResource(
                            R.string.inicio_proximo_aviso,
                            aviso.hora.format(FormatoHora),
                            textoDuracion(aviso.desdeInicio),
                        )
                    },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeSinJornadaPreview() {
    MiTacografoTheme {
        HomeContent(
            state = HomeUiState(hoy = LocalDate.of(2026, 11, 4)),
            onIniciarJornada = {},
            onFinalizarJornada = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeConJornadaPreview() {
    val madrid = ZoneId.of("Europe/Madrid")
    val inicio = ZonedDateTime.of(2026, 11, 4, 6, 0, 0, 0, madrid)
    MiTacografoTheme {
        HomeContent(
            state =
                HomeUiState(
                    hoy = LocalDate.of(2026, 11, 4),
                    jornada =
                        JornadaEnCursoUi(
                            inicio = inicio,
                            transcurrido = Duration.ofHours(5).plusMinutes(23),
                            kmInicio = 245_310,
                            proximoAviso = AvisoUi(inicio.plusHours(12).plusMinutes(30), Duration.ofMinutes(750)),
                        ),
                ),
            onIniciarJornada = {},
            onFinalizarJornada = {},
        )
    }
}
