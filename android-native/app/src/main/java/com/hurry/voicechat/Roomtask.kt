package com.hurry.voicechat

import android.content.Intent
import android.widget.Toast

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import org.json.JSONArray

private data class NativeRoomTaskRow(val title: String, val reward: String, val icon: String)

private suspend fun requestNativeRoomTasks(roomId: String, claimTaskId: Int? = null): JSONObject = withContext(Dispatchers.IO) {
    val user = FirebaseAuth.getInstance().currentUser ?: error("Please sign in again.")
    val token = Tasks.await(user.getIdToken(false), 30, TimeUnit.SECONDS).token ?: error("Authentication token unavailable.")
    val encodedRoomId = java.net.URLEncoder.encode(roomId, "UTF-8")
    val endpoint = if (claimTaskId == null) "https://zyora-server.onrender.com/api/room-owner-tasks/$encodedRoomId"
        else "https://zyora-server.onrender.com/api/room-owner-tasks/$encodedRoomId/claim"
    val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
        requestMethod = if (claimTaskId == null) "GET" else "POST"
        connectTimeout = 12000
        readTimeout = 16000
        setRequestProperty("Authorization", "Bearer $token")
        setRequestProperty("Accept", "application/json")
        if (claimTaskId != null) { doOutput = true; setRequestProperty("Content-Type", "application/json") }
    }
    try {
        if (claimTaskId != null) connection.outputStream.use {
            it.write(JSONObject().put("taskId", claimTaskId).toString().toByteArray(Charsets.UTF_8))
        }
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        val result = JSONObject(body.ifBlank { "{}" })
        if (connection.responseCode !in 200..299 || !result.optBoolean("success")) {
            error(result.optString("error").ifBlank { "Room Task request failed" })
        }
        result
    } finally { connection.disconnect() }
}

