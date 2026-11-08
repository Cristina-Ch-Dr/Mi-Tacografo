package com.cristinachdr.mitacografo.platform

import android.content.Context
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject

/**
 * Dónde va el PDF: al sitio que elige el conductor (Descargas, Drive…) o a una carpeta temporal
 * para compartirlo (WhatsApp, correo). Las rutas van como texto para poder probar el ViewModel sin Android.
 */
interface ArchivosInforme {
    /** @throws IOException si no se puede escribir. */
    suspend fun guardar(
        destino: String,
        bytes: ByteArray,
    )

    /**
     * Deja el PDF listo para compartir.
     *
     * @return la dirección `content://` que se pasa a la otra app.
     */
    suspend fun paraCompartir(
        nombre: String,
        bytes: ByteArray,
    ): String
}

class ArchivosInformeAndroid
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ArchivosInforme {
        override suspend fun guardar(
            destino: String,
            bytes: ByteArray,
        ) = withContext(Dispatchers.IO) {
            val salida =
                context.contentResolver.openOutputStream(destino.toUri(), "wt") ?: throw IOException("Sin destino")
            salida.use { it.write(bytes) }
        }

        override suspend fun paraCompartir(
            nombre: String,
            bytes: ByteArray,
        ): String =
            withContext(Dispatchers.IO) {
                // En la caché: Android la limpia solo si hace falta espacio. Se borra lo anterior
                // para no acumular informes viejos.
                val carpeta =
                    File(context.cacheDir, CARPETA).apply {
                        deleteRecursively()
                        mkdirs()
                    }
                val archivo = File(carpeta, nombre)
                archivo.writeBytes(bytes)
                FileProvider.getUriForFile(context, "${context.packageName}.archivos", archivo).toString()
            }

        private companion object {
            /** Tiene que coincidir con `res/xml/archivos_compartidos.xml`. */
            const val CARPETA = "informes"
        }
    }
