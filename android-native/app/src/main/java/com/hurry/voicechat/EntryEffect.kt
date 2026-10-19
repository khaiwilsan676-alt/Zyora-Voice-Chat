package com.hurry.voicechat
import android.net.Uri
import android.view.ViewGroup
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun EntryEffect(vehicleUrl:String,userName:String="",enabled:Boolean=true,onComplete:()->Unit={}){
 if(!enabled || vehicleUrl.isBlank()) return
 Box(Modifier.fillMaxSize().background(Color.Transparent)){
  AndroidView(factory={ctx->
   VideoView(ctx).apply{
    layoutParams=ViewGroup.LayoutParams(-1,-1)
    setVideoURI(Uri.parse(vehicleUrl))
    setOnPreparedListener{ mp->mp.isLooping=false;mp.isMutedSafe();start()}
    setOnCompletionListener{onComplete()}
    setOnErrorListener{_,_,_->onComplete();true}
   }
  },modifier=Modifier.fillMaxSize())
 }
}
private fun android.media.MediaPlayer.isMutedSafe(){setVolume(0f,0f)}
