package com.ecocp.capstoneenvirotrack.api

import android.R.attr.level
import com.ecocp.capstoneenvirotrack.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object OpenAiClient {
    private const val OPENAI_BASE_URL = "https://api.openai.com/"

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply{
            level = HttpLoggingInterceptor.Level.NONE
        })
        .addInterceptor{ chain ->
            val newRequest = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(newRequest)
        }.build()

    val instance: OpenAiApiService by lazy{
        Retrofit.Builder()
            .baseUrl(OPENAI_BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenAiApiService::class.java)
    }
}