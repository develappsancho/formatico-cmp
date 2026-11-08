package com.developersancho.formatico.feature.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.Format
import com.pantrixui.components.data.codeblock.PantrixCodeBlock
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.button.PantrixButton
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.components.overlay.dialog.PantrixDialog
import com.pantrixui.components.overlay.select.PantrixSelect
import com.pantrixui.components.overlay.select.SelectOption
import com.pantrixui.foundation.adaptive.isNarrow
import com.pantrixui.foundation.theme.PantrixTheme
import formatico.feature.start.generated.resources.Res
import formatico.feature.start.generated.resources.start_unknown_file
import formatico.feature.start.generated.resources.start_unknown_leave
import formatico.feature.start.generated.resources.start_unknown_open
import formatico.feature.start.generated.resources.start_unknown_pasted
import formatico.feature.start.generated.resources.start_unknown_read_as
import formatico.feature.start.generated.resources.start_unknown_rule
import formatico.feature.start.generated.resources.start_unknown_title
import org.jetbrains.compose.resources.stringResource

/** How many lines of the text the question shows, and how much of each. */
private const val SHOWN_LINES = 4
private const val SHOWN_CHARACTERS = 80

/**
 * What Settings' "Ask me" asks of a [document] whose format could not be told, shown while there is one: the start of
 * its text, the format to read it as, and whether to leave it as text. [onOpen] is told the format chosen, or null for
 * plain text; dismissing the question opens nothing.
 */
@Composable
fun UnknownFormatDialog(document: OpenedDocument?, onOpen: (Format?) -> Unit, onDismiss: () -> Unit) {
    var format by remember(document) { mutableStateOf(document?.let { likelyFormat(it.text) } ?: Format.Json) }
    val phone = PantrixTheme.window.isNarrow
    PantrixDialog(
        visible = document != null,
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.start_unknown_title),
        message =
            document?.name?.let { stringResource(Res.string.start_unknown_file, it) } ?: stringResource(Res.string.start_unknown_pasted),
        actions = {
            PantrixButton(
                text = stringResource(Res.string.start_unknown_leave),
                onClick = { onOpen(null) },
                variant = ButtonVariant.Text,
                tagKey = "leave_as_text"
            )
            PantrixButton(text = stringResource(Res.string.start_unknown_open), onClick = { onOpen(format) }, tagKey = "open_as")
        },
        tagKey = "unknown_format"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.lg)) {
            PantrixCodeBlock(
                code = document?.text?.let(::startOf).orEmpty(),
                modifier = Modifier.fillMaxWidth(),
                copyable = false,
                tagKey = "unknown_text"
            )
            ReadAs(format = format, onFormat = { format = it }, stacked = phone)
            PantrixText(
                text = stringResource(Res.string.start_unknown_rule),
                style = PantrixTheme.typography.footnote.copy(fontWeight = FontWeight.Medium),
                color = PantrixTheme.colors.textSecondary
            )
        }
    }
}

/** "Read it as", and the formats to choose from; [stacked], the choice goes under the words, as a phone has room. */
@Composable
private fun ReadAs(format: Format, onFormat: (Format) -> Unit, stacked: Boolean) {
    val label = stringResource(Res.string.start_unknown_read_as)
    val choice = @Composable { modifier: Modifier ->
        PantrixSelect(
            options = Format.entries.map { SelectOption(it, it.displayName, tagKey = it.name.lowercase()) },
            selected = format,
            onSelect = onFormat,
            modifier = modifier,
            contentDescription = label,
            tagKey = "read_as"
        )
    }
    val words = @Composable { PantrixText(text = label, style = PantrixTheme.typography.label.copy(fontWeight = FontWeight.Medium)) }
    if (stacked) {
        Column(verticalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.sm)) {
            words()
            choice(Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            words()
            choice(Modifier.width(ReadAsWidth))
        }
    }
}

/** How wide the choice of a format is beside its words: the board's 200. */
private val ReadAsWidth = 200.dp

/** The start of [text], as the question shows it: its first lines, each cut short. */
internal fun startOf(text: String): String = text.lineSequence()
    .take(SHOWN_LINES)
    .joinToString("\n") { if (it.length > SHOWN_CHARACTERS) it.take(SHOWN_CHARACTERS - 1) + "…" else it }

/**
 * The format [text] looks most like, as the question chooses it first: YAML where more lines hold `key: value` than
 * `key = value`, properties where fewer do, JSON otherwise.
 */
internal fun likelyFormat(text: String): Format {
    val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith('#') }.toList()
    val colons = lines.count { YamlPair.containsMatchIn(it) }
    val equals = lines.count { PropertiesPair.containsMatchIn(it) }
    return when {
        colons == 0 && equals == 0 -> Format.Json
        colons >= equals -> Format.Yaml
        else -> Format.Properties
    }
}

private val YamlPair = Regex("""^[\w.-]+:\s""")
private val PropertiesPair = Regex("""^[\w.-]+\s*=""")
