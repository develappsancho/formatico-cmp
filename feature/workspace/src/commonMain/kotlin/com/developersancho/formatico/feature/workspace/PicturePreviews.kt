package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint

/** The side of the preview's portrait, in pixels. */
private const val PORTRAIT_SIDE = 96

/** Where the portrait's head stands and how large it is, as shares of its side. */
private const val HEAD_Y = 0.4f
private const val HEAD_RADIUS = 0.2f

/** Where the portrait's shoulders start, and how far past its sides they reach, as shares of its side. */
private const val SHOULDERS_TOP = 0.66f
private const val SHOULDERS_OVERHANG = 0.1f

/**
 * A portrait drawn for the previews, in the place of the avatar the order's `avatar` holds: a head and shoulders on a
 * light ground, its own colours, as a photograph has them.
 */
internal val PreviewPortrait: ImageBitmap by lazy {
    val side = PORTRAIT_SIDE.toFloat()
    ImageBitmap(PORTRAIT_SIDE, PORTRAIT_SIDE).also { image ->
        val canvas = Canvas(image)
        canvas.drawRect(Rect(0f, 0f, side, side), Paint().apply { color = Color(0xFFCCE3DE) })
        canvas.drawOval(
            Rect(-side * SHOULDERS_OVERHANG, side * SHOULDERS_TOP, side * (1 + SHOULDERS_OVERHANG), side * (1 + SHOULDERS_TOP)),
            Paint().apply { color = Color(0xFF3F6E8C) }
        )
        canvas.drawCircle(Offset(side / 2, side * HEAD_Y), side * HEAD_RADIUS, Paint().apply { color = Color(0xFFE9B99A) })
    }
}

/** A preview's or a test's thumbnails, known at once: every picture is [PreviewPortrait] where [read], and none else. */
internal class PreviewThumbnails(private val read: Boolean) : PictureThumbnails {
    override fun known(address: String): Thumbnail = if (read) Thumbnail.Shown(PreviewPortrait) else Thumbnail.None

    override suspend fun load(address: String): Thumbnail = known(address)
}

/** [content] with its pictures' thumbnails read, where [read] says so, or none of them: their icons in their places. */
@Composable
internal fun PreviewPictures(read: Boolean = true, content: @Composable () -> Unit) {
    val thumbnails = remember(read) { PreviewThumbnails(read) }
    CompositionLocalProvider(LocalPictureThumbnails provides thumbnails, content = content)
}
