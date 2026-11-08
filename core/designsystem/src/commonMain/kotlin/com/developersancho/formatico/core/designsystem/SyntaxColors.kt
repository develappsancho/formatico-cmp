package com.developersancho.formatico.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The colours a document's text is drawn in, by what each part of it is. Each reads at 4.5:1 or more on the
 * theme's surface, and the kinds differ in lightness as well as in hue.
 *
 * @property key an object's key.
 * @property string a string value.
 * @property number a number.
 * @property literal `true`, `false` and `null`.
 * @property punctuation braces, brackets, commas and colons.
 * @property comment a comment.
 * @property currentLine the ground of the line the caret is on.
 */
@Immutable
data class SyntaxColors(
    val key: Color,
    val string: Color,
    val number: Color,
    val literal: Color,
    val punctuation: Color,
    val comment: Color,
    val currentLine: Color
) {
    companion object {
        /** The colours on a light theme. */
        val Light: SyntaxColors = SyntaxColors(
            key = Color(0xFF115E59),
            string = Color(0xFFB45309),
            number = Color(0xFF7C3AED),
            literal = Color(0xFFBE123C),
            punctuation = Color(0xFF475569),
            comment = Color(0xFF64748B),
            currentLine = Color(0xFFF0FDFA)
        )

        /** The colours on a dark theme. */
        val Dark: SyntaxColors = SyntaxColors(
            key = Color(0xFF5EEAD4),
            string = Color(0xFFFCD34D),
            number = Color(0xFFC4B5FD),
            literal = Color(0xFFFDA4AF),
            punctuation = Color(0xFFCBD5E1),
            comment = Color(0xFF94A3B8),
            currentLine = Color(0xFF13262B)
        )
    }
}

/** The syntax colours in effect, which [FormaticoTheme] provides. */
val LocalSyntaxColors = staticCompositionLocalOf { SyntaxColors.Light }
