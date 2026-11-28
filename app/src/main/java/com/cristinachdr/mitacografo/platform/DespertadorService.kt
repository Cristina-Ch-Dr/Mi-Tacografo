package com.cristinachdr.mitacografo.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.net.toUri
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.domain.port.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * El despertador sonando (MT-57): como el de un reloj, suena el tono elegido en bucle y vibra hasta que se
 * apaga (o 10 minutos, para no gastar la batería si no está). Funciona con la app cerrada y el móvil bloqueado.
 *
 * Es un servicio en primer plano: su notificación lleva la pantalla de alarma ([AlarmaActivity]), que Android
 * abre encima del bloqueo, y el botón «Apagar». El sonido usa el volumen de **alarma** del móvil, no el de
 * notificaciones: suena aunque esté en silencio.
 */
@AndroidEntryPoint
class DespertadorService : Service() {
    @Inject lateinit var settings: SettingsRepository

    private var reproductor: MediaPlayer? = null
    private var vibrador: Vibrator? = null
    private val temporizador = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == ACCION_APAGAR) {
            apagar()
            return START_NOT_STICKY
        }
        crearCanal(this)
        ServiceCompat.startForeground(
            this,
            ID_NOTIFICACION,
            notificacion(),
            if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            },
        )
        sonar()
        temporizador.postDelayed(::apagar, DURACION_MAXIMA_MS)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        parar()
        super.onDestroy()
    }

    private fun sonar() {
        parar()
        // Es un momento: los ajustes ya están en memoria. El servicio no puede esperar a una corrutina para sonar.
        val elegido = runBlocking { settings.observar().first().tonoDespertador }?.toUri()
        reproductor =
            runCatching { reproductor(elegido ?: tonoPorDefecto()) }
                .recoverCatching { reproductor(tonoPorDefecto()) }
                .getOrNull()
        vibrador =
            getSystemService(Vibrator::class.java)?.also {
                it.vibrate(VibrationEffect.createWaveform(VIBRACION, 0))
            }
    }

    private fun reproductor(tono: Uri): MediaPlayer =
        MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            setDataSource(this@DespertadorService, tono)
            isLooping = true
            setWakeMode(this@DespertadorService, PowerManager.PARTIAL_WAKE_LOCK)
            prepare()
            start()
        }

    private fun tonoPorDefecto(): Uri =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

    private fun parar() {
        temporizador.removeCallbacksAndMessages(null)
        reproductor?.run {
            runCatching { stop() }
            release()
        }
        reproductor = null
        vibrador?.cancel()
        vibrador = null
    }

    private fun apagar() {
        parar()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
        // Si la pantalla de alarma sigue abierta, se cierra sola.
        sendBroadcast(Intent(AlarmaActivity.ACCION_CERRAR).setPackage(packageName))
    }

    private fun notificacion() =
        NotificationCompat
            .Builder(this, CANAL)
            .setSmallIcon(R.drawable.ic_aviso)
            .setContentTitle(getString(R.string.despertador_titulo))
            .setContentText(getString(R.string.despertador_texto))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setFullScreenIntent(pantallaAlarma(), true)
            .setContentIntent(pantallaAlarma())
            .addAction(0, getString(R.string.accion_apagar), apagarIntent(this))
            .build()

    private fun pantallaAlarma(): PendingIntent =
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, AlarmaActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        private const val CANAL = "despertador"
        private const val ID_NOTIFICACION = 5_000
        private const val ACCION_APAGAR = "com.cristinachdr.mitacografo.APAGAR_DESPERTADOR"
        private const val DURACION_MAXIMA_MS = 10 * 60 * 1000L
        private val VIBRACION = longArrayOf(0, 800, 600)

        fun intentSonar(context: Context): Intent = Intent(context, DespertadorService::class.java)

        fun apagarIntent(context: Context): PendingIntent =
            PendingIntent.getService(
                context,
                1,
                Intent(context, DespertadorService::class.java).setAction(ACCION_APAGAR),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        /** Canal sin sonido propio: el sonido lo pone el servicio, con el tono elegido y en bucle. */
        private fun crearCanal(context: Context) {
            val canal =
                NotificationChannel(
                    CANAL,
                    context.getString(R.string.canal_despertador_nombre),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    setSound(null, null)
                    enableVibration(false)
                }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
    }
}
