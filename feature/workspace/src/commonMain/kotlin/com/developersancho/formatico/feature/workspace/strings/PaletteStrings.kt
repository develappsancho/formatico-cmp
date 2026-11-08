package com.developersancho.formatico.feature.workspace.strings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.developersancho.formatico.core.designsystem.inLanguage
import com.developersancho.formatico.core.designsystem.inTheLanguage

/**
 * The words of the document's commands in the palette.
 *
 * In English, [PaletteStringsEnglish], and in Turkish, [PaletteStringsTurkish]. A sentence that holds values is a
 * function of them, in their order; one whose form follows a number takes it first, as `count`.
 */
internal interface PaletteStrings {
    val groupDocument: String
    val groupView: String
    val groupConvert: String
    val format: String
    val formatKeywords: String
    val minifyKeywords: String
    val sortKeywords: String
    val compare: String
    val compareKeywords: String
    val tree: String
    val table: String
    val queryJsonpath: String
    val queryXpath: String
    val queryKeywords: String

    /** "Convert to {first}" */
    fun convertTo(first: String): String
}

/** [PaletteStrings] in the theme's language. */
internal val paletteStrings: PaletteStrings
    @Composable
    @ReadOnlyComposable
    get() = inTheLanguage(PaletteStringsEnglish, PaletteStringsTurkish)

/** [PaletteStrings] in the language whose tag is [language], away from a screen. */
internal fun paletteStringsIn(language: String): PaletteStrings = inLanguage(language, PaletteStringsEnglish, PaletteStringsTurkish)
