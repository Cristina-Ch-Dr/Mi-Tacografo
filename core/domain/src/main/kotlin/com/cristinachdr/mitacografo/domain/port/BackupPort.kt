package com.cristinachdr.mitacografo.domain.port

import com.cristinachdr.mitacografo.domain.model.CopiaInvalidaException
import com.cristinachdr.mitacografo.domain.model.CopiaSeguridad

/**
 * Puerto de salida: convertir una [CopiaSeguridad] en texto y al revés.
 *
 * El dominio no sabe que es JSON: la implementación con kotlinx.serialization vive en `core:data`.
 * Leer y escribir el archivo lo hace la app (Android elige dónde se guarda).
 */
interface BackupPort {
    fun codificar(copia: CopiaSeguridad): String

    /** @throws CopiaInvalidaException si el texto no es una copia válida. */
    fun decodificar(texto: String): CopiaSeguridad
}
