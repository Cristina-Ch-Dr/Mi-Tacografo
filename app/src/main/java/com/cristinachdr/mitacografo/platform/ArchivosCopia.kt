package com.cristinachdr.mitacografo.platform

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject

/**
 * Leer y escribir el archivo de la copia en el sitio que eligió el conductor (Descargas, Drive…).
 *
 * Las rutas van como texto (`content://…`) para que el ViewModel no dependa de Android
 * y se pueda probar con un archivo falso.
 */
interface ArchivosCopia {
    /** @throws IOException si no se puede escribir. */
    suspend fun escribir(
        destino: String,
        texto: String,
    )

    /** @throws IOException si no se puede leer o el archivo es demasiado grande para ser una copia. */
    suspend fun leer(origen: String): String
}

class ArchivosCopiaAndroid
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ArchivosCopia {
        override suspend fun escribir(
            destino: String,
            texto: String,
        ) = withContext(Dispatchers.IO) {
            // "wt": si el archivo ya existía, se sobrescribe entero (no se mezcla con lo de antes).
            val salida =
                context.contentResolver.openOutputStream(destino.toUri(), "wt") ?: throw IOException("Sin destino")
            salida.use { it.write(texto.toByteArray(Charsets.UTF_8)) }
        }

        override suspend fun leer(origen: String): String =
            withContext(Dispatchers.IO) {
                val entrada =
                    context.contentResolver.openInputStream(origen.toUri()) ?: throw IOException("Sin archivo")
                entrada.use {
                    // Una copia de años de jornadas ocupa unos pocos MB. Algo mucho mayor no es una copia
                    // (un vídeo elegido por error) y leerlo entero podría cerrar la app por falta de memoria.
                    val bytes = it.readNBytesCompat(TAMANO_MAXIMO + 1)
                    if (bytes.size > TAMANO_MAXIMO) throw IOException("Archivo demasiado grande")
                    String(bytes, Charsets.UTF_8)
                }
            }

        private companion object {
            const val TAMANO_MAXIMO = 20 * 1024 * 1024
        }
    }

/** `InputStream.readNBytes` solo existe desde Android 13; esto funciona desde Android 8. */
private fun InputStream.readNBytesCompat(maximo: Int): ByteArray {
    val salida = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (salida.size() < maximo) {
        val leidos = read(buffer, 0, minOf(buffer.size, maximo - salida.size()))
        if (leidos < 0) break
        salida.write(buffer, 0, leidos)
    }
    return salida.toByteArray()
}
