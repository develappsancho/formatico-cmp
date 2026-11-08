package com.developersancho.formatico.core.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** A [TextStore] of files in [directory], one a text, each written whole beside it and then moved into its place. */
internal class FileTextStore(private val directory: File) : TextStore {
    override suspend fun read(name: String): String? = withContext(Dispatchers.IO) {
        requireTextName(name)
        File(directory, name).takeIf { it.isFile }?.readText()
    }

    override suspend fun write(name: String, text: String) = withContext(Dispatchers.IO) {
        requireTextName(name)
        directory.mkdirs()
        val written = File(directory, "$name.writing")
        written.writeText(text)
        check(written.renameTo(File(directory, name)) || replace(written, File(directory, name))) {
            "The text $name could not be kept in $directory"
        }
    }

    override suspend fun delete(name: String) {
        withContext(Dispatchers.IO) {
            requireTextName(name)
            File(directory, name).delete()
        }
    }

    /** Where a move does not replace a file, as on Windows: the old one goes first. */
    private fun replace(from: File, to: File): Boolean = to.delete() && from.renameTo(to)
}
