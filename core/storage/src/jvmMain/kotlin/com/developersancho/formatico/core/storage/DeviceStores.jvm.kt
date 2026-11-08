package com.developersancho.formatico.core.storage

import eu.anifantakis.lib.ksafe.KSafe
import java.io.File

internal actual fun deviceStores(): DeviceStores {
    val base = dataDirectory()
    val ksafe = KSafe(fileName = STORE_NAME, baseDir = File(base, "ksafe"))
    return DeviceStores(small = KSafeSmallStore(ksafe), texts = encryptedTexts(ksafe, FileTextStore(File(base, "texts"))))
}

internal actual suspend fun KSafe.awaitReady() = Unit

/** Where the desktop keeps an app's own data: the system's place for it, which no backup of documents takes. */
private fun dataDirectory(): File {
    val home = System.getProperty("user.home")
    val system = System.getProperty("os.name").lowercase()
    return when {
        "mac" in system -> File(home, "Library/Application Support/Formatico")
        "win" in system -> File(System.getenv("APPDATA") ?: home, "Formatico")
        else -> File(System.getenv("XDG_DATA_HOME") ?: "$home/.local/share", "formatico")
    }
}
