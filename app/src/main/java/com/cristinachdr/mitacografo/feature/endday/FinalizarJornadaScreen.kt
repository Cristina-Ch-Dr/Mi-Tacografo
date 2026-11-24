package com.cristinachdr.mitacografo.feature.endday

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.cristinachdr.mitacografo.ui.FormatoHora
import com.cristinachdr.mitacografo.ui.components.BigButton
import com.cristinachdr.mitacografo.ui.components.CampoAntesMedianoche
import com.cristinachdr.mitacografo.ui.components.CampoFechaHora
import com.cristinachdr.mitacografo.ui.components.CampoNumero
import com.cristinachdr.mitacografo.ui.components.CampoPais
import com.cristinachdr.mitacografo.ui.components.CampoRegion
import com.cristinachdr.mitacografo.ui.components.CampoTexto
import com.cristinachdr.mitacografo.ui.components.ListaErrores
import com.cristinachdr.mitacografo.ui.formatoFecha
import com.cristinachdr.mitacografo.ui.textoDuracion
import com.cristinachdr.mitacografo.ui.textoKm
import com.cristinachdr.mitacografo.ui.theme.MiTacografoTheme
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

/** Todas las acciones del formulario juntas, para no pasar 8 parámetros sueltos. */
data class AccionesFinalizar(
    val onFechaHora: (LocalDateTime) -> Unit = {},
    val onKm: (String) -> Unit = {},
    val onConduccionHoras: (String) -> Unit = {},
    val onConduccionMinutos: (String) -> Unit = {},
    val onApuntarConduccion: () -> Unit = {},
    val onAntesHoras: (String) -> Unit = {},
    val onAntesMinutos: (String) -> Unit = {},
    val onPais: (String) -> Unit = {},
    val onRegion: (String) -> Unit = {},
    val onCiudad: (String) -> Unit = {},
    val onObservaciones: (String) -> Unit = {},
    val onFinalizar: () -> Unit = {},
)

@Composable
fun FinalizarJornadaContent(
    estado: FinalizarJornadaUiState,
    acciones: AccionesFinalizar,
    modifier: Modifier = Modifier,
    enPantallaPropia: Boolean = true,
) {
    // Dentro de Inicio (MT-51) el formulario no se desplaza solo: lo hace la pantalla entera.
    Column(
        modifier =
            if (enPantallaPropia) {
                modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(16.dp)
            } else {
                modifier.fillMaxWidth()
            },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val jornada = estado.jornada
        if (jornada == null) {
            if (estado.sinJornada) {
                Text(stringResource(R.string.fin_sin_jornada), style = MaterialTheme.typography.headlineSmall)
            }
            return@Column
        }

        // En Inicio la tarjeta de la jornada en curso ya lo dice.
        if (enPantallaPropia) {
            Text(
                text =
                    stringResource(
                        R.string.fin_resumen_inicio,
                        jornada.inicio.atZone(jornada.zonaInicio).format(FormatoHora),
                        jornada.inicio.atZone(jornada.zonaInicio).format(formatoFecha()),
                        textoKm(jornada.kmInicio),
                    ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
        ApuntarConduccionAhora(estado, acciones.onApuntarConduccion)
        estado.medianoche?.let { medianoche ->
            CampoAntesMedianoche(
                medianoche = medianoche,
                horas = estado.antesHoras,
                minutos = estado.antesMinutos,
                onHoras = acciones.onAntesHoras,
                onMinutos = acciones.onAntesMinutos,
                errorMinutos = estado.errorMinutosAntes,
            )
        }

        ResumenEnVivo(kmRecorridos = estado.kmRecorridos, duracion = estado.duracion)

        CampoPais(
            valor = estado.pais,
            onCambio = acciones.onPais,
            etiqueta = stringResource(R.string.campo_pais),
            preferencias = estado.paises,
        )
        CampoRegion(pais = estado.pais, valor = estado.region, onCambio = acciones.onRegion)
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

/**
 * «Apuntar la conducción de ahora» (MT-53): con la jornada en curso, guarda lo que marca el tacógrafo
 * sin finalizarla. Así «Lo que te queda» cuenta con ello. Debajo, cuándo se apuntó por última vez.
 */
@Composable
private fun ApuntarConduccionAhora(
    estado: FinalizarJornadaUiState,
    onApuntar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedButton(
            onClick = onApuntar,
            enabled = estado.puedeApuntar,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.accion_apuntar_conduccion))
        }
        Text(
            estado.conduccionApuntadaEn?.let {
                stringResource(R.string.conduccion_apuntada_a_las, it.format(FormatoHora))
            } ?: stringResource(R.string.apuntar_conduccion_ayuda),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
