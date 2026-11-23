package com.cristinachdr.mitacografo.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.domain.rules.Intervalo
import com.cristinachdr.mitacografo.feature.endday.AccionesFinalizar
import com.cristinachdr.mitacografo.feature.endday.FinalizarJornadaContent
import com.cristinachdr.mitacografo.feature.endday.FinalizarJornadaViewModel
import com.cristinachdr.mitacografo.feature.startday.IniciarJornadaContent
import com.cristinachdr.mitacografo.feature.startday.IniciarJornadaViewModel
import com.cristinachdr.mitacografo.ui.FormatoHora
import com.cristinachdr.mitacografo.ui.components.EstadoPermisosAvisos
import com.cristinachdr.mitacografo.ui.formatoDiaSemana
import com.cristinachdr.mitacografo.ui.formatoFecha
import com.cristinachdr.mitacografo.ui.idiomaApp
import com.cristinachdr.mitacografo.ui.reglas.ChipEstado
import com.cristinachdr.mitacografo.ui.textoDuracion
import com.cristinachdr.mitacografo.ui.textoKm
import com.cristinachdr.mitacografo.ui.theme.MiTacografoTheme
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Versión "con estado": obtiene los ViewModels de Hilt. */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onAjustes: () -> Unit = {},
    onInforme: (LocalDate) -> Unit = {},
    onAnadirJornada: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val semana by viewModel.semana.collectAsStateWithLifecycle()
    HomeContent(
        state = estado,
        modifier = modifier,
        onIniciarPausa = viewModel::onIniciarPausa,
        onTerminarPausa = viewModel::onTerminarPausa,
        onAjustes = onAjustes,
        permisos = { EstadoPermisosAvisos(soloSiFalta = true) },
        semana = semana,
        onInforme = onInforme,
        onAnadirJornada = onAnadirJornada,
        formulario = {
            // Un formulario nuevo para cada jornada: al iniciar o finalizar, cambia la clave y se vacía.
            val jornada = estado.jornada
            if (jornada == null) {
                FormularioIniciar(clave = "iniciar-${estado.descanso?.desde}", ahora = estado.ahora)
            } else {
                FormularioFinalizar(clave = "finalizar-${jornada.inicio}", ahora = estado.ahora)
            }
        },
    )
}

/** «Iniciar jornada» dentro de Inicio (MT-51), con los mismos campos y validaciones que tenía su pantalla. */
@Composable
private fun FormularioIniciar(
    clave: String,
    ahora: Instant,
    viewModel: IniciarJornadaViewModel = hiltViewModel(key = clave),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(ahora) { viewModel.onPasaElTiempo() }
    IniciarJornadaContent(
        estado = estado,
        onFechaHora = viewModel::onFechaHora,
        onKm = viewModel::onKm,
        onPais = viewModel::onPais,
        onCiudad = viewModel::onCiudad,
        onObservaciones = viewModel::onObservaciones,
        onIniciar = viewModel::onIniciar,
        enPantallaPropia = false,
    )
}

/** «Finalizar jornada» dentro de Inicio (MT-51). */
@Composable
private fun FormularioFinalizar(
    clave: String,
    ahora: Instant,
    viewModel: FinalizarJornadaViewModel = hiltViewModel(key = clave),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(ahora) { viewModel.onPasaElTiempo() }
    FinalizarJornadaContent(
        estado = estado,
        acciones =
            AccionesFinalizar(
                onFechaHora = viewModel::onFechaHora,
                onKm = viewModel::onKm,
                onConduccionHoras = viewModel::onConduccionHoras,
                onConduccionMinutos = viewModel::onConduccionMinutos,
                onApuntarConduccion = viewModel::onApuntarConduccion,
                onAntesHoras = viewModel::onAntesHoras,
                onAntesMinutos = viewModel::onAntesMinutos,
                onPais = viewModel::onPais,
                onCiudad = viewModel::onCiudad,
                onObservaciones = viewModel::onObservaciones,
                onFinalizar = viewModel::onFinalizar,
            ),
        enPantallaPropia = false,
    )
}

/**
 * Versión "sin estado": solo pinta. Se puede previsualizar y testear sin Hilt.
 *
 * Organizada como la app que se hizo el conductor (MT-51): arriba los avisos, luego la jornada con el formulario
 * para iniciarla o finalizarla ahí mismo, y debajo lo que le queda (conducción, ampliaciones, reducidos,
 * semanal).
 */
