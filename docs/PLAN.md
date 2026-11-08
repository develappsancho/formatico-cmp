# Plan

Formatico'nun fazları, sahibinin onayıyla (2026-11-03). Formatico'nun eksikleri
[`OPEN_ITEMS.md`](OPEN_ITEMS.md)'de, Pantrix UI'ınkiler [`PANTRIX_FINDINGS.md`](PANTRIX_FINDINGS.md)'de; bu dosya
yalnızca sırayı tutar.

Bir faz bitince işaretlenir, hangi commit'lerle bittiği yazılır. Her fazın sonunda: ilgili panolar 1440, 834 ve 390'da
bire bir karşılanmış; bütün hedefler derleniyor ve testler geçiyor; bulgular ve açık maddeler yazılmış; Türkçe bir rapor
ve bir commit mesajı. Bir fazı `tıkıyor` diye işaretlenmiş bir bulgu durdurursa önce o düzeltilir, yerel paket yenilenir.

## Bitenler

*   [x] **Faz 0: Kurulum.** Sihirbazın projesi Pantrix'e taşındı: Teal preset, Geist ve Geist Mono, convention
    eklentileri, `core:base`, `core:designsystem`, `core:format`, Koin, navigation3, detekt ve Kotlinter; web Wasm'da,
    JavaScript'e dönüşüyle. `53ee107`.
*   [x] **Faz 1: Tasarım.** Kimlik panoları ve v1'in 11 ekranı 1440, 834 ve 390'da (33 pano); Summary düzen'den
    sonra. `922a17f`, `b1a2619`, `419804d`, `236d4bd`, `6b783ac`.
*   [x] **Faz 2: Start ve Workspace.** Start: aç, yapıştır, bırak, örnekler, Recent. Workspace: sekmeler, araç
    çubuğu, ağaç, yol izi, durum çubuğu, sözcükle değer önizlemesi, Format, Minify, Sort keys ve Undo. `23292c8`,
    `01b88d6`.
*   [x] **Faz 3: İlk biçimler.** JSON, JSONC ve JSON5 kayıpsız okunup yazılıyor; YAML `snakeyaml-engine-kmp` ile
    okunup Formatico'nun yazıcısıyla yazılıyor; CSV ve TSV okunuyor, Table'da açılıyor. `01b88d6`, `af5b894`, `be7a6e0`.

Pantrix 0.1.2-SNAPSHOT'a geçiş: `516f99d`, `c235d27`.

### Faz 0–3'ün denetimi (2026-11-03)

Kod; CLAUDE.md, kanvasın güncel Start, Workspace ve Table panoları, `OPEN_ITEMS.md` ve `PANTRIX_FINDINGS.md` ile
karşılaştırıldı. Panolarla karşılaştırma etiketler ve metinler üzerinden yapıldı, ekranlar yan yana açılmadı. Çıkan
eksikler Faz 4'te.

## v1

