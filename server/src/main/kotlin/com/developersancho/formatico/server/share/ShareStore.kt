package com.developersancho.formatico.server.share

import com.developersancho.formatico.core.share.ShareKeep
import com.developersancho.formatico.core.share.ShareServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import reactor.core.publisher.Flux
import java.io.IOException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64

/** A share the store keeps: its [id], the [deleteToken] that deletes it, and the moment it [expiresAt]. */
data class KeptShare(val id: String, val deleteToken: String, val expiresAt: Instant)

/** What came of keeping a share. */
sealed interface Keeping {
    /** It is kept as [share]. */
    data class Kept(val share: KeptShare) : Keeping

    /** There was nothing to keep. */
    data object Empty : Keeping

    /** It is larger than [ShareServer.MAX_SHARE_BYTES]. */
    data object TooLarge : Keeping
}

/** What came of deleting a share. */
enum class Deleting {
    /** It is deleted. */
    Deleted,

    /** The token is not the share's. */
    Refused,

    /** There is no such share: it never was, it expired or it was deleted. */
    Missing
}

/**
 * The shares, on disk under [root], a folder for each, named by its id: the encrypted bytes, which the server cannot
 * read, and a [ShareRecord] of when it was made, when it expires, its size and the SHA-256 of its delete token, never
 * the token itself. [clock] says what time it is; [random] draws the ids and the tokens.
 *
 * A share is written in a folder of its own under `.incoming` and moved into place whole, so a share half written is
 * never served. It is deleted record first: a folder with no record is no share.
 */
class ShareStore(private val root: Path, private val clock: Clock, private val random: SecureRandom = SecureRandom()) {
    private val incoming = root.resolve(INCOMING)

    init {
        Files.createDirectories(incoming)
    }

    /** Keeps the share [body] for [keep]'s days, reading no more of it than one byte past the most a share has. */
    suspend fun keep(body: Flux<DataBuffer>, keep: ShareKeep): Keeping {
        val draft = withContext(Dispatchers.IO) { Files.createTempDirectory(incoming, DRAFT_PREFIX) }
        try {
            val data = draft.resolve(DATA)
            DataBufferUtils.write(DataBufferUtils.takeUntilByteCount(body, ShareServer.MAX_SHARE_BYTES + 1L), data).awaitSingleOrNull()
            return withContext(Dispatchers.IO) {
                val size = if (Files.exists(data)) Files.size(data) else 0L
                when {
                    size == 0L -> Keeping.Empty
                    size > ShareServer.MAX_SHARE_BYTES -> Keeping.TooLarge
                    else -> Keeping.Kept(publish(draft, size, keep))
                }
            }
        } finally {
            withContext(NonCancellable + Dispatchers.IO) { ShareFolders.deleteTree(draft) }
        }
    }

    /** The bytes of the share [id]; null where there is no such share. A share that has expired is deleted. */
    suspend fun read(id: String): ByteArray? = withContext(Dispatchers.IO) {
        val share = live(id) ?: return@withContext null
        try {
            Files.readAllBytes(share.folder.resolve(DATA))
        } catch (_: NoSuchFileException) {
            null
        }
    }

    /** Deletes the share [id] where [token] is its delete token. A share that has expired is deleted, and missing. */
    suspend fun delete(id: String, token: String?): Deleting = withContext(Dispatchers.IO) {
        val share = live(id)
        when {
            share == null -> Deleting.Missing
            token == null || !share.record.isDeletedBy(token) -> Deleting.Refused
            else -> Deleting.Deleted.also { remove(share.folder) }
        }
    }

    /**
     * Deletes every share that has expired, every folder that is no share, and every draft no one has written to for
     * an hour: what an upload that was cut off, or a server that stopped, left. Blocks; returns how many shares went.
     */
    fun deleteExpired(): Int {
        val now = clock.instant()
        var deleted = 0
        for (entry in ShareFolders.children(root)) {
            val name = entry.fileName.toString()
            if (name == INCOMING) {
                deleteStaleDrafts(now)
            } else if (ShareServer.Id.matches(name)) {
                val record = ShareRecord.readFrom(entry)
                if (record == null || record.hasExpired(now)) {
                    remove(entry)
                    deleted++
                }
            }
        }
        return deleted
    }

    private fun publish(draft: Path, size: Long, keep: ShareKeep): KeptShare {
        val created = clock.instant().truncatedTo(ChronoUnit.SECONDS)
        val expiresAt = created.plus(keep.days.toLong(), ChronoUnit.DAYS)
        val token = ByteArray(TOKEN_BYTES).also(random::nextBytes).let(TokenEncoder::encodeToString)
        repeat(ID_ATTEMPTS) {
            val id = newId()
            ShareRecord(id, created, expiresAt, size, ShareRecord.hashOf(token)).writeTo(draft)
            if (ShareFolders.moveIfFree(draft, root.resolve(id))) return KeptShare(id, token, expiresAt)
        }
        throw IOException("No free share id after $ID_ATTEMPTS tries")
    }

    /**
     * The share [id] where it is there and has not expired; one that has expired is deleted. Its record must be the
     * record of [id]: on a disk that does not tell `a` from `A`, the folder of another id is not.
     */
    private fun live(id: String): Share? {
        // The id's form keeps every other path out: `..`, a slash or `.incoming` is none.
        if (!ShareServer.Id.matches(id)) return null
        val folder = root.resolve(id)
        val record = ShareRecord.readFrom(folder)?.takeIf { it.id == id } ?: return null
        if (record.hasExpired(clock.instant())) {
            remove(folder)
            return null
        }
        return Share(folder, record)
    }

    private fun remove(folder: Path) {
        Files.deleteIfExists(folder.resolve(ShareRecord.FILE))
        ShareFolders.deleteTree(folder)
    }

    private fun deleteStaleDrafts(now: Instant) {
        for (draft in ShareFolders.children(incoming)) {
            val written = try {
                ShareFolders.lastWritten(draft)
            } catch (_: NoSuchFileException) {
                // Its upload finished, or failed, in the meantime.
                continue
            }
            if (Duration.between(written, now) > STALE_DRAFT) ShareFolders.deleteTree(draft)
        }
    }

    private fun newId(): String = String(CharArray(ID_LENGTH) { ID_ALPHABET[random.nextInt(ID_ALPHABET.length)] })

    private class Share(val folder: Path, val record: ShareRecord)

    private companion object {
        const val INCOMING = ".incoming"
        const val DRAFT_PREFIX = "share-"
        const val DATA = "share.bin"

        /** A share id's length, as [ShareServer.Id] has it. */
        const val ID_LENGTH = 10
        const val ID_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        const val ID_ATTEMPTS = 8
        const val TOKEN_BYTES = 32
        val STALE_DRAFT: Duration = Duration.ofHours(1)

        val TokenEncoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()
    }
}
