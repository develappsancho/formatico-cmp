# Açık Maddeler

Formatico'nun kendisinde göze çarpan, eldeki işin dışında kalan her şey. Pantrix UI'ın eksikleri burada değil,
[`PANTRIX_FINDINGS.md`](PANTRIX_FINDINGS.md)'de.

Her madde: **ne**, **nerede**, **öneri**, **durum**. Kapanan madde silinmez: ne zaman ve neyle kapandığı yazılır.

## Kurulum

*   [x] **Sihirbazın projesi olduğu gibi derlenmiyordu (2026-11-02).**
    *   Ne: `Greeting.kt` sihirbazın silinmiş `core` modülündeki `sayHello`'yu çağırıyordu; README var olmayan `/core` ve `/server` modüllerini, kök `build.gradle.kts` sunucusuz bir projede Ktor eklentisini anıyordu.
    *   Durum: kapandı (2026-11-02). `Greeting`, `Platform` ve örnek çizim silindi, Ktor ve Material katalogdan çıktı, README yeniden yazıldı.
*   [x] **iOS paket kimliği CLAUDE.md'dekinden farklıydı (2026-11-02).**
    *   Ne: `Config.xcconfig` `com.developersancho.formatico.Formatico$(TEAM_ID)` diyordu; CLAUDE.md `com.developersancho.formatico` diyor.
    *   Durum: kapandı (2026-11-02). `PRODUCT_BUNDLE_IDENTIFIER=com.developersancho.formatico`; masaüstünün macOS paket kimliği de aynı.
*   [x] **Koin ve navigation3 henüz bağlı değil (2026-11-02).**
    *   Ne: Yığının ikisi de sahibinin kararı; ama tuvalde ekran panosu yok, ilk özellik modülü de yok. Boş bir Koin modülü ve tek hedefli bir gezinti, sınanamayan kod olurdu.
    *   Öneri: ilk özellik (Start ya da Workspace) panosu çizilince bir `formatico.kmp.feature` convention eklentisi: `core:base`, `core:designsystem`, `koin-compose-viewmodel`, `navigation3-ui`. Sürümler bugün Koin 4.2.2, navigation3 1.2.0.
    *   Durum: kapandı (2026-11-03). `formatico.feature` eklentisi `core:base`, `core:designsystem`, coroutines, lifecycle ve Koin'i getiriyor; gezinti özelliklerde değil uygulamada (`app:shared`): `Destination` anahtarları, `rememberNavBackStack` ve `NavDisplay`. İlk özellik `feature:start`.
*   [ ] **Web: derleyen makinenin yolları betiklerde kalabilir (2026-11-02).**
    *   Ne: Klib'lerin yolları `-Xklib-relative-path-base` ile köke göre yazılıyor (convention eklentilerinde). Ama Skia'nın betiğindeki `import.meta.url`, webpack'in yazdığı `file:///Users/…` yolu olarak kalıyor; Pantrix'in rehberi bunu bir webpack eklentisiyle (`stripFilePaths.js`) siliyor, eklentinin kendisini vermiyor (bulgu 1.x).
    *   Öneri: yayından önce `webpack.config.d/` altına kendi eklentimiz ve dağıtımda `strings | grep /Users/` denetimi.
    *   Durum: açık.
*   [x] **Web: yükleme ekranının zemini temanınkinden farklıydı (2026-11-02).**
    *   Ne: `styles.css` koyu zemini `#0f172a` diye yazıyordu; Pantrix'in koyu `background`'ı `#020617` (Android ekran görüntüsünden okundu; açık zemin `#f8fafc`, aynı). İlk karede bir renk sıçraması olurdu.
    *   Durum: kapandı (2026-11-02). `styles.css` koyu zemini `#020617`. Değerler elle yazılı: Pantrix'in renkleri değişirse burası da değişmeli.
