package com.cristinachdr.mitacografo.platform

import java.time.Duration

/** Programa un aviso de prueba para comprobar que las notificaciones llegan a este móvil. */
fun interface ProbadorAvisos {
    fun programarPrueba(dentroDe: Duration)
}
