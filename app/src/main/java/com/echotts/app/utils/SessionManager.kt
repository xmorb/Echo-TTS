package com.echotts.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.webkit.CookieManager

/**
 * Manages the Amazon session cookies used to authenticate API calls.
 * The login WebView populates the system CookieManager; we persist
 * the important cookies so they survive app restarts.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_LOGGED_IN, false)

    fun markLoggedIn() {
        prefs.edit().putBoolean(KEY_LOGGED_IN, true).apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
        CookieManager.getInstance().removeAllCookies(null)
    }

    /**
     * Build a cookie header string from the WebKit CookieManager for the
     * given URL (alexa.amazon.com). Retrofit's cookie jar will handle
     * subsequent requests; this is a convenience for the first call.
     */
    fun getCookiesForUrl(url: String): String {
        return CookieManager.getInstance().getCookie(url) ?: ""
    }

    /**
     * Extract the csrf token from the cookies stored by the WebKit
     * CookieManager. Amazon requires the csrf value as a header on
     * mutating requests.
     */
    fun getCsrfToken(): String {
        val cookieString = CookieManager.getInstance()
            .getCookie("https://www.amazon.com") ?: return "-1"
        return cookieString.split(";")
            .map { it.trim() }
            .firstOrNull { it.startsWith("csrf=") }
            ?.removePrefix("csrf=")
            ?: "-1"
    }

    companion object {
        private const val PREFS_NAME = "echo_tts_session"
        private const val KEY_LOGGED_IN = "logged_in"
    }
}
