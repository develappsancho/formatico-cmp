package com.developersancho.formatico.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.feature.settings.strings.settingsStrings
import com.pantrixui.components.data.listitem.PantrixListItem
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.button.PantrixButton
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.components.elements.text.TextRole
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme

/**
 * The libraries Formatico is built on, each with its licence: pressing one shows its text. A phone lists those its
 * app is built on, which is not the web app's.
 */
@Composable
internal fun LicencesPage(layout: SettingsLayout, onIntent: (SettingsIntent) -> Unit) {
    val phone = layout == SettingsLayout.Phone
    Column(verticalArrangement = Arrangement.spacedBy(if (phone) PantrixTheme.spacing.md else PantrixTheme.spacing.lg)) {
        if (!phone) PageTitle(title = settingsStrings.licences, onIntent = onIntent)
        PantrixText(
            text = if (phone) settingsStrings.licencesBodyPhone else settingsStrings.licencesBody,
            modifier = Modifier.padding(horizontal = if (phone) PantrixTheme.spacing.xs else 0.dp),
            style = PantrixTheme.typography.label.copy(fontWeight = FontWeight.Medium),
            color = PantrixTheme.colors.textSecondary
        )
        SettingsGroup {
            val libraries = OpenSourceLibrary.entries.filter { !(phone && it.inTheWebApp) }
            libraries.forEachIndexed { index, library ->
                if (index > 0) RowDivider()
                val licence = library.licence.title
                PantrixListItem(
                    headline = library.title,
                    supportingText = if (library.inTheWebApp) settingsStrings.inTheWebApp(licence) else licence,
                    chevron = true,
                    onClick = { onIntent(SettingsIntent.ShowLicence(library)) },
                    tagKey = "library_${library.name.lowercase()}"
                )
            }
        }
    }
}

/** The licence [library] is used under, written out; on a phone, the bar says whose it is. */
@Composable
internal fun LicencePage(library: OpenSourceLibrary, layout: SettingsLayout, onIntent: (SettingsIntent) -> Unit) {
    val text by produceState(initialValue = "", library) { value = licenceText(library.licence) }
    Column(verticalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.lg)) {
        if (layout != SettingsLayout.Phone) {
            PageTitle(title = library.title, onIntent = onIntent, back = settingsStrings.licences)
        }
        SettingsGroup {
            PantrixText(
                text = text,
                modifier = Modifier.fillMaxWidth().padding(PantrixTheme.spacing.lg),
                style = PantrixTheme.typography.code.copy(
                    fontSize = PantrixTheme.typography.footnote.fontSize,
                    lineHeight = PantrixTheme.typography.footnote.lineHeight
                )
            )
        }
    }
}

/**
 * A page's title in a wide window and on a tablet, under the way back to [back], Settings by default: a phone's bar
 * has both.
 */
@Composable
private fun PageTitle(title: String, onIntent: (SettingsIntent) -> Unit, back: String = settingsStrings.title) {
    Column(horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.sm)) {
        PantrixButton(
            text = back,
            onClick = { onIntent(SettingsIntent.Back) },
            variant = ButtonVariant.Text,
            size = ButtonSize.Small,
            leadingIcon = { PantrixIcon(imageVector = PantrixIcons.ChevronBack, contentDescription = null) },
            tagKey = "back"
        )
        PantrixText(text = title, style = PantrixTheme.typography.headline, role = TextRole.Heading(level = 1))
    }
}
