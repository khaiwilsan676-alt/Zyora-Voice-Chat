package com.hurry.voicechat

import android.provider.Settings
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Tasks
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val HURRY_API_BASE = "https://zyora-server.onrender.com"
private const val HURRY_VIDEO = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/VID_20261031_074114_256_bsl~2.mp4"
private const val HURRY_LOGO = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/logo.png"
private const val GOOGLE_LOADING_IMAGE = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261031_215612.png"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeLoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var page by remember { mutableStateOf("welcome") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var googleLoading by remember { mutableStateOf(false) }
    var showDeviceRegisteredDialog by remember { mutableStateOf(false) }
    var registeredDeviceAccountId by remember { mutableStateOf("") }
    var registeredDeviceEmail by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    val legacyGoogleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK || result.data == null) {
            loading = false
            error = "Google sign-in was cancelled. Please try again."
        } else {
            scope.launch {
                loading = true
                error = ""
                try {
                    val user = withContext(Dispatchers.IO) { legacyGoogleBackendSignIn(context, result.data!!) }
                    if (isDifferentRegisteredGoogleAccount(user, context)) {
                        val prefs = context.getSharedPreferences("hurry_auth", android.content.Context.MODE_PRIVATE)
                        registeredDeviceAccountId = prefs.getString("registeredGoogleAccountId", "").orEmpty()
                            .ifBlank { prefs.getString("accountId", "").orEmpty() }
                        registeredDeviceEmail = prefs.getString("registeredGoogleEmail", "").orEmpty()
                        showDeviceRegisteredDialog = true
                        FirebaseAuth.getInstance().signOut()
                        return@launch
                    }
                    googleLoading = true
                    withContext(Dispatchers.IO) { syncHurryUser(user, context) }
                    rememberRegisteredGoogleAccount(user, context)
                    context.getSharedPreferences("hurry_auth", android.content.Context.MODE_PRIVATE).edit()
                        .putString("uid", user.optString("localId"))
                        .putString("email", user.optString("email"))
                        .putString("idToken", user.optString("idToken"))
                        .apply()
                    onLoginSuccess()
                } catch (t: Throwable) {
                    error = authErrorText(t.message ?: "Google sign-in failed. Please try again.")
                } finally {
                    googleLoading = false
                    loading = false
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF111827))) {
        val backgroundPlayer = remember(context) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(Uri.parse(HURRY_VIDEO)))
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 0f
                playWhenReady = true
                prepare()
            }
        }
        DisposableEffect(backgroundPlayer) {
            onDispose { backgroundPlayer.release() }
        }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = backgroundPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    isClickable = false
                    isFocusable = false
                }
            },
            update = { view ->
                if (view.player !== backgroundPlayer) view.player = backgroundPlayer
                if (backgroundPlayer.playbackState == Player.STATE_IDLE) backgroundPlayer.prepare()
                if (!backgroundPlayer.isPlaying) backgroundPlayer.play()
            }
        )
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (page == "welcome") .30f else .40f)))
        
        if (showPrivacyPolicy) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF374151),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showPrivacyPolicy = false }
                    )
                    Text(
                        "Privacy Policy",
                        color = Color(0xFF111827),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
                Text("Privacy Policy", color = Color(0xFF111827), fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("Last updated: October 14, 2026", color = Color(0xFF6B7280), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp, bottom = 22.dp))
                PrivacySection("1. About Zyora", "Zyora is a social voice-chat application where people can create profiles, join voice rooms, meet others, chat, and enjoy social features together.")
                PrivacySection("2. Information We Collect", "When you use Zyora, we may handle the details you choose to share, such as your profile name, photo, account details, messages, and activity in the app. Microphone access is used when you participate in live voice rooms. We only describe information that is needed to provide and support the app’s features.")
                PrivacySection("3. How We Use Information", "Your information helps you access your account, maintain your profile, connect with other users, participate in rooms and chats, improve your experience, and help keep Zyora safe. Zyora does not sell your personal information.")
                PrivacySection("4. Your Account and Data", "Your account information and content associated with your account are retained to help keep your Zyora experience consistent over time, including when you return to the app. Deleting the app from your phone does not automatically delete information associated with your account. If you want to request access, correction, or deletion of your account information, contact Zyora Support. Some information may need to be retained where required for safety, security, or legal reasons.")
                PrivacySection("5. App Permissions", "Zyora may request internet access to provide online features and microphone access when you use live voice chat. You can manage app permissions through your device settings.")
                PrivacySection("6. Keeping Your Information Safe", "We take reasonable steps to protect accounts and information and to support a safer community. Keep your sign-in details private and be careful about sharing sensitive information in public rooms. No online service can promise absolute security.")
                PrivacySection("7. Sharing Information", "We do not sell your personal information. Information may be used when necessary to provide Zyora’s features, protect users and the service, respond to support requests, or meet applicable legal obligations.")
                PrivacySection("8. Policy Updates and Contact", "We may update this policy as Zyora evolves. If you have questions or a privacy-related request, reach out to the Zyora team through the support option in the app.")
                Spacer(Modifier.height(30.dp))
            }
        } else if (page == "official_login") {
            // Updated Official Login Screen with proper strong blue 🔵 at top, exactly 25% coverage
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color(0xFF2563EB), // Strong Blue (no sky blue)
                                0.25f to Color(0xFFF3F4F6), // Mixes into Greyish White at exactly 25%
                                1.0f to Color(0xFFF3F4F6)  // Greyish White till bottom
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Back Arrow only (No Forgot Password)
                    Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 40.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    page = "welcome"
                                    email = ""
                                    password = ""
                                    error = ""
                                }
                        )
                    }

                    // Welcome Text
                    Text(
                        text = "👋 Welcome\nSign in with Account",
                        color = Color.Black,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp
                    )
                    
                    Spacer(Modifier.height(40.dp))

                    if (error.isNotBlank()) {
                        Text(error, color = Color.White, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFEF4444)).padding(12.dp))
                        Spacer(Modifier.height(16.dp))
                    }

                    // Account Field (White Box)
                    OfficialAuthField(
                        icon = Icons.Filled.Person,
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "Account",
                        hideText = false,
                        keyboardType = KeyboardType.Email
                    )
                    
                    Spacer(Modifier.height(16.dp))

                    // Password Field (White Box)
                    OfficialAuthField(
                        icon = Icons.Filled.Lock,
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "Password",
                        hideText = true,
                        keyboardType = KeyboardType.Password
                    )

                    Spacer(Modifier.height(48.dp))

                    // Login Button (Blue)
                    Button(
                        onClick = {
                            scope.launch {
                                loading = true
                                error = ""
                                try {
                                    if (email.trim().isEmpty()) throw IllegalArgumentException("Please enter a valid account.")
                                    if (password.length < 6) throw IllegalArgumentException("Password must be at least 6 characters long.")
                                    
                                    val accountInput = email.trim()
                                    val loginEmail = if (accountInput.matches(Regex("\\d{4,12}"))) {
                                        withContext(Dispatchers.IO) { lookupAccountEmail(accountInput) }
                                    } else accountInput
                                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(loginEmail).matches()) {
                                        throw IllegalArgumentException("Enter a valid Zyora Account ID.")
                                    }
                                    val user = withContext(Dispatchers.IO) { backendEmailAuth(context, loginEmail, password, false) }
                                    withContext(Dispatchers.IO) { syncHurryUser(user, context) }
                                    
                                    val authPrefs = context.getSharedPreferences("hurry_auth", android.content.Context.MODE_PRIVATE)
                                    authPrefs.edit()
                                        .putString("uid", user.optString("localId"))
                                        .putString("email", user.optString("email", email.trim()))
                                        .putString("idToken", user.optString("idToken"))
                                        .apply()
                                        
                                    onLoginSuccess()
                                } catch (t: Throwable) {
                                    error = authErrorText(t.message ?: "Authentication failed. Please try again.")
                                } finally {
                                    loading = false
                                }
                            }
                        },
                        enabled = !loading && email.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB), contentColor = Color.White, disabledContainerColor = Color(0xFF2563EB).copy(alpha = 0.5f))
                    ) {
                        if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("Log in", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    
                    Spacer(Modifier.height(30.dp))
                }
            }
        } else if (page == "welcome") {
            Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.weight(0.80f)) 
                
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(model = HURRY_LOGO, contentDescription = "Zyora", modifier = Modifier.size(80.dp).clip(RoundedCornerShape(18.dp)))
                    Text("Zyora", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(Modifier.weight(1.25f)) 
                
                // Google Button - Icon Left, Text Middle
                Button(
                    onClick = {
                        error = ""
                        scope.launch {
                            loading = true
                            try {
                                val user = googleBackendSignIn(context)
                                if (isDifferentRegisteredGoogleAccount(user, context)) {
                                    val prefs = context.getSharedPreferences("hurry_auth", android.content.Context.MODE_PRIVATE)
                                    registeredDeviceAccountId = prefs.getString("registeredGoogleAccountId", "").orEmpty()
                                        .ifBlank { prefs.getString("accountId", "").orEmpty() }
                                    registeredDeviceEmail = prefs.getString("registeredGoogleEmail", "").orEmpty()
                                    showDeviceRegisteredDialog = true
                                    FirebaseAuth.getInstance().signOut()
                                    return@launch
                                }
                                googleLoading = true
                                withContext(Dispatchers.IO) { syncHurryUser(user, context) }
                                rememberRegisteredGoogleAccount(user, context)
                                context.getSharedPreferences("hurry_auth", android.content.Context.MODE_PRIVATE).edit()
                                    .putString("uid", user.optString("localId"))
                                    .putString("email", user.optString("email"))
                                    .putString("idToken", user.optString("idToken"))
                                    .apply()
                                onLoginSuccess()
                            } catch (t: Throwable) {
                                if (t is androidx.credentials.exceptions.NoCredentialException) {
                                    val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestIdToken(context.getString(R.string.default_web_client_id))
                                        .requestEmail()
                                        .build()
                                    legacyGoogleLauncher.launch(GoogleSignIn.getClient(context, options).signInIntent)
                                } else {
                                    error = authErrorText(t.message ?: "Google sign-in failed. Please try again.")
                                }
                            } finally {
                                googleLoading = false
                                loading = false
                            }
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth(0.96f).height(54.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF222222)),
                    contentPadding = PaddingValues(horizontal = 20.dp) // Added padding for edges
                ) {
                    Box(Modifier.fillMaxSize()) {
                        GoogleLoginIcon(Modifier.size(24.dp).align(Alignment.CenterStart))
                        Text("Google", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Center))
                    }
                }
                
                Spacer(Modifier.height(14.dp))
                
                // Account Button - Icon Left, Text Middle
                Button(
                    onClick = { page = "login"; error = "" },
                    modifier = Modifier.fillMaxWidth(0.96f).height(54.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB), contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 20.dp)
                ) {
                    Box(Modifier.fillMaxSize()) {
                        SimpleUserIcon(Modifier.size(24.dp).align(Alignment.CenterStart))
                        Text("Account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Center))
                    }
                }
                
                Spacer(Modifier.height(18.dp))
                
                // Divider Line
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Divider(Modifier.weight(1f), color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)
                    Text(" Or ", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, modifier = Modifier.padding(horizontal = 8.dp))
                    Divider(Modifier.weight(1f), color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)
                }
                
                Spacer(Modifier.height(18.dp))
                
                // Official Login Small Square Card (Thora chota kiya gaya)
                Button(
                    onClick = { page = "official_login"; error = "" }, 
                    modifier = Modifier.size(50.dp), // Square card size reduced
                    shape = RoundedCornerShape(13.dp), // Rounded like iOS style app icon
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF2563EB)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    OfficialLoginIcon(Modifier.size(29.dp)) // Icon size adjusted
                }
                
                if (error.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(error, color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0x66EF4444)).padding(10.dp))
                }
                Spacer(Modifier.height(24.dp))
                
                Text(
                    text = buildAnnotatedString {
                        append("Login means you agree to the ")
                        withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Medium)) { append("Terms of Service") }
                        append(" & ")
                        withStyle(SpanStyle(color = Color(0xFF93C5FD), fontWeight = FontWeight.Medium)) { append("Privacy Policy") }
                    },
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showPrivacyPolicy = true },
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(22.dp))
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                page = if (page == "signup") "login" else "welcome"
                                email = ""
                                password = ""
                                error = ""
                            }
                    )
                }
                Spacer(Modifier.height(18.dp))
                AsyncImage(model = HURRY_LOGO, contentDescription = "Zyora", modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)))
                Spacer(Modifier.height(12.dp))
                Text(if (page == "signup") "Create Account" else "Welcome Back!", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(if (page == "signup") "Sign up to get started" else "Sign in to your account", color = Color.White.copy(alpha = .75f), fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))
                if (error.isNotBlank()) {
                    Text(error, color = Color.White, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x66EF4444)).padding(12.dp))
                    Spacer(Modifier.height(12.dp))
                }
                AuthField("E-mail Address", email, { email = it }, "Enter your email", false, KeyboardType.Email)
                Spacer(Modifier.height(16.dp))
                AuthField("Password", password, { password = it }, if (page == "signup") "Create a password (min 6 characters)" else "Enter your password", !showPassword, KeyboardType.Password,
                    trailing = if (showPassword) "Hide" else "Show",
                    onTrailing = { showPassword = !showPassword })
                Spacer(Modifier.height(22.dp))
                Button(
                    onClick = {
                        scope.launch {
                            loading = true
                            error = ""
                            try {
                                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) throw IllegalArgumentException("Please enter a valid email.")
                                if (password.length < 6) throw IllegalArgumentException("Password must be at least 6 characters long.")
                                val user = withContext(Dispatchers.IO) { backendEmailAuth(context, email.trim(), password, page == "signup") }
                                withContext(Dispatchers.IO) { syncHurryUser(user, context) }
                                val authPrefs = context.getSharedPreferences("hurry_auth", android.content.Context.MODE_PRIVATE)
                                authPrefs.edit()
                                    .putString("uid", user.optString("localId"))
                                    .putString("email", user.optString("email", email.trim()))
                                    .putString("idToken", user.optString("idToken"))
                                    .apply()
                                if (page == "signup") {
                                    authPrefs.edit()
                                        .putBoolean("needsProfileSetup", true)
                                        .putBoolean("profileSetupComplete", false)
                                        .apply()
                                }
                                onLoginSuccess()
                            } catch (t: Throwable) {
                                error = authErrorText(t.message ?: "Authentication failed. Please try again.")
                            } finally {
                                loading = false
                            }
                        }
                    },
                    enabled = !loading && email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF2563EB), disabledContainerColor = Color.White.copy(alpha = .4f))
                ) {
                    if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color(0xFF2563EB), strokeWidth = 2.dp)
                    else Text(if (page == "signup") "Sign Up" else "Login", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (page == "signup") "Already have an account? " else "Don't have an account? ", color = Color.White.copy(alpha = .8f), fontSize = 14.sp)
                    Text(if (page == "signup") "Login" else "Sign Up", color = Color(0xFF93C5FD), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                        page = if (page == "signup") "login" else "signup"
                        password = ""; error = ""
                    })
                }
                Spacer(Modifier.height(28.dp))
            }
        }

        if (googleLoading) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Column(
                    Modifier.size(82.dp).clip(RoundedCornerShape(0.12.dp)).background(Color.Black.copy(alpha = 0.20f)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AsyncImage(model = GOOGLE_LOADING_IMAGE, contentDescription = null, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(0.1.dp))
                    CircularProgressIndicator(Modifier.size(15.dp), color = Color.White, strokeWidth = 1.5.dp)
                }
            }
        }

        if (showDeviceRegisteredDialog) {
            Dialog(onDismissRequest = { showDeviceRegisteredDialog = false }) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(0.12.dp)).background(Color(0xFF171717)).padding(18.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text("Tips", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(14.dp))
                    Text("Your Device has a Registered account (ID: " + registeredDeviceAccountId.ifBlank { "USER ID" } + ")",
                        color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Register Method: Google (" + registeredDeviceEmail.ifBlank { "Google Email" } + ")",
                        color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = { showDeviceRegisteredDialog = false },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(0.12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB), contentColor = Color.White)
                    ) { Text("OK", fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

// New component for the Official Login Screen input fields
@Composable
private fun OfficialAuthField(
    icon: ImageVector,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    hideText: Boolean,
    keyboardType: KeyboardType
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.Black, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, color = Color(0xFF9CA3AF), fontSize = 15.sp)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black, fontSize = 15.sp),
                visualTransformation = if (hideText) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
            )
        }
    }
}

