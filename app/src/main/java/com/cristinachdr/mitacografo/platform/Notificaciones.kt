package com.cristinachdr.mitacografo.platform

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cristinachdr.mitacografo.MainActivity
import com.cristinachdr.mitacografo.R

/**
 * Canal y notificaciones de los avisos de jornada.
 *
 * Desde Android 8 el sonido y la vibración son del **canal**, no de cada notificación, y una vez
 * creado la app ya no puede cambiarlos (solo el usuario, en los ajustes de Android). Por eso, para
 * poner el sonido propio (MT-45) se crea un canal nuevo y se borra el antiguo.
 */
object Notificaciones {
    /** v2: con el sonido propio y la vibración larga. */
    const val CANAL_AVISOS = "avisos_jornada_v2"

    /** El de la v0.1, con el sonido normal de notificación. */
    private const val CANAL_ANTIGUO = "avisos_jornada"

    /** Larga-corta-larga: se distingue de la vibración de un mensaje. Pausa, vibra, pausa… (ms). */
    private val VIBRACION = longArrayOf(0, 700, 250, 300, 250, 700)

    /** `android.resource://…/raw/aviso_jornada`: el sonido generado con `herramientas/generar_sonido_aviso.py`. */
    fun sonido(context: Context): Uri =
        Uri
            .Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(context.packageName)
            .appendPath("raw")
            .appendPath("aviso_jornada")
            .build()

    /** Se llama al arrancar la app. Crear un canal que ya existe no hace nada (ni cambia lo que eligió el usuario). */
    fun crearCanal(context: Context) {
        val gestor = context.getSystemService(NotificationManager::class.java)
        val canal =
            NotificationChannel(
                CANAL_AVISOS,
                context.getString(R.string.canal_avisos_nombre),
                // Alta: suena, vibra y aparece encima de la pantalla.
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.canal_avisos_descripcion)
                setSound(
                    sonido(context),
                    AudioAttributes
                        .Builder()
                        // Como una notificación: respeta el modo silencio y «No molestar».
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                enableVibration(true)
                vibrationPattern = VIBRACION
                enableLights(true)
            }
        gestor.createNotificationChannel(canal)
        gestor.deleteNotificationChannel(CANAL_ANTIGUO)
    }

    /**
     * El sonido que tiene **ahora** el canal: el nuestro, o el que haya elegido el conductor en los
     * ajustes de Android. `null` = sin sonido.
     */
    fun sonidoActual(context: Context): Uri? =
        context.getSystemService(NotificationManager::class.java).getNotificationChannel(CANAL_AVISOS)?.sound

    /** Pantalla de Android con el sonido, la vibración y demás del canal de avisos, para cambiarlos. */
    fun intentAjustesCanal(context: Context): Intent =
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, CANAL_AVISOS)

    /**
     * Muestra el aviso. Si no hay permiso de notificaciones, no hace nada
     * (la pantalla de Inicio ya avisa de que faltan permisos).
     */
    @SuppressLint("MissingPermission") // se comprueba con PermisosAvisos justo antes
    fun mostrarAviso(
        context: Context,
        id: Int,
        titulo: String,
        texto: String,
    ) {
        if (!PermisosAvisos.notificacionesPermitidas(context)) return

        // Tocar la notificación abre la app.
        val intentApp =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val abrirApp =
            PendingIntent.getActivity(
                context,
                0,
                intentApp,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notificacion =
            NotificationCompat
                .Builder(context, CANAL_AVISOS)
                .setSmallIcon(R.drawable.ic_aviso)
                .setContentTitle(titulo)
                .setContentText(texto)
                .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(abrirApp)
                .setAutoCancel(true)
                .build()
        NotificationManagerCompat.from(context).notify(id, notificacion)
    }
}
