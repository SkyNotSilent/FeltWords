package com.mima.feltwords.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.mima.feltwords.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * 构造 OkHttpClient + Retrofit 实例。
 * - 90s 超时
 * - Authorization: Bearer 拦截器
 * - kotlinx-serialization converter
 * - baseUrl = https://apihub.agnes-ai.com/v1/
 */
object NetworkModule {

    private const val BASE_URL = "https://apihub.agnes-ai.com/v1/"

    /** 宽松的 JSON 解析器：忽略未知字段 */
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** 鉴权拦截器 */
    private val authInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer ${BuildConfig.AGNES_API_KEY}")
            .build()
        chain.proceed(request)
    }

    /** 日志拦截器（仅 Debug） */
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val agnesApi: AgnesApi = retrofit.create(AgnesApi::class.java)
}
