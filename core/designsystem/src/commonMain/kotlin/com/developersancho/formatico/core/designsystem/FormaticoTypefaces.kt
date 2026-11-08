package com.developersancho.formatico.core.designsystem

import androidx.compose.ui.text.font.FontWeight
import com.pantrixui.foundation.token.typography.PantrixFont
import com.pantrixui.foundation.token.typography.PantrixTypeface
import formatico.core.designsystem.generated.resources.Res
import formatico.core.designsystem.generated.resources.geist
import formatico.core.designsystem.generated.resources.geist_mono
import org.jetbrains.compose.resources.FontResource

/**
 * Formatico's typefaces, both under the SIL Open Font License (`files/licenses/geist-OFL.txt`): Geist for the
 * interface and Geist Mono for every text the user brings. Each is one variable font, given once for each weight the
 * type scale draws in.
 */
object FormaticoTypefaces {
    private val ScaleWeights = listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)

    /** Geist: the interface. Pantrix's own fallbacks draw the marks of a Mac's keys on the web. */
    val Geist: PantrixTypeface = variable(Res.font.geist)

    /** Geist Mono: the documents, values and code. */
    val GeistMono: PantrixTypeface = variable(Res.font.geist_mono)

    /** Every typeface the first frame waits for on the web. */
    val all: List<PantrixTypeface> = listOf(Geist, GeistMono)

    private fun variable(font: FontResource): PantrixTypeface = PantrixTypeface(
        fonts = ScaleWeights.map { weight -> PantrixFont(font, weight) },
        fallbacks = PantrixTypeface.Default.fallbacks
    )
}
