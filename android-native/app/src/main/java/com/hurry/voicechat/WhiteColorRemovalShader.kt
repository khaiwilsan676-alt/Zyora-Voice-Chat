package com.hurry.voicechat
import androidx.compose.runtime.Composable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
@Composable fun WhiteColorRemovalShader(imageSrc:String,threshold:Float=0.9f,className:Modifier=Modifier){AsyncImage(model=imageSrc,contentDescription=null,modifier=className.size(120.dp),contentScale=ContentScale.Fit)}