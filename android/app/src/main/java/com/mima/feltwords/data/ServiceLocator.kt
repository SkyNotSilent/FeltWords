package com.mima.feltwords.data

import android.app.Application
import com.mima.feltwords.data.api.AgnesRepository
import com.mima.feltwords.data.store.ImageStore
import com.mima.feltwords.data.store.LocalStore
import com.mima.feltwords.data.store.ProfileStore
import com.mima.feltwords.data.weather.WeatherRepository
import com.mima.feltwords.speech.TtsManager

/**
 * 简易服务定位器 —— 持有 Application context，惰性提供各数据层实例。
 * 在 FeltApplication.onCreate 中调用 init(app)。
 * 不引入 DI 框架，保持 P2 轻量。
 */
object ServiceLocator {

    private lateinit var app: Application

    fun init(application: Application) {
        app = application
    }

    val localStore: LocalStore by lazy { LocalStore(app) }

    val imageStore: ImageStore by lazy { ImageStore(app) }

    val profileStore: ProfileStore by lazy { ProfileStore(app) }

    val agnesRepository: AgnesRepository by lazy { AgnesRepository(imageStore) }

    val ttsManager: TtsManager by lazy { TtsManager(app) }

    val weatherRepository: WeatherRepository by lazy { WeatherRepository() }
}
