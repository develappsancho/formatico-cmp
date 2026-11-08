package com.developersancho.formatico.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.pantrixui.foundation.theme.preset.DesignPreset
import com.pantrixui.foundation.token.color.Dark
import com.pantrixui.foundation.token.color.Light
import com.pantrixui.foundation.token.color.PantrixColor
import com.pantrixui.foundation.token.color.PantrixColors
import com.pantrixui.foundation.token.shape.PantrixShapes
import com.pantrixui.foundation.token.shape.rememberPantrixShapes
import com.pantrixui.foundation.token.typography.PantrixTypography
import com.pantrixui.foundation.token.typography.rememberPantrixTypography

// The brand role, Teal (identity A on the design canvas). The rest of the theme is Pantrix's own.
private val TealLight = Color(0xFF0F766E)
private val OnTealLight = Color(0xFFFFFFFF)
private val TealContainerLight = Color(0xFFCCFBF1)
private val OnTealContainerLight = Color(0xFF134E4A)
private val FocusRingLight = Color(0xFF0D9488)

private val TealDark = Color(0xFF2DD4BF)
private val OnTealDark = Color(0xFF042F2E)
private val TealContainerDark = Color(0xFF134E4A)
private val OnTealContainerDark = Color(0xFFCCFBF1)
private val FocusRingDark = Color(0xFF2DD4BF)

private val FormaticoLight: PantrixColors = PantrixColors.Light.copy(
    primary = PantrixColor(
        color = TealLight,
        onColor = OnTealLight,
        container = TealContainerLight,
        onContainer = OnTealContainerLight
    ),
    focusRing = FocusRingLight
)

private val FormaticoDark: PantrixColors = PantrixColors.Dark.copy(
    primary = PantrixColor(
        color = TealDark,
        onColor = OnTealDark,
        container = TealContainerDark,
        onContainer = OnTealContainerDark
    ),
    focusRing = FocusRingDark
)

/**
 * Formatico's look as a Pantrix [DesignPreset]: the Teal brand role over Pantrix's slate, Geist for the interface
 * and Geist Mono for code, and Pantrix's shapes.
 */
object FormaticoPreset : DesignPreset {
    override val name: String = "Formatico"

    override fun colors(dark: Boolean): PantrixColors = if (dark) FormaticoDark else FormaticoLight

    @Composable
    override fun typography(): PantrixTypography =
        rememberPantrixTypography(FormaticoTypefaces.Geist, codeTypeface = FormaticoTypefaces.GeistMono)

    @Composable
    override fun shapes(): PantrixShapes = rememberPantrixShapes()
}
