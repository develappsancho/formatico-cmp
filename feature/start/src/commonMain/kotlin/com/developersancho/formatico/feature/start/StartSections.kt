package com.developersancho.formatico.feature.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.developersancho.formatico.feature.start.strings.startStrings
import com.pantrixui.components.container.surface.PantrixSurface
import com.pantrixui.components.data.listitem.ListItemDivider
import com.pantrixui.components.data.listitem.PantrixListItem
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.link.LinkUnderline
import com.pantrixui.components.elements.link.PantrixLink
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.components.elements.text.TextRole
import com.pantrixui.foundation.date.LocalPantrixClock
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme
import kotlin.time.Instant

/** How many documents Recent lists, as the design's boards do: four, and three on a phone. */
private const val RECENT_SHOWN = 4
private const val RECENT_SHOWN_ON_PHONE = 3

/** How many samples a phone lists until "All samples" is pressed. */
private const val SAMPLES_SHOWN_ON_PHONE = 2

/** Recent and Samples: side by side and as tall as each other in a wide window, one under the other elsewhere. */
@Composable
internal fun RecentAndSamples(
    recent: List<RecentDocument>,
    samples: List<Sample>,
    layout: StartLayout,
    onIntent: (StartIntent) -> Unit
) {
    if (layout == StartLayout.Wide) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.xl)
        ) {
            RecentSection(recent, layout, onIntent, Modifier.weight(1f).fillMaxHeight())
            SamplesSection(samples, layout, onIntent, Modifier.weight(1f).fillMaxHeight())
        }
    } else {
        RecentSection(recent, layout, onIntent)
        SamplesSection(samples, layout, onIntent)
    }
}

