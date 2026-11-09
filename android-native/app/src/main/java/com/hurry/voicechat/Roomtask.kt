package com.hurry.voicechat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import java.util.Calendar

private data class NativeRoomTaskRow(val title:String,val reward:String,val icon:String)

@Composable fun Roomtask(onBack:()->Unit={}){
    val raw="https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/"
    var rules by remember{mutableStateOf(false)}
    var hour by remember{mutableIntStateOf(0)}
    var minute by remember{mutableIntStateOf(0)}
    var second by remember{mutableIntStateOf(0)}
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit){
        while(true){
            val now=Calendar.getInstance()
            val target=Calendar.getInstance().apply{
                val days=(Calendar.MONDAY-get(Calendar.DAY_OF_WEEK)+7)%7
                add(Calendar.DAY_OF_MONTH,if(days==0)7 else days)
                set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,0);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0)
            }
            val left=((target.timeInMillis-now.timeInMillis).coerceAtLeast(0L))/1000L
            hour=((left/3600)%24).toInt();minute=((left/60)%60).toInt();second=(left%60).toInt()
            delay(1000)
        }
    }
    
    val tasks=listOf(
        NativeRoomTaskRow("10 Users Enter the Room","10,000","file_00000000858082118b5c81b85cd6d2a8.png"),
        NativeRoomTaskRow("5 Users Enter the Room for Two Consecutive Days","20,000","file_00000000858082118b5c81b85cd6d2a8.png"),
        NativeRoomTaskRow("Successfully Share on WhatsApp","5,000","file_0000000019a0821193463686d6fc9184.png"),
        NativeRoomTaskRow("Successfully Share on Facebook","5,000","file_0000000019a0821193463686d6fc9184.png"),
        NativeRoomTaskRow("3 New Users Enter the Room","10,000","file_00000000858082118b5c81b85cd6d2a8.png"),
        NativeRoomTaskRow("1 New Follower of the Room","2,000","file_00000000858082118b5c81b85cd6d2a8.png"),
        NativeRoomTaskRow("3 New Users Follow the Room","10,000","file_00000000858082118b5c81b85cd6d2a8.png"),
        NativeRoomTaskRow("Spend 10 Minutes on Mic","5,000","file_00000000f8d88211ba5ff45c06383e5f.png"),
        NativeRoomTaskRow("Spend 30 Minutes on Mic","25,000","file_00000000f8d88211ba5ff45c06383e5f.png"),
        NativeRoomTaskRow("Spend 60 Minutes on Mic","50,000","file_00000000f8d88211ba5ff45c06383e5f.png"),
        NativeRoomTaskRow("10 New Users on Mic for 10 Minutes at the Same Time","50,000","file_00000000f8d88211ba5ff45c06383e5f.png"),
        NativeRoomTaskRow("5 New Followers of the Room","25,000","file_00000000858082118b5c81b85cd6d2a8.png"),
        NativeRoomTaskRow("3 Users on Mic at the Same Time","10,000","file_00000000f8d88211ba5ff45c06383e5f.png"),
        NativeRoomTaskRow("Send a Gift of 500","2,000","file_0000000081f48211afe58f6348196b55.png"),
        NativeRoomTaskRow("Send Gifts of 5,000","10,000","file_0000000081f48211afe58f6348196b55.png"),
        NativeRoomTaskRow("Successfully Invite 1 User on Mic","500","file_0000000019a0821193463686d6fc9184.png"),
        NativeRoomTaskRow("Successfully Invite 3 New Users on Mic","10,000","file_0000000019a0821193463686d6fc9184.png"),
        NativeRoomTaskRow("Send 10 Gifts on Mic","20,000","file_0000000081f48211afe58f6348196b55.png"),
        NativeRoomTaskRow("New User Sends a Gift of 1,000 on Mic","5,000","file_0000000081f48211afe58f6348196b55.png"),
        NativeRoomTaskRow("Successfully Invite 10 People to the Mic","25,000","file_0000000019a0821193463686d6fc9184.png"),
        NativeRoomTaskRow("3 Users on Mic at the Same Time for 10 Minutes","10,000","file_00000000f8d88211ba5ff45c06383e5f.png"),
        NativeRoomTaskRow("Send 1 Gift","1,000","file_0000000081f48211afe58f6348196b55.png")
    )
    
    Box(Modifier.fillMaxSize().background(Color(0xFF380308))){
        
        // Pura page ek sath scroll karega
        Box(Modifier.fillMaxSize().verticalScroll(scrollState)) {
            
            // Background Image Header
            Box(modifier = Modifier.fillMaxWidth().height(480.dp)){
                AsyncImage(model=raw+"file_00000000cb748211bf0120855b80f449.png",contentDescription=null,modifier=Modifier.fillMaxSize().drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xFF380308)),
                            startY = size.height * 0.95f,
                            endY = size.height
                        )
                    )
                },contentScale=ContentScale.Crop,alignment=Alignment.TopCenter)
            }
            
            // Main Content
            Column(Modifier.fillMaxWidth(), horizontalAlignment=Alignment.CenterHorizontally){
                
                // Top Bar
                Box(Modifier.fillMaxWidth().padding(top=24.dp,start=8.dp,end=12.dp)){
                    IconButton(onClick=onBack, modifier=Modifier.align(Alignment.CenterStart)){
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    IconButton(onClick={rules=true}, modifier=Modifier.align(Alignment.CenterEnd)){
                        Box(modifier = Modifier.size(24.dp).border(1.5.dp, Color.White, androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                            Text("?", color = Color.White, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(305.dp))

                // Rewards image
                AsyncImage(model=raw+"file_00000000f2908208a7b6a2b73c3bbf36.png",contentDescription="Room task rewards",modifier=Modifier.padding(horizontal = 8.dp).fillMaxWidth().height(138.dp),contentScale=ContentScale.Fit)
                
                // ROOM TASK FRAME (Ab sirf 1.08f rakha hai, halka sa wide bas)
                Box(Modifier.fillMaxWidth().wrapContentWidth(unbounded = true), contentAlignment=Alignment.Center){
                    AsyncImage(
                        model=raw+"file_00000000680881faa3dfdb17cce60858.png",
                        contentDescription="Room Task frame",
                        modifier=Modifier.fillMaxWidth(1.08f), 
                        contentScale=ContentScale.FillWidth 
                    )
                    Text("ROOM TASK",color=Color.White,fontSize=16.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Black,letterSpacing=1.2.sp, modifier=Modifier.offset(y=(-4).dp).shadow(4.dp, spotColor=Color.Black.copy(alpha=0.9f)))
                }
                
                // Countdown
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(3.dp),modifier=Modifier.offset(y=(-15).dp).padding(vertical=0.dp)){
                    Text("Countdown",color=Color(0xFFF5B8B8),fontSize=10.sp)
                    listOf(hour to "Hour",minute to "Minute",second to "Second").forEach{pair->
                        Box(Modifier.background(Brush.verticalGradient(listOf(Color(0xFF8F1A1A),Color(0xFF470A0A))),RoundedCornerShape(4.dp)).padding(horizontal=5.dp,vertical=3.dp)){Text(pair.first.toString(),color=Color(0xFFFFE0E0),fontSize=11.sp)}
                        Text(pair.second,color=Color(0xFFF5B8B8),fontSize=10.sp)
                    }
                }
                
                // Tasks List
                Column(
                    modifier = Modifier.fillMaxWidth().offset(y=(-25).dp), 
                    verticalArrangement = Arrangement.spacedBy((-60).dp) 
                ) {
                    tasks.forEach { task ->
                        Box(Modifier.fillMaxWidth().height(175.dp)){
                            
                            // Left and Right Border Image
                            AsyncImage(
                                model=raw+"IMG_20260903_105647.png",
                                contentDescription=null,
                                modifier=Modifier.align(Alignment.CenterStart).offset(x = (-15).dp, y = (-12).dp).width(45.dp).fillMaxHeight(1.1f),
                                contentScale=ContentScale.FillBounds
                            )
                            AsyncImage(
                                model=raw+"IMG_20260903_105705.png",
                                contentDescription=null,
                                modifier=Modifier.align(Alignment.CenterEnd).offset(x = 15.dp, y = (-12).dp).width(45.dp).fillMaxHeight(1.1f),
                                contentScale=ContentScale.FillBounds
                            )
                            
                            // Task Card Background
                            Box(Modifier.fillMaxSize().padding(horizontal=10.dp)) {
                                AsyncImage(model=raw+"file_000000004fd0821198ed4e26d5008b16.png",contentDescription="Task background",modifier=Modifier.fillMaxSize(),contentScale=ContentScale.FillBounds)
                                
                                Row(Modifier.fillMaxSize().padding(start=8.dp,end=16.dp),verticalAlignment=Alignment.CenterVertically){
                                    val lowerTitle = task.title.lowercase()
                                    val isMic = lowerTitle.contains("mic")
                                    val isShare = lowerTitle.contains("share")
                                    val isGift = lowerTitle.contains("gift")
                                    val isUserOrFollower = lowerTitle.contains("user") || lowerTitle.contains("follower")

                                    val iconPath = when {
                                        isMic -> "IMG_20261109_003921.png"
                                        isShare -> "IMG_20261109_003805.png"
                                        isGift -> "IMG_20261109_003851.png"
                                        isUserOrFollower -> "IMG_20261109_122201.png"
                                        else -> task.icon
                                    }

                                    AsyncImage(
                                        model=raw+iconPath,
                                        contentDescription=task.title,
                                        modifier=Modifier.size(54.dp).padding(start=8.dp),
                                        contentScale=ContentScale.Fit
                                    )
                                    
                                    Text(task.title,color=Color.White,fontSize=16.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold,modifier=Modifier.weight(1f).padding(start=12.dp), maxLines=3, style=androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(color = Color.Black.copy(alpha=0.5f), blurRadius = 4f, offset = androidx.compose.ui.geometry.Offset(0f,2f))), lineHeight=18.sp)
                                    
                                    Column(horizontalAlignment=Alignment.CenterHorizontally){
                                        Row(verticalAlignment=Alignment.CenterVertically){
                                            AsyncImage(model=raw+"file_00000000e56882119c217d508b6733dc.png",contentDescription="Coins",modifier=Modifier.size(24.dp))
                                            val rewardClean = task.reward.replace(Regex("coins", RegexOption.IGNORE_CASE), "").trim()
                                            Text(rewardClean,color=Color(0xFFFFD700),fontSize=14.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.ExtraBold)
                                        }
                                        Button(onClick={},modifier=Modifier.width(85.dp).height(28.dp).shadow(elevation=4.dp, shape=RoundedCornerShape(18.dp), spotColor=Color(0xFFFFD700).copy(alpha=0.55f)),contentPadding=PaddingValues(0.dp),shape=RoundedCornerShape(18.dp),border=androidx.compose.foundation.BorderStroke(2.dp,Color(0xFFF5E6A8)),colors=ButtonDefaults.buttonColors(containerColor=Color.Transparent)){
                                            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFF7DFA0),Color(0xFFE0A92A))),RoundedCornerShape(18.dp)),contentAlignment=Alignment.Center){
                                                Text("CLAIM",color=Color(0xFF4A1F00),fontSize=11.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Black, letterSpacing=1.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Sabse aakhiri wali bottom frame (Ab isko bhi sirf 1.08f rakha hai)
                Box(Modifier.fillMaxWidth().offset(y=(-65).dp).wrapContentWidth(unbounded = true), contentAlignment = Alignment.Center){
                    AsyncImage(
                        model=raw+"file_0000000066c88211aa777b1f6da8683f.png",
                        contentDescription="Frame Bottom Border",
                        modifier=Modifier.fillMaxWidth(1.08f), 
                        contentScale=ContentScale.FillWidth
                    )
                }
                
                Spacer(Modifier.height(10.dp))
            }
        }
        
        // RULES DIALOG 
        if(rules) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.70f)).clickable{rules=false},contentAlignment=Alignment.BottomCenter){
            Column(Modifier.fillMaxWidth().heightIn(max=620.dp).background(Brush.verticalGradient(listOf(Color(0xFF4A0A10),Color(0xFF380308))),RoundedCornerShape(topStart=16.dp,topEnd=16.dp)).padding(horizontal=20.dp,vertical=18.dp)){
                Text("ROOM TASK RULES",color=Color.White,fontSize=18.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Black,modifier=Modifier.align(Alignment.CenterHorizontally).padding(bottom=14.dp))
                listOf("Each user can complete a 'Room Owner' task only once per day.","Each user can complete a daily task once per day. Tasks reset at 5:30+GMT.","For gift-giving tasks, only gifts sent within the room are counted; gifts sent via private message are not included.","Treasure Chest\nWhen a gift is received in a personal room, the room owner can earn a 5% coin reward of the gift's value. There is no limit on the coin bonus; the more gifts received in the room, the more coin rewards you earn.","If you create multiple accounts using the same device or SIM card, you can only claim rewards once for each task.").forEachIndexed{index,rule->
                    Row(Modifier.fillMaxWidth().padding(vertical=8.dp), horizontalArrangement=Arrangement.spacedBy(12.dp)){
                        Box(Modifier.size(24.dp).background(Brush.verticalGradient(listOf(Color(0xFFF7DFA0),Color(0xFFE0A92A))),androidx.compose.foundation.shape.CircleShape).border(1.dp, Color(0xFFF5E6A8), androidx.compose.foundation.shape.CircleShape),contentAlignment=Alignment.Center){
                            Text((index+1).toString(),color=Color(0xFF4A1F00),fontSize=12.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Black)
                        }
                        if (index == 3) {
                            Column{
                                Text("Treasure Chest", color=Color.White, fontSize=13.sp, fontWeight=androidx.compose.ui.text.font.FontWeight.Bold, modifier=Modifier.padding(bottom=4.dp))
                                Text(rule.substringAfter("\n"), color=Color(0xFFF5D0D0), fontSize=13.sp, lineHeight=18.sp)
                            }
                        } else {
                            Text(rule,color=Color(0xFFF5D0D0),fontSize=13.sp, lineHeight=18.sp)
                        }
                    }
                }
                Text("Regards, Zyora Team",color=Color(0xFFFFD700),fontSize=13.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold,modifier=Modifier.align(Alignment.End).padding(top=8.dp))
            }
        }
    }
}
