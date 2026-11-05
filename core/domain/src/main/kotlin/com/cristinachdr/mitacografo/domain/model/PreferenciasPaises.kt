package com.cristinachdr.mitacografo.domain.model

/** Continentes para filtrar el selector de país (los de la ONU, M49). */
enum class Continente {
    EUROPA,
    ASIA,
    AFRICA,
    AMERICA,
    OCEANIA,
}

/**
 * Qué países salen primero en el selector.
 *
 * Si no se ha elegido ningún continente ni país habitual, salen todos igual (es lo de por defecto).
 * Los demás países nunca se esconden: se pueden seguir buscando en «Otros países».
 */
data class PreferenciasPaises(
    /** Los últimos usados en las jornadas, el más reciente primero. */
    val recientes: List<String> = emptyList(),
    /** Donde suele cargar y descargar (códigos ISO alfa-2). */
    val habituales: Set<String> = emptySet(),
    val continentes: Set<Continente> = emptySet(),
) {
    /** `true` si el conductor ha elegido algo; si no, no hay sección «Otros países». */
    val hayFiltro: Boolean get() = habituales.isNotEmpty() || continentes.isNotEmpty()
}
