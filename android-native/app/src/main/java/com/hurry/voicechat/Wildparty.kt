package com.hurry.voicechat

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import kotlin.math.*

private data class WpAnimal(val name:String,val image:String,val angle:Float,val dx:Float,val dy:Float,val size:Int,val multiplier:Int)
private data class WpChip(val label:String,val value:Long,val image:String)
private data class WpRound(val round:Int,val winner:String,val time:Long,val myBet:Long=0L,val winAmount:Long=0L)
private data class WpFlyingCoin(
 val id: Long,
 val start: Offset,
 val end: Offset,
 val delayMs: Long,
 val curveOffsetX: Float,
 val curveOffsetY: Float
)
private enum class WpPhase { BETTING, SPINNING, RESULT }
private enum class WpMode { SINGLE, MIX_BIG, MIX_SMALL }

private val wpAnimals = listOf(
 WpAnimal("Rabbit","img_20261121_095524",270f,0f,-11f,71,5),
 WpAnimal("Deer","img_20261121_095459",315f,-4f,-9f,62,5),
 WpAnimal("Dog","img_20261121_102923",0f,-8f,-17f,62,5),
 WpAnimal("Camel","img_20261121_095328",45f,-6f,-18f,60,5),
 WpAnimal("Eagle","img_20261121_103004",90f,0f,-18f,61,10),
 WpAnimal("Bear","img_20261121_102944",135f,5f,-15f,60,15),
 WpAnimal("Tiger","img_20261121_095350",180f,7f,-8f,63,25),
 WpAnimal("Lion","img_20261121_095405",225f,5f,-5f,65,45)
)
private val wpChips = listOf(
 WpChip("1K",1000,"img_20261121_102630"),
 WpChip("10K",10000,"img_20261121_102707"),
 WpChip("50K",50000,"img_20261121_102752"),
 WpChip("500K",500000,"img_20261121_102814"),
 WpChip("5M",5000000,"img_20261121_102835")
)
private val wpGold=Color(0xFFFFD33D)
private val wpDark=Color(0xF20C0C0E)
private fun wpAsset(n:String):String {
 if (n.isBlank()) return ""
 val raw=n.removePrefix("wp_")
 val file=when {
  raw.startsWith("img_") -> "IMG_${raw.removePrefix("img_")}.png"
  raw.startsWith("IMG_") -> if(raw.endsWith(".png")) raw else "$raw.png"
  raw.startsWith("file_") -> if(raw.endsWith(".png")) raw else "$raw.png"
  raw.startsWith("1787337855180_2") || raw.startsWith("1787337855180~2") -> "1787337855180~2.jpg"
  else -> if(raw.endsWith(".png") || raw.endsWith(".jpg")) raw else "$raw.png"
 }
 return "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/$file"
}
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
private fun wpFormatTime(timeMillis: Long): String {
 val c = Calendar.getInstance().apply { timeInMillis = timeMillis }
 return String.format("%02d-%02d %02d:%02d", c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
}

private class WpSoundManager(private val context: Context) {
 private val baseUrl = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/"
 private val spinFileName = "VID_20261121_003408_517_bsl.mp4"
 private val betFileName = "VID_20261121_003527_312_bsl.mp4"
 private val winnerFileName = "VID_20261121_003223_909_bsl.mp4"

 private val spinPool = mutableListOf<MediaPlayer>()
 private val betPool = mutableListOf<MediaPlayer>()
 private var winnerPlayer: MediaPlayer? = null
 private var spinCursor = 0
 private var betCursor = 0
 @Volatile private var muted = false
 @Volatile private var released = false

 suspend fun preload() = withContext(Dispatchers.IO) {
  val spinFile = ensureCached(spinFileName)
  val betFile = ensureCached(betFileName)
  val winFile = ensureCached(winnerFileName)
  if (released) return@withContext
  synchronized(this@WpSoundManager) {
   if (released) return@synchronized
   if (spinFile != null && spinPool.isEmpty()) {
    repeat(4) { createPreparedPlayer(spinFile)?.let { spinPool.add(it) } }
   }
   if (betFile != null && betPool.isEmpty()) {
    repeat(4) { createPreparedPlayer(betFile)?.let { betPool.add(it) } }
   }
   if (winFile != null && winnerPlayer == null) {
    winnerPlayer = createPreparedPlayer(winFile)
   }
   applyVolumeLocked(if (muted) 0f else 1f)
  }
 }

 private fun ensureCached(fileName: String): File? {
  val target = File(context.cacheDir, "wp_sfx_$fileName")
  if (target.exists() && target.length() > 0L) return target
  return runCatching {
   val tmp = File(context.cacheDir, "wp_sfx_${fileName}.tmp")
   val conn = (URL(baseUrl + fileName).openConnection() as HttpURLConnection).apply {
    connectTimeout = 8000
    readTimeout = 10000
    instanceFollowRedirects = true
   }
   try {
    if (conn.responseCode in 200..299) {
     conn.inputStream.use { input ->
      tmp.outputStream().use { output -> input.copyTo(output) }
     }
     if (tmp.exists() && tmp.length() > 0L) {
      if (target.exists()) target.delete()
      tmp.renameTo(target)
      target
     } else null
    } else null
   } finally {
    conn.disconnect()
   }
  }.getOrNull()
 }

 private fun createPreparedPlayer(file: File): MediaPlayer? {
  return runCatching {
   MediaPlayer().apply {
    setAudioAttributes(
     AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_GAME)
      .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
      .build()
    )
    setDataSource(file.absolutePath)
    prepare()
   }
  }.getOrNull()
 }

 private fun applyVolumeLocked(vol: Float) {
  spinPool.forEach { runCatching { it.setVolume(vol, vol) } }
  betPool.forEach { runCatching { it.setVolume(vol, vol) } }
  winnerPlayer?.let { runCatching { it.setVolume(vol, vol) } }
 }

 fun setMuted(isMuted: Boolean) {
  synchronized(this) {
   muted = isMuted
   if (released) return
   if (isMuted) {
    applyVolumeLocked(0f)
    spinPool.forEach { runCatching { if (it.isPlaying) it.pause(); it.seekTo(0) } }
    betPool.forEach { runCatching { if (it.isPlaying) it.pause(); it.seekTo(0) } }
    winnerPlayer?.let { runCatching { if (it.isPlaying) it.pause(); it.seekTo(0) } }
   } else {
    applyVolumeLocked(1f)
   }
  }
 }

 fun playSpinTick(speedMultiplier: Float) {
  if (muted || released) return
  synchronized(this) {
   if (muted || released || spinPool.isEmpty()) return
   val mp = spinPool[spinCursor % spinPool.size]
   spinCursor++
   runCatching {
    if (mp.isPlaying) mp.pause()
    mp.setVolume(1f, 1f)
    mp.seekTo(0)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
     runCatching {
      mp.playbackParams = mp.playbackParams.setSpeed(speedMultiplier.coerceIn(0.95f, 1.95f))
     }
    }
    if (!mp.isPlaying) mp.start()
   }
  }
 }

 fun playBetClick() {
  if (muted || released) return
  synchronized(this) {
   if (muted || released || betPool.isEmpty()) return
   val mp = betPool[betCursor % betPool.size]
   betCursor++
   runCatching {
    if (mp.isPlaying) mp.pause()
    mp.setVolume(1f, 1f)
    mp.seekTo(0)
    mp.start()
   }
  }
 }

 fun playWinner() {
  if (muted || released) return
  synchronized(this) {
   if (muted || released) return
   val mp = winnerPlayer ?: return
   runCatching {
    if (mp.isPlaying) mp.pause()
    mp.setVolume(1f, 1f)
    mp.seekTo(0)
    mp.start()
   }
  }
 }

 fun stopWinner() {
  synchronized(this) {
   if (released) return
   winnerPlayer?.let { mp ->
    runCatching {
     if (mp.isPlaying) mp.pause()
     mp.seekTo(0)
    }
   }
  }
 }

 fun release() {
  synchronized(this) {
   released = true
   spinPool.forEach { runCatching { it.stop(); it.release() } }
   spinPool.clear()
   betPool.forEach { runCatching { it.stop(); it.release() } }
   betPool.clear()
   winnerPlayer?.let { runCatching { it.stop(); it.release() } }
   winnerPlayer = null
  }
 }
}

@Composable
private fun WpFlyingCoinItem(
 coinItem: WpFlyingCoin,
 coinRes: String,
 onFinished: (Long) -> Unit
) {
 val progress = remember { Animatable(0f) }
 var started by remember { mutableStateOf(false) }

 LaunchedEffect(coinItem.id) {
  if (coinItem.delayMs > 0L) delay(coinItem.delayMs)
  started = true
  progress.animateTo(
   targetValue = 1f,
   animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
  )
  onFinished(coinItem.id)
 }

 if (!started) return

 val t = progress.value
 val oneMinusT = 1f - t
 val midX = (coinItem.start.x + coinItem.end.x) / 2f + coinItem.curveOffsetX
 val midY = min(coinItem.start.y, coinItem.end.y) - 46f + coinItem.curveOffsetY
 val x = oneMinusT * oneMinusT * coinItem.start.x + 2f * oneMinusT * t * midX + t * t * coinItem.end.x
 val y = oneMinusT * oneMinusT * coinItem.start.y + 2f * oneMinusT * t * midY + t * t * coinItem.end.y
 val scale = if (t < 0.2f) 0.72f + (t / 0.2f) * 0.33f else 1.05f - ((t - 0.2f) / 0.8f) * 0.28f

 AsyncImage(
  model = coinRes,
  contentDescription = null,
  modifier = Modifier
   .size(18.dp)
   .graphicsLayer {
    translationX = x - 9.dp.toPx()
    translationY = y - 9.dp.toPx()
    scaleX = scale
    scaleY = scale
   },
  contentScale = ContentScale.Fit
 )
}