@Composable
private fun AuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    hideText: Boolean,
    keyboardType: KeyboardType,
    trailing: String? = null,
    onTrailing: (() -> Unit)? = null
) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, color = Color.White.copy(alpha = .92f), fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = .12f)).padding(horizontal = 14.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) Text(placeholder, color = Color.White.copy(alpha = .5f), fontSize = 14.sp)
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp),
                    visualTransformation = if (hideText) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
                )
            }
            if (trailing != null && onTrailing != null) {
                Text(trailing, color = Color.White.copy(alpha = .75f), fontSize = 12.sp, modifier = Modifier.clickable { onTrailing() }.padding(start = 8.dp))
            }
        }
    }
}

private fun isDifferentRegisteredGoogleAccount(user: JSONObject, context: android.content.Context): Boolean {
    val prefs = context.getSharedPreferences("hurry_auth", android.content.Context.MODE_PRIVATE)
    val registeredUid = prefs.getString("registeredGoogleUid", "").orEmpty()
    val incomingUid = user.optString("localId").trim()
    return registeredUid.isNotBlank() && incomingUid.isNotBlank() && registeredUid != incomingUid
}

private fun rememberRegisteredGoogleAccount(user: JSONObject, context: android.content.Context) {
    val prefs = context.getSharedPreferences("hurry_auth", android.content.Context.MODE_PRIVATE)
    prefs.edit()
        .putString("registeredGoogleUid", user.optString("localId"))
        .putString("registeredGoogleEmail", user.optString("email"))
        .putString("registeredGoogleAccountId", prefs.getString("accountId", "").orEmpty())
        .apply()
}

