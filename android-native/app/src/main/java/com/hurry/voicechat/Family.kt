package com.hurry.voicechat

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import java.util.Calendar

private const val FAMILY_ASSET_BASE = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/"
private fun familyAsset(file: String) = FAMILY_ASSET_BASE + file
private val familyGold = Color(0xFFFFD700)
private val familyDark = Color(0xFF1A0D06)
private val familyBrown = Color(0xFF2A1610)
private val familyRemoveGreen = ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(1f,0f,0f,0f,0f, 0f,1f,0f,0f,0f, 0f,0f,1f,0f,0f, 1.5f,-2.5f,1.5f,1f,0f)))

private val familyRules = listOf(
 "1. The top 10 families in the weekly family rankings will receive various rewards. Rewards are distributed every Sunday at 5:30 AM (GMT+5:30).",
 "2. There is no need to wait for official approval.",
 "3. A family can have a maximum of 100 members.",
 "4. Individuals can apply to join a family. Once the family leader approves the request, the applicant can join the family immediately.",
 "5. Each user can join only one family. Regular family members can leave the family at any time without the leader's approval.",
 "6. The family leader has the authority to remove other members from the family at any time without the removed member's consent. The family leader can also disband the family freely without official approval.",
 "7. Upon the disbanding of the family, all members will immediately leave the original family.",
 "8. Family rankings reflect the family's strength. Zaho determines family rankings based on the total spending of all family members.",
 "9. Any benefits or relationships formed within the family are the result of voluntary actions taken by the members themselves and have no connection to Zaho."
)

@Composable
fun Family(onBack: () -> Unit) {
 val context = LocalContext.current
 val prefs = remember { context.getSharedPreferences("hurry_user", Context.MODE_PRIVATE) }
 var view by remember { mutableStateOf("main") }
 var showRules by remember { mutableStateOf(false) }
 var showApplyMode by remember { mutableStateOf(false) }
 var applyMode by remember { mutableStateOf(prefs.getString("applyMode", "free") ?: "free") }
 var familyName by remember { mutableStateOf(prefs.getString("familyName", "") ?: "") }
 var announcement by remember { mutableStateOf(prefs.getString("familyAnnouncement", "") ?: "") }
 var familyImage by remember { mutableStateOf(prefs.getString("familyImage", "") ?: "") }
 var createBusy by remember { mutableStateOf(false) }
 var createError by remember { mutableStateOf<String?>(null) }
 val scope = rememberCoroutineScope()
 val walletDao = remember { WalletDatabase.get(context).walletDao() }
 val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
  if (uri != null) runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
  familyImage = uri?.toString().orEmpty()
 }
 var activeRank by remember { mutableIntStateOf(0) }
 var seconds by remember { mutableIntStateOf(0) }
 LaunchedEffect(Unit) {
  while (true) {
   val now = Calendar.getInstance()
   val days = (Calendar.SUNDAY - now.get(Calendar.DAY_OF_WEEK) + 7) % 7
   val end = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, if (days == 0) 7 else days); set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }
   seconds = ((end.timeInMillis - now.timeInMillis).coerceAtLeast(0L) / 1000L).toInt()
   delay(1000)
  }
 }
 Box(Modifier.fillMaxSize().background(if (view == "create") Color.White else if (view == "top") familyBrown else familyDark)) {
  when (view) {
   "create" -> FamilyCreate(onBack = { view = "main" }, name = familyName, onName = { familyName = it }, announcement = announcement, onAnnouncement = { announcement = it }, image = familyImage, onImage = { imagePicker.launch(arrayOf("image/*")) }, mode = applyMode, onMode = { showApplyMode = true }, busy = createBusy, error = createError, onCreate = {
    if (createBusy) return@FamilyCreate
    if (familyName.isBlank()) {
     createError = "Please enter a Family Name."
    } else {
     createBusy = true
     createError = null
     scope.launch {
      val result = withContext(Dispatchers.IO) {
       runCatching {
        val current = walletDao.getUserData() ?: UserDataEntity()
        if (current.balance < 10_000_000L) {
         "You need 10,000,000 coins to create a family."
        } else {
         walletDao.saveUserData(current.copy(balance = current.balance - 10_000_000L))
         val now = System.currentTimeMillis()
         walletDao.insertTransaction(TransactionEntity(
          title = "Create Family", amount = -10_000_000L,
          date = java.text.SimpleDateFormat("yyyy.MM.dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(now)),
          timestamp = now, type = "coins"
         ))
         null
        }
       }.getOrElse { "Could not create family. Please try again." }
      }
      createBusy = false
      if (result == null) {
       prefs.edit().putString("familyName", familyName.trim())
        .putString("familyAnnouncement", announcement)
        .putString("familyImage", familyImage)
        .putString("applyMode", applyMode)
        .putString("familyCode", prefs.getString("familyCode", "")?.ifBlank { "FAM" + System.currentTimeMillis().toString().takeLast(6) } ?: "FAM" + System.currentTimeMillis().toString().takeLast(6))
        .putBoolean("familyCreated", true).apply()
       view = "top"
       Toast.makeText(context, "Family created successfully", Toast.LENGTH_SHORT).show()
      } else createError = result
     }
    }
   })
   "top" -> FamilyTop(onBack = { view = "main" }, onRules = { showRules = true }, activeRank = activeRank, onRank = { activeRank = it })
   else -> FamilyHome(onBack = onBack, onRules = { showRules = true }, seconds = seconds, activeRank = activeRank, onRank = { activeRank = it; if (it == 2) view = "top" }, onCreate = { view = "create" })
  }
  if (showRules) FamilyRules(onClose = { showRules = false })
  if (showApplyMode) FamilyModeSheet(applyMode, { applyMode = it; prefs.edit().putString("applyMode", it).apply(); showApplyMode = false }, { showApplyMode = false })
 }
}

