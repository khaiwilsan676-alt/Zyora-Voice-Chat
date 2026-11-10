package com.hurry.voicechat

import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.*

private data class FruitItem(val id:Int,val label:String,val image:String,val multiplier:Int)
private data class FruitHistory(val round:Int,val winnerId:Int,val bets:Map<Int,Long>,val won:Long,val time:Long)
private enum class FruitPhase { BETTING, SPINNING, RESULT }

private val fruitItems=listOf(
 FruitItem(1,"Lemon","fp_img_20260908_192143",5),
 FruitItem(5,"Grapes","fp_img_20260908_192120",10),
 FruitItem(3,"Orange","fp_img_20260908_192203",5),
 FruitItem(8,"Cherry","fp_img_20260908_192013",45),
 FruitItem(2,"Apple","fp_img_20260908_192050",25),
 FruitItem(7,"Mango","fp_img_20260908_191941",5),
 FruitItem(4,"Strawberry","fp_img_20260908_191906",15),
 FruitItem(6,"Pear","fp_img_20260908_191930",5)
)
private val fruitOrder=listOf(1,5,3,8,2,7,4,6)
private val fruitGridIds=listOf(1,5,3,8,0,2,7,4,6)
private val fruitSpinPath=listOf(0,1,2,5,8,7,6,3)
private val fruitChipValues=listOf(1_000L,50_000L,1_000_000L,5_000_000L)
private val fruitChipLabels=listOf("1K","50K","1M","5M")
private val fruitGold=Color(0xFFFFD65A)
private val fruitBrown=Color(0xFF4A2810)
private val fruitPurple=Color(0xFF330C36)
private const val fruitDefaultBalance=82927L

private fun fruitAsset(context:Context,name:String)=context.resources.getIdentifier(name,"drawable",context.packageName)
private fun fruitRound(now:Long):Int=((now/41_000L)%10_000L).toInt()+1000
private fun fruitWinner(round:Int):Int {
 val seed=sin(round.toDouble())*10000.0
 val random=seed-floor(seed)
 return when {
  random<.025->10
  random<.05->11
  random<.85->listOf(0,2,8,6)[floor(random*100).toInt()%4]
  else->listOf(1,5,7,3)[floor(random*100).toInt()%4]
 }
}
private fun fruitMultiplier(id:Int)=fruitItems.firstOrNull{it.id==id}?.multiplier?:when(id){10->20;11->95;else->0}
private fun fruitImage(id:Int)=when(id){10->"fp_img_20260910_114515";11->"fp_img_20260910_114613";else->fruitItems.firstOrNull{it.id==id}?.image.orEmpty()}
private fun fruitWinnerId(index:Int)=if(index==10||index==11)index else fruitGridIds.getOrNull(index)?:0
private fun fruitAward(winnerIndex:Int,bets:Map<Int,Long>):Long=when(winnerIndex){
 10->(bets[10]?:0L)*20L+listOf(1,3,7,6).sumOf{(bets[it]?:0L)*5L}
 11->(bets[11]?:0L)*95L+(bets[5]?:0L)*10L+(bets[4]?:0L)*15L+(bets[2]?:0L)*25L+(bets[8]?:0L)*45L
 else->{val id=fruitWinnerId(winnerIndex);(bets[id]?:0L)*fruitMultiplier(id)}
}
private fun fruitBoundary(now:Long=System.currentTimeMillis()):Long {
 val c=java.util.Calendar.getInstance().apply{timeInMillis=now}
 if(c.get(java.util.Calendar.HOUR_OF_DAY)<5 || (c.get(java.util.Calendar.HOUR_OF_DAY)==5&&c.get(java.util.Calendar.MINUTE)<30)) c.add(java.util.Calendar.DAY_OF_YEAR,-1)
 c.set(java.util.Calendar.HOUR_OF_DAY,5);c.set(java.util.Calendar.MINUTE,30);c.set(java.util.Calendar.SECOND,0);c.set(java.util.Calendar.MILLISECOND,0)
 return c.timeInMillis
}
private fun fruitFormat(value:Long):String=when{
 value>=1_000_000_000L->"${"%.1f".format(value/1_000_000_000.0)}B"
 value>=1_000_000L->"${"%.1f".format(value/1_000_000.0).removeSuffix(".0")}M"
 value>=1_000L->"${"%.0f".format(value/1_000.0)}K"
 else->value.toString()
}
private fun fruitLoadHistory(context:Context):List<FruitHistory>{
 val prefs=context.getSharedPreferences("fruit_party_native",Context.MODE_PRIVATE)
 val boundary=fruitBoundary()
 val a=JSONArray(prefs.getString("history","[]"))
 return (0 until a.length()).mapNotNull{i->runCatching{
  val o=a.getJSONObject(i);val b=o.getJSONObject("bets");val map=mutableMapOf<Int,Long>()
  b.keys().forEach{k->map[k.toInt()]=b.optLong(k)}
  FruitHistory(o.optInt("round"),o.optInt("winner"),map,o.optLong("won"),o.optLong("time"))
 }.getOrNull()}.filter{it.time>=boundary}.sortedByDescending{it.time}.take(50)
}
private fun fruitSaveHistory(context:Context,list:List<FruitHistory>){
 val a=JSONArray()
 list.take(50).forEach{h->val b=JSONObject();h.bets.forEach{(k,v)->b.put(k.toString(),v)}
  a.put(JSONObject().put("round",h.round).put("winner",h.winnerId).put("bets",b).put("won",h.won).put("time",h.time))}
 context.getSharedPreferences("fruit_party_native",Context.MODE_PRIVATE).edit().putString("history",a.toString()).apply()
}


