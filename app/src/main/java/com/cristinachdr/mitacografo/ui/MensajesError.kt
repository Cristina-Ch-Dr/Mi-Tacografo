package com.cristinachdr.mitacografo.ui

import androidx.annotation.StringRes
import com.cristinachdr.mitacografo.R
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
        }
