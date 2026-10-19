package com.hurry.voicechat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.TimeZone

// GitHub Raw URLs for your images
private const val ROOM_SUPPORT_HEADER = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261018_165235.png"

@Composable
fun RoomSupport(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    
    // Light blue color
    val bgColor = Color(0xFF0F265C) 

    // Popup State
    var popupMessage by remember { mutableStateOf<String?>(null) }

    // Auto-hide popup timer
    LaunchedEffect(popupMessage) {
        if (popupMessage != null) {
            delay(2000L) // Popup 2 second tak show hoga
            popupMessage = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        
        // Pura Content aur top bar scrollable banaya gaya hai
        RoomSupportContent(
            bgColor = bgColor,
            onShowPopup = { msg -> popupMessage = msg },
            onBack = onBack
        )

        // Custom Pill Popup (Black/40%)
        AnimatedVisibility(
            visible = popupMessage != null,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(50)) // Pill shape
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = popupMessage ?: "",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ==========================================
// 1. ROOM SUPPORT PAGE 
// ==========================================
@Composable
fun RoomSupportContent(bgColor: Color, onShowPopup: (String) -> Unit, onBack: () -> Unit) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Box banaya taaki Back icon image ke upar rahe aur dono sath mein scroll ho
        Box(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = ROOM_SUPPORT_HEADER,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .drawWithCache {
                        val gradient = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, bgColor),
                            startY = size.height * 0.90f, 
                            endY = size.height
                        )
                        onDrawWithContent {
                            drawContent()
                            drawRect(gradient, blendMode = BlendMode.SrcOver)
                        }
                    },
                contentScale = ContentScale.FillWidth 
            )

            // Back Icon ab float nahi hoga, page scroll ke sath upar jayega
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null // Grey effect hta diya
                    ) { onBack() }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-30).dp) 
                .padding(horizontal = 10.dp)
        ) {
            RoomStatsTable()
            Spacer(modifier = Modifier.height(16.dp))
            ThisWeekCountdownCard(onShowPopup = onShowPopup)
            Spacer(modifier = Modifier.height(16.dp))
            TargetRewardTable()
            Spacer(modifier = Modifier.height(16.dp))
            RewardCountdownCard()
            Spacer(modifier = Modifier.height(24.dp))
            RulesSection()
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ==========================================
// Components (Baki All Logics Untouched)
// ==========================================

@Composable
fun RoomStatsTable() {
    val borderColor = Color(0xFF388FC2)
    val headerBg = Brush.verticalGradient(listOf(Color(0xFFFFF0C5), Color(0xFFFFE0A0)))
    val tableBgSolid = Color(0xFF104B7A)
    val textColorLight = Color(0xFFE2EAF0)
    val titleColor = Color(0xFF155B91)
    val mdCorner = RoundedCornerShape(12.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, mdCorner)
            .clip(mdCorner)
            .background(tableBgSolid)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBg)
                .padding(vertical = 6.dp), 
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "My Room",
                color = titleColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Divider(thickness = 2.dp, color = borderColor)

        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.weight(1f))
            VerticalDivider(borderColor)
            HeaderCell("Room\nLevel", Modifier.weight(1f), textColorLight)
            VerticalDivider(borderColor)
            HeaderCell("Reward\nCoins", Modifier.weight(1f), textColorLight)
            VerticalDivider(borderColor)
            HeaderCell("Room\nVisitors", Modifier.weight(1f), textColorLight)
            VerticalDivider(borderColor)
            HeaderCell("Room\nCoins", Modifier.weight(1f), textColorLight)
        }

        Divider(thickness = 2.dp, color = borderColor)

        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            HeaderCell("This\nweek", Modifier.weight(1f), textColorLight, 11.sp)
            VerticalDivider(borderColor)
            ValueCell("0", Modifier.weight(1f), textColorLight, 14.sp)
            VerticalDivider(borderColor)
            ValueCell("0", Modifier.weight(1f), textColorLight, 14.sp)
            VerticalDivider(borderColor)
            ValueCell("0", Modifier.weight(1f), textColorLight, 14.sp)
            VerticalDivider(borderColor)
            ValueCell("0", Modifier.weight(1f), textColorLight, 14.sp)
        }

        Divider(thickness = 2.dp, color = borderColor)

        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            HeaderCell("Last\nweek", Modifier.weight(1f), textColorLight, 11.sp)
            VerticalDivider(borderColor)
            ValueCell("0", Modifier.weight(1f), textColorLight, 14.sp)
            VerticalDivider(borderColor)
            ValueCell("0", Modifier.weight(1f), textColorLight, 14.sp)
            VerticalDivider(borderColor)
            ValueCell("0", Modifier.weight(1f), textColorLight, 14.sp)
            VerticalDivider(borderColor)
            ValueCell("0", Modifier.weight(1f), textColorLight, 14.sp)
        }
    }
}

