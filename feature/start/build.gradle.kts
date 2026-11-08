plugins {
    alias(libs.plugins.formatico.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.format)
        }
    }
}
