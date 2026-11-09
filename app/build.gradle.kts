import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// ---------------------------------------------------------------------------
// Automatic versioning
//
// versionCode is derived from the git commit count, so it is always monotonic
// no matter which branch or machine produces the build. The 1000 offset keeps
// it above the hand-maintained codes used before CI took over.
//
// versionName is "<base>.<commitCount>", where <base> comes from the
// app.versionBase property in gradle.properties. CI may override both through
// APP_VERSION_NAME / APP_VERSION_CODE (nightly builds append a date suffix).
// ---------------------------------------------------------------------------
fun gitOutput(vararg args: String): String =
    providers.exec {
        commandLine("git", *args)
        isIgnoreExitValue = true
    }.standardOutput.asText.map { it.trim() }.getOrElse("")

val gitCommitCount = gitOutput("rev-list", "--count", "HEAD").toIntOrNull() ?: 1
val gitShortSha = gitOutput("rev-parse", "--short", "HEAD").ifBlank { "unknown" }
val versionBase = providers.gradleProperty("app.versionBase").orElse("2.8").get()

val autoVersionCode = 1000 + gitCommitCount
val autoVersionName = "$versionBase.$gitCommitCount"

val envVersionName = providers.environmentVariable("APP_VERSION_NAME").orNull
val envVersionCode = providers.environmentVariable("APP_VERSION_CODE").orNull?.toIntOrNull()
val buildChannel = providers.environmentVariable("APP_BUILD_CHANNEL")
    .orElse("stable")
    .get()
    .replace("\"", "")

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        FileInputStream(keystorePropertiesFile).use { load(it) }
    }
}

// Release signing material comes from keystore.properties locally, or from
// environment variables in CI (GitHub Actions secrets). Never commit either.
val keystorePath = providers.environmentVariable("KEYSTORE_FILE").orNull
    ?: keystoreProperties.getProperty("storeFile")
val keystoreStorePassword = providers.environmentVariable("KEYSTORE_PASSWORD").orNull
    ?: keystoreProperties.getProperty("storePassword")
val keystoreAlias = providers.environmentVariable("KEY_ALIAS").orNull
    ?: keystoreProperties.getProperty("keyAlias")
val keystoreKeyPassword = providers.environmentVariable("KEY_PASSWORD").orNull
    ?: keystoreProperties.getProperty("keyPassword")

val signingReady = keystorePath != null &&
    rootProject.file(keystorePath).exists() &&
    !keystoreStorePassword.isNullOrBlank() &&
    !keystoreAlias.isNullOrBlank() &&
    !keystoreKeyPassword.isNullOrBlank()

android {
    namespace = "com.raywdude.waterresistance"
    compileSdk = 37

    defaultConfig {
        // Placeholder publishing identity: replace with a package ID based on a domain you own
        // before the first Play upload. The internal namespace stays unchanged.
        applicationId = "com.waterresistance.tester"
        minSdk = 26
        targetSdk = 36
        // Bump only the base in gradle.properties; the build number comes from git.
        versionCode = envVersionCode ?: autoVersionCode
        versionName = envVersionName ?: autoVersionName

        buildConfigField("String", "BUILD_CHANNEL", "\"$buildChannel\"")
        buildConfigField("String", "GIT_COMMIT", "\"$gitShortSha\"")
    }

    signingConfigs {
        if (signingReady) {
            create("release") {
                storeFile = rootProject.file(keystorePath!!)
                storePassword = keystoreStorePassword
                keyAlias = keystoreAlias
                keyPassword = keystoreKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            // Sign the debug build with the release key so it can be installed over the release
            // APK during local iteration without wiping the device's test history.
            if (signingReady) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (signingReady) {
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