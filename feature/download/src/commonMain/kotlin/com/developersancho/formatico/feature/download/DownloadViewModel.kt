package com.developersancho.formatico.feature.download

import androidx.lifecycle.viewModelScope
import com.developersancho.formatico.core.base.MviViewModel
import com.developersancho.formatico.core.network.FORMATICO_RELEASES_PAGE
import com.developersancho.formatico.core.network.LatestRelease
import com.developersancho.formatico.core.network.Releases
import kotlinx.coroutines.launch

/**
 * The download menu's view model: opened, it asks [releases] for the latest release, once it is found and again each
 * time it was not; a file chosen is downloaded, and the page of every release opened, as effects.
 */
internal class DownloadViewModel(private val releases: Releases) :
    MviViewModel<DownloadState, DownloadIntent, DownloadEffect>(DownloadState()) {
    override fun handle(intent: DownloadIntent) {
        when (intent) {
            DownloadIntent.Open -> open()

            DownloadIntent.Close -> setState { copy(open = false) }

            is DownloadIntent.Choose -> {
                setState { copy(open = false) }
                sendEffect(DownloadEffect.Download(intent.file))
            }

            DownloadIntent.OpenReleases -> {
                setState { copy(open = false) }
                sendEffect(DownloadEffect.OpenAddress(FORMATICO_RELEASES_PAGE))
            }
        }
    }

    private fun open() {
        val lookup = state.value.lookup
        setState { copy(open = true) }
        if (lookup is ReleaseLookup.Found || lookup == ReleaseLookup.Looking) return
        setState { copy(lookup = ReleaseLookup.Looking) }
        viewModelScope.launch {
            val found = when (val latest = releases.latest()) {
                is LatestRelease.Found -> ReleaseLookup.Found(latest.release)
                LatestRelease.None -> ReleaseLookup.None
                LatestRelease.Unreachable -> ReleaseLookup.Unreachable
            }
            setState { copy(lookup = found) }
        }
    }
}
