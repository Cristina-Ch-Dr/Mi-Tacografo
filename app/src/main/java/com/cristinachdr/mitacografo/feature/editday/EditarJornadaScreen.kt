package com.cristinachdr.mitacografo.feature.editday

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.ui.components.BigButton
import com.cristinachdr.mitacografo.ui.components.CampoFechaHora
import com.cristinachdr.mitacografo.ui.components.CampoNumero
import com.cristinachdr.mitacografo.ui.components.CampoPais
import com.cristinachdr.mitacografo.ui.components.CampoTexto
import com.cristinachdr.mitacografo.ui.components.ListaErrores
import com.cristinachdr.mitacografo.ui.textoDuracion
import com.cristinachdr.mitacografo.ui.textoKm
import com.cristinachdr.mitacografo.ui.theme.MiTacografoTheme
import java.time.LocalDateTime
import java.time.ZoneId

@Composable
fun EditarJornadaScreen(
    onTerminado: () -> Unit,
    viewModel: EditarJornadaViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    val alTerminar by rememberUpdatedState(onTerminado)
    LaunchedEffect(estado.terminado) { if (estado.terminado) alTerminar() }

    EditarJornadaContent(
        estado = estado,
        acciones =
            AccionesEditar(
                onInicio = viewModel::onInicio,
                onKmInicio = viewModel::onKmInicio,
                onPaisInicio = viewModel::onPaisInicio,
                onCiudadInicio = viewModel::onCiudadInicio,
                onFin = viewModel::onFin,
                onKmFin = viewModel::onKmFin,
                onPaisFin = viewModel::onPaisFin,
                onCiudadFin = viewModel::onCiudadFin,
                onConduccionHoras = viewModel::onConduccionHoras,
                onConduccionMinutos = viewModel::onConduccionMinutos,
                onObservaciones = viewModel::onObservaciones,
                onGuardar = viewModel::onGuardar,
                onPedirBorrar = viewModel::onPedirBorrar,
                onCancelarBorrar = viewModel::onCancelarBorrar,
                onConfirmarBorrar = viewModel::onConfirmarBorrar,
            ),
    )
}

data class AccionesEditar(
    val onInicio: (LocalDateTime) -> Unit = {},
    val onKmInicio: (String) -> Unit = {},
    val onPaisInicio: (String) -> Unit = {},
    val onCiudadInicio: (String) -> Unit = {},
    val onFin: (LocalDateTime) -> Unit = {},
    val onKmFin: (String) -> Unit = {},
    val onPaisFin: (String) -> Unit = {},
    val onCiudadFin: (String) -> Unit = {},
    val onConduccionHoras: (String) -> Unit = {},
    val onConduccionMinutos: (String) -> Unit = {},
    val onObservaciones: (String) -> Unit = {},
    val onGuardar: () -> Unit = {},
    val onPedirBorrar: () -> Unit = {},
    val onCancelarBorrar: () -> Unit = {},
    val onConfirmarBorrar: () -> Unit = {},
)

