package com.developersancho.formatico.core.settings

import com.developersancho.formatico.core.storage.MemorySmallStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsStoreTest {
    @Test
    fun aStoreWithNothingKeptHasTheDefaults() = runTest(UnconfinedTestDispatcher()) {
        val store = SettingsStore(MemorySmallStore(), backgroundScope)

        assertTrue(store.loaded.value)
        assertEquals(Preferences(), store.preferences.value)
        assertEquals(ThemeChoice.System, store.preferences.value.theme)
        assertEquals(HistoryKeep.Days30, store.preferences.value.keepFor)
    }

    @Test
    fun aChangeIsKeptAndReadBackWhenTheAppOpensAgain() = runTest(UnconfinedTestDispatcher()) {
        val small = MemorySmallStore()
        SettingsStore(small, backgroundScope).update { it.copy(indentation = IndentChoice.Tabs, language = AppLanguage.Turkish) }

        val again = SettingsStore(small, backgroundScope)

        assertEquals(IndentChoice.Tabs, again.preferences.value.indentation)
        assertEquals(AppLanguage.Turkish, again.preferences.value.language)
    }

    @Test
    fun theThemeTheBarChoseBeforeSettingsIsReadOnce() = runTest(UnconfinedTestDispatcher()) {
        val small = MemorySmallStore()
        small.put("theme", true, Boolean.serializer())

        assertEquals(ThemeChoice.Dark, SettingsStore(small, backgroundScope).preferences.value.theme)
    }

    @Test
    fun aSystemIsSpokenToInItsLanguageWhereFormaticoHasIt() {
        assertEquals(AppLanguage.Turkish, AppLanguage.forSystem("tr-TR"))
        assertEquals(AppLanguage.English, AppLanguage.forSystem("en"))
        assertEquals(AppLanguage.English, AppLanguage.forSystem("de-DE"))
    }
}
