package com.developersancho.formatico.core.storage

import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.awaitCacheReady

internal actual suspend fun KSafe.awaitReady() = awaitCacheReady()
