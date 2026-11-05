package com.cristinachdr.mitacografo.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.domain.model.Continente
import com.cristinachdr.mitacografo.ui.components.CampoPais
import com.cristinachdr.mitacografo.ui.components.CampoTexto
import com.cristinachdr.mitacografo.ui.components.EstadoPermisosAvisos
import com.cristinachdr.mitacografo.ui.theme.MiTacografoTheme

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsContent(
        estado = estado,
        acciones =
            AccionesAjustes(
                onAviso = viewModel::onAviso,
                onEditarAviso = viewModel::onEditarAviso,
                onNuevoAviso = viewModel::onNuevoAviso,
                onEditorHoras = viewModel::onEditorHoras,
                onEditorMinutos = viewModel::onEditorMinutos,
                onGuardarAviso = viewModel::onGuardarAviso,
                onBorrarAviso = viewModel::onBorrarAviso,
                onCancelarAviso = viewModel::onCancelarAviso,
                onRestaurarAvisos = viewModel::onRestaurarAvisos,
                onProbarAviso = viewModel::onProbarAviso,
                onPaisEmpresa = viewModel::onPaisEmpresa,
                onPaisResidencia = viewModel::onPaisResidencia,
                onContinente = viewModel::onContinente,
                onAnadirPaisHabitual = viewModel::onAnadirPaisHabitual,
                onQuitarPaisHabitual = viewModel::onQuitarPaisHabitual,
                onNombreConductor = viewModel::onNombreConductor,
            ),
        permisos = { EstadoPermisosAvisos(soloSiFalta = false) },
        copia = { SeccionCopia() },
    )
}

data class AccionesAjustes(
    val onAviso: (Int, Boolean) -> Unit = { _, _ -> },
    val onEditarAviso: (Int) -> Unit = {},
    val onNuevoAviso: () -> Unit = {},
    val onEditorHoras: (String) -> Unit = {},
    val onEditorMinutos: (String) -> Unit = {},
    val onGuardarAviso: () -> Unit = {},
    val onBorrarAviso: () -> Unit = {},
    val onCancelarAviso: () -> Unit = {},
    val onRestaurarAvisos: () -> Unit = {},
    val onProbarAviso: () -> Unit = {},
    val onPaisEmpresa: (String) -> Unit = {},
    val onPaisResidencia: (String) -> Unit = {},
    val onContinente: (Continente, Boolean) -> Unit = { _, _ -> },
    val onAnadirPaisHabitual: (String) -> Unit = {},
    val onQuitarPaisHabitual: (String) -> Unit = {},
    val onNombreConductor: (String) -> Unit = {},
)

/**
 * @param permisos se pasa como parámetro porque lee el estado real de Android:
 *   así la vista previa y los tests pueden pintar la pantalla sin él.
 */
@Composable
fun SettingsContent(
    estado: AjustesUiState,
    acciones: AccionesAjustes,
    modifier: Modifier = Modifier,
    permisos: @Composable () -> Unit = {},
    copia: @Composable () -> Unit = {},
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
        Seccion(stringResource(R.string.ajustes_seccion_avisos))
        SeccionAvisos(estado = estado, acciones = acciones)
        permisos()
        OutlinedButton(onClick = acciones.onProbarAviso, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.ajustes_probar_aviso))
        }
        if (estado.pruebaProgramada) {
            Text(stringResource(R.string.ajustes_prueba_programada), style = MaterialTheme.typography.bodyMedium)
        }

        HorizontalDivider()
        Seccion(stringResource(R.string.ajustes_seccion_reglas))
        Text(
            stringResource(R.string.ajustes_paises_ayuda),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CampoPais(
            valor = estado.paisEmpresa,
            onCambio = acciones.onPaisEmpresa,
            etiqueta = stringResource(R.string.ajustes_pais_empresa),
            preferencias = estado.preferenciasPaises,
        )
        CampoPais(
            valor = estado.paisResidencia,
            onCambio = acciones.onPaisResidencia,
            etiqueta = stringResource(R.string.ajustes_pais_residencia),
            preferencias = estado.preferenciasPaises,
        )

        HorizontalDivider()
        Seccion(stringResource(R.string.ajustes_seccion_paises))
        PaisesBuscador(estado = estado, acciones = acciones)

        HorizontalDivider()
        Seccion(stringResource(R.string.ajustes_seccion_informes))
        CampoTexto(
            valor = estado.nombreConductor,
            onCambio = acciones.onNombreConductor,
            etiqueta = stringResource(R.string.ajustes_nombre_conductor),
        )

        HorizontalDivider()
        Seccion(stringResource(R.string.ajustes_seccion_copia))
        copia()
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

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun SettingsPreview() {
    MiTacografoTheme {
        SettingsContent(
            estado =
                AjustesUiState(
                    paisEmpresa = "ES",
                    paisResidencia = "ES",
                    paisesHabituales = setOf("DE", "FR"),
                    continentes = setOf(Continente.EUROPA),
                    cargando = false,
                ),
            acciones = AccionesAjustes(),
        )
    }
}
