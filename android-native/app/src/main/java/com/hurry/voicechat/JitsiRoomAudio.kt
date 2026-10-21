package com.hurry.voicechat

import android.Manifest
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.content.pm.PackageManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.CookieManager
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

    // Do not force loudspeaker: WebRTC/Android can route to wired and Bluetooth headsets.
    DisposableEffect(context, roomId) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val previousMode = audioManager.mode
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        onDispose { audioManager.mode = previousMode }
    }
    var hasMicPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    var hasBluetoothPermission by remember {
        mutableStateOf(Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        hasMicPermission = results[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        hasBluetoothPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            results[Manifest.permission.BLUETOOTH_CONNECT] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    }
    val webViewState = remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(canSpeak, hasMicPermission, hasBluetoothPermission) {
        if (canSpeak && !hasMicPermission) {
            val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasBluetoothPermission) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            permissionLauncher.launch(permissions.toTypedArray())
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasBluetoothPermission) {
            permissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT))
        }
        webViewState.value?.evaluateJavascript(
            "window.setZyoraMuted && window.setZyoraMuted(${!(canSpeak && hasMicPermission)});", null
        )
    }

    AndroidView(
        modifier = Modifier.size(1.dp).alpha(0.01f),
        factory = { viewContext ->
            // Keep Jitsi room names stable and URL-safe even when backend IDs contain punctuation.
            val safeRoomId = roomId.trim().replace(Regex("[^A-Za-z0-9_-]"), "-").take(120).ifBlank { "default-room" }
            val roomName = JSONObject.quote("ZyoraVoice-$safeRoomId")
            val displayName = JSONObject.quote(userName.ifBlank { "User" })
            val initialMuted = !(canSpeak && hasMicPermission)
            WebView(viewContext).apply {
                // Jitsi's embedded conference uses cross-site cookies inside its WebView iframe.
                // Without third-party cookies some Android builds stay stuck before joining.
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                webViewState.value = this
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
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
                      let retryTimer = null;
                      let joinTimer = null;
                      let retryDelay = 1000;
                      let starting = false;
                      function scheduleReconnect() {
                        if (retryTimer) return;
                        starting = false;
                        if (joinTimer) { clearTimeout(joinTimer); joinTimer = null; }
                        // Schedule first so dispose-triggered leave events cannot start duplicate retries.
                        retryTimer = setTimeout(function() { retryTimer = null; startConference(); }, retryDelay);
                        retryDelay = Math.min(retryDelay * 2, 15000);
                        try { if (api) api.dispose(); } catch (e) {}
                        api = null;
                      }
                      window.setZyoraMuted = function(muted) {
                        desiredMuted = !!muted;
                        if (api) { try { api.executeCommand('setAudioMute', desiredMuted); } catch (e) {} }
                      };
                      function startConference() {
                        if (starting) return;
                        if (typeof JitsiMeetExternalAPI === 'undefined') { setTimeout(startConference, 500); return; }
                        starting = true;
                        try {
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
                        } catch (error) {
                          // A WebView/Jitsi constructor exception previously left starting=true forever.
                          console.error('Zyora Jitsi initialization failed; retrying', error);
                          starting = false;
                          api = null;
                          if (joinTimer) { clearTimeout(joinTimer); joinTimer = null; }
                          if (!retryTimer) {
                            retryTimer = setTimeout(function() {
                              retryTimer = null;
                              startConference();
                            }, retryDelay);
                            retryDelay = Math.min(retryDelay * 2, 15000);
                          }
                          return;
                        }
                        let localParticipantId = null;
                      api.addEventListener('videoConferenceJoined', function(event) {
                        starting = false;
                        if (joinTimer) { clearTimeout(joinTimer); joinTimer = null; }
                        retryDelay = 1000;
                        localParticipantId = event && event.id ? event.id : null;
                        window.setZyoraMuted(desiredMuted);
                      });
                      // Some WebView/network failures never emit connectionFailed.
                      // Retry if the room has not joined within 25 seconds.
                      joinTimer = setTimeout(function() {
                        if (starting) scheduleReconnect();
                      }, 25000);
                      api.addEventListener('connectionFailed', scheduleReconnect);
                      api.addEventListener('conferenceFailed', scheduleReconnect);
                      api.addEventListener('videoConferenceLeft', scheduleReconnect);
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
