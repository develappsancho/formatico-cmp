plugins {
    alias(libs.plugins.formatico.webApplication)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.app.shared)
            implementation(projects.core.designsystem)

            implementation(libs.compose.ui)
            implementation(libs.pantrix.components)
        }
    }
}