@Composable
private fun FruitTopGlyph(kind:String, disabled:Boolean=false){
 Canvas(Modifier.size(15.dp)){
  val sx=size.width/24f; val sy=size.height/24f
  fun pt(x:Float,y:Float)=Offset(x*sx,y*sy)
  fun ln(x1:Float,y1:Float,x2:Float,y2:Float,w:Float=1.8f){drawLine(fruitBrown,pt(x1,y1),pt(x2,y2),w*sx,cap=StrokeCap.Round)}
  when(kind){
   "vibration"->{
    val p=Path().apply{moveTo(8f*sx,1f*sy);lineTo(16f*sx,1f*sy);quadraticTo(18f*sx,1f*sy,18f*sx,3f*sy);lineTo(18f*sx,21f*sy);quadraticTo(18f*sx,23f*sy,16f*sx,23f*sy);lineTo(8f*sx,23f*sy);quadraticTo(6f*sx,23f*sy,6f*sx,21f*sy);lineTo(6f*sx,3f*sy);quadraticTo(6f*sx,1f*sy,8f*sx,1f*sy);close()}
    drawPath(p,fruitBrown,style=Stroke(1.8f*sx,join=StrokeJoin.Round));ln(9f,4f,15f,4f,1.2f);ln(9f,20f,15f,20f,1.2f)
    if(disabled)ln(2f,2f,22f,22f,2f) else {ln(3f,7f,3f,17f);ln(21f,7f,21f,17f)}
   }
   "sound"->{
    val p=Path().apply{moveTo(3f*sx,9f*sy);lineTo(7f*sx,9f*sy);lineTo(12f*sx,4f*sy);lineTo(12f*sx,20f*sy);lineTo(7f*sx,15f*sy);lineTo(3f*sx,15f*sy);close()}
    drawPath(p,fruitBrown)
    if(disabled){ln(15f,9f,21f,15f);ln(21f,9f,15f,15f)}else{
     val w=Path().apply{moveTo(15f*sx,8f*sy);quadraticTo(19f*sx,12f*sy,15f*sx,16f*sy);moveTo(17f*sx,4f*sy);quadraticTo(24f*sx,12f*sy,17f*sx,20f*sy)}
     drawPath(w,fruitBrown,style=Stroke(1.7f*sx,cap=StrokeCap.Round,join=StrokeJoin.Round))
    }
   }
   "minimize"->{val p=Path().apply{moveTo(6f*sx,9f*sy);lineTo(12f*sx,15f*sy);lineTo(18f*sx,9f*sy)};drawPath(p,fruitBrown,style=Stroke(4f*sx,cap=StrokeCap.Round,join=StrokeJoin.Round))}
   "history"->{drawCircle(fruitBrown,10f*sx,pt(12f,12f),style=Stroke(1.8f*sx));ln(12f,6f,12f,12f);ln(12f,12f,17f,15f)}
   "close"->{ln(6f,6f,18f,18f,2f);ln(18f,6f,6f,18f,2f)}
  }
 }
}

