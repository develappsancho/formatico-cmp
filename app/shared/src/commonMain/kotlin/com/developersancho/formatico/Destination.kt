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

    /** The workspace of the open document whose id is [documentId]. */
    @Serializable
    data class Workspace(val documentId: Long) : Destination

    companion object {
        /**
         * Puts the workspace of the document whose id is [documentId] over Start, the root, or none where it is null:
         * the back stack follows the chosen tab.
         */
        fun MutableList<NavKey>.showDocument(documentId: Long?) {
            removeAll { it is Workspace }
            if (documentId != null) add(Workspace(documentId))
        }

        /** How the back stack is saved: every destination, by its serializer. */
        val Saving = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(Start::class)
                    subclass(Workspace::class)
                }
            }
        }
    }
}
