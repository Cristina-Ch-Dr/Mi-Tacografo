package com.cristinachdr.mitacografo.platform

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.graphics.withTranslation
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.domain.model.ClaseDescanso
import com.cristinachdr.mitacografo.domain.usecase.Informe
import com.cristinachdr.mitacografo.ui.nombre
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject

/** Convierte un [Informe] en un PDF. Es un adaptador: el dominio no sabe nada de PDF. */
interface GeneradorPdf {
    suspend fun generar(informe: Informe): ByteArray
}

/**
 * PDF A4 vertical con `PdfDocument` (sin librerías): cabecera, resumen, tabla de jornadas
 * y tabla de descansos. Si una tabla no cabe, sigue en la página siguiente y repite su cabecera.
 * Cada página lleva el aviso legal y «Página X de Y».
 *
 * Se dibuja dos veces: la primera solo cuenta las páginas (para saber la Y de «X de Y»).
 * La fuente es la de Android (Roboto), que ya tiene cirílico: el ucraniano sale bien.
 */
class PdfInformeAndroid
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : GeneradorPdf {
        override suspend fun generar(informe: Informe): ByteArray =
            withContext(Dispatchers.Default) {
                val textos = Textos(context)
                val paginas = Maquetador(informe, textos, documento = null).dibujar(totalPaginas = 0)
                val documento = PdfDocument()
                try {
                    Maquetador(informe, textos, documento).dibujar(totalPaginas = paginas)
                    ByteArrayOutputStream().use { salida ->
                        documento.writeTo(salida)
                        salida.toByteArray()
                    }
                } finally {
                    documento.close()
                }
            }
    }

/** Textos y formatos en el idioma de la app (el mismo que se ve en pantalla). */
private class Textos(
    private val context: Context,
) {
    val idioma: Locale = Locale.forLanguageTag(context.getString(R.string.idioma_app))
    val fecha: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(idioma)
    val diaSemana: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE", idioma)
    val hora: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val numero = NumberFormat.getIntegerInstance(idioma)

    fun s(
        id: Int,
        vararg args: Any,
    ): String = context.getString(id, *args)

    fun km(km: Int): String = numero.format(km)

    fun duracion(d: Duration): String {
        val minutos = d.toMinutes().coerceAtLeast(0)
        return s(R.string.formato_duracion, minutos / 60, minutos % 60)
    }

    fun clase(
        clase: ClaseDescanso,
        deuda: Duration,
    ): String = s(clase.nombre, duracion(deuda))
}

private const val ANCHO = 595 // A4 en puntos
private const val ALTO = 842
private const val MARGEN = 40f
private const val PIE = 70f // espacio reservado abajo para el aviso legal y el número de página
private const val FILA = 18f
private const val AZUL = 0xFF234886.toInt()

/**
 * Coloca el contenido página a página. Con [documento] `null` no dibuja nada: solo cuenta páginas.
 */
