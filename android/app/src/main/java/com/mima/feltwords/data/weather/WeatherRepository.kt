package com.mima.feltwords.data.weather

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Request
import com.mima.feltwords.data.api.NetworkModule

/**
 * 天气驱动的主题模式 —— 对齐 iOS ThemeMode。
 */
enum class ThemeMode(val title: String) {
    Automatic("跟随时间"),
    Light("浅色"),
    Dark("深色"),
}

/**
 * 天气服务 —— 对齐 iOS WeatherService。
 * 通过 ipapi.co（IP 定位）+ open-meteo（天气数据），免密钥免 GPS 权限。
 *
 * 功能：
 * - 获取城市名、气温、天气代码
 * - 根据 is_day 字段判断昼夜
 * - 支持手动切换浅色/深色/跟随时间
 * - 天气图标映射（WMO 代码 → Material Icons 描述）
 */
class WeatherRepository(context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val preferences = context.getSharedPreferences("feltwords.weather", Context.MODE_PRIVATE)

    // ──────────────── 状态 ────────────────
    // 启动时用上次缓存的天气秒显，避免约 5 秒的「定位中…」占位闪烁，随后后台静默刷新。

    private val _temperature = MutableStateFlow(
        preferences.getInt(CACHE_TEMP_KEY, Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
    )
    val temperature: StateFlow<Int?> = _temperature.asStateFlow()

    private val _city = MutableStateFlow(preferences.getString(CACHE_CITY_KEY, null))
    val city: StateFlow<String?> = _city.asStateFlow()

    private val _weatherCode = MutableStateFlow(preferences.getInt(CACHE_CODE_KEY, 0))
    val weatherCode: StateFlow<Int> = _weatherCode.asStateFlow()

    private val _themeMode = MutableStateFlow(
        preferences.getString(THEME_MODE_KEY, null)
            ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
            ?: ThemeMode.Automatic
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _isDay = MutableStateFlow(preferences.getBoolean(CACHE_IS_DAY_KEY, true))
    val isDayFlow: StateFlow<Boolean> = _isDay.asStateFlow()

    private val _didLoad = MutableStateFlow(false)
    val didLoad: StateFlow<Boolean> = _didLoad.asStateFlow()

    /** 当前有效的昼夜状态（受手动强制或自动昼夜影响） */
    val isDay: Boolean
        get() = when (_themeMode.value) {
            ThemeMode.Automatic -> _isDay.value
            ThemeMode.Light -> true
            ThemeMode.Dark -> false
        }

    // ──────────────── 公开方法 ────────────────

    fun toggleLightDark() {
        setThemeMode(if (isDay) ThemeMode.Dark else ThemeMode.Light)
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        preferences.edit().putString(THEME_MODE_KEY, mode.name).apply()
    }

    suspend fun loadIfNeeded() {
        if (_didLoad.value) return
        load()
    }

    suspend fun load() {
        try {
            val location = fetchLocation()
            val weather = fetchWeather(location.latitude, location.longitude)
            val temperature = weather.current.temperature_2m.toInt()
            val isDay = weather.current.is_day != 0
            val code = weather.current.weather_code
            _temperature.value = temperature
            _isDay.value = isDay
            _weatherCode.value = code
            _city.value = location.city
            _didLoad.value = true
            cacheWeather(temperature, isDay, code, location.city)
        } catch (_: Exception) {
            // 失败时保留上次缓存值（已在初始化时读取），UI 不回退到占位
        }
    }

    // ──────────────── 网络请求 ────────────────

    /**
     * 多源 IP 定位：ipapi.co 偶发触发 Cloudflare 人机校验（返回 HTML 而非 JSON），
     * 故按序回退到 ipwho.is（同样免密钥、HTTPS、字段名一致）。
     * 仅当响应确为 JSON 且经纬度有效时才采用。
     */
    private suspend fun fetchLocation(): IPLocation = withContext(Dispatchers.IO) {
        var lastError: Exception? = null
        for (url in LOCATION_PROVIDERS) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "FeltWords/1.0")
                    .header("Accept", "application/json")
                    .build()
                val body = NetworkModule.okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    response.body?.string()
                }
                if (body != null && body.trimStart().startsWith("{")) {
                    val location = json.decodeFromString<IPLocation>(body)
                    if (location.latitude != 0.0 || location.longitude != 0.0) {
                        return@withContext location
                    }
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: Exception("所有 IP 定位服务均不可用")
    }

    private suspend fun fetchWeather(latitude: Double, longitude: Double): MeteoResponse =
        withContext(Dispatchers.IO) {
            val url = "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$latitude&longitude=$longitude" +
                    "&current=temperature_2m,weather_code,is_day"
            val request = Request.Builder().url(url).build()
            val response = NetworkModule.okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("天气请求失败: ${response.code}")
            val body = response.body?.string() ?: throw Exception("响应为空")
            json.decodeFromString<MeteoResponse>(body)
        }

    /** 成功拉取后落盘，供下次启动秒显。 */
    private fun cacheWeather(temperature: Int, isDay: Boolean, code: Int, city: String?) {
        preferences.edit()
            .putInt(CACHE_TEMP_KEY, temperature)
            .putBoolean(CACHE_IS_DAY_KEY, isDay)
            .putInt(CACHE_CODE_KEY, code)
            .putString(CACHE_CITY_KEY, city)
            .apply()
    }

    companion object {
        private const val THEME_MODE_KEY = "feltwords.themeMode"
        private const val CACHE_TEMP_KEY = "feltwords.weather.temp"
        private const val CACHE_CITY_KEY = "feltwords.weather.city"
        private const val CACHE_CODE_KEY = "feltwords.weather.code"
        private const val CACHE_IS_DAY_KEY = "feltwords.weather.isDay"

        /** IP 定位服务按序回退；字段名 city/latitude/longitude 一致，可复用同一模型。 */
        private val LOCATION_PROVIDERS = listOf(
            "https://ipapi.co/json/",
            "https://ipwho.is/",
        )

        /**
         * WMO 天气代码 → Material Icons 名称描述（供 UI 层映射）。
         * 对齐 iOS symbol(forWMOCode:isDay:)。
         */
        fun weatherIconDescription(code: Int, isDay: Boolean): String = when (code) {
            0 -> if (isDay) "晴天" else "夜晚"
            1, 2, 3 -> if (isDay) "多云白天" else "多云夜晚"
            45, 48 -> "雾"
            in 51..67 -> if (isDay) "小雨" else "夜晚小雨"
            in 71..77 -> "雪"
            in 80..82 -> "大雨"
            in 95..99 -> "雷雨"
            else -> "多云"
        }
    }
}

// ──────────────── 数据模型 ────────────────

@Serializable
private data class IPLocation(
    val city: String? = null,
    val latitude: Double,
    val longitude: Double,
)

@Serializable
private data class MeteoResponse(
    val current: MeteoCurrent,
)

@Serializable
private data class MeteoCurrent(
    val temperature_2m: Double,
    val weather_code: Int,
    val is_day: Int,
)
