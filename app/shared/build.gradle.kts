plugins {
    alias(libs.plugins.formatico.kmpCompose)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    // The boards' screenshots as Android draws them, in Robolectric (`recordRoborazziAndroidHostTest`). The host tests
    // take the common ones in too, which are run on the JVM, iOS and the web: here only Android's own run.
    android {
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.base)
            implementation(projects.core.designsystem)
            implementation(projects.core.navigation)
            implementation(projects.core.settings)
            implementation(projects.core.storage)
            implementation(projects.feature.settings)
            implementation(projects.feature.start)
            implementation(projects.feature.workspace)

            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.navigation3.ui)
            implementation(libs.androidx.lifecycle.viewmodelNavigation3)
            implementation(libs.kotlinx.serialization.core)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
            // The links a test pastes are made as Formatico makes them.
            implementation(projects.core.share)
        }
        // The boards' screenshots in the iOS simulator, as iOS draws them (`recordRoborazziIosSimulatorArm64`).
        iosSimulatorArm64Test.dependencies {
            implementation(libs.roborazzi.composeIos)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.kotlin.testJunit)
            implementation(libs.robolectric)
            // Compose's Android test brings Espresso 3.5, which looks for an input manager Android 17 has no more.
            implementation(libs.androidx.test.espressoCore)
            implementation(libs.roborazzi.android)
            implementation(libs.roborazzi.compose)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

tasks.withType<Test>().matching { it.name == "testAndroidHostTest" }.configureEach {
    filter { includeTestsMatching("com.developersancho.formatico.BoardScreenshotAndroidTest") }
    // Robolectric's Android 17 sets a file descriptor's fields through the JDK's own access, which JDK 21 keeps closed.
    jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED", "--add-opens=java.base/java.io=ALL-UNNAMED")
}
