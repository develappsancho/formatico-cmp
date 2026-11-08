package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.developersancho.formatico.core.format.json.jsonPath
import com.developersancho.formatico.feature.workspace.strings.schemaStrings
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.container.alert.AlertTone
import com.pantrixui.components.container.alert.PantrixAlert
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.button.PantrixButton
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.components.overlay.menu.PantrixMenu
import com.pantrixui.components.overlay.menu.PantrixMenuItem
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme

/** A label before Schema's choice: "Against", "Validate order.json against". */
@Composable
internal fun SchemaLabel(text: String) {
    PantrixText(
        text = text,
        style = PantrixTheme.typography.label.copy(fontWeight = FontWeight.Medium),
        color = PantrixTheme.colors.textSecondary
    )
}

/** The name of the schema the document is validated against, where one is: chosen, or named by its `$schema`. */
internal val SchemaCheck?.schemaName: String?
    get() = when (this) {
        is SchemaCheck.Failed -> schema
        is SchemaCheck.Valid -> schema
        is SchemaCheck.Unusable -> schema
        else -> null
    }

/**
 * The button that says the schema the document is validated against, and opens the menu of the open schemas: the
 * address its `$schema` names, to open, where none of them has it; and no schema, where one is chosen.
 */
@Composable
internal fun SchemaMenu(state: WorkspaceState, size: ButtonSize, onIntent: (WorkspaceIntent) -> Unit) {
    val document = state.document ?: return
    var open by rememberSaveable { mutableStateOf(false) }
    val strings = schemaStrings
    val chosen = state.schema.schemaName
    val opener = LocalSchemaOpener.current
    val choose = { id: Long? ->
        open = false
        onIntent(WorkspaceIntent.Schema(SchemaChoice(with = id)))
    }
    PantrixMenu(
        visible = open,
        onDismissRequest = { open = false },
        anchor = {
            PantrixButton(
                text = chosen ?: strings.noSchema,
                onClick = { open = true },
                variant = ButtonVariant.Outline,
                size = size,
                trailingIcon = { PantrixIcon(imageVector = PantrixIcons.ChevronDown, contentDescription = null) },
                contentDescription = chosen?.let(strings::chooserLabel) ?: strings.chooseSchemaLabel,
                tagKey = "schema_against"
            )
        },
        tagKey = "schemas"
    ) {
        state.schemas.forEach { schema ->
            PantrixMenuItem(
                text = schema.name ?: workspaceStrings.untitled,
                onClick = { choose(schema.id) },
                selected = schema.id == document.view.schema.with,
                tagKey = "schema_${schema.id}"
            )
        }
        (state.schema as? SchemaCheck.NoSchema)?.named?.let { address ->
            PantrixMenuItem(
                text = strings.openAddress(address),
                onClick = {
                    open = false
                    opener?.openAddress(address) { id -> onIntent(WorkspaceIntent.Schema(SchemaChoice(with = id))) }
                },
                tagKey = "open_schema_address"
            )
        }
        if (document.view.schema.with != null) {
            PantrixMenuItem(text = strings.noSchema, onClick = { choose(null) }, tagKey = "no_schema")
        }
    }
}

/** The button that picks a file and validates the document against it, opened in a tab of its own. */
@Composable
internal fun ChooseSchemaFile(
    size: ButtonSize,
    variant: ButtonVariant,
    onIntent: (WorkspaceIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val opener = LocalSchemaOpener.current
    PantrixButton(
        text = schemaStrings.chooseFile,
        onClick = { opener?.pickFile { id -> onIntent(WorkspaceIntent.Schema(SchemaChoice(with = id))) } },
        modifier = modifier,
        variant = variant,
        size = size,
        leadingIcon = { PantrixIcon(imageVector = PantrixIcons.File, contentDescription = null) },
        tagKey = "choose_schema_file"
    )
}

/**
 * What validating the document came to, for [documentName]: its problems against the schema, that it is valid against
 * it, that none is chosen, or why the one chosen cannot be used.
 */
@Composable
internal fun SchemaAlert(check: SchemaCheck, documentName: String, format: String, modifier: Modifier = Modifier) {
    val strings = schemaStrings
    val (tone, title, body) = when (check) {
        is SchemaCheck.Failed -> Triple(
            AlertTone.Danger,
            strings.failedTitle(check.total, check.total, check.schema),
            strings.failedBody(documentName, format) +
                if (check.total > MOST_LISTED_ISSUES) " " + strings.listedFirst(MOST_LISTED_ISSUES, check.total) else ""
        )

        is SchemaCheck.Valid -> Triple(
            AlertTone.Success,
            strings.validTitle(check.schema),
            strings.validBody(documentName, check.draft.displayName)
        )

        is SchemaCheck.NoSchema -> Triple(
            AlertTone.Info,
            strings.noneTitle,
            check.named?.let { strings.namedBody(documentName, it) } ?: strings.noneBody(documentName)
        )

        is SchemaCheck.Unusable -> Triple(AlertTone.Warning, strings.unusableTitle(check.schema), strings.faultOf(check, documentName))

        else -> return
    }
    PantrixAlert(message = body, modifier = modifier, tone = tone, title = title, announce = false, tagKey = "schema_outcome")
}

/**
 * A problem against the schema: its sign, what is wrong and where, a press on which goes to it ([onGoTo]). The
 * [current] one, which the caret is in, stands on the brand's soft fill.
 */
@Composable
internal fun SchemaIssueRow(issue: SchemaIssue, current: Boolean, onGoTo: () -> Unit) {
    val spacing = PantrixTheme.spacing
    val colors = PantrixTheme.colors
    val strings = schemaStrings
    val path = jsonPath(issue.problem.at)
    val where = if (issue.line != null &&
        issue.column != null
    ) {
        "${workspaceStrings.problemWhere(issue.line, issue.column)} · $path"
    } else {
        path
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (current) colors.primary.container else colors.surface, PantrixTheme.shapes.sm)
            .clickable(role = Role.Button, onClick = onGoTo)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        PantrixIcon(imageVector = PantrixIcons.Error, contentDescription = null, tint = colors.danger.color)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.none)) {
            PantrixText(
                text = strings.sentenceOf(issue.problem),
                style = PantrixTheme.typography.label.copy(fontWeight = FontWeight.Medium),
                color = if (current) colors.primary.onContainer else colors.textPrimary
            )
            PantrixText(
                text = where,
                style = PantrixTheme.typography.footnote.copy(fontWeight = FontWeight.Medium),
                color = if (current) colors.primary.onContainer else colors.textSecondary
            )
        }
    }
}
