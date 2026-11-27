package com.hurry.voicechat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import coil3.compose.AsyncImage
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File

private val DiscoverTopGradient = Brush.verticalGradient(
    0.0f  to Color(0xFF2563EB),
    0.97f to Color(0xFFEFF6FF),
    1.0f  to Color(0xFFF9FAFB)
)

private data class DiscoverCommentItem(
    val id: String,
    val authorName: String,
    val authorAvatar: String,
    val text: String,
    val createdAt: Long
)

private data class DiscoverPostItem(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorAvatar: String,
    val authorGender: String = "male",
    val text: String,
    val images: List<String>,
    val createdAt: Long,
    val likeCount: Int = 0,
    val liked: Boolean = false,
    val isMine: Boolean = true,
    val comments: List<DiscoverCommentItem> = emptyList()
)

private object DiscoverPostStore {
    val posts = mutableStateListOf<DiscoverPostItem>()
    private const val PREFS = "discover_local_cache"
    private const val KEY_POSTS = "posts_v1"
    private const val MAX_POSTS = 20
    private const val POST_TTL_MS = 24L * 60L * 60L * 1000L

    fun pruneExpired() {
        val cutoff = System.currentTimeMillis() - POST_TTL_MS
        posts.removeAll { it.createdAt <= 0L || it.createdAt < cutoff }
        while (posts.size > MAX_POSTS) posts.removeAt(posts.lastIndex)
    }

    fun load(context: Context, currentUserId: String) {
        if (posts.isNotEmpty()) { pruneExpired(); save(context); return }
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_POSTS, null) ?: return
        runCatching {
            val array = JSONArray(raw)
            val loaded = (0 until array.length()).mapNotNull { index ->
                array.optJSONObject(index)?.let { fromJson(it, currentUserId) }
            }.filter { it.createdAt > System.currentTimeMillis() - POST_TTL_MS }
                .sortedByDescending { it.createdAt }.take(MAX_POSTS)
            posts.addAll(loaded)
            save(context)
        }
    }

    fun save(context: Context) {
        runCatching {
            pruneExpired()
            val array = JSONArray()
            posts.sortedByDescending { it.createdAt }.take(MAX_POSTS).forEach { array.put(toJson(it)) }
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_POSTS, array.toString()).apply()
        }
    }

    fun upsert(post: DiscoverPostItem) {
        pruneExpired()
        if (post.createdAt <= 0L || post.createdAt < System.currentTimeMillis() - POST_TTL_MS) return
        val index = posts.indexOfFirst { it.id == post.id }
        if (index >= 0) posts[index] = post else posts.add(0, post)
        posts.sortByDescending { it.createdAt }
        while (posts.size > MAX_POSTS) posts.removeAt(posts.lastIndex)
    }

    private fun persistRemoteImage(context: Context, postId: String, index: Int, value: String): String {
        if (!value.startsWith("data:image/")) return value
        return runCatching {
            val comma = value.indexOf(',')
            require(comma > 0)
            val bytes = Base64.decode(value.substring(comma + 1), Base64.DEFAULT)
            val directory = File(context.filesDir, "discover_received").apply { mkdirs() }
            val safeId = postId.hashCode().toUInt().toString(16)
            val target = File(directory, "${safeId}_${index}.jpg")
            target.writeBytes(bytes)
            target.absolutePath
        }.getOrDefault("")
    }

    fun fromJson(item: JSONObject, currentUserId: String, context: Context): DiscoverPostItem {
        val images = item.optJSONArray("images") ?: JSONArray()
        val commentsJson = item.optJSONArray("comments") ?: JSONArray()
        val comments = (0 until commentsJson.length()).mapNotNull { index ->
            commentsJson.optJSONObject(index)?.let { c ->
                DiscoverCommentItem(c.optString("id"), c.optString("authorName"),
                    c.optString("authorAvatar"), c.optString("text"), c.optLong("createdAt"))
            }
        }
        val likedBy = item.optJSONArray("likedBy") ?: JSONArray()
        val liked = (0 until likedBy.length()).any { likedBy.optString(it) == currentUserId }
            || item.optBoolean("liked", false)
        return DiscoverPostItem(
            id = item.optString("id"), authorId = item.optString("authorId"),
            authorName = item.optString("authorName", "Zyora User"),
            authorAvatar = item.optString("authorAvatar"),
            authorGender = item.optString("authorGender", "male"),
            text = item.optString("text"),
            images = (0 until images.length()).mapNotNull { index ->
                persistRemoteImage(context, item.optString("id"), index, images.optString(index))
                    .takeIf { it.isNotBlank() }
            },
            createdAt = item.optLong("createdAt"), likeCount = item.optInt("likeCount"),
            liked = liked, isMine = item.optString("authorId") == currentUserId,
            comments = comments
        )
    }

    private fun toJson(post: DiscoverPostItem) = JSONObject().apply {
        put("id", post.id); put("authorId", post.authorId); put("authorName", post.authorName)
        put("authorAvatar", post.authorAvatar); put("authorGender", post.authorGender)
        put("text", post.text); put("createdAt", post.createdAt); put("likeCount", post.likeCount)
        put("liked", post.liked); put("images", JSONArray(post.images))
        put("comments", JSONArray().apply {
            post.comments.forEach { c -> put(JSONObject().put("id", c.id).put("authorName", c.authorName)
                .put("authorAvatar", c.authorAvatar).put("text", c.text).put("createdAt", c.createdAt)) }
        })
    }
}

