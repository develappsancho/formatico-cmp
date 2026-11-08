plugins {
    alias(libs.plugins.formatico.kmpCompose)
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
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