@Composable
private fun FamilyHome(onBack: () -> Unit, onRules: () -> Unit, seconds: Int, activeRank: Int, onRank: (Int) -> Unit, onCreate: () -> Unit) {
 Box(Modifier.fillMaxSize().background(familyDark)) {
  AsyncImage(model = familyAsset("IMG_20260901_160704.png"), contentDescription = null, modifier = Modifier.fillMaxWidth().height(260.dp), contentScale = ContentScale.Crop)
  Box(Modifier.fillMaxWidth().height(260.dp).background(Brush.verticalGradient(listOf(Color.Transparent, familyDark.copy(alpha=.1f), familyDark))))
  Column(Modifier.fillMaxSize()) {
   Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start=8.dp,end=16.dp,top=8.dp), verticalAlignment=Alignment.CenterVertically) {
    IconButton(onClick=onBack) { Icon(Icons.Default.ArrowBack,"Back",tint=Color.White,modifier=Modifier.size(28.dp)) }
    Spacer(Modifier.weight(1f)); FamilyQuestion(Modifier.size(28.dp).clickable(onClick=onRules))
   }
   Box(Modifier.fillMaxWidth().height(215.dp)) {
    AsyncImage(model=familyAsset("1788258909655~2.jpg"),contentDescription=null,modifier=Modifier.align(Alignment.CenterStart).offset(x=(-14).dp,y=16.dp).size(148.dp),contentScale=ContentScale.Fit)
    AsyncImage(model=familyAsset("1788258915366~2.jpg"),contentDescription=null,modifier=Modifier.align(Alignment.CenterEnd).offset(x=14.dp,y=16.dp).size(148.dp),contentScale=ContentScale.Fit,colorFilter=familyRemoveGreen)
    AsyncImage(model=familyAsset("IMG_20260901_161023.png"),contentDescription=null,modifier=Modifier.align(Alignment.TopCenter).offset(y=(-24).dp).size(270.dp),contentScale=ContentScale.Fit,colorFilter=familyRemoveGreen)
   }
   Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.Bottom) {
    listOf("IMG_20260901_230303.jpg","IMG_20260901_230319.jpg","IMG_20260901_230330.jpg").forEachIndexed { index,file ->
     AsyncImage(model=familyAsset(file),contentDescription="Family rank",modifier=Modifier.weight(1f).height(if(index==1) 112.dp else 94.dp).clickable { onRank(index) },contentScale=ContentScale.Fit,alpha=if(activeRank==index) 1f else .5f,colorFilter=familyRemoveGreen)
    }
   }
   Row(Modifier.fillMaxWidth().padding(top=16.dp,bottom=10.dp).background(Brush.horizontalGradient(listOf(Color.Transparent,familyGold.copy(alpha=.12f),Color.Transparent))).border(1.dp,familyGold.copy(alpha=.18f)),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
    Text("Countdown",color=Color(0xFFFDF6E3),fontSize=14.sp,modifier=Modifier.padding(end=6.dp))
    Text("%02d".format(seconds/86400),color=Color.White,modifier=Modifier.background(Color(0xFF1A0F02),RoundedCornerShape(4.dp)).padding(4.dp),fontWeight=FontWeight.Bold)
    Text(" Days ",color=Color(0xFFFDF6E3),fontSize=12.sp)
    Text("%02d".format((seconds/3600)%24),color=Color.White,modifier=Modifier.background(Color(0xFF1A0F02),RoundedCornerShape(4.dp)).padding(4.dp),fontWeight=FontWeight.Bold)
    Text(":",color=Color.White)
    Text("%02d".format((seconds/60)%60),color=Color.White,modifier=Modifier.background(Color(0xFF1A0F02),RoundedCornerShape(4.dp)).padding(4.dp),fontWeight=FontWeight.Bold)
    Text(":",color=Color.White)
    Text("%02d".format(seconds%60),color=Color.White,modifier=Modifier.background(Color(0xFF1A0F02),RoundedCornerShape(4.dp)).padding(4.dp),fontWeight=FontWeight.Bold)
   }
   Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom=90.dp)) {
    AsyncImage(model=familyAsset("1788258921361~2.jpg"),contentDescription="Top three families",modifier=Modifier.fillMaxWidth().height(150.dp),contentScale=ContentScale.FillBounds)
    for(rank in 4..50) Box(Modifier.fillMaxWidth().height(64.dp),contentAlignment=Alignment.CenterStart) {
     AsyncImage(model=familyAsset("1788259008478~2.jpg"),contentDescription=null,modifier=Modifier.fillMaxSize(),contentScale=ContentScale.FillBounds)
     Text(rank.toString(),color=familyGold,fontSize=22.sp,fontWeight=FontWeight.Black,modifier=Modifier.padding(start=24.dp))
    }
   }
  }
  Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
   AsyncImage(model=familyAsset("file_000000009d1081f59878648feb821b5e.png"),contentDescription=null,modifier=Modifier.fillMaxWidth().offset(y=24.dp),contentScale=ContentScale.FillWidth)
   AsyncImage(model=familyAsset("file_0000000056508230808da89fb794a97f.png"),contentDescription="Create Family",modifier=Modifier.align(Alignment.Center).padding(bottom=8.dp).fillMaxWidth(.52f).clickable(onClick=onCreate),contentScale=ContentScale.Fit)
  }
 }
}

