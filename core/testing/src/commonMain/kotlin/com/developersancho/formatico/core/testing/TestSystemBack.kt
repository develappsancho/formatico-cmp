package com.developersancho.formatico.core.testing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.navigationevent.NavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner

/**
 * The system's back as a test sends it, the desktop's Escape and Android's back alike: given to the window's dispatcher
 * of navigation events by [SendsSystemBack], [send] goes back once. A test's window turns no key into a back, as a
 * desktop's or a browser's does with Escape.
 */
class TestSystemBack : NavigationEventInput() {
    /** Goes back once, as a key or a button does: with no gesture before it. */
    fun send() = dispatchOnBackCompleted()
}

/** Gives [back] to the window's dispatcher of navigation events, for as long as it is composed. */
@Composable
fun SendsSystemBack(back: TestSystemBack) {
    val owner = checkNotNull(LocalNavigationEventDispatcherOwner.current) { "The test's window has no way of going back" }
    DisposableEffect(owner) {
        owner.navigationEventDispatcher.addInput(back)
        onDispose { owner.navigationEventDispatcher.removeInput(back) }
    }
}
