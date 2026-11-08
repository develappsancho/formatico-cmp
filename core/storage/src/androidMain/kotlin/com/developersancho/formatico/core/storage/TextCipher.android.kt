package com.developersancho.formatico.core.storage

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.providers.jdk.JDK

internal actual val TextCipherProvider: CryptographyProvider get() = CryptographyProvider.JDK
