package com.cristinachdr.mitacografo.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId

/** La comunidad autónoma (MT-55). Datos inventados. */
class RegionesEspanaTest {
    private val jornada =
        Jornada(inicio = Instant.EPOCH, zonaInicio = ZoneId.of("Europe/Madrid"), kmInicio = 0, paisInicio = "ES")

    @Test
    fun `solo vale en Espana y con un codigo del tacografo`() {
        assertEquals("AN", " an ".comoRegion("ES"))
        assertNull("AN".comoRegion("FR"))
        assertNull("XX".comoRegion("ES"))
        assertNull(null.comoRegion("ES"))
    }

    @Test
    fun `el lugar lleva la comunidad entre parentesis`() {
        assertEquals("ES (NA)", lugar("ES", "NA"))
        assertEquals("FR", lugar("FR", null))
        assertNull(lugar(null, "NA"))
    }

    @Test
    fun `una comunidad apuntada en Ciudad pasa a su campo`() {
        val antigua = jornada.copy(ciudadInicio = "na", paisFin = "ES", ciudadFin = "Pamplona")

        val nueva = antigua.conComunidadDeCiudad()

        assertEquals("NA", nueva.regionInicio)
        assertNull(nueva.ciudadInicio)
        // Una ciudad de verdad se queda como está.
        assertNull(nueva.regionFin)
        assertEquals("Pamplona", nueva.ciudadFin)
    }

    @Test
    fun `fuera de Espana o con comunidad ya puesta no se toca`() {
        val enFrancia = jornada.copy(paisInicio = "FR", ciudadInicio = "NA")
        assertEquals(enFrancia, enFrancia.conComunidadDeCiudad())

        val yaPuesta = jornada.copy(regionInicio = "M", ciudadInicio = "CAT")
        assertEquals(yaPuesta, yaPuesta.conComunidadDeCiudad())
    }
}
