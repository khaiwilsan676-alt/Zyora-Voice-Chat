package com.hurry.voicechat

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

data class RoomPageOwner(val id:String="",val uid:String="",val accountId:String="",val name:String="Room",val image:String="")
data class RoomPageCurrentUser(val id:String="",val uid:String="",val accountId:String="",val name:String="User",val image:String="")
data class NativeRoomSeat(val number:Int,val occupied:Boolean=false,val locked:Boolean=false,val muted:Boolean=false,val speaking:Boolean=false,val userName:String="",val userImage:String="",val accountId:String="")
data class NativeRoomMessage(val id:String,val text:String,val sender:String,val senderImage:String,val senderAccountId:String="",val timestamp:Long=System.currentTimeMillis(),val type:String="message")

@Composable
fun RoomPage(
    roomOwner: RoomPageOwner,
    currentUser: RoomPageCurrentUser,
    onClose:()->Unit={},
    onBack:()->Unit=onClose,
    onKeepRoom:(String,String,String)->Unit={_,_,_->},
    onFollowToggle:(String,Boolean)->Unit={_,_->}
) {
    val roomId=roomOwner.id.ifBlank{roomOwner.accountId.ifBlank{roomOwner.uid.ifBlank{"default-room"}}}
    val accountId=currentUser.accountId.ifBlank{currentUser.uid.ifBlank{currentUser.id.ifBlank{"guest"}}}
    var roomName by remember{mutableStateOf(roomOwner.name.ifBlank{"Room"})}
    var roomImage by remember{mutableStateOf(roomOwner.image)}
    var followed by remember{mutableStateOf(false)}
    var speaker by remember{mutableStateOf(true)}
    var locked by remember{mutableStateOf(false)}
    var showChat by remember{mutableStateOf(false)}
    var showMenu by remember{mutableStateOf(false)}
    var showInfo by remember{mutableStateOf(false)}
    var showSettings by remember{mutableStateOf(false)}
    var showGift by remember{mutableStateOf(false)}
    var showEmoji by remember{mutableStateOf(false)}
    var showGames by remember{mutableStateOf(false)}
    var showMusic by remember{mutableStateOf(false)}
    var input by remember{mutableStateOf("")}
    var announcement by remember{mutableStateOf("")}
    var selectedSeat by remember{mutableIntStateOf(0)}
    val messages=remember{mutableStateListOf<NativeRoomMessage>()}
    val seats=remember{mutableStateListOf<NativeRoomSeat>().apply{for(i in 1..8)add(NativeRoomSeat(i))}}

    Box(Modifier.fillMaxSize().background(Color.Black)){
        AsyncImage(model=roomImage,contentDescription=null,modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        Column(Modifier.fillMaxSize()){
            Row(Modifier.fillMaxWidth().padding(top=28.dp,start=6.dp,end=6.dp),verticalAlignment=Alignment.CenterVertically){
                IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,null,tint=Color.White)}
                AsyncImage(model=roomImage,contentDescription=null,modifier=Modifier.size(44.dp).clip(CircleShape),contentScale=ContentScale.Crop)
                Column(Modifier.weight(1f).padding(start=8.dp)){
                    Text(roomName,Color.White,16.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                    Text("ID: "+roomId,Color.White.copy(.65f),11.sp,maxLines=1)
                }
                TextButton(onClick={followed=!followed;onFollowToggle(roomId,followed)}){Text(if(followed)"Following" else "Follow",Color.White)}
                IconButton(onClick={onClose}){Icon(Icons.Default.Close,null,tint=Color.White)}
                IconButton(onClick={showMenu=!showMenu}){Icon(Icons.Default.MoreVert,null,tint=Color.White)}
            }
            if(announcement.isNotBlank()) Surface(Modifier.fillMaxWidth().padding(10.dp),color=Color.Black.copy(.4f),shape=RoundedCornerShape(18.dp)){
                Text(announcement,Color.White,13.sp,modifier=Modifier.padding(10.dp),maxLines=1,overflow=TextOverflow.Ellipsis)
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth().padding(horizontal=10.dp),horizontalArrangement=Arrangement.SpaceEvenly){
                seats.take(4).forEach{seat->NativeSeat(seat,roomOwner.accountId.ifBlank{roomOwner.id}){selectedSeat=seat.number}}
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal=10.dp),horizontalArrangement=Arrangement.SpaceEvenly){
                seats.drop(4).forEach{seat->NativeSeat(seat,roomOwner.accountId.ifBlank{roomOwner.id}){selectedSeat=seat.number}}
            }
            Spacer(Modifier.weight(1f))
            if(messages.isNotEmpty()) LazyColumn(Modifier.fillMaxWidth().height(150.dp).padding(horizontal=10.dp)){items(messages,key={it.id}){NativeMessage(it)}}
            if(showChat) Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically){
                IconButton(onClick={showEmoji=!showEmoji}){Icon(Icons.Default.EmojiEmotions,null,tint=Color.White)}
                OutlinedTextField(value=input,onValueChange={input=it},modifier=Modifier.weight(1f),singleLine=true,placeholder={Text("Message")})
                IconButton(onClick={if(input.isNotBlank()){messages.add(NativeRoomMessage(System.currentTimeMillis().toString(),input,currentUser.name,currentUser.image,accountId));input=""}}){Icon(Icons.Default.Send,null,tint=Color.White)}
            }
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(8.dp),horizontalArrangement=Arrangement.SpaceEvenly){
                IconButton(onClick={speaker=!speaker}){Icon(if(speaker)Icons.Default.VolumeUp else Icons.Default.VolumeOff,null,tint=Color.White)}
                IconButton(onClick={showChat=!showChat}){Icon(Icons.Default.ChatBubbleOutline,null,tint=Color.White)}
                IconButton(onClick={showGift=true}){Icon(Icons.Default.CardGiftcard,null,tint=Color.White)}
                IconButton(onClick={showMusic=!showMusic}){Icon(Icons.Default.MusicNote,null,tint=Color.White)}
                IconButton(onClick={showGames=true}){Icon(Icons.Default.SportsEsports,null,tint=Color.White)}
            }
        }
        if(showMenu) RoomMenu(
            owner=accountId==roomOwner.accountId.ifBlank{roomOwner.id},
            onClose={showMenu=false},
            onSettings={showMenu=false;showSettings=true},
            onLeave={showMenu=false;onClose()}
        )
        if(showInfo) RoomInfo(roomName,roomImage,roomId){showInfo=false}
        if(showSettings) RoomSettings(roomName,announcement,locked,{roomName=it},{announcement=it},{locked=it},{showSettings=false}){onKeepRoom(roomName,roomImage,roomId);showSettings=false}
        if(showGift) GiftSheet{showGift=false}
        if(showGames) GamesSheet{showGames=false}
        if(showMusic) MusicSheet{showMusic=false}
        if(showEmoji) EmojiSheet({showEmoji=false}){input+=it;showEmoji=false}
        if(selectedSeat>0 && seats.firstOrNull{it.number==selectedSeat}?.occupied==true) {
            MemberSheet(seats.first{it.number==selectedSeat}){selectedSeat=0}
        }
    }
}

