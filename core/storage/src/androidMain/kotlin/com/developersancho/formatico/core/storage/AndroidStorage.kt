package com.developersancho.formatico.core.storage

import android.content.Context
import eu.anifantakis.lib.ksafe.KSafe
import java.io.File

/** Android's stores are in the app's own folders, which they need the app's context to find. */
object AndroidStorage {
    private var context: Context? = null

    /** Gives the stores the app's [context]: once, before the app's first screen. */
    fun init(context: Context) {
        this.context = context.applicationContext
    }

    internal fun context(): Context =
        checkNotNull(context) { "AndroidStorage.init(context) comes before the app's first screen" }
}

// The folder Android's backup leaves out: what is opened in Formatico stays on this device.
internal actual fun deviceStores(): DeviceStores {
    val context = AndroidStorage.context()
    val base = context.noBackupFilesDir
    val ksafe = KSafe(context = context, fileName = STORE_NAME, baseDir = File(base, "ksafe"))
    return DeviceStores(small = KSafeSmallStore(ksafe), texts = encryptedTexts(ksafe, FileTextStore(File(base, "texts"))))
}

internal actual suspend fun KSafe.awaitReady() = Unit
