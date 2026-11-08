package com.developersancho.formatico.server.share

import com.developersancho.formatico.core.share.ShareKeep
import com.developersancho.formatico.core.share.ShareServer
import com.developersancho.formatico.server.MutableClock
import kotlinx.coroutines.runBlocking
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import reactor.core.publisher.Flux
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.HexFormat
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The shares on disk: how they are written, read, deleted and cleaned away. */
class ShareStoreTest {
    private val root: Path = Files.createTempDirectory("formatico-store")
    private val clock = MutableClock(Instant.parse("2026-11-06T12:00:00.750Z"))
    private val store = ShareStore(root, clock)
    private val buffers = DefaultDataBufferFactory.sharedInstance

    @AfterTest
    fun delete() {
        root.toFile().deleteRecursively()
    }

    @Test
    fun keepsAShareAndReadsItBack() = runBlocking {
        val bytes = ByteArray(100_000) { (it % 199).toByte() }
        val kept = kept(store.keep(chunks(bytes, 8_192), ShareKeep.Month))
        assertTrue(ShareServer.Id.matches(kept.id), kept.id)
        assertEquals(Instant.parse("2026-12-06T12:00:00Z"), kept.expiresAt)
        assertContentEquals(bytes, store.read(kept.id))
        assertEquals(listOf(".incoming", kept.id), Files.list(root).use { it.map { path -> path.fileName.toString() }.sorted().toList() })
        assertEquals(emptyList(), Files.list(root.resolve(".incoming")).use { it.toList() })
    }

