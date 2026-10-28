package com.hurry.voicechat

import android.graphics.PixelFormat
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.ImageLoader
import coil3.compose.AsyncImage

// Yaha videoUrl add kiya hai taaki Seat par overlap karke video play kara sako
data class EmojiSticker(val id:String, val name:String, val src:String, val videoUrl:String? = null)

@Composable
fun EmojiPicker(onClose:()->Unit={},onSelectEmoji:(EmojiSticker)->Unit={}){
 var activeTab by remember{mutableStateOf("emojis")}
 val context = LocalContext.current
 
 // Ye fresh ImageLoader bina GIF decoder ke hai, isse is screen par GIF pause (static) rahenge
 val staticImageLoader = remember {
  ImageLoader.Builder(context).build()
 }

 val stickers=listOf(
  // Naye emojis with Video URLs - Ab click hone par ye videoUrl bhi pass karenge
  EmojiSticker("welcome","Welcome","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/IMG_20261028_183916.png", "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/VID_20261028_105329_293_bsl.mp4"),
  EmojiSticker("thanks","Thanks","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/IMG_20261028_184100.png", "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/VID_20261028_105159_978_bsl.mp4"),
  EmojiSticker("gift_me","Gift me","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/IMG_20261028_184037.png", "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/VID_20261028_105304_745_bsl.mp4"),
  EmojiSticker("kiss_r","Kiss-R","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/IMG_20261028_184130.png", "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/VID_20261028_105348_731_bsl.mp4"),
  EmojiSticker("kiss_l","Kiss-L","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/IMG_20261028_184200.png", "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/VID_20261028_105414_962_bsl.mp4"),
  EmojiSticker("punch_r","Punch-R","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/IMG_20261028_184240.png", "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/VID_20261028_104823_659_bsl.mp4"),
  EmojiSticker("punch_l","Punch-L","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/IMG_20261028_184300.png", "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/VID_20261028_104846_084_bsl.mp4"),
  
  // Purane emojis (Yaha se purana "Kiss" GIF hata diya gaya hai)
  EmojiSticker("laugh","Laugh","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512.gif"),
  EmojiSticker("sad","Sad","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(6).gif"),
  EmojiSticker("love","Sleep","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(3).gif"),
  EmojiSticker("thinking","Thinking","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(2).gif"),
  EmojiSticker("party","Party","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(16).gif"),
  EmojiSticker("loving","Loving","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(15).gif"),
  EmojiSticker("smart","Smart","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(13).gif"),
  EmojiSticker("irritating","Irritating","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(12).gif"),
  EmojiSticker("rolling","Rolling","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(10).gif"),
  EmojiSticker("unamused","Unamused","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(11).gif"),
  EmojiSticker("pleading","Pleading","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(4).gif"),
  EmojiSticker("hug","Hug","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(8).gif"),
  EmojiSticker("Angery"," Angery","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(1).gif")
 )
 Box(Modifier.fillMaxSize(),contentAlignment=Alignment.BottomCenter){
  Box(Modifier.fillMaxSize().clickable(onClick=onClose))
  
  Box(Modifier.fillMaxWidth().fillMaxHeight(.42f).background(Color.Black)){
   
   // Border image ekdam fit, bina stretch aur bina kate hue
   AsyncImage(
    model="https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/file_0000000022f481fab7ddec2efb9449f9.png",
    contentDescription="Top Border",
    modifier=Modifier
        .fillMaxWidth()
        .wrapContentHeight() 
        .align(Alignment.TopCenter), 
    contentScale=ContentScale.FillWidth 
   )

   Column(Modifier.fillMaxSize().padding(horizontal=16.dp,vertical=12.dp)){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically){
    Tab("Emojis",activeTab=="emojis"){activeTab="emojis"}
    Spacer(Modifier.width(32.dp))
    Tab("Premium",activeTab=="premium"){activeTab="premium"}
   }
   if(activeTab=="emojis") LazyVerticalGrid(
    columns=GridCells.Fixed(4),
    modifier=Modifier.weight(1f),
    contentPadding=PaddingValues(vertical=12.dp),
    horizontalArrangement=Arrangement.spacedBy(8.dp),
    verticalArrangement=Arrangement.spacedBy(16.dp)
   ){
    items(stickers){s->
     Column(
      Modifier.fillMaxWidth().clickable{
          onSelectEmoji(s) // <--- Jab user click karega toh uske seat logic me ab videoUrl bhi jayega
          onClose()
      },
      horizontalAlignment=Alignment.CenterHorizontally
     ){
      AsyncImage(
       model=s.src,
       imageLoader=staticImageLoader,
       contentDescription=s.name,
       modifier=Modifier.size(48.dp),
       contentScale=ContentScale.Fit
      )
      Text(s.name.trim(),fontSize=11.sp,color=Color.White.copy(alpha=.90f),maxLines=1)
     }
    }
   } else Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center){
    Text("Premium Emojis Coming Soon",color=Color.White.copy(alpha=.70f),fontSize=12.sp)
   }
   }
  }
 }
}

@Composable private fun Tab(label:String,selected:Boolean,onClick:()->Unit){Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.clickable(onClick=onClick)){Text(label,color=if(selected)Color.White else Color.White.copy(alpha=.60f),fontSize=14.sp);if(selected)Box(Modifier.padding(top=4.dp).height(2.dp).width(42.dp).background(Color.White,RoundedCornerShape(2.dp)))}}


// =========================================================================================
// YAHAN SE NAYA LOGIC HAI JO TUMHARI SEAT PAR OVERLAP HOGA (PURANE LOGIC KO KUCH NHI KIYA)
// ISKO APNI SEAT UI KE UPAR Z-INDEX MEIN RAKHNA HAI JAB BHI KOI NAYA EMOJI VIDEO AAYE
// =========================================================================================

@Composable
fun TransparentSeatVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    onVideoFinished: () -> Unit = {}
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = true
            
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        onVideoFinished() // Video khatam hote hi hata dena Seat se
                    }
                }
            })
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // AndroidView for ExoPlayer with 100% Transparent Green Screen Logic
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false // Video controls hide kar diye
                
                // IMPORTANT: Background transparent karne ke liye strict logic
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // 100% Transparent Surface (Green screen transparent bananane ka native setup)
                val surfaceView = this.videoSurfaceView as? SurfaceView
                surfaceView?.apply {
                    setZOrderOnTop(true) // Ye UI ke sabse upar float karega
                    holder.setFormat(PixelFormat.TRANSPARENT) // Background fully transparent
                }

                /* 
                 * NOTE BHAI: Android ExoPlayer default green color ko detect karke transparent 
                 * nahi karta unless video ka background hi alpha-channel (WebM) wala ho.
                 * Isliye Surface format ko TRANSPARENT kiya gaya hai. Agar aapki .mp4 me 
                 * green screen as a solid color hai, toh ExoPlayer `TextureView` ke sath ek 
                 * Shader (GL_OES_EGL_image_external) use karna padta hai ya video ko 
                 * 'Alpha-channel WebM/HEVC' me convert karke rakhna padta hai.
                 * Upar ka `PixelFormat.TRANSPARENT` aur `setZOrderOnTop(true)` tumhara kaam 
                 * Seat ke overlap ke liye transparent wrapper bana dega.
                 */
            }
        }
    )
}