private suspend fun googleBackendSignIn(context: android.content.Context): JSONObject {
    val credentialManager = CredentialManager.create(context)
    val serverClientId = context.getString(R.string.default_web_client_id)
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId).build()
    val signInRequest = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()
    val credential = try {
        credentialManager.getCredential(context, signInRequest).credential
    } catch (firstError: Exception) {
        if (firstError is kotlinx.coroutines.CancellationException) throw firstError
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(false)
            .build()
        val fallbackRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        try {
            credentialManager.getCredential(context, fallbackRequest).credential
        } catch (fallbackError: Exception) {
            fallbackError.addSuppressed(firstError)
            throw fallbackError
        }
    }

    if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        throw IllegalStateException("Google did not return a valid sign-in credential.")
    }
    val googleCredential = try {
        GoogleIdTokenCredential.createFrom(credential.data)
    } catch (_: GoogleIdTokenParsingException) {
        throw IllegalStateException("Could not read the Google sign-in response.")
    }

    return withContext(Dispatchers.IO) {
        val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
        val result = Tasks.await(
            FirebaseAuth.getInstance().signInWithCredential(firebaseCredential),
            60,
            java.util.concurrent.TimeUnit.SECONDS
        )
        val firebaseUser = result.user ?: throw IllegalStateException("Firebase did not return a Google account.")
        val firebaseToken = Tasks.await(
            firebaseUser.getIdToken(true),
            60,
            java.util.concurrent.TimeUnit.SECONDS
        ).token ?: throw IllegalStateException("Could not obtain Firebase sign-in token.")
        postAuthJson("/api/auth/firebase", JSONObject().put("idToken", firebaseToken))
    }
}
private fun legacyGoogleBackendSignIn(context: android.content.Context, data: android.content.Intent): JSONObject {
    val account = Tasks.await(GoogleSignIn.getSignedInAccountFromIntent(data))
    val idToken = account.idToken
        ?: throw IllegalStateException("Google did not return an ID token. Check the Firebase Web OAuth client ID.")
    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
    val result = Tasks.await(
        FirebaseAuth.getInstance().signInWithCredential(firebaseCredential),
        60,
        java.util.concurrent.TimeUnit.SECONDS
    )
    val firebaseUser = result.user ?: throw IllegalStateException("Firebase did not return a Google account.")
    val firebaseToken = Tasks.await(
        firebaseUser.getIdToken(true),
        60,
        java.util.concurrent.TimeUnit.SECONDS
    ).token ?: throw IllegalStateException("Could not obtain Firebase sign-in token.")
    return postAuthJson("/api/auth/firebase", JSONObject().put("idToken", firebaseToken))
}

