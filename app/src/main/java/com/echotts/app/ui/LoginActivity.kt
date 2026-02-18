package com.echotts.app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.echotts.app.databinding.ActivityLoginBinding
import com.echotts.app.utils.SessionManager

/**
 * Presents the Amazon sign-in page in a WebView. Once the user successfully
 * logs in and is redirected to the Alexa home page we capture the session
 * cookies and return to MainActivity.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager

    // Prevents a false-positive login detection on the initial page load.
    // The Alexa SPA URL (alexa.amazon.com/spa/index.html) would match our
    // success indicators immediately — we must first see the Amazon sign-in
    // page before treating a return to that URL as successful authentication.
    private var hasSeenSignInPage = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Enable cookies in the WebView
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(binding.webView, true)

        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            // Desktop UA is required: the mobile Android UA causes Amazon's server
            // to respond with an intent:// redirect to the native Alexa app instead
            // of serving the web sign-in page. Desktop UA gets the full web flow,
            // which means cookies are captured by the WebView and can be reused for
            // subsequent API calls.
            settings.userAgentString = DESKTOP_USER_AGENT

            webViewClient = object : WebViewClient() {

                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    binding.progressBar.visibility = View.VISIBLE
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    binding.progressBar.visibility = View.GONE
                    url?.let { checkIfLoggedIn(it) }
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val scheme = request?.url?.scheme ?: return false
                    // Block every non-http(s) URL (intent://, market://, etc.) entirely.
                    // We must never hand off to a native app: authentication must happen
                    // inside this WebView so that cookies are captured here.
                    return scheme != "http" && scheme != "https"
                }
            }

            // Start at the Amazon sign-in page, targeting the Alexa web app
            loadUrl(LOGIN_URL)
        }
    }

    private fun checkIfLoggedIn(url: String) {
        // Ignore anything that isn't a normal https URL
        if (!url.startsWith("https://")) return

        // Amazon's sign-in/auth pages live under amazon.com/ap/.
        // Record when we've passed through sign-in so we can distinguish
        // "initial load of the Alexa SPA URL" from "redirected back after login".
        if (url.contains("amazon.com/ap/", ignoreCase = true)) {
            hasSeenSignInPage = true
            return
        }

        // Don't fire until the user has actually been through the sign-in flow
        if (!hasSeenSignInPage) return

        val successIndicators = listOf(
            "alexa.amazon.com",
            "/spa/index.html",
            "echo.amazon"
        )
        if (successIndicators.any { url.contains(it, ignoreCase = true) }) {
            // Flush cookies to disk so they persist across process restarts
            CookieManager.getInstance().flush()
            sessionManager.markLoggedIn()
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    companion object {
        // Amazon sign-in page that then redirects to the Alexa web app
        private const val LOGIN_URL = "https://alexa.amazon.com/spa/index.html"

        // Desktop Chrome UA. Must match AlexaRepository.USER_AGENT so all
        // requests look like they come from the same browser session.
        const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Safari/537.36"
    }
}
