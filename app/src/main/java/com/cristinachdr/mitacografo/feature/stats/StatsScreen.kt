package com.cristinachdr.mitacografo.feature.stats

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.domain.model.ClaseDescanso
import com.cristinachdr.mitacografo.domain.model.KmDia
import com.cristinachdr.mitacografo.domain.model.ResumenPeriodo
import com.cristinachdr.mitacografo.ui.components.BigButton
import com.cristinachdr.mitacografo.ui.formatoDiaSemana
import com.cristinachdr.mitacografo.ui.formatoFecha
import com.cristinachdr.mitacografo.ui.nombre
import com.cristinachdr.mitacografo.ui.textoDuracion
import com.cristinachdr.mitacografo.ui.textoKm
import com.cristinachdr.mitacografo.ui.theme.MiTacografoTheme
import java.time.LocalDate

@Composable
fun StatsScreen(viewModel: ResumenViewModel = hiltViewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val guardar =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
            uri?.let { viewModel.onGuardarEn(it.toString()) }
        }

    // Cuando el PDF está listo, se abre el menú de compartir de Android (WhatsApp, correo, Drive…).
    val compartir = estado.compartir
    val tituloCompartir = stringResource(R.string.pdf_compartir_titulo)
    LaunchedEffect(compartir) {
        if (compartir != null) {
            val envio =
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, compartir.toUri())
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            context.startActivity(Intent.createChooser(envio, tituloCompartir))
            viewModel.onCompartido()
        }
    }

    ResumenContent(
        estado = estado,
        acciones =
            AccionesResumen(
                onSemanas = viewModel::onSemanas,
                onAnterior = viewModel::onAnterior,
                onSiguiente = viewModel::onSiguiente,
                onCompartir = viewModel::onCompartir,
                onGuardar = { guardar.launch(viewModel.nombreArchivo()) },
            ),
    )
}

data class AccionesResumen(
    val onSemanas: (Int) -> Unit = {},
    val onAnterior: () -> Unit = {},
    val onSiguiente: () -> Unit = {},
    val onCompartir: () -> Unit = {},
    val onGuardar: () -> Unit = {},
)

/** La semana o la bisemana, como la suma que se hace en la libreta. */
@Composable
fun ResumenContent(
    estado: ResumenUiState,
    acciones: AccionesResumen,
    modifier: Modifier = Modifier,
) {
    val resumen = estado.resumen
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf(1 to R.string.resumen_semana, 2 to R.string.resumen_bisemana).forEachIndexed { i, (semanas, texto) ->
                SegmentedButton(
                    selected = estado.periodo?.semanas == semanas,
                    onClick = { acciones.onSemanas(semanas) },
                    shape = SegmentedButtonDefaults.itemShape(index = i, count = 2),
                    // Azul, no naranja: el naranja queda para los avisos (MT-44).
                    colors =
                        SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(texto), style = MaterialTheme.typography.titleMedium) }
            }
        }
        if (resumen == null) return@Column

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = acciones.onAnterior) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.resumen_anterior),
                )
            }
            Text(
                stringResource(
                    R.string.resumen_periodo,
                    resumen.desde.format(formatoFecha()),
                    resumen.hasta.format(formatoFecha()),
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = acciones.onSiguiente, enabled = estado.puedeAvanzar) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.resumen_siguiente),
                )
            }
        }

        Totales(resumen)
        if (resumen.semanas > 1) PorSemana(resumen)
        KmPorDia(resumen.kmPorDia)
        Descansos(resumen)
        Text(
            stringResource(R.string.resumen_aviso_reglas),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        BigButton(
            text = stringResource(R.string.accion_compartir_pdf),
            onClick = acciones.onCompartir,
            enabled = !estado.exportando,
            icon = Icons.Filled.Share,
        )
        OutlinedButton(
            onClick = acciones.onGuardar,
            enabled = !estado.exportando,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) { Text(stringResource(R.string.accion_guardar_pdf), style = MaterialTheme.typography.titleMedium) }
        estado.mensaje?.let {
            Text(
                stringResource(if (it == MensajeResumen.PDF_GUARDADO) R.string.pdf_guardado else R.string.pdf_error),
                style = MaterialTheme.typography.bodyLarge,
                color =
                    if (it ==
                        MensajeResumen.PDF_GUARDADO
                    ) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
            )
        }
    }
}