/** Fully native Fruit Party game. Artwork is bundled in Android drawable resources; no WebView or network page. */
@Composable
fun Fruitparty(onClose:()->Unit,onMinimize:(()->Unit)?=null,isMinimized:Boolean=false){
 val context= LocalContext.current
 val scope= rememberCoroutineScope()
 val dao= remember{WalletDatabase.get(context).walletDao()}
 var balance by remember{mutableLongStateOf(fruitDefaultBalance)}
 var totalWon by remember{mutableLongStateOf(0L)}
 var phase by remember{mutableStateOf(FruitPhase.BETTING)}
 var round by remember{mutableIntStateOf(fruitRound(System.currentTimeMillis()))}
 var winner by remember{mutableIntStateOf(fruitWinner(fruitRound(System.currentTimeMillis())))}
 var highlight by remember{mutableIntStateOf(-1)}
 var handPointer by remember{mutableIntStateOf(0)}
 var countdown by remember{mutableIntStateOf(30)}
 var chipIndex by remember{mutableIntStateOf(0)}
 var bets by remember{mutableStateOf<Map<Int,Long>>(emptyMap())}
 var history by remember{mutableStateOf<List<FruitHistory>>(emptyList())}
 var winners by remember{mutableStateOf<List<Int>>(emptyList())}
 var showHistory by remember{mutableStateOf(false)}
 var showRules by remember{mutableStateOf(false)}
 var showResult by remember{mutableStateOf(false)}
 var muted by remember{mutableStateOf(false)}
 var vibration by remember{mutableStateOf(true)}
 var loaded by remember{mutableStateOf(false)}
 var processedRound by remember{mutableIntStateOf(-1)}
 var lastBet by remember{mutableLongStateOf(0L)}
 var lastWin by remember{mutableLongStateOf(0L)}
 var lastRound by remember{mutableIntStateOf(round)}
 var minimizedSession by remember{mutableStateOf(false)}

 LaunchedEffect(Unit){
  runCatching{
   balance=dao.getUserData()?.balance?:fruitDefaultBalance
   history=fruitLoadHistory(context)
   totalWon=context.getSharedPreferences("fruit_party_native",Context.MODE_PRIVATE).getLong("totalWon",0L)
  }
  delay(200);loaded=true
 }
 suspend fun updateWallet(delta:Long,title:String){
  val old=dao.getUserData()?:UserDataEntity(balance=balance)
  val next=(old.balance+delta).coerceAtLeast(0L)
  dao.saveUserData(old.copy(balance=next));balance=next
  val c=java.util.Calendar.getInstance()
  val date=String.format("%04d.%02d.%02d %02d:%02d",c.get(java.util.Calendar.YEAR),c.get(java.util.Calendar.MONTH)+1,c.get(java.util.Calendar.DAY_OF_MONTH),c.get(java.util.Calendar.HOUR_OF_DAY),c.get(java.util.Calendar.MINUTE))
  dao.insertTransaction(TransactionEntity(title=title,amount=delta,date=date,timestamp=System.currentTimeMillis(),type="coin"))
 }
 fun placeBet(id:Int){
  val amount=fruitChipValues[chipIndex]
  if(phase!=FruitPhase.BETTING||!loaded||balance<amount)return
  scope.launch{updateWallet(-amount,"Bet placed on Fruit Party");bets=bets.toMutableMap().also{it[id]=(it[id]?:0L)+amount}}
 }
 LaunchedEffect(loaded){
  if(!loaded)return@LaunchedEffect
  while(true){
   val now=System.currentTimeMillis();val elapsed=now%41_000L
   val r=fruitRound(now);val win=fruitWinner(r)
   round=r;winner=win
   when{
    elapsed<30_000L->{phase=FruitPhase.BETTING;countdown=30-(elapsed/1000L).toInt();highlight=-1;handPointer=fruitSpinPath[((elapsed/1000L).toInt())%8];showResult=false}
    elapsed<36_000L->{
     phase=FruitPhase.SPINNING;countdown=6-((elapsed-30_000L)/1000L).toInt()
     val spinElapsed=elapsed-31_000L
     if(spinElapsed<0)highlight=fruitSpinPath[0] else{
      val target=fruitSpinPath.indexOf(win).let{if(it<0)0 else it}
      val progress=(spinElapsed/5000.0).coerceIn(0.0,1.0)
      val eased=1.0-(1.0-progress).pow(3)
      val step=(55*eased).roundToInt()
      val start=(target-(55%8)+80)%8
      highlight=fruitSpinPath[(start+step)%8]
     }
    }
    else->{phase=FruitPhase.RESULT;countdown=5-((elapsed-36_000L)/1000L).toInt();highlight=win;showResult=elapsed>=36_500L}
   }
   delay(50)
  }
 }
 LaunchedEffect(phase,round,loaded){
  if(!loaded||phase!=FruitPhase.RESULT||processedRound==round)return@LaunchedEffect
  processedRound=round
  val current=bets.toMap()
  val winnerId=fruitWinnerId(winner)
  val earned=fruitAward(winner,current)
  val totalBet=current.values.sum()
  val winningImage=fruitImage(winnerId)
  if(winningImage.isNotEmpty()){
   lastBet=totalBet;lastWin=earned;lastRound=round
   if(earned>0){updateWallet(earned,"Won in Fruit Party");totalWon+=earned;context.getSharedPreferences("fruit_party_native",Context.MODE_PRIVATE).edit().putLong("totalWon",totalWon).apply()}
   val record=FruitHistory(round,winnerId,current,earned,System.currentTimeMillis())
   if(totalBet>0){history=(listOf(record)+history).take(50);fruitSaveHistory(context,history)}
   winners=(listOf(winnerId)+winners).take(18)
   bets=emptyMap()
  }
 }
 LaunchedEffect(isMinimized){if(isMinimized)minimizedSession=true}
 if(isMinimized)return
 BoxWithConstraints(Modifier.fillMaxSize().background(Color.Transparent),contentAlignment=Alignment.BottomCenter){
  val screenH=maxHeight
  val panelH=screenH*.68f
  Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.5f)).clickable(interactionSource=null,indication=null){onClose()})
  Box(Modifier.fillMaxWidth().height(panelH).align(Alignment.BottomCenter).background(fruitPurple)){
   val bg=fruitAsset(context,"fp_file_000000005f948210bbb51c62a68f797a")
   if(bg!=0)Image(painterResource(bg),null,Modifier.fillMaxSize(),contentScale=ContentScale.FillBounds)
   else Box(Modifier.fillMaxSize().background(fruitPurple))
   Row(Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(horizontal=22.dp, vertical=5.dp),verticalAlignment=Alignment.CenterVertically){
    Row(horizontalArrangement=Arrangement.spacedBy(2.dp),verticalAlignment=Alignment.CenterVertically){
     Box(Modifier.size(24.dp).border(2.dp,fruitBrown,CircleShape).clickable(interactionSource=null,indication=null){vibration=!vibration},contentAlignment=Alignment.Center){FruitTopGlyph("vibration",!vibration)}
     Box(Modifier.size(24.dp).border(2.dp,fruitBrown,CircleShape).clickable(interactionSource=null,indication=null){muted=!muted},contentAlignment=Alignment.Center){FruitTopGlyph("sound",muted)}
     Box(Modifier.size(24.dp).border(2.dp,fruitBrown,CircleShape).clickable(interactionSource=null,indication=null){showRules=true},contentAlignment=Alignment.Center){Text("?",color=fruitBrown,fontSize=18.sp,fontWeight=FontWeight.Black,fontFamily=androidx.compose.ui.text.font.FontFamily.Serif)}
    }
    Spacer(Modifier.weight(1f))
    Text("Round of Today $round",color=Color.White,fontSize=10.sp,fontWeight=FontWeight.Bold)
    Spacer(Modifier.weight(1f))
    Row(horizontalArrangement=Arrangement.spacedBy(2.dp),verticalAlignment=Alignment.CenterVertically){
     Box(Modifier.size(24.dp).border(2.dp,fruitBrown,CircleShape).clickable(interactionSource=null,indication=null){minimizedSession=true;onMinimize?.invoke()},contentAlignment=Alignment.Center){FruitTopGlyph("minimize")}
     Box(Modifier.size(24.dp).border(2.dp,fruitBrown,CircleShape).clickable(interactionSource=null,indication=null){showHistory=true},contentAlignment=Alignment.Center){FruitTopGlyph("history")}
     Box(Modifier.size(24.dp).border(2.dp,fruitBrown,CircleShape).clickable(interactionSource=null,indication=null){onClose()},contentAlignment=Alignment.Center){FruitTopGlyph("close")}
    }
   }
   Row(Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top=34.dp,start=26.dp,end=26.dp),horizontalArrangement=Arrangement.SpaceBetween){
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){val coin=fruitAsset(context,"fp_file_00000000e56882119c217d508b6733dc");if(coin!=0)Image(painterResource(coin),null,Modifier.size(20.dp));Text(balance.toString(),color=Color.White,fontSize=12.sp,fontWeight=FontWeight.Bold)}
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){val coin=fruitAsset(context,"fp_file_00000000e56882119c217d508b6733dc");if(coin!=0)Image(painterResource(coin),null,Modifier.size(20.dp));Text(totalWon.toString(),color=Color.White,fontSize=12.sp,fontWeight=FontWeight.Bold)}
   }
   Row(Modifier.align(Alignment.BottomCenter).padding(bottom=(screenH*.025f-2.dp)).fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start=(screenH*.12f-8.dp)),horizontalArrangement=Arrangement.spacedBy(8.dp)){
    winners.asReversed().forEachIndexed{i,id->
     Box(Modifier.size(19.dp).border(1.dp,Color(0xFF3A1D09),CircleShape),contentAlignment=Alignment.Center){
      val res=fruitAsset(context,fruitImage(id))
      if(res!=0)Image(painterResource(res),null,Modifier.size(19.dp).clip(CircleShape),contentScale=ContentScale.Crop)
      if(i==0)Text("NEW",Modifier.align(Alignment.BottomCenter).offset(y=7.dp).background(Color(0xFF16A34A),RoundedCornerShape(2.dp)).padding(horizontal=2.dp,vertical=0.dp),color=Color.White,fontSize=5.sp,fontWeight=FontWeight.Black)
     }
    }
   }
   // Keep all four betting chips on one evenly spaced row; prevent the 5M chip from slipping behind adjacent artwork.
   Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal=4.dp).padding(bottom=screenH*.14f),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.Bottom){
    fruitChipLabels.forEachIndexed{i,label->
     Box(Modifier.weight(1f).height(120.dp).clickable(interactionSource=null,indication=null){chipIndex=i},contentAlignment=Alignment.TopCenter){
      val plate=fruitAsset(context,"fp_file_00000000d9b08211b0304c61b802348b")
      val chip=fruitAsset(context,"fp_file_000000003d24821182882f8ca412d2b6")
      if(plate!=0)Image(painterResource(plate),null,Modifier.fillMaxWidth().offset(y=if(chipIndex==i)44.dp else 36.dp).zIndex(if(chipIndex==i)0f else 1f),contentScale=ContentScale.Fit)
      if(chip!=0)Image(painterResource(chip),null,Modifier.fillMaxWidth().offset(y=42.dp).zIndex(if(chipIndex==i)1f else 0f),contentScale=ContentScale.Fit)
      Text(label,Modifier.align(Alignment.BottomCenter).padding(bottom=42.dp).zIndex(2f),color=Color.White,fontSize=14.sp,fontWeight=FontWeight.Bold)
     }
    }
   }
  // Eight original fruit tiles around the timer.
  Column(Modifier.fillMaxWidth().padding(horizontal=screenH*.055f).offset(y=screenH*.045f),verticalArrangement=Arrangement.spacedBy(0.dp)){
   val rows=listOf(fruitOrder.take(3),listOf(fruitOrder[3],0,fruitOrder[4]),fruitOrder.drop(5))
   rows.forEachIndexed{rowIndex,row->
    Row(Modifier.fillMaxWidth().height(screenH*.115f),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically){
     row.forEach{index->
      if(index==0){
       Box(Modifier.weight(1f).fillMaxHeight().padding(5.dp),contentAlignment=Alignment.Center){
        val timerTile=fruitAsset(context,"fp_file_0000000023cc8230baec62632d74e698")
        if(timerTile!=0)Image(painterResource(timerTile),null,Modifier.fillMaxSize(),contentScale=ContentScale.FillBounds)
        Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(2.dp)){
         Text(when(phase){FruitPhase.BETTING->"BETTING";FruitPhase.SPINNING->"SPINNING";FruitPhase.RESULT->"RESULT"},color=Color.White,fontSize=8.sp,fontWeight=FontWeight.Bold)
         Text("${countdown}s",color=Color(0xFFFFC34D),fontSize=20.sp,fontWeight=FontWeight.Black)
        }
       }
      }else{
       val item=fruitItems.first{it.id==index}
       val gridIndex=fruitGridIds.indexOf(item.id)
       val active=(phase==FruitPhase.BETTING&&handPointer==gridIndex)||(phase!=FruitPhase.BETTING&&highlight==gridIndex)
       val final=phase==FruitPhase.RESULT&&(winner==gridIndex||(winner==10&&gridIndex in listOf(0,2,6,8))||(winner==11&&gridIndex in listOf(1,3,5,7)))
       val applyGreen=active||final
       val highlightMatrix=ColorMatrix(
        android.graphics.ColorMatrix().apply {
         val hue=android.graphics.ColorMatrix().apply { setRotate(0,-150f) }
         val saturation=android.graphics.ColorMatrix().apply { setSaturation(2f) }
         setConcat(hue,saturation)
        }.array
       )
       Box(Modifier.weight(1f).fillMaxHeight().padding(3.dp).clickable(interactionSource=null,indication=null){placeBet(item.id)},contentAlignment=Alignment.Center){
        val tile=fruitAsset(context,"fp_file_00000000d0ec820ba666eab8bea30204");if(tile!=0)Image(painterResource(tile),null,Modifier.fillMaxSize(),contentScale=ContentScale.FillBounds,colorFilter=if(applyGreen)ColorFilter.colorMatrix(highlightMatrix)else null)
        val image=fruitAsset(context,item.image);if(image!=0)Image(painterResource(image),null,Modifier.fillMaxSize(.57f).align(Alignment.Center).padding(bottom=12.dp),contentScale=ContentScale.Fit)
        if(phase==FruitPhase.BETTING&&handPointer==gridIndex){val pointer=fruitAsset(context,"fp_file_000000000f0c820b95490c9d927692d9");if(pointer!=0)Image(painterResource(pointer),null,Modifier.align(Alignment.BottomEnd).size(40.dp).graphicsLayer{rotationZ=-45f},contentScale=ContentScale.Fit)}
        Text("×${item.multiplier}",Modifier.align(Alignment.BottomCenter).padding(bottom=5.dp),color=Color.White,fontSize=11.sp,fontWeight=FontWeight.Black)
        val bet=bets[item.id]?:0L
        if(bet>0)Row(Modifier.align(Alignment.Center).offset(y=12.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xCC4F65B9)).padding(horizontal=3.dp,vertical=1.dp),verticalAlignment=Alignment.CenterVertically){val coin=fruitAsset(context,"fp_file_00000000e56882119c217d508b6733dc");if(coin!=0)Image(painterResource(coin),null,Modifier.size(10.dp));Text(fruitFormat(bet),color=Color.White,fontSize=7.sp,fontWeight=FontWeight.Bold)}
       }
      }
     }
    }
   }
  }
  // Left and right mix cards.
  Row(Modifier.fillMaxWidth().padding(horizontal=screenH*.13f).offset(y=-screenH*.19f),horizontalArrangement=Arrangement.spacedBy(14.dp)){
   listOf(10,11).forEach{id->
    Box(Modifier.weight(1f).height(screenH*.105f).clickable(interactionSource=null,indication=null){placeBet(id)},contentAlignment=Alignment.Center){
     val frame=fruitAsset(context,"fp_img_20260910_114625");if(frame!=0)Image(painterResource(frame),null,Modifier.fillMaxSize(),contentScale=ContentScale.FillBounds)
     val image=fruitAsset(context,fruitImage(id));if(image!=0)Image(painterResource(image),null,Modifier.fillMaxSize(.60f),contentScale=ContentScale.Fit)
     Text(if(id==10)"MIX ×5" else "MIX ×10–95",Modifier.align(Alignment.BottomCenter).padding(bottom=2.dp),color=Color.White,fontSize=8.sp,fontWeight=FontWeight.Black)
     val bet=bets[id]?:0L;if(bet>0)Text(fruitFormat(bet),Modifier.align(Alignment.TopCenter).background(Color(0xCC4F65B9)).padding(2.dp),color=Color.White,fontSize=8.sp)
    }
   }
  }
  if(showResult&&phase==FruitPhase.RESULT){
   Box(Modifier.fillMaxSize(),contentAlignment=Alignment.BottomCenter){
    Box(Modifier.fillMaxWidth().fillMaxHeight(.647f).clip(RoundedCornerShape(topStart=8.dp,topEnd=8.dp))){
     val resultBg=fruitAsset(context,"fp_file_00000000ced481fa9117afc4fa91791e")
     if(resultBg!=0)Image(painterResource(resultBg),null,Modifier.fillMaxSize(),contentScale=ContentScale.FillBounds) else Box(Modifier.fillMaxSize().background(Color(0xF0221025)))
     Row(Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(horizontal=16.dp,vertical=12.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
      Text("Round $round",color=Color.White,fontSize=18.sp,fontWeight=FontWeight.Bold)
      Text("${countdown}s",color=Color(0xFFFFC34D),fontSize=18.sp,fontWeight=FontWeight.Black)
     }
     Box(Modifier.align(Alignment.TopCenter).padding(top=42.dp).size(width=200.dp,height=145.dp),contentAlignment=Alignment.Center){
      val winFrame=fruitAsset(context,"fp_file_00000000eb0081f4885ade7d7db3bef8")
      if(winFrame!=0)Image(painterResource(winFrame),null,Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
      val winImage=fruitAsset(context,fruitImage(fruitWinnerId(winner)))
      if(winImage!=0)Image(painterResource(winImage),null,Modifier.size(50.dp),contentScale=ContentScale.Fit)
     }
     Column(Modifier.align(Alignment.Center).padding(top=32.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(3.dp)){
      Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){
       Text("Your Winning Amount",color=Color.White,fontSize=14.sp,fontWeight=FontWeight.Bold)
       val coin=fruitAsset(context,"fp_file_00000000e56882119c217d508b6733dc");if(coin!=0)Image(painterResource(coin),null,Modifier.size(16.dp))
       Text(fruitFormat(lastWin),color=Color(0xFF4ADE80),fontSize=14.sp,fontWeight=FontWeight.Bold)
      }
      Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){
       Text("Your Bet Amount",color=Color.White,fontSize=14.sp,fontWeight=FontWeight.Bold)
       val coin=fruitAsset(context,"fp_file_00000000e56882119c217d508b6733dc");if(coin!=0)Image(painterResource(coin),null,Modifier.size(16.dp))
       Text(fruitFormat(lastBet),color=Color(0xFFFDE68A),fontSize=14.sp,fontWeight=FontWeight.Bold)
      }
     }
     Column(Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom=10.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)){
      Box(Modifier.fillMaxWidth(.85f).height(1.dp).background(Color.White.copy(alpha=.2f)))
      Text("Top winner Of this Round",color=Color(0xFFFFE6A0),fontSize=10.sp,fontWeight=FontWeight.ExtraBold)
      Text("No winners yet.",color=Color.White,fontSize=12.sp,fontWeight=FontWeight.Bold)
     }
    }
   }
  }
  if(!loaded)Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.7f)),contentAlignment=Alignment.Center){CircularProgressIndicator(color=fruitGold)}
  if(showHistory)FruitNativeSheet("My Records",{showHistory=false}){
   if(history.isEmpty())Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("No played records available yet.",color=Color.Gray,fontSize=12.sp)}
   else LazyColumn(Modifier.fillMaxWidth().weight(1f)){
    items(history,key={"${it.round}_${it.time}"}){h->
     Column(Modifier.fillMaxWidth().padding(vertical=9.dp)){
      Text("Round ${h.round}",color=Color.White,fontWeight=FontWeight.Bold,fontSize=14.sp)
      Row(verticalAlignment=Alignment.CenterVertically){Text("Award Results: ",color=Color.Gray,fontSize=11.sp);val id=fruitAsset(context,fruitImage(h.winnerId));if(id!=0)Image(painterResource(id),null,Modifier.size(24.dp))}
      Spacer(Modifier.height(7.dp))
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceAround){Text("Fruit",color=Color.Gray,fontSize=10.sp);Text("Bet",color=Color.Gray,fontSize=10.sp);Text("Award",color=Color.Gray,fontSize=10.sp)}
      h.bets.filterValues{it>0}.forEach{(fruitId,amount)->
       val award=when(h.winnerId){10->if(fruitId==10)amount*20 else if(fruitId in listOf(1,3,7,6))amount*5 else 0L;11->when(fruitId){11->amount*95;5->amount*10;4->amount*15;2->amount*25;8->amount*45;else->0L};else->if(fruitId==h.winnerId)amount*fruitMultiplier(fruitId)else 0L}
       Row(Modifier.fillMaxWidth().padding(vertical=3.dp),horizontalArrangement=Arrangement.SpaceAround,verticalAlignment=Alignment.CenterVertically){
        val im=fruitAsset(context,fruitImage(fruitId));if(im!=0)Image(painterResource(im),null,Modifier.size(22.dp))
        Text(fruitFormat(amount),color=Color.White,fontSize=11.sp);Text(fruitFormat(award),color=if(award>0)Color(0xFF4ADE80)else Color.Gray,fontSize=11.sp)
       }
      }
     }
     HorizontalDivider(color=Color.White.copy(alpha=.12f))
    }
   }
  }
  if(showRules)FruitNativeSheet("Rules",{showRules=false}){
   Column(Modifier.verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(13.dp)){
    listOf("You can place bet by Clicking on the Red button and Place the bet On the Fruit Card","10,15,25,45 give you hight coins","You will Receive Coins according to ( Your Bet × Multipler)","Left mix Card Give you all 5 times Coins ( Your Bet × all ×5 Times )","Right Mix card Give you all High Cards ( Your Bet × 10,15,25,45 Times)").forEach{Text("•  $it",color=Color.White,fontSize=13.sp,fontWeight=FontWeight.SemiBold)}
   }
  }
  }
 }
}

@Composable
private fun FruitNativeSheet(title:String,onDismiss:()->Unit,content:@Composable ColumnScope.()->Unit){
 Box(Modifier.fillMaxSize().background(Color.Transparent).clickable(interactionSource=null,indication=null){onDismiss()},contentAlignment=Alignment.BottomCenter){
  Column(Modifier.fillMaxWidth().fillMaxHeight(.45f).clip(RoundedCornerShape(topStart=8.dp,topEnd=8.dp)).background(Color(0xF20A080B)).clickable(enabled=false){}.padding(horizontal=15.dp,vertical=12.dp)){
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("‹",Modifier.clickable(interactionSource=null,indication=null){onDismiss()}.padding(horizontal=8.dp),color=Color.White,fontSize=28.sp);Text(title,Modifier.weight(1f),color=Color.White,fontWeight=FontWeight.Bold,fontSize=16.sp,textAlign=TextAlign.Center);Spacer(Modifier.width(32.dp))}
   HorizontalDivider(color=Color.White.copy(alpha=.12f));content()
  }
 }
}
