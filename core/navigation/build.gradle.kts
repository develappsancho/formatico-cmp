plugins {
    alias(libs.plugins.formatico.kmpCompose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            // The system's back, which leaves a screen where the platform has one: navigation3's own version.
            implementation(libs.navigationevent.compose)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}
