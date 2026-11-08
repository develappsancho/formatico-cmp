package com.developersancho.formatico.feature.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import com.developersancho.formatico.core.base.MviScreen
import com.developersancho.formatico.feature.start.strings.startStrings
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.button.PantrixButton
import com.pantrixui.components.elements.filedrop.FileRejection
import com.pantrixui.components.elements.filedrop.PantrixFileDrop
import com.pantrixui.components.elements.filedrop.RejectedFile
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.scrollbar.pantrixVerticalScroll
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.components.elements.text.TextRole
import com.pantrixui.components.overlay.snackbar.LocalPantrixSnackbars
import com.pantrixui.foundation.adaptive.WindowWidthClass
import com.pantrixui.foundation.adaptive.isNarrow
import com.pantrixui.foundation.files.FilePickRequest
import com.pantrixui.foundation.files.rememberFilePicker
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.keyboard.PantrixShortcut
import com.pantrixui.foundation.keyboard.shortcutRoot
import com.pantrixui.foundation.platform.PlatformType
import com.pantrixui.foundation.platform.onPaste
import com.pantrixui.foundation.platform.rememberPantrixClipboard
import com.pantrixui.foundation.theme.PantrixTheme
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/** How much of the window the lead paragraph takes in a wide window: about 720 of the design's 1040. */
private const val LEAD_WIDTH = 0.7f

/** The Start screen's three layouts, by the window's width: the design's boards at 390, 834 and 1440. */
internal enum class StartLayout { Phone, Tablet, Wide }

/**
 * The Start screen with its view model: what a new tab shows. What the user opens there is handed to
 * [onOpenDocument]; a web address is asked for with [onOpenAddress], in the dialog the app opens addresses with.
 */
@Composable
fun StartRoute(onOpenDocument: (OpenedDocument) -> Unit, modifier: Modifier = Modifier, onOpenAddress: () -> Unit = {}) {
    val viewModel = koinViewModel<StartViewModel>()
    val snackbars = LocalPantrixSnackbars.current
    val scope = rememberCoroutineScope()
    val nothingToPaste = startStrings.clipboardEmpty
    val brokenLink = startStrings.linkBroken
    val sharedLinkGone = startStrings.sharedLinkGone
    val sharedLinkUnreachable = startStrings.sharedLinkUnreachable
    val tryAgain = startStrings.sharedLinkTryAgain
    val words = startStrings
    val sizes = PantrixTheme.strings
    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is StartEffect.OpenDocument -> onOpenDocument(effect.document)

                StartEffect.NothingToPaste -> scope.launch {
                    snackbars?.show(message = nothingToPaste, tagKey = "nothing_to_paste")
                }

                is StartEffect.PasteTooLarge -> scope.launch {
                    snackbars?.show(message = words.pasteTooLarge(sizes.fileSize(effect.maxSize)), tagKey = "paste_too_large")
                }

                StartEffect.BrokenLink -> scope.launch { snackbars?.showBrokenLink(brokenLink) }

                StartEffect.SharedLinkGone -> scope.launch { snackbars?.showSharedLinkGone(sharedLinkGone) }

                is StartEffect.SharedLinkUnreachable -> scope.launch {
                    if (snackbars?.showSharedLinkUnreachable(sharedLinkUnreachable, tryAgain) == true) {
                        viewModel.dispatch(StartIntent.OpenLink(effect.address))
                    }
                }

                StartEffect.AskAddress -> onOpenAddress()
            }
        }
    ) { state, dispatch ->
        StartScreen(state = state, onIntent = dispatch, modifier = modifier)
    }
}

/**
 * The Start screen, drawn from [state]: what the user does goes to [onIntent]. What is pasted, by the paste button or
 * by Cmd+V or Ctrl+V anywhere on the screen, is read from the clipboard here and handed on as a [StartIntent.Paste].
 */
