package com.hurry.voicechat

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val RAW_WALLET = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/"

// ═══════════════════════════════════════════════════════
// ROOM DB — FruitPartyDB equivalent
// ═══════════════════════════════════════════════════════
@Entity(tableName = "user_data")
data class UserDataEntity(
    @PrimaryKey val key: String = "user_data",
    val balance: Long = 0L,
    val diamonds: Long = 0L
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Long,
    val date: String,
    val timestamp: Long,
    val type: String
)

@Dao
interface WalletDao {
    @Query("SELECT * FROM user_data WHERE `key` = 'user_data' LIMIT 1")
    suspend fun getUserData(): UserDataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserData(data: UserDataEntity)

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY timestamp DESC LIMIT 20")
    suspend fun getRecentTransactions(type: String): List<TransactionEntity>

    @Insert
    suspend fun insertTransaction(tx: TransactionEntity)

    @Query("DELETE FROM transactions WHERE timestamp < :cutoff")
    suspend fun deleteOldTransactions(cutoff: Long)
}

@Database(entities = [UserDataEntity::class, TransactionEntity::class], version = 1, exportSchema = false)
abstract class WalletDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao
    companion object {
        @Volatile private var INSTANCE: WalletDatabase? = null
        fun get(context: Context): WalletDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext, WalletDatabase::class.java, "fruit_party.db"
            ).build().also { INSTANCE = it }
        }
    }
}

// ─── DB helpers ───
private suspend fun loadBalance(dao: WalletDao): Long = withContext(Dispatchers.IO) { dao.getUserData()?.balance ?: 0L }
private suspend fun loadDiamonds(dao: WalletDao): Long = withContext(Dispatchers.IO) { dao.getUserData()?.diamonds ?: 0L }

private suspend fun addCoinsToDB(dao: WalletDao, amount: Long) = withContext(Dispatchers.IO) {
    val cur = dao.getUserData() ?: UserDataEntity()
    dao.saveUserData(cur.copy(balance = (cur.balance + amount).coerceAtLeast(0)))
}

private suspend fun subtractDiamondsFromDB(dao: WalletDao, amount: Long): Boolean = withContext(Dispatchers.IO) {
    val cur = dao.getUserData() ?: UserDataEntity()
    if (cur.diamonds < amount) return@withContext false
    dao.saveUserData(cur.copy(diamonds = cur.diamonds - amount))
    true
}

private suspend fun recordTransaction(dao: WalletDao, title: String, amount: Long, type: String) = withContext(Dispatchers.IO) {
    val now = java.util.Calendar.getInstance()
    val dateStr = String.format(
        "%04d.%02d.%02d %02d:%02d",
        now.get(java.util.Calendar.YEAR), now.get(java.util.Calendar.MONTH) + 1,
        now.get(java.util.Calendar.DAY_OF_MONTH),
        now.get(java.util.Calendar.HOUR_OF_DAY), now.get(java.util.Calendar.MINUTE)
    )
    dao.insertTransaction(TransactionEntity(
        title = title, amount = amount, date = dateStr,
        timestamp = System.currentTimeMillis(), type = type
    ))
}

// ═══════════════════════════════════════════════════════
// PUBLIC ENTRY POINTS
// ═══════════════════════════════════════════════════════
@Composable fun CoinsNativePage(onBack: () -> Unit) = WalletNativePage(onBack, 0)
@Composable fun DiamondsNativePage(onBack: () -> Unit) = WalletNativePage(onBack, 1)

