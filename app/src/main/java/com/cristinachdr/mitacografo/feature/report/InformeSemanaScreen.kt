package com.cristinachdr.mitacografo.feature.report

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.domain.rules.Deuda
import com.cristinachdr.mitacografo.domain.rules.EstadoDeuda
import com.cristinachdr.mitacografo.domain.rules.InformeJornada
import com.cristinachdr.mitacografo.domain.rules.Resultado
import com.cristinachdr.mitacografo.ui.formatoDiaSemana
import com.cristinachdr.mitacografo.ui.formatoFecha
import com.cristinachdr.mitacografo.ui.reglas.ChipEstado
import com.cristinachdr.mitacografo.ui.reglas.estado
import com.cristinachdr.mitacografo.ui.reglas.explicacion
import com.cristinachdr.mitacografo.ui.reglas.nombre
import com.cristinachdr.mitacografo.ui.reglas.peor
import com.cristinachdr.mitacografo.ui.reglas.texto
import com.cristinachdr.mitacografo.ui.reglas.textoIntervalo
import com.cristinachdr.mitacografo.ui.textoDuracion

@Composable
fun InformeSemanaScreen(viewModel: InformeSemanaViewModel = hiltViewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    InformeSemanaContent(estado)
}

/**
 * Cada regla con su estado, qué dice el reglamento, el valor medido y, si no se puede saber, por qué.
 * Arriba las de la semana; debajo, las de cada jornada.
 */
@Composable
fun InformeSemanaContent(
    estado: InformeSemanaUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(
                R.string.resumen_periodo,
                estado.lunes.format(formatoFecha()),
                estado.lunes.plusDays(6).format(formatoFecha()),
            ),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            stringResource(R.string.informe_aviso),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val semana = estado.semana
        if (semana == null) {
            if (!estado.cargando) {
                Text(
                    stringResource(R.string.informe_sin_reglas),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            return@Column
        }

        Seccion(stringResource(R.string.informe_reglas_semana))
        semana.resultados.forEach { FilaRegla(it) }
        if (estado.deudas.isNotEmpty()) {
            Seccion(stringResource(R.string.informe_deudas))
            estado.deudas.forEach { FilaDeuda(it, estado) }
        }

        Seccion(stringResource(R.string.informe_reglas_jornadas))
        semana.jornadas.filter { it.resultados.isNotEmpty() }.forEach { TarjetaJornada(it) }
    }
}

@Composable
private fun Seccion(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

/** Una regla: nombre y estado, lo que dice el reglamento, el valor y, si no se sabe, el motivo. */
@Composable
private fun FilaRegla(
    resultado: Resultado,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, resultado.estado.color.copy(alpha = 0.6f)),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(resultado.regla.nombre),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                ChipEstado(resultado.estado)
            }
            Text(stringResource(resultado.regla.explicacion), style = MaterialTheme.typography.bodyMedium)
            valor(resultado)?.let { Text(it, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold) }
            if (resultado is Resultado.NoVerificado) {
                Text(
                    stringResource(resultado.motivo.texto),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** «43 h 08 min (límite 56 h 00 min)» o «1 (como mucho 2)». */
@Composable
private fun valor(resultado: Resultado): String? {
    val detalle = resultado.detalle
    val valor = detalle.valor
    val limite = detalle.limite
    val cuenta = detalle.cuenta
    val maximo = detalle.maximo
    val minimo = detalle.minimo
    return when {
        valor != null && limite != null -> {
            stringResource(
                R.string.informe_valor,
                textoIntervalo(valor),
                textoDuracion(limite),
            )
        }

        valor != null -> {
            textoIntervalo(valor)
        }

        cuenta != null && maximo != null -> {
            stringResource(R.string.informe_cuenta, cuenta, maximo)
        }

        cuenta != null && minimo != null -> {
            stringResource(R.string.informe_cuenta_minimo, cuenta, minimo)
        }

        else -> {
            null
        }
    }
}

@Composable
private fun TarjetaJornada(
    jornada: InformeJornada,
    modifier: Modifier = Modifier,
) {
    val inicio = jornada.jornada.inicio.atZone(jornada.jornada.zonaInicio)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(
                    R.string.historial_dia,
                    inicio.format(formatoDiaSemana()),
                    inicio.format(formatoFecha()),
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            jornada.resultados.peor()?.let { ChipEstado(it, conTexto = false) }
        }
        jornada.resultados.forEach { FilaRegla(it) }
    }
}

@Composable
private fun FilaDeuda(
    deuda: Deuda,
    estado: InformeSemanaUiState,
) {
    val horas = textoDuracion(deuda.horas)
    val limite =
        deuda.fechaLimite
            .atZone(estado.zona)
            .toLocalDate()
            .minusDays(1)
            .format(formatoFecha())
    val texto =
        when (deuda.estado) {
            EstadoDeuda.PENDIENTE -> stringResource(R.string.deuda_pendiente, horas, limite)
            EstadoDeuda.COMPENSADA -> stringResource(R.string.deuda_compensada, horas)
            EstadoDeuda.VENCIDA -> stringResource(R.string.deuda_vencida, horas, limite)
            EstadoDeuda.NO_VERIFICADO -> stringResource(R.string.deuda_no_verificada, horas)
        }
    Text(texto, style = MaterialTheme.typography.bodyLarge)
}
