package com.hurry.voicechat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

@Composable
fun MineNativePage(onBack: () -> Unit, createdRoom: HurryRoom? = null, onCreateRoom: () -> Unit = {}, onSearch: () -> Unit = {}, languageCode: String = "en") {
    var recent by remember { mutableStateOf(false) }
    val card = Modifier.fillMaxWidth().height(112.dp)
    
    LazyColumn(
        Modifier.fillMaxSize().background(Color(0xFFEFEFEF)),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // ═══════════ FIXED TOP HEADER ═══════════
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color(0xFF2563EB),
                            0.97f to Color(0xFFEFF6FF),
                            1.0f to Color(0xFFF9FAFB)
                        )
                    )
                    // Top Safe Area decrease kiya
                    .padding(top = (WindowInsets.statusBars.asPaddingValues().calculateTopPadding() - 16.dp).coerceAtLeast(0.dp))
                    // Right padding 0.dp taaki corner me chipke
                    .padding(top = 0.dp, start = 12.dp, end = 0.dp, bottom = 0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onBack() }
                                .padding(horizontal = 4.dp, vertical = 3.dp)
                        ) {
                            Text(
                                nativeText(languageCode, "popular"),
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Normal,
                                color = HurryMuted
                            )
                        }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { }
                                .padding(horizontal = 4.dp, vertical = 3.dp)
                        ) {
                            Text(
                                nativeText(languageCode, "mine"),
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = HurryText
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    
                    // Gap bilkul HomeScreen jaisa (8.dp) kar diya hai
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp) 
                    ) {
                        Box(
                            modifier = Modifier.clickable(
                                indication = null,
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            ) { onSearch() }
                        ) {
                            HurrySearchIcon()
                        }
                        
                        Box(Modifier.clickable { onCreateRoom() }) {
                            HurryHouseIcon()
                        }
                    }
                }
            }
        }
        // ═══════════ END FIXED TOP HEADER ═══════════

        item {
            Spacer(Modifier.height(6.dp))
            Box(card.padding(horizontal = 12.dp).clip(RoundedCornerShape(6.dp)).background(
                Brush.linearGradient(listOf(Color(0xFF667EEA), Color(0xFF764BA2)))
            ).clickable { onCreateRoom() }) {
                Row(Modifier.fillMaxSize().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha=.2f)), contentAlignment=Alignment.Center) {
                        if (createdRoom == null) {
                            Text("+", color=Color.White, fontSize=32.sp)
                        } else {
                            FallbackAsyncImage(model=createdRoom.image, contentDescription=null, modifier=Modifier.fillMaxSize().clip(RoundedCornerShape(50)))
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(createdRoom?.name ?: nativeText(languageCode, "create_room"), color=Color.White, fontSize=16.sp, maxLines=1, softWrap=false)
                        Text(nativeText(languageCode, if (createdRoom == null) "tap_create" else "tap_enter"), color=Color.White.copy(alpha=.8f), fontSize=14.sp)
                    }
                }
            }
            Row(Modifier.padding(horizontal = 12.dp).padding(top=12.dp), horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                Column(Modifier.clickable { recent=false }, horizontalAlignment=Alignment.Start) {
                    Text("Following", fontSize=14.sp, color=if(!recent) Color(0xFF111827) else Color(0xFF9CA3AF))
                    if (!recent) Box(Modifier.padding(top=2.dp).width(48.dp).height(2.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF111827)))
                }
                Column(Modifier.clickable { recent=true }, horizontalAlignment=Alignment.Start) {
                    Text("Recent", fontSize=14.sp, color=if(recent) Color(0xFF111827) else Color(0xFF9CA3AF))
                    if (recent) Box(Modifier.padding(top=2.dp).width(38.dp).height(2.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF111827)))
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(top=24.dp), horizontalAlignment=Alignment.CenterHorizontally) {
                AsyncImage("https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/file_0000000047308211a02722299d1fda2e.png", null, Modifier.size(50.dp))
                Text(nativeText(languageCode, "no_data"), color=Color(0xFF9CA3AF), fontSize=14.sp)
            }
        }
    }
}