/** Recent: the documents opened before, or what will be listed here while there are none. */
@Composable
private fun RecentSection(
    recent: List<RecentDocument>,
    layout: StartLayout,
    onIntent: (StartIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val phone = layout == StartLayout.Phone
    Section(
        title = startStrings.recent,
        action = {
            if (recent.isNotEmpty()) {
                PantrixLink(
                    text = startStrings.clearHistory,
                    onClick = { onIntent(StartIntent.ClearHistory) },
                    underline = LinkUnderline.OnHover,
                    tagKey = "clear_history"
                )
            }
        },
        fillHeight = layout == StartLayout.Wide,
        modifier = modifier
    ) {
        if (recent.isEmpty()) {
            EmptyRecent(Modifier.fillMaxSize())
        } else {
            val shown = recent.take(if (phone) RECENT_SHOWN_ON_PHONE else RECENT_SHOWN)
            shown.forEachIndexed { index, document ->
                PantrixListItem(
                    headline = document.name ?: startStrings.untitled,
                    supportingText = recentMeta(document, short = phone),
                    leading = { PantrixIcon(imageVector = PantrixIcons.File, contentDescription = null) },
                    onClick = { onIntent(StartIntent.OpenRecent(document.id)) },
                    divider = if (index < shown.lastIndex) ListItemDivider.Full else ListItemDivider.None,
                    tagKey = "recent_${document.id}"
                )
            }
        }
    }
}

/** Samples: the documents that come with Formatico; a phone lists two until it is asked for all. */
@Composable
private fun SamplesSection(
    samples: List<Sample>,
    layout: StartLayout,
    onIntent: (StartIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val phone = layout == StartLayout.Phone
    var all by rememberSaveable { mutableStateOf(!phone) }
    Section(
        title = startStrings.samples,
        action = {
            when {
                !phone -> PantrixText(
                    text = startStrings.samplesHint,
                    style = PantrixTheme.typography.footnote,
                    color = PantrixTheme.colors.textSecondary
                )

                !all -> PantrixLink(
                    text = startStrings.allSamples,
                    onClick = { all = true },
                    underline = LinkUnderline.OnHover,
                    tagKey = "all_samples"
                )
            }
        },
        fillHeight = layout == StartLayout.Wide,
        modifier = modifier
    ) {
        val shown = if (phone && !all) samples.take(SAMPLES_SHOWN_ON_PHONE) else samples
        shown.forEachIndexed { index, sample ->
            PantrixListItem(
                headline = sampleName(sample, short = phone),
                supportingText = sampleDetail(sample, short = phone),
                trailingText = sample.format.displayName,
                leading = { PantrixIcon(imageVector = PantrixIcons.Folder, contentDescription = null) },
                chevron = true,
                onClick = { onIntent(StartIntent.OpenSample(sample)) },
                divider = if (index < shown.lastIndex) ListItemDivider.Full else ListItemDivider.None,
                tagKey = "sample_" + sample.name.lowercase()
            )
        }
    }
}

/**
 * A titled list on a card: [title] at the start of its head and [action] at the end. With [fillHeight], the card
 * takes the height the section is given, as both cards of a wide window do.
 */
@Composable
private fun Section(
    title: String,
    action: @Composable () -> Unit,
    fillHeight: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.sm)) {
        Row(
            // Side by side, both heads are as tall as a link's row, the platform's touch target: Recent's "Clear
            // history" would otherwise start its card lower than Samples' beside it.
            modifier = Modifier
                .fillMaxWidth()
                .then(if (fillHeight) Modifier.heightIn(min = PantrixTheme.platform.minTouchTarget) else Modifier),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PantrixText(
                text = title,
                style = PantrixTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                role = TextRole.Heading(level = 2)
            )
            action()
        }
        PantrixSurface(
            modifier = Modifier.fillMaxWidth().then(if (fillHeight) Modifier.weight(1f) else Modifier),
            color = PantrixTheme.colors.surface,
            shape = PantrixTheme.shapes.lg,
            border = PantrixTheme.colors.divider
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun EmptyRecent(modifier: Modifier = Modifier) {
    val spacing = PantrixTheme.spacing
    Column(
        modifier = modifier.padding(spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.sm, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PantrixIcon(
            imageVector = PantrixIcons.Clock,
            contentDescription = null,
            tint = PantrixTheme.colors.textSecondary
        )
        PantrixText(
            text = startStrings.recentEmptyTitle,
            style = PantrixTheme.typography.label.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center
        )
        PantrixText(
            text = startStrings.recentEmptyText,
            style = PantrixTheme.typography.footnote,
            color = PantrixTheme.colors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/** `JSON · 665 B · edited 2 minutes ago`, or `opened` where it was not changed; [short], on a phone, without either. */
@Composable
private fun recentMeta(document: RecentDocument, short: Boolean): String = startStrings.recentMeta(
    document.format?.displayName ?: startStrings.plainText,
    PantrixTheme.strings.fileSize(document.size),
    lastTouched(document.at, edited = document.edited, short = short)
)

/** When a document was last opened, or [edited], as Recent says it. */
@Composable
private fun lastTouched(at: Instant, edited: Boolean, short: Boolean): String {
    val clock = LocalPantrixClock.current
    val strings = PantrixTheme.strings
    return when (val recency = recency(then = at, now = clock.clock.now(), utcOffset = clock.utcOffset)) {
        Recency.JustNow -> {
            when {
                short -> startStrings.justNow
                edited -> startStrings.editedJustNow
                else -> startStrings.openedJustNow
            }
        }

        is Recency.MinutesAgo -> {
            val ago = when {
                short -> startStrings::minutesAgo
                edited -> startStrings::editedMinutesAgo
                else -> startStrings::openedMinutesAgo
            }
            ago(recency.minutes, recency.minutes)
        }

        is Recency.Today -> {
            if (short) {
                startStrings.today
            } else {
                val at = if (edited) startStrings::editedTodayAt else startStrings::openedTodayAt
                at(strings.clock(recency.hour, recency.minute))
            }
        }

        Recency.Yesterday -> {
            when {
                short -> startStrings.yesterday
                edited -> startStrings.editedYesterday
                else -> startStrings.openedYesterday
            }
        }

        is Recency.OnDate -> {
            onDate(recency, edited = edited, short = short)
        }
    }
}

/** A day before yesterday, as Recent says it: its day and month in this year, and its whole date before. */
@Composable
private fun onDate(recency: Recency.OnDate, edited: Boolean, short: Boolean): String {
    val strings = PantrixTheme.strings
    val date = recency.date
    val day = if (recency.thisYear) strings.dayMonth(date.day, date.month) else strings.date(date.day, date.month, date.year)
    val on = if (edited) startStrings::editedOn else startStrings::openedOn
    return if (short) day else on(day)
}

@Composable
private fun sampleName(sample: Sample, short: Boolean): String = when (sample) {
    Sample.Order -> startStrings.sampleOrder
    Sample.Deployment -> if (short) startStrings.sampleDeploymentShort else startStrings.sampleDeployment
    Sample.Manifest -> startStrings.sampleManifest
    Sample.Countries -> startStrings.sampleCountries
}

@Composable
private fun sampleDetail(sample: Sample, short: Boolean): String = when (sample) {
    Sample.Order -> {
        if (short) startStrings.sampleOrderDetailShort else startStrings.sampleOrderDetail
    }

    Sample.Deployment -> {
        if (short) startStrings.sampleDeploymentDetailShort else startStrings.sampleDeploymentDetail
    }

    Sample.Manifest -> {
        startStrings.sampleManifestDetail
    }

    Sample.Countries -> {
        startStrings.sampleCountriesDetail
    }
}