@Composable
private fun FamilyTop(onBack: () -> Unit,onRules: () -> Unit,activeRank: Int,onRank: (Int)->Unit) {
 Box(Modifier.fillMaxSize().background(familyBrown)) {
  AsyncImage(model=familyAsset("IMG_20260901_160704.png"),contentDescription=null,modifier=Modifier.fillMaxWidth().height(250.dp),contentScale=ContentScale.Crop)
  Column(Modifier.fillMaxSize()) {
   Row(Modifier.fillMaxWidth().statusBarsPadding().padding(top=8.dp,start=8.dp,end=16.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
    IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,"Back",tint=Color.White,modifier=Modifier.size(28.dp))}
    FamilyQuestion(Modifier.size(28.dp).clickable(onClick=onRules))
   }
   Box(Modifier.fillMaxWidth().height(245.dp)) {
    AsyncImage(model=familyAsset("1788258909655~2.jpg"),contentDescription=null,modifier=Modifier.align(Alignment.CenterStart).size(150.dp),contentScale=ContentScale.Fit)
    AsyncImage(model=familyAsset("1788258915366~2.jpg"),contentDescription=null,modifier=Modifier.align(Alignment.CenterEnd).size(150.dp),contentScale=ContentScale.Fit)
    AsyncImage(model=familyAsset("IMG_20260901_161023.png"),contentDescription=null,modifier=Modifier.align(Alignment.TopCenter).offset(y=(-34).dp).size(275.dp),contentScale=ContentScale.Fit)
   }
   Row(Modifier.fillMaxWidth().padding(horizontal=14.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.Bottom) {
    listOf("IMG_20260901_230303.jpg","IMG_20260901_230319.jpg","IMG_20260901_230330.jpg").forEachIndexed { i,file -> AsyncImage(model=familyAsset(file),contentDescription=null,modifier=Modifier.weight(1f).height(if(i==1)110.dp else 90.dp).clickable{onRank(i)},contentScale=ContentScale.Fit,alpha=if(activeRank==i)1f else .45f) }
   }
   Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top=12.dp,bottom=20.dp)) {
    AsyncImage(model=familyAsset("1788258921361~2.jpg"),contentDescription=null,modifier=Modifier.fillMaxWidth().height(150.dp),contentScale=ContentScale.FillBounds)
    for(rank in 4..50) Box(Modifier.fillMaxWidth().height(64.dp),contentAlignment=Alignment.CenterStart) {
     AsyncImage(model=familyAsset("1788259008478~2.jpg"),contentDescription=null,modifier=Modifier.fillMaxSize(),contentScale=ContentScale.FillBounds)
     Text(rank.toString(),color=familyGold,fontSize=22.sp,fontWeight=FontWeight.Black,modifier=Modifier.padding(start=24.dp))
    }
   }
  }
 }
}

