package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.convert.Loss
import com.pantrixui.components.container.alert.AlertTone
import com.pantrixui.components.container.alert.PantrixAlert
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme
import formatico.feature.workspace.generated.resources.Res
import formatico.feature.workspace.generated.resources.convert_invalid
import formatico.feature.workspace.generated.resources.convert_nothing_lost
import formatico.feature.workspace.generated.resources.workspace_status_problems
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Whether [converted] offers the choice of what a null becomes: its format has none, and the document has one. */
internal val ConvertOutcome.Converted.choosesNulls: Boolean get() = to.choosesNulls && losses.any { it is Loss.Nulls }

/**
 * What [converted] lost, an alert for each kind, a warning; or, where it lost nothing, that it converts back as it is,
 * with what was done for it where [withNotes] says so. Many alerts scroll in a room of their own.
 */
@Composable
internal fun ConvertAlerts(converted: ConvertOutcome.Converted, withNotes: Boolean, modifier: Modifier = Modifier) {
    if (converted.losses.isEmpty()) {
        PantrixAlert(
            message = nothingLostMessage(converted, withNotes),
            modifier = modifier,
            tone = AlertTone.Success,
            title = stringResource(Res.string.convert_nothing_lost),
            tagKey = "nothing_lost"
        )
        return
    }
    Column(
        modifier = modifier.heightIn(max = AlertsHeight).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.sm)
    ) {
        converted.losses.forEach { loss ->
            val words = lossWords(loss, converted.from, converted.to)
            PantrixAlert(message = words.message, tone = AlertTone.Warning, title = words.title, tagKey = "lost_${loss.key}")
        }
    }
}

/** How tall the alerts stand before they scroll: room for about three. */
private val AlertsHeight = 280.dp

/** Why there is nothing converted: the tree's reason, or the document's errors, which are repaired first. */
@Composable
internal fun ConvertMessage(state: WorkspaceState, modifier: Modifier = Modifier) {
    val invalid = state.conversion as? ConvertOutcome.Invalid
    if (invalid == null) {
        TreePaneMessage(state = state, modifier = modifier)
        return
    }
    PaneMessage(
        icon = PantrixIcons.Warning,
        title = pluralStringResource(Res.plurals.workspace_status_problems, invalid.errors, invalid.errors),
        body = pluralStringResource(Res.plurals.convert_invalid, invalid.errors, invalid.format.displayName, invalid.errors),
        modifier = modifier
    )
}