*   [ ] **Faz 4: Bitenlerin eksikleri.**
    *   Kurulum ve test: Roborazzi ve Maestro; tarayıcıda atlanan 19 UI testi; testler arasında kapanmayan Koin
        bağlamı; web'de derleyen makinenin yolları ve konsoldaki hata satırı; iOS'un beyaz ilk karesi; uygulama
        simgeleri; Kotlin kaynaklarında görünmeyen karakter denetimi. Yapıldı (2026-11-03); simgeler 2026-11-04'te,
        tuvalin simge panosundan. Compose'un konsol satırı Compose'un sürümünü bekliyor.
    *   Start: Recent, açık sekmeler ve tema seçimi cihazda saklanıyor; Recent "edited" ile "opened"ı ayırıyor;
        telefonda açık belgelere dönen değiştirici (önce Start-390'a çizilir); Start panolarındaki 742 B, 665 B olur.
        Yapıldı (2026-11-04): `core:storage` (KSafe ve kendi metin deposu), tuval sürüm 70.
    *   Workspace: ağaç seçili satıra kaydırılıyor; panolardaki erişilebilirlik etiketleri ("Document tree",
        "Sort by", kayıt listesi, sekme kapatma); Minify yorumları sileceğini önceden söylüyor; sondaki virgül ve
        anahtarla değer arasındaki yorum korunuyor; iOS'ta üst çubuk içerikten önce değişmiyor. Yapıldı (2026-11-04),
        ağacın kaydırılması `tıkıyor` (Pantrix), bölge adlarının Compose'da karşılığı yok.
    *   YAML ve CSV: YAML'da bütün hatalar, yorum ve biçim kuralları, anchor'ının önüne düşen alias, birleştirme
        anahtarı ve YAML 1.1 değerleri; CSV'nin başlık ve sayı tahmini, Format'ın kapsamı, tablonun süzgecinin
        kalması, telefonun kaydının başlığı. Yapıldı (2026-11-04): tireden sonraki yorum, baştaki yorumlar, `...`, kod
        sütunları, süzgeç, kaydın başlığı. YAML'ın hataları Faz 6'ya, YAML 1.1 ve "ilk satır başlık" Settings'e kaldı.
    *   Pantrix: 0.1.2'nin kapattığı bulgular işaretlenir; işi tıkayanlar `tıkıyor` olur (ağaç satırında önizleme,
        iki metni karşılaştıran görünüm, web'de panodan okuma, bölmelerin ayırıcısı). Yapıldı (2026-11-04): ağaç,
        ayırıcı ve panodan okuma `tıkıyor`; karşılaştırma Faz 11'de yeniden bakılacak.
    *   Web'de gezinti (sahibinin isteği, 2026-11-04): navigation3'ün geri yığını tarayıcının geçmişine
        `ChronologicalBrowserNavigation` ile bağlanır (https://github.com/terrakok/navigation3-browser): tarayıcının geri
        ve ileri tuşları ve adres çubuğu uygulamanın ekranlarını izler. Önce kütüphanenin kodu incelenir; bağımlılık mı
        olacağı, kodun projeye mi alınacağı (lisansıyla) sahibinin onayıyla seçilir, indirme de onaya bağlı.
        Dikkat: v1'in paylaşımı belgeyi bağlantının fragment'ında taşıyor (Faz 13); kütüphanenin `main`'i de yolu
        fragment'a yazıyor gibi. Birleşmemiş PR #9 (tam URL yolları, `ChronologicalBrowserRouteNavigation`, MIT) bu
        çakışmayı çözer, ama yolların sunucuda `index.html`'e yönlendirilmesini ister ve henüz incelenmemiş.
        Yapıldı (2026-11-04): 1.1.0'ın kronolojik bağlantısı projeye alındı (sahibinin "kodu içeri al" kararı; MIT
        lisansı web dağıtımıyla gidiyor), `core:navigation`'da. Adres `#start` ve `#document?id=3`; yığın geçmişin
        her girdisinin `state`'inde. Fragment çakışması Faz 13'e kaldı (OPEN_ITEMS).
    *   Önizlemeler ve testler (sahibinin kuralı, 2026-11-04; CLAUDE.md): bugüne kadarki her ekrana ve bileşene
        (Start, Workspace, Table, çubuk, değiştirici, kayıt ve ötekiler) her durumu içeren `PreviewParameterProvider`'lı
        önizlemeler; aynı durumların ekran görüntüsü testleri; eksik Compose UI ve birim testleri.
        Start yapıldı (2026-11-04): `core:designsystem`'de önizlemenin parçaları (`Board`, `BoardPreviews`,
        `PreviewCases`, `FormaticoPreview`), Roborazzi her çizen modülde; Start'ın dört hâli üç panoda, açık ve koyu (24
        görüntü), yeni UI testleri. Workspace ve çubuk yapıldı (2026-11-04): Workspace'in sekiz hâli üç panoda (48
        görüntü), sekiz bileşenin kendi hâlleri (58), çubuğun on hâli (20), uygulamanın panoları koyu temada da (12);
        boş belge, sayı sütununun hizası ve hâllerin UI testleri. Menülerin açık hâli kaldı (OPEN_ITEMS).
