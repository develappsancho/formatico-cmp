package com.developersancho.formatico.core.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle

/**
 * The base of every screen: draws [viewModel]'s state with [content], and hands its effects to [onEffect] while the
 * screen is started.
 *
 * @param viewModel the screen's view model.
 * @param onEffect what the screen does with each effect.
 * @param content the screen, drawn from the state; it sends what the user does to the view model with `dispatch`.
 */
@Composable
fun <State, Intent, Effect> MviScreen(
    viewModel: MviViewModel<State, Intent, Effect>,
    onEffect: suspend (Effect) -> Unit,
    content: @Composable (state: State, dispatch: (Intent) -> Unit) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnEffect by rememberUpdatedState(onEffect)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { currentOnEffect(it) }
        }
    }
    content(state, viewModel::dispatch)
}
