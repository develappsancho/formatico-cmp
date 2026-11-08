package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.text.wholeRegex
import com.pantrixui.foundation.date.PantrixDate
import kotlin.time.Instant

/** What a string looks like it holds, which the tree previews beside it. */
internal sealed interface ValuePreview {
    /** A moment, written in ISO 8601 with its offset from UTC: `2026-10-28T14:32:00Z`. */
    data class Moment(val instant: Instant) : ValuePreview

    /** A day, written in ISO 8601: `2026-11-04`. */
    data class Day(val date: PantrixDate) : ValuePreview

    /** A colour written in hexadecimal, `#0F766E`, as [argb]: its alpha, red, green and blue. */
    data class Colour(val argb: Long) : ValuePreview

    /** An address on the web. */
    data class Link(val url: String) : ValuePreview

    /**
     * The address of a picture: one on the web whose path ends in a picture's extension, or a picture written into its
     * address, `data:image/png;base64,…`.
     */
    data class Picture(val url: String) : ValuePreview
}

private val MomentPattern = wholeRegex("""\d{4}-\d{2}-\d{2}[Tt ]\d{2}:\d{2}(:\d{2}(\.\d+)?)?([Zz]|[+-]\d{2}:\d{2})""")
private val DayPattern = wholeRegex("""\d{4}-\d{2}-\d{2}""")
private val ColourPattern = wholeRegex("""#([0-9A-Fa-f]{3,4}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})""")
private val LinkPattern = wholeRegex("""https?://[^\s/$.?#][^\s]*""", RegexOption.IGNORE_CASE)
private val PicturePath = Regex("""\.(png|jpe?g|gif|webp|avif|svg)$""", RegexOption.IGNORE_CASE)
private val DataPicturePattern = wholeRegex("""data:image/(png|jpe?g|gif|webp);base64,[A-Za-z0-9+/=\s]+""", RegexOption.IGNORE_CASE)

private const val HEX_RADIX = 16
private const val OPAQUE = 0xFF000000L
private const val SHORT_RGBA = 4
private const val RGB = 6
private const val ALPHA_SHIFT = 24

/** What [value] looks like it holds, or null for a string that is only a string. */
internal fun previewOf(value: String): ValuePreview? = when {
    MomentPattern.containsMatchIn(value) -> {
        momentOf(value)
    }

    DayPattern.containsMatchIn(value) -> {
        runCatching { ValuePreview.Day(PantrixDate.parse(value)) }.getOrNull()
    }

    ColourPattern.containsMatchIn(value) -> {
        ValuePreview.Colour(argbOf(value.drop(1)))
    }

    DataPicturePattern.containsMatchIn(value) -> {
        ValuePreview.Picture(value)
    }

    LinkPattern.containsMatchIn(value) -> {
        val path = value.substringBefore('?').substringBefore('#')
        if (PicturePath.containsMatchIn(path)) ValuePreview.Picture(value) else ValuePreview.Link(value)
    }

    else -> {
        null
    }
}

/** The moment ISO 8601 writes as [value], its `T` a space or in lower case as RFC 3339 allows. */
private fun momentOf(value: String): ValuePreview? =
    runCatching { ValuePreview.Moment(Instant.parse(value.uppercase().replace(' ', 'T'))) }.getOrNull()

/** The colour written as [hex], with no `#`: `RGB`, `RGBA`, `RRGGBB` or `RRGGBBAA`, as CSS reads them. */
private fun argbOf(hex: String): Long {
    val long = if (hex.length <= SHORT_RGBA) hex.map { "$it$it" }.joinToString("") else hex
    val rgb = long.take(RGB).toLong(HEX_RADIX)
    val alpha = if (long.length > RGB) long.drop(RGB).toLong(HEX_RADIX) shl ALPHA_SHIFT else OPAQUE
    return alpha or rgb
}
