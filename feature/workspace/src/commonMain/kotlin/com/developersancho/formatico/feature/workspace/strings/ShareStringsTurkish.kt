package com.developersancho.formatico.feature.workspace.strings

/** [ShareStrings] in Turkish. */
internal object ShareStringsTurkish : ShareStrings {
    override fun title(first: String) = "$first belgesini paylaş"

    override val body =
        "Bağlantı belgenin kendisini sıkıştırılmış olarak taşır. Hiçbir sunucuya gönderilmez: bağlantısı olan herkes belgeyi okuyabilir."
    override val bodyPhone =
        "Bağlantı belgenin kendisini sıkıştırılmış olarak taşır. Hiçbir sunucuya gönderilmez: bağlantısı olan herkes onu okuyabilir."
    override val link = "Bağlantı"

    override fun fitsShort(first: String, second: String, third: String) =
        "$first $second, $third karakterlik bir bağlantıda: her tarayıcı ve uygulama için yeterince kısa."

    override fun fitsShortPhone(first: String, second: String, third: String) = "$first $second, $third karakterlik bağlantıda"

    override fun fitsLong(first: String, second: String, third: String) =
        "$first $second, $third karakterlik bir bağlantıda: tarayıcılar açar, ama bir sohbet ya da e-posta uygulaması bu kadar " +
            "uzun bir bağlantıyı kesebilir."

    override val plainText = "metin"
    override val formatted = "Bağlantıdaki belgeyi biçimlendir"
    override val cancel = "Vazgeç"
    override val copy = "Bağlantıyı kopyala"
    override val send = "Başka bir uygulamayla gönder"

    override fun tooLargeTitle(first: String) = "$first bir bağlantı için çok büyük"

    override fun tooLargeBody(first: String) =
        "Bir bağlantı yaklaşık 32 KB metin taşır, bu belge ise $first. Bunun yerine dosya olarak kaydedip dosyayı gönder."

    override val linkLabel = "Bağlantı"
    override val linkCopied = "Bağlantı kopyalandı"
    override val linkCopyFailed = "Bağlantı kopyalanamadı"
}