@Composable
private fun WpWildPartyTitle(
 height: Dp = 22.dp,
 modifier: Modifier = Modifier
) {
 val width = height * 3.68f
 Canvas(modifier = modifier.size(width = width, height = height)) {
  val sx = size.width / 220f
  val sy = size.height / 60f
  fun x(v: Float) = v * sx
  fun y(v: Float) = v * sy

  val titlePath = Path().apply {
   fillType = PathFillType.EvenOdd

   // W
   moveTo(x(3f), y(15f))
   quadraticTo(x(8f), y(14f), x(11.5f), y(18f))
   lineTo(x(12f), y(38f))
   quadraticTo(x(15f), y(42f), x(19.5f), y(37f))
   lineTo(x(21f), y(18f))
   lineTo(x(18f), y(15f))
   quadraticTo(x(21.5f), y(12f), x(26.5f), y(16.5f))
   lineTo(x(26f), y(38f))
   quadraticTo(x(29.5f), y(42f), x(34f), y(37f))
   lineTo(x(36.5f), y(16f))
   quadraticTo(x(33.5f), y(12f), x(33f), y(9.5f))
   quadraticTo(x(39f), y(8f), x(44f), y(15f))
   cubicTo(x(43.5f), y(28f), x(42f), y(40f), x(37f), y(45.5f))
   quadraticTo(x(31f), y(48f), x(24.5f), y(42f))
   quadraticTo(x(18.5f), y(48f), x(12f), y(46.5f))
   cubicTo(x(6.5f), y(43f), x(5.5f), y(32f), x(6f), y(19.5f))
   close()

   // i - dot
   moveTo(x(53.5f), y(9.5f))
   lineTo(x(57.5f), y(13.5f))
   lineTo(x(52.5f), y(18.5f))
   lineTo(x(49f), y(14.5f))
   close()

   // i - stem
   moveTo(x(48.5f), y(25f))
   quadraticTo(x(52f), y(22f), x(55.5f), y(21f))
   lineTo(x(55.5f), y(41f))
   lineTo(x(58.5f), y(42.5f))
   quadraticTo(x(54.5f), y(47f), x(51.5f), y(46.5f))
   quadraticTo(x(49.5f), y(44.5f), x(50f), y(41f))
   lineTo(x(50f), y(27f))
   close()

   // l
   moveTo(x(60.5f), y(13.5f))
   quadraticTo(x(64.5f), y(10f), x(68.5f), y(9f))
   lineTo(x(68.5f), y(41f))
   lineTo(x(73f), y(42.5f))
   quadraticTo(x(68.5f), y(47f), x(65f), y(46.5f))
   quadraticTo(x(62.5f), y(44.5f), x(63f), y(41f))
   lineTo(x(63f), y(16f))
   close()

   // d - outer
   moveTo(x(83.5f), y(11.5f))
   quadraticTo(x(87.5f), y(8.5f), x(92f), y(8.5f))
   lineTo(x(92f), y(41f))
   lineTo(x(96f), y(43f))
   quadraticTo(x(91.5f), y(47f), x(88.5f), y(46.5f))
   lineTo(x(86.5f), y(43.5f))
   quadraticTo(x(80.5f), y(47.5f), x(76f), y(43.5f))
   cubicTo(x(73f), y(39.5f), x(73.5f), y(29f), x(77.5f), y(24.5f))
   quadraticTo(x(81.5f), y(21.5f), x(86.5f), y(24f))
   lineTo(x(86.5f), y(14f))
   close()

   // d - counter
   moveTo(x(86.5f), y(28f))
   quadraticTo(x(81.5f), y(26.5f), x(79.5f), y(31.5f))
   cubicTo(x(78.5f), y(35.5f), x(79.5f), y(40.5f), x(83f), y(41.5f))
   quadraticTo(x(85.5f), y(41.5f), x(86.5f), y(39.5f))
   close()

   // P - outer
   moveTo(x(109.5f), y(6f))
   quadraticTo(x(112.5f), y(9.5f), x(115f), y(12f))
   cubicTo(x(124f), y(11.5f), x(130.5f), y(17f), x(130.5f), y(24.5f))
   cubicTo(x(130.5f), y(32f), x(124f), y(37f), x(115f), y(37.5f))
   lineTo(x(115f), y(44.5f))
   cubicTo(x(114.5f), y(51.5f), x(109f), y(56f), x(101.5f), y(57f))
   cubicTo(x(106.5f), y(53.5f), x(109f), y(49f), x(109f), y(43f))
   lineTo(x(109f), y(20f))
   lineTo(x(103.5f), y(17.5f))
   quadraticTo(x(107.5f), y(13.5f), x(109.5f), y(6f))
   close()

   // P - counter
   moveTo(x(115f), y(17.5f))
   cubicTo(x(121.5f), y(17.5f), x(124.5f), y(20.5f), x(124.5f), y(25f))
   cubicTo(x(124.5f), y(29.5f), x(121f), y(32.5f), x(115f), y(32.5f))
   close()

   // a - outer
   moveTo(x(139.5f), y(22f))
   quadraticTo(x(144.5f), y(21.5f), x(148.5f), y(24.5f))
   lineTo(x(148.5f), y(41f))
   lineTo(x(152f), y(43f))
   quadraticTo(x(148f), y(47f), x(145f), y(46.5f))
   lineTo(x(143f), y(43.5f))
   quadraticTo(x(137.5f), y(47.5f), x(133f), y(43.5f))
   cubicTo(x(130f), y(39.5f), x(130.5f), y(29f), x(134.5f), y(24.5f))
   quadraticTo(x(136.5f), y(22.5f), x(139.5f), y(22f))
   close()

   // a - counter
   moveTo(x(143f), y(28f))
   quadraticTo(x(138f), y(26.5f), x(136.5f), y(32f))
   cubicTo(x(135.5f), y(36f), x(136.5f), y(40.5f), x(139.5f), y(41.5f))
   quadraticTo(x(142f), y(41.5f), x(143f), y(39.5f))
   close()

   // r
   moveTo(x(153.5f), y(25.5f))
   quadraticTo(x(157.5f), y(22f), x(161f), y(21.5f))
   lineTo(x(161f), y(27f))
   quadraticTo(x(165f), y(21.5f), x(169.5f), y(21.5f))
   quadraticTo(x(172f), y(22.5f), x(171.5f), y(27f))
   quadraticTo(x(170.5f), y(29.5f), x(167.5f), y(29.5f))
   quadraticTo(x(164.5f), y(27.5f), x(161f), y(31.5f))
   lineTo(x(161f), y(42f))
   quadraticTo(x(158.5f), y(46.5f), x(155.5f), y(46.5f))
   lineTo(x(155.5f), y(28f))
   close()

   // t - stem
   moveTo(x(181.5f), y(13f))
   quadraticTo(x(183.5f), y(16f), x(183.5f), y(19.5f))
   lineTo(x(183.5f), y(40f))
   quadraticTo(x(185.5f), y(42f), x(190f), y(41f))
   quadraticTo(x(186f), y(47f), x(181.5f), y(47f))
   quadraticTo(x(177.5f), y(46f), x(178f), y(40.5f))
   lineTo(x(178f), y(19.5f))
   close()

   // t - crossbar
   moveTo(x(173f), y(26f))
   lineTo(x(190f), y(23f))
   lineTo(x(189.5f), y(27.5f))
   lineTo(x(173.5f), y(30f))
   close()

   // y
   moveTo(x(191.5f), y(25f))
   quadraticTo(x(195.5f), y(22f), x(199f), y(21.5f))
   lineTo(x(199.5f), y(39f))
   quadraticTo(x(202.5f), y(42f), x(208f), y(38f))
   lineTo(x(208f), y(24.5f))
   quadraticTo(x(211.5f), y(22f), x(214.5f), y(21.5f))
   lineTo(x(214.5f), y(41.5f))
   cubicTo(x(214.5f), y(50.5f), x(208f), y(56f), x(198.5f), y(57f))
   cubicTo(x(204.5f), y(53f), x(208f), y(49f), x(208f), y(43f))
   quadraticTo(x(202f), y(47.5f), x(197f), y(44.5f))
   quadraticTo(x(194f), y(42f), x(194f), y(37f))
   lineTo(x(194f), y(28f))
   close()
  }

  // Subtle shadow
  val shadowOffsetPx = 1.6.dp.toPx()
  withTransform({ translate(left = 0f, top = shadowOffsetPx) }) {
   drawPath(path = titlePath, color = Color.Black.copy(alpha = 0.48f))
  }
  // Main crisp white lettering
  drawPath(path = titlePath, color = Color.White)
 }
}

@Composable
private fun WpHistoryNewPill(modifier: Modifier = Modifier) {
 val shape = RoundedCornerShape(50)
 Box(
  modifier = modifier
   .wrapContentSize(unbounded = true)
   .shadow(elevation = 2.dp, shape = shape, clip = false)
   .clip(shape)
   .background(
    Brush.verticalGradient(
     colors = listOf(
      Color(0xFFFFF49A),
      Color(0xFFFFD028),
      Color(0xFFE69500),
      Color(0xFFB36300)
     )
    )
   )
   .border(
    width = 0.65.dp,
    brush = Brush.verticalGradient(
     colors = listOf(
      Color(0xFFFFFDE0),
      Color(0xFFFFD95A),
      Color(0xFF944C00)
     )
    ),
    shape = shape
   )
   .drawBehind {
    drawRoundRect(
     brush = Brush.verticalGradient(
      colors = listOf(
       Color.White.copy(alpha = 0.62f),
       Color.Transparent
      ),
      startY = 0f,
      endY = size.height * 0.48f
     ),
     topLeft = Offset(0.5.dp.toPx(), 0.4.dp.toPx()),
     size = Size(size.width - 1.dp.toPx(), size.height * 0.45f),
     cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
    )
   }
   .padding(horizontal = 3.5.dp, vertical = 0.5.dp),
  contentAlignment = Alignment.Center
 ) {
  Text(
   text = "New",
   color = Color(0xFF4A1D00),
   fontSize = 6.5.sp,
   lineHeight = 7.sp,
   fontWeight = FontWeight.Black,
   maxLines = 1,
   softWrap = false,
   style = TextStyle(shadow = Shadow(Color(0xFFFFF7B8), Offset(0f, 0.5f), 0.6f))
  )
 }
}

