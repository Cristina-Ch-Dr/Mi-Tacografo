package com.cristinachdr.mitacografo.feature.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.cristinachdr.mitacografo.domain.model.DescansoPrevio
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.SemanaJornadas
import com.cristinachdr.mitacografo.domain.model.agruparPorSemana
import com.cristinachdr.mitacografo.ui.FormatoHora
import com.cristinachdr.mitacografo.ui.formatoDiaSemana
import com.cristinachdr.mitacografo.ui.formatoFecha
import com.cristinachdr.mitacografo.ui.nombre
import com.cristinachdr.mitacografo.ui.reglas.ChipEstado
import com.cristinachdr.mitacografo.ui.reglas.EstadoRegla
import com.cristinachdr.mitacografo.ui.textoDuracion
import com.cristinachdr.mitacografo.ui.textoKm
import com.cristinachdr.mitacografo.ui.theme.MiTacografoTheme
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Composable
fun HistoryScreen(
    onJornada: (Long) -> Unit,
    onSemana: (LocalDate) -> Unit = {},
    viewModel: HistorialViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    HistorialContent(
        estado = estado,
        onJornada = onJornada,
        onSemana = onSemana,
        onAnterior = viewModel::onAnterior,
        onSiguiente = viewModel::onSiguiente,
    )
}

/**
 * Las jornadas de una semana (de lunes a domingo), como en la libreta, con flechas para ir a otras semanas
 * (MT-48). Tocar una jornada abre su detalle.
 */
@Composable
fun HistorialContent(
    estado: HistorialUiState,
    onJornada: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onSemana: (LocalDate) -> Unit = {},
    onAnterior: () -> Unit = {},
    onSiguiente: () -> Unit = {},
) {
    if (estado.vacio) {
        Text(
            stringResource(R.string.historial_vacio),
            style = MaterialTheme.typography.bodyLarge,
            modifier = modifier.padding(24.dp),
        )
        return
    }
    val semana = estado.semana ?: return
    Column(modifier.fillMaxSize()) {
        SelectorSemana(semana, estado, onAnterior, onSiguiente)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "semana-${semana.lunes}") {
                CabeceraSemana(semana, estado.estadoSemana[semana.lunes], onInforme = { onSemana(semana.lunes) })
            }
            items(semana.jornadas, key = { it.id }) { jornada ->
                FilaJornada(
                    jornada = jornada,
                    estado = estado.estadoJornada[jornada.id],
                    descansoPrevio = estado.descansoPrevio[jornada.id],
                    onClick = { onJornada(jornada.id) },
                )
            }
        }
    }
}

/** ‹ Del 9 nov 2026 al 15 nov 2026 ›, como en el Resumen. Las flechas saltan a la semana con jornadas más cercana. */
@Composable
private fun SelectorSemana(
    semana: SemanaJornadas,
    estado: HistorialUiState,
    onAnterior: () -> Unit,
    onSiguiente: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onAnterior, enabled = estado.hayAnterior) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.resumen_anterior),
            )
        }
        Text(
            stringResource(
                R.string.resumen_periodo,
                semana.lunes.format(formatoFecha()),
                semana.domingo.format(formatoFecha()),
            ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onSiguiente, enabled = estado.haySiguiente) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.resumen_siguiente),
            )
        }
    }
}

@Composable
private fun CabeceraSemana(
    semana: SemanaJornadas,
    estado: EstadoRegla?,
    onInforme: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(bottom = 4.dp)) {
        val conduccion = textoDuracion(semana.conduccion)
        Text(
            if (semana.conduccionCompleta) {
                stringResource(R.string.historial_total_semana, textoKm(semana.km), conduccion)
            } else {
                stringResource(R.string.historial_total_semana_incompleta, textoKm(semana.km), conduccion)
            },
            style = MaterialTheme.typography.bodyLarge,
        )
        // El estado de las reglas de la semana y el acceso a su informe.
        if (estado != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChipEstado(estado)
                TextButton(onClick = onInforme) { Text(stringResource(R.string.accion_ver_informe)) }
            }
        }
    }
}

@Composable
private fun FilaJornada(
    jornada: Jornada,
    estado: EstadoRegla?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    descansoPrevio: DescansoPrevio? = null,
) {
    val inicio = jornada.inicio.atZone(jornada.zonaInicio)
    val fin = jornada.fin?.atZone(jornada.zonaFin ?: jornada.zonaInicio)
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        colors =
            if (jornada.abierta) {
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            } else {
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            },
        // Blanca con borde azul claro, como las demás tarjetas (MT-44).
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(
                        R.string.historial_dia,
                        inicio.format(formatoDiaSemana()),
                        inicio.format(formatoFecha()),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                estado?.let { ChipEstado(it, conTexto = false) }
            }
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
            // Conducción y descanso juntos (MT-46): lo que descansó desde la jornada anterior.
            descansoPrevio?.let { TextoDescansoPrevio(it) }
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

/** «Descanso antes: 11 h 20 min · Diario normal». */
@Composable
private fun TextoDescansoPrevio(previo: DescansoPrevio) {
    val clase = previo.principal.clase
    val nombre = stringResource(clase.nombre, textoDuracion(previo.principal.deuda))
    Text(
        stringResource(R.string.historial_descanso_antes, textoDuracion(previo.total), nombre),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
    )
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
