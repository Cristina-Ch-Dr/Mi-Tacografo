package com.cristinachdr.mitacografo.feature.salary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.domain.model.ModoCobro
import com.cristinachdr.mitacografo.domain.model.ResultadoSalario
import com.cristinachdr.mitacografo.domain.model.ViajeSalario
import com.cristinachdr.mitacografo.ui.components.BigButton
import com.cristinachdr.mitacografo.ui.components.CampoNumero
import com.cristinachdr.mitacografo.ui.components.CampoTexto
import com.cristinachdr.mitacografo.ui.formatoFecha
import com.cristinachdr.mitacografo.ui.idiomaApp
import com.cristinachdr.mitacografo.ui.textoKm
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun SalarioScreen(viewModel: SalarioViewModel = hiltViewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    SalarioContent(
        estado = estado,
        acciones =
            AccionesSalario(
                onAnterior = viewModel::onAnterior,
                onSiguiente = viewModel::onSiguiente,
                onModo = viewModel::onModo,
                onDiaInicio = viewModel::onDiaInicio,
                onNomina = viewModel::onNomina,
                onPorKm = viewModel::onPorKm,
                onPorDia = viewModel::onPorDia,
                onFechaGasto = viewModel::onFechaGasto,
                onDescripcionGasto = viewModel::onDescripcionGasto,
                onImporteGasto = viewModel::onImporteGasto,
                onAnadirGasto = viewModel::onAnadirGasto,
                onBorrarGasto = viewModel::onBorrarGasto,
            ),
    )
}

data class AccionesSalario(
    val onAnterior: () -> Unit = {},
    val onSiguiente: () -> Unit = {},
    val onModo: (ModoCobro) -> Unit = {},
    val onDiaInicio: (String) -> Unit = {},
    val onNomina: (String) -> Unit = {},
    val onPorKm: (String) -> Unit = {},
    val onPorDia: (String) -> Unit = {},
    val onFechaGasto: (LocalDate) -> Unit = {},
    val onDescripcionGasto: (String) -> Unit = {},
    val onImporteGasto: (String) -> Unit = {},
    val onAnadirGasto: () -> Unit = {},
    val onBorrarGasto: (Long) -> Unit = {},
)

/** Lo que gana en el periodo y por qué: el total, el desglose, los viajes que cuentan y los gastos; abajo, cómo cobra. */
@Composable
fun SalarioContent(
    estado: SalarioUiState,
    acciones: AccionesSalario,
    modifier: Modifier = Modifier,
) {
    val resultado = estado.resultado ?: return
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SelectorPeriodo(estado, resultado, acciones)
        Total(resultado)
        Titulo(stringResource(R.string.salario_viajes))
        if (resultado.viajes.isEmpty()) {
            Text(stringResource(R.string.salario_sin_viajes), style = MaterialTheme.typography.bodyLarge)
        }
        resultado.viajes.forEach { FilaViaje(it) }
        Text(
            stringResource(R.string.salario_regla_viajes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider()
        Titulo(stringResource(R.string.salario_gastos))
        resultado.gastos.forEach { gasto ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        listOfNotNull(gasto.fecha.format(formatoFecha()), gasto.descripcion).joinToString(" · "),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Text(
                    textoEuros(gasto.importe),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = { acciones.onBorrarGasto(gasto.id) }) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.accion_borrar))
                }
            }
        }
        NuevoGasto(estado.gasto, acciones)
        HorizontalDivider()
        ComoCobras(estado, resultado, acciones)
    }
}

@Composable
private fun Titulo(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
}

/** ‹ 20 nov 2026 – 19 dic 2026 › */
@Composable
private fun SelectorPeriodo(
    estado: SalarioUiState,
    resultado: ResultadoSalario,
    acciones: AccionesSalario,
) {
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
                resultado.periodo.desde.format(formatoFecha()),
                resultado.periodo.hasta.format(formatoFecha()),
            ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
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
}

/** «1.602,50 €» en grande y, debajo, de dónde sale. */
@Composable
private fun Total(resultado: ResultadoSalario) {
    val config = resultado.config
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.salario_total), style = MaterialTheme.typography.bodyLarge)
            Text(
                textoEuros(resultado.total),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Linea(stringResource(R.string.salario_nomina), textoEuros(config.nomina))
            if (config.modo != ModoCobro.KM) {
                Linea(
                    stringResource(R.string.salario_dietas, resultado.dias, textoEuros(config.porDia)),
                    textoEuros(resultado.importeDietas),
                )
            }
            if (config.modo != ModoCobro.DIETA) {
                Linea(
                    stringResource(
                        R.string.salario_km,
                        textoKm(resultado.km),
                        textoEuros(config.porKm, maxDecimales = 3),
                    ),
                    textoEuros(resultado.importeKm),
                )
            }
            Linea(stringResource(R.string.salario_gastos), textoEuros(resultado.totalGastos))
        }
    }
}

