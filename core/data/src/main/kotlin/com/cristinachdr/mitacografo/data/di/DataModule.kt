package com.cristinachdr.mitacografo.data.di

import android.content.Context
import androidx.room.Room
import com.cristinachdr.mitacografo.data.room.AppDatabase
import com.cristinachdr.mitacografo.data.room.jornada.JornadaDao
import com.cristinachdr.mitacografo.data.room.jornada.RoomJornadaRepository
import com.cristinachdr.mitacografo.domain.port.JornadaRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Le dice a Hilt qué implementación usar para cada puerto del dominio.
 *
 * Es el único sitio donde se conectan los puertos con sus adaptadores:
 * el resto de la app solo pide `JornadaRepository` y no sabe que detrás está Room.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindJornadaRepository(impl: RoomJornadaRepository): JornadaRepository

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(
            @ApplicationContext context: Context,
        ): AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NOMBRE).build()

        @Provides
        fun provideJornadaDao(database: AppDatabase): JornadaDao = database.jornadaDao()
    }
}
