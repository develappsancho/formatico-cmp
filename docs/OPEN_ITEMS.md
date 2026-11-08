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
*   [x] **Web: derleyen makinenin yolları betiklerde kalabilir (2026-11-02).**
    *   Ne: Klib'lerin yolları `-Xklib-relative-path-base` ile köke göre yazılıyor (convention eklentilerinde). Ama Skia'nın betiğindeki `import.meta.url`, webpack'in yazdığı `file:///Users/…` yolu olarak kalıyor; Pantrix'in rehberi bunu bir webpack eklentisiyle (`stripFilePaths.js`) siliyor, eklentinin kendisini vermiyor (bulgu 1.x).
    *   Öneri: yayından önce `webpack.config.d/` altına kendi eklentimiz ve dağıtımda `strings | grep /Users/` denetimi.
    *   Durum: kapandı (2026-11-03). Dağıtımda iki betikte `file:///Users/…/kotlin/skiko.mjs` vardı. `config/webpack/strip-file-paths.js` (her iki derlemenin `configDirectory`'si) betiklerdeki `file:///…/` yollarını dosyanın adına indiriyor; `copyJsApp` bittikten sonra dağıtım klasörü deponun yolunu ya da kullanıcının ev klasörünü içeriyorsa derleme duruyor.
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
    *   Durum: düzeltildi, gözle doğrulanacak (2026-11-03). `Assets.xcassets/LaunchBackground` açıkta `#F8FAFC`, koyuda `#020617`; `Info.plist`'in `UILaunchScreen`'i onu gösteriyor. Derlenen uygulamanın `Info.plist`'inde görüldü; açılışın ilk karesi gözle görülmedi.
*   [x] **Uygulama simgeleri sihirbazınki (2026-11-02).**
    *   Ne: Android'in açılış ekranında ve başlatıcıda sihirbazın Android simgesi, iOS'ta sihirbazın simgesi, web'de favicon yok.
    *   Öneri: tuvalin kimlik panosundaki `{ }` işaretinden bir simge seti; tuvale bir "simge" panosu.
    *   Durum: kapandı (2026-11-04). Simgeler tuvalin simge panosundan (sürüm 71) `tools/icons/generate.py` ile çiziliyor: uygulamanın kendi Geist Mono'su, tek bir geometri. Android'de uyarlanabilir simge (arka plan, vektör ön plan, temalı simge için tek renk; minSdk 30 olduğundan sihirbazın PNG'leri silindi), iOS'ta 1024 px açık, koyu ve renklendirilmiş, masaüstünde `.icns`, `.ico`, `.png` ve pencerenin simgesi, web'de `favicon.svg`, `favicon.ico`, dokunma simgesi, PWA simgeleri ve `manifest.webmanifest`. Android'in başlatıcısında ve iOS'un ana ekranında görüldü; iOS'un koyu ve renklendirilmiş simgeleri, masaüstünün paketleri ve penceresi gözle görülmedi. Bir not: `drawable-v24/ic_launcher_foreground.xml` silinip aynı ad `drawable/`'a yazılınca Android'in artımlı birleştirmesi onu düşürdü ve bu bozuk çıktı Gradle'ın yapı önbelleğine girdi; görev yeniden çalıştırılıp önbelleğe doğrusu yazıldı.

*   [ ] **Ekran görüntüleri yalnızca masaüstünde (2026-11-03).**
    *   Ne: Roborazzi 1.77.0 `app/shared`'in masaüstü testlerinde: `BoardScreenshotTest` uygulamayı panoların üç genişliğinde (1440×900, 834×1194, 390×844) açık temada çiziyor, Start, Workspace, YAML ve Table olarak 12 resim `app/shared/screenshots/`'ta. `recordRoborazziJvm` yazıyor, `verifyRoborazziJvm` karşılaştırıyor. Android (Robolectric) ve iOS'un kendi resimleri, koyu tema ve panolarla gözle karşılaştırma yok.
    *   Öneri: Faz 15'te (`PLAN.md`) her pano ve her platform.
    *   Durum: açık.

## core:format

*   [ ] **Büyük dosyalar ölçülmedi (2026-11-02).**
    *   Ne: Hedef masaüstünde 50 MB, telefonda ve web'de 10 MB. JSON okuyucusu her belirteci ve her değeri bir nesne olarak tutuyor, iç içe değerleri özyinelemeyle okuyor (512 düzeyle sınırlı).
    *   Öneri: 10 ve 50 MB'lık örneklerle her hedefte süre ve bellek ölçümü; gerekirse belirteçleri dizilerde tutmak ve okuyucuyu yığıtla yazmak.
    *   Durum: açık.
*   [x] **Sütun UTF-16 birimiyle sayılıyor (2026-11-02).**
    *   Ne: `TextPosition.column` UTF-16 birimi sayıyor; bir emoji iki sütun sayılır. Editörlerin çoğu kod noktası ya da görünen karakter sayar.
    *   Öneri: PantrixCodeEditor'ın imleci neyi sayıyorsa onu sayan bir dönüşüm.
    *   Durum: kapandı (2026-11-04). PantrixCodeEditor da bir yeri UTF-16 birimiyle sayıyor ("A place in the text counts its characters from nought", `String`'in karakterleri): durum çubuğundaki sütun editörünkiyle aynı; bir emoji ikisinde de iki sütun.
*   [x] **Kaynaklarda görünmeyen karakterler kaçış dizisiyle yazılmalı (2026-11-02).**
    *   Ne: `'﻿'`, `' '` gibi karakterler dosyaya kendileri olarak girerse Kotlinter'ın biçimlendiricisi BOM'u siliyor ve `''` derlenmiyor. Bu oturumda öyle oldu ve düzeltildi.
    *   Öneri: detekt'e ya da bir teste "Kotlin kaynağında görünmeyen karakter yok" denetimi.
    *   Durum: kapandı (2026-11-03). `formatico.quality` her modüle `checkInvisibleCharacters`'ı ekliyor (`check`'in parçası): `src` altındaki her `.kt` ve `.kts`'te bayt sırası işareti, bölünmeyen boşluk, yumuşak tire, genişliği olmayan boşluk ve birleştiriciler, yazı yönü işaretleri aranıyor; bulunan her biri dosya, satır ve sütunuyla, kaçış dizisi önerilerek bildiriliyor. Dikilmiş bir `\u00A0` ile denendi, yakaladı.
*   [ ] **Biçimlerin yalnızca JSON ailesi okunuyor (2026-11-02).**
    *   Ne: `core:format`'ta bugün JSON, JSONC ve JSON5 okuyucusu, biçim algılama, satır ve sütunlu problemler var. Biçimlendirme, küçültme, onarım, NDJSON okuyucusu, XML, YAML (`snakeyaml-engine-kmp` ve kendi yazıcımız), TOML, CSV/TSV, properties/INI sırada.
    *   Durum: açık. Biçimlendirme, küçültme ve anahtar sıralama JSON ailesinde yazıldı (2026-11-03); YAML `snakeyaml-engine-kmp` 5.0.0 ile okunuyor ve Formatico'nun kendi yazıcısıyla yazılıyor (2026-11-03). CSV ve TSV kendi okuyucumuzla okunuyor ve biçimlendiriliyor (2026-11-03): ayırıcı tahmini (`,` `;` sekme `|`), tırnaklı alanlar, BOM, kapanmayan tırnak ve alan sayısı problemleri. NDJSON, XML, TOML, properties/INI ve onarım sırada.

## Start

*   [x] **Açılan belge bir yere gitmiyor (2026-11-03).**
    *   Ne: Start bir belgeyi açınca `StartEffect.OpenDocument` gönderiyor; uygulama onu şimdilik bırakıyor (`App.kt`, `StartRoute(onOpenDocument = {})`). Belge yalnızca Recent'e ekleniyor.
    *   Öneri: Workspace yazılınca `Destination.Workspace` ve belgenin sekmesi.
    *   Durum: kapandı (2026-11-03). Açılan belge `Workbench`'e giriyor, kendi sekmesinde seçiliyor ve geri yığınında Start'ın üstüne `Destination.Workspace(documentId)` geliyor.
*   [ ] **Bırakma alanının sözcükleri panodakinden farklı (2026-11-03).**
    *   Ne: Start panoları "Drop a file here" ve "Choose a file" diyor, destek metni ("JSON, JSONC, … up to 50 MB") alanın içinde. Kodda `PantrixFileDrop` kendi sözcüklerini yazıyor: "Drop files here", "or choose files" (tek dosya alırken de çoğul); destek metni alanın altında. Masaüstünün ekran görüntüsünde görüldü (`app/shared/screenshots/Start-1440.png`).
    *   Öneri: Pantrix'in bulgusu kapanınca panodaki sözcükler; o zamana kadar ya pano koda uyar ya `label` panonun ilk satırını alır. Bkz. `PANTRIX_FINDINGS.md`.
    *   Durum: açık.
*   [x] **Recent yalnızca uygulama açıkken tutuluyor (2026-11-03).**
    *   Ne: `SessionDocumentHistory` bellekte; uygulama kapanınca geçmiş gidiyor. Her açılışta Recent'in boş hâli görünüyor. Tasarım "History stays on this device" diyor.
    *   Öneri: cihazda bir depo: üst bilgiler için küçük bir anahtar-değer deposu, içerik için dosyalar (web'de tarayıcının deposu). 50 MB'lık bir belgenin içeriği bir ayar deposuna sığmaz; ayrı bir tasarım kararı.
    *   Durum: kapandı (2026-11-04). Yeni `core:storage`: Recent'in listesi KSafe'te (sahibinin kararı, 2026-11-04), her belgenin metni kendi deposunda (`history-<id>`): Android, iOS ve masaüstünde uygulamanın klasöründe dosya, web'de IndexedDB. Android'de ikisi de `noBackupFilesDir`'de, iOS'ta metinlerin klasörü yedeğin dışında: belge bu cihazda kalıyor. Metin, belge Recent'ten yeniden açılınca okunuyor; yazmalar arka planda, sırayla.
*   [ ] **Üst çubuğun bazı düğmeleri basılmıyor (2026-11-03).**
    *   Ne: Başlatıcı (komut paleti) ve Ayarlar kendi ekranları gelene kadar `enabled = false`; tek sekme var ve yeni sekme düğmesi çizilmiyor.
    *   Öneri: Palette, Settings ve sekmeler yazıldıkça bağlamak.
    *   Durum: açık. Sekmeler ve yeni sekme düğmesi Workspace'le bağlandı (2026-11-03); başlatıcı ve Ayarlar hâlâ basılmıyor. Workspace'in Query, Convert, Compare, ağaçta arama ve Table görünümü de kendi ekranlarını bekliyor.
*   [x] **Temanın seçimi saklanmıyor (2026-11-03).**
    *   Ne: Çubuğun tema düğmesi seçimi `rememberSaveable` ile tutuyor; uygulama yeniden açılınca sistemin teması geri geliyor.
    *   Öneri: Settings ekranıyla birlikte cihazda saklamak.
    *   Durum: kapandı (2026-11-04). Çubuğun düğmesiyle seçilen tema KSafe'te (`theme`); uygulama açılınca okunuyor. Settings gelince aynı değeri yazacak.
*   [x] **Recent "edited" diyor, belge yalnızca açılmış (2026-11-03).**
    *   Ne: Tasarımın metni "edited 2 minutes ago". Workspace olmadığı için bir belge düzenlenemiyor; Recent'teki zaman açıldığı an.
    *   Öneri: Workspace düzenlemeleri geçmişe yazınca zaman kendiliğinden doğru olur; o zamana kadar bilinçli bir sapma.
    *   Durum: kapandı (2026-11-04). Recent açılan belgeye "opened …", Workspace'te değişen belgeye "edited …" diyor (Türkçesi "son açılış", "son düzenleme"). Açık belge history'deki yerini (`historyId`) taşıyor; uygulama bir belgenin metni değişince history'ye söylüyor (`followEdits`), belge başa geçiyor. Start panoları da öyle çizildi (tuval sürüm 70).
*   [x] **Ülkeler örneği 249 satır (2026-11-03).**
    *   Ne: Panolar "250 rows" diyordu; ISO 3166-1'de 249 kayıt var. Panolar ve metin 249'a düzeltildi.
    *   Durum: kapandı (2026-11-03).
*   [x] **Tarayıcıda metne bakan UI testleri atlanıyor (2026-11-03).**
    *   Ne: `StartScreenTest` ve `AppTest` masaüstünde ve iOS'ta geçiyor; `wasmJsBrowserTest`'te ekranın metinleri (Compose kaynakları) test beklerken hiç gelmiyor. Karma'nın kaynakları sunup `/composeResources/`'u oraya yönlendirmesi ve Mocha'nın süresini uzatmak sonucu değiştirmedi. Olası neden: `waitUntil` tarayıcının olay döngüsünü bırakmıyor, `fetch` tamamlanamıyor. İki sınıf web'de `@IgnoreInBrowser` ile atlanıyor (`kotlin.test.Ignore`, yalnız `webTest`'te); görünüm modeli ve zaman testleri tarayıcıda koşuyor.
    *   Öneri: Compose'un web testlerinde kaynakları önceden yükleme yolu (`preloadResources` ya da askıya alınan bir bekleme) ya da metne değil etikete bakan ayrı bir tarayıcı testi; Maestro'nun web akışı da bir yol.
    *   Durum: kapandı (2026-11-03). Üç şey birlikte: testler `waitUntil` yerine kendi askıya alınan beklemeleriyle bekliyor (`awaitText`: `Dispatchers.Default`'ta kısa bir `delay`, sonra `awaitIdle`), böylece tarayıcının olay döngüsü dönüyor ve `fetch` bitiyor; Karma test paketinin `kotlin/composeResources/`'unu sunuyor ve `/composeResources/`'u oraya yönlendiriyor (`config/karma/compose-resources.js`); Mocha'nın süresi 30 saniye (`config/karma/mocha-timeout.js`). `@IgnoreInBrowser` kalktı: 161 testin hepsi Wasm'da koşuyor. Kelimeler gelene kadar adı boş kalan satırları teşhislerin bildirmesi bir bulgu (`PANTRIX_FINDINGS.md`, "MissingName, kelimeler yüklenirken"); testler o arada bulunanı unutuyor.
*   [ ] **Compose'un kütüphane modülleri Wasm'da çalıştırılabilir ikili üretiyor (2026-11-03).**
    *   Ne: Compose, `commonTest`'te UI testi olan bir modülün Wasm testini, hedef `binaries.executable()` demedikçe çalıştırmıyor (CMP-4906). `formatico.kmp.compose` bunu her çizen modüle ekliyor; kütüphane yine kütüphane olarak derleniyor, ama her modül için bir de çalıştırılabilir çıktı kuruluyor.
    *   Öneri: Compose bu kısıtı kaldırınca geri almak.
    *   Durum: açık.

## Workspace

*   [x] **Belge sekmelerinin altında panoda olmayan bir çizgi (2026-11-03).**
    *   Ne: Geniş pencerede üst çubuğun sekmelerinin altında, "+" düğmesine kadar bir çizgi var; çubuğun kendi alt kenarıyla iki çizgi oluyor. Panolarda yalnızca seçili sekmenin çizgisi var. Çizgi `PantrixTabs`'ın; `TabsStyle`'da onu kaldıran bir alan yok, `TabsColors.divider` onun rengi. Masaüstünün ekran görüntüsünde görüldü (`Workspace-1440.png`).
    *   Durum: kapandı (2026-11-03). Üst çubuk `TabsColors(divider = Color.Transparent)` veriyor; ekran görüntüleri yeniden yazıldı. Bkz. `PANTRIX_FINDINGS.md`.
*   [ ] **Panoların bölge adları Compose'da karşılıksız (2026-11-04).**
    *   Ne: Panolar bölgelere ad veriyor: ağaç "Document tree", telefonda tablonun sıralama düğmeleri "Sort by", kayıtların listesi "$.orders, 248 records". HTML'de bu bir `aria-label`; Compose'da bir bölgeye ad verecek bir anlam (landmark) yok, `PantrixTreeView` de bir ad almıyor; Pantrix'in belgeleri bunun yolunu söylemiyor.
    *   Öneri: ekran okuyucuyla Android'de ve iOS'ta denemek; gerekirse bölümün başlığını bir başlık (`TextRole.Heading`) yapmak.
    *   Durum: açık. Tabletin More düğmesi panodaki gibi içindekileri söylüyor (2026-11-04): "More actions: sort keys, compare, save as a file".
*   [x] **Metin bölmesi editörü bekliyor (2026-11-03).**
    *   Ne: Sahibinin kararıyla (2026-11-03) metin bölmesi `PantrixCodeEditor` bir sürümde gelene kadar boş: "Text editing is on its way". Metin ile ağacın seçimi birlikte yürümüyor; durum çubuğunda "Ln, Col" yok; telefondaki "Show in text" basılmıyor. Belge yalnızca ağaçta okunuyor, Format, Minify, Sort keys, Copy ve Save as a file metnin tamamında çalışıyor.
    *   Öneri: Pantrix'in editörü yayımlanınca metin bölmesi, seçimin iki yönlü bağı ve imlecin satırı ile sütunu.
    *   Durum: kapandı (2026-11-04). Metin bölmesi `PantrixCodeEditor` (Pantrix'in yerel 0.1.2-SNAPSHOT'ı): her biçimin kendi renklendiricisi (JSON ailesi, YAML, CSV ve TSV, TOML, INI, properties, XML), Formatico'nun sözdizimi renkleri, belgenin girintisi. Yazılan duraklayınca belgeye gidiyor; Format, Minify ve Sort keys editöre geri alınabilir bir adım olarak geliyor. Ağaçta seçilen değer metinde seçiliyor, imlecin durduğu değer ağaçta seçiliyor. Durum çubuğunda "Ln, Col"; telefonda "Show in text" basılıyor.
*   [ ] **Yalnızca JSON ailesi okunuyor; dört örneğin üçü okunmuyor (2026-11-03).**
    *   Ne: Workspace'in ağacı ve düzenlemeleri JSON, JSONC ve JSON5'te çalışıyor. YAML, TOML, CSV ve diğerleri "isn’t read yet" diyor; Start'ın dört örneğinden üçü (deployment.yaml, Cargo.toml, countries.csv) bugün yalnızca bu hâli gösteriyor. NDJSON da okunmuyor.
    *   Öneri: `core:format`'ın sıradaki okuyucuları; ilk olarak YAML (`snakeyaml-engine-kmp`) ve CSV, çünkü örnekleri var.
    *   Durum: açık. YAML okunuyor (2026-11-03): deployment.yaml'ın ağacı, anchor'ları ve alias'ları görünüyor. countries.csv okunuyor (2026-11-03): Table'da açılıyor, ağaçta kayıtları görünüyor. Cargo.toml hâlâ "isn’t read yet" diyor.
*   [x] **Geri alma tek adım ve yalnızca bildirimden (2026-11-03).**
    *   Ne: Format, Minify ve Sort keys bir "Undo" bildirimi gösteriyor; bildirim kapanınca önceki metin gidiyor. Minify yorumları siliyor: bildirim kapandıktan sonra JSONC'nin ve YAML'ın yorumları geri gelmez. Telefonda eylemli bildirim, kapatılana kadar alttaki araç çubuğunun üstünde duruyor (Pantrix'in kuralı: eylemli bildirim kendiliğinden kapanmaz).
    *   Öneri: editörle birlikte belge başına bir geri alma geçmişi (`⌘Z`); o zamana kadar yorumu olan bir belgeyi küçültmeden önce uyarmak ya da yorumlar silinecekse bildirimde söylemek.
    *   Durum: kapandı (2026-11-04). Her açık belgenin editörü kendi geri alma geçmişini tutuyor (`Editors`), belge açık kaldıkça, Start'a gidip gelince de: ⌘Z ve telefonun tuş satırındaki geri al, yazılanı bir kelime bir kelime, Format'ı, Minify'ı ve Sort keys'i bir adımda geri alıyor. Bildirimdeki Undo da duruyor.
*   [x] **Açık belgeler uygulama kapanınca gidiyor (2026-11-03).**
    *   Ne: `Workbench` belgeleri bellekte tutuyor; uygulama kapanınca sekmeler de içerikleri de gidiyor. Android süreci öldürülürse geri yığını `Workspace(id)`'yi geri getiriyor, `Workbench` boş olduğu için Start'a dönülüyor.
    *   Öneri: kalıcı geçmişle aynı depo: açık sekmelerin listesi ve içerikleri cihazda.
    *   Durum: kapandı (2026-11-04). `WorkbenchKeeper` açık sekmeleri KSafe'te, metinleri `open-<id>` olarak tutuyor ve uygulama açılınca geri veriyor; seçili sekme de geliyor. Bir değişiklikten yarım saniye sonra yazıyor, yalnızca değişen metni. Maestro'nun `kept` akışı Android'de ve iOS'ta uygulamayı kapatıp açarak sınıyor.
*   [x] **Telefonda Start'tan açık belgelere dönülemiyor (2026-11-03).**
    *   Ne: Telefonda geri tuşu belgeyi açık bırakıp Start'a dönüyor. Start'ın çubuğunda belge değiştirici yok (panoda da yok); açık belgeye ancak Recent'ten ya da aynı örnekten dönülüyor (aynı ad ve metin, açık sekmeyi seçiyor).
    *   Öneri: telefonda Start'ın çubuğunda, açık belge varken değiştiriciyi ve sayısını göstermek; önce tuvalde Start-390'a çizilmeli.
    *   Durum: kapandı (2026-11-04). Start-390'a çizildi (durum `documents: open`, tuval sürüm 70): açık belge varken çubuk Workspace-390'daki değiştirici, "New tab" ve açık belgelerin sayısıyla. Kodda aynısı; `AppTest` telefonda sınıyor.
*   [x] **Açılışta Start bir an görünüyor, sonra geri gelen belgeye geçiliyor (2026-11-04).**
    *   Ne: Açık belgeler depodan okunurken uygulama Start'ı gösteriyor; okuma bitince seçili belgeye geçiyor. Web'de (yerel dağıtım, tarayıcıda yeniden yükleme) bir iki saniye görülüyor, çünkü önce kelimeler ve KSafe'in önbelleği geliyor.
    *   Öneri: depo okunana kadar ilk ekranı göstermemek (Start'ın iskeleti ya da boş bir zemin), ya da geri gelen belgeyi beklemek.
    *   Durum: kapandı (2026-11-04). Açık belgeler depodan gelene kadar ekran boş kalıyor (`WorkbenchKeeper.restored`); sonra doğrudan geri gelen belge ya da Start çiziliyor.
*   [x] **Açık bir belgenin metni iki kez saklanıyor (2026-11-04).**
    *   Ne: Recent'teki ve açık sekmedeki aynı belge, metnini iki ayrı adla tutuyor (`history-<id>`, `open-<id>`): 10 MB'lık bir belge web'de 20 MB yer tutuyor.
    *   Öneri: açık sekme history'deki metne bağlansın (`historyId` varken ayrı metin yazmasın), değişince ikisi birlikte yazılsın.
    *   Durum: kapandı (2026-11-04). Recent'ten açılan bir belgenin metni yalnızca history'de (`historyTextName`); keeper onu yazmıyor, açılışta oradan okuyor. History açık bir belgeyi unutursa (Clear history, 20'lik sınır) uygulama belgeyi history'den ayırıyor (`followForgotten`, `Workbench.leaveHistory`), metni o andan sonra `open-<id>`'de. Önceden iki kez saklanmış metnin kopyası açılışta siliniyor. `WorkbenchKeeperTest` ve `FollowEditsTest` sınıyor; iOS simülatöründe `open-*` kopyalarının kalktığı görüldü.
*   [x] **Bir belgenin ağacının hâli saklanmıyor (2026-11-04).**
    *   Ne: Uygulama yeniden açılınca açık belgeler ve metinleri geliyor, ama ağacın açık dalları, seçili değer, tablonun sıralaması ve gizli sütunları gelmiyor; belge ağacında ya da tablosunda, açıldığı gibi açılıyor.
    *   Öneri: `DocumentView`'ın kalanını da `WorkbenchKeeper`'ın sekme kaydına yazmak.
    *   Durum: kapandı (2026-11-04). Sekmenin kaydı açık dalları, seçili değeri, tablonun sıralamasını, gizli sütunları ve süzgeci de tutuyor (`KeptTab`); önceden kaydedilmiş bir sekme belgenin açıldığı hâlle geliyor. `WorkbenchKeeperTest` sınıyor. Sütun genişlikleri hâlâ tutulmuyor.
*   [x] **Belgelerin metni cihazda şifresiz (2026-11-04).**
    *   Ne: Recent'in ve açık sekmelerin listesi KSafe'te şifreli, metinler ise uygulamanın klasöründe (web'de IndexedDB'de) düz metin. Yapıştırılan bir belge bir anahtar ya da parola taşıyabilir.
    *   Öneri: sahibinin kararı: metinleri de şifrelemek (KSafe'in anahtarıyla) ya da Settings'e "geçmişi tutma" seçeneği.
    *   Durum: kapandı (2026-11-04). Sahibinin kararıyla metinler şifreli: AES-GCM, KSafe'in kasasında duran 256 bitlik bir anahtarla (`getOrCreateSecret`), cryptography-kotlin 0.6.0 ile (JVM ve Android'de JDK, iOS'ta CryptoKit, web'de WebCrypto). Önceden şifresiz yazılmış metin olduğu gibi okunuyor ve bir sonraki yazışta şifreleniyor; çözülemeyen metin (anahtarı kaybolmuş) yok sayılıyor. `EncryptedTextStoreTest` her hedefte sınıyor; web'de IndexedDB'de yalnızca şifreli metin görüldü. Sorun olursa şifresize dönülür (sahibinin kararı). Metin Base64 yazıldığı için cihazda üçte bir daha yer tutuyor.
*   [x] **Biçimlendirme sondaki virgülü siliyor, anahtarla değer arasındaki yorumu öne alıyor (2026-11-03).**
    *   Ne: `JsonDocument.formatted` JSONC ve JSON5'te sondaki virgülü yazmıyor; bir üyenin anahtarıyla değeri arasındaki yorum (`"b": /* not */ 2`) üyenin önüne kendi satırına geçiyor. İkisi de belgeyi geçerli bırakıyor, ama yazanın biçimi değil.
    *   Öneri: Settings'e "sondaki virgülü koru" seçeneği; ya da JSON5'te varsayılan olarak korumak.
    *   Durum: kapandı (2026-11-04). Format, belgede sondaki virgül olan bir kabın son girdisinden sonra onu koruyor (JSONC, JSON5), anahtarlar sıralanınca da; bir üyenin anahtarıyla değeri arasındaki blok yorumu yerinde kalıyor. Satır yorumu orada kalsa değeri satırdan atacağı için üyenin önüne geçiyor. Settings'e seçenek gerekmedi.
*   [ ] **Değer önizlemesi yalnızca sözcük (2026-11-03).**
    *   Ne: Panolarda ağacın satırı bir renk örneği, bir bağlantı simgesi ve bir küçük resim gösteriyor; `PantrixTreeView` satırın sonuna yalnızca metin aldığı için Formatico "Colour", "Link", "Picture" yazıyor. Tarih, okuyucunun dilinde UTC olarak yazılıyor. Bkz. `PANTRIX_FINDINGS.md`.
    *   Durum: açık.
*   [ ] **Tabletin metin ile ağaç arasındaki ayırıcısı sürüklenmiyor (2026-11-03).**
    *   Ne: Workspace-834 panosundaki "Resize the text and the tree" tutamacı çizilmiyor; metin 480 dp, ağaç geri kalanı. Bkz. `PANTRIX_FINDINGS.md`.
    *   Durum: açık.
*   [x] **Koin'in bağlamı `App()` bitince kapanmıyor (2026-11-03).**
    *   Ne: Masaüstünün UI testlerinde bir testin açtığı belgeler bir sonraki testin `App()`'inde açık duruyor: `KoinApplication` (Koin 4.2.2) aynı süreçte bağlamını yeniden kullanıyor, `Workbench` tekil olarak yaşıyor. Uygulamada istenen bu (döndürmede sekmeler kalıyor), testlerde ise sıra bağımlılığı demek. `AppTest` bugün geçiyor, çünkü belge açan tek test sonunda yeni sekmeye dönüyor.
    *   Öneri: `App()`'e testin vereceği bir Koin yapılandırması ya da testlerde `stopKoin()`; Koin'in `KoinApplication` belgesinde bu davranışa bakmak.
    *   Durum: kapandı (2026-11-03). Koin 4.2.2'nin `CompositionKoinApplicationLoader`'ı kaynağında okundu: bileşim bitince bağlamı bırakıyor ama durdurmuyor. Uygulamada bu isteniyor; `AppTest` ve `BoardScreenshotTest` her testten sonra `stopKoin()` diyor, her test boş bir uygulamayla başlıyor.
*   [x] **YAML okunuyor, yazılmıyor (2026-11-03).**
    *   Ne: YAML ağaçta okunuyor; Format, Minify ve Sort keys YAML'da kapalı, çünkü sahibinin kararıyla YAML Formatico'nun kendi yazıcısıyla yazılacak ve o yazıcı henüz yok. Kapalı düğmeler nedenini söylemiyor.
    *   Öneri: sıradaki iş olarak YAML yazıcısı (yorumlar, anchor'lar ve skaler biçimleri korunarak); o zamana kadar düğmelere bir ipucu.
    *   Durum: kapandı (2026-11-03). `core:format`'ta `YamlDocument.formatted` (blok biçimi; yorumlar, boş satırlar, anchor, alias, etiket, skalerin yazılışı, belge işaretleri korunuyor) ve `minified` (tek satır, akış biçimi). Sıralama bir alias'ı anchor'ının önüne koyacaksa yapılmıyor ve nedeni söyleniyor.
*   [ ] **YAML yazıcısının yorum ve biçim kuralları (2026-11-03).**
    *   Ne: Bir yorum, satırın sonundaysa o satırın sonunda, kendi satırındaysa altındaki girdinin önünde kalıyor. Bunun sonucunda bir dizinin tirelerinden sonra gelen yorum (`- # not`) kendi satırına geçiyor; Sort keys'te dosyanın en üstündeki yorum ilk anahtarla birlikte taşınıyor. Tek satırlık bir akış koleksiyonu (`[a, b]`, `{a: 1}`) olduğu gibi kalıyor, blok biçimine açılmıyor; birkaç satıra yayılmış olanı tek satıra iniyor (içinde yorum varsa olduğu gibi kalıyor). Birkaç satıra yayılmış düz ya da tırnaklı bir skaler tek satıra iniyor. `...` belge sonu işareti yazılmıyor. Dört boşlukla girintilenen bir belgede dizinin öğeleri `-   a: 1` biçiminde, tireden sonra üç boşlukla yazılıyor.
    *   Öneri: Settings'e "akış koleksiyonlarını blok biçimine aç" seçeneği; diğerleri için kullanıcıların ne beklediğine bakmak.
    *   Durum: açık. Düzeltilenler (2026-11-04): tireden sonraki yorum tirenin satırında kalıyor; altında boş satır olan baştaki yorumlar belgenin, Sort keys'te başta kalıyor; `...` yazılıyor. Kalanlar sahibinin kararını ya da Settings'i bekliyor (Faz 12): akış koleksiyonlarının blok biçimine açılması, birkaç satıra yayılan skalerin tek satıra inmesi, dört boşlukla girintide `-   a: 1`.
*   [ ] **Sıralama bir alias'ı anchor'ının önüne koyacaksa yapılmıyor (2026-11-03).**
    *   Ne: YAML'da alias'ın anchor'ından sonra gelmesi gerekiyor. Sort keys sonucu okuyup bunu bulursa belgeyi değiştirmiyor ve "sıralamak bir alias'ı anchor'ının önüne koyardı" diyor.
    *   Öneri: sıralamadan sonra anchor'ı ilk geçtiği yere, alias'ı sonrakine taşımak (değer aynı kalır).
    *   Durum: açık.
*   [ ] **YAML'da yalnızca ilk sözdizimi hatası bulunuyor (2026-11-03).**
    *   Ne: `snakeyaml-engine-kmp` ilk sözdizimi hatasında duruyor; Formatico o hatayı, ondan önce okunanı ve yinelenen anahtarlar ile anchor'ı olmayan alias'ları gösteriyor. Hatanın kütüphanedeki İngilizce açıklaması saklanmıyor (`ProblemCode.YamlSyntax`): problem listesi yazılınca neyin yanlış olduğunu söyleyecek bir yol gerekecek.
    *   Öneri: Invalid ekranıyla birlikte kütüphanenin sorun türlerini (`ScannerException`, `ParserException`) kendi kodlarımıza eşlemek.
    *   Durum: açık. `snakeyaml-engine-kmp` ilk hatada duruyor; YAML'ı onun okuması sahibinin kararı. Hatanın türünü kendi kodlarımıza eşlemek, problem listesiyle birlikte Faz 6'da (Invalid).
*   [ ] **YAML'ın birleştirme anahtarı ve YAML 1.1 değerleri (2026-11-03).**
    *   Ne: YAML 1.2'nin çekirdek şemasıyla okunuyor: `<<: *defaults` birleştirilmiyor, ağaçta `<<` adlı bir anahtar ve bir alias olarak duruyor; `yes`, `no`, `on`, `off` mantıksal değil metin. Kubernetes gibi YAML 1.1 alışkanlığıyla yazılmış belgelerde bu şaşırtabilir.
    *   Öneri: Settings'e "YAML 1.1" seçeneği ya da ağaçta birleştirilmiş anahtarları gösteren bir görünüm.
    *   Durum: açık. Bir seçenek olarak Settings'le (Faz 12); sahibinin kararı.
*   [ ] **Ağaç seçilen satıra kaydırılamıyor (2026-11-03).**
    *   Ne: "Go to the anchor" ve izin adımları bir değeri seçiyor, üstündeki dalları açıyor; ama satır ekranın dışındaysa ağaç ona kaymıyor. Bkz. `PANTRIX_FINDINGS.md`.
    *   Durum: açık, Pantrix'i bekliyor: "TreeView satırının sonunda yalnızca bir metin var…" bulgusu `tıkıyor` (2026-11-04).
*   [ ] **CSV'de Format yalnızca tırnakları ve satır sonlarını düzeltiyor (2026-11-03).**
    *   Ne: `CsvDocument.formatted` her alanı yalnızca gerektiğinde tırnaklıyor, belgenin ayırıcısını ve satır sonunu koruyor, sona bir satır sonu koyuyor, boş satırları atıyor. Sütunları hizalamıyor. Minify ve Sort keys CSV ile TSV'de kapalı (panodaki gibi); kapalı düğmeler nedenini söylemiyor.
    *   Öneri: Settings'e ya da More menüsüne "ayırıcıyı değiştir" (CSV ↔ TSV dönüşümü Convert'le gelecek); hizalama istenirse ayrı bir eylem.
    *   Durum: açık. Kapalı Minify ve Sort keys nedenini söylemiyor; panolar da söylemiyor, kapalı bir düğmenin ipucu Pantrix'te sınanmadı. Ayırıcı değiştirme Convert'le (Faz 10).
*   [ ] **CSV'nin ilk satırı her zaman başlık; sayıya benzeyen alan sayı (2026-11-03).**
    *   Ne: İlk kayıt sütunların adları sayılıyor; başlıksız bir CSV'nin ilk satırı tabloda başlık olur. Bir alan sayıya benziyorsa (`020`, `1e3`) ağaçta ve tabloda sayı: sağa yaslanıyor, sayı olarak sıralanıyor ve özetleniyor (countries.csv'nin `numeric` sütunu, en küçük `004` "4" olarak). Yazılışı korunuyor (`020`).
    *   Öneri: "ilk satır başlık" seçeneği; baştaki sıfırı olan bir alanı kod sayıp metin olarak tutmak.
    *   Durum: açık. Baştaki sıfırı olan bir alan sütununu kod sütunu yapıyor (2026-11-04): countries.csv'nin `numeric` sütunu `004` ile `248` birlikte metin, sola yaslı, metin gibi sıralanıyor; sayı sütunları sayı. "İlk satır başlık" seçeneği kaldı (Settings ya da tablonun araç çubuğu; önce panoya).
*   [x] **Tablonun süzgeci bölmeden çıkınca siliniyor (2026-11-03).**
    *   Ne: Sıralama ve gizlenen sütunlar belgenin görünümünde (`DocumentView`) tutuluyor, Tree'ye gidip gelince kalıyor. Süzgecin metni ise bölmenin kendisinde: Tree'ye geçince, başka bir belgeye geçince ya da uygulama yeniden başlayınca boşalıyor. Sürüklenen sütun genişlikleri de tutulmuyor.
    *   Öneri: süzgeci de `DocumentView`'a almak; genişlikleri `DataTableState`'ten okuyup saklamak.
    *   Durum: kapandı (2026-11-04). Süzgecin metni belgenin görünümünde (`DocumentView.tableFilter`): Tree'ye ya da başka bir belgeye gidip gelince kalıyor. Uygulama yeniden açılınca gelmiyor (bkz. "Bir belgenin ağacının hâli saklanmıyor"); sütun genişlikleri hâlâ tutulmuyor.
*   [x] **Telefonun kaydı ilk iki sütunu başlık alıyor (2026-11-03).**
    *   Ne: Table-390 panosunda bir kaydın başlığı veriye göre seçilmiş: siparişte `id` ve tutar, ülkede `name` ve kodlar. Kod genel bir kural kullanıyor: ilk görünen sütun kalın başlık, ikincisi sağda, gerisi `key: value` satırında. countries.csv'de başlık bu yüzden "AF", ad satırın içinde.
    *   Öneri: sütun seçicide "kaydın başlığı" seçimi ya da `name`, `title`, `id` gibi adları öne alan bir kural.
    *   Durum: kapandı (2026-11-04). Kaydın başlığı, varsa kaydı adlandıran sütun (`name`, `title`, `label`), yoksa ilk sütun: countries.csv'de "Afghanistan", yanında kodu; siparişte `id`.
*   [ ] **iOS'ta belge açılırken üst çubuk içerikten önce değişiyor (2026-11-03).**
    *   Ne: Start'tan bir örnek açılırken bir an üst çubuk belgenin çubuğu ("countries.csv", sayaç, more) olurken içerik hâlâ Start'ı gösteriyor; geçiş bitince Table geliyor. Android'de gözle görülmedi.
    *   Öneri: çubuğu geri yığının o an gösterdiği hedefe göre değil, geçişin vardığı hedefe göre seçmek ya da çubuğu da geçişle birlikte değiştirmek.
    *   Durum: düzeltildi, gözle doğrulanacak (2026-11-04). Telefonun çubuğu ekranla birlikte, temanın yavaş süresinde (350 ms) bir kararmayla değişiyor; azaltılmış harekette hemen. Simülatörde geçişin ortası gözle görülmedi.
*   [x] **Panolar düzen'den alınanlarla genişledi, kod henüz değil (2026-11-03).**
    *   Ne: Sahibinin yaptırdığı ayrı tasarımdan ("düzen") seçilenler tuvale çizildi (sürüm 59).
        *   Workspace (1440, 834, 390): ağaçta arama, eşleşme sayısı ve önceki/sonraki (durum `search`); ağacı 1, 2, 3 ya da bütün düzeylere açma (1440, 834); seçili değerin yolunu JSONPath, JSON Pointer ya da jq olarak kopyalama (durum `path`).
        *   Table (1440, 834): her sütunun türü adının altında. Kaydetme düğmesi bir menü: CSV olarak kaydet, CSV ya da Markdown tablosu olarak kopyala (durum `save`, 1440 ve 390).
        *   Yeni Summary panoları (1440, 834, 390): belgenin boyutu, küçültülmüş ve sıkıştırılmış boyu, değer ve anahtar sayıları, en derin yol, türlerin dağılımı, değerlerde bulunan tarih, resim, bağlantı ve renkler, notlar. Durum çubuğundaki boyuttan açılıyor.
        *   Kodda bunların hiçbiri yok; Table'ın kaydetme düğmesi hâlâ doğrudan "Save as CSV".
    *   Nerede: tuval, Workspace, Table ve Summary satırları; CLAUDE.md'nin v1 kapsamı ve pano listesi Summary'yi anmıyor.
    *   Öneri: sahibinin kararıyla v1 kapsamına ve pano listesine eklemek, sonra ekran ekran koda geçirmek.
    *   Durum: kapandı (2026-11-04). Sahibinin kararıyla düzen'den alınanların hepsi v1'de: CLAUDE.md'nin v1 kapsamına ve pano listesine yazıldı; kodu Faz 8'de (ağaç ve tablo, Summary).
*   [x] **Workspace panolarının durum çubuğu belgenin boyunu yanlış yazıyordu (2026-11-03).**
    *   Ne: Workspace panolarının (1440, 834, 390) editör hâli "30 lines · 742 B" diyordu; çizilen order.json 665 bayt, kodun durum çubuğu da 665 B diyor.
    *   Durum: kapandı (2026-11-03). Panolarda 665 B'ye düzeltildi (tuval sürüm 59).
*   [ ] **Web'de gezinti ile paylaşım bağlantısı aynı fragment'ı istiyor (2026-11-04).**
    *   Ne: `core:navigation` ekranı adresin `#` kısmına yazıyor (`#start`, `#document?id=3`). v1'in paylaşımı (Faz 13) da belgeyi bağlantının fragment'ında taşıyacak. Bir paylaşım bağlantısıyla açılan sayfa ilk ekranı yazınca bağlantının fragment'ı siliniyor; bugün tanımadığı bir `#` kısmını uygulama yok sayıyor, ama adresi yine kendi girdisiyle değiştiriyor.
    *   Öneri: Faz 13'te paylaşılan belgeyi fragment'tan okuyup açtıktan sonra adresi gezintiye bırakmak (`#share=…` okunur, sonra `#document?id=N` olur); ya da navigation3-browser'ın birleşmemiş PR #9'u gibi gezintiyi yola (`/document/3`) taşımak, bu da sunucunun her yolu `index.html`'e yönlendirmesini ister.
    *   Durum: açık, Faz 13'le.
*   [ ] **Uygulama aldığı kodun lisanslarını göstermiyor (2026-11-04).**
    *   Ne: navigation3-browser'dan alınan kodun MIT lisansı web dağıtımında bir dosya olarak gidiyor (`licenses/navigation3-browser.txt`); uygulamanın içinde, öteki platformlarda ve öteki bağımlılıklar (Geist, KSafe, snakeyaml-engine-kmp, Koin, Compose) için bir lisans sayfası yok. Geist'in OFL'si de yazı tipiyle birlikte lisansının verilmesini istiyor.
    *   Öneri: Settings'e (Faz 12) bir "Licenses" satırı; önce tuvalde Settings panosuna çizilir.
    *   Durum: açık, Faz 12'yle.
*   [x] **Web'de geri gelen belgeden geri tuşu Start'a dönmüyor (2026-11-04).**
    *   Ne: Uygulama yeni bir sekmede açılıp saklanan belgeyi geri verince ekranlar `#start` ve `#document?id=1` olur, ama tarayıcının geçmişinde bunlar tek bir girdi (`history.state` ikisini de tutuyor). Tarayıcının geri tuşu Start'a değil, sitenin öncesine gidiyor. Start'tan bir belge açılınca ya da adres elle yazılınca geri ve ileri doğru çalışıyor (yerel dağıtımda, 2026-11-04).
    *   Öneri: `BrowserHistory` ilk yazışında, tarayıcının girdisinin hâli yokken ve birden çok ekran varken, kökü bulunduğu girdiye yazsın ve kalan ekranları birer yeni girdi olarak eklesin.
    *   Durum: kapandı (2026-11-04). Öneri gibi; `BrowserHistoryTest` sınıyor. Chrome, bir sayfanın kullanıcı ona dokunmadan eklediği girdiyi geri tuşunda atlıyor (history manipulation intervention): sayfa açılır açılmaz geri basılırsa Start atlanıyor, sayfaya bir kez tıklandıktan sonra geri Start'a dönüyor (yerel dağıtımda, uygulamanın tarayıcı bölmesinde denendi). Bu tarayıcının kuralı; değiştirilemez.
*   [x] **Start'a gidip dönünce belge yeniden okunuyor (2026-11-04).**
    *   Ne: Workspace'in görünüm modeli gezinti girdisine bağlı (`rememberViewModelStoreNavEntryDecorator`). Start'a geçince girdi yığından çıkıyor, görünüm modeli siliniyor; belgeye dönünce yenisi belgeyi baştan okuyor (durum çubuğunda "Reading…"). Küçük belgede fark edilmiyor; 50 MB'lık bir belgede her dönüş yeniden okuma demek. Web'de geri ve ileri tuşlarıyla görüldü (2026-11-04); telefonun geri tuşunda da aynısı olmalı.
    *   Öneri: okunmuş belgeyi (`DocumentReading`) görünüm modeli yerine `Workbench`'te belgeyle birlikte tutmak ya da metni değişmeyen bir belgenin okumasını önbelleğe almak; Faz 14'ün büyük dosya ölçümüyle birlikte.
    *   Durum: kapandı (2026-11-04). Açık belgelerin okunmuşu `Readings`'te tutuluyor; yeniden kurulan workspace metni ya da biçimi değişmemişse okumadan açılıyor, belge kapanınca okunmuşu bırakılıyor. `WorkspaceViewModelTest` sınıyor.
*   [x] **Geniş pencerede Recent ile Samples'ın kartları aynı hizada başlamıyor (2026-11-04).**
    *   Ne: Geçmişte belge varken Recent'in başlık satırında "Clear history" bağlantısı var, Samples'ınkinde küçük bir metin. Bağlantı satırı daha yüksek tutuyor: 1440'ta Recent'in başlığı ve kartı Samples'ınkinden 11 px aşağıda başlıyor (önizleme `StartScreen`, `recent`, 1440). Geçmiş boşken bağlantı yok, ikisi hizalı; Start panosu da boş geçmişle çizili.
    *   Öneri: iki bölümün başlık satırına aynı en küçük yükseklik, ya da geniş pencerede başlıkları bir satırda, kartları altında bir satırda dizmek. Önce Start-1440'ın geçmişli hâli tuvalde çizilir.
    *   Durum: kapandı (2026-11-04). Yan yana duran iki bölümün başlığı en az platformun dokunma hedefi kadar yüksek; kartlar aynı hizada başlıyor. `StartScreenTest.aWideWindowStartsRecentAndSamplesOnOneLine` sınıyor. Geçmişli Start-1440 tuvale çizilmedi: hizalama tasarımın değişmesi değil, ama geçmiş boşken de Samples'ın başlığı birkaç piksel yükseldi.
*   [x] **Ekran görüntüsü testinin yardımcısı her modülde ayrı (2026-11-04).**
    *   Ne: Bir önizlemeyi bir panonun penceresinde, `PantrixTestTheme`'in içinde çizip yazan küçük `capture` fonksiyonu her modülün `jvmTest`'inde yeniden yazılıyor (`feature:start`, sırada `feature:workspace` ve `app:shared`). KMP'nin `testFixtures`'ı yok; test kaynakları modüller arasında paylaşılmıyor.
    *   Öneri: yalnızca testlerin bağımlı olduğu bir `core:testing` modülü (Roborazzi, `PantrixTestTheme`, panolar). CLAUDE.md "her taban tek modülde" diyor: test yardımcısının `core:base`'e mi, yeni bir modüle mi gideceği sahibinin kararı.
    *   Durum: kapandı (2026-11-04). Sahibinin kararıyla `core:testing`: önizlemenin ekran görüntüsü (`capturePreview`, `captureOpenMenu`), İngilizce çalıştırma ve tarayıcıda bekleme (`awaitText`, `awaitNode`, `awaitUntil`), yalnızca testlerin bağımlılığı.
*   [x] **Bir JSON dizisinin tablosunda sayılar sütunun başında duruyordu (2026-11-04).**
    *   Ne: Sayı sütunlarının başlığı sona yaslıydı, hücreleri başa. Hücrenin kutusu, seçilince çevresine çizgi çekilsin diye sütun kadar geniş; içi kutunun başında duruyordu ve sütunun hizasını eziyordu. CSV'nin örneğinde sayı sütunu olmadığı için Table panosunda görünmemişti; önizleme (`WorkspaceScreen`, `json table`) gösterdi.
    *   Durum: kapandı (2026-11-04). Hücre sütununun hizasını alıyor; `WorkspaceScreenTest.aColumnOfNumbersLinesThemUpAtItsEndAsItsHeader` sınıyor.
*   [x] **Yeni, boş bir belge "Plain text has no tree" diyordu (2026-11-04).**
    *   Ne: "New empty document" biçimsiz (düz metin) bir belge açıyor; ağacın yeri "The document is empty" yerine düz metnin sözünü söylüyordu. Boş belgenin sözü yalnızca okunan bir biçimde çıkıyordu.
    *   Durum: kapandı (2026-11-04). Boş bir belge, biçimi ne olursa olsun boş olduğunu söylüyor; `WorkspaceScreenTest.anEmptyDocumentSaysThereIsNothingToRead` sınıyor.
*   [x] **Telefonun çubuğundaki "more" menüsü belgeleri Koin'den alıyor (2026-11-04).**
    *   Ne: `DocumentMoreMenu(documentId)` `Workbench`'i kendi içinde `koinInject` ile buluyor. `AppTopBar`'ın önizlemesi ve testi bu yüzden kendi Koin'ini başlatıp `stopKoin()` diyor; çubuk dışarıdan verilenle çizilen bir bileşen değil.
    *   Öneri: `App`, menünün işlerini (sırala, kapat) ve belgenin biçimini çubuğa versin; menü Koin'i bilmesin.
    *   Durum: kapandı (2026-11-04). `DocumentMoreMenu` workbench'i parametre olarak alıyor; çubuk menüyü uygulamadan bir yuva olarak alıyor (`documentMenu`). Önizleme ve test Koin'siz.
*   [x] **Menülerin açık hâli önizlenmiyor (2026-11-04).**
    *   Ne: Biçim menüsü, "more" menüsü, sütun seçici ve telefonun belge listesi açılır pencereler. Önizlemeler ve ekran görüntüleri onları kapalı çiziyor; açık hâlleri yalnızca UI testlerinde sınanıyor. Masaüstünün testinde açılır pencere ayrı bir kök; Roborazzi'nin `onRoot()`'u birden çok kök görünce duruyor.
    *   Öneri: açık menüyü çizen ayrı bir ekran görüntüsü testi (menüyü açıp `onAllNodes(isRoot())`'un sonuncusunu yazmak), ya da Pantrix'in menüsünün yerinde açılan bir önizleme biçimi.
    *   Durum: kapandı (2026-11-04). Ekran görüntüsü testi menüyü açıp onun penceresini yazıyor (`captureOpenMenu`): biçim menüsü, belgenin "more" menüsü, sütun seçici, telefonun belge listesi ve "more" sayfası. Bu sırada sütun seçicinin açılınca çöktüğü bulundu ve düzeltildi (aşağıda).
*   [x] **Sütun seçici açılınca uygulama çöküyordu (2026-11-04).**
    *   Ne: Table'ın "Columns" düğmesi `PantrixPopover` açıyor; içine Formatico kendi `verticalScroll`'unu koymuştu. Popover içindekini kendisi kaydırıyor (Pantrix'in Popover sayfası); iç içe iki kaydırma sonsuz yükseklikle ölçülüp çöküyordu. Hiçbir test seçiciyi açmamıştı; açık menünün ekran görüntüsü buldu.
    *   Durum: kapandı (2026-11-04). İç kaydırma kalktı; `WorkspaceScreenTest.theColumnsChooserOpensAndHidesAColumn` açıp bir sütunu gizliyor.
*   [x] **Web uygulaması şifrelemeyle açılmıyordu (2026-11-04).**
    *   Ne: cryptography-kotlin'in getirdiği kotlinx-io, Node için yazdığı yardımcıda (`kotlinx.io.node.getRequire`) `import.meta`'yı çıplak okuyor; webpack onu klasik bir betikte bırakıyor, tarayıcı "Cannot use 'import.meta' outside a module" deyip uygulamayı başlatmıyordu. Testlerin Karma sayfası da aynı hatayla düşüyordu.
    *   Durum: kapandı (2026-11-04). `config/webpack/node-only-import-meta.js` ve `config/karma/node-only-import-meta.js` çıplak `import.meta`'yı boş bir nesneye çeviriyor (tarayıcı o yardımcıyı hiç çağırmıyor). Yerel dağıtım tarayıcıda açıldı, belge şifreli yazılıp yeniden yüklemede geri geldi.
*   [x] **Webpack'in ve Karma'nın parçaları değişince derleme kendini yenilemiyordu (2026-11-04).**
    *   Ne: `config/webpack` ve `config/karma` Gradle'ın girdileri değildi: yeni bir parça eklenince web dağıtımı UP-TO-DATE kalıyor, eski betik çıkıyordu.
    *   Durum: kapandı (2026-11-04). İki klasör `KotlinWebpack` ve `KotlinJsTest` görevlerinin girdisi.
*   [ ] **Telefonda bul ve değiştir açılamıyor (2026-11-04).**
    *   Ne: PantrixCodeEditor'ın bul ve değiştir çubuğu ⌘F ve ⌘H ile açılıyor; masaüstünde, tablette klavyeyle ve web'de çalışıyor. Klavyesi olmayan bir telefonda onu açacak bir düğme yok; panolarda da çizili değil.
    *   Öneri: telefonun "more" menüsüne ya da metin bölmesinin araç çubuğuna "Find" (`CodeEditorState.find`); önce Workspace-390'a çizilir.
    *   Durum: açık.
*   [ ] **TOML, INI, properties ve XML'in renklendiricisi basit (2026-11-04).**
    *   Ne: Bu biçimler henüz okunmuyor (Faz 7). Editör onları bir satırda gördüğü kadarıyla renklendiriyor: bölüm adı, anahtar, değer, yorum, etiket ve öznitelik. TOML'un birkaç satıra yayılan dizgisi ve dizisi, XML'in CDATA'sı satır satır renkleniyor.
    *   Öneri: Faz 7'de her biçimin okuyucusuyla birlikte renklendiricisini de tamamlamak.
    *   Durum: açık, Faz 7'yle.