@Composable
fun Roomtask(roomId: String, onBack: () -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val raw = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/"
    var rules by remember { mutableStateOf(false) }
    var hour by remember { mutableIntStateOf(0) }
    var minute by remember { mutableIntStateOf(0) }
    var second by remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val claimedTaskIds = remember { mutableStateListOf<Int>() }
    val completedTaskIds = remember { mutableStateListOf<Int>() }
    val claimingTaskIds = remember { mutableStateListOf<Int>() }
    var taskError by remember { mutableStateOf("") }

    LaunchedEffect(roomId) {
        if (roomId.isBlank()) return@LaunchedEffect
        while (true) {
            runCatching { requestNativeRoomTasks(roomId) }.onSuccess { response ->
                val rows = response.optJSONArray("tasks") ?: JSONArray()
                val completed = mutableListOf<Int>()
                val claimed = mutableListOf<Int>()
                for (index in 0 until rows.length()) {
                    val row = rows.optJSONObject(index) ?: continue
                    val id = row.optInt("taskId")
                    if (row.optBoolean("complete")) completed.add(id)
                    if (row.optBoolean("claimed")) claimed.add(id)
                }
                completedTaskIds.clear(); completedTaskIds.addAll(completed)
                claimedTaskIds.clear(); claimedTaskIds.addAll(claimed)
                taskError = ""
            }.onFailure { taskError = it.message.orEmpty() }
            delay(15000)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
            val target = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                set(Calendar.HOUR_OF_DAY, 5)
                set(Calendar.MINUTE, 30)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_MONTH, 1)
            }
            val left = ((target.timeInMillis - now.timeInMillis).coerceAtLeast(0L)) / 1000L
            hour = (left / 3600).toInt()
            minute = ((left / 60) % 60).toInt()
            second = (left % 60).toInt()
            delay(1000)
        }
    }

    val tasks = listOf(
        NativeRoomTaskRow("10 Users Enter the Room", "10,000", "file_00000000858082118b5c81b85cd6d2a8.png"),
        NativeRoomTaskRow("5 Users Enter the Room for Two Consecutive Days", "20,000", "file_00000000858082118b5c81b85cd6d2a8.png"),
        NativeRoomTaskRow("Successfully Share on WhatsApp", "5,000", "file_0000000019a0821193463686d6fc9184.png"),
        NativeRoomTaskRow("Successfully Share on Facebook", "5,000", "file_0000000019a0821193463686d6fc9184.png"),
        NativeRoomTaskRow("3 New Users Enter the Room", "10,000", "file_00000000858082118b5c81b85cd6d2a8.png"),
        NativeRoomTaskRow("1 New Follower of the Room", "2,000", "file_00000000858082118b5c81b85cd6d2a8.png"),
        NativeRoomTaskRow("3 New Users Follow the Room", "10,000", "file_00000000858082118b5c81b85cd6d2a8.png"),
        NativeRoomTaskRow("Spend 10 Minutes on Mic", "5,000", "file_00000000f8d88211ba5ff45c06383e5f.png"),
        NativeRoomTaskRow("Spend 30 Minutes on Mic", "25,000", "file_00000000f8d88211ba5ff45c06383e5f.png"),
        NativeRoomTaskRow("Spend 60 Minutes on Mic", "50,000", "file_00000000f8d88211ba5ff45c06383e5f.png"),
        NativeRoomTaskRow("10 New Users on Mic for 10 Minutes at the Same Time", "50,000", "file_00000000f8d88211ba5ff45c06383e5f.png"),
        NativeRoomTaskRow("5 New Followers of the Room", "25,000", "file_00000000858082118b5c81b85cd6d2a8.png"),
        NativeRoomTaskRow("3 Users on Mic at the Same Time", "10,000", "file_00000000f8d88211ba5ff45c06383e5f.png"),
        NativeRoomTaskRow("Send a Gift of 500", "2,000", "file_0000000081f48211afe58f6348196b55.png"),
        NativeRoomTaskRow("Send Gifts of 5,000", "10,000", "file_0000000081f48211afe58f6348196b55.png"),
        NativeRoomTaskRow("Successfully Invite 1 User on Mic", "500", "file_0000000019a0821193463686d6fc9184.png"),
        NativeRoomTaskRow("Successfully Invite 3 New Users on Mic", "10,000", "file_0000000019a0821193463686d6fc9184.png"),
        NativeRoomTaskRow("Send 10 Gifts on Mic", "20,000", "file_0000000081f48211afe58f6348196b55.png"),
        NativeRoomTaskRow("New User Sends a Gift of 1,000 on Mic", "5,000", "file_0000000081f48211afe58f6348196b55.png"),
        NativeRoomTaskRow("Successfully Invite 10 People to the Mic", "25,000", "file_0000000019a0821193463686d6fc9184.png"),
        NativeRoomTaskRow("3 Users on Mic at the Same Time for 10 Minutes", "10,000", "file_00000000f8d88211ba5ff45c06383e5f.png"),
        NativeRoomTaskRow("Send 1 Gift", "1,000", "file_0000000081f48211afe58f6348196b55.png")
    )

    Box(Modifier.fillMaxSize().background(Color(0xFF380308))) {

        Box(Modifier.fillMaxSize().verticalScroll(scrollState)) {

            // Top image ab full wide hai
            Box(modifier = Modifier.fillMaxWidth().height(480.dp)) {
                AsyncImage(
                    model = raw + "file_00000000cb748211bf0120855b80f449.png",
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(480.dp).align(Alignment.TopCenter).drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xFF380308)),
                                startY = size.height * 0.95f,
                                endY = size.height
                            )
                        )
                    },
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter
                )
            }

            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {

                Box(Modifier.fillMaxWidth().padding(top = 27.dp, start = 8.dp)) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        AsyncImage(
                            model = raw + "IMG_20261119_225146.png",
                            contentDescription = "Back",
                            modifier = Modifier.size(32.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    AsyncImage(
                        model = raw + "IMG_20261119_225214.png",
                        contentDescription = "Rules",
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .height(32.dp)
                            .wrapContentWidth()
                            .clickable { rules = true },
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.CenterEnd
                    )
                }

                Spacer(modifier = Modifier.height(302.dp))

                // Middle image ke ekdam center mein Yellow 0 add kar diya hai
                Box(
                    modifier = Modifier.padding(horizontal = 8.dp).fillMaxWidth().height(138.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = raw + "file_00000000f2908208a7b6a2b73c3bbf36.png",
                        contentDescription = "Room task rewards",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        text = "0",
                        color = Color(0xFFFFD700),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Room task image ka zIndex increase kar diya hai taaki left/right borders ke upar rahe
                Box(Modifier.fillMaxWidth().offset(y = (2).dp).zIndex(10f), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = raw + "file_00000000680881faa3dfdb17cce60858.png",
                        contentDescription = "Room Task frame",
                        modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = 1.12f },
                        contentScale = ContentScale.FillWidth
                    )
                    Text(
                        "ROOM TASK",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.offset(y = (-4).dp).shadow(4.dp, spotColor = Color.Black.copy(alpha = 0.9f))
                    )
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 34.dp)
                        .offset(y = (-15).dp)
                        .zIndex(5f)
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0.00f to Color.Transparent,
                                    0.16f to Color(0xFF8F1A1A).copy(alpha = 0.45f),
                                    0.50f to Color(0xFFFFD7A0),
                                    0.84f to Color(0xFF8F1A1A).copy(alpha = 0.45f),
                                    1.00f to Color.Transparent
                                )
                            ),
                            RoundedCornerShape(50)
                        )
                )

                Column(
                    modifier = Modifier.fillMaxWidth().offset(y = (-25).dp).zIndex(1f),
                    verticalArrangement = Arrangement.spacedBy((-60).dp)
                ) {
                    tasks.forEachIndexed { taskIndex, task ->
                        val taskId = taskIndex + 1
                        val isClaimed = taskId in claimedTaskIds
                        val isComplete = taskId in completedTaskIds
                        val isClaiming = taskId in claimingTaskIds
                        Box(Modifier.fillMaxWidth().height(175.dp)) {

                            AsyncImage(
                                model = raw + "IMG_20260903_105647.png",
                                contentDescription = null,
                                modifier = Modifier.align(Alignment.CenterStart).offset(x = (-15).dp, y = (-12).dp).width(45.dp).fillMaxHeight(1.1f),
                                contentScale = ContentScale.FillBounds
                            )
                            AsyncImage(
                                model = raw + "IMG_20260903_105705.png",
                                contentDescription = null,
                                modifier = Modifier.align(Alignment.CenterEnd).offset(x = 15.dp, y = (-12).dp).width(45.dp).fillMaxHeight(1.1f),
                                contentScale = ContentScale.FillBounds
                            )

                            Box(Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
                                AsyncImage(
                                    model = raw + "file_000000004fd0821198ed4e26d5008b16.png",
                                    contentDescription = "Task background",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.FillBounds
                                )

                                Row(Modifier.fillMaxSize().padding(start = 8.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
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
                                        model = raw + iconPath,
                                        contentDescription = task.title,
                                        modifier = Modifier.size(54.dp).padding(start = 8.dp),
                                        contentScale = ContentScale.Fit
                                    )

                                    Text(
                                        task.title,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                                        maxLines = 3,
                                        style = TextStyle(
                                            shadow = Shadow(
                                                color = Color.Black.copy(alpha = 0.5f),
                                                blurRadius = 4f,
                                                offset = Offset(0f, 2f)
                                            )
                                        ),
                                        lineHeight = 18.sp
                                    )

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            AsyncImage(
                                                model = raw + "file_00000000e56882119c217d508b6733dc.png",
                                                contentDescription = "Coins",
                                                modifier = Modifier.size(24.dp)
                                            )
                                            val rewardClean = task.reward.replace(Regex("coins", RegexOption.IGNORE_CASE), "").trim()
                                            Text(
                                                rewardClean,
                                                color = Color(0xFFFFD700),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                if (taskId == 3 || taskId == 4) {
                                                    val packageName = if (taskId == 3) "com.whatsapp" else "com.facebook.katana"
                                                    runCatching {
                                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                            type = "text/plain"
                                                            putExtra(Intent.EXTRA_TEXT, "Join me on Zyora Voice Chat! https://zyora-voice-chat.vercel.app")
                                                            setPackage(packageName)
                                                        }
                                                        context.startActivity(shareIntent)
                                                    }.onFailure {
                                                        Toast.makeText(context, if (taskId == 3) "WhatsApp is not installed" else "Facebook is not installed", Toast.LENGTH_SHORT).show()
                                                    }
                                                } else if (isComplete && !isClaimed && !isClaiming) {
                                                    claimingTaskIds.add(taskId)
                                                    scope.launch {
                                                        runCatching { requestNativeRoomTasks(roomId, taskId) }
                                                            .onSuccess { claimedTaskIds.add(taskId); taskError = "" }
                                                            .onFailure { taskError = it.message.orEmpty() }
                                                        claimingTaskIds.remove(taskId)
                                                    }
                                                }
                                            },
                                            enabled = (taskId == 3 || taskId == 4) || (isComplete && !isClaimed && !isClaiming),
                                            modifier = Modifier.padding(top = 6.dp).width(85.dp).height(28.dp),
                                            contentPadding = PaddingValues(0.dp),
                                            shape = RoundedCornerShape(18.dp),
                                            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFF5E6A8)),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                                        ) {
                                            Box(
                                                Modifier.fillMaxSize().background(
                                                    Brush.verticalGradient(if (isClaimed) listOf(Color(0xFF777777), Color(0xFF555555)) else listOf(Color(0xFFF7DFA0), Color(0xFFE0A92A))),
                                                    RoundedCornerShape(18.dp)
                                                ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    when { taskId == 3 || taskId == 4 -> "SHARE"; isClaimed -> "CLAIMED"; isClaiming -> "..."; else -> "CLAIM" },
                                                    color = if (isClaimed) Color.White else Color(0xFF4A1F00),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    letterSpacing = 1.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom image thori upar kar di aur bottom scroll space bhi kam kar diya
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(10f)
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            val shiftUp = 80.dp.roundToPx()
                            layout(placeable.width, (placeable.height - shiftUp).coerceAtLeast(0)) {
                                placeable.placeRelative(0, -shiftUp)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = raw + "file_0000000066c88211aa777b1f6da8683f.png",
                        contentDescription = "Frame Bottom Border",
                        modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = 1.12f },
                        contentScale = ContentScale.FillWidth
                    )
                }
            }
        }

        if (rules) Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = .70f)).clickable { rules = false },
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 620.dp).background(
                    Brush.verticalGradient(listOf(Color(0xFF4A0A10), Color(0xFF380308))),
                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ).padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Text(
                    "ROOM TASK RULES",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 14.dp)
                )
                listOf(
                    "Each user can complete a 'Room Owner' task only once per day.",
                    "Each user can complete a daily task once per day. Tasks reset at 5:30+GMT.",
                    "For gift-giving tasks, only gifts sent within the room are counted; gifts sent via private message are not included.",
                    "Treasure Chest\nWhen a gift is received in a personal room, the room owner can earn a 5% coin reward of the gift's value. There is no limit on the coin bonus; the more gifts received in the room, the more coin rewards you earn.",
                    "If you create multiple accounts using the same device or SIM card, you can only claim rewards once for each task."
                ).forEachIndexed { index, rule ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            Modifier.size(24.dp).background(
                                Brush.verticalGradient(listOf(Color(0xFFF7DFA0), Color(0xFFE0A92A))),
                                CircleShape
                            ).border(1.dp, Color(0xFFF5E6A8), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text((index + 1).toString(), color = Color(0xFF4A1F00), fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                        if (index == 3) {
                            Column {
                                Text("Treasure Chest", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                                Text(rule.substringAfter("\n"), color = Color(0xFFF5D0D0), fontSize = 13.sp, lineHeight = 18.sp)
                            }
                        } else {
                            Text(rule, color = Color(0xFFF5D0D0), fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }
                }
                Text(
                    "Regards, Zyora Team",
                    color = Color(0xFFFFD700),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
                )
            }
        }
    }
}
