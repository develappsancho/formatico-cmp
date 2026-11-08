package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.developersancho.formatico.core.share.ShareKeep
import com.developersancho.formatico.core.share.ShareOutcome
import com.developersancho.formatico.feature.workspace.strings.shareStrings
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.button.PantrixButton
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.components.overlay.dialog.PantrixDialog
import com.pantrixui.components.overlay.sheet.PantrixSheet
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.platform.ShareResult
import com.pantrixui.foundation.platform.rememberShareSheet
import com.pantrixui.foundation.theme.PantrixTheme

/**
 * The document's share while [state] holds one, as the Share boards draw it: in the link itself, or as an encrypted
 * link Formatico's server keeps, in a dialog; on a phone in a sheet, with the platform's own sheet for sending the link
 * to another app where it has one ([rememberShareSheet]). A document a link cannot hold goes as an encrypted link, and
 * one too large for that as well is saved as a file instead.
 */
@Composable
internal fun ShareDocument(
    state: WorkspaceState,
    layout: WorkspaceLayout,
    onIntent: (WorkspaceIntent) -> Unit,
    commands: DocumentCommands
) {
    // What was shown stays while the dialog leaves.
    var shown by remember { mutableStateOf<DocumentShare?>(null) }
    state.share?.let { shown = it }
    val share = shown ?: return
    val sheet = rememberShareSheet()
    val title = share.title()
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
        // The share stays where the reader closed the platform's sheet without sending, or it could not be shown.
        send = sheet?.let { platform ->
            { link: String ->
                platform.share(link, title) { result -> if (result == ShareResult.Shared || result == ShareResult.Started) close() }
            }
        },
        intent = onIntent
    )
    if (layout == WorkspaceLayout.Phone) {
        ShareSheet(visible = state.share != null, share = share, actions = actions)
    } else {
        ShareDialog(visible = state.share != null, share = share, actions = actions)
    }
}

/**
 * What the share's buttons do: close it, copy or send the link, save the document as a file; the rest are asked of the
 * workspace by [intent]: format it in the link, choose how it is shared and for how long, make its encrypted link and
 * delete it.
 */
internal class ShareActions(
    val close: () -> Unit,
    val copy: (String) -> Unit,
    val save: () -> Unit,
    val send: ((String) -> Unit)?,
    private val intent: (WorkspaceIntent) -> Unit = {}
) {
    val format: (Boolean) -> Unit = { intent(WorkspaceIntent.ShareFormatted(it)) }
    val way: (ShareWay) -> Unit = { intent(WorkspaceIntent.ShareBy(it)) }
    val keep: (ShareKeep) -> Unit = { intent(WorkspaceIntent.ShareKept(it)) }
    val create: () -> Unit = { intent(WorkspaceIntent.CreateEncryptedLink) }
    val delete: () -> Unit = { intent(WorkspaceIntent.DeleteEncryptedLink) }
}

/** The share's name for the document: its own, or the untitled one's. */
@Composable
private fun DocumentShare.title(): String = name ?: workspaceStrings.untitled

/** The share's title: the document's name, or that it is too large to share at all. */
@Composable
private fun DocumentShare.heading(): String = if (tooLarge) shareStrings.tooLargeTitle(title()) else shareStrings.title(title())

/** What the share says before its choices: why the document goes as an encrypted link, or as a file. */
@Composable
private fun DocumentShare.preface(): String? {
    val bytes = (outcome as? ShareOutcome.TooLarge)?.bytes ?: return null
    val size = PantrixTheme.strings.fileSize(bytes.toLong())
    return if (tooLarge) shareStrings.tooLargeBody(size) else shareStrings.encryptedOnlyBody(title(), size)
}

/** The share in a dialog, in a wide window and on a tablet: Share-1440 and Share-834. */
@Composable
private fun ShareDialog(visible: Boolean, share: DocumentShare, actions: ShareActions) {
    val link = share.outcome as? ShareOutcome.Link
    PantrixDialog(
        visible = visible,
        onDismissRequest = actions.close,
        title = share.heading(),
        message = share.preface(),
        icon = if (share.tooLarge) PantrixIcons.Warning else null,
        actions = { DialogActions(share, link, actions) },
        tagKey = "share"
    ) {
        if (!share.tooLarge) {
            Column(verticalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.lg)) {
                ShareChoices(share, link, actions, phone = false)
            }
        }
    }
}

/** The dialog's buttons, as each state of the boards ends. */
@Composable
private fun RowScope.DialogActions(share: DocumentShare, link: ShareOutcome.Link?, actions: ShareActions) {
    val encrypted = share.encrypted
    when {
        share.tooLarge -> {
            CancelButton(actions)
            SaveButton(actions)
        }

        share.shownWay == ShareWay.Link -> {
            CancelButton(actions)
            CopyButton(link?.link, actions)
        }

        encrypted is EncryptedShare.Created -> {
            DeleteButton(actions)
            Spacer(Modifier.weight(1f))
            PantrixButton(text = shareStrings.done, onClick = actions.close, variant = ButtonVariant.Text, tagKey = "done")
            CopyButton(encrypted.link.link, actions)
        }

        else -> {
            CancelButton(actions)
            CreateButton(encrypted, actions)
        }
    }
}

/** The share in a sheet, on a phone: Share-390, with the platform's own way of sending the link where it has one. */
@Composable
private fun ShareSheet(visible: Boolean, share: DocumentShare, actions: ShareActions) {
    val link = share.outcome as? ShareOutcome.Link
    val encrypted = share.encrypted
    PantrixSheet(visible = visible, onDismissRequest = actions.close, title = share.heading(), tagKey = "share") {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = PantrixTheme.spacing.lg).padding(bottom = PantrixTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.lg)
        ) {
            share.preface()?.let {
                PantrixText(
                    text = it,
                    style = PantrixTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                    color = PantrixTheme.colors.textSecondary
                )
            }
            if (!share.tooLarge) ShareChoices(share, link, actions, phone = true)
            Column(verticalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.sm)) {
                val wide = Modifier.fillMaxWidth()
                when {
                    share.tooLarge -> SaveButton(actions, wide, ButtonSize.Large)

                    share.shownWay == ShareWay.Link -> {
                        CopyButton(link?.link, actions, wide, ButtonSize.Large)
                        SendButton(link?.link, actions)
                    }

                    encrypted is EncryptedShare.Created -> {
                        CopyButton(encrypted.link.link, actions, wide, ButtonSize.Large)
                        SendButton(encrypted.link.link, actions)
                        DeleteButton(actions, wide, ButtonSize.Large)
                    }

                    else -> CreateButton(encrypted, actions, wide, ButtonSize.Large)
                }
            }
        }
    }
}