private fun backendEmailAuth(context: android.content.Context, email: String, password: String, create: Boolean): JSONObject {
    val auth = FirebaseAuth.getInstance()
    val result = if (create) {
        Tasks.await(auth.createUserWithEmailAndPassword(email, password), 60, java.util.concurrent.TimeUnit.SECONDS)
    } else {
        Tasks.await(auth.signInWithEmailAndPassword(email, password), 60, java.util.concurrent.TimeUnit.SECONDS)
    }
    val firebaseUser = result.user ?: throw IllegalStateException("Firebase did not return an account.")
    val token = Tasks.await(firebaseUser.getIdToken(true), 60, java.util.concurrent.TimeUnit.SECONDS).token
        ?: throw IllegalStateException("Could not obtain Firebase sign-in token.")
    return postAuthJson("/api/auth/firebase", JSONObject().put("idToken", token))
}
private fun postAuthJson(path: String, payload: JSONObject): JSONObject {
    val connection = (URL("$HURRY_API_BASE$path").openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"; connectTimeout = 15000; readTimeout = 20000; doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
    }
    connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
    val code = connection.responseCode
    val stream = if (code in 200..299) connection.inputStream else connection.errorStream
    val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
    connection.disconnect()
    val json = runCatching { JSONObject(response) }.getOrElse { JSONObject() }
    if (code !in 200..299) throw IllegalStateException(json.optString("error").ifBlank { "Authentication failed. Please try again." })
    return json
}

