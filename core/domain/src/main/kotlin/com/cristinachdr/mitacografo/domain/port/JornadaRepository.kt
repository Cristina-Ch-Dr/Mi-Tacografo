package com.cristinachdr.mitacografo.domain.port

import com.cristinachdr.mitacografo.domain.model.Jornada
import kotlinx.coroutines.flow.Flow

/**
 * Puerto de salida para guardar y leer jornadas.
 *
 * El dominio solo conoce esta interfaz. La implementación con Room vive en `core:data`.
 * Los `Flow` vuelven a emitir cada vez que cambian los datos, y la UI se actualiza sola.
 */
interface JornadaRepository {
    /** La jornada abierta, o `null` si no hay ninguna. */
    fun observarAbierta(): Flow<Jornada?>

    /** Todas las jornadas, de la más reciente a la más antigua. */
    fun observarTodas(): Flow<List<Jornada>>

    suspend fun obtenerPorId(id: Long): Jornada?

    suspend fun obtenerAbierta(): Jornada?

    /** La última jornada finalizada. Sirve para proponer el km de inicio de la siguiente. */
    suspend fun obtenerUltimaFinalizada(): Jornada?

    /**
     * Inserta la jornada si su id es [Jornada.SIN_ID] o la actualiza si ya existe.
     *
     * @return el id de la jornada guardada.
     */
    suspend fun guardar(jornada: Jornada): Long

    suspend fun borrar(id: Long)
}
