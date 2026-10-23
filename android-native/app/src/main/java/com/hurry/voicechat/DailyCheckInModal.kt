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
                            // HIGHT DECREASE: Top aur bottom padding kam ki hai main card ki
                            .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        
                        // Subtitle (Sign in for 7 days for rich rewards)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp)) // Corner kam kiye
                                .background(Color(0xFFFFF3C9))
                                .padding(vertical = 8.dp), // Height kam ki
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sign in for 7 days for rich rewards",
                                color = Color(0xFF80622E),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp)) // Spacing decrease ki

                        // Row 1: Days 1 to 4
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Nayi coins image yahan laga di
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

                        Spacer(modifier = Modifier.height(16.dp)) // Spacing decrease ki

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
                            // BUTTON SIZE DECREASE: Width 0.85f ki aur height 46.dp kardi
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(46.dp)
                                .background(buttonBrush, RoundedCornerShape(50.dp))
                        ) {
                            Text(
                                text = if (claimedToday) "Claimed Today" else if (currentDay > 7) "All Rewards Claimed!" else "Sign in",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // 2. BANNER IMAGE - TOP BANNER KO AUR OUPER KAR DIYA HAI (-35.dp offset ke sath)
                    AsyncImage(
                        model = "file_000000004b6c8211855003bf899492fd.png".fullUrl,
                        contentDescription = "Top Banner",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .align(Alignment.TopCenter)
                            .offset(y = (-35).dp) // <--- BANNER OUPER KAR DIYA YAHAN SE!
                            .zIndex(2f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

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
            .clip(RoundedCornerShape(6.dp)) // CORNERS KAM KIYE
            .background(Color(0xFFF3F3F3))
            .border(2.dp, borderColor, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        // YE BADGE AB TOP LEFT CHIPKA RAHEGA
        DayBadge(day = day)
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 12.dp)
        ) {
            AsyncImage(
                model = image.fullUrl,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
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
            .height(80.dp) // HEIGHT KAM KI
            .clip(RoundedCornerShape(6.dp)) // CORNERS KAM KIYE
            .background(Color(0xFFF3F3F3))
            .border(2.dp, borderColor, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        // YE BADGE AB TOP LEFT CHIPKA RAHEGA
        DayBadge(day = day)
        
        Box(modifier = Modifier.padding(top = 12.dp)) {
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
            .height(85.dp) // HEIGHT KAM KI
            .clip(RoundedCornerShape(8.dp)) // CORNERS KAM KIYE
            .background(Color(0xFFFFF9EB))
            .border(2.dp, borderColor, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        // SVG Day 7 Badge Top-Left Corner 
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .clip(RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFFFF5038), Color(0xFFFFB62B))))
                .padding(horizontal = 8.dp, vertical = 2.dp) // PATLI PATTI KAR DI HEIGHT KAM KARKE
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
        // Nayi coins image yahan laga di
        RewardItemIcon("file_00000000b2d481fd8cd233482dbeb9ef.png", "+10,000", 28.dp)
        RewardItemIcon("IMG_20260903_141944.png", "×1 Day", 36.dp)
    }
}

@Composable
private fun renderDay6Special() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        // Nayi coins image yahan laga di
        RewardItemIcon("file_00000000b2d481fd8cd233482dbeb9ef.png", "+10,000", 28.dp)
        RewardItemIcon("IMG-20260903-WA0076.jpg", "×2 Days", 28.dp)
    }
}

@Composable
private fun renderDay7Special() {
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
        // Nayi coins image yahan laga di
        RewardItemIcon("file_00000000b2d481fd8cd233482dbeb9ef.png", "+15,000", 36.dp)
        RewardItemIcon("file_0000000044388211996656afc9ce9c03.png", "×3 days", 42.dp)
        RewardItemIcon("IMG-20260903-WA0077.jpg", "×3 days", 42.dp)
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

// YELLOW BADGE JINKI HEIGHT/WIDTH KAM KI HAI AUR CORNERS MODIFY KIYE HAIN
@Composable
private fun BoxScope.DayBadge(day: Int) {
    Box(
        modifier = Modifier
            .align(Alignment.TopStart) // EKDAM TOP LEFT CHIPKA DIYA
            .size(width = 24.dp, height = 18.dp) // HIGHT AUR WIDE DECREASE KAR DI BSS
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
            modifier = Modifier.size(10.dp)
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
