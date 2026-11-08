package com.developersancho.formatico.feature.workspace.strings

/** [ShareStrings] in Turkish. */
@Suppress("TooManyFunctions") // A sentence that holds values is a function of them.
internal object ShareStringsTurkish : ShareStrings {
    override fun title(first: String) = "$first belgesini paylaş"

    override val ways = "Nasıl paylaşılsın"
    override val inLink = "Bağlantının içinde"
    override val encrypted = "Şifreli bir bağlantı olarak"
    override val inLinkBody =
        "Bağlantı belgenin kendisini sıkıştırılmış olarak taşır. Hiçbir sunucuya gönderilmez: bağlantısı olan herkes onu okuyabilir."
    override val encryptedBody =
        "Belgeye kısa bir bağlantı: belge bu cihazda şifrelenir ve süresi dolana dek Formatico’nun sunucusunda tutulur."
    override val encryptedBodyPhone =
        "Belgeye kısa bir bağlantı: belge bu telefonda şifrelenir ve süresi dolana dek Formatico’nun sunucusunda tutulur."
    override val link = "Bağlantı"

    override fun fitsShort(first: String, second: String, third: String) =
        "$first $second, $third karakterlik bir bağlantıda: her tarayıcı ve uygulama için yeterince kısa."

    override fun fitsShortPhone(first: String, second: String, third: String) = "$first $second, $third karakterlik bağlantıda"

    override fun fitsLong(first: String, second: String, third: String) =
        "$first $second, $third karakterlik bir bağlantıda: tarayıcılar açar, ama bir sohbet ya da e-posta uygulaması bu kadar " +
            "uzun bir bağlantıyı kesebilir."

    override val plainText = "metin"
    override val formatted = "Bağlantıdaki belgeyi biçimlendir"
    override val formattedEncrypted = "Belgeyi şifrelemeden önce biçimlendir"
    override val keepFor = "Ne kadar tutulsun"

    override fun days(count: Int) = "$count gün"

    override val trust =
        "Formatico’nun sunucusu yalnızca şifreli belgeyi tutar ve onu okuyamaz: anahtar yalnızca bağlantıdadır. Bağlantısı " +
            "olan herkes belgeyi okuyabilir."
    override val unreachableTitle = "Formatico’nun sunucusuna ulaşılamadı"
    override val unreachableBody = "Belge gönderilmedi. Yeniden dene ya da bağlantının içinde paylaş."
    override val unreachableBodyEncryptedOnly = "Belge gönderilmedi. Yeniden dene."
    override val busyTitle = "Formatico’nun sunucusu şu an paylaşım almıyor"
    override val busyBody = "Belge gönderilmedi. Sonra yeniden dene ya da bağlantının içinde paylaş."
    override val busyBodyEncryptedOnly = "Belge gönderilmedi. Sonra yeniden dene."
    override val dailyLimitTitle = "Bu ağdan bugün başka şifreli bağlantı yapılamıyor"

    override fun dailyLimitBody(count: Int) = "Formatico’nun sunucusu bir ağdan günde en çok $count tane alıyor."

    override val dailyLimitLater = "Yarın yeniden dene ya da bağlantının içinde paylaş."
    override val dailyLimitLaterEncryptedOnly = "Yarın yeniden dene."

    override val deleteFailed = "Bağlantı silinmedi. Yeniden dene."
    override val cancel = "Vazgeç"
    override val copy = "Bağlantıyı kopyala"
    override val send = "Başka bir uygulamayla gönder"
    override val create = "Bağlantıyı oluştur"
    override val creating = "Bağlantı oluşturuluyor"
    override val tryAgain = "Yeniden dene"
    override val done = "Tamam"
    override val delete = "Bağlantıyı sil"

    override fun kept(first: String, second: String) =
        "$first tarihine dek şifreli olarak tutulur. $second karakter: her tarayıcı ve uygulama için yeterince kısa."

    override fun keptPhone(first: String) = "$first tarihine dek şifreli olarak tutulur"

    override fun encryptedOnlyBody(first: String, second: String) =
        "$second ile $first bir bağlantıya sığmayacak kadar büyük; bir bağlantı yaklaşık 32 KB taşır. Bunun yerine şifreli " +
            "bir bağlantı olarak gidebilir."

    override fun tooLargeTitle(first: String) = "$first paylaşmak için çok büyük"

    override fun tooLargeBody(first: String) =
        "Bir bağlantı yaklaşık 32 KB, şifreli bir bağlantı en çok 10 MB taşır, bu belge ise $first. Bunun yerine dosya olarak " +
            "kaydedip dosyayı gönder."

    override val linkLabel = "Bağlantı"
    override val linkCopied = "Bağlantı kopyalandı"
    override val linkCopyFailed = "Bağlantı kopyalanamadı"
}
