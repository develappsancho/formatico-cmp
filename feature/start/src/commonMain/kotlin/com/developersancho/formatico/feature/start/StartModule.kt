package com.developersancho.formatico.feature.start

import com.pantrixui.foundation.platform.currentPlatformType
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import kotlin.time.Clock

/** What the Start screen is built from: the history, the samples and its view model. */
val startModule: Module = module {
    single<DocumentHistory> { SessionDocumentHistory() }
    single<SampleSource> { ResourceSampleSource() }
    viewModel {
        StartViewModel(
            history = get(),
            samples = get(),
            clock = Clock.System,
            maxFileSize = maxFileSize(currentPlatformType())
        )
    }
}
