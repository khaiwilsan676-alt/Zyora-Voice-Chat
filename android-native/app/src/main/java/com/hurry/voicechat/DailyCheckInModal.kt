package com.hurry.voicechat

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage

private const val RAW_URL = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/"

// Helper property to fix image URLs
private val String.fullUrl: String
    get() = if (this.startsWith("http")) this else RAW_URL + this.removePrefix("/")

@Composable
fun DailyCheckInModal(
    isOpen: Boolean,
    onClose: () -> Unit,
    currentDay: Int,
    onSignIn: () -> Unit,
    claimedToday: Boolean = false
) {
    if (!isOpen) return

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        // Modal ko center se thoda niche (0.15f bias) align kiya hai 
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose 
                ),
            contentAlignment = BiasAlignment(0f, 0.15f) 
        ) {
            
            // Ye Column card aur tere bottom wale close button ko stack karega
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                
                // Box jo Banner aur Card ko overlap karwane ke liye hai
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 1. Naya SVGA Style Outer Card
                    Column(
                        modifier = Modifier
                            .padding(top = 50.dp) // Banner ke overlap ke liye space
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color.White)
                            .border(6.dp, Color(0xFFFFE57C), RoundedCornerShape(32.dp)) // SVG wala golden border
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {} // Card par click se dismiss na ho
                            )
                            .padding(start = 20.dp, end = 20.dp, top = 60.dp, bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        
                        // Subtitle (Sign in for 7 days for rich rewards)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFF3C9))
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sign in for 7 days for rich rewards",
                                color = Color(0xFF80622E),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Row 1: Days 1 to 4
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DayCard(day = 1, reward = "+5000", image = "file_00000000e56882119c217d508b6733dc.png", currentDay = currentDay, modifier = Modifier.weight(1f))
                            DayCard(day = 2, reward = "+5000", image = "file_00000000e56882119c217d508b6733dc.png", currentDay = currentDay, modifier = Modifier.weight(1f))
                            DayCard(day = 3, reward = "×2 Days", image = "file_00000000d808821186c1b7b612eea3fc.png", currentDay = currentDay, modifier = Modifier.weight(1f))
                            DayCard(day = 4, reward = "+10,000", image = "file_00000000e56882119c217d508b6733dc.png", currentDay = currentDay, modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Row 2: Days 5 to 6
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SpecialDayCard(day = 5, currentDay = currentDay, modifier = Modifier.weight(1f)) {
                                renderDay5Special()
                            }
                            SpecialDayCard(day = 6, currentDay = currentDay, modifier = Modifier.weight(1f)) {
                                renderDay6Special()
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Row 3: Day 7 (Big Reward)
                        Day7Card(currentDay = currentDay)

                        Spacer(modifier = Modifier.height(24.dp))

                        // Sign-in Button (SVG wala Gold Gradient)
                        val isButtonDisabled = claimedToday || currentDay > 7
                        val buttonBrush = if (isButtonDisabled) {
                            Brush.verticalGradient(listOf(Color(0xFF9CA3AF), Color(0xFF6B7280)))
                        } else {
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFFFFF080),
                                    Color(0xFFFFD633),
                                    Color(0xFFFFBB00),
                                    Color(0xFFE99A00)
                                )
                            )
                        }

                        Button(
                            onClick = onSignIn,
                            enabled = !isButtonDisabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent, 
                                disabledContainerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues(),
                            shape = RoundedCornerShape(50.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(buttonBrush, RoundedCornerShape(50.dp))
                        ) {
                            Text(
                                text = if (claimedToday) "Claimed Today" else if (currentDay > 7) "All Rewards Claimed!" else "Sign in",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // 2. BANNER IMAGE - Ye offset wala banner wahi hai jisko tu aram se adjust kar sakta hai
                    AsyncImage(
                        model = "file_000000004b6c8211855003bf899492fd.png".fullUrl,
                        contentDescription = "Top Banner",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .align(Alignment.TopCenter)
                            .offset(y = (-5).dp) // <--- Yahan se Upar/Niche manage kar lena
                            .zIndex(2f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 3. Tere Purane Code Wala CLOSE (X) BUTTON - Card se bahar, bottom me!
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// ----------------- Helper UI Components (SVG styled) -----------------

@Composable
private fun DayCard(day: Int, reward: String, image: String, currentDay: Int, modifier: Modifier = Modifier) {
    val borderColor = getBorderColor(day, currentDay)
    
    Box(
        modifier = modifier
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF3F3F3))
            .border(2.dp, borderColor, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        DayBadge(day = day)
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            AsyncImage(
                model = image.fullUrl,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = reward,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF544B00),
                maxLines = 1
            )
        }
        
        if (day < currentDay) {
            CompletedCheckmark()
        }
    }
}

@Composable
private fun SpecialDayCard(day: Int, currentDay: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val borderColor = getBorderColor(day, currentDay)

    Box(
        modifier = modifier
            .height(90.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF3F3F3))
            .border(2.dp, borderColor, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        DayBadge(day = day)
        
        Box(modifier = Modifier.padding(top = 16.dp)) {
            content()
        }
        
        if (day < currentDay) {
            CompletedCheckmark()
        }
    }
}

@Composable
private fun Day7Card(currentDay: Int) {
    val borderColor = getBorderColor(7, currentDay)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFFF9EB))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        // SVG Day 7 Badge Top-Left Corner 
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .clip(RoundedCornerShape(topStart = 16.dp, bottomEnd = 16.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFFFF5038), Color(0xFFFFB62B))))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "7",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Big Reward",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(modifier = Modifier.padding(top = 12.dp, start = 16.dp)) {
            renderDay7Special()
        }

        if (7 < currentDay) {
            CompletedCheckmark()
        }
    }
}

