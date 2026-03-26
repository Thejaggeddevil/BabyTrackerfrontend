package com.example.babyparenting.di

import android.content.Context
import com.example.babyparenting.data.api.MillionaireApiService
import com.example.babyparenting.data.repository.MillionaireRepository
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MillionaireModule {

    /**
     * Provides Gson instance with lenient parsing
     */
    @Singleton
    @Provides
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .serializeNulls()  // Include null values in serialization
            .create()
    }

    /**
     * Provides OkHttpClient with logging interceptor
     */
    @Singleton
    @Provides
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    /**
     * Provides Retrofit instance with Gson converter
     */
    @Singleton
    @Provides
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(MillionaireApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    /**
     * Provides MillionaireApiService (Retrofit proxy)
     */
    @Singleton
    @Provides
    fun provideMillionaireApiService(retrofit: Retrofit): MillionaireApiService {
        return retrofit.create(MillionaireApiService::class.java)
    }

    /**
     * Provides MillionaireRepository with API service and Context
     */
    @Singleton
    @Provides
    fun provideMillionaireRepository(
        @ApplicationContext context: Context,
        apiService: MillionaireApiService
    ): MillionaireRepository {
        return MillionaireRepository(apiService, context)
    }
}