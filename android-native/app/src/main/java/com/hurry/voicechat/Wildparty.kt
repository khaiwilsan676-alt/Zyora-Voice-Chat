package com.hurry.voicechat

import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import kotlin.math.*

private data class WpAnimal(val name:String,val image:String,val angle:Float,val dx:Float,val dy:Float,val size:Int,val multiplier:Int)
private data class WpChip(val label:String,val value:Long,val image:String)
private data class WpRound(val round:Int,val winner:String,val time:Long)
private enum class WpPhase { BETTING, SPINNING, RESULT }
private enum class WpMode { SINGLE, MIX_BIG, MIX_SMALL }

private val wpAnimals = listOf(
 WpAnimal("Deer","wp_img_20260822_011134",270f,0f,-13f,71,5),
 WpAnimal("Dog","wp_img_20260822_011118",315f,-4f,-9f,55,5),
 WpAnimal("Zebra","wp_img_20260822_011103",0f,-8f,-19f,68,5),
 WpAnimal("Fox","wp_img_20260822_011041",45f,-6f,-18f,52,5),
 WpAnimal("Eagle","wp_img_20260822_011151",90f,0f,-18f,61,10),
 WpAnimal("Bear","wp_img_20260822_011205",135f,5f,-15f,60,15),
 WpAnimal("Tiger","wp_img_20260822_011218",180f,7f,-8f,63,25),
 WpAnimal("Lion","wp_img_20260822_011028",225f,5f,-5f,65,45)
)
private val wpChips = listOf(
 WpChip("1K",1000,"wp_1787389350745_2"),
 WpChip("10K",10000,"wp_img_20260822_143032"),
 WpChip("50K",50000,"wp_img_20260822_143348"),
 WpChip("500K",500000,"wp_img_20260822_143408"),
 WpChip("5M",5000000,"wp_img_20260822_143422_2")
)
private val wpGold=Color(0xFFFFD33D)
private val wpDark=Color(0xF20C0C0E)
private fun wpAsset(c:Context,n:String)=c.resources.getIdentifier(n,"drawable",c.packageName)
private fun wpReset(now:Long=System.currentTimeMillis()):Long {
 val c=Calendar.getInstance().apply{timeInMillis=now}
 if(c.get(Calendar.HOUR_OF_DAY)<5)c.add(Calendar.DAY_OF_YEAR,-1)
 c.set(Calendar.HOUR_OF_DAY,5);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0)
 return c.timeInMillis
}
private fun wpOutcome(round:Int):Pair<WpMode,Int>{
 val seed=sin(round*127.1+311.7)*43758.5453123
 val roll=(seed-floor(seed))*100
 return when {
  roll<.8 -> WpMode.MIX_BIG to 6
  roll<2.3 -> WpMode.MIX_SMALL to 1
  roll<3.3 -> WpMode.SINGLE to 7
  roll<4.8 -> WpMode.SINGLE to 6
  roll<6.5 -> WpMode.SINGLE to 5
  roll<9.0 -> WpMode.SINGLE to 4
  else -> WpMode.SINGLE to floor(((roll-9.0)/91.0)*4).toInt().coerceIn(0,3)
 }
}
private fun wpFormat(v:Long)=when{v>=1_000_000->"${"%.1f".format(v/1_000_000.0).removeSuffix(".0")}M";v>=1000->"${"%.0f".format(v/1000.0)}K";else->v.toString()}

