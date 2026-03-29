package com.example.babyparenting.network.api


import com.example.babyparenting.network.model.AdviceRequest
import com.example.babyparenting.network.model.AdviceResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface BabyApi {
    @POST("predict")
    suspend fun getAdvice(@Body request: AdviceRequest): Response<AdviceResponse>
}

object RetrofitProvider {

    private const val BASE_URL = "http://192.168.1.21:8000/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val babyApi: BabyApi by lazy {
        retrofit.create(BabyApi::class.java)
    }
}
