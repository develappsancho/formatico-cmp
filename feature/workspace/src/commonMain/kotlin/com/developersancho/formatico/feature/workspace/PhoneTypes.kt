package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.navigation.SystemBack
import com.developersancho.formatico.feature.workspace.editor.GeneratedTypesText
import com.developersancho.formatico.feature.workspace.strings.typesStrings
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.container.topbar.PantrixBackButton
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.divider.PantrixHorizontalDivider
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.components.elements.text.TextRole
import com.pantrixui.foundation.theme.PantrixTheme

/**
 * A phone's types, a screen of their own in the app bar's place, as Types-390 draws them: the way back to the document,
 * "Types of order.json" and the copying; the language and the root type's name; what could not be told; the code; and
 * at the bottom its saving. The system's back leaves it as its own button does.
 */
@Composable
internal fun PhoneTypes(
    state: WorkspaceState,
    onIntent: (WorkspaceIntent) -> Unit,
    commands: DocumentCommands,
    modifier: Modifier = Modifier
) {
    val document = state.document ?: return
    val back = { onIntent(WorkspaceIntent.ShowPane(openingPane(document.format))) }
    SystemBack(onBack = back)
    val written = state.types.written
    val name = document.name ?: workspaceStrings.untitled
    Column(modifier = modifier.fillMaxSize().background(PantrixTheme.colors.surface)) {
        TypesBar(name = name, written = written, onBack = back, commands = commands)
        if (written == null) {
            TypesMessage(state = state, modifier = Modifier.weight(1f).fillMaxWidth())
            return@Column
        }
        val spacing = PantrixTheme.spacing
        val field = rememberRootField(document, onIntent)
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                TypesLabel(typesStrings.language)
                LanguageMenu(written.language, ButtonSize.Medium) {
                    onIntent(WorkspaceIntent.Types(document.view.types.copy(language = it)))
                }
            }
            RootField(field, Modifier.fillMaxWidth(), labelled = true)
            TypesAlert(written = written, documentName = name, withLibrary = false, modifier = Modifier.fillMaxWidth())
        }
        PantrixHorizontalDivider()
        GeneratedTypesText(
            text = written.text,
            language = written.language,
            name = written.fileName,
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
        PantrixHorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PantrixTheme.colors.background)
                .height(72.dp)
                .padding(horizontal = PantrixTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SaveTypesButton(written, ButtonSize.Large, commands)
        }
    }
}

/** The bar of a phone's types, where the app's stands: back, "Types of order.json", and the copying. */
@Composable
private fun TypesBar(name: String, written: TypesOutcome.Written?, onBack: () -> Unit, commands: DocumentCommands) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = PantrixTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.xs)
    ) {
        PantrixBackButton(onClick = onBack, tagKey = "back")
        PantrixText(
            text = typesStrings.title(name),
            modifier = Modifier.weight(1f),
            style = PantrixTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
            role = TextRole.Heading(level = 1),
            maxLines = 1
        )
        written?.let { CopyTypesButton(it, ButtonSize.Large, commands) }
    }
    PantrixHorizontalDivider()
}
