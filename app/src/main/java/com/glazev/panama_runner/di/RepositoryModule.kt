package com.glazev.panama_runner.di

import com.glazev.panama_runner.data.repository.FirebaseAuthRepository
import com.glazev.panama_runner.data.repository.FirebaseProgressRepository
import com.glazev.panama_runner.domain.repository.AuthRepository
import com.glazev.panama_runner.domain.repository.ProgressRepository
import com.glazev.panama_runner.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProgressRepository(
        impl: FirebaseProgressRepository
    ): ProgressRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: FirebaseAuthRepository
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: com.glazev.panama_runner.data.settings.DataStoreSettingsRepository
    ): SettingsRepository
}