// ═══════════════════════════════════════════════════════
// MAIN WALLET PAGE
// ═══════════════════════════════════════════════════════
@Composable
private fun WalletNativePage(onBack: () -> Unit, initialTab: Int) {
    val context = LocalContext.current
    val dao = remember { WalletDatabase.get(context).walletDao() }
    val scope = rememberCoroutineScope()

    var tab by remember { mutableStateOf(initialTab) }
    var coins by remember { mutableStateOf(0L) }
    var diamonds by remember { mutableStateOf(0L) }
    var din by remember { mutableStateOf("") }
    var cin by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("100%") }

    var showPaymentSheet by remember { mutableStateOf(false) }
    var showPayUsingSheet by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            coins = loadBalance(dao)
            diamonds = loadDiamonds(dao)
            delay(1000)
        }
    }

    if (showDetails != null) {
        DetailsNativePage(type = showDetails!!, onBack = { showDetails = null })
        return
    }

    val banner = if (tab == 1) "file_0000000085a482088fb089cb76f3d1af.png"
                 else "file_00000000f3d88211964f0057da4bc797.png"

    val bgGradient = androidx.compose.ui.graphics.Brush.verticalGradient(
        0.0f to Color(0xFF1A66FF),
        0.15f to Color(0xFF1A66FF),
        0.30f to Color(0xFFF3F4F6),
        1.0f to Color(0xFFF3F4F6)
    )

    Box(Modifier.fillMaxSize().background(Color(0xFFF3F4F6))) {
        Column(
            Modifier.fillMaxSize().background(bgGradient)
                .statusBarsPadding().navigationBarsPadding()
        ) {
            // ─── Top Bar ───
            Row(
                Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(44.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
                    BackArrowWhite()
                }
                Text("Recharge", Modifier.weight(1f), textAlign = TextAlign.Center,
                    fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Box(
                    Modifier.size(44.dp).clickable {
                        showDetails = if (tab == 1) "diamond" else "coin"
                    }, contentAlignment = Alignment.Center
                ) { HistoryIconWhite() }
            }

            // ─── Scrollable Body ───
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

                // ─── Banner ───
                Box(Modifier.fillMaxWidth().then(if (tab == 1) Modifier.padding(horizontal = 8.dp) else Modifier)) {
                    AsyncImage(RAW_WALLET + banner, null,
                        Modifier.fillMaxWidth().then(if (tab == 0) Modifier.widthIn(min = 360.dp) else Modifier),
                        contentScale = ContentScale.Fit)
                    Column(Modifier.padding(start = 34.dp, top = 34.dp)) {
                        Text(if (tab == 1) "My Diamonds" else "My Coins",
                            fontSize = 14.sp, color = Color(0xFFE5E7EB), fontWeight = FontWeight.Medium)
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text((if (tab == 1) diamonds else coins).toString(),
                                fontSize = 22.sp, color = Color(0xFFFACC15), fontWeight = FontWeight.Bold)
                            AsyncImage(RAW_WALLET + "file_00000000e56882119c217d508b6733dc.png",
                                null, Modifier.size(20.dp), contentScale = ContentScale.Fit)
                        }
                    }
                }

                // ─── Pill Tabs ───
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 18.dp)
                        .clip(RoundedCornerShape(30.dp)).background(Color(0x33000000)).padding(4.dp)
                ) {
                    PillTab("Coins", tab == 0, Modifier.weight(1f)) { tab = 0 }
                    PillTab("Diamonds", tab == 1, Modifier.weight(1f)) { tab = 1 }
                    PillTab("Agent", tab == 2, Modifier.weight(1f)) { tab = 2 }
                }

                when (tab) {
                    0 -> CoinsContent(onBuy = { showPaymentSheet = true })
                    1 -> DiamondsContent(
                        din = din, cin = cin, rate = rate, diamonds = diamonds,
                        onDinChange = { v -> din = v; cin = ((v.toLongOrNull() ?: 0L) * 33 / 100).toString() },
                        onCinChange = { v -> cin = v; din = ((v.toLongOrNull() ?: 0L) * 100 / 33).toString() },
                        onRateSelect = { r ->
                            rate = r
                            val pct = r.replace("%", "").toLongOrNull() ?: 0L
                            val sel = diamonds * pct / 100
                            din = if (sel > 0) sel.toString() else ""
                            cin = if (sel > 0) (sel * 33 / 100).toString() else ""
                        },
                        onExchange = {
                            val d = din.toLongOrNull() ?: 0L
                            val c = cin.toLongOrNull() ?: 0L
                            if (d > 0 && c > 0) {
                                scope.launch {
                                    if (subtractDiamondsFromDB(dao, d)) {
                                        recordTransaction(dao, "Diamond Exchange", -d, "diamond")
                                        addCoinsToDB(dao, c)
                                        din = ""; cin = ""
                                    }
                                }
                            }
                        }
                    )
                    2 -> AgentContent()
                }

                Spacer(Modifier.height(if (tab == 1) 100.dp else 24.dp))
            }
        }

        if (tab == 1) {
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.fillMaxWidth(0.75f).height(50.dp)
                        .clip(RoundedCornerShape(28.dp)).background(Color(0xFF0044FF))
                        .clickable {
                            val d = din.toLongOrNull() ?: 0L
                            val c = cin.toLongOrNull() ?: 0L
                            if (d > 0 && c > 0) {
                                scope.launch {
                                    if (subtractDiamondsFromDB(dao, d)) {
                                        recordTransaction(dao, "Diamond Exchange", -d, "diamond")
                                        addCoinsToDB(dao, c)
                                        din = ""; cin = ""
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Exchange", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }

        if (showPaymentSheet || showPayUsingSheet) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f))
                .clickable { showPaymentSheet = false; showPayUsingSheet = false })
        }

        if (showPaymentSheet) {
            PaymentSheet(onClose = { showPaymentSheet = false },
                onRecharge = { showPaymentSheet = false; showPayUsingSheet = true })
        }

        if (showPayUsingSheet) {
            PaySheet(onClose = { showPayUsingSheet = false },
                onSelect = {
                    scope.launch {
                        addCoinsToDB(dao, 1_030_000L)
                        recordTransaction(dao, "Buy Coins", 1_030_000L, "coin")
                        showPayUsingSheet = false
                    }
                })
        }
    }
}

