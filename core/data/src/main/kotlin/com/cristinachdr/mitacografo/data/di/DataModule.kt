package com.cristinachdr.mitacografo.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.cristinachdr.mitacografo.data.datastore.DataStoreSettingsRepository
import com.cristinachdr.mitacografo.data.room.AppDatabase
import com.cristinachdr.mitacografo.data.room.jornada.JornadaDao
import com.cristinachdr.mitacografo.data.room.jornada.RoomJornadaRepository
import com.cristinachdr.mitacografo.domain.port.JornadaRepository
import com.cristinachdr.mitacografo.domain.port.SettingsRepository
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

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(
            @ApplicationContext context: Context,
        ): AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NOMBRE).build()

        /** Un único DataStore por archivo en toda la app (si hubiera dos, fallaría). */
        @Provides
        @Singleton
        fun provideDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> = PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("ajustes") }

        @Provides
        fun provideJornadaDao(database: AppDatabase): JornadaDao = database.jornadaDao()
    }
}
