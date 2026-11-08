package com.developersancho.formatico

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** What the Swift side shows: the app in a view controller. */
@Suppress("FunctionNaming", "ktlint:standard:function-naming") // Swift calls it by the name of what it makes.
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