private class Maquetador(
    private val informe: Informe,
    private val t: Textos,
    private val documento: PdfDocument?,
) {
    private val titulo = pincel(18f, negrita = true, color = AZUL)
    private val subtitulo = pincel(13f, negrita = true, color = AZUL)
    private val normal = pincel(10f)
    private val negrita = pincel(10f, negrita = true)
    private val pequeno = pincel(8f, color = Color.DKGRAY)
    private val linea =
        Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 0.7f
        }
    private val fondoCabecera = Paint().apply { color = 0xFFD7E6FE.toInt() }

    private var pagina: PdfDocument.Page? = null
    private var canvas: Canvas? = null
    private var numero = 0
    private var total = 0
    private var y = MARGEN

    /** @return cuántas páginas ocupa. */
    fun dibujar(totalPaginas: Int): Int {
        total = totalPaginas
        nuevaPagina()
        cabecera()
        resumen()
        tablaJornadas()
        tablaDescansos()
        cerrarPagina()
        return numero
    }

    private fun nuevaPagina() {
        numero++
        y = MARGEN
        if (documento != null) {
            val info = PdfDocument.PageInfo.Builder(ANCHO, ALTO, numero).create()
            pagina = documento.startPage(info)
            canvas = pagina?.canvas
        }
    }

    private fun cerrarPagina() {
        val c = canvas
        if (c != null) {
            val aviso =
                StaticLayout.Builder
                    .obtain(
                        t.s(R.string.pdf_aviso_legal),
                        0,
                        t.s(R.string.pdf_aviso_legal).length,
                        pequeno,
                        (
                            ANCHO -
                                2 * MARGEN
                        ).toInt(),
                    ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .build()
            c.withTranslation(MARGEN, ALTO - PIE + 8f) { aviso.draw(this) }
            val texto = t.s(R.string.pdf_pagina, numero, total)
            c.drawText(texto, ANCHO - MARGEN - pequeno.measureText(texto), ALTO - 20f, pequeno)
            documento?.finishPage(pagina)
        }
        pagina = null
        canvas = null
    }

    /** Si no caben [alto] puntos más, pasa a la página siguiente. @return `true` si ha cambiado de página. */
    private fun asegurar(alto: Float): Boolean {
        if (y + alto <= ALTO - PIE) return false
        cerrarPagina()
        nuevaPagina()
        return true
    }

    private fun texto(
        s: String,
        pincel: Paint,
        x: Float = MARGEN,
        alto: Float = pincel.textSize + 6f,
    ) {
        asegurar(alto)
        canvas?.drawText(s, x, y + pincel.textSize, pincel)
        y += alto
    }

    private fun cabecera() {
        val r = informe.resumen
        texto(t.s(R.string.app_name) + " · " + t.s(R.string.pdf_titulo), titulo, alto = 28f)
        informe.conductor?.takeIf { it.isNotBlank() }?.let { texto(t.s(R.string.pdf_conductor, it), negrita) }
        texto(t.s(R.string.pdf_periodo, r.desde.format(t.fecha), r.hasta.format(t.fecha)), normal)
        val generado = informe.generado.atZone(informe.zona)
        texto(t.s(R.string.pdf_generado, generado.format(t.fecha), generado.format(t.hora)), pequeno)
        y += 10f
    }

    private fun resumen() {
        val r = informe.resumen
        texto(t.s(R.string.pdf_resumen), subtitulo, alto = 22f)
        val filas =
            buildList {
                add(t.s(R.string.resumen_km) to t.km(r.km))
                add(t.s(R.string.resumen_jornadas) to r.numJornadas.toString())
                add(t.s(R.string.resumen_tiempo) to t.duracion(r.tiempoJornadas))
                add(
                    t.s(R.string.resumen_conduccion) to
                        t.duracion(r.conduccion) + if (r.conduccionCompleta) "" else " *",
                )
                CLASES_RESUMEN.forEach { clase ->
                    val n = r.cuantos(clase)
                    if (n > 0) add(t.clase(clase, Duration.ZERO).substringBefore(" ·") to n.toString())
                }
                if (!r.deudaSemanal.isZero) add(t.s(R.string.resumen_deuda, t.duracion(r.deudaSemanal)) to "")
            }
        filas.forEach { (etiqueta, valor) ->
            asegurar(FILA)
            canvas?.drawText(etiqueta, MARGEN, y + 11f, normal)
            canvas?.drawText(valor, MARGEN + 260f, y + 11f, negrita)
            y += FILA - 2f
        }
        if (!r.conduccionCompleta) texto("* " + t.s(R.string.resumen_conduccion_incompleta), pequeno)
        if (r.semanas > 1) {
            r.porSemana.forEach { semana ->
                texto(
                    t.s(R.string.resumen_semana_de, semana.desde.format(t.fecha)) +
                        ": " + t.km(semana.km) + " km · " + t.duracion(semana.conduccion),
                    normal,
                )
            }
        }
        y += 12f
    }

    private fun tablaJornadas() {
        texto(t.s(R.string.resumen_jornadas), subtitulo, alto = 22f)
        val jornadas = informe.resumen.jornadas
        if (jornadas.isEmpty()) {
            texto(t.s(R.string.pdf_sin_jornadas), normal)
            y += 10f
            return
        }
        val columnas =
            listOf(
                Columna(t.s(R.string.pdf_col_fecha), 90f),
                Columna(t.s(R.string.pdf_col_inicio), 50f),
                Columna(t.s(R.string.pdf_col_fin), 72f),
                Columna(t.s(R.string.pdf_col_km_inicio), 64f, derecha = true),
                Columna(t.s(R.string.pdf_col_km_fin), 64f, derecha = true),
                Columna(t.s(R.string.pdf_col_km), 44f, derecha = true),
                Columna(t.s(R.string.pdf_col_conduccion), 72f, derecha = true),
                Columna(t.s(R.string.pdf_col_pais), 59f),
            )
        val filas =
            jornadas.map { j ->
                val inicio = j.inicio.atZone(j.zonaInicio)
                val fin = j.fin?.atZone(j.zonaFin ?: j.zonaInicio)
                listOf(
                    inicio.format(t.diaSemana) + " " + inicio.format(t.fecha),
                    inicio.format(t.hora),
                    when {
                        fin == null -> t.s(R.string.pdf_en_curso)
                        fin.toLocalDate() == inicio.toLocalDate() -> fin.format(t.hora)
                        else -> fin.format(t.hora) + " (" + fin.format(t.diaSemana) + ")"
                    },
                    t.km(j.kmInicio),
                    j.kmFin?.let(t::km) ?: "—",
                    j.kmRecorridos?.let(t::km) ?: "—",
                    j.conduccionTacografo?.let(t::duracion) ?: "—",
                    listOfNotNull(j.paisInicio, j.paisFin).distinct().joinToString(" → ").ifEmpty { "—" },
                )
            }
        tabla(columnas, filas)
    }

    private fun tablaDescansos() {
        val descansos = informe.resumen.descansos
        if (descansos.isEmpty()) return
        texto(t.s(R.string.nav_descansos), subtitulo, alto = 22f)
        val columnas =
            listOf(
                Columna(t.s(R.string.pdf_col_fecha), 90f),
                Columna(t.s(R.string.pdf_col_inicio), 50f),
                Columna(t.s(R.string.pdf_col_fin), 72f),
                Columna(t.s(R.string.pdf_col_duracion), 72f, derecha = true),
                Columna(t.s(R.string.pdf_col_clase), 178f),
                Columna(t.s(R.string.pdf_col_pais), 53f),
            )
        val filas =
            descansos.map { c ->
                val d = c.descanso
                val inicio = d.inicio.atZone(d.zonaInicio)
                val fin = d.fin?.atZone(d.zonaFin ?: d.zonaInicio)
                listOf(
                    inicio.format(t.diaSemana) + " " + inicio.format(t.fecha),
                    inicio.format(t.hora),
                    when {
                        fin == null -> t.s(R.string.pdf_en_curso)
                        fin.toLocalDate() == inicio.toLocalDate() -> fin.format(t.hora)
                        else -> fin.format(t.hora) + " (" + fin.format(t.diaSemana) + ")"
                    },
                    d.duracion?.let(t::duracion) ?: "—",
                    t.clase(c.clase, c.deuda),
                    d.pais ?: "—",
                )
            }
        tabla(columnas, filas)
    }

    private data class Columna(
        val titulo: String,
        val ancho: Float,
        val derecha: Boolean = false,
    )

    /** Tabla con cabecera de fondo azul claro; si salta de página, la cabecera se repite. */
    private fun tabla(
        columnas: List<Columna>,
        filas: List<List<String>>,
    ) {
        asegurar(FILA * 2)
        cabeceraTabla(columnas)
        filas.forEach { celdas ->
            if (asegurar(FILA)) cabeceraTabla(columnas)
            fila(columnas, celdas, normal)
            canvas?.drawLine(MARGEN, y, ANCHO - MARGEN, y, linea)
        }
        y += 14f
    }

    private fun cabeceraTabla(columnas: List<Columna>) {
        canvas?.drawRect(MARGEN, y, ANCHO - MARGEN, y + FILA, fondoCabecera)
        fila(columnas, columnas.map { it.titulo }, negrita)
    }

    private fun fila(
        columnas: List<Columna>,
        celdas: List<String>,
        pincel: Paint,
    ) {
        var x = MARGEN + 3f
        columnas.zip(celdas).forEach { (columna, celda) ->
            val textoCelda = recortar(celda, pincel, columna.ancho - 6f)
            val xTexto = if (columna.derecha) x + columna.ancho - 6f - pincel.measureText(textoCelda) else x
            canvas?.drawText(textoCelda, xTexto, y + 12.5f, pincel)
            x += columna.ancho
        }
        y += FILA
    }

    /** Si no cabe, se corta con «…» (mejor que pisar la columna de al lado). */
    private fun recortar(
        s: String,
        pincel: Paint,
        ancho: Float,
    ): String {
        if (pincel.measureText(s) <= ancho) return s
        var corte = s
        while (corte.isNotEmpty() && pincel.measureText("$corte…") > ancho) corte = corte.dropLast(1)
        return "$corte…"
    }

    private companion object {
        /** Las clases que se cuentan en el resumen (las pausas y las que no cuentan, no). */
        val CLASES_RESUMEN =
            listOf(
                ClaseDescanso.DIARIO_NORMAL,
                ClaseDescanso.FRACCIONADO_COMPLETO,
                ClaseDescanso.DIARIO_REDUCIDO,
                ClaseDescanso.SEMANAL_NORMAL,
                ClaseDescanso.SEMANAL_REDUCIDO,
            )

        fun pincel(
            tamano: Float,
            negrita: Boolean = false,
            color: Int = Color.BLACK,
        ) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = tamano
            this.color = color
            typeface = if (negrita) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
    }
}
