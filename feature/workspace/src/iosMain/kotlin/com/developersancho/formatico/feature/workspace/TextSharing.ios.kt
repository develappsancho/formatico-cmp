package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindowScene
import platform.UIKit.popoverPresentationController

@Composable
internal actual fun rememberTextSharing(): ((String) -> Unit)? = remember {
    { text ->
        // The sheet is shown over whatever the app's window shows when it is asked for.
        presenter()?.let { top ->
            val sheet = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
            // An iPad shows the sheet as a popover, which needs a place to point at.
            sheet.popoverPresentationController?.sourceView = top.view
            top.presentViewController(sheet, animated = true, completion = null)
        }
    }
}

/** The view controller the app's key window shows on top, which a sheet is presented from. */
private fun presenter(): UIViewController? {
    val scene = UIApplication.sharedApplication.connectedScenes.filterIsInstance<UIWindowScene>().firstOrNull()
    var top = scene?.keyWindow?.rootViewController ?: return null
    while (true) top = top.presentedViewController ?: break
    return top
}
