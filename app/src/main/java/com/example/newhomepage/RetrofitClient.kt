package com.example.newhomepage.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // เปลี่ยน BASE_URL เป็น IP address ของเครื่องที่รัน Backend
    // หรือใช้ IP ของเครื่องที่รัน API (localhost ไม่สามารถใช้จาก emulator ได้โดยตรง)
    private const val BASE_URL = "http://10.0.2.2:3000/"  // 10.0.2.2 คือ IP ของ localhost จาก emulator

    // สร้าง OkHttpClient ที่มี logging และ timeout
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // สร้าง Retrofit instance
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // สร้าง ApiService
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}