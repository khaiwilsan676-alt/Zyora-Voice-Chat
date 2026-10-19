package com.hurry.voicechat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import java.util.Calendar

private data class NativeRoomTaskRow(val title:String,val reward:String,val icon:String)

@Composable fun Roomtask(onBack:()->Unit={}){
    val raw="https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/"
    var rules by remember{mutableStateOf(false)}
    var hour by remember{mutableIntStateOf(0)}
    var minute by remember{mutableIntStateOf(0)}
    var second by remember{mutableIntStateOf(0)}
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
        AsyncImage(model=raw+"file_00000000cb748211bf0120855b80f449.png",contentDescription=null,modifier=Modifier.fillMaxWidth().height(300.dp),contentScale=ContentScale.Crop,alignment=Alignment.TopCenter)
        Column(Modifier.fillMaxSize()){
            Row(Modifier.fillMaxWidth().padding(top=24.dp,start=4.dp,end=6.dp),verticalAlignment=Alignment.CenterVertically){
                IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,null,tint=Color(0xFFFFD700))}
                Spacer(Modifier.weight(1f))
                IconButton(onClick={rules=true}){Icon(Icons.Default.HelpOutline,null,tint=Color(0xFFFFD700))}
            }
            Column(Modifier.fillMaxSize().padding(horizontal=8.dp),horizontalAlignment=Alignment.CenterHorizontally){
                AsyncImage(model=raw+"file_00000000f2908208a7b6a2b73c3bbf36.png",contentDescription="Room task rewards",modifier=Modifier.fillMaxWidth().height(138.dp),contentScale=ContentScale.Fit)
                Text("0",color=Color(0xFFFFD700),fontSize=24.sp)
                Button(onClick={},shape=RoundedCornerShape(20.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFE0A92A)),contentPadding=PaddingValues(horizontal=26.dp,vertical=3.dp)){Text("Collect",color=Color(0xFF4A1F00),fontSize=11.sp)}
                Text("*If anyone sends gifts in the room, the room owner can get 5% coins worth the gift value.",color=Color.White,fontSize=11.sp,modifier=Modifier.padding(vertical=8.dp))
                AsyncImage(model=raw+"file_00000000680881faa3dfdb17cce60858.png",contentDescription="Room Task",modifier=Modifier.fillMaxWidth().height(45.dp),contentScale=ContentScale.FillBounds)
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(3.dp),modifier=Modifier.padding(vertical=5.dp)){
                    Text("Countdown",color=Color(0xFFF5B8B8),fontSize=10.sp)
                    listOf(hour to "Hour",minute to "Minute",second to "Second").forEach{pair->
                        Box(Modifier.background(Brush.verticalGradient(listOf(Color(0xFF8F1A1A),Color(0xFF470A0A))),RoundedCornerShape(4.dp)).padding(horizontal=5.dp,vertical=3.dp)){Text(pair.first.toString(),color=Color(0xFFFFE0E0),fontSize=11.sp)}
                        Text(pair.second,color=Color(0xFFF5B8B8),fontSize=10.sp)
                    }
                }
                LazyColumn(Modifier.fillMaxWidth().weight(1f),contentPadding=PaddingValues(bottom=24.dp),verticalArrangement=Arrangement.spacedBy((-38).dp)){
                    items(tasks){task->
                        Box(Modifier.fillMaxWidth().height(175.dp)){
                            AsyncImage(model=raw+"file_000000004fd0821198ed4e26d5008b16.png",contentDescription="Task background",modifier=Modifier.fillMaxSize(),contentScale=ContentScale.FillBounds)
                            Row(Modifier.fillMaxSize().padding(start=5.dp,end=10.dp),verticalAlignment=Alignment.CenterVertically){
                                AsyncImage(model=raw+task.icon,contentDescription=task.title,modifier=Modifier.size(if(task.title.contains("mic",true)||task.title.contains("gift",true)||task.title.contains("user",true))60.dp else 46.dp),contentScale=ContentScale.Fit)
                                Text(task.title,color=Color.White,fontSize=12.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold,modifier=Modifier.weight(1f).padding(start=2.dp))
                                Column(horizontalAlignment=Alignment.CenterHorizontally){
                                    Row(verticalAlignment=Alignment.CenterVertically){AsyncImage(model=raw+"file_00000000e56882119c217d508b6733dc.png",contentDescription="Coins",modifier=Modifier.size(21.dp));Text(task.reward,color=Color(0xFFFFD700),fontSize=12.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.ExtraBold)}
                                    Button(onClick={},modifier=Modifier.width(85.dp).height(28.dp),contentPadding=PaddingValues(0.dp),shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFE0A92A))){Text("CLAIM",color=Color(0xFF4A1F00),fontSize=10.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Black)}
                                }
                            }
                        }
                    }
                }
            }
        }
        if(rules) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.70f)).clickable{rules=false},contentAlignment=Alignment.BottomCenter){
            Column(Modifier.fillMaxWidth().heightIn(max=620.dp).background(Brush.verticalGradient(listOf(Color(0xFF4A0A10),Color(0xFF380308))),RoundedCornerShape(topStart=16.dp,topEnd=16.dp)).padding(horizontal=20.dp,vertical=18.dp)){
                Text("ROOM TASK RULES",color=Color.White,fontSize=18.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Black,modifier=Modifier.align(Alignment.CenterHorizontally).padding(bottom=14.dp))
                listOf("Each user can complete a 'Room Owner' task only once per day.","Each user can complete a daily task once per day. Tasks reset at 5:30+GMT.","For gift-giving tasks, only gifts sent within the room are counted; gifts sent via private message are not included.","Treasure Chest: in a personal room, the owner can earn a 5% coin reward of the gift value with no limit.","Multiple accounts on the same device or SIM can claim each task only once.").forEachIndexed{index,rule->Text((index+1).toString()+". "+rule,color=Color(0xFFF5D0D0),fontSize=13.sp,modifier=Modifier.padding(vertical=6.dp))}
                Text("Regards, Zyora Team",color=Color(0xFFFFD700),fontSize=13.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold,modifier=Modifier.align(Alignment.End).padding(top=8.dp))
                TextButton(onClick={rules=false},modifier=Modifier.align(Alignment.End)){Text("Close",color=Color.White)}
            }
        }
    }
}
