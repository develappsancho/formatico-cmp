# Plan

Formatico'nun fazları, sahibinin onayıyla (2026-11-03). Formatico'nun eksikleri
[`OPEN_ITEMS.md`](OPEN_ITEMS.md)'de, Pantrix UI'ınkiler [`PANTRIX_FINDINGS.md`](PANTRIX_FINDINGS.md)'de; bu dosya
yalnızca sırayı tutar.

Bir faz bitince işaretlenir, hangi commit'lerle bittiği yazılır. Her fazın sonunda: ilgili panolar 1440, 834 ve 390'da
bire bir karşılanmış; bütün hedefler derleniyor ve testler geçiyor; bulgular ve açık maddeler yazılmış; Türkçe bir rapor
ve bir commit mesajı. Bir fazı `tıkıyor` diye işaretlenmiş bir bulgu durdurursa önce o düzeltilir ve yayımlanır.

## Bitenler

*   [x] **Faz 0: Kurulum.** Sihirbazın projesi Pantrix'e taşındı: Teal preset, Geist ve Geist Mono, convention
    eklentileri, `core:base`, `core:designsystem`, `core:format`, Koin, navigation3, detekt ve Kotlinter; web Wasm'da,
    JavaScript'e dönüşüyle. `efe4dcf`.
*   [x] **Faz 1: Tasarım.** Kimlik panoları ve v1'in 11 ekranı 1440, 834 ve 390'da (33 pano); Summary düzen'den
    sonra. `649667d`, `fa17a9e`, `c912b59`, `c34cf4e`, `c7369f5`.
*   [x] **Faz 2: Start ve Workspace.** Start: aç, yapıştır, bırak, örnekler, Recent. Workspace: sekmeler, araç
    çubuğu, ağaç, yol izi, durum çubuğu, sözcükle değer önizlemesi, Format, Minify, Sort keys ve Undo. `27efaaa`,
    `9ae728c`.
*   [x] **Faz 3: İlk biçimler.** JSON, JSONC ve JSON5 kayıpsız okunup yazılıyor; YAML `snakeyaml-engine-kmp` ile
    okunup Formatico'nun yazıcısıyla yazılıyor; CSV ve TSV okunuyor, Table'da açılıyor. `9ae728c`, `42ba6e3`, `d3b990c`.

Pantrix 0.1.2-SNAPSHOT'a geçiş: `69e8eb8`, `57e2007`.

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
    2026-11-06). App Links ve Universal Links Faz 22'de; yapılmadı (sahibinin kararı, 2026-11-12).
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

