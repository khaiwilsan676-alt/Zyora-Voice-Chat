package com.hurry.voicechat

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Native container for the original Fruitparty.tsx screen.
 * Keeps the real game's artwork, layout, round state, records, rules,
 * socket events and wallet transaction logic instead of a fake Compose clone.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Fruitparty(
    onClose: () -> Unit,
    onMinimize: (() -> Unit)? = null,
    isMinimized: Boolean = false
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.BLACK)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.setSupportZoom(false)
                settings.allowFileAccess = false
                settings.allowContentAccess = true
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        view.evaluateJavascript(
                            "window.NativeFruitParty = {" +
                                "close: function(){ NativeFruitPartyBridge.close(); }," +
                                "minimize: function(){ NativeFruitPartyBridge.minimize(); }" +
                            "};",
                            null
                        )
                    }
                }
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun close() {
                        post { onClose() }
                    }

                    @JavascriptInterface
                    fun minimize() {
                        post { onMinimize?.invoke() }
                    }
                }, "NativeFruitPartyBridge")
                loadUrl("https://hurry-voice-chat.vercel.app/native-fruitparty")
            }
        },
        update = { view ->
            if (view.url.isNullOrBlank()) {
                view.loadUrl("https://hurry-voice-chat.vercel.app/native-fruitparty")
            }
        }
    )
}
