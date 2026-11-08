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

*   [x] **Ekran görüntüleri yalnızca masaüstünde (2026-11-03).**
    *   Ne: Roborazzi 1.77.0 `app/shared`'in masaüstü testlerinde: `BoardScreenshotTest` uygulamayı panoların üç genişliğinde (1440×900, 834×1194, 390×844) açık temada çiziyor, Start, Workspace, YAML ve Table olarak 12 resim `app/shared/screenshots/`'ta. `recordRoborazziJvm` yazıyor, `verifyRoborazziJvm` karşılaştırıyor. Android (Robolectric) ve iOS'un kendi resimleri, koyu tema ve panolarla gözle karşılaştırma yok.
    *   Öneri: Faz 15'te (`PLAN.md`) her pano ve her platform.
    *   Durum: kapandı (2026-11-06, Faz 15). Kanvasın 11 panosu ve Start'ın iki cevabı (biçimi anlaşılmayan metin, kırık bağlantı), YAML'ın çalışma alanıyla 14 pano. Uygulama her panoya ortak bir gezintiyle (`app/shared/src/commonTest/…/BoardTour.kt`) getiriliyor: Invalid'in `settings.json`'ı, Query'nin `feed.xml`'i ve Compare'in iki siparişi dosya seçiciden açılıyor. Masaüstünde 126 resim: üç genişlik, açık ve koyu tema, açık temada Türkçe (`-tr`). iOS simülatöründe 56 (`screenshots/ios/`, 834 ve 390, iOS'un kendi kontrolleriyle, `recordRoborazziIosSimulatorArm64`). Android'de Robolectric'le 56 (`screenshots/android/`, `recordRoborazziAndroidHostTest`). Her resimde teşhisler bulgusuz olmak zorunda. Eski resimlerdeki gri satırlar, işaretçinin bıraktığı hover'dı; yeni gezinti ekran okuyucu gibi bastığı için onlar kalktı. Panolarla gözle karşılaştırma yalnızca yeni dört panoda (Invalid, Query, Compare, Summary) ve Türkçe resimlerde yapıldı; bütün panoların bire bir denetimi Faz 21'de.
*   [x] **Tarayıcıda uçtan uca test yok (2026-11-06).**
    *   Ne: `wasmJsBrowserTest` ortak testleri Karma'da koşuyor, uygulamanın dağıtımını değil: yükleyicinin Wasm ile JavaScript arasındaki seçimi, sekmenin geçmişi ve adresi, `#d=` bağlantıları, yeniden yüklemede saklananlar hiç sınanmıyordu. Maestro'nun Chromium'u Compose'un ağacını görmüyor (PANTRIX_FINDINGS).
    *   Durum: kapandı (2026-11-06, Faz 15; sahibinin "Kotlin, JDK + Chrome" kararı). `app/webTest`: dağıtımı JDK'nın sunucusuyla sunan, kurulu Chrome'u başsız açıp DevTools protokolüyle süren, Compose'un gölge kökteki erişilebilirlik ağacında etiketi bulup ortasına basan sekiz test (`./gradlew :app:webTest:test`). İndirme yok; Chrome başka bir yerdeyse `-Pformatico.chrome=<yol>`.
*   [x] **Türkçe ve İngilizce sözcükler birbirinden ayrılabiliyordu (2026-11-06).**
    *   Ne: Bir anahtarın bir dilde olup ötekinde olmaması, bir yer tutucunun (`%1$s`) düşmesi ya da İngilizce kalan bir Türkçe sözcük derlemede yakalanmıyordu.
    *   Durum: kapandı (2026-11-06, Faz 15). Önce `checkTranslations` görevi `strings.xml`'leri karşılaştırdı (`d090e14`). Sonra sahibinin isteğiyle (2026-11-06) sözcükler XML'den çıktı: her özelliğin `strings` paketinde Kotlin'de bir arayüz ve her dil için bir nesnesi var (Workspace'te ekran başına: Workspace, Problem, Table, Query, Convert, Compare, Summary, Share, Palette). Eksik bir sözcüğü, ya da başka değerler alan bir cümleyi artık derleyici yakalıyor; İngilizce kalan Türkçe sözcüğü her modülün `StringsTest`'i (`core:testing`'in `untranslated`'ı). Görev ve `config/l10n` kalktı.
