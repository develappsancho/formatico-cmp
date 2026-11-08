package com.developersancho.formatico.core.storage

import org.koin.core.module.Module
import org.koin.dsl.module

/** The stores of this device, in Koin: on Android after `AndroidStorage.init`. */
val storageModule: Module = module {
    single { deviceStores() }
    single<SmallStore> { get<DeviceStores>().small }
    single<TextStore> { get<DeviceStores>().texts }
}

/** Stores that keep nothing past the app: what a test of the whole app is given in the place of the device's. */
val memoryStorageModule: Module = module {
    single<SmallStore> { MemorySmallStore() }
    single<TextStore> { MemoryTextStore() }
}

/** The stores of the platform the app runs on. */
internal expect fun deviceStores(): DeviceStores

/** The name of Formatico's stores: KSafe's file, and its namespace where a platform keeps more than one app's. */
internal const val STORE_NAME = "formatico"
