package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.developersancho.formatico.core.share.ShareOutcome
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.button.PantrixButton
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.switchbutton.PantrixSwitchButton
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.components.inputs.textfield.PantrixTextField
import com.pantrixui.components.overlay.dialog.PantrixDialog
import com.pantrixui.components.overlay.sheet.PantrixSheet
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme
import formatico.feature.workspace.generated.resources.Res
import formatico.feature.workspace.generated.resources.share_body
import formatico.feature.workspace.generated.resources.share_body_phone
import formatico.feature.workspace.generated.resources.share_cancel
import formatico.feature.workspace.generated.resources.share_copy
import formatico.feature.workspace.generated.resources.share_fits_long
import formatico.feature.workspace.generated.resources.share_fits_short
import formatico.feature.workspace.generated.resources.share_fits_short_phone
import formatico.feature.workspace.generated.resources.share_formatted
import formatico.feature.workspace.generated.resources.share_link
import formatico.feature.workspace.generated.resources.share_plain_text
import formatico.feature.workspace.generated.resources.share_send
import formatico.feature.workspace.generated.resources.share_title
import formatico.feature.workspace.generated.resources.share_too_large_body
import formatico.feature.workspace.generated.resources.share_too_large_title
import formatico.feature.workspace.generated.resources.workspace_save_file
import formatico.feature.workspace.generated.resources.workspace_untitled
import org.jetbrains.compose.resources.stringResource

/**
 * The document's share while [state] holds one, as the Share boards draw it: its link, how long it is, and whether the
 * link carries the document formatted, in a dialog; on a phone in a sheet, with the platform's own way of sending it
 * to another app where it has one ([sending]). A document too large for a link is told so, and saved as a file instead.
 */
@Composable
internal fun ShareDocument(
    state: WorkspaceState,
    layout: WorkspaceLayout,
    onIntent: (WorkspaceIntent) -> Unit,
    commands: DocumentCommands,
    sending: ((String) -> Unit)? = rememberTextSharing()
) {
    // What was shown stays while the dialog leaves.
    var shown by remember { mutableStateOf<DocumentShare?>(null) }
    state.share?.let { shown = it }
    val share = shown ?: return
    val close = { onIntent(WorkspaceIntent.Share(open = false)) }
    val actions = ShareActions(
        close = close,
        copy = { link ->
            commands.copyText(Copied.Link, link)
            close()
        },
        save = {
            state.document?.let(commands.save)
            close()
        },
        send = sending?.let { send ->
            { link: String ->
                send(link)
                close()
            }
        },
        format = { onIntent(WorkspaceIntent.ShareFormatted(it)) }
    )
    if (layout == WorkspaceLayout.Phone) {
        ShareSheet(visible = state.share != null, share = share, actions = actions)
    } else {
        ShareDialog(visible = state.share != null, share = share, actions = actions)
    }
}

/** What the share's buttons do: close it, copy or send the link, save the document as a file, format it in the link. */
internal class ShareActions(
    val close: () -> Unit,
    val copy: (String) -> Unit,
    val save: () -> Unit,
    val send: ((String) -> Unit)?,
    val format: (Boolean) -> Unit
)

/** The share's name for the document: its own, or the untitled one's. */
@Composable
private fun DocumentShare.title(): String = name ?: stringResource(Res.string.workspace_untitled)

/** The share in a dialog, in a wide window and on a tablet: Share-1440, and Share-834 for a document too large. */
@Composable
private fun ShareDialog(visible: Boolean, share: DocumentShare, actions: ShareActions) {
    val tooLarge = share.outcome as? ShareOutcome.TooLarge
    val link = share.outcome as? ShareOutcome.Link
    PantrixDialog(
        visible = visible,
        onDismissRequest = actions.close,
        title = stringResource(if (tooLarge != null) Res.string.share_too_large_title else Res.string.share_title, share.title()),
        message = if (tooLarge != null) tooLargeBody(tooLarge) else stringResource(Res.string.share_body),
        icon = if (tooLarge != null) PantrixIcons.Warning else null,
        actions = {
            PantrixButton(
                text = stringResource(Res.string.share_cancel),
                onClick = actions.close,
                variant = ButtonVariant.Text,
                tagKey = "cancel"
            )
            if (tooLarge != null) SaveButton(actions) else CopyButton(link, actions)
        },
        tagKey = "share"
    ) {
        if (tooLarge == null) {
            Column(verticalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.lg)) {
                LinkField(share, link, phone = false)
                FormattedSwitch(share, actions)
            }
        }
    }
}

