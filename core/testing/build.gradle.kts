plugins {
    alias(libs.plugins.formatico.kmpCompose)
}

// What the tests of every module that draws share: waiting for a screen while the platform runs, and a preview's
// screenshot. Only tests depend on it.
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.designsystem)
            api(libs.pantrix.testing)
            implementation(libs.kotlinx.coroutines.core)
        }
        jvmMain.dependencies {
            api(libs.roborazzi.composeDesktop)
        }
    }
}
