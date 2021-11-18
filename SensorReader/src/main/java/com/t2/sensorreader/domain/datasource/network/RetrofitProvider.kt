package com.t2.sensorreader.domain.datasource.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private val loggingInterceptor by lazy {
    HttpLoggingInterceptor()
        .apply { level = HttpLoggingInterceptor.Level.BODY }
}


private val okHttpClient by lazy {
    OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .callTimeout(60, TimeUnit.SECONDS)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
}

val retrofit by lazy {
    Retrofit.Builder()
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .baseUrl("http://stgavailo-behavior.t2.sa/")
        .build()
}

inline fun <reified T : Any> buildApi(): T = retrofit.create(T::class.java)