@Composable
private fun WpWinnerAvatarPlaceholder(modifier: Modifier = Modifier) {
 Canvas(
  modifier = modifier
   .clip(CircleShape)
   .background(Color(0xFF16171C))
 ) {
  val iconColor = Color(0xFF70747D)
  val cx = size.width / 2f
  drawCircle(
   color = iconColor,
   radius = size.minDimension * 0.17f,
   center = Offset(cx, size.height * 0.36f)
  )
  drawOval(
   color = iconColor,
   topLeft = Offset(size.width * 0.21f, size.height * 0.57f),
   size = Size(size.width * 0.58f, size.height * 0.36f)
  )
 }
}

@Composable
private fun WpLoadingPage(
 progress: Float,
 modifier: Modifier = Modifier
) {
 val context = LocalContext.current
 val bgRes = wpAsset( "file_00000000db188211a99b006efa3949e4")
 val logoRes = wpAsset( "file_000000009d808211b8ffb7c2183b4ef5")
 val clampedProgress = progress.coerceIn(0f, 1f)
 val barShape = RoundedCornerShape(50)

 Box(
  modifier = modifier
   .fillMaxSize()
   .background(Color(0xFF1A120B))
   .clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    onClick = {}
   )
 ) {
  if (bgRes.isNotBlank()) {
   AsyncImage(
    model = bgRes,
    contentDescription = null,
    modifier = Modifier.fillMaxSize(),
    contentScale = ContentScale.Crop
   )
  }

  // Background image ko thoda sa dim karne ke liye subtle overlay
  Box(
   modifier = Modifier
    .fillMaxSize()
    .background(Color.Black.copy(alpha = 0.24f))
  )

  // Top Title: Wild Party (chota & image jaisa same style)
  WpWildPartyTitle(
   height = 22.dp,
   modifier = Modifier
    .align(Alignment.TopCenter)
    .padding(top = 12.dp)
  )

  // Center Game Logo
  if (logoRes.isNotBlank()) {
   AsyncImage(
    model = logoRes,
    contentDescription = "Wild Party Logo",
    modifier = Modifier
     .align(Alignment.Center)
     .offset(y = (-14).dp)
     .size(126.dp),
    contentScale = ContentScale.Fit
   )
  }

  // Bottom Golden-Bordered Glossy Blue Striped Loading Bar
  Box(
   modifier = Modifier
    .align(Alignment.BottomCenter)
    .fillMaxWidth()
    .padding(start = 22.dp, end = 22.dp, bottom = 26.dp)
    .height(25.dp)
    .shadow(elevation = 6.dp, shape = barShape, clip = false)
    .clip(barShape)
    .background(
     Brush.verticalGradient(
      colors = listOf(
       Color(0xFF562C05),
       Color(0xFF7E460A),
       Color(0xFF693706)
      )
     )
    )
    .border(
     width = 2.8.dp,
     brush = Brush.verticalGradient(
      colors = listOf(
       Color(0xFFFFF8B8),
       Color(0xFFFFDA3E),
       Color(0xFFE09200),
       Color(0xFFFFE56D)
      )
     ),
     shape = barShape
    )
    .padding(2.8.dp),
   contentAlignment = Alignment.Center
  ) {
   Box(
    modifier = Modifier
     .fillMaxSize()
     .clip(barShape),
    contentAlignment = Alignment.Center
   ) {
    Canvas(Modifier.fillMaxSize()) {
     // Subtle inner top shadow on dark brown track
     drawRect(
      brush = Brush.verticalGradient(
       colors = listOf(
        Color.Black.copy(alpha = 0.36f),
        Color.Transparent
       ),
       startY = 0f,
       endY = size.height * 0.42f
      )
     )

     val fillWidth = size.width * clampedProgress
     if (fillWidth > 0f) {
      clipRect(left = 0f, top = 0f, right = fillWidth, bottom = size.height) {
       // Glossy Blue Base Fill (Green ki jagah Blue)
       drawRect(
        brush = Brush.verticalGradient(
         colors = listOf(
          Color(0xFF8BE8FF),
          Color(0xFF21A6FF),
          Color(0xFF006EE6),
          Color(0xFF0047AB)
         )
        ),
        size = size
       )

       // Diagonal Glossy Stripes (same pattern as reference image)
       val stripeWidth = 14.dp.toPx()
       val stripeGap = 15.dp.toPx()
       val step = stripeWidth + stripeGap
       val slant = size.height * 0.85f
       var x = -slant
       while (x < size.width + slant) {
        val stripePath = Path().apply {
         moveTo(x, size.height)
         lineTo(x + slant, 0f)
         lineTo(x + slant + stripeWidth, 0f)
         lineTo(x + stripeWidth, size.height)
         close()
        }
        drawPath(
         path = stripePath,
         brush = Brush.verticalGradient(
          colors = listOf(
           Color.White.copy(alpha = 0.52f),
           Color(0xFFB3F0FF).copy(alpha = 0.28f),
           Color(0xFF66D1FF).copy(alpha = 0.16f)
          )
         )
        )
        x += step
       }

       // Top Glossy Shine Strip
       drawRect(
        brush = Brush.verticalGradient(
         colors = listOf(
          Color.White.copy(alpha = 0.50f),
          Color.White.copy(alpha = 0.12f),
          Color.Transparent
         ),
         startY = 0f,
         endY = size.height * 0.50f
        ),
        size = Size(size.width, size.height * 0.50f)
       )
      }
     }
    }

    Text(
     text = "${(clampedProgress * 100).toInt()}%",
     color = Color.White,
     fontSize = 12.5.sp,
     fontWeight = FontWeight.ExtraBold,
     style = TextStyle(
      shadow = Shadow(
       color = Color.Black.copy(alpha = 0.75f),
       offset = Offset(0f, 1.2f),
       blurRadius = 2.5f
      )
     )
    )
   }
  }
 }
}

@Composable
private fun WpBetBadge(
 amountText: String,
 coinResId: String,
 modifier: Modifier = Modifier
) {
 val shape = RoundedCornerShape(4.5.dp)
 Box(
  modifier = modifier
   .wrapContentWidth(unbounded = true)
   .shadow(elevation = 3.dp, shape = shape, clip = false)
   .clip(shape)
   .background(
    Brush.verticalGradient(
     colors = listOf(
      Color(0xFFFFF4BD),
      Color(0xFFFFE192),
      Color(0xFFFAC670),
      Color(0xFFF2AA56)
     )
    )
   )
   .border(
    width = 1.dp,
    brush = Brush.verticalGradient(
     colors = listOf(
      Color(0xFFFFFCE0),
      Color(0xFFFFE7A3),
      Color(0xFFE0953F)
     )
    ),
    shape = shape
   )
   .drawBehind {
    drawRoundRect(
     brush = Brush.verticalGradient(
      colors = listOf(
       Color.White.copy(alpha = 0.55f),
       Color.Transparent
      ),
      startY = 0f,
      endY = size.height * 0.45f
     ),
     topLeft = Offset(1.dp.toPx(), 0.8.dp.toPx()),
     size = Size(size.width - 2.dp.toPx(), size.height * 0.42f),
     cornerRadius = CornerRadius(3.5.dp.toPx(), 3.5.dp.toPx())
    )
   }
   .padding(horizontal = 7.5.dp, vertical = 0.5.dp),
  contentAlignment = Alignment.Center
 ) {
  Row(
   verticalAlignment = Alignment.CenterVertically,
   horizontalArrangement = Arrangement.spacedBy(2.5.dp)
  ) {
   Text(
    text = "You:",
    color = Color(0xFFB5440A),
    fontSize = 8.5.sp,
    lineHeight = 9.5.sp,
    fontWeight = FontWeight.ExtraBold,
    fontFamily = FontFamily.Serif,
    maxLines = 1,
    softWrap = false,
    style = TextStyle(shadow = Shadow(Color(0xFFFFF6CC), Offset(0f, 0.8f), 1f))
   )
   if (coinResId.isNotBlank()) {
    AsyncImage(
     model = coinResId,
     contentDescription = null,
     modifier = Modifier.size(10.dp),
     contentScale = ContentScale.Fit
    )
   }
   Text(
    text = amountText,
    color = Color(0xFFBC3E05),
    fontSize = 9.sp,
    lineHeight = 9.5.sp,
    fontWeight = FontWeight.Black,
    maxLines = 1,
    softWrap = false,
    style = TextStyle(shadow = Shadow(Color(0xFFFFF4C2), Offset(0f, 0.8f), 1f))
   )
  }
 }
}

@Composable
private fun WpGlassTopButton(
 onClick: () -> Unit,
 modifier: Modifier = Modifier,
 content: @Composable BoxScope.() -> Unit
) {
 val shape = RoundedCornerShape(10.dp)
 Box(
  modifier = modifier
   .size(28.dp)
   .clip(shape)
   .background(
    Brush.verticalGradient(
     colors = listOf(
      Color.White.copy(alpha = 0.42f),
      Color(0xFFB8C4DC).copy(alpha = 0.16f),
      Color(0xFF1F2640).copy(alpha = 0.24f),
      Color.White.copy(alpha = 0.26f)
     )
    )
   )
   .border(
    width = 1.25.dp,
    brush = Brush.verticalGradient(
     colors = listOf(
      Color.White.copy(alpha = 0.92f),
      Color.White.copy(alpha = 0.32f),
      Color.White.copy(alpha = 0.58f)
     )
    ),
    shape = shape
   )
   .clickable(onClick = onClick),
  contentAlignment = Alignment.Center
 ) {
  Canvas(Modifier.fillMaxSize()) {
   drawOval(
    brush = Brush.verticalGradient(
     colors = listOf(
      Color.White.copy(alpha = 0.72f),
      Color.White.copy(alpha = 0.18f),
      Color.Transparent
     ),
     startY = 0f,
     endY = size.height * 0.52f
    ),
    topLeft = Offset(size.width * 0.10f, size.height * 0.04f),
    size = Size(size.width * 0.80f, size.height * 0.44f)
   )
   drawOval(
    brush = Brush.verticalGradient(
     colors = listOf(
      Color.Transparent,
      Color.White.copy(alpha = 0.24f)
     ),
     startY = size.height * 0.72f,
     endY = size.height
    ),
    topLeft = Offset(size.width * 0.16f, size.height * 0.74f),
    size = Size(size.width * 0.68f, size.height * 0.24f)
   )
  }
  content()
 }
}