// ----------------- Internal Render Functions -----------------

@Composable
private fun renderDay5Special() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        RewardItemIcon("file_00000000e56882119c217d508b6733dc.png", "+10,000", 32.dp)
        RewardItemIcon("IMG_20260903_141944.png", "×1 Day", 40.dp)
    }
}

@Composable
private fun renderDay6Special() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        RewardItemIcon("file_00000000e56882119c217d508b6733dc.png", "+10,000", 32.dp)
        RewardItemIcon("IMG-20260903-WA0076.jpg", "×2 Days", 32.dp)
    }
}

@Composable
private fun renderDay7Special() {
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
        RewardItemIcon("file_00000000e56882119c217d508b6733dc.png", "+15,000", 40.dp)
        RewardItemIcon("file_0000000044388211996656afc9ce9c03.png", "×3 days", 48.dp)
        RewardItemIcon("IMG-20260903-WA0077.jpg", "×3 days", 48.dp)
    }
}

@Composable
private fun RewardItemIcon(image: String, text: String, size: androidx.compose.ui.unit.Dp) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(
            model = image.fullUrl,
            contentDescription = null,
            modifier = Modifier.size(size),
            contentScale = ContentScale.Fit
        )
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF544B00), 
            textAlign = TextAlign.Center
        )
    }
}

// ----------------- Reusable UI Bits -----------------

@Composable
private fun DayBadge(day: Int) {
    Box(
        modifier = Modifier
            .absoluteOffset(x = 0.dp, y = 0.dp)
            .size(width = 30.dp, height = 24.dp)
            .clip(RoundedCornerShape(topStart = 12.dp, bottomEnd = 12.dp))
            .background(Color(0xFFFFE000)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            color = Color(0xFF544B00),
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun BoxScope.CompletedCheckmark() {
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(4.dp)
            .size(18.dp)
            .clip(CircleShape)
            .background(Color(0xFF22C55E)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Claimed",
            tint = Color.White,
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
private fun getBorderColor(day: Int, currentDay: Int): Color {
    return when {
        day < currentDay -> Color(0xFF4ADE80)
        day == currentDay -> {
            val infiniteTransition = rememberInfiniteTransition()
            val colorAlpha by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
            Color(0xFF3B82F6).copy(alpha = colorAlpha)
        }
        else -> Color.Transparent
    }
}

