package com.developersancho.formatico.core.storage

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer

/** A [SmallStore] that keeps its values while it lives: for tests, and for an app that is to keep nothing. */
class MemorySmallStore : SmallStore {
    private val lock = Mutex()
    private val values = mutableMapOf<String, Any>()

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Any> get(key: String, serializer: KSerializer<T>): T? =
        lock.withLock { values[key] as T? }

    override suspend fun <T : Any> put(key: String, value: T, serializer: KSerializer<T>) {
        lock.withLock { values[key] = value }
    }

    override suspend fun delete(key: String) {
        lock.withLock { values.remove(key) }
    }
}

/** A [TextStore] that keeps its texts while it lives: for tests, and for an app that is to keep nothing. */
class MemoryTextStore : TextStore {
    private val lock = Mutex()
    private val texts = mutableMapOf<String, String>()

    /** The names of the texts it keeps, in no order: what a test looks at. */
    suspend fun names(): Set<String> = lock.withLock { texts.keys.toSet() }

    override suspend fun read(name: String): String? {
        requireTextName(name)
        return lock.withLock { texts[name] }
    }

    override suspend fun write(name: String, text: String) {
        requireTextName(name)
        lock.withLock { texts[name] = text }
    }

    override suspend fun delete(name: String) {
        requireTextName(name)
        lock.withLock { texts.remove(name) }
    }
}
