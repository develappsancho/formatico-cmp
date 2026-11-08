package com.developersancho.formatico.core.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.dsl.module

/** The reader's preferences, in Koin: kept in the small store, for as long as the app lives. */
val settingsModule: Module = module {
    single { SettingsStore(small = get(), scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)) }
}
