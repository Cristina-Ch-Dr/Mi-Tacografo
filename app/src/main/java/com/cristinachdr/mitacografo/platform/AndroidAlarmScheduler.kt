package com.cristinachdr.mitacografo.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.cristinachdr.mitacografo.domain.model.AvisoProgramado
import com.cristinachdr.mitacografo.domain.port.AlarmScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/**
 * Adaptador de plataforma: implementa el puerto [AlarmScheduler] con `AlarmManager`.
 *
 * - Con permiso de alarmas exactas: `setExactAndAllowWhileIdle`, que suena a su hora
 *   aunque el móvil esté en reposo.
 * - Sin permiso: `setWindow` con un margen de [MARGEN_SIN_PERMISO]. Mejor un aviso
 *   unos minutos tarde que ninguno.
 *
 * Cada aviso usa un hueco fijo (0, 1, 2…) como `requestCode`. Para cancelar basta con
 * recorrer los huecos: no hace falta guardar nada en la base de datos.
 */
class AndroidAlarmScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : AlarmScheduler,
        ProbadorAvisos {
        private val alarmManager = context.getSystemService(AlarmManager::class.java)

        override suspend fun programar(avisos: List<AvisoProgramado>) {
            avisos.take(MAX_AVISOS).forEachIndexed { hueco, aviso ->
                programarEn(hueco, aviso.disparo, AlarmReceiver.intent(context, aviso.jornadaId, aviso.desdeInicio))
            }
        }

        override suspend fun cancelar(jornadaId: Long) {
            repeat(MAX_AVISOS) { hueco ->
                val existente =
                    PendingIntent.getBroadcast(
                        context,
                        hueco,
                        Intent(context, AlarmReceiver::class.java),
                        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
                    )
                if (existente != null) {
                    alarmManager.cancel(existente)
                    existente.cancel()
                }
            }
        }

        /** Aviso de prueba (Ajustes → «Probar aviso»): comprueba que las notificaciones llegan. */
        override fun programarPrueba(dentroDe: Duration) {
            programarEn(
                HUECO_PRUEBA,
                Instant.now().plus(dentroDe),
                AlarmReceiver.intent(context, AlarmReceiver.JORNADA_PRUEBA, Duration.ZERO),
            )
        }

        private fun programarEn(
            hueco: Int,
            cuando: Instant,
            intent: Intent,
        ) {
            val pendiente =
                PendingIntent.getBroadcast(
                    context,
                    hueco,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            val ms = cuando.toEpochMilli()
            if (PermisosAvisos.alarmasExactasPermitidas(context)) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pendiente)
            } else {
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, ms, MARGEN_SIN_PERMISO.toMillis(), pendiente)
            }
        }

        private companion object {
            /** Más que de sobra para los 4 avisos por defecto. */
            const val MAX_AVISOS = 8
            const val HUECO_PRUEBA = 99
            val MARGEN_SIN_PERMISO: Duration = Duration.ofMinutes(10)
        }
    }
