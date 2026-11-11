package com.cristinachdr.mitacografo.ui.reglas

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.domain.rules.Intervalo
import com.cristinachdr.mitacografo.domain.rules.MotivoNoVerificado
import com.cristinachdr.mitacografo.domain.rules.ReglaId
import com.cristinachdr.mitacografo.domain.rules.Resultado
import com.cristinachdr.mitacografo.ui.textoDuracion
import com.cristinachdr.mitacografo.ui.theme.EstadoCumple
import com.cristinachdr.mitacografo.ui.theme.EstadoExcedido
import com.cristinachdr.mitacografo.ui.theme.EstadoNoVerificado

// El dominio devuelve códigos (ReglaId, MotivoNoVerificado); aquí se traducen a textos de strings.xml.

/** Los tres estados que se enseñan: ✅ ⚠️ ❔. */
enum class EstadoRegla(
    val icono: ImageVector,
    @StringRes val texto: Int,
    val color: Color,
) {
    // Iconos con color en vez de emojis: se ven igual en todos los móviles (y el texto va al lado).
    CUMPLE(Icons.Filled.CheckCircle, R.string.estado_cumple, EstadoCumple),
    NO_VERIFICADO(Icons.Filled.Info, R.string.estado_no_verificado, EstadoNoVerificado),
    EXCEDIDO(Icons.Filled.Warning, R.string.estado_excedido, EstadoExcedido),
}

val Resultado.estado: EstadoRegla
    get() =
        when (this) {
            is Resultado.Cumple -> EstadoRegla.CUMPLE
            is Resultado.Excedido -> EstadoRegla.EXCEDIDO
            is Resultado.NoVerificado -> EstadoRegla.NO_VERIFICADO
        }

/**
 * El peor de varios resultados (excedido > no verificado > cumple), o `null` si no hay ninguno.
 *
 * La pausa no cuenta: sin los tramos de conducción casi nunca se puede comprobar (D-4) y pondría ❔
 * en todas las jornadas. Se sigue viendo en el informe de la semana.
 */
fun List<Resultado>.peor(): EstadoRegla? =
    filterNot { it.regla == ReglaId.PAUSA }.maxOfOrNull { it.estado.ordinal }?.let { EstadoRegla.entries[it] }

@get:StringRes
val ReglaId.nombre: Int
    get() =
        when (this) {
            ReglaId.CONDUCCION_DIARIA -> R.string.regla_conduccion_diaria
            ReglaId.AMPLIACIONES_SEMANA -> R.string.regla_ampliaciones
            ReglaId.CONDUCCION_SEMANAL -> R.string.regla_conduccion_semanal
            ReglaId.CONDUCCION_BISEMANAL -> R.string.regla_conduccion_bisemanal
            ReglaId.PAUSA -> R.string.regla_pausa
            ReglaId.DESCANSO_DIARIO -> R.string.regla_descanso_diario
            ReglaId.REDUCIDOS_ENTRE_SEMANALES -> R.string.regla_reducidos
            ReglaId.SEMANAL_DOS_SEMANAS -> R.string.regla_semanal
            ReglaId.SEIS_PERIODOS -> R.string.regla_seis_periodos
            ReglaId.REDUCIDOS_FUERA_DE_CASA -> R.string.regla_fuera_de_casa
            ReglaId.COMPENSACION -> R.string.regla_compensacion
            ReglaId.COMPENSACION_ANTES_DEL_SEMANAL -> R.string.regla_compensacion_antes
        }

/** Qué dice la regla, en una frase, con el artículo del reglamento. */
@get:StringRes
val ReglaId.explicacion: Int
    get() =
        when (this) {
            ReglaId.CONDUCCION_DIARIA -> R.string.regla_conduccion_diaria_exp
            ReglaId.AMPLIACIONES_SEMANA -> R.string.regla_ampliaciones_exp
            ReglaId.CONDUCCION_SEMANAL -> R.string.regla_conduccion_semanal_exp
            ReglaId.CONDUCCION_BISEMANAL -> R.string.regla_conduccion_bisemanal_exp
            ReglaId.PAUSA -> R.string.regla_pausa_exp
            ReglaId.DESCANSO_DIARIO -> R.string.regla_descanso_diario_exp
            ReglaId.REDUCIDOS_ENTRE_SEMANALES -> R.string.regla_reducidos_exp
            ReglaId.SEMANAL_DOS_SEMANAS -> R.string.regla_semanal_exp
            ReglaId.SEIS_PERIODOS -> R.string.regla_seis_periodos_exp
            ReglaId.REDUCIDOS_FUERA_DE_CASA -> R.string.regla_fuera_de_casa_exp
            ReglaId.COMPENSACION -> R.string.regla_compensacion_exp
            ReglaId.COMPENSACION_ANTES_DEL_SEMANAL -> R.string.regla_compensacion_antes_exp
        }

/** Por qué no se puede comprobar, en palabras del conductor. */
@get:StringRes
val MotivoNoVerificado.texto: Int
    get() =
        when (this) {
            MotivoNoVerificado.SIN_CONDUCCION_TACOGRAFO -> R.string.motivo_sin_conduccion
            MotivoNoVerificado.SIN_TRAMOS_CONDUCCION -> R.string.motivo_sin_tramos
            MotivoNoVerificado.DESCANSO_NO_REGISTRADO -> R.string.motivo_descanso_no_registrado
            MotivoNoVerificado.JORNADA_CRUZA_SEMANA -> R.string.motivo_cruza_semana
            MotivoNoVerificado.DATOS_INSUFICIENTES_PERIODO -> R.string.motivo_datos_insuficientes
            MotivoNoVerificado.SIN_PAIS_DESCANSO -> R.string.motivo_sin_pais
            MotivoNoVerificado.PAISES_NO_CONFIGURADOS -> R.string.motivo_paises
        }

/** «9 h 12 min» si se sabe exacto; «entre 43 h 00 min y 45 h 18 min» si no. */
@Composable
fun textoIntervalo(intervalo: Intervalo): String =
    // Iguales en minutos (p. ej. una jornada que acaba de empezar): un solo valor.
    if (intervalo.min.toMinutes() == intervalo.max.toMinutes()) {
        textoDuracion(intervalo.min)
    } else {
        stringResource(R.string.intervalo, textoDuracion(intervalo.min), textoDuracion(intervalo.max))
    }

/** ✅ Cumple / ⚠️ Excedido / ❔ No verificado, con el color de cada estado (siempre con texto, no solo color). */
@Composable
fun ChipEstado(
    estado: EstadoRegla,
    modifier: Modifier = Modifier,
    conTexto: Boolean = true,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = estado.color.copy(alpha = 0.14f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                estado.icono,
                // Sin texto al lado, el icono lleva la descripción para TalkBack.
                contentDescription = if (conTexto) null else stringResource(estado.texto),
                tint = estado.color,
                modifier = Modifier.size(20.dp),
            )
            if (conTexto) {
                Text(
                    stringResource(estado.texto),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}
