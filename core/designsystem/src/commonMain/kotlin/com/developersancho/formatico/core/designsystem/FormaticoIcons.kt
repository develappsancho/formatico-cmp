package com.developersancho.formatico.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Formatico's own icons, for what `PantrixIcons` has none of (PANTRIX_FINDINGS, "Simgeler kapalı bir liste; bir
 * uygulamanın sık simgeleri yok"): drawn on its 24-unit grid with its 2-unit round stroke, in black, which
 * `PantrixIcon` tints. Their paths are the design canvas's.
 */
object FormaticoIcons {
    /** Settings: two sliders, each with its knob. */
    val Settings: ImageVector by lazy {
        icon(
            "Settings",
            "M4 7h9M17 7h3M4 17h3M11 17h9",
            "M13 7a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
            "M7 17a2 2 0 1 0 4 0a2 2 0 1 0 -4 0"
        )
    }

    /** The dark theme: a crescent moon. */
    val Moon: ImageVector by lazy { icon("Moon", "M20 14.5A8 8 0 0 1 9.5 4 8 8 0 1 0 20 14.5z") }

    /** The light theme: a sun and its rays. */
    val Sun: ImageVector by lazy {
        icon(
            "Sun",
            "M8 12a4 4 0 1 0 8 0a4 4 0 1 0 -8 0",
            "M12 2v2M12 20v2M2 12h2M20 12h2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M4.9 19.1l1.4 -1.4M17.7 6.3l1.4 -1.4"
        )
    }

    private fun icon(name: String, vararg paths: String): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = GRID.dp,
        defaultHeight = GRID.dp,
        viewportWidth = GRID,
        viewportHeight = GRID
    ).apply {
        paths.forEach { path ->
            addPath(
                pathData = addPathNodes(path),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
    }.build()

    private const val GRID = 24f
    private const val STROKE = 2f
}
