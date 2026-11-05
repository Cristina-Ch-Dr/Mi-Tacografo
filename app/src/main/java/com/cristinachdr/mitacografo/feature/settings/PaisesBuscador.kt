package com.cristinachdr.mitacografo.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.domain.model.Continente
import com.cristinachdr.mitacografo.ui.components.CampoPais
import com.cristinachdr.mitacografo.ui.components.rememberPaises

/**
 * Ajustes del selector de país: continentes y países habituales.
 * Lo elegido sale primero al buscar; el resto queda en «Otros países».
 */
@Composable
fun PaisesBuscador(
    estado: AjustesUiState,
    acciones: AccionesAjustes,
    modifier: Modifier = Modifier,
) {
    val paises = rememberPaises()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.ajustes_paises_buscador_ayuda),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(stringResource(R.string.ajustes_continentes), style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Continente.entries.forEach { continente ->
                val elegido = continente in estado.continentes
                FilterChip(
                    selected = elegido,
                    onClick = { acciones.onContinente(continente, !elegido) },
                    label = { Text(stringResource(continente.nombre), style = MaterialTheme.typography.bodyLarge) },
                )
            }
        }

        Text(stringResource(R.string.ajustes_paises_habituales), style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            estado.paisesHabituales.sortedBy { paises.textoDe(it) }.forEach { codigo ->
                val texto = paises.textoDe(codigo)
                InputChip(
                    selected = false,
                    onClick = { acciones.onQuitarPaisHabitual(codigo) },
                    label = { Text(texto, style = MaterialTheme.typography.bodyLarge) },
                    trailingIcon = {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.ajustes_quitar_pais, texto),
                        )
                    },
                )
            }
        }
        // Siempre vacío: al elegir un país se añade a la lista de arriba.
        CampoPais(
            valor = "",
            onCambio = acciones.onAnadirPaisHabitual,
            etiqueta = stringResource(R.string.ajustes_anadir_pais_habitual),
            preferencias = estado.preferenciasPaises.copy(habituales = emptySet(), continentes = emptySet()),
        )
    }
}

private val Continente.nombre: Int
    get() =
        when (this) {
            Continente.EUROPA -> R.string.continente_europa
            Continente.ASIA -> R.string.continente_asia
            Continente.AFRICA -> R.string.continente_africa
            Continente.AMERICA -> R.string.continente_america
            Continente.OCEANIA -> R.string.continente_oceania
        }
