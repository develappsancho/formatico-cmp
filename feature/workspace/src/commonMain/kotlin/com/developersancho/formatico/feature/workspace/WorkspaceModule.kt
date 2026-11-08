package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.settings.SettingsStore
import com.pantrixui.foundation.platform.currentPlatformType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * What the workspaces are built from: the open documents and the queries used, kept on this device from the moment the
 * app opens, and a view model for each document's workspace.
 */
val workspaceModule: Module = module {
    // Format writes as Settings has it.
    single { Workbench(formatting = { get<SettingsStore>().preferences.value.formatOptions }) }
    single(createdAtStart = true) {
        WorkbenchKeeper(
            workbench = get(),
            small = get(),
            texts = get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        ).also { it.start() }
    }
    single(createdAtStart = true) {
        QueryHistory(small = get(), scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)).also { it.start() }
    }
    single { Readings(workbench = get(), budget = readingsBudget(currentPlatformType())) }
    single { Editors(workbench = get()) }
    viewModel { (documentId: Long) -> WorkspaceViewModel(documentId = documentId, workbench = get(), readings = get(), queries = get()) }
}
