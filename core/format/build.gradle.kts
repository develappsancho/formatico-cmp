plugins {
    alias(libs.plugins.formatico.kmpLibrary)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // YAML is read by snakeyaml-engine-kmp, and written by Formatico's own printer (the owner's decision).
            implementation(libs.snakeyaml.engine.kmp)
        }
    }
}
