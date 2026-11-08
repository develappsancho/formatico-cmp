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
import formatico.feature.start.generated.resources.Res
import formatico.feature.start.generated.resources.start_all_samples
import formatico.feature.start.generated.resources.start_clear_history
import formatico.feature.start.generated.resources.start_edited_just_now
import formatico.feature.start.generated.resources.start_edited_minutes_ago
import formatico.feature.start.generated.resources.start_edited_on
import formatico.feature.start.generated.resources.start_edited_today_at
import formatico.feature.start.generated.resources.start_edited_yesterday
import formatico.feature.start.generated.resources.start_just_now
import formatico.feature.start.generated.resources.start_minutes_ago
import formatico.feature.start.generated.resources.start_opened_just_now
import formatico.feature.start.generated.resources.start_opened_minutes_ago
import formatico.feature.start.generated.resources.start_opened_on
import formatico.feature.start.generated.resources.start_opened_today_at
import formatico.feature.start.generated.resources.start_opened_yesterday
import formatico.feature.start.generated.resources.start_plain_text
import formatico.feature.start.generated.resources.start_recent
import formatico.feature.start.generated.resources.start_recent_empty_text
import formatico.feature.start.generated.resources.start_recent_empty_title
import formatico.feature.start.generated.resources.start_recent_meta
import formatico.feature.start.generated.resources.start_sample_countries
import formatico.feature.start.generated.resources.start_sample_countries_detail
import formatico.feature.start.generated.resources.start_sample_deployment
import formatico.feature.start.generated.resources.start_sample_deployment_detail
import formatico.feature.start.generated.resources.start_sample_deployment_detail_short
import formatico.feature.start.generated.resources.start_sample_deployment_short
import formatico.feature.start.generated.resources.start_sample_manifest
import formatico.feature.start.generated.resources.start_sample_manifest_detail
import formatico.feature.start.generated.resources.start_sample_order
import formatico.feature.start.generated.resources.start_sample_order_detail
import formatico.feature.start.generated.resources.start_sample_order_detail_short
import formatico.feature.start.generated.resources.start_samples
import formatico.feature.start.generated.resources.start_samples_hint
import formatico.feature.start.generated.resources.start_today
import formatico.feature.start.generated.resources.start_untitled
import formatico.feature.start.generated.resources.start_yesterday
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
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
        title = stringResource(Res.string.start_recent),
        action = {
            if (recent.isNotEmpty()) {
                PantrixLink(
                    text = stringResource(Res.string.start_clear_history),
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
                    headline = document.name ?: stringResource(Res.string.start_untitled),
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
        title = stringResource(Res.string.start_samples),
        action = {
            when {
                !phone -> PantrixText(
                    text = stringResource(Res.string.start_samples_hint),
                    style = PantrixTheme.typography.footnote,
                    color = PantrixTheme.colors.textSecondary
                )

                !all -> PantrixLink(
                    text = stringResource(Res.string.start_all_samples),
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
            modifier = Modifier.fillMaxWidth(),
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
            text = stringResource(Res.string.start_recent_empty_title),
            style = PantrixTheme.typography.label.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center
        )
        PantrixText(
            text = stringResource(Res.string.start_recent_empty_text),
            style = PantrixTheme.typography.footnote,
            color = PantrixTheme.colors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/** `JSON · 665 B · edited 2 minutes ago`, or `opened` where it was not changed; [short], on a phone, without either. */
@Composable
private fun recentMeta(document: RecentDocument, short: Boolean): String = stringResource(
    Res.string.start_recent_meta,
    document.format?.displayName ?: stringResource(Res.string.start_plain_text),
    PantrixTheme.strings.fileSize(document.size),
    lastTouched(document.at, edited = document.edited, short = short)
)

/** When a document was last opened, or [edited], as Recent says it. */
@Composable
private fun lastTouched(at: Instant, edited: Boolean, short: Boolean): String {
    val clock = LocalPantrixClock.current
    val strings = PantrixTheme.strings
    return when (val recency = recency(then = at, now = clock.clock.now(), utcOffset = clock.utcOffset)) {
        Recency.JustNow -> stringResource(
            when {
                short -> Res.string.start_just_now
                edited -> Res.string.start_edited_just_now
                else -> Res.string.start_opened_just_now
            }
        )

        is Recency.MinutesAgo -> pluralStringResource(
            when {
                short -> Res.plurals.start_minutes_ago
                edited -> Res.plurals.start_edited_minutes_ago
                else -> Res.plurals.start_opened_minutes_ago
            },
            recency.minutes,
            recency.minutes
        )

        is Recency.Today ->
            if (short) {
                stringResource(Res.string.start_today)
            } else {
                stringResource(
                    if (edited) Res.string.start_edited_today_at else Res.string.start_opened_today_at,
                    strings.clock(recency.hour, recency.minute)
                )
            }

        Recency.Yesterday -> stringResource(
            when {
                short -> Res.string.start_yesterday
                edited -> Res.string.start_edited_yesterday
                else -> Res.string.start_opened_yesterday
            }
        )

        is Recency.OnDate -> {
            val date = recency.date
            val day = if (recency.thisYear) {
                strings.dayMonth(date.day, date.month)
            } else {
                strings.date(date.day, date.month, date.year)
            }
            if (short) {
                day
            } else {
                stringResource(
                    if (edited) Res.string.start_edited_on else Res.string.start_opened_on,
                    day
                )
            }
        }
    }
}

@Composable
private fun sampleName(sample: Sample, short: Boolean): String = stringResource(
    when (sample) {
        Sample.Order -> Res.string.start_sample_order
        Sample.Deployment -> if (short) Res.string.start_sample_deployment_short else Res.string.start_sample_deployment
        Sample.Manifest -> Res.string.start_sample_manifest
        Sample.Countries -> Res.string.start_sample_countries
    }
)

@Composable
private fun sampleDetail(sample: Sample, short: Boolean): String = stringResource(
    when (sample) {
        Sample.Order -> if (short) Res.string.start_sample_order_detail_short else Res.string.start_sample_order_detail

        Sample.Deployment ->
            if (short) Res.string.start_sample_deployment_detail_short else Res.string.start_sample_deployment_detail

        Sample.Manifest -> Res.string.start_sample_manifest_detail

        Sample.Countries -> Res.string.start_sample_countries_detail
    }
)