// ═══════════════════════════════════════════════════════
// DETAILS PAGE
// ═══════════════════════════════════════════════════════
@Composable
private fun DetailsNativePage(type: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { WalletDatabase.get(context).walletDao() }
    var transactions by remember { mutableStateOf<List<TransactionEntity>>(emptyList()) }

    LaunchedEffect(type) {
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        withContext(Dispatchers.IO) { dao.deleteOldTransactions(cutoff) }
        transactions = withContext(Dispatchers.IO) { dao.getRecentTransactions(type) }
    }

    val valueColor = if (type == "diamond") Color(0xFF3B82F6) else Color(0xFFF59E0B)

    Column(
        Modifier.fillMaxSize().background(Color.White)
            .statusBarsPadding().navigationBarsPadding()
    ) {
        Box(Modifier.fillMaxWidth().height(48.dp)) {
            Box(Modifier.align(Alignment.CenterStart).size(44.dp).clickable { onBack() },
                contentAlignment = Alignment.Center) { BackArrowBlack() }
            Text("Details", Modifier.align(Alignment.Center),
                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A0A0A))
        }

        if (transactions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No recent history", fontSize = 14.sp,
                    fontWeight = FontWeight.Medium, color = Color(0xFF9CA3AF))
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)) {
                items(transactions, key = { it.id }) { tx ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(tx.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF111827))
                            Text(tx.date, Modifier.padding(top = 4.dp),
                                fontSize = 13.sp, color = Color(0xFF9CA3AF))
                        }
                        Text(if (tx.amount > 0) "+${tx.amount}" else tx.amount.toString(),
                            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = valueColor)
                    }
                    HorizontalDivider(color = Color(0xFFF3F4F6))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// TAB CONTENT
// ═══════════════════════════════════════════════════════
@Composable
private fun CoinsContent(onBuy: () -> Unit) {
    // "Recharge Coins" text hata diya — TSX me nahi hai
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 20.dp)) {
        Pack("1,000,000", "₹ 100", "+Bounce 30,000", Modifier.width(120.dp), onBuy)
    }
}

@Composable
private fun DiamondsContent(
    din: String, cin: String, rate: String, diamonds: Long,
    onDinChange: (String) -> Unit, onCinChange: (String) -> Unit,
    onRateSelect: (String) -> Unit, onExchange: () -> Unit
) {
    Column(Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 16.dp)) {

        // ─── Exchange card ───
        Column(
            Modifier.fillMaxWidth()
                .padding(top = 20.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color(0xFFF0F7FF), Color.White)
                    )
                )
                .border(1.dp, Color(0xFFE0EFFF), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Exchange", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("100 =", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6B7280))
                    AsyncImage(
                        RAW_WALLET + "1787321690452.png",
                        null, Modifier.size(14.dp), contentScale = ContentScale.Fit
                    )
                    Text("33", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6B7280))
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DiamondInputBox("Input multiple", din, Modifier.weight(1f), onDinChange,
                    icon = "1787321690452.png", suffix = "x100")
                Text("=", color = Color(0xFFD1D5DB), fontWeight = FontWeight.Bold)
                DiamondInputBox("Coins", cin, Modifier.weight(1f), onCinChange,
                    icon = "file_00000000e56882119c217d508b6733dc.png")
            }
        }

        // ─── Exchange rate ───
        Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Text("exchange rate", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
            Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("20%", "40%", "60%", "80%", "100%").chunked(3).forEach { rowRates ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowRates.forEach { r ->
                            Box(
                                Modifier.weight(1f).height(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (rate == r) Color(0xFF0044FF) else Color.White)
                                    .border(1.dp,
                                        if (rate == r) Color(0xFF0044FF) else Color(0xFFBFDBFE),
                                        RoundedCornerShape(12.dp))
                                    .clickable { onRateSelect(r) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(r, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    color = if (rate == r) Color.White else Color(0xFF0044FF))
                            }
                        }
                        if (rowRates.size < 3) repeat(3 - rowRates.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }


    }
}

@Composable
private fun AgentContent() {
    Row(
        Modifier.fillMaxWidth().padding(top = 20.dp, start = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(50)).background(Color(0xFFE5E7EB))
            .border(2.dp, Color.White, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Canvas(Modifier.size(34.dp)) {
                drawCircle(Color(0xFF9CA3AF), radius = size.width * .17f,
                    center = androidx.compose.ui.geometry.Offset(size.width * .5f, size.height * .32f))
                val p = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * .16f, size.height * .92f)
                    cubicTo(size.width * .16f, size.height * .62f,
                            size.width * .84f, size.height * .62f,
                            size.width * .84f, size.height * .92f)
                    close()
                }
                drawPath(p, Color(0xFF9CA3AF))
            }
        }
        Text("Agent Anmol", Modifier.weight(1f).padding(start = 12.dp),
            fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
        ChatIcon()
    }
}

