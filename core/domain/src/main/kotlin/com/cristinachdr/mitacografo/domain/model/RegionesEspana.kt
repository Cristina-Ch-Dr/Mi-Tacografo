package com.cristinachdr.mitacografo.domain.model

/** El país en el que el tacógrafo pide, además, la comunidad autónoma. */
const val PAIS_CON_REGIONES = "ES"

/**
 * Comunidades y ciudades autónomas de España con su código del tacógrafo (MT-55).
 *
 * Son los de la tabla oficial (Reglamento (CE) 1360/2002 y (UE) 2016/799, apéndice I), en su orden.
 * Ceuta y Melilla solo existen en los tacógrafos inteligentes.
 */
val REGIONES_ESPANA: List<String> =
    listOf(
        "AN",
        "AR",
        "AST",
        "C",
        "CAT",
        "CL",
        "CM",
        "CV",
        "EXT",
        "G",
        "IB",
        "IC",
        "LR",
        "M",
        "MU",
        "NA",
        "PV",
        "CE",
        "ME",
    )

/**
 * La comunidad tal y como se guarda: el código en mayúsculas, y solo si el país es España
 * y el código existe. Si no, `null` (en otro país no tiene sentido).
 */
fun String?.comoRegion(pais: String?): String? =
    this
        ?.trim()
        ?.uppercase()
        ?.takeIf { pais == PAIS_CON_REGIONES && it in REGIONES_ESPANA }

/** «ES (AN)» o solo «FR»: el país con la comunidad, si la hay. `null` si no hay país. */
fun lugar(
    pais: String?,
    region: String?,
): String? = pais?.let { if (region != null) "$it ($region)" else it }

/**
 * Antes de MT-55 el conductor apuntaba la comunidad en «Ciudad» («NA», «CAT»…). Si en España la ciudad es
 * solo un código del tacógrafo y aún no hay comunidad, pasa a ser la comunidad y la ciudad se queda vacía.
 * Lo mismo hace la migración 3 → 4 de la base de datos; esto es para las copias de seguridad antiguas.
 */
fun Jornada.conComunidadDeCiudad(): Jornada {
    val inicio = ciudadInicio.comoRegion(paisInicio)?.takeIf { regionInicio == null }
    val fin = ciudadFin.comoRegion(paisFin)?.takeIf { regionFin == null }
    if (inicio == null && fin == null) return this
    return copy(
        regionInicio = inicio ?: regionInicio,
        ciudadInicio = if (inicio != null) null else ciudadInicio,
        regionFin = fin ?: regionFin,
        ciudadFin = if (fin != null) null else ciudadFin,
    )
}
