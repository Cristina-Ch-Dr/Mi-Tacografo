package com.cristinachdr.mitacografo.domain.port

import java.time.Instant

/**
 * Puerto de salida para el despertador (MT-57): una alarma de verdad, que suena con la app cerrada.
 *
 * La implementación con `AlarmManager` vive en `app` (necesita Android). Solo hay uno a la vez.
 */
interface Despertador {
    /** Pone el despertador a esa hora (si ya había uno, lo cambia). */
    fun programar(hora: Instant)

    fun cancelar()
}
