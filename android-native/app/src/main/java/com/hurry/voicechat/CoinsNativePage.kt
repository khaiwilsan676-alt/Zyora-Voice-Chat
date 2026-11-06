package com.hurry.voicechat

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.socket.client.IO
import io.socket.emitter.Emitter

import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val RAW_WALLET = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/"

// Real App Logos
private const val GPAY_LOGO = RAW_WALLET + "IMG_20261030_135231.jpg"
private const val PHONEPE_LOGO = RAW_WALLET + "IMG_20261030_135211.jpg"
private const val PAYTM_LOGO = RAW_WALLET + "IMG_20261030_135245.jpg"
private const val DIAMOND_ICON = "file_00000000552c81f4b6f31ba13346015e.png"

// ═══════════════════════════════════════════════════════
// ROOM DB
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

private suspend fun fetchCanonicalWallet(userId: String): JSONObject? = withContext(Dispatchers.IO) {
    runCatching {
        val encoded = URLEncoder.encode(userId, "UTF-8")
        val connection = (URL("https://zyora-server.onrender.com/api/lucky-bag/wallet/$encoded").openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000
            readTimeout = 12000
        }
        try {
            if (connection.responseCode !in 200..299) return@runCatching null
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally { connection.disconnect() }
    }.getOrNull()
}

private suspend fun exchangeCanonicalWallet(userId: String, diamonds: Long): JSONObject? = withContext(Dispatchers.IO) {
    runCatching {
        val connection = (URL("https://zyora-server.onrender.com/api/wallet/exchange").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12000
            readTimeout = 12000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        try {
            val payload = JSONObject().put("userId", userId).put("diamonds", diamonds)
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode !in 200..299) return@runCatching null
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally { connection.disconnect() }
    }.getOrNull()
}

private data class WalletAgentSeller(
    val uid: String,
    val accountId: String,
    val name: String,
    val image: String,
    val online: Boolean,
    val coins: Long,
    val dealLast30Days: String,
    val paymentMethod: String,
    val whatsapp: String
)

private fun JSONObject.firstAgentValue(vararg keys: String): String =
    keys.firstNotNullOfOrNull { key ->
        if (!has(key) || isNull(key)) null else opt(key)?.toString()?.trim()?.takeIf {
            it.isNotBlank() && it != "null" && it != "undefined"
        }
    }.orEmpty()

private suspend fun fetchWalletAgentSellers(): List<WalletAgentSeller> = withContext(Dispatchers.IO) {
    runCatching {
        val connection = (URL("https://zyora-server.onrender.com/api/users?sellers=true").openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000
            readTimeout = 20000
        }
        val response = try {
            if (connection.responseCode !in 200..299) return@runCatching emptyList()
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally { connection.disconnect() }
        val users = response.optJSONArray("users") ?: return@runCatching emptyList()
        val allowedSellerIds = setOf("100002", "100003")
        val rows = (0 until users.length()).mapNotNull { index ->
            val user = users.optJSONObject(index) ?: return@mapNotNull null
            val uid = user.optString("uid").ifBlank { user.optString("id") }
            val account = user.optString("accountId").ifBlank { user.optString("accountNumber") }
            if (uid.isBlank() || account !in allowedSellerIds) return@mapNotNull null
            WalletAgentSeller(
                uid = uid,
                accountId = account,
                name = user.optString("name").ifBlank { "User" },
                image = user.optString("image").ifBlank { "https://zyora-voice-chat.vercel.app/default-avatar.png" },
                online = user.optBoolean("isOnline", false),
                coins = 0L,
                dealLast30Days = user.firstAgentValue("dealLast30Days", "last30DaysDeal", "dealOfLast30Days", "monthlyDeal"),
                paymentMethod = user.firstAgentValue("paymentMethod", "payment_method", "sellerPaymentMethod"),
                whatsapp = user.firstAgentValue("whatsapp", "whatsApp", "whatsappNumber", "whatsAppNumber", "phoneNumber")
            )
        }
        coroutineScope {
            rows.map { seller ->
                async {
                    val byUid = fetchCanonicalWallet(seller.uid)?.optLong("balance", 0L)?.coerceAtLeast(0L)
                    val byAccount = if (seller.accountId != seller.uid) {
                        fetchCanonicalWallet(seller.accountId)?.optLong("balance", 0L)?.coerceAtLeast(0L)
                    } else null
                    seller.copy(coins = maxOf(byUid ?: 0L, byAccount ?: 0L))
                }
            }.map { it.await() }
        }
    }.getOrDefault(emptyList())
}

// ═══════════════════════════════════════════════════════
// PUBLIC ENTRY POINTS
// ═══════════════════════════════════════════════════════
@Composable fun CoinsNativePage(onBack: () -> Unit, onChat: (String, String, String) -> Unit = { _, _, _ -> }) = WalletNativePage(onBack, 0, onChat)
@Composable fun DiamondsNativePage(onBack: () -> Unit) = WalletNativePage(onBack, 1)

// ═══════════════════════════════════════════════════════
// MAIN WALLET PAGE
// ═══════════════════════════════════════════════════════
@Composable
private fun WalletNativePage(onBack: () -> Unit, initialTab: Int, onChat: (String, String, String) -> Unit = { _, _, _ -> }) {
    val context = LocalContext.current
    val dao = remember { WalletDatabase.get(context).walletDao() }
    val scope = rememberCoroutineScope()
    val authPrefs = remember { context.getSharedPreferences("hurry_auth", Context.MODE_PRIVATE) }
    val walletCachePrefs = remember {
        context.getSharedPreferences("zyora_wallet_cache", Context.MODE_PRIVATE)
    }
    
    val walletUserId = remember {
        authPrefs.getString("uid", null)?.takeIf { it.isNotBlank() }
            ?: authPrefs.getString("accountId", "").orEmpty()
    }

    var tab by remember { mutableStateOf(initialTab) }
    // Render the last known wallet snapshot immediately; never flash zero while refreshes run.
    var coins by remember(walletUserId) {
        mutableStateOf(walletCachePrefs.getLong("balance_$walletUserId", 0L).coerceAtLeast(0L))
    }
    var diamonds by remember(walletUserId) {
        mutableStateOf(walletCachePrefs.getLong("diamonds_$walletUserId", 0L).coerceAtLeast(0L))
    }
    var din by remember { mutableStateOf("") }
    var cin by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("100%") }

    var showPaymentSheet by remember { mutableStateOf(false) }
    var showPayUsingSheet by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(walletUserId) {
        if (walletUserId.isBlank()) return@LaunchedEffect

        val balanceKey = "balance_$walletUserId"
        val diamondsKey = "diamonds_$walletUserId"
        val syncKey = "last_sync_$walletUserId"

        // Only fall back to Room when this account has no saved wallet snapshot.
        // Existing cached values remain visible while any refresh is in flight.
        if (!walletCachePrefs.contains(balanceKey) || !walletCachePrefs.contains(diamondsKey)) {
            val localBalance = loadBalance(dao)
            val localDiamonds = loadDiamonds(dao)
            coins = localBalance
            diamonds = localDiamonds
            walletCachePrefs.edit()
                .putLong(balanceKey, localBalance)
                .putLong(diamondsKey, localDiamonds)
                .apply()
        }

        // Avoid repeating the same wallet request when the page is reopened rapidly.
        val lastSync = walletCachePrefs.getLong(syncKey, 0L)
        if (System.currentTimeMillis() - lastSync < 15_000L) return@LaunchedEffect

        val wallet = fetchCanonicalWallet(walletUserId) ?: return@LaunchedEffect
        val serverBalance = wallet.optLong("balance").coerceAtLeast(0L)
        val serverDiamonds = wallet.optLong("diamonds").coerceAtLeast(0L)

        dao.saveUserData(UserDataEntity(balance = serverBalance, diamonds = serverDiamonds))
        walletCachePrefs.edit()
            .putLong(balanceKey, serverBalance)
            .putLong(diamondsKey, serverDiamonds)
            .putLong(syncKey, System.currentTimeMillis())
            .apply()

        // Update only the balance fields; the rest of the wallet UI stays mounted.
        if (coins != serverBalance) coins = serverBalance
        if (diamonds != serverDiamonds) diamonds = serverDiamonds
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

            // ─── Scrollable Body with Swipe Gesture ───
            var dragX by remember { mutableFloatStateOf(0f) }
            
            Column(Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragX < -40f && tab < 2) tab += 1
                            else if (dragX > 40f && tab > 0) tab -= 1
                            dragX = 0f
                        },
                        onDragCancel = { dragX = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            dragX += dragAmount
                        }
                    )
                }
                .verticalScroll(rememberScrollState())
            ) {

                // ─── Banner ───
                Box(Modifier.fillMaxWidth().then(if (tab == 1) Modifier.padding(horizontal = 8.dp) else Modifier)) {
                    AsyncImage(
                        RAW_WALLET + banner, 
                        null,
                        Modifier.fillMaxWidth(),
                        contentScale = ContentScale.FillWidth
                    )
                    Column(Modifier.padding(start = if (tab == 1) 34.dp else 42.dp, top = 34.dp)) {
                        Text(if (tab == 1) "My Diamonds" else "My Coins",
                            fontSize = 14.sp, color = Color(0xFFE5E7EB), fontWeight = FontWeight.Medium)
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text((if (tab == 1) diamonds else coins).toString(),
                                fontSize = 22.sp, color = Color(0xFFFACC15), fontWeight = FontWeight.Bold)
                            
                            val iconImage = if (tab == 1) DIAMOND_ICON else "file_00000000e56882119c217d508b6733dc.png"
                            AsyncImage(RAW_WALLET + iconImage,
                                null, Modifier.size(20.dp), contentScale = ContentScale.Fit)
                        }
                    }
                }

                // ─── Pill Tabs with Extra 30% Tag ───
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 18.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(30.dp)).background(Color(0x33000000)).padding(4.dp)
                    ) {
                        PillTab("Coins", tab == 0, Modifier.weight(1f)) { tab = 0 }
                        PillTab("Diamonds", tab == 1, Modifier.weight(1f)) { tab = 1 }
                        PillTab("Agent", tab == 2, Modifier.weight(1f)) { tab = 2 }
                    }

                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 12.dp)
                            .offset(y = (-14).dp)
                    ) {
                        Box(
                            Modifier
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 8.dp,
                                        topEnd = 8.dp,
                                        bottomEnd = 8.dp,
                                        bottomStart = 2.dp
                                    )
                                )
                                .background(Color(0xFF0044FF)) 
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "Extra 30%",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
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
                                    val updated = exchangeCanonicalWallet(walletUserId, d)
                                    if (updated != null) {
                                        val serverBalance = updated.optLong("balance").coerceAtLeast(0L)
                                        val serverDiamonds = updated.optLong("diamonds").coerceAtLeast(0L)
                                        dao.saveUserData(UserDataEntity(balance = serverBalance, diamonds = serverDiamonds))
                                        walletCachePrefs.edit()
                                            .putLong("balance_$walletUserId", serverBalance)
                                            .putLong("diamonds_$walletUserId", serverDiamonds)
                                            .putLong("last_sync_$walletUserId", System.currentTimeMillis())
                                            .apply()
                                        recordTransaction(dao, "Diamond Exchange", -d, "diamond")
                                        din = ""; cin = ""
                                        coins = serverBalance
                                        diamonds = serverDiamonds
                                    }
                                }
                            }
                        }
                    )
                    2 -> AgentContent(onChat = onChat)
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
                                    val updated = exchangeCanonicalWallet(walletUserId, d)
                                    if (updated != null) {
                                        val serverBalance = updated.optLong("balance").coerceAtLeast(0L)
                                        val serverDiamonds = updated.optLong("diamonds").coerceAtLeast(0L)
                                        dao.saveUserData(UserDataEntity(balance = serverBalance, diamonds = serverDiamonds))
                                        walletCachePrefs.edit()
                                            .putLong("balance_$walletUserId", serverBalance)
                                            .putLong("diamonds_$walletUserId", serverDiamonds)
                                            .putLong("last_sync_$walletUserId", System.currentTimeMillis())
                                            .apply()
                                        recordTransaction(dao, "Diamond Exchange", -d, "diamond")
                                        din = ""; cin = ""
                                        coins = serverBalance
                                        diamonds = serverDiamonds
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
                        RAW_WALLET + DIAMOND_ICON,
                        null, Modifier.size(14.dp), contentScale = ContentScale.Fit
                    )
                    Text("33", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6B7280))
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DiamondInputBox("Input multiple", din, Modifier.weight(1f), onDinChange,
                    icon = DIAMOND_ICON, suffix = "x100")
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

