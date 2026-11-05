package com.cristinachdr.mitacografo.feature.startday

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.ui.components.BigButton
import com.cristinachdr.mitacografo.ui.components.CampoFechaHora
import com.cristinachdr.mitacografo.ui.components.CampoNumero
import com.cristinachdr.mitacografo.ui.components.CampoPais
import com.cristinachdr.mitacografo.ui.components.CampoTexto
import com.cristinachdr.mitacografo.ui.components.ListaErrores
import com.cristinachdr.mitacografo.ui.theme.MiTacografoTheme
import java.time.LocalDateTime
import java.time.ZoneId

@Composable
fun IniciarJornadaScreen(
    onGuardado: () -> Unit,
    viewModel: IniciarJornadaViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    // rememberUpdatedState: el efecto usa siempre la última versión del callback.
    val alGuardar by rememberUpdatedState(onGuardado)
    LaunchedEffect(estado.guardado) { if (estado.guardado) alGuardar() }

    IniciarJornadaContent(
        estado = estado,
        onFechaHora = viewModel::onFechaHora,
        onKm = viewModel::onKm,
        onPais = viewModel::onPais,
        onCiudad = viewModel::onCiudad,
        onObservaciones = viewModel::onObservaciones,
        onIniciar = viewModel::onIniciar,
    )
}

@Composable
fun IniciarJornadaContent(
    estado: IniciarJornadaUiState,
    onFechaHora: (LocalDateTime) -> Unit,
    onKm: (String) -> Unit,
    onPais: (String) -> Unit,
    onCiudad: (String) -> Unit,
    onObservaciones: (String) -> Unit,
    onIniciar: () -> Unit,
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
        CampoFechaHora(
            etiqueta = stringResource(R.string.campo_hora_inicio),
            valor = estado.fechaHora,
            onCambio = onFechaHora,
        )
        CampoNumero(
            valor = estado.km,
            onCambio = onKm,
            etiqueta = stringResource(R.string.campo_km_inicio),
            error = estado.errorKm.takeUnless { estado.km.isEmpty() },
        )
        CampoPais(
            valor = estado.pais,
            onCambio = onPais,
            etiqueta = stringResource(R.string.campo_pais),
            preferencias = estado.paises,
        )
        CampoTexto(valor = estado.ciudad, onCambio = onCiudad, etiqueta = stringResource(R.string.campo_ciudad))
        CampoTexto(
            valor = estado.observaciones,
            onCambio = onObservaciones,
            etiqueta = stringResource(R.string.campo_observaciones),
            unaLinea = false,
        )
        ListaErrores(estado.errores)
        BigButton(
            text = stringResource(R.string.accion_iniciar_jornada),
            onClick = onIniciar,
            enabled = estado.puedeGuardar,
            icon = Icons.Filled.PlayArrow,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun IniciarJornadaPreview() {
    MiTacografoTheme {
        IniciarJornadaContent(
            estado =
                IniciarJornadaUiState(
                    fechaHora = LocalDateTime.of(2026, 11, 4, 6, 0),
                    zona = ZoneId.of("Europe/Madrid"),
                    km = "245310",
                    pais = "ES",
                    ciudad = "Zaragoza",
                    errorKm = null,
                    cargando = false,
                ),
            onFechaHora = {},
            onKm = {},
            onPais = {},
            onCiudad = {},
            onObservaciones = {},
            onIniciar = {},
        )
    }
}
