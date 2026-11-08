package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.RepairKind
import com.developersancho.formatico.core.format.json.PathStep
import com.developersancho.formatico.core.format.query.QueryErrorCode
import com.developersancho.formatico.core.schema.SchemaRule
import com.developersancho.formatico.feature.workspace.strings.QueryStringsEnglish
import com.developersancho.formatico.feature.workspace.strings.SchemaStringsEnglish
import com.pantrixui.testing.PantrixTestTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The words the workspace says of what it found: problems and their repairs, losses, differences, a schema's rules. */
@OptIn(ExperimentalTestApi::class)
class WordsTest {
    /** What [words] make in a composition, in English, checked by [check]. */
    private fun <T : Any> wordsOf(words: @Composable () -> T, check: (T) -> Unit) = runComposeUiTest {
        val made = mutableListOf<T>()
        setContent { PantrixTestTheme { made += words() } }
        awaitIdle()
        check(made.last())
    }

    @Test
    fun eachProblemOfTheInvalidBoardsIsSaidWithWhatPutsItRight() = wordsOf({
        Invalid.problems.map { problemMessage(it, PreviewInvalid, Format.Json) }
    }) { messages ->
        assertEquals(
            listOf(
                "Take out the comment: JSON has none. Or read the document as JSONC.",
                "Add a comma after the value on line 9.",
                "Put this key in double quotes: \"timeout\".",
                "Take out the comma after the last member."
            ),
            messages
        )
    }

    @Test
    fun aYamlFlowLeftOpenIsSaidSo() = wordsOf({
        InvalidYaml.problems.map { problemMessage(it, PreviewInvalidYaml, Format.Yaml) }
    }) { messages ->
        assertEquals(listOf("These brackets are never closed."), messages)
    }

    @Test
    fun eachRepairHasALabelOfItsOwn() = wordsOf({ RepairKind.entries.associateWith { repairLabel(it) } }) { labels ->
        assertEquals("Remove the comment", labels[RepairKind.RemoveComment])
        assertEquals("Add the comma", labels[RepairKind.AddComma])
        assertEquals("Quote the key", labels[RepairKind.QuoteKey])
        assertTrue(labels.values.all { it.isNotBlank() }, "$labels")
        assertEquals(labels.size, labels.values.toSet().size, "each label its own: $labels")
    }

    @Test
    fun aLossIsNamedAndSeveralAreCounted() = wordsOf({
        val toml = assertIs<ConvertOutcome.Converted>(OrderToToml.conversion)
        val xml = assertIs<ConvertOutcome.Converted>(
            previewState(
                "order.json",
                PreviewOrder,
                Format.Json,
                DocumentView(pane = DocumentPane.Convert, convert = ConvertChoice(to = Format.Xml))
            )
                .conversion
        )
        listOf(
            toml.losses.map { lossWords(it, toml.from, toml.to).title },
            listOf(lossesTag(toml.losses, toml.from, toml.to), lossWords(toml.losses.single(), toml.from, toml.to).tag),
            xml.losses.map { lossWords(it, xml.from, xml.to).title },
            listOf(lossesTag(xml.losses, xml.from, xml.to))
        )
    }) { (tomlTitles, tomlTags, xmlTitles, xmlTag) ->
        assertEquals(listOf("TOML has no null"), tomlTitles)
        // One loss is tagged with its own words, as the status bar says it.
        assertEquals(tomlTags[1], tomlTags[0])
        assertTrue("XML has no null" in xmlTitles && "XML has only text" in xmlTitles, "$xmlTitles")
        assertEquals("${xmlTitles.size} losses", xmlTag.single())
    }

    @Test
    fun nothingLostSaysTheDocumentComesBackAsItIs() = wordsOf({
        val yaml = assertIs<ConvertOutcome.Converted>(OrderToYaml.conversion)
        assertTrue(yaml.losses.isEmpty())
        nothingLostMessage(yaml, withNotes = false)
    }) { message ->
        assertEquals("It converts back to this JSON as it is.", message)
    }

    @Test
    fun aDifferenceSaysWhereAndWhatChanged() = wordsOf({
        val compared = assertIs<CompareOutcome.Compared>(OrdersCompared.comparison)
        Triple(
            compared.differences.map { differenceWhere(it) },
            compared.differences.map { differenceWhat(it) } + compared.differences.map { changeName(it.change) },
            listOfNotNull(notCountedMessage(compared), comparedName(compared.a), comparedName(compared.b))
        )
    }) { (where, what, names) ->
        assertTrue("\$.status" in where, "$where")
        assertTrue("\"shipped\" → \"delivered\"" in what && "1 → 2" in what, "$what")
        assertEquals(listOf("order.json", "order-v2.json"), names.takeLast(2))
        assertEquals(3, names.size, "what was not counted is said: $names")
    }

    @Test
    fun aSchemasRulesAreSaidOfTheValue() {
        val words = SchemaStringsEnglish
        assertEquals("coupon", words.nameOf(listOf(PathStep.Key("coupon"))))
        assertTrue(words.requiredOf("customer", listOf("phone")).contains("phone"))
        assertTrue(words.boundOf(SchemaRule.Bound(limit = "0", lower = true, exclusive = false)).contains("0"))
        assertTrue(words.bothOf(listOf("a string", "a number")).contains("a number"))
    }

    @Test
    fun everyQueryErrorIsSaidInWordsOfItsOwn() {
        val words = QueryErrorCode.entries.associateWith { QueryStringsEnglish.errorOf(it) }
        assertTrue(words.values.all { it.isNotBlank() }, "$words")
        assertEquals(words.size, words.values.toSet().size, "each its own: $words")
    }

    @Test
    fun theSummarysSizesAreTheTextsTheMinifiedAndNotYetTheGzipped() {
        val document = assertNotNull(Order.document)
        val minified = assertNotNull(minifiedOf(document))
        assertTrue(minified.length < document.text.length && '\n' !in minified.trimEnd())
        val sizes = sizesOf(document, minified)
        assertEquals(document.text.encodeToByteArray().size.toLong(), sizes.text)
        assertEquals(minified.encodeToByteArray().size.toLong(), sizes.minified)
        assertEquals(null, sizes.compressed)
    }

    @Test
    fun everyColumnTypeHasItsWord() = wordsOf({ columnTypeWords() }) { words ->
        assertEquals(ColumnType.entries.toSet(), words.keys)
        assertTrue(words.values.all { it.isNotBlank() }, "$words")
    }
}
