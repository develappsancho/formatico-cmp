package com.developersancho.formatico.core.format.types

/**
 * The root type's name for a document named [documentName]: its stem, the name without its folder and its
 * extension, made a type name as [typeNameOf] makes one ("order.json" → `Order`, "user-profile.yaml" →
 * `UserProfile`, "2026 sales.csv" → `Sales2026`); "Root" where there is no name or nothing usable in it.
 */
fun rootTypeName(documentName: String?): String = typeNameOf(stemOf(documentName.orEmpty()))

/**
 * A type name made of what a reader typed: its letters and digits kept, in words split where a character is neither
 * and where a capital starts a word ("userProfile", "HTTPServer"), the words joined in PascalCase ("user-profile",
 * "user_profile", "user profile", "userProfile" → `UserProfile`). A name in capitals of more than one word is read in
 * small letters ("USER_PROFILE" → `UserProfile`); a word in capitals among others keeps them ("orderDTO" →
 * `OrderDTO`).
 *
 * A type name cannot start with a digit: the words at its start that are numbers move to its end ("2026 sales" →
 * `Sales2026`), and a name that would still start with one, after a word such as "2fa" or where there are only
 * numbers, gets "Type" before it (`Type2fa`, `Type2026`). "Root" where nothing is left.
 */
fun typeNameOf(typed: String): String = pascalName(wordsOf(typed), ::capitalised) ?: ROOT

/**
 * The file the types of a root named [rootName] are saved in, in [language], named as each language's convention has
 * it: the type name for Kotlin and Swift (`Order.kt`, `UserProfile.swift`); in lower camel case for TypeScript
 * (`order.ts`, `userProfile.ts`); in small letters with nothing between its words for Go (`order.go`,
 * `userprofile.go`), as Go names its packages, since a `_` would make a name such as `order_test.go` or
 * `user_linux.go` a test or a file of one system; and a JSON Schema in small letters, its words joined by `-`, before
 * `.schema.json` (`order.schema.json`, `user-profile.schema.json`), as schemas are commonly named.
 */
fun typesFileName(rootName: String, language: TypeLanguage): String {
    val name = typeNameOf(rootName)
    return when (language) {
        TypeLanguage.Kotlin -> "$name.kt"
        TypeLanguage.TypeScript -> "${camelName(wordsOf(name))}.ts"
        TypeLanguage.Swift -> "$name.swift"
        TypeLanguage.Go -> "${lowered(name)}.go"
        TypeLanguage.JsonSchema -> wordsOf(name).joinToString("-") { lowered(it) } + ".schema.json"
    }
}

/** [documentName] without its folder and its extension; a name that starts with its only dot, `.env`, keeps it. */
private fun stemOf(documentName: String): String {
    val file = documentName.substringAfterLast('/').substringAfterLast('\\')
    val dot = file.lastIndexOf('.')
    return if (dot > 0) file.substring(0, dot) else file
}

/**
 * The words of [text]: its runs of letters and digits, each split where a capital starts a word, after a small
 * letter or a digit or before a small letter ("firstName" → first, Name; "HTTPServer" → HTTP, Server; "2026Sales" →
 * 2026, Sales). Where [text] has no small letters and more than one word, they are in small letters.
 */
internal fun wordsOf(text: String): List<String> {
    val runs = buildString { text.forEach { append(if (it.isLetterOrDigit()) it else ' ') } }.split(' ').filter { it.isNotEmpty() }
    val words = runs.flatMap(::camelWords)
    return if (words.size > 1 && text.none { it.isLowerCase() }) words.map(::lowered) else words
}

/** [run], letters and digits only, split where a capital starts a word. */
private fun camelWords(run: String): List<String> {
    val starts = (1 until run.length).filter { index ->
        run[index].isUpperCase() && (!run[index - 1].isUpperCase() || run.getOrNull(index + 1)?.isLowerCase() == true)
    }
    return (listOf(0) + starts + run.length).zipWithNext { from, to -> run.substring(from, to) }
}

/** [word] with its first letter a capital. */
internal fun capitalised(word: String): String = word.replaceFirstChar { it.uppercaseChar() }

/** [text] in small letters, a character for a character. */
internal fun lowered(text: String): String = buildString { text.forEach { append(it.lowercaseChar()) } }

/**
 * [words] joined in PascalCase, each written by [capitalise]: the numbers at the start moved to the end, and "Type"
 * before a name that would still start with a digit; null where there are no words.
 */
internal fun pascalName(words: List<String>, capitalise: (String) -> String): String? {
    val numbers = words.takeWhile { word -> word.all { it.isDigit() } }
    val name = (words.drop(numbers.size) + numbers).joinToString("") { capitalise(it) }
    return when {
        name.isEmpty() -> null
        name.first().isDigit() -> TYPE + name
        else -> name
    }
}

/** [words] joined in lower camel case: the first in small letters, each other with its first letter a capital. */
internal fun camelName(words: List<String>): String =
    words.mapIndexed { index, word -> if (index == 0) lowered(word) else capitalised(word) }.joinToString("")

/** The name of a root that has none. */
private const val ROOT = "Root"

/** What goes before a type name that would start with a digit. */
private const val TYPE = "Type"
