package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.data.tag.PantrixTag
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.image.PantrixImage
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme

/**
 * What a row draws at its end, in the order its board draws them: what its string looks like it holds, its anchor,
 * and where its alias points. On a [chosen] row the quieter words are in the chosen row's colour.
 */
@Composable
internal fun RowEndOf(end: RowEnd, layout: WorkspaceLayout, chosen: Boolean, words: PreviewWords) {
    val colors = PantrixTheme.colors
    val quiet = if (chosen) colors.primary.onContainer else colors.textSecondary
    end.preview?.let { PreviewOf(it, end.written.orEmpty(), layout, words) }
    end.anchor?.let { PantrixTag(text = it) }
    end.target?.let {
        val typography = PantrixTheme.typography
        PantrixText(
            text = "→ $it",
            style = typography.code.copy(fontSize = typography.footnote.fontSize, lineHeight = typography.footnote.lineHeight),
            color = quiet,
            maxLines = 1
        )
    }
}

/**
 * What a string looks like it holds, at its row's end: a date in the reader's language, a colour's swatch, a picture's
 * thumbnail. A phone, with less room, shows a date by its icon.
 */
@Composable
private fun PreviewOf(preview: ValuePreview, written: String, layout: WorkspaceLayout, words: PreviewWords) {
    val phone = layout == WorkspaceLayout.Phone
    when (preview) {
        is ValuePreview.Moment, is ValuePreview.Day -> {
            if (phone) {
                PantrixIcon(imageVector = PantrixIcons.Calendar, contentDescription = words.of(preview), size = 18.dp)
            } else {
                PantrixTag(text = words.of(preview), icon = PantrixIcons.Calendar)
            }
        }

        is ValuePreview.Colour -> {
            Swatch(
                colour = Color(preview.argb),
                size = if (phone) 18.dp else 16.dp,
                description = workspaceStrings.colourOf(written)
            )
        }

        // The tree draws a link's icon as the button that opens it, the row's action, after what stands here.
        is ValuePreview.Link -> Unit

        is ValuePreview.Picture -> PictureThumbnail(preview.url, size = if (phone) 28.dp else 24.dp)
    }
}

/**
 * A picture value's thumbnail, [size] square: the picture at [address] cut to its square, with an edge drawn inside it
 * that holds a light picture apart from the row, or while it is read and where it could not be, the picture's icon on
 * the neutral container, as the Workspace boards' `nopicture` state draws it. A screen reader hears the same of both.
 */
@Composable
internal fun PictureThumbnail(address: String, size: Dp, shape: Shape = PantrixTheme.shapes.sm) {
    val description = workspaceStrings.pictureAt
    val thumbnail = thumbnailOf(address)
    if (thumbnail is Thumbnail.Shown) {
        ThumbnailImage(thumbnail.image, description, Modifier.size(size), shape)
        return
    }
    Box(
        modifier = Modifier
            .size(size)
            .background(PantrixTheme.colors.neutral.container, shape)
            .semantics {
                contentDescription = description
                role = Role.Image
                thumbnailState = if (thumbnail == null) ThumbnailState.Reading else ThumbnailState.None
            },
        contentAlignment = Alignment.Center
    ) {
        PantrixIcon(
            imageVector = PantrixIcons.Image,
            contentDescription = null,
            size = if (size > 24.dp) 16.dp else 14.dp,
            tint = PantrixTheme.colors.neutral.onContainer
        )
    }
}

/**
 * A thumbnail's [image] in [modifier]'s place, cut to [shape], with the edge inside it that holds a light picture
 * apart, read to a screen reader as [description].
 */
@Composable
internal fun ThumbnailImage(image: ImageBitmap, description: String, modifier: Modifier, shape: Shape) {
    PantrixImage(
        bitmap = image,
        modifier = modifier
            .clip(shape)
            .border(1.dp, PantrixTheme.colors.textPrimary.copy(alpha = THUMBNAIL_EDGE_ALPHA), shape)
            .semantics { thumbnailState = ThumbnailState.Shown },
        contentDescription = description,
        contentScale = ContentScale.Crop
    )
}

/** Where a picture value's thumbnail stands: being read, drawn, or none, its icon in its place. */
enum class ThumbnailState { Reading, Shown, None }

/**
 * Where a picture value's thumbnail stands, [ThumbnailState]: what a test waits for before it writes a screen, as the
 * picture is read away from the main thread.
 */
val PictureThumbnailState = SemanticsPropertyKey<ThumbnailState>("PictureThumbnailState")

/** See [PictureThumbnailState]. */
var SemanticsPropertyReceiver.thumbnailState by PictureThumbnailState

/** How strong the edge inside a thumbnail is: the boards' 14% of the text's colour. */
private const val THUMBNAIL_EDGE_ALPHA = 0.14f

/** A square of [colour] with the theme's border, as large as [size], that a screen reader reads as [description]. */
@Composable
private fun Swatch(colour: Color, size: Dp, description: String) {
    val shape = PantrixTheme.shapes.sm
    Box(
        modifier = Modifier
            .size(size)
            .background(colour, shape)
            .border(1.dp, PantrixTheme.colors.border, shape)
            .semantics {
                contentDescription = description
                role = Role.Image
            }
    )
}
