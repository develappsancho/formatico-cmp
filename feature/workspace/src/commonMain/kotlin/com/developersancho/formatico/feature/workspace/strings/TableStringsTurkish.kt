package com.developersancho.formatico.feature.workspace.strings

/** [TableStrings] in Turkish. */
internal object TableStringsTurkish : TableStrings {
    override val typeText = "metin"
    override val typeDate = "tarih"
    override val typeNumber = "sayı"
    override val typeBoolean = "mantıksal"
    override val typeObject = "nesne"
    override val typeArray = "dizi"
    override val typeAlias = "takma ad"
    override val typeNull = "null"
    override val firstRowHeader = "İlk satır başlık"
    override val firstRowHeaderOff = "Kapalıyken ilk satır bir kayıttır, sütunlar da sütun 1, sütun 2… diye adlandırılır"

    override fun columnAt(place: Int) = "sütun $place"
}
