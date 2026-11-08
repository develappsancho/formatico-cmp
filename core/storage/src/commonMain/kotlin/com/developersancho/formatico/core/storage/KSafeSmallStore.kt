package com.developersancho.formatico.core.storage

import eu.anifantakis.lib.ksafe.KSafe
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable

/** A [SmallStore] in KSafe (the owner's decision, 2026-11-04): encrypted, on every platform. */
internal class KSafeSmallStore(private val ksafe: KSafe) : SmallStore {
    override suspend fun <T : Any> get(key: String, serializer: KSerializer<T>): T? {
        ksafe.awaitReady()
        return ksafe.get(key, null, serializer.nullable)
    }

    override suspend fun <T : Any> put(key: String, value: T, serializer: KSerializer<T>) {
        ksafe.awaitReady()
        ksafe.put(key, value, serializer)
    }

    override suspend fun delete(key: String) {
        ksafe.awaitReady()
        ksafe.delete(key)
    }
}

/** Waits until KSafe has read what it keeps: in a browser it reads it after it is made, elsewhere it has. */
internal expect suspend fun KSafe.awaitReady()