@Composable
fun Wildparty(onClose:()->Unit={},onMinimize:()->Unit={},modifier:Modifier=Modifier){
 val context=LocalContext.current
 val scope= rememberCoroutineScope()
 val dao= remember{WalletDatabase.get(context).walletDao()}
 var balance by remember{mutableLongStateOf(82927L)}
 var phase by remember{mutableStateOf(WpPhase.BETTING)}
 var round by remember{mutableIntStateOf(1)}
 var countdown by remember{mutableIntStateOf(30)}
 var highlight by remember{mutableIntStateOf(-1)}
 var winnerIndex by remember{mutableIntStateOf(0)}
 var mode by remember{mutableStateOf(WpMode.SINGLE)}
 var chipIndex by remember{mutableIntStateOf(1)}
 var bets by remember{mutableStateOf<Map<String,Long>>(emptyMap())}
 var lastBets by remember{mutableStateOf<Map<String,Long>>(emptyMap())}
 var history by remember{mutableStateOf<List<WpRound>>(emptyList())}
 var historyOpen by remember{mutableStateOf(false)}
 var rulesOpen by remember{mutableStateOf(false)}
 var winnerOpen by remember{mutableStateOf(false)}
 var muted by remember{mutableStateOf(false)}
 var winAmount by remember{mutableLongStateOf(0L)}
 var betAmount by remember{mutableLongStateOf(0L)}
 var winnerTimer by remember{mutableIntStateOf(5)}
 var loading by remember{mutableStateOf(true)}
 var processedRound by remember{mutableIntStateOf(-1)}

 LaunchedEffect(Unit){
  runCatching{
   balance=dao.getUserData()?.balance?:82927L
   val arr=JSONArray(context.getSharedPreferences("wild_party_native",Context.MODE_PRIVATE).getString("history","[]"))
   history=(0 until arr.length()).mapNotNull{i->arr.optJSONObject(i)?.let{WpRound(it.optInt("round"),it.optString("winner"),it.optLong("time"))}}.filter{it.time>=wpReset()}.sortedBy{it.round}.takeLast(50)
  }
  delay(180);loading=false
 }
 suspend fun walletChange(delta:Long,title:String){
  val old=dao.getUserData()?:UserDataEntity(balance=balance)
  val next=(old.balance+delta).coerceAtLeast(0L)
  dao.saveUserData(old.copy(balance=next));balance=next
  val c=Calendar.getInstance()
  val date=String.format("%04d.%02d.%02d %02d:%02d",c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH),c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE))
  dao.insertTransaction(TransactionEntity(title=title,amount=delta,date=date,timestamp=System.currentTimeMillis(),type="coin"))
 }
 fun persistHistory(){
  val arr=JSONArray()
  history.takeLast(50).forEach{arr.put(JSONObject().put("round",it.round).put("winner",it.winner).put("time",it.time))}
  context.getSharedPreferences("wild_party_native",Context.MODE_PRIVATE).edit().putString("history",arr.toString()).apply()
 }
 LaunchedEffect(loading){
  if(loading)return@LaunchedEffect
  while(true){
   val elapsed=((System.currentTimeMillis()-wpReset())/1000L).coerceAtLeast(0)
   val cycle=(elapsed%47).toInt();round=(elapsed/47).toInt()+1
   val outcome=wpOutcome(round);mode=outcome.first;winnerIndex=outcome.second
   when{
    cycle<30->{phase=WpPhase.BETTING;countdown=30-cycle;highlight=-1;winnerOpen=false}
    cycle<40->{phase=WpPhase.SPINNING;countdown=40-cycle;val p=((cycle*1000L+System.currentTimeMillis()%1000L-30000L).coerceIn(0,10000))/10000.0;highlight=(floor((1-(1-p).pow(3))*(120+winnerIndex)).toInt()%8)}
    else->{phase=WpPhase.RESULT;countdown=47-cycle;highlight=winnerIndex}
   }
   delay(100)
  }
 }
 LaunchedEffect(phase,round,loading){
  if(loading)return@LaunchedEffect
  if(phase==WpPhase.BETTING){if(bets.isNotEmpty()){lastBets=bets;bets=emptyMap()}}
  if(phase==WpPhase.RESULT && processedRound!=round){
   processedRound=round
   val result=wpOutcome(round);val animal=wpAnimals[result.second]
   val label=when(result.first){WpMode.MIX_BIG->"Mix Big";WpMode.MIX_SMALL->"Mix Small";else->animal.name}
   history=(history+WpRound(round,label,System.currentTimeMillis())).takeLast(50);persistHistory()
   betAmount=bets.values.sum()
   winAmount=when(result.first){
    WpMode.MIX_BIG->listOf("Eagle","Bear","Tiger","Lion").sumOf{n->(bets[n]?:0L)*wpAnimals.first{it.name==n}.multiplier}
    WpMode.MIX_SMALL->listOf("Deer","Dog","Zebra","Fox").sumOf{n->(bets[n]?:0L)*5L}
    WpMode.SINGLE->(bets[animal.name]?:0L)*animal.multiplier
   }
   delay(1500);if(winAmount>0)walletChange(winAmount,"Won in Wild Party")
   winnerTimer=5;winnerOpen=true
  }
 }
 LaunchedEffect(winnerOpen){if(winnerOpen){repeat(5){delay(1000);winnerTimer=4-it};winnerOpen=false}}
 fun place(name:String){scope.launch{val amount=wpChips[chipIndex].value;if(phase==WpPhase.BETTING&&balance>=amount){walletChange(-amount,"Bet on $name in Wild Party");bets=bets.toMutableMap().also{it[name]=(it[name]?:0L)+amount}}}}
 BoxWithConstraints(modifier.fillMaxSize().background(Color.Black),contentAlignment=Alignment.BottomCenter){
  val h=maxHeight*.65f
  Box(Modifier.fillMaxWidth().height(h).background(Color(0xFF28180D))){
   val bg=wpAsset(context,"wp_1787337855180_2")
   if(bg!=0)Image(painterResource(bg),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
   // Original TSX top multiplier badges (same artwork and placement).
   val multiplierBg=wpAsset(context,"wp_file_00000000330c8211b80be136b631b3e0")
   @Composable fun MultiplierBadge(big:Boolean){
    Box(Modifier.size(65.dp),contentAlignment=Alignment.Center){
     if(multiplierBg!=0)Image(painterResource(multiplierBg),null,Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
     val names=if(big) listOf("Eagle","Bear","Tiger","Lion") else listOf("Dog","Deer","Zebra","Fox")
     val positions=listOf(
      Triple((-9).dp,(-8).dp,-10f),
      Triple(9.dp,(-8).dp,10f),
      Triple((-14).dp,12.dp,-15f),
      Triple(14.dp,12.dp,10f)
     )
     names.forEachIndexed{index,name->
      val id=wpAsset(context,wpAnimals.first{it.name==name}.image)
      val (x,y,rotation)=positions[index]
      if(id!=0)Image(painterResource(id),name,Modifier.offset(x,y).size(if(index<2)32.dp else 34.dp).graphicsLayer{rotationZ=rotation},contentScale=ContentScale.Crop)
     }
    }
   }
   Box(Modifier.fillMaxWidth().padding(top=88.dp)){
    Box(Modifier.align(Alignment.TopStart).padding(start=4.dp)){MultiplierBadge(true)}
    Box(Modifier.align(Alignment.TopEnd).padding(end=4.dp)){MultiplierBadge(false)}
   }
   Column(Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top=8.dp)){
    Row(Modifier.fillMaxWidth().padding(horizontal=10.dp),verticalAlignment=Alignment.CenterVertically){
     Text("WILD PARTY",Modifier.weight(1f),color=wpGold,fontSize=17.sp,fontWeight=FontWeight.Black)
     Text("Round #$round",color=Color.White,fontSize=11.sp)
     IconButton(onClick={muted=!muted},modifier=Modifier.size(34.dp)){Icon(if(muted)Icons.Default.VolumeOff else Icons.Default.VolumeUp,null,tint=Color.White)}
     Text("－",Modifier.clickable{onMinimize()}.padding(6.dp),color=Color.White,fontSize=20.sp)
     Icon(Icons.Default.Close,null,Modifier.clickable{onClose()}.padding(6.dp),tint=Color.White)
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically){
     Text("History",Modifier.clickable{historyOpen=true}.padding(7.dp),color=Color.White,fontSize=12.sp)
     Text("Rules",Modifier.clickable{rulesOpen=true}.padding(7.dp),color=Color.White,fontSize=12.sp)
     Text(when(phase){WpPhase.BETTING->"Betting";WpPhase.SPINNING->"Spinning";WpPhase.RESULT->"Result"}+"  $countdown s",color=wpGold,fontWeight=FontWeight.Bold,fontSize=13.sp)
    }
   }
   Box(Modifier.align(Alignment.Center).size(320.dp),contentAlignment=Alignment.Center){
    // Original TSX decorative wheel image behind all animal icons.
    val wheelBg=wpAsset(context,"wp_1787344138649_2")
    if(wheelBg!=0)Image(painterResource(wheelBg),null,Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
    Box(Modifier.size(260.dp).clip(CircleShape).background(Color(0xCC311D0C)).border(5.dp,Color(0xFFFFD16B),CircleShape))
    Canvas(Modifier.size(250.dp)){drawCircle(Color(0xFF6B3B14),style=androidx.compose.ui.graphics.drawscope.Stroke(5.dp.toPx()));for(i in 0..7){val a=Math.toRadians(i*45.0-90);val c=size.width/2;drawLine(Color(0xFFFFD56A),androidx.compose.ui.geometry.Offset(c,c),androidx.compose.ui.geometry.Offset(c+cos(a).toFloat()*size.width*.48f,c+sin(a).toFloat()*size.height*.48f),2.dp.toPx())}}
    wpAnimals.forEachIndexed{i,a->
     val rad=Math.toRadians(a.angle.toDouble());val x=(cos(rad)*130).dp+a.dx.dp;val y=(sin(rad)*130).dp+a.dy.dp
     val active=highlight==i||(phase==WpPhase.RESULT&&((mode==WpMode.MIX_BIG&&i>=4)||(mode==WpMode.MIX_SMALL&&i<4)))
     val image=wpAsset(context,a.image)
     Box(Modifier.offset(x,y).size(a.size.dp).clip(CircleShape).background(Color(0xFF4A2B13)).border(if(active)3.dp else 1.dp,if(active)wpGold else Color(0xFFB8864B),CircleShape).clickable{place(a.name)}.padding(4.dp),contentAlignment=Alignment.Center){
      if(image!=0)Image(painterResource(image),a.name,Modifier.fillMaxSize().graphicsLayer{alpha=if(phase==WpPhase.BETTING||active)1f else .4f},contentScale=ContentScale.Fit)
      val amount=bets[a.name]?:0L
      if(amount>0)Text(wpFormat(amount),Modifier.align(Alignment.BottomCenter).clip(CircleShape).background(Color(0xFFF6D7B0)).padding(horizontal=5.dp,vertical=1.dp),color=Color(0xFF65330E),fontSize=8.sp,fontWeight=FontWeight.Black)
     }
    }
    Box(Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF8B4D16)).border(3.dp,wpGold,CircleShape),contentAlignment=Alignment.Center){Text(if(phase==WpPhase.SPINNING)"SPIN" else "WP",color=Color.White,fontWeight=FontWeight.Black)}
   }
   Row(Modifier.align(Alignment.BottomStart).padding(start=10.dp,bottom=12.dp),verticalAlignment=Alignment.CenterVertically){
    val coin=wpAsset(context,"wp_file_00000000e56882119c217d508b6733dc");if(coin!=0)Image(painterResource(coin),null,Modifier.size(20.dp))
    Text(balance.toString(),color=wpGold,fontSize=12.sp,fontWeight=FontWeight.Black)
   }
   // Original TSX bottom decoration sits behind the balance and chip controls.
   val bottomDecor=wpAsset(context,"wp_img_20260822_011000")
   if(bottomDecor!=0)Image(
    painterResource(bottomDecor),null,
    Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(112.dp),
    contentScale=ContentScale.Fit,
    alignment=Alignment.BottomCenter
   )
   Row(Modifier.align(Alignment.BottomEnd).padding(end=8.dp,bottom=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){
    Text("Repeat",Modifier.clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha=.2f)).clickable{scope.launch{val cost=lastBets.values.sum();if(phase==WpPhase.BETTING&&cost>0&&balance>=cost){walletChange(-cost,"Repeat Bet in Wild Party");bets=bets.toMutableMap().also{m->lastBets.forEach{(k,v)->m[k]=(m[k]?:0L)+v}}}}}.padding(horizontal=7.dp,vertical=7.dp),color=Color.White,fontSize=9.sp,fontWeight=FontWeight.Black)
    wpChips.forEachIndexed{i,c->val id=wpAsset(context,c.image);Box(Modifier.size(34.dp).graphicsLayer{val scale=if(chipIndex==i)1.08f else 1f;scaleX=scale;scaleY=scale;translationY=if(chipIndex==i)-8.dp.toPx() else 0f}.border(if(chipIndex==i)2.dp else 0.dp,wpGold,CircleShape).clip(CircleShape).background(Color(0xFF54351C)).clickable{chipIndex=i},contentAlignment=Alignment.Center){if(id!=0)Image(painterResource(id),c.label,Modifier.fillMaxSize(),contentScale=ContentScale.Fit)else Text(c.label,color=Color.White,fontSize=8.sp)}}
   }
   if(loading)Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.8f)),contentAlignment=Alignment.Center){CircularProgressIndicator(color=wpGold)}
  }
  if(historyOpen)WpSheet("History",{historyOpen=false}){
   Row(Modifier.fillMaxWidth().padding(vertical=7.dp),horizontalArrangement=Arrangement.SpaceAround){Text("Round",color=wpGold,fontSize=10.sp);wpAnimals.forEach{val id=wpAsset(context,it.image);if(id!=0)Image(painterResource(id),null,Modifier.size(23.dp))}}
   LazyColumn(Modifier.fillMaxWidth().weight(1f)){items(history.reversed(),key={it.round}){r->Row(Modifier.fillMaxWidth().padding(vertical=7.dp),horizontalArrangement=Arrangement.SpaceAround,verticalAlignment=Alignment.CenterVertically){Text("#${r.round}",color=Color.White,fontSize=10.sp);wpAnimals.forEachIndexed{i,a->val yes=r.winner==a.name||(r.winner=="Mix Big"&&i>=4)||(r.winner=="Mix Small"&&i<4);Text(if(yes)"✓" else "✕",color=if(yes)Color(0xFF34D399)else Color(0xFF9F5360),fontSize=12.sp)}};HorizontalDivider(color=Color.White.copy(alpha=.06f))}}}
  if(rulesOpen)WpSheet("Rules",{rulesOpen=false}){Column(Modifier.verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(13.dp)){listOf("Select your Chip and Choose the animals.","If Lion wins you will receive 45 times × your Bet.","For placing bet click on the Animals.","All 5 time gives 5 × your bet.","45, 25, 15, 10 TIME gives you × your bet.","Mix Big gives rewards on Eagle, Bear, Tiger, Lion. Mix Small rewards Deer, Dog, Zebra, Fox.").forEach{Text("•  $it",color=Color.White,fontSize=13.sp,fontWeight=FontWeight.SemiBold)}}}
  if(winnerOpen)WpSheet("Wild Party Winners",{winnerOpen=false}){
   Column(Modifier.fillMaxWidth().fillMaxHeight().verticalScroll(rememberScrollState()).padding(horizontal=16.dp,vertical=12.dp),horizontalAlignment=Alignment.CenterHorizontally){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){Text("${winnerTimer}s",color=Color.White,fontWeight=FontWeight.Black,fontSize=12.sp)}
    Text("Winning Amount   ${winAmount}",color=wpGold,fontWeight=FontWeight.Black,fontSize=17.sp)
    Text("Bet Amount   ${betAmount}",color=Color.White,fontSize=13.sp)
    Spacer(Modifier.height(12.dp))
    val heading=wpAsset(context,"wp_img_20260913_000423")
    if(heading!=0)Image(painterResource(heading),null,Modifier.fillMaxWidth(.80f).height(70.dp),contentScale=ContentScale.Fit)
    Spacer(Modifier.height(10.dp))
    Row(
     Modifier.fillMaxWidth().height(150.dp),
     horizontalArrangement=Arrangement.spacedBy(4.dp,Alignment.CenterHorizontally),
     verticalAlignment=Alignment.Bottom
    ){
     val frameItems=listOf(
      Triple("2nd","wp_img_20260912_235156",.84f),
      Triple("1st","wp_img_20260912_235215",1f),
      Triple("3rd","wp_img_20260912_235230",.84f)
     )
     frameItems.forEachIndexed{index,item->
      val frame=wpAsset(context,item.second)
      val animalImage=wpAsset(context,wpAnimals[winnerIndex.coerceIn(0,7)].image)
      Box(Modifier.weight(1f).fillMaxHeight(if(index==1)1f else .88f),contentAlignment=Alignment.Center){
       if(frame!=0)Image(painterResource(frame),null,Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
       if(animalImage!=0)Image(
        painterResource(animalImage),null,
        Modifier.align(Alignment.Center).size(if(index==1)48.dp else 40.dp).clip(CircleShape),
        contentScale=ContentScale.Crop
       )
      }
     }
    }
   }
  }
 }
}

@Composable
private fun WpSheet(title:String,onDismiss:()->Unit,content:@Composable ColumnScope.()->Unit){
 Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.45f)).clickable{onDismiss()},contentAlignment=Alignment.BottomCenter){
  Column(Modifier.fillMaxWidth().fillMaxHeight(.40f).clip(RoundedCornerShape(topStart=12.dp,topEnd=12.dp)).background(wpDark).padding(horizontal=14.dp,vertical=12.dp)){
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("‹",Modifier.clickable{onDismiss()}.padding(horizontal=8.dp),color=Color.White,fontSize=28.sp);Text(title,Modifier.weight(1f),color=Color.White,fontWeight=FontWeight.Black,fontSize=16.sp,textAlign=TextAlign.Center);Spacer(Modifier.width(32.dp))}
   HorizontalDivider(color=Color.White.copy(alpha=.1f));content()
  }
 }
}
