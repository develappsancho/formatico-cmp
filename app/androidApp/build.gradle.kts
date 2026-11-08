import java.io.StringReader
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.formatico.quality)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}
dependencies {
    implementation(projects.app.shared)
    // The stores find the app's folders by its context, given before the first screen.
    implementation(projects.core.storage)

    implementation(libs.androidx.activity.compose)
}

/**
 * The release's signing key, as local.properties gives it on the machine that signs: storeFile (a path from the
 * repository's root), storePassword, keyAlias and keyPassword. That file never leaves the machine, and the key's file
 * stays out of the repository (signing/ is ignored). A machine without them builds the release unsigned.
 */
val signing = Properties().apply {
    val local = providers.fileContents(layout.settingsDirectory.file("local.properties")).asText.orNull
    if (local != null) load(StringReader(local))
}

/** The version as Android counts it: 1.2.3 is 10203, so that each release counts above the last. */
fun versionCodeOf(version: String): Int {
    val (major, minor, patch) = version.split('.').map(String::toInt)
    return major * 10_000 + minor * 100 + patch
}

android {
    namespace = "com.developersancho.formatico"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.developersancho.formatico"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionName = providers.gradleProperty("formatico.version").get()
        versionCode = versionCodeOf(versionName!!)
    }
    signingConfigs {
        signing.getProperty("storeFile")?.let { path ->
            create("release") {
                storeFile = rootProject.file(path)
                storePassword = signing.getProperty("storePassword")
                keyAlias = signing.getProperty("keyAlias")
                keyPassword = signing.getProperty("keyPassword")
            }
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
    }
    // AGP's Compose Preview task, which Android Studio creates as it syncs, draws a preview with the module's compiled
    // resources and refuses a module whose unit tests leave them out, as they do by default.
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}
