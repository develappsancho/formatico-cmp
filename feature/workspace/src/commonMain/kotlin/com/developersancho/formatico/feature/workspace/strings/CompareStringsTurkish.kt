package com.developersancho.formatico.feature.workspace.strings

/** [CompareStrings] in Turkish. */
@Suppress("TooManyFunctions") // A sentence that holds values is a function of them.
internal object CompareStringsTurkish : CompareStrings {
    override val a = "A"
    override val b = "B"

    override fun bMenu(first: String) = "B: $first. Başka bir belge seç"

    override val by = "Karşılaştırma"
    override val byMeaning = "Anlam"
    override val byText = "Metin"

    override fun changed(count: Int, first: Int) = "$first değişti"

    override fun added(count: Int, first: Int) = "$first eklendi"

    override fun removed(count: Int, first: Int) = "$first çıkarıldı"

    override val stop = "Karşılaştırmayı bitir"

    override fun differences(count: Int, first: Int) = "$first fark"

    override val noDifferences = "Fark yok"
    override val sameMeaning = "A ile B aynı anlama geliyor."
    override val sameText = "A ile B aynı metin."
    override val changeChanged = "Değişti"
    override val changeAdded = "Eklendi"
    override val changeRemoved = "Çıkarıldı"

    override fun what(first: String, second: String) = "$first → $second"

    override fun line(first: Int) = "Satır $first"

    override fun lines(first: Int, second: Int) = "Satır $first–$second"

    override val notCounted = "Fark sayılmayanlar"

    override fun movedPair(first: String, second: String) = "Anahtarların sırası: $first ile $second yer değiştirdi."

    override fun moved(count: Int, first: String, second: Int) =
        if (count == 1) {
            "Anahtarların sırası: $first yer değiştirdi."
        } else {
            "Anahtarların sırası: $first ve $second anahtar daha yer değiştirdi."
        }

    override fun respelledAlone(first: String, second: String) = "Sayıların yazılışı: $first ile $second aynı sayı."

    override fun respelledAfter(first: String, second: String) = "Bir de aynı sayı olan $first ile $second."

    override fun respelledMore(count: Int, first: String, second: String, third: Int) =
        "Bir de aynı sayı olan $first ile $second ve $third sayı daha."

    override fun notCountedBoth(first: String, second: String) = "$first $second"

    override fun differenceOf(first: Int, second: Int) = "Fark $first / $second"

    override val byMeaningNote = "Anlama göre karşılaştırıldı: anahtarların sırası ve sayıların yazılışı fark sayılmıyor"
    override val byTextNote = "Metne göre karşılaştırıldı: her karakter hesaba katılıyor"

    override fun sideShort(first: String, second: String) = "$first · $second"

    override val title = "Karşılaştır"
    override val show = "Göster"

    override fun showIn(first: String) = "$first değerini A ile B’de göster"

    override val inA = "A’da"
    override val inB = "B’de"
    override val notThere = "yok"
    override val aloneTitle = "Karşılaştırılacak bir belge yok"
    override val aloneBody = "Yeni bir sekmede başka bir belge aç, bunu onunla karşılaştır."

    override fun unreadTitle(first: String) = "$first belgesinde anlama göre karşılaştırılacak bir şey yok"

    override val unreadBody = "Düz metin ya da hiçbir yeri okunamadı: bunun yerine metne göre karşılaştır."

    override fun invalidTitle(count: Int, first: String, second: Int) =
        if (count == 1) {
            "$first belgesinde bir hata var"
        } else {
            "$first belgesinde $second hata var"
        }

    override val invalidBody = "Anlama göre karşılaştırmak için onar ya da metne göre karşılaştır."
}
