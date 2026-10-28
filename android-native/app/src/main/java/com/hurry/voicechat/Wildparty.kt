package com.hurry.voicechat

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Loads the real Wildparty.tsx game UI in the native room.
 * Keep the original web game's artwork, layout, controls and game logic.
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
                settings.javaScriptCanOpenWindowsAutomatically = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.setSupportZoom(false)
                settings.allowFileAccess = false
                settings.allowContentAccess = true
                settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT

                // The original game uses browser storage and live socket requests.
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

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
                loadUrl("https://zyora-voice-chat.vercel.app/native-wildparty")
            }
        },
        update = { view ->
            if (view.url.isNullOrBlank()) {
                view.loadUrl("https://zyora-voice-chat.vercel.app/native-wildparty")
            }
        }
    )
}