@Composable
fun RewardCountdownCard() {
    var timeLeftInSeconds by remember { mutableLongStateOf(calculateSecondsUntilNextWednesday()) }

    LaunchedEffect(Unit) {
        while (timeLeftInSeconds > 0) {
            delay(1000L)
            timeLeftInSeconds--
        }
    }

    val days = timeLeftInSeconds / (24 * 3600)
    val hours = (timeLeftInSeconds % (24 * 3600)) / 3600
    val minutes = (timeLeftInSeconds % 3600) / 60
    val seconds = timeLeftInSeconds % 60

    val timeString = "${days}Day ${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"

    val borderColor = Color(0xFF388FC2) 
    val headerGradient = Brush.verticalGradient(listOf(Color(0xFFFFF0C9), Color(0xFFFFE2A3)))
    val bodyBgSolid = Color(0xFF0D4880)
    val mdCorner = RoundedCornerShape(12.dp)

    val headingColor = Color(0xFF155B91)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, mdCorner) 
            .clip(mdCorner)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerGradient)
                .padding(horizontal = 12.dp, vertical = 6.dp), 
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CardGiftcard,
                contentDescription = "Gift",
                tint = headingColor,
                modifier = Modifier.size(20.dp) 
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Reward send time:",
                color = headingColor, 
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = timeString,
                color = Color(0xFFE44825),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bodyBgSolid)
                .padding(16.dp)
        ) {
            Text(
                text = "If you achieve Room Support Owner and multiple Room Partner rewards then you will get only one of the highest rewards",
                color = Color(0xFFF4FAFF),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun TargetRewardTable() {
    val borderColor = Color(0xFF388FC2)
    val headerBg = Brush.verticalGradient(listOf(Color(0xFFFFF0C5), Color(0xFFFFE0A0)))
    val tableBgSolid = Color(0xFF104B7A)
    val textColorLight = Color(0xFFE2EAF0)
    val titleColor = Color(0xFF155B91)
    val mdCorner = RoundedCornerShape(12.dp)

    val headerSize = 9.sp 
    val dataSize = 10.sp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, mdCorner)
            .clip(mdCorner)
            .background(tableBgSolid)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBg)
                .padding(vertical = 4.dp), 
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Target & Reward",
                color = titleColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Divider(thickness = 1.dp, color = borderColor)

        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.weight(0.5f))
            VerticalDivider(borderColor)
            HeaderCell("Target", Modifier.weight(2f), textColorLight, 11.sp, FontWeight.Medium)
            VerticalDivider(borderColor)
            HeaderCell("Reward", Modifier.weight(4f), textColorLight, 11.sp, FontWeight.Medium)
        }

        Divider(thickness = 1.dp, color = borderColor)

        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            HeaderCell("Lv.", Modifier.weight(0.5f), textColorLight, headerSize)
            VerticalDivider(borderColor)
            HeaderCell("Room\nVisitors", Modifier.weight(1f), textColorLight, headerSize)
            VerticalDivider(borderColor)
            HeaderCell("Room\nCoins", Modifier.weight(1f), textColorLight, headerSize)
            VerticalDivider(borderColor)
            HeaderCell("Total\nCoins", Modifier.weight(1f), textColorLight, headerSize)
            VerticalDivider(borderColor)
            HeaderCell("Owner\nCoins", Modifier.weight(1f), textColorLight, headerSize)
            VerticalDivider(borderColor)
            HeaderCell("Partner\nCoins", Modifier.weight(1f), textColorLight, headerSize)
            VerticalDivider(borderColor)
            HeaderCell("Number\nof\nPartners", Modifier.weight(1f), textColorLight, headerSize)
        }

        Divider(thickness = 1.dp, color = borderColor)

        val targetRewardData = getTargetRewardData()
        targetRewardData.forEach { data ->
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                ValueCell(data.lv, Modifier.weight(0.5f), textColorLight, dataSize)
                VerticalDivider(borderColor)
                ValueCell(data.visitors, Modifier.weight(1f), textColorLight, dataSize)
                VerticalDivider(borderColor)
                ValueCell(data.roomCoins, Modifier.weight(1f), textColorLight, dataSize)
                VerticalDivider(borderColor) 
                ValueCell(data.totalCoins, Modifier.weight(1f), textColorLight, dataSize, FontWeight.Bold)
                VerticalDivider(borderColor)
                ValueCell(data.ownerCoins, Modifier.weight(1f), textColorLight, dataSize)
                VerticalDivider(borderColor)
                ValueCell(data.partnerCoins, Modifier.weight(1f), textColorLight, dataSize)
                VerticalDivider(borderColor)
                ValueCell(data.numPartners, Modifier.weight(1f), textColorLight, dataSize)
            }
            Divider(thickness = 1.dp, color = borderColor)
        }
    }
}

