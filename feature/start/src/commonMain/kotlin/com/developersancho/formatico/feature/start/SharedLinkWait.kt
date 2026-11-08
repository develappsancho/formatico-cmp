package com.developersancho.formatico.feature.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.feature.start.strings.startStrings
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.button.PantrixButton
import com.pantrixui.components.elements.spinner.PantrixSpinner
import com.pantrixui.components.overlay.dialog.PantrixDialog
import com.pantrixui.foundation.adaptive.isNarrow
import com.pantrixui.foundation.theme.PantrixTheme

/**
 * While [opener] fetches an encrypted link's share, the app waits for it in a dialog over whatever it shows, as the
 * Start boards draw it (link: opening); cancelling it opens nothing.
 */
@Composable
internal fun SharedLinkWait(opener: DocumentOpener) {
    val fetching by opener.fetching.collectAsState()
    SharedLinkWait(fetching = fetching, onCancel = opener::cancelFetching)
}

/** The wait for an encrypted link's share while [fetching]; [onCancel] stops it. */
@Composable
internal fun SharedLinkWait(fetching: Boolean, onCancel: () -> Unit) {
    val phone = PantrixTheme.window.isNarrow
    PantrixDialog(
        visible = fetching,
        onDismissRequest = onCancel,
        title = startStrings.sharedOpeningTitle,
        message = if (phone) startStrings.sharedOpeningMessagePhone else startStrings.sharedOpeningMessage,
        actions = {
            PantrixButton(
                text = startStrings.sharedOpeningCancel,
                onClick = onCancel,
                variant = ButtonVariant.Text,
                size = if (phone) ButtonSize.Large else ButtonSize.Medium,
                tagKey = "shared_cancel"
            )
        },
        tagKey = "shared_opening"
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
            PantrixSpinner(size = SPINNER, contentDescription = startStrings.sharedOpeningTitle)
        }
    }
}

/** The spinner's size: the boards' 24. */
private val SPINNER = 24.dp