*   [x] **Faz 16: Sunucu.** Spring Boot: uçtan uca şifreli paylaşım bağlantıları ve URL vekili (5ba32d7, ebacb0e;
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
        *   `formatico-web`: `nginx:alpine` ve web dağıtımı (Wasm, JS yedeği, imaj yapılırken sıkıştırılmış `.gz`'ler).
            Uygulama gezinirken adresi değiştirdiği için bilinmeyen her adreste `index.html` verir. `/api/`'yi
            `formatico-server`'a aktarır. Yüklemeye yaklaşık 11 MB sınır, paylaşıma ve vekile ayrı istek sınırı; ziyaretçinin
            adresi Traefik'in `X-Forwarded-For`'undan okunur.
        *   `formatico-server`: `eclipse-temurin:21-jre-alpine` ve sunucunun jar'ı. Dışarıya port açmaz, ona yalnızca nginx
            ulaşır; 512 MB bellek sınırı, hakları olmayan bir kullanıcı, sağlık denetimi `/api/health`.
        *   Disk birimi `shares` (`formatico-server`'da `/data`): her paylaşım bir dosya ve küçük bir bilgi kaydı (oluşturulma,
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
*   [x] **Faz 17: Tip üretimi.** Kotlin, TypeScript, Swift ve Go (1ad9a85). Panolar kanvasta (2026-11-07, sürüm 97–101):
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

*   [x] **Faz 21: Tam denetim.** Faz 0'dan 19'a her şey yeniden denetlenir: CLAUDE.md, bütün panolar,
    `OPEN_ITEMS.md` ve `PANTRIX_FINDINGS.md`. Eksik kalan her şey bu fazda kapanır.
    *   **Kapandı (2026-11-12),** sahibinin kararıyla ("Evet, kapat"): yayından önce yapılacaklar bitti, gerisi yayından
        sonraya alındı (aşağıda, "Yayına kadar kalan"). 5 iptal; 6'nın üç kararı kodda; 8'in son denetimi bütün
        hedeflerde geçti. `e9a41af`, `9e3f83e`, `c477e62`, `7321aa0`, `bf51b6c`, `c07a5ce`, `fadaaf7`, `4016a94`,
        `02d4f12`, `be3dbd5`, `7fb9db5`, `f1b9aad`, `4f8c932`, `4de981b`, `d00ccd6`, `3899c50`.
    *   **Yayından sonraya kalanlar:** `OPEN_ITEMS.md`'deki 33 açık madde, 4'ün Android kısmı, 7 (gerçek cihazda
        ekran okuyucu, tablette odak halkası, görüntülerin topluca yeniden kaydı) ve açık Pantrix bulgularının
        kütüphaneye iletilmesi: Compose kütüphanesinde 1, sitede 1, tasarım sisteminde 14 (2'si Compose'da kapalı),
        Compose'un kendi web sınırı 1.
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
                Düzeldi (2026-11-10): tur her panonun durumunu bekliyor; Android'in iki Invalid resmi yeniden
                kaydedildi.
            *   Gerçek cihazda ekran okuyucu, tablette odak halkası denenir.
        8.  **Son denetim:**
            *   Bütün hedeflerde testler; `verifyRoborazzi`, web testleri, lint ve detekt.
            *   Pantrix'e kalan bulgular kütüphanenin oturumuna toplu olarak iletilir (sahibinin onayıyla).
    *   **İlerleme (2026-11-10):**
        *   1–3 bitti (`9e3f83e`).
        *   4 bitti, Android hariç. Her önizleme hâli her hedefte bir UI testinde geziliyor (`checkPreviewCases`). JS'nin
            testleri açıldı ve JavaScript yedeğinin kendi hataları düzeldi. Ortak testler Android'in ana makine
            ağacında koşmuyor; Pantrix'in rehberi de öyle diyor.
        *   5 tuval oturumunda (tuvalin bağlamı onda); liste paylaşılan notlarda,
            `2026-11-10-faz21-canvas-redraws.md`. Telefonun iki panosu çizildi: durum çubuğundan biçim menüsü, More'da
            "Find in the text" (tuval sürümü 1794291668-7e49).
        *   6 sürüyor. Bitenler:
            *   Telefonda biçim, durum etiketinden açılan menüyle seçiliyor (`StatusFormatMenu`). "More" menüsünün ilk
                satırı "Find in the text": editörün bul çubuğunu açıyor. Pantrix'te menü açan bir durum etiketi yok;
                bulgusu yazıldı.
            *   XML, TOML ve INI onarımları (`XmlRepairs.kt`).
            *   YAML'da Sort keys, alias'ı anchor'ının önüne düşürecekse anchor'ı ilk alias'ın yerine taşıyor
                (`YamlAnchorSwaps.kt`). Bir akış koleksiyonunun içinde ise yine reddediliyor.
            *   Ağacın araması ve Query, yazım durunca çalışıyor (150 ms).
            *   JavaScript'te düşen JSONPath testindeki tek başına vekil karakter, çalışırken üretiliyor.
        *   6'da kalanlar:
            *   Panosu önce çizilecek üç karar (tuval oturumunda): resim değerinin küçük resmi, CSV'de "First row is the
                header" seçeneği, açık belgeye dosya bırakmak.
            *   Ölçümler yapıldı (2026-11-10, `MEASURE.md`, "Uygulamanın bütünü"): uygulamanın bütünü masaüstünün
                1440×900'lük penceresinde, 4 GB yığınla, 50 MB'lık CSV'yi 5,9 s'de açıyor (yığın en çok 3,35 GB); JS
                yedeği 10 MB'lık JSON'u 2,4 s'de, CSV'yi 3 s'de açıyor. 50 MB'lık JSON açılmıyordu: Pantrix'in kod editörü
                bir milyonu aşkın satırda çöküyordu. Kütüphanenin C partisiyle (2026-11-11) açılıyor: 2,0 s, yığın en
                çok 2,77 GB.
            *   Kütüphanenin C partisi (2026-11-11, 19 madde ve iki bulgu) alındı: kapatılabilen sekmeler, Pantrix'in
                paylaşma sayfası, tablonun seçili hücresi, boş hâller, menünün başlığı, basılabilen durum etiketi,
                editörün notundaki düğmeler ve şimdiki eşleşme, ağacın kaynağı (`TreeSource`); geçici çözümleri
                söküldü. Panolarla ayrılan yerler tuval oturumuna iletildi.
            *   Compare `PantrixDiffView`'a geçti (2026-11-11), tasarım sisteminin DiffView'u yayımlanıp Compare panoları
                onunla çizildikten sonra. Anlamca karşılaştırmada yalnızca farkların satırları işaretli (sahibinin
                kararı); panolar orada tasarım sistemi satır alana kadar koddan ayrı.
            *   Kütüphanenin yerel pakete koyduğu ilk parti düzeltmeyle sökülecek geçici çözümler bitti: paletin
                arama alanının adı, `FileDrop`'un sözcükleri, dokuz yeni simge, `PantrixToolbar` ve `PantrixStatusBar`
                (2026-11-10). Workspace'in araç ve durum çubuğu artık Pantrix'in. Panoların bu iki bileşenle yeniden
                çizilmesi tuval oturumunda; liste paylaşılan notlarda, `2026-11-10-faz21-toolbar-statusbar.md`.
            *   Compare `PantrixDiffView`'e geçiyor (sahibinin kararı, 2026-11-10, bulgu "İki metni yan yana
                karşılaştıran bir görünüm yok"). Sırası: tasarım sisteminin `DiffView`'i yayımlanır (sahibinin
                onayıyla), Compare panoları onunla çizilir, sonra kod. Liste paylaşılan notlarda,
                `2026-11-10-faz21-compare-diffview.md`.
            *   Kütüphanenin davranış partisi geldi (0.1.2-SNAPSHOT, 2026-11-10 18:58): on dört madde denendi, geçici
                yollar kaldırıldı (yol izinin kırpılması, seçili satırın renksiz etiketi, testlerde dialog'u ekranı
                dolduran kutu, durum çubuğunun tablete taşınan önizleme hâli), bulgular işaretlendi. Araç çubuğu artık
                tek öncelik sırasıyla katlanıyor; açık görünümün düğmesi Repair kadar uzun kalıyor. Yeni bir bulgu,
                "Seçili satırda uygulamanın işaretinin yazı rengi de siliniyor", aynı akşam kapandı. Snackbar'ın
                web'deki ekran okuyucusu Compose 1.13'ü bekliyor; salt okunur editörün bandı tasarım sisteminde açık.
            *   Kütüphanenin rehber partisi geldi (0.1.2-SNAPSHOT, 2026-11-10 20:50): on üç madde rehberlerde denendi ve
                işaretlendi; derleme dosyalarının ve testlerin yorumları artık bulguları değil rehberleri anıyor.
                `FormaticoPreview` Pantrix'in yeni `PantrixPreviewTheme`'inde çiziyor; tabletin dört testi
                `AtWindowSize` ile tanılamayı da soruyor. Yeni bir bulgu: "Test rehberinin pencere boyundaki kutusu
                ekranı odanın ortasına koyuyor".
        *   7 ve 8 sırada.
    *   **Yayına kadar kalan (sahibinin kararı, 2026-11-11):**
        *   **5 iptal:** tuval olduğu gibi kabul edildi. Tuval oturumunda onay bekleyen üç düzenleme düştü.
        *   **6'dan kalan üç karar koda girer,** 2026-11-10'da çizilen panolarına göre:
            *   Resim değerinin küçük resmi. Pantrix resim yüklemiyor; Formatico `core:network`'ün Ktor'uyla yükler:
                yalnızca https, yalnızca görünen satırlar, resim başına bir boyut sınırı, bellekte önbellek;
                `data:image` adresleri ağa çıkmadan çözülür. Yüklenemeyen resim bugünkü simgeli kutuda kalır; web'de
                CORS izni vermeyen sunucuların resimleri böyle kalır.
            *   CSV'de "First row is the header": Columns seçicisinde, yalnızca CSV ve TSV'de; kapalıyken sütunlar
                "column 1, column 2…". Seçim sekmeyle saklanır; tablo, tablonun kaydetmesi ve kopyalaması ona uyar.
                Convert'in kodu değişmez; seçenek belgenin okunuşu olduğu için onun çıktısı da ona uyar (2026-11-12).
            *   Açık belgeye bırakılan dosya yeni bir sekmede açılır. Pantrix'in `fileDropTarget`'ıyla; sürüklenirken
                görünen katmanı Formatico çizer, Pantrix'te olmayışı bulgu olarak yazılır.
        *   **8'in son denetimi yayından önce:** bütün hedeflerde testler, `verifyRoborazzi`, web testleri, lint ve
            detekt.
        *   **Yayından sonra:** `OPEN_ITEMS.md`'deki açık maddeler, 4'ün Android kısmı (ortak testler Android'in ana
            makine testlerinde), 7 (gerçek cihazda ekran okuyucu, tablette odak halkası, görüntülerin topluca yeniden
            kaydı) ve kalan bulguların kütüphaneye iletilmesi.
        *   **İlerleme (2026-11-12):** üç karar kodda.
            *   Resmin küçük resmi: `core:network`'te `WebPictures` (yalnızca https, 2 MB'a kadar, resim olmayan cevap
                yok sayılır, Formatico'nun sunucusundan hiç geçmez; web'de sayfanın politikası resmin sitesine https
                ile ulaşmaya izin verir, `connect-src https:` (sahibinin kararı, 2026-11-12), resmi CORS izni veren site
                verir); Workspace'te `PictureThumbnails` (bir kez okunur,
                ortasından kare kesilir, en çok 192 piksel, bellekte 64 tane, `data:image` ağa çıkmadan). Ağaçta 24'lük,
                telefonda 28'lik; telefonun seçili değer kartında 56'lık. Okunamayan ya da okunurken bugünkü simgeli kutu.
                Uygulamanın testleri resimleri ağdan değil, `boardNetworkModule`'den alıyor.
            *   CSV'de "First row is the header": Columns seçicisinde (telefonda alttan açılan sayfa), yalnızca CSV ve
                TSV'de; kapalıyken sütunlar okuyucunun dilinde "column 1"/"sütun 1". Seçenek belgenin okunuşu: ağaç,
                tablo, sorgu ve dönüştürme ona uyuyor (Convert'in kodu değişmedi, belgenin okunuşunu alıyor). Seçili kayıt
                metindeki satırında kalıyor; sekmeyle saklanıyor. Dil değişince sütunlar yeni dilde adlandırılıyor; metinde
                ilk satır kayıtlar gibi renkleniyor (Table-1440'ın `noheader` hâli de öyle, tuval `1794463269-4d2e`).
            *   Açık belgeye bırakılan dosya: Pantrix'in `fileDropTarget`'ı, Start'ın açıcısı (`openDropped`: tür ve
                boyut Start'taki gibi denetleniyor), yeni bir sekmede. Kapağı Formatico çiziyor (`DropCover`); bulgusu
                yazıldı. Geniş pencerede ve tablette, panoların çizdiği yerde; telefon, Start'ı gibi, bırakılan dosya
                almıyor.
            *   Denetim: JVM, iOS simülatörü, Wasm ve JS testleri (Workspace, Start, `core:network`, uygulama); web
                testleri; `verifyRoborazzi` JVM'de, Android'de (Robolectric) ve iOS'ta; lint ve detekt temiz. Android'de
                Pixel 7 Pro emülatöründe bir `data:image` değeriyle ağaçtaki ve karttaki küçük resim görüldü. Android'in
                panoları da küçük resimle: okuma isteyen iş parçacığında başlıyor, küçük resim orada hemen kesiliyor.
    *   **Sahibinin kararları (2026-11-10):** sahibi önerileri kabul etti ("önerilerin olsun"):
        *   **Kodda seçilip panolarda olmayanlar: kod onaylandı, panolar koda göre çizilir.**
            *   Query'nin, Convert'in ve Compare'in hâlleri ve kuralları.
            *   Sorgu sonuçlarının biçimi ve dosya adı; son sorguların saklanması.
            *   Share, komut paleti ve Settings'in panolardan ayrıldığı yerler.
            *   Yeni biçimlerin ağaçları.
            *   Tuvaldeki "unread" hâli kalkar.
        *   **Ölçeğe uymayan ölçüler:** panolar Pantrix'in ölçeğine çekilir; Summary'nin yazı ve aralıkları ile
            ağaç satırı 14/20 olur. CSV'nin `numeric` sütunu kodda olduğu gibi `text` kalır, çünkü CSV'de tür
            yok; pano düzeltilir.
        *   **Seçenekler:**
            *   v1'de yalnızca iki seçenek gelir: Android'in uygulama başına dili (`localeConfig`, İngilizce ve
                Türkçe) ve CSV'de "ilk satır başlık".
            *   YAML yazıcısının kuralları ve YAML 1.2 olduğu gibi kalır; YAML 1.1 seçeneği yok.
            *   CSV'de ayırıcı ve hizalama seçeneği yok.
        *   **Telefon:**
            *   Invalid'de panodaki gibi üç görünüm.
            *   Biçim, durum çubuğundaki biçim yazısına basınca açılan menüden seçilir.
            *   Bul ve değiştir, "more" menüsünden açılır.
            *   Üçü önce panolara çizilir.
        *   **Öteki kararlar:**
            *   Ağacın kökü `$ {4}` diye yazılır.
            *   Hiçbir değer seçili değilken grafik kökü göstererek açılır.
            *   Web'de paylaşım bağlantısı sayfanın kendi adresiyle kurulur.
            *   Büyük belgelerde v1'e giren yalnızca ölçüm: masaüstü penceresinde 50 MB ve JavaScript yedeği.
                Web Worker, bellek ve iptal edilebilen okuma v1'den sonraya kalır.
            *   Resim değerleri simge yerine küçük resimle gösterilir (CLAUDE.md "images" diyor).
            *   Belge açıkken sürüklenip bırakılan dosya yeni bir sekmede açılır.
            *   Önizleme tabanları `core:designsystem`'da, ekran görüntüsü tabanları `core:testing`'de kalır.
                CLAUDE.md'nin "her taban tek modülde" kuralı MVI tabanları için okunur.
            *   CLAUDE.md'nin editörü hâlâ bekleyen iki cümlesi düzeltildi.
        *   **Formatico'yu artık ilgilendirmeyen üç bulgu** (316 JsonView, 331 yan yana karşılaştırma, 275
            MissingName): sahibinin kararıyla kütüphanede düzeltildi, üçü de kapandı. JsonView'in seçili yolu ve
            MissingName'in beklemesi 2026-11-10'da geldi; yan yana karşılaştırma `PantrixDiffView` oldu, Compare ona
            2026-11-11'de geçti.
    *   **Kimin işi (2026-11-10):** `PANTRIX_FINDINGS.md`'deki açık bulgular kütüphanenin, `OPEN_ITEMS.md`'deki
        maddeler ve denetimin çıkardıkları Formatico'nun. Ayrıntısı:
        *   **Kütüphanenin oturumu yapar.** 70 açık bulgu var, ikisi aynı (460 ve 478); başına sayı konanlar
            bulgunun dosyadaki satırı.
            *   **Kütüphanenin kodu (37):**
                *   Tema ve platform: 239 temanın koyu olup olmadığı okunamıyor, 407 dile göre küçük harf,
                    417 haftanın günü olmayan tarih, 526 iOS'ta cihazın dili, 562 adları birleştirme.
                *   Testler: 223 test temasının penceresi, 295 dialog 0×0.
                *   Bileşenler: 261 `onKeyboardAction`, 289 paylaşım hizmeti, 301 kapatılabilen sekme,
                    316 JsonView'in seçili yolu, 331 yan yana karşılaştırma, 336 DataTable'da hücre seçimi,
                    351 menüden yapıştırma, 356 PageScaffold'un başlığı, 361 boş hâl, 381 menünün başlığı ve
                    sondaki yazısı, 402 sık çubukta dokunma hedefi, 422 Breadcrumb'ın taşması, 432 FileDrop'un
                    sözcükleri, 437 sekmelerin çizgisi, 460 ve 478 SegmentButton grubunun adı, 472 satırın eylem
                    düğmesi, 550 paletin arama alanının adı, 610 taşan araç çubuğu.
                *   Simgeler: 427 `ChevronUp`, 555 kod simgesi.
                *   Kod editörü: 442 işaretin iletisinde düğme, 448 imlecin satırındaki işaret, 454 aralıkların
                    vurgusu (şimdiki eşleşmenin çerçevesi), 484 salt okunur editörün imleç satırı, 508 işaretli
                    satırın ekran okuyucuya söylenmesi.
                *   Ağaç: 532 seçili satırın biçimli etiketi, 538 tembel ağaç, 544 yüklenmemiş dalın çocuk
                    sayısı.
                *   Web: 275 kelimeler yüklenirken MissingName, 281 snackbar'ın iletisi ekran okuyucuya.
            *   **Tasarım sistemi (9):** 141 DataTable, 147 CodeBlock, 152 simgelerin kapalı listesi,
                157 açık Menu ve Popover, 162 `Button`'ın `loading`'i, 167 CodeEditor'ün stili ve renkleri,
                172 CodeEditor'ün bir satırdan açılması, 177 yerinde çizilen Dialog'un karartması, 412 bölme
                tutamacı (kütüphanenin yarısı geldi).
            *   **Site ve belgeler (21):**
                *   Kurulum: 13, 18, 23, 28, 33, 38, 43, 48, 53.
                *   Detekt: 185, 190, 195.
                *   API başvurusu ve "For AI": 203, 208.
                *   Test rehberi: 218, 229, 234, 251, 256.
                *   Web ve dil: 269 webpack eklentisi, 520 uygulamanın kendi sözcükleri.
            *   **MCP ve eklenti (2):** 213 ekipçe kurulum ve oturumun yenilenmesi, 245 eski jar'la cevap veren
                sunucu.
        *   **Formatico yapar:**
            *   `OPEN_ITEMS.md`'deki 56 maddenin 51'i: kapanacak üç madde, kararlar, küçük hatalar, tuval ile
                kodun eşlenmesi, özellikler ve performans, ekran görüntüleri ve cihazlar.
            *   CLAUDE.md denetiminin çıkardıkları:
                *   önizleme ve test eksikleri;
                *   Android ve JavaScript'te testler;
                *   bulgusunu anmayan yaklaşık 25 geçici çözümün yorumu;
                *   `EditorComing`'in silinmesi;
                *   lisans listesi;
                *   yapıştırmanın sınırı ve MiB ile MB farkı.
            *   Bulgulardan şimdi yapılabilenler:
                *   Query'de öteki eşleşmelerin `CodeRangeStyle.Highlight` ile işaretlenmesi (454'ün yarısı);
                *   tuvalde Workspace-834'ün tutamacı (412);
                *   460 ile 478'in birleştirilmesi.
        *   **Önce kütüphane, sonra Formatico:**
            *   `OPEN_ITEMS.md`'de: Start'ın bırakma alanının sözcükleri (432'yi bekliyor), durum çubuğunun boyu
                (402'yi bekliyor), sekmelerin kesilen ilki (önce bir bulgu olarak yazılmalı).
            *   Kütüphane her bulguyu düzelttikçe, Formatico'daki geçici çözümü sökülür.
        *   **İkisinin de elinde değil:** Compose Multiplatform'un iki maddesi: web konsolundaki `wasmExports`
            uyarısı ve Wasm'da kütüphane modüllerinin `binaries.executable()` gerektirmesi (CMP-4906).
        *   **Sahibinin kararı (2026-11-10):** 316, 331 ve 275 kütüphanede düzeltilir. Üçü de kapandı: 316 ve 275
            2026-11-10'da, 331 2026-11-11'de, Compare `PantrixDiffView`'a geçince.
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
    *   **İlerleme (2026-11-11):**
        *   Pantrix 0.2.1 yayımlandı. Formatico onu GitHub Packages'tan alıyor (`pantrix = "0.2.1"`, `f276dab`).
            Settings'in About satırı sürümü yazdığı için sekiz görüntü yeniden kaydedildi: altısı JVM'de, ikisi iOS'ta.
        *   Yerel derleme yolu kapandı (sahibinin kararı): `mavenLocal` bloğu kalktı, CLAUDE.md "Pantrix comes from its
            releases" diyor (`a41e931`). `pantrix-ui-local` yok; `pantrix-ui@pantrix-ui` 0.2.1 yalnızca proje
            kapsamında, MCP'si `pantrixui.com/mcp`. Yerel kapsamda ikinci bir kurulum çıktı ve kaldırıldı.
        *   Bütün hedefler 0.2.1 ile derlendi, testleri geçti.
        *   Depo herkese açık ve gönderildi (2026-11-12): `github.com/develappsancho/formatico-cmp`, dal `main`.
    *   **Sahibinin kararları (2026-11-12):**
        *   Windows şimdilik yok: 1.0.0 macOS DMG'si ve Android APK'sıyla çıkar. Değişti (2026-11-12, aşağıda): MSI bir
            arkadaşın Windows makinesinde yapılır.
        *   Android'in APK'sı sahibinin var olan yayın anahtarıyla imzalanır; anahtar ve parolaları depoda değil.
        *   İndirme menüsünün panolarını bu oturum çizer, sonra kodu.
        *   Sıra: web ve sunucu (`deploy.sh web`, GHCR, Coolify) → APK ve DMG'nin imzası → indirme menüsü →
            GitHub'da 1.0.0 sürümü (yayımlanmadan önce sahibinin onayı).
        *   Lisans: Apache-2.0 (`LICENSE`, README'de; imajların etiketinde de).
    *   **İlerleme (2026-11-12):**
        *   `deploy.sh web`: imajlar iki mimari için GHCR'ye, Coolify kaynağı yeniden kuruyor, canlı adreste `check.sh`.
            Adımları `docs/DEPLOY_GUIDE.md`'de, sitenin yayın rehberi gibi.
        *   Web ve sunucu yayında (2026-11-12): `https://formatico.pantrixui.com`, 1.0.0 (`2a27520`). İki paket
            GHCR'de herkese açık, sahibi Coolify'da kaynağı kurdu; `deploy.sh web` yayımladı ve `check.sh`'ın yedi
            denetimi canlıda geçti. Sertifika Let's Encrypt'ten; pantrixui.com yanında çalışmaya devam ediyor.
            Tarayıcıda Start açıldı, bir örnek metinde ve ağaçta çizildi.
        *   HSTS (sahibinin onayı, 2026-11-12): nginx yalnızca HTTPS ile gelen isteğe (Traefik'in
            `X-Forwarded-Proto`'su) `Strict-Transport-Security: max-age=31536000` gönderir, alt alan adları olmadan.
            `check.sh` HTTPS'te başlığı, düz HTTP'de yokluğunu ve vekilin HTTPS dediği istekte varlığını dener;
            tarayıcı testleri düz HTTP'de sunduğu için başlığı göndermez.
        *   APK'nın imzası: anahtarın dosyası ve parolaları `local.properties`'te (`storeFile`, `storePassword`,
            `keyAlias`, `keyPassword`), anahtar depoya girmiyor (`signing/` yok sayılıyor). `assembleRelease` imzalı
            APK veriyor (APK İmza Şeması v2), `versionCode` sürümden: 1.0.0 → 10000.
    *   **Sahibinin kararları (2026-11-12, ikinci):**
        *   Windows'un MSI'ı da 1.0.0'da: sahibinin Windows makinesi yok; depoya ortak (Write) eklenen bir arkadaş
            MSI'ı kendi Windows makinesinde yapar ve sürüme koyar. Ona yalnızca bir betiği çalıştırmak kalır
            (`deploy/windows.sh`, Git Bash'te). MSI imzasız: SmartScreen ilk açılışta uyarır.
        *   App Links (ve iOS yayımlanmadığı için Universal Links) yok: APK sürümde doğrudan bir bağlantıyla verilir;
            paylaşılan bir bağlantı web'de açılır.
        *   Sıra: DMG'nin hattı → indirme menüsü (Windows'la birlikte) → sürüm. Sürümün dosyaları menü kodlandıktan
            sonra, aynı commit'ten yapılır.
    *   **İlerleme (2026-11-12, ikinci):**
        *   `deploy.sh`'a dört kip: `desktop` (Developer ID ile imzalı, Apple'ın onayından geçmiş DMG; sitenin
            `macos-dmg.sh`'ı ve JVM'in hakları uyarlandı), `android` (imzalı APK, sürümü ve imzası denetli),
            `release` (GitHub'da taslak sürüm, DMG ve APK ile), `publish` (üç dosya da taslaktaysa yayımlar). Dosyalar
            `build/release`'te, sabit adlarla: `Formatico-macOS.dmg`, `Formatico-Android.apk`, `Formatico-Windows.msi`.
        *   Windows: `deploy/windows.sh` MSI'ı taslağın commit'inden yapar, taslağa koyar, notlara SHA-256'sını yazar.
            `app/desktopApp`'in Windows ayarları: sabit `upgradeUuid`, kullanıcıya kurulum (yönetici istemez), menü
            ve kısayol. `.gitattributes` betiklerin satır sonlarını Windows'ta da LF tutar. Masaüstü uygulaması
            Android SDK'sız derleniyor (bu Mac'te SDK'sız bir kopyada denendi). Betik, Gradle dışında, sahte bir
            Windows'ta sahte bir `gh` ile denendi; gerçek bir Windows'ta denenmedi.
        *   Adımlar `docs/DEPLOY_GUIDE.md`'nin 6–9. bölümlerinde.
        *   Windows sürümden bağımsız (sahibinin kararı, 2026-11-12): sürüm DMG ve APK ile yayımlanır (`publish` MSI'ı
            beklemez, yoksa söyler); arkadaş `deploy/windows.sh`'ı istediği gün çalıştırır, MSI taslağa ya da
            yayımlanmış sürüme konur. Deponun Immutable releases ayarı kapalı kalmalı (bugün kapalı). Betikler sahte bir
            Windows'ta 15 durumda denendi.
        *   İndirme menüsünün panoları kanvasta (2026-11-12, sürüm `1794507062-4c5b`; Start-1440'ı sahibi onayladı):
            *   Start: notun sonunda "Get the app" (masaüstünde ve web'de); Settings: About'ta "Get the app" satırı,
                **Download** düğmesiyle. Pantrix'in `Menu`'sü, yukarı açılır; telefonda (yalnızca tarayıcıda, Android
                uygulamasında yok) alttan açılan sayfa.
            *   Satırlar GitHub'ın `releases/latest`'inden: başlıkta sürüm ve günü, macOS (Apple silikon, DMG), Windows
                (64-bit, MSI), Android (11 ve sonrası, APK), her birinin boyutu; çizgiden sonra "Every release on
                GitHub". MSI henüz yoksa Windows satırı kapalı ("Not in this release yet").
            *   Bir dosya seçilince tarayıcı onu GitHub'dan indirir; snackbar SHA-256'sını verir, "Copy SHA-256" ile.
            *   Durumlar (Tweak `download`): kapalı, açık, yükleniyor, GitHub'a ulaşılamadı (sürümler sayfasına
                bağlantı), MSI'sız, indiriliyor; telefonda ayrıca `web`.
            *   Settings'in About satırı "Pantrix UI 0.2.1" diyor (0.1.2-SNAPSHOT'tan düzeltildi).
        *   İndirme menüsü kodda (2026-11-12):
            *   `core:network`'te `Releases`: GitHub'ın `releases/latest`'i; bulunan sürüm uygulama açık kaldıkça tutulur
                (GitHub bir adrese saatte 60 kez yanıt verir), bulunamayan bir dahaki açılışta yeniden sorulur. 404
                "henüz sürüm yok", öteki hatalar "GitHub'a ulaşılamadı".
            *   Yeni modül `feature:download`: MVI view model'i, Pantrix'in `Menu`'süyle menü (dar pencerede alttan
                açılan sayfa), Türkçe ve İngilizce sözcükleri. Start'ın ve Settings'in düğmeleri menüyü
                `LocalDownloadMenu`'den bulur: uygulama GitHub'a soranı verir, önizleme ve testler açılmayan bir
                düğme çizer.
            *   Yalnızca masaüstünde ve web'de (`offersDownloads`); Android uygulamasında ve iOS'ta yok. Bir dosya
                seçilince tarayıcı onu GitHub'dan indirir, snackbar SHA-256'sını verir, "Copy SHA-256" kopyalar.
            *   Panolardan ayrılan: boyutlar uygulamanın her yerindeki gibi Pantrix'in `fileSize`'ıyla ("95 MB";
                panolarda "94.7 MB"); panolarda olmayan bir durum daha var, "GitHub'da henüz sürüm yok".
            *   Settings'in kenar çubuğunda About hiç seçilmiyordu (kısa ve sayfanın sonunda olduğu için en üste
                çıkamıyor): basılan ya da açılan bölüm sayfa orada durdukça seçili kalır, sayfanın sonunda son bölüm
                seçilir. Testi var.
            *   Testler bütün hedeflerde geçti: JVM, iOS simülatörü, JS, Wasm, Android (Robolectric) ve Chrome'daki
                tarayıcı testleri; yerel yığında web'den GitHub'ın API'sine gidildi, sürüm yok yanıtı menüde göründü.
                Ekran görüntüleri: menünün 30'u yeni; Start'ın ve Settings'in 1440 görüntüleri yeniden kaydedildi
                (834 ve 390 Android olduğu için değişmedi).
        *   Taslak sürüm açıldı (2026-11-12, sahibinin onayı): `v1.0.0`, hedefi `872ba25`, yalnızca depo ortaklarına
            açık. `Formatico-macOS.dmg` (94,8 MB, Developer ID ile imzalı, Apple onaylı) ve `Formatico-Android.apk`
            (42,0 MB, yayın anahtarıyla imzalı); GitHub'ın verdiği SHA-256'lar dosyalarınkiyle aynı. Kalan: arkadaşın
            MSI'ı (`deploy/windows.sh`), sahibinin yayımlaması (`deploy/deploy.sh publish`) ve web (`deploy.sh web`).
        *   **Formatico 1.0.0 yayımlandı** (2026-11-12, sahibinin kararıyla MSI'ı beklemeden): GitHub'ın son sürümü,
            etiketi `v1.0.0` (`872ba25`). `releases/latest/download/` bağlantıları DMG'yi ve APK'yı veriyor; MSI'ı
            arkadaş `deploy/windows.sh` ile yayımlanmış sürüme ekleyecek. Kalan: MSI ve web (`deploy.sh web`).
        *   Web yeniden yayında (2026-11-12, `dc45aa4`): indirme menüsü, HSTS ve Wasm konsolunun düzeltmesi.
            `check.sh`'ın sekiz denetimi canlıda geçti; tarayıcıda menü 1.0.0'ı gösteriyor (DMG ve APK, Windows "henüz
            yok"), temiz bir sekmenin konsolunda hata yok. Kalan: arkadaşın MSI'ı.
        *   **1.0.0'ın DMG'si açılmıyor** (2026-11-12, sahibi indirip kurunca gördü): "java/net/http/HttpClient$Version".
            Compose'un uygulamaya koyduğu Java ortamı yalnızca sayılan modülleri tutar ve hiçbiri sayılmamıştı: `jdeps`'e
            göre `java.net.http` (Ktor), `java.management` (KSafe, Ktor), `jdk.unsupported` ve `java.instrument`
            (kotlinx.coroutines) eksikti. Yayından önce uygulama açılıp penceresine bakılmamıştı: ilk denemede yalnızca
            sürecin ayakta olduğuna bakıldı, onu da hata penceresi ayakta tutuyordu.
            *   Düzeltme: `app/desktopApp`'te `modules(...)` (bu dört modül, ayrıca Windows'un ekran okuyucusu için
                `jdk.accessibility` ve kodlamalar için `jdk.charsets`).
            *   İki denetim, her DMG'de: `runtime-modules-check.sh` (jdeps'in istediği her modül ortamda mı; Windows'ta
                da) ve `macos-launch-check.sh` (DMG'deki uygulama açılıyor mu: ana penceresi, istisnasız günlük). İkisi
                de bozuk 1.0.0'ı yakalıyor, düzeltilmişi geçiriyor; DMG'nin imzalı ve sıkılaştırılmış hâli de açıldı.
            *   Arkadaşın MSI'ı bu düzeltmeyi beklemeli: 1.0.0'ın commit'inden yapılan MSI aynı hatayla açılmaz.
        *   **Formatico 1.0.1 yayımlandı** (2026-11-12, sahibinin onayıyla): etiketi `v1.0.1` (`47c3aa5`), GitHub'ın son
            sürümü. DMG 97,2 MB (iki denetimden geçti, Apple onaylı), APK 1.0.1 (`versionCode` 10001); GitHub'ın verdiği
            SHA-256'lar yerel dosyalarınkiyle aynı. Notlarının başında neyi düzelttiği yazıyor. 1.0.0'ın bozuk DMG'si
            o sürümden silindi (sahibinin onayı), notları macOS için 1.0.1'i gösteriyor.
        *   Web 1.0.1'de (2026-11-12, `5a9f371`): sekiz denetim canlıda geçti. Tarayıcıda About "Formatico 1.0.1" ve
            "Get the app" satırı, kenar çubuğunda About seçili; menü 1.0.1'in DMG'sini (97 MB) ve APK'sını (42 MB)
            gösteriyor, Windows "henüz yok"; temiz bir sekmenin konsolunda hata yok. Kalan: arkadaşın MSI'ı
            (`47c3aa5`'ten) ve Coolify panelinin 8000, 6001, 6002 portlarının kapanması.
        *   Masaüstünde anahtar zinciri sorusu (2026-11-12): kurulu uygulama her açılışta giriş parolasını soruyordu.
            KSafe anahtarlarını herkesin ortak adıyla (`eu.anifantakis.ksafe.shared`) tutuyordu; kayıtları koddan
            çalıştırma yapmıştı ve anahtar zinciri onları yalnızca o Java'ya sormadan veriyordu. Artık kurulu uygulama
            (jpackage'ın başlatıcısı `jpackage.app-path`'i verir) anahtarlarını kendi adıyla
            (`com.developersancho.formatico`) tutuyor; koddan çalıştırma ayrı bir ad ve ayrı bir klasör
            (`Formatico Development`) kullanıyor. Eski kurulumun verisini KSafe yeni ada bir kez taşıyor: anahtar
            zincirinde atılacak adlarla bir denemede eski anahtarla yazılmış değer ve şifreli metin yeni adla okundu.
            Açılış denetimi anahtarlarını kendi geçici klasöründe tutuyor (`-Dksafe.jvm.keyVault=software`) ve
            pencereye her masaüstü alanında (Space) bakıyor: önde tam ekran bir uygulama varken pencere başka alanda
            açılıyordu.
        *   **Formatico 1.0.2 yayımlandı** (2026-11-12, sahibinin onayıyla): etiketi `v1.0.2` (`addca45`), GitHub'ın son
            sürümü, anahtar zinciri düzeltmesiyle. DMG 97,5 MB (iki denetimden geçti, Apple onaylı; açılış denetimi
            anahtar zincirine kayıt eklemedi), APK 1.0.2 (`versionCode` 10002); GitHub'ın verdiği SHA-256'lar yerel
            dosyalarınkiyle aynı, `releases/latest/download` bağlantıları 1.0.2'nin dosyalarını veriyor. Notlarının
            başında neyi düzelttiği yazıyor. Sahibinin Mac'inde 1.0.2'nin ilk açılışta bir kez sorup sonra sormadığı
            henüz görülmedi.
        *   Web 1.0.2'de (2026-11-12, `9423c88`): sekiz denetim canlıda geçti. Tarayıcıda About "Formatico 1.0.2"; menü
            "released Nov 12, 2026" ile 1.0.2'nin DMG'sini (97 MB) ve APK'sını (42 MB) gösteriyor, Windows "henüz yok";
            temiz bir sekmenin konsolunda hata yok. Kalan: arkadaşın MSI'ı (`v1.0.2`'den) ve Coolify panelinin 8000,
            6001, 6002 portlarının kapanması.

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
        Karara bağlandı (2026-11-12): şimdilik yok. Aynı gün değişti: MSI, bir arkadaşın Windows makinesinde
        (`deploy/windows.sh`), imzasız.
