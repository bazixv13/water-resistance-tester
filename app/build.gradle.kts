import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        FileInputStream(keystorePropertiesFile).use { load(it) }
    }
}

android {
    namespace = "com.raywdude.waterresistance"
    compileSdk = 37

    defaultConfig {
        // Placeholder publishing identity: replace with a package ID based on a domain you own
        // before the first Play upload. The internal namespace stays unchanged.
        applicationId = "com.waterresistance.tester"
        minSdk = 26
        targetSdk = 36
        // Bump both values for every distributed change.
        versionCode = 10
        versionName = "2.8-expressive-polish"
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Sign the debug build with the release key so it can be installed over the release
            // APK during local iteration without wiping the device's test history.
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeVersion = "1.12.0"

    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")

    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.ui:ui-graphics:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    implementation("androidx.compose.foundation:foundation:$composeVersion")
    implementation("androidx.compose.animation:animation:$composeVersion")

    // Material 3 Expressive: MaterialExpressiveTheme, ButtonGroup, ToggleButton,
    // LoadingIndicator, LargeFlexibleTopAppBar, FloatingToolbar, MaterialShapes.
    implementation("androidx.compose.material3:material3:1.5.0-beta02")

    // Navigation Compose. This is what provides the DEFAULT predictive back behaviour: a NavHost
    // registers its own back callback at the right priority, so the platform runs its own
    // predictive animation (previous destination visible behind a shrinking window) instead of the
    // app having to draw a preview by hand. A hand-rolled PredictiveBackHandler cannot reproduce
    // that, because any PRIORITY_DEFAULT callback suppresses the system animation.
    implementation("androidx.navigation:navigation-compose:2.10.2")

    // Progressive backdrop blur for the top bar as content scrolls underneath.
    implementation("dev.chrisbanes.haze:haze:1.7.3")
    implementation("dev.chrisbanes.haze:haze-materials:1.7.3")

    debugImplementation("androidx.compose.ui:ui-tooling:$composeVersion")
}