@Composable
fun HomeContent(
    state: HomeUiState,
    modifier: Modifier = Modifier,
    onIniciarPausa: () -> Unit = {},
    onTerminarPausa: () -> Unit = {},
    onAjustes: () -> Unit = {},
    permisos: @Composable () -> Unit = {},
    semana: ResumenSemanaUi? = null,
    onInforme: (LocalDate) -> Unit = {},
    onAnadirJornada: () -> Unit = {},
    formulario: @Composable () -> Unit = {},
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Solo aparece si faltan permisos para que los avisos lleguen.
        permisos()
        avisosInicio(state, semana?.plazoSemanal).forEach { TarjetaAviso(it) }
        if (state.faltanPaises) AvisoPaises(onAjustes)
        Text(
            text = state.hoy.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(idiomaApp())),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        val jornada = state.jornada
        if (jornada == null) {
            SinJornadaCard(state.descanso)
            TarjetaFormulario(stringResource(R.string.accion_iniciar_jornada), formulario)
        } else {
            JornadaEnCursoCard(jornada, state.pausa)
            BotonPausa(enPausa = state.pausa != null, onIniciar = onIniciarPausa, onTerminar = onTerminarPausa)
            TarjetaFormulario(stringResource(R.string.accion_finalizar_jornada), formulario)
        }
        // Para pasar lo que está en el tacógrafo o en la libreta pero no en la app (MT-47, MT-52).
        OutlinedButton(onClick = onAnadirJornada, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(stringResource(R.string.accion_anadir_jornada_pasada), modifier = Modifier.padding(start = 8.dp))
        }
        semana?.let {
            LoQueQueda(
                it,
                enCurso = jornada != null,
                conduccionApuntadaEn = jornada?.conduccionApuntadaEn,
                onInforme = { onInforme(it.lunes) },
            )
        }
    }
}

