package com.hurry.voicechat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

private const val DAISY_IMAGE = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/1785612362650~2.jpg"
private val SupportBlue = Color(0xFF3B82F6)
private val SupportBg = Color(0xFFF0F2F5)
private data class SupportMessage(val text: String, val isBot: Boolean, val image: String? = null)

@Composable
fun HurrySupport(onBack: () -> Unit) {
    var draft by remember { mutableStateOf("") }
    var typing by remember { mutableStateOf(false) }
    val messages = remember {
        mutableStateListOf(
            SupportMessage("Welcome to Zyora Support\n\nI am Daisy, your Official Customer Support Assistant.\n\nHow can I help you today? Aap Zyora App se juda koi bhi sawal pooch sakte hain!", true)
        )
    }
    val keyboard = LocalSoftwareKeyboardController.current

    fun answer(raw: String): SupportMessage {
        val msg = raw.lowercase().trim()
        val yesNo = msg.startsWith("can i") || msg.startsWith("can we") || msg.startsWith("is it") ||
            msg.startsWith("is there") || msg.startsWith("do you") || msg.startsWith("kya ") ||
            msg.contains("ho sakta hai") || msg.contains("kya main") || msg.contains("kya room") ||
            msg.contains("kya coin") || msg.contains("kya hum")
        val prefix = when {
            msg.contains("kya") || msg.contains("ho sakta") -> "Yes, bilkul! "
            yesNo -> "Yes, absolutely! "
            else -> ""
        }
        return when {
            listOf("seller", "become seller", "seller lena", "seller banna", "seller ki policy").any { msg.contains(it) } ->
                SupportMessage("Here is our Official Seller Policy. Please check the policy image below:", true, "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/file_00000000a4a882089e08989f17098062.png")
            listOf("hi", "hello", "hlw", "hey", "नमस्ते", "नमस्कार", "good morning", "good evening").any { msg.contains(it) } ->
                SupportMessage("Hello!\n\nWelcome to Zyora Support. I am Daisy, your AI Customer Support Assistant.\n\nKaise help kar sakti hoon aaj aapki? Please feel free to ask!", true)
            listOf("thank", "thanks", "thx", "धन्यवाद", "शुक्रिया").any { msg.contains(it) } ->
                SupportMessage("You're most welcome!\n\nHelp karke khushi hui. Agar koi aur doubt ho toh zaroor batayein!", true)
            listOf("rate", "coin", "price", "recharge", "dollar", "payment", "paytm", "phonepe", "google play", "purchase").any { msg.contains(it) } ->
                SupportMessage(prefix + "Zyora App Coins Rates & Recharge Info\n\nCoins Exchange Rates:\n• Offline (Through Official Seller):\n  1$ = 1,050,000 Coins\n\n• Online Recharge (In-App Wallet):\n  1$ = 1,000,000 Coins (+30,000 Bonus Coins)\n  (Total: 1,030,000 Coins aapke Wallet me show honge)\n\nOnline Recharge Kaise Karein:\nStep 1: Profile section me jayein.\nStep 2: Coins / Wallet par tap karein.\nStep 3: Package & preferred payment method select karein.\n\nPayment Methods: Google Play, PhonePe, Paytm & Official Site.\n\nOffline Seller se buy karne ke liye Official Team se contact karein.", true)
            listOf("music", "song", "room music", "संगीत", "गाना", "म्यूजिक", "dj").any { msg.contains(it) } ->
                SupportMessage(prefix + "Room Me Music Kaise Play Karein:\n\nStep 1: Apna room open karein.\nStep 2: Bottom me 4-Grid Option (4 squares icon) par tap karein.\nStep 3: Music Play option par click karein.\nStep 4: Song select karke add karein aur Play dabaayein.\n\nRoom members ke saath music enjoy karein!", true)
            listOf("game", "gaming", "खेल", "गेम", "khel").any { msg.contains(it) } ->
                SupportMessage(prefix + "Games Khelkar Coins Kaise Jeetein:\n\nStep 1: Room open karein.\nStep 2: Bottom me 4-Grid Option par tap karein.\nStep 3: Game Remote icon choose karein.\nStep 4: Kisi bhi game ko select karke dosto ke saath khele!", true)
            listOf("create room", "open room", "new room", "रूम", "कमरा", "room banao", "room create", "room kholo").any { msg.contains(it) } ->
                SupportMessage(prefix + "Apna Room Kaise Banayein:\n\nStep 1: Homepage par jayein.\nStep 2: Top me Mine tab par tap karein.\nStep 3: + Icon Card par click karein.\nStep 4: Room me enter karke apne hisab se customize karein.", true)
            listOf("abuse", "gali", "गाली", "complain", "report", "शिकायत", "bad word", "ban", "harass").any { msg.contains(it) } ->
                SupportMessage("Complaint & Report Kaise Karein:\n\nStep 1: Help & Feedback section me jayein.\nStep 2: Clear video ya screenshot proof upload karein.\nStep 3: Official Team ko complaint submit karein.\n\nRules & Warning:\nKripya khud abusive language use na karein. Agar dono side se gali di gayi ho, toh dono IDs ban ho sakti hain.", true)
            listOf("what is zyora", "zyora app", "app kya", "about zyora").any { msg.contains(it) } ->
                SupportMessage("About Zyora App:\n\nZyora ek Social Dating & Live Party Platform hai jahan aap naye dosto se mil sakte hain, party rooms join kar sakte hain aur games khel sakte hain.\n\nKey Features:\n• Live Audio Party Rooms\n• Interactive Mini Games\n• Family Creation System\n• Social Dating & Chatting", true)
            listOf("official", "admin", "contact", "officer", "owner", "team", "अधिकारी", "मालिक").any { msg.contains(it) } ->
                SupportMessage("Zyora Official Team Details:\n\n• Zyora_IN\n  ID: 100002\n\n• Paras\n  ID: 100003\n\nSeller Registration, Payment issues ya Serious Complaints ke liye Official Team se contact karein.", true)
            listOf("family", "परिवार", "family tag", "family create", "parivaar").any { msg.contains(it) } ->
                SupportMessage(prefix + "Family Kaise Banayein:\n\nSteps:\n• Step 1: Profile me jayein.\n• Step 2: Family option par tap karein.\n• Step 3: Family Name & Banner Image set karein.\n• Step 4: Confirm karke create karein.\n\nCost: 1,300,000 Coins\nBenefits: Public Profile par Custom Family Tag milega aur exclusive privileges milenge.", true)
            listOf("name change", "dp change", "profile picture", "bio", "cover", "age", "edit profile", "change name", "change dp").any { msg.contains(it) } ->
                SupportMessage(prefix + "Profile Details Edit Kaise Karein:\n\nStep 1: Profile open karein.\nStep 2: Top Right Arrow click karke Public Profile me jayein.\nStep 3: Top-Right corner me Pencil Icon par click karein.\nStep 4: Yahan se Name, DP, Bio, aur Age change karein.\n\nCover Photo: Level 24 achieve hone par unlock hoti hai.", true)
            msg.contains("login") || msg.contains("log in") || msg.contains("sign in") || msg.contains("account") || msg.contains("otp") || msg.contains("verification") || msg.contains("password") ->
                SupportMessage("Zyora Account & Login Help\\n\\n• Login screen par available sign-in method use karein.\\n• OTP ya Google sign-in fail ho to internet check karke app restart karein.\\n• Password, OTP ya verification code kisi ke saath share na karein.\\n• Issue continue ho to Help & Feedback me screenshot aur exact error bhejein.", true)
            msg.contains("wallet") || msg.contains("balance") || msg.contains("coin missing") || msg.contains("coins missing") || msg.contains("coin reset") || msg.contains("insufficient") ->
                SupportMessage("Zyora Wallet Help\\n\\n• Mine/Profile se Wallet kholkar current balance check karein.\\n• Gift, Lucky Bag ya recharge ke baad balance galat lage to transaction screenshot, time aur account ID note karein.\\n• App restart karke dobara check karein; issue rahe to Help & Feedback me report karein. Daisy wallet manually credit nahi kar sakti.", true)
            msg.contains("gift") || msg.contains("diamond") || msg.contains("lucky bag") || msg.contains("lucky") ->
                SupportMessage("Gifts & Lucky Bag\\n\\n• Room me Gift option kholkar available gift select karein.\\n• Send karne se pehle coin cost aur recipient check karein.\\n• Lucky Bag ke displayed cost/rules check karke balance confirm karein.\\n• Issue ho to room, time, amount aur screenshot ke saath Help & Feedback me report karein.", true)
            msg.contains("level") || msg.contains("experience") || msg.contains("exp") || msg.contains("cover photo") ->
                SupportMessage("Zyora Levels & Rewards\\n\\n• Profile me current level aur progress check karein.\\n• Profile items/cover options ki requirements item par check karein.\\n• Reward unlock na ho to refresh karke screenshot ke saath Help & Feedback me report karein.", true)
            msg.contains("check in") || msg.contains("check-in") || msg.contains("daily reward") || msg.contains("daily check") ->
                SupportMessage("Daily Check-in\\n\\n• Mine/Profile me Daily Check-in option kholein.\\n• Screen par diye gaye button se aaj ka available reward claim karein.\\n• Reward na mile to date/time aur screenshot ke saath Help & Feedback me report karein.", true)
            msg.contains("seat") || msg.contains("microphone") || msg.contains("mic") || msg.contains("mute") || msg.contains("speak") || msg.contains("audio") || msg.contains("headphone") || msg.contains("bluetooth") ->
                SupportMessage("Room Seats & Voice Help\\n\\n• Room me available seat par tap karein; bolne ke liye mic permission aur unmuted mic zaroori ho sakta hai.\\n• Phone Settings me Zyora ki Microphone permission allow karein.\\n• Bluetooth/headphones connect karke audio output check karein.\\n• Awaaz na aaye to mute status, Bluetooth device aur internet check karein; issue rahe to report karein.", true)
            msg.contains("follow") || msg.contains("follower") || msg.contains("following") || msg.contains("friend") ->
                SupportMessage("Friends & Follow\\n\\n• User ki Public Profile kholkar available Follow/Unfollow option use karein.\\n• User ID verify karein taaki sahi profile follow ho.\\n• List update na ho to refresh karein; issue rahe to Help & Feedback me report karein.", true)
            msg.contains("delete account") || msg.contains("deactivate") || msg.contains("privacy") || msg.contains("personal data") ->
                SupportMessage("Privacy & Account Deletion\\n\\n• Mine/Profile ke Settings section me Privacy Policy aur Delete Account options check karein.\\n• Delete karne se pehle confirmation screen dhyan se padhein; deletion permanent ho sakta hai.\\n• Password ya OTP public chat me share na karein.", true)
            msg.contains("avatar") || msg.contains("dp") || msg.contains("public profile") || msg.contains("edit name") ->
                SupportMessage("Zyora Public Profile Help\\n\\n• Apni profile kholkar Public Profile par jayein.\\n• Edit/Pencil option available ho to Name, profile photo, Bio aur Age update karein.\\n• Changes save/confirm karna na bhoolein.", true)
            msg.contains("crash") || msg.contains("not working") || msg.contains("bug") || msg.contains("error") || msg.contains("problem") ->
                SupportMessage("Zyora Troubleshooting\\n\\n1. Internet connection check karein.\\n2. App close karke dobara open karein.\\n3. App update available ho to update karein.\\n4. Error ka screenshot aur exact steps note karein.\\n5. Help & Feedback me report karein. OTP/password share na karein.", true)
            msg.contains("block") || msg.contains("harass") || msg.contains("fake account") || msg.contains("safety") ->
                SupportMessage("Safety & Reporting\\n\\n• Harassment, impersonation ya abusive behaviour ke liye Report/Help & Feedback use karein.\\n• Screenshot/video, user ID aur incident time provide karein.\\n• Personal information, OTP ya password share na karein.", true)
            yesNo -> SupportMessage("Yes! Aap aisa kar sakte hain. Thoda aur details me batayein taaki main sahi guide kar sakoon.", true)
            else -> SupportMessage("Thank you for reaching out!\n\nMain aapki kya help kar sakti hoon? Aap in topics ke bare me pooch sakte hain:\n\n• Coin Rates & Online Recharge\n• Official Seller Policy\n• Room Me Song Play Karna\n• Games Khelna\n• Room Create Karna\n• Misconduct Report Karna\n• Official Team Support\n• Family Tag Option\n• Profile Edit Options", true)
        }
    }

    fun sendMessage() {
        val text = draft.trim()
        if (text.isEmpty() || typing) return
        if (listOf("gandu", "chutiya", "bhosdike", "madarchod", "behenchod", "fuck", "shit", "bitch").any { text.lowercase().split(Regex("\\s+")).contains(it) }) {
            draft = ""
            keyboard?.hide()
            return
        }
        messages.add(SupportMessage(text, false))
        draft = ""
        keyboard?.hide()
        typing = true
    }

    LaunchedEffect(messages.size, typing) {
        if (typing) {
            delay(1000)
            val last = messages.lastOrNull { !it.isBot }?.text.orEmpty()
            messages.add(answer(last))
            typing = false
        }
    }

    // Yahan imePadding() add kiya hai jisse keyboard open hone par pura chat input bar upar jayega
    Column(Modifier.fillMaxSize().background(SupportBg).imePadding()) {
        Row(
            Modifier.fillMaxWidth()
                // Top blue color ko grey ke sath strictly mix kiya gaya hai
                .background(Brush.verticalGradient(listOf(Color(0xFF3B82F6), Color(0xFFF0F2F5))))
                .statusBarsPadding()
                // start padding ko 16.dp se 4.dp kiya hai jisse back icon corner me jaye
                .padding(start = 4.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.width(6.dp)) // Spacing thoda adjust kiya hai bss UI balance ke liye
            Box(Modifier.size(42.dp).clip(CircleShape).background(Brush.horizontalGradient(listOf(Color(0xFFF472B6), Color(0xFFA855F7)))).border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape), contentAlignment = Alignment.Center) {
                AsyncImage(model = DAISY_IMAGE, contentDescription = "Daisy", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Daisy", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 21.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF4ADE80)))
                    Spacer(Modifier.width(5.dp))
                    Text("Online", color = Color(0xFFDBEAFE), fontSize = 12.sp)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(messages) { _, item ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (item.isBot) Arrangement.Start else Arrangement.End, verticalAlignment = Alignment.Top) {
                    if (item.isBot) {
                        AsyncImage(model = DAISY_IMAGE, contentDescription = "Daisy", modifier = Modifier.padding(end = 8.dp).size(32.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                    }
                    Column(
                        Modifier.widthIn(max = 310.dp)
                            .clip(RoundedCornerShape(topStart = if (item.isBot) 0.dp else 16.dp, topEnd = if (item.isBot) 16.dp else 0.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                            .then(if (item.isBot) Modifier.background(Color.White).border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)) else Modifier.background(SupportBlue))
                            .padding(12.dp)
                    ) {
                        Text(item.text, color = if (item.isBot) Color(0xFF334155) else Color.White, fontSize = 14.sp, lineHeight = 20.sp)
                        if (item.image != null) {
                            Spacer(Modifier.height(8.dp))
                            AsyncImage(model = item.image, contentDescription = "Seller Policy", modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Fit)
                        }
                    }
                }
            }
            if (typing) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = DAISY_IMAGE, contentDescription = "Daisy", modifier = Modifier.padding(end = 8.dp).size(32.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                        Surface(color = Color.White, shape = RoundedCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp), shadowElevation = 1.dp) {
                            Text("●  ●  ●", modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Surface(color = Color.White, shadowElevation = 0.dp) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF9FAFB)).padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.weight(1f).padding(vertical = 6.dp),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 14.sp, color = Color(0xFF1E293B)),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { sendMessage() }),
                        decorationBox = { inner ->
                            if (draft.isEmpty()) Text("Type your message... | अपना संदेश लिखें...", fontSize = 14.sp, color = Color(0xFF94A3B8), maxLines = 1)
                            inner()
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.size(36.dp).clip(CircleShape).background(SupportBlue).clickable { sendMessage() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
    }
}