@Composable
private fun FamilyCreate(onBack:()->Unit,name:String,onName:(String)->Unit,announcement:String,onAnnouncement:(String)->Unit,image:String,onImage:()->Unit,mode:String,onMode:()->Unit,busy:Boolean,error:String?,onCreate:()->Unit) {
 Column(Modifier.fillMaxSize().background(Color.White)) {
  Row(Modifier.fillMaxWidth().statusBarsPadding().padding(top=16.dp,start=8.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,"Back",tint=Color.Black,modifier=Modifier.size(28.dp))}}
  Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom=20.dp)) {
   Box(Modifier.align(Alignment.CenterHorizontally).padding(top=4.dp).size(96.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF9FAFB)).border(2.dp,familyGold,RoundedCornerShape(8.dp)).clickable(onClick=onImage),contentAlignment=Alignment.Center) {
    if(image.isNotBlank()) AsyncImage(model=image,contentDescription="Family image",modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop) else Icon(Icons.Default.Add,"Upload Image",tint=Color.Gray,modifier=Modifier.size(36.dp))
   }
   Text("Upload Image",color=Color.Gray,fontSize=14.sp,fontWeight=FontWeight.Bold,modifier=Modifier.align(Alignment.CenterHorizontally).padding(top=8.dp))
   Column(Modifier.padding(horizontal=20.dp).padding(top=12.dp)) {
    Text("Family name",color=Color.Black,fontWeight=FontWeight.Bold,fontSize=14.sp)
    OutlinedTextField(value=name,onValueChange=onName,modifier=Modifier.fillMaxWidth(),singleLine=true,shape=RoundedCornerShape(12.dp),colors=OutlinedTextFieldDefaults.colors(unfocusedContainerColor=Color(0xFFF3F4F6),focusedContainerColor=Color(0xFFF3F4F6)))
    Spacer(Modifier.height(12.dp)); Text("Family Announcement",color=Color.Black,fontWeight=FontWeight.Bold,fontSize=14.sp)
    OutlinedTextField(value=announcement,onValueChange=onAnnouncement,modifier=Modifier.fillMaxWidth(),singleLine=true,shape=RoundedCornerShape(12.dp),colors=OutlinedTextFieldDefaults.colors(unfocusedContainerColor=Color(0xFFF3F4F6),focusedContainerColor=Color(0xFFF3F4F6)))
    Spacer(Modifier.height(12.dp)); Text("Setting",color=Color.Gray,fontWeight=FontWeight.Bold,fontSize=14.sp)
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFF3F4F6)).clickable(onClick=onMode).padding(16.dp),verticalAlignment=Alignment.CenterVertically){Text("Apply Mode",color=Color.Black,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));Text(if(mode=="admin")"Apply Mode / Admin & Owner" else "Free mode",color=Color.Gray,fontSize=12.sp);Icon(Icons.Default.ChevronRight,null,tint=Color.Gray)}
    if (error != null) Text(error,color=Color(0xFFDC2626),fontSize=13.sp,fontWeight=FontWeight.Medium,modifier=Modifier.padding(top=10.dp))
    Spacer(Modifier.height(16.dp)); Text("1. Creating a family requires ≥ LV.5",color=Color.Gray,fontSize=13.sp)
    Text("2. Family info can be modified once a week",color=Color.Gray,fontSize=13.sp,modifier=Modifier.padding(top=4.dp))
    Text("3. If family with only 1 member for 7 days will be disbanded automatically",color=Color.Gray,fontSize=13.sp,modifier=Modifier.padding(top=4.dp))
   }
  }
  Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom=16.dp),contentAlignment=Alignment.Center){Button(onClick=onCreate,enabled=!busy,shape=RoundedCornerShape(50),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF00BFA5)),modifier=Modifier.height(52.dp).widthIn(min=280.dp)){Text(if(busy) "Creating family..." else "Created with 10,000,000 coins",color=Color.White,fontWeight=FontWeight.Bold,fontSize=16.sp)}}
 }
}

