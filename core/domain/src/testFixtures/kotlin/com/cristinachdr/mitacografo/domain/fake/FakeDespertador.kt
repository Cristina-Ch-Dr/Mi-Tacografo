package com.cristinachdr.mitacografo.domain.fake

import com.cristinachdr.mitacografo.domain.port.Despertador
import java.time.Instant

/** Despertador en memoria para los tests: recuerda a qué hora está puesto. */
class FakeDespertador : Despertador {
    var puesto: Instant? = null
        private set

    override fun programar(hora: Instant) {
        puesto = hora
    }

    override fun cancelar() {
        puesto = null
    }
}
