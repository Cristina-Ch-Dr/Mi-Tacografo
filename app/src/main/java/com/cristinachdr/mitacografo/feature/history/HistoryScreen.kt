package com.cristinachdr.mitacografo.feature.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.SemanaJornadas
import com.cristinachdr.mitacografo.domain.model.agruparPorSemana
import com.cristinachdr.mitacografo.ui.FormatoHora
import com.cristinachdr.mitacografo.ui.formatoDiaSemana
import com.cristinachdr.mitacografo.ui.formatoFecha
import com.cristinachdr.mitacografo.ui.textoDuracion
import com.cristinachdr.mitacografo.ui.textoKm
import com.cristinachdr.mitacografo.ui.theme.MiTacografoTheme
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

@Composable
fun HistoryScreen(
    onJornada: (Long) -> Unit,
    viewModel: HistorialViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    HistorialContent(estado = estado, onJornada = onJornada)
}

/** Las jornadas por semanas (de lunes a domingo), como en la libreta. Tocar una abre su detalle. */
@Composable
fun HistorialContent(
    estado: HistorialUiState,
    onJornada: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (estado.vacio) {
        Text(
            stringResource(R.string.historial_vacio),
            style = MaterialTheme.typography.bodyLarge,
            modifier = modifier.padding(24.dp),
        )
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        estado.semanas.forEach { semana ->
            item(key = "semana-${semana.lunes}") { CabeceraSemana(semana) }
            items(semana.jornadas, key = { it.id }) { jornada ->
                FilaJornada(jornada = jornada, onClick = { onJornada(jornada.id) })
            }
        }
    }
}

@Composable
private fun CabeceraSemana(
    semana: SemanaJornadas,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(top = 12.dp, bottom = 4.dp)) {
        Text(
            stringResource(
                R.string.historial_semana,
                semana.lunes.format(formatoFecha()),
                semana.domingo.format(formatoFecha()),
            ),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        val conduccion = textoDuracion(semana.conduccion)
        Text(
            if (semana.conduccionCompleta) {
                stringResource(R.string.historial_total_semana, textoKm(semana.km), conduccion)
            } else {
                stringResource(R.string.historial_total_semana_incompleta, textoKm(semana.km), conduccion)
            },
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun FilaJornada(
    jornada: Jornada,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val inicio = jornada.inicio.atZone(jornada.zonaInicio)
    val fin = jornada.fin?.atZone(jornada.zonaFin ?: jornada.zonaInicio)
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        colors =
            if (jornada.abierta) {
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            } else {
                CardDefaults.cardColors()
            },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(
                    R.string.historial_dia,
                    inicio.format(formatoDiaSemana()),
                    inicio.format(formatoFecha()),
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            val horas =
                when {
                    fin == null -> {
                        stringResource(R.string.historial_en_curso, inicio.format(FormatoHora))
                    }

                    fin.toLocalDate() == inicio.toLocalDate() -> {
                        stringResource(R.string.historial_horas, inicio.format(FormatoHora), fin.format(FormatoHora))
                    }

                    else -> {
                        stringResource(
                            R.string.historial_horas_otro_dia,
                            inicio.format(FormatoHora),
                            fin.format(FormatoHora),
                            fin.format(formatoFecha()),
                        )
                    }
                }
            val duracion = jornada.duracion?.let { " · " + textoDuracion(it) }.orEmpty()
            Text(horas + duracion, style = MaterialTheme.typography.bodyLarge)

            if (!jornada.abierta) {
                val km = jornada.kmRecorridos?.let { stringResource(R.string.historial_km, textoKm(it)) }.orEmpty()
                val conduccion =
                    jornada.conduccionTacografo?.let {
                        stringResource(
                            R.string.historial_conduccion,
                            textoDuracion(it),
                        )
                    }
                        ?: stringResource(R.string.historial_sin_conduccion)
                Text("$km · $conduccion", style = MaterialTheme.typography.bodyLarge)
            }
            val paises = listOfNotNull(jornada.paisInicio, jornada.paisFin).distinct().joinToString(" → ")
            if (paises.isNotEmpty()) {
                Text(
                    paises,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun HistorialPreview() {
    val madrid = ZoneId.of("Europe/Madrid")

    fun jornada(
        id: Long,
        dia: Int,
        abierta: Boolean = false,
    ): Jornada {
        val inicio = LocalDateTime.of(2026, 11, dia, 6, 0).atZone(madrid).toInstant()
        return Jornada(
            id = id,
            inicio = inicio,
            zonaInicio = madrid,
            kmInicio = 245_000,
            paisInicio = "ES",
            fin = if (abierta) null else inicio.plusSeconds(11 * 3600),
            zonaFin = madrid,
            kmFin = if (abierta) null else 245_560,
            paisFin = if (abierta) null else "FR",
            conduccionTacografo = if (id == 2L) null else Duration.ofHours(9),
        )
    }
    MiTacografoTheme {
        HistorialContent(
            estado =
                HistorialUiState(
                    semanas = agruparPorSemana(listOf(jornada(1, 2), jornada(2, 3), jornada(3, 9, abierta = true))),
                    cargando = false,
                ),
            onJornada = {},
        )
    }
}
