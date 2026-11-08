plugins {
    alias(libs.plugins.formatico.kmpLibrary)
    alias(libs.plugins.kotlinSerialization)
}

// What the app asks of the network (Faz 16): Formatico's server, which keeps encrypted shares, and the documents at web
// addresses. Ktor's client alone (the owner's decision, 2026-11-06), each platform on its own engine, and AES-GCM from
// each platform's own cryptography, as core:storage encrypts the texts.
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.share)
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
            implementation(libs.cryptography.core)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.java)
            implementation(libs.cryptography.providerJdk)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.cryptography.providerJdk)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.cryptography.providerCryptokit)
        }
        webMain.dependencies {
            implementation(libs.ktor.client.js)
            implementation(libs.kotlinx.browser)
            implementation(libs.cryptography.providerWebcrypto)
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
