package com.developersancho.formatico.core.storage

import eu.anifantakis.lib.ksafe.KSafe
import kotlinx.coroutines.await
import kotlin.js.Promise

internal actual fun deviceStores(): DeviceStores =
    DeviceStores(small = KSafeSmallStore(KSafe(fileName = STORE_NAME)), texts = IndexedDbTextStore(STORE_NAME))

/** A [TextStore] in the browser's IndexedDB, which takes a text as large as a document: localStorage does not. */
internal class IndexedDbTextStore(private val database: String) : TextStore {
    override suspend fun read(name: String): String? {
        requireTextName(name)
        return readText(database, name).await()
    }

    override suspend fun write(name: String, text: String) {
        requireTextName(name)
        writeText(database, name, text).await()
    }

    override suspend fun delete(name: String) {
        requireTextName(name)
        deleteText(database, name).await()
    }
}

// Its parameters are read by the JavaScript inside js(), which detekt does not see.
@Suppress("UnusedParameter")
private fun readText(database: String, name: String): Promise<String?> = js(
    """new Promise(function (resolve, reject) {
        var open = indexedDB.open(database, 1);
        open.onupgradeneeded = function () { open.result.createObjectStore("texts"); };
        open.onerror = function () { reject(open.error); };
        open.onsuccess = function () {
            var db = open.result;
            var request = db.transaction("texts", "readonly").objectStore("texts").get(name);
            request.onsuccess = function () { db.close(); resolve(request.result === undefined ? null : request.result); };
            request.onerror = function () { db.close(); reject(request.error); };
        };
    })"""
)

// Its parameters are read by the JavaScript inside js(), which detekt does not see.
@Suppress("UnusedParameter")
private fun writeText(database: String, name: String, text: String): Promise<Unit?> = js(
    """new Promise(function (resolve, reject) {
        var open = indexedDB.open(database, 1);
        open.onupgradeneeded = function () { open.result.createObjectStore("texts"); };
        open.onerror = function () { reject(open.error); };
        open.onsuccess = function () {
            var db = open.result;
            var transaction = db.transaction("texts", "readwrite");
            transaction.objectStore("texts").put(text, name);
            transaction.oncomplete = function () { db.close(); resolve(null); };
            transaction.onerror = function () { db.close(); reject(transaction.error); };
        };
    })"""
)

// Its parameters are read by the JavaScript inside js(), which detekt does not see.
@Suppress("UnusedParameter")
private fun deleteText(database: String, name: String): Promise<Unit?> = js(
    """new Promise(function (resolve, reject) {
        var open = indexedDB.open(database, 1);
        open.onupgradeneeded = function () { open.result.createObjectStore("texts"); };
        open.onerror = function () { reject(open.error); };
        open.onsuccess = function () {
            var db = open.result;
            var transaction = db.transaction("texts", "readwrite");
            transaction.objectStore("texts").delete(name);
            transaction.oncomplete = function () { db.close(); resolve(null); };
            transaction.onerror = function () { db.close(); reject(transaction.error); };
        };
    })"""
)
