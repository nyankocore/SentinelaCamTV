import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val sentinelaLocalProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.isFile) {
        localPropertiesFile.inputStream().use(::load)
    }
}

fun sentinelaLocalProperty(
    name: String,
    defaultValue: String = "",
): String = sentinelaLocalProperties.getProperty(name, defaultValue)

fun buildConfigString(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val sentinelaDvrRtspPort = sentinelaLocalProperty(
    name = "sentinela.dvr.rtspPort",
    defaultValue = "554",
).toIntOrNull()?.toString() ?: "554"

val releaseSigningPropertiesFile = File(
    System.getProperty("user.home"),
    "Documents/SentinelaCamTV/release-signing/keystore.properties",
)
val releaseSigningProperties = Properties().apply {
    if (releaseSigningPropertiesFile.isFile) {
        releaseSigningPropertiesFile.inputStream().use(::load)
    }
}
val hasReleaseSigning = releaseSigningPropertiesFile.isFile

fun releaseSigningProperty(name: String): String =
    releaseSigningProperties.getProperty(name)
        ?: error("Campo '$name' ausente em ${releaseSigningPropertiesFile.absolutePath}")

android {
    namespace = "com.sentinela.camtv"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sentinela.camtv"
        minSdk = 23
        targetSdk = 35
        versionCode = 8
        versionName = "2.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "SENTINELA_DVR_HOST",
            buildConfigString(""),
        )
        buildConfigField(
            "String",
            "SENTINELA_DVR_USERNAME",
            buildConfigString(""),
        )
        buildConfigField(
            "String",
            "SENTINELA_DVR_PASSWORD",
            buildConfigString(""),
        )
        buildConfigField(
            "int",
            "SENTINELA_DVR_RTSP_PORT",
            "554",
        )
        buildConfigField(
            "boolean",
            "SEED_DEBUG_CAMERAS",
            "false",
        )
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = File(releaseSigningProperty("storeFile"))
                storePassword = releaseSigningProperty("storePassword")
                keyAlias = releaseSigningProperty("keyAlias")
                keyPassword = releaseSigningProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            buildConfigField(
                "String",
                "SENTINELA_DVR_HOST",
                buildConfigString(sentinelaLocalProperty("sentinela.dvr.host")),
            )
            buildConfigField(
                "String",
                "SENTINELA_DVR_USERNAME",
                buildConfigString(sentinelaLocalProperty("sentinela.dvr.username")),
            )
            buildConfigField(
                "String",
                "SENTINELA_DVR_PASSWORD",
                buildConfigString(sentinelaLocalProperty("sentinela.dvr.password")),
            )
            buildConfigField(
                "int",
                "SENTINELA_DVR_RTSP_PORT",
                sentinelaDvrRtspPort,
            )
            buildConfigField(
                "boolean",
                "SEED_DEBUG_CAMERAS",
                "true",
            )
        }
        release {
            buildConfigField(
                "boolean",
                "SEED_DEBUG_CAMERAS",
                "false",
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":core:onvif"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.rtsp)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.timber)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