// ═══════════════════════════════════════════════════════
// UI UPDATED AGENT CONTENT (COMPACT LIKE IMAGE) 
// ═══════════════════════════════════════════════════════
@Composable
private fun AgentContent(onChat: (String, String, String) -> Unit) {
    val context = LocalContext.current
    var sellers by remember { mutableStateOf<List<WalletAgentSeller>>(emptyList()) }
    var onlineById by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        sellers = fetchWalletAgentSellers().filter { it.accountId == "100002" || it.accountId == "100003" }
        loaded = true
    }

    DisposableEffect(sellers) {
        if (sellers.isEmpty()) return@DisposableEffect onDispose { }
        val socket = runCatching {
            IO.socket("https://zyora-server.onrender.com", IO.Options().apply {
                transports = arrayOf("websocket")
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                reconnectionDelay = 1000
                reconnectionDelayMax = 5000
            })
        }.getOrNull() ?: return@DisposableEffect onDispose { }

        val statusListener = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject ?: return@Listener
            val id = payload.optString("userId")
            val account = payload.optString("accountId")
            val online = payload.optBoolean("online", false)
            sellers.filter { it.uid == id || it.accountId == id || it.uid == account || it.accountId == account }
                .forEach { seller ->
                    onlineById = onlineById + (seller.uid to online) + (seller.accountId to online)
                }
        }
        val onlineListener = Emitter.Listener { args ->
            val id = args.firstOrNull()?.toString().orEmpty()
            sellers.filter { it.uid == id || it.accountId == id }.forEach { seller ->
                onlineById = onlineById + (seller.uid to true) + (seller.accountId to true)
            }
        }
        val offlineListener = Emitter.Listener { args ->
            val id = args.firstOrNull()?.toString().orEmpty()
            sellers.filter { it.uid == id || it.accountId == id }.forEach { seller ->
                onlineById = onlineById + (seller.uid to false) + (seller.accountId to false)
            }
        }
        socket.on("presence_status", statusListener)
        socket.on("user_online", onlineListener)
        socket.on("user_offline", offlineListener)
        socket.connect()
        sellers.forEach { seller ->
            socket.emit("check_presence", seller.uid)
            socket.emit("check_presence", seller.accountId)
        }
        onDispose {
            socket.off("presence_status", statusListener)
            socket.off("user_online", onlineListener)
            socket.off("user_offline", offlineListener)
            socket.disconnect()
            socket.close()
        }
    }

    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        sellers.forEach { seller ->
            val isOnline = onlineById[seller.uid] ?: onlineById[seller.accountId] ?: seller.online
            
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)) // Compact padding and corners
                    .background(Color.White)
                    .padding(14.dp) 
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    
                    // Smaller Avatar Box like image with Border and thin Pill
                    Box(Modifier.size(46.dp), contentAlignment = Alignment.TopCenter) {
                        AsyncImage(
                            model = seller.image,
                            contentDescription = seller.name,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(50))
                                .border(
                                    width = 1.5.dp, 
                                    color = if (isOnline) Color(0xFF0044FF) else Color(0xFFD1D5DB), 
                                    shape = RoundedCornerShape(50)
                                ),
                            contentScale = ContentScale.Crop
                        )
                        // Pill at exact bottom center with patli si padding
                        Box(Modifier.align(Alignment.BottomCenter).offset(y = 2.dp)) {
                            Text(
                                text = if (isOnline) "online" else "offline",
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isOnline) Color(0xFF0044FF) else Color(0xFFD1D5DB))
                                    .border(1.dp, Color.White, RoundedCornerShape(50))
                                    .padding(horizontal = 4.dp, vertical = 0.dp), // Patli si padding
                                fontSize = 8.sp, // Choti font
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(
                            text = seller.name, 
                            fontSize = 15.sp, // Smaller name text
                            fontWeight = FontWeight.Bold, 
                            color = Color(0xFF111827), 
                            maxLines = 1
                        )
                        Row(
                            Modifier.padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Deal of last 30 days:", fontSize = 12.sp, color = Color(0xFF6B7280), maxLines = 1)
                            AsyncImage(
                                model = RAW_WALLET + "file_00000000e56882119c217d508b6733dc.png",
                                contentDescription = "Coins",
                                modifier = Modifier.size(13.dp),
                                contentScale = ContentScale.Fit
                            )
                            Text(
                                seller.dealLast30Days.ifBlank { "—" },
                                fontSize = 12.sp, color = Color(0xFF6B7280), maxLines = 1
                            )
                        }
                    }

                    // Scaled down chat icon
                    AsyncImage(
                        model = RAW_WALLET + "IMG_20261105_125330.png",
                        contentDescription = "Chat",
                        modifier = Modifier
                            .size(36.dp) 
                            .clip(RoundedCornerShape(50))
                            .clickable { onChat(seller.uid, seller.name, seller.image) },
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(Modifier.height(14.dp)) // Less height to make it compact

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Payment method:", fontSize = 13.sp, color = Color(0xFF374151))
                    Spacer(Modifier.weight(1f))
                    
                    val pMethod = seller.paymentMethod.lowercase()
                    when {
                        pMethod.contains("paytm") -> AsyncImage(PAYTM_LOGO, null, Modifier.height(14.dp), contentScale = ContentScale.Fit)
                        pMethod.contains("gpay") || pMethod.contains("google") -> AsyncImage(GPAY_LOGO, null, Modifier.height(14.dp), contentScale = ContentScale.Fit)
                        pMethod.contains("phonepe") -> AsyncImage(PHONEPE_LOGO, null, Modifier.height(14.dp), contentScale = ContentScale.Fit)
                        else -> Text(seller.paymentMethod.ifBlank { "—" }, fontSize = 13.sp, color = Color(0xFF374151), fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Whatsapp:", fontSize = 13.sp, color = Color(0xFF374151))
                    Spacer(Modifier.weight(1f))
                    Text(seller.whatsapp.ifBlank { "—" }, fontSize = 13.sp, color = Color(0xFF374151))
                    
                    if (seller.whatsapp.isNotBlank()) {
                        Text(
                            " ⧉",
                            Modifier.clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("WhatsApp", seller.whatsapp))
                            },
                            fontSize = 15.sp, color = Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        }
        if (loaded && sellers.isEmpty()) {
            Text("No sellers available", Modifier.fillMaxWidth().padding(vertical = 28.dp),
                textAlign = TextAlign.Center, fontSize = 13.sp, color = Color(0xFF9CA3AF))
        }
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

// ═══════════════════════════════════════════════════════
// SHEETS
// ═══════════════════════════════════════════════════════
@Composable
private fun PaymentSheet(onClose: () -> Unit, onRecharge: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Column(Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Color.White)) {

            Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text("Payment Method", Modifier.align(Alignment.Center),
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                
                Row(Modifier.align(Alignment.CenterEnd)
                    .clip(RoundedCornerShape(50.dp)).background(Color(0xFFF3F4F6))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
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
                
                AsyncImage(GPAY_LOGO, null, Modifier.padding(start = 10.dp).size(22.dp).clip(RoundedCornerShape(50)), contentScale = ContentScale.Crop)
                AsyncImage(PHONEPE_LOGO, null, Modifier.padding(start = 8.dp).size(22.dp).clip(RoundedCornerShape(50)), contentScale = ContentScale.Crop)
                AsyncImage(PAYTM_LOGO, null, Modifier.padding(start = 8.dp).size(22.dp).clip(RoundedCornerShape(50)), contentScale = ContentScale.Crop)
                
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(RAW_WALLET + "file_00000000e56882119c217d508b6733dc.png",
                        null, Modifier.size(16.dp), contentScale = ContentScale.Fit)
                    Text("1,030,000", Modifier.padding(start = 4.dp),
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
                }
                
                Box(Modifier.padding(start = 12.dp).size(20.dp)
                    .clip(RoundedCornerShape(50))
                    .border(2.dp, Color(0xFF0044FF), RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(Color(0xFF0044FF)))
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
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Color.White).padding(bottom = 8.dp)) {

            Box(Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 16.dp)) {
                Text("Pay Using", Modifier.align(Alignment.Center),
                    fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
            }
            HorizontalDivider(color = Color(0xFFF9FAFB))

            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                PayRow("GPay", onSelect) {
                    AsyncImage(GPAY_LOGO, null, Modifier.fillMaxSize().clip(RoundedCornerShape(50)), contentScale = ContentScale.Crop)
                }
                HorizontalDivider(color = Color(0xFFF9FAFB))
                PayRow("PhonePe", onSelect) {
                    AsyncImage(PHONEPE_LOGO, null, Modifier.fillMaxSize().clip(RoundedCornerShape(50)), contentScale = ContentScale.Crop)
                }
                HorizontalDivider(color = Color(0xFFF9FAFB))
                PayRow("Paytm", onSelect) {
                    AsyncImage(PAYTM_LOGO, null, Modifier.fillMaxSize().clip(RoundedCornerShape(50)), contentScale = ContentScale.Crop)
                }
                HorizontalDivider(color = Color(0xFFF9FAFB))
                
                PayRow("Other", onSelect) {
                    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(50)).background(Color(0xFFF3F4F6)), contentAlignment = Alignment.Center) {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(4.5.dp).clip(RoundedCornerShape(50)).background(Color(0xFF9CA3AF)))
                            Box(Modifier.size(4.5.dp).clip(RoundedCornerShape(50)).background(Color(0xFF9CA3AF)))
                            Box(Modifier.size(4.5.dp).clip(RoundedCornerShape(50)).background(Color(0xFF9CA3AF)))
                        }
                    }
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
            Modifier.size(44.dp), 
            contentAlignment = Alignment.Center
        ) { icon() }
        Text(name, Modifier.padding(start = 16.dp),
            fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF111827))
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