*   [ ] **Faz 5: Metin editörü.** `PantrixCodeEditor` ve Formatico'nun renklendiricisi; metin ile ağacın seçimi iki
    yönde; Ln ve Col durum çubuğunda ve Table'da; telefonda "Show in text"; belge başına geri alma geçmişi; bul ve
    değiştir; sütun editörün saydığı birimle.
*   [ ] **Faz 6: Geçersiz belge.** Satırlarda işaretler, bütün problemlerin listesi, her probleme kendi onarımı.
*   [ ] **Faz 7: Kalan biçimler.** NDJSON, XML, TOML, properties ve INI: kayıpsız okuyucu, yazıcı ve ağaç.
*   [ ] **Faz 8: Ağaç ve tablo.** Ağaçta arama, bir düzeye kadar açma, yolu JSONPath, JSON Pointer ya da jq olarak
    kopyalama; sütun türleri; tabloyu kaydetme ve kopyalama menüsü; renk ve resim önizlemesi; Summary (v1'deyse).
*   [ ] **Faz 9: Query.** JSONPath ve XPath.
*   [ ] **Faz 10: Convert.** Biçimler arasında; kaybolacak olan önceden söylenir.
*   [ ] **Faz 11: Compare.** Anlamsal karşılaştırma.
*   [ ] **Faz 12: Komut paleti, kısayollar ve Settings.** Faz 4'ün cihazda saklamasının üstünde.
*   [ ] **Faz 13: Paylaşım.** Küçük bir belge sıkıştırılıp bağlantının fragment'ında; sunucu yok.
*   [ ] **Faz 14: Büyük dosyalar.** Masaüstünde 50 MB, telefonda ve web'de 10 MB, ölçülerek.
*   [ ] **Faz 15: v1'in testi.** Roborazzi her panoyla, Maestro akışları Android ve iOS'ta, tarayıcı testleri;
    Türkçe ve İngilizce metinler, erişilebilirlik.

## v2

Her v2 ekranı, kodundan önce kanvasta üç genişlikte çizilir.

*   [ ] **Faz 16: Sunucu.** Spring Boot: uçtan uca şifreli paylaşım bağlantıları ve URL vekili.
*   [ ] **Faz 17: Tip üretimi.** Kotlin, TypeScript, Swift ve Go.
*   [ ] **Faz 18: Grafik görünümü.**
*   [ ] **Faz 19: JSON Schema.**
*   [ ] **Faz 20: Uygulama içi yapay zekâ,** kullanıcının kendi anahtarıyla.

## Kapanış

*   [ ] **Faz 21: Tam denetim.** Faz 0'dan 20'ye her şey yeniden denetlenir: CLAUDE.md, bütün panolar,
    `OPEN_ITEMS.md` ve `PANTRIX_FINDINGS.md`. Eksik kalan her şey bu fazda kapanır.
*   [ ] **Faz 22: Yayın.** Pantrix'in gerçek yayını; Formatico'nun yayımlanan sürüme dönmesi (`mavenLocal` bloğu
    kalkar, `pantrix-ui@pantrix-ui` açılır, `pantrix-ui-local` kaldırılır); GitHub Packages'tan gelen paketle bütün
    hedeflerde yeniden derleme ve testler; `formatico.pantrixui.com`'un ve v2 sunucusunun yayını.

## Sahibinin kararını bekleyenler

*   **Summary v1'de mi?** CLAUDE.md'nin v1 kapsamı onu anmıyor; şimdilik Faz 8'de.
*   **Paylaşım:** CLAUDE.md fragment'ta paylaşımı v1'de, "share links"i v2'de anlatıyor. Şimdilik fragment
    bağlantısı Faz 13'te, şifreli bağlantılar Faz 16'da.
