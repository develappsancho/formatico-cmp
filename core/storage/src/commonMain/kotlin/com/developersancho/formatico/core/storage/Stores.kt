package com.developersancho.formatico.core.storage

import kotlinx.serialization.KSerializer

/**
 * Small values kept on this device and read back when the app opens again: the theme, the lists of the documents
 * opened. Each is kept under a key, written with its [KSerializer].
 */
interface SmallStore {
    /** The value kept under [key], or null where none is. */
    suspend fun <T : Any> get(key: String, serializer: KSerializer<T>): T?

    /** Keeps [value] under [key], in place of what was there. */
    suspend fun <T : Any> put(key: String, value: T, serializer: KSerializer<T>)

    /** Forgets what is kept under [key]. */
    suspend fun delete(key: String)
}

/**
 * The texts of documents, kept on this device: as large as a document may be, which a [SmallStore] is not made for.
 * A text is kept under a name of small letters, digits, `-` and `_`.
 */
interface TextStore {
    /** The text kept under [name], or null where none is. */
    suspend fun read(name: String): String?

    /** Keeps [text] under [name], in place of what was there. */
    suspend fun write(name: String, text: String)

    /** Forgets the text kept under [name]. */
    suspend fun delete(name: String)
}

/** The stores of this device, as the platform has them. */
class DeviceStores(val small: SmallStore, val texts: TextStore)

/** Whether [name] can name a text: what a file, a key in a browser's store and a test's map all take. */
internal fun requireTextName(name: String) {
    require(name.isNotEmpty() && name.all { it in 'a'..'z' || it in '0'..'9' || it == '-' || it == '_' }) {
        "A text's name is small letters, digits, '-' and '_': $name"
    }
}
