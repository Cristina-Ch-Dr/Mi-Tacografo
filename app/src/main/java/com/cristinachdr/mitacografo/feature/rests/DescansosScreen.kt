package com.cristinachdr.mitacografo.feature.rests

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.domain.model.ClaseDescanso
import com.cristinachdr.mitacografo.domain.model.Descanso
import com.cristinachdr.mitacografo.domain.model.DescansoClasificado
import com.cristinachdr.mitacografo.domain.model.TipoDescanso
import com.cristinachdr.mitacografo.domain.model.calculado
import com.cristinachdr.mitacografo.ui.FormatoHora
import com.cristinachdr.mitacografo.ui.components.CampoFechaHora
import com.cristinachdr.mitacografo.ui.components.ListaErroresDescanso
import com.cristinachdr.mitacografo.ui.formatoDiaSemana
import com.cristinachdr.mitacografo.ui.formatoFecha
import com.cristinachdr.mitacografo.ui.nombre
import com.cristinachdr.mitacografo.ui.textoDuracion
import com.cristinachdr.mitacografo.ui.theme.MiTacografoTheme
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

@Composable
fun DescansosScreen(
    onDescanso: (Long) -> Unit,
    onNuevo: () -> Unit,
    viewModel: DescansosViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    DescansosContent(
        estado = estado,
        onDescanso = onDescanso,
        onNuevo = onNuevo,
        pausa =
            AccionesPausaTresHoras(
                onPedir = viewModel::onPedirPausaTresHoras,
                onFin = viewModel::onFinPausa,
                onCancelar = viewModel::onCancelarPausa,
                onApuntar = viewModel::onApuntarPausa,
            ),
    )
}

/** Las acciones del diálogo «Pausa de 3 horas» (MT-54). */
data class AccionesPausaTresHoras(
    val onPedir: () -> Unit = {},
    val onFin: (LocalDateTime) -> Unit = {},
    val onCancelar: () -> Unit = {},
    val onApuntar: () -> Unit = {},
)

/**
 * La pestaña Descanso (MT-51): por semanas, del más reciente al más antiguo, los descansos entre una jornada
 * y la siguiente (calculados solos) y lo que se apuntó a mano (pausas y otros descansos), con su clase.
 * Tocar uno apuntado lo abre; los calculados se cambian corrigiendo las horas de las jornadas.
 */
