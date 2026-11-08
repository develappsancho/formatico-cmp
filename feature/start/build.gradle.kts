plugins {
    alias(libs.plugins.formatico.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // What it opens is in a format of core:format's, which the app hands on.
            api(projects.core.format)
        }
    }
}
