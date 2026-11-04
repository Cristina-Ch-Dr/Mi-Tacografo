package com.cristinachdr.mitacografo.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
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
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
