plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}
android {
    namespace = "com.hurry.voicechat"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.hawa.app"
        // Support Android 6.0 (Marshmallow) and newer devices.
        minSdk = 23
        targetSdk = 35
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionName = System.getenv("HURRY_VERSION_NAME") ?: "1.0"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    val supabaseAnonKey = providers.gradleProperty("SUPABASE_ANON_KEY")
        .orElse(providers.environmentVariable("SUPABASE_ANON_KEY"))
        .orElse(providers.environmentVariable("NEXT_PUBLIC_SUPABASE_ANON_KEY"))
        .orElse("")
        .get()
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
    defaultConfig {
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
        // Use the Firebase project's registered Web OAuth client when CI secret is not configured.
        resValue("string", "default_web_client_id", providers.environmentVariable("GOOGLE_WEB_CLIENT_ID").orElse("164837688319-uu6uhd20mner48etd4ci6aeu3nuvt8gp.apps.googleusercontent.com").get())
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin {
    jvmToolchain(17)
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.09.00"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.media3:media3-exoplayer:1.8.0")
    implementation("androidx.media3:media3-ui:1.8.0")
    implementation("androidx.navigation:navigation-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("io.coil-kt.coil3:coil:3.3.0")
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    // Coil does not animate GIFs unless its GIF decoder is explicitly installed.
    implementation("io.coil-kt.coil3:coil-gif:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")
    implementation("io.socket:socket.io-client:2.1.1")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
}
