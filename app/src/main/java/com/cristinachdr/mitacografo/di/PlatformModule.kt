package com.cristinachdr.mitacografo.di

import com.cristinachdr.mitacografo.domain.port.AlarmScheduler
import com.cristinachdr.mitacografo.platform.AndroidAlarmScheduler
import com.cristinachdr.mitacografo.platform.ArchivosCopia
import com.cristinachdr.mitacografo.platform.ArchivosCopiaAndroid
import com.cristinachdr.mitacografo.platform.ProbadorAvisos
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Puertos del dominio que necesitan Android y por eso se implementan en `app`. */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformModule {
    @Binds
    @Singleton
    abstract fun bindAlarmScheduler(impl: AndroidAlarmScheduler): AlarmScheduler

    @Binds
    abstract fun bindProbadorAvisos(impl: AndroidAlarmScheduler): ProbadorAvisos

    @Binds
    abstract fun bindArchivosCopia(impl: ArchivosCopiaAndroid): ArchivosCopia
}
