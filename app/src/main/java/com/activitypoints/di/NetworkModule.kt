package com.activitypoints.di

import com.activitypoints.BuildConfig
import com.activitypoints.data.api.AuthInterceptor
import com.activitypoints.data.api.CertificateDeserializer
import com.activitypoints.data.api.StudentDeserializer
import com.activitypoints.data.api.StudentApi
import com.activitypoints.data.api.TutorApi
import com.activitypoints.data.api.TutorPendingCertDeserializer
import com.activitypoints.models.Certificate
import com.activitypoints.models.Student
import com.activitypoints.models.TutorPendingCert
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideGson(): com.google.gson.Gson =
        GsonBuilder()
            // Student deserializer handles batch/branch as populated objects { _id, name }
            // instead of plain strings — this is the root cause of the missing student name.
            .registerTypeAdapter(Student::class.java, StudentDeserializer())
            .registerTypeAdapter(Certificate::class.java, CertificateDeserializer())
            .registerTypeAdapter(TutorPendingCert::class.java, TutorPendingCertDeserializer())
            .create()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: com.google.gson.Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideStudentApi(retrofit: Retrofit): StudentApi =
        retrofit.create(StudentApi::class.java)

    @Provides
    @Singleton
    fun provideTutorApi(retrofit: Retrofit): TutorApi =
        retrofit.create(TutorApi::class.java)
}
