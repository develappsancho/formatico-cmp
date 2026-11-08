plugins {
    alias(libs.plugins.formatico.kmpLibrary)
}

// A document validated against a JSON Schema (Faz 19): by optimumcode's json-schema-validator, drafts 4 to 2020-12, on
// every target (the owner's decision, 2026-11-09). Its errors are turned into Formatico's own problems, in its words.
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.format)
            implementation(libs.json.schema.validator)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
