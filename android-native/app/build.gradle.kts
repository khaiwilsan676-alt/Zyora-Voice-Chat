plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
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
    // Force debug APKs to use the same cached certificate reported to Firebase.
    signingConfigs {
        getByName("debug") {
            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// The native onboarding screen uses the original gender illustrations stored in
// the web app's public directory. Copy them into generated Android resources
// with Android-safe lowercase resource names before resource processing.
val generatedGenderResources = layout.buildDirectory.dir("generated/res/genderAssets")
val copyGenderAssets = tasks.register<Copy>("copyGenderAssets") {
    from(rootProject.layout.projectDirectory.dir("../public")) {
        include(
            "IMG_20261031_121547.png", "IMG_20261031_121627.png",
            "IMG_20260804_211013.jpg", "IMG_20260804_211031.jpg",
            "IMG_20260822_143032.png", "1787389350745~2.jpg", "IMG_20260822_143348.png",
            "IMG_20260822_143408.png", "IMG_20260822_143422~2.jpg",
            "IMG_20260822_011134.png", "IMG_20260822_011118.png", "IMG_20260822_011103.png",
            "IMG_20260822_011041.png", "IMG_20260822_011151.png", "IMG_20260822_011205.png",
            "IMG_20260822_011218.png", "IMG_20260822_011028.png", "1787337855180~2.jpg",
            "IMG_20260822_011000.png", "file_00000000993c8208ae462545f9ccb0ba.png",
            "file_00000000330c8211b80be136b631b3e0.png", "file_000000009d808211b8ffb7c2183b4ef5.png",
            "1787344138649~2.jpg", "file_00000000e56882119c217d508b6733dc.png",
            "IMG_20260913_000423.png", "IMG_20260912_235156.png", "IMG_20260912_235215.png",
            "IMG_20260912_235230.png"
        )
        rename { fileName -> if (fileName.startsWith("IMG_20261031") || fileName.startsWith("IMG_20260804")) fileName.lowercase() else "wp_" + fileName.lowercase().replace("~", "_").replace("-", "_").replace(" ", "_") }
    }
    into(generatedGenderResources.map { it.dir("drawable") })
}

android.sourceSets.getByName("main").res.srcDir(generatedGenderResources)
tasks.named("preBuild").configure {
    dependsOn(copyGenderAssets)
}

kotlin {
    jvmToolchain(17)
}
dependencies {
    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.gms:play-services-tasks:18.3.2")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.android.gms:play-services-auth:21.3.0")
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
    implementation("androidx.media3:media3-datasource:1.8.0")
    implementation("androidx.media3:media3-database:1.8.0")
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
