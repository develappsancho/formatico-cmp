package com.developersancho.formatico.core.base

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class MviViewModelTest {
    private data class CounterState(val count: Int = 0)

    private sealed interface CounterIntent {
        data object Increment : CounterIntent

        data object Share : CounterIntent
    }

    private data class Shared(val count: Int)

    private class CounterViewModel : MviViewModel<CounterState, CounterIntent, Shared>(CounterState()) {
        override fun handle(intent: CounterIntent) {
            when (intent) {
                CounterIntent.Increment -> setState { copy(count = count + 1) }
                CounterIntent.Share -> sendEffect(Shared(state.value.count))
            }
        }
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun anIntentChangesTheState() {
        val viewModel = CounterViewModel()
        viewModel.dispatch(CounterIntent.Increment)
        viewModel.dispatch(CounterIntent.Increment)
        assertEquals(CounterState(count = 2), viewModel.state.value)
    }

    @Test
    fun anEffectIsDeliveredOnce() = runTest {
        val viewModel = CounterViewModel()
        viewModel.dispatch(CounterIntent.Increment)
        viewModel.dispatch(CounterIntent.Share)
        assertEquals(Shared(count = 1), viewModel.effects.first())
    }
}
