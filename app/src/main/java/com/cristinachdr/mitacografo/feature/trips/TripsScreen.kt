package com.cristinachdr.mitacografo.feature.trips

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.domain.model.ResumenViaje
import com.cristinachdr.mitacografo.domain.model.Viaje
import com.cristinachdr.mitacografo.ui.components.BigButton
import com.cristinachdr.mitacografo.ui.components.CampoTexto
import com.cristinachdr.mitacografo.ui.components.ListaErroresViaje
import com.cristinachdr.mitacografo.ui.formatoFecha
import com.cristinachdr.mitacografo.ui.textoKm
import com.cristinachdr.mitacografo.ui.theme.MiTacografoTheme
import java.time.LocalDateTime
import java.time.ZoneId

@Composable
fun TripsScreen(
    onViaje: (Long) -> Unit,
    viewModel: ViajesViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val formato = formatoFecha()
    val propuesto = stringResource(R.string.viaje_nombre_propuesto, "%s")
    ViajesContent(
        estado = estado,
        acciones =
            AccionesViajes(
                onViaje = onViaje,
                onPedirIniciar = { viewModel.onPedirIniciar { hoy -> propuesto.format(hoy.format(formato)) } },
                onNombreNuevo = viewModel::onNombreNuevo,
                onCancelarIniciar = viewModel::onCancelarIniciar,
                onIniciar = viewModel::onIniciar,
                onPedirFinalizar = viewModel::onPedirFinalizar,
                onCancelarFinalizar = viewModel::onCancelarFinalizar,
                onFinalizar = viewModel::onFinalizar,
            ),
    )
}

data class AccionesViajes(
    val onViaje: (Long) -> Unit = {},
    val onPedirIniciar: () -> Unit = {},
    val onNombreNuevo: (String) -> Unit = {},
    val onCancelarIniciar: () -> Unit = {},
    val onIniciar: () -> Unit = {},
    val onPedirFinalizar: () -> Unit = {},
    val onCancelarFinalizar: () -> Unit = {},
    val onFinalizar: () -> Unit = {},
)

/** El viaje en curso arriba (con sus km), y debajo los anteriores. Tocar uno abre su detalle. */
@Composable
fun ViajesContent(
    estado: ViajesUiState,
    acciones: AccionesViajes,
    modifier: Modifier = Modifier,
) {
    if (estado.cargando) return
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "explicacion") {
            Text(
                stringResource(R.string.viajes_explicacion),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item(key = "en-curso") {
            val enCurso = estado.enCurso
            if (enCurso == null) {
                BigButton(
                    text = stringResource(R.string.accion_iniciar_viaje),
                    onClick = acciones.onPedirIniciar,
                    icon = Icons.Filled.Place,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TarjetaViaje(enCurso, onClick = { acciones.onViaje(enCurso.viaje.id) }, destacada = true)
                    BigButton(
                        text = stringResource(R.string.accion_finalizar_viaje),
                        onClick = acciones.onPedirFinalizar,
                        icon = Icons.Filled.Done,
                    )
                }
            }
        }
        item(key = "titulo-anteriores") {
            Text(
                stringResource(R.string.viajes_anteriores),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 12.dp),
            )
            if (estado.anteriores.isEmpty()) {
                Text(stringResource(R.string.viajes_vacio), style = MaterialTheme.typography.bodyLarge)
            }
        }
        items(estado.anteriores, key = { it.viaje.id }) { viaje ->
            TarjetaViaje(viaje, onClick = { acciones.onViaje(viaje.viaje.id) })
        }
    }

    estado.nombreNuevo?.let { nombre ->
        AlertDialog(
            onDismissRequest = acciones.onCancelarIniciar,
            title = { Text(stringResource(R.string.accion_iniciar_viaje)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CampoTexto(nombre, acciones.onNombreNuevo, stringResource(R.string.viaje_nombre))
                    ListaErroresViaje(estado.errores)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = acciones.onIniciar,
                ) { Text(stringResource(R.string.accion_aceptar)) }
            },
            dismissButton = {
                TextButton(onClick = acciones.onCancelarIniciar) { Text(stringResource(R.string.accion_cancelar)) }
            },
        )
    }
    if (estado.confirmandoFin) {
        AlertDialog(
            onDismissRequest = acciones.onCancelarFinalizar,
            title = { Text(stringResource(R.string.finalizar_viaje_titulo)) },
            text = {
                Text(
                    stringResource(
                        R.string.finalizar_viaje_texto,
                        estado.enCurso
                            ?.viaje
                            ?.nombre
                            .orEmpty(),
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = acciones.onFinalizar) { Text(stringResource(R.string.accion_finalizar_viaje)) }
            },
            dismissButton = {
                TextButton(onClick = acciones.onCancelarFinalizar) { Text(stringResource(R.string.accion_cancelar)) }
            },
        )
    }
}

/** Nombre, fechas, km y jornadas de un viaje. El de en curso, con borde azul como la jornada de Inicio. */
@Composable
internal fun TarjetaViaje(
    resumen: ResumenViaje,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destacada: Boolean = false,
) {
    val viaje = resumen.viaje
    val inicio =
        viaje.inicio
            .atZone(viaje.zonaInicio)
            .toLocalDate()
            .format(formatoFecha())
    val fin =
        viaje.fin
            ?.atZone(viaje.zonaFin ?: viaje.zonaInicio)
            ?.toLocalDate()
            ?.format(formatoFecha())
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border =
            BorderStroke(
                if (destacada) 2.dp else 1.dp,
                if (destacada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
            ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (destacada) {
                Text(
                    stringResource(R.string.viaje_en_curso),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(viaje.nombre, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                when (fin) {
                    null -> stringResource(R.string.viaje_desde, inicio)

                    // Un viaje de un solo día: la fecha una vez.
                    inicio -> inicio

                    else -> stringResource(R.string.historial_horas, inicio, fin)
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                pluralStringResource(
                    R.plurals.viaje_resumen,
                    resumen.numJornadas,
                    textoKm(resumen.km),
                    resumen.numJornadas,
                ),
                style = if (destacada) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            if (resumen.paises.isNotEmpty()) {
                Text(
                    stringResource(R.string.viaje_paises, resumen.paises.joinToString(" → ")),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ViajesPreview() {
    val madrid = ZoneId.of("Europe/Madrid")
    val inicio = LocalDateTime.of(2026, 11, 2, 6, 0).atZone(madrid).toInstant()
    MiTacografoTheme {
        ViajesContent(
            estado =
                ViajesUiState(
                    enCurso =
                        ResumenViaje(
                            Viaje(id = 2, nombre = "Murcia – Kyiv", inicio = inicio, zonaInicio = madrid),
                            emptyList(),
                        ),
                    cargando = false,
                ),
            acciones = AccionesViajes(),
        )
    }
}
