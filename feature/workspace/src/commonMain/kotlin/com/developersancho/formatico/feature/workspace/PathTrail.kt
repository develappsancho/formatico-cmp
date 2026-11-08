package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.developersancho.formatico.core.format.json.PathStep
import com.pantrixui.components.navigation.breadcrumb.BreadcrumbItem
import com.pantrixui.components.navigation.breadcrumb.PantrixBreadcrumb

/** The trail from the document's root to the chosen value; a step of it chooses that value. */
@Composable
internal fun PathTrail(state: WorkspaceState, onIntent: (WorkspaceIntent) -> Unit, modifier: Modifier = Modifier) {
    PathTrail(nodes = state.trail.ifEmpty { listOfNotNull(state.root) }, onIntent = onIntent, modifier = modifier)
}

/** The trail of [nodes], from the root down; a step of it chooses that value. */
@Composable
internal fun PathTrail(nodes: List<ValueNode>, onIntent: (WorkspaceIntent) -> Unit, modifier: Modifier = Modifier) {
    val trail = nodes
    if (trail.isEmpty()) return
    PantrixBreadcrumb(
        items = trail.map { node -> BreadcrumbItem(key = node.key, label = stepLabel(node), tagKey = stepTagOf(node)) },
        onSelect = { onIntent(WorkspaceIntent.Select(it)) },
        modifier = modifier,
        tagKey = "path"
    )
}

private const val ROOT_LABEL = "$"

/** The last step to [node], as the trail writes it: `$` for the root, a key, or an index in brackets. */
internal fun stepLabel(node: ValueNode): String = when (val step = node.path.lastOrNull()) {
    null -> ROOT_LABEL
    is PathStep.Index -> "[${step.index}]"
    is PathStep.Key -> step.name
}

/**
 * A step's automation key in the trail: `root`, or its depth and its last step, `1_customer`, `5_0`. Pantrix cuts a
 * tag at 32 characters, so a deep path written out whole would give several steps the same tag.
 */
internal fun stepTagOf(node: ValueNode): String = when (val step = node.path.lastOrNull()) {
    null -> "root"
    is PathStep.Index -> "${node.path.size}_${step.index}"
    is PathStep.Key -> "${node.path.size}_${step.name}"
}

/** A node's automation key, the same in every language: `root`, or its path without the `$`. */
internal fun tagOf(node: ValueNode): String = if (node.path.isEmpty()) "root" else node.key.drop(1).removePrefix(".")
