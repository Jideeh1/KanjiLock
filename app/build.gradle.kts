import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.jideeh.kanjilock"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.jideeh.kanjilock"
        minSdk = 26
        targetSdk = 37
        versionCode = 10
        versionName = "2.1.0"
    }

    //put ur own key in keystore.properties (never commit it), otherwise release uses the debug key
    val keys = rootProject.file("keystore.properties")
    if (keys.exists()) {
        val p = Properties().apply { keys.inputStream().use { load(it) } }
        signingConfigs.create("release") {
            storeFile = rootProject.file(p.getProperty("storeFile"))
            storePassword = p.getProperty("storePassword")
            keyAlias = p.getProperty("keyAlias")
            keyPassword = p.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            //phones are all arm, the x86 libs are just for emulators and double the size of the handwriting lib
            ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    //github builds can update themselves from releases, play builds cant (play policy)
    flavorDimensions += "store"
    productFlavors {
        create("github") {
            dimension = "store"
            isDefault = true
            buildConfigField("boolean", "SELF_UPDATE", "true")
        }
        create("play") {
            dimension = "store"
            buildConfigField("boolean", "SELF_UPDATE", "false")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.animation:animation")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("com.github.luben:zstd-jni:1.5.7-21@aar")
    implementation("com.google.android.gms:play-services-auth:21.6.0")
    implementation("dev.chrisbanes.haze:haze:2.0.1")
    implementation("dev.chrisbanes.haze:haze-blur:2.0.1")
    implementation("com.google.mlkit:digital-ink-recognition:19.0.0")
    //japanese ocr for the text extractor. the model ships inside the apk so it works with no wifi
    implementation("com.google.mlkit:text-recognition-japanese:16.0.1")
    //camerax just for the scanner preview, nothing else touches it
    val cameraX = "1.6.2"
    implementation("androidx.camera:camera-core:$cameraX")
    implementation("androidx.camera:camera-camera2:$cameraX")
    implementation("androidx.camera:camera-lifecycle:$cameraX")
    implementation("androidx.camera:camera-view:$cameraX")

    testImplementation("junit:junit:4.13.2")
    testImplementation("com.github.luben:zstd-jni:1.5.7-21")
    testImplementation("org.xerial:sqlite-jdbc:3.53.4.0")
    testImplementation("org.json:json:20260814")
}
