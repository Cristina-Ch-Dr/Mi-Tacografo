package com.cristinachdr.mitacografo.ui

import androidx.annotation.StringRes
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.domain.model.ClaseDescanso
import com.cristinachdr.mitacografo.domain.model.ErrorDescanso
import com.cristinachdr.mitacografo.domain.model.ErrorJornada

/** Errores de un campo del formulario que se detectan antes de llegar al dominio. */
enum class ErrorCampo(
    @StringRes val mensaje: Int,
) {
    OBLIGATORIO(R.string.error_obligatorio),
    NO_ES_NUMERO(R.string.error_no_numero),
    MINUTOS_INVALIDOS(R.string.error_minutos),
}

/** El dominio no sabe de idiomas: aquí se traduce cada error a un texto de `strings.xml`. */
@get:StringRes
val ErrorJornada.mensaje: Int
    get() =
        when (this) {
            ErrorJornada.JORNADA_YA_ABIERTA -> R.string.error_jornada_ya_abierta
            ErrorJornada.SIN_JORNADA_ABIERTA -> R.string.error_sin_jornada_abierta
            ErrorJornada.JORNADA_NO_EXISTE -> R.string.error_jornada_no_existe
            ErrorJornada.KM_NEGATIVOS -> R.string.error_km_negativos
            ErrorJornada.KM_FIN_MENOR_QUE_INICIO -> R.string.error_km_fin_menor
            ErrorJornada.INICIO_EN_EL_FUTURO -> R.string.error_inicio_futuro
            ErrorJornada.INICIO_ANTES_DEL_FIN_ANTERIOR -> R.string.error_inicio_antes_fin_anterior
            ErrorJornada.FIN_NO_POSTERIOR_AL_INICIO -> R.string.error_fin_no_posterior
            ErrorJornada.FIN_EN_EL_FUTURO -> R.string.error_fin_futuro
            ErrorJornada.CONDUCCION_NEGATIVA -> R.string.error_conduccion_negativa
            ErrorJornada.CONDUCCION_MAYOR_QUE_JORNADA -> R.string.error_conduccion_mayor
            ErrorJornada.SOLAPA_CON_OTRA_JORNADA -> R.string.error_solapa
        }

@get:StringRes
val ErrorDescanso.mensaje: Int
    get() =
        when (this) {
            ErrorDescanso.DESCANSO_NO_EXISTE -> R.string.error_descanso_no_existe
            ErrorDescanso.SIN_JORNADA_ABIERTA -> R.string.error_descanso_sin_jornada
            ErrorDescanso.YA_HAY_UNO_EN_CURSO -> R.string.error_descanso_ya_en_curso
            ErrorDescanso.SIN_PAUSA_EN_CURSO -> R.string.error_descanso_sin_pausa
            ErrorDescanso.INICIO_EN_EL_FUTURO -> R.string.error_inicio_futuro
            ErrorDescanso.FIN_NO_POSTERIOR_AL_INICIO -> R.string.error_fin_no_posterior
            ErrorDescanso.FIN_EN_EL_FUTURO -> R.string.error_fin_futuro
            ErrorDescanso.SOLAPA_CON_OTRO_DESCANSO -> R.string.error_descanso_solapa
        }

/** Nombre de cada clase de descanso para el chip de la lista. El semanal reducido lleva la deuda (`%1$s`). */
@get:StringRes
val ClaseDescanso.nombre: Int
    get() =
        when (this) {
            ClaseDescanso.PAUSA -> R.string.clase_pausa
            ClaseDescanso.EN_CURSO -> R.string.clase_en_curso
            ClaseDescanso.INSUFICIENTE -> R.string.clase_insuficiente
            ClaseDescanso.FRACCIONADO_PRIMERA_PARTE -> R.string.clase_fraccionado_1
            ClaseDescanso.FRACCIONADO_COMPLETO -> R.string.clase_fraccionado_completo
            ClaseDescanso.DIARIO_REDUCIDO -> R.string.clase_diario_reducido
            ClaseDescanso.DIARIO_NORMAL -> R.string.clase_diario_normal
            ClaseDescanso.SEMANAL_REDUCIDO -> R.string.clase_semanal_reducido
            ClaseDescanso.SEMANAL_NORMAL -> R.string.clase_semanal_normal
        }
