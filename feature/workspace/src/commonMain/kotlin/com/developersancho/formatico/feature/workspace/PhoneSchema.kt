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
import com.developersancho.formatico.feature.workspace.strings.schemaStrings
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.container.topbar.PantrixBackButton
import com.pantrixui.components.data.codeeditor.CodeEditorState
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.divider.PantrixHorizontalDivider
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.components.elements.text.TextRole
import com.pantrixui.foundation.theme.PantrixTheme

/**
 * A phone's schema, a screen of its own in the app bar's place, as Schema-390 draws it: the way back to the document
 * and "Validate order.json"; the schema chosen and what validating came to; the problems, a press on one showing the
 * text at its place; and at the bottom the file to choose a schema from. The system's back leaves it as its own button
 * does.
 */
@Composable
internal fun PhoneSchema(
    state: WorkspaceState,
    editor: CodeEditorState,
    onIntent: (WorkspaceIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val document = state.document ?: return
    val back = { onIntent(WorkspaceIntent.ShowPane(openingPane(document.format))) }
    SystemBack(onBack = back)
    val name = document.name ?: workspaceStrings.untitled
    val spacing = PantrixTheme.spacing
    val check = state.schema
    Column(modifier = modifier.fillMaxSize().background(PantrixTheme.colors.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            PantrixBackButton(onClick = back, tagKey = "back")
            PantrixText(
                text = schemaStrings.title(name),
                modifier = Modifier.weight(1f),
                style = PantrixTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                role = TextRole.Heading(level = 1),
                maxLines = 1
            )
        }
        PantrixHorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = spacing.lg, end = spacing.lg, top = spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            SchemaLabel(schemaStrings.against)
            SchemaMenu(state, ButtonSize.Large, onIntent)
        }
        if (check == null || !check.isShownInThePane) {
            SchemaMessage(state, Modifier.weight(1f).fillMaxWidth())
        } else {
            SchemaIssues(state, check, editor, WorkspaceLayout.Phone, onIntent, Modifier.weight(1f).padding(horizontal = spacing.sm))
        }
        PantrixHorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PantrixTheme.colors.background)
                .height(72.dp)
                .padding(horizontal = spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChooseSchemaFile(ButtonSize.Large, ButtonVariant.Outline, onIntent)
        }
    }
}
