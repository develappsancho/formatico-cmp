package com.developersancho.formatico.core.storage

import eu.anifantakis.lib.ksafe.KSafe
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

internal actual fun deviceStores(): DeviceStores {
    val support = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
        .first() as String
    val ksafe = KSafe(fileName = STORE_NAME)
    return DeviceStores(small = KSafeSmallStore(ksafe), texts = encryptedTexts(ksafe, FileTextStore("$support/Formatico/texts")))
}

internal actual suspend fun KSafe.awaitReady() = Unit

/**
 * A [TextStore] of files in [directory], one a text. The folder is left out of the device's backup: what is opened in
 * Formatico stays on this device.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal class FileTextStore(private val directory: String) : TextStore {
    private val files = NSFileManager.defaultManager

    override suspend fun read(name: String): String? = withContext(Dispatchers.IO) {
        requireTextName(name)
        NSString.stringWithContentsOfFile(path(name), NSUTF8StringEncoding, null)
    }

    override suspend fun write(name: String, text: String) = withContext(Dispatchers.IO) {
        requireTextName(name)
        if (!files.fileExistsAtPath(directory)) {
            files.createDirectoryAtPath(directory, withIntermediateDirectories = true, attributes = null, error = null)
            NSURL.fileURLWithPath(directory).setResourceValue(true, forKey = NSURLIsExcludedFromBackupKey, error = null)
        }
        val written = NSString.create(string = text)
            .writeToFile(path(name), atomically = true, encoding = NSUTF8StringEncoding, error = null)
        check(written) { "The text $name could not be kept in $directory" }
    }

    override suspend fun delete(name: String) {
        withContext(Dispatchers.IO) {
            requireTextName(name)
            if (files.fileExistsAtPath(path(name))) files.removeItemAtPath(path(name), error = null)
        }
    }

    private fun path(name: String) = "$directory/$name"
}
