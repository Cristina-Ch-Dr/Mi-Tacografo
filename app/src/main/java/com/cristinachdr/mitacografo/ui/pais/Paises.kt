package com.cristinachdr.mitacografo.ui.pais

import com.cristinachdr.mitacografo.domain.model.Continente
import com.cristinachdr.mitacografo.domain.model.PreferenciasPaises
import java.text.Normalizer
import java.util.Locale

/** Un país: el código de 2 letras que se guarda ("DE") y su nombre en el idioma del móvil ("Alemania"). */
data class Pais(
    val codigo: String,
    val nombre: String,
) {
    /** Lo que se ve en el campo: "Alemania (DE)". */
    val texto: String get() = "$nombre ($codigo)"
}

/** Resultado de una búsqueda: los que encajan con lo que suele usar el conductor y «Otros países». */
data class BusquedaPaises(
    val principales: List<Pais>,
    val otros: List<Pais> = emptyList(),
) {
    val todos: List<Pais> get() = principales + otros
}

/**
 * Lista de países y su búsqueda.
 *
 * Los nombres salen de Android ([Locale]), así que ya vienen traducidos a cualquier idioma
 * sin mantener ninguna lista a mano.
 */
class Paises(
    idioma: Locale,
    continenteDePais: (String) -> Continente? = ::continenteDe,
) {
    private val ingles = Locale.ENGLISH

    /** Todos los países: primero los de Europa y después el resto, cada grupo por orden alfabético. */
    val todos: List<Pais> =
        Locale
            .getISOCountries()
            .map { codigo ->
                Pais(
                    codigo,
                    Locale
                        .Builder()
                        .setRegion(codigo)
                        .build()
                        .getDisplayCountry(idioma),
                )
            }.sortedWith(compareBy<Pais> { it.codigo !in EUROPA }.thenBy { normalizar(it.nombre) })

    private val porCodigo = todos.associateBy { it.codigo }

    private val continentes: Map<String, Continente?> = todos.associate { it.codigo to continenteDePais(it.codigo) }

    /** Nombres normalizados en el idioma del móvil y en inglés, para buscar en los dos. */
    private val nombresBusqueda: Map<String, List<String>> =
        todos.associate { pais ->
            val enIngles =
                Locale
                    .Builder()
                    .setRegion(pais.codigo)
                    .build()
                    .getDisplayCountry(ingles)
            pais.codigo to listOf(normalizar(pais.nombre), normalizar(enIngles)).distinct()
        }

    /** El país de ese código, o `null` si no es un código conocido. */
    fun buscarCodigo(codigo: String): Pais? {
        val limpio = codigo.trim().uppercase()
        return porCodigo[ALIAS[limpio] ?: limpio]
    }

    /** Lo que se muestra en el campo para un código guardado. Un código desconocido se muestra tal cual. */
    fun textoDe(codigo: String): String = buscarCodigo(codigo)?.texto ?: codigo

    /**
     * Países que encajan con lo escrito, de mejor a peor:
     * 1. el código exacto ("DE", también "UK" → Reino Unido, "EL" → Grecia);
     * 2. el nombre empieza por lo escrito ("Alem");
     * 3. alguna palabra del nombre empieza por lo escrito ("Unido");
     * 4. el nombre contiene lo escrito.
     *
     * No distingue mayúsculas ni tildes ("peru" encuentra "Perú"). Sin texto, devuelve todos.
     *
     * Con las [preferencias]: los recientes van siempre primero; después, los habituales y los de
     * los continentes elegidos. El resto va a [BusquedaPaises.otros] (nunca se esconde). Escribir el
     * código exacto lo pone arriba aunque sea de otro continente.
     */
    fun buscar(
        texto: String,
        preferencias: PreferenciasPaises = PreferenciasPaises(),
    ): BusquedaPaises {
        val consulta = normalizar(texto)
        val recientes = preferencias.recientes.mapNotNull { buscarCodigo(it)?.codigo }.distinct()
        val ordenRecientes = recientes.withIndex().associate { (indice, codigo) -> codigo to indice }
        val exacto = buscarCodigo(texto)

        fun principal(pais: Pais): Boolean =
            !preferencias.hayFiltro ||
                pais == exacto ||
                pais.codigo in ordenRecientes ||
                pais.codigo in preferencias.habituales ||
                continentes[pais.codigo] in preferencias.continentes

        val encontrados =
            todos
                .mapNotNull { pais -> nivel(pais, consulta, exacto)?.let { pais to it } }
                .sortedWith(
                    compareBy(
                        // Sin texto, todos tienen el mismo nivel y manda el orden de abajo.
                        { (_, nivel) -> nivel },
                        { (pais, _) -> ordenRecientes[pais.codigo] ?: Int.MAX_VALUE },
                        { (pais, _) -> pais.codigo !in preferencias.habituales },
                    ),
                ).map { it.first }
        val (principales, otros) = encontrados.partition(::principal)
        return BusquedaPaises(principales, otros)
    }

    private fun nivel(
        pais: Pais,
        consulta: String,
        exacto: Pais?,
    ): Int? {
        if (pais == exacto) return 0
        val nombres = nombresBusqueda.getValue(pais.codigo)
        return when {
            nombres.any { it.startsWith(consulta) } -> 1
            nombres.any { nombre -> nombre.split(' ', '-').any { it.startsWith(consulta) } } -> 2
            nombres.any { it.contains(consulta) } -> 3
            else -> null
        }
    }

    companion object {
        /** Unión Europea, EEE, Suiza, Reino Unido y vecinos por los que se pasa conduciendo. */
        val EUROPA: Set<String> =
            setOf(
                // Unión Europea
                "AT",
                "BE",
                "BG",
                "CY",
                "CZ",
                "DE",
                "DK",
                "EE",
                "ES",
                "FI",
                "FR",
                "GR",
                "HR",
                "HU",
                "IE",
                "IT",
                "LT",
                "LU",
                "LV",
                "MT",
                "NL",
                "PL",
                "PT",
                "RO",
                "SE",
                "SI",
                "SK",
                // Resto de Europa
                "AD",
                "AL",
                "BA",
                "BY",
                "CH",
                "GB",
                "IS",
                "LI",
                "MC",
                "MD",
                "ME",
                "MK",
                "NO",
                "RS",
                "SM",
                "TR",
                "UA",
            )

        /** Códigos que se usan en carretera y en el tacógrafo y que no son ISO. */
        val ALIAS: Map<String, String> = mapOf("UK" to "GB", "EL" to "GR")

        /** Minúsculas y sin tildes: "Perú" → "peru". */
        fun normalizar(texto: String): String =
            Normalizer
                .normalize(texto.trim().lowercase(), Normalizer.Form.NFD)
                .replace(MARCAS, "")

        private val MARCAS = "\\p{Mn}+".toRegex()
    }
}
