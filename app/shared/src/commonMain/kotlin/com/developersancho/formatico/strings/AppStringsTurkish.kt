package com.developersancho.formatico.strings

/** [AppStrings] in Turkish. */
internal object AppStringsTurkish : AppStrings {
    override val appName = "Formatico"
    override val newTab = "Yeni sekme"
    override val searchOrRunACommand = "Ara ya da bir komut çalıştır"
    override val useDarkTheme = "Koyu temayı kullan"
    override val useLightTheme = "Açık temayı kullan"
    override val settings = "Ayarlar"
    override val untitled = "Adsız"
    override val changesNotSaved = "dosyasına kaydedilmemiş değişiklikleri var"

    override fun switchDocuments(first: String, second: Int) = "$first, açık $second belgeden biri. Belgeler arasında geçiş yap"

    override fun switchDocumentsFromNewTab(count: Int, first: Int) = "Yeni sekme, açık $first belgenin yanında. Belgeler arasında geçiş yap"

    override fun openDocuments(count: Int, first: Int) = "$first açık belge"

    override val palettePlaceholder = "Bir komut ya da biçim yaz"
    override val paletteEmpty = "Bu sözcüklerle bir komut yok"
    override val paletteNewDocument = "Yeni belge"
    override val paletteOpenFile = "Dosya aç"
    override val palettePaste = "Yeni belge olarak yapıştır"
}
