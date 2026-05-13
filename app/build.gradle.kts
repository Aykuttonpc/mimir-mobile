plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.services)   // ADR-017: FCM
}

android {
    namespace = "com.aykutcincik.mimir"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.aykutcincik.mimir"
        minSdk = 24
        targetSdk = 34
        versionCode = 16
        versionName = "0.9.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Sabit debug keystore — her build aynı imzayı uretir, APK seamless update
    // (Android signature mismatch ile "uygulama yuklenemedi" hatasi olmaz).
    // Debug only — release Play Store'a giderse ayri keystore kullanilir.
    signingConfigs {
        create("mimirDebug") {
            storeFile = file("mimir-debug.keystore")
            storePassword = "mimirdebug"
            keyAlias = "mimirdebug"
            keyPassword = "mimirdebug"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("mimirDebug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("mimirDebug")   // gecici, release-signed Sprint #15
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true   // T-039: BuildConfig.VERSION_NAME erişimi için
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets["main"].java.srcDirs("src/main/kotlin")

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":data"))

    // AndroidX core
    implementation(libs.androidx.core.ktx)

    // Compose
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // DataStore — JWT secure storage
    implementation(libs.androidx.datastore.preferences)

    // Splash (Android 12+ API + backport for 6+)
    implementation(libs.androidx.core.splashscreen)

    // Coroutines (Android dispatcher)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.rx3)

    // SignalR Java client (T-037 real-time DM) — Android-only şu an, Sprint #6 KMP refactor
    implementation(libs.microsoft.signalr)

    // FCM (ADR-017) — signal-only push, sadece messaging artifact (Auth/Firestore yok)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging.ktx)

    // WebRTC voice call (Sprint #12)
    implementation(libs.webrtc.android)

    testImplementation(libs.junit)
}