private fun lookupAccountEmail(accountId: String): String {
    val normalizedId = accountId.trim()
    if (!normalizedId.matches(Regex("\\d{4,12}"))) {
        throw IllegalArgumentException("Enter a valid Zyora Account ID.")
    }
    // Resolve the live profile by its public Zyora ID. Do not depend on stale
    // hard-coded Firebase UIDs from a previous Firebase project.
    val url = URL("$HURRY_API_BASE/api/users?accountId=${java.net.URLEncoder.encode(normalizedId, "UTF-8")}")
    val connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"; connectTimeout = 12000; readTimeout = 15000
        setRequestProperty("Accept", "application/json")
    }
    try {
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        val root = runCatching { JSONObject(response) }.getOrElse { JSONObject() }
        if (code !in 200..299) throw IllegalArgumentException("Zyora Account ID not found.")
        val profile = root.optJSONObject("user")
            ?: root.optJSONArray("users")?.optJSONObject(0)
            ?: throw IllegalArgumentException("Zyora Account ID not found.")
        val returnedId = sequenceOf("accountId", "accountNumber", "displayUserNumber", "Account Number")
            .map { profile.optString(it, "").trim() }
            .firstOrNull { it.isNotBlank() && it != "null" }
            .orEmpty()
        if (returnedId != normalizedId) throw IllegalArgumentException("Zyora Account ID verification failed.")
        return sequenceOf("email", "gmail", "emailPhone")
            .map { profile.optString(it, "").trim() }
            .firstOrNull { it.contains("@") }
            ?: throw IllegalArgumentException("This Zyora account has no linked email. Contact Zyora Support.")
    } finally {
        connection.disconnect()
    }
}

