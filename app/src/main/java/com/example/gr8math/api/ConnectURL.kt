package com.example.gr8math.api

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ConnectURL {
    private const val BASE_URL =  "http://10.0.2.2:8000/"
    //"https://gr8mathbackend.onrender.com"
    //    "http://10.0.2.2:8000/"
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val publicClient: OkHttpClient
        get() = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    private val protectedClient: OkHttpClient
        get() = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
//            .addInterceptor { chain ->
//                val prefs = appContext?.getSharedPreferences("user_session", Context.MODE_PRIVATE)
//                val token = prefs?.getString("auth_token", null)
//                Log.d("TOKEN_CHECK", "TOKEN SENT: $token")
//
//                val originalRequest = chain.request()
//                val newRequest = if (!token.isNullOrEmpty()) {
//                    originalRequest.newBuilder()
//                        .addHeader("Authorization", "Bearer $token")
//                        .build()
//                } else {
//                    originalRequest
//                }
//
//                chain.proceed(newRequest)
//            }
            .build()

    // Public API (for /login, /register, /password/*)
    val publicApi: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(publicClient)
            .build()
            .create(ApiService::class.java)
    }

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(protectedClient)
            .build()
            .create(ApiService::class.java)
    }
}