    @Test
    fun recordsTheTokensHashAndNeverTheToken() = runBlocking {
        val kept = kept(store.keep(chunks("secret".encodeToByteArray()), ShareKeep.Day))
        val record = Files.readString(root.resolve(kept.id).resolve("share.json"))
        val hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(kept.deleteToken.encodeToByteArray()))
        assertTrue(record.contains(hash), record)
        assertFalse(record.contains(kept.deleteToken), record)
        assertTrue(record.contains("\"expiresAt\":\"2026-11-07T12:00:00Z\""), record)
        assertTrue(record.contains("\"created\":\"2026-11-06T12:00:00Z\""), record)
        assertTrue(record.contains("\"size\":6"), record)
    }

    @Test
    fun drawsADifferentIdAndTokenForEachShare() = runBlocking {
        val shares = List(50) { kept(store.keep(chunks(byteArrayOf(1)), ShareKeep.Day)) }
        assertEquals(50, shares.map { it.id }.toSet().size)
        assertEquals(50, shares.map { it.deleteToken }.toSet().size)
        for (share in shares) assertTrue(Regex("[A-Za-z0-9_-]{43}").matches(share.deleteToken), share.deleteToken)
    }

    @Test
    fun drawsAnotherIdWhereOneIsTaken() = runBlocking {
        // The first id drawn is AAAAAAAAAA, which another share has; the second, BBBBBBBBBB, is free.
        val draws = AtomicInteger()
        val random = object : SecureRandom() {
            override fun nextInt(bound: Int): Int = if (draws.getAndIncrement() < 10) 0 else 1
        }
        val taken = root.resolve("AAAAAAAAAA")
        Files.createDirectories(taken)
        Files.writeString(taken.resolve("share.json"), "another share's")
        val kept = kept(ShareStore(root, clock, random).keep(chunks(byteArrayOf(7)), ShareKeep.Day))
        assertEquals("BBBBBBBBBB", kept.id)
        assertEquals("another share's", Files.readString(taken.resolve("share.json")))
    }

    @Test
    fun keepsTheLargestShare() = runBlocking {
        val bytes = ByteArray(ShareServer.MAX_SHARE_BYTES) { (it % 7).toByte() }
        val kept = kept(store.keep(chunks(bytes, 1024 * 1024), ShareKeep.Day))
        assertEquals(ShareServer.MAX_SHARE_BYTES, store.read(kept.id)?.size)
    }

    @Test
    fun stopsReadingAShareThatTurnsOutTooLarge() = runBlocking {
        val delivered = AtomicInteger()
        val chunk = ByteArray(1024 * 1024)
        // Far more than a share has, as a client that says no length might send.
        val endless = Flux.range(0, 1_000).map { buffers.wrap(chunk) as DataBuffer }.doOnNext { delivered.incrementAndGet() }
        assertEquals(Keeping.TooLarge, store.keep(endless, ShareKeep.Week))
        assertTrue(delivered.get() <= ShareServer.MAX_SHARE_BYTES / chunk.size + 2, "read ${delivered.get()} MB")
        assertEquals(listOf(".incoming"), Files.list(root).use { it.map { path -> path.fileName.toString() }.toList() })
        assertEquals(emptyList(), Files.list(root.resolve(".incoming")).use { it.toList() })
    }

    @Test
    fun keepsNothingOfAnEmptyShare() = runBlocking {
        assertEquals(Keeping.Empty, store.keep(Flux.empty(), ShareKeep.Week))
        assertEquals(Keeping.Empty, store.keep(chunks(ByteArray(0)), ShareKeep.Week))
        assertEquals(listOf(".incoming"), Files.list(root).use { it.map { path -> path.fileName.toString() }.toList() })
    }

    @Test
    fun leavesNoDraftWhenTheUploadFails() = runBlocking {
        val failing = Flux.concat(chunks(ByteArray(1_000)), Flux.error(IllegalStateException("the client went away")))
        runCatching { store.keep(failing, ShareKeep.Week) }
        assertEquals(emptyList(), Files.list(root.resolve(".incoming")).use { it.toList() })
        assertEquals(listOf(".incoming"), Files.list(root).use { it.map { path -> path.fileName.toString() }.toList() })
    }

    @Test
    fun readsNoShareThatIsNotThere() = runBlocking {
        for (id in listOf("Zz9Zz9Zz9Z", "short", "../../etc", ".incoming", "abcdefghijk", "")) assertNull(store.read(id), id)
    }

    @Test
    fun readsNoShareUnderAnotherId() = runBlocking {
        // A disk that does not tell `a` from `A` finds the folder of another id: its record says whose it is.
        val kept = kept(store.keep(chunks(byteArrayOf(1, 2, 3)), ShareKeep.Day))
        val other = "Qq1Qq1Qq1Q"
        Files.move(root.resolve(kept.id), root.resolve(other))
        assertNull(store.read(other))
    }

    @Test
    fun deletesAShareThatHasExpiredWhenItIsRead() = runBlocking {
        val kept = kept(store.keep(chunks(byteArrayOf(1)), ShareKeep.Day))
        clock.now = kept.expiresAt.minusSeconds(1)
        assertContentEquals(byteArrayOf(1), store.read(kept.id))
        clock.now = kept.expiresAt
        assertNull(store.read(kept.id))
        assertFalse(Files.exists(root.resolve(kept.id)))
    }

    @Test
    fun deletesAShareOnlyWithItsToken() = runBlocking {
        val kept = kept(store.keep(chunks(byteArrayOf(1)), ShareKeep.Week))
        assertEquals(Deleting.Refused, store.delete(kept.id, null))
        assertEquals(Deleting.Refused, store.delete(kept.id, ""))
        assertEquals(Deleting.Refused, store.delete(kept.id, kept.deleteToken.uppercase()))
        assertEquals(Deleting.Missing, store.delete("Zz9Zz9Zz9Z", kept.deleteToken))
        assertEquals(Deleting.Missing, store.delete("../${kept.id}", kept.deleteToken))
        assertEquals(Deleting.Deleted, store.delete(kept.id, kept.deleteToken))
        assertEquals(Deleting.Missing, store.delete(kept.id, kept.deleteToken))
        assertFalse(Files.exists(root.resolve(kept.id)))
    }

    @Test
    fun answersAShareThatHasExpiredAsMissingWhenItIsDeleted() = runBlocking {
        val kept = kept(store.keep(chunks(byteArrayOf(1)), ShareKeep.Day))
        clock.advance(Duration.ofDays(1))
        assertEquals(Deleting.Missing, store.delete(kept.id, kept.deleteToken))
        assertFalse(Files.exists(root.resolve(kept.id)))
    }

    @Test
    fun cleansAwayWhatHasExpiredAndWhatIsNoShare() = runBlocking {
        val day = kept(store.keep(chunks(byteArrayOf(1)), ShareKeep.Day))
        val week = kept(store.keep(chunks(byteArrayOf(2)), ShareKeep.Week))
        val month = kept(store.keep(chunks(byteArrayOf(3)), ShareKeep.Month))
        // A share whose record went with a deletion cut short, a draft an upload left, and a file that is not the store's.
        val recordless = root.resolve("Rr2Rr2Rr2R").also { Files.createDirectories(it) }
        Files.write(recordless.resolve("share.bin"), byteArrayOf(9))
        val stale = Files.createDirectory(root.resolve(".incoming").resolve("share-stale"))
        Files.write(stale.resolve("share.bin"), byteArrayOf(9))
        val fresh = Files.createDirectory(root.resolve(".incoming").resolve("share-fresh"))
        Files.write(fresh.resolve("share.bin"), byteArrayOf(9))
        val unrelated = Files.writeString(root.resolve("README"), "not a share")

        clock.advance(Duration.ofDays(8))
        written(stale, clock.now.minus(Duration.ofHours(2)))
        written(fresh, clock.now.minusSeconds(60))

        assertEquals(3, store.deleteExpired())
        assertFalse(Files.exists(root.resolve(day.id)))
        assertFalse(Files.exists(root.resolve(week.id)))
        assertTrue(Files.exists(root.resolve(month.id)))
        assertFalse(Files.exists(recordless))
        assertFalse(Files.exists(stale))
        assertTrue(Files.exists(fresh))
        assertTrue(Files.exists(unrelated))
        assertContentEquals(byteArrayOf(3), store.read(month.id))
    }

    @Test
    fun keepsNoShareThatWouldTakeTheSharesPastTheMostTheyHold() = runBlocking {
        val small = ShareStore(root, clock, maxBytes = 250)
        val first = kept(small.keep(chunks(ByteArray(100)), ShareKeep.Day))
        kept(small.keep(chunks(ByteArray(100)), ShareKeep.Day))
        assertEquals(Keeping.Full, small.keep(chunks(ByteArray(100)), ShareKeep.Day))
        assertEquals(200, small.usedBytes)
        assertEquals(emptyList(), Files.list(root.resolve(".incoming")).use { it.toList() })

        assertEquals(Deleting.Deleted, small.delete(first.id, first.deleteToken))
        assertEquals(100, small.usedBytes)
        kept(small.keep(chunks(ByteArray(150)), ShareKeep.Day))
        assertEquals(250, small.usedBytes)
    }

    @Test
    fun readsNoShareOnceTheSharesHoldTheMost() = runBlocking {
        val small = ShareStore(root, clock, maxBytes = 100)
        kept(small.keep(chunks(ByteArray(100)), ShareKeep.Day))
        val unread = Flux.error<DataBuffer>(AssertionError("A full store reads no share"))
        assertEquals(Keeping.Full, small.keep(unread, ShareKeep.Day))
    }

    @Test
    fun countsTheSharesOnDiskWhenItOpensAndAsTheyExpire() = runBlocking {
        kept(store.keep(chunks(ByteArray(300)), ShareKeep.Day))
        kept(store.keep(chunks(ByteArray(500)), ShareKeep.Month))
        Files.writeString(root.resolve("README"), "not a share")
        val opened = ShareStore(root, clock)
        assertEquals(800, opened.usedBytes)

        clock.advance(Duration.ofDays(2))
        assertEquals(1, opened.deleteExpired())
        assertEquals(500, opened.usedBytes)
    }

    /** Says [draft] and what it holds were last written [at]. */
    private fun written(draft: Path, at: Instant) {
        Files.list(draft).use { files -> files.forEach { Files.setLastModifiedTime(it, FileTime.from(at)) } }
        Files.setLastModifiedTime(draft, FileTime.from(at))
    }

    private fun kept(keeping: Keeping): KeptShare = assertIs<Keeping.Kept>(keeping).share

    private fun chunks(bytes: ByteArray, size: Int = bytes.size.coerceAtLeast(1)): Flux<DataBuffer> =
        Flux.fromIterable((bytes.indices step size).map { bytes.copyOfRange(it, minOf(it + size, bytes.size)) }.ifEmpty { listOf(bytes) })
            .map { buffers.wrap(it) as DataBuffer }
}
