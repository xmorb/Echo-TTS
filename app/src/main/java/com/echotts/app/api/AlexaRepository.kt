package com.echotts.app.api

import android.webkit.CookieManager
import com.echotts.app.api.models.Device
import com.echotts.app.api.models.DeviceListResponse
import com.echotts.app.api.models.TtsRequest
import com.echotts.app.utils.SessionManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int = -1) : ApiResult<Nothing>()
}

class AlexaRepository(private val sessionManager: SessionManager) {

    private val alexaBaseUrl = "https://alexa.amazon.com/"

    // Sync cookies from the Android WebKit CookieManager into OkHttp
    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            // Cookies are already managed by the WebKit CookieManager during login
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val rawCookies = CookieManager.getInstance().getCookie(url.toString()) ?: return emptyList()
            return rawCookies.split(";").mapNotNull { part ->
                val trimmed = part.trim()
                val eqIdx = trimmed.indexOf('=')
                if (eqIdx < 0) return@mapNotNull null
                val name = trimmed.substring(0, eqIdx).trim()
                val value = trimmed.substring(eqIdx + 1).trim()
                Cookie.Builder()
                    .name(name)
                    .value(value)
                    .domain(url.host)
                    .build()
            }
        }
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val csrf = sessionManager.getCsrfToken()
            val request = chain.request().newBuilder()
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("csrf", csrf)
                .header("Referer", "https://alexa.amazon.com/")
                .header("Origin", "https://alexa.amazon.com")
                .build()
            chain.proceed(request)
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(alexaBaseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api: AlexaApiService = retrofit.create(AlexaApiService::class.java)

    suspend fun getDevices(): ApiResult<List<Device>> {
        return try {
            val response = api.getDevices()
            if (response.isSuccessful) {
                val devices = response.body()?.devices ?: emptyList()
                // Filter to only Echo/Alexa speaker-type devices
                val echoDevices = devices.filter { it.isEchoDevice }
                ApiResult.Success(echoDevices)
            } else {
                ApiResult.Error(
                    "Failed to load devices: ${response.message()}",
                    response.code()
                )
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error fetching devices")
        }
    }

    suspend fun speakOnDevice(text: String, device: Device): ApiResult<Unit> {
        return try {
            val request = TtsRequest.forDevice(text, device)
            val response = api.sendTts(request)
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error(
                    "TTS failed: ${response.message()}",
                    response.code()
                )
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error sending TTS")
        }
    }

    suspend fun speakOnAllDevices(text: String, devices: List<Device>): ApiResult<Unit> {
        return try {
            val request = TtsRequest.forAllDevices(text, devices)
            val response = api.sendTts(request)
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error(
                    "Broadcast TTS failed: ${response.message()}",
                    response.code()
                )
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error sending broadcast TTS")
        }
    }

    companion object {
        // Must match the desktop UA used in LoginActivity's WebView so that all
        // requests appear to come from the same browser session that captured the cookies.
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Safari/537.36"
    }
}