private fun discoverEncodeImage(path: String): String? = runCatching {
    val source = BitmapFactory.decodeFile(path) ?: return null
    val scale = minOf(1f, 640f / maxOf(source.width, source.height))
    val bitmap = if (scale < 1f) Bitmap.createScaledBitmap(source,
        (source.width * scale).toInt().coerceAtLeast(1),
        (source.height * scale).toInt().coerceAtLeast(1), true) else source
    var quality = 58
    var bytes = ByteArray(0)
    do {
        bytes = ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
            output.toByteArray()
        }
        if (bytes.size <= 145 * 1024 || quality <= 40) break
        quality -= 6
    } while (quality >= 40)
    if (bitmap !== source) bitmap.recycle()
    source.recycle()
    if (bytes.isEmpty() || bytes.size > 170 * 1024) return null
    "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
}.getOrNull()


@Composable
fun DiscoverPage(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var selectedTab by remember { mutableStateOf("Discover") }
    var showCreatePost by remember { mutableStateOf(false) }
    var activeCommentPostId by remember { mutableStateOf<String?>(null) }

    val authPrefs = remember { context.getSharedPreferences("hurry_auth", Context.MODE_PRIVATE) }
    val currentUserId = remember {
        authPrefs.getString("accountId", null)?.takeIf { it.isNotBlank() }
            ?: authPrefs.getString("uid", null)?.takeIf { it.isNotBlank() }
            ?: "local_me"
    }
    val currentUserName = remember {
        sequenceOf("name", "username", "displayName", "nickName")
            .mapNotNull { authPrefs.getString(it, null)?.trim() }
            .firstOrNull { it.isNotBlank() && !it.equals("null", true) }
            ?: "Zyora User"
    }
    val currentUserAvatar = remember {
        sequenceOf("image", "avatar", "photoUrl", "profileImage")
            .mapNotNull { authPrefs.getString(it, null)?.trim() }
            .firstOrNull { it.isNotBlank() && !it.equals("null", true) }
            .orEmpty()
    }
    val currentUserGender = remember {
        sequenceOf("gender", "sex")
            .mapNotNull { authPrefs.getString(it, null)?.trim() }
            .firstOrNull { it.isNotBlank() && !it.equals("null", true) }
            ?: "male"
    }

    val discoverSocket = remember { runCatching { IO.socket("https://zyora-server.onrender.com") }.getOrNull() }
    LaunchedEffect(currentUserId) { DiscoverPostStore.load(context, currentUserId) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            DiscoverPostStore.pruneExpired()
            DiscoverPostStore.save(context)
        }
    }
    DisposableEffect(discoverSocket, currentUserId) {
        val socket = discoverSocket
        val onCreated = io.socket.emitter.Emitter.Listener { args ->
            val data = args.firstOrNull() as? JSONObject ?: return@Listener
            mainHandler.post {
                runCatching { DiscoverPostStore.upsert(DiscoverPostStore.fromJson(data, currentUserId, context)); DiscoverPostStore.save(context) }
            }
        }
        val onUpdated = io.socket.emitter.Emitter.Listener { args ->
            val data = args.firstOrNull() as? JSONObject ?: return@Listener
            mainHandler.post {
                runCatching { DiscoverPostStore.upsert(DiscoverPostStore.fromJson(data, currentUserId, context)); DiscoverPostStore.save(context) }
            }
        }
        val requestFeed = {
            socket?.emit("discover_posts_request", JSONObject(), Ack { args ->
                val response = args.firstOrNull() as? JSONObject ?: return@Ack
                if (!response.optBoolean("ok", false)) return@Ack
                val array = response.optJSONArray("posts") ?: JSONArray()
                mainHandler.post {
                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        runCatching { DiscoverPostStore.upsert(DiscoverPostStore.fromJson(item, currentUserId, context)) }
                    }
                    DiscoverPostStore.save(context)
                }
            })
        }
        val onConnect = io.socket.emitter.Emitter.Listener { requestFeed() }
        socket?.on("connect", onConnect)
        socket?.on("discover_post_created", onCreated)
        socket?.on("discover_post_updated", onUpdated)
        if (socket?.connected() == true) requestFeed() else socket?.connect()
        onDispose {
            socket?.off("connect", onConnect)
            socket?.off("discover_post_created", onCreated)
            socket?.off("discover_post_updated", onUpdated)
            socket?.disconnect()
        }
    }

    // Mine tab shows ONLY posts created by the current user.
    val visiblePosts = when (selectedTab) {
        "Mine" -> DiscoverPostStore.posts.filter { it.isMine && it.authorId == currentUserId }
        else -> DiscoverPostStore.posts.toList()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            // ═══════════ SAME HOMEPAGE BLUE TOP HEADER & STATUS BAR AREA ═══════════
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DiscoverTopGradient)
                    .padding(top = (WindowInsets.statusBars.asPaddingValues().calculateTopPadding() - 16.dp).coerceAtLeast(0.dp))
                    .padding(top = 0.dp, start = 12.dp, end = 12.dp, bottom = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Mine", "Discover").forEach { item ->
                            val isSelected = selectedTab == item
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(
                                        interactionSource = null,
                                        indication = null
                                    ) { selectedTab = item }
                                    .padding(horizontal = 4.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = item,
                                    fontSize = 18.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF1E1E1E) else Color(0xFF6B7280)
                                )
                            }
                        }
                    }
                }
            }

            // ═══════════ POSTS FEED ═══════════
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                contentPadding = PaddingValues(top = 10.dp, bottom = 110.dp)
            ) {
                items(
                    items = visiblePosts,
                    key = { it.id }
                ) { post ->
                    DiscoverFeedCard(
                        post = post,
                        onToggleLike = {
                            val idx = DiscoverPostStore.posts.indexOfFirst { it.id == post.id }
                            if (idx >= 0) {
                                val current = DiscoverPostStore.posts[idx]
                                val nextLiked = !current.liked
                                val updated = current.copy(
                                    liked = nextLiked,
                                    likeCount = (current.likeCount + if (nextLiked) 1 else -1).coerceAtLeast(0)
                                )
                                DiscoverPostStore.posts[idx] = updated
                                DiscoverPostStore.save(context)
                                discoverSocket?.emit("discover_post_like", JSONObject()
                                    .put("postId", current.id).put("userId", currentUserId)
                                    .put("liked", nextLiked))
                            }
                        },
                        onOpenComments = {
                            activeCommentPostId = post.id
                        },
                        onMoreClick = {}
                    )
                }
            }
        }

        // ═══════════ BOTTOM RIGHT 3D BLUE PENCIL FAB (EXACT HOMEPAGE IMAGE SPACE) ═══════════
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 110.dp)
        ) {
            DiscoverPencilFab3D(
                onClick = { showCreatePost = true }
            )
        }

        // ═══════════ FULL SCREEN UPLOAD POST PAGE (EDGE-TO-EDGE WHITE, NO BLACK TOP) ═══════════
        if (showCreatePost) {
            DiscoverCreatePostFullScreen(
                onDismiss = { showCreatePost = false },
                onPublish = { postText, imagePaths ->
                    val postId = "post_${currentUserId}_${System.currentTimeMillis()}"
                    val createdAt = System.currentTimeMillis()
                    val localPost = DiscoverPostItem(
                        id = postId, authorId = currentUserId, authorName = currentUserName,
                        authorAvatar = currentUserAvatar, authorGender = currentUserGender,
                        text = postText, images = imagePaths, createdAt = createdAt,
                        likeCount = 0, liked = false, isMine = true, comments = emptyList()
                    )
                    DiscoverPostStore.upsert(localPost)
                    DiscoverPostStore.save(context)
                    scope.launch {
                        val encodedImages = withContext(Dispatchers.IO) {
                            imagePaths.mapNotNull { discoverEncodeImage(it) }.take(4)
                        }
                        val payload = JSONObject().put("id", postId).put("authorId", currentUserId)
                            .put("authorName", currentUserName).put("authorAvatar", currentUserAvatar)
                            .put("authorGender", currentUserGender).put("text", postText)
                            .put("createdAt", createdAt).put("imagesBase64", JSONArray(encodedImages))
                        discoverSocket?.emit("discover_post_publish", payload, Ack { args ->
                            val response = args.firstOrNull() as? JSONObject
                            val serverPost = response?.optJSONObject("post")
                            if (response?.optBoolean("ok") == true && serverPost != null) {
                                mainHandler.post {
                                    runCatching {
                                        DiscoverPostStore.upsert(DiscoverPostStore.fromJson(serverPost, currentUserId, context))
                                        DiscoverPostStore.save(context)
                                    }
                                }
                            }
                        })
                    }
                    showCreatePost = false
                }
            )
        }

        // ═══════════ 60VH COMMENT BOTTOM SHEET ═══════════
        val commentTargetPost = activeCommentPostId?.let { id ->
            DiscoverPostStore.posts.firstOrNull { it.id == id }
        }
        if (commentTargetPost != null) {
            DiscoverCommentBottomSheet(
                comments = commentTargetPost.comments,
                onDismiss = { activeCommentPostId = null },
                onSendComment = { commentText ->
                    val idx = DiscoverPostStore.posts.indexOfFirst { it.id == commentTargetPost.id }
                    if (idx >= 0) {
                        val current = DiscoverPostStore.posts[idx]
                        val newComment = DiscoverCommentItem(
                            id = "cmt_${System.currentTimeMillis()}",
                            authorName = currentUserName,
                            authorAvatar = currentUserAvatar,
                            text = commentText,
                            createdAt = System.currentTimeMillis()
                        )
                        DiscoverPostStore.posts[idx] = current.copy(
                            comments = current.comments + newComment
                        )
                        DiscoverPostStore.save(context)
                        discoverSocket?.emit("discover_post_comment", JSONObject()
                            .put("postId", current.id).put("id", newComment.id)
                            .put("authorId", currentUserId).put("authorName", currentUserName)
                            .put("authorAvatar", currentUserAvatar).put("text", commentText)
                            .put("createdAt", newComment.createdAt))
                    }
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// FEED POST CARD (AVATAR TOP ROW + CONTENT BELOW AVATAR LEFT CORNER)
// ═══════════════════════════════════════════════════════
@Composable
private fun DiscoverFeedCard(
    post: DiscoverPostItem,
    onToggleLike: () -> Unit,
    onOpenComments: () -> Unit,
    onMoreClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // ── Top Row: Avatar + Black Username + Gender Tag + 3 Vertical Dots ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (post.authorAvatar.isBlank()) {
                DefaultUserAvatarIcon(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                )
            } else {
                AsyncImage(
                    model = post.authorAvatar,
                    contentDescription = post.authorName,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE5E7EB)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.width(10.dp))

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = post.authorName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(Modifier.width(6.dp))

                DiscoverGenderTag(gender = post.authorGender)
            }

            Spacer(Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        interactionSource = null,
                        indication = null
                    ) { onMoreClick() },
                contentAlignment = Alignment.Center
            ) {
                DiscoverThreeVerticalDotsIcon(
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // ── Below Avatar (Left Corner Aligned): Text, Images, Time + Like + Comment ──
        if (post.text.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = post.text,
                fontSize = 14.sp,
                color = Color(0xFF374151),
                lineHeight = 19.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (post.images.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            DiscoverPostImageGrid(images = post.images)
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatDiscoverTime(post.createdAt),
                fontSize = 12.sp,
                color = Color(0xFF9CA3AF)
            )
            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .clickable(
                        interactionSource = null,
                        indication = null
                    ) { onToggleLike() }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                DiscoverHeartIcon(
                    liked = post.liked,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = post.likeCount.toString(),
                    fontSize = 13.sp,
                    color = if (post.liked) Color(0xFFEF4444) else Color(0xFF9CA3AF)
                )
            }

            Spacer(Modifier.width(20.dp))

            Row(
                modifier = Modifier
                    .clickable(
                        interactionSource = null,
                        indication = null
                    ) { onOpenComments() }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                DiscoverCommentIcon(
                    modifier = Modifier.size(20.dp)
                )
                if (post.comments.isNotEmpty()) {
                    Text(
                        text = post.comments.size.toString(),
                        fontSize = 13.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.8.dp)
                .background(Color(0xFFF1F5F9))
        )
    }
}

@Composable
private fun DiscoverGenderTag(gender: String) {
    // Match the PublicProfile gender tag exactly: same gender normalization,
    // circular 20dp badge, colors, and shared gender icon.
    val genderStr = gender.trim().ifEmpty { "Male" }
    val isFemale = genderStr.equals("female", ignoreCase = true)

    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(if (isFemale) Color(0xFFFF5874) else Color(0xFF3B82F6)),
        contentAlignment = Alignment.Center
    ) {
        CustomGenderIcon(isFemale = isFemale, modifier = Modifier.size(10.dp))
    }
}

// ═══════════════════════════════════════════════════════
// 60VH COMMENT BOTTOM SHEET
// ═══════════════════════════════════════════════════════
@Composable
private fun DiscoverCommentBottomSheet(
    comments: List<DiscoverCommentItem>,
    onDismiss: () -> Unit,
    onSendComment: (String) -> Unit
) {
    var commentInput by remember { mutableStateOf("") }
    val sheetHeight = (LocalConfiguration.current.screenHeightDp * 0.60f).dp

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val view = LocalView.current
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.let { window ->
                window.setDimAmount(0f)
                window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                @Suppress("DEPRECATION")
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                @Suppress("DEPRECATION")
                window.statusBarColor = AndroidColor.TRANSPARENT
                @Suppress("DEPRECATION")
                window.navigationBarColor = AndroidColor.TRANSPARENT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val params = window.attributes
                    params.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    window.attributes = params
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f))
                .clickable(
                    interactionSource = null,
                    indication = null
                ) { onDismiss() }
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(sheetHeight)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(Color.White)
                    .clickable(
                        interactionSource = null,
                        indication = null
                    ) {}
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // ── Top Heading: Comment ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Comment",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.8.dp)
                        .background(Color(0xFFF1F5F9))
                )

                // ── Comments List ──
                if (comments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No comments yet",
                            fontSize = 13.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = comments,
                            key = { it.id }
                        ) { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                if (item.authorAvatar.isBlank()) {
                                    DefaultUserAvatarIcon(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    AsyncImage(
                                        model = item.authorAvatar,
                                        contentDescription = item.authorName,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE5E7EB)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = item.authorName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF374151)
                                        )
                                        Text(
                                            text = formatDiscoverTime(item.createdAt),
                                            fontSize = 11.sp,
                                            color = Color(0xFF9CA3AF)
                                        )
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = item.text,
                                        fontSize = 14.sp,
                                        color = Color(0xFF1F2937),
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.8.dp)
                        .background(Color(0xFFF1F5F9))
                )

                // ── Bottom Bar: Slim Full-Round Grey Input (Left) + Blue Circle White Send Button (Right Outside) ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFF3F4F6))
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = commentInput,
                            onValueChange = { commentInput = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(
                                color = Color(0xFF1F2937),
                                fontSize = 13.sp
                            ),
                            cursorBrush = SolidColor(Color(0xFF2563EB)),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (commentInput.isEmpty()) {
                                        Text(
                                            text = "Write a comment...",
                                            color = Color(0xFF9CA3AF),
                                            fontSize = 13.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2563EB))
                            .clickable(
                                interactionSource = null,
                                indication = null
                            ) {
                                val trimmed = commentInput.trim()
                                if (trimmed.isNotEmpty()) {
                                    onSendComment(trimmed)
                                    commentInput = ""
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        DiscoverWhiteSendIcon(modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverPostImageGrid(images: List<String>) {
    if (images.size == 1) {
        AsyncImage(
            model = images[0],
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF3F4F6)),
            contentScale = ContentScale.Crop
        )
    } else {
        val rows = images.chunked(2)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rows.forEach { rowImages ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowImages.forEach { imgPath ->
                        AsyncImage(
                            model = imgPath,
                            contentDescription = null,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF3F4F6)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (rowImages.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun formatDiscoverTime(createdAt: Long): String {
    val diffSec = ((System.currentTimeMillis() - createdAt) / 1000L).coerceAtLeast(0L)
    return when {
        diffSec < 60L -> "Just now"
        diffSec < 3600L -> "${diffSec / 60L} mins ago"
        diffSec < 86400L -> "${diffSec / 3600L} hours ago"
        else -> "${diffSec / 86400L} days ago"
    }
}

// ═══════════════════════════════════════════════════════
// FULL-SCREEN UPLOAD POST PAGE (TRUE EDGE-TO-EDGE WHITE, NO BLACK TOP)
// ═══════════════════════════════════════════════════════
@Composable
private fun DiscoverCreatePostFullScreen(
    onDismiss: () -> Unit,
    onPublish: (String, List<String>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var postText by remember { mutableStateOf("") }
    val selectedImages = remember { mutableStateListOf<String>() }
    var showLimitPopup by remember { mutableStateOf(false) }
    var popupTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(popupTrigger) {
        if (popupTrigger > 0) {
            showLimitPopup = true
            delay(1800L)
            showLimitPopup = false
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            val remaining = (4 - selectedImages.size).coerceAtLeast(0)
            if (remaining <= 0) return@launch
            val savedPaths = withContext(Dispatchers.IO) {
                uris.take(remaining).mapIndexedNotNull { idx, uri ->
                    runCatching {
                        val outFile = File(
                            context.filesDir,
                            "discover_img_${System.currentTimeMillis()}_$idx.jpg"
                        )
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            outFile.outputStream().use { output -> input.copyTo(output) }
                        }
                        outFile.absolutePath
                    }.getOrNull()
                }
            }
            selectedImages.addAll(savedPaths)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val view = LocalView.current
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.let { window ->
                window.setDimAmount(0f)
                window.setBackgroundDrawableResource(android.R.color.white)
                window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                @Suppress("DEPRECATION")
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                @Suppress("DEPRECATION")
                window.statusBarColor = AndroidColor.TRANSPARENT
                @Suppress("DEPRECATION")
                window.navigationBarColor = AndroidColor.WHITE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val params = window.attributes
                    params.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    window.attributes = params
                }
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = true
                    isAppearanceLightNavigationBars = true
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // ── Top Bar: Back Arrow Left + Middle Heading "Upload" + Small 3D Blue Post Pill Right ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = null,
                                indication = null
                            ) { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        DiscoverBackArrowIcon(modifier = Modifier.size(24.dp))
                    }

                    Text(
                        text = "Upload",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                        DiscoverPostBluePill3D(
                            enabled = postText.isNotBlank() || selectedImages.isNotEmpty(),
                            onClick = {
                                if (postText.isNotBlank() || selectedImages.isNotEmpty()) {
                                    onPublish(postText.trim(), selectedImages.toList())
                                }
                            }
                        )
                    }
                }

                // ── Title: Image (Left Corner Before + Card) ──
                Text(
                    text = "Image",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937),
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                )

                // ── Grey Square Card (+) with 12dp Corner Radius ──
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(selectedImages) { imagePath ->
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF3F4F6))
                        ) {
                            AsyncImage(
                                model = imagePath,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .clickable(
                                        interactionSource = null,
                                        indication = null
                                    ) { selectedImages.remove(imagePath) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "×",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (selectedImages.size < 4) {
                        item {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF3F4F6))
                                    .clickable { imagePickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(Modifier.size(30.dp)) {
                                    val stroke = 2.6.dp.toPx()
                                    val c = Color(0xFF9CA3AF)
                                    drawLine(
                                        color = c,
                                        start = Offset(size.width / 2f, 2.dp.toPx()),
                                        end = Offset(size.width / 2f, size.height - 2.dp.toPx()),
                                        strokeWidth = stroke,
                                        cap = StrokeCap.Round
                                    )
                                    drawLine(
                                        color = c,
                                        start = Offset(2.dp.toPx(), size.height / 2f),
                                        end = Offset(size.width - 2.dp.toPx(), size.height / 2f),
                                        strokeWidth = stroke,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Title: Description (Left Corner Above Input Box) ──
                Text(
                    text = "Description",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937),
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )

                // ── Large Grey Input Square Box with 0/300 Counter ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(210.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF3F4F6))
                        .padding(14.dp)
                ) {
                    BasicTextField(
                        value = postText,
                        onValueChange = { incoming ->
                            if (incoming.length > 300) {
                                postText = incoming.take(300)
                                popupTrigger++
                            } else {
                                postText = incoming
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 22.dp),
                        textStyle = TextStyle(
                            color = Color(0xFF1F2937),
                            fontSize = 15.sp,
                            lineHeight = 21.sp
                        ),
                        cursorBrush = SolidColor(Color(0xFF2563EB)),
                        decorationBox = { innerTextField ->
                            Box(Modifier.fillMaxSize()) {
                                if (postText.isEmpty()) {
                                    Text(
                                        text = "Say something...",
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    Text(
                        text = "${postText.length}/300",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF9CA3AF),
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
            }

            // ── Small Centered Black/70 Popup Card (> 300 Limit) ──
            AnimatedVisibility(
                visible = showLimitPopup,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.70f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Not more then 300 words",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// 3D BLUE PENCIL FAB
// ═══════════════════════════════════════════════════════
@Composable
private fun DiscoverPencilFab3D(
    onClick: () -> Unit
) {
    val blue3DBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF60A5FA),
            Color(0xFF3B82F6),
            Color(0xFF1D4ED8)
        ),
        start = Offset(90f, 0f),
        end = Offset(0f, 120f)
    )

    Box(
        modifier = Modifier
            .size(56.dp)
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                ambientColor = Color(0xFF2563EB),
                spotColor = Color(0xFF1D4ED8)
            )
            .clip(CircleShape)
            .background(blue3DBrush)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.55f),
                        Color(0xFF1E3A8A).copy(alpha = 0.30f)
                    )
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = null,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawOval(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.34f),
                        Color.Transparent
                    ),
                    startY = size.height * 0.04f,
                    endY = size.height * 0.45f
                ),
                topLeft = Offset(size.width * 0.14f, size.height * 0.05f),
                size = Size(size.width * 0.72f, size.height * 0.38f)
            )

            drawCircle(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF1E3A8A).copy(alpha = 0.28f)
                    ),
                    startY = size.height * 0.6f,
                    endY = size.height
                ),
                radius = size.minDimension / 2f,
                center = center
            )

            val s = size.minDimension / 56f
            rotate(degrees = 45f, pivot = center) {
                val pencilWidth = 7.4f * s
                val left = center.x - pencilWidth / 2f
                val shadowOffset = 1.4f * s
                val bodyTop = center.y - 6.5f * s
                val bodyHeight = 14.2f * s
                val eraserTop = center.y - 13.8f * s
                val eraserHeight = 5.0f * s

                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.14f),
                    topLeft = Offset(left + shadowOffset, eraserTop + shadowOffset),
                    size = Size(pencilWidth, eraserHeight),
                    cornerRadius = CornerRadius(3.2f * s, 3.2f * s)
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(left, eraserTop),
                    size = Size(pencilWidth, eraserHeight),
                    cornerRadius = CornerRadius(3.2f * s, 3.2f * s)
                )

                val tipPath = Path().apply {
                    moveTo(left, bodyTop)
                    lineTo(left + pencilWidth, bodyTop)
                    lineTo(left + pencilWidth, bodyTop + bodyHeight)
                    lineTo(center.x, bodyTop + bodyHeight + 6.4f * s)
                    lineTo(left, bodyTop + bodyHeight)
                    close()
                }
                val shadowPath = Path().apply {
                    moveTo(left + shadowOffset, bodyTop + shadowOffset)
                    lineTo(left + pencilWidth + shadowOffset, bodyTop + shadowOffset)
                    lineTo(left + pencilWidth + shadowOffset, bodyTop + bodyHeight + shadowOffset)
                    lineTo(center.x + shadowOffset, bodyTop + bodyHeight + 6.4f * s + shadowOffset)
                    lineTo(left + shadowOffset, bodyTop + bodyHeight + shadowOffset)
                    close()
                }
                drawPath(path = shadowPath, color = Color.Black.copy(alpha = 0.14f))
                drawPath(path = tipPath, color = Color.White)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// COMPACT 3D BLUE POST PILL BUTTON (TOP RIGHT)
// ═══════════════════════════════════════════════════════
@Composable
private fun DiscoverPostBluePill3D(
    enabled: Boolean,
    onClick: () -> Unit
) {
    val pillBrush = if (enabled) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF60A5FA),
                Color(0xFF3B82F6),
                Color(0xFF1D4ED8)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF93C5FD),
                Color(0xFF60A5FA),
                Color(0xFF3B82F6)
            )
        )
    }

    Box(
        modifier = Modifier
            .shadow(
                elevation = 5.dp,
                shape = RoundedCornerShape(50),
                ambientColor = Color(0xFF2563EB),
                spotColor = Color(0xFF1D4ED8)
            )
            .clip(RoundedCornerShape(50))
            .background(pillBrush)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.65f),
                        Color(0xFF1E3A8A).copy(alpha = 0.35f)
                    )
                ),
                shape = RoundedCornerShape(50)
            )
            .clickable(
                interactionSource = null,
                indication = null
            ) { onClick() }
            .padding(horizontal = 13.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Post",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ═══════════════════════════════════════════════════════
// VECTOR CANVAS ICONS
// ═══════════════════════════════════════════════════════
@Composable
private fun DiscoverThreeVerticalDotsIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val dotColor = Color(0xFF6B7280)
        val radius = 1.6.dp.toPx()
        val cx = size.width / 2f
        drawCircle(color = dotColor, radius = radius, center = Offset(cx, size.height * 0.22f))
        drawCircle(color = dotColor, radius = radius, center = Offset(cx, size.height * 0.50f))
        drawCircle(color = dotColor, radius = radius, center = Offset(cx, size.height * 0.78f))
    }
}

