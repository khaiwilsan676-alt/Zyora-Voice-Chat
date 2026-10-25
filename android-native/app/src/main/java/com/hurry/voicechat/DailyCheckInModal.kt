package com.hurry.voicechat

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
import androidx.compose.ui.draw.scale
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
        // Background thoda halka dark kiya hai bss (Black drop removed)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)) // Yahan dark effect thoda kam kiya hai
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
                            // MAIN CARD CORNER CURVE 12.dp HAI
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(6.dp, Color(0xFFFFE57C), RoundedCornerShape(12.dp)) 
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {} // Card par click se dismiss na ho
                            )
                            .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        
                        // Subtitle (Sign in for 7 days for rich rewards)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp)) 
                                .background(Color(0xFFFFF3C9))
                                .padding(vertical = 8.dp), 
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sign in for 7 days for rich rewards",
                                color = Color(0xFF80622E),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp)) 

                        // Row 1: Days 1 to 4
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DayCard(day = 1, reward = "+5000", image = "file_00000000b2d481fd8cd233482dbeb9ef.png", currentDay = currentDay, modifier = Modifier.weight(1f))
                            DayCard(day = 2, reward = "+5000", image = "file_00000000b2d481fd8cd233482dbeb9ef.png", currentDay = currentDay, modifier = Modifier.weight(1f))
                            DayCard(day = 3, reward = "×2 Days", image = "file_00000000d808821186c1b7b612eea3fc.png", currentDay = currentDay, modifier = Modifier.weight(1f))
                            DayCard(day = 4, reward = "+10,000", image = "file_00000000b2d481fd8cd233482dbeb9ef.png", currentDay = currentDay, modifier = Modifier.weight(1f))
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

                        Spacer(modifier = Modifier.height(16.dp)) 

                        // Sign-in Button (3D hta diya, Solid color, Curve kam)
                        val isButtonDisabled = claimedToday || currentDay > 7
                        val buttonColor = if (isButtonDisabled) {
                            Color(0xFF9CA3AF) // Solid Gray jab disable/claimed ho
                        } else {
                            Color(0xFFFFD633) // Solid Yellow 
                        }

                        Button(
                            onClick = onSignIn,
                            enabled = !isButtonDisabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = buttonColor, 
                                disabledContainerColor = buttonColor
                            ),
                            contentPadding = PaddingValues(),
                            shape = RoundedCornerShape(12.dp), // Curve thoda kam kar diya
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(46.dp)
                        ) {
                            Text(
                                // Text update kar diya
                                text = if (claimedToday) "Signed In today" else if (currentDay > 7) "All Rewards Claimed!" else "Sign in",
                                color = if (isButtonDisabled) Color.White else Color.Black, // Text color adjust kar diya logic ke hisab se
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // 2. BANNER IMAGE
                    AsyncImage(
                        model = "file_000000004b6c8211855003bf899492fd.png".fullUrl,
                        contentDescription = "Top Banner",
                        contentScale = ContentScale.Fit, 
                        modifier = Modifier
                            .fillMaxWidth() 
                            .scale(1.15f) 
                            .align(Alignment.TopCenter)
                            .offset(y = (-45).dp)
                            .zIndex(2f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 3. CLOSE (X) BUTTON
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
            .clip(RoundedCornerShape(6.dp)) 
            .background(Color(0xFFF3F3F3))
            .border(2.dp, borderColor, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        DayBadge(day = day)
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 18.dp) 
        ) {
            AsyncImage(
                model = image.fullUrl,
                contentDescription = null,
                modifier = Modifier.size(38.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = reward,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF544B00),
                maxLines = 1
            )
        }
        
        // Yahan card ke upar blackish overlay aur center blue tick wala logic add kiya
        if (day < currentDay) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.35f)) // Card ke upar transparent black/10 effect
            )
            CenterCompletedCheckmark()
        }
    }
}

@Composable
private fun SpecialDayCard(day: Int, currentDay: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val borderColor = getBorderColor(day, currentDay)

    Box(
        modifier = modifier
            .height(80.dp) 
            .clip(RoundedCornerShape(6.dp)) 
            .background(Color(0xFFF3F3F3))
            .border(2.dp, borderColor, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        DayBadge(day = day)
        
        Box(modifier = Modifier.padding(top = 18.dp)) {
            content()
        }
        
        // Yahan card ke upar blackish overlay aur center blue tick wala logic add kiya
        if (day < currentDay) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )
            CenterCompletedCheckmark()
        }
    }
}

@Composable
private fun Day7Card(currentDay: Int) {
    val borderColor = getBorderColor(7, currentDay)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(85.dp) 
            .clip(RoundedCornerShape(8.dp)) 
            .background(Color(0xFFFFF9EB))
            .border(2.dp, borderColor, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .clip(RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFFFF5038), Color(0xFFFFB62B))))
                .padding(horizontal = 8.dp, vertical = 2.dp) 
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "7",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Big Reward",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(modifier = Modifier.padding(top = 18.dp, start = 16.dp)) {
            renderDay7Special()
        }

        // Yahan card ke upar blackish overlay aur center blue tick wala logic add kiya
        if (7 < currentDay) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
            )
            CenterCompletedCheckmark()
        }
    }
}

// ----------------- Internal Render Functions -----------------

@Composable
private fun renderDay5Special() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        RewardItemIcon("file_00000000b2d481fd8cd233482dbeb9ef.png", "+10,000", 34.dp)
        RewardItemIcon("IMG_20260903_141944.png", "×1 Day", 42.dp)
    }
}

@Composable
private fun renderDay6Special() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        RewardItemIcon("file_00000000b2d481fd8cd233482dbeb9ef.png", "+10,000", 34.dp)
        RewardItemIcon("IMG-20260903-WA0076.jpg", "×2 Days", 34.dp)
    }
}

@Composable
private fun renderDay7Special() {
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
        RewardItemIcon("file_00000000b2d481fd8cd233482dbeb9ef.png", "+15,000", 42.dp)
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
private fun BoxScope.DayBadge(day: Int) {
    Box(
        modifier = Modifier
            .align(Alignment.TopStart) 
            .size(width = 24.dp, height = 18.dp) 
            .clip(RoundedCornerShape(topStart = 6.dp, bottomEnd = 8.dp))
            .background(Color(0xFFFFE000)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            color = Color(0xFF544B00),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

// Ye naya Center Tick logic hai
@Composable
private fun BoxScope.CenterCompletedCheckmark() {
    Box(
        modifier = Modifier
            .align(Alignment.Center) // Ekdam center mein
            .size(24.dp)
            .clip(CircleShape)
            .background(Color(0xFF3B82F6)), // Blue circle color
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Claimed",
            tint = Color.White, // White Tick
            modifier = Modifier.size(16.dp)
        )
    }
}

fun getBorderColor(day: Int, currentDay: Int): Color {
    return when {
        day < currentDay -> Color(0xFF4ADE80) 
        day == currentDay -> Color(0xFFFFD633) 
        else -> Color.Transparent
    }
}
