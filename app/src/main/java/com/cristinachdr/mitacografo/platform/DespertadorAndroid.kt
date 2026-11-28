package com.cristinachdr.mitacografo.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.cristinachdr.mitacografo.MainActivity
import com.cristinachdr.mitacografo.domain.port.Despertador
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject

/**
 * El despertador (MT-57) con `AlarmManager.setAlarmClock`: es la alarma de un reloj despertador.
 * Suena a su hora exacta aunque el móvil esté en reposo, y Android enseña el ⏰ en la barra de estado.
 *
 * Al sonar, [DespertadorReceiver] arranca [DespertadorService], que hace sonar el tono y abre la pantalla
 * de alarma. Sin el permiso de alarmas exactas, se pone una alarma normal (puede llegar unos minutos tarde):
 * mejor eso que ninguna.
 */
class DespertadorAndroid
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Despertador {
        private val alarmManager = context.getSystemService(AlarmManager::class.java)

        override fun programar(hora: Instant) {
            val ms = hora.toEpochMilli()
            if (PermisosAvisos.alarmasExactasPermitidas(context)) {
                alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(ms, abrirApp()), alSonar())
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, alSonar())
            }
        }

        override fun cancelar() {
            alarmManager.cancel(alSonar())
        }

        private fun alSonar(): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                CODIGO,
                Intent(context, DespertadorReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        /** Al tocar el ⏰ de la barra de estado o del reloj de Android, se abre la app. */
        private fun abrirApp(): PendingIntent =
            PendingIntent.getActivity(
                context,
                CODIGO,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        private companion object {
            /** Distinto de los huecos de los avisos de jornada (0–8 y 99). */
            const val CODIGO = 500
        }
    }
