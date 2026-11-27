import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

/**
 * Release signing comes from keystore.properties in the project root (git-ignored; copy
 * keystore.properties.template). Without it, assembleRelease still works and produces an
 * unsigned APK. See docs/RELEASE.md.
 */
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.isFile) file.inputStream().use(::load)
}
val hasReleaseKey = listOf("storeFile", "storePassword", "keyAlias", "keyPassword").all {
    val value = keystoreProperties.getProperty(it)?.trim()
    // "[PLACEHOLDER]" values copied straight from the template count as not configured.
    !value.isNullOrEmpty() && !value.startsWith("[")
}
if (rootProject.file("keystore.properties").isFile && !hasReleaseKey) {
    logger.warn("keystore.properties is incomplete or still has [PLACEHOLDER] values; release APK will be unsigned.")
}

android {
    namespace = "com.northeast.launcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.northeast.launcher"
        // Android 9: covers the Onn 4K Pro, Google TV Streamer, Shield (Android 11)
        // and most Android/Google TV televisions from 2019 on. See docs/FEASIBILITY.md §5.
        minSdk = 28
        targetSdk = 35
        versionCode = 2
        versionName = "0.9.0"
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseKey) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = false
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.tv.material)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