// ═══════════════════════════════════════════════════════
// SMALL COMPONENTS
// ═══════════════════════════════════════════════════════
@Composable
private fun PillTab(t: String, selected: Boolean, m: Modifier, onClick: () -> Unit) {
    Box(
        m.height(40.dp).clip(RoundedCornerShape(25.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Text(t, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827)) }
}

@Composable
private fun Pack(c: String, p: String, b: String, m: Modifier, onClick: () -> Unit) {
    Column(
        m.width(120.dp).aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp)).background(Color.White)
            .clickable { onClick() }.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AsyncImage(RAW_WALLET + "file_00000000e56882119c217d508b6733dc.png", null,
            Modifier.size(32.dp), contentScale = ContentScale.Fit)
        Text(c, Modifier.padding(top = 6.dp), fontSize = 17.sp,
            fontWeight = FontWeight.Bold, color = Color(0xFF111827), lineHeight = 17.sp)
        Text(b, Modifier.padding(top = 5.dp)
            .background(Color(0xFFEF4444), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 3.dp),
            fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold, lineHeight = 9.sp)
        Text(p, Modifier.padding(top = 6.dp), fontSize = 14.sp,
            color = Color(0xFF6B7280), fontWeight = FontWeight.Medium, lineHeight = 14.sp)
    }
}

@Composable
private fun DiamondInputBox(
    hint: String,
    value: String,
    m: Modifier,
    onChange: (String) -> Unit,
    icon: String,
    suffix: String? = null
) {
    Row(
        m.height(42.dp).clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF9FAFB).copy(alpha = 0.9f))
            .border(1.dp, Color(0xFFDBEAFE), RoundedCornerShape(12.dp))
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AsyncImage(RAW_WALLET + icon, null, Modifier.size(16.dp), contentScale = ContentScale.Fit)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(hint, fontSize = 12.sp, color = Color(0xFF9CA3AF), maxLines = 1)
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF374151)
                )
            )
        }
        if (suffix != null) {
            Text(suffix, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9CA3AF))
        }
    }
}

