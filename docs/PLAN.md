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

*   [x] **Faz 4: Bitenlerin eksikleri.** Kapandı (2026-11-04); iOS'un iki "gözle doğrulanacak" maddesi kaldı.
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
        bölge adlarının Compose'da karşılığı yok; ağacın seçili satıra kaydırılması `PantrixLazyTreeView` ile yapıldı
        (2026-11-04).
    *   YAML ve CSV: YAML'da bütün hatalar, yorum ve biçim kuralları, anchor'ının önüne düşen alias, birleştirme
        anahtarı ve YAML 1.1 değerleri; CSV'nin başlık ve sayı tahmini, Format'ın kapsamı, tablonun süzgecinin
        kalması, telefonun kaydının başlığı. Yapıldı (2026-11-04): tireden sonraki yorum, baştaki yorumlar, `...`, kod
        sütunları, süzgeç, kaydın başlığı. YAML'ın hataları Faz 6'ya, YAML 1.1 ve "ilk satır başlık" Settings'e kaldı.
    *   Pantrix: 0.1.2'nin kapattığı bulgular işaretlenir; işi tıkayanlar `tıkıyor` olur (ağaç satırında önizleme,
        iki metni karşılaştıran görünüm, web'de panodan okuma, bölmelerin ayırıcısı). Yapıldı (2026-11-04): ağaç,
        ayırıcı ve panodan okuma `tıkıyor`; karşılaştırma Faz 11'de yeniden bakılacak. Panodan okuma kapandı
        (2026-11-04): Start, Pantrix'in `readText()`'i ve `onPaste`'iyle okuyor, web'de de.
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
        boş belge, sayı sütununun hizası ve hâllerin UI testleri. Açık menüler, `core:testing`, metinlerin şifrelenmesi,
        sekmenin görünümünün saklanması, tek metin, açılışın ve dönüşün yeniden okumaması da yapıldı (2026-11-04).
*   [x] **Faz 5: Metin editörü.** Yapıldı (2026-11-04); telefonda bul ve değiştirin düğmesi panoyu bekliyor (OPEN_ITEMS). `PantrixCodeEditor` ve Formatico'nun renklendiricisi; metin ile ağacın seçimi iki
    yönde; Ln ve Col durum çubuğunda ve Table'da; telefonda "Show in text"; belge başına geri alma geçmişi; bul ve
    değiştir; sütun editörün saydığı birimle.
*   [x] **Faz 6: Geçersiz belge.** Satırlarda işaretler, bütün problemlerin listesi, her probleme kendi onarımı.
    Yapıldı (2026-11-04): editörde işaretler, Problems görünümü (uyarı, Repair all, Read as JSON5, her problem ve
    onarımı), araç çubuğunda Repair, `core:format`'ta onarımlar; telefonun şeridi ve problem kartı; YAML'ın hatalarının
    kendi kodları, iletileri ve onarımları. İmlecin satırındaki işaret Pantrix'in bulgusunu bekliyor (PANTRIX_FINDINGS).
*   [x] **Faz 7: Kalan biçimler.** NDJSON, XML, TOML, properties ve INI: kayıpsız okuyucu, yazıcı ve ağaç.
    Yapıldı (2026-11-04): beşinin de `core:format`'ta okuyucusu, yazıcısı (Format, Minify; XML dışında Sort keys) ve
    problemleri var. Hepsi workspace'te ağacıyla, iletileriyle ve renklendiricisiyle okunuyor. Onarımlar biçime bağlı.
    "isn’t read yet" hâli kalktı. Ağaçların biçimi panolarda yok, kodda seçildi (OPEN_ITEMS).
*   [x] **Faz 8: Ağaç ve tablo.** Ağaçta arama, bir düzeye kadar açma, yolu JSONPath, JSON Pointer ya da jq olarak
    kopyalama; sütun türleri; tabloyu kaydetme ve kopyalama menüsü; renk ve resim önizlemesi; Summary (sahibinin
    kararıyla v1'de, 2026-11-04: düzen'den gelenlerin hepsi).
    Büyük bölümü yapıldı (2026-11-04): ağaçta arama (sayı, önceki, sonraki), 1, 2, 3 ve
    tümüne açma, yolu üç biçimde kopyalama, tabloyu CSV olarak kaydetme, CSV ya da Markdown olarak kopyalama, Summary
    (üç genişlikte, durum çubuğunun boyutundan; gzip boyu platformun kendi sıkıştırıcısıyla). Telefonda arama ve
    Summary "more" menüsünden açılıyor (sahibinin kararı, 2026-11-04). Ağaç `PantrixLazyTreeView`'e geçti
    (2026-11-04): satırlar sözdizimi renkleriyle, tarih, renk ve resim önizlemesi, aramanın vurgusu, seçili satıra
    kaydırma. Kapandı (2026-11-05): sütun türleri tablonun başlığında, bağlantının ve resmin simgesi ağaçta ve Summary'de;
    ikisi de Pantrix'in tıkayan iki bulgusu düzelince geldi. Ağaçta bağlantının simgesi onu açan düğme (2026-11-05):
    Pantrix'in satır eylemiyle, Shift+Enter ve ekran okuyucunun eylemiyle de.
*   [x] **Faz 9: Query.** JSONPath ve XPath. Yapıldı (2026-11-05): `core:format`'ta RFC 9535'in bütün JSONPath'i ve
    XPath 1.0 (ad alanı çözümü olmadan); Workspace'te Query görünümü üç genişlikte, araç çubuğundan, telefonda More
    menüsünden. JSONPath XML dışındaki her biçimin ağacında, XPath XML'in kendi düğümlerinde; eşleşmeler ağaçta ve
    metinde seçiliyor, sonuçlar kopyalanıyor ya da belge olarak açılıyor, son sorgular cihazda. Eşleşmelerin metinde
    işaretlenmesi Pantrix'i bekliyor (PANTRIX_FINDINGS).
