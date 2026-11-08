package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.storage.SmallStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/** How many queries of each language are offered again. */
internal const val RECENT_QUERIES = 5

/** The queries used last, the latest first: [jsonPath]'s and [xPath]'s. */
@Serializable
internal data class RecentQueries(val jsonPath: List<String> = emptyList(), val xPath: List<String> = emptyList()) {
    /** The queries of [language], the latest first. */
    fun of(language: QueryLanguage): List<String> = when (language) {
        QueryLanguage.JsonPath -> jsonPath
        QueryLanguage.XPath -> xPath
    }

    /** These with [query] first among [language]'s, once, and no more than [RECENT_QUERIES] of them. */
    fun with(language: QueryLanguage, query: String): RecentQueries {
        val queries = (listOf(query) + of(language).filter { it != query }).take(RECENT_QUERIES)
        return when (language) {
            QueryLanguage.JsonPath -> copy(jsonPath = queries)
            QueryLanguage.XPath -> copy(xPath = queries)
        }
    }

    /** [later]'s queries, used after these, before these. */
    fun before(later: RecentQueries): RecentQueries = QueryLanguage.entries.fold(this) { queries, language ->
        later.of(language).asReversed().fold(queries) { kept, query -> kept.with(language, query) }
    }
}

/**
 * The queries used on this device, kept in [small]: what the Query view offers again. A query is used once its
 * matches are looked at, copied or opened, not as it is typed. Nothing is written before [start] has read what was
 * kept, which a query used meanwhile would write over.
 */
internal class QueryHistory(private val small: SmallStore, private val scope: CoroutineScope) {
    private val mutableQueries = MutableStateFlow(RecentQueries())
    private val read = CompletableDeferred<Unit>()

    /** The queries used last. */
    val queries: StateFlow<RecentQueries> = mutableQueries.asStateFlow()

    /** Reads the queries kept before, under those used since. */
    fun start(): Job = scope.launch {
        try {
            val kept = small.get(KEY, RecentQueries.serializer())
            if (kept != null) mutableQueries.update { kept.before(it) }
        } finally {
            read.complete(Unit)
        }
    }

    /** Remembers that [query], in [language], was used; a blank one is not. */
    fun remember(language: QueryLanguage, query: String) {
        val used = query.trim()
        if (used.isEmpty()) return
        mutableQueries.update { it.with(language, used) }
        scope.launch {
            read.await()
            small.put(KEY, mutableQueries.value, RecentQueries.serializer())
        }
    }

    private companion object {
        const val KEY = "queries"
    }
}
