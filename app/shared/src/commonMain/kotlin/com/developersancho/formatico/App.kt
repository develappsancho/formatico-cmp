package com.developersancho.formatico

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import com.developersancho.formatico.core.designsystem.FormaticoTheme
import com.developersancho.formatico.core.designsystem.SyntaxColors
import com.pantrixui.components.container.surface.PantrixSurface
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.components.elements.text.TextRole
import com.pantrixui.foundation.theme.PantrixTheme
import formatico.app.shared.generated.resources.Res
import formatico.app.shared.generated.resources.app_name
import formatico.app.shared.generated.resources.app_tagline
import org.jetbrains.compose.resources.stringResource

/**
 * Formatico, on every platform.
 *
 * The screens are drawn on the design canvas before they are written; until the first of them, the app shows its
 * name in its theme, its typefaces and its syntax colours.
 */
@Composable
fun App() {
    FormaticoTheme {
        PantrixSurface(
            modifier = Modifier.fillMaxSize(),
            color = PantrixTheme.colors.background,
            shape = PantrixTheme.shapes.none
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeContentPadding()
                    .padding(PantrixTheme.spacing.xl),
                verticalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.md, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PantrixText(
                    text = stringResource(Res.string.app_name),
                    style = PantrixTheme.typography.display,
                    role = TextRole.Heading(level = 1)
                )
                PantrixText(
                    text = stringResource(Res.string.app_tagline),
                    style = PantrixTheme.typography.callout,
                    color = PantrixTheme.colors.textSecondary,
                    textAlign = TextAlign.Center
                )
                PantrixText(
                    text = sampleDocument(FormaticoTheme.syntax),
                    style = PantrixTheme.typography.code
                )
            }
        }
    }
}

/** A line of JSON in the syntax colours: `{"format": "json", "valid": true}`. */
private fun sampleDocument(syntax: SyntaxColors): AnnotatedString = buildAnnotatedString {
    fun part(text: String, color: Color) = withStyle(SpanStyle(color = color)) {
        append(text)
    }
    part("{", syntax.punctuation)
    part("\"format\"", syntax.key)
    part(": ", syntax.punctuation)
    part("\"json\"", syntax.string)
    part(", ", syntax.punctuation)
    part("\"valid\"", syntax.key)
    part(": ", syntax.punctuation)
    part("true", syntax.literal)
    part("}", syntax.punctuation)
}
