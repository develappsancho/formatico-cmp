package com.developersancho.formatico.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The base of every screen's view model: one state the screen draws, the intents the screen sends it, and the
 * effects it sends back once, such as a message to show or a file to save.
 *
 * A screen sends what the user did with [dispatch]; the view model answers in [handle], by [setState] and
 * [sendEffect].
 *
 * @param State what the screen draws: an immutable value.
 * @param Intent what the user did, or asked for.
 * @param Effect what happens once and is not part of the state.
 * @param initialState the state before any intent.
 */
abstract class MviViewModel<State, Intent, Effect>(initialState: State) : ViewModel() {
    private val mutableState = MutableStateFlow(initialState)
    private val effectChannel = Channel<Effect>(Channel.BUFFERED)

    /** The state the screen draws. */
    val state: StateFlow<State> = mutableState.asStateFlow()

    /** The effects, each delivered once, to the screen that collects them. */
    val effects: Flow<Effect> = effectChannel.receiveAsFlow()

    /** Sends [intent] to the view model. */
    fun dispatch(intent: Intent) {
        handle(intent)
    }

    /** Answers [intent]: changes the state, sends an effect, or starts work. */
    protected abstract fun handle(intent: Intent)

    /** Gives the state its next value, made from the current one. */
    protected fun setState(reduce: State.() -> State) {
        mutableState.update(reduce)
    }

    /** Sends [effect] to the screen. */
    protected fun sendEffect(effect: Effect) {
        viewModelScope.launch { effectChannel.send(effect) }
    }
}