private fun syncHurryUser(user: JSONObject, context: android.content.Context) {
    val uid = user.optString("localId")
    val email = user.optString("email")
    if (uid.isBlank()) throw IllegalStateException("Sign-in service did not return a user ID.")

    val specialAccounts = mapOf(
        "Ed89IA0lyzUXlIplzH660OUdBmT2" to "100002",
        "dCt4l5GbjnMpOeBywiDSH6XfCoj1" to "100003",
        "a0yVqX9dEiS2ZZH6eQmpjs3QrUf1" to "500001",
        "HUSxSvQnabgU029dWYt1TUV04hd2" to "100002",
        "ADqW31RGBMaosOzy0HiqexKSD7h1" to "100003",
        "100002" to "100002",
        "100003" to "100003"
    )
    val officialNames = mapOf(
        "Ed89IA0lyzUXlIplzH660OUdBmT2" to "Zyora Official_IN",
        "dCt4l5GbjnMpOeBywiDSH6XfCoj1" to "Zyora.IN",
        "a0yVqX9dEiS2ZZH6eQmpjs3QrUf1" to "500001"
    )
    val officialIds = setOf("500001", "500002", "500003", "500004", "500005")
    val adminIds = setOf("700001", "700002", "700003")
    val accountHash = kotlin.math.abs(uid.hashCode().toLong())
    val generatedAccountId = when {
        officialIds.contains(uid) || adminIds.contains(uid) -> uid
        specialAccounts.containsKey(uid) -> specialAccounts.getValue(uid)
        else -> (10000000L + accountHash % 90000000L).toString()
    }

    val existingProfile = runCatching {
        val get = (URL("$HURRY_API_BASE/api/users?uid=${java.net.URLEncoder.encode(uid, "UTF-8")}").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 10000; readTimeout = 10000
            setRequestProperty("Accept", "application/json")
        }
        val code = get.responseCode
        val stream = if (code in 200..299) get.inputStream else get.errorStream
        val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        get.disconnect()
        if (code !in 200..299 || response.isBlank()) JSONObject()
        else {
            val root = JSONObject(response)
            root.optJSONObject("user")
                ?: root.optJSONObject("data")
                ?: root.optJSONArray("users")?.optJSONObject(0)
                ?: root.optJSONArray("results")?.optJSONObject(0)
                ?: if (root.has("name") || root.has("displayName") || root.has("image") || root.has("photo")) root else JSONObject()
        }
    }.getOrDefault(JSONObject())

    val existingAccount = existingProfile.optString("accountId").ifBlank {
        existingProfile.optString("accountNumber").ifBlank { existingProfile.optString("Account Number") }
    }
    // Server mapping for this Firebase UID is the stable account-ID source.
    val serverAccountId = user.optString("accountId").trim()
    val accountId = specialAccounts[uid] ?: serverAccountId.ifBlank { existingAccount.ifBlank { generatedAccountId } }
    val emailName = email.substringBefore("@").ifBlank { "User" }
    val name = officialNames[uid] ?: existingProfile.optString("name").ifBlank {
        existingProfile.optString("Name").ifBlank { user.optString("displayName").ifBlank { emailName } }
    }
    val image = if (officialNames.containsKey(uid)) HURRY_LOGO else existingProfile.optString("image").ifBlank {
        existingProfile.optString("avatar").ifBlank {
            existingProfile.optString("photo").ifBlank { user.optString("photoUrl").ifBlank { "/default-avatar.png" } }
        }
    }
    val country = existingProfile.optString("country").ifBlank {
        existingProfile.optString("Country").ifBlank { "🇮🇳" }
    }
    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()

    val isBanned = runCatching {
        val banConnection = (URL("$HURRY_API_BASE/api/check-ban").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 8000; readTimeout = 8000; doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }
        try {
            banConnection.outputStream.use {
                it.write(JSONObject().put("accountId", accountId).put("deviceId", deviceId).toString().toByteArray(Charsets.UTF_8))
            }
            val banCode = banConnection.responseCode
            val banStream = if (banCode in 200..299) banConnection.inputStream else banConnection.errorStream
            val banJson = runCatching {
                JSONObject(banStream?.bufferedReader()?.use { it.readText() }.orEmpty())
            }.getOrDefault(JSONObject())
            banJson.optBoolean("banned")
        } finally {
            banConnection.disconnect()
        }
    }.onFailure {
        android.util.Log.w("ZyoraAuth", "Ban check unavailable; allowing Firebase login and continuing profile sync", it)
    }.getOrDefault(false)
    if (isBanned) throw IllegalStateException("This account or device is blocked from signing in.")

    val profile = JSONObject()
        .put("id", uid).put("uid", uid).put("appLongId", uid)
        .put("name", name).put("email", email.ifBlank { existingProfile.optString("email") })
        .put("gmail", email.ifBlank { existingProfile.optString("gmail") })
        .put("image", image).put("avatar", image)
        .put("accountId", accountId).put("accountNumber", accountId)
        .put("country", country).put("deviceId", deviceId)
    val keys = existingProfile.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        if (!profile.has(key)) profile.put(key, existingProfile.opt(key))
    }
    val save = (URL("$HURRY_API_BASE/api/users").openConnection() as HttpURLConnection).apply {
        requestMethod = "PUT"; connectTimeout = 12000; readTimeout = 12000; doOutput = true
        setRequestProperty("Content-Type", "application/json")
    }
    
    val authPrefs = context.getSharedPreferences("hurry_auth", android.content.Context.MODE_PRIVATE)
    val existingName = sequenceOf("name", "Name", "displayName", "userName")
        .map { existingProfile.optString(it, "").trim() }
        .firstOrNull { it.isNotBlank() && it != "null" && !it.equals("User", ignoreCase = true) }
        .orEmpty()
    val existingBirthday = sequenceOf("birthday", "birthDate", "dateOfBirth")
        .map { existingProfile.optString(it, "").trim() }
        .firstOrNull { it.isNotBlank() && it != "null" }
        .orEmpty()
    val existingGender = existingProfile.optString("gender", "").trim()
    val needsSetup = existingName.isBlank() || existingBirthday.isBlank() ||
        existingGender.isBlank() || existingGender == "null"
    authPrefs.edit()
        .putString("uid", uid)
        .putString("email", email)
        .putString("name", name)
        .putString("accountId", accountId)
        .putString("accountNumber", accountId)
        .putString("photo", image)
        .putString("image", image)
        .putString("country", country)
        .putString("deviceId", deviceId)
        .putBoolean("needsProfileSetup", needsSetup)
        .putBoolean("profileSetupComplete", if (needsSetup) false else authPrefs.getBoolean("profileSetupComplete", true))
        .putString("pendingProfileJson", profile.toString())
        .putBoolean("profileSyncPending", true)
        .apply()

    var saveCode = -1
    var saveResponse = ""
    try {
        save.outputStream.use { it.write(profile.toString().toByteArray(Charsets.UTF_8)) }
        saveCode = save.responseCode
        val saveStream = if (saveCode in 200..299) save.inputStream else save.errorStream
        saveResponse = runCatching { saveStream?.bufferedReader()?.use { it.readText().orEmpty() }.orEmpty() }.getOrDefault("")
    } catch (networkError: Throwable) {
        saveCode = -1
        saveResponse = networkError.message ?: "Network unavailable"
    } finally {
        save.disconnect()
    }

    if (saveCode in 200..299) {
        authPrefs.edit()
            .remove("pendingProfileJson")
            .putBoolean("profileSyncPending", false)
            .apply()
    } else {
        val serverReason = runCatching { JSONObject(saveResponse).optString("error") }.getOrDefault("")
        throw IllegalStateException(
            "Could not save your account to Zyora server (HTTP $saveCode)" +
                if (serverReason.isNotBlank()) ": $serverReason" else ". Please try again."
        )
    }
}

