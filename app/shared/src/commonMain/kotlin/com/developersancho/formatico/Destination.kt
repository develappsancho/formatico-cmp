package com.developersancho.formatico

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/** Where the app can be: a screen of it, at the top of the back stack. */
@Serializable
internal sealed interface Destination : NavKey {
    /** What a new tab shows: paste, open, drop, or pick a sample. */
    @Serializable
    data object Start : Destination

    companion object {
        /** How the back stack is saved: every destination, by its serializer. */
        val Saving = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(Start::class)
                }
            }
        }
    }
}
