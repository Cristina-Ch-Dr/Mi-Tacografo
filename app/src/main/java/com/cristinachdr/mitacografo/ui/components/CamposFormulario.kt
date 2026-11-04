package com.cristinachdr.mitacografo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cristinachdr.mitacografo.domain.model.ErrorJornada
import com.cristinachdr.mitacografo.ui.ErrorCampo
import com.cristinachdr.mitacografo.ui.mensaje

/** Campo de texto grande, con su mensaje de error debajo si lo hay. */
@Composable
fun CampoTexto(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    error: ErrorCampo? = null,
    teclado: KeyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    unaLinea: Boolean = true,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        isError = error != null,
        supportingText = error?.let { { Text(stringResource(it.mensaje)) } },
        keyboardOptions = teclado,
        singleLine = unaLinea,
        minLines = if (unaLinea) 1 else 2,
        textStyle = MaterialTheme.typography.bodyLarge,
        modifier = modifier.fillMaxWidth(),
    )
}

/** Campo solo numérico: abre el teclado de números. */
@Composable
fun CampoNumero(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    error: ErrorCampo? = null,
) {
    CampoTexto(
        valor = valor,
        onCambio = { nuevo -> onCambio(nuevo.filter(Char::isDigit)) },
        etiqueta = etiqueta,
        error = error,
        teclado = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

/**
 * País en 2 letras ("ES", "UA"…).
 *
 * El texto no se transforma mientras se escribe (cambiarlo a mayúsculas "en caliente"
 * confunde al teclado cuando está componiendo una palabra y se pierden letras).
 * El teclado se abre en mayúsculas y el dominio guarda siempre el país en mayúsculas.
 */
@Composable
fun CampoPais(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
) {
    CampoTexto(
        valor = valor,
        onCambio = { nuevo -> onCambio(nuevo.take(2)) },
        etiqueta = etiqueta,
        teclado =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Ascii,
            ),
        modifier = modifier,
    )
}

/** Lista de errores del dominio, en rojo, encima del botón de guardar. */
@Composable
fun ListaErrores(
    errores: Set<ErrorJornada>,
    modifier: Modifier = Modifier,
) {
    if (errores.isEmpty()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        errores.forEach { error ->
            Text(
                text = "• " + stringResource(error.mensaje),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
