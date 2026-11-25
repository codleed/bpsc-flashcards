import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Release signing for the in-app updater. The keystore is gitignored; without it
// (e.g. a fresh clone) release builds fall back to the debug signing config.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore/keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val hasReleaseKeystore = keystoreProps.getProperty("storeFile") != null &&
    rootProject.file(keystoreProps.getProperty("storeFile")).exists()

// Parse the version out of data/AppVersion.kt so there is exactly one place to
// bump. The publish script reads the same file, so APK and manifest never disagree.
val appVersionSrc = file("src/main/java/com/bpsc/flashcards/data/AppVersion.kt").readText()
val appVersionCode = Regex("""CODE:\s*Int\s*=\s*(\d+)""").find(appVersionSrc)!!.groupValues[1].toInt()
val appVersionName = Regex("""NAME:\s*String\s*=\s*"([^"]+)"""").find(appVersionSrc)!!.groupValues[1]

android {
    namespace = "com.bpsc.flashcards"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bpsc.flashcards"
        minSdk = 26
        targetSdk = 35

        // The single source of truth is data/AppVersion.kt, which the publish
        // script also reads to stamp version.json. Bumping a release is one edit
        // there; these values are parsed from it so they can never drift.
        versionCode = appVersionCode
        versionName = appVersionName

        buildConfigField("int", "APP_VERSION_CODE", versionCode.toString())
        buildConfigField("String", "APP_VERSION_NAME", "\"$versionName\"")
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
}