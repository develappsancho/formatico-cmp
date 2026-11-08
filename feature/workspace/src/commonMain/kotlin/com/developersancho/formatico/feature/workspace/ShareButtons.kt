package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.developersancho.formatico.feature.workspace.strings.shareStrings
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.button.PantrixButton
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.foundation.icons.PantrixIcons

@Composable
internal fun CancelButton(actions: ShareActions) {
    PantrixButton(text = shareStrings.cancel, onClick = actions.close, variant = ButtonVariant.Text, tagKey = "cancel")
}

@Composable
internal fun CopyButton(
    link: String?,
    actions: ShareActions,
    modifier: Modifier = Modifier,
    size: ButtonSize = ButtonSize.Medium
) {
    PantrixButton(
        text = shareStrings.copy,
        onClick = { link?.let(actions.copy) },
        modifier = modifier,
        size = size,
        enabled = link != null,
        leadingIcon = { PantrixIcon(imageVector = PantrixIcons.Copy, contentDescription = null) },
        tagKey = "copy_link"
    )
}

@Composable
internal fun SendButton(link: String?, actions: ShareActions) {
    val send = actions.send ?: return
    PantrixButton(
        text = shareStrings.send,
        onClick = { link?.let(send) },
        modifier = Modifier.fillMaxWidth(),
        variant = ButtonVariant.Outline,
        size = ButtonSize.Large,
        enabled = link != null,
        tagKey = "send"
    )
}

/** Makes the encrypted link, waiting while it is made; again where the server was not reached. */
@Composable
internal fun CreateButton(
    encrypted: EncryptedShare,
    actions: ShareActions,
    modifier: Modifier = Modifier,
    size: ButtonSize = ButtonSize.Medium
) {
    if (encrypted == EncryptedShare.Failed) {
        PantrixButton(
            text = shareStrings.tryAgain,
            onClick = actions.create,
            modifier = modifier,
            size = size,
            leadingIcon = { PantrixIcon(imageVector = PantrixIcons.Refresh, contentDescription = null) },
            tagKey = "try_again"
        )
    } else {
        val creating = encrypted == EncryptedShare.Creating
        PantrixButton(
            text = if (creating) shareStrings.creating else shareStrings.create,
            onClick = actions.create,
            modifier = modifier,
            size = size,
            loading = creating,
            tagKey = "create_link"
        )
    }
}

@Composable
internal fun DeleteButton(actions: ShareActions, modifier: Modifier = Modifier, size: ButtonSize = ButtonSize.Medium) {
    PantrixButton(
        text = shareStrings.delete,
        onClick = actions.delete,
        modifier = modifier,
        variant = ButtonVariant.Text,
        size = size,
        tagKey = "delete_link"
    )
}

@Composable
internal fun SaveButton(actions: ShareActions, modifier: Modifier = Modifier, size: ButtonSize = ButtonSize.Medium) {
    PantrixButton(
        text = workspaceStrings.saveFile,
        onClick = actions.save,
        modifier = modifier,
        size = size,
        leadingIcon = { PantrixIcon(imageVector = PantrixIcons.Download, contentDescription = null) },
        tagKey = "save"
    )
}
