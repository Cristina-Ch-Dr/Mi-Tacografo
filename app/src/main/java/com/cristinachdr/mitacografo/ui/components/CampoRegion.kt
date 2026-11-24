package com.cristinachdr.mitacografo.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.domain.model.PAIS_CON_REGIONES
import com.cristinachdr.mitacografo.domain.model.REGIONES_ESPANA

/**
 * Comunidad autónoma (MT-55): solo sale si el país es España, como en el tacógrafo, que la pide al meter el país.
 * Se elige de la lista («Andalucía (AN)») y se guarda el código.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoRegion(
    pais: String,
    valor: String,
    onCambio: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (pais != PAIS_CON_REGIONES) return
    var expandido by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expandido, onExpandedChange = { expandido = it }, modifier = modifier) {
        OutlinedTextField(
            value = textoRegion(valor),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.campo_region)) },
            placeholder = { Text(stringResource(R.string.region_ninguna)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.region_ninguna), fontStyle = FontStyle.Italic) },
                onClick = {
                    onCambio("")
                    expandido = false
                },
                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
            )
            REGIONES_ESPANA.forEach { codigo ->
                DropdownMenuItem(
                    text = { Text(textoRegion(codigo), style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        onCambio(codigo)
                        expandido = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

/** «Andalucía (AN)»; vacío si no hay. Los nombres son los oficiales, iguales en todos los idiomas. */
fun textoRegion(codigo: String): String = NOMBRES_REGIONES[codigo]?.let { "$it ($codigo)" } ?: codigo

private val NOMBRES_REGIONES: Map<String, String> =
    mapOf(
        "AN" to "Andalucía",
        "AR" to "Aragón",
        "AST" to "Asturias",
        "C" to "Cantabria",
        "CAT" to "Cataluña",
        "CL" to "Castilla y León",
        "CM" to "Castilla-La Mancha",
        "CV" to "Comunitat Valenciana",
        "EXT" to "Extremadura",
        "G" to "Galicia",
        "IB" to "Illes Balears",
        "IC" to "Canarias",
        "LR" to "La Rioja",
        "M" to "Madrid",
        "MU" to "Región de Murcia",
        "NA" to "Navarra",
        "PV" to "País Vasco",
        "CE" to "Ceuta",
        "ME" to "Melilla",
    )
