package com.hurry.voicechat

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Uses the actual Wildparty.tsx screen so the native app renders the same game UI,
 * assets, history, wallet interactions and game controls as the real app.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Wildparty(
    onClose: () -> Unit = {},
    onMinimize: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
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
                webViewClient = WebViewClient()
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun close() {
                        post { onClose() }
                    }

                    @JavascriptInterface
                    fun minimize() {
                        post { onMinimize() }
                    }
                }, "NativeWildParty")
                loadUrl("https://hurry-voice-chat.vercel.app/native-wildparty")
            }
        },
        update = { view ->
            if (view.url.isNullOrBlank()) {
                view.loadUrl("https://hurry-voice-chat.vercel.app/native-wildparty")
            }
        }
    )
}