@Composable
fun EditarJornadaContent(
    estado: EditarJornadaUiState,
    acciones: AccionesEditar,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val inicio = estado.inicio
        if (estado.noExiste) {
            Text(stringResource(R.string.error_jornada_no_existe), style = MaterialTheme.typography.headlineSmall)
        }
        if (inicio == null) return@Column

        Seccion(stringResource(R.string.editar_seccion_inicio))
        CampoFechaHora(stringResource(R.string.campo_hora_inicio), inicio, acciones.onInicio)
        CampoNumero(
            valor = estado.kmInicio,
            onCambio = acciones.onKmInicio,
            etiqueta = stringResource(R.string.campo_km_inicio),
            error = estado.errorKmInicio,
        )
        CampoPais(
            valor = estado.paisInicio,
            onCambio = acciones.onPaisInicio,
            etiqueta = stringResource(R.string.campo_pais),
            preferencias = estado.paises,
        )
        CampoTexto(estado.ciudadInicio, acciones.onCiudadInicio, stringResource(R.string.campo_ciudad))

        val fin = estado.fin
        if (estado.terminada && fin != null) {
            HorizontalDivider()
            Seccion(stringResource(R.string.editar_seccion_fin))
            CampoFechaHora(stringResource(R.string.campo_hora_fin), fin, acciones.onFin)
            CampoNumero(
                valor = estado.kmFin,
                onCambio = acciones.onKmFin,
                etiqueta = stringResource(R.string.campo_km_fin),
                error = estado.errorKmFin,
            )
            CampoPais(
                valor = estado.paisFin,
                onCambio = acciones.onPaisFin,
                etiqueta = stringResource(R.string.campo_pais),
                preferencias = estado.paises,
            )
            CampoTexto(estado.ciudadFin, acciones.onCiudadFin, stringResource(R.string.campo_ciudad))

            Text(stringResource(R.string.campo_conduccion), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoNumero(
                    valor = estado.conduccionHoras,
                    onCambio = acciones.onConduccionHoras,
                    etiqueta = stringResource(R.string.campo_horas),
                    modifier = Modifier.weight(1f),
                )
                CampoNumero(
                    valor = estado.conduccionMinutos,
                    onCambio = acciones.onConduccionMinutos,
                    etiqueta = stringResource(R.string.campo_minutos),
                    error = estado.errorMinutos,
                    modifier = Modifier.weight(1f),
                )
            }
            Resumen(estado)
        } else {
            Text(
                stringResource(R.string.editar_en_curso_ayuda),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HorizontalDivider()
        CampoTexto(
            valor = estado.observaciones,
            onCambio = acciones.onObservaciones,
            etiqueta = stringResource(R.string.campo_observaciones),
            unaLinea = false,
        )
        ListaErrores(estado.errores)
        BigButton(
            text = stringResource(R.string.accion_guardar_cambios),
            onClick = acciones.onGuardar,
            enabled = estado.puedeGuardar,
            icon = Icons.Filled.Done,
        )
        OutlinedButton(
            onClick = acciones.onPedirBorrar,
            enabled = !estado.guardando,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Delete, contentDescription = null)
            Text(stringResource(R.string.accion_borrar_jornada), modifier = Modifier.padding(start = 8.dp))
        }
    }

    if (estado.confirmandoBorrado) {
        AlertDialog(
            onDismissRequest = acciones.onCancelarBorrar,
            title = { Text(stringResource(R.string.borrar_titulo)) },
            text = { Text(stringResource(R.string.borrar_texto)) },
            confirmButton = {
                TextButton(onClick = acciones.onConfirmarBorrar) {
                    Text(stringResource(R.string.accion_borrar), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = acciones.onCancelarBorrar) { Text(stringResource(R.string.accion_cancelar)) }
            },
        )
    }
}

@Composable
private fun Seccion(
    titulo: String,
    modifier: Modifier = Modifier,
) {
    Text(
        titulo,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}

/** Km recorridos y duración con lo que hay escrito. */
@Composable
private fun Resumen(
    estado: EditarJornadaUiState,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        estado.kmRecorridos?.takeIf { it >= 0 }?.let {
            Text(
                stringResource(R.string.fin_km_recorridos, textoKm(it)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        estado.duracion?.let {
            Text(stringResource(R.string.fin_duracion, textoDuracion(it)), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Preview(showBackground = true, heightDp = 1500)
@Composable
private fun EditarJornadaPreview() {
    val madrid = ZoneId.of("Europe/Madrid")
    val inicio = LocalDateTime.of(2026, 11, 4, 6, 0)
    val jornada =
        Jornada(
            id = 1,
            inicio = inicio.atZone(madrid).toInstant(),
            zonaInicio = madrid,
            kmInicio = 245_310,
            fin = inicio.plusHours(11).atZone(madrid).toInstant(),
            zonaFin = madrid,
            kmFin = 245_872,
        )
    MiTacografoTheme {
        EditarJornadaContent(
            estado =
                EditarJornadaUiState(
                    original = jornada,
                    inicio = inicio,
                    kmInicio = "245310",
                    paisInicio = "ES",
                    fin = inicio.plusHours(11),
                    kmFin = "245872",
                    paisFin = "FR",
                    conduccionHoras = "8",
                    conduccionMinutos = "45",
                    kmRecorridos = 562,
                    cargando = false,
                ),
            acciones = AccionesEditar(),
        )
    }
}
