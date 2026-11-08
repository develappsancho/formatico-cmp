plugins {
    alias(libs.plugins.formatico.kmpCompose)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.roborazzi)
}

kotlin {
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
        // The boards' screens drawn on the desktop, at their widths: recordRoborazziJvm writes them under
        // screenshots/, verifyRoborazziJvm compares against them.
        jvmTest.dependencies {
            implementation(libs.roborazzi.composeDesktop)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
