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
        versionCode = 7
        versionName = "1.1.0"
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

    testImplementation("junit:junit:4.13.2")
    testImplementation("com.github.luben:zstd-jni:1.5.7-21")
    testImplementation("org.xerial:sqlite-jdbc:3.53.4.0")
    testImplementation("org.json:json:20260814")
}