// ─── Agent chat icon (blue circle + white bubble + 3 dots) ───
@Composable
private fun ChatIcon() {
    Box(Modifier.size(44.dp).clickable {}, contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.size(32.dp)) {
            val scale = size.width / 48f
            val blue = Color(0xFF0044FF)
            drawCircle(blue, radius = 23f * scale)
            drawCircle(Color.White, radius = 14f * scale)
            val tail = androidx.compose.ui.graphics.Path().apply {
                moveTo(14f * scale, 32f * scale)
                lineTo(13f * scale, 38f * scale)
                lineTo(19f * scale, 34f * scale)
                close()
            }
            drawPath(tail, Color.White)
            listOf(18f, 24f, 30f).forEach { x ->
                drawCircle(blue, radius = 2f * scale,
                    center = androidx.compose.ui.geometry.Offset(x * scale, 24f * scale))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// SHEETS
// ═══════════════════════════════════════════════════════
@Composable
private fun PaymentSheet(onClose: () -> Unit, onRecharge: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Column(Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(Color.White)) {

            Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text("Payment Method", Modifier.align(Alignment.Center),
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                Row(Modifier.align(Alignment.CenterEnd)
                    .clip(RoundedCornerShape(6.dp)).background(Color(0xFFF3F4F6))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("🇮🇳", fontSize = 12.sp)
                    Text("in", Modifier.padding(start = 5.dp),
                        fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF374151))
                }
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(12.dp)).background(Color(0xFFF8F9FA)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Coins", fontSize = 13.sp, color = Color(0xFF6B7280), fontWeight = FontWeight.Medium)
                    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(RAW_WALLET + "file_00000000e56882119c217d508b6733dc.png",
                            null, Modifier.size(20.dp), contentScale = ContentScale.Fit)
                        Text("1,030,000", Modifier.padding(start = 6.dp),
                            fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Price", fontSize = 13.sp, color = Color(0xFF6B7280), fontWeight = FontWeight.Medium)
                    Text("₹100.00", Modifier.padding(top = 4.dp),
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                }
            }

            Text("Select payment method",
                Modifier.padding(start = 20.dp, top = 16.dp, bottom = 14.dp),
                fontSize = 13.sp, color = Color(0xFF6B7280), fontWeight = FontWeight.Medium)

            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("UPI", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF1F2937))
                Box(Modifier.padding(start = 10.dp).size(18.dp), contentAlignment = Alignment.Center) {
                    GoogleGLogo(Modifier.size(18.dp))
                }
                Box(Modifier.padding(start = 8.dp).size(18.dp), contentAlignment = Alignment.Center) {
                    PhonePeLogo(Modifier.fillMaxSize(), 4.dp, 10.sp)
                }
                Row(Modifier.padding(start = 8.dp)) {
                    Text("Pay", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF002970))
                    Text("tm", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF00BAF2))
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(RAW_WALLET + "file_00000000e56882119c217d508b6733dc.png",
                        null, Modifier.size(16.dp), contentScale = ContentScale.Fit)
                    Text("1,030,000", Modifier.padding(start = 4.dp),
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
                }
                Box(Modifier.padding(start = 12.dp).size(20.dp)
                    .clip(RoundedCornerShape(50))
                    .border(2.dp, Color(0xFF22C55E), RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(Color(0xFF22C55E)))
                }
            }

            Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp).height(50.dp)
                .clip(RoundedCornerShape(12.dp)).background(Color(0xFF0044FF))
                .clickable { onRecharge() },
                contentAlignment = Alignment.Center) {
                Text("Recharge", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun PaySheet(onClose: () -> Unit, onSelect: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Column(Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Color.White).padding(bottom = 8.dp)) {

            Box(Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 16.dp)) {
                Text("Pay Using", Modifier.align(Alignment.Center),
                    fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                Box(Modifier.align(Alignment.CenterEnd).size(28.dp).clickable { onClose() },
                    contentAlignment = Alignment.Center) {
                    Text("×", fontSize = 24.sp, color = Color(0xFF111827))
                }
            }
            HorizontalDivider(color = Color(0xFFF9FAFB))

            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                PayRow("GPay", onSelect) {
                    GoogleGLogo(Modifier.size(22.dp))
                }
                HorizontalDivider(color = Color(0xFFF9FAFB))
                PayRow("PhonePe", onSelect) {
                    PhonePeLogo(Modifier.fillMaxSize(), 12.dp, 26.sp)
                }
                HorizontalDivider(color = Color(0xFFF9FAFB))
                PayRow("Paytm", onSelect) {
                    Row {
                        Text("Pay", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF002970))
                        Text("tm", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF00BAF2))
                    }
                }
                HorizontalDivider(color = Color(0xFFF9FAFB))
                PayRow("Other", onSelect) {
                    Text("...", fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color(0xFF9CA3AF))
                }
            }
        }
    }
}

@Composable
private fun PayRow(name: String, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(50))
                .background(
                    if (name == "PhonePe" || name == "Other") Color(0xFFF3F4F6)
                    else Color.White
                )
                .then(
                    if (name == "GPay" || name == "Paytm")
                        Modifier.border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(50))
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) { icon() }
        Text(name, Modifier.padding(start = 16.dp),
            fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF111827))
    }
}

