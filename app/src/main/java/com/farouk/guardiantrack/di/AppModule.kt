package com.farouk.guardiantrack.di

import android.content.Context
import androidx.room.Room
import com.farouk.guardiantrack.data.local.GuardianDatabase
import com.farouk.guardiantrack.data.local.dao.EmergencyContactDao
import com.farouk.guardiantrack.data.local.dao.IncidentDao
import com.farouk.guardiantrack.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing Room database and DAO instances as singletons.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GuardianDatabase =
        Room.databaseBuilder(
            context,
            GuardianDatabase::class.java,
            GuardianDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration(true)
         .build()

    @Provides
    @Singleton
    fun provideIncidentDao(database: GuardianDatabase): IncidentDao =
        database.incidentDao()

    @Provides
    @Singleton
    fun provideEmergencyContactDao(database: GuardianDatabase): EmergencyContactDao =
        database.emergencyContactDao()

    @Provides
    @Singleton
    fun provideUserDao(database: GuardianDatabase): UserDao =
        database.userDao()
}
