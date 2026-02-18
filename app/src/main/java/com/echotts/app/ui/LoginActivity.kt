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
            settings.userAgentString =
                "Mozilla/5.0 (Linux; Android 10; Pixel 4) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Mobile Safari/537.36"

            webViewClient = object : WebViewClient() {

                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    binding.progressBar.visibility = View.VISIBLE
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    binding.progressBar.visibility = View.GONE
                    url?.let { checkIfLoggedIn(it) }
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    return false // let the WebView handle all navigation
                }
            }

            // Start at the Amazon sign-in page, targeting the Alexa web app
            loadUrl(LOGIN_URL)
        }
    }

    private fun checkIfLoggedIn(url: String) {
        // After a successful login Amazon redirects to the Alexa home page
        val successIndicators = listOf(
            "alexa.amazon.com",
            "/spa/index.html",
            "echo.amazon"
        )
        if (successIndicators.any { url.contains(it, ignoreCase = true) }) {
            // Flush cookies to disk so they persist
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
        private const val LOGIN_URL =
            "https://alexa.amazon.com/spa/index.html"
    }
}
