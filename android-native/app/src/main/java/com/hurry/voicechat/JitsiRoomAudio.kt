package com.hurry.voicechat

import android.Manifest
import android.content.pm.PackageManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.JavascriptInterface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import org.json.JSONObject

/** Hidden audio-only Jitsi conference; existing Compose room UI stays unchanged. */
@Composable
fun JitsiRoomAudio(roomId: String, userName: String, canSpeak: Boolean, onSpeakingChanged: (Boolean) -> Unit = {}) {
    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasMicPermission = granted
    }
    val webViewState = remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(canSpeak, hasMicPermission) {
        if (canSpeak && !hasMicPermission) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        webViewState.value?.evaluateJavascript(
            "window.setZyoraMuted && window.setZyoraMuted(${!(canSpeak && hasMicPermission)});", null
        )
    }

    AndroidView(
        modifier = Modifier.size(1.dp).alpha(0.01f),
        factory = { viewContext ->
            val roomName = JSONObject.quote("HurryVoice-$roomId")
            val displayName = JSONObject.quote(userName.ifBlank { "User" })
            val initialMuted = !(canSpeak && hasMicPermission)
            WebView(viewContext).apply {
                webViewState.value = this
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.javaScriptCanOpenWindowsAutomatically = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                addJavascriptInterface(object {
                    @JavascriptInterface fun onVoiceActivity(active: Boolean) {
                        android.os.Handler(android.os.Looper.getMainLooper()).post { onSpeakingChanged(active) }
                    }
                }, "ZyoraVoiceBridge")
                webChromeClient = object : WebChromeClient() {
                    override fun onPermissionRequest(request: PermissionRequest) {
                        post {
                            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                            if (granted && request.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                                request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
                            } else request.deny()
                        }
                    }
                }
                webViewClient = WebViewClient()
                val html = """
                    <!doctype html>
                    <html><head><meta name="viewport" content="width=device-width, initial-scale=1"></head>
                    <body style="margin:0;background:transparent">
                    <div id="meet" style="width:1px;height:1px;overflow:hidden"></div>
                    <script src="https://meet.jit.si/external_api.js"></script>
                    <script>
                      let desiredMuted = ${initialMuted};
                      let api = null;
                      window.setZyoraMuted = function(muted) {
                        desiredMuted = !!muted;
                        if (api) { try { api.executeCommand('setAudioMute', desiredMuted); } catch (e) {} }
                      };
                      function startConference() {
                        if (typeof JitsiMeetExternalAPI === 'undefined') { setTimeout(startConference, 500); return; }
                        api = new JitsiMeetExternalAPI('meet.jit.si', {
                          roomName: ${roomName},
                          parentNode: document.getElementById('meet'),
                          width: '1px',
                          height: '1px',
                          configOverwrite: {
                            startAudioOnly: true,
                            startWithAudioMuted: true,
                            startWithVideoMuted: true,
                            prejoinConfig: { enabled: false },
                            disableDeepLinking: true
                          },
                          interfaceConfigOverwrite: { TOOLBAR_BUTTONS: [] },
                          userInfo: { displayName: ${displayName} }
                        });
                        let localParticipantId = null;
                      api.addEventListener('videoConferenceJoined', function(event) {
                        localParticipantId = event && event.id ? event.id : null;
                        window.setZyoraMuted(desiredMuted);
                      });
                      api.addEventListener('audioLevelChanged', function(event) {
                        if (localParticipantId && event && event.participantId === localParticipantId) {
                          try { ZyoraVoiceBridge.onVoiceActivity(!desiredMuted && event.audioLevel > 0.08); } catch (e) {}
                        }
                      });
                      api.addEventListener('audioMuteStatusChanged', function(event) {
                        if (event && event.muted) { try { ZyoraVoiceBridge.onVoiceActivity(false); } catch (e) {} }
                      });
                      }
                      startConference();
                    </script>
                    </body></html>
                """.trimIndent()
                loadDataWithBaseURL("https://meet.jit.si/", html, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webViewState.value = webView
            webView.evaluateJavascript(
                "window.setZyoraMuted && window.setZyoraMuted(${!(canSpeak && hasMicPermission)});", null
            )
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            webViewState.value?.let { it.stopLoading(); it.loadUrl("about:blank"); it.destroy() }
            webViewState.value = null
        }
    }
}