@Composable
fun DescansosContent(
    estado: DescansosUiState,
    onDescanso: (Long) -> Unit,
    onNuevo: () -> Unit,
    modifier: Modifier = Modifier,
    pausa: AccionesPausaTresHoras = AccionesPausaTresHoras(),
) {
    Box(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // Abajo, hueco para que el botón «Añadir» no tape el último.
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "explicacion") {
                Text(
                    stringResource(if (estado.vacio) R.string.descansos_vacio else R.string.descansos_explicacion),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            item(key = "pausa-3-horas") { BotonPausaTresHoras(pausa.onPedir) }
            estado.descansos
                .groupBy { lunesDe(it.descanso) }
                .forEach { (lunes, descansos) ->
                    item(key = "semana-$lunes") {
                        Text(
                            stringResource(
                                R.string.resumen_periodo,
                                lunes.format(formatoFecha()),
                                lunes.plusDays(6).format(formatoFecha()),
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    items(descansos, key = { it.descanso.id }) { clasificado ->
                        val descanso = clasificado.descanso
                        val alTocar: (() -> Unit)? = if (descanso.calculado) null else { -> onDescanso(descanso.id) }
                        FilaDescanso(clasificado, onClick = alTocar)
                    }
                }
        }
        ExtendedFloatingActionButton(
            onClick = onNuevo,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.accion_anadir_descanso)) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
    estado.pausaTresHoras?.let { DialogoPausaTresHoras(it, pausa) }
}

/**
 * «Pausa de 3 horas» (MT-54): para la 1.ª parte del fraccionado 3 + 9, más rápido que «Añadir» con inicio y fin.
 */
@Composable
private fun BotonPausaTresHoras(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(onClick = onClick, modifier = modifier.fillMaxWidth().heightIn(min = 56.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.accion_pausa_3_horas), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.pausa_3_horas_subtitulo), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** «La pausa terminó a las…» y la app apunta las 3 h anteriores. */
@Composable
private fun DialogoPausaTresHoras(
    estado: PausaTresHorasUi,
    acciones: AccionesPausaTresHoras,
) {
    AlertDialog(
        onDismissRequest = acciones.onCancelar,
        title = { Text(stringResource(R.string.accion_pausa_3_horas)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoFechaHora(stringResource(R.string.pausa_3_horas_fin), estado.fin, acciones.onFin)
                Text(
                    stringResource(
                        R.string.pausa_3_horas_se_apunta,
                        estado.inicio.format(FormatoHora),
                        estado.fin.format(FormatoHora),
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
                ListaErroresDescanso(estado.errores)
            }
        },
        confirmButton = {
            TextButton(onClick = acciones.onApuntar, enabled = !estado.guardando) {
                Text(stringResource(R.string.accion_apuntar))
            }
        },
        dismissButton = {
            TextButton(onClick = acciones.onCancelar) { Text(stringResource(R.string.accion_cancelar)) }
        },
    )
}

@Composable
private fun FilaDescanso(
    clasificado: DescansoClasificado,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val descanso = clasificado.descanso
    val inicio = descanso.inicio.atZone(descanso.zonaInicio)
    val fin = descanso.fin?.atZone(descanso.zonaFin ?: descanso.zonaInicio)
    Card(
        modifier = modifier.fillMaxWidth().then(onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier),
        colors =
            if (descanso.enCurso) {
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            } else {
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            },
        // Blanca con borde azul claro, como la tarjeta de jornada de Inicio (MT-44).
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    painterResource(
                        if (descanso.tipo ==
                            TipoDescanso.PAUSA
                        ) {
                            R.drawable.ic_pausa
                        } else {
                            R.drawable.ic_descanso
                        },
                    ),
                    contentDescription = null,
                )
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
                descanso.pais?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
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
            Text(horas, style = MaterialTheme.typography.bodyLarge)
            descanso.duracion?.let {
                Text(
                    stringResource(R.string.descanso_duracion, textoDuracion(it)),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipClase(clasificado)
                if (descanso.calculado) {
                    Text(
                        stringResource(R.string.descanso_entre_jornadas),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** El lunes de la semana en la que empieza el descanso, en su zona. */
private fun lunesDe(descanso: Descanso): LocalDate =
    descanso.inicio
        .atZone(descanso.zonaInicio)
        .toLocalDate()
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/** La clase en una etiqueta: azul para los descansos que cuentan, gris para pausas y los que no cuentan. */
@Composable
private fun ChipClase(
    clasificado: DescansoClasificado,
    modifier: Modifier = Modifier,
) {
    val cuenta =
        clasificado.clase !in setOf(ClaseDescanso.PAUSA, ClaseDescanso.EN_CURSO, ClaseDescanso.INSUFICIENTE)
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = if (cuenta) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        contentColor =
            if (cuenta) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
    ) {
        Text(
            stringResource(clasificado.clase.nombre, textoDuracion(clasificado.deuda)),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun DescansosPreview() {
    val madrid = ZoneId.of("Europe/Madrid")

    fun descanso(
        id: Long,
        inicio: LocalDateTime,
        horas: Long?,
        tipo: TipoDescanso = TipoDescanso.DESCANSO,
    ) = Descanso(
        id = id,
        inicio = inicio.atZone(madrid).toInstant(),
        zonaInicio = madrid,
        fin = horas?.let { inicio.plusHours(it).atZone(madrid).toInstant() },
        zonaFin = horas?.let { madrid },
        tipo = tipo,
        pais = "FR",
    )
    val dia = LocalDateTime.of(2026, 11, 4, 19, 0)
    MiTacografoTheme {
        DescansosContent(
            estado =
                DescansosUiState(
                    descansos =
                        listOf(
                            DescansoClasificado(
                                descanso(3, dia.plusDays(1).withHour(11), null, TipoDescanso.PAUSA),
                                ClaseDescanso.EN_CURSO,
                            ),
                            DescansoClasificado(descanso(2, dia, 11), ClaseDescanso.DIARIO_NORMAL),
                            DescansoClasificado(
                                descanso(1, dia.minusDays(2), 30),
                                ClaseDescanso.SEMANAL_REDUCIDO,
                                Duration.ofHours(15),
                            ),
                        ),
                    cargando = false,
                ),
            onDescanso = {},
            onNuevo = {},
        )
    }
}