@Composable
private fun WpGlassBackButton(
 onClick: () -> Unit,
 modifier: Modifier = Modifier
) {
 val shape = RoundedCornerShape(10.dp)
 Box(
  modifier = modifier
   .size(30.dp)
   .clip(shape)
   .background(
    Brush.verticalGradient(
     colors = listOf(
      Color.White.copy(alpha = 0.36f),
      Color(0xFF424652).copy(alpha = 0.38f),
      Color(0xFF14151A).copy(alpha = 0.85f),
      Color.White.copy(alpha = 0.18f)
     )
    )
   )
   .border(
    width = 1.dp,
    brush = Brush.verticalGradient(
     colors = listOf(
      Color.White.copy(alpha = 0.85f),
      Color.White.copy(alpha = 0.25f),
      Color.White.copy(alpha = 0.45f)
     )
    ),
    shape = shape
   )
   .clickable(onClick = onClick),
  contentAlignment = Alignment.Center
 ) {
  Canvas(Modifier.fillMaxSize()) {
   drawOval(
    brush = Brush.verticalGradient(
     colors = listOf(
      Color.White.copy(alpha = 0.58f),
      Color.White.copy(alpha = 0.10f),
      Color.Transparent
     ),
     startY = 0f,
     endY = size.height * 0.50f
    ),
    topLeft = Offset(size.width * 0.12f, size.height * 0.04f),
    size = Size(size.width * 0.76f, size.height * 0.42f)
   )
  }
  Canvas(Modifier.size(13.dp)) {
   val chevron = Path().apply {
    moveTo(8.5.dp.toPx(), 2.2.dp.toPx())
    lineTo(3.8.dp.toPx(), size.height / 2f)
    lineTo(8.5.dp.toPx(), size.height - 2.2.dp.toPx())
   }
   drawPath(
    chevron,
    Color.White,
    style = Stroke(2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
   )
  }
 }
}

@Composable
private fun WpBottomBalanceBar(
 coinResId: String,
 balance: Long,
 modifier: Modifier = Modifier
) {
 val pillShape = RoundedCornerShape(7.dp)
 val goldColor = Color(0xFFFFD84D)
 Row(
  modifier = modifier,
  verticalAlignment = Alignment.CenterVertically,
  horizontalArrangement = Arrangement.spacedBy(5.dp)
 ) {
  Box(
   modifier = Modifier
    .clip(pillShape)
    .background(
     Brush.verticalGradient(
      colors = listOf(
       Color.White.copy(alpha = 0.58f),
       Color(0xFFD5BFA6).copy(alpha = 0.34f),
       Color(0xFF5A3418).copy(alpha = 0.50f),
       Color(0xFF3E200C).copy(alpha = 0.55f)
      )
     )
    )
    .border(
     width = 1.dp,
     brush = Brush.verticalGradient(
      colors = listOf(
       Color.White.copy(alpha = 0.90f),
       Color.White.copy(alpha = 0.38f),
       Color(0xFFE6C8A0).copy(alpha = 0.45f)
      )
     ),
     shape = pillShape
    )
    .drawBehind {
     drawRect(
      brush = Brush.verticalGradient(
       colors = listOf(
        Color.White.copy(alpha = 0.42f),
        Color.Transparent
       ),
       startY = 0f,
       endY = size.height * 0.48f
      )
     )
    }
    .padding(horizontal = 4.dp, vertical = 3.dp),
   contentAlignment = Alignment.Center
  ) {
   Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(3.dp)
   ) {
    if (coinResId.isNotBlank()) {
     AsyncImage(
      model = coinResId,
      contentDescription = null,
      modifier = Modifier.size(18.dp),
      contentScale = ContentScale.Fit
     )
    }
    Canvas(Modifier.size(width = 8.dp, height = 7.dp)) {
     val tri = Path().apply {
      moveTo(size.width / 2f, 0.8.dp.toPx())
      lineTo(size.width - 0.8.dp.toPx(), size.height - 0.8.dp.toPx())
      lineTo(0.8.dp.toPx(), size.height - 0.8.dp.toPx())
      close()
     }
     drawPath(tri, goldColor, style = Fill)
     drawPath(tri, goldColor, style = Stroke(1.3.dp.toPx(), join = StrokeJoin.Round))
    }
   }
  }
  Row(
   verticalAlignment = Alignment.CenterVertically,
   horizontalArrangement = Arrangement.spacedBy(3.dp)
  ) {
   Text(
    if (balance >= 100_000_000L) wpFormat(balance) else balance.toString(),
    color = goldColor,
    fontSize = 13.sp,
    fontWeight = FontWeight.Black,
    style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.65f), Offset(0f, 1.5f), 3f))
   )
   Canvas(Modifier.size(width = 7.dp, height = 11.dp)) {
    val chevron = Path().apply {
     moveTo(1.5.dp.toPx(), 1.5.dp.toPx())
     lineTo(size.width - 1.5.dp.toPx(), size.height / 2f)
     lineTo(1.5.dp.toPx(), size.height - 1.5.dp.toPx())
    }
    drawPath(
     chevron,
     Color.Black.copy(alpha = 0.45f),
     style = Stroke(2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    drawPath(
     chevron,
     goldColor,
     style = Stroke(2.0.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
   }
  }
 }
}

@Composable
private fun WpGlossyRepeatButton(
 onClick: () -> Unit,
 modifier: Modifier = Modifier
) {
 val outerShape = RoundedCornerShape(9.dp)
 val innerShape = RoundedCornerShape(7.dp)
 Box(
  modifier = modifier
   .clip(outerShape)
   .background(Color(0xFF472710).copy(alpha = 0.72f))
   .border(
    width = 1.2.dp,
    brush = Brush.verticalGradient(
     colors = listOf(
      Color.White.copy(alpha = 0.94f),
      Color(0xFFEBDCD0).copy(alpha = 0.75f),
      Color.White.copy(alpha = 0.85f)
     )
    ),
    shape = outerShape
   )
   .clickable(onClick = onClick)
   .padding(2.dp),
  contentAlignment = Alignment.Center
 ) {
  Box(
   modifier = Modifier
    .clip(innerShape)
    .background(
     Brush.verticalGradient(
      colors = listOf(
       Color.White.copy(alpha = 0.52f),
       Color(0xFFCBB5A0).copy(alpha = 0.30f),
       Color(0xFF563218).copy(alpha = 0.68f),
       Color(0xFF6B4122).copy(alpha = 0.58f)
      )
     )
    )
    .border(
     width = 0.8.dp,
     brush = Brush.verticalGradient(
      colors = listOf(
       Color.White.copy(alpha = 0.85f),
       Color.White.copy(alpha = 0.28f),
       Color.White.copy(alpha = 0.52f)
      )
     ),
     shape = innerShape
    )
    .drawBehind {
     drawRoundRect(
      brush = Brush.verticalGradient(
       colors = listOf(
        Color.White.copy(alpha = 0.45f),
        Color.White.copy(alpha = 0.10f),
        Color.Transparent
       ),
       startY = 0f,
       endY = size.height * 0.52f
      ),
      topLeft = Offset(1.dp.toPx(), 0.5.dp.toPx()),
      size = Size(size.width - 2.dp.toPx(), size.height * 0.48f),
      cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
     )
    }
    .padding(horizontal = 7.dp, vertical = 4.dp),
   contentAlignment = Alignment.Center
  ) {
   Text(
    "Repeat",
    color = Color.White,
    fontSize = 10.sp,
    fontWeight = FontWeight.Black,
    style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.55f), Offset(0f, 1.5f), 2.5f))
   )
  }
 }
}