private fun authErrorText(raw: String): String = when {
    raw.contains("EMAIL_EXISTS") -> "An account already exists with this email. Please login instead."
    raw.contains("EMAIL_NOT_FOUND") || raw.contains("INVALID_PASSWORD") || raw.contains("INVALID_LOGIN_CREDENTIALS") -> "Invalid email or password. Please try again."
    raw.contains("WEAK_PASSWORD") -> "Password must be at least 6 characters long."
    raw.contains("INVALID_EMAIL") -> "Please enter a valid email."
    raw.contains("TOO_MANY_ATTEMPTS_TRY_LATER") -> "Too many attempts. Please try again later."
    raw.contains("OPERATION_NOT_ALLOWED") -> "Email/password sign-in is not enabled for this Firebase project."
    else -> raw
}

@Composable
private fun PrivacySection(title: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(bottom = 22.dp)) {
        Text(title, color = Color(0xFF111827), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(body, color = Color(0xFF374151), fontSize = 14.sp, lineHeight = 21.sp)
    }
}

@Composable
private fun GoogleLoginIcon(modifier: Modifier = Modifier) {
    val googleIcon = remember {
        ImageVector.Builder(
            name = "Google", defaultWidth = 48.dp, defaultHeight = 48.dp, viewportWidth = 48f, viewportHeight = 48f
        ).path(fill = SolidColor(Color(0xFF4285F4))) {
            moveTo(43.6f, 24.5f)
            curveToRelative(0f, -1.6f, -0.1f, -3.1f, -0.4f, -4.5f)
            horizontalLineTo(24f)
            verticalLineToRelative(8.5f)
            horizontalLineToRelative(11f)
            arcToRelative(9.4f, 9.4f, 0f, false, true, -4.1f, 6.2f)
            verticalLineToRelative(5.5f)
            horizontalLineToRelative(6.7f)
            curveToRelative(3.9f, -3.6f, 6f, -9f, 6f, -15.7f)
            close()
        }.path(fill = SolidColor(Color(0xFF34A853))) {
            moveTo(24f, 44f)
            curveToRelative(5.5f, 0f, 10.1f, -1.8f, 13.5f, -4.8f)
            lineToRelative(-6.7f, -5.5f)
            curveToRelative(-1.8f, 1.2f, -4f, 2f, -6.8f, 2f)
            curveToRelative(-5.2f, 0f, -9.6f, -3.5f, -11.2f, -8.2f)
            horizontalLineTo(5.9f)
            verticalLineToRelative(5.7f)
            arcTo(20f, 20f, 0f, false, false, 24f, 44f)
            close()
        }.path(fill = SolidColor(Color(0xFFFBBC05))) {
            moveTo(12.8f, 27.5f)
            arcToRelative(12f, 12f, 0f, false, true, 0f, -7f)
            verticalLineTo(14.8f)
            horizontalLineTo(5.9f)
            arcToRelative(20f, 20f, 0f, false, false, 0f, 18.4f)
            close()
        }.path(fill = SolidColor(Color(0xFFEA4335))) {
            moveTo(24f, 12.3f)
            curveToRelative(3f, 0f, 5.7f, 1f, 7.8f, 3.1f)
            lineToRelative(5.9f, -5.9f)
            curveTo(34.1f, 6.2f, 29.5f, 4f, 24f, 4f)
            arcTo(20f, 20f, 0f, false, false, 5.9f, 14.8f)
            lineToRelative(6.9f, 5.7f)
            curveToRelative(1.6f, -4.7f, 6f, -8.2f, 11.2f, -8.2f)
            close()
        }.build()
    }
    Icon(imageVector = googleIcon, contentDescription = "Google Logo", modifier = modifier, tint = Color.Unspecified)
}

