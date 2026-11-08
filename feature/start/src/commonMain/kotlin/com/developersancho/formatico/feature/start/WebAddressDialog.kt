package com.developersancho.formatico.feature.start

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.developersancho.formatico.core.network.AddressReading
import com.developersancho.formatico.feature.start.strings.startStrings
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.button.PantrixButton
import com.pantrixui.components.inputs.textfield.PantrixTextField
import com.pantrixui.components.overlay.dialog.PantrixDialog
import com.pantrixui.foundation.adaptive.isNarrow
import com.pantrixui.foundation.platform.PlatformType
import com.pantrixui.foundation.theme.PantrixTheme

/** HTTP's answers that say there is no document at an address: Not Found and Gone. */
private val NoDocument = setOf(404, 410)

/** The dialog of [opening]'s web address, while it asks for one, as [WebAddressDialog] draws it. */
@Composable
internal fun WebAddressDialog(opening: DocumentOpening) {
    WebAddressDialog(
        ask = opening.address,
        maxFileSize = opening.opener.maxFileSize,
        onChange = opening::changeAddress,
        onOpen = opening::openAddress,
        onDismiss = opening::dismissAddress
    )
}

/**
 * The web address asked for while [ask] holds one, as the Start boards draw it (address): the address, typed or
 * pasted, what the platform opens and how much, of [maxFileSize] at most, and why the document was not opened, under
 * the field. What the reader types goes to [onChange]; Open, and the keyboard's Go, to [onOpen].
 */
@Composable
internal fun WebAddressDialog(
    ask: AddressAsk?,
    maxFileSize: Long,
    onChange: (String) -> Unit,
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    // What was shown stays while the dialog leaves.
    var shown by remember { mutableStateOf<AddressAsk?>(null) }
    ask?.let { shown = it }
    val asked = shown ?: return
    val phone = PantrixTheme.window.isNarrow
    val size = if (phone) ButtonSize.Large else ButtonSize.Medium
    PantrixDialog(
        visible = ask != null,
        onDismissRequest = onDismiss,
        title = startStrings.openAddress,
        message = startStrings.addressMessage,
        actions = {
            PantrixButton(
                text = startStrings.addressCancel,
                onClick = onDismiss,
                variant = ButtonVariant.Text,
                size = size,
                tagKey = "address_cancel"
            )
            PantrixButton(
                text = if (asked.opening) startStrings.addressOpening else startStrings.addressOpen,
                onClick = onOpen,
                size = size,
                enabled = asked.text.isNotBlank(),
                loading = asked.opening,
                tagKey = "address_open"
            )
        },
        tagKey = "address"
    ) {
        PantrixTextField(
            value = asked.text,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            readOnly = asked.opening,
            label = startStrings.addressLabel,
            placeholder = startStrings.addressPlaceholder,
            supportingText = asked.refusal?.let { refusalOf(it, maxFileSize) } ?: helpOf(maxFileSize),
            error = asked.refusal != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onOpen() }),
            tagKey = "address"
        )
    }
}

/** What the platform opens from an address, and how much: on the web through Formatico's server. */
@Composable
private fun helpOf(maxFileSize: Long): String =
    if (PantrixTheme.platform.type == PlatformType.Web) {
        startStrings.addressHelpWeb
    } else {
        startStrings.addressHelp(PantrixTheme.strings.fileSize(maxFileSize))
    }

/** Why the document at the address was not opened, said as what to put right. */
@Composable
internal fun refusalOf(reading: AddressReading, maxFileSize: Long): String {
    val strings = startStrings
    val fileSize = PantrixTheme.strings::fileSize
    return when (reading) {
        AddressReading.NotAllowed -> strings.addressNotAllowed

        is AddressReading.Answered ->
            if (reading.status in NoDocument) {
                strings.addressNotThere(reading.status.toString())
            } else {
                strings.addressAnswered(reading.status.toString())
            }

        is AddressReading.TooLarge -> reading.bytes?.let { strings.addressTooLarge(fileSize(it), fileSize(maxFileSize)) }
            ?: strings.addressTooLargeUntold(fileSize(maxFileSize))

        AddressReading.NotText -> strings.addressNotText

        AddressReading.ServerUnreachable -> strings.addressServerUnreachable

        // A document read is opened, and never said: the dialog has left by then.
        AddressReading.Unreachable, is AddressReading.Read -> strings.addressUnreachable
    }
}
