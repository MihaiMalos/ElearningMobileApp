package com.elearning.ui.data.api

import com.elearning.ui.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * API configuration and Retrofit setup
 * Configure BASE_URL when backend is ready
 */
object ApiConfig {


    private const val BASE_URL = "http://10.0.2.2:8000/api/v1/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val authInterceptor = Interceptor { chain ->
        val request = chain.request()
        val token = TokenManager.getToken()
        
        val newRequest = if (token != null) {
            val tokenType = TokenManager.getTokenType()
            val capitalizedType = tokenType.replaceFirstChar { it.uppercase() }
            request.newBuilder()
                .addHeader("Authorization", "$capitalizedType $token")
                .build()
        } else {
            request
        }
        chain.proceed(newRequest)
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