@Composable
private fun SimpleUserIcon(modifier: Modifier = Modifier) {
    val userIcon = remember {
        ImageVector.Builder(
            name = "User", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(12f, 12f)
            curveToRelative(2.21f, 0f, 4f, -1.79f, 4f, -4f)
            reflectiveCurveToRelative(-1.79f, -4f, -4f, -4f)
            reflectiveCurveToRelative(-4f, 1.79f, -4f, 4f)
            reflectiveCurveToRelative(1.79f, 4f, 4f, 4f)
            close()
            moveTo(12f, 14f)
            curveToRelative(-2.67f, 0f, -8f, 1.34f, -8f, 4f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(16f)
            verticalLineToRelative(-2f)
            curveToRelative(0f, -2.66f, -5.33f, -4f, -8f, -4f)
            close()
        }.build()
    }
    Icon(imageVector = userIcon, contentDescription = "User Icon", modifier = modifier, tint = Color.Unspecified)
}

@Composable
private fun OfficialLoginIcon(modifier: Modifier = Modifier) {
    val accountIcon = remember {
        ImageVector.Builder(
            name = "OfficialAccount", defaultWidth = 48.dp, defaultHeight = 48.dp, viewportWidth = 512f, viewportHeight = 512f
        ).path(fill = SolidColor(Color(0xFF2563EB))) {
            moveTo(224f, 41f)
            arcToRelative(108f, 108f, 0f, true, true, 0f, 216f)
            arcToRelative(108f, 108f, 0f, true, true, 0f, -216f)
            close()
            moveTo(220f, 280f)
            curveTo(157f, 283f, 112f, 319f, 88f, 369f)
            curveTo(75f, 396f, 68f, 425f, 68f, 454f)
            lineTo(224f, 454f)
            lineTo(224f, 352f)
            curveTo(224f, 324f, 223f, 301f, 220f, 280f)
            close()
            moveTo(253f, 361f)
            verticalLineTo(328f)
            curveTo(253f, 291f, 274f, 271f, 303f, 271f)
            curveTo(332f, 271f, 353f, 291f, 353f, 328f)
            verticalLineTo(361f)
            horizontalLineTo(329f)
            verticalLineTo(328f)
            curveTo(329f, 306f, 320f, 295f, 303f, 295f)
            curveTo(286f, 295f, 277f, 306f, 277f, 328f)
            verticalLineTo(361f)
            close()
            moveTo(241f, 357f)
            quadTo(241f, 348f, 251f, 348f)
            horizontalLineTo(355f)
            quadTo(365f, 348f, 365f, 358f)
            verticalLineTo(437f)
            quadTo(365f, 447f, 355f, 447f)
            horizontalLineTo(251f)
            quadTo(241f, 447f, 241f, 437f)
            close()
        }.build()
    }
    Icon(imageVector = accountIcon, contentDescription = "Official Login Logo", modifier = modifier, tint = Color.Unspecified)
}
