package com.developersancho.formatico.server.share

import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant

/**
 * The store's work on folders, which another request or the cleanup may be deleting at the same time: a folder that
 * has gone is one with nothing left to do.
 */
internal object ShareFolders {
    /** What [folder] holds; nothing where it has gone. */
    fun children(folder: Path): List<Path> = try {
        Files.newDirectoryStream(folder).use { it.toList() }
    } catch (_: NoSuchFileException) {
        emptyList()
    }

    /** Deletes [path], and all it holds where it is a folder. A link is deleted, never followed. */
    fun deleteTree(path: Path) {
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) children(path).forEach(::deleteTree)
        Files.deleteIfExists(path)
    }

    /** When [path], or anything it holds, was last written. */
    fun lastWritten(path: Path): Instant {
        val own = Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS).toInstant()
        if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) return own
        return children(path).map(::lastWritten).fold(own, ::maxOf)
    }

    /**
     * Moves [draft] to [target] in one step, where nothing is at [target]: false where something is, so that the
     * caller draws another name.
     */
    fun moveIfFree(draft: Path, target: Path): Boolean {
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) return false
        try {
            Files.move(draft, target, StandardCopyOption.ATOMIC_MOVE)
        } catch (failed: FileSystemException) {
            // Another took the name in between: rename(2) says EEXIST or ENOTEMPTY, which Java names variously.
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS) && Files.exists(draft, LinkOption.NOFOLLOW_LINKS)) return false
            throw failed
        }
        return true
    }
}
