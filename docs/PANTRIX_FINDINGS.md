# Pantrix UI Bulguları

Formatico, Pantrix UI'ı yalnızca pantrixui.com'dan, Claude Code eklentisinden ve MCP sunucusundan öğrenen bir geliştiricinin gözüyle yazılıyor. Bu dosya, o geliştiricinin takıldığı her yeri tutar: neyi aradı, sitede nerede aradı, ne eksikti, ne öneriliyor.

Bir bulgu kapanınca silinmez: işaretlenir, ne zaman ve neyle kapandığı yazılır. Kütüphanede ya da sitede yapılan düzeltme, o deponun kendi kurallarıyla (açık maddeler, değişiklik günlüğü, testler) ve sahibinin onayıyla gider.

Her madde: **ne**, **nerede** (sitenin sayfası ya da aracı), **öneri**, **durum**. "Doğrulanacak" diyen bir madde, derlemede ya da cihazda görülünce kesinleşir.

## 1. Kurulum

*   [ ] **Sihirbazdan gelen proje Material'la başlıyor; Kurulum sayfası bunu anmıyor (2026-11-02).**
    *   Ne: Android Studio'nun Kotlin Multiplatform sihirbazı `app/shared`'e `compose-material3` bağımlılığını ve `MaterialTheme`, `Button`, `Text` kullanan bir `App.kt` koyuyor. Kurulum sayfası temayı boş bir `App()` üstünde gösteriyor; sihirbazın verdiği dosyada neyin silineceğini, `MaterialTheme`'in yerine `PantrixTheme`'in konacağını, Material'ın bağımlılıktan çıkarılacağını söylemiyor. Sitenin kuralı "Material yok" diyor, ama yeni bir projenin ilk dosyası Material'la geliyor.
    *   Nerede: [Installation](https://pantrixui.com/docs/installation), "Wrap the app in the theme".
    *   Öneri: Kurulum'a "Sihirbazdan gelen bir projede" adımı: `compose-material3` satırını kaldırmak (ya da ekranları birer birer taşıyan bir uygulama için `com.pantrixui:material3` köprüsü), `App.kt`'yi `PantrixTheme` ve `PantrixButton`, `PantrixText` ile yeniden yazmak, `Greeting` ve `compose_multiplatform` örneklerini silmek.
    *   Durum: açık. Formatico'da yapıldı (2026-11-02): Material katalogdan çıktı, `App.kt` `PantrixSurface` ve `PantrixText` ile yazıldı. Sihirbazın `Greeting.kt`'si, sihirbazın sildiği bir `core` modülünü çağırdığı için proje olduğu gibi derlenmiyordu da; bu Pantrix'in değil, Formatico'nun açık maddelerinde.
*   [ ] **Sürüm kataloğu satırları yok (2026-11-02).**
    *   Ne: Sihirbaz bağımlılıkları `gradle/libs.versions.toml`'da tutuyor; Kurulum yalnızca düz koordinat veriyor (`implementation("com.pantrixui:components:0.1.1")`).
    *   Nerede: [Installation](https://pantrixui.com/docs/installation), "Add the dependency".
    *   Öneri: aynı satırlar sürüm kataloğuyla, bir sekmede: `pantrix = "0.1.1"`, `pantrix-components = { module = "com.pantrixui:components", version.ref = "pantrix" }` ve `implementation(libs.pantrix.components)`.
    *   Durum: açık. Formatico katalogda `pantrix-components`, `pantrix-testing`, `pantrix-detektRules` satırlarıyla yazdı.
*   [ ] **GitHub Packages'ın token'ı için adım yok (2026-11-02).**
    *   Ne: Sayfa, herkese açık paket için bile `read:packages` izinli bir token gerektiğini ve `~/.gradle/gradle.properties`'e yazılacağını söylüyor; token'ın nereden ve nasıl alınacağını göstermiyor. Dış geliştirici için ilk engel bu: GitHub hesabı ve klasik bir kişisel erişim token'ı.
    *   Nerede: [Installation](https://pantrixui.com/docs/installation), "Add the repository".
    *   Öneri: GitHub'ın token sayfasına bağlantı, seçilecek izin ve `gradle.properties`'in iki satırının örneği. Uzun vadede Maven Central (yol haritasında).
    *   Durum: açık.
*   [ ] **İlk örneğin `Res.string.save`'i yeni projede yok (2026-11-02).**
    *   Ne: Kurulum'un ve `llms.txt`'nin ilk örneği `stringResource(Res.string.save)` çağırıyor; yeni projede `save` diye bir metin yok ve `Res`'in import'u gösterilmiyor (sihirbazda `formatico.app.shared.generated.resources.Res`). Kopyalanan örnek derlenmez.
    *   Nerede: [Installation](https://pantrixui.com/docs/installation), "Wrap the app in the theme"; [llms.txt](https://pantrixui.com/llms.txt).
    *   Öneri: örneğin yanında `composeResources/values/strings.xml`'in satırı ve `Res`'in import'u için bir yorum.
    *   Durum: açık.
*   [ ] **Web: sihirbazın yükleyicisi Safari 27.2'de boş kalabilir; Kurulum web uyumluluğu rehberine götürmüyor (2026-11-02).**
    *   Ne: Sihirbazın `webApp`'i JS ve Wasm hedeflerini birlikte kuruyor. Web uyumluluğu rehberi, Compose'un kendi yükleyicisinin (`composeCompatibilityBrowserDistribution`) tarayıcıyı bir kez sınadığını ve WebKit 27.2'de Wasm sürümü ilk karesinden sonra durunca JS'e geçmediğini söylüyor. Kurulum'un "On each platform → Web" satırı yalnızca yazı tiplerini anıyor.
    *   Nerede: [Installation](https://pantrixui.com/docs/installation), "On each platform".
    *   Öneri: Kurulum'un Web satırına web uyumluluğu rehberinin yükleyicisine (WebKit'e JS sürümü) bir bağlantı.
    *   Durum: açık; doğrulandı (2026-11-02). Sihirbazın `index.html`'i hiçbir yükleyici açmıyordu: yalnızca `webApp.js`'i, yani Wasm derlemesini yüklüyordu; JS derlemesi ayrı bir dağıtımdı. Formatico rehberin yükleyicisini (5.2) kurdu: `webApp-wasm.js`, `webApp-js.js`, `webApp.js` yükleyici; Chrome'da Wasm derlemesi çizdi. Kurulum sayfası hâlâ rehbere götürmüyor.
*   [ ] **AGP için bir alt sınır yazmıyor (2026-11-02).**
    *   Ne: "What it needs" Kotlin'i, Compose'u ve Android API'sini söylüyor, AGP'yi ve `compileSdk`'yı söylemiyor.
    *   Nerede: [Installation](https://pantrixui.com/docs/installation), "What it needs".
    *   Öneri: kütüphanenin Android paketlerinin istediği en düşük AGP ve `compileSdk` "What it needs"e.
    *   Durum: açık. Düzeltme (2026-11-02): bu maddenin ilk hâli "sihirbaz AGP 9.1.1 verdi" diyordu; yanlıştı, sürüm kataloğu `agp = "9.5.0"` diyor. Pantrix 0.1.1, AGP 9.5.0 ve `compileSdk` 37 ile derlendi ve Pixel 7 Pro emülatöründe çalıştı; alt sınır hâlâ sitede yok.
*   [ ] **Kurulum'un marka örneği API'de olmayan adlar kullanıyor (2026-11-02).**
    *   Ne: "Give it your brand"in `PantrixPreset` örneği `PantrixDark` ve `PantrixLight` diyor; API'de böyle adlar yok (`search_api` de bulmuyor). Doğrusu `PantrixColors.Dark` ve `PantrixColors.Light`, `com.pantrixui.foundation.token.color.Dark` ve `.Light` diye içe aktarılarak; becerinin `references/theming.md`'si doğru biçimi veriyor. Örnekte hiç `import` yok, oysa kural 7 "içe aktarmaları belgelerden al" diyor.
    *   Nerede: [Installation](https://pantrixui.com/docs/installation), "Give it your brand".
    *   Öneri: örneği `theming.md`'deki `Harbor` gibi içe aktarmalarıyla ve var olan adlarla yazmak; site örnekleri derlenen örneklerden üretiliyorsa bu sayfanınki de.
    *   Durum: açık.
*   [ ] **Test bağımlılıkları iki yerde iki türlü (2026-11-02).**
    *   Ne: Kurulum sayfası `commonTest`'e yalnızca `com.pantrixui:testing`'i koyuyor. Becerinin `references/setup.md`'si `kotlin("test")`'i ve `jvmTest`'e `compose.desktop.currentOs`'u da ekliyor ("runComposeUiTest masaüstünde çizer"); o olmadan masaüstündeki UI testi çizemez.
    *   Nerede: [Installation](https://pantrixui.com/docs/installation), "Add the dependency"; beceri, `references/setup.md`, "Dependencies".
    *   Öneri: iki yerde aynı satırlar. (Bir convention eklentisinde `ComposePlugin.DesktopDependencies.currentOs` Compose 1.12.2'de kullanımdan kalkmış; `ComposeExtension`'ın `dependencies.desktop.currentOs`'u değil. Kotlin sınıfıyla yazan biri için bir cümle işe yarar.)
    *   Durum: açık.
*   [ ] **Test örnekleri kullanımdan kalkmış `runComposeUiTest`'i içe aktarıyor (2026-11-02).**
    *   Ne: Becerinin `references/testing.md`'si `androidx.compose.ui.test.runComposeUiTest`'i içe aktarıyor. Compose Multiplatform 1.12.2'de bu kullanımdan kalkmış: "Use `androidx.compose.ui.test.v2.runComposeUiTest` instead" (v2 `StandardTestDispatcher` kullanıyor). Formatico'nun testi v2 ile Pantrix'in teması altında geçti.
    *   Nerede: beceri, `references/testing.md`; [Guide to Testing a Screen](https://pantrixui.com/docs/guides/testing).
    *   Öneri: örneklerde `v2`; `onEachPlatform` ve `assertNoFindings` v2'nin `ComposeUiTest`'iyle çalışıyorsa bunu söylemek.
    *   Durum: açık.

## 2. Tasarım sistemi (Claude Design)

*   [x] **"Pantrix UI" tasarım sistemi `tokens.css` sunmuyor, bileşenleri ise onun değişkenlerini okuyor (2026-11-02).**
    *   Ne: Sistemin README'si bileşenlerin `tokens.css` ve `components/bundle.css` ile çizildiğini söylüyor; `bundle.css` `--primary-color`, `--surface`, `--space-md` gibi değişkenleri okuyor. Ama sistemin dosyaları arasında `tokens.css` yok; tasarım tuvaline kurulan bileşenler o değişkenler olmadan renksiz ve aralıksız çizilir. Formatico'nun tuvali değişkenleri kendi dosyasında (`formatico-options.css`) `tokens.json`'dan yazarak çözdü.
    *   Nerede: Claude Design'daki "Pantrix UI" tasarım sistemi, `project/README.md`'nin son bölümü.
    *   Öneri: sistemde değişkenleri açık ve koyu tema için veren bir stil dosyası; ya da README'de bir tuvalin değişkenleri nereden alacağı.
    *   Durum: kapandı (2026-11-02). Sisteme `project/tokens.css` eklendi (sürüm 8): `tokens.json`'dan derlenmiş, açık tema kökte, koyu tema `data-theme="dark"` taşıyan her öğede; README'de bir cümle bunu söylüyor. Formatico'nun tuvali artık değişkenleri oradan okuyor; kendi dosyasında yalnızca markası, yazı tipleri ve sözdizimi renkleri kaldı. Not: Design System türü bu dosyayı yalnızca sayfada bir değişiklik kaydedilince yeniden yazıyor; sistem koddan yeniden eşitlenirse `tokens.css` de yeniden yazılmalı.
*   [ ] **Düzenlenebilir bir kod editörü yok (2026-11-02).**
    *   Ne: Formatico'nun ana yüzeyi satır numaralı, renklendirilmiş, hata işaretli bir editör. Tasarım sisteminde ve kütüphanede salt okunur `CodeBlock` ile `JsonView` var; editör yok. Panolarda editör, bileşen değil işaretleme olarak çiziliyor.
    *   Nerede: kütüphanenin bileşenleri; tasarım sisteminin `Data` bölümü.
    *   Öneri: kütüphaneye `PantrixCodeEditor`, kendi tasarım önerisiyle (sahibinin kararı, 2026-11-02); tasarım sistemine web karşılığı.
    *   Durum: açık, öneri bekliyor. Workspace panolarında (1440, 834) editör yine işaretleme; tasarım sisteminin 2026-11-02'deki sürümünde (`1793640513-8bb4`) de `CodeEditor` yok. Invalid, Table ve Query panoları (2026-11-02) editörden beklenenleri gösteriyor: imlecin satırı ve seçim, bir problemin dalgalı alt çizgisi ve oluktaki işareti, imlecin altında düğmeli bir problem kartı, bir aramanın bütün eşleşmeleri ve şimdiki eşleşme, XML'in kendi renkleri (öğe, öznitelik, değer).
*   [ ] **Tasarım sistemindeki DataTable, Compose'dakinin gerisinde (2026-11-02).**
    *   Ne: Compose'un `PantrixDataTable`'ı `onRowClick`, `rowTone` (bir satırı bir rolün yumuşak dolgusuna yatırır), tek seçim, sabitlenen sütunlar, `toolbar` ve `footer` yuvaları ve yükseklik verilince yalnızca görünen satırları kuran bir kaydırma sunuyor. Tasarım sistemindeki `DataTable`'da bunların hiçbiri yok: `columns`, `rows`, `sort`, onay kutulu seçim, `striped`, `bordered`, `renderDetail`. Formatico'nun Table panolarında metindeki imlecin satırı (`$[2]`) `rowTone` ile işaretlenebilirdi; tuvalde işaretlenemedi, filtre ve sayfalama tablonun dışına kondu.
    *   Nerede: tasarım sistemi, `project/components/index.d.ts` (`DataTableProps`); MCP `get_api PantrixDataTable`.
    *   Öneri: web karşılığına `rowTone`, `onRowClick`, `toolbar`, `footer` ve sabitlenen sütunlar; tasarım sisteminin bileşenleri Compose API'sinden üretiliyorsa ayrılığı gösteren bir denetim.
    *   Durum: açık.
*   [ ] **Simgeler kapalı bir liste; bir uygulamanın sık simgeleri yok (2026-11-02).**
    *   Ne: Tasarım sistemindeki bileşenlerin `icon` özelliği `IconName` türünde, PantrixIcons'ın 35 adından biri; uygulamanın kendi simgesi verilemiyor. Formatico'nun panolarında ayarlar, tema (ay), yapıştır (pano), dış bağlantı ve resim simgeleri yok; bu yüzden o düğmeler `Pantrix.IconButton` değil, elle çizilmiş SVG'li işaretleme. Compose tarafında sorun yok: `PantrixIconButton` her `ImageVector`'ü alıyor. README "uygulamanın simgeleri ızgarayı ve çizgiyi paylaşır" diyor, ama tasarım tuvalinde paylaşmanın yolu yok.
    *   Nerede: tasarım sistemi, `project/components/index.d.ts` (`IconName`), `project/assets/Icons/README.md`.
    *   Öneri: web karşılıklarında `icon`'a bir SVG ya da bir düğüm de verilebilmesi; PantrixIcons'a sık kullanılanlar: ayarlar, güneş ve ay, pano, dış bağlantı, resim, geçmiş, çöp kutusu, paylaş.
    *   Durum: açık.

## 3. Detekt

*   [ ] **Kuralların hangi detekt'e yazıldığı söylenmiyor (2026-11-02).**
    *   Ne: Kurulum ve Detekt rehberi `detektPlugins("com.pantrixui:detekt-rules:0.1.1")` diyor; detekt eklentisinin kimliğini ve sürümünü söylemiyor. Yayımlanan jar detekt 1.x API'sine (`io.gitlab.arturbosch.detekt.api`) yazılmış, yani `io.gitlab.arturbosch.detekt` 1.23.x ister; 2.0'ın `dev.detekt` API'siyle yüklenmez. Bunu bulmak için jar'ın sınıflarına bakmak gerekti. Kuralların POM'u `kotlin-stdlib 2.4.21`'i derleme bağımlılığı olarak getiriyor; detekt 1.23.8 kendi Kotlin'ini (2.0.21) `strictly` ile tutuyor ve ikisi yan yana çalıştı.
    *   Nerede: [Installation](https://pantrixui.com/docs/installation), "The other modules"; [Detekt Guide](https://pantrixui.com/docs/guides/detekt), bölüm 5.
    *   Öneri: rehberin 5. bölümüne `plugins { id("io.gitlab.arturbosch.detekt") version "1.23.8" }` satırı; uzun vadede detekt 2'ye taşınmak.
    *   Durum: açık. Formatico detekt 1.23.8 ile kurdu; kurallar yükleniyor, `NoMaterialImport` bir deneme dosyasında tuttu. Not: ilk kurulumda "Kotlin 2.4'le detekt'in Kotlin'ini sabitlemek gerekir" sanılıp sabitleme yazılmıştı; sabitlemesiz bir koşu gereksiz olduğunu gösterdi ve kaldırıldı.
*   [ ] **Çok platformlu bir uygulamada detekt'in neye bakacağı gösterilmiyor (2026-11-02).**
    *   Ne: Rehber, KMP'de düz `detekt` görevinin "hiçbir şeye bakmadan yeşil geçeceğini", kütüphanenin bunu kendi `build-logic`'inde bağladığını söylüyor; bir uygulamanın nasıl bağlayacağını göstermiyor.
    *   Nerede: [Detekt Guide](https://pantrixui.com/docs/guides/detekt), bölüm 2.
    *   Öneri: bölüm 5'e bir uygulamanın `source.setFrom("src")` (ya da kaynak kümelerine göre görevler) satırları.
    *   Durum: açık.
*   [ ] **Rehberin bağlantıları kapalı bir depoya gidiyor (2026-11-02).**
    *   Ne: `config/detekt/detekt.yml` ve `demo.yml` bağlantıları `github.com/develappsancho/pantrix-ui-cmp`'ye gidiyor; depo özel, dışarıdan 404. Rehber ayrıca `build-logic`'teki `DetektSetupPlugin`'i ve `pantrix.demo.detekt`'i anıyor; dışarıdan görülemiyorlar.
    *   Nerede: [Detekt Guide](https://pantrixui.com/docs/guides/detekt), bölüm 2.
    *   Öneri: demonun yapılandırması (bir uygulamanın tutacağı) rehberin içinde ya da paket deposunda; kütüphanenin iç dosyaları anılmadan.
    *   Durum: açık.

## 4. Belgeler ve yapay zekâ araçları

*   [ ] **API başvurusu sitenin rehberleri yerine deponun yollarını gösteriyor (2026-11-02).**
    *   Ne: `foundation`'ın API'sinde 9, `components`'ınkinde 3 yerde `docs/guide/system-bars/SYSTEM_BARS_GUIDE.md`, `docs/guide/date-time/DATE_TIME_GUIDE.md` gibi deponun yolları yazıyor (`PantrixTheme`'in `followSystemBars`'ı, `PantrixDate`, `Modifier.scrollPadding`…). Dışarıdan bu yolların karşılığı sitenin `/docs/guides/…` sayfaları; yol bağlantı da değil.
    *   Nerede: [llms/api/foundation.md](https://pantrixui.com/llms/api/foundation.md), [llms/api/components.md](https://pantrixui.com/llms/api/components.md); MCP'nin `get_api`'si aynı metni veriyor.
    *   Öneri: KDoc'tan belge üretilirken depo yollarını sitenin rehber adresine çevirmek.
    *   Durum: açık.
*   [ ] **"For AI" sayfası 7 kural diyor, beceri ve llms.txt 11 (2026-11-02).**
    *   Ne: Sayfa "7 rules the skill keeps" diyor ve yedisini sayıyor; `SKILL.md` ile `llms.txt` 11 temel kural veriyor (belgelerden parametre, kaynaklardan sözcük, cihaz değil pencere, testler Pantrix'in yardımcılarıyla).
    *   Nerede: [For AI](https://pantrixui.com/ai), "The rules it keeps".
    *   Öneri: sayfanın kural listesini becerinin listesinden üretmek.
    *   Durum: açık.
*   [ ] **Eklentiyi ekipçe kurmanın yolu ve oturumun yenilenmesi söylenmiyor (2026-11-02).**
    *   Ne: Sayfa `claude plugin install pantrix-ui@pantrix-ui`'yi veriyor; bu kullanıcı kapsamına kurar. Beceri zip'i için "ekip git'le alsın" diyor, eklenti için `--scope project` demiyor (Formatico öyle kurdu: `.claude/settings.json`). "Then /mcp lists pantrix-ui as connected" diyor; eklenti ancak yeni bir oturumda yükleniyor: kurulduğu oturumda araçlar görünmedi, sunucu `https://pantrixui.com/mcp` adresinden doğrudan soruldu.
    *   Nerede: [For AI](https://pantrixui.com/ai), "Set it up in a minute".
    *   Öneri: `--scope project` satırı ve "yeni bir oturum açın" cümlesi.
    *   Durum: açık.

## 5. Web

*   [ ] **Yol silen webpack eklentisi rehberde yok (2026-11-02).**
    *   Ne: Web uyumluluğu rehberi, Skia betiğindeki `import.meta.url`'nin derleyen makinenin yolunu (`file:///Users/…`) yayına taşıdığını ve bunun `webpack.config.d/stripFilePaths.js` ile silindiğini söylüyor; eklentinin kendisini vermiyor. Dışarıdan bir uygulama aynısını yapamıyor.
    *   Nerede: [Old Browsers and a Wasm That Will Not Start](https://pantrixui.com/docs/guides/web-compatibility), bölüm 7 ve 9.
    *   Öneri: eklentinin metni rehberde, ya da paket deposunda indirilebilir.
    *   Durum: açık.

## 6. Bileşenler

*   [ ] **Belge sekmeleri için kapatılabilen bir sekme yok (2026-11-02).**
    *   Ne: Workspace açık belgeleri sekmelerde tutuyor: her sekmenin bir kapatma düğmesi ve kaydedilmemiş değişiklik işareti var. `PantrixTabs`'ın `TabItem`'ı etiket, simge, rozet, `enabled` ve `tagKey` alıyor; kapatma ya da sekmenin sonuna bir düğme koyma yolu yok. Panolarda belge sekmeleri işaretleme.
    *   Nerede: [Tabs](https://pantrixui.com/components/tabs); MCP `get_api TabItem`.
    *   Öneri: `TabItem`'a `onClose` (ve okuyucuya "Kapat {label}") ya da sekmenin sonu için bir yuva; kaydedilmemiş işareti için `TabBadge`'in noktası yeterli olabilir.
    *   Durum: açık.
*   [ ] **TreeView satırının sonunda yalnızca bir metin var; büyük ağaçta her açık satır oluşturuluyor (2026-11-02).**
    *   Ne: Formatico'nun ağacı değerleri önizliyor: tarih bir etiketle, renk bir örnekle, adres bir bağlantı simgesiyle, resim bir küçük resimle. `TreeNode.trailing` yalnızca `String`; satıra bir öğe konamıyor. Ayrıca `PantrixTreeView` kaydırmıyor ve her açık satırı oluşturuyor ("binlerce açık satır kendi listesini ister"); masaüstünde 50 MB'lık belge hedefi için yetmez. Panolarda ağaç işaretleme.
    *   Nerede: [Tree View](https://pantrixui.com/components/tree-view); MCP `get_api PantrixTreeView`, `TreeNode`.
    *   Öneri: satırın sonu için bir `@Composable` yuva; tembel bir liste üstünde çalışan bir biçim (ya da `PantrixTreeView`'ın `LazyColumn` içinde yaşayan bir kardeşi).
    *   Durum: açık.
*   [ ] **JsonView seçili bir yolu tutmuyor; metinle birlikte yürüyemiyor (2026-11-02).**
    *   Ne: Formatico'da metindeki imleç ağaçta bir değeri seçiyor, ağaçta seçilen değer metinde imleci taşıyor. `PantrixJsonView` açık yolları (`expanded`) dışarıya veriyor ama seçili bir yol, `onSelect` ya da "bu yola kaydır" yok. JSON'u kendisi bir `String`'den okuyor ve yalnızca RFC 8259 JSON'u alıyor: JSONC, JSON5, YAML, TOML ağaçları için kullanılamıyor, Formatico'nun kendi okuyucusunun ağacını da alamıyor.
    *   Nerede: [JSON View](https://pantrixui.com/components/json-view); MCP `get_api PantrixJsonView`.
    *   Öneri: düğümlerle beslenen bir belge ağacı (anahtar, değer türü, yol), seçili yol ve `onSelect`, yola kaydırma; değer önizlemesi için satır yuvası.
    *   Durum: açık.
*   [ ] **Kısayolun tuşlarını çizen bir öğe yok (2026-11-02).**
    *   Ne: `PantrixShortcut.keysOn` bir kısayolu tuş tuş veriyor (`⌘`, `K`), ama tuşları çizen bir bileşen yok; tasarım sisteminde de yok. Formatico'nun Start ve Workspace panolarındaki `⌘K` ve `⌘V` işaretleme.
    *   Nerede: [Keyboard shortcuts](https://pantrixui.com/docs/guides/keyboard-shortcuts); tasarım sisteminin bileşen listesi.
    *   Öneri: `CommandPalette`'in kısayol yazımını kullanan küçük bir `PantrixKeys` (ya da `PantrixShortcutLabel`).
    *   Durum: açık.
