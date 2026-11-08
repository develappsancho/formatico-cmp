package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.developersancho.formatico.feature.workspace.editor.ConvertedText
import com.pantrixui.components.container.topbar.PantrixBackButton
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.divider.PantrixHorizontalDivider
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.components.elements.text.TextRole
import com.pantrixui.foundation.theme.PantrixTheme
import formatico.feature.workspace.generated.resources.Res
import formatico.feature.workspace.generated.resources.convert_title
import formatico.feature.workspace.generated.resources.workspace_untitled
import org.jetbrains.compose.resources.stringResource

/**
 * A phone's conversion, a screen of its own in the app bar's place: the way back to the document, its name and the
 * copying of the new document; the format it is converted to, and whether its keys are sorted; what is lost; the new
 * document; and at the bottom its opening in a tab of its own and its saving. The system's back leaves it as its own
 * button does.
 */
@Composable
internal fun PhoneConvert(
    state: WorkspaceState,
    onIntent: (WorkspaceIntent) -> Unit,
    commands: DocumentCommands,
    modifier: Modifier = Modifier
) {
    val document = state.document ?: return
    val back = { onIntent(WorkspaceIntent.ShowPane(openingPane(document.format))) }
    SystemBack(onBack = back)
    val converted = state.conversion.converted
    Column(modifier = modifier.fillMaxSize().background(PantrixTheme.colors.surface)) {
        ConvertBar(
            name = document.name ?: stringResource(Res.string.workspace_untitled),
            converted = converted,
            onBack = back,
            commands = commands
        )
        if (converted == null) {
            ConvertMessage(state = state, modifier = Modifier.weight(1f).fillMaxWidth())
            return@Column
        }
        PhoneChoices(converted, document.view.convert, onIntent)
        PantrixHorizontalDivider()
        ConvertedText(
            text = converted.text,
            format = converted.to,
            name = convertedFileName(converted),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            marked = converted.nullLines
        )
        PantrixHorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PantrixTheme.colors.background)
                .height(72.dp)
                .padding(horizontal = PantrixTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.sm)
        ) {
            OpenConvertedButton(converted, ButtonSize.Large, commands)
            Spacer(modifier = Modifier.weight(1f))
            SaveConvertedButton(converted, ButtonSize.Large, commands, withName = false)
        }
    }
}

/** The bar of a phone's conversion, where the app's stands: back, "Convert order.json", and the copying. */
@Composable
private fun ConvertBar(name: String, converted: ConvertOutcome.Converted?, onBack: () -> Unit, commands: DocumentCommands) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = PantrixTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.xs)
    ) {
        PantrixBackButton(onClick = onBack, tagKey = "back")
        PantrixText(
            text = stringResource(Res.string.convert_title, name),
            modifier = Modifier.weight(1f),
            style = PantrixTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
            role = TextRole.Heading(level = 1),
            maxLines = 1
        )
        converted?.let { CopyConvertedButton(it, ButtonSize.Large, commands) }
    }
    PantrixHorizontalDivider()
}

/** A phone's choices: the format converted to and whether the keys are sorted, what is lost, and what a null becomes. */
@Composable
private fun PhoneChoices(converted: ConvertOutcome.Converted, choice: ConvertChoice, onIntent: (WorkspaceIntent) -> Unit) {
    val spacing = PantrixTheme.spacing
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            ConvertToLabel(from = converted.from, layout = WorkspaceLayout.Phone)
            TargetMenu(converted.from, converted.to, ButtonSize.Medium) { onIntent(WorkspaceIntent.Convert(choice.copy(to = it))) }
            Spacer(modifier = Modifier.weight(1f))
            SortKeysSwitch(choice, onIntent)
        }
        ConvertAlerts(converted = converted, withNotes = false)
        if (converted.choosesNulls) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
                itemVerticalAlignment = Alignment.CenterVertically
            ) { NullChoice(choice, onIntent) }
        }
    }
}