*   [x] **Faz 10: Convert.** Biçimler arasında; kaybolacak olan önceden söylenir. Yapıldı (2026-11-05): `core:format`'ta
    dönüştürücü (`convert`): biçimden bağımsız bir değer, her biçimin yazıcısı ve kaybın on beş türü. Workspace'te Convert
    görünümü üç genişlikte: geniş pencerede metnin yanında, tablette bütün alanda, telefonda kendi ekranı (More
    menüsünden, kendi çubuğu ve geri hareketiyle). Hedef biçim, girinti, anahtarların sırası ve null'ın ne olacağı
    belgenin görünümünde saklanıyor; yeni belge kopyalanıyor, kaydediliyor ya da kendi sekmesinde açılıyor. Panoda
    olmayan hâller ve dönüştürmenin kuralları OPEN_ITEMS'ta.
*   [x] **Faz 11: Compare.** Anlamsal karşılaştırma. Yapıldı (2026-11-05): `core:format`'ta iki karşılaştırma (`diff`):
    değerlerin anlamı (anahtarların sırası ve sayının yazılışı fark değil; dizilerde en uzun ortak dizi) ve satır satır
    metin. Workspace'te Compare açık başka bir belgeyle, biçimleri ayrı olsa da: geniş pencerede farklar ve iki metin yan
    yana, tablette farklar iki metnin üstünde, telefonda kendi ekranında kartlar ve iki metni gösteren bir sayfa. B, anlam
    ya da metin ve gösterilen fark belgenin görünümünde. İki metinde farkların satırları yalnızca kendi tonlarının
    bandında, olukta `~ + −`, değişen değer dolgusuyla; gösterilen fark birincil çubukla ve imleç taşınmadan ekrana
    geliyor (Pantrix'in üç `tıkıyor` bulgusu kapandı). Panodan kalan, tıkamayan bir ayrım bulgularda: işaretsiz ilk
    satırın imleç bandı.
*   [x] **Faz 12: Komut paleti, kısayollar ve Settings.** Faz 4'ün cihazda saklamasının üstünde. Tuvalde eksikler çizildi
    (2026-11-05, sürüm 76): Settings'in Keyboard shortcuts bölümü ve Open-source licences sayfası, telefonda alt kısım,
    Start'ta biçimi anlaşılmayan metnin sorusu ("Ask me"). Komut paleti ve kısayollar yapıldı (2026-11-05): palet
    başlatıcıdan ve Command ya da Control ve K ile açılıyor; belgenin komutları (Document, View, Convert) ve
    Formatico'nunkiler; her komut kendi kısayoluyla paletsiz de çalışıyor; dosya açma ve yapıştırma Start'ın
    kurallarıyla her ekrandan (`DocumentOpener`). Settings yapıldı (2026-11-05): `core:settings` (tercihler, KSafe'te)
    ve `feature:settings`; geniş pencerede kenar çubuğuyla bir sekme, tablette sekme, telefonda kendi ekranı. Tema,
    dil (uygulamanın ve Pantrix'in sözcükleri, anında), Format'ın girintisi ve anahtar sıralaması, biçimi anlaşılmayan
    metnin sorusu, geçmişin tutulması, süresi ve temizlenmesi, About (sürüm derlemeden) ve açık kaynak lisansları.
    "Wrap long lines" Pantrix'in satır kaydırmasıyla bağlandı (2026-11-05). Keyboard shortcuts bölümü Pantrix'in
    `PantrixKeycaps`'iyle yapıldı (2026-11-06): paletin kısayollu komutları, gruplarıyla, uygulamanın çalıştığı yerin
    tuşlarıyla (telefonda bölüm yok, tarayıcıda kendi tuttuğu kısayollar yok); başlatıcının ve Start'ın ipucunun
    tuşları da kapaklarda. Masaüstünde ve web'de Escape artık ekrandan çıkarmıyor (2026-11-06).
*   [x] **Faz 13: Paylaşım.** Küçük bir belge sıkıştırılıp bağlantının fragment'ında; sunucu yok. Yapıldı (2026-11-06):
    tuvalde eksikler çizildi (sürüm 80: uzun bağlantı, `&n=` ile ad, bozuk bağlantının uyarısı). `core:share`: saf Kotlin
    deflate ve inflate (zlib biçiminde, Adler-32'yle; Java'nın zlib'iyle iki yönlü sınandı), bağlantı
    `https://formatico.pantrixui.com/#d=…&n=…`, en çok 32.768 karakter, 2.000'e kadar "her uygulama alır". Paylaşım
    belgenin "More actions" menüsünden: genişte ve tablette pencere, telefonda sayfa ve platformun paylaşma sayfası;
    çok büyük belge dosya olarak kaydediliyor. Bağlantıyı web açıyor (adres sonra gezintiye kalıyor), uygulamada
    herhangi bir yere yapıştırınca da açılıyor; bozuk bağlantı bir uyarıyla söyleniyor (sahibinin kararları,
    2026-11-06). App Links ve Universal Links Faz 22'de.
*   [x] **Faz 14: Büyük dosyalar.** Masaüstünde 50 MB, telefonda ve web'de 10 MB, ölçülerek. Yapıldı (2026-11-06):
    `LargeDocumentsMeasure` altı biçimi 10 ve 50 MB'ta ölçüyor; sonuçlar ve Android, iOS, web'deki denemeler
    [`MEASURE.md`](MEASURE.md)'de. Düzeltilenler: okumanın belleği (yollar, adlar ve adımlar paylaşılıyor, %20–30 az),
    ağacın kapalı dalları kurulmuyor ve seçimde değişmeyen satırlar korunuyor, açılışta en çok 500 satırlık dal açılıyor,
    tablo bir kez ve ana iş parçacığının dışında, XPath'in eşleşmeleri karesel değil, karşılaştırma ortak uçları ayırıyor,
    sütunlar yalnızca sayılarsa sayı diye okunuyor (iOS'ta 48 s'den 6 s'ye), ağaçta arama ana iş parçacığının dışında,
    okunmuşlar bir sınırla tutuluyor, geçmiş yazdığı metinleri bellekte tutmuyor. Android `largeHeap` istiyor, masaüstü
    belleğin yarısını alıyor. Kalanlar
    `OPEN_ITEMS.md`'de, "Büyük belgelerde ölçülüp bırakılanlar".
*   [x] **Faz 15: v1'in testi.** Roborazzi her panoyla, Maestro akışları Android ve iOS'ta, tarayıcı testleri;
    Türkçe ve İngilizce metinler, erişilebilirlik. Yapıldı (2026-11-06; sahibinin kararları: masaüstü, Android ve iOS
    resimleri; tarayıcıda Kotlin, JDK ve Chrome; her pano bir Maestro akışı). Uygulama, kanvasın 11 panosuna ve
    Start'ın iki cevabına ortak bir gezintiyle (`BoardTour`) getiriliyor. Masaüstünde 126 resim çiziliyor: üç
    genişlik, açık ve koyu tema, Türkçe. iOS simülatöründe ve Android'de (Robolectric) 56'şar resim var, 834 ve 390'da.
    Her resimde Pantrix'in teşhisleri bulgusuz olmak zorunda. `BoardAccessibilityTest` aynı gezintiyi JVM'de, iOS'ta
    ve Wasm'da koşturuyor (42 test). Maestro'nun 15 akışı her panoya bir tane. `app/webTest`'in tarayıcı testleri
    dağıtımı kurulu Chrome'da sınıyor: yükleyici, geçmiş, bağlantılar, yeniden yükleme, telefon genişliği, Türkçe.
    Türkçenin 74 metni bir dil incelemesiyle düzeltildi; sonra sahibinin isteğiyle sözcükler `strings.xml`'den çıktı, her
    özelliğin Kotlin'deki `strings` paketine (arayüz ve dil başına nesne), dil de temanın dili oldu.
    Testlerin bulduğu dört hata düzeltildi: telefonda ağaçtayken yapılan değişikliğin metne gelmemesi, sorgu alanında
    Enter'ın hiçbir şey yapmaması, şeridin Android'in dokunma hedefinden alçak olması, Türkçe telefonda iki taşma.
    Kalanlar `OPEN_ITEMS.md`'de, Pantrix'in beş yeni bulgusu `PANTRIX_FINDINGS.md`'de.

## v2

Her v2 ekranı, kodundan önce kanvasta üç genişlikte çizilir.

*   [x] **Faz 16: Sunucu.** Spring Boot: uçtan uca şifreli paylaşım bağlantıları ve URL vekili (989b632, 65fe0a2;
    yayını Faz 22'de). Sahibinin kararları
    (2026-11-06):
    *   **Yeri:** aynı depoda bir `server` modülü. Spring Boot, pantrix-ui-site'ın sunucusuyla aynı sürüm (4.1.2,
        WebFlux), Java 21.
    *   **Uygulamanın HTTP istemcisi:** Ktor Client 3.5.0, yalnızca istemcisi (sahibinin kararı, 2026-11-06). Pantrix'te
        ağ için bir hizmet yok; Spring Boot uygulamada çalışmaz. Şifreli belgeyi yüklemek, indirmek ve bir adresteki
        belgeyi açmak ortak kodda tek bir arayüzle; her platformda kendi motoru: Android'de OkHttp, iOS'ta Darwin
        (`NSURLSession`), masaüstünde Java'nın istemcisi, web'de tarayıcının `fetch`'i. Testlerde Ktor'un sahte motoru
        (`MockEngine`).
    *   **Barındırma:** sitenin kurulduğu gibi: Coolify'da ayrı bir Docker Compose kaynağı
        (`formatico`), önünde Coolify'ın Traefik'i (HTTPS, alan adı). Sitenin yayın düzenine sahibinin izniyle bakıldı,
        yalnızca `deploy/` ve `docs/deploy/` klasörlerine.
        *   `web`: `nginx:alpine` ve web dağıtımı (Wasm, JS yedeği, imaj yapılırken sıkıştırılmış `.gz`'ler).
            Uygulama gezinirken adresi değiştirdiği için bilinmeyen her adreste `index.html` verir. `/api/`'yi
            `server`'a aktarır. Yüklemeye yaklaşık 11 MB sınır, paylaşıma ve vekile ayrı istek sınırı; ziyaretçinin
            adresi Traefik'in `X-Forwarded-For`'undan okunur.
        *   `server`: `eclipse-temurin:21-jre-alpine` ve sunucunun jar'ı. Dışarıya port açmaz, ona yalnızca nginx
            ulaşır; 512 MB bellek sınırı, hakları olmayan bir kullanıcı, sağlık denetimi `/api/health`.
        *   Disk birimi `shares` (`server`'da `/data`): her paylaşım bir dosya ve küçük bir bilgi kaydı (oluşturulma,
            son gün, boyut, silme anahtarının özeti). Süresi dolanları sunucu her gün siler. Ayrı bir veritabanı ve
            yedek yok: paylaşımlar en çok 30 gün yaşar.
        *   Yayın: imajlar bu Mac'te iki mimari için (amd64, arm64) derlenir, GHCR'ye gönderilir, Coolify'ın API'siyle
            yayımlanır; sunucuda hiçbir şey derlenmez. Formatico'nun `deploy/` klasörü: `compose.yaml`, iki Dockerfile,
            nginx şablonu, `deploy.sh` (`local`, `web`). Önce bu Mac'te `localhost`'ta denenir.
        *   DNS: `formatico.pantrixui.com` için A kaydı sahibince açıldı (2026-11-06). Ad çözülüyor;
            Coolify'da kaynak kurulana kadar Traefik 404 veriyor, HTTPS sertifikası alan adı kaynağa eklenince gelir.
        *   GHCR imajlarının kaynağı olacak GitHub deposu Faz 22'de açılır.
    *   **Paylaşım, iki hâli birlikte:** varsayılan bugünkü bağlantı; belge bağlantıya sığıyorsa sunucuya hiç uğramaz.
        Sığmıyorsa ya da kullanıcı isterse, açık bir onayla şifreli kısa bağlantı: belge cihazda rastgele bir anahtarla
        AES-GCM ile şifrelenir, sunucuya yalnızca şifreli hâli gider. Bağlantı
        `https://formatico.pantrixui.com/#s=<kimlik>&k=<anahtar>`: anahtar fragment'ta kalır, sunucu onu hiç görmez.
        En çok 10 MB; süresi 1, 7 ya da 30 gün; paylaşan silebilir (`POST`, `GET`, `DELETE /api/shares`). v1'in
        `#d=` bağlantıları açılmaya devam eder. Start'ın "Metnin bu cihazdan hiç çıkmaz" sözü şifreli bağlantıyı
        seçene açıkça söylenir. Paylaşma penceresi iki seçeneğiyle önce kanvasta çizilir.
    *   **URL vekili:** bir adresteki belgeyi açmak v1'de yok; Start'a ve komut paletine eklenir, önce kanvasta çizilir.
        Android, iOS ve masaüstü adresi kendisi indirir; web, tarayıcının izin kuralları (CORS) yüzünden
        `GET /api/fetch?url=…` ile sunucudan. Yalnızca dışarıdaki `http` ve `https` adresleri: özel IP'ler, localhost,
        Docker ağı ve `169.254.169.254` kapalı, her yönlendirme yeniden denetlenir; en çok 10 MB ve 15 saniye.
    *   **Durum (2026-11-07):** panolar kanvasta (sürüm 85–92: Share ve Start üç genişlikte, palette komut).
        `core:network` (Ktor Client 3.5.0, `ServerShares`, `HttpWebAddresses`), `core:share`'de şifreli bağlantı
        (`#s=…&k=…`), zarf (`ShareEnvelope`) ve anlaşma (`ShareServer`), `server` modülü (Spring Boot 4.1.2, WebFlux;
        paylaşımlar diskte, vekilin adres koruması çözücüde), Share'in yeni penceresi, Start'ta ve paletde "Open a web
        address", şifreli bağlantının açılışı ve uyarıları, `deploy/` (compose, iki Dockerfile, nginx, `deploy.sh
        local`, `check.sh`). Yığın bu Mac'te çalıştı (`deploy.sh local`, `localhost:4005`): `check.sh` geçti, tarayıcıda
        şifreli bağlantı yapıldı, açıldı, silindi; bir web adresi sunucu üzerinden açıldı, özel bir adres geri
        çevrildi. Sınırlar (2026-11-07, sahibinin onayı): paylaşımlar toplamda en çok 5 GB (`507`), bir adresten
        günde en çok 50 (`429`, UTC gece yarısında sıfırlanıyor); Share'de iki yeni uyarı. Web'in sayfası zorlayan bir
        CSP ile sunuluyor (`deploy/web/headers.conf`), tarayıcı testleri aynı başlıklarla. Kalan: yayın (GHCR,
        Coolify'da kaynak), Faz 22'de.
*   [x] **Faz 17: Tip üretimi.** Kotlin, TypeScript, Swift ve Go (c798597). Panolar kanvasta (2026-11-07, sürüm 97–101):
    Types-1440, Types-834, Types-390 ve paletin dört komutu. Önerilen kararlar, sahibinin onayını bekliyor:
    *   **Yeri:** Types, Convert'in yanında bir bölme (`DocumentPane.Types`): masaüstünde ve tablette araç çubuğunda
        Convert'ten sonra, telefonda "more" menüsünde ve kendi sayfasında; komut paletinde "Generate Kotlin, TypeScript,
        Swift, Go types". PantrixIcons'ta kod simgesi yok: düğme Compare gibi yalnızca yazıyla.
    *   **Diller:** Kotlin, kotlinx.serialization'ın `@Serializable data class`'ları (Formatico'nun kendi yığını; JSON'daki
        ad Kotlin'de yazılamıyorsa `@SerialName`); TypeScript `export interface`; Swift `Codable` `struct` (ad farklıysa
        `CodingKeys`); Go `encoding/json` etiketli `struct` (alan adları Go'nun büyük harfiyle, `ID` gibi kısaltmalar).
    *   **Çıkarım:** `core:format`'ta saf Kotlin bir `types` paketi, her biçimin `DataValue`'sundan: her nesne bir tip,
        adı anahtarından (`customer` → `Customer`, `items`'ın öğesi `Item`), kök dosyanın adından (`order.json` →
        `Order`, değiştirilebilir); aynı biçimdeki nesneler tek tip; dizideki nesneler birleşir (bazısında olmayan alan
        isteğe bağlı, bazısında `null` olan boş olabilir); tam sayı `Int` (2³¹'i aşınca `Long`; Go'da `int`), öteki
        `Double`; karışık türler dilin "herhangi bir"i; tarihler `String` kalır; yalnızca `null` olan değerin tipi
        bilinemez (Kotlin `JsonElement?`, TypeScript `unknown`, Go `any`, Swift `String?` ve bir yorum) ve bölmenin
        uyarısı bunu söyler. Ayrılmış sözcükler ve geçersiz adlar her dilin kuralıyla (Kotlin ve Swift'te ters tırnak,
        Go'da ekli ad).
    *   **Biçimler:** okunan her biçim: JSON ailesi, YAML, TOML, CSV ve TSV (bir satırın tipi), properties ve INI;
        NDJSON'da her satır kökün bir örneği; XML, Convert'in okuduğu gibi. Hatalı bir belgede bölme Convert gibi
        önce düzeltmeyi ister.
    *   **Bölme:** dil ve kökün adı; en son seçilen dil hatırlanır. Kod salt okunur editörde, Formatico'nun dört yeni
        highlighter'ıyla (anahtar sözcükler literal'lerin, tipler anahtarların, notasyonlar sayıların, Go etiketleri
        dizgilerin renginde); Kopyala ve "Save as Order.kt / order.ts / Order.swift / order.go".
    *   **Testler:** çıkarımın ve dört yazıcının birim testleri (örnek çıktılar, adlar, ayrılmış sözcükler, birleşme,
        boş dizi, karışık türler, her biçim), highlighter'lar, view model, bölmenin önizlemeleri, ekran görüntüleri ve
        UI testleri her hedefte. Yazılan kodun derlendiği de denenir: Kotlin'inki testlerde derlenen bir örnekle
        (kotlinc bu makinede yok), Swift'inki `swiftc` ile (var); `tsc` ve `go` bu makinede yok, indirmeleri sahibinin
        onayıyla.
    *   **Durum (2026-11-07):** sahibi panoları ve kararları onayladı ("Evet, başla"). `core:format`'ta `types`
        paketi (çıkarım, adlar, dört yazıcı), Workspace'te Types bölmesi, telefon sayfası, araç çubuğu ve "more"
        menüsünde Types, paletin dört komutu, dört highlighter. Kotlin çıktısı `feature:workspace`'in testlerinde
        her hedefte derleniyor ve belgeyi okuyor. Panolar sürüm 116: Types düğmesi Workspace, Invalid, Table, Query,
        Convert ve Summary panolarının araç çubuklarına da eklendi.
    *   **Kapanış (2026-11-07):** Go 1.27.3 ve TypeScript 5.9.3 sahibinin onayıyla indirildi (go.dev'den 68,2 MB,
        npm'den 23,6 MB; makineye kurulmadı, scratchpad'de). `core:format`'ın `TypesCompileTest`'i üç dilin
        çıktısını kendi derleyicisiyle deniyor: Go'nunki `go vet` ve `go test` (belgeyi artan alan olmadan okuyor,
        aynı yazıyor), TypeScript'inki `tsc --strict` (belgenin kendisini tipe atıyor), Swift'inki `swiftc` ile
        derlenip çalışıyor; panoların order'ı ve zor durumları toplayan bir olay listesi (snake case, ayrılmış
        sözcükler, boş dizi, kimi öğede olan alan, iki türlü değer, dizi dizisi, yalnızca null, rakamla başlayan ad).
        Üçü geçti. Derleyicisi verilmeyen test atlanıyor. Açık kalan, Faz 17'yi bekletmeden: tablette menünün
        düğmesindeki odak halkası (`OPEN_ITEMS.md`, gerçek bir tablette denenecek).
*   [x] **Faz 18: Grafik görünümü.** Panolar kanvasta (2026-11-09, sürüm 117–118): Graph-1440, Graph-834,
    Graph-390. Kararlar, sahibinin onayıyla (2026-11-09: "Evet, onaylıyorum"):
    *   **Yeri:** Graph, Tree ve Table'ın yanında üçüncü görünüm (`DocumentPane.Graph`): masaüstünde metnin yanında,
        tablette metnin altında, telefonda Text, Tree, Table, Graph seçiminde.
    *   **Kartlar:** belgenin her nesnesi ve dizisi bir kart; başında adı ve sayısı (`{4}`, `[2]`), satırlarında bir
        satırlık değerleri, ağacın renkleri ve önizlemeleriyle (tarih, renk, bağlantı, resim); içindeki nesne ve dizi
        kendi kartı, ona anahtarıyla ya da sırasıyla bağlı. Kök kartın başında belgenin adı.
    *   **Yerleşim ve gezinme:** masaüstünde ve tablette soldan sağa, telefonda yukarıdan aşağıya; sürükleyince
        kayma, tekerlek ve iki parmakla yakınlaşma, Zoom out, yüzde, Zoom in ve Fit; ağaçtaki gibi Open to 1, 2, 3,
        All; kartın oku çocuklarını kapatıyor. Telefon %70'te açılıyor, seçili kart ve komşuları görünür.
    *   **Seçim:** seçilen kart metinle ve ağaçla birlikte yürüyor; yolu kırıntıda ve durum çubuğunda, Copy path;
        telefonda alttaki kart, Show in text ve Copy the value ile.
    *   **Büyük belgeler:** açılan düzey kartların sayısına göre; öneri, 200'den çok kartta 2. düzeye kadar, gerisi
        kartın okuyla ya da Open to ile açılır.
    *   **Pantrix:** düğümleri ve bağları çizen, kaydırılıp yakınlaştırılan bir grafik bileşeni yoktu; bulgu
        `tıkıyor`du (`PANTRIX_FINDINGS.md`). Kütüphanenin oturumu `PantrixGraph`'ı yerel pakete koydu (2026-11-09),
        bulgu kapandı. Tasarım sistemine de Graph ve `FitScreen` geldi (`1794244829-b310`, 2026-11-09). Panolar artık
        `Pantrix.Graph`, `Pantrix.GraphZoom` ve `Pantrix.GraphFoldButton` ile çiziliyor; o bulgu da kapandı. Tuvalde
        görülen eksikler ayrı ve tıkamayan bir bulguda.
    *   **Durum (2026-11-09):** Workspace'te Graph görünümü (`DocumentPane.Graph`, `GraphPane.kt`,
        `DocumentGraph.kt`): masaüstünde metnin yanında soldan sağa (%85), tablette metnin altında (%100), telefonda
        yukarıdan aşağıya (%70), seçili değerin kartı altta, ağacınkiyle aynı. Kartın başı adı ve sayısı, seçili
        kartta markanın yumuşak dolgusu ve katlama oku; satırları ağacın renkleri ve önizlemeleriyle (takvim, renk
        örneği, bağlantı, resim), en çok 50 satır ve "and N more". Seçim metinle ve ağaçla, açık kartlar ağaçla
        birlikte yürüyor; seçilen değeri taşıyan en yakın kart seçili ve görünür kılınıyor. Açılış: bütün kartlar,
        200'den çok kartta 2. düzeye kadar; Open to 1, 2, 3, All. Yakınlaşma başlıkta (`PantrixGraphZoom`); bölme
        600 dp'den darsa ve telefonda grafiğin köşesinde, başlık 520 dp'den darsa "Open to" sözcüğü düşüyor.
        Görünüm seçiminde Graph her yerde (telefonda sorunlar varken Problems onun yerinde), komut paletinde ve
        Settings'in kısayollarında "Show the graph" ⌘3 (telefonda yok, tarayıcıda kısayolsuz). Telefonun metninde
        şema işaretleri de artık çiziliyor (Faz 19'dan kalan bir eksik). Panolar sürüm 149–170: Graph panoları kodun
        çizdiği gibi (Fit simgesi, telefonun seçili değer kartı, durum çubuğu Ln 14, Col 4), öteki panoların görünüm
        seçimlerinde Graph, palette ve Settings'te ⌘3. Denetim: Workspace'in testleri JVM'de 319, iOS simülatöründe ve
        Wasm'da 297, uygulamanınkiler JVM'de 105, hepsi geçti; web'in JavaScript yedeği derleniyor, web testleri (10) geçti; ekran görüntüleri
        masaüstünde doğrulandı, Graph panoları iOS'ta ve Android'de (Robolectric) kaydedildi; lint, detekt ve görünmeyen
        karakter denetimi temiz. Açık kalan, Faz 18'i bekletmeden: büyük bir dizinin grafiği kök kartı görünmeden
        açılıyor (`OPEN_ITEMS.md`).
*   [x] **Faz 19: JSON Schema.** Panolar kanvasta (2026-11-09, sürüm 119–122): Schema-1440, Schema-834, Schema-390;
    dört durum (`state`): problems, valid, none, unreadable. Kararlar, sahibinin onayıyla (2026-11-09: "Evet, başla";
    JavaScript varyantlarıyla en çok 4,9 MB'lık indirme de: "Evet, indir"):
    *   **Yeri:** Schema, araç çubuğunda Types'tan sonra (`DocumentPane.Schema`): masaüstünde metnin yanında (Types
        gibi `BesideTextPanes`), tablette bütün oda, telefonda kendi ekranı, "more" menüsünde Types'tan sonra.
        Komut paletinde "Validate against a schema". Öteki panoların araç çubukları Schema'yı onaydan sonra alır.
    *   **Şema:** Against menüsü açık sekmelerden JSON Schema olanları listeler (kökü bir nesne, `$schema`'sı bir
        taslağı adlandırıyor ya da `type`, `properties`, `$defs` taşıyor); Choose a file bir dosyayı yeni bir sekmede
        açar ve seçer, şema böylece kendi sekmesinde düzeltilebilir. Belgenin `$schema`'sı açık bir şemanın `$id`'sini
        adlandırıyorsa o seçili gelir; bir web adresiyse ve öyle bir sekme yoksa menü "Open <adres>" der ve onu Faz
        16'nın web adresi açmasıyla bir sekmede açar (basılmadan ağa gidilmez). Seçim belgenin görünümünde kalır;
        şemanın sekmesi kapanınca seçim düşer.
    *   **Doğrulama:** taslak şemanın `$schema`'sından: 2020-12 (ilk değer), 2019-09, 7, 6, 4. `$ref` şemanın içinde
        ve açık şemaların `$id`'leri arasında çözülür; dışarıdaki bir adrese gidilmez. Çözülemeyen bir `$ref` ya da
        şema olmayan bir şema durum unreadable'dır: uyarı, yeri ve neyi göstermediği. `format` bir onay olarak da
        denetlenir (date, date-time, email, uri, uuid, ipv4, ipv6…): 2020-12'de yalnızca bir not, ama bir doğrulayıcı
        ekranında denetlenmemesi şaşırtır. Belge ya da şema değişince yeniden doğrulanır, arka planda.
    *   **Sorunlar:** Invalid'deki gibi listelenir: cümle, satır ve sütun, yol (`$.coupon`); metindeki sırayla.
        Cümleler Formatico'nun, anahtar sözcüğe göre (`type`, `required`, `enum`, `const`, `minimum`, `maximum`,
        `minLength`, `pattern`, `format`, `minItems`, `uniqueItems`, `additionalProperties`, `oneOf`, `anyOf`, `not`,
        `if`…), İngilizce ve Türkçe; kütüphanenin İngilizce cümleleri gösterilmez. `required` sorunu nesnenin kendi
        yerini gösterir. Basınca metindeki yerine gidilir: masaüstünde metin ve liste birlikte yürür, tablette ve
        telefonda metin o yerde açılır. Metinde dalgalı alt çizgi; durum çubuğunda "2 schema problems". İlk 200
        sorun listelenir, gerisi sayıyla.
    *   **Biçimler:** değerleri bir ağaç olan biçimler: JSON ailesi, YAML, TOML (YAML ve TOML'ın değerleri JSON'a
        çevrilir). CSV/TSV, properties/INI ve XML'de bölme, değerleri hep metin olduğu için doğrulamadığını söyler.
        Hatalı bir belgede önce onarım, Types gibi.
    *   **Kütüphane:** `io.github.optimumcode:json-schema-validator` 0.5.5 (MIT; Formatico'nun bütün hedefleri: JVM,
        Android, iOS, Wasm). Taslak 4'ten 2020-12'ye, `$ref`, `format` ve birden çok şema; hatası şemadaki ve
        belgedeki yolu JSON Pointer olarak veriyor, cümleleri bu yollardan kurulur. Belge `core:format`'ın
        değerlerinden kotlinx.serialization'ın `JsonElement`'ine çevrilir, yol oradan metindeki yere döner. Kendi
        doğrulayıcımız da olabilir, ama beş taslak ve `$ref` büyük bir iş; öneri kütüphane. İndirme sahibinin
        onayıyla, Maven Central'dan, en çok 4,0 MB (bir kısmı Gradle'ın önbelleğinde): json-schema-validator 0.5.5
        (2,3 MB, beş varyant), karacteristics 0.0.9 (1,0 MB), uri-kmp 0.0.21 (0,5 MB), normalize 1.4.0 ve
        kotlin-codepoints 0.11.0 (0,1 MB).
    *   **Şema üretmek (öneri):** Types'ın menüsüne beşinci dil olarak JSON Schema (2020-12): çıkarım zaten türleri,
        her öğede olan alanları (`required`) ve boş olabilenleri biliyor. Onaylanırsa Types panoları beşinci dili
        gösterecek biçimde güncellenir.
    *   **Pantrix:** gereken her şey var (Menu, Alert, liste satırları, editörün işaretleri, dosya açma); yeni bulgu
        beklenmiyor.
    *   **Testler:** belgenin `JsonElement`'e çevrilmesi, yolun metindeki yere dönmesi, her anahtar sözcüğün cümlesi
        iki dilde, şemaların seçilmesi (`$schema`, `$id`), dört durum, view model; bölmenin önizlemeleri (dört durum,
        hatalı belge, doğrulanmayan biçim, üç genişlik, açık ve koyu), ekran görüntüleri ve UI testleri her hedefte.
        Büyük belgede süre ölçülür (10 MB'lık bir JSON).
    *   **Durum (2026-11-09):** yeni modül `core:schema` (doğrulayıcının üstünde Formatico'nun sorunları ve cümle
        kuralları; `anyOf`/`oneOf`'un hiçbirine uymayan dalları tek sorun), Workspace'te Schema bölmesi (geniş
        ekranda metnin yanında, tablette bütün oda, telefonda kendi ekranı), araç çubuğunda, telefonun "more"
        menüsünde ve komut paletinde Schema, metinde işaretler, durum çubuğunda sayı; Choose a file ve "Open <adres>"
        şemayı seçili sekmeyi değiştirmeden yeni bir sekmede açıyor. Types'ta beşinci dil JSON Schema; üretilen şema
        belgenin kendisini doğruluyor (testte). Settings'in lisanslarına beş kütüphane eklendi (json-schema-validator,
        karacteristics, uri-kmp, doistx-normalize, kotlin-codepoints; MIT ve Apache). Ölçüm (JVM, LargeDocumentsMeasure):
        üretilen şemaya göre doğrulama 10 MB JSON'da 0,43 s, YAML'da 0,34 s; 50 MB JSON'da 1,6 s, YAML'da 1,5 s; şemayı yazmak
        0,1–0,3 s. Panolar sürüm 123–145: Schema panoları kodun cümleleriyle, öteki panoların araç çubuklarında Schema,
        Types panolarında JSON Schema, Settings'in lisansları. Tablette iki şey sığmadı ve Formatico'da çözüldü:
        Türkçe araç çubuğunda Format "Biçimle" oldu; onarılacak bir belgede Repair, Minify'ın yerinde (Invalid-834'ün
        çizdiği gibi). Pantrix'te sığmayan eylemleri menüye alan bir araç çubuğu yok: bulgu, tıkamıyor. Denetim:
        JVM'de, iOS simülatöründe ve Wasm'da bütün testler (core:schema 12, Workspace JVM 308, iOS ve Wasm 287), web'in
        JavaScript yedeği derleniyor, pano görüntüleri masaüstünde, iOS'ta ve Android'de (Robolectric) kaydedildi,
        web testleri (10) geçti; lint, detekt ve görünmeyen karakter denetimi temiz. Açık kalan, Faz 19'u bekletmeden:
        tablette menünün düğmesindeki odak halkası şema menüsünde de (`OPEN_ITEMS.md`, gerçek bir tablette denenecek).
*   **Faz 20: Uygulama içi yapay zekâ,** kullanıcının kendi anahtarıyla: **iptal edildi.** Sahibinin kararı
    (2026-11-09): "bana gereksiz geldi". Faz numaraları değişmiyor; ardından Faz 21 ve Faz 22 geliyor.

## Kapanış

*   [ ] **Faz 21: Tam denetim.** Faz 0'dan 19'a her şey yeniden denetlenir: CLAUDE.md, bütün panolar,
    `OPEN_ITEMS.md` ve `PANTRIX_FINDINGS.md`. Eksik kalan her şey bu fazda kapanır.
    *   **Döküm (2026-11-09):** derleme ve test koşturulmadan, dosyalar ve kod okunarak çıkarıldı.
        *   **Açık maddeler:** `OPEN_ITEMS.md`'de 56, `PANTRIX_FINDINGS.md`'de 70. Hiçbiri Formatico'yu tıkamıyor.
        *   **Kodda düzelmiş görünenler:** üç Formatico maddesi (değer önizlemeleri, ağacın seçilen satıra
            kayması, lisanslar).
        *   **Pantrix'te düzelmiş olan:** yok; yerel pakette tamamen düzelmiş bulgu bulunmadı.
        *   **Bulguların yeri:** yaklaşık 37'si kütüphanede, 8'i tasarım sisteminde, 23'ü sitede ve
            belgelerde, 3'ü MCP'de ya da eklentide düzeltilmeli.
    *   **Sıra:**
        1.  **Ev işleri:**
            *   Düzelmiş üç maddeyi kapatmak ve eskimiş durum yazılarını düzeltmek.
            *   İki kez yazılmış bulguyu ("SegmentButton'a grubun adı verilemiyor") birleştirmek.
            *   iOS'un ilk karesine ve üst çubuğun geçişine simülatörde bakıp kapatmak.
            *   Bulgusunu anmayan yaklaşık 25 geçici çözümün yorumuna bulgunun adını yazmak.
            *   Kullanılmayan `EditorComing`'i sözcükleri, ekran görüntüsü testi ve görüntüleriyle silmek.
        2.  **Sahibinin kararları** (aşağıda): sonraki adımların kapsamını belirler.
        3.  **Küçük hatalar:**
            *   Ağacın arama çubuğu dar bölmede sıkışıyor.
            *   Telefonda Query'den ve Summary'den geri hareketi Start'a gidiyor.
            *   Enter önceki sorguyu kullanıyor.
            *   Problem satırının etiketi yok.
            *   Web'de açılamayan bağlantı adreste kalıyor.
            *   Sorgu sonuçları ayrı bir `JsonWriter`'la yazılıyor.
            *   Query öteki eşleşmeleri işaretlemiyor: Pantrix'in `CodeRangeStyle.Highlight`'ı artık var.
            *   `build-logic`'e lint ve detekt bakmıyor.
            *   Settings'in lisanslarında Ktor, OkHttp, kotlinx-browser ve navigationevent eksik.
            *   Paylaşım sınırı MiB ile, öteki sınırlar MB ile.
            *   Yapıştırılan metnin boyut sınırı yok.
        4.  **Önizleme ve test kuralı:**
            *   Komut paletinin önizlemesi yok.
            *   Summary, Problems, Table, editör ve telefonun kartlarının kendi önizlemeleri yok.
            *   Eksik hâller: Start'ın WrongType ve Unreadable reddi; tablonun süzülmüş, boş ve sütunu gizli
                hâli; JSONC, JSON5 ve TSV.
            *   UI testleri her önizleme hâlini gezmiyor.
            *   Yardımcı işlevlerin birim testleri eksik: problem ve onarım sözcükleri, dönüştürmenin kayıpları,
                Summary'nin notları, imlecin yeri ve öteki sözcük işlevleri.
            *   Testler her hedefte koşmuyor: feature ve core modüllerinin `commonTest`'i Android'de (host
                test) ve JavaScript yedeğinde koşmuyor.
        5.  **Tuval ile kodun eşlenmesi:**
            *   Kararlara göre ya panolar ya kod değişir.
            *   Ardından her pano, kodun ekran görüntüsüyle bire bir karşılaştırılır.
        6.  **Özellikler ve performans:**
            *   XML, TOML ve INI onarımları.
            *   Alias'ı anchor'ının önüne düşüren sıralamada anchor'ı taşımak.
            *   Kararlarla gelen seçenekler.
            *   Ağacın aramanın her harfinde yeniden kurulması; XPath dizininin her sorguda yeniden kurulması.
            *   Büyük belgelerde kararla seçilenler: masaüstü penceresinde 50 MB ölçümü ve JavaScript yedeğinin
                ölçümü bunlardan.
        7.  **Ekran görüntüleri ve cihazlar:**
            *   Önizlemelerin siparişi panolardakiyle eşlenir ve bütün görüntüler bir kez yeniden kaydedilir.
            *   Invalid-834'ün Android görüntüsü arada bir değişiyor; düzeltilir.
            *   Gerçek cihazda ekran okuyucu, tablette odak halkası denenir.
        8.  **Son denetim:**
            *   Bütün hedeflerde testler; `verifyRoborazzi`, web testleri, lint ve detekt.
            *   Pantrix'e kalan bulgular kütüphanenin oturumuna toplu olarak iletilir (sahibinin onayıyla).
    *   **Sahibinin kararları:**
        *   **Kodda seçilip panolarda olmayanlar:**
            *   Query'nin, Convert'in ve Compare'in hâlleri ve kuralları.
            *   Sorgu sonuçlarının biçimi ve dosya adı; son sorguların saklanması.
            *   Share, komut paleti ve Settings'in panolardan ayrıldığı yerler.
            *   Yeni biçimlerin ağaçları.
            *   Tuvaldeki "unread" hâli.
        *   **Ölçeğe uymayan ölçüler:** Summary'nin yazı ve aralıkları, ağaç satırının 13/20 ile 14/20 farkı,
            CSV'nin `numeric` sütununun türü.
        *   **Seçenekler:**
            *   YAML yazıcısının kuralları.
            *   YAML 1.1 (merge anahtarı, yes ve no).
            *   CSV'de ayırıcı, hizalama ve "ilk satır başlık".
            *   Android'in uygulama başına dili (`localeConfig`).
        *   **Telefon:**
            *   Invalid'de dört görünüm mü, üç mü.
            *   Biçim nereden seçilecek.
            *   Bul ve değiştir nereden açılacak.
        *   **Öteki kararlar:**
            *   Kökün `${4}` diye yazılması.
            *   Büyük dizinin grafiğinde kökün gösterilmesi.
            *   Web'de paylaşım bağlantısının sayfanın kendi adresiyle kurulması.
            *   Büyük belgelerin takiplerinden hangilerinin v1'de olacağı.
            *   Resim değerlerinin simge yerine küçük resimle gösterilmesi.
            *   Belge açıkken sürükle bırak.
            *   Önizleme ve ekran görüntüsü tabanlarının `core:base` dışında kalması.
            *   CLAUDE.md'de editörün hâlâ beklendiğini söyleyen cümleler.
*   [ ] **Faz 22: Yayın.** Pantrix'in gerçek yayını; Formatico'nun yayımlanan sürüme dönmesi (`mavenLocal` bloğu
    kalkar, `pantrix-ui@pantrix-ui` açılır, `pantrix-ui-local` kaldırılır); GitHub Packages'tan gelen paketle bütün
    hedeflerde yeniden derleme ve testler; `formatico.pantrixui.com`'un ve v2 sunucusunun yayını. Sahibinin planı
    (2026-11-06):
    *   Bütün fazlar bitip sahibi onaylayınca Formatico için bir GitHub deposu açılır, proje oraya gönderilir. Yayın o
        depodan: web Coolify'a (Faz 16'nın düzeni); macOS uygulaması sahibinin Mac'inde derlenen, imzalı ve Apple'ın
        onayından geçmiş bir DMG; Android uygulaması imzalı bir APK (bir yayın anahtarı gerekecek). İkisi deponun
        GitHub sürümlerinde indirilir. Google Play yok. iOS yayımlanmaz; hedefi kodda ve testlerde kalır.
    *   **İndir:** masaüstünde ve web'de bir indirme menüsü, Android'i ve masaüstünü indirmek için; Android
        uygulamasında yok. Sürümü, boyutu ve SHA-256'yı GitHub'ın API'sinden okur, pantrixui.com'un indirme sayfası
        gibi. Yeni bir ekran: kodundan önce kanvasta 1440, 834 ve 390'da çizilir. Menünün yeri (sahibinin kararı,
        2026-11-09): hem Ayarlar'da hem Start ekranında.

## Sahibinin kararını bekleyenler

*   **Paylaşım** (karara bağlandı, 2026-11-06): CLAUDE.md fragment'ta paylaşımı v1'de, "share links"i v2'de
    anlatıyor. Fragment bağlantısı Faz 13'te yapıldı; sahibinin kararıyla v2'de kalıyor, şifreli kısa bağlantı onun
    yanına geliyor (Faz 16, "Paylaşım, iki hâli birlikte").
*   **Yayın (Faz 22)** (karara bağlandı, 2026-11-06): iOS yayımlanmaz, ama hedefi kodda ve testlerde (simülatör
    testleri, iOS ekran görüntüleri, Maestro) kalır; masaüstü yalnızca macOS; Android yalnızca GitHub'daki APK;
    indirme menüsü masaüstünde ve web'de, bilgileri GitHub'ın API'sinden (Faz 22). Açık kalan:
    *   Windows: sahibi olup olamayacağını öğrenmek istiyor. Compose bir Windows paketini (MSI, EXE) yalnızca
        Windows'ta yapabiliyor, Mac'te yapamıyor. Yol, deponun GitHub Actions'ında bir Windows makinesi: sürüm
        çıkınca JVM testlerini Windows'ta koşturur, MSI'ı yapıp sürüme ekler. İmzasız bir MSI'da Windows ilk açılışta
        "Windows bilgisayarınızı korudu" uyarısı verir; imza için ücretli bir kod imzalama sertifikası gerekir.
        Sahibinin kararı.
