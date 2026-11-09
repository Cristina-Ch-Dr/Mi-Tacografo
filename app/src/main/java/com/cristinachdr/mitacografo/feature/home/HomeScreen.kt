package com.cristinachdr.mitacografo.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.ui.FormatoHora
import com.cristinachdr.mitacografo.ui.components.BigButton
import com.cristinachdr.mitacografo.ui.components.EstadoPermisosAvisos
import com.cristinachdr.mitacografo.ui.formatoFecha
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
    HomeContent(
        state = estado,
        onIniciarJornada = onIniciarJornada,
        onFinalizarJornada = onFinalizarJornada,
        onIniciarPausa = viewModel::onIniciarPausa,
        onTerminarPausa = viewModel::onTerminarPausa,
        permisos = { EstadoPermisosAvisos(soloSiFalta = true) },
    )
}

/** Versión "sin estado": solo pinta. Se puede previsualizar y testear sin Hilt. */
@Composable
fun HomeContent(
    state: HomeUiState,
    onIniciarJornada: () -> Unit,
    onFinalizarJornada: () -> Unit,
    modifier: Modifier = Modifier,
    onIniciarPausa: () -> Unit = {},
    onTerminarPausa: () -> Unit = {},
    permisos: @Composable () -> Unit = {},
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
        // Solo aparece si faltan permisos para que los avisos lleguen.
        permisos()
        Spacer(Modifier.height(16.dp))
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
            JornadaEnCursoCard(jornada, state.pausa)
            Spacer(Modifier.height(24.dp))
            BotonPausa(enPausa = state.pausa != null, onIniciar = onIniciarPausa, onTerminar = onTerminarPausa)
            Spacer(Modifier.height(16.dp))
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
    pausa: PausaUi?,
    modifier: Modifier = Modifier,
) {
    // Tarjeta blanca con borde azul del logo (opción B de MT-44); el naranja, solo para el aviso.
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.inicio_jornada_en_curso),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = textoDuracion(jornada.transcurrido),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text =
                    stringResource(
                        R.string.inicio_desde,
                        jornada.inicio.format(FormatoHora),
                        jornada.inicio.format(formatoFecha()),
                    ),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.inicio_km_inicio, textoKm(jornada.kmInicio)),
                style = MaterialTheme.typography.bodyLarge,
            )
            jornada.viaje?.let {
                Text(
                    text = stringResource(R.string.inicio_viaje, it),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
            pausa?.let {
                Text(
                    stringResource(
                        R.string.inicio_pausa_en_curso,
                        it.desde.format(FormatoHora),
                        textoDuracion(it.transcurrido),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
            }
            val aviso = jornada.proximoAviso
            if (aviso == null) {
                Text(
                    text = stringResource(R.string.inicio_sin_avisos),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            } else {
                EtiquetaAviso(
                    hora = stringResource(R.string.inicio_proximo_aviso, aviso.hora.format(FormatoHora)),
                    detalle = stringResource(R.string.inicio_proximo_aviso_jornada, textoDuracion(aviso.desdeInicio)),
                )
            }
        }
    }
}

/** «Iniciar pausa» o «Terminar pausa»: secundario (con borde) para no confundirlo con «Finalizar jornada». */
@Composable
private fun BotonPausa(
    enPausa: Boolean,
    onIniciar: () -> Unit,
    onTerminar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = if (enPausa) onTerminar else onIniciar,
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
    ) {
        Icon(
            painterResource(if (enPausa) R.drawable.ic_reanudar else R.drawable.ic_pausa),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
        )
        Text(
            stringResource(if (enPausa) R.string.accion_terminar_pausa else R.string.accion_iniciar_pausa),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

/** Etiqueta naranja con campana: el naranja de la onda del logo significa «aviso». */
@Composable
private fun EtiquetaAviso(
    hora: String,
    detalle: String,
) {
    Surface(
        modifier = Modifier.padding(top = 4.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.Notifications, contentDescription = null, modifier = Modifier.size(24.dp))
            Column {
                Text(hora, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(detalle, style = MaterialTheme.typography.bodyMedium)
            }
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
