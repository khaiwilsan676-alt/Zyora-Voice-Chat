package com.hurry.voicechat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.CircularProgressIndicator
import coil3.compose.AsyncImage
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

private const val SellerApi = "https://zyora-server.onrender.com"
private val SellerText = Color(0xFF1F2937)
private val SellerMuted = Color(0xFF9CA3AF)
private val SellerBlue = Color(0xFF60A5FA)
private val SellerField = Color(0xFFF7F8FA)
private val SellerLine = Color(0xFFF3F4F6)

private data class SellerUser(val uid: String, val accountId: String, val name: String, val avatar: String, val idImage: String)
private data class SellerTransfer(val accountId: String, val name: String, val avatar: String, val amount: Long, val createdAt: Long)

private suspend fun sellerRequest(path: String, method: String = "GET", body: JSONObject? = null): JSONObject = withContext(Dispatchers.IO) {
    val connection = (URL(SellerApi + path).openConnection() as HttpURLConnection).apply {
        requestMethod = method
        connectTimeout = 12000
        readTimeout = 15000
        setRequestProperty("Accept", "application/json")
        if (body != null) {
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
    }
    try {
        if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val response = JSONObject(stream?.bufferedReader()?.use { it.readText() } ?: "{}")
        if (status !in 200..299) throw IllegalStateException(response.optString("error", "Request failed"))
        response
    } finally {
        connection.disconnect()
    }
}

private fun sellerUser(json: JSONObject): SellerUser = SellerUser(
    uid = json.optString("uid"),
    accountId = json.optString("accountId"),
    name = json.optString("name", "User"),
    avatar = json.optString("avatar"),
    idImage = json.optString("idImage")
)

@Composable
fun SellerCenter(
    onBack: () -> Unit = {},
    userId: String = "",
    accountId: String = "",
    ownerName: String = "",
    ownerAvatar: String = ""
) {
    val context = LocalContext.current
    val walletCachePrefs = remember(context) { context.getSharedPreferences("zyora_seller_wallet_cache", android.content.Context.MODE_PRIVATE) }
    val walletAccountKey = accountId.ifBlank { userId }
    var showingDetails by remember { mutableStateOf(false) }
    var showWhatsappDialog by remember { mutableStateOf(false) }
    var showPaymentMethodSheet by remember { mutableStateOf(false) }
    var whatsappNumber by remember(userId) { mutableStateOf("") }
    var paymentMethod by remember(userId) { mutableStateOf("") }
    var isSavingContact by remember { mutableStateOf(false) }
    var targetId by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var owner by remember { mutableStateOf(SellerUser(userId, accountId, ownerName.ifBlank { "User" }, ownerAvatar, "")) }
    var selectedUser by remember { mutableStateOf<SellerUser?>(null) }
    var balance by remember(userId) { mutableStateOf(walletCachePrefs.getLong("balance_$userId", 0L).coerceAtLeast(0L)) }
    var isChecking by remember { mutableStateOf(false) }
    var isTransferring by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var transfers by remember { mutableStateOf<List<SellerTransfer>>(emptyList()) }
    var detailsLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val walletSocket = remember(userId, accountId) {
        runCatching { IO.socket(SellerApi, IO.Options().apply {
            transports = arrayOf("websocket"); reconnection = true
            reconnectionAttempts = Int.MAX_VALUE; reconnectionDelay = 1000; reconnectionDelayMax = 5000
        }) }.getOrNull()
    }
    DisposableEffect(walletSocket, userId, accountId) {
        val registerWallet = { if (userId.isNotBlank()) walletSocket?.emit(
            "register", JSONObject().put("userId", userId).put("accountId", walletAccountKey)
        ) }
        val onConnect = Emitter.Listener { registerWallet() }
        val onBalanceUpdated = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject ?: return@Listener
            val eventUserId = payload.optString("userId")
            val updated = payload.optLong("balance", -1L)
            if (eventUserId in setOf(userId, accountId) && updated >= 0L) {
                scope.launch {
                    balance = updated
                    walletCachePrefs.edit().putLong("balance_$userId", updated)
                        .putLong("last_sync_$userId", System.currentTimeMillis()).apply()
                }
            }
        }
        walletSocket?.on(Socket.EVENT_CONNECT, onConnect)
        walletSocket?.on("seller_wallet_balance_updated", onBalanceUpdated)
        if (walletSocket?.connected() == true) registerWallet() else walletSocket?.connect()
        onDispose {
            walletSocket?.off(Socket.EVENT_CONNECT, onConnect)
            walletSocket?.off("seller_wallet_balance_updated", onBalanceUpdated)
            walletSocket?.disconnect(); walletSocket?.close()
        }
    }

    LaunchedEffect(userId) {
        if (userId.isBlank()) return@LaunchedEffect
        runCatching {
            val encoded = URLEncoder.encode(userId, "UTF-8")
            val profile = sellerRequest("/api/users?uid=$encoded").optJSONObject("user")
                ?: sellerRequest("/api/users?uid=$encoded").optJSONArray("users")?.optJSONObject(0)
            if (profile != null) {
                owner = SellerUser(
                    uid = profile.optString("uid", userId),
                    accountId = profile.optString("accountId", accountId),
                    name = profile.optString("name", ownerName.ifBlank { "User" }),
                    avatar = profile.optString("image", ownerAvatar),
                    idImage = profile.optString("idImage", profile.optString("idCardImage"))
                )
                whatsappNumber = profile.optString("whatsapp", profile.optString("whatsApp", profile.optString("whatsappNumber")))
                paymentMethod = profile.optString("paymentMethod", profile.optString("payment_method", profile.optString("sellerPaymentMethod")))
            }
            balance = sellerRequest("/api/seller/wallet/$encoded").optLong("balance", 0L).coerceAtLeast(0L)
            walletCachePrefs.edit().putLong("balance_$userId", balance).apply()
        }.onFailure { statusMessage = it.message ?: "Could not load wallet" }
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SellerBackArrow { if (showingDetails) showingDetails = false else onBack() }
            Text(
                if (showingDetails) "Details" else "Coin Seller Center",
                modifier = Modifier.weight(1f).padding(end = 24.dp),
                fontSize = 17.sp, fontWeight = FontWeight.Bold, color = SellerText,
                textAlign = TextAlign.Center, letterSpacing = 0.3.sp
            )
        }
        if (showingDetails) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                LaunchedEffect(userId, showingDetails) {
                    if (!showingDetails || userId.isBlank()) return@LaunchedEffect
                    detailsLoading = true
                    runCatching {
                        val encoded = URLEncoder.encode(userId, "UTF-8")
                        val rows = sellerRequest("/api/seller/transfers?senderId=$encoded").optJSONArray("transfers")
                        transfers = (0 until (rows?.length() ?: 0)).mapNotNull { index ->
                            val row = rows?.optJSONObject(index) ?: return@mapNotNull null
                            SellerTransfer(
                                row.optString("recipientAccountId"),
                                row.optString("recipientName", "User"),
                                row.optString("recipientAvatar"),
                                row.optLong("amount"),
                                row.optLong("createdAt")
                            )
                        }
                    }.onFailure { statusMessage = it.message ?: "Could not load transfer history" }
                    detailsLoading = false
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Order Type", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SellerText)
                    Spacer(Modifier.width(8.dp))
                    Text("Transfer", fontSize = 14.sp, color = SellerMuted)
                }
                if (detailsLoading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = SellerBlue)
                if (!detailsLoading && transfers.isEmpty()) Text("No transfer records", fontSize = 13.sp, color = SellerMuted, modifier = Modifier.padding(vertical = 20.dp))
                transfers.forEach { record ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text("Transfer", Modifier.background(Color(0xFFDBEAFE), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 3.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("To User", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SellerText)
                                Spacer(Modifier.width(8.dp))
                                SellerAvatar(record.avatar, record.name, 32)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(record.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SellerText)
                                    Text(record.accountId, fontSize = 10.sp, color = SellerMuted)
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(java.text.DateFormat.getDateTimeInstance().format(java.util.Date(record.createdAt)), fontSize = 12.sp, color = SellerMuted)
                        }
                        Row(Modifier.padding(top = 62.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            AsyncImage(model = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/file_00000000e56882119c217d508b6733dc.png", contentDescription = "Coins", modifier = Modifier.size(16.dp))
                            Text("%,d".format(-record.amount), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SellerText)
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(SellerLine))
                }
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
                Column(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                        SellerAvatar(owner.avatar, owner.name, 48)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(owner.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SellerText)
                            Spacer(Modifier.height(2.dp))
                            if (owner.idImage.isNotBlank()) {
                                AsyncImage(model = owner.idImage, contentDescription = "Account ID", modifier = Modifier.height(22.dp).widthIn(max = 100.dp), contentScale = ContentScale.Fit)
                            } else {
                                Text("ID:${owner.accountId}", fontSize = 12.sp, color = SellerMuted)
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(SellerLine))
                    Row(Modifier.fillMaxWidth().clickable { showWhatsappDialog = true }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("WhatsApp", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = SellerText)
                        Spacer(Modifier.weight(1f))
                        Text(whatsappNumber.ifBlank { "—" } + "   ›", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = SellerText)
                    }
                    Row(Modifier.fillMaxWidth().clickable { showPaymentMethodSheet = true }.padding(top = 4.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Payment Method", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = SellerText)
                        Spacer(Modifier.weight(1f))
                        SellerPaymentMethodValue(paymentMethod)
                        Text("   ›", fontSize = 17.sp, color = SellerText)
                    }
                }
                Box(Modifier.fillMaxWidth().height(8.dp).background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp)))
                Column(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(model = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/file_00000000e56882119c217d508b6733dc.png", contentDescription = "Coins", modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("%,d".format(balance), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = SellerText)
                            }
                            Spacer(Modifier.height(2.dp))
                            Text("Available Balance", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = SellerMuted)
                        }
                        Row(Modifier.clickable { showingDetails = true }.padding(3.dp), verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Icon(Icons.Filled.Description, contentDescription = "Details", tint = SellerBlue, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Details", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SellerBlue)
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(SellerLine))
                    Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Total Balance:", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6B7280))
                        Spacer(Modifier.weight(1f))
                        AsyncImage(model = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/file_00000000e56882119c217d508b6733dc.png", contentDescription = "Coins", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("%,d".format(balance), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SellerText)
                    }
                    Row(Modifier.fillMaxWidth().padding(bottom = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Sales method:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SellerText)
                        Spacer(Modifier.width(24.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(16.dp).border(2.dp, SellerBlue, CircleShape), contentAlignment = Alignment.Center) {
                                Box(Modifier.size(8.dp).background(SellerBlue, CircleShape))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("User", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = SellerText)
                        }
                    }
                    Text("User ID:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SellerText)
                    Spacer(Modifier.height(8.dp))
                    SellerInput(value = targetId, onValueChange = { targetId = it.filter(Char::isDigit); selectedUser = null; statusMessage = "" }, hint = "Please input the id", modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), trailing = if (isChecking) "..." else "Check", onTrailingClick = {
                        if (targetId.isNotBlank() && !isChecking) scope.launch {
                            isChecking = true; selectedUser = null; statusMessage = ""
                            runCatching {
                                val encoded = URLEncoder.encode(targetId, "UTF-8")
                                val found = sellerRequest("/api/seller/user/$encoded").getJSONObject("user")
                                selectedUser = sellerUser(found)
                            }.onFailure { statusMessage = it.message ?: "User not found" }
                            isChecking = false
                        }
                    })
                    selectedUser?.let { found ->
                        Row(Modifier.fillMaxWidth().padding(bottom = 16.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFF7F8FA)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            SellerAvatar(found.avatar, found.name, 38)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(found.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SellerText)
                                Text("ID:${found.accountId}", fontSize = 11.sp, color = SellerMuted)
                            }
                        }
                    }
                    Text("Amount:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SellerText)
                    Spacer(Modifier.height(8.dp))
                    SellerInput(value = amount, onValueChange = { amount = it.filter(Char::isDigit); statusMessage = "" }, hint = "Please input the number", modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
                    if (amount.toLongOrNull() != null && amount.toLong() > balance) {
                        Text("Insufficient balance", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFFEF4444), modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
                    }
                    if (statusMessage.isNotBlank()) {
                        Text(statusMessage, fontSize = 12.sp, color = Color(0xFFDC2626), modifier = Modifier.padding(bottom = 10.dp))
                    }
                    Box(
                        Modifier.fillMaxWidth().height(48.dp)
                            .background(if (selectedUser != null && amount.toLongOrNull()?.let { it > 0 && it <= balance } == true && !isTransferring) SellerBlue else Color(0xFFBFDBFE), CircleShape)
                            .clickable(enabled = selectedUser != null && amount.toLongOrNull()?.let { it > 0 && it <= balance } == true && !isTransferring) {
                                val recipient = selectedUser ?: return@clickable
                                val coins = amount.toLongOrNull() ?: return@clickable
                                scope.launch {
                                    isTransferring = true; statusMessage = ""
                                    runCatching {
                                        val response = sellerRequest("/api/seller/transfer", "POST", JSONObject()
                                            .put("senderId", userId)
                                            .put("recipientAccountId", recipient.accountId)
                                            .put("amount", coins)
                                            .put("transferId", UUID.randomUUID().toString()))
                                        balance = response.optLong("balance", (balance - coins)).coerceAtLeast(0L)
                                        walletCachePrefs.edit().putLong("balance_$userId", balance).putLong("last_sync_$userId", System.currentTimeMillis()).apply()
                                        amount = ""; targetId = ""; selectedUser = null
                                        statusMessage = "Transfer successful"
                                    }.onFailure { statusMessage = it.message ?: "Transfer failed" }
                                    isTransferring = false
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isTransferring) "Transferring..." else "Transfer", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    if (showWhatsappDialog) {
        Dialog(onDismissRequest = { if (!isSavingContact) showWhatsappDialog = false }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).padding(18.dp)) {
                Text("WhatsApp", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = SellerText)
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF1F2F4)).padding(horizontal = 13.dp), contentAlignment = Alignment.CenterStart) {
                    if (whatsappNumber.isEmpty()) Text("Enter WhatsApp number", fontSize = 14.sp, color = SellerMuted)
                    BasicTextField(value = whatsappNumber,
                        onValueChange = { whatsappNumber = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' || ch == '-' }.take(20) },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = SellerText))
                }
                Spacer(Modifier.height(18.dp))
                Box(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(8.dp)).background(SellerBlue)
                    .clickable(enabled = !isSavingContact) {
                        scope.launch {
                            isSavingContact = true
                            runCatching {
                                sellerRequest("/api/users", "PUT", JSONObject().put("uid", userId).put("id", userId)
                                    .put("appLongId", userId).put("accountId", accountId).put("whatsapp", whatsappNumber.trim()))
                                showWhatsappDialog = false
                            }.onFailure { statusMessage = it.message ?: "Could not save WhatsApp number" }
                            isSavingContact = false
                        }
                    }, contentAlignment = Alignment.Center) {
                    Text(if (isSavingContact) "Saving..." else "Save", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
    if (showPaymentMethodSheet) {
        SellerPaymentMethodSheet(onClose = { showPaymentMethodSheet = false }, onSelect = { selected ->
            scope.launch {
                isSavingContact = true
                runCatching {
                    sellerRequest("/api/users", "PUT", JSONObject().put("uid", userId).put("id", userId)
                        .put("appLongId", userId).put("accountId", accountId).put("paymentMethod", selected))
                    paymentMethod = selected
                    showPaymentMethodSheet = false
                }.onFailure { statusMessage = it.message ?: "Could not save payment method" }
                isSavingContact = false
            }
        })
    }
}

@Composable
private fun SellerPaymentMethodValue(method: String) {
    when (method.trim().lowercase()) {
        "gpay", "google pay", "googlepay" -> AsyncImage("https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/IMG_20261030_135231.jpg", null, Modifier.size(22.dp), contentScale = ContentScale.Fit)
        "phonepe", "phone pe" -> AsyncImage("https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/IMG_20261030_135211.jpg", null, Modifier.size(22.dp), contentScale = ContentScale.Fit)
        "paytm" -> AsyncImage("https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/IMG_20261030_135245.jpg", null, Modifier.size(22.dp), contentScale = ContentScale.Fit)
        else -> Text(method.ifBlank { "—" }, fontSize = 13.sp, color = SellerText)
    }
}

@Composable
private fun SellerAvatar(url: String, name: String, size: Int) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(Color(0xFFF3F4F6)), contentAlignment = Alignment.Center) {
        if (url.isNotBlank()) AsyncImage(model = url, contentDescription = name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(name.firstOrNull()?.uppercase() ?: "U", fontSize = (size / 2.5f).sp, fontWeight = FontWeight.Bold, color = SellerBlue)
    }
}

@Composable
private fun SellerBackArrow(onClick: () -> Unit) {
    Box(Modifier.size(32.dp).clickable { onClick() }, contentAlignment = Alignment.Center) {
        androidx.compose.material3.Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun SellerInput(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    onTrailingClick: (() -> Unit)? = null
) {
    Row(modifier.background(SellerField, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(hint, fontSize = 14.sp, color = Color(0xFF9CA3AF))
            BasicTextField(value = value, onValueChange = onValueChange, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = SellerText), modifier = Modifier.fillMaxWidth())
        }
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            Text(trailing, Modifier.clickable(enabled = onTrailingClick != null) { onTrailingClick?.invoke() }, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = SellerBlue)
        }
    }
}
