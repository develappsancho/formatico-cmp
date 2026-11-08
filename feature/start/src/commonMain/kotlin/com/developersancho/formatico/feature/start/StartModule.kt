package com.developersancho.formatico.feature.start

import com.pantrixui.foundation.platform.currentPlatformType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import kotlin.time.Clock

/**
 * What the Start screen is built from: the history kept on this device, the samples, what opens a document from
 * anywhere, and its view model.
 */
val startModule: Module = module {
    // The history lives as long as the app does, and so does the scope its writes are made in.
    single<DocumentHistory> {
        StoredDocumentHistory(
            small = get(),
            texts = get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        )
    }
    single<SampleSource> { ResourceSampleSource() }
    single { DocumentOpener(history = get(), clock = Clock.System, maxFileSize = maxFileSize(currentPlatformType())) }
    viewModel {
        StartViewModel(
            history = get(),
            samples = get(),
            clock = Clock.System,
            maxFileSize = maxFileSize(currentPlatformType())
        )
    }
}
