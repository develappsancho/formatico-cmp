package com.developersancho.formatico.feature.workspace.strings

import com.pantrixui.foundation.l10n.PantrixStrings

/** [SchemaStrings] in Turkish. */
@Suppress("TooManyFunctions") // A sentence that holds values is a function of them.
internal object SchemaStringsTurkish : SchemaStrings {
    override val pantrix = PantrixStrings.Turkish

    override val schema = "Şema"
    override val against = "Şema"

    override fun validateAgainst(first: String) = "$first belgesinin şeması"

    override fun title(first: String) = "$first belgesini doğrula"

    override fun chooserLabel(first: String) = "$first şemasına göre doğrula. Başka bir şema seç"

    override val chooseSchemaLabel = "Bir şemaya göre doğrula. Bir şema seç"
    override val noSchema = "Şema yok"
    override val chooseFile = "Bir dosya seç"

    override fun openAddress(first: String) = "$first adresini aç"

    override fun failedTitle(count: Int, first: Int, second: String) = "$second şemasına göre $first sorun"

    override fun failedBody(first: String, second: String) = "$first geçerli bir $second belgesi, ama şemanın istediği gibi değil."

    override fun listedFirst(first: Int, second: Int) = "$second sorunun ilk $first tanesi listeleniyor."

    override fun validTitle(first: String) = "$first şemasına göre geçerli"

    override fun validBody(first: String, second: String) = "$first belgesinin her değeri şemanın istediği gibi (JSON Schema $second)."

    override val noneTitle = "Bir şema seç"

    override fun noneBody(first: String) =
        "Açık JSON Schema belgelerinden biri ya da bir dosya. $first kendi \$schema alanında bir şema adlandırmıyor."

    override fun namedBody(first: String, second: String) =
        "$first, \$schema alanında şemasını adlandırıyor: $second. Menüden aç ya da başka birini seç."

    override fun unusableTitle(first: String) = "$first şema olarak kullanılamıyor"

    override fun unknownDraft(first: String) =
        "\$schema alanı $first adresini adlandırıyor; Formatico bu sürümü tanımıyor: 2020-12, 2019-09, draft-07, draft-06 ya da draft-04."

    override fun unresolvedAt(first: Int, second: String, third: String) =
        "$first. satırdaki \$ref, $second, hiçbir yeri göstermiyor. Onu kendi sekmesinde düzelt, $third yeniden doğrulanır."

    override fun unresolved(first: String, second: String) =
        "\$ref $first hiçbir yeri göstermiyor. Onu kendi sekmesinde düzelt, $second yeniden doğrulanır."

    override fun notASchemaAt(first: Int, second: String, third: String, fourth: String) =
        "$first. satırdaki $second, JSON Schema $third sürümünün orada izin verdiği bir değer değil. Onu kendi sekmesinde düzelt, " +
            "$fourth yeniden doğrulanır."

    override fun notASchema(first: String, second: String) =
        "JSON Schema $first sürümünün izin verdiği gibi değil. Onu kendi sekmesinde düzelt, $second yeniden doğrulanır."

    override fun unreadable(first: String) = "Doğrulayıcı onu okuyamadı: $first"

    override fun unsupportedTitle(first: String) = "$first için şema yok"

    override fun unsupported(first: String) =
        "JSON Schema; JSON, YAML ve TOML belgelerini doğrular: bir $first belgesinin değerleri hep metin."

    override fun invalid(count: Int, first: String, second: Int) =
        if (count == 1) {
            "$first belgesinde 1 hata var. Belgeyi bir şemaya göre doğrulamak için onu onar."
        } else {
            "$first belgesinde $second hata var. Belgeyi bir şemaya göre doğrulamak için onları onar."
        }

    override val theDocument = "Belge"
    override val anObject = "bir nesne"
    override val anArray = "bir dizi"
    override val aString = "bir dizgi"
    override val aNumber = "bir sayı"
    override val anInteger = "bir tam sayı"
    override val trueOrFalse = "true ya da false"
    override val nullValue = "null"

    override fun type(first: String, second: String, third: String) = "$first $second olmalı, ama $third."

    override fun requiredOne(first: String, second: String) = "$first içinde $second yok; şema bunu istiyor."

    override fun requiredMany(first: String, second: String) = "$first içinde $second yok; şema bunları istiyor."

    override fun dependentRequired(first: String, second: String, third: String) = "$first içinde $second var, bu yüzden $third de olmalı."

    override fun enumeration(first: String, second: String, third: String) = "$first şunlardan biri olmalı: $second; ama $third."

    override fun constant(first: String, second: String, third: String) = "$first $second olmalı, ama $third."

    override fun atLeast(first: String) = "en az $first"

    override fun moreThan(first: String) = "$first değerinden büyük"

    override fun atMost(first: String) = "en çok $first"

    override fun lessThan(first: String) = "$first değerinden küçük"

    override fun bound(first: String, second: String, third: String) = "$first $second olmalı, ama $third."

    override fun multipleOf(first: String, second: String, third: String) = "$first, $second sayısının bir katı olmalı, ama $third."

    override fun minLength(count: Int, first: String, second: String, third: String) = "$first en az $second karakter olmalı, ama $third."

    override fun maxLength(count: Int, first: String, second: String, third: String) = "$first en çok $second karakter olmalı, ama $third."

    override fun pattern(first: String, second: String, third: String) = "$first $second kalıbına uymalı, ama $third."

    override fun format(first: String, second: String, third: String) = "$first $second biçiminde yazılmalı, ama $third."

    override fun minItems(count: Int, first: String, second: String) = "$first en az $second öğe taşımalı."

    override fun maxItems(count: Int, first: String, second: String) = "$first en çok $second öğe taşımalı."

    override fun uniqueItems(first: String) = "$first öğeleri birbirinden farklı olmalı."

    override fun contains(first: String) = "$first şemanın tarif ettiği bir öğe taşımalı."

    override fun minKeys(count: Int, first: String, second: String) = "$first en az $second anahtar taşımalı."

    override fun maxKeys(count: Int, first: String, second: String) = "$first en çok $second anahtar taşımalı."

    override fun notAllowed(first: String) = "$first burada olamaz."

    override fun not(first: String) = "$first şemanın dışladığı bir değer olmamalı."

    override fun noChoice(first: String, second: Int, third: String) = "$first, $third altındaki $second şemanın hiçbirine uymuyor."

    override fun manyChoices(first: String) = "$first, oneOf altındaki şemaların birden çoğuna uyuyor; yalnızca birine uymalı."

    override fun other(first: String, second: String) = "$first şemanın $second kuralına uymuyor."

    override fun statusProblems(count: Int, first: Int) = "$first şema sorunu"

    override val statusValid = "Şemaya göre geçerli"
    override val groupSchema = "Şema"
    override val validate = "Bir şemaya göre doğrula"
}
