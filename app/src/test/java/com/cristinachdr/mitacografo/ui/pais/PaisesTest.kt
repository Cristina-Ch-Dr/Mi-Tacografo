package com.cristinachdr.mitacografo.ui.pais

import com.cristinachdr.mitacografo.domain.model.Continente
import com.cristinachdr.mitacografo.domain.model.PreferenciasPaises
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class PaisesTest {
    private val espanol = Paises(Locale.forLanguageTag("es"))

    private fun codigos(
        texto: String,
        recientes: List<String> = emptyList(),
    ) = espanol.buscar(texto, PreferenciasPaises(recientes = recientes)).todos.map { it.codigo }

    @Test
    fun `se encuentra por el nombre aunque no se sepa el codigo`() {
        assertEquals("DE", codigos("Alem").first())
        assertEquals("DE", codigos("alemania").first())
    }

    @Test
    fun `se encuentra por el codigo y el codigo exacto va primero`() {
        assertEquals("DE", codigos("de").first())
        assertEquals("ES", codigos("ES").first())
    }

    @Test
    fun `no importan las tildes ni las mayusculas`() {
        assertEquals("ES", codigos("Espana").first())
        assertEquals("PE", codigos("PERU").first())
        assertEquals("AT", codigos("austria").first())
        assertTrue("NL" in codigos("paises bajos"))
    }

    @Test
    fun `busca tambien por una palabra del medio y en ingles`() {
        assertEquals("GB", codigos("unido").first())
        assertEquals("DE", codigos("germany").first())
    }

    @Test
    fun `los codigos de carretera UK y EL tambien valen`() {
        assertEquals("GB", codigos("UK").first())
        assertEquals("GR", codigos("el").first())
        assertEquals("Reino Unido (GB)", espanol.textoDe("uk"))
    }

    @Test
    fun `sin texto salen primero los recientes y despues los de Europa`() {
        val lista = codigos("", recientes = listOf("FR", "DE", "XX"))

        assertEquals(listOf("FR", "DE"), lista.take(2))
        val primerNoEuropeo = lista.indexOfFirst { it !in Paises.EUROPA }
        assertTrue(lista.drop(primerNoEuropeo).none { it in Paises.EUROPA })
        assertEquals(lista.size, lista.distinct().size)
    }

    @Test
    fun `un reciente sube dentro de su grupo`() {
        // "es": primero España (código exacto). Después Eslovaquia, Eslovenia, Estonia…
        // pero si Estonia es reciente, pasa delante de las otras que empiezan igual.
        assertEquals(listOf("ES", "SK"), codigos("es").take(2))
        assertEquals(listOf("ES", "EE"), codigos("es", recientes = listOf("EE")).take(2))
    }

    @Test
    fun `los nombres salen en el idioma del movil`() {
        val ucraniano = Paises(Locale.forLanguageTag("uk"))

        assertEquals(
            "DE",
            ucraniano
                .buscar("Нім")
                .todos
                .first()
                .codigo,
        )
        assertEquals(
            "DE",
            ucraniano
                .buscar("DE")
                .todos
                .first()
                .codigo,
        )
    }

    @Test
    fun `un codigo desconocido se muestra tal cual`() {
        assertNull(espanol.buscarCodigo("ZZ"))
        assertEquals("ZZ", espanol.textoDe("ZZ"))
        assertEquals("España (ES)", espanol.textoDe("es"))
    }

    @Test
    fun `sin nada elegido en Ajustes no hay Otros paises`() {
        assertTrue(espanol.buscar("").otros.isEmpty())
        assertTrue(espanol.buscar("a").otros.isEmpty())
    }

    @Test
    fun `con Europa elegida el resto va a Otros paises pero se sigue encontrando`() {
        val europa = PreferenciasPaises(continentes = setOf(Continente.EUROPA))

        val todos = espanol.buscar("", europa)
        assertTrue(todos.principales.all { it.codigo in Paises.EUROPA || continenteDe(it.codigo) == Continente.EUROPA })
        assertTrue("US" in todos.otros.map { it.codigo })

        // "marr": Marruecos no es de Europa, pero sale (en Otros).
        assertEquals(listOf("MA"), espanol.buscar("marr", europa).otros.map { it.codigo })
    }

    @Test
    fun `los habituales y los recientes van arriba aunque sean de otro continente`() {
        val preferencias =
            PreferenciasPaises(
                recientes = listOf("FR"),
                habituales = setOf("MA"),
                continentes = setOf(Continente.EUROPA),
            )

        val principales = espanol.buscar("", preferencias).principales.map { it.codigo }

        assertEquals(listOf("FR", "MA"), principales.take(2))
        assertTrue("CN" !in principales)
    }

    @Test
    fun `escribir el codigo exacto lo pone arriba aunque no este elegido`() {
        val soloEspana = PreferenciasPaises(habituales = setOf("ES"))

        assertEquals(
            "US",
            espanol
                .buscar("us", soloEspana)
                .principales
                .first()
                .codigo,
        )
    }
}