@Composable private fun NativeSeat(seat:NativeRoomSeat,ownerId:String,onClick:()->Unit){
    val pulse=rememberInfiniteTransition(label="seat").animateFloat(1f,if(seat.speaking)1.1f else 1f,infiniteRepeatable(tween(600),RepeatMode.Reverse),label="pulse")
    Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.width(62.dp).clickable(onClick=onClick)){
        Box(Modifier.size(62.dp),contentAlignment=Alignment.Center){
            Box(Modifier.size(62.dp).clip(CircleShape).background(Brush.linearGradient(listOf(if(seat.locked)Color(0xFFBFC3C8) else Color(0xFFBDEFFF),if(seat.locked)Color(0xFF4B5563) else Color(0xFF167DB9)))).border(if(seat.speaking)2.dp else 0.dp,Color(0xFF55C6F3),CircleShape))
            if(seat.locked) Icon(Icons.Default.Lock,null,tint=Color.White,modifier=Modifier.size(26.dp))
            else if(seat.occupied&&seat.userImage.isNotBlank()) AsyncImage(model=seat.userImage,contentDescription=seat.userName,modifier=Modifier.size(56.dp).clip(CircleShape),contentScale=ContentScale.Crop)
            else Icon(Icons.Default.Person,null,tint=Color.White.copy(.8f),modifier=Modifier.size(28.dp))
            if(seat.muted) Icon(Icons.Default.MicOff,null,tint=Color.White,modifier=Modifier.align(Alignment.BottomEnd).size(17.dp))
        }
        Text(if(seat.occupied)seat.userName else seat.number.toString(),Color.White.copy(.9f),10.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}

@Composable private fun NativeMessage(m:NativeRoomMessage){
    Row(Modifier.fillMaxWidth().padding(vertical=2.dp),verticalAlignment=Alignment.CenterVertically){
        AsyncImage(model=m.senderImage,contentDescription=null,modifier=Modifier.size(26.dp).clip(CircleShape),contentScale=ContentScale.Crop)
        Column(Modifier.padding(start=6.dp)){Text(m.sender,Color.White.copy(.7f),11.sp);Text(m.text,Color.White,13.sp)}
    }
}

@Composable private fun RoomMenu(owner:Boolean,onClose:()->Unit,onSettings:()->Unit,onLeave:()->Unit){
    Box(Modifier.fillMaxSize().background(Color.Black.copy(.35f)).clickable{onClose()},contentAlignment=Alignment.BottomCenter){
        Surface(Modifier.fillMaxWidth().padding(14.dp),color=Color(0xFF181818),shape=RoundedCornerShape(22.dp)){
            Column(Modifier.padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally){
                Text("Room",Color.White,17.sp)
                if(owner)TextButton(onClick=onSettings){Text("Room settings",Color.White)}
                TextButton(onClick=onLeave){Text("Leave room",Color.White)}
                TextButton(onClick=onClose){Text("Cancel",Color.White.copy(.7f))}
            }
        }
    }
}

@Composable private fun RoomInfo(name:String,image:String,id:String,onClose:()->Unit){
    SimpleSheet("Room info",onClose){
        AsyncImage(model=image,contentDescription=null,modifier=Modifier.size(82.dp).clip(CircleShape),contentScale=ContentScale.Crop)
        Text(name,Color.White,18.sp);Text("ID: "+id,Color.White.copy(.65f),12.sp)
    }
}

@Composable private fun MemberSheet(seat:NativeRoomSeat,onClose:()->Unit)=SimpleSheet("Member",onClose){
    AsyncImage(model=seat.userImage,contentDescription=null,modifier=Modifier.size(76.dp).clip(CircleShape),contentScale=ContentScale.Crop)
    Text(seat.userName,Color.White,17.sp);Text("ID: "+seat.accountId,Color.White.copy(.65f),12.sp)
}

@Composable private fun RoomSettings(name:String,announcement:String,locked:Boolean,onName:(String)->Unit,onAnnouncement:(String)->Unit,onLocked:(Boolean)->Unit,onClose:()->Unit,onSave:()->Unit)=SimpleSheet("Room settings",onClose){
    OutlinedTextField(name,onName,singleLine=true,label={Text("Room name")})
    Spacer(Modifier.height(8.dp));OutlinedTextField(announcement,onAnnouncement,singleLine=true,label={Text("Announcement")})
    Row(verticalAlignment=Alignment.CenterVertically){Text("Lock room",Color.White,modifier=Modifier.weight(1f));TextButton(onClick={onLocked(!locked)}){Text(if(locked)"ON" else "OFF",Color.White)}}
    TextButton(onClick=onSave){Text("Save",Color.White)}
}

@Composable private fun GiftSheet(onClose:()->Unit)=SimpleSheet("Gift",onClose){
    Row(Modifier.horizontalScroll(rememberScrollState())){listOf("Hot","Lucky","Luxury","Event").forEach{TextButton(onClick={}){Text(it,Color.White)}}}
    Text("Select a gift",Color.White.copy(.7f),modifier=Modifier.padding(12.dp))
}

@Composable private fun GamesSheet(onClose:()->Unit)=SimpleSheet("Games",onClose){
    listOf("Wild Party","Fruit Party","Room task","Store").forEach{Text(it,Color.White,15.sp,modifier=Modifier.padding(10.dp))}
}

@Composable private fun MusicSheet(onClose:()->Unit){
    var volume by remember{mutableFloatStateOf(1f)}
    SimpleSheet("Music",onClose){
        Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.MusicNote,null,tint=Color.White);Slider(volume,{volume=it},Modifier.weight(1f));Text((volume*100).toInt().toString()+"%",Color.White,11.sp)}
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center){TextButton(onClick={}){Text("Previous",Color.White)};TextButton(onClick={}){Text("Play",Color.White)};TextButton(onClick={}){Text("Next",Color.White)}}
    }
}

@Composable private fun EmojiSheet(onClose:()->Unit,onEmoji:(String)->Unit)=SimpleSheet("Emoji",onClose){
    Row(Modifier.horizontalScroll(rememberScrollState())){listOf("😀","😂","😍","😎","🥰","😭","🔥","❤️").forEach{Text(it,27.sp,modifier=Modifier.padding(7.dp).clickable{onEmoji(it)})}}
}

@Composable private fun SimpleSheet(title:String,onClose:()->Unit,content:@Composable()->Unit){
    Box(Modifier.fillMaxSize().background(Color.Black.copy(.42f)).clickable{onClose()},contentAlignment=Alignment.BottomCenter){
        Surface(Modifier.fillMaxWidth().clickable(enabled=false){},color=Color(0xFF171717),shape=RoundedCornerShape(topStart=22.dp,topEnd=22.dp)){
            Column(Modifier.padding(18.dp).navigationBarsPadding()){
                Row(verticalAlignment=Alignment.CenterVertically){Text(title,Color.White,18.sp,Modifier.weight(1f));IconButton(onClick=onClose){Icon(Icons.Default.Close,null,tint=Color.White)}}
                content();Spacer(Modifier.height(18.dp))
            }
        }
    }
}
