package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.network.EncryptedLink
import com.developersancho.formatico.core.share.ShareKeep
import com.developersancho.formatico.core.share.ShareOutcome
import com.developersancho.formatico.feature.workspace.strings.shareStrings
import com.pantrixui.components.container.alert.AlertTone
import com.pantrixui.components.container.alert.PantrixAlert
import com.pantrixui.components.elements.radiobutton.PantrixRadioGroup
import com.pantrixui.components.elements.radiobutton.RadioOption
import com.pantrixui.components.elements.switchbutton.PantrixSwitchButton
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.components.inputs.textfield.PantrixTextField
import com.pantrixui.components.overlay.select.PantrixSelect
import com.pantrixui.components.overlay.select.SelectOption
import com.pantrixui.foundation.date.LocalPantrixClock
import com.pantrixui.foundation.date.PantrixDate
import com.pantrixui.foundation.theme.PantrixTheme

/**
 * What the share holds over its buttons: how to share it, where a link can hold the document, and what that way
 * takes: the link and its format; or how long the encrypted link is kept, and what the server can see of it, until it
 * is made and shown.
 */
@Composable
internal fun ShareChoices(share: DocumentShare, link: ShareOutcome.Link?, actions: ShareActions, phone: Boolean) {
    val strings = shareStrings
    if (!share.encryptedOnly) {
        PantrixRadioGroup(
            options = listOf(
                RadioOption(ShareWay.Link, strings.inLink, tagKey = "link"),
                RadioOption(ShareWay.Encrypted, strings.encrypted, tagKey = "encrypted")
            ),
            selected = share.shownWay,
            onSelect = actions.way,
            label = strings.ways,
            supportingText = when {
                share.shownWay == ShareWay.Link -> strings.inLinkBody
                phone -> strings.encryptedBodyPhone
                else -> strings.encryptedBody
            },
            tagKey = "way"
        )
    }
    val encrypted = share.encrypted
    when {
        share.shownWay == ShareWay.Link -> {
            LinkField(share, link, phone)
            FormattedSwitch(share, actions, label = strings.formatted)
        }

        encrypted is EncryptedShare.Created -> {
            EncryptedLinkField(encrypted.link, phone)
            if (encrypted.deleteFailed) {
                PantrixAlert(
                    message = strings.deleteFailed,
                    tone = AlertTone.Danger,
                    title = strings.unreachableTitle,
                    tagKey = "delete_failed"
                )
            }
        }

        else -> {
            KeepFor(share, actions, phone)
            FormattedSwitch(share, actions, label = strings.formattedEncrypted)
            if (encrypted == EncryptedShare.Failed) {
                PantrixAlert(
                    message = if (share.encryptedOnly) strings.unreachableBodyEncryptedOnly else strings.unreachableBody,
                    tone = AlertTone.Danger,
                    title = strings.unreachableTitle,
                    tagKey = "unreachable"
                )
            } else {
                PantrixAlert(message = strings.trust, tone = AlertTone.Info, announce = false, tagKey = "trust")
            }
        }
    }
}

/**
 * The link, to read and select, and under it how much of the document it carries in how many characters, and whether
 * every app takes it whole. Empty while the first link is being made.
 */
@Composable
private fun LinkField(share: DocumentShare, link: ShareOutcome.Link?, phone: Boolean) {
    val strings = PantrixTheme.strings
    val fits = link?.let {
        val words = when {
            !it.short -> shareStrings::fitsLong
            phone -> shareStrings::fitsShortPhone
            else -> shareStrings::fitsShort
        }
        words(
            strings.fileSize(it.bytes.toLong()),
            share.format?.displayName ?: shareStrings.plainText,
            strings.decimal(it.characters.toDouble())
        )
    }
    PantrixTextField(
        value = link?.link.orEmpty(),
        onValueChange = {},
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        label = shareStrings.link,
        supportingText = fits,
        tagKey = "link"
    )
}

/** The encrypted link made, and under it the day its share expires, by the device's clock. */
@Composable
private fun EncryptedLinkField(link: EncryptedLink, phone: Boolean) {
    val strings = PantrixTheme.strings
    val day = PantrixDate.of(link.expiresAt, LocalPantrixClock.current.utcOffset(link.expiresAt))
    val until = strings.longDate(day.day, day.month, day.year)
    PantrixTextField(
        value = link.link,
        onValueChange = {},
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        label = shareStrings.link,
        supportingText = if (phone) {
            shareStrings.keptPhone(until)
        } else {
            shareStrings.kept(until, strings.decimal(link.link.length.toDouble()))
        },
        tagKey = "encrypted_link"
    )
}

/** How long the server keeps the encrypted link: a day, a week or a month. */
@Composable
private fun KeepFor(share: DocumentShare, actions: ShareActions, phone: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PantrixText(
            text = shareStrings.keepFor,
            modifier = Modifier.weight(1f),
            style = if (phone) PantrixTheme.typography.body else PantrixTheme.typography.label
        )
        PantrixSelect(
            options = ShareKeep.entries.map { SelectOption(it, shareStrings.days(it.days), tagKey = "${it.days}") },
            selected = share.keep,
            onSelect = actions.keep,
            modifier = Modifier.width(KEEP_WIDTH),
            enabled = share.encrypted != EncryptedShare.Creating,
            contentDescription = shareStrings.keepFor,
            tagKey = "keep"
        )
    }
}

/** Whether the link carries the document as Format writes it: not for plain text, nor a document with errors. */
@Composable
private fun FormattedSwitch(share: DocumentShare, actions: ShareActions, label: String) {
    PantrixSwitchButton(
        checked = share.formatted && share.canFormat,
        onCheckedChange = actions.format,
        label = label,
        enabled = share.canFormat && share.encrypted != EncryptedShare.Creating,
        tagKey = "formatted"
    )
}

/** How wide the select of how long a link is kept stands: the boards' 200. */
private val KEEP_WIDTH = 200.dp