*   [x] **Dil, platformun dili değiştirilerek seçiliyordu (2026-11-06).**
    *   Ne: Compose'un kaynakları platformun dilini okuduğu için Settings'te seçilen dil her platformda platformun diline yazılıyordu (`LocalAppLocale`: JVM'in `Locale`'i, Android'in yapılandırması, iOS'un `AppleLanguages`'ı, web'de `navigator.languages`'ı yamalayan bir betik) ve uygulamanın bileşimi baştan kuruluyordu (`key(language)`). Bileşim dışında okunan sözcükler (`getString`) bir tuzaktı: web'de `navigator.language`'tan geliyordu.
    *   Durum: kapandı (2026-11-06, sahibinin isteği). Sözcükler temanın dilinden geliyor (`inTheLanguage`, `PantrixTheme.locale`), Pantrix'in kendi sözcükleri gibi. `LocalAppLocale`, `index.html`'in betiği ve `key(language)` kalktı; dil değişince ekranlar yerinde kalıyor. Görünüm modeli Convert'in yorumunu Settings'in dilinde yazıyor (`convertStringsIn`). Testler makinenin dilinden bağımsız: `PantrixTestTheme`'in dili; `inEnglish` kalktı, `AppLanguageTest` ortak testlerde, her hedefte. Bir sonuç: platformun kendi sözcükleri (Android'in metin menüsü, iOS'un sistem pencereleri) artık seçilen dili değil sistemin dilini izliyor.

*   [x] **Kullanılmayan importlar lint'ten geçiyor (2026-11-04).**
    *   Ne: `StartScreenTest.kt`'de yedi kullanılmayan import vardı (`Dispatchers`, `delay`, `withContext`, `TimeSource`, `seconds`, `milliseconds`, `ComposeUiTest`); bekleme yardımcıları `core:testing`'e taşınırken kalmışlar. `lintKotlin` ve detekt geçiyordu. ktlint 1.8.0'ın `no-unused-imports` kuralı jar'da var, ama bu dosyada bir şey söylemedi. detekt'in `style.UnusedImports`'u varsayılan ayarda kapalı ve `config/detekt/detekt.yml` onu açmıyor.
    *   Nerede: `.editorconfig`, `config/detekt/detekt.yml`, Kotlinter'in `lintKotlinCommonTest`'i.
    *   Öneri: kuralın neden susduğuna bakılsın (kural kümesi, `.editorconfig`, Kotlinter'in ayarı); olmazsa detekt'in `style.UnusedImports`'u açılsın. Ardından bütün modüllerde bir tarama.
    *   Durum: kapandı (2026-11-04). ktlint 1.8 kuralı 2.0'da kaldırılacak diye işaretlemiş ("Marked for removal in Ktlint 2.0.0": kullanılan bir importu kullanılmıyor sanabiliyor) ve açıkça açılmadıkça koşturmuyor. `.editorconfig`'de `ktlint_standard_no-unused-imports = enabled` ile açıldı; yorumu nedenini ve ktlint 2.0'da ne yapılacağını söylüyor (detekt'in `UnusedImports`'u açılır ve testler detekt'e verilir; detekt bugün testlere bakmıyor). Açılınca 9 dosyada 56 kullanılmayan import çıktı: çoğu bekleme yardımcıları `core:testing`'e taşınırken testlerde, 19'u `WorkspacePanes.kt`'de, parçaları başka dosyalara taşınırken kalmış. `formatKotlin` ile silindi; yalnızca import satırları gitti, her hedef derleniyor ve testleri geçiyor.

*   [x] **Tam denetimde Kotlin daemon'ının belleği yetmeyebiliyor (2026-11-06).**
    *   Ne: Bütün hedeflerin testleri birlikte koşunca (`jvmTest … wasmJsBrowserTest … --continue`) Wasm'ın test yürütülebilirlerinin derlemesi iki kez `OutOfMemoryError: Java heap space` ile düştü (`feature:workspace`, `feature:settings`, bir kez de `core:navigation`); aynı görev tek başına yeniden koşunca geçti. `gradle.properties`'te `kotlin.daemon.jvmargs=-Xmx3072M`.
    *   Öneri: `kotlin.daemon.jvmargs`'ı 6 GB'a çıkarmak (makinede 128 GB var), ya da Wasm testlerini ayrı bir koşuda.
    *   Durum: kapandı (2026-11-06, sahibinin kararı). `kotlin.daemon.jvmargs=-Xmx6144M`. Tam denetim (`jvmTest verifyRoborazziJvm iosSimulatorArm64Test wasmJsBrowserTest lintKotlin detekt`) `--rerun-tasks` ile, bütün derlemeler baştan, bellek hatası olmadan geçti: JVM'de 734, iOS'ta 704, Wasm'da 693 test. `build-logic`'in derlemesi (Gradle'ın kendi Kotlin'i, 2.4.10) bu ayarı almıyor, Gradle daemon'ının 4 GB'ıyla koşuyor; küçük bir derleme, yetiyor.

*   [ ] **build-logic'e lint ve detekt bakmıyor (2026-11-04).**
    *   Ne: `QualityConventionPlugin` (Kotlinter ve detekt) uygulamanın modüllerine uygulanıyor; `build-logic/convention`'ın kendi `build.gradle.kts`'inde ikisi de yok. Convention eklentilerinin kodu biçim ve kural denetiminden geçmiyor.
    *   Nerede: `build-logic/convention/build.gradle.kts`.
    *   Öneri: `build-logic`'e Kotlinter (ve istenirse detekt) eklensin. Eklenti `build-logic`'in kendi derlemesinde olduğu için sürümü kataloğundan alınır.
    *   Durum: açık. Bunun bir sonucu (2026-11-04): `InvisibleCharactersTask.kt`'nin kendi karakter tablosunda ve KDoc'unda görünmeyen karakterler kaçış yerine kendileri olarak yazılıydı (bölünmeyen boşluk, BOM, yön işaretleri). `checkInvisibleCharacters` `build-logic`'e bakmadığı için yakalanmadı. Kaçışla yazıldılar; görevin işi değişmedi.

## core:format

*   [x] **Büyük dosyalar ölçülmedi (2026-11-02).**
    *   Ne: Hedef masaüstünde 50 MB, telefonda ve web'de 10 MB. JSON okuyucusu her belirteci ve her değeri bir nesne olarak tutuyor, iç içe değerleri özyinelemeyle okuyor (512 düzeyle sınırlı).
    *   Öneri: 10 ve 50 MB'lık örneklerle her hedefte süre ve bellek ölçümü; gerekirse belirteçleri dizilerde tutmak ve okuyucuyu yığıtla yazmak.
    *   Durum: kapandı (2026-11-06). Ölçüldü: `LargeDocumentsMeasure` (`-Pformatico.measure`) altı biçimde 10 ve 50 MB'ta her adımın süresini ve tuttuğu belleği yazıyor; sonuçlar ve cihazlar `docs/MEASURE.md`'de. Okuma JVM'de 10 MB'ta 0,4–1,0 s, 50 MB'ta 1,7–4,0 s; okunmuş belge 10 MB'ta 135–320 MB (önce 182–408), 50 MB'ta 0,7–1,6 GB. Düğümler babalarının yolunu, belgenin adlarını ve adımlarını paylaşıyor; ağaç kapalı dalları kurmuyor; tablo bir kez ve ana iş parçacığının dışında kuruluyor; XPath'in eşleşmeleri ağaçta yarılanarak bulunuyor; karşılaştırma ortak uçları ayırıyor. Cihazlarda 10 MB: Android'de JSON 5,1 s, CSV 9,8 s (uygulama `largeHeap` istiyor); iOS'ta 4 s ve 5,9 s; web'de 2 s ve 10 s. Kalanlar "Büyük belgelerde ölçülüp bırakılanlar"da. Okuyucu belirteçleri hâlâ nesne olarak tutuyor ve özyinelemeli; 50 MB'ta sorun olmadı. Önceki ölçüm (Faz 7): Faz 7'de iki ölçüm (JVM, geçici testlerle; testler silindi): 8,7 milyon karakterlik bir TOML 485 ms'de okunuyor ve yaklaşık 220 MB tutuyor; karşılaştırılabilir bir JSON 365 ms ve yaklaşık 262 MB. İkisi de doğrusal görünüyor, ama bu oranla 50 MB'lık bir belge 1,2–1,5 GB, 10 MB'lık bir belge yaklaşık 250 MB ister. XML okuyucusunda bulunan karesel bir yavaşlık giderildi: 10 MB 25 saniyeden yaklaşık 120 ms'ye indi.
*   [x] **Sütun UTF-16 birimiyle sayılıyor (2026-11-02).**
    *   Ne: `TextPosition.column` UTF-16 birimi sayıyor; bir emoji iki sütun sayılır. Editörlerin çoğu kod noktası ya da görünen karakter sayar.
    *   Öneri: PantrixCodeEditor'ın imleci neyi sayıyorsa onu sayan bir dönüşüm.
    *   Durum: kapandı (2026-11-04). PantrixCodeEditor da bir yeri UTF-16 birimiyle sayıyor ("A place in the text counts its characters from nought", `String`'in karakterleri): durum çubuğundaki sütun editörünkiyle aynı; bir emoji ikisinde de iki sütun.
*   [x] **Kaynaklarda görünmeyen karakterler kaçış dizisiyle yazılmalı (2026-11-02).**
    *   Ne: `'﻿'`, `' '` gibi karakterler dosyaya kendileri olarak girerse Kotlinter'ın biçimlendiricisi BOM'u siliyor ve `''` derlenmiyor. Bu oturumda öyle oldu ve düzeltildi.
    *   Öneri: detekt'e ya da bir teste "Kotlin kaynağında görünmeyen karakter yok" denetimi.
    *   Durum: kapandı (2026-11-03). `formatico.quality` her modüle `checkInvisibleCharacters`'ı ekliyor (`check`'in parçası): `src` altındaki her `.kt` ve `.kts`'te bayt sırası işareti, bölünmeyen boşluk, yumuşak tire, genişliği olmayan boşluk ve birleştiriciler, yazı yönü işaretleri aranıyor; bulunan her biri dosya, satır ve sütunuyla, kaçış dizisi önerilerek bildiriliyor. Dikilmiş bir `\u00A0` ile denendi, yakaladı.
*   [x] **Biçimlerin yalnızca JSON ailesi okunuyor (2026-11-02).**
    *   Ne: `core:format`'ta bugün JSON, JSONC ve JSON5 okuyucusu, biçim algılama, satır ve sütunlu problemler var. Biçimlendirme, küçültme, onarım, NDJSON okuyucusu, XML, YAML (`snakeyaml-engine-kmp` ve kendi yazıcımız), TOML, CSV/TSV, properties/INI sırada.
    *   Durum: kapandı (2026-11-04, Faz 7). `core:format`'ta her v1 biçiminin kayıpsız okuyucusu ve yazıcısı var: NDJSON (her satır RFC 8259 JSON'u), XML (kendi okuyucumuz; DOCTYPE olduğu gibi tutuluyor, iç alt kümesi okunmuyor), TOML 1.0, Java properties (`Properties.load`'ın kuralları) ve INI. Her biri metni karakter karakter geri veriyor (`print()`), problemleri satır ve sütunuyla buluyor, Format ve Minify'ı (XML dışında Sort keys'i de) yazıyor. Daha önce: JSON ailesi, YAML (2026-11-03), CSV ve TSV (2026-11-03).

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
*   [x] **Üst çubuğun bazı düğmeleri basılmıyor (2026-11-03).**
    *   Ne: Başlatıcı (komut paleti) ve Ayarlar kendi ekranları gelene kadar `enabled = false`; tek sekme var ve yeni sekme düğmesi çizilmiyor.
    *   Öneri: Palette, Settings ve sekmeler yazıldıkça bağlamak.
    *   Durum: açık. Sekmeler ve yeni sekme düğmesi Workspace'le bağlandı (2026-11-03); başlatıcı ve Ayarlar hâlâ basılmıyor. Workspace'in Query, Convert, Compare, ağaçta arama ve Table görünümü de kendi ekranlarını bekliyor. Query, Convert ve Compare bağlandı (2026-11-05). Başlatıcı bağlandı (2026-11-05, Faz 12): komut paletini açıyor; Command ya da Control ve K her yerde açıyor. Kapandı (2026-11-05): Ayarlar Settings'i açıyor (geniş pencerede ve tablette bir sekme, telefonda kendi ekranı), Command ya da Control ve virgül de.
*   [x] **Temanın seçimi saklanmıyor (2026-11-03).**
    *   Ne: Çubuğun tema düğmesi seçimi `rememberSaveable` ile tutuyor; uygulama yeniden açılınca sistemin teması geri geliyor.
    *   Öneri: Settings ekranıyla birlikte cihazda saklamak.
    *   Durum: kapandı (2026-11-04). Çubuğun düğmesiyle seçilen tema KSafe'te (`theme`); uygulama açılınca okunuyor. Settings geldi (2026-11-05): tema `core:settings`'in tercihlerinde (açık, koyu, sistemdeki gibi); eski `theme` anahtarı bir kez okunup açık ya da koyu sayılıyor.
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
*   [x] **Yalnızca JSON ailesi okunuyor; dört örneğin üçü okunmuyor (2026-11-03).**
    *   Ne: Workspace'in ağacı ve düzenlemeleri JSON, JSONC ve JSON5'te çalışıyor. YAML, TOML, CSV ve diğerleri "isn’t read yet" diyor; Start'ın dört örneğinden üçü (deployment.yaml, Cargo.toml, countries.csv) bugün yalnızca bu hâli gösteriyor. NDJSON da okunmuyor.
    *   Öneri: `core:format`'ın sıradaki okuyucuları; ilk olarak YAML (`snakeyaml-engine-kmp`) ve CSV, çünkü örnekleri var.
    *   Durum: kapandı (2026-11-04, Faz 7). Cargo.toml okunuyor: ağacında tabloları, satır içi tabloları ve tablo dizileri görünüyor; Format, Minify ve Sort keys çalışıyor. Start'ın dört örneği de okunuyor. "isn’t read yet" hâli kodda kalmadı; ağacı olmayan tek şey düz metin.
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
*   [x] **Tabletin metin ile ağaç arasındaki ayırıcısı sürüklenmiyor (2026-11-03).**
    *   Ne: Workspace-834 panosundaki "Resize the text and the tree" tutamacı çizilmiyor; metin 480 dp, ağaç geri kalanı. Bkz. `PANTRIX_FINDINGS.md`.
    *   Durum: kapandı (2026-11-04). Tablette metin ile ağaç ya da problemler arasında panodaki 13 dp'lik şerit ve `PantrixTwoPane`'in tutamacı var; metin 480 dp'den başlıyor, sürüklenen boyu ekran tutuyor. Tutamacın rengi ve dokunmatikteki şeridi panodan ayrılıyor: iki yeni Pantrix bulgusu.
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
*   [x] **YAML'da yalnızca ilk sözdizimi hatası bulunuyor (2026-11-03).**
    *   Ne: `snakeyaml-engine-kmp` ilk sözdizimi hatasında duruyor; Formatico o hatayı, ondan önce okunanı ve yinelenen anahtarlar ile anchor'ı olmayan alias'ları gösteriyor. Hatanın kütüphanedeki İngilizce açıklaması saklanmıyor (`ProblemCode.YamlSyntax`): problem listesi yazılınca neyin yanlış olduğunu söyleyecek bir yol gerekecek.
    *   Öneri: Invalid ekranıyla birlikte kütüphanenin sorun türlerini (`ScannerException`, `ParserException`) kendi kodlarımıza eşlemek.
    *   Durum: kapandı (2026-11-04). Kütüphanenin hatası kendi kodlarımıza eşleniyor, her birinin iletisi ve yapılabildiği yerde onarımı var: değerdeki iki nokta (`YamlColonInValue`, "Quote the value"), girintideki sekme (`YamlTab`, "Use spaces"), kapanmayan akış (`YamlUnclosedFlow`, parantez satırın sonunda kapanıyor), kapanmayan dizgi, eksik iki nokta, geçersiz kaçış, girinti, anchor'ı olmayan alias; kalanı yine `YamlSyntax`. Okuyucu ilk hatada durmaya devam ediyor (sahibinin kararı); Repair all metni her onarımdan sonra yeniden okuduğu için sıradaki hataya da geçiyor.
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
    *   Durum: açık. Kapalı Minify ve Sort keys nedenini söylemiyor; panolar da söylemiyor, kapalı bir düğmenin ipucu Pantrix'te sınanmadı. CSV ↔ TSV Convert'te var (2026-11-05), ama yeni bir belge olarak: belgenin kendi ayırıcısını değiştirmek değil.
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
*   [x] **Web'de gezinti ile paylaşım bağlantısı aynı fragment'ı istiyor (2026-11-04).**
    *   Ne: `core:navigation` ekranı adresin `#` kısmına yazıyor (`#start`, `#document?id=3`). v1'in paylaşımı (Faz 13) da belgeyi bağlantının fragment'ında taşıyacak. Bir paylaşım bağlantısıyla açılan sayfa ilk ekranı yazınca bağlantının fragment'ı siliniyor; bugün tanımadığı bir `#` kısmını uygulama yok sayıyor, ama adresi yine kendi girdisiyle değiştiriyor.
    *   Öneri: Faz 13'te paylaşılan belgeyi fragment'tan okuyup açtıktan sonra adresi gezintiye bırakmak (`#share=…` okunur, sonra `#document?id=N` olur); ya da navigation3-browser'ın birleşmemiş PR #9'u gibi gezintiyi yola (`/document/3`) taşımak, bu da sunucunun her yolu `index.html`'e yönlendirmesini ister.
    *   Durum: kapandı (2026-11-06). İlk öneriyle: bağlantı `#d=…&n=…` (sahibinin seçtiği ad, `&n=`). Sayfa onunla açılınca `FollowBrowserHistory` fragment'ı belgeyi açana (`DocumentOpening.openLink`) veriyor, sonra adres gezintiye kalıyor: `#start`, belge açılınca `#document?id=N`. Paylaşılan belge tarayıcının geçmişinde kalmıyor; tarayıcı fragment'ı sunucuya hiç göndermiyor.
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
*   [x] **TOML, INI, properties ve XML'in renklendiricisi basit (2026-11-04).**
    *   Ne: Bu biçimler henüz okunmuyor (Faz 7). Editör onları bir satırda gördüğü kadarıyla renklendiriyor: bölüm adı, anahtar, değer, yorum, etiket ve öznitelik. TOML'un birkaç satıra yayılan dizgisi ve dizisi, XML'in CDATA'sı satır satır renkleniyor.
    *   Öneri: Faz 7'de her biçimin okuyucusuyla birlikte renklendiricisini de tamamlamak.
    *   Durum: kapandı (2026-11-04, Faz 7). TOML'un kendi renklendiricisi var (`TomlHighlighter`): çok satırlı dizgiler ve satırlara yayılan diziler sonraki satırlara taşınıyor, satır içi tablonun anahtarları anahtar renginde, tarih ve saatler sayı renginde. Properties'in (`PropertiesHighlighter`) ters eğik çizgiyle devam eden değeri sonraki satırlarda da dizgi; anahtar, kaçışı olmayan ilk `=`, `:` ya da boşlukta bitiyor. XML'de CDATA ve satırlara yayılan öznitelik değerleri taşınıyor. INI eski renklendiricide kaldı (`SettingsHighlighter`), çünkü satırları tek başına duruyor. Taşınmayan: XML'de birkaç satıra yayılan bir DOCTYPE iç alt kümesi ve işleme yönergesi.
*   [x] **Telefonun Invalid panosundaki şerit ve problem kartı yok (2026-11-04).**
    *   Ne: Invalid-390 metnin üstünde "Not valid JSON: 4 problems" şeridini ve "Repair all"u, altında da bir problem kartını çiziyor ("Problem 2 of 4 · line 10, column 5", önceki ve sonraki, onarım). Kodda telefonun Problems görünümü problemlerin listesi; metinde problemler işaretli, imleç üstündeyken iletisi görünüyor; Repair alttaki araç çubuğunda.
    *   Öneri: şeridi ve kartı Text görünümüne eklemek; kart Pantrix'te bir bileşen değil, Formatico'nun Pantrix'in parçalarıyla kurduğu bir kart olur.
    *   Durum: kapandı (2026-11-04). Telefonun Text görünümünde şerit (`PantrixTag` ve "Repair all") ve kart (`PantrixCard`: "Problem i of n · line, column", önceki ve sonraki, ileti, onarım); kartın problemi imlecin içinde olduğu ya da ondan sonraki problem. Alttaki çubukta Repair varken Format ikinci planda, Minify yok, panodaki gibi. Telefonun Problems listesinde bir probleme basmak metne geçiyor. İmlecin satırındaki işaret çizilmiyor; o Pantrix'in bulgusu.

*   [ ] **Telefonun Invalid görünümünde dört görünüm var, panoda üç (2026-11-04).**
    *   Ne: Invalid-390'ın görünüm seçimi Text, Tree ve Problems. Kodda Problems, belgenin tablosu olmadığında Table'ın yerini alıyor; panonun settings.json'ının bir tablosu çıktığı için telefonda dört görünüm var (Text, Tree, Table, Problems). 390'a sığıyorlar, ama pano bire bir değil.
    *   Öneri: sahibinin kararı: geçersiz belgede Table'ı telefonda hep Problems'a bırakmak (tablo Tree'den ya da düzeltilince açılır) ya da panoyu dört görünümle çizmek.
    *   Durum: açık.
*   [ ] **Workspace panolarının "unread" hâli artık olmuyor (2026-11-04).**
    *   Ne: Workspace-1440, 834 ve 390'ın `unread` hâli Cargo.toml'u "TOML isn’t read yet" ve "Formatico reads JSON, JSONC, JSON5 and YAML today; TOML comes next." ile, durum çubuğunda "TOML, not checked" ile, Format, Minify ve Sort keys'i kapalı çiziyor. Faz 7'den beri her biçim okunuyor; kodda bu hâl ve sözcükleri kalktı. Ağacı olmayan tek şey düz metin ("Plain text has no tree"); onun panosu yok.
    *   Nerede: tuvaldeki Workspace panolarının `state` seçeneği (`unread`).
    *   Öneri: sahibinin kararı. Bu hâl ya tuvalden kalkar, ya düz metnin hâli olarak yeniden çizilir.
    *   Durum: açık.
*   [ ] **Yeni biçimlerin ağacı panolarda yok (2026-11-04).**
    *   Ne: Tuvalde XML yalnızca Query-834'te ve yalnızca metin olarak var. TOML, NDJSON, properties ve INI'nin ağacı hiçbir panoda yok. Ağacın biçimi bu yüzden kodda seçildi, YAML'ın satırlarına göre:
        *   XML: kök öğe `$`'ın altında, kendi adıyla. Öznitelikler `@ad` (değeri tırnaklarıyla), aynı adlı öğeler bir dizi, karışık içerikte ya da özniteliği olan öğede metin `#text`. Yalnızca metni olan öğe o metin, boş öğe boş bir dizgi. Yorumlar ve yönergeler ağaçta yok.
        *   TOML: tablolar nesne, tablo dizileri dizi. Tarih ve saat yazıldığı gibi bir dizgi, önizlemesi tarih.
        *   NDJSON: kayıtların dizisi, her satır bir öğe.
        *   Properties: anahtarların düz bir nesnesi. `server.port` tek bir anahtar.
        *   INI: ilk bölümden önceki anahtarlar ve bölümler, her bölüm bir nesne.
        *   Tek kökün altındaki dal kendiliğinden açılıyor; XML'de bu, kök öğenin çocukları demek.
    *   Öneri: sahibinin kararı. Ya bu ağaçlar tuvale çizilir, ya koddaki seçim kabul edilir.
    *   Durum: açık.
*   [ ] **XML, TOML ve INI'de onarım yok (2026-11-04).**
    *   Ne: `Repairs.of` artık biçimi alıyor; bir kodun onarımı yalnızca o biçimin onarımıysa uygulanıyor. Önceden JSON'un onarımları her biçime uygulanabilirdi: XML'de kapanmayan bir yorum ` */` ile kapatılırdı, tek tırnaklı bir değer `"` ile kapanırdı, metindeki bir `<` silinirdi. Bugün XML'in ve INI'nin hiç onarımı yok. TOML'da yalnızca virgül onarımları var (fazla virgülü çıkarmak, eksik virgülü eklemek), properties'te yalnızca kaçış onarımı var.
    *   Öneri: XML'de kapanmayan öğeye bitiş etiketi, tırnaksız özniteliğe tırnak, `&` yerine `&amp;`; TOML'da dizgiyi kendi tırnağıyla kapatmak; INI'de başlığı `]` ile kapatmak.
    *   Durum: açık.
*   [x] **Telefonda ağaçta aramayı ve Summary'yi açan bir şey panolarda yok (2026-11-04).**
    *   Ne: Workspace-390 panosu ağaçta aramanın çubuğunu ve seçili değer kartında "match 1 of 4"ü çiziyor; Summary-390 panosu Summary'yi çiziyor. Telefonda ikisini neyin açtığı çizilmemiş: telefonda durum çubuğu yok, üst çubuğun arama düğmesi komut paleti ("Search or run a command").
    *   Öneri: ikisi için telefonun "more" menüsüne birer satır; ya da arama için ağacın üstünde bir simge, Summary için görünüm seçicisinde kalıcı bir "Summary".
    *   Durum: kapandı (2026-11-04). Sahibinin kararıyla ikisi de telefonun "more" menüsünde: "Find in the tree" ağacı açıp arama çubuğunu yolun yerine koyuyor, "Show the summary" Summary'yi gösteriyor (görünüm seçicisinde tablonun yerinde). Seçili değerin kartı hangi eşleşme olduğunu da söylüyor ("match 1 of 3"), panodaki gibi. Aramanın açık olduğu artık belgenin görünümünde (`DocumentView.searching`): menü üst çubukta, workspace'in dışında. Tuvalde telefonun "more" menüsü çizili değil.
*   [ ] **Summary panolarının yazı boyları ve aralıkları ölçekte yok (2026-11-04).**
    *   Ne: Summary panoları başlıkları ve notları 13/18, karoların değerini 20/28 piksel yazıyor; aralıklarda 14, 10, 6 ve 20 piksel var. Tasarım sisteminin ve Pantrix'in ölçeğinde bunlar yok (yazı: 11, 12, 14, 16, 18, 22, 28, 36; aralık: 4, 8, 12, 16, 24, 32). Panolar elle çizilirken ölçeğin dışına çıkılmış.
    *   Öneri: tuvalde Summary panolarını ölçeğe getirmek (başlık `label`, değer `title`); ya da bu sapmayı kabul etmek.
    *   Durum: açık. Kodda temanın rolleri: başlık ve notlar `label`, karonun değeri `title`, aralıklar `PantrixTheme.spacing`.
*   [ ] **Durum çubuğu, boyutun düğmesiyle panodan uzun (2026-11-04).**
    *   Ne: Boyut Summary'yi açan bir düğme oldu; Pantrix ona platformun dokunma hedefini ayırdığı için çubuk masaüstünde 28'den 32'ye, tablette 32'den platformun hedefine (iOS'ta 44) uzuyor. Bkz. `PANTRIX_FINDINGS.md`, "Sık bir çubuktaki bir düğme dokunma hedefini alamıyor".
    *   Durum: açık.
*   [ ] **Ağacın satırı panolarda ölçekte olmayan bir boyda (2026-11-04).**
    *   Ne: Workspace panoları ağacın satırını Geist Mono'yla 13/20 ve normal kalınlıkta yazıyor (telefonda 14/20). Pantrix'in ölçeğinde 13 yok; kodun rolü `code` 14/20 ve orta kalınlıkta. Ağaç `TreeViewStyle(labelRole = TypeRole.Code)` ile o rolü kullanıyor.
    *   Öneri: tuvalde satırı `code`'a getirmek; ya da bu sapmayı kabul etmek.
    *   Durum: açık. Kodda temanın `code`'u. Query panolarının eşleşme satırları da 13/20 (telefonda değer 15/22, yol 12/16); kodda `code`, telefonda `code` gövdenin ve dipnotun boyunda (2026-11-05).
*   [ ] **Ağacın kökü panolarda `${4}` diye yazılıyor (2026-11-04).**
    *   Ne: Panolar kökün satırını iki nokta olmadan ve boşluksuz `$` ile boyutunu yan yana yazıyor: `${11}`, `${4}`. JavaScript'in şablon yazısına benziyor; bir boşluk (`$ {11}`) ya da yalnızca `$` daha az karışabilir. Kod panodaki gibi.
    *   Öneri: sahibinin kararı.
    *   Durum: açık.
*   [x] **Panoda ağaçtaki bağlantı simgesi bir bağlantı; satırın sonu basılamıyor (2026-11-04).**
    *   Ne: Workspace panolarında bir adresin satırının sonundaki simge bir bağlantı ("Open the link"). `PantrixLazyTreeView`'in `trailing`'i "satırla okunur, kendi basışı yoktur" diyor; satırın içinde ikinci bir basılan şey de ekran okuyucuda ve klavyede satırı ikiye böler. Kodda bağlantının yerinde "Link" sözcüğü (simge yok, bkz. PANTRIX_FINDINGS "Resmin ve bağlantının simgesi yok").
    *   Öneri: bağlantıyı açmak seçili değerin kartında ve ağacın satır menüsünde ("Open the link"); satırın sonundaki simge yalnızca gösterir.
    *   Durum: kapandı (2026-11-05). Sahibinin seçtiği biçimle Pantrix'e satırın eylemi geldi (`rowAction`, `TreeRowAction`; bkz. PANTRIX_FINDINGS "Ağacın satırının sonu basılamıyor"). Bağlantının satırı panodaki gibi bağlantı simgesiyle bitiyor ve o simge adresi açan düğme: tarayıcıda ya da adresi açan uygulamada, satırı seçmeden. Klavyede Shift+Enter, ekran okuyucuda satırın "Open the link" eylemi; hiçbir uygulamanın açamadığı bir adres yok sayılıyor. Renk ve resim yalnızca gösteriyor. `TreeLinkTest` her hedefte sınıyor.
*   [ ] **Ağacın arama çubuğu dar bir bölmede kapatma düğmesini sıkıştırıyor (2026-11-06).**
    *   Ne: Geniş düzende ağacın bölmesi dar kalınca, çubukta "No matches" yazınca satır sığmıyor: kapatma düğmesine 16×48
        dp kalıyor (tanılamanın `SmallTouchTarget`'ı; web'deki UI testinde, arama sürerken bir an görülen "No matches"la
        çıktı). Alan genişte 240, tablette 280 dp sabit; konum yazısı ve üç düğme kalanı paylaşıyor.
    *   Öneri: konum yazısı daralıp üç noktayla kısalsın (`weight(1f, fill = false)`), ya da alan kalan yere göre küçülsün.
    *   Durum: açık. Arama sürerken çubuk artık bir şey yazmıyor, bu yüzden test geçiyor; aranan gerçekten bulunmayınca
        sıkışma duruyor.
*   [ ] **Ağacın satırları her seçimde ve aramanın her harfinde yeniden kuruluyor (2026-11-04).**
    *   Ne: Seçili satırın ve aramanın işaretleri satırın etiketinde (`styledLabel`), bu yüzden `documentRows` seçim ya da aranan değişince bütün düğümleri yeniden kuruyor. Küçük belgelerde fark edilmiyor; Faz 14'ün 50 MB hedefinde ölçülmeli.
    *   Öneri: Faz 14'te ölçmek; gerekirse yalnızca değişen satırların yolunu yeniden kurmak.
    *   Durum: seçimde ve dal açılınca kapandı, aramada açık (2026-11-06). Ölçüldü: bütün satırları kurmak 10 MB'lık CSV'de (194 bin satır) 53 ms, 50 MB'ta (960 bin) 204 ms (JVM), emülatörde seçim yarım saniye. Ağaç artık kapalı dalların satırlarını kurmuyor, açılışta en çok 500 satırlık dalı açıyor (`OPENED_ROWS`) ve `RowMaker` değişmeyen satırları koruyor: seçimde ve bir dal açılınca yalnızca o satırların yolu yeniden kuruluyor (CSV 10 MB'ta 24 ms, 50 MB'ta 95 ms; çoğu kökün çocuklarını dolaşmak). Satırların biçimleri bütün satırlarca paylaşılıyor, etiketin metni bir kez yazılıyor: 112 bin satırlık NDJSON ağacı 91 MB'tan 47 MB'a indi. Aramanın her harfi hâlâ bütün satırları kuruyor; eşleşmeler artık ana iş parçacığının dışında aranıyor (10 MB CSV'de 76 ms). Seçili satırın renksiz kurulması Pantrix'in bir eksiği (PANTRIX_FINDINGS, "Seçili satırın biçimli etiketi kendi renkleriyle çiziliyor"). `DocumentTreeTest` sınıyor.

*   [ ] **Telefonda Query'ye giriş panolarda yok (2026-11-05).**
    *   Ne: Query-390 sorguyu görünümlerin içinde, tablonun yerinde gösteriyor (`Text`, `Tree`, `Query`), ama Workspace-390'da Query'yi açan bir şey yok: araç çubuğunun Query'si telefonda yok, More menüsü hiçbir hâlde çizilmemiş. Tabletin More düğmesinin adı ("More actions: sort keys, compare, save as a file") Query'yi saymıyor; tablette Query araç çubuğunda.
    *   Öneri: telefonun More menüsünü tuvale çizmek: Find in tree, Summary, Query, Sort keys, Compare, Close.
    *   Durum: açık. Kodda, arama ve Summary için verilen karar gibi (2026-11-04), Query telefonun More menüsünde, Summary'nin altında.
*   [ ] **Query'nin panolarda olmayan hâlleri kodda seçildi (2026-11-05).**
    *   Ne: Query panoları yalnızca eşleşmeleri bulunmuş bir sorguyu çiziyor. Kodda seçilenler:
        *   Boş sorgu: alanın altında "Write a JSONPath, as $.items[*].name" (XPath için "Write an XPath, as //item/title"); eşleşme listesi yok.
        *   Sorgu olmayan bir yazı: alan hata hâlinde, nedeni ve yeri ("… (character 9)"); liste yok.
        *   Eşleşmesiz bir sorgu: "No matches"; kopyalama ve belge olarak açma kapalı.
        *   Bir değere varan XPath (`count(//item)`, Query-834'ün son sorgularında var): başlık "Value", tek satırda XPath'in kendisi ve değeri.
        *   Okunamamış bir belge: ağacın bölmesinin iletisi.
    *   Öneri: tuvale bu hâllerin çizilmesi, ya da kodun seçiminin kabulü.
    *   Durum: açık.
*   [ ] **Sorgunun sonuçlarının biçimi ve adı kodda seçildi (2026-11-05).**
    *   Ne: "Copy the results" ve "Open as a document" panolarda var, ne verdikleri yok. Kodda JSONPath'in değerleri bir JSON dizisi (belge hangi biçimde olursa olsun; YAML'ın alias'ı gösterdiği değer olarak), XPath'in düğümleri bir XML belgesinin `<results>`'ı: bir öğe yazıldığı gibi, öteki düğümler değerleriyle bir `<result>`'ta; bir değere varan XPath tek bir `<result>`. İkisi de Format'la dizilmiş. Yeni belgenin adı belgenin adı ve `-query`: `order-query.json`, `feed-query.xml`.
    *   Öneri: tuvalde sonuçların belgesini çizmek, ya da bu seçimi kabul etmek.
    *   Durum: açık.
*   [ ] **Son sorgular ne zaman saklanıyor, kodda seçildi (2026-11-05).**
    *   Ne: Panolarda "Recent" altında son sorgular var. Kodda bir sorgu yazılırken değil, kullanılınca saklanıyor: klavyenin eylemi, bir eşleşmenin seçilmesi, sonuçların kopyalanması ya da belge olarak açılması. Her dilden en çok beş tanesi cihazda (`core:storage`), en yenisi önde; o anki sorgu aralarında gösterilmiyor. Geniş pencere ve tablet üçünü gösteriyor, telefon hepsini kayan bir satırda.
    *   Öneri: tasarımda bu kuralın onayı.
    *   Durum: açık.
*   [ ] **Önizlemelerin siparişi panolarınkinden başka (2026-11-05).**
    *   Ne: Panoların `order.json`'unda iki kalem var (T-shirt, Mug) ve bir `shipping` nesnesi; kodun önizlemelerindeki siparişte (`PreviewOrder`) üç kalem (Keyboard, Mouse, Desk pad) ve `shipping` yok. Query-1440 "2 matches" diyor, önizlemesi "3 matches"; çizim aynı, içerik değil. Önizlemeler bir zaman panolardan alınmış olmalı; panolar sonra değişmiş.
    *   Öneri: `PreviewOrder`'ı panolardaki belgeye getirmek ve bütün ekran görüntülerini yeniden kaydetmek (Workspace, Table, Summary, Query, Convert, uygulamanın panoları), ayrı bir değişiklikte. Uygulamanın Convert panoları (`app/shared/screenshots/Convert-*`) panolardaki siparişle çiziliyor: uygulamanın örneği o.
    *   Durum: açık.
*   [ ] **Büyük bir XML'de XPath her sorguda dizinini yeniden kuruyor (2026-11-05).**
    *   Ne: Workspace bir XML metnini her metin için bir kez okuyor, ama `XPath.evaluate` her sorguda belgenin dizinini (ebeveynler, sıra) yeniden kuruyor; her eşleşmenin değeri de (`XPathMatch.value`) hemen yazılıyor: `//*` büyük bir belgede metni derinlik kadar kopyalıyor. Sorgu her harfte soruluyor.
    *   Öneri: Faz 14'te 50 MB'ta ölçmek; gerekirse dizini belgeyle tutmak, değeri gösterilen satırlar için yazmak, sorguyu yazım durunca sormak.
    *   Durum: açık, ölçüldü (2026-11-06). Ölçümde asıl yük başka çıktı: her eşleşmenin ağaçtaki düğümü kökten, her düzeyde bütün çocuklara bakılarak aranıyordu (`nodeAt`); 25 bin `<order>`'lık 10 MB'ta `//order` karesel, dakikalar sürüyordu. `NodeLocator` sıralı çocukları yarılayarak arıyor; şimdi `//order` 10 MB'ta 442 ms, 50 MB'ta 2 s (JVM, ana iş parçacığının dışında). Dizinin ve değerlerin önerisi duruyor: Query her harfte soruyor ve bir harfin sorgusu öncekinin bitmesini bekliyor (iptal edilen hesap durmuyor), 50 MB'ta harf başına 2 s.
*   [ ] **CSV panosunun `numeric` sütunu `number` diyor, kod `text` (2026-11-05).**
    *   Ne: Table panolarının CSV hâlinde `numeric` sütununun türü `number`; değerleri ise başında sıfır olan kodlar ("004", "020"). Formatico böyle bir sütunu kod olarak, metin diye okuyor (Faz 4'ün kararı: baştaki sıfır kaybolmasın), türü de bu yüzden `text`, değerleri sola dayalı.
    *   Öneri: panoda o sütunu `text` yapmak, ya da sayı gibi görünen kodları sayı saymak (sıfırları kaybetmeden); karar tasarımın.
    *   Durum: açık. Kodda `text`.
*   [ ] **Convert'in panolarda olmayan hâlleri kodda seçildi (2026-11-05).**
    *   Ne: Convert panoları iki hâli çiziyor: hiçbir şeyin kaybolmadığı bir YAML (1440, 390) ve null'ı dışarıda kalan bir TOML (834). Kodda seçilenler:
        *   Kaybın on beş türü var (null, yorumlar, YAML'ın takma adları, yalnızca metin, tek düzey, kök, ad, iki kez verilen anahtar, sayının yazılışı, yazılamayan sayı, TOML'ın tarihi, yer değiştiren anahtar, boş değer, tek satır, XML'in `item`'ı); her biri kendi uyarısı, başlığı ve iletisiyle, alt alta. Üçten fazlası 280 dp'lik bir alanda kayıyor.
        *   Durum çubuğunun etiketi tek bir kayıpta onun etiketi ("2 values left out"), birden fazlasında sayısı ("3 losses").
        *   Null'ın seçimi geniş pencerede ve telefonda da var, TOML'dan başka XML, properties ve INI için de; CSV'nin null'ı her zaman boş alan.
        *   Tablette girintinin seçimi, girintili bir biçime (JSON, YAML, XML) dönüştürülürken; Convert-834 TOML'ı çizdiği için orada yok. Telefonda girintinin seçimi yok; belgenin seçimi geniş pencereden geliyorsa o kullanılıyor.
        *   "Nothing is lost"un iletisi yapılanları sayıyor: "129.90 and 50.00 keep their spelling" (panoda yalnız 129.90).
        *   Geçersiz bir belge dönüştürülmüyor: "The JSON has 2 errors. Repair them to convert the document."; düz metnin ağacın iletisi var.
        *   Telefonda Convert More menüsünden, Query'nin altından açılıyor; çubuğun geri düğmesi ve sistemin geri hareketi ağaca (tablonun belgesinde tabloya) dönüyor.
        *   Geniş pencerede soldaki metin editörün kendisi: yazılınca dönüşüm de yenileniyor.
    *   Öneri: tuvale kayıplı birkaç hâlin (XML, CSV) ve telefonun null seçiminin çizilmesi, ya da kodun seçiminin kabulü.
    *   Durum: açık.
*   [ ] **Dönüştürmenin kuralları kodda seçildi (2026-11-05).**
    *   Ne: Panolar JSON'ın YAML'a ve TOML'a nasıl yazıldığını gösteriyor; öteki biçimler kodda, Formatico'nun ağacının okuduğu gibi:
        *   XML: `@ad` öznitelik, `#text` metin, bir dizi aynı adlı öğeler; tek üyeli bir nesne kök öğe, başka her şey `<root>`'ta; dizinin içindeki dizinin öğeleri `<item>`; XML'in adı olmayan anahtar `_` ile (`first name` → `first_name`); null bir yorum ya da boş öğe.
        *   CSV ve TSV: dizinin öğeleri satırlar, bir nesne tek satır; iç değerler yolları adında sütunlar (`address.city`, `items[0].sku`); nesne olmayan bir öğe `value` sütunu; başlık satırların ilk verdiği sırayla.
        *   Properties: Spring'in adlandırması (`server.port`, `hosts[0]`). INI: belgenin kendi anahtarları önce, her nesne bir bölüm, bölümün içindeki iç değerler yollarıyla; satır sonları `\n`.
        *   TOML: bir tablonun kendi anahtarları tablolarından önce; nesne dizisi `[[ad]]`, öteki diziler satırında; tablo olmayan bir belge `items` ya da `value` altında; 64 biti aşan tam sayı dizgi.
        *   YAML: YAML 1.1'in de başka bir değer diye okuyacağı her dizgi tırnakta (`yes`, `no`, `on`, `y`, `007`, `1:20`, tarihler).
        *   JSON: sonsuz ve NaN dizgi; JSON5 kendi yazılışlarını (`0x1F`, `Infinity`) koruyor. NDJSON: dizinin öğeleri satırlar, başka her şey tek satır.
        *   İki kez verilen anahtar JSON'da ikisiyle, öteki biçimlerde sonuncusuyla.
    *   Öneri: bu kuralların sahibinin onayı; ayarlanabilir olması istenenler (CSV'nin düzleştirme biçimi, XML'in kök adı) Settings'e (Faz 12).
    *   Durum: açık.
*   [ ] **Telefonda sistemin geri hareketi Query ve Summary'den Start'a dönüyor (2026-11-05).**
    *   Ne: Telefonun Convert'i kendi ekranı; çubuğunun geri düğmesi de sistemin geri hareketi de onu bırakıp ağaca dönüyor (`NavigationBackHandler`). Query ve Summary de telefonda More menüsünden açılıyor, ama sistemin geri hareketi onlarda `NavDisplay`'in `onBack`'ine gidiyor: sekme bırakılıyor, Start açılıyor.
    *   Öneri: telefonda Query'nin ve Summary'nin de geri hareketiyle ağaca dönmesi.
    *   Durum: açık.
*   [x] **Null'ın yorumu sistemin dilinde yazılıyor (2026-11-05).**
    *   Ne: Dışarıda bırakılan bir null'ın yeni belgedeki yorumu (`# coupon = null in the JSON: TOML has no null`) view model'de `getString` ile okunuyor; o da sistemin dilini kullanıyor, uygulamanınkini değil.
    *   Öneri: Settings uygulamanın kendi dilini getirince (Faz 12) yorumun dili de ondan.
    *   Durum: kapandı (2026-11-06). Cihazlarda denendi: Android'de, iOS simülatöründe ve web'de Türkçe seçilince TOML'daki yorum `# coupon = null (JSON); TOML biçiminde null yok`; masaüstünde `AppLanguageTest` aynısını sınıyor. Web'de önce İngilizce çıktı: bileşim dışında `getString` `navigator.language`'ı okuyor, `index.html` yalnızca `navigator.languages`'ı değiştiriyordu; şimdi ikisini de değiştiriyor (PANTRIX_FINDINGS: "Uygulamanın kendi sözcüklerinin dili için yol gösterilmiyor"). Önceki durum: açık. Settings'in dili geldi (2026-11-05): seçilen dil platformun dili yapılıyor (`LocalAppLocale`: JVM'in ve Android'in `Locale`'i, iOS'un `AppleLanguages`'ı, web'de `navigator.languages`), `getString` de onu okumalı. Masaüstünde ekranın sözcükleri sınandı; yorumun dili hiçbir platformda sınanmadı.
*   [x] **Masaüstünde ve web'de Escape belgeden Yeni sekme'ye geçiyor (2026-11-06).**
    *   Ne: Compose masaüstünde ve web'de Escape'i sistemin geri hareketi sayıyor; o da `NavDisplay`'in `onBack`'ine gidiyor ve bir belgeden geri, Yeni sekme (Start) demek (telefonun geri hareketi için seçilmişti). Odak metinde değilken (Convert'in boş bir yerine tıklanınca, bir seçim kutusundan sonra) Escape'e basılınca belge sekmesi açık kalıyor ama Start'a geçiliyor. Web'de denendi; metnin içindeyken Escape belgede kalıyor.
    *   Öneri: masaüstünde ve web'de Escape açık olanı kapatsın (palet, menü, arama çubuğu), belgeden çıkarmasın; geri yığınının geri'si yalnızca telefonda ve tablette sistemin geri hareketiyle.
    *   Durum: kapandı (2026-11-06). Compose'un masaüstündeki ve web'deki penceresi, ekranda hiçbir şeyin almadığı Escape'i geri sayıyor (`BackNavigationEventInput`); ayırt etmenin yolu yok, ondan karar platforma göre: `core:navigation`'ın `systemBackLeavesScreens`'i Android'de ve iOS'ta doğru, masaüstünde ve web'de yanlış. `NavDisplay`'in geri'si, Settings'in ve telefonun Convert ile Compare ekranlarının geri'si (ortak `SystemBack`) ona bakıyor: masaüstünde ve web'de Escape belgeyi, Settings'i ve dar pencerede Convert'i, Compare'i yerinde bırakıyor, bunları kendi geri düğmeleri kapatıyor. Paleti, menüleri, seçim kutularını Escape eskisi gibi kapatıyor; tarayıcının geri düğmesi `BrowserHistory`'den geçiyor, değişmedi. Bilinen sınır: iPad'e bağlı klavyenin Escape'i de iOS'ta geri sayılıyor ve kaydırma hareketinden ayırt edilemiyor; orada belgeden çıkarmaya devam ediyor. Pantrix'in list-detail düzeni Escape'i masaüstünde geri sayıyor (sitede "Escape on a desktop"); Formatico onu kullanmıyor. Sınandı: `AppBackTest` (masaüstünde ve web'de kalıyor, Android'de ve iOS'ta gidiyor) ve `SystemBackTest`, JVM'de, iOS simülatöründe ve Wasm'da. Elle: web'de Escape belgede ve Settings'te kalıyor, paleti kapatıyor, tarayıcının geri'si Settings'ten belgeye dönüyor; Android emülatöründe geri tuşu belgeden Yeni sekme'ye gidiyor. Masaüstünde elle denenmedi.
*   [ ] **Sorgu sonuçlarının JSON'ı ayrı bir yazıcıyla yazılıyor (2026-11-05).**
    *   Ne: `QueryResults.kt`'nin kendi `JsonWriter`'ı var; `core:format`'ın dönüştürücüsü aynı işi (değerleri JSON olarak yazmak, YAML'ın takma adlarını açmak, JSON'ın yazamadığı sayıları dizgi yapmak) yapıyor.
    *   Öneri: sorgunun sonuçlarını dönüştürücüyle yazmak; ayrı bir temizlik değişikliği.
    *   Durum: açık.
*   [ ] **Compare'in panolarda olmayan hâlleri kodda seçildi (2026-11-05).**
    *   Ne: Compare panoları iki siparişin anlamla karşılaştırılmasını çiziyor. Kodda seçilenler:
        *   B, A'nın sekmesinden sonraki belge (A sonuncuysa öncekisi); B'nin menüsü öteki açık belgeleri sayıyor.
        *   Tek bir belge açıksa: "Nothing to compare with", yeni bir sekmede başka bir belge açmayı söylüyor.
        *   Anlamla karşılaştırılamayan bir belge (düz metin, okunamayan, hatalı): nedeni ve metinle karşılaştırmanın
            seçimi; seçim çubuğu duruyor.
        *   Metinle karşılaştırma satır satır: listede "Line 2" ya da "Lines 4–5" ve ilk satırın metni; "Not counted"
            uyarısı yok, alt şerit "Compared by text: every character counts".
        *   Hiç fark yoksa: "No differences" ve "A and B mean the same." (ya da "are the same text").
        *   Biçimleri ayrı iki belge anlamla karşılaştırılıyor: bir JSON ile onun YAML'ı aynı anlama geliyor.
        *   Telefonda "Show" bir sayfa açıyor: A'nın ve B'nin metni üst üste, farkın satırları birincil çubukla. Çubuğun More'u
            belgenin "more" menüsü. "Not counted" kartların altında, tek satır yazı.
        *   Geniş pencerede ve tablette çubuk araç çubuğunun, alt şerit durum çubuğunun yerinde; X görünümü ağaca (tablonun
            belgesinde tabloya) döndürüyor.
    *   Öneri: tuvale bu hâllerin çizilmesi, ya da kodun seçiminin kabulü.
    *   Durum: açık.
*   [ ] **Anlamla karşılaştırmanın kuralları kodda seçildi (2026-11-05).**
    *   Ne: Bir nesnenin üyeleri adlarıyla eşleşiyor (iki kez verilen bir ad, o adlıların arasındaki yeriyle); bir
        dizinin öğeleri ikisinin sırayla ortak en uzun dizisiyle, aradakiler yer yer karşılaştırılıyor, kalanlar eklenmiş
        ya da çıkarılmış. Sayılar değerleriyle (`129.90` = `129.9`, `0x1F` = `31`, `1e3` = `1000`), dizgiler değerleriyle
        (kaçışlar fark değil), TOML'ın tarihi aynı yazılışlı bir dizgiyle eşit; `1` ile `"1"` farklı. Farklar B'nin
        sırasıyla, A'dan çıkarılan bir değer A'da ardından geldiği değerin arkasında. Fark sayılmayanlar: yer değiştiren
        anahtarlar ve başka yazılmış sayılar.
    *   Öneri: sahibinin onayı; dizilerin anahtarla eşleşmesi (`id`'si aynı öğeler) istenirse Settings'e.
    *   Durum: açık.
*   [x] **Geçmiş, kaydettiği her metni uygulama kapanana dek bellekte tutuyordu (2026-11-06).**
    *   Ne: `StoredDocumentHistory`'nin `recorded`'ı, cihaza yazılmamış metinleri okumak için tutuluyordu, ama yazıldıktan
        sonra bırakılmıyordu: açılan ve her değiştirilen belgenin metni (10 MB'ta 20 MB) uygulama açık kaldıkça bellekteydi.
        Android'de iki 10 MB'lık belgeyle yığın küçük belgelere dönülünce bile 146–160 MB kalıyordu; iki büyük belge
        arasında geçişte uygulama bellek yetmezliğiyle kapandı.
    *   Öneri: metin cihaza yazılınca bırakılsın; o andan sonra cihazdan okunsun.
    *   Durum: kapandı (2026-11-06). Yazma bitince, metin o arada değişmediyse `recorded`'dan çıkıyor;
        `StoredDocumentHistoryTest` sınıyor. Android'de 10 MB'lık JSON açıkken yığın 161 MB; düzeltmeden önce yalnızca küçük belgeler açıkken bile 146–160 MB'tı.
*   [ ] **Büyük belgelerde ölçülüp bırakılanlar (2026-11-06).**
    *   Ne: Faz 14'ün ölçümünde (`docs/MEASURE.md`) hedefler karşılandı, ama şunlar yavaş ya da ağır kaldı:
        *   **Okumanın belleği:** okunmuş belge metnin 15–32 katı (10 MB'ta JSON 135 MB, CSV 320 MB; 50 MB'lık CSV 1,6
            GB). Her düğümün anahtarı ayrı bir metin (10 MB'lık CSV'de 1,36 milyon), aralığı ve yolu birer nesne.
            Telefonda yığın sınıra yaklaştıkça çöp toplayıcı sürekli çalışıyor ve okuma uzuyor.
        *   **Ağaçta arama:** her harf bütün satırları yeniden kuruyor (10 MB'lık CSV'de 53 ms, JVM'de).
        *   **Karşılaştırma:** ortası iki binden çok öğe olan, dağınık değişiklikli iki dizi hâlâ öğe öğe
            karşılaştırılıyor; ortaya konan birkaç öğe sonrakileri kaydırıyor. 50 MB'lık CSV'yi anlamla karşılaştırmak 8 s.
        *   **Biçimi sezmek:** uzantısı olmayan bir metin (yapıştırılan) bütünüyle JSON5 diye okunuyor: 10 MB'ta 250 ms,
            50 MB'ta 1 s, okumanın geçici belleğiyle; Workspace sonra bir daha okuyor.
        *   **YAML:** 50 MB'ta okuma 4 s, Format 3,7 s, Minify 4,4 s.
        *   **Yazarken:** yazım durunca belge baştan okunuyor (10 MB'ta JVM'de 0,4–1 s, telefonda birkaç saniye); okuma
            iptal edilemiyor, sonraki duraklama onu bekliyor.
        *   **Açık sekmeler:** okunmuşlar telefonda ve web'de 12 milyon, masaüstünde 60 milyon karakterle sınırlı
            (`readingsBudget`): sınırın dışında kalan büyük bir sekme, dönülünce yeniden okunuyor (telefonda 10 MB'ta
            5–10 s). Her açık sekmenin metni ve editörü de bellekte kalıyor.
        *   **iOS'un belleği:** 10 MB'lık CSV açıkken uygulama 1,35 GB, JSON'la 1,0 GB tutuyor; Kotlin/Native'in nesneleri
            JVM'dekilerin iki katı kadar. Yeni bir iPhone'da sorun değil, 4 GB'lık eskilerde sınıra yakın.
        *   **Web'in donması:** Wasm'da okuma ana iş parçacığında: 10 MB'lık JSON'da sayfa 2 s, CSV'de 2,3 s'ye kadar
            donuyor; Convert, Compare ve Summary de öyle.
        *   **Telefonda iki büyük belge arasında:** Android'de 10 MB'lık CSV kapatılıp 10 MB'lık JSON'a geçilince JSON
            okunurken yığın 547/576 MB'a çıkıyor: kapanan belgenin okunmuşu ve tablosu okuma sürerken bir süre
            bırakılmıyor. Geçmişin metin kopyaları bırakılmadan önce (aşağıda) aynı adımda uygulama bellek yetmezliğiyle
            kapandı (2026-11-06).
    *   Öneri: anahtarı ve yolu düğümde tutmamak, babasından türetmek; aramanın işaretlerini yalnızca eşleşen satırlarda
        kurmak; Myers'ın algoritması; sezmeyi metnin başıyla yapmak; okumayı iptal edilebilir yapmak; web'de okumayı bir
        Web Worker'a ya da parça parça, araya kare bırakarak yapmak.
    *   Durum: açık.
*   [x] **Büyük belgelerde karşılaştırma kısa kesiliyor (2026-11-05).**
    *   Ne: Dizilerin ve satırların ortak dizisi en çok dört milyon karşılaştırmayla (iki bin satıra iki bin satır)
        aranıyor; ötesinde iki dizi baştan sona değişmiş sayılıyor. Metinde baştaki ve sondaki ortak satırlar önce
        ayrılıyor, ama ortası büyükse fark tek bir koşu.
    *   Öneri: Faz 14'te ölçmek; Myers'ın algoritması ya da satırları özetleriyle (hash) karşılaştırmak.
    *   Durum: kapandı (2026-11-06). `commonRun` iki dizinin baştaki ve sondaki ortak öğelerini önce ayırıyor, sınır yalnızca aradakine uygulanıyor: 10 bin öğelik bir dizinin ortasına konan bir öğe tek fark (`CompareTest`). Ölçümde (`docs/MEASURE.md`) ortasında bir değeri değişmiş iki belge, altı biçimde 10 ve 50 MB'ta tek fark buluyor; anlamla karşılaştırma 10 MB'ta 0,8–1,5 s, 50 MB'ta 3,6–8,2 s (JVM, ana iş parçacığının dışında). Ortası büyük ve dağınık değişiklikli iki dizi hâlâ sınırın ötesinde: "Büyük belgelerde ölçülüp bırakılanlar".

*   [x] **Sorgu alanında Enter ve klavyenin tuşu hiçbir şey yapmıyordu (2026-11-06).**
    *   Ne: Query'nin alanı `onKeyboardAction` veriyor ama eylemi adlandırmıyordu; Pantrix adsız eylemde işleyiciyi hiç çağırmıyor (PANTRIX_FINDINGS). Bir sorgu yalnızca bir eşleşme seçilince, kopyalanınca ya da açılınca "son kullanılanlar"a giriyordu. Query panosunu çizen yeni gezinti buldu.
    *   Durum: kapandı (2026-11-06). Alan `ImeAction.Search`'ü adlandırıyor; `QueryScreenTest.theKeyboardsActionUsesTheQueryAsked` sınıyor.
*   [x] **Telefonda ağaç gösterilirken yapılan bir değişiklik metne gelmiyordu (2026-11-06).**
    *   Ne: Telefon metni ya da ağacı gösteriyor. Ağaç gösterilirken Minify (alt çubuktan ya da paletten) belgeyi küçültüyor, ama metne dönülünce editör eski metni gösteriyordu. Okuyucu o metne yazarsa eski metin belgeye geri yazılacak, Minify kaybolacaktı. `DocumentEditor` belgeden en son aldığı metni yalnızca kompozisyondayken tutuyordu: geri gelince belgenin yeni metniyle başlıyor, farkı görmüyordu. Ayrıca yazının yarım saniyelik beklemesi bitmeden metin ekrandan çıkarsa son yazılan belgeye hiç yazılmıyordu. Maestro'nun palet akışı Android'de buldu.
    *   Durum: kapandı (2026-11-06). Editörün bildiği metin editörün kendi metniyle başlıyor; ekrandan çıkarken yazılmamış olan yazılıyor. `DocumentEditorTest`'in iki yeni testi önce kırmızıydı.
*   [x] **Telefonun geçersiz belge şeridindeki "Repair all" Android'in dokunma hedefinden alçaktı (2026-11-06).**
    *   Ne: Şerit panodaki gibi 44 dp yüksekti; Android'in dokunma hedefi 48 dp. Teşhisler `SmallTouchTarget` buldu (pano testi, 390).
    *   Durum: kapandı (2026-11-06). Şerit en az 44 dp: Android'de 48'e uzuyor, iOS'ta 44 kalıyor. Panodan 4 dp'lik bir ayrılık; panoya Android'in hâli çizilmedi.
*   [ ] **Telefonda biçimi okunamayan bir metnin biçimi seçilemiyor (2026-11-06).**
    *   Ne: Telefonda yeni boş belgeye yazılan JSON "Plain text" kalıyor: biçim seçici (`FormatMenu`) yalnızca geniş araç çubuğunda, telefonun "more" menüsünde ve paletinde biçim seçen bir komut yok. Biçim yalnızca açılırken (yapıştırma, dosya) anlaşılıyor. Maestro'nun Invalid akışını kurarken görüldü; akış metni kopyalayıp Start'ta yapıştırarak açıyor.
    *   Nerede: `WorkspaceScreen.kt` (`FormatMenu`), `PhoneWorkspace.kt`, `DocumentPalette.kt`.
    *   Öneri: telefonda durum etiketi ("Plain text") biçim menüsünü açsın, ya da paletin Document grubuna "Read as …" komutları. Önce Workspace-390'a çizilir.
    *   Durum: açık. Sahibinin kararı.
*   [ ] **Enter sorgu yazıldıktan hemen sonra basılırsa önceki sorguyu saklıyor (2026-11-06).**
    *   Ne: Alanın metni görünüm modeline bir kare sonra ulaşıyor (`snapshotFlow`); `UseQuery` sorguyu taşımıyor, durumdakini okuyor. Yazıp o karede Enter'a basan önceki sorguyu "son kullanılanlar"a ekler. Gezinti bunu, eşleşmenin gelmesini bekleyerek aşıyor.
    *   Öneri: `WorkspaceIntent.UseQuery(query)`, alanın o anki metniyle.
    *   Durum: açık, küçük.
*   [ ] **Problem satırının kendisinin etiketi yok; onarım düğmesi `problem_N` (2026-11-06).**
    *   Ne: Problems listesinde satırın probleme götüren tıklanabilir kısmı Formatico'nun kendi `clickable`'ı ve etiketsiz; `Button_problem_1` satırın değil, onarım düğmesinin etiketi. Gezinti ikinci probleme giderken önce onu onardı. Bir Maestro akışı da satıra etiketle basamaz.
    *   Öneri: satır `Modifier.pantrixTestTag(rememberPantrixTestTag("ProblemRow", "problem_N"))` (ya da bir Pantrix bileşeni), düğme `repair_N`.
    *   Durum: açık.
*   [ ] **Masaüstünün testi telefonun paylaşma sayfasını bağlantı alanı odaklı çiziyor; cihazda ve panoda odaksız (2026-11-06).**
    *   Ne: Faz 15'in gezintisi kontrollere ekran okuyucu gibi bastığından beri Share-390 ve Share-834'ün masaüstü resminde bağlantı alanı odaklı (kalın, birincil renkte kenar). Eski resimlerde ve panoda odaksız. Android'de (Pixel 7 Pro) ve iOS simülatöründe Maestro'nun `share` resmi de odaksız. Uygulamanın değil, testin çiziminin farkı: dokunma kipine geçmek değiştirmedi, nedeni bulunmadı.
    *   Nerede: `app/shared/screenshots/Share-390.png`, `BoardTour.kt`.
    *   Öneri: masaüstü testinde sayfa açılmadan odağın nerede olduğuna bakılsın.
    *   Durum: açık, testin.

*   [ ] **Tablette bir menüden seçilince menünün düğmesinde odak halkası kalıyor (2026-11-07).**
    *   Ne: Types-834'te dil menüsünden TypeScript seçilince menü kapanırken odağı düğmesine geri veriyor, düğmede
        klavyenin halkası görünüyor: JVM'nin tablet panosunda ve iOS simülatöründe, satıra dokunarak seçilince de.
        Android'in tablet panosunda (Robolectric) ve iOS'un telefon panosunda aynı akışta halka yok. Panoda halka
        yok. Gerçek bir iPad'de ya da Android tablette denenmedi.
        Faz 19'da Schema-834'ün şema menüsünde de aynısı (2026-11-09): order.schema.json seçilince menünün düğmesi
        halkayla kalıyor.
    *   Nerede: `app/shared/screenshots/Types-834.png`, `ios/Types-834.png`, `Schema-834.png`; turları
        `BoardTour.types()` ve `BoardTour.schema()`.
    *   Öneri: gerçek bir tablette dokunarak denemek; orada da görünürse Pantrix'in bulgusu (PantrixMenu odağı geri
        verirken menünün dokunmayla kullanıldığını hesaba katmıyor), görünmezse testin ortamına özgü.
    *   Durum: açık.
*   [ ] **Android'in Invalid-834 panosunda imleç bazen 1. satırda kalıyor (2026-11-09).**
    *   Ne: `recordRoborazziAndroidHostTest` aynı kodla iki koşuda Invalid-834-dark'ı farklı çizdi: birinde imlecin
        satırı 1, ötekinde panodaki gibi 10. Tur metni seçip problemi gösteriyor; Robolectric'te imlecin yerine
        oturması görüntüden sonraya kalabiliyor. Doğrulama koşusu (`verify…`) bu yüzden ara sıra düşebilir.
    *   Nerede: `app/shared/screenshots/android/Invalid-834-dark.png`; turu `BoardTour.invalid()`.
    *   Öneri: tur, görüntüden önce durum çubuğunun "Ln 10, Col 5"ini beklesin.
    *   Durum: açık.
*   [ ] **Büyük bir dizinin grafiğinde kök kart görünmeden açılıyor (2026-11-09).**
    *   Ne: Faz 18'in Graph görünümü, hiçbir değer seçili değilken grafiği sol üstünden gösteriyor. 150 kişilik bir
        dizide (`GraphCases`'in "large" durumu) `PantrixGraph` kök kartı çocuklarının ortasına koyuyor: ekranda
        `people.json[0]`'dan `[5]`'e kartlar ve soldan gelen bağlar var, kökün kartı birkaç bin dp aşağıda. Seçili
        bir değer olunca grafik ona gidiyor; Fit hepsini gösteriyor ama okunmayacak kadar küçük.
    *   Nerede: `feature/workspace/screenshots/Graph/large-light-1440.png`; `DocumentGraphView`'ın açılışı.
    *   Öneri: seçili değer yokken açılışta kökü görünür kılmak (`bringIntoView(root)`), ya da Pantrix'e bir öneri:
        `GraphStyle`'da ebeveynin çocuklarının ortasına değil ilkinin hizasına konması (ağacın satırları gibi).
        Hangisinin daha iyi okunduğu sahibinin kararı.
    *   Durum: açık.

## Üst çubuk

*   [ ] **Sekmeler sığmayınca ilki logonun yanında yarım kesiliyor (2026-11-06).**
    *   Ne: Belge sekmeleri `PantrixTabs(scrollable = true)`; şerit, seçili sekmeyi ortasında tutuyor. Şeridin genişliği logo ile arama alanı arasında kalanla sınırlı: Android'in geniş penceresinde iki sekme ("order.json", "New tab") bile sığmıyor ve ilki "ler.json" diye logonun yanında kesiliyor; web'de altı sekmede ilki "ız" kalıyor. Kenarda bir solma ya da devamının olduğunu gösteren bir işaret yok.
    *   Öneri: şeridin başında ve sonunda bir solma (Pantrix'te `PantrixTabs`'ın kaydırılan kenarı için bir seçenek yoksa bulgu); ya da seçili sekme ortada değil görünür olduğu kadar kaydırılsın, ilk sekmeler sığdığı sürece başta kalsın.
    *   Durum: açık.

## Paylaşım

*   [ ] **Paylaşımın panolardan ayrıldığı yerler (2026-11-06).**
    *   Ne:
        *   Share, belgenin "More actions" menüsünde (geniş, tablet, telefon), Close'un üstünde; menü panolarda çizili değil. Satırın simgesi yok: `PantrixIcons`'ta paylaşma simgesi yok. Komut paletinde Share yok: Palette panolarının listesinde yok.
        *   Telefonda çok büyük bir belge, Share-834'ün penceresinin sözcükleriyle bir sayfada (alt sayfa): Share-390 yalnızca bağlantıyı çiziyor.
        *   "Send with another app" platformun kendi paylaşma sayfası: Android'in seçicisi, iOS'un UIActivityViewController'ı, tarayıcıda `navigator.share` (telefonların tarayıcıları); masaüstünde ve onu olmayan bir tarayıcıda düğme yok.
        *   Bağlantı hazırlanırken alan boş, Copy the link kapalı; anahtar değişince önceki bağlantı yenisi gelene kadar kalıyor.
        *   Biçimlendirme anahtarı düz metinde ve hatalı bir belgede kapalı (Format onları yazamıyor); belgenin kendisi değişmiyor, yalnızca bağlantıdaki metin biçimlendiriliyor. Anahtar her açılışta kapalı başlıyor.
        *   Bağlantının sıkıştırması Formatico'nun kendi saf Kotlin deflate'i (`core:share`), zlib biçiminde: her platformda aynı bağlantı. 2 MB'tan büyük bir metin sıkıştırılmadan "çok büyük" sayılıyor.
        *   Bağlantı Android ve iOS uygulamasını açmıyor (sahibinin kararı): web açıyor, uygulamada herhangi bir yere yapıştırılınca da açılıyor. App Links ve Universal Links site yayına girince (Faz 22).
    *   Öneri: sahibinin onayı.
    *   Durum: açık.

*   [ ] **Web'de açılamayan bir bağlantı adres çubuğunda kalıyor (2026-11-07).**
    *   Ne: Açık bir Formatico sekmesinin adresine yazılan bir bağlantı açılamayınca (`#d=` bozuk, `#s=` silinmiş ya da
        süresi dolmuş, sunucuya ulaşılamıyor) uyarı çıkıyor, ekran olduğu gibi kalıyor, ama adres `#s=…&k=…` olarak
        kalıyor: yeniden yükleyince aynı uyarı yine geliyor. Açılınca adres `#document?id=…` oluyor, anahtar o girişten
        gidiyor. Sayfa bağlantıyla yeni açıldığında da adres `#start`'a dönüyor; yalnızca açık bir sekmede kalıyor.
        Faz 16'da yerel yığında, tarayıcıda görüldü; `#d=` için Faz 15'ten beri böyle.
    *   Nerede: `core:navigation` (`BrowserHistory.web.kt`: adres yalnızca ekranlar değişince yazılıyor),
        `app/shared` (`FollowBrowserHistory`).
    *   Öneri: `onLink` bir belge açmadan bitince, sekmenin girişine o anki ekranlar yazılsın (`replaceState`).
    *   Durum: açık.

*   [ ] **Yerel yığında ve bir deneme ortamında yapılan bağlantı sitenin adresini taşıyor (2026-11-07).**
    *   Ne: Bağlantılar her yerde `https://formatico.pantrixui.com/#…` (`ShareLinks.SITE`). Web'de sunucu sayfanın kendi
        adresinde (`platformServer()`), ama bağlantı değil: `localhost:4005`'te yapılan şifreli bir bağlantı sitede
        açılıyor, paylaşım ise yerel sunucuda. Faz 16'nın denemesinde adres elle `localhost:4005` yapıldı. Yayındaki
        sitede fark yok.
    *   Öneri: web'de, sayfa http(s) ile açıldıysa, bağlantı sayfanın kendi adresiyle (`window.location.origin`);
        uygulamalarda site. Sahibinin kararı: bir deneme ortamı (Coolify'da `staging`) olacaksa gerekli.
    *   Durum: açık.

## Komut paleti

*   [ ] **Komut paletinin panolardan ayrıldığı yerler (2026-11-05).**
    *   Ne: Kodda tek bir kural var, panolar genişliğe göre biraz ayrılıyor:
        *   Tablette liste masaüstününki. Palette-834'te Save as a file ve Use the dark theme yok, Query ile Paste'in kısayolu yazılmamış; bunun bir nedeni görünmüyor.
        *   Telefonda Convert'te XML de var. Palette-390 YAML, TOML ve CSV'yi listeliyor; kod her genişlikte JSON, YAML, TOML, XML ve CSV'den belgenin kendi biçimi dışındakileri listeliyor.
        *   Web'de Command ya da Control ile T, 1 ve 2 tarayıcının (yeni sekme, sekmeler arası geçiş): sayfa bunları duymuyor. Yeni sekmenin, ağacın ve tablonun komutları web'de kısayolsuz.
        *   Start'ta palet yalnızca Formatico'nun komutlarını listeliyor; bu hâl panoda çizilmemiş.
    *   Öneri: sahibinin kararı: Palette-834 ve Palette-390'ı koda göre düzeltmek, ya da kodu panolara göre ayırmak.
    *   Durum: açık.

## Settings

*   [ ] **Settings'in panolardan ve önceki davranıştan ayrıldığı yerler (2026-11-05).**
    *   Ne:
        *   Telefonda tema ve girinti seçimleri satırı doldurmuyor, her seçenek sözcüğü kadar: üç eşit parçada "As the system" sığmıyordu.
        *   Pantrix'in seçim kutusu (40 dp) ve anahtarlı satırı (etiket birkaç piksel içeride) panodakinden biraz farklı.
        *   Tablette Settings'in üstünde "+" (yeni sekme) duruyor; Settings-834'te yok.
        *   "Keep each document for"un ilk değeri panodaki gibi 30 gün: 30 günden eski belgeler artık uygulama açılınca geçmişten çıkıyor. Önceden yalnızca 20 belge sınırı vardı.
        *   "Keep a history" kapatılınca geçmiş temizleniyor ve yeni belge kaydedilmiyor (bir karar: kapatmak "tutma" demek).
        *   Biçimi anlaşılmayan metin, soru sorulmadan önce geçmişe "Text" olarak yazılıyor; sonra JSON olarak açılsa da Recent'te "Text".
        *   Dil değişince uygulamanın bileşimi baştan kuruluyordu (`key`), ekranların kaydırma yeri gibi kalıcı olmayan hâller gidiyordu; sözcükler Kotlin'e taşınınca (2026-11-06) `key` kalktı, ekranlar dil değişince yerinde kalıyor.
        *   Lisans listesi elle tutuluyor (`OpenSourceLibrary`): bir bağımlılık eklenince ya da çıkınca güncellenmeli.
        *   Keyboard shortcuts, paletin kısayolu olan komutlarını uygulamanın çalıştığı yerde listeliyor: tarayıcıda New tab, Show the tree ve Show the table yok (⌘T, ⌘1, ⌘2 tarayıcının); panolar web'i çizmiyor. Girişteki cümle ("On Windows and Linux, Ctrl takes the place of ⌘") panodaki gibi her sistemde aynı; Windows'ta kapaklar zaten Ctrl diyor.
        *   Panoların "shortcuts" hâli sayfanın aşağısı; önizleme Settings'i o bölüme kaydırılmış açıyor (`at`), önceki bölümün alt kenarı üstte görünüyor. Telefonda bölüm yok, orada History'ye kaydırılıyor.
        *   Başlatıcının `⌘K`'sı ve Start'ın `⌘V` ipucu panolarda tek kapak; `PantrixKeycaps` her tuşa bir kapak veriyor (`Ctrl` `K`), paletin ve Settings panosunun yaptığı gibi. Panolar tuş başına kapağa çekilebilir, ya da Pantrix'ten tek kapaklı bir biçim istenebilir.
    *   Öneri: sahibinin onayı; geçmişin "Text" yazılması için kayıt sorunun cevabından sonraya alınabilir.
    *   Durum: açık.

*   [x] **Bir dil seçilince cihazın diline dönülemiyor (2026-11-06).**
    *   Ne: Tema'da "As the system" var, Dil'de yoktu: seçim kutusu yalnızca English ve Türkçe sunuyordu, panolar da öyle çiziyordu. Seçim yapılmadıkça uygulama cihazın dilini izliyor (cihazlarda denendi, 2026-11-06: Android'de uygulama açıkken anında; iOS'ta uygulama yeniden açılınca, çünkü iOS dil değişince uygulamaları kapatıyor). Ama bir dil seçildikten sonra cihazın dilini izlemeye dönmenin yolu yoktu, uygulamanın verisini silmekten başka.
    *   Nerede: `feature/settings` `SettingsSections.kt` (`GeneralSection`).
    *   Durum: kapandı (2026-11-06, sahibinin onayı). Dil seçiminin ilk seçeneği "As the system" / "Sistemdeki gibi" (`language = null`), bir dil seçilmedikçe seçili olan o; panolar da öyle çiziyor (kanvas v81–82). "Sistemdeki gibi" telefonun 160'lık kutusuna sığmıyordu: Settings'in seçim kutuları her genişlikte 200, telefonun panosu da. `AppLanguage.forSystem` artık kullanılmadığı için kalktı.

*   [ ] **Android'in uygulama başına dil ayarı yok (2026-11-06).**
    *   Ne: Android 13'ün Ayarlar > Uygulamalar > Formatico > Dil satırı çıkmıyor: uygulama `localeConfig` bildirmiyor. `cmd locale set-app-locales` ile verilen dili uygulama yine de anında izliyor (denendi, 2026-11-06). iOS'ta `Info.plist`'in `CFBundleLocalizations`'ı aynı satırı Ayarlar'da açıyor olmalı; denenmedi.
    *   Nerede: `app/androidApp/src/main/AndroidManifest.xml`.
    *   Öneri: `res/xml/locales_config.xml` (`en`, `tr`) ve manifest'te `android:localeConfig`. Settings'in "Sistemdeki gibi"si o zaman Android'in uygulama için seçtiği dili izler. Sahibinin kararı.
    *   Durum: açık.

## Sunucu

*   [x] **Paylaşımların sayısına ve diskine sınır yok (2026-11-07).**
    *   Ne: Sunucu bir paylaşımı en çok 10 MB ve 30 gün tutuyor; nginx bir adresten saniyede ikiden fazla paylaşım ve
        vekil isteği almıyor (`limit_req`). Ama bir adresin toplamda kaç paylaşım bırakabileceğine, sunucunun diskte
        ne kadar tutacağına sınır yok: biri günlerce yükleyerek sunucunun diskini doldurabilir.
    *   Nerede: `server` (`ShareStore`), `deploy/web/default.conf`.
    *   Öneri: sunucuya toplam bir sınır (örneğin 5 GB; aşılınca `507` ve uygulamada "sunucu şu an paylaşım
        almıyor"), ve bir adrese günde bir paylaşım sayısı. Sahibinin kararı: diskin ve ne kadar paylaşılacağının sınırı.
    *   Durum: kapandı (2026-11-07), sahibinin onayıyla önerilen değerler. Paylaşımlar toplamda en çok 5 GB
        (`formatico.shares.max-total`): `ShareStore` kayıtlardan saydığını açılışta topluyor, her kayıtta ve silmede
        güncelliyor; dolunca `507`. Bir adres günde en çok 50 (`formatico.shares.per-address-per-day`), UTC gece
        yarısından gece yarısına: sayım yalnızca bellekte, ne log'a ne diske yazılıyor; tutulmayan paylaşım sayılmıyor;
        aşılınca `429`, `{"error": "daily-limit", "limit": 50}` ve `Retry-After`. Adres nginx'in `X-Real-IP`'si. Share
        penceresinde iki yeni uyarı (Share-1440'ta busy ve limit): "Formatico’s server is not taking shares right now"
        (507 ya da nginx'in 429'u, Try again ile) ve "No more encrypted links from this network today" (Try again
        yok).

*   [x] **Web'in sayfası bir Content-Security-Policy olmadan sunuluyor (2026-11-07).**
    *   Ne: `deploy/web/default.conf` `nosniff`, `no-referrer` ve `X-Frame-Options` gönderiyor, ama sayfanın neyi
        yükleyip çalıştırabileceğini söyleyen bir CSP yok. Site raporlayan (report-only) bir CSP gönderiyor.
        Vekilin verdiği belgeler kendi `sandbox` CSP'siyle, indirilecek dosya olarak geliyor (sunucunun kararı).
    *   Öneri: sayfanın yüklediklerini (Wasm, JS yedeği, yükleyicinin satır içi betiği, yazı tipleri) ölçüp önce
        raporlayan bir CSP; tarayıcı testleriyle denendikten sonra zorlayan.
    *   Durum: kapandı (2026-11-07). Zorlayan CSP, `deploy/web/headers.conf`'ta: `default-src 'none'; script-src 'self'
        'wasm-unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self'; font-src 'self'; connect-src 'self';
        manifest-src 'self'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'`. Ölçülerek: yükleyicide satır
        içi betik yok; `'unsafe-eval'` gerekmiyor (`new Function` yalnızca `globalThis` olmayan tarayıcıda); Compose
        kendi kökünde bir `<style>` yazıyor, `'unsafe-inline'` olmadan dokuz tarayıcı testinin dokuzu
        `style-src-elem`'de reddediliyor. `app/webTest` uygulamayı aynı başlıklarla sunuyor ve her sayfada
        reddedilen her şeyi hata sayıyor; Wasm, JS yedeği, Safari'nin kimliği, sekmeler, bağlantılar, yeniden yükleme,
        telefon genişliği ve Türkçe onunla geçiyor; yeni bir test sayfaya yazılan bir betiğin çalışmadığını gösteriyor.
        Aynı dosya nginx'in `/api` yerlerine de giriyor: önce `nosniff`, `Referrer-Policy` ve `X-Frame-Options` oraya
        hiç gitmiyordu (bir yerin kendi `add_header`'ı sunucununkini siliyor). Denenmedi: gerçek Safari ve Firefox.

## Metinler

*   [x] **Türkçe metinlerin gözden geçirilmesi (2026-11-06).**
    *   Ne: 632 metnin Türkçesi bir dil incelemesinden geçti. Yanlış anlamlar vardı: "Belgeyi değiştir" (switch documents) "düzenle" diye okunuyordu; "Bir değer okunamadı" "bir değer" diyordu; null "Boş" yazılıyordu. Yer tutucular yanlış dolan cümleler kurulmuştu ("…, 552 karakterde"). Terimler karışıktı: "problem" ve "sorun", "alias" ve "takma ad", "anchor" ve "çapa", "metin" ve "dizgi". Kesme işareti düz de kıvrık da yazılıyordu.
    *   Durum: kapandı (2026-11-06). 74 metin düzeltildi. Seçilen terimler: sorun, takma ad, çapa, dizgi, indis, `null`. Kesme işareti İngilizcedeki gibi kıvrık (’). "en yeni önce" yerine "yeniden eskiye"; "anlamla karşılaştır" yerine "anlama göre". Türkçe telefonda iki taşma da kalktı: dördüncü görünüm artık "Sorunlar" ("Problemler" kesiliyordu); Summary'nin boyut kutucukları ("665 bayt") sığmadığında `PantrixAutoText` ile küçülüyor. Terimler sahibinin onayına açık.

