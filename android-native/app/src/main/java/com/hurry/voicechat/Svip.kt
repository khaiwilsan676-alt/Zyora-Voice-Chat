package com.hurry.voicechat

import android.annotation.SuppressLint
import android.app.Activity
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Svip(onBack: () -> Unit = {}) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                addJavascriptInterface(object { @JavascriptInterface fun close() { (context as? Activity)?.runOnUiThread { onBack() } } }, "NativeSvip")
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()
                setBackgroundColor(android.graphics.Color.BLACK)
                loadDataWithBaseURL(
                    "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/",
                    """
                    <!doctype html><html><head><meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no, viewport-fit=cover">
                    <style>
                    *{box-sizing:border-box}html,body{margin:0;width:100%;height:100%;min-height:100%;background:#150C07;color:white;font-family:Arial,sans-serif;overflow-x:hidden}
                    .wrap{position:relative;width:100%;min-height:100vh;overflow:hidden}.top{height:40vh;position:relative;z-index:3}
                    .bg{position:absolute;inset:0;width:100%;height:100%;object-fit:cover}
                    header{position:relative;z-index:5;display:flex;align-items:center;justify-content:space-between;width:100%;padding:calc(max(env(safe-area-inset-top,0px),var(--status-bar-height,0px)) + 8px) 12px 8px}
                    .back{display:flex;align-items:center;justify-content:center;border:0;background:transparent;color:white;padding:4px;line-height:1;transition:opacity .15s}.back:active{opacity:.7}
                    .title{position:absolute;left:50%;transform:translateX(-50%);top:calc(max(env(safe-area-inset-top,0px),var(--status-bar-height,0px)) + 12px);font-size:20px;font-weight:900;letter-spacing:1px}
                    .icons{display:flex;gap:8px}.icons img{width:28px;height:28px;object-fit:contain}
                    .videoWrap{position:absolute;inset:0;display:flex;align-items:center;justify-content:center;z-index:2;pointer-events:none}
                    video{display:none}canvas{width:125vw;height:125vw;object-fit:contain;transform:translateY(0)}
                    .bottom{position:relative;z-index:1;min-height:60vh;display:flex;align-items:center;justify-content:center;background:#150C07;overflow:hidden}
                    .shine{position:absolute;inset:0;background:radial-gradient(120% 60% at 50% 0%,rgba(255,215,120,.18),rgba(255,215,120,.06) 35%,transparent 70%),linear-gradient(180deg,rgba(255,200,90,.08),transparent 40%);animation:shine 4s ease-in-out infinite}
                    @keyframes shine{0%,100%{opacity:.7}50%{opacity:1}}
                    .coming{position:relative;font-size:24px;font-weight:900;letter-spacing:4px;white-space:nowrap}
                    .middle{position:absolute;left:0;top:40vh;transform:translateY(-50%);width:100%;z-index:6;pointer-events:none}
                    </style></head><body><div class="wrap">
                    <section class="top"><img class="bg" src="file_00000000bd448211892548a9f0469619.png">
                    <header><button class="back" onclick="if(window.NativeSvip){NativeSvip.close()}else{history.back()}" aria-label="Back"><svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="#FFFFFF" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="19" y1="12" x2="5" y2="12"/><polyline points="12 19 5 12 12 5"/></svg></button><span class="title">SVIP</span><div class="icons"><img src="IMG_20260918_170725.png"><img src="IMG_20260918_170736.png"></div></header>
                    <div class="videoWrap"><video id="v" src="1000197130-background%20(1).mp4" muted loop playsinline autoplay></video><canvas id="c"></canvas></div></section>
                    <section class="bottom"><div class="shine"></div><span class="coming">SVIP COMING SOON</span></section>
                    <img class="middle" src="file_00000000e5d881faa59ce174257e208e.png"></div>
                    <script>
                    const v=document.getElementById('v'),c=document.getElementById('c'),x=c.getContext('2d',{willReadFrequently:true});
                    function draw(){if(v.readyState>=2&&v.videoWidth){if(c.width!==v.videoWidth||c.height!==v.videoHeight){c.width=v.videoWidth;c.height=v.videoHeight}
                    x.clearRect(0,0,c.width,c.height);x.drawImage(v,0,0,c.width,c.height);const f=x.getImageData(0,0,c.width,c.height),d=f.data;
                    for(let i=0;i<d.length;i+=4){const r=d[i],g=d[i+1],b=d[i+2];if(g>60&&g>r*1.15&&g>b*1.15)d[i+3]=0}x.putImageData(f,0,0)}requestAnimationFrame(draw)}
                    v.play().catch(()=>{});requestAnimationFrame(draw);
                    </script></body></html>
                    """.trimIndent(),
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        }
    )
}
