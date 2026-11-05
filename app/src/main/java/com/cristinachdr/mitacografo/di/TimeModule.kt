package com.cristinachdr.mitacografo.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import javax.inject.Singleton

/**
 * Proporciona el reloj del sistema. Inyectar [Clock] en vez de llamar a
 * `Instant.now()` permite usar un reloj fijo en los tests.
 */
@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    @Provides
    @Singleton
    fun provideClock(): Clock = RelojDelMovil
}

/**
 * Reloj que sigue la zona horaria **actual** del móvil.
 *
 * `Clock.systemDefaultZone()` se queda con la zona del momento en que se crea: si el conductor
 * pasa de España a Ucrania sin cerrar la app, seguiría viendo la hora de España.
 * Este reloj pregunta la zona cada vez.
 */
private object RelojDelMovil : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun withZone(zone: ZoneId): Clock = system(zone)

    override fun instant(): Instant = Instant.now()
}