@Composable
fun Wildparty(onClose:()->Unit={},onMinimize:()->Unit={},modifier:Modifier=Modifier){
 val context=LocalContext.current
 val scope= rememberCoroutineScope()
 val dao= remember{WalletDatabase.get(context).walletDao()}
 val soundManager = remember(context) { WpSoundManager(context.applicationContext) }
 var balance by remember{mutableLongStateOf(82927L)}
 var phase by remember{mutableStateOf(WpPhase.BETTING)}
 var round by remember{mutableIntStateOf(1)}
 var countdown by remember{mutableIntStateOf(30)}
 var highlight by remember{mutableIntStateOf(-1)}
 var winnerIndex by remember{mutableIntStateOf(0)}
 var mode by remember{mutableStateOf(WpMode.SINGLE)}
 var chipIndex by remember{mutableIntStateOf(1)}
 var bets by remember{mutableStateOf>(emptyMap())}
 var lastBets by remember{mutableStateOf>(emptyMap())}
 var history by remember{mutableStateOf<List<WpRound>>(emptyList())}
 var historyOpen by remember{mutableStateOf(false)}
 var rulesOpen by remember{mutableStateOf(false)}
 var winnerOpen by remember{mutableStateOf(false)}
 var muted by remember{mutableStateOf(false)}
 var winAmount by remember{mutableLongStateOf(0L)}
 var betAmount by remember{mutableLongStateOf(0L)}
 var winnerTimer by remember{mutableIntStateOf(5)}
 var loading by remember{mutableStateOf(true)}
 var loadingProgress by remember{mutableFloatStateOf(0f)}
 var processedRound by remember{mutableIntStateOf(-1)}

 // Flying coins + bottom-left popup states
 val bottomLeftPopup = remember { Animatable(0f) }
 var gameContainerRootPos by remember { mutableStateOf(Offset.Zero) }
 var bottomLeftRootCenter by remember { mutableStateOf(Offset.Zero) }
 val animalRootCenters = remember { mutableStateMapOf<String, Offset>() }
 var flyingCoins by remember { mutableStateOf<List<WpFlyingCoin>>(emptyList()) }
 var coinIdSeq by remember { mutableLongStateOf(0L) }

 val topHistoryScrollState = rememberScrollState()
 val grayColorFilter = remember {
  ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
 }

 DisposableEffect(soundManager) {
  onDispose { soundManager.release() }
 }

 LaunchedEffect(muted) {
  withContext(Dispatchers.IO) {
   soundManager.setMuted(muted)
  }
 }

 LaunchedEffect(Unit){
  launch(Dispatchers.IO) { soundManager.preload() }
  runCatching{
   balance=dao.getUserData()?.balance?:82927L
   val arr=JSONArray(context.getSharedPreferences("wild_party_native",Context.MODE_PRIVATE).getString("history","[]"))
   history=(0 until arr.length()).mapNotNull{i->arr.optJSONObject(i)?.let{WpRound(it.optInt("round"),it.optString("winner"),it.optLong("time"),it.optLong("myBet",0L),it.optLong("winAmount",0L))}}.filter{it.time>=wpReset()}.sortedBy{it.round}.takeLast(50)
  }
  for (step in 1..50) {
   loadingProgress = step / 50f
   delay(24)
  }
  delay(120)
  loading=false
 }
 LaunchedEffect(history.size){
  topHistoryScrollState.scrollTo(0)
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
  history.takeLast(50).forEach{arr.put(JSONObject().put("round",it.round).put("winner",it.winner).put("time",it.time).put("myBet",it.myBet).put("winAmount",it.winAmount))}
  context.getSharedPreferences("wild_party_native",Context.MODE_PRIVATE).edit().putString("history",arr.toString()).apply()
 }
 fun triggerBetVisuals(targetAnimalName: String, amount: Long) {
  scope.launch {
   bottomLeftPopup.snapTo(0f)
   bottomLeftPopup.animateTo(1f, animationSpec = tween(85, easing = FastOutSlowInEasing))
   bottomLeftPopup.animateTo(0f, animationSpec = tween(110, easing = FastOutSlowInEasing))
  }
  val targetRoot = animalRootCenters[targetAnimalName]
  if (targetRoot != null && bottomLeftRootCenter != Offset.Zero) {
   val start = bottomLeftRootCenter - gameContainerRootPos
   val end = targetRoot - gameContainerRootPos
   val coinCount = when {
    amount >= 5_000_000L -> 6
    amount >= 500_000L -> 5
    amount >= 50_000L -> 4
    amount >= 10_000L -> 3
    else -> 2
   }
   val spawned = List(coinCount) { idx ->
    coinIdSeq++
    val spread = (idx - (coinCount - 1) / 2f) * 14f
    WpFlyingCoin(
     id = coinIdSeq,
     start = start,
     end = end,
     delayMs = idx * 42L,
     curveOffsetX = spread,
     curveOffsetY = (idx % 2) * -10f
    )
   }
   flyingCoins = flyingCoins + spawned
  }
 }
 LaunchedEffect(loading){
  if(loading)return@LaunchedEffect
  var lastSpinStep = -1
  while(true){
   val now=System.currentTimeMillis()
   val elapsedMs=(now-wpReset(now)).coerceAtLeast(0L)
   val elapsed=elapsedMs/1000L
   val cycleMs=elapsedMs%47_000L
   val cycle=(cycleMs/1000L).toInt()
   round=(elapsed/47L).toInt()+1
   val outcome=wpOutcome(round);mode=outcome.first;winnerIndex=outcome.second
   when{
    cycle<30->{
     phase=WpPhase.BETTING;countdown=30-cycle;highlight=-1;winnerOpen=false
     lastSpinStep = -1
    }
    cycle<40->{
     phase=WpPhase.SPINNING
     countdown=40-cycle
     val p=((cycleMs-30_000L).coerceIn(0L,10_000L))/10_000.0
     val totalSteps=48+winnerIndex
     val eased=1.0-(1.0-p).pow(2.2)
     val step=floor(eased*(totalSteps+0.5)).toInt().coerceAtMost(totalSteps)
     val nextHighlight=step%8
     if (step != lastSpinStep) {
      lastSpinStep = step
      val spinSpeed = (1.0f + (1.0 - p).pow(1.2).toFloat() * 0.85f).coerceIn(1.0f, 1.85f)
      launch(Dispatchers.IO) { soundManager.playSpinTick(spinSpeed) }
     }
     highlight=nextHighlight
    }
    else->{
     phase=WpPhase.RESULT;countdown=47-cycle;highlight=winnerIndex
     lastSpinStep = -1
    }
   }
   delay(if(cycle in 30..39) 16L else 100L)
  }
 }
 LaunchedEffect(phase,round,loading){
  if(loading)return@LaunchedEffect
  if(phase==WpPhase.BETTING){if(bets.isNotEmpty()){lastBets=bets;bets=emptyMap()}}
  if(phase==WpPhase.RESULT && processedRound!=round){
   processedRound=round
   val result=wpOutcome(round);val animal=wpAnimals[result.second]
   val label=when(result.first){WpMode.MIX_BIG->"Mix Big";WpMode.MIX_SMALL->"Mix Small";else->animal.name}
   betAmount=bets.values.sum()
   winAmount=when(result.first){
    WpMode.MIX_BIG->listOf("Eagle","Bear","Tiger","Lion").sumOf{n->(bets[n]?:0L)*wpAnimals.first{it.name==n}.multiplier}
    WpMode.MIX_SMALL->listOf("Rabbit","Deer","Dog","Camel").sumOf{n->(bets[n]?:0L)*5L}
    WpMode.SINGLE->(bets[animal.name]?:0L)*animal.multiplier
   }
   history=(history+WpRound(round,label,System.currentTimeMillis(),betAmount,winAmount)).takeLast(50);persistHistory()
   delay(1500);if(winAmount>0)walletChange(winAmount,"Won in Wild Party")
   winnerTimer=5;winnerOpen=true
  }
 }
 LaunchedEffect(winnerOpen){
  if(winnerOpen){
   launch(Dispatchers.IO) { soundManager.playWinner() }
   repeat(5){delay(1000);winnerTimer=4-it}
   winnerOpen=false
  } else {
   launch(Dispatchers.IO) { soundManager.stopWinner() }
  }
 }
 fun place(name:String){
  scope.launch{
   val amount=wpChips[chipIndex].value
   if(phase==WpPhase.BETTING&&balance>=amount){
    launch(Dispatchers.IO) { soundManager.playBetClick() }
    triggerBetVisuals(name, amount)
    walletChange(-amount,"Bet on $name in Wild Party")
    bets=bets.toMutableMap().also{it[name]=(it[name]?:0L)+amount}
   }
  }
 }
 BoxWithConstraints(
  modifier
   .fillMaxSize()
   .zIndex(100f)
   .background(Color.Transparent),
  contentAlignment = Alignment.BottomCenter
 ){
  val h=maxHeight*.65f
  val multiplierBg=wpAsset("wp_file_00000000330c8211b80be136b631b3e0")
  val coin=wpAsset("wp_file_00000000e56882119c217d508b6733dc")
  // Full-screen dim backdrop blocks touches from reaching the Room page behind the game
  Box(
   Modifier
    .fillMaxSize()
    .background(Color.Black.copy(alpha = .5f))
    .clickable(
     interactionSource = remember { MutableInteractionSource() },
     indication = null,
     onClick = {}
    )
  )
  Box(
   Modifier
    .fillMaxWidth()
    .height(h)
    .background(Color(0xFF28180D))
    .clickable(
     interactionSource = remember { MutableInteractionSource() },
     indication = null,
     onClick = {}
    )
    .onGloballyPositioned { coords ->
     gameContainerRootPos = coords.positionInRoot()
    }
  ){
   val bg=wpAsset("wp_1787337855180_2")
   if (bg.isNotBlank())AsyncImage(
    model = bg, contentDescription = null, modifier = Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
   // Original TSX top multiplier badges (same artwork and placement).
   @Composable fun MultiplierBadge(big:Boolean){
    Box(Modifier.size(65.dp),contentAlignment=Alignment.Center){
     if (multiplierBg.isNotBlank())AsyncImage(
      model = multiplierBg, contentDescription = null, modifier = Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
     val names=if(big) listOf("Eagle","Bear","Tiger","Lion") else listOf("Dog","Camel","Rabbit","Deer")
     val positions=if(big) listOf(
      Triple((-11).dp,(-12).dp,-10f),
      Triple(9.dp,(-12).dp,10f),
      Triple((-14).dp,12.dp,-15f),
      Triple(14.dp,12.dp,10f)
     ) else listOf(
      Triple((-9).dp,(-8).dp,-10f),
      Triple(9.dp,(-8).dp,10f),
      Triple((-14).dp,12.dp,-15f),
      Triple(14.dp,12.dp,10f)
     )
     names.forEachIndexed{index,name->
      val id=wpAsset(wpAnimals.first{it.name==name}.image)
      val (x,y,rotation)=positions[index]
      if (id.isNotBlank())AsyncImage(
       model = id,
       contentDescription = name,
       modifier = Modifier
        .offset(x,y)
        .size(if(index<2)32.dp else 34.dp)
        .zIndex(if(name=="Eagle" || name=="Rabbit" || name=="Deer") 5f else 0f)
        .graphicsLayer{rotationZ=rotation},
       contentScale=if(name=="Rabbit") ContentScale.Fit else ContentScale.Crop
      )
     }
    }
   }
   Box(Modifier.fillMaxWidth().padding(top=88.dp)){
    Box(Modifier.align(Alignment.TopStart).padding(start=4.dp)){MultiplierBadge(true)}
    Box(Modifier.align(Alignment.TopEnd).padding(end=4.dp)){MultiplierBadge(false)}
   }
   Box(Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top=8.dp)){
    Row(Modifier.align(Alignment.CenterStart).padding(start=8.dp),horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically){
     WpGlassTopButton(onClick = { muted = !muted }) {
      Canvas(Modifier.size(15.dp)) {
       val p = Path().apply {
        moveTo(1.5.dp.toPx(), 5.dp.toPx())
        lineTo(5.dp.toPx(), 5.dp.toPx())
        lineTo(9.5.dp.toPx(), 1.8.dp.toPx())
        lineTo(9.5.dp.toPx(), 13.2.dp.toPx())
        lineTo(5.dp.toPx(), 10.dp.toPx())
        lineTo(1.5.dp.toPx(), 10.dp.toPx())
        close()
       }
       drawPath(p, Color.White.copy(alpha = if (muted) 0.55f else 1f), style = Fill)
       drawPath(p, Color.White.copy(alpha = if (muted) 0.55f else 1f), style = Stroke(1.2.dp.toPx(), join = StrokeJoin.Round))
       if (muted) {
        drawLine(
         Color.White,
         Offset(2.dp.toPx(), 2.dp.toPx()),
         Offset(13.dp.toPx(), 13.dp.toPx()),
         2.2.dp.toPx(),
         cap = StrokeCap.Round
        )
       } else {
        drawArc(
         Color.White,
         -52f,
         104f,
         false,
         topLeft = Offset(7.2.dp.toPx(), 2.5.dp.toPx()),
         size = Size(7.2.dp.toPx(), 10.dp.toPx()),
         style = Stroke(1.9.dp.toPx(), cap = StrokeCap.Round)
        )
       }
      }
     }
     WpGlassTopButton(onClick = { rulesOpen = true }) {
      Text(
       "?",
       color = Color.White,
       fontSize = 15.sp,
       fontWeight = FontWeight.Black,
       style = TextStyle(shadow = Shadow(Color.White.copy(alpha = 0.55f), Offset.Zero, 6f))
      )
     }
    }
    WpWildPartyTitle(
     height = 22.dp,
     modifier = Modifier.align(Alignment.Center)
    )
    Row(Modifier.align(Alignment.CenterEnd).padding(end=8.dp),horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically){
     WpGlassTopButton(onClick = { historyOpen = true }) {
      Canvas(Modifier.size(14.dp)) {
       val hBar = 2.4.dp.toPx()
       val r = CornerRadius(hBar / 2f, hBar / 2f)
       drawRoundRect(Color.White, Offset(0.5.dp.toPx(), 2.dp.toPx()), Size(13.dp.toPx(), hBar), r)
       drawRoundRect(Color.White, Offset(0.5.dp.toPx(), 6.dp.toPx()), Size(13.dp.toPx(), hBar), r)
       drawRoundRect(Color.White, Offset(0.5.dp.toPx(), 10.dp.toPx()), Size(8.5.dp.toPx(), hBar), r)
       drawRoundRect(Color.White, Offset(10.5.dp.toPx(), 10.dp.toPx()), Size(3.dp.toPx(), hBar), r)
      }
     }
     WpGlassTopButton(onClick = { onMinimize() }) {
      Canvas(Modifier.size(14.dp)) {
       val tri = Path().apply {
        moveTo(2.2.dp.toPx(), 4.2.dp.toPx())
        lineTo(11.8.dp.toPx(), 4.2.dp.toPx())
        lineTo(7.dp.toPx(), 10.8.dp.toPx())
        close()
       }
       drawPath(tri, Color.White.copy(alpha = 0.35f), style = Stroke(3.6.dp.toPx(), join = StrokeJoin.Round))
       drawPath(
        tri,
        Brush.verticalGradient(listOf(Color(0xFFDDE3EE), Color.White)),
        style = Fill
       )
       drawPath(
        tri,
        Brush.verticalGradient(listOf(Color(0xFFDDE3EE), Color.White)),
        style = Stroke(2.2.dp.toPx(), join = StrokeJoin.Round)
       )
      }
     }
     WpGlassTopButton(onClick = { onClose() }) {
      Canvas(Modifier.size(14.dp)) {
       val a = 3.1.dp.toPx()
       val b = 10.9.dp.toPx()
       drawLine(Color.White.copy(alpha = 0.35f), Offset(a, a), Offset(b, b), 4.6.dp.toPx(), cap = StrokeCap.Round)
       drawLine(Color.White.copy(alpha = 0.35f), Offset(b, a), Offset(a, b), 4.6.dp.toPx(), cap = StrokeCap.Round)
       drawLine(Color.White, Offset(a, a), Offset(b, b), 3.0.dp.toPx(), cap = StrokeCap.Round)
       drawLine(Color.White, Offset(b, a), Offset(a, b), 3.0.dp.toPx(), cap = StrokeCap.Round)
      }
     }
    }
   }
   Row(
    Modifier
     .fillMaxWidth()
     .padding(top=44.dp,start=8.dp,end=8.dp)
     .heightIn(min=28.dp)
     .clip(RoundedCornerShape(50))
     .background(Color.Black.copy(alpha=.25f))
     .border(1.dp,Color.White.copy(alpha=.20f),RoundedCornerShape(50))
     .horizontalScroll(topHistoryScrollState)
     .padding(horizontal=10.dp,vertical=3.dp),
    horizontalArrangement=Arrangement.spacedBy(6.dp),
    verticalAlignment=Alignment.CenterVertically
   ){
    if(history.isEmpty())Text("History",color=Color.White.copy(alpha=.5f),fontSize=10.sp,fontStyle=FontStyle.Italic)
    else {
     val topHistory = history.takeLast(20).reversed()
     topHistory.forEachIndexed{recordIdx,record->
      val isNewest = recordIdx == 0
      val mixBig=record.winner=="Mix Big";val mixSmall=record.winner=="Mix Small"
      Box(Modifier.size(24.dp),contentAlignment=Alignment.Center){
       if(mixBig||mixSmall){
        val group=if(mixBig)listOf("Eagle","Bear","Tiger","Lion") else listOf("Rabbit","Deer","Dog","Camel")
        group.forEachIndexed{idx,n->val aid=wpAsset(wpAnimals.first{it.name==n}.image);if (aid.isNotBlank())AsyncImage(
         model = aid, contentDescription = null, modifier = Modifier.align(when(idx){0->Alignment.TopStart;1->Alignment.TopEnd;2->Alignment.BottomStart;else->Alignment.BottomEnd}).size(12.dp),contentScale=ContentScale.Fit)}
       }else{val aid=wpAsset(wpAnimals.firstOrNull{it.name==record.winner}?.image?: "");if (aid.isNotBlank())AsyncImage(
        model = aid, contentDescription = record.winner, modifier = Modifier.fillMaxSize(),contentScale=ContentScale.Fit)}
       if(isNewest){
        WpHistoryNewPill(modifier = Modifier.align(Alignment.BottomCenter).offset(y = 1.dp))
       }
      }
     }
    }
   }
   Box(Modifier.align(Alignment.Center).offset(y = 19.dp).size(320.dp),contentAlignment=Alignment.Center){
    // Center background image (public/IMG_20261121_102855.png)
    val centerBg=wpAsset("img_20261121_102855")
    if (centerBg.isNotBlank())AsyncImage(
     model = centerBg, contentDescription = null, modifier = Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
    wpAnimals.forEachIndexed{i,a->
     val rad=Math.toRadians(a.angle.toDouble());val x=(cos(rad)*130).dp+a.dx.dp;val y=(sin(rad)*130).dp+a.dy.dp-5.dp
     val active=highlight==i||(phase==WpPhase.RESULT&&((mode==WpMode.MIX_BIG&&i>=4)||(mode==WpMode.MIX_SMALL&&i<4)))
     val isColorless = phase != WpPhase.BETTING && !active
     val image=wpAsset(a.image)
     val amount=bets[a.name]?:0L
     Box(
      Modifier
       .offset(x,y)
       .size(a.size.dp)
       .zIndex(if(amount>0)3f else 0f)
       .onGloballyPositioned { coords ->
        val p = coords.positionInRoot()
        animalRootCenters[a.name] = Offset(p.x + coords.size.width / 2f, p.y + coords.size.height / 2f)
       },
      contentAlignment=Alignment.Center
     ){
      Box(
       Modifier
        .fillMaxSize()
        .clickable(
         interactionSource = remember { MutableInteractionSource() },
         indication = null
        ) { place(a.name) },
       contentAlignment = Alignment.Center
      ){
       if (image.isNotBlank())AsyncImage(
        model = image,
        contentDescription = a.name,
        colorFilter = if (isColorless) grayColorFilter else null,
        modifier = Modifier
         .fillMaxSize()
         .graphicsLayer{
          alpha = 1f
          val s = if(active) 1.08f else 1f
          scaleX = s
          scaleY = s
         },
        contentScale = ContentScale.Fit
       )
      }
      if(amount>0)WpBetBadge(
       amountText=wpFormat(amount),
       coinResId=coin,
       modifier=Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-4).dp)
      )
     }
    }
    Box(
     Modifier
      .size(68.dp)
      .clip(CircleShape)
      .background(
       Brush.verticalGradient(
        colors = listOf(
         Color(0xFFFFFAD6),
         Color(0xFFFFEBA8),
         Color(0xFFFDBA68),
         Color(0xFFEC7A30)
        )
       )
      )
      .border(
       width = 2.5.dp,
       brush = Brush.verticalGradient(
        colors = listOf(
         Color(0xFFFFEC9E),
         Color(0xFFF5B756),
         Color(0xFFD97A2B)
        )
       ),
       shape = CircleShape
      ),
     contentAlignment = Alignment.Center
    ){
     Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
     ){
      if(phase == WpPhase.BETTING){
       Text(
        "Select\nAnimal now",
        color = Color(0xFF4A1F06),
        fontSize = 8.5.sp,
        lineHeight = 9.5.sp,
        fontWeight = FontWeight.ExtraBold,
        fontStyle = FontStyle.Italic,
        textAlign = TextAlign.Center
       )
       Spacer(Modifier.height(2.dp))
       Text(
        "${countdown}s",
        color = Color(0xFF4E2105),
        fontWeight = FontWeight.Black,
        fontSize = 20.sp,
        lineHeight = 20.sp
       )
      } else {
       Text(
        "${countdown}s",
        color = Color(0xFFC84600),
        fontWeight = FontWeight.Black,
        fontSize = 21.sp,
        lineHeight = 21.sp,
        style = TextStyle(shadow = Shadow(Color(0xFFFFE59E).copy(alpha = 0.7f), Offset(0f, 1f), 2f))
       )
       Spacer(Modifier.height(3.dp))
       Text(
        "Waiting..",
        color = Color(0xFF5A2206),
        fontWeight = FontWeight.ExtraBold,
        fontStyle = FontStyle.Italic,
        fontSize = 8.5.sp,
        lineHeight = 9.5.sp
       )
      }
     }
    }
   }
   // Original TSX bottom decoration sits behind the balance and chip controls.
   val bottomDecor=wpAsset("wp_img_20260822_011000")
   if (bottomDecor.isNotBlank())AsyncImage(
    model = bottomDecor, contentDescription = null, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(112.dp),
    contentScale=ContentScale.Fit,
    alignment=Alignment.BottomCenter
   )
   // TSX Bottom Left Decor: separate artwork above the bottom decoration (not the coin).
   val bottomLeftDecor=wpAsset("wp_file_00000000993c8208ae462545f9ccb0ba")
   if (bottomLeftDecor.isNotBlank())AsyncImage(
    model = bottomLeftDecor,
    contentDescription = null,
    modifier = Modifier
     .align(Alignment.BottomStart)
     .padding(start=8.dp,bottom=60.dp)
     .size(44.dp)
     .onGloballyPositioned { coords ->
      val p = coords.positionInRoot()
      bottomLeftRootCenter = Offset(p.x + coords.size.width / 2f, p.y + coords.size.height / 2f)
     }
     .graphicsLayer {
      translationY = -1.dp.toPx() * bottomLeftPopup.value
      val s = 1f + 0.04f * bottomLeftPopup.value
      scaleX = s
      scaleY = s
     },
    contentScale=ContentScale.Fit,
    alignment=Alignment.BottomStart
   )
   WpBottomBalanceBar(
    coinResId = coin,
    balance = balance,
    modifier = Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 10.dp)
   )
   Row(
    Modifier
     .align(Alignment.BottomEnd)
     .clickable(
      interactionSource = remember { MutableInteractionSource() },
      indication = null,
      onClick = {}
     )
     .padding(end=8.dp,bottom=8.dp),
    verticalAlignment=Alignment.CenterVertically,
    horizontalArrangement=Arrangement.spacedBy(4.dp)
   ){
    WpGlossyRepeatButton(
     onClick = {
      scope.launch {
       val cost = lastBets.values.sum()
       if (phase == WpPhase.BETTING && cost > 0 && balance >= cost) {
        launch(Dispatchers.IO) { soundManager.playBetClick() }
        lastBets.forEach { (k, v) -> if (v > 0L) triggerBetVisuals(k, v) }
        walletChange(-cost, "Repeat Bet in Wild Party")
        bets = bets.toMutableMap().also { m -> lastBets.forEach { (k, v) -> m[k] = (m[k] ?: 0L) + v } }
       }
      }
     }
    )
    wpChips.forEachIndexed{i,c->
     val id=wpAsset(c.image)
     val isSelected = chipIndex == i
     val chipRotation by animateFloatAsState(
      targetValue = if (isSelected) 10f else 0f,
      animationSpec = tween(durationMillis = 140),
      label = "chipRotation"
     )
     Box(
      Modifier
       .size(34.dp)
       .graphicsLayer{
        val scale=if(isSelected)1.08f else 1f
        scaleX=scale
        scaleY=scale
        translationY=if(isSelected)-8.dp.toPx() else 0f
        rotationZ=chipRotation
       }
       .then(
        if (isSelected) Modifier.border(1.8.dp, Color(0xFFFFD83D), CircleShape)
        else Modifier
       )
       .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
       ){chipIndex=i},
      contentAlignment=Alignment.Center
     ){
      if (id.isNotBlank())AsyncImage(
       model = id,
       contentDescription = c.label,
       modifier = Modifier.fillMaxSize(),
       contentScale = ContentScale.Fit
      ) else Text(c.label,color=Color.White,fontSize=8.sp)
     }
    }
   }
   if (flyingCoins.isNotEmpty() && coin.isNotBlank()) {
    Box(Modifier.fillMaxSize().zIndex(25f)) {
     flyingCoins.forEach { fc ->
      key(fc.id) {
       WpFlyingCoinItem(
        coinItem = fc,
        coinRes = coin,
        onFinished = { finishedId ->
         flyingCoins = flyingCoins.filterNot { it.id == finishedId }
        }
       )
      }
     }
    }
   }
   if(loading)WpLoadingPage(progress=loadingProgress)
  }

  // History Page (same height as Main page, max 20 rows, Time | Result | My Bet | Win Amount)
  if(historyOpen)WpFullHeightSheet(title="History",sheetHeight=h,onDismiss={historyOpen=false}){
   Row(
    modifier = Modifier
     .fillMaxWidth()
     .background(Color(0xFF151515))
     .padding(horizontal = 14.dp, vertical = 11.dp),
    verticalAlignment = Alignment.CenterVertically
   ) {
    Text("Time", Modifier.weight(1.35f), color = Color(0xFFAFAFAF), fontSize = 13.sp, fontWeight = FontWeight.Medium)
    Text("Result", Modifier.weight(0.90f), color = Color(0xFFAFAFAF), fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    Text("My Bet", Modifier.weight(0.95f), color = Color(0xFFAFAFAF), fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    Text("Win Amount", Modifier.weight(1.10f), color = Color(0xFFAFAFAF), fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End)
   }
   val recentTwenty = history.takeLast(20).reversed()
   if (recentTwenty.isEmpty()) {
    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
     Text("No History Yet", color = Color.White.copy(alpha = 0.45f), fontSize = 13.sp)
    }
   } else {
    LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
     items(recentTwenty, key = { it.round }) { r ->
      Row(
       modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 9.dp),
       verticalAlignment = Alignment.CenterVertically
      ) {
       Text(
        wpFormatTime(r.time),
        Modifier.weight(1.35f),
        color = Color.White,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium
       )
       Box(Modifier.weight(0.90f), contentAlignment = Alignment.Center) {
        val mixBig = r.winner == "Mix Big"
        val mixSmall = r.winner == "Mix Small"
        if (mixBig || mixSmall) {
         Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
          val group = if (mixBig) listOf("Eagle", "Bear", "Tiger", "Lion") else listOf("Rabbit", "Deer", "Dog", "Camel")
          group.forEachIndexed { idx, n ->
           val aid = wpAsset( wpAnimals.first { it.name == n }.image)
           if (aid.isNotBlank()) AsyncImage(
            model = aid, contentDescription = null, modifier = Modifier.align(when (idx) { 0 -> Alignment.TopStart; 1 -> Alignment.TopEnd; 2 -> Alignment.BottomStart; else -> Alignment.BottomEnd }).size(15.dp),
            contentScale = ContentScale.Fit
           )
          }
         }
        } else {
         val aid = wpAsset( wpAnimals.firstOrNull { it.name == r.winner }?.image ?: "")
         if (aid.isNotBlank()) AsyncImage(
          model = aid, contentDescription = r.winner, modifier = Modifier.size(30.dp), contentScale = ContentScale.Fit)
        }
       }
       Text(
        if (r.myBet > 0L) wpFormat(r.myBet) else "0",
        Modifier.weight(0.95f),
        color = Color.White,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
       )
       Text(
        if (r.winAmount > 0L) wpFormat(r.winAmount) else "0",
        Modifier.weight(1.10f),
        color = if (r.winAmount > 0L) wpGold else Color.White,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.End
       )
      }
      HorizontalDivider(
       modifier = Modifier.padding(horizontal = 12.dp),
       thickness = 0.7.dp,
       color = Color.White.copy(alpha = 0.08f)
      )
     }
    }
   }
  }

  // Rules Page (same height as Main page, exact layout as 2nd image)
  if(rulesOpen)WpFullHeightSheet(title="Rules",sheetHeight=h,onDismiss={rulesOpen=false}){
   Column(
    modifier = Modifier
     .fillMaxWidth()
     .weight(1f)
     .verticalScroll(rememberScrollState())
     .padding(horizontal = 16.dp, vertical = 10.dp)
   ) {
    // Basic Rules
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
     Box(Modifier.size(6.dp).clip(CircleShape).background(Color.White))
     Text("Basic Rules", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(10.dp))
    Row(Modifier.padding(start = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
     Text("•", color = Color(0xFFE0E0E0), fontSize = 14.sp, fontWeight = FontWeight.Bold)
     Text(
      "Players can choose 8 areas to place bets within the specified time, and will be rewarded with the corresponding multiplier if the prediction is correct.",
      color = Color(0xFFE0E0E0),
      fontSize = 13.5.sp,
      lineHeight = 19.sp
     )
    }

    Spacer(Modifier.height(20.dp))

    // Mix Prize
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
     Box(Modifier.size(6.dp).clip(CircleShape).background(Color.White))
     Text("Mix Prize", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(10.dp))
    Row(Modifier.padding(start = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
     Text("•", color = Color(0xFFE0E0E0), fontSize = 14.sp, fontWeight = FontWeight.Bold)
     Text(
      "The mix prize will be triggered irregularly (Open 4 large or 4 small animals together)",
      color = Color(0xFFE0E0E0),
      fontSize = 13.5.sp,
      lineHeight = 19.sp
     )
    }

    Spacer(Modifier.height(20.dp))

    // Payout Rule
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
     Box(Modifier.size(6.dp).clip(CircleShape).background(Color.White))
     Text("Payout Rule", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(12.dp))
    Text("Prize = Bet × Multiple", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(10.dp))

    val gridLine = Color.White.copy(alpha = 0.08f)
    val smallGroup = wpAnimals.subList(0, 4)
    val bigGroup = wpAnimals.subList(4, 8)

    @Composable
    fun PayoutAnimalRow(group: List<WpAnimal>) {
     Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
      Box(Modifier.weight(1.15f).padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
       Text("Position", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
      }
      group.forEach { a ->
       Box(Modifier.fillMaxHeight().width(0.7.dp).background(gridLine))
       Box(Modifier.weight(1f).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        val aid = wpAsset( a.image)
        if (aid.isNotBlank()) AsyncImage(
         model = aid, contentDescription = a.name, modifier = Modifier.size(38.dp), contentScale = ContentScale.Fit)
       }
      }
     }
    }

    @Composable
    fun PayoutMultipleRow(group: List<WpAnimal>) {
     Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
      Box(Modifier.weight(1.15f).padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
       Text("Multiple", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
      }
      group.forEach { a ->
       Box(Modifier.fillMaxHeight().width(0.7.dp).background(gridLine))
       Box(Modifier.weight(1f).padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
        Text("${a.multiplier}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
       }
      }
     }
    }

    Column(
     modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(Color(0xFF141414))
    ) {
     PayoutAnimalRow(smallGroup)
     HorizontalDivider(thickness = 0.7.dp, color = gridLine)
     PayoutMultipleRow(smallGroup)
     HorizontalDivider(thickness = 0.7.dp, color = gridLine)
     PayoutAnimalRow(bigGroup)
     HorizontalDivider(thickness = 0.7.dp, color = gridLine)
     PayoutMultipleRow(bigGroup)
    }
    Spacer(Modifier.height(14.dp))
   }
  }

  // Winner Page (Auto-closes only when countdown ends; user cannot manually dismiss)
  if(winnerOpen){
   val winnerSheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
   Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
    Box(
     Modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.45f))
      .clickable(
       interactionSource = remember { MutableInteractionSource() },
       indication = null,
       onClick = {}
      )
    )
    Column(
     modifier = Modifier
      .fillMaxWidth()
      .heightIn(max = h * 0.86f)
      .clip(winnerSheetShape)
      .background(Color(0xFF060608))
      .clickable(
       interactionSource = remember { MutableInteractionSource() },
       indication = null,
       onClick = {}
      )
      .verticalScroll(rememberScrollState())
      .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 18.dp),
     horizontalAlignment = Alignment.CenterHorizontally
    ) {
     // Top Section: Countdown (top-right) + Winner Animal (left) + Prizes & Your Bet (right)
     Box(Modifier.fillMaxWidth()) {
      Text(
       text = "${winnerTimer}s",
       modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(top = 2.dp, end = 2.dp),
       color = Color.White,
       fontWeight = FontWeight.Bold,
       fontSize = 15.sp
      )
      Row(
       modifier = Modifier
        .fillMaxWidth()
        .padding(top = 12.dp, start = 4.dp, end = 10.dp),
       verticalAlignment = Alignment.CenterVertically
      ) {
       Box(Modifier.size(82.dp), contentAlignment = Alignment.Center) {
        if (mode == WpMode.MIX_BIG || mode == WpMode.MIX_SMALL) {
         val group = if (mode == WpMode.MIX_BIG) listOf("Eagle", "Bear", "Tiger", "Lion") else listOf("Rabbit", "Deer", "Dog", "Camel")
         group.forEachIndexed { idx, n ->
          val aid = wpAsset( wpAnimals.first { it.name == n }.image)
          if (aid.isNotBlank()) AsyncImage(
           model = aid, contentDescription = null, modifier = Modifier
            .align(when (idx) { 0 -> Alignment.TopStart; 1 -> Alignment.TopEnd; 2 -> Alignment.BottomStart; else -> Alignment.BottomEnd })
            .size(40.dp),
           contentScale = ContentScale.Fit
          )
         }
        } else {
         val winAnimalImg = wpAsset( wpAnimals[winnerIndex.coerceIn(0, 7)].image)
         if (winAnimalImg.isNotBlank()) {
          AsyncImage(
           model = winAnimalImg,
           contentDescription = null,
           modifier = Modifier.fillMaxSize(),
           contentScale = ContentScale.Fit
          )
         }
        }
       }

       Spacer(Modifier.width(24.dp))

       Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(14.dp)
       ) {
        Row(
         modifier = Modifier.fillMaxWidth(),
         verticalAlignment = Alignment.CenterVertically
        ) {
         Text(
          text = "Prizes",
          modifier = Modifier.weight(0.70f),
          color = Color.White,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold
         )
         Row(
          modifier = Modifier.weight(1.15f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
         ) {
          if (coin.isNotBlank()) {
           AsyncImage(
            model = coin,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            contentScale = ContentScale.Fit
           )
          }
          Text(
           text = wpFormat(winAmount),
           color = Color.White,
           fontSize = 15.sp,
           fontWeight = FontWeight.Bold
          )
         }
        }

        Row(
         modifier = Modifier.fillMaxWidth(),
         verticalAlignment = Alignment.CenterVertically
        ) {
         Text(
          text = "Your Bet",
          modifier = Modifier.weight(0.70f),
          color = Color.White,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold
         )
         Row(
          modifier = Modifier.weight(1.15f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
         ) {
          if (coin.isNotBlank()) {
           AsyncImage(
            model = coin,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            contentScale = ContentScale.Fit
           )
          }
          Text(
           text = wpFormat(betAmount),
           color = Color.White,
           fontSize = 15.sp,
           fontWeight = FontWeight.Bold
          )
         }
        }
       }
      }
     }

     Spacer(Modifier.height(16.dp))

     // Middle Heading Artwork (Top Winner image - thoda niche + size increased without shifting other elements)
     val heading = wpAsset( "wp_img_20260913_000423")
     if (heading.isNotBlank()) {
      Box(
       modifier = Modifier
        .fillMaxWidth()
        .height(36.dp),
       contentAlignment = Alignment.Center
      ) {
       AsyncImage(
        model = heading,
        contentDescription = null,
        modifier = Modifier
         .fillMaxWidth()
         .offset(y = 6.dp)
         .requiredHeight(72.dp),
        contentScale = ContentScale.Fit
       )
      }
     }

     Spacer(Modifier.height(14.dp))

     // Bottom Podium: 2nd | 1st (slightly higher) | 3rd using existing frame drawables
     val frameItems = listOf(
      Triple("2nd", "wp_img_20260912_235156", 0),
      Triple("1st", "wp_img_20260912_235215", 1),
      Triple("3rd", "wp_img_20260912_235230", 2)
     )
     Row(
      modifier = Modifier
       .fillMaxWidth()
       .padding(horizontal = 4.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.Bottom
     ) {
      frameItems.forEachIndexed { index, item ->
       val isFirst = index == 1
       val frame = wpAsset( item.second)
       val frameSize = if (isFirst) 84.dp else 76.dp
       val avatarSize = if (isFirst) 58.dp else 52.dp
       val showMe = isFirst && winAmount > 0L

       Column(
        modifier = Modifier
         .weight(1f)
         .padding(bottom = if (isFirst) 12.dp else 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally
       ) {
        Box(
         modifier = Modifier.size(frameSize),
         contentAlignment = Alignment.Center
        ) {
         WpWinnerAvatarPlaceholder(
          modifier = Modifier
           .size(avatarSize)
           .offset(y = 2.dp)
         )
         if (frame.isNotBlank()) {
          AsyncImage(
           model = frame,
           contentDescription = item.first,
           modifier = Modifier.fillMaxSize(),
           contentScale = ContentScale.Fit
          )
         }
        }
        Spacer(Modifier.height(6.dp))
        Text(
         text = if (showMe) "You" else "-",
         color = Color.White,
         fontSize = 13.sp,
         fontWeight = FontWeight.Bold,
         maxLines = 1
        )
        Spacer(Modifier.height(4.dp))
        Row(
         verticalAlignment = Alignment.CenterVertically,
         horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
         if (coin.isNotBlank()) {
          AsyncImage(
           model = coin,
           contentDescription = null,
           modifier = Modifier.size(15.dp),
           contentScale = ContentScale.Fit
          )
         }
         Text(
          text = if (showMe) wpFormat(winAmount) else "-",
          color = Color.White,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1
         )
        }
       }
      }
     }
    }
   }
  }
 }
}