@Composable
private fun FamilyRules(onClose:()->Unit) {
 Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)) {
  Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.5f)),contentAlignment=Alignment.BottomCenter) {
   Column(Modifier.fillMaxWidth().fillMaxHeight(.8f).clip(RoundedCornerShape(topStart=24.dp,topEnd=24.dp)).background(Color.White).padding(horizontal=24.dp,vertical=20.dp)) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("Family rules",color=Color.Black,fontSize=18.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));IconButton(onClick=onClose){Icon(Icons.Default.Close,"Close",tint=Color.Gray)}}
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom=12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){familyRules.forEach{Text(it,color=Color(0xCC000000),fontSize=14.sp,lineHeight=20.sp)};Text("Regards, Zyora Team",color=Color.Gray,fontSize=12.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.align(Alignment.End))}
   }
  }
 }
}

@Composable
private fun FamilyModeSheet(selected:String,onSelect:(String)->Unit,onClose:()->Unit) {
 Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)) {
  Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.5f)),contentAlignment=Alignment.BottomCenter) {
   Column(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(topStart=24.dp,topEnd=24.dp)).background(Color.White).padding(horizontal=24.dp,vertical=8.dp)) {
    Row(Modifier.fillMaxWidth().weight(1f).clickable{onSelect("free")},verticalAlignment=Alignment.CenterVertically){Text("Free mode",color=Color.Black,fontSize=14.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));RadioButton(selected=="free",onClick={onSelect("free")})}
    HorizontalDivider(color=Color(0xFFF3F4F6))
    Row(Modifier.fillMaxWidth().weight(1f).clickable{onSelect("admin")},verticalAlignment=Alignment.CenterVertically){Text("Apply Mode / Admin & Owner",color=Color.Black,fontSize=14.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));RadioButton(selected=="admin",onClick={onSelect("admin")})}
   }
  }
 }
}

@Composable
private fun FamilyQuestion(modifier:Modifier=Modifier) {
 Canvas(modifier) {
  drawCircle(Color.White,style=Stroke(2.dp.toPx()))
  drawContext.canvas.nativeCanvas.drawText("?",size.width/2f,size.height*.73f,android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply{color=android.graphics.Color.WHITE;textSize=size.minDimension*.72f;textAlign=android.graphics.Paint.Align.CENTER;typeface=android.graphics.Typeface.DEFAULT_BOLD})
 }
}
