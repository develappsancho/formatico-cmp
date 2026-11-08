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
import java.util.concurrent.atomic.AtomicLong

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

    /** The shares the store keeps would hold more than the most it keeps with it. */
    data object Full : Keeping
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
 * The shares hold [maxBytes] at the most, together, so that the disk the host shares with other applications is never
 * filled: what they hold is counted from their records when the store opens, and kept as shares come and go.
 *
 * A share is written in a folder of its own under `.incoming` and moved into place whole, so a share half written is
 * never served. It is deleted record first: a folder with no record is no share.
 */
class ShareStore(
    private val root: Path,
    private val clock: Clock,
    private val random: SecureRandom = SecureRandom(),
    private val maxBytes: Long = Long.MAX_VALUE
) {
    private val incoming = root.resolve(INCOMING)
    private val used: AtomicLong

    init {
        Files.createDirectories(incoming)
        val shares = ShareFolders.children(root).filter { ShareServer.Id.matches(it.fileName.toString()) }
        used = AtomicLong(shares.sumOf { folder -> ShareRecord.readFrom(folder)?.size ?: 0L })
    }

    /** How many bytes the shares hold, together, as their records say. */
    val usedBytes: Long get() = used.get()

    /**
     * Keeps the share [body] for [keep]'s days, reading no more of it than one byte past the most a share has. A share
     * the store has no room left for is not read.
     */
    suspend fun keep(body: Flux<DataBuffer>, keep: ShareKeep): Keeping {
        if (used.get() >= maxBytes) return Keeping.Full
        val draft = withContext(Dispatchers.IO) { Files.createTempDirectory(incoming, DRAFT_PREFIX) }
        try {
            val data = draft.resolve(DATA)
            DataBufferUtils.write(DataBufferUtils.takeUntilByteCount(body, ShareServer.MAX_SHARE_BYTES + 1L), data).awaitSingleOrNull()
            return withContext(Dispatchers.IO) {
                val size = if (Files.exists(data)) Files.size(data) else 0L
                when {
                    size == 0L -> Keeping.Empty
                    size > ShareServer.MAX_SHARE_BYTES -> Keeping.TooLarge
                    else -> publishWithin(draft, size, keep)?.let(Keeping::Kept) ?: Keeping.Full
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
            else -> Deleting.Deleted.also { remove(share.folder, share.record) }
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
                    remove(entry, record)
                    deleted++
                }
            }
        }
        return deleted
    }

    /**
     * Publishes the share in [draft] where the shares, with its [size], stay within [maxBytes]: null where they would
     * not. Its size counts from here, and is given back where it could not be published.
     */
    private fun publishWithin(draft: Path, size: Long, keep: ShareKeep): KeptShare? {
        val before = used.getAndAccumulate(size) { now, more -> if (now + more > maxBytes) now else now + more }
        if (before + size > maxBytes) return null
        return try {
            publish(draft, size, keep)
        } catch (failure: IOException) {
            used.addAndGet(-size)
            throw failure
        }
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
            remove(folder, record)
            return null
        }
        return Share(folder, record)
    }

    /**
     * Deletes the share in [folder], [record] first: its size stops counting where this deletion took the record away,
     * so that two deletions of one share count it once.
     */
    private fun remove(folder: Path, record: ShareRecord?) {
        if (Files.deleteIfExists(folder.resolve(ShareRecord.FILE)) && record != null) used.addAndGet(-record.size)
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
