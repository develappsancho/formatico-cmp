package com.developersancho.formatico.feature.start

import androidx.lifecycle.viewModelScope
import com.developersancho.formatico.core.base.MviViewModel
import com.pantrixui.foundation.files.PantrixFile
import kotlinx.coroutines.launch

/**
 * The Start screen's view model: what the user pastes, opens, drops or picks becomes an [OpenedDocument], which is
 * recorded in [history] and handed on with [StartEffect.OpenDocument].
 *
 * @param history the documents opened before, which Recent lists.
 * @param samples where the samples' texts are read from.
 * @param opener what turns what is pasted, dropped or picked into a document: the app's own, whose wait for an
 *   encrypted link the app shows over every screen.
 */
class StartViewModel(
    private val history: DocumentHistory,
    private val samples: SampleSource,
    private val opener: DocumentOpener
) : MviViewModel<StartState, StartIntent, StartEffect>(StartState()) {
    init {
        viewModelScope.launch {
            history.documents.collect { recent -> setState { copy(recent = recent) } }
        }
    }

    override fun handle(intent: StartIntent) {
        when (intent) {
            is StartIntent.Paste -> paste(intent.text)

            StartIntent.NewDocument -> viewModelScope.launch {
                open(OpenedDocument(name = null, text = "", format = null))
            }

            is StartIntent.OpenFiles -> intent.files.firstOrNull()?.let(::openFile)

            is StartIntent.RefuseFile -> setState { copy(refusal = intent.refusal) }

            is StartIntent.OpenRecent -> viewModelScope.launch { history.document(intent.id)?.let { open(it) } }

            is StartIntent.OpenSample -> openSample(intent.sample)

            StartIntent.ClearHistory -> viewModelScope.launch { history.clear() }

            is StartIntent.OpenLink -> viewModelScope.launch { opener.link(intent.address)?.let(::show) }

            StartIntent.AskAddress -> sendEffect(StartEffect.AskAddress)
        }
    }

    private fun paste(text: String?) {
        viewModelScope.launch { show(opener.paste(text)) }
    }

    private fun openFile(file: PantrixFile) {
        viewModelScope.launch { show(opener.open(file)) }
    }

    private fun openSample(sample: Sample) {
        viewModelScope.launch {
            val text = runCatching { samples.read(sample) }.getOrNull() ?: return@launch
            open(OpenedDocument(name = sample.fileName, text = text, format = sample.format))
        }
    }

    /** Hands [document] on: one that holds text goes to the top of the history first, and on with its place there. */
    private suspend fun open(document: OpenedDocument) = show(Opening.Opened(opener.record(document)))

    /**
     * Shows what came of an [opening]: the document handed on, nothing to paste, a broken link, or why a file was
     * refused.
     */
    private fun show(opening: Opening) {
        when (opening) {
            is Opening.Opened -> {
                setState { copy(refusal = null) }
                sendEffect(StartEffect.OpenDocument(opening.document))
            }

            Opening.NothingToPaste -> sendEffect(StartEffect.NothingToPaste)

            Opening.BrokenLink -> sendEffect(StartEffect.BrokenLink)

            Opening.SharedLinkGone -> sendEffect(StartEffect.SharedLinkGone)

            is Opening.SharedLinkUnreachable -> sendEffect(StartEffect.SharedLinkUnreachable(opening.address))

            is Opening.Refused -> setState { copy(refusal = opening.refusal) }

            // Start opens no web address itself: the app's DocumentOpening does, from Start's button too.
            is Opening.AddressRefused -> Unit
        }
    }
}
