package com.developersancho.formatico.core.storage

import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileTextStoreTest {
    private val directory: File = Files.createTempDirectory("formatico-texts").toFile()

    @AfterTest
    fun removeTheFolder() {
        directory.deleteRecursively()
    }

    @Test
    fun aTextIsKeptInAFileOfItsNameAndReplacedWhole() = runTest {
        val store = FileTextStore(File(directory, "texts"))

        assertNull(store.read("open-1"))
        store.write("open-1", "name: Elif Yılmaz\n")
        store.write("open-1", "name: Mert Demir\n")

        assertEquals("name: Mert Demir\n", store.read("open-1"))
        assertEquals(listOf("open-1"), File(directory, "texts").list()?.toList())
    }

    @Test
    fun aDeletedTextIsGoneAndDeletingNothingIsNoMistake() = runTest {
        val store = FileTextStore(directory)

        store.write("history-3", "a,b\n1,2\n")
        store.delete("history-3")
        store.delete("history-4")

        assertNull(store.read("history-3"))
        assertTrue(directory.list().isNullOrEmpty())
    }
}
