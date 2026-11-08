package com.developersancho.formatico.feature.start

import androidx.lifecycle.viewModelScope
import com.developersancho.formatico.core.base.MviViewModel
import com.developersancho.formatico.core.format.FormatDetector
import com.pantrixui.foundation.files.PantrixFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
 */
class StartViewModel(
    private val history: DocumentHistory,
    private val samples: SampleSource,
    private val clock: Clock,
    private val maxFileSize: Long,
    private val reader: CoroutineDispatcher = Dispatchers.Default
) : MviViewModel<StartState, StartIntent, StartEffect>(StartState()) {
    init {
        viewModelScope.launch {
            history.documents.collect { recent -> setState { copy(recent = recent) } }
        }
    }

    override fun handle(intent: StartIntent) {
        when (intent) {
            is StartIntent.Paste -> paste(intent.text)
            StartIntent.NewDocument -> open(OpenedDocument(name = null, text = "", format = null))
            is StartIntent.OpenFiles -> intent.files.firstOrNull()?.let(::openFile)
            is StartIntent.RefuseFile -> setState { copy(refusal = intent.refusal) }
            is StartIntent.OpenRecent -> history.document(intent.id)?.let(::open)
            is StartIntent.OpenSample -> openSample(intent.sample)
            StartIntent.ClearHistory -> history.clear()
        }
    }

    private fun paste(text: String?) {
        if (text.isNullOrBlank()) {
            sendEffect(StartEffect.NothingToPaste)
            return
        }
        viewModelScope.launch {
            val format = withContext(reader) { FormatDetector.detect(text) }
            open(OpenedDocument(name = null, text = text, format = format))
        }
    }

    private fun openFile(file: PantrixFile) {
        val size = file.size
        if (size != null && size > maxFileSize) {
            setState { copy(refusal = FileRefusal.TooLarge(file.name, maxFileSize)) }
            return
        }
        viewModelScope.launch {
            val bytes = runCatching { file.readBytes() }.getOrNull()
            when {
                bytes == null -> setState { copy(refusal = FileRefusal.Unreadable(file.name)) }

                bytes.size > maxFileSize -> setState { copy(refusal = FileRefusal.TooLarge(file.name, maxFileSize)) }

                else -> {
                    val document = withContext(reader) {
                        val text = bytes.decodeToString()
                        OpenedDocument(name = file.name, text = text, format = FormatDetector.detect(text, file.name))
                    }
                    open(document)
                }
            }
        }
    }

    private fun openSample(sample: Sample) {
        viewModelScope.launch {
            val text = runCatching { samples.read(sample) }.getOrNull() ?: return@launch
            open(OpenedDocument(name = sample.fileName, text = text, format = sample.format))
        }
    }

    /** Hands [document] on; one that holds text goes to the top of the history first. */
    private fun open(document: OpenedDocument) {
        setState { copy(refusal = null) }
        if (document.text.isNotEmpty()) history.record(document, clock.now())
        sendEffect(StartEffect.OpenDocument(document))
    }
}
