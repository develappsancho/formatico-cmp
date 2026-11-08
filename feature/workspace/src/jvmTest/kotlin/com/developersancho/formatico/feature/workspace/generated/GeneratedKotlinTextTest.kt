package com.developersancho.formatico.feature.workspace.generated

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.types.TypeLanguage
import com.developersancho.formatico.core.format.types.generateTypes
import com.developersancho.formatico.feature.workspace.PreviewBoardOrder
import com.developersancho.formatico.feature.workspace.Reading
import com.developersancho.formatico.feature.workspace.dataValueOf
import com.developersancho.formatico.feature.workspace.readDocument
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The compiled Kotlin of [GeneratedKotlinTest] is the generator's text of the boards' order, word for word: what the
 * file holds after its package. Read from the module's sources, which a JVM's test runs in.
 */
class GeneratedKotlinTextTest {
    @Test
    fun theCompiledKotlinIsWhatTypesWritesOfTheOrder() {
        val read = readDocument(PreviewBoardOrder, Format.Json) as Reading.Read
        val written = generateTypes(dataValueOf(checkNotNull(read.root), Format.Json), TypeLanguage.Kotlin, "Order")?.text
        val file = File("src/commonTest/kotlin/com/developersancho/formatico/feature/workspace/generated/GeneratedOrder.kt").readText()

        assertEquals(file.substringAfter("package com.developersancho.formatico.feature.workspace.generated\n\n"), written)
    }
}
