package com.hurry.voicechat

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

@Composable
fun GreenColorRemovalImage(
    imageUrl:String,
    contentDescription:String?,
    modifier:Modifier=Modifier,
    contentScale:ContentScale=ContentScale.Fit,
    threshold:Float=0.45f
){
    var bitmap by remember(imageUrl,threshold){mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)}
    LaunchedEffect(imageUrl,threshold){
        bitmap=withContext(Dispatchers.IO){
            runCatching{
                val source=URL(imageUrl).openStream().use{BitmapFactory.decodeStream(it)} ?: return@runCatching null
                val result=source.copy(android.graphics.Bitmap.Config.ARGB_8888,true)
                val pixels=IntArray(result.width*result.height)
                result.getPixels(pixels,0,result.width,0,0,result.width,result.height)
                for(i in pixels.indices){
                    val p=pixels[i]
                    val r=android.graphics.Color.red(p)
                    val g=android.graphics.Color.green(p)
                    val b=android.graphics.Color.blue(p)
                    if(g>70 && g>r*1.2f && g>b*1.2f){
                        pixels[i]=android.graphics.Color.argb(0,r,g,b)
                    }
                }
                result.setPixels(pixels,0,result.width,0,0,result.width,result.height)
                result.asImageBitmap()
            }.getOrNull()
        }
    }
    val loaded=bitmap
    if(loaded!=null) Image(bitmap=loaded,contentDescription=contentDescription,modifier=modifier,contentScale=contentScale)
}