/** Sin jornada en curso: desde cuándo descansa (el tiempo desde la última jornada ya es descanso, MT-46). */
@Composable
private fun SinJornadaCard(
    descanso: PausaUi?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.inicio_sin_jornada),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            descanso?.let {
                Text(
                    stringResource(
                        R.string.inicio_descansando,
                        it.desde.format(formatoDiaSemana()) + " " + it.desde.format(FormatoHora),
                        textoDuracion(it.transcurrido),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** El formulario de iniciar o finalizar, en una tarjeta con su título. */
@Composable
private fun TarjetaFormulario(
    titulo: String,
    formulario: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                titulo,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            formulario()
        }
    }
}

/** Un aviso de arriba: naranja si es para prepararse, rojo si ya se ha pasado. Siempre con texto. */
@Composable
private fun TarjetaAviso(
    aviso: AvisoInicio,
    modifier: Modifier = Modifier,
) {
    val texto =
        when (aviso) {
            AvisoInicio.Jornada13h -> {
                stringResource(R.string.aviso_jornada_13h)
            }

            AvisoInicio.Jornada15h -> {
                stringResource(R.string.aviso_jornada_15h)
            }

            is AvisoInicio.SemanalPronto -> {
                stringResource(R.string.aviso_semanal_pronto, textoDiaHora(aviso.plazo))
            }

            is AvisoInicio.SemanalVencido -> {
                stringResource(R.string.aviso_semanal_vencido, textoDiaHora(aviso.plazo))
            }
        }
    val colores = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (aviso.grave) colores.errorContainer else colores.secondaryContainer,
        contentColor = if (aviso.grave) colores.onErrorContainer else colores.onSecondaryContainer,
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(painterResource(R.drawable.ic_aviso), contentDescription = null, modifier = Modifier.size(28.dp))
            Text(texto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

/** «sáb 21 nov 2026 18:00». */
@Composable
private fun textoDiaHora(hora: ZonedDateTime): String =
    "${hora.format(formatoDiaSemana())} ${hora.format(formatoFecha())} ${hora.format(FormatoHora)}"

/**
 * Lo que le queda, en grande y con barras, como en su app: lo que antes sumaba a mano en la libreta.
 * Todo sale del motor de reglas; si la conducción no se sabe exacta, se cuenta con lo más que pudo ser.
 */
@Composable
private fun LoQueQueda(
    semana: ResumenSemanaUi,
    enCurso: Boolean,
    conduccionApuntadaEn: ZonedDateTime?,
    onInforme: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.inicio_queda_titulo),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                semana.estado?.let { ChipEstado(it) }
            }
            val nota = notaContador(enCurso, conduccionApuntadaEn)
            ContadorTiempo(stringResource(R.string.contador_semana), semana.conduccion, LIMITE_SEMANA, nota)
            ContadorTiempo(
                stringResource(R.string.contador_bisemana),
                semana.conduccion + semana.conduccionAnterior,
                LIMITE_DOS_SEMANAS,
                nota,
            )
            ContadorCuenta(stringResource(R.string.contador_ampliaciones), semana.ampliadas, MAXIMO_AMPLIADAS)
            semana.reducidos?.let {
                ContadorCuenta(stringResource(R.string.contador_reducidos), it, MAXIMO_REDUCIDOS)
            }
            semana.plazoSemanal?.let { plazo ->
                Column {
                    Text(stringResource(R.string.contador_semanal), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        textoDiaHora(plazo),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            semana.deuda?.let { deuda ->
                val hasta =
                    deuda.fechaLimite
                        .atZone(deuda.semanal.zonaInicio)
                        .toLocalDate()
                        .minusDays(1)
                Text(
                    stringResource(R.string.semana_deuda, textoDuracion(deuda.horas), hasta.format(formatoFecha())),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            OutlinedButton(onClick = onInforme, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.accion_ver_informe))
            }
        }
    }
}

private val LIMITE_SEMANA: Duration = Duration.ofHours(56)
private val LIMITE_DOS_SEMANAS: Duration = Duration.ofHours(90)
private const val MAXIMO_AMPLIADAS = 2
private const val MAXIMO_REDUCIDOS = 3

/**
 * La nota de debajo de los contadores cuando la conducción no se sabe exacta. Con la jornada en curso,
 * si apuntó la de «hasta ahora» (MT-53), se cuenta desde esa hora; si no, se le recuerda que puede apuntarla.
 */
@Composable
private fun notaContador(
    enCurso: Boolean,
    conduccionApuntadaEn: ZonedDateTime?,
): String =
    when {
        !enCurso -> {
            stringResource(R.string.contador_nota_falta)
        }

        conduccionApuntadaEn != null -> {
            stringResource(
                R.string.contador_nota_apuntada,
                conduccionApuntadaEn.format(FormatoHora),
            )
        }

        else -> {
            stringResource(R.string.contador_nota_hoy)
        }
    }

/**
 * «43 h 20 min», una barra y «Restan 12 h 40 min» (o «Pasado en …», en rojo).
 *
 * Cuenta lo que ya está apuntado (lo mínimo seguro). Con la jornada en curso la app no sabe cuánto ha
 * conducido desde la última vez que lo apuntó, y contar lo máximo posible le diría que se ha pasado sin
 * ser verdad: se le avisa con [nota].
 */
@Composable
private fun ContadorTiempo(
    titulo: String,
    valor: Intervalo,
    limite: Duration,
    nota: String,
    modifier: Modifier = Modifier,
) {
    val apuntado = valor.min
    val queda = limite - apuntado
    val pasado = queda.isNegative
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(titulo, style = MaterialTheme.typography.bodyLarge)
        Text(textoDuracion(apuntado), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Barra(apuntado.toMinutes().toFloat() / limite.toMinutes(), pasado)
        Text(
            if (pasado) {
                stringResource(R.string.contador_pasado, textoDuracion(queda.negated()))
            } else {
                stringResource(R.string.contador_restan, textoDuracion(queda))
            },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (pasado) FontWeight.Bold else FontWeight.Normal,
            color = if (pasado) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        if (!valor.exacto) {
            Text(
                nota,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** «1 / 2», una barra y «Restan 1». */
@Composable
private fun ContadorCuenta(
    titulo: String,
    cuenta: Int,
    maximo: Int,
    modifier: Modifier = Modifier,
) {
    val pasado = cuenta > maximo
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(titulo, style = MaterialTheme.typography.bodyLarge)
        Text("$cuenta / $maximo", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Barra(cuenta.toFloat() / maximo, pasado)
        Text(
            if (pasado) {
                stringResource(R.string.contador_pasado, (cuenta - maximo).toString())
            } else {
                stringResource(R.string.contador_restan, (maximo - cuenta).toString())
            },
            style = MaterialTheme.typography.bodyLarge,
            color = if (pasado) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun Barra(
    fraccion: Float,
    pasado: Boolean,
    modifier: Modifier = Modifier,
) {
    LinearProgressIndicator(
        progress = { fraccion.coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth().height(10.dp),
        color = if (pasado) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.primaryContainer,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

@Composable
private fun JornadaEnCursoCard(
    jornada: JornadaEnCursoUi,
    pausa: PausaUi?,
    modifier: Modifier = Modifier,
) {
    // Tarjeta blanca con borde azul del logo (opción B de MT-44); el naranja, solo para el aviso.
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.inicio_jornada_en_curso),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = textoDuracion(jornada.transcurrido),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text =
                    stringResource(
                        R.string.inicio_desde,
                        jornada.inicio.format(FormatoHora),
                        jornada.inicio.format(formatoFecha()),
                    ),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.inicio_km_inicio, textoKm(jornada.kmInicio)),
                style = MaterialTheme.typography.bodyLarge,
            )
            jornada.viaje?.let {
                Text(
                    text = stringResource(R.string.inicio_viaje, it),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
            pausa?.let {
                Text(
                    stringResource(
                        R.string.inicio_pausa_en_curso,
                        it.desde.format(FormatoHora),
                        textoDuracion(it.transcurrido),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
            }
            val aviso = jornada.proximoAviso
            if (aviso == null) {
                Text(
                    text = stringResource(R.string.inicio_sin_avisos),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            } else {
                EtiquetaAviso(
                    hora = stringResource(R.string.inicio_proximo_aviso, aviso.hora.format(FormatoHora)),
                    detalle = stringResource(R.string.inicio_proximo_aviso_jornada, textoDuracion(aviso.desdeInicio)),
                )
            }
        }
    }
}

/**
 * Sin el país de la empresa y el de residencia no se puede saber si un semanal reducido se tomó
 * fuera de casa (R-DS-3). Se avisa hasta que se completen en Ajustes.
 */
@Composable
private fun AvisoPaises(
    onAjustes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.inicio_faltan_paises), style = MaterialTheme.typography.bodyLarge)
            OutlinedButton(onClick = onAjustes, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.accion_ir_ajustes))
            }
        }
    }
}

/** «Iniciar pausa» o «Terminar pausa»: secundario (con borde) para no confundirlo con «Finalizar jornada». */
@Composable
private fun BotonPausa(
    enPausa: Boolean,
    onIniciar: () -> Unit,
    onTerminar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = if (enPausa) onTerminar else onIniciar,
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
    ) {
        Icon(
            painterResource(if (enPausa) R.drawable.ic_reanudar else R.drawable.ic_pausa),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
        )
        Text(
            stringResource(if (enPausa) R.string.accion_terminar_pausa else R.string.accion_iniciar_pausa),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

/** Etiqueta naranja con campana: el naranja de la onda del logo significa «aviso». */
@Composable
private fun EtiquetaAviso(
    hora: String,
    detalle: String,
) {
    Surface(
        modifier = Modifier.padding(top = 4.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.Notifications, contentDescription = null, modifier = Modifier.size(24.dp))
            Column {
                Text(hora, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(detalle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeSinJornadaPreview() {
    MiTacografoTheme {
        HomeContent(
            state = HomeUiState(hoy = LocalDate.of(2026, 11, 4)),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeConJornadaPreview() {
    val madrid = ZoneId.of("Europe/Madrid")
    val inicio = ZonedDateTime.of(2026, 11, 4, 6, 0, 0, 0, madrid)
    MiTacografoTheme {
        HomeContent(
            state =
                HomeUiState(
                    hoy = LocalDate.of(2026, 11, 4),
                    jornada =
                        JornadaEnCursoUi(
                            inicio = inicio,
                            transcurrido = Duration.ofHours(5).plusMinutes(23),
                            kmInicio = 245_310,
                            proximoAviso = AvisoUi(inicio.plusHours(12).plusMinutes(30), Duration.ofMinutes(750)),
                        ),
                ),
        )
    }
}
