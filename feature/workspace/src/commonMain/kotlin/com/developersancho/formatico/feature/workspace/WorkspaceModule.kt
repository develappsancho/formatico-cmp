package com.developersancho.formatico.feature.workspace

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** What the workspaces are built from: the open documents, and a view model for each document's workspace. */
val workspaceModule: Module = module {
    single { Workbench() }
    viewModel { (documentId: Long) -> WorkspaceViewModel(documentId = documentId, workbench = get()) }
}