@Composable
private fun WpFullHeightSheet(
 title: String,
 sheetHeight: Dp,
 onDismiss: () -> Unit,
 content: @Composable ColumnScope.() -> Unit
) {
 Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
  Box(
   Modifier
    .fillMaxSize()
    .background(Color.Black.copy(alpha = 0.45f))
    .clickable(
     interactionSource = remember { MutableInteractionSource() },
     indication = null,
     onClick = { onDismiss() }
    )
  )
  Column(
   modifier = Modifier
    .fillMaxWidth()
    .height(sheetHeight)
    .background(Color.Black)
    .clickable(
     interactionSource = remember { MutableInteractionSource() },
     indication = null,
     onClick = {}
    )
  ) {
   Box(
    modifier = Modifier
     .fillMaxWidth()
     .padding(horizontal = 12.dp, vertical = 10.dp)
   ) {
    WpGlassBackButton(
     onClick = onDismiss,
     modifier = Modifier.align(Alignment.CenterStart)
    )
    Text(
     title,
     modifier = Modifier.align(Alignment.Center),
     color = Color.White,
     fontSize = 18.sp,
     fontWeight = FontWeight.Bold
    )
   }
   content()
  }
 }
}

@Composable
private fun WpSheet(title:String,onDismiss:()->Unit,content:@Composable ColumnScope.()->Unit){
 Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.45f)).clickable{onDismiss()},contentAlignment=Alignment.BottomCenter){
  Column(
   Modifier
    .fillMaxWidth()
    .fillMaxHeight(.40f)
    .clip(RoundedCornerShape(topStart=12.dp,topEnd=12.dp))
    .background(wpDark)
    .clickable(
     interactionSource = remember { MutableInteractionSource() },
     indication = null,
     onClick = {}
    )
    .padding(horizontal=14.dp,vertical=12.dp)
  ){
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("‹",Modifier.clickable{onDismiss()}.padding(horizontal=8.dp),color=Color.White,fontSize=28.sp);Text(title,Modifier.weight(1f),color=Color.White,fontWeight=FontWeight.Black,fontSize=16.sp,textAlign=TextAlign.Center);Spacer(Modifier.width(32.dp))}
   HorizontalDivider(color=Color.White.copy(alpha=.1f));content()
  }
 }
}