@Composable
private fun Linea(
    concepto: String,
    importe: String,
) {
    Row {
        Text(concepto, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(importe, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FilaViaje(viaje: ViajeSalario) {
    Column {
        Text(viaje.resumen.viaje.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            stringResource(
                R.string.salario_viaje,
                viaje.salida.format(formatoFecha()),
                viaje.vuelta.format(formatoFecha()),
                textoKm(viaje.km),
                viaje.dias,
            ),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

/** Apuntar un gasto: fecha (hoy, si no se cambia), qué fue e importe. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NuevoGasto(
    gasto: GastoNuevo,
    acciones: AccionesSalario,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        var eligiendoFecha by remember { mutableStateOf(false) }
        OutlinedButton(onClick = { eligiendoFecha = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.salario_fecha_gasto, gasto.fecha.format(formatoFecha())))
        }
        CampoTexto(gasto.descripcion, acciones.onDescripcionGasto, stringResource(R.string.salario_descripcion_gasto))
        CampoTexto(
            valor = gasto.importe,
            onCambio = acciones.onImporteGasto,
            etiqueta = stringResource(R.string.salario_importe),
            teclado = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        BigButton(
            text = stringResource(R.string.salario_anadir_gasto),
            onClick = acciones.onAnadirGasto,
            enabled = gasto.valido,
            icon = Icons.Filled.Add,
        )
        if (eligiendoFecha) {
            val estado =
                rememberDatePickerState(
                    initialSelectedDateMillis =
                        gasto.fecha
                            .atStartOfDay(ZoneOffset.UTC)
                            .toInstant()
                            .toEpochMilli(),
                )
            DatePickerDialog(
                onDismissRequest = { eligiendoFecha = false },
                confirmButton = {
                    TextButton(onClick = {
                        estado.selectedDateMillis?.let {
                            acciones.onFechaGasto(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                        }
                        eligiendoFecha = false
                    }) { Text(stringResource(R.string.accion_aceptar)) }
                },
                dismissButton = {
                    TextButton(
                        onClick = { eligiendoFecha = false },
                    ) { Text(stringResource(R.string.accion_cancelar)) }
                },
            ) { DatePicker(state = estado) }
        }
    }
}

/** «Cómo cobras»: día de inicio del periodo, modo, nómina y tarifas. Se guarda al escribir. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ComoCobras(
    estado: SalarioUiState,
    resultado: ResultadoSalario,
    acciones: AccionesSalario,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val modo = resultado.config.modo
        Titulo(stringResource(R.string.salario_como_cobras))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                ModoCobro.KM to R.string.salario_modo_km,
                ModoCobro.DIETA to R.string.salario_modo_dieta,
                ModoCobro.DIETA_Y_KM to R.string.salario_modo_dieta_km,
            ).forEach { (opcion, texto) ->
                FilterChip(
                    selected = modo == opcion,
                    onClick = { acciones.onModo(opcion) },
                    label = { Text(stringResource(texto)) },
                )
            }
        }
        CampoNumero(estado.campos.diaInicio, acciones.onDiaInicio, stringResource(R.string.salario_dia_inicio))
        CampoImporte(estado.campos.nomina, acciones.onNomina, stringResource(R.string.salario_campo_nomina))
        if (modo !=
            ModoCobro.KM
        ) {
            CampoImporte(estado.campos.porDia, acciones.onPorDia, stringResource(R.string.salario_campo_por_dia))
        }
        if (modo !=
            ModoCobro.DIETA
        ) {
            CampoImporte(estado.campos.porKm, acciones.onPorKm, stringResource(R.string.salario_campo_por_km))
        }
    }
}

@Composable
private fun CampoImporte(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
) {
    CampoTexto(
        valor = valor,
        onCambio = onCambio,
        etiqueta = etiqueta,
        teclado = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

/** «1.602,50 €» en el idioma de la app. Las tarifas por km pueden llevar 3 decimales (0,045 €). */
@Composable
private fun textoEuros(
    importe: BigDecimal,
    maxDecimales: Int = 2,
): String {
    val idioma = idiomaApp()
    val formato =
        remember(idioma, maxDecimales) {
            NumberFormat.getNumberInstance(idioma).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = maxDecimales
            }
        }
    return formato.format(importe) + " €"
}
