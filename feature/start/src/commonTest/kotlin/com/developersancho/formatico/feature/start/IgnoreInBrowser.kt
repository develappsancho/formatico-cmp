@file:OptIn(ExperimentalMultiplatform::class)

package com.developersancho.formatico.feature.start

/**
 * A UI test the browser does not run: in Karma the screen's words, Compose resources, never come while the test waits
 * for them (docs/OPEN_ITEMS.md). It runs on the desktop and on iOS.
 */
@OptionalExpectation
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
expect annotation class IgnoreInBrowser()
