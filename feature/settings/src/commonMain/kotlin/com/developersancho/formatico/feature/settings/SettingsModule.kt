package com.developersancho.formatico.feature.settings

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Settings' view model, over the preferences and the history the app gives Koin. */
val settingsFeatureModule: Module = module {
    viewModel { SettingsViewModel(settings = get(), history = get()) }
}