/** The share in a sheet, on a phone: Share-390, with the platform's own way of sending the link where it has one. */
@Composable
private fun ShareSheet(visible: Boolean, share: DocumentShare, actions: ShareActions) {
    val tooLarge = share.outcome as? ShareOutcome.TooLarge
    val link = share.outcome as? ShareOutcome.Link
    PantrixSheet(
        visible = visible,
        onDismissRequest = actions.close,
        title = stringResource(if (tooLarge != null) Res.string.share_too_large_title else Res.string.share_title, share.title()),
        tagKey = "share"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = PantrixTheme.spacing.lg).padding(bottom = PantrixTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.lg)
        ) {
            PantrixText(
                text = if (tooLarge != null) tooLargeBody(tooLarge) else stringResource(Res.string.share_body_phone),
                style = PantrixTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                color = PantrixTheme.colors.textSecondary
            )
            if (tooLarge == null) {
                LinkField(share, link, phone = true)
                FormattedSwitch(share, actions)
            }
            Column(verticalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.sm)) {
                if (tooLarge != null) {
                    SaveButton(actions, Modifier.fillMaxWidth(), ButtonSize.Large)
                } else {
                    CopyButton(link, actions, Modifier.fillMaxWidth(), ButtonSize.Large)
                    actions.send?.let { send ->
                        PantrixButton(
                            text = stringResource(Res.string.share_send),
                            onClick = { link?.let { send(it.link) } },
                            modifier = Modifier.fillMaxWidth(),
                            variant = ButtonVariant.Outline,
                            size = ButtonSize.Large,
                            enabled = link != null,
                            tagKey = "send"
                        )
                    }
                }
            }
        }
    }
}

/** Why the document has no link: how large it is beside what a link holds. */
@Composable
private fun tooLargeBody(tooLarge: ShareOutcome.TooLarge): String =
    stringResource(Res.string.share_too_large_body, PantrixTheme.strings.fileSize(tooLarge.bytes.toLong()))

/**
 * The link, to read and select, and under it how much of the document it carries in how many characters, and whether
 * every app takes it whole. Empty while the first link is being made.
 */
@Composable
private fun LinkField(share: DocumentShare, link: ShareOutcome.Link?, phone: Boolean) {
    val strings = PantrixTheme.strings
    val fits = link?.let {
        val words = when {
            !it.short -> Res.string.share_fits_long
            phone -> Res.string.share_fits_short_phone
            else -> Res.string.share_fits_short
        }
        stringResource(
            words,
            strings.fileSize(it.bytes.toLong()),
            share.format?.displayName ?: stringResource(Res.string.share_plain_text),
            strings.decimal(it.characters.toDouble())
        )
    }
    PantrixTextField(
        value = link?.link.orEmpty(),
        onValueChange = {},
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        label = stringResource(Res.string.share_link),
        supportingText = fits,
        tagKey = "link"
    )
}

/** Whether the link carries the document as Format writes it: not for plain text, nor a document with errors. */
@Composable
private fun FormattedSwitch(share: DocumentShare, actions: ShareActions) {
    PantrixSwitchButton(
        checked = share.formatted && share.canFormat,
        onCheckedChange = actions.format,
        label = stringResource(Res.string.share_formatted),
        enabled = share.canFormat,
        tagKey = "formatted"
    )
}

@Composable
private fun CopyButton(
    link: ShareOutcome.Link?,
    actions: ShareActions,
    modifier: Modifier = Modifier,
    size: ButtonSize = ButtonSize.Medium
) {
    PantrixButton(
        text = stringResource(Res.string.share_copy),
        onClick = { link?.let { actions.copy(it.link) } },
        modifier = modifier,
        size = size,
        enabled = link != null,
        leadingIcon = { PantrixIcon(imageVector = PantrixIcons.Copy, contentDescription = null) },
        tagKey = "copy_link"
    )
}

@Composable
private fun SaveButton(actions: ShareActions, modifier: Modifier = Modifier, size: ButtonSize = ButtonSize.Medium) {
    PantrixButton(
        text = stringResource(Res.string.workspace_save_file),
        onClick = actions.save,
        modifier = modifier,
        size = size,
        leadingIcon = { PantrixIcon(imageVector = PantrixIcons.Download, contentDescription = null) },
        tagKey = "save"
    )
}