@Composable
private fun DiscoverWhiteSendIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.12f, h * 0.16f)
            lineTo(w * 0.92f, h * 0.50f)
            lineTo(w * 0.12f, h * 0.84f)
            lineTo(w * 0.28f, h * 0.54f)
            lineTo(w * 0.62f, h * 0.50f)
            lineTo(w * 0.28f, h * 0.46f)
            close()
        }
        drawPath(path = path, color = Color.White)
    }
}

@Composable
private fun DiscoverBackArrowIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = 2.3.dp.toPx()
        val color = Color(0xFF1F2937)
        val midY = size.height / 2f
        val leftX = size.width * 0.20f
        val rightX = size.width * 0.82f

        drawLine(
            color = color,
            start = Offset(rightX, midY),
            end = Offset(leftX, midY),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        val head = Path().apply {
            moveTo(leftX + size.width * 0.26f, midY - size.height * 0.26f)
            lineTo(leftX, midY)
            lineTo(leftX + size.width * 0.26f, midY + size.height * 0.26f)
        }
        drawPath(
            path = head,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun DiscoverHeartIcon(
    liked: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.50f, h * 0.86f)
            cubicTo(w * 0.14f, h * 0.62f, w * 0.06f, h * 0.36f, w * 0.18f, h * 0.20f)
            cubicTo(w * 0.29f, h * 0.07f, w * 0.45f, h * 0.14f, w * 0.50f, h * 0.26f)
            cubicTo(w * 0.55f, h * 0.14f, w * 0.71f, h * 0.07f, w * 0.82f, h * 0.20f)
            cubicTo(w * 0.94f, h * 0.36f, w * 0.86f, h * 0.62f, w * 0.50f, h * 0.86f)
            close()
        }
        if (liked) {
            drawPath(path = path, color = Color(0xFFEF4444))
        } else {
            drawPath(
                path = path,
                color = Color(0xFF9CA3AF),
                style = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

@Composable
private fun DiscoverCommentIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.7.dp.toPx()
        val path = Path().apply {
            moveTo(w * 0.50f, h * 0.14f)
            cubicTo(w * 0.76f, h * 0.14f, w * 0.92f, h * 0.29f, w * 0.92f, h * 0.48f)
            cubicTo(w * 0.92f, h * 0.67f, w * 0.75f, h * 0.81f, w * 0.50f, h * 0.81f)
            cubicTo(w * 0.42f, h * 0.81f, w * 0.34f, h * 0.79f, w * 0.28f, h * 0.76f)
            lineTo(w * 0.12f, h * 0.86f)
            lineTo(w * 0.18f, h * 0.68f)
            cubicTo(w * 0.11f, h * 0.62f, w * 0.08f, h * 0.55f, w * 0.08f, h * 0.48f)
            cubicTo(w * 0.08f, h * 0.29f, w * 0.24f, h * 0.14f, w * 0.50f, h * 0.14f)
            close()
        }
        drawPath(
            path = path,
            color = Color(0xFF9CA3AF),
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
