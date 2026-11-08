package com.developersancho.formatico.core.testing

/** A word of three letters or more, which a format's name, a symbol or a value standing for itself is not. */
private val Word = Regex("\\p{Ll}{3,}")

/** A value a sentence was given, standing for itself: `{1}`. */
private val Given = Regex("\\{\\d+\\}")

/** What a sentence's number is given as: one more than one, so that a plural says its `other`. */
private const val SOME = 7

/**
 * The words [turkish] writes as [english] does, by the names of their members: two objects of one feature's strings
 * interface, Formatico's words in its two languages. A property is read as it is; a function is given its values as
 * themselves, a text as `{1}`, a number as 7. A word that holds no word of its own, a name or a symbol, is not one.
 */
@Suppress("SpreadOperator") // A method found by reflection takes its values as an array.
fun untranslated(english: Any, turkish: Any): Set<String> {
    val strings = english.javaClass.interfaces.single()
    return strings.methods.filter { it.returnType == String::class.java }.mapNotNull { method ->
        val given = method.parameterTypes.mapIndexed<Class<*>, Any> { index, type ->
            if (type == Int::class.javaPrimitiveType) SOME else "{${index + 1}}"
        }.toTypedArray()
        val inEnglish = method.invoke(english, *given) as String
        val inTurkish = method.invoke(turkish, *given) as String
        val name = if (method.parameterCount == 0) method.name.removePrefix("get").replaceFirstChar { it.lowercase() } else method.name
        name.takeIf { inEnglish == inTurkish && Word.containsMatchIn(inEnglish.replace(Given, "")) }
    }.toSet()
}
