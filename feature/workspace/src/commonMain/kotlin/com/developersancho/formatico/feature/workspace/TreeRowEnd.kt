package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pantrixui.components.data.tag.PantrixTag
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme
import formatico.feature.workspace.generated.resources.Res
import formatico.feature.workspace.generated.resources.workspace_colour_of
import formatico.feature.workspace.generated.resources.workspace_picture_at
import org.jetbrains.compose.resources.stringResource

/**
 * What a row draws at its end, in the order its board draws them: what its string looks like it holds, its anchor,
 * and where its alias points. On a [chosen] row the quieter words are in the chosen row's colour.
 */
@Composable
internal fun RowEndOf(end: RowEnd, layout: WorkspaceLayout, chosen: Boolean, words: PreviewWords) {
    val colors = PantrixTheme.colors
    val quiet = if (chosen) colors.primary.onContainer else colors.textSecondary
    end.preview?.let { PreviewOf(it, end.written.orEmpty(), layout, quiet, words) }
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
 * What a string looks like it holds, at its row's end: a date in the reader's language, a colour's swatch, a link's
 * icon, a picture's place with its icon. A phone, with less room, shows a date by its icon.
 */
@Composable
private fun PreviewOf(preview: ValuePreview, written: String, layout: WorkspaceLayout, quiet: Color, words: PreviewWords) {
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
                description = stringResource(Res.string.workspace_colour_of, written)
            )
        }

        is ValuePreview.Link -> {
            PantrixIcon(
                imageVector = PantrixIcons.ExternalLink,
                contentDescription = words.of(preview),
                size = if (phone) 18.dp else 16.dp,
                tint = quiet
            )
        }

        is ValuePreview.Picture -> {
            // The board's place for a picture: its icon on the neutral container, not the picture itself.
            val description = stringResource(Res.string.workspace_picture_at)
            Box(
                modifier = Modifier
                    .size(if (phone) 28.dp else 24.dp)
                    .background(PantrixTheme.colors.neutral.container, PantrixTheme.shapes.sm)
                    .semantics {
                        contentDescription = description
                        role = Role.Image
                    },
                contentAlignment = Alignment.Center
            ) {
                PantrixIcon(
                    imageVector = PantrixIcons.Image,
                    contentDescription = null,
                    size = if (phone) 16.dp else 14.dp,
                    tint = PantrixTheme.colors.neutral.onContainer
                )
            }
        }
    }
}

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
