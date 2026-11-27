package com.hurry.voicechat

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import com.google.firebase.auth.FirebaseUser
import java.util.Calendar

private data class NativeRoomTaskRow(val title: String, val reward: String, val icon: String)
private data class NativeRoomTaskStatus(val complete: Boolean, val claimed: Boolean, val progress: Float = 0f)

private fun parseNativeRoomTaskStatuses(response: JSONObject): Map<Int, NativeRoomTaskStatus> {
    val array = response.optJSONArray("tasks") ?: return emptyMap()
    return (0 until array.length()).mapNotNull { index ->
        array.optJSONObject(index)?.let { item ->
            item.optInt("taskId") to NativeRoomTaskStatus(
                item.optBoolean("complete"),
                item.optBoolean("claimed"),
                (item.optDouble("progressPercent", 0.0) / 100.0).toFloat().coerceIn(0f, 1f)
            )
        }
    }.toMap()
}

private object NativeRoomTaskApi {
    private const val BASE = "https://zyora-server.onrender.com"

    // MainActivity restores its local session before Firebase may finish restoring
    // its persisted user. Wait for Firebase's auth-state callback during that window.
    private suspend fun awaitSignedInUser(): FirebaseUser {
        val auth = FirebaseAuth.getInstance()
        auth.currentUser?.let { return it }
        return withTimeout(15000L) {
            suspendCancellableCoroutine { continuation ->
                lateinit var listener: FirebaseAuth.AuthStateListener
                listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                    val restoredUser = firebaseAuth.currentUser
                    if (restoredUser != null && continuation.isActive) {
                        firebaseAuth.removeAuthStateListener(listener)
                        continuation.resume(restoredUser)
                    }
                }
                auth.addAuthStateListener(listener)
                continuation.invokeOnCancellation { auth.removeAuthStateListener(listener) }
                auth.currentUser?.let { restoredUser ->
                    if (continuation.isActive) {
                        auth.removeAuthStateListener(listener)
                        continuation.resume(restoredUser)
                    }
                }
            }
        }
    }

    suspend fun request(roomId: String, claimTaskId: Int? = null, shareTask: Boolean = false): JSONObject = withContext(Dispatchers.IO) {
        val user = awaitSignedInUser()
        // Firebase refreshes expired ID tokens automatically when needed.
        val token = Tasks.await(user.getIdToken(false), 30, TimeUnit.SECONDS).token
            ?: error("Authentication token unavailable.")
        val encodedRoomId = java.net.URLEncoder.encode(roomId, "UTF-8")
        val endpoint = if (claimTaskId == null) "/api/room-owner-tasks/$encodedRoomId"
            else if (shareTask) "/api/room-owner-tasks/$encodedRoomId/share"
            else "/api/room-owner-tasks/$encodedRoomId/claim"
        val connection = (URL(BASE + endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = if (claimTaskId == null) "GET" else "POST"
            connectTimeout = 10000
            // Render free instances can need time to wake from idle; allow one
            // request to finish instead of timing out and stacking slow retries.
            readTimeout = 60000
            useCaches = false
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Cache-Control", "no-cache")
            if (claimTaskId != null) doOutput = true
        }
        try {
            if (claimTaskId != null) connection.outputStream.use {
                it.write(JSONObject().put("taskId", claimTaskId).toString().toByteArray(Charsets.UTF_8))
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = if (raw.isBlank()) JSONObject() else JSONObject(raw)
            if (code !in 200..299) error(json.optString("error").ifBlank { "Room task request failed ($code)." })
            json
        } finally {
            connection.disconnect()
        }
    }
}

@Composable
fun Roomtask(roomId: String, onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val taskRoomId = remember(roomId) { roomId.trim() }
    val taskStatusPrefs = remember(context) {
        context.getSharedPreferences("zyora_room_task_cache", android.content.Context.MODE_PRIVATE)
    }
    val taskCacheKey = remember(taskRoomId) { "statuses_$taskRoomId" }
    // Restore the last server-confirmed snapshot immediately while the API refreshes.
    var taskStatuses by remember(taskRoomId) {
        mutableStateOf(
            runCatching {
                parseNativeRoomTaskStatuses(
                    JSONObject(taskStatusPrefs.getString(taskCacheKey, "{}") ?: "{}")
                )
            }.getOrDefault(emptyMap())
        )
    }
    var claimingTask by remember { mutableIntStateOf(0) }
    var pendingShareTask by remember { mutableIntStateOf(0) }
    val raw = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/"
    var rules by remember { mutableStateOf(false) }
    var hour by remember { mutableIntStateOf(0) }
    var minute by remember { mutableIntStateOf(0) }
    var second by remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()
    val shareResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val completedTaskId = pendingShareTask
        if (completedTaskId != 0) {
            pendingShareTask = 0
            scope.launch {
                runCatching { NativeRoomTaskApi.request(taskRoomId, completedTaskId, shareTask = true) }
                    .onSuccess {
                        val refreshed = NativeRoomTaskApi.request(taskRoomId)
                        val array = refreshed.optJSONArray("tasks")
                        if (array != null) {
                            taskStatuses = parseNativeRoomTaskStatuses(refreshed)
                            taskStatusPrefs.edit().putString(taskCacheKey, refreshed.toString()).apply()
                        } else {
                            taskStatuses = taskStatuses + (completedTaskId to NativeRoomTaskStatus(true, false, 1f))
                        }
                    }
                    .onFailure { error ->
                        android.widget.Toast.makeText(context, error.message ?: "Share could not be recorded.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                claimingTask = 0
            }
        }
    }

    LaunchedEffect(taskRoomId) {
        if (taskRoomId.isNotBlank()) {
            var lastError: Throwable? = null
            repeat(2) { attempt ->
                val result = runCatching { NativeRoomTaskApi.request(taskRoomId) }
                if (result.isSuccess) {
                    val response = result.getOrThrow()
                    val array = response.optJSONArray("tasks")
                    if (array != null) {
                        taskStatuses = parseNativeRoomTaskStatuses(response)
                        taskStatusPrefs.edit().putString(taskCacheKey, response.toString()).apply()
                    }
                    lastError = null
                    return@LaunchedEffect
                } else {
                    lastError = result.exceptionOrNull()
                    if (attempt < 1) delay(1500L)
                }
            }
            lastError?.let { error ->
                android.widget.Toast.makeText(
                    context,
                    error.message ?: "Room tasks could not be loaded. Please try again.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                val days = (Calendar.MONDAY - get(Calendar.DAY_OF_WEEK) + 7) % 7
                add(Calendar.DAY_OF_MONTH, if (days == 0) 7 else days)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val left = ((target.timeInMillis - now.timeInMillis).coerceAtLeast(0L)) / 1000L
            hour = ((left / 3600) % 24).toInt()
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
                        text = java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(
                            tasks.mapIndexedNotNull { index, task ->
                                if (taskStatuses[index + 1]?.claimed == true) task.reward.replace(",", "").toLongOrNull() else null
                            }.sum()
                        ),
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.offset(y = (-15).dp).padding(vertical = 0.dp).zIndex(5f)
                ) {
                    Text("Countdown", color = Color(0xFFF5B8B8), fontSize = 10.sp)
                    listOf(hour to "Hour", minute to "Minute", second to "Second").forEach { pair ->
                        Box(
                            Modifier.background(
                                Brush.verticalGradient(listOf(Color(0xFF8F1A1A), Color(0xFF470A0A))),
                                RoundedCornerShape(4.dp)
                            ).padding(horizontal = 5.dp, vertical = 3.dp)
                        ) {
                            Text(pair.first.toString(), color = Color(0xFFFFE0E0), fontSize = 11.sp)
                        }
                        Text(pair.second, color = Color(0xFFF5B8B8), fontSize = 10.sp)
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth().offset(y = (-25).dp).zIndex(1f),
                    verticalArrangement = Arrangement.spacedBy((-60).dp)
                ) {                    tasks.forEach { task ->
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

                                    val taskId = tasks.indexOf(task) + 1
                                    val status = taskStatuses[taskId]
                                    Column(
                                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(5.dp),
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Text(
                                            task.title,
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.fillMaxWidth(),
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
                                        Box(
                                            Modifier.fillMaxWidth().height(3.dp)
                                                .background(Color.White.copy(alpha = 0.20f), RoundedCornerShape(8.dp))
                                        ) {
                                            Box(
                                                Modifier.fillMaxWidth(status?.progress ?: 0f).fillMaxHeight()
                                                    .background(Color(0xFFFFD45A), RoundedCornerShape(8.dp))
                                            )
                                        }
                                    }

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
                                        val isShareTask = taskId == 3 || taskId == 4
                                        val isClaimed = status?.claimed == true
                                        val canClaim = status?.complete == true && !isClaimed
                                        val buttonLabel = when {
                                            isClaimed -> "CLAIM"
                                            canClaim -> "CLAIM"
                                            isShareTask -> "SHARE"
                                            else -> "CLAIM"
                                        }
                                        Button(
                                            enabled = !isClaimed && (canClaim || isShareTask) && claimingTask == 0,
                                            onClick = {
                                                if (claimingTask != 0 || taskRoomId.isBlank()) return@Button
                                                if (isShareTask && status?.complete != true) {
                                                    val packageName = if (taskId == 3) "com.whatsapp" else "com.facebook.katana"
                                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "text/plain"
                                                        putExtra(Intent.EXTRA_TEXT, "Join me on Zyora Voice Chat! https://zyora-voice-chat.vercel.app")
                                                        setPackage(packageName)
                                                    }
                                                    runCatching {
                                                        claimingTask = taskId
                                                        pendingShareTask = taskId
                                                        shareResultLauncher.launch(shareIntent)
                                                    }.onFailure {
                                                        pendingShareTask = 0
                                                        claimingTask = 0
                                                        android.widget.Toast.makeText(context, if (taskId == 3) "WhatsApp is not installed." else "Facebook is not installed.", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                } else if (canClaim) {
                                                    claimingTask = taskId
                                                    scope.launch {
                                                        runCatching { NativeRoomTaskApi.request(taskRoomId, taskId) }
                                                            .onSuccess { response ->
                                                                val newBalance = response.optLong("balance", -1L)
                                                                if (newBalance >= 0L) {
                                                                    val authPrefs = context.getSharedPreferences("hurry_auth", android.content.Context.MODE_PRIVATE)
                                                                    val walletUserId = authPrefs.getString("uid", null)?.takeIf { it.isNotBlank() }
                                                                        ?: authPrefs.getString("accountId", "").orEmpty()
                                                                    context.getSharedPreferences("zyora_wallet_cache", android.content.Context.MODE_PRIVATE)
                                                                        .edit().putLong("balance_$walletUserId", newBalance).putLong("last_sync_$walletUserId", System.currentTimeMillis()).apply()
                                                                    withContext(Dispatchers.IO) {
                                                                        val dao = WalletDatabase.get(context).walletDao()
                                                                        val current = dao.getUserData() ?: UserDataEntity()
                                                                        dao.saveUserData(current.copy(balance = newBalance))
                                                                        dao.insertTransaction(TransactionEntity(
                                                                            title = "Room Task Reward",
                                                                            amount = response.optLong("reward", (newBalance - current.balance).coerceAtLeast(0L)),
                                                                            date = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date()),
                                                                            timestamp = System.currentTimeMillis(),
                                                                            type = "coin"
                                                                        ))
                                                                    }
                                                                }
                                                                taskStatuses = taskStatuses + (taskId to NativeRoomTaskStatus(true, true, 1f))
                                                                runCatching { NativeRoomTaskApi.request(taskRoomId) }
                                                                    .onSuccess { refreshed ->
                                                                        val array = refreshed.optJSONArray("tasks")
                                                                        if (array != null) {
                                                                            taskStatuses = parseNativeRoomTaskStatuses(refreshed)
                                                                            taskStatusPrefs.edit().putString(taskCacheKey, refreshed.toString()).apply()
                                                                        }
                                                                    }
                                                            }
                                                            .onFailure { error ->
                                                                android.widget.Toast.makeText(context, error.message ?: "Unable to claim reward.", android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        claimingTask = 0
                                                    }
                                                }
                                            },
                                            modifier = Modifier.padding(top = 6.dp).width(85.dp).height(28.dp),
                                            contentPadding = PaddingValues(0.dp),
                                            shape = RoundedCornerShape(18.dp),
                                            border = if (isClaimed) null else androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFF5E6A8)),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.Transparent,
                                                disabledContainerColor = Color.Transparent,
                                                disabledContentColor = Color(0xFFB8B8B8)                                            )
                                        ) {
                                            if (isClaimed) {
                                                Text("CLAIMED", color = Color.Unspecified, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                            } else {
                                                Box(
                                                    Modifier.fillMaxSize().background(
                                                        Brush.verticalGradient(listOf(Color(0xFFF7DFA0), Color(0xFFE0A92A))),
                                                        RoundedCornerShape(18.dp)
                                                    ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(buttonLabel, color = Color(0xFF4A1F00), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom frame 35dp upar + left/right borders par overlap + niche ka scroll gap kam
                AsyncImage(
                    model = raw + "file_0000000066c88211aa777b1f6da8683f.png",
                    contentDescription = "Frame Bottom Border",
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(10f)
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            val shiftPx = 35.dp.roundToPx()
                            layout(placeable.width, (placeable.height - shiftPx).coerceAtLeast(0)) {
                                placeable.place(0, -shiftPx)
                            }
                        }
                        .graphicsLayer { scaleX = 1.12f },
                    contentScale = ContentScale.FillWidth
                )
            }
        }

        if (rules) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { rules = false },
                properties = androidx.compose.ui.window.DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                val cardShape = RoundedCornerShape(14.dp)
                val buttonShape = RoundedCornerShape(12.dp)
                val goldenBorderBrush = Brush.verticalGradient(
                    listOf(Color(0xFFF3D578), Color(0xFFC59B38), Color(0xFFE5C158))
                )
                val cardBgColors = listOf(Color(0xFF5A0A12), Color(0xFF3D050A), Color(0xFF260205))
                val buttonBgColors = listOf(Color(0xFF7A121C), Color(0xFF4A080F))

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable { rules = false },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.86f)
                            .clip(cardShape)
                            .background(Brush.verticalGradient(colors = cardBgColors))
                            .border(1.5.dp, goldenBorderBrush, cardShape)
                            .clickable(enabled = false) {}
                            .padding(horizontal = 18.dp, vertical = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "ROOM TASK RULES",
                                style = TextStyle(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color.White, Color(0xFFFCE28B), Color(0xFFE5B83B))
                                    ),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 14.dp)
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "Each user can complete a 'Room Owner' task only once per day.",
                                    "Each user can complete a daily task once per day. Tasks reset at 5:30+GMT.",
                                    "For gift-giving tasks, only gifts sent within the room are counted; gifts sent via private message are not included.",
                                    "Treasure Chest\nWhen a gift is received in a personal room, the room owner can earn a 5% coin reward of the gift's value. There is no limit on the coin bonus; the more gifts received in the room, the more coin rewards you earn.",
                                    "If you create multiple accounts using the same device or SIM card, you can only claim rewards once for each task."
                                ).forEachIndexed { index, rule ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "•",
                                            color = Color(0xFFFCE28B),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (index == 3) {
                                            Column {
                                                Text("Treasure Chest", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 2.dp))
                                                Text(rule.substringAfter("\n"), color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp, lineHeight = 16.sp)
                                            }
                                        } else {
                                            Text(rule, color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp, lineHeight = 16.sp)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(buttonShape)
                                    .background(Brush.verticalGradient(colors = buttonBgColors))
                                    .border(1.5.dp, Color(0xFFE5C158), buttonShape)
                                    .clickable { rules = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Got it",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}