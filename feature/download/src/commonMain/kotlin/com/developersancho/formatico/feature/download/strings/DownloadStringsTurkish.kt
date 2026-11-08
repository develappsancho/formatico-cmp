package com.developersancho.formatico.feature.download.strings

/** [DownloadStrings] in Turkish. */
@Suppress("TooManyFunctions") // A sentence that holds values is a function of them.
internal object DownloadStringsTurkish : DownloadStrings {
    override val getTheApp = "Uygulamayı indir"
    override val download = "İndir"

    override fun release(first: String, second: String) = "Formatico $first · $second tarihinde çıktı"

    override fun releaseUndated(first: String) = "Formatico $first"

    override val formatico = "Formatico"
    override val macOs = "macOS"
    override val macOsFor = "Apple silikonlu Mac'ler"
    override val windows = "Windows"
    override val windowsFor = "64 bit Windows 10 ve sonrası"
    override val android = "Android"
    override val androidFor = "Android 11 ve sonrası"

    override fun file(first: String, second: String, third: String) = "$first · $second · $third"

    override val notInRelease = "Bu sürümde henüz yok"
    override val allReleases = "GitHub'daki bütün sürümler"
    override val allReleasesBody = "Notları ve SHA-256 özetleriyle"
    override val looking = "Son sürüme bakılıyor"
    override val lookingBody = "GitHub'da"
    override val unreachable = "GitHub'a ulaşılamadı"
    override val unreachableBody = "Sürümler sayfasında bütün dosyalar var"
    override val noRelease = "GitHub'da henüz sürüm yok"
    override val noReleaseBody = "İlki çıkınca sürümler sayfasında olur"
    override val openReleases = "Sürümler sayfasını aç"

    override fun downloading(first: String, second: String) = "$first GitHub'dan indiriliyor. SHA-256'sı $second."

    override fun downloadingUnchecked(first: String) = "$first GitHub'dan indiriliyor."

    override val copySha = "SHA-256'yı kopyala"
    override val shaCopied = "SHA-256 kopyalandı"
    override val shaNotCopied = "SHA-256 kopyalanamadı"
}
