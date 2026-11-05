package com.cristinachdr.mitacografo.ui.pais

import com.cristinachdr.mitacografo.domain.model.Continente

/**
 * Continente de un país (regiones de la ONU, M49). `null` para la Antártida o un código desconocido.
 *
 * Android no expone estas regiones en su API pública, así que van aquí. Cambian muy poco (casi
 * nunca aparece un país nuevo) y `ContinentesTest` comprueba que no falte ninguno.
 *
 * Los países de [Paises.EUROPA] cuentan siempre como Europa: para la ONU, Turquía es Asia,
 * pero para un camionero que sale de España es un viaje por Europa.
 */
fun continenteDe(codigo: String): Continente? =
    if (codigo in
        Paises.EUROPA
    ) {
        Continente.EUROPA
    } else {
        CONTINENTE_DE_CADA_PAIS[codigo]
    }

private val CONTINENTE_DE_CADA_PAIS: Map<String, Continente> =
    mapOf(
        Continente.EUROPA to
            "AD AL AT AX BA BE BG BY CH CZ DE DK EE ES FI FO FR GB GG GI GR HR HU IE IM IS IT JE LI LT LU LV " +
            "MC MD ME MK MT NL NO PL PT RO RS RU SE SI SJ SK SM UA VA XK",
        Continente.ASIA to
            "AE AF AM AZ BD BH BN BT CN CY GE HK ID IL IN IQ IR JO JP KG KH KP KR KW KZ LA LB LK MM MN MO " +
            "MV MY NP OM PH PK PS QA SA SG SY TH TJ TL TM TR TW UZ VN YE",
        Continente.AFRICA to
            "AO BF BI BJ BW CD CF CG CI CM CV DJ DZ EG EH ER ET GA GH GM GN GQ GW IO KE KM LR LS LY MA MG " +
            "ML MR MU MW MZ NA NE NG RE RW SC SD SH SL SN SO SS ST SZ TD TF TG TN TZ UG YT ZA ZM ZW",
        Continente.AMERICA to
            "AG AI AR AW BB BL BM BO BQ BR BS BV BZ CA CL CO CR CU CW DM DO EC FK GD GF GL GP GS GT GY HN " +
            "HT JM KN KY LC MF MQ MS MX NI PA PE PM PR PY SR SV SX TC TT US UY VC VE VG VI",
        Continente.OCEANIA to
            "AS AU CC CK CX FJ FM GU HM KI MH MP NC NF NR NU NZ PF PG PN PW SB TK TO TV UM VU WF WS",
    ).flatMap { (continente, codigos) -> codigos.split(' ').map { it to continente } }
        .toMap()