@Composable
internal fun StartScreen(state: StartState, onIntent: (StartIntent) -> Unit, modifier: Modifier = Modifier) {
    val window = PantrixTheme.window
    val spacing = PantrixTheme.spacing
    val layout = when {
        window.isNarrow -> StartLayout.Phone
        window.widthClass >= WindowWidthClass.Expanded -> StartLayout.Wide
        else -> StartLayout.Tablet
    }
    // The paste itself is heard by onPaste, and not as a shortcut, which would keep a browser from pasting: this one
    // only writes the keys out for the hint.
    val pasteShortcut = remember { PantrixShortcut(Key.V, command = true) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .shortcutRoot()
            .onPaste { onIntent(StartIntent.Paste(it)) }
            .pantrixVerticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = PantrixTheme.layout.margin)
                // The theme's content width, in the middle of a wide window as the design has it:
                // Modifier.maxContentWidth keeps it at the start.
                .widthIn(max = PantrixTheme.layout.maxContentWidth)
                .fillMaxWidth()
                .padding(
                    top = when (layout) {
                        StartLayout.Phone -> spacing.xl
                        StartLayout.Tablet -> spacing.xxl + spacing.sm
                        StartLayout.Wide -> spacing.xxl + spacing.lg
                    },
                    bottom = if (layout == StartLayout.Phone) spacing.lg else spacing.xxl
                ),
            verticalArrangement = Arrangement.spacedBy(
                if (layout == StartLayout.Phone) spacing.lg + spacing.xs else spacing.xl
            )
        ) {
            Heading(layout)
            Actions(
                layout = layout,
                onIntent = onIntent,
                onPaste = clipboardPaste(onIntent),
                pasteShortcut = pasteShortcut
            )
            if (layout == StartLayout.Phone) {
                RefusalMessage(state.refusal)
            } else {
                FileZone(refusal = state.refusal, onIntent = onIntent)
            }
            RecentAndSamples(recent = state.recent, samples = state.samples, layout = layout, onIntent = onIntent)
            if (layout != StartLayout.Phone) {
                PantrixText(
                    // On the web a web address is fetched through the server as well, which the apps fetch themselves.
                    text = if (PantrixTheme.platform.type == PlatformType.Web) startStrings.privacyWeb else startStrings.privacy,
                    style = PantrixTheme.typography.footnote,
                    color = PantrixTheme.colors.textSecondary
                )
            }
        }
    }
}

/**
 * Start's title and the sentence under it, in the page's content at every width as the boards have them: the page
 * scaffold's title stands in its bar on a phone (PANTRIX_FINDINGS, "PageScaffold'un başlığı her genişlikte içerikte
 * duramıyor").
 */
@Composable
private fun Heading(layout: StartLayout) {
    val typography = PantrixTheme.typography
    Column(verticalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.sm)) {
        PantrixText(
            text = startStrings.title,
            style = if (layout == StartLayout.Phone) typography.headline else typography.display,
            role = TextRole.Heading(level = 1)
        )
        PantrixText(
            text = if (layout ==
                StartLayout.Phone
            ) {
                startStrings.leadShort
            } else {
                startStrings.lead
            },
            modifier = if (layout == StartLayout.Wide) Modifier.fillMaxWidth(LEAD_WIDTH) else Modifier,
            style = if (layout == StartLayout.Phone) typography.body else typography.callout,
            color = PantrixTheme.colors.textSecondary
        )
    }
}

/**
 * What the paste button does: reads the clipboard's text, which a platform may ask its reader about first, and hands
 * it to [onIntent]. Where there is no clipboard to read, as in a browser that gives the page none, it says so.
 */
@Composable
private fun clipboardPaste(onIntent: (StartIntent) -> Unit): () -> Unit {
    val clipboard = rememberPantrixClipboard()
    val snackbars = LocalPantrixSnackbars.current
    val scope = rememberCoroutineScope()
    val unreadable = startStrings.clipboardUnreadable
    return {
        scope.launch {
            if (clipboard.traits.readable) {
                onIntent(StartIntent.Paste(clipboard.readText()))
            } else {
                snackbars?.show(message = unreadable, tagKey = "clipboard_unreadable")
            }
        }
    }
}

