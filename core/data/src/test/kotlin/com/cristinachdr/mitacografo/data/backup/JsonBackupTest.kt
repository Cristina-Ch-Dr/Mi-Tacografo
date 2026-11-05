package com.cristinachdr.mitacografo.data.backup

import com.cristinachdr.mitacografo.domain.model.Ajustes
import com.cristinachdr.mitacografo.domain.model.AvisoConfig
import com.cristinachdr.mitacografo.domain.model.Continente
import com.cristinachdr.mitacografo.domain.model.CopiaInvalidaException
import com.cristinachdr.mitacografo.domain.model.CopiaSeguridad
import com.cristinachdr.mitacografo.domain.model.ErrorCopia
import com.cristinachdr.mitacografo.domain.model.Jornada
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** El formato JSON no necesita Android: test normal de JVM. Datos inventados (solo horas y km). */
class JsonBackupTest {
    private val backup = JsonBackup()
    private val madrid = ZoneId.of("Europe/Madrid")
    private val kyiv = ZoneId.of("Europe/Kyiv")

    private fun hora(
        dia: Int,
        h: Int,
        zona: ZoneId = madrid,
    ): Instant = LocalDateTime.of(2026, 11, dia, h, 0).atZone(zona).toInstant()

    private val copia =
        CopiaSeguridad(
            creada = hora(3, 20),
            jornadas =
                listOf(
                    Jornada(
                        id = 1,
                        inicio = hora(2, 6),
                        zonaInicio = madrid,
                        kmInicio = 245_310,
                        paisInicio = "ES",
                        ciudadInicio = "Ciudad A",
                        fin = hora(2, 18, kyiv),
                        zonaFin = kyiv,
                        kmFin = 245_872,
                        paisFin = "UA",
                        ciudadFin = "Ciudad B",
                        conduccionTacografo = Duration.ofMinutes(525),
                        viajeId = 3,
                        observaciones = "Nota con \"comillas\" y ñ",
                    ),
                    Jornada(id = 2, inicio = hora(3, 6), zonaInicio = madrid, kmInicio = 245_872),
                ),
            ajustes =
                Ajustes(
                    avisos =
                        listOf(
                            AvisoConfig(Duration.ofMinutes(750), activo = false),
                            AvisoConfig(Duration.ofHours(15)),
                        ),
                    paisEmpresa = "ES",
                    paisResidencia = "UA",
                    nombreConductor = "Conductor de prueba",
                    zonaReferencia = madrid,
                    paisesHabituales = setOf("DE", "FR"),
                    continentes = setOf(Continente.EUROPA),
                ),
        )

    /** El error al leer [texto]; si se lee sin error, el test falla. */
    private fun errorAlLeer(texto: String): ErrorCopia =
        try {
            backup.decodificar(texto)
            throw AssertionError("Se esperaba un error al leer: $texto")
        } catch (e: CopiaInvalidaException) {
            e.error
        }

    @Test
    fun `exportar y volver a leer da exactamente los mismos datos`() {
        val leida = backup.decodificar(backup.codificar(copia))

        assertEquals(copia, leida)
    }

    @Test
    fun `el archivo se entiende al abrirlo con un editor`() {
        val texto = backup.codificar(copia)

        assertTrue(texto.contains("\"app\": \"mi-tacografo\""))
        assertTrue(texto.contains("\"version\": 1"))
        assertTrue(texto.contains("\"inicio\": \"2026-11-02T05:00:00Z\""))
        assertTrue(texto.contains("\"zonaFin\": \"Europe/Kyiv\""))
    }

    @Test
    fun `una copia de la version 1 escrita a mano se sigue leyendo`() {
        // Si un cambio rompe este test, las copias que ya tiene el conductor dejarían de valer.
        val texto =
            """
            {
              "app": "mi-tacografo",
              "version": 1,
              "creada": "2026-11-03T19:00:00Z",
              "jornadas": [
                { "id": 1, "inicio": "2026-11-02T05:00:00Z", "zonaInicio": "Europe/Madrid", "kmInicio": 100,
                  "fin": "2026-11-02T16:00:00Z", "zonaFin": "Europe/Madrid", "kmFin": 600, "conduccionMinutos": 540 }
              ],
              "ajustes": { "paisEmpresa": "ES" },
              "campoDeUnaVersionFutura": true
            }
            """.trimIndent()

        val leida = backup.decodificar(texto)

        assertEquals(500, leida.jornadas.single().kmRecorridos)
        assertEquals(Duration.ofHours(9), leida.jornadas.single().conduccionTacografo)
        assertEquals(Ajustes(paisEmpresa = "ES"), leida.ajustes)
    }

    @Test
    fun `un archivo que no es nuestro no se acepta`() {
        assertEquals(ErrorCopia.NO_ES_UNA_COPIA, errorAlLeer("esto no es JSON"))
        assertEquals(ErrorCopia.NO_ES_UNA_COPIA, errorAlLeer("""{ "nombre": "otra app" }"""))
        assertEquals(
            ErrorCopia.NO_ES_UNA_COPIA,
            errorAlLeer("""{ "app": "otra-app", "version": 1, "creada": "2026-11-03T19:00:00Z" }"""),
        )
    }

    @Test
    fun `una copia de una version mas nueva pide actualizar la app`() {
        assertEquals(
            ErrorCopia.VERSION_MAS_NUEVA,
            errorAlLeer("""{ "app": "mi-tacografo", "version": 99, "creada": "2026-11-03T19:00:00Z" }"""),
        )
    }

    @Test
    fun `fechas o zonas que no existen son datos incorrectos`() {
        val zonaMala =
            """{ "app": "mi-tacografo", "version": 1, "creada": "2026-11-03T19:00:00Z",
                "jornadas": [ { "id": 1, "inicio": "2026-11-02T05:00:00Z", "zonaInicio": "Marte/Base", "kmInicio": 1 } ] }"""

        assertEquals(ErrorCopia.DATOS_INCORRECTOS, errorAlLeer(zonaMala))
        assertEquals(
            ErrorCopia.DATOS_INCORRECTOS,
            errorAlLeer("""{ "app": "mi-tacografo", "version": 1, "creada": "ayer" }"""),
        )
    }
}
