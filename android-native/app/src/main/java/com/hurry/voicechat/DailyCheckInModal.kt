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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage

data class SignInReward(val day: Int, val reward: String, val image: String)

private val SIGN_IN_REWARDS = listOf(
    SignInReward(1, "+5000", "file_00000000e56882119c217d508b6733dc.png"),
    SignInReward(2, "+5000", "file_00000000e56882119c217d508b6733dc.png"),
    SignInReward(3, "×2 Days", "/file_00000000d808821186c1b7b612eea3fc.png"),
    SignInReward(4, "+10,000", "file_00000000e56882119c217d508b6733dc.png")
)

@Composable
fun DailyCheckInModal(
    isOpen: Boolean,
    onClose: () -> Unit,
    currentDay: Int,
    onSignIn: () -> Unit,
    claimedToday: Boolean = false
) {
    if (!isOpen) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.60f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Prevents closing when clicking inside the modal
                    )
            ) {
                // Top Banner Image (Overlapping logic -mt-6 jaisa)
                AsyncImage(
                    model = "/file_000000004b6c8211855003bf899492fd.png",
                    contentDescription = "Top Banner",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = 24.dp) // Adjusting to create the overlapping effect
                        .zIndex(20f)
                )

                // Main White Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 50.dp) // Push content down to accommodate the overlapping banner
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .border(2.dp, Color(0xFF3B82F6), RoundedCornerShape(24.dp))
                        .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 20.dp)
                ) {
                    
                    // Row 1: Days 1 to 4
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SIGN_IN_REWARDS.forEach { item ->
                            DayCard(
                                day = item.day,
                                currentDay = currentDay,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f) // Square aspect ratio
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    AsyncImage(
                                        model = item.image,
                                        contentDescription = "reward",
                                        modifier = Modifier.size(32.dp).padding(bottom = 4.dp)
                                    )
                                    Text(
                                        text = item.reward,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF374151),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 2: Days 5 and 6
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Day 5
                        DayCard(
                            day = 5,
                            currentDay = currentDay,
                            modifier = Modifier.weight(1f).height(80.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    AsyncImage(model = "/file_00000000e56882119c217d508b6733dc.png", contentDescription = null, modifier = Modifier.size(24.dp))
                                    Text("+10,000", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    AsyncImage(model = "/IMG_20260903_141944.png", contentDescription = null, modifier = Modifier.size(32.dp))
                                    Text("×1 Day", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
                                }
                            }
                        }

                        // Day 6
                        DayCard(
                            day = 6,
                            currentDay = currentDay,
                            modifier = Modifier.weight(1f).height(80.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    AsyncImage(model = "/file_00000000e56882119c217d508b6733dc.png", contentDescription = null, modifier = Modifier.size(24.dp))
                                    Text("+10,000", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    AsyncImage(model = "/IMG-20260903-WA0076.jpg", contentDescription = null, modifier = Modifier.size(24.dp))
                                    Text("×2 Days", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Row 3: Day 7 Full Width
                    Day7Card(currentDay = currentDay)

                    Spacer(modifier = Modifier.height(20.dp))

                    // Sign In Button
                    Button(
                        onClick = onSignIn,
                        enabled = !claimedToday && currentDay <= 7,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(
                                elevation = if (!claimedToday && currentDay <= 7) 8.dp else 0.dp,
                                shape = RoundedCornerShape(12.dp),
                                spotColor = Color(0xFF3B82F6)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3B82F6),
                            disabledContainerColor = Color(0xFF9CA3AF)
                        )
                    ) {
                        Text(
                            text = if (claimedToday) "Claimed Today — Come Back Tomorrow" 
                                   else if (currentDay > 7) "All Rewards Claimed!" 
                                   else "Sign In",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Close Button below modal
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.20f), CircleShape)
                    .clickable(onClick = onClose),
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

@Composable
private fun DayCard(
    day: Int,
    currentDay: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val borderColor = when {
        day < currentDay -> Color(0xFF4ADE80) // Green completed
        day == currentDay -> Color(0xFF3B82F6) // Blue active (can add pulse animation if needed later)
        else -> Color(0xFFE5E7EB) // Gray pending
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        // Day Label (Top Left)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .background(
                    color = Color(0xFF3B82F6),
                    shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp)
                )
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = day.toString(),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Main Content
        Box(modifier = Modifier.padding(top = 16.dp)) {
            content()
        }

        // Completed Checkmark (Top Right)
        if (day < currentDay) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(16.dp)
                    .background(Color(0xFF22C55E), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun Day7Card(currentDay: Int) {
    val borderColor = when {
        7 < currentDay -> Color(0xFF4ADE80)
        7 == currentDay -> Color(0xFF3B82F6)
        else -> Color(0xFFE5E7EB)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Gradient Label (Top Left)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFFACC15), Color(0xFFF97316))
                    ),
                    shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp)
                )
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "7 Days Big Rewards",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Checkmark (Top Right)
        if (7 < currentDay) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(16.dp)
                    .background(Color(0xFF22C55E), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        // Content
        Row(
            modifier = Modifier.padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(model = "/file_00000000e56882119c217d508b6733dc.png", contentDescription = null, modifier = Modifier.size(32.dp))
                Text("+15,000", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(model = "/file_0000000044388211996656afc9ce9c03.png", contentDescription = null, modifier = Modifier.size(40.dp))
                Text("×3 days", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(model = "/IMG-20260903-WA0077.jpg", contentDescription = null, modifier = Modifier.size(40.dp))
                Text("×3 days", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
            }
        }
    }
}
