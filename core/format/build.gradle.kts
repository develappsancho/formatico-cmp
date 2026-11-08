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

// The compilers the types Formatico writes are tried with (TypesCompileTest), where they are given:
// -Pformatico.go=…/bin/go, -Pformatico.tsc=…/bin/tsc, -Pformatico.swiftc=/usr/bin/swiftc. One not given is skipped.
tasks.withType<Test>().matching { it.name == "jvmTest" }.configureEach {
    listOf("go", "tsc", "swiftc").forEach { tool ->
        providers.gradleProperty("formatico.$tool").orNull?.let {
            systemProperty("formatico.$tool", it)
            inputs.property("formatico.$tool", it)
        }
    }
}