// ─── Google G logo (Canvas — real 4-color G) ───
@Composable
private fun GoogleGLogo(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.width * .38f
        val strokeW = size.width * .24f
        val topLeft = androidx.compose.ui.geometry.Offset(cx - r, cy - r)
        val arcSize = androidx.compose.ui.geometry.Size(r * 2, r * 2)

        drawArc(Color(0xFFEA4335), -50f, 100f, false, topLeft, arcSize, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW))
        drawArc(Color(0xFF4285F4), 50f, 90f, false, topLeft, arcSize, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW))
        drawArc(Color(0xFFFBBC05), 140f, 90f, false, topLeft, arcSize, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW))
        drawArc(Color(0xFF34A853), 230f, 100f, false, topLeft, arcSize, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW))
    }
}

// ─── PhonePe logo (purple squircle + white "पे") ───
@Composable
private fun PhonePeLogo(modifier: Modifier, cornerRadius: androidx.compose.ui.unit.Dp, textSize: androidx.compose.ui.unit.TextUnit) {
    Box(modifier.clip(RoundedCornerShape(cornerRadius)).background(Color(0xFF5F259F)),
        contentAlignment = Alignment.Center) {
        Text("पे", fontSize = textSize, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

// ─── Icons ───
@Composable
private fun BackArrowWhite() {
    androidx.compose.foundation.Canvas(Modifier.size(24.dp)) {
        val c = Color.White
        val w = 2.4.dp.toPx()
        drawLine(c, androidx.compose.ui.geometry.Offset(size.width * .82f, size.height * .5f),
            androidx.compose.ui.geometry.Offset(size.width * .18f, size.height * .5f), w)
        drawLine(c, androidx.compose.ui.geometry.Offset(size.width * .18f, size.height * .5f),
            androidx.compose.ui.geometry.Offset(size.width * .46f, size.height * .22f), w)
        drawLine(c, androidx.compose.ui.geometry.Offset(size.width * .18f, size.height * .5f),
            androidx.compose.ui.geometry.Offset(size.width * .46f, size.height * .78f), w)
    }
}

@Composable
private fun BackArrowBlack() {
    androidx.compose.foundation.Canvas(Modifier.size(26.dp)) {
        val c = Color(0xFF111827)
        val w = 2.5.dp.toPx()
        drawLine(c, androidx.compose.ui.geometry.Offset(size.width * .82f, size.height * .5f),
            androidx.compose.ui.geometry.Offset(size.width * .18f, size.height * .5f), w)
        drawLine(c, androidx.compose.ui.geometry.Offset(size.width * .18f, size.height * .5f),
            androidx.compose.ui.geometry.Offset(size.width * .46f, size.height * .22f), w)
        drawLine(c, androidx.compose.ui.geometry.Offset(size.width * .18f, size.height * .5f),
            androidx.compose.ui.geometry.Offset(size.width * .46f, size.height * .78f), w)
    }
}

@Composable
private fun HistoryIconWhite() {
    androidx.compose.foundation.Canvas(Modifier.size(23.dp)) {
        val c = Color.White
        val st = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 2.dp.toPx(), join = androidx.compose.ui.graphics.StrokeJoin.Round
        )
        val p = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * .58f, size.height * .08f)
            lineTo(size.width * .27f, size.height * .08f)
            cubicTo(size.width * .18f, size.height * .08f, size.width * .14f, size.height * .13f,
                    size.width * .14f, size.height * .23f)
            lineTo(size.width * .14f, size.height * .87f)
            cubicTo(size.width * .14f, size.height * .96f, size.width * .19f, size.height * .99f,
                    size.width * .28f, size.height * .99f)
            lineTo(size.width * .79f, size.height * .99f)
            cubicTo(size.width * .88f, size.height * .99f, size.width * .92f, size.height * .94f,
                    size.width * .92f, size.height * .85f)
            lineTo(size.width * .92f, size.height * .34f)
            close()
        }
        drawPath(p, c, style = st)
        drawLine(c, androidx.compose.ui.geometry.Offset(size.width * .58f, size.height * .08f),
            androidx.compose.ui.geometry.Offset(size.width * .58f, size.height * .34f), st.width)
        drawLine(c, androidx.compose.ui.geometry.Offset(size.width * .58f, size.height * .34f),
            androidx.compose.ui.geometry.Offset(size.width * .92f, size.height * .34f), st.width)
        drawLine(c, androidx.compose.ui.geometry.Offset(size.width * .36f, size.height * .51f),
            androidx.compose.ui.geometry.Offset(size.width * .70f, size.height * .51f), st.width)
        drawLine(c, androidx.compose.ui.geometry.Offset(size.width * .36f, size.height * .68f),
            androidx.compose.ui.geometry.Offset(size.width * .70f, size.height * .68f), st.width)
    }
}
