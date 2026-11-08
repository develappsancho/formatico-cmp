package com.developersancho.formatico.feature.download

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** The download menu's view model, on core:network's releases. */
val downloadModule: Module = module {
    viewModel { DownloadViewModel(releases = get()) }
}