/** Paste, a new document, and on a phone the picker in place of the drop zone; [pasteShortcut] is the paste's keys. */
@Composable
private fun Actions(layout: StartLayout, onIntent: (StartIntent) -> Unit, onPaste: () -> Unit, pasteShortcut: PantrixShortcut) {
    val spacing = PantrixTheme.spacing
    val size = if (layout == StartLayout.Wide) ButtonSize.Medium else ButtonSize.Large
    val paste = @Composable {
        PantrixButton(text = startStrings.paste, onClick = onPaste, size = size, tagKey = "paste")
    }
    val newDocument = @Composable {
        PantrixButton(
            text = if (layout == StartLayout.Phone) startStrings.newDocument else startStrings.newEmptyDocument,
            onClick = { onIntent(StartIntent.NewDocument) },
            variant = if (layout == StartLayout.Phone) ButtonVariant.Text else ButtonVariant.Outline,
            size = size,
            leadingIcon = { PantrixIcon(imageVector = PantrixIcons.Plus, contentDescription = null) },
            tagKey = "new_document"
        )
    }
    val openAddress = @Composable {
        PantrixButton(
            text = startStrings.openAddress,
            onClick = { onIntent(StartIntent.AskAddress) },
            variant = ButtonVariant.Outline,
            size = size,
            tagKey = "open_address"
        )
    }
    when (layout) {
        StartLayout.Wide -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            paste()
            newDocument()
            openAddress()
            PasteHint(pasteShortcut, Modifier.padding(start = spacing.xs))
        }

        StartLayout.Tablet -> FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            paste()
            newDocument()
            openAddress()
        }

        StartLayout.Phone -> Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            paste()
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                OpenFileButton(onIntent)
                openAddress()
                newDocument()
            }
        }
    }
}

@Composable
private fun OpenFileButton(onIntent: (StartIntent) -> Unit) {
    val picker = rememberFilePicker()
    PantrixButton(
        text = startStrings.openFile,
        onClick = { picker.pick(FilePickRequest(accept = DocumentFileTypes)) { onIntent(StartIntent.OpenFiles(it)) } },
        variant = ButtonVariant.Outline,
        size = ButtonSize.Large,
        leadingIcon = { PantrixIcon(imageVector = PantrixIcons.File, contentDescription = null) },
        tagKey = "open_file"
    )
}

/** The drop zone, which opens the picker when pressed: what it takes, or why it did not take a file. */
@Composable
private fun FileZone(refusal: FileRefusal?, onIntent: (StartIntent) -> Unit) {
    val maxSize = maxFileSize(PantrixTheme.platform.type)
    PantrixFileDrop(
        onFiles = { onIntent(StartIntent.OpenFiles(it)) },
        accept = DocumentFileTypes,
        maxSize = maxSize,
        onRejected = { rejected ->
            rejected.firstNotNullOfOrNull { it.toRefusal(maxSize) }?.let { onIntent(StartIntent.RefuseFile(it)) }
        },
        label = startStrings.dropAFile,
        supportingText = refusal?.let { refusalText(it) }
            ?: startStrings.fileDropSupporting(PantrixTheme.strings.fileSize(maxSize)),
        error = refusal != null,
        actionLabel = startStrings.chooseAFile,
        tagKey = "file"
    )
}

/** On a phone, which has no drop zone to say it in, why the file picked was not opened. */
@Composable
private fun RefusalMessage(refusal: FileRefusal?) {
    val snackbars = LocalPantrixSnackbars.current
    val message = refusal?.let { refusalText(it) }
    LaunchedEffect(message) {
        if (message != null) snackbars?.show(message = message, tagKey = "file_refused")
    }
}

/** What the zone says of a file it did not take, or null for one it left only because it takes one at a time. */
private fun RejectedFile.toRefusal(maxSize: Long): FileRefusal? = when (reason) {
    FileRejection.Size -> FileRefusal.TooLarge(file.name, maxSize)
    FileRejection.Type -> FileRefusal.WrongType(file.name)
    FileRejection.Count -> null
}

/** What is said of a file that was not opened, for [refusal]. */
@Composable
internal fun refusalText(refusal: FileRefusal): String = when (refusal) {
    is FileRefusal.TooLarge -> {
        startStrings.fileTooLarge(
            refusal.fileName,
            PantrixTheme.strings.fileSize(refusal.maxSize)
        )
    }

    is FileRefusal.WrongType -> {
        startStrings.fileWrongType(refusal.fileName)
    }

    is FileRefusal.Unreadable -> {
        startStrings.fileUnreadable(refusal.fileName)
    }
}
