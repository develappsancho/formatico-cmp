plugins {
    alias(libs.plugins.formatico.kmpLibrary)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
            // The small values (the owner's decision, 2026-11-04): KSafe, on every target.
            implementation(libs.ksafe)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
