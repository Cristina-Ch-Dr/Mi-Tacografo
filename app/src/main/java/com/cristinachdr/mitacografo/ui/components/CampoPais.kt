package com.cristinachdr.mitacografo.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.domain.model.PreferenciasPaises
import com.cristinachdr.mitacografo.ui.idiomaApp
import com.cristinachdr.mitacografo.ui.pais.Pais
import com.cristinachdr.mitacografo.ui.pais.Paises

/**
 * Selector de país con búsqueda: se escribe "Alem" o "DE" y se elige "Alemania (DE)".
 * Se guarda siempre el código de 2 letras.
 *
 * - Al tocar el campo se abre la lista: recientes, habituales y continentes elegidos (ver [preferencias])
 *   y, debajo, «Otros países».
 * - «Hecho» en el teclado elige el primer resultado (escribir "DE" + Hecho basta).
 * - Si se sale del campo sin elegir nada, se queda el país que había.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoPais(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    preferencias: PreferenciasPaises = PreferenciasPaises(),
) {
    val paises = rememberPaises()
    val focusManager = LocalFocusManager.current

    var consulta by remember { mutableStateOf("") }
    var conFoco by remember { mutableStateOf(false) }
    var expandido by remember { mutableStateOf(false) }
    val resultados = remember(paises, consulta, preferencias) { paises.buscar(consulta, preferencias) }

    fun elegir(codigo: String) {
        onCambio(codigo)
        consulta = ""
        expandido = false
        focusManager.clearFocus()
    }

    ExposedDropdownMenuBox(
        expanded = expandido && conFoco,
        onExpandedChange = { expandido = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            // Mientras se busca se ve lo escrito; si no, el país elegido.
            value = if (conFoco) consulta else paises.textoDe(valor),
            onValueChange = {
                consulta = it
                expandido = true
            },
            label = { Text(etiqueta) },
            placeholder = {
                Text(if (valor.isBlank()) stringResource(R.string.pais_buscar) else paises.textoDe(valor))
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido && conFoco) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions =
                KeyboardActions(onDone = {
                    val primero = resultados.todos.firstOrNull()
                    if (consulta.isNotBlank() && primero != null) elegir(primero.codigo) else focusManager.clearFocus()
                }),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                    .onFocusChanged { foco ->
                        if (conFoco && !foco.isFocused) {
                            // Se sale sin elegir: solo cambia si lo escrito es un código exacto ("DE").
                            paises.buscarCodigo(consulta)?.let { onCambio(it.codigo) }
                            consulta = ""
                            expandido = false
                        }
                        if (!conFoco && foco.isFocused) expandido = true
                        conFoco = foco.isFocused
                    },
        )

        ExposedDropdownMenu(expanded = expandido && conFoco, onDismissRequest = { expandido = false }) {
            if (consulta.isBlank() && valor.isNotBlank()) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.pais_ninguno), fontStyle = FontStyle.Italic) },
                    onClick = { elegir("") },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
            if (resultados.todos.isEmpty()) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.pais_sin_resultados)) },
                    onClick = {},
                    enabled = false,
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
            resultados.principales.forEach { OpcionPais(it, ::elegir) }
            if (resultados.otros.isNotEmpty()) {
                // Los que no son de los continentes ni países elegidos en Ajustes: no se esconden.
                HorizontalDivider()
                Text(
                    stringResource(R.string.pais_otros),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier =
                        Modifier
                            .padding(
                                ExposedDropdownMenuDefaults.ItemContentPadding,
                            ).padding(vertical = 8.dp),
                )
                resultados.otros.forEach { OpcionPais(it, ::elegir) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpcionPais(
    pais: Pais,
    onElegir: (String) -> Unit,
) {
    DropdownMenuItem(
        text = { Text(pais.texto, style = MaterialTheme.typography.bodyLarge) },
        onClick = { onElegir(pais.codigo) },
        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
    )
}

/**
 * La lista de países en el idioma en el que se ve la app (no el del móvil): si el móvil está en
 * inglés, la app se ve en español y los países también tienen que salir en español.
 */
@Composable
fun rememberPaises(): Paises {
    val idioma = idiomaApp()
    return remember(idioma) { Paises(idioma) }
}
