package com.developersancho.formatico.feature.start

import androidx.lifecycle.viewModelScope
import com.developersancho.formatico.core.base.MviViewModel
import com.pantrixui.foundation.files.PantrixFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * The Start screen's view model: what the user pastes, opens, drops or picks becomes an [OpenedDocument], which is
 * recorded in [history] and handed on with [StartEffect.OpenDocument].
 *
 * @param history the documents opened before, which Recent lists.
 * @param samples where the samples' texts are read from.
 * @param clock what a document is recorded as opened by.
 * @param maxFileSize the most bytes a file may have, by the platform.
 * @param reader where files are read and their formats told: off the screen's thread.
 * @param keeps whether the history is kept, as Settings has it.
 */
class StartViewModel(
    private val history: DocumentHistory,
    private val samples: SampleSource,
    clock: Clock,
    maxFileSize: Long,
    reader: CoroutineDispatcher = Dispatchers.Default,
    keeps: () -> Boolean = { true }
) : MviViewModel<StartState, StartIntent, StartEffect>(StartState()) {
    private val opener = DocumentOpener(history, clock, maxFileSize, reader, keeps)

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

            is Opening.Refused -> setState { copy(refusal = opening.refusal) }
        }
    }
}
