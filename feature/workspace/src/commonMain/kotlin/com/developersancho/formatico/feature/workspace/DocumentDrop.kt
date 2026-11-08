package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.foundation.files.PantrixFile
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme

/** Opens the files dropped on an open document, each in a tab of its own, as the Start screen opens a file. */
interface DocumentDrop {
    /** The most bytes a file may have, which the cover over the document says. */
    val maxFileSize: Long

    /** Opens the first of [files] in a new tab, or says why it was not opened. */
    fun open(files: List<PantrixFile>)
}

/** The app's [DocumentDrop]; none in a preview or a test that gives none, where a document takes no file. */
val LocalDocumentDrop = staticCompositionLocalOf<DocumentDrop?> { null }

/** The widest the drop zone stands: the Workspace boards' 640 in a wide window and 600 on a tablet. */
private val WideZoneWidth = 640.dp
private val ZoneWidth = 600.dp

/** The room above and below the zone's words: the boards' 40. */
private val ZoneVerticalPadding = 40.dp

/** The size of the zone's icon. */
private val ZoneIconSize = 28.dp

/** How much of the page's background the cover lets through: the boards' 80%. */
private const val COVER_ALPHA = 0.8f

/** How much of the primary container the zone's fill is, over the surface: the boards' 75%. */
private const val ZONE_FILL = 0.75f

/** The width of the zone's dashed edge, and of its dashes and the gaps between them. */
private val EdgeWidth = 2.dp
private val DashLength = 6.dp
private val DashGap = 4.dp

/**
 * What covers an open document while a file is dragged over it, as the Workspace boards' `drop` state draws it: the
 * page's background over everything under the tabs, and in its middle a zone with a dashed edge that says the file opens
 * in a new tab, what kinds it takes, and files of [maxFileSize] at most. A screen reader is told it as it comes. Pantrix
 * has no such cover, and Formatico draws it until it has one (PANTRIX_FINDINGS, "Bir pencerenin ya da bölmenin üstüne
 * dosya sürüklenince görünen bir kapak yok").
 */
@Composable
internal fun DropCover(maxFileSize: Long, layout: WorkspaceLayout, modifier: Modifier = Modifier) {
    val colors = PantrixTheme.colors
    val spacing = PantrixTheme.spacing
    val shape = PantrixTheme.shapes.lg
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background.copy(alpha = COVER_ALPHA))
            .padding(spacing.xl),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = if (layout == WorkspaceLayout.Wide) WideZoneWidth else ZoneWidth)
                .fillMaxWidth()
                .background(lerp(colors.surface, colors.primary.container, ZONE_FILL), shape)
                .dashedEdge(colors.primary.color, shape, EdgeWidth)
                .padding(horizontal = spacing.xxl, vertical = ZoneVerticalPadding)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            PantrixIcon(imageVector = PantrixIcons.Upload, contentDescription = null, size = ZoneIconSize, tint = colors.primary.color)
            PantrixText(
                text = workspaceStrings.dropToOpen,
                style = PantrixTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                color = colors.primary.onContainer,
                textAlign = TextAlign.Center
            )
            PantrixText(
                text = workspaceStrings.dropSupporting(PantrixTheme.strings.fileSize(maxFileSize)),
                style = PantrixTheme.typography.footnote,
                color = colors.primary.onContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * A dashed edge of [width] in [color] along [shape], drawn inside it as a border is: Pantrix draws one round its own drop
 * zone alone, which opens the picker when pressed.
 */
private fun Modifier.dashedEdge(color: Color, shape: Shape, width: Dp): Modifier = drawBehind {
    val stroke = width.toPx()
    val outline = shape.createOutline(size.copy(size.width - stroke, size.height - stroke), layoutDirection, this)
    val dashes = PathEffect.dashPathEffect(floatArrayOf(DashLength.toPx(), DashGap.toPx()))
    translate(left = stroke / 2, top = stroke / 2) {
        drawOutline(outline, color, style = Stroke(width = stroke, pathEffect = dashes))
    }
}
