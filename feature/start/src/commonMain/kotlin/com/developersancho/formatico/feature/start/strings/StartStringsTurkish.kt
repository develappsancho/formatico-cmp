package com.developersancho.formatico.feature.start.strings

/** [StartStrings] in Turkish. */
@Suppress("TooManyFunctions") // A sentence that holds values is a function of them.
internal object StartStringsTurkish : StartStrings {
    override val title = "Ne varsa yapıştır"
    override val lead =
        "JSON, XML, YAML, TOML, CSV ya da properties: Formatico hangisi olduğunu anlar, denetler ve düzenler. Metnin bu cihazdan " +
            "hiç çıkmaz."
    override val leadShort = "JSON, XML, YAML, TOML, CSV ya da properties. Metnin bu cihazdan hiç çıkmaz."
    override val paste = "Panodan yapıştır"

    override fun pasteHint(first: String) = "ya da bu ekranın herhangi bir yerinde $first tuşlarına bas"

    override val newEmptyDocument = "Yeni boş belge"
    override val newDocument = "Yeni belge"
    override val openFile = "Dosya aç"
    override val dropAFile = "Bir dosyayı buraya bırakın"
    override val chooseAFile = "Bir dosya seçin"
    override val clipboardEmpty = "Panodan metin okunamadı"

    override fun pasteTooLarge(first: String) = "Yapıştırılan metin $first sınırından büyük"

    override val linkBroken = "Bu bağlantıda Formatico’nun okuyabileceği bir belge yok. Yarıda kesilmiş olabilir."
    override val clipboardUnreadable = "Bu tarayıcı Formatico’nun panoyu okumasına izin vermiyor. Klavyeyle yapıştır ya da bir dosya aç."

    override fun fileDropSupporting(first: String) =
        "JSON, JSONC, JSON5, NDJSON, XML, YAML, TOML, CSV, TSV, properties ve INI, en çok $first"

    override fun fileTooLarge(first: String, second: String) = "$first, $second sınırından büyük"

    override fun fileWrongType(first: String) = "$first, Formatico’nun okuduğu bir metin türü değil"

    override fun fileUnreadable(first: String) = "$first okunamadı"

    override val recent = "Son açılanlar"
    override val clearHistory = "Geçmişi temizle"
    override val recentEmptyTitle = "Henüz bir şey açılmadı"
    override val recentEmptyText = "Yapıştırdığın, açtığın ya da bıraktığın her şey burada listelenir ve bu cihazda kalır."
    override val untitled = "Adsız"
    override val plainText = "Metin"

    override fun recentMeta(first: String, second: String, third: String) = "$first · $second · $third"

    override val editedJustNow = "son düzenleme: az önce"

    override fun editedMinutesAgo(count: Int, first: Int) = "son düzenleme: $first dakika önce"

    override fun editedTodayAt(first: String) = "son düzenleme: bugün $first"

    override val editedYesterday = "son düzenleme: dün"

    override fun editedOn(first: String) = "son düzenleme: $first"

    override val openedJustNow = "son açılış: az önce"

    override fun openedMinutesAgo(count: Int, first: Int) = "son açılış: $first dakika önce"

    override fun openedTodayAt(first: String) = "son açılış: bugün $first"

    override val openedYesterday = "son açılış: dün"

    override fun openedOn(first: String) = "son açılış: $first"

    override val justNow = "az önce"

    override fun minutesAgo(count: Int, first: Int) = "$first dakika önce"

    override val today = "bugün"
    override val yesterday = "dün"
    override val samples = "Örnekler"
    override val samplesHint = "Formatico’yu denemek için birini aç"
    override val allSamples = "Tüm örnekler"
    override val sampleOrder = "Bir sipariş"
    override val sampleOrderDetail = "Tarihler, renkler, bağlantılar ve bir resim"
    override val sampleOrderDetailShort = "Tarihler, renkler, bağlantılar"
    override val sampleDeployment = "Bir Kubernetes dağıtımı"
    override val sampleDeploymentShort = "Bir dağıtım"
    override val sampleDeploymentDetail = "İç içe listeler ve çapalar"
    override val sampleDeploymentDetailShort = "İç içe listeler"
    override val sampleManifest = "Bir Rust manifesti"
    override val sampleManifestDetail = "Tablolar ve tablo dizileri"
    override val sampleCountries = "Ülkeler"
    override val sampleCountriesDetail = "249 satır, 6 sütun"
    override val privacy =
        "Geçmiş bu cihazda kalır, Formatico hesap da istemez. Bir belge sunucusuna yalnızca şifreli bir bağlantı paylaştığında gider."
    override val privacyWeb =
        "Geçmiş bu cihazda kalır, Formatico hesap da istemez. Bir belge sunucusundan yalnızca şifreli bir bağlantı paylaştığında " +
            "ya da web’de bir web adresi açtığında geçer."
    override val unknownTitle = "Bu hangi biçim?"
    override val unknownPasted = "Formatico yapıştırdığın metnin biçimini anlayamadı. Nasıl okunacağını seç."

    override fun unknownFile(first: String) = "Formatico $first dosyasının biçimini anlayamadı. Nasıl okunacağını seç."

    override val unknownReadAs = "Şöyle oku"
    override val unknownRule = "Ayarlar’daki Biçimler bölümünden bunlardan birini varsayılan yapabilirsin."
    override val unknownLeave = "Metin olarak bırak"
    override val unknownOpen = "Aç"
    override val openAddress = "Bir web adresi aç"
    override val addressMessage = "Formatico herkese açık bir http ya da https adresindeki belgeyi açar."
    override val addressLabel = "Adres"
    override val addressPlaceholder = "https://example.com/orders/8842.json"
    override val addressHelpWeb = "Web’de Formatico’nun sunucusundan gelir; sunucu hiçbir şey tutmaz. En çok 10 MB."

    override fun addressHelp(first: String) = "Formatico onu kendisi indirir. En çok $first."

    override val addressCancel = "Vazgeç"
    override val addressOpen = "Aç"
    override val addressOpening = "Açılıyor"
    override val addressNotAllowed = "Formatico yalnızca herkese açık http ve https adreslerini açar."

    override fun addressNotThere(first: String) = "Adres $first ile cevap verdi: orada bir belge yok."

    override fun addressAnswered(first: String) = "Adres $first ile cevap verdi ve bir belge vermedi."

    override fun addressTooLarge(first: String, second: String) = "Belge $first; Formatico burada en çok $second açar."

    override fun addressTooLargeUntold(first: String) = "Belge, Formatico’nun burada açtığından büyük: $first."

    override val addressNotText = "Bu adresteki şey metin değil; Formatico yalnızca metin açar."
    override val addressUnreachable = "Adrese ulaşılamadı. Adresi denetle ya da yeniden dene."
    override val addressServerUnreachable = "Web’de belgeyi getiren Formatico sunucusuna ulaşılamadı. Yeniden dene."
    override val sharedOpeningTitle = "Paylaşılan bir belge açılıyor"
    override val sharedOpeningMessage = "Formatico’nun sunucusundan alınıyor ve bu cihazda çözülüyor."
    override val sharedOpeningMessagePhone = "Formatico’nun sunucusundan alınıyor ve bu telefonda çözülüyor."
    override val sharedOpeningCancel = "Vazgeç"
    override val sharedLinkGone = "Bu paylaşılan bağlantının süresi dolmuş ya da paylaşan kişi onu silmiş."
    override val sharedLinkUnreachable = "Formatico’nun sunucusuna ulaşılamadı, bu yüzden paylaşılan belge açılamadı."
    override val sharedLinkTryAgain = "Yeniden dene"
}
