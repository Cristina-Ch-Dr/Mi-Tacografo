package com.cristinachdr.mitacografo.feature.endday

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
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.cristinachdr.mitacografo.ui.FormatoFecha
import com.cristinachdr.mitacografo.ui.FormatoHora
import com.cristinachdr.mitacografo.ui.components.BigButton
import com.cristinachdr.mitacografo.ui.components.CampoFechaHora
import com.cristinachdr.mitacografo.ui.components.CampoNumero
import com.cristinachdr.mitacografo.ui.components.CampoPais
import com.cristinachdr.mitacografo.ui.components.CampoTexto
import com.cristinachdr.mitacografo.ui.components.ListaErrores
import com.cristinachdr.mitacografo.ui.textoDuracion
import com.cristinachdr.mitacografo.ui.textoKm
import com.cristinachdr.mitacografo.ui.theme.MiTacografoTheme
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

@Composable
fun FinalizarJornadaScreen(
    onGuardado: () -> Unit,
    viewModel: FinalizarJornadaViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    // rememberUpdatedState: el efecto usa siempre la última versión del callback.
    val alGuardar by rememberUpdatedState(onGuardado)
    LaunchedEffect(estado.guardado) { if (estado.guardado) alGuardar() }

    FinalizarJornadaContent(
        estado = estado,
        acciones =
            AccionesFinalizar(
                onFechaHora = viewModel::onFechaHora,
                onKm = viewModel::onKm,
                onConduccionHoras = viewModel::onConduccionHoras,
                onConduccionMinutos = viewModel::onConduccionMinutos,
                onPais = viewModel::onPais,
                onCiudad = viewModel::onCiudad,
                onObservaciones = viewModel::onObservaciones,
                onFinalizar = viewModel::onFinalizar,
            ),
    )
}

/** Todas las acciones del formulario juntas, para no pasar 8 parámetros sueltos. */
data class AccionesFinalizar(
    val onFechaHora: (LocalDateTime) -> Unit = {},
    val onKm: (String) -> Unit = {},
    val onConduccionHoras: (String) -> Unit = {},
    val onConduccionMinutos: (String) -> Unit = {},
    val onPais: (String) -> Unit = {},
    val onCiudad: (String) -> Unit = {},
    val onObservaciones: (String) -> Unit = {},
    val onFinalizar: () -> Unit = {},
)

@Composable
fun FinalizarJornadaContent(
    estado: FinalizarJornadaUiState,
    acciones: AccionesFinalizar,
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
        val jornada = estado.jornada
        if (jornada == null) {
            if (estado.sinJornada) {
                Text(stringResource(R.string.fin_sin_jornada), style = MaterialTheme.typography.headlineSmall)
            }
            return@Column
        }

        Text(
            text =
                stringResource(
                    R.string.fin_resumen_inicio,
                    jornada.inicio.atZone(jornada.zonaInicio).format(FormatoHora),
                    jornada.inicio.atZone(jornada.zonaInicio).format(FormatoFecha),
                    textoKm(jornada.kmInicio),
                ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CampoFechaHora(
            etiqueta = stringResource(R.string.campo_hora_fin),
            valor = estado.fechaHora,
            onCambio = acciones.onFechaHora,
        )
        CampoNumero(
            valor = estado.km,
            onCambio = acciones.onKm,
            etiqueta = stringResource(R.string.campo_km_fin),
            error = estado.errorKm.takeUnless { estado.km.isEmpty() },
        )

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
        Text(
            stringResource(R.string.fin_conduccion_ayuda),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ResumenEnVivo(kmRecorridos = estado.kmRecorridos, duracion = estado.duracion)

        CampoPais(valor = estado.pais, onCambio = acciones.onPais, etiqueta = stringResource(R.string.campo_pais))
        CampoTexto(
            valor = estado.ciudad,
            onCambio = acciones.onCiudad,
            etiqueta = stringResource(R.string.campo_ciudad),
        )
        CampoTexto(
            valor = estado.observaciones,
            onCambio = acciones.onObservaciones,
            etiqueta = stringResource(R.string.campo_observaciones),
            unaLinea = false,
        )
        ListaErrores(estado.errores)
        BigButton(
            text = stringResource(R.string.accion_finalizar_jornada),
            onClick = acciones.onFinalizar,
            enabled = estado.puedeGuardar,
            icon = Icons.Filled.Done,
        )
    }
}

/** Km recorridos y duración, calculados mientras se escribe. */
@Composable
private fun ResumenEnVivo(
    kmRecorridos: Int?,
    duracion: Duration?,
    modifier: Modifier = Modifier,
) {
    if (kmRecorridos == null && duracion == null) return
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (kmRecorridos != null && kmRecorridos >= 0) {
                Text(
                    stringResource(R.string.fin_km_recorridos, textoKm(kmRecorridos)),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (duracion != null) {
                Text(
                    stringResource(R.string.fin_duracion, textoDuracion(duracion)),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun FinalizarJornadaPreview() {
    val madrid = ZoneId.of("Europe/Madrid")
    val inicio = LocalDateTime.of(2026, 11, 4, 6, 0)
    MiTacografoTheme {
        FinalizarJornadaContent(
            estado =
                FinalizarJornadaUiState(
                    fechaHora = inicio.plusHours(11),
                    zona = madrid,
                    jornada =
                        Jornada(
                            inicio = inicio.atZone(madrid).toInstant(),
                            zonaInicio = madrid,
                            kmInicio = 245_310,
                        ),
                    km = "245872",
                    conduccionHoras = "8",
                    conduccionMinutos = "45",
                    pais = "ES",
                    errorKm = null,
                    kmRecorridos = 562,
                    duracion = Duration.ofHours(11),
                    cargando = false,
                ),
            acciones = AccionesFinalizar(),
        )
    }
}