*   [ ] **Web: konsolda Compose'un kaynak okuyucusundan bir hata satırı (2026-11-02).**
    *   Ne: Wasm derlemesi açılınca konsola "Accessing `memory` via `wasmExports` is deprecated" yazılıyor. Kaynağı Compose Multiplatform 1.12.2'nin `org.jetbrains.compose.resources.copyArrayBufferToWasmMemory`'si (Kotlin'in ürettiği `import-object.mjs`); Formatico'nun ya da Pantrix'in kodu değil. Uygulama çalışıyor.
    *   Öneri: Compose Multiplatform'un sonraki sürümünde yeniden bakmak.
    *   Durum: açık.
*   [ ] **iOS: ilk kare beyaz (2026-11-02).**
    *   Ne: Simülatörde uygulama açılırken Compose çizene kadar ekran bir an beyaz; koyu temada bu bir parlama olur. `Info.plist`'in açılış ekranı boş.
    *   Öneri: açılış ekranına temanın zemini (`#f8fafc` / `#020617`) için bir renk varlığı.
    *   Durum: açık.
*   [ ] **Uygulama simgeleri sihirbazınki (2026-11-02).**
    *   Ne: Android'in açılış ekranında ve başlatıcıda sihirbazın Android simgesi, iOS'ta sihirbazın simgesi, web'de favicon yok.
    *   Öneri: tuvalin kimlik panosundaki `{ }` işaretinden bir simge seti; tuvale bir "simge" panosu.
    *   Durum: açık.

## core:format

*   [ ] **Büyük dosyalar ölçülmedi (2026-11-02).**
    *   Ne: Hedef masaüstünde 50 MB, telefonda ve web'de 10 MB. JSON okuyucusu her belirteci ve her değeri bir nesne olarak tutuyor, iç içe değerleri özyinelemeyle okuyor (512 düzeyle sınırlı).
    *   Öneri: 10 ve 50 MB'lık örneklerle her hedefte süre ve bellek ölçümü; gerekirse belirteçleri dizilerde tutmak ve okuyucuyu yığıtla yazmak.
    *   Durum: açık.
*   [ ] **Sütun UTF-16 birimiyle sayılıyor (2026-11-02).**
    *   Ne: `TextPosition.column` UTF-16 birimi sayıyor; bir emoji iki sütun sayılır. Editörlerin çoğu kod noktası ya da görünen karakter sayar.
    *   Öneri: PantrixCodeEditor'ın imleci neyi sayıyorsa onu sayan bir dönüşüm.
    *   Durum: açık.
*   [ ] **Kaynaklarda görünmeyen karakterler kaçış dizisiyle yazılmalı (2026-11-02).**
    *   Ne: `'﻿'`, `' '` gibi karakterler dosyaya kendileri olarak girerse Kotlinter'ın biçimlendiricisi BOM'u siliyor ve `''` derlenmiyor. Bu oturumda öyle oldu ve düzeltildi.
    *   Öneri: detekt'e ya da bir teste "Kotlin kaynağında görünmeyen karakter yok" denetimi.
    *   Durum: açık.
*   [ ] **Biçimlerin yalnızca JSON ailesi okunuyor (2026-11-02).**
    *   Ne: `core:format`'ta bugün JSON, JSONC ve JSON5 okuyucusu, biçim algılama, satır ve sütunlu problemler var. Biçimlendirme, küçültme, onarım, NDJSON okuyucusu, XML, YAML (`snakeyaml-engine-kmp` ve kendi yazıcımız), TOML, CSV/TSV, properties/INI sırada.
    *   Durum: açık.

## Start

*   [x] **Açılan belge bir yere gitmiyor (2026-11-03).**
    *   Ne: Start bir belgeyi açınca `StartEffect.OpenDocument` gönderiyor; uygulama onu şimdilik bırakıyor (`App.kt`, `StartRoute(onOpenDocument = {})`). Belge yalnızca Recent'e ekleniyor.
    *   Öneri: Workspace yazılınca `Destination.Workspace` ve belgenin sekmesi.
    *   Durum: kapandı (2026-11-03). Açılan belge `Workbench`'e giriyor, kendi sekmesinde seçiliyor ve geri yığınında Start'ın üstüne `Destination.Workspace(documentId)` geliyor.
*   [ ] **Recent yalnızca uygulama açıkken tutuluyor (2026-11-03).**
    *   Ne: `SessionDocumentHistory` bellekte; uygulama kapanınca geçmiş gidiyor. Her açılışta Recent'in boş hâli görünüyor. Tasarım "History stays on this device" diyor.
    *   Öneri: cihazda bir depo: üst bilgiler için küçük bir anahtar-değer deposu, içerik için dosyalar (web'de tarayıcının deposu). 50 MB'lık bir belgenin içeriği bir ayar deposuna sığmaz; ayrı bir tasarım kararı.
    *   Durum: açık.
*   [ ] **Üst çubuğun bazı düğmeleri basılmıyor (2026-11-03).**
    *   Ne: Başlatıcı (komut paleti) ve Ayarlar kendi ekranları gelene kadar `enabled = false`; tek sekme var ve yeni sekme düğmesi çizilmiyor.
    *   Öneri: Palette, Settings ve sekmeler yazıldıkça bağlamak.
    *   Durum: açık. Sekmeler ve yeni sekme düğmesi Workspace'le bağlandı (2026-11-03); başlatıcı ve Ayarlar hâlâ basılmıyor. Workspace'in Query, Convert, Compare, ağaçta arama ve Table görünümü de kendi ekranlarını bekliyor.
*   [ ] **Temanın seçimi saklanmıyor (2026-11-03).**
    *   Ne: Çubuğun tema düğmesi seçimi `rememberSaveable` ile tutuyor; uygulama yeniden açılınca sistemin teması geri geliyor.
    *   Öneri: Settings ekranıyla birlikte cihazda saklamak.
    *   Durum: açık.
*   [ ] **Recent "edited" diyor, belge yalnızca açılmış (2026-11-03).**
    *   Ne: Tasarımın metni "edited 2 minutes ago". Workspace olmadığı için bir belge düzenlenemiyor; Recent'teki zaman açıldığı an.
    *   Öneri: Workspace düzenlemeleri geçmişe yazınca zaman kendiliğinden doğru olur; o zamana kadar bilinçli bir sapma.
    *   Durum: açık. Workspace'te Format, Minify ve Sort keys belgeyi değiştiriyor (2026-11-03), ama değişiklik Recent'e yazılmıyor: geçmiş açılan metni ve açıldığı anı tutuyor. Kalıcı geçmişle birlikte ele alınmalı.
*   [x] **Ülkeler örneği 249 satır (2026-11-03).**
    *   Ne: Panolar "250 rows" diyordu; ISO 3166-1'de 249 kayıt var. Panolar ve metin 249'a düzeltildi.
    *   Durum: kapandı (2026-11-03).
*   [ ] **Tarayıcıda metne bakan UI testleri atlanıyor (2026-11-03).**
    *   Ne: `StartScreenTest` ve `AppTest` masaüstünde ve iOS'ta geçiyor; `wasmJsBrowserTest`'te ekranın metinleri (Compose kaynakları) test beklerken hiç gelmiyor. Karma'nın kaynakları sunup `/composeResources/`'u oraya yönlendirmesi ve Mocha'nın süresini uzatmak sonucu değiştirmedi. Olası neden: `waitUntil` tarayıcının olay döngüsünü bırakmıyor, `fetch` tamamlanamıyor. İki sınıf web'de `@IgnoreInBrowser` ile atlanıyor (`kotlin.test.Ignore`, yalnız `webTest`'te); görünüm modeli ve zaman testleri tarayıcıda koşuyor.
    *   Öneri: Compose'un web testlerinde kaynakları önceden yükleme yolu (`preloadResources` ya da askıya alınan bir bekleme) ya da metne değil etikete bakan ayrı bir tarayıcı testi; Maestro'nun web akışı da bir yol.
    *   Durum: açık.
*   [ ] **Compose'un kütüphane modülleri Wasm'da çalıştırılabilir ikili üretiyor (2026-11-03).**
    *   Ne: Compose, `commonTest`'te UI testi olan bir modülün Wasm testini, hedef `binaries.executable()` demedikçe çalıştırmıyor (CMP-4906). `formatico.kmp.compose` bunu her çizen modüle ekliyor; kütüphane yine kütüphane olarak derleniyor, ama her modül için bir de çalıştırılabilir çıktı kuruluyor.
    *   Öneri: Compose bu kısıtı kaldırınca geri almak.
    *   Durum: açık.

## Workspace

*   [ ] **Metin bölmesi editörü bekliyor (2026-11-03).**
    *   Ne: Sahibinin kararıyla (2026-11-03) metin bölmesi `PantrixCodeEditor` bir sürümde gelene kadar boş: "Text editing is on its way". Metin ile ağacın seçimi birlikte yürümüyor; durum çubuğunda "Ln, Col" yok; telefondaki "Show in text" basılmıyor. Belge yalnızca ağaçta okunuyor, Format, Minify, Sort keys, Copy ve Save as a file metnin tamamında çalışıyor.
    *   Öneri: Pantrix'in editörü yayımlanınca metin bölmesi, seçimin iki yönlü bağı ve imlecin satırı ile sütunu.
    *   Durum: açık.
*   [ ] **Yalnızca JSON ailesi okunuyor; dört örneğin üçü okunmuyor (2026-11-03).**
    *   Ne: Workspace'in ağacı ve düzenlemeleri JSON, JSONC ve JSON5'te çalışıyor. YAML, TOML, CSV ve diğerleri "isn’t read yet" diyor; Start'ın dört örneğinden üçü (deployment.yaml, Cargo.toml, countries.csv) bugün yalnızca bu hâli gösteriyor. NDJSON da okunmuyor.
    *   Öneri: `core:format`'ın sıradaki okuyucuları; ilk olarak YAML (`snakeyaml-engine-kmp`) ve CSV, çünkü örnekleri var.
    *   Durum: açık.
*   [ ] **Geri alma tek adım ve yalnızca bildirimden (2026-11-03).**
    *   Ne: Format, Minify ve Sort keys bir "Undo" bildirimi gösteriyor; bildirim kapanınca önceki metin gidiyor. Minify yorumları siliyor: bildirim kapandıktan sonra JSONC'nin yorumları geri gelmez. Telefonda eylemli bildirim, kapatılana kadar alttaki araç çubuğunun üstünde duruyor (Pantrix'in kuralı: eylemli bildirim kendiliğinden kapanmaz).
    *   Öneri: editörle birlikte belge başına bir geri alma geçmişi (`⌘Z`); o zamana kadar yorumu olan bir belgeyi küçültmeden önce uyarmak ya da yorumlar silinecekse bildirimde söylemek.
    *   Durum: açık.
*   [ ] **Açık belgeler uygulama kapanınca gidiyor (2026-11-03).**
    *   Ne: `Workbench` belgeleri bellekte tutuyor; uygulama kapanınca sekmeler de içerikleri de gidiyor. Android süreci öldürülürse geri yığını `Workspace(id)`'yi geri getiriyor, `Workbench` boş olduğu için Start'a dönülüyor.
    *   Öneri: kalıcı geçmişle aynı depo: açık sekmelerin listesi ve içerikleri cihazda.
    *   Durum: açık.
*   [ ] **Telefonda Start'tan açık belgelere dönülemiyor (2026-11-03).**
    *   Ne: Telefonda geri tuşu belgeyi açık bırakıp Start'a dönüyor. Start'ın çubuğunda belge değiştirici yok (panoda da yok); açık belgeye ancak Recent'ten ya da aynı örnekten dönülüyor (aynı ad ve metin, açık sekmeyi seçiyor).
    *   Öneri: telefonda Start'ın çubuğunda, açık belge varken değiştiriciyi ve sayısını göstermek; önce tuvalde Start-390'a çizilmeli.
    *   Durum: açık.
*   [ ] **Biçimlendirme sondaki virgülü siliyor, anahtarla değer arasındaki yorumu öne alıyor (2026-11-03).**
    *   Ne: `JsonDocument.formatted` JSONC ve JSON5'te sondaki virgülü yazmıyor; bir üyenin anahtarıyla değeri arasındaki yorum (`"b": /* not */ 2`) üyenin önüne kendi satırına geçiyor. İkisi de belgeyi geçerli bırakıyor, ama yazanın biçimi değil.
    *   Öneri: Settings'e "sondaki virgülü koru" seçeneği; ya da JSON5'te varsayılan olarak korumak.
    *   Durum: açık.
*   [ ] **Değer önizlemesi yalnızca sözcük (2026-11-03).**
    *   Ne: Panolarda ağacın satırı bir renk örneği, bir bağlantı simgesi ve bir küçük resim gösteriyor; `PantrixTreeView` satırın sonuna yalnızca metin aldığı için Formatico "Colour", "Link", "Picture" yazıyor. Tarih, okuyucunun dilinde UTC olarak yazılıyor. Bkz. `PANTRIX_FINDINGS.md`.
    *   Durum: açık.
*   [ ] **Tabletin metin ile ağaç arasındaki ayırıcısı sürüklenmiyor (2026-11-03).**
    *   Ne: Workspace-834 panosundaki "Resize the text and the tree" tutamacı çizilmiyor; metin 480 dp, ağaç geri kalanı. Bkz. `PANTRIX_FINDINGS.md`.
    *   Durum: açık.
*   [ ] **Koin'in bağlamı `App()` bitince kapanmıyor (2026-11-03).**
    *   Ne: Masaüstünün UI testlerinde bir testin açtığı belgeler bir sonraki testin `App()`'inde açık duruyor: `KoinApplication` (Koin 4.2.2) aynı süreçte bağlamını yeniden kullanıyor, `Workbench` tekil olarak yaşıyor. Uygulamada istenen bu (döndürmede sekmeler kalıyor), testlerde ise sıra bağımlılığı demek. `AppTest` bugün geçiyor, çünkü belge açan tek test sonunda yeni sekmeye dönüyor.
    *   Öneri: `App()`'e testin vereceği bir Koin yapılandırması ya da testlerde `stopKoin()`; Koin'in `KoinApplication` belgesinde bu davranışa bakmak.
    *   Durum: açık.
