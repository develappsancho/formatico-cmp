package com.developersancho.formatico.core.format.types

/**
 * The singular of [word] where it reads as an English plural, in simple English: "items" → "item", "addresses" →
 * "address", "categories" → "category", "boxes" → "box", "statuses" → "status", "people" → "person"; in capitals
 * where [word] is. Null where it does not read as one: a word that does not end in "s", and one that ends in "ss",
 * "us" or "is" ("class", "status", "analysis"), but for the few plurals that do ("menus", "emojis", "apis").
 */
internal fun singular(word: String): String? {
    val lower = lowered(word)
    val irregular = Irregular[lower]
    if (irregular != null) {
        return when {
            word.none { it.isLowerCase() } -> capitals(irregular)
            word.first().isUpperCase() -> capitalised(irregular)
            else -> irregular
        }
    }
    val ending = endingOf(lower) ?: return null
    val added = if (word.none { it.isLowerCase() }) capitals(ending.added) else ending.added
    return word.dropLast(ending.dropped) + added
}

/** [text] in capitals, a character for a character. */
internal fun capitals(text: String): String = buildString { text.forEach { append(it.uppercaseChar()) } }

/** How a plural ends: how many of its letters its singular drops, and what it adds in their place. */
private class Ending(val dropped: Int, val added: String = "")

private val DropS = Ending(1)
private val DropEs = Ending(2)
private val IesToY = Ending(IES, "y")

/** How [lower], a word in small letters, ends as a plural; null where it reads as none. */
private fun endingOf(lower: String): Ending? = when {
    lower.length < SHORTEST || !lower.endsWith('s') || lower in NotPlurals -> null
    lower in PlainPlurals -> DropS
    lower.endsWith("ss") || lower.endsWith("us") || lower.endsWith("is") -> null
    lower.endsWith("ies") -> if (lower.length <= IES + 1 || IeWords.any { lower.endsWith(it + "s") }) DropS else IesToY
    EsEndings.any { lower.endsWith(it) } -> DropEs
    lower.endsWith("ches") -> if (CheWords.any { lower.endsWith(it + "s") }) DropS else DropEs
    lower.endsWith("uses") -> if (lower.getOrNull(lower.length - USES - 1)?.let { it in VOWELS } != false) DropS else DropEs
    else -> DropS
}

/** How long "ies" and "uses" are, and the shortest word read as a plural: "ids". */
private const val IES = 3
private const val USES = 4
private const val SHORTEST = 3
private const val VOWELS = "aeiou"

/** The endings whose plural adds "es": "addresses", "boxes", "buzzes", "wishes". */
private val EsEndings = listOf("sses", "xes", "zzes", "shes")

/** The plurals that are not made by adding an "s" or an "es". */
private val Irregular = mapOf(
    "people" to "person",
    "children" to "child",
    "men" to "man",
    "women" to "woman",
    "mice" to "mouse",
    "geese" to "goose",
    "feet" to "foot",
    "teeth" to "tooth",
    "leaves" to "leaf",
    "knives" to "knife",
    "lives" to "life",
    "wives" to "wife",
    "halves" to "half",
    "shelves" to "shelf",
    "wolves" to "wolf",
    "indices" to "index",
    "vertices" to "vertex",
    "matrices" to "matrix",
    "appendices" to "appendix",
    "analyses" to "analysis",
    "crises" to "crisis",
    "theses" to "thesis",
    "diagnoses" to "diagnosis",
    "axes" to "axis",
    "criteria" to "criterion",
    "phenomena" to "phenomenon",
    "quizzes" to "quiz",
    "aliases" to "alias",
    "radii" to "radius",
    "cacti" to "cactus"
)

/** Words that end in "s" and are no plurals, or the same in both. */
private val NotPlurals = setOf(
    "news",
    "series",
    "species",
    "means",
    "lens",
    "gas",
    "yes",
    "canvas",
    "atlas",
    "alias",
    "bias",
    "chaos",
    "analytics",
    "physics",
    "mathematics",
    "economics",
    "politics",
    "ethics",
    "ios",
    "macos",
    "gps",
    "dns",
    "sms"
)

/** Plurals that end in "us" or "is", of words that end in "u" or "i". */
private val PlainPlurals =
    setOf("menus", "skus", "gurus", "emus", "haikus", "emojis", "apis", "uris", "kpis", "wikis", "taxis", "skis", "kiwis")

/** Words that end in "ie", whose plural ends in "ies" and drops only its "s". */
private val IeWords = listOf(
    "movie",
    "cookie",
    "calorie",
    "zombie",
    "rookie",
    "selfie",
    "smoothie",
    "brownie",
    "goalie",
    "hippie",
    "prairie",
    "freebie",
    "auntie",
    "genie",
    "pixie",
    "newbie",
    "techie",
    "foodie",
    "hoodie",
    "sortie",
    "lingerie"
)

/** Words that end in "che", whose plural ends in "ches" and drops only its "s". */
private val CheWords = listOf("cache", "niche", "headache", "avalanche", "moustache", "mustache", "cliche", "quiche", "psyche", "creche")
