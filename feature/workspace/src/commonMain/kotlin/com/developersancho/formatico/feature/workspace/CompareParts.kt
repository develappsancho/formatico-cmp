package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import com.developersancho.formatico.core.format.diff.Change
import com.developersancho.formatico.feature.workspace.strings.compareStrings
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.data.tag.PantrixTag
import com.pantrixui.components.data.tag.TagVariant
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.button.PantrixButton
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.iconbutton.PantrixIconButton
import com.pantrixui.components.elements.segmentbutton.PantrixSegmentButton
import com.pantrixui.components.elements.segmentbutton.SegmentOption
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.components.overlay.menu.PantrixMenu
import com.pantrixui.components.overlay.menu.PantrixMenuItem
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme

/** The letter a document compared is called by, A or B, on a tag of its own. */
@Composable
internal fun SideTag(side: Side) {
    PantrixTag(
        text = if (side == Side.A) compareStrings.a else compareStrings.b,
        variant = TagVariant.Solid,
        tagKey = if (side == Side.A) "side_a" else "side_b"
    )
}

/** A, the document compared, by its name: B's letter and its menu come after it. */
@Composable
internal fun SideName(name: String) {
    PantrixText(text = name, style = PantrixTheme.typography.label.copy(fontWeight = FontWeight.Medium), maxLines = 1)
}

/**
 * The button that says which document A is compared with, [partner], and opens the menu of the [others] open; choosing
 * one compares A with it.
 */
@Composable
internal fun PartnerMenu(
    partner: OtherDocument?,
    others: List<OtherDocument>,
    choice: CompareChoice,
    size: ButtonSize,
    onIntent: (WorkspaceIntent) -> Unit
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val untitled = workspaceStrings.untitled
    val name = partner?.let { it.name ?: untitled }.orEmpty()
    PantrixMenu(
        visible = open,
        onDismissRequest = { open = false },
        anchor = {
            PantrixButton(
                text = name,
                onClick = { open = true },
                variant = ButtonVariant.Outline,
                size = size,
                trailingIcon = { PantrixIcon(imageVector = PantrixIcons.ChevronDown, contentDescription = null) },
                contentDescription = compareStrings.bMenu(name),
                tagKey = "partner"
            )
        },
        tagKey = "partners"
    ) {
        others.forEach { other ->
            PantrixMenuItem(
                text = other.name ?: untitled,
                onClick = {
                    open = false
                    onIntent(WorkspaceIntent.Compare(choice.copy(with = other.id, current = 0)))
                },
                selected = other.id == partner?.id,
                tagKey = "document_${other.id}"
            )
        }
    }
}

/** What the documents are compared by: their values' meaning, or their text. */
@Composable
internal fun CompareByChoice(choice: CompareChoice, onIntent: (WorkspaceIntent) -> Unit) {
    PantrixSegmentButton(
        options = listOf(
            SegmentOption(value = CompareBy.Meaning, label = compareStrings.byMeaning, tagKey = "meaning"),
            SegmentOption(value = CompareBy.Text, label = compareStrings.byText, tagKey = "text")
        ),
        selected = choice.by,
        onSelect = { onIntent(WorkspaceIntent.Compare(choice.copy(by = it, current = 0))) },
        fullWidth = false,
        tagKey = "compare_by"
    )
}

/** "Compare by", before the choice of a wide window. */
@Composable
internal fun CompareByLabel() {
    PantrixText(
        text = compareStrings.by,
        style = PantrixTheme.typography.label.copy(fontWeight = FontWeight.Medium),
        color = PantrixTheme.colors.textSecondary
    )
}

/** How many values or runs of lines changed, were added and were removed, a tag for each there is. */
@Composable
internal fun ChangeTags(compared: CompareOutcome.Compared?) {
    val differences = compared?.differences ?: return
    Row(horizontalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        Change.entries.forEach { change ->
            val count = differences.count { it.change == change }
            if (count == 0) return@forEach
            val words = when (change) {
                Change.Changed -> compareStrings::changed
                Change.Added -> compareStrings::added
                Change.Removed -> compareStrings::removed
            }
            PantrixTag(text = words(count, count), tone = change.tagTone, tagKey = "count_${change.name.lowercase()}")
        }
    }
}

/** The button that stops comparing, and gives back the view the document opens with. */
@Composable
internal fun StopComparingButton(document: OpenDocument, size: ButtonSize, onIntent: (WorkspaceIntent) -> Unit) {
    PantrixIconButton(
        imageVector = PantrixIcons.Close,
        contentDescription = compareStrings.stop,
        onClick = { onIntent(WorkspaceIntent.ShowPane(openingPane(document.format))) },
        size = size,
        tagKey = "stop_comparing"
    )
}