@Composable
private fun Tarjeta(
    titulo: String,
    modifier: Modifier = Modifier,
    contenido: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            contenido()
        }
    }
}

@Composable
private fun Dato(
    etiqueta: String,
    valor: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(etiqueta, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(valor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Totales(resumen: ResumenPeriodo) {
    Tarjeta(stringResource(R.string.resumen_totales)) {
        Dato(stringResource(R.string.resumen_km), textoKm(resumen.km))
        Dato(stringResource(R.string.resumen_jornadas), resumen.numJornadas.toString())
        Dato(stringResource(R.string.resumen_tiempo), textoDuracion(resumen.tiempoJornadas))
        Dato(stringResource(R.string.resumen_conduccion), textoDuracion(resumen.conduccion))
        Dato(stringResource(R.string.resumen_tiempo_descanso), textoDuracion(resumen.tiempoDescanso))
        if (!resumen.conduccionCompleta) {
            Text(
                stringResource(R.string.resumen_conduccion_incompleta),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun PorSemana(resumen: ResumenPeriodo) {
    Tarjeta(stringResource(R.string.resumen_bisemana)) {
        resumen.porSemana.forEach { semana ->
            Dato(
                stringResource(R.string.resumen_semana_de, semana.desde.format(formatoFecha())),
                "${textoKm(semana.km)} km · ${textoDuracion(semana.conduccion)}",
            )
        }
    }
}

/** Barras sencillas: de un vistazo se ven los días fuertes y los de descanso. */
@Composable
private fun KmPorDia(dias: List<KmDia>) {
    val maximo = dias.maxOfOrNull { it.km }?.takeIf { it > 0 } ?: return
    Tarjeta(stringResource(R.string.resumen_km_por_dia)) {
        dias.forEach { dia ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(28.dp)) {
                Text(
                    "${dia.dia.format(formatoDiaSemana())} ${dia.dia.dayOfMonth}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(64.dp),
                )
                Box(Modifier.weight(1f).fillMaxHeight().padding(vertical = 6.dp)) {
                    if (dia.km > 0) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(dia.km.toFloat() / maximo)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)),
                        )
                    }
                }
                Text(
                    textoKm(dia.km),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(64.dp),
                )
            }
        }
    }
}

@Composable
private fun Descansos(resumen: ResumenPeriodo) {
    Tarjeta(stringResource(R.string.resumen_descansos)) {
        val clases =
            listOf(
                ClaseDescanso.DIARIO_NORMAL,
                ClaseDescanso.FRACCIONADO_COMPLETO,
                ClaseDescanso.DIARIO_REDUCIDO,
                ClaseDescanso.SEMANAL_NORMAL,
                ClaseDescanso.SEMANAL_REDUCIDO,
            ).filter { resumen.cuantos(it) > 0 }
        if (clases.isEmpty()) {
            Text(stringResource(R.string.resumen_sin_descansos), style = MaterialTheme.typography.bodyLarge)
        }
        clases.forEach { clase ->
            // El nombre del semanal reducido lleva la deuda: aquí se quita, la deuda va aparte.
            Dato(stringResource(clase.nombre, "").substringBefore(" ·"), resumen.cuantos(clase).toString())
        }
        if (!resumen.deudaSemanal.isZero) {
            Text(
                stringResource(R.string.resumen_deuda, textoDuracion(resumen.deudaSemanal)),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun ResumenPreview() {
    val lunes = LocalDate.of(2026, 11, 2)
    val dias = (0L..6L).map { KmDia(lunes.plusDays(it), listOf(500, 450, 600, 400, 550, 0, 0)[it.toInt()]) }
    MiTacografoTheme {
        ResumenContent(
            estado =
                ResumenUiState(
                    periodo = Periodo(lunes, 1),
                    lunesActual = lunes,
                    resumen = ResumenPeriodo(lunes, 1, emptyList(), emptyList(), dias),
                ),
            acciones = AccionesResumen(),
        )
    }
}
