package com.geosurface.tester.diagnostics

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.geosurface.tester.model.WebInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class WebViewDiagnostics(private val context: Context) {
    
    private var webInfo: WebInfo? = null
    private var continuation: ((WebInfo) -> Unit)? = null
    
    suspend fun runDiagnostics(): WebInfo = withContext(Dispatchers.Main) {
        withTimeoutOrNull(15000) {
            suspendCancellableCoroutine { cont ->
                continuation = { result ->
                    cont.resume(result)
                }
                
                val webView = WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    addJavascriptInterface(WebAppInterface(), "Android")
                }
                
                // Load the local HTML file with fingerprinting tests
                webView.loadUrl("file:///android_asset/webview_test.html")
            }
        } ?: WebInfo() // Return empty WebInfo if timeout
    }
    
    inner class WebAppInterface {
        @JavascriptInterface
        fun sendResults(
            jsTimezone: String?,
            intlLocale: String?,
            userAgent: String?,
            acceptLanguage: String?,
            canvasFingerprint: String?,
            webGlRenderer: String?,
            webGlVendor: String?
        ) {
            Log.d("WebViewDiagnostics", "Received WebView results")
            val result = WebInfo(
                jsTimezone = jsTimezone,
                intlLocale = intlLocale,
                userAgent = userAgent,
                acceptLanguage = acceptLanguage,
                canvasFingerprint = canvasFingerprint,
                webGlRenderer = webGlRenderer,
                webGlVendor = webGlVendor
            )
            continuation?.invoke(result)
            continuation = null
        }
    }
}
