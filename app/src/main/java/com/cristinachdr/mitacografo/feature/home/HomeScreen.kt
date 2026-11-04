package com.cristinachdr.mitacografo.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.ui.components.BigButton
import com.cristinachdr.mitacografo.ui.theme.MiTacografoTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Versión "con estado": obtiene el ViewModel de Hilt. */
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    HomeContent(state = viewModel.uiState, onIniciarJornada = { /* MT-10 */ })
}

/** Versión "sin estado": solo pinta. Se puede previsualizar y testear sin Hilt. */
@Composable
fun HomeContent(
    state: HomeUiState,
    onIniciarJornada: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = state.hoy.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.inicio_sin_jornada),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        BigButton(
            text = stringResource(R.string.accion_iniciar_jornada),
            onClick = onIniciarJornada,
            icon = Icons.Filled.PlayArrow,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeContentPreview() {
    MiTacografoTheme {
        HomeContent(state = HomeUiState(hoy = LocalDate.of(2026, 11, 4)), onIniciarJornada = {})
    }
}