// User Data Class For Partner State
data class PartnerData(val id: String, val name: String, val avatarUrl: String)

@Composable
fun ThisWeekCountdownCard(onShowPopup: (String) -> Unit) {
    var timeLeftInSeconds by remember { mutableLongStateOf(7 * 24 * 60 * 60L) }

    LaunchedEffect(Unit) {
        while (timeLeftInSeconds > 0) {
            delay(1000L)
            timeLeftInSeconds--
        }
    }

    val days = timeLeftInSeconds / (24 * 3600)
    val hours = (timeLeftInSeconds % (24 * 3600)) / 3600
    val minutes = (timeLeftInSeconds % 3600) / 60
    val seconds = timeLeftInSeconds % 60

    val timeString = "${days}Day ${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"

    val borderColor = Color(0xFF438CC1)
    val headerGradient = Brush.verticalGradient(listOf(Color(0xFFFFF0C9), Color(0xFFFFE2A0)))
    val bodyBgSolid = Color(0xFF0D4880)
    val mdCorner = RoundedCornerShape(12.dp)
    val smallCurve = RoundedCornerShape(6.dp) // + wale card ka curve kam kiya

    // Check karega agar aaj Monday ya Tuesday hai
    val canAddPartner = remember { isMondayOrTuesday() }

    // Dialog aur Partners ka State Management
    val partnersList = remember { mutableStateListOf<PartnerData?>(null, null, null) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var currentSlotIndex by remember { mutableStateOf(-1) }
    var searchQuery by remember { mutableStateOf("") }
    var pendingPartner by remember { mutableStateOf<PartnerData?>(null) } // Temp hold karne k liye

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, mdCorner)
            .clip(mdCorner)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerGradient)
                .padding(horizontal = 12.dp, vertical = 6.dp), 
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = "Clock",
                    tint = Color(0xFF155B91), 
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "This week countdown",
                    color = Color(0xFF155B91), 
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = timeString,
                color = Color(0xFFD83D1D),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(bodyBgSolid)
                .padding(16.dp)
        ) {
            Text(
                text = "Room Partners for the previous week can be filled in every Monday 00:00 to Tuesday 24:00 (UTC +5.30), and rewards will be sent out every Wednesday.",
                color = Color(0xFFEAF5FF),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // 3 Cards / Partners Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                partnersList.forEachIndexed { index, partner ->
                    if (partner == null) {
                        // Empty State (Curve kam kiya yahan smallCurve use karke)
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(smallCurve) 
                                .background(borderColor)
                                .clickable {
                                    if (canAddPartner) {
                                        currentSlotIndex = index
                                        searchQuery = "" // Input clear kar diya pichla
                                        showSearchDialog = true
                                    } else {
                                        onShowPopup("Fill-in Time not Yet")
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Partner",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        // Jab Partner Add ho jaye: Avatar or Niche uska naam
                        Column(
                            modifier = Modifier.width(60.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncImage(
                                model = partner.avatarUrl,
                                contentDescription = "Partner Avatar",
                                modifier = Modifier
                                    .size(45.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, borderColor, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = partner.name,
                                color = Color.White,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    // 1. Center Search Dialog (Top heading "Partner")
    if (showSearchDialog) {
        Dialog(onDismissRequest = { showSearchDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(mdCorner)
                    .background(bodyBgSolid) // Same Table ka color
                    .border(2.dp, borderColor, mdCorner)
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Partner",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by ID...", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = borderColor, // Border same ussi color ka
                            unfocusedBorderColor = borderColor,
                            cursorColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (searchQuery.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = "https://ui-avatars.com/api/?name=${searchQuery}&background=random",
                                contentDescription = "Search Avatar",
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, borderColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Text(
                                text = "User $searchQuery",
                                color = Color.White,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            // Right Side Blue Add Button
                            Text(
                                text = "Add",
                                color = borderColor, 
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        // Directly add nai karna, Confirm dialog show karna hai
                                        pendingPartner = PartnerData(
                                            id = searchQuery,
                                            name = "User $searchQuery",
                                            avatarUrl = "https://ui-avatars.com/api/?name=${searchQuery}&background=random"
                                        )
                                        showSearchDialog = false
                                        showConfirmDialog = true
                                    }
                                    .padding(8.dp) 
                            )
                        }
                    }
                }
            }
        }
    }

    // 2. Center Confirmation Dialog (Top heading "Tips")
    if (showConfirmDialog) {
        Dialog(onDismissRequest = { showConfirmDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(mdCorner)
                    .background(bodyBgSolid)
                    .border(2.dp, borderColor, mdCorner)
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Tips",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "If you add partner, then you can't remove",
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text(
                            text = "Cancel",
                            color = Color.Gray,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { 
                                showConfirmDialog = false
                                pendingPartner = null 
                            }.padding(8.dp)
                        )
                        
                        Text(
                            text = "Confirm",
                            color = borderColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { 
                                // Yahan aake actual mein add hoga
                                pendingPartner?.let {
                                    partnersList[currentSlotIndex] = it
                                }
                                showConfirmDialog = false
                                pendingPartner = null
                            }.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RulesSection() {
    val textColor = Color(0xFFEAF5FF)
    val highlightColor = Color(0xFFFFD700) 

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Text(
            text = "Rules",
            color = highlightColor,
            fontSize = 20.sp, 
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = buildAnnotatedString {
                append("1. Weekly Room Visitors and Weekly Room Coins are counted from ")
                withStyle(SpanStyle(color = highlightColor)) { append("Monday 00:00 to Sunday 23:59 (UTC +5:30)") }
                append(".")
            },
            color = textColor, fontSize = 12.sp, lineHeight = 16.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = buildAnnotatedString {
                append("2. ")
                withStyle(SpanStyle(color = highlightColor)) { append("Reaching the Room Level requires meeting both the Room Visitors and Room Coins targets simultaneously.") }
            },
            color = textColor, fontSize = 12.sp, lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = buildAnnotatedString {
                append("3. Room Owners must fill in Room Partner information ")
                withStyle(SpanStyle(color = highlightColor)) { append("from Monday 00:00 to Tuesday 24:00 (UTC +5:30)") }
                append("; Otherwise, the reward will be expired.")
            },
            color = textColor, fontSize = 12.sp, lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = buildAnnotatedString {
                append("4. ")
                withStyle(SpanStyle(color = highlightColor)) { append("Rewards will be sent every Wednesday.") }
            },
            color = textColor, fontSize = 12.sp, lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = buildAnnotatedString {
                append("5. ")
                withStyle(SpanStyle(color = highlightColor)) { append("Each user can only receive 1 reward per week. ") }
                append("The reward will be sent based on the highest target reached by the user, whether it is as an Owner or a Partner. For example, if the user is both the Owner of their room and a Partner in other rooms, or a Partner in multiple rooms simultaneously, the reward with the most coins will be sent.")
            },
            color = textColor, fontSize = 12.sp, lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "6. Cheating and violations are prohibited. Once discovered, all rewards will be cancelled.",
            color = textColor, fontSize = 12.sp, lineHeight = 16.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Regard, Zyora Team",
            color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End
        )
    }
}

// --- Helper Functions and Data Classes ---

@Composable
private fun HeaderCell(
    text: String, 
    modifier: Modifier = Modifier, 
    color: Color, 
    fontSize: androidx.compose.ui.unit.TextUnit = 11.sp,
    fontWeight: FontWeight = FontWeight.Normal
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 8.dp, horizontal = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = color, fontSize = fontSize, textAlign = TextAlign.Center, lineHeight = (fontSize.value + 2).sp, fontWeight = fontWeight)
    }
}

@Composable
private fun ValueCell(
    text: String, 
    modifier: Modifier = Modifier, 
    color: Color, 
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.Normal
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 10.dp, horizontal = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = color, fontSize = fontSize, textAlign = TextAlign.Center, fontWeight = fontWeight)
    }
}

@Composable
private fun VerticalDivider(color: Color) {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(color)
    )
}

fun calculateSecondsUntilNextWednesday(): Long {
    val timeZone = TimeZone.getTimeZone("Asia/Kolkata")
    val now = Calendar.getInstance(timeZone)
    val nextWed = Calendar.getInstance(timeZone)
    
    var daysToAdd = Calendar.WEDNESDAY - now.get(Calendar.DAY_OF_WEEK)
    if (daysToAdd <= 0) {
        daysToAdd += 7
    }
    
    nextWed.add(Calendar.DAY_OF_YEAR, daysToAdd)
    nextWed.set(Calendar.HOUR_OF_DAY, 0)
    nextWed.set(Calendar.MINUTE, 0)
    nextWed.set(Calendar.SECOND, 0)
    nextWed.set(Calendar.MILLISECOND, 0)
    
    val diffMillis = nextWed.timeInMillis - now.timeInMillis
    return if (diffMillis > 0) diffMillis / 1000L else 7 * 24 * 3600L
}

// Function checking if today is Monday or Tuesday
fun isMondayOrTuesday(): Boolean {
    val timeZone = TimeZone.getTimeZone("Asia/Kolkata")
    val now = Calendar.getInstance(timeZone)
    val dayOfWeek = now.get(Calendar.DAY_OF_WEEK)
    return dayOfWeek == Calendar.MONDAY || dayOfWeek == Calendar.TUESDAY
}

data class TargetRewardRow(
    val lv: String, val visitors: String, val roomCoins: String, val totalCoins: String, 
    val ownerCoins: String, val partnerCoins: String, val numPartners: String
)

fun getTargetRewardData() = listOf(
    TargetRewardRow("1", "≥20", "≥13.5M", "1.61M", "1.27M", "340K", "1"),
    TargetRewardRow("2", "≥50", "≥27M", "3.09M", "2.27M", "410K", "2"),
    TargetRewardRow("3", "≥75", "≥57M", "6.4M", "4.66M", "580K", "3"),
    TargetRewardRow("4", "≥100", "≥87M", "9.58M", "6.97M", "870K", "3"),
    TargetRewardRow("5", "≥150", "≥150M", "16.49M", "11.89M", "1.15M", "4"),
    TargetRewardRow("6", "≥200", "≥225M", "24.68M", "17.64M", "1.76M", "4"),
    TargetRewardRow("7", "≥300", "≥315M", "33.6M", "23.65M", "1.99M", "5"),
    TargetRewardRow("8", "≥350", "≥447M", "47.64M", "35.04M", "2.52M", "5"),
    TargetRewardRow("9", "≥400", "≥597M", "62.16M", "45.78M", "2.73M", "6"),
    TargetRewardRow("10", "≥450", "≥915M", "93.96M", "69.3M", "4.11M", "6"),
    TargetRewardRow("11", "≥500", "≥1.26B", "129.97M", "92.1M", "5.41M", "7"),
    TargetRewardRow("12", "≥600", "≥1.65B", "169.94M", "119.96M", "7.14M", "7"),
    TargetRewardRow("13", "≥750", "≥2.1B", "216.03M", "152.4M", "9.09M", "7"),
    TargetRewardRow("14", "≥1,000", "≥2.85B", "292.92M", "193.24M", "12.46M", "8"),
    TargetRewardRow("15", "≥1,500", "≥3.75B", "379.45M", "231.49M", "16.44M", "9"),
    TargetRewardRow("16", "≥2,000", "≥5.55B", "546.59M", "318.89M", "22.77M", "10"),
    TargetRewardRow("17", "≥3,000", "≥7.65B", "738.14M", "402.2M", "30.54M", "11"),
    TargetRewardRow("18", "≥3,500", "≥10.35B", "983.14M", "521.14M", "38.5M", "12")
)
