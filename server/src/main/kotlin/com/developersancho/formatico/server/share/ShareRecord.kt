package com.developersancho.formatico.server.share

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.security.MessageDigest
import java.time.DateTimeException
import java.time.Instant
import java.util.HexFormat

/**
 * What the store keeps of a share beside its bytes, in its folder's `share.json`: its [id], when it was [created] and
 * when it [expiresAt], its [size], and the SHA-256 of its delete token in hex, [deleteTokenSha256], never the token.
 */
@Serializable
internal data class ShareRecord(
    val id: String,
    @Serializable(with = InstantSerializer::class) val created: Instant,
    @Serializable(with = InstantSerializer::class) val expiresAt: Instant,
    val size: Long,
    val deleteTokenSha256: String
) {
    /** Whether the share has expired at [now]. */
    fun hasExpired(now: Instant): Boolean = !now.isBefore(expiresAt)

    /** Whether [token] is the share's delete token, compared in constant time: how long it takes tells nothing of the hash. */
    fun isDeletedBy(token: String): Boolean = MessageDigest.isEqual(Hex.parseHex(deleteTokenSha256), sha256(token))

    /** Writes the record into [folder]. */
    fun writeTo(folder: Path) {
        Files.writeString(folder.resolve(FILE), Records.encodeToString(serializer(), this))
    }

    companion object {
        /** The record's file in a share's folder. */
        const val FILE = "share.json"

        private val Hex = HexFormat.of()
        private val Records = Json { ignoreUnknownKeys = true }

        /** What a record keeps of the delete [token]: its SHA-256, in hex. */
        fun hashOf(token: String): String = Hex.formatHex(sha256(token))

        /** The record in [folder]; null where it has none, or one that cannot be read. */
        fun readFrom(folder: Path): ShareRecord? {
            val text = try {
                Files.readString(folder.resolve(FILE))
            } catch (_: NoSuchFileException) {
                return null
            }
            return try {
                Records.decodeFromString(serializer(), text)
            } catch (_: IllegalArgumentException) {
                null
            } catch (_: DateTimeException) {
                null
            }
        }

        private fun sha256(token: String): ByteArray = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
    }
}

/** An instant as ISO-8601 writes it, in UTC. */
private object InstantSerializer : KSerializer<Instant> {
    override val descriptor = PrimitiveSerialDescriptor("java.time.Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
}
