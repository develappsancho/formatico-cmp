package com.developersancho.formatico.feature.start

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import com.pantrixui.components.elements.keycaps.KeycapsColors
import com.pantrixui.components.elements.keycaps.KeycapsSize
import com.pantrixui.components.elements.keycaps.PantrixKeycaps
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.foundation.keyboard.PantrixShortcut
import com.pantrixui.foundation.platform.LocalOperatingSystem
import com.pantrixui.foundation.theme.PantrixTheme
import formatico.feature.start.generated.resources.Res
import formatico.feature.start.generated.resources.start_paste_hint
import org.jetbrains.compose.resources.stringResource

/** Where the hint's words are split: what stands for its keys, in a language that puts them anywhere. */
private const val KEYS = ""

/**
 * The hint beside Paste, "or press ⌘V anywhere on this screen": the [shortcut]'s keys on small keycaps, on the line of
 * the words around them wherever the language puts them. A screen reader hears the sentence as one, the keys written
 * as the system writes them.
 */
@Composable
internal fun PasteHint(shortcut: PantrixShortcut, modifier: Modifier = Modifier) {
    val sentence = stringResource(Res.string.start_paste_hint, shortcut.writtenOn(LocalOperatingSystem.current))
    val words = stringResource(Res.string.start_paste_hint, KEYS).split(KEYS, limit = 2)
    val color = PantrixTheme.colors.textSecondary
    Row(modifier = modifier.clearAndSetSemantics { text = AnnotatedString(sentence) }) {
        PantrixText(text = words.first().trimEnd(), modifier = Modifier.alignByBaseline(), color = color)
        PantrixKeycaps(
            shortcut = shortcut,
            modifier = Modifier.alignByBaseline().padding(horizontal = PantrixTheme.spacing.xs),
            size = KeycapsSize.Small,
            colors = KeycapsColors(container = PantrixTheme.colors.surface)
        )
        PantrixText(text = words.getOrElse(1) { "" }.trimStart(), modifier = Modifier.alignByBaseline(), color = color)
    }
}
