package com.activitypoints.di

import com.activitypoints.data.api.StudentApi
import com.activitypoints.data.api.TutorApi
import com.activitypoints.data.local.TokenStore
import com.activitypoints.data.repository.AuthRepository
import com.activitypoints.data.repository.AuthRepositoryImpl
import com.activitypoints.data.repository.CertificateRepository
import com.activitypoints.data.repository.CertificateRepositoryImpl
import com.activitypoints.data.repository.TutorRepository
import com.activitypoints.data.repository.TutorRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindCertificateRepository(impl: CertificateRepositoryImpl): CertificateRepository

    @Binds @Singleton
    abstract fun bindTutorRepository(impl: TutorRepositoryImpl): TutorRepository
}
