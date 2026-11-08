plugins {
    alias(libs.plugins.formatico.kmpCompose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}
