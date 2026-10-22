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

// Helper property to fix image URLs (removes leading slash if any)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose 
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // White main box
                    Column(
                        modifier = Modifier
                            .padding(top = 50.dp) // Space for overlapping banner
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White)
                            .border(2.dp, Color(0xFF3B82F6), RoundedCornerShape(24.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {} // Prevent taps on white box from closing the modal
                            )
                            .padding(start = 24.dp, end = 24.dp, top = 60.dp, bottom = 20.dp)
                    ) {
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

                        // Row 3: Day 7
                        Day7Card(currentDay = currentDay)

                        Spacer(modifier = Modifier.height(20.dp))

                        // Sign-in Button
                        val isButtonDisabled = claimedToday || currentDay > 7
                        Button(
                            onClick = onSignIn,
                            enabled = !isButtonDisabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isButtonDisabled) Color(0xFF9CA3AF) else Color(0xFF3B82F6),
                                disabledContainerColor = Color(0xFF9CA3AF)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(
                                text = if (claimedToday) "Claimed Today — Come Back Tomorrow" else if (currentDay > 7) "All Rewards Claimed!" else "Sign In",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Banner Image - Overlaps the white box (Z-Index is implicitly higher because it's placed later in Box, but we enforce it)
                    AsyncImage(
                        model = "file_000000004b6c8211855003bf899492fd.png".fullUrl,
                        contentDescription = "Top Banner",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Close (X) Button at the bottom
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

// ----------------- Helper UI Components -----------------

@Composable
private fun DayCard(day: Int, reward: String, image: String, currentDay: Int, modifier: Modifier = Modifier) {
    val borderColor = getBorderColor(day, currentDay)
    
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(2.dp, borderColor, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        DayBadge(day = day)
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 12.dp)
        ) {
            AsyncImage(
                model = image.fullUrl,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = reward,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF374151),
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
            .height(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(2.dp, borderColor, RoundedCornerShape(8.dp)),
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
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Top Left Gradient Badge
        Box(
            modifier = Modifier
                .absoluteOffset(x = (-16).dp, y = (-16).dp)
                .clip(RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFFFBBF24), Color(0xFFF97316))))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "7 Days Big Rewards",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(modifier = Modifier.padding(top = 12.dp)) {
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
            color = Color(0xFF374151),
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
            .size(width = 24.dp, height = 20.dp)
            .clip(RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp))
            .background(Color(0xFF3B82F6)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BoxScope.CompletedCheckmark() {
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(4.dp)
            .size(16.dp)
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
        day < currentDay -> Color(0xFF4ADE80) // green-400
        day == currentDay -> {
            // Pulse animation logic for current day
            val infiniteTransition = rememberInfiniteTransition()
            val colorAlpha by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
            Color(0xFF3B82F6).copy(alpha = colorAlpha) // blue-500 pulsing
        }
        else -> Color(0xFFE5E7EB) // gray-200
    }
}
