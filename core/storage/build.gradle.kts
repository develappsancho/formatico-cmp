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
            // The documents' texts are encrypted with a key KSafe keeps (the owner's decision, 2026-11-04): AES-GCM
            // from each platform's own cryptography.
            implementation(libs.cryptography.core)
        }
        jvmMain.dependencies {
            implementation(libs.cryptography.providerJdk)
        }
        androidMain.dependencies {
            implementation(libs.cryptography.providerJdk)
        }
        iosMain.dependencies {
            implementation(libs.cryptography.providerCryptokit)
        }
        webMain.dependencies {
            implementation(libs.cryptography.providerWebcrypto)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
