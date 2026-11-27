package com.hurry.voicechat

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
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
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val text: String,
    val images: List<String>,
    val createdAt: Long,
    val likeCount: Int = 0,
    val liked: Boolean = false,
    val isMine: Boolean = true,
    val isFollowed: Boolean = false,
    val comments: List<DiscoverCommentItem> = emptyList()
)

private object DiscoverPostStore {
    val posts = mutableStateListOf<DiscoverPostItem>()
}

@Composable
fun DiscoverPage(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
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

    // Direct state read so like/comment/follow updates recompose immediately.
    // Mine tab shows ONLY posts created by the current user.
    val visiblePosts = when (selectedTab) {
        "Mine" -> DiscoverPostStore.posts.filter { it.isMine && it.authorId == currentUserId }
        "Following" -> DiscoverPostStore.posts.filter { it.isFollowed }
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
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Mine", "Following", "Discover").forEach { item ->
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
                                    fontSize = 21.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF1E1E1E) else Color(0xFF6B7280)
                                )
                            }
                        }
                    }
                }
            }

            // ═══════════ POSTS FEED (2ND IMAGE STYLE CARD) ═══════════
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
                                DiscoverPostStore.posts[idx] = current.copy(
                                    liked = nextLiked,
                                    likeCount = (current.likeCount + if (nextLiked) 1 else -1).coerceAtLeast(0)
                                )
                            }
                        },
                        onOpenComments = {
                            activeCommentPostId = post.id
                        },
                        onToggleFollow = {
                            val idx = DiscoverPostStore.posts.indexOfFirst { it.id == post.id }
                            if (idx >= 0) {
                                val current = DiscoverPostStore.posts[idx]
                                DiscoverPostStore.posts[idx] = current.copy(
                                    isFollowed = !current.isFollowed
                                )
                            }
                        },
                        onMoreClick = {}
                    )
                }
            }
        }

        // ═══════════ BOTTOM RIGHT 3D PENCIL FAB (35DP FROM BOTTOM) ═══════════
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 35.dp)
        ) {
            DiscoverPencilFab3D(
                onClick = { showCreatePost = true }
            )
        }

        // ═══════════ FULL SCREEN CREATE POST PAGE (NO TOP GAP) ═══════════
        if (showCreatePost) {
            DiscoverCreatePostFullScreen(
                onDismiss = { showCreatePost = false },
                onPublish = { postText, imagePaths ->
                    DiscoverPostStore.posts.add(
                        0,
                        DiscoverPostItem(
                            id = "post_${System.currentTimeMillis()}",
                            authorId = currentUserId,
                            authorName = currentUserName,
                            authorAvatar = currentUserAvatar,
                            text = postText,
                            images = imagePaths,
                            createdAt = System.currentTimeMillis(),
                            likeCount = 0,
                            liked = false,
                            isMine = true,
                            isFollowed = false,
                            comments = emptyList()
                        )
                    )
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
                    }
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// FEED POST CARD (EXACT 2ND IMAGE LAYOUT + BLUE + PILL + 3 DOTS)
// ═══════════════════════════════════════════════════════
@Composable
private fun DiscoverFeedCard(
    post: DiscoverPostItem,
    onToggleLike: () -> Unit,
    onOpenComments: () -> Unit,
    onToggleFollow: () -> Unit,
    onMoreClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Left Circular User Avatar
            if (post.authorAvatar.isBlank()) {
                DefaultUserAvatarIcon(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                )
            } else {
                AsyncImage(
                    model = post.authorAvatar,
                    contentDescription = post.authorName,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE5E7EB)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.width(10.dp))

            // Right Content Column (Username + Blue + Pill + 3 Dots + Text + Images + Time/Like/Comment)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = post.authorName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(Modifier.width(8.dp))

                    // Exact Pill Outline (+) Icon in Blue Color
                    DiscoverBluePlusPillIcon(
                        followed = post.isFollowed,
                        onClick = onToggleFollow
                    )

                    Spacer(Modifier.width(6.dp))

                    // 3 Vertical Dots Right After (+)
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

                if (post.text.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = post.text,
                        fontSize = 14.sp,
                        color = Color(0xFF374151),
                        lineHeight = 19.sp
                    )
                }

                if (post.images.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    DiscoverPostImageGrid(images = post.images)
                }

                Spacer(Modifier.height(10.dp))

                // Bottom Bar: Time Left, Heart + Count + Comment Right
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
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.let { window ->
                window.setDimAmount(0f)
                window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
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

                    // Outside Input Right Corner: Blue Circle + White Send Icon
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
// FULL-SCREEN CREATE POST SHEET (NO GAP FROM TOP)
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
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.let { window ->
                window.setDimAmount(0f)
                window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
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
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // ── Top Bar: Back Arrow Left + Small 3D Blue Post Pill Right ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
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

                    Spacer(Modifier.weight(1f))

                    DiscoverPostBluePill3D(
                        enabled = postText.isNotBlank() || selectedImages.isNotEmpty(),
                        onClick = {
                            if (postText.isNotBlank() || selectedImages.isNotEmpty()) {
                                onPublish(postText.trim(), selectedImages.toList())
                            }
                        }
                    )
                }

                // ── Below Back Icon: Grey Square Card (+) with 12dp Corner Radius ──
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
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
// 3D GREEN PENCIL FAB (EXACT 1ST IMAGE STYLE)
// ═══════════════════════════════════════════════════════
@Composable
private fun DiscoverPencilFab3D(
    onClick: () -> Unit
) {
    val green3DBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF55E875),
            Color(0xFF2CD39B),
            Color(0xFF14B8A6)
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
                ambientColor = Color(0xFF22C55E),
                spotColor = Color(0xFF14B8A6)
            )
            .clip(CircleShape)
            .background(green3DBrush)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.55f),
                        Color(0xFF0F766E).copy(alpha = 0.25f)
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
                        Color(0xFF0F766E).copy(alpha = 0.24f)
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
                    color = Color.Black.copy(alpha = 0.12f),
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
                drawPath(path = shadowPath, color = Color.Black.copy(alpha = 0.12f))
                drawPath(path = tipPath, color = Color.White)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// SMALL 3D BLUE POST PILL BUTTON (TOP RIGHT)
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
                elevation = 7.dp,
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
            .padding(horizontal = 18.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Post",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ═══════════════════════════════════════════════════════
// VECTOR CANVAS ICONS
// ═══════════════════════════════════════════════════════
@Composable
private fun DiscoverBluePlusPillIcon(
    followed: Boolean,
    onClick: () -> Unit
) {
    val blueColor = Color(0xFF3B82F6)
    Box(
        modifier = Modifier
            .width(38.dp)
            .height(22.dp)
            .clip(RoundedCornerShape(50))
            .background(if (followed) blueColor.copy(alpha = 0.12f) else Color.Transparent)
            .border(1.6.dp, blueColor, RoundedCornerShape(50))
            .clickable(
                interactionSource = null,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(11.dp)) {
            val stroke = 1.8.dp.toPx()
            if (followed) {
                val checkPath = Path().apply {
                    moveTo(size.width * 0.12f, size.height * 0.54f)
                    lineTo(size.width * 0.42f, size.height * 0.82f)
                    lineTo(size.width * 0.90f, size.height * 0.22f)
                }
                drawPath(
                    path = checkPath,
                    color = blueColor,
                    style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            } else {
                drawLine(
                    color = blueColor,
                    start = Offset(size.width / 2f, size.height * 0.08f),
                    end = Offset(size.width / 2f, size.height * 0.92f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = blueColor,
                    start = Offset(size.width * 0.08f, size.height / 2f),
                    end = Offset(size.width * 0.92f, size.height / 2f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

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
