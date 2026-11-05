package com.cristinachdr.mitacografo.ui.pais

import com.cristinachdr.mitacografo.domain.model.Continente
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class ContinentesTest {
    @Test
    fun `cada pais va a su continente`() {
        assertEquals(Continente.EUROPA, continenteDe("DE"))
        assertEquals(Continente.ASIA, continenteDe("CN"))
        assertEquals(Continente.AFRICA, continenteDe("MA"))
        assertEquals(Continente.AMERICA, continenteDe("US"))
        assertEquals(Continente.AMERICA, continenteDe("BR"))
        assertEquals(Continente.OCEANIA, continenteDe("AU"))
    }

    @Test
    fun `Turquia cuenta como Europa`() {
        assertEquals(Continente.EUROPA, continenteDe("TR"))
    }

    @Test
    fun `todos los paises de Java tienen continente menos la Antartida`() {
        val sinContinente = Locale.getISOCountries().filter { continenteDe(it) == null }

        assertEquals(listOf("AQ"), sinContinente)
    }

    @Test
    fun `un codigo desconocido no tiene continente`() {
        assertNull(continenteDe("ZZ"))
    }
}
