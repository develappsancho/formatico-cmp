# Pantrix UI Bulguları

Formatico, Pantrix UI'ı yalnızca pantrixui.com'dan, Claude Code eklentisinden ve MCP sunucusundan öğrenen bir geliştiricinin gözüyle yazılıyor. Bu dosya, o geliştiricinin takıldığı her yeri tutar: neyi aradı, sitede nerede aradı, ne eksikti, ne öneriliyor.

Bir bulgu kapanınca silinmez: işaretlenir, ne zaman ve neyle kapandığı yazılır. Kütüphanede ya da sitede yapılan düzeltme, o deponun kendi kurallarıyla (açık maddeler, değişiklik günlüğü, testler) ve sahibinin onayıyla gider.

Her madde: **ne**, **nerede** (sitenin sayfası ya da aracı), **öneri**, **durum**. "Doğrulanacak" diyen bir madde, derlemede ya da cihazda görülünce kesinleşir.

Formatico'nun işini tıkayan bir maddenin durumu `tıkıyor` der (`Durum: açık, tıkıyor.`): kütüphanede önce o düzeltilir, yerel paket yenilenir. Formatico'da onun için yazılan geçici bir çözüm, yorumunda maddenin başlığını anar ve düzeltme gelince sökülür.

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
    *   Durum: Compose'da kapandı (0.1.2-SNAPSHOT, `PantrixCodeEditor`, 2026-11-03); tasarım sisteminde (`1793640513-8bb4`, 2026-11-04'te bakıldı) hâlâ `CodeEditor` yok, panolarda editör işaretleme kalıyor. açık, öneri bekliyor. Workspace panolarında (1440, 834) editör yine işaretleme; tasarım sisteminin 2026-11-02'deki sürümünde (`1793640513-8bb4`) de `CodeEditor` yok. Invalid, Table ve Query panoları (2026-11-02) editörden beklenenleri gösteriyor: imlecin satırı ve seçim, bir problemin dalgalı alt çizgisi ve oluktaki işareti, imlecin altında düğmeli bir problem kartı, bir aramanın bütün eşleşmeleri ve şimdiki eşleşme, XML'in kendi renkleri (öğe, öznitelik, değer). Table · 1440 (yeniden çizildi, 2026-11-02) bir şey daha istiyor: tabloda seçili kaydın satırlarını oluktaki bir bantla işaretlemek. Kodda (2026-11-03) Workspace'in metin bölmesi, sahibinin kararıyla, editörü bekleyen boş bir hâl: tuvalde Workspace panolarının "state" ayarında `waiting`.
*   [ ] **Tasarım sistemindeki DataTable, Compose'dakinin gerisinde (2026-11-02).**
    *   Ne: Compose'un `PantrixDataTable`'ı `onRowClick`, `rowTone` (bir satırı bir rolün yumuşak dolgusuna yatırır), tek seçim, sabitlenen sütunlar, `toolbar` ve `footer` yuvaları ve yükseklik verilince yalnızca görünen satırları kuran bir kaydırma sunuyor. Tasarım sistemindeki `DataTable`'da bunların hiçbiri yok: `columns`, `rows`, `sort`, onay kutulu seçim, `striped`, `bordered`, `renderDetail`. Formatico'nun Table panolarında metindeki imlecin satırı (`$[2]`) `rowTone` ile işaretlenebilirdi; tuvalde işaretlenemedi, filtre ve sayfalama tablonun dışına kondu.
    *   Ne (ek, 2026-11-02): Table panoları yeniden çizilince eksik büyüdü. Compose'da bir sütunun `cell`'i bir `@Composable`: bir değer türüne göre yazılır (sayı, `true`/`false`, `null`, olmayan anahtar), iç içe bir dizi açılan bir düğme olur. Tasarım sistemindeki sütunun `render`'ı ise bir JavaScript işlevi; tuvaldeki bir pano işaretlemeden ibaret olduğu için ona öğe veremiyor. Bunlar da yok: `DataTableVariant.Grid` (sütunların arasında çizgi), genişliği sürüklenen sütunlar, `headerTextStyle` ve `DataTableColors.headerContainer`, yükseklik verilen tabloda yerinde kalan başlık, dar ekranda kayıtların listesi (`DataTableLayout.Auto`), listenin üstünde sıralama şeridi ve `listContent`. Table panoları (1440, 834, 390) bu yüzden tabloyu bileşenle değil işaretlemeyle çiziyor; kodda karşılığı `PantrixDataTable`.
    *   Nerede: tasarım sistemi, `project/components/index.d.ts` (`DataTableProps`); MCP `get_api PantrixDataTable`, `DataTableColumn`, `DataTableStyle`, `DataTableColors`.
    *   Öneri: web karşılığına `rowTone`, `onRowClick`, `toolbar`, `footer`, sabitlenen ve sürüklenen sütunlar, `Grid`, başlığın yazısı ve dolgusu, dar ekranın listesi; bir hücreye işaretlemeden içerik verilebilmesi (örneğin sütun başına bir yuva adı); tasarım sisteminin bileşenleri Compose API'sinden üretiliyorsa ayrılığı gösteren bir denetim.
    *   Durum: açık.
*   [ ] **Tasarım sistemindeki CodeBlock da Compose'dakinin gerisinde (2026-11-02).**
    *   Ne: Compose'un `PantrixCodeBlock`'ı satır satır `CodeLine` alıyor; her satıra bir `CodeMark` (bir rolün tonunda bant, başında çubuk, altında bir ileti ve kaynağı) ve `CodeBlockColors` verilebiliyor. Bu, Formatico'nun salt okunur görünümlerinin çoğunu karşılıyor: dönüşümün çıktısı, karşılaştırmanın eklenen, çıkan ve değişen satırları, bir problemin satırın altındaki iletisi. Tasarım sistemindeki `CodeBlock` yalnızca `code`, `tokens`, `title`, `markedLines`, `lineNumbers` ve `copyable` alıyor; Convert ve Compare panolarındaki kod bu yüzden işaretleme.
    *   Nerede: tasarım sistemi, `project/components/index.d.ts` (`CodeBlockProps`); MCP `get_api CodeLine`, `CodeMark`.
    *   Öneri: web karşılığına satır başına işaret (ton, ileti, kaynak) ve renkler.
    *   Durum: açık.
*   [ ] **Simgeler kapalı bir liste; bir uygulamanın sık simgeleri yok (2026-11-02).**
    *   Ne: Tasarım sistemindeki bileşenlerin `icon` özelliği `IconName` türünde, PantrixIcons'ın 35 adından biri; uygulamanın kendi simgesi verilemiyor. Formatico'nun panolarında ayarlar, tema (ay), yapıştır (pano), dış bağlantı ve resim simgeleri yok; bu yüzden o düğmeler `Pantrix.IconButton` değil, elle çizilmiş SVG'li işaretleme. Compose tarafında sorun yok: `PantrixIconButton` her `ImageVector`'ü alıyor. README "uygulamanın simgeleri ızgarayı ve çizgiyi paylaşır" diyor, ama tasarım tuvalinde paylaşmanın yolu yok.
    *   Nerede: tasarım sistemi, `project/components/index.d.ts` (`IconName`), `project/assets/Icons/README.md`.
    *   Öneri: web karşılıklarında `icon`'a bir SVG ya da bir düğüm de verilebilmesi; PantrixIcons'a sık kullanılanlar: ayarlar, güneş ve ay, pano, dış bağlantı, resim, geçmiş, çöp kutusu, paylaş.
    *   Durum: açık.
*   [ ] **Menu ve Popover tuvalde açık çizilemiyor (2026-11-03).**
    *   Ne: Workspace ve Table panolarına iki menü çizildi. Biri seçili değerin yolunu JSONPath, JSON Pointer ya da jq olarak kopyalıyor, öteki tabloyu kaydediyor ya da kopyalıyor. İkisi de açık gösterilmek istendi. Tasarım sistemindeki `Menu`'nün ve `Popover`'ın `anchor`'u bir React düğümü ya da düğüm döndüren bir işlev. Panodaki `x-import` bir bileşene yalnızca `renderVals()`'tan değer verebiliyor, düğüm veremiyor; öğenin içeriği de `anchor`'a değil `children`'a gidiyor. Masaüstündeki menüler bu yüzden işaretleme: yüzey, ayraç ve gölge belirteçleri, satırlar `role="menuitem"`. Telefonda aynı seçimler `Sheet` ile (`visible`, `contained`) gerçek bileşenle çizildi.
    *   Nerede: tasarım sistemi, `project/components/index.d.ts` (`MenuProps`, `PopoverProps`); tasarım türünün `design-system-components.md`'si ("An overlay shown open").
    *   Öneri: `Menu`'ye ve `Popover`'a çapasız açık çizilebilen bir biçim. Örneğin tetikleyiciyi `children` olarak almak ya da `visible` ile yerinde çizilen bir hâl. README'de bir panoda açık menünün nasıl gösterileceğini söyleyen bir cümle.
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
*   [ ] **Test rehberi tarayıcıda UI testinin iki şartını söylemiyor (2026-11-03).**
    *   Ne: Rehber "The common tests run on every target: jvmTest, iosSimulatorArm64Test, jsBrowserTest, wasmJsBrowserTest" diyor. Formatico'da `runComposeUiTest`'li ortak testler tarayıcıda iki yerde takıldı: Compose, Wasm hedefi `binaries.executable()` demedikçe UI testini hiç çalıştırmıyor (`checkComposeUiTestConfigurationForWasmJs`, CMP-4906); çalıştırınca da uygulamanın kendi kaynaklarından gelen metinler (`stringResource`) test boyunca hiç görünmüyor. `PantrixTestTheme` ve Pantrix'in kendi metinleri bunu yaşamıyor, çünkü onlar kaynaktan okunmuyor. Bu Compose'un davranışı, ama rehber bir uygulamanın ekranlarının tarayıcıda öyle sınanacağını söylüyor.
    *   Nerede: `skill/testing` ("Running"); MCP `search_documentation testing`.
    *   Öneri: rehberde `binaries.executable()` şartı ve uygulama kaynaklı metinlerin tarayıcıda nasıl beklenip yükleneceği; olmuyorsa tarayıcı testlerinin etiketle sınanması gerektiği.
    *   Durum: açık. Formatico'da çözüldü (2026-11-03), rehbere girecek olan bu: metin `waitUntil` ile değil, askıya alınan bir beklemeyle beklenmeli (`withContext(Dispatchers.Default) { delay(…) }` ve `awaitIdle()`), çünkü `waitUntil` tarayıcının olay döngüsünü tutuyor ve kaynakların `fetch`'i bitmiyor; Karma test paketinin `kotlin/composeResources/`'unu sunmalı ve `/composeResources/`'u oraya yönlendirmeli; Mocha'nın 2 saniyesi yetmiyor. Formatico'nun 161 testi böyle Wasm'da koşuyor.
*   [ ] **Test temasına verilen pencere, testin çizdiği boy değil (2026-11-03).**
    *   Ne: `PantrixTestTheme(window = PantrixWindow(DpSize(1440.dp, 900.dp)))` ekrana 1440'lık bir pencerede olduğunu söylüyor, ama masaüstünde `runComposeUiTest` ekranı kendi 1024×768 piksellik yüzeyinde çiziyor. Formatico'nun geniş Table testi bu yüzden 1024'te koştu: araç çubuğu sığmadı, sondaki "Save as CSV" düğmesi sıfır genişlikte kaldı ve tıklama hiçbir şey yapmadı. Test ekranın kendisi değil, düzenin pencereden küçük olduğu bir hâli sınıyordu; hata bulunana kadar "düğme çalışmıyor" gibi göründü. `window`'un belgesi "the window the screen is told it is in" diyor, yani söylenen bu; ama test rehberi, ekranı o boyda çizmek için `PantrixWindowFrame`'in boyutlu bir kutuyla kullanılacağını söylemiyor (yalnızca uyarlanabilir rehberde, katlanan pencere örneğinde geçiyor).
    *   Nerede: MCP `get_api PantrixTestTheme` (`window`); `guide/testing`; `guide/adaptive` ("A folding window in a preview and a test").
    *   Öneri: test rehberine "bir genişlikteki düzeni sınamak için ekranı o boyda bir `PantrixWindowFrame`'e koyun" cümlesi ve örneği; ya da `PantrixTestTheme`'in `window` verildiğinde içeriği o boyda çizmesi.
    *   Durum: açık. Formatico (2026-11-03) araç çubuğunu dar genişlikte de sığacak biçimde düzeltti (yol izi önce daralıyor); testler 1024'te koşmaya devam ediyor.

*   [ ] **Ekran görüntüsü testinin kurulumu sitede yok; Maestro görevleri yayımlanmıyor (2026-11-03).**
    *   Ne: Test rehberi Roborazzi için "Pantrix's own setup is in the Roborazzi guide" diyor ve deponun bir dosyasına bağlıyor; dışarıdan biri o depoyu okuyamıyor, sitede ve MCP'de o rehber yok. Kurulum özetin üç maddesinden çıkarıldı (eklenti, `roborazzi-compose-desktop`, `captureRoboImage`, `runDesktopComposeUiTest(width, height)`). Maestro'da rehber `maestroTest` ve `maestroIosTest` görevlerini anlatıyor; görevler Pantrix'in kendi `build-logic`'inde (`pantrix.demo.maestro`), bir uygulamaya verilmiyor. Formatico akışlarını `maestro test` ile koşturuyor.
    *   Nerede: MCP `get_documentation_page guide/testing`, bölüm 3.6 ve 3.7.
    *   Öneri: Roborazzi rehberi sitede ve MCP'de; Maestro görevleri yayımlanan bir Gradle eklentisi ya da rehberde kopyalanabilir bir görev.
    *   Durum: açık.
*   [ ] **Bir ekranın önizlemesi nasıl yazılır, belgelerde yok (2026-11-04).**
    *   Ne: Formatico her ekranı ve bileşeni `@Preview` ile, her hâlini sayan bir `PreviewParameterProvider`'la, panoların genişliklerinde, açık ve koyu temada önizliyor. Belgelerde önizlemeye dair bir rehber yok: hangi temanın içinde çizileceği, bir önizlemede masaüstünün ya da telefonun platformunun nasıl verileceği, saatin nasıl durdurulacağı yazmıyor. Sayfalar yalnızca geçerken anıyor ("a preview or a test gives…": `LocalPantrixClock`, `LocalFilePicker`, `PantrixWindowFrame`). `PantrixTestTheme` tam bunu yapıyor (platform, pencere, koyu tema, saat), ama `com.pantrixui:testing`'de: uygulamanın `commonMain`'indeki bir önizleme onu kullanamaz. Test rehberi ekran görüntüsünü bilerek uygulamaya bırakıyor ("There is no helper in the module for screenshot tests").
    *   Nerede: MCP `search_documentation "preview"`, `search_api "preview"`; `guide/testing`.
    *   Öneri: `guide/previews`: bir ekranın önizlemesi uygulamanın temasında, bir `PreviewParameterProvider`'la hâlleri, `@Preview(widthDp, heightDp)` ile pencereleri; ve `foundation`'da `PantrixTestTheme`'in önizlemeye uyan bir eşi (`PantrixPreviewTheme(platform, dark, window, clock)`), aynı hâller hem önizlemede hem ekran görüntüsünde çizilsin diye.
    *   Durum: açık. Formatico kendisi yazdı (`core:designsystem`'de `FormaticoPreview`, `PreviewCases`, `BoardPreviews`, `Board`): önizleme Formatico'nun temasında, saat sabit; ekran görüntüsü aynı önizlemeyi `PantrixTestTheme`'in içinde çiziyor.
*   [ ] **Bir bileşen, içinde durduğu temanın koyu olup olmadığını okuyamıyor (2026-11-04).**
    *   Ne: `PantrixTheme`'in koyu temayı dıştaki temadan alan varsayılanı `inheritedDarkThemeOrSystem()`; API'de yok, uygulama çağıramıyor. Formatico'nun teması (`FormaticoTheme`) kendi sözdizimi renklerini koyu ya da açık seçiyor; dışında koyu bir tema varken onu bilemiyor, `isSystemInDarkTheme()`'e bakıyor. Bir testte `PantrixTestTheme(dark = true)` içine konan `FormaticoTheme()` sistemin moduna göre açık çiziliyor; önizlemede de aynı.
    *   Nerede: MCP `get_api com.pantrixui.foundation.theme.PantrixTheme` (`darkTheme`'in `defaultNote`'u), `search_api "isDark"`.
    *   Öneri: `inheritedDarkThemeOrSystem()`'i açmak ya da `PantrixTheme.isDark` (renklerin koyu takımdan olup olmadığı).
    *   Durum: açık. Formatico'nun önizleme hâlleri koyu ya da açık olduklarını kendileri taşıyor ve temayı açıkça veriyor.

## 5. Web

*   [ ] **Yol silen webpack eklentisi rehberde yok (2026-11-02).**
    *   Ne: Web uyumluluğu rehberi, Skia betiğindeki `import.meta.url`'nin derleyen makinenin yolunu (`file:///Users/…`) yayına taşıdığını ve bunun `webpack.config.d/stripFilePaths.js` ile silindiğini söylüyor; eklentinin kendisini vermiyor. Dışarıdan bir uygulama aynısını yapamıyor.
    *   Nerede: [Old Browsers and a Wasm That Will Not Start](https://pantrixui.com/docs/guides/web-compatibility), bölüm 7 ve 9.
    *   Öneri: eklentinin metni rehberde, ya da paket deposunda indirilebilir.
    *   Durum: açık.

*   [ ] **Teşhisler, web'de kelimeler yüklenirken adı boş satırları bildiriyor (MissingName, kelimeler yüklenirken) (2026-11-03).**
    *   Ne: Tarayıcıda uygulamanın kendi kelimeleri (Compose kaynakları) ilk kareden sonra geliyor. O arada `PantrixListItem`'ın başlığı boş; teşhisler bunu hemen `MissingName` diye bildiriyor ("ListItem has a blank name"). Kelimeler birkaç kare sonra geliyor, satırın adı oluyor, bulgu ise kalıyor ve `assertNoFindings` başarısız. Sıkışan dokunma alanı yarım saniye, iki düğümdeki etiket bir buçuk saniye bekleniyor; boş ad hiç beklenmiyor. Masaüstünde ve iOS'ta kelimeler ilk karede olduğu için görülmüyor.
    *   Nerede: MCP `guide/diagnostics`, `get_api checkName`; `guide/testing` ("Some findings come late").
    *   Öneri: `MissingName` da kısa bir süre beklesin, ya da bir ad yalnızca boş kalmaya devam ederse bildirilsin.
    *   Durum: açık. Formatico'nun testleri (2026-11-03) kelimeleri beklemek zorunda kaldıklarında o arada bulunanı `PantrixFindings.clear()` ile unutuyor; yorumda bu bulgu anılıyor.

## 6. Bileşenler

*   [ ] **Belge sekmeleri için kapatılabilen bir sekme yok (2026-11-02).**
    *   Ne: Workspace açık belgeleri sekmelerde tutuyor: her sekmenin bir kapatma düğmesi ve kaydedilmemiş değişiklik işareti var. `PantrixTabs`'ın `TabItem`'ı etiket, simge, rozet, `enabled` ve `tagKey` alıyor; kapatma ya da sekmenin sonuna bir düğme koyma yolu yok. Panolarda belge sekmeleri işaretleme.
    *   Nerede: [Tabs](https://pantrixui.com/components/tabs); MCP `get_api TabItem`.
    *   Öneri: `TabItem`'a `onClose` (ve okuyucuya "Kapat {label}") ya da sekmenin sonu için bir yuva; kaydedilmemiş işareti için `TabBadge`'in noktası yeterli olabilir.
    *   Durum: açık. Kodda (2026-11-03) üst çubuk tek sekmeyi `PantrixTabs` ile çiziyor, kapatma düğmesi yok. `PantrixTabs` kendisine verilen genişliği dolduruyor; sekmelerin hemen ardından gelecek "+" düğmesi için bir yuva da yok, düğme şimdilik çizilmiyor. Workspace'le (2026-11-03) sekmeler açık belgeler oldu: kaydedilmemiş değişiklik `TabBadge`'in noktasıyla gösteriliyor, kapatmak belgenin "more" menüsünde ("Close the document"). "+" düğmesi sekmelerin hemen ardında değil, sekme şeridinin sonunda duruyor: `PantrixTabs`, `Modifier.weight(1f, fill = false)` verilince de kendisine kalan genişliğin tamamını alıyor.
*   [x] **TreeView satırının sonunda yalnızca bir metin var; büyük ağaçta her açık satır oluşturuluyor (2026-11-02).**
    *   Ne: Formatico'nun ağacı değerleri önizliyor: tarih bir etiketle, renk bir örnekle, adres bir bağlantı simgesiyle, resim bir küçük resimle. `TreeNode.trailing` yalnızca `String`; satıra bir öğe konamıyor. Ayrıca `PantrixTreeView` kaydırmıyor ve her açık satırı oluşturuyor ("binlerce açık satır kendi listesini ister"); masaüstünde 50 MB'lık belge hedefi için yetmez. Panolarda ağaç işaretleme.
    *   Nerede: [Tree View](https://pantrixui.com/components/tree-view); MCP `get_api PantrixTreeView`, `TreeNode`.
    *   Öneri: satırın sonu için bir `@Composable` yuva; tembel bir liste üstünde çalışan bir biçim (ya da `PantrixTreeView`'ın `LazyColumn` içinde yaşayan bir kardeşi).
    *   Durum: kapandı (0.1.2-SNAPSHOT, 2026-11-04). Önerildiği gibi geldi: `PantrixLazyTreeView` yalnızca görünen satırları oluşturuyor ve kendi alanında kayıyor; `TreeViewState.scrollToKey` bir satırı görünür kılıyor; `TreeNode.styledLabel` etiketi kendi biçimleriyle, `TreeViewStyle.labelRole` kodun yazısıyla çiziyor; ağacın `trailing`'i satırın sonuna bir öğe koyuyor. Workspace'in ağacı ona geçti: satır panodaki gibi `anahtar: değer`, sözdizimi renkleriyle ve Geist Mono'yla (seçili satırda seçimin kendi rengi); tarih takvimli bir etiket (telefonda takvim simgesi), renk bir örnek, resim panodaki 24×24'lük (telefonda 28) yer, YAML'ın anchor'ı bir etiket, alias'ın gösterdiği yer satırın sonunda kodun yazısıyla. Seçilen satır, metinde, aramada ya da anchor'a gidince görünür kılınıyor; arama eşleşen harfleri işaretliyor (şimdiki eşleşme daha koyu). Kalanlar başka bulgularda: bağlantının ve resmin simgesi ("Resmin ve bağlantının simgesi yok"; bağlantının yerinde şimdilik "Link" sözcüğü). Denendi: JVM, iOS simülatörü ve Wasm testleri; 80 satırlık bir ağaçta son satırın seçilince görünmesi. Önceki durum: açık, tıkıyor. (2026-11-04) Buna bağlı olanlar: ağacın seçilen satıra kaydırılması (Faz 4), metinle ağacın birlikte yürümesi (Faz 5), değer önizlemesi (Faz 8), büyük belgeler (Faz 14). Formatico kendi ağacını yazamaz: bu, Formatico'nun içinde bir bileşen yazmak olur.  Kodda (2026-11-03) Workspace'in ağacı `PantrixTreeView`: satırın sonunda tarih yazıyla ("Oct 28, 2026, 14:32 UTC"), renk, bağlantı ve resim birer sözcükle ("Colour", "Link", "Picture"); renk örneği, bağlantı simgesi ve küçük resim yok. Bir eksik daha görüldü: satırın yazısı Geist Mono'ya ve sözdizimi renklerine (anahtar, metin, sayı, `true`/`false`/`null`) geçirilemiyor. `TreeViewStyle` satır yüksekliğini, girintiyi ve boşlukları alıyor, yazının biçimini almıyor; `TreeViewColors` satırın tek bir yazı rengini alıyor. Panolarda satır `anahtar: değer`, iki renkte ve eş aralıklı; kodda gövde yazısıyla ve tek renkte. Öneri: `TreeNode`'a bir `AnnotatedString` etiket ya da etiket için bir yuva, `TreeViewStyle`'a etiketin yazı biçimi. YAML'la (2026-11-03) iki eksik daha görüldü. Anchor (`&labels`) ve alias'ın gösterdiği yer (`→ $.metadata.labels`) de satırın sonunda yazı; panodaki gibi bir etiket olamıyor. Seçilen bir satır, ekranın dışındaysa görünür kılınamıyor: `PantrixTreeView` kendisi kaymıyor ve bir satıra kaydırmanın (`scrollToKey` ya da bir `BringIntoViewRequester`) yolunu vermiyor; "Go to the anchor" seçiyor ve dalları açıyor, satıra götüremiyor. Faz 8'de (2026-11-04) iki şey daha görüldü. Ağaçta arama eşleşen harfleri satırda vurgulayamıyor (panoda `<mark>`), çünkü etiket bir `String`; seçilen eşleşme ekranın dışındaysa ağaç ona kaymıyor. Arama eşleşmeyi seçiyor ve dallarını açıyor; vurgu ve kaydırma bu bulguyu bekliyor.
*   [x] **scrollToKey, ağacın ilk yerleşiminden önce görünecek bir satırı da kaydırıyor (2026-11-04).**
    *   Ne: `TreeViewState.scrollToKey`, belgesinin örneğindeki gibi `LaunchedEffect(key) { state.scrollToKey(key) }` ile ağaç ilk kez çizilirken çağrılınca, listenin henüz görünen satırı yok (`layoutInfo.visibleItemsInfo` boş) ve `reveal` satırı görünmüyor sayıp `scrollToItem` ile en üste getiriyor. Satır kaydırmadan da görünecek olsa bile ağaç açılışta kayıyor; üstteki satırlar gizleniyor. Belge "zaten görünen bir satır için bir şey olmaz", "gerekenden öteye gitmez" diyor. Workspace-390'da bir belge seçili adla açılınca ağaç `currency`'den başlıyordu; panoda `$`'dan başlıyor.
    *   Nerede: MCP `get_api TreeViewState`; kaynak jar'da `TreeViewState.kt` (`reveal`).
    *   Öneri: `scrollToKey` listenin ilk yerleşimini beklesin (ya da yerleşimden önce gelen isteği o zaman uygulasın); belge ilk çizimdeki hâli söylesin.
    *   Durum: kapandı (0.1.2-SNAPSHOT, 2026-11-05). Önerildiği gibi geldi: `scrollToKey` ağacın kompoze edilip ilk kez yerleşmesini bekliyor, o anda görünen bir satır yerinde kalıyor; belgesi de ilk çizimdeki hâli söylüyor. Formatico'nun beklemesi kaldırıldı; `DocumentTree` `LaunchedEffect`'ten doğrudan `scrollToKey` çağırıyor. Denendi: telefonda bir belge açılışta görünen bir değer seçili açılınca ağaç `$`'dan başlıyor (`WorkspaceScreenTest`, JVM, iOS simülatörü ve Wasm; eski davranış taklit edilince düşüyor); ekran görüntüleri ve uzun ağaçta son satıra kaydırma değişmedi. Önceki durum: açık; Formatico `scrollToKey`'den önce `snapshotFlow { layoutInfo.totalItemsCount > 0 }` ile ilk yerleşimi bekliyordu.
*   [ ] **JsonView seçili bir yolu tutmuyor; metinle birlikte yürüyemiyor (2026-11-02).**
    *   Ne: Formatico'da metindeki imleç ağaçta bir değeri seçiyor, ağaçta seçilen değer metinde imleci taşıyor. `PantrixJsonView` açık yolları (`expanded`) dışarıya veriyor ama seçili bir yol, `onSelect` ya da "bu yola kaydır" yok. JSON'u kendisi bir `String`'den okuyor ve yalnızca RFC 8259 JSON'u alıyor: JSONC, JSON5, YAML, TOML ağaçları için kullanılamıyor, Formatico'nun kendi okuyucusunun ağacını da alamıyor.
    *   Nerede: [JSON View](https://pantrixui.com/components/json-view); MCP `get_api PantrixJsonView`.
    *   Öneri: düğümlerle beslenen bir belge ağacı (anahtar, değer türü, yol), seçili yol ve `onSelect`, yola kaydırma; değer önizlemesi için satır yuvası.
    *   Durum: açık.
*   [ ] **Kısayolun tuşlarını çizen bir öğe yok (2026-11-02).**
    *   Ne: `PantrixShortcut.keysOn` bir kısayolu tuş tuş veriyor (`⌘`, `K`), ama tuşları çizen bir bileşen yok; tasarım sisteminde de yok. Formatico'nun Start ve Workspace panolarındaki `⌘K` ve `⌘V` işaretleme.
    *   Nerede: [Keyboard shortcuts](https://pantrixui.com/docs/guides/keyboard-shortcuts); tasarım sisteminin bileşen listesi.
    *   Öneri: `CommandPalette`'in kısayol yazımını kullanan küçük bir `PantrixKeys` (ya da `PantrixShortcutLabel`).
    *   Durum: açık. Kodda (2026-11-03) başlatıcının `⌘K`'sı ve Start'ın `⌘V` ipucu `PantrixShortcut.writtenOn` ile düz metin; tuş kapağı çizilmiyor.
*   [x] **CodeBlock'un renk türlerinde anahtar yok (2026-11-02).**
    *   Ne: `CodeTokenKind`'in türleri `Plain`, `Comment`, `Keyword`, `Type`, `StringLiteral`, `Number`, `Punctuation`, `Annotation`; belgesi "bir özellik `Plain`'dir" diyor. JSON, YAML, TOML ve properties metninin yarısı anahtar; Formatico'nun sözdizimi renkleri (sahibinin kararı) anahtarı ayrı bir renkle yazıyor. `PantrixCodeBlock` bu yüzden dönüşümün çıktısını ve karşılaştırmayı Formatico'nun renkleriyle çizemiyor: anahtarlar düz metin olur ya da `Type` gibi anlamı başka bir türe yazılır.
    *   Nerede: [Code Block](https://pantrixui.com/components/code-block); MCP `get_api CodeTokenKind`.
    *   Öneri: veri biçimleri için bir `Property` (anahtar) türü, `CodeBlockColors`'ta rengiyle.
    *   Durum: kapandı (0.1.2-SNAPSHOT, 2026-11-03): `CodeTokenKind.Property` ve `CodeTokenKind.Tag`, renkleri `CodeBlockColors.property` ve `.tag`. Formatico'da henüz kullanılmıyor; Faz 5'in renklendiricisi kullanacak.
*   [ ] **İki metni yan yana karşılaştıran bir görünüm yok (2026-11-02).**
    *   Ne: Formatico'nun Compare ekranı iki belgeyi yan yana gösteriyor: değişen, eklenen ve çıkan satırlar, aynı yerdeki satırlar hizalı, farkların listesi. `PantrixCodeBlock`'ın `CodeMark`'ı bir satırı işaretliyor, ama iki bloğu hizalamak ve bir farkı ikisinde birden göstermek uygulamaya kalıyor. Panolarda karşılaştırma işaretleme.
    *   Nerede: kütüphanenin bileşenleri; tasarım sisteminin `Data` bölümü.
    *   Öneri: `PantrixCodeEditor`'ın yanına iki belgeyi hizalayan bir karşılaştırma görünümü; ya da kütüphanenin rehberlerinde iki `PantrixCodeBlock`'la nasıl kurulacağı.
    *   Durum: açık.
*   [ ] **DataTable'da bir hücre seçilemiyor ya da işaretlenemiyor (2026-11-02).**
    *   Ne: Formatico'da metindeki imleç tabloda bir hücreyi gösteriyor (`$.orders[2].total`): satırı seçili, hücresi çerçeveli. `PantrixDataTable` satırları seçiyor (`selected`, `rowTone`), ama hücre düzeyinde bir seçim, odak ya da işaret yok. `cell` yalnızca hücrenin iç dolgusunun içini çiziyor; hücrenin tamamını çevreleyen bir çerçeveyi uygulama çizemez. Table panolarında (1440, 834) seçili hücre bu yüzden işaretleme.
    *   Nerede: [Data Table](https://pantrixui.com/components/data-table); MCP `get_api PantrixDataTable`, `DataTableColumn`.
    *   Öneri: `selectedCell` (satırın anahtarı ve sütunun anahtarı) ya da hücre başına bir ton; klavyeyle hücreden hücreye gidiş istenmiyorsa yalnızca çizim yeterli.
    *   Durum: açık. Kodda (2026-11-03) seçili satır `selected` ile çiziliyor: kutular olmadan da (`onSelectionChange` verilmeden) `primary`'nin soluk dolgusu geliyor, panodaki gibi. Belge bunu söylemiyor ("Null leaves the table without boxes"); denenerek görüldü. Seçili hücrenin çerçevesini Formatico hücrenin içeriğine kendisi çiziyor: çerçeve hücrenin iç dolgusunun içinde kalıyor ve yalnızca yazının yüksekliğinde, panodaki gibi hücrenin tamamında değil.
*   [x] **Pano okunamıyor; Pantrix yalnızca yazıyor (2026-11-03).**
    *   Ne: Start'ın "Panodan yapıştır" düğmesi ve `⌘V` kısayolu panodaki metni okumalı. `PantrixClipboard` yalnızca `copyText` veriyor; okuma yolu yok. Compose'un ortak `Clipboard`'ı bir `ClipEntry` veriyor, ama `ClipEntry` her platformda başka bir tür ve ortak kodda metni okumanın yolu yok. Formatico bu yüzden Compose'un artık önerilmeyen `LocalClipboardManager`'ının `getText()`'ini kullanıyor: masaüstünde, Android'de ve iOS'ta çalışıyor; web'de her zaman `null` dönüyor (`WasmPlatformClipboardManager`: "Clipboard.readText() is async; no synchronous access on Web"). Web'de düğme bunu söyleyen bir ileti gösteriyor, kısayolun ipucu gösterilmiyor.
    *   Nerede: belgelerin `clipboard` sayfası; MCP `get_api PantrixClipboard`; `skill/platform` ("Clipboard").
    *   Öneri: `PantrixClipboard`'a askıya alınan bir `readText(): String?` ve `traits`'e okunup okunamadığı; web'de `navigator.clipboard.readText()` (izin isteğiyle) ve bir `paste` olayını dinleyen bir değiştirici (`Modifier.onPaste`), çünkü tarayıcı metni en rahat o olayla veriyor.
    *   Durum: kapandı (0.1.2-SNAPSHOT, 2026-11-04). Önerildiği gibi geldi: `PantrixClipboard.readText()`, `ClipboardTraits.readable`, `Modifier.onPaste` ve testte `TestClipboard.putText`. Start'ın düğmesi `readText()` ile, `⌘V` ya da Ctrl+V `onPaste` ile okuyor; `LocalClipboardManager` ve web'deki "desteklenmiyor" iletisi kalktı. İleti artık yalnızca okunacak bir pano olmadığında (`readable` yanlış) çıkıyor ve klavyeyle yapıştırmayı öneriyor. Web'de kısayolun ipucu da gösteriliyor. Denendi: JVM, iOS simülatörü ve Wasm testleri; yerel web derlemesinde tarayıcının yapıştırma olayı ve düğme (`navigator.clipboard.read` sayfada bir sahte metinle değiştirilerek). Denerken bir bulgu çıktı: "onPaste, shortcutRoot'un içinde web'de duymuyor".
*   [x] **onPaste, shortcutRoot'un içinde web'de duymuyor (2026-11-04).**
    *   Ne: `onPaste`'in KDoc'u pencerenin her yerinde duymak için `Box(Modifier.shortcutRoot().onPaste { … })` diyor. Bu sırayla web'de tarayıcının yapıştırması, odak kökteyken duyulmuyor: sayfa açılınca, yeni bir sekmede Start belirince, boş bir yere tıklanınca. Odak bir denetime geçince (Tab) duyuluyor. `PasteNode` bir `FocusEventModifierNode`; zincirde kökün odak hedefinin ardında, yalnızca içindeki hedeflerin odağını görüyor, kökün kendisininkini değil. Dinleyiciyi de odağı gördüğünde takıyor (`pasteFromBrowser`). Masaüstünde ve iOS'ta tuş yine geliyor: Compose tuşu odak hedefinin ardındaki düğümlere de veriyor. Sıra çevrilince (`onPaste { … }.shortcutRoot()`) web'de de duyuluyor.
    *   Nerede: MCP `get_api onPaste`, `shortcutRoot`; kaynak jar'da `OnPaste.kt`'nin KDoc örneği.
    *   Öneri: `PasteNode`, ardında durduğu kökün odağını da saysın: örneğin odağı içinde ya da kendisinde tutan en yakın odak hedefine baksın. Ya da KDoc örneği `onPaste { … }.shortcutRoot()` sırasını göstersin ve sıranın neden önemli olduğunu söylesin. Tarayıcı testi için: sayfaya bir `ClipboardEvent('paste')` gönderen bir yardımcı (`com.pantrixui:testing`); Formatico'nun testi bunu kendisi yapıyor.
    *   Durum: kapandı (0.1.2-SNAPSHOT, 2026-11-04). Düğüm artık odağa değil yapıştırma tuşuna bakıyor: web'de ⌘V ya da Ctrl+V ona inince, tuşun ardından gelen tarayıcı yapıştırmasını alıyor. Belgedeki `shortcutRoot().onPaste { … }` sırası da çalışıyor. Start'taki geçici çözüm kaldırıldı. `StartBrowserPasteTest` artık tuşu indirip sayfaya yapıştırmayı gönderiyor, sonra tuşu kaldırıyor; tuşsuz bir yapıştırmanın duyulmadığını da sınıyor. Bunun bir sonucu yeni bir bulgu: "onPaste, tarayıcının menüsünden yapıştırmayı duymuyor".
*   [ ] **onPaste, tarayıcının menüsünden yapıştırmayı duymuyor (2026-11-04).**
    *   Ne: 0.1.2-SNAPSHOT'ın `onPaste`'i web'de yalnızca ⌘V ya da Ctrl+V'nin ardından gelen yapıştırmayı alıyor. Tarayıcının Düzen › Yapıştır menüsü ve sağ tıklamanın Yapıştır'ı da sayfaya bir yapıştırma gönderiyor, ama onlarda tuş yok ve artık duyulmuyorlar. Önceki sürüm, düğümün odağı içindeyken her yapıştırmayı duyuyordu. Start'ın "or press ⌘V anywhere on this screen" ipucu klavyeyi söylüyor; menüden yapıştıran bir okuyucu için ekran bir şey yapmıyor.
    *   Nerede: kaynak jar'da `OnPaste.kt` (`armed`), KDoc'u ("the browser's paste that follows the key").
    *   Öneri: tuşla gelmeyen bir yapıştırmayı da, düğüm ya da içindeki bir şey odağı tutarken almak; ya da bunun neden alınmadığını KDoc'ta söylemek.
    *   Durum: açık. Formatico'yu tıkamıyor: düğme ve kısayol çalışıyor.
*   [ ] **PageScaffold'un başlığı her genişlikte içerikte duramıyor (2026-11-03).**
    *   Ne: Formatico'nun bir uygulama çubuğu var (marka, belge sekmeleri, başlatıcı); ekranların kendi çubuğu yok, başlıkları içerikte. Start'ın başlığı ("Paste anything") her genişlikte içerikte bir `display` başlığı. `PantrixPageScaffold`'un `PageTitlePlacement`'ı `Bar` ya da `Adaptive`: `Adaptive` dar pencerede başlığı yine kendi çubuğuna koyuyor, uygulamanın çubuğunun altına ikinci bir çubuk çiziliyor. Start bu yüzden `PantrixPageScaffold` değil, `pantrixVerticalScroll`'lu bir sütun; sayfanın verdiği pencere başlığı, `show` olayı ve klavyeden kaçış elle kalıyor.
    *   Nerede: [Page Scaffold](https://pantrixui.com/components/page-scaffold); MCP `get_component_metadata PageScaffold`; `skill/layout` ("A screen inside the shell").
    *   Öneri: başlığı her genişlikte içerikte tutan bir `PageTitlePlacement.Content` (ya da çubuğu hiç çizmeyen bir seçenek); rehberde, sekmeleri ya da kendi çubuğu olan bir kabuğun ekranlarının nasıl kurulacağı.
    *   Durum: açık.
*   [ ] **Boş bir listenin hâlini çizen bir bileşen yok (2026-11-03).**
    *   Ne: Start'ın Recent kartı ilk açılışta boş: bir simge, bir başlık ve bir açıklama ("Nothing opened yet"). Pantrix'te bunu çizen bir bileşen yok; `search_components "empty state"` yalnızca `DataTable`'ın kendi boş hâlini buluyor. Formatico bunu `PantrixIcon` ve iki `PantrixText` ile kuruyor. Tipografi belgesi `callout`'u "an empty state's description" diye anıyor, yani tasarım sistemi bu hâli biliyor.
    *   Nerede: [Components](https://pantrixui.com/components); MCP `search_components`.
    *   Öneri: simge, başlık, açıklama ve isteğe bağlı bir eylem alan bir `PantrixEmptyState`; tasarım sisteminde karşılığı.
    *   Durum: açık. Workspace (2026-11-03) aynı hâli beş yerde kendi çiziyor (`PaneMessage`): editörü bekleyen metin bölmesi, okunmayan bir biçim, düz metin, boş belge ve hiçbir değeri okunamayan belge.
*   [x] **İki bölmenin arasındaki ayırıcı sürüklenmiyor (2026-11-03).**
    *   Ne: Workspace-834 panosunda metin ile ağaç üst üste; aralarındaki ayırıcının bir tutamacı var ("Resize the text and the tree"): sürüklenince bölmelerin payı değişiyor. `PantrixTwoPane` iki bölmeyi `firstSize` (pay ya da sabit boy) ve `gap` ile ayırıyor, katlanan ekranın menteşesine uyuyor; ama ayırıcıyı sürükleyip `firstSize`'ı değiştirmenin yolu yok, `search_components "resizable split pane divider drag"` de başka bir bileşen bulmuyor.
    *   Nerede: [Two Pane](https://pantrixui.com/components/adaptive-two-pane); MCP `get_api PantrixTwoPane`, `search_components`.
    *   Öneri: `PantrixTwoPane`'e `onFirstSizeChange` ve sürüklenen bir ayırıcı (klavyeyle oklar, ekran okuyucuya "ayırıcı, yüzde 45"); ya da ayrı bir `PantrixSplitPane`.
    *   Durum: kapandı (0.1.2-SNAPSHOT, 2026-11-04). Önerildiği gibi geldi: `PantrixTwoPane(onFirstSizeChange)` boşluğa bir tutamaç koyuyor; parmak, işaretçi ve klavye (oklar, Home, End) ilk bölmeyi büyütüp küçültüyor, ekran okuyucuya bölmenin payı ayarlanabilir bir değer olarak söyleniyor, menteşe bölünce tutamaç yok. Tablette metin ile ağaç ya da problemler arasında artık panodaki 13 dp'lik şerit var: iki çizgi arasında sayfanın zemini ve ortasında tutamaç. Metin 480 dp'den başlıyor, sürüklenen boy ekranda tutuluyor (`rememberSaveable`). Denendi: JVM, iOS simülatörü ve Wasm testlerinde ok, Home ve sürükleme; geniş pencerede tutamaç yok. Denerken iki bulgu çıktı: "İki bölmenin tutamacı altındaki dokunuşları alıyor", "Bölme tutamacı tasarım sistemindekinden başka çiziliyor".
*   [x] **İki bölmenin tutamacı altındaki dokunuşları alıyor (2026-11-04).**
    *   Ne: `PantrixTwoPane`'in tutamacı, yakalandığı şeridi (`ListDetailScaffoldStyle.handleTarget`: Android'de 48, iOS'ta 44, masaüstünde ve web'de 16 dp) boşluğun ortasına, bölmelerin üstüne ve odanın bütün genişliğince koyuyor. Şerit boşluktan genişse bölmelerin kenarındaki dokunuşları da o alıyor: sürüklenmeyen bir dokunuş alttaki denetime geçmiyor. Workspace-834'te boşluk panodaki gibi 11 dp; Android'de şerit ağacın görünüm seçicisinin (Tree, Table, Problems) üst 15 dp'sini, iOS'ta 11 dp'sini kapatıyor ve oraya dokunmak hiçbir şey yapmıyor; metnin son satırının yarısı da öyle. Masaüstünde 16 dp'lik şerit seçiciye yetişmiyor. Bir sınamayla görüldü: seçicinin üst kenarının 2 dp altına dokunmak Android'de ve iOS'ta görünüm değiştirmiyor, ortasına dokunmak değiştiriyor.
    *   Nerede: MCP `get_component TwoPane` ("A handle"); kaynak jar'da `TwoPane.kt` ve `PaneHandle.kt` (`Modifier.fillMaxWidth().height(look.target)`).
    *   Öneri: şerit yalnızca çubuğun çevresini yakalasın (Material 3'ün sürükleme tutamacı gibi çubuğun boyunca 48 dp'lik bir dokunma hedefi); ya da sürüklenmeyen dokunuşları alttaki bölmeye bıraksın. Belge, şeridin boşluktan geniş olduğunda bölmelerin kenarını kapattığını söylesin.
    *   Durum: kapandı (0.1.2-SNAPSHOT, 2026-11-04). Denendi: Formatico'nun tablet ekranında, Android'in ve iOS'un dokunma hedefleriyle, görünüm seçicisinin üst kenarının 2 dp altına dokunmak Table'a geçiyor (`WorkspaceScreenTest`, JVM, iOS simülatörü ve Wasm). Telefon emülatöründe ve iPhone simülatöründe tablet düzeni çıkmadığı için cihazda bakılmadı. Kodda boşluk panodaki 11 dp. Pantrix oturumunun notu: şerit boşluktan genişse üstündeki bir dokunuş alttaki bölmeye de gidiyor; bölünme yönündeki bir sürükleme, aynı yönde kayan bir bölmenin üstünde de tutamacı taşıyor; fare paylaşılmıyor. 
*   [ ] **DataTable'ın başlığı yalnızca bir metin; sütunun türü altına yazılamıyor (2026-11-04).**
    *   Ne: Table panoları her sütunun adının altına türünü yazıyor ("text", "date", "number", "text, null"). `DataTableColumn`'un `header`'ı bir `String`; başlığa ikinci bir satır, bir yuva ya da bir alt yazı konamıyor.
    *   Nerede: MCP `get_api DataTableColumn`; kaynak jar'da `DataTableColumn.kt` ve `DataTableHeader.kt`.
    *   Öneri: `DataTableColumn`'a başlığın altında duran bir `subtitle` (ekran okuyucuya başlıktan sonra okunan), ya da başlık için bir `@Composable` yuva.
    *   Durum: açık. Formatico'yu tıkamıyor; Faz 8'in "sütun türleri" maddesi buna bağlı ve kodda yok.
*   [ ] **Menünün bir başlığı, satırın sonunda bir yazısı yok (2026-11-04).**
    *   Ne: Workspace-1440 panosunun "Copy path" menüsü "Copy the path of createdAt as" diye bir başlıkla açılıyor; her satır biçimin adını solda, verdiği yolu sağda eş aralıklı yazıyor. Workspace-390'da aynı menü başlıklı bir sayfa. `PantrixMenu`'nün başlığı yok, dar pencerede açılan sayfasının da yok; `PantrixMenuItem` bir `shortcut` (yalnızca kısayol için, dokunmatikte yazılmıyor) ve etiketin altında bir `supportingText` alıyor.
    *   Nerede: MCP `get_api PantrixMenu`, `PantrixMenuItem`.
    *   Öneri: `PantrixMenu`'ye bir `title` (panelde başlık satırı, sayfada sayfanın başlığı); `PantrixMenuItem`'a satırın sonunda bir `trailingText`.
    *   Durum: açık. Kodda başlık menünün içinde bir `PantrixText`, yol satırın altında (`supportingText`, kodun yazı biçimiyle).
*   [ ] **Resmin ve bağlantının simgesi yok (2026-11-04).**
    *   Ne: `PantrixIcons` 37 simge taşıyor; bir resmin ya da bir bağlantının (dışarıya açılan) simgesi yok. Panolarda ağacın değer önizlemesi ve Summary'nin "Found in the values" satırları ikisini de çiziyor; tasarım sistemi de onları kendi `<svg>`'siyle çiziyor.
    *   Nerede: MCP `search_icons "image picture photo link external"`; kaynak jar'da `PantrixIcons.kt`.
    *   Öneri: `PantrixIcons.Image` ve `PantrixIcons.ExternalLink` (sağdan sola dillerde aynalanmayan); tasarım sisteminde karşılıkları.
    *   Durum: açık. Summary'de tarihin satırı takvimle, rengin satırı rengin kendisiyle açılıyor; resmin ve bağlantının satırında simgenin yeri boş.
*   [x] **Bir adresten resim yükleyen bir şey yok (2026-11-04).**
    *   Ne: Ağacın değer önizlemesi bir resmin adresini tanıyor ("https://images.example.com/u/elif.png"). CLAUDE.md değerlerin önizlenmesini istiyor, resimler de içinde. `PantrixImage` yalnızca bir `Painter`, bir `ImageVector` ya da bir `ImageBitmap` alıyor: ağdan indirmiyor, önbelleğe almıyor, yüklenirken ya da yüklenemeyince ne göstereceğini bilmiyor. Panolar şimdilik gerçek bir küçük resim değil, 24×24'lük bir yer tutucu çiziyor: `neutral-container` zeminli, 4 piksel köşeli bir kutu ve içinde bir resim simgesi ("Picture at this address").
    *   Nerede: MCP `get_api PantrixImage`; Pantrix ekibinin yanıtı (2026-11-04).
    *   İstenen:
        *   **Boy:** ağaçta 24×24, köşesi `shapes.sm`; seçili değer kartında daha büyüğü olabilir.
        *   **Yüklenirken:** panonun yer tutucusu (resim simgeli kutu) ya da bir iskelet; bir dönen gösterge değil, çünkü bir ağaçta onlarca satırda aynı anda dönerdi.
        *   **Yüklenemeyince:** aynı yer tutucu, ekran okuyucuya "resim yüklenemedi".
        *   **Web'de:** başka bir sitedeki resmi Compose'un çözmesi için baytların okunabilmesi gerekiyor, bu da CORS ister; çoğu resim sunucusu izin vermez. O zaman da yer tutucu kalmalı. Uygulama, adresten bir şey indirmeden önce okuyucuya sormak (ya da Settings'te kapatmak) isteyebilir: belgedeki bir adres, belgeyi açanın IP'sini o sunucuya verir.
        *   **Önbellek:** aynı adres bir oturumda bir kez indirilmeli.
    *   Öneri: `PantrixImage(url = …, placeholder = …, error = …)` ya da ayrı bir `PantrixAsyncImage`; ya da Pantrix'in bir yükleyiciyle (Coil 3) nasıl kullanılacağını gösteren bir sayfa.
    *   Durum: kapandı, sahibinin kararıyla (2026-11-04, Pantrix oturumundan iletildi: "coil kullanılacak ihtiyaç olursa"). Pantrix'e bir yükleyici eklenmeyecek. Formatico gerçek bir küçük resme geçerse Coil 3'ü kendisi ekler: `rememberAsyncImagePainter(url)` ile `PantrixImage(painter = …)`; yüklenirken ve yüklenemeyince yer tutucu, web'de CORS, önbellek ve adresi indirmeden önce sormak o zaman Formatico'nun işi. Şimdilik kodda resim adresi panodaki yer tutucuyla gösterilecek; resim simgesi yok (bkz. "Resmin ve bağlantının simgesi yok").
*   [ ] **Sık bir çubuktaki bir düğme dokunma hedefini alamıyor (2026-11-04).**
    *   Ne: Workspace panolarının durum çubuğu 1440'ta 28, 834'te 32 piksel; Summary'yi açan boyut bu çubukta bir düğme. Pantrix bir denetime platformun dokunma hedefini yerleşimde ayırıyor (masaüstünde 32, iOS'ta 44, Android'de 48 dp); daha az yer verilen düğme teşhislerde `SmallTouchTarget` oluyor. Masaüstü uygulamalarının durum çubukları (bir düzenleyicinin satır, sütun ve kodlama düğmeleri) işaretçiyle kullanılan sık çubuklar.
    *   Nerede: MCP `get_documentation_page skill/accessibility` ("Touch targets", `Modifier.minTouchTarget`).
    *   Öneri: dokunma hedefini yerleşimi büyütmeden, çizilenin dışına taşan bir basma alanıyla sağlamak (komşusuyla çakışmadığı sürece), ya da sık çubuklar için bir `PantrixStatusBar` ve öğesi.
    *   Durum: açık. Kodda çubuk düğmeye yetecek kadar uzuyor: masaüstünde 32, tablette platformun hedefi kadar.
*   [ ] **Temanın diline göre küçük harfe çevirme dışarıya açık değil (2026-11-04).**
    *   Ne: Pantrix aramalarında harfleri temanın dilinde küçültüyor (`PantrixSelect`, `PantrixJsonView`: Türkçede "i" "İzmir"i buluyor). Ağaçta arama için aynısı gerekti; bunu yapan işlev dışarıya açık değil. Formatico Türkçe ve Azerice için I ile İ'yi kendisi çeviriyor (`folded`).
    *   Nerede: MCP `search_api "lowercase language compare"`; `textOrder` sıralıyor, küçültmüyor.
    *   Öneri: `PantrixLocale.lowercase(text)` ya da bir metnin başka bir metni dilin harfleriyle içerip içermediğini söyleyen bir işlev.
    *   Durum: açık. Formatico'yu tıkamıyor.
*   [ ] **Bölme tutamacı tasarım sistemindekinden başka çiziliyor (2026-11-04).**
    *   Ne: Tasarım sisteminin `.ptx-pane-handle`'ı 4×32 px'lik bir çubuk, `--border-strong` renginde (açıkta slate-500); odakta şeridin çevresine `--focus-ring` çiziyor. Kütüphanenin tutamacı çubuğu `textSecondary`'nin %40'ında çiziyor (bir sayfanın tutamacının rengi), işaretçinin altında ve sürüklenirken koyulaştırıyor, odakta çubuğu odak rengine boyuyor. Android'de ve iOS'ta çubuk 48 dp uzun. Panolar tasarım sistemine uyuyor: Workspace-834'te çubuk koyu ve 32 px, kodda açık; dokunmatikte 48 dp. `PantrixTwoPane` renk almıyor, `ListDetailScaffoldStyle` yalnızca boyları alıyor.
    *   Nerede: Claude Design'daki tasarım sistemi, `components/bundle.css` (`.ptx-pane-handle`); MCP `get_api ListDetailScaffoldStyle`; kaynak jar'da `PaneHandle.kt` (`barColor`).
    *   Öneri: iki taraftan biri ötekine uysun: kütüphane çubuğu `borderStrong` ile çizsin, ya da tasarım sistemi `text-secondary`'nin %40'ını ve dokunmatikte 48'i alsın. Tutamacın rengi bir stilde ya da `colors`'ta verilebilsin.
    *   Durum: açık. Formatico'yu tıkamıyor. Kodda kütüphanenin çizdiği. Pantrix'te kapanıyor (2026-11-04, Pantrix oturumu): tasarım sistemi kütüphaneye uydu (henüz yayımlanmadı), kütüphanenin varsayılan renkleri aynı kaldı (dururken `textSecondary`'nin %40'ı, işaretçinin altında `textSecondary`, odakta `focusRing`); rengi vermek için `PaneHandleColors(bar, hoveredBar, focusedBar)`, `PantrixTwoPane`, `PantrixListDetailScaffold` ve `PantrixSupportingPaneScaffold`'da son parametre `handleColors`. Yeni yerel paketle ve tasarım sisteminin yayımıyla doğrulanacak; Workspace-834 panosunun tutamacı o zaman tasarım sistemine göre yeniden çizilir.
*   [ ] **Ayın adıyla yazılan, haftanın günü olmayan bir tarih yok (2026-11-03).**
    *   Ne: Workspace-390 panosunun seçili değer kartı bir tarihi "28 October 2026, 14:32 UTC" diye, haftanın gününü alt satırda yazıyor. `PantrixStrings`'in `date` kısa biçimi veriyor ("Oct 28, 2026"); `longDate` ayın adını yazıyor ama haftanın gününü de katıyor ("Wednesday, October 28, 2026"), çünkü ekran okuyucunun bir takvim gününü duyduğu biçim (`longDateTemplate`). Haftanın günü olmadan ayın adıyla yazan bir biçim yok.
    *   Nerede: MCP `get_api PantrixStrings` (`date`, `longDate`, `longDateTemplate`); MCP `get_documentation_page guide/date-time`.
    *   Öneri: `fullDate(day, month, year)` ve bir `fullDateTemplate` ("{month} {day}, {year}", "{day} {month} {year}").
    *   Durum: açık. Kodda (2026-11-03) kartın başlığı `longDate`, haftanın günü başlıkta; alt satır yalnızca ne kadar önce olduğunu ve okuyucunun saatini söylüyor.
*   [ ] **Breadcrumb, kısa bir sayfaya yetmeyen yerde sınırının dışına taşıyor (2026-11-03).**
    *   Ne: Table'ın araç çubuğunda yol izi (`$ › orders`) dar pencerede önce daralsın diye `Modifier.weight(1f, fill = false)` alıyor. 800 piksellik bir tarayıcı penceresinde ona "$"dan az yer kaldı; `PantrixBreadcrumb` "$"ı yine de çizdi ve yanındaki sayının üstüne bindi ("$249 rows"). Belge "a short one keeps all of its own" diyor, yani kısa sayfa kesilmiyor; ama tek bir kısa sayfa bile sığmadığında ne olduğunu söylemiyor ve bileşen kendi sınırını kırpmıyor.
    *   Nerede: [Breadcrumb](https://pantrixui.com/components/breadcrumb); MCP `get_api PantrixBreadcrumb`.
    *   Öneri: izin kendi sınırında kırpılması ya da en kısa hâlinin genişliğini en küçük içsel genişlik olarak vermesi (satırın ağırlıkları onu gözetsin); belgede bu durumun bir cümlesi.
    *   Durum: açık. Formatico (2026-11-03) izi `clipToBounds()` ile kırpıyor: yer kalmayınca iz görünmüyor, sayı okunuyor.
*   [ ] **PantrixIcons'ta ChevronUp yok; aramanın önceki ve sonraki eşleşmesi yukarı ve aşağı çizilemiyor (2026-11-03).**
    *   Ne: Workspace panolarına ağaçta arama çizildi: bir alan, eşleşme sayısı ("1 of 4"), önceki ve sonraki eşleşme düğmeleri. Editörlerde ve tarayıcılarda bu iki düğme yukarı ve aşağı ok. PantrixIcons'ta `ChevronDown` var, `ChevronUp` yok; `ChevronBack` ve `ChevronForward` sayfalamanın simgeleri ve sağdan sola yazılan dillerde dönüyorlar. Panolarda önceki ve sonraki bu yüzden `ChevronBack` ve `ChevronForward`.
    *   Nerede: MCP `search_icons "chevron up"`; tasarım sistemi, `project/components/index.d.ts` (`IconName`).
    *   Öneri: dönmeyen bir `ChevronUp`.
    *   Durum: açık.
*   [ ] **FileDrop'un sözcükleri tasarım sistemindekinden farklı ve tek dosyada da çoğul (2026-11-03).**
    *   Ne: Tasarım sistemindeki `FileDrop` bir başlık (`title`), bir eylem (`action`) ve alanın içinde bir destek metni alıyor; Start panoları "Drop a file here", "Choose a file" ve biçimlerin listesini alanın içinde çiziyor. Compose'daki `PantrixFileDrop`'ta yalnızca `label` ("what the zone says first") var, eylemin sözcükleri verilemiyor, `supportingText` alanın altında. `multiple = false` iken de alan "Drop files here", "or choose files" diyor.
    *   Nerede: MCP `get_component_metadata PantrixFileDrop`; tasarım sistemi, `FileDrop`'un özellikleri.
    *   Öneri: tek dosya alan bir alanın sözcükleri tekil; eylemin sözcükleri için bir parametre; tasarım sistemi ile Compose aynı yerleşimde.
    *   Durum: açık.
*   [ ] **PantrixTabs'ın altındaki çizgi yalnızca saydam bir renkle gizleniyor (2026-11-03).**
    *   Ne: Formatico'nun belge sekmeleri üst çubuğun içinde bir `PantrixTabs` (kaydırılabilir). Sekmelerin altında satır boyunca bir çizgi çiziliyor; çubuğun kendi alt kenarıyla iki çizgi oluyordu. `TabsStyle`'da bu çizgi için bir alan yok; `TabsColors.divider` "the hairline under the row" diyor, çizgi ancak ona `Color.Transparent` verilerek gizleniyor. Sayfası bir çubuğun içindeki sekmeleri anmıyor.
    *   Nerede: MCP `get_api TabsStyle`, `get_api TabsColors`, `get_component tabs`.
    *   Öneri: `TabsStyle`'da çizgiyi kapatan bir alan, ya da sayfada "bir çubuğun içinde: `TabsColors(divider = Color.Transparent)`".
    *   Durum: açık. Formatico (2026-11-03) üst çubukta `TabsColors(divider = Color.Transparent)` veriyor.
*   [ ] **CodeEditor'ın işaretinin iletisinde düğme yok (2026-11-04).**
    *   Ne: Invalid panoları, imleç bir problemin üstündeyken metnin içinde bir kutu çiziyor: problemin iletisi ve altında iki düğme, onarım ("Add the comma") ve "Next problem". `CodeRangeMark`'ın `CodeMark`'ı yalnızca bir ileti ve bir kaynak alıyor (`message`, `source`); iletinin yanına bir eylem konamıyor. Formatico problemleri işaretleyip iletilerini gösteriyor, onarımı ve sonraki problemi problemler bölmesinden veriyor.
    *   Nerede: MCP `get_component PantrixCodeEditor` ("Marks", `CodeRangeMark`); `get_api CodeMark`.
    *   Öneri: `CodeMark`'a bir ya da iki eylem (`actions: List<CodeMarkAction>`: etiket ve `onClick`), iletinin altında küçük düğmeler olarak; klavyeyle de ulaşılan (bir kısayolla, ör. ⌘.).
    *   Durum: açık.

*   [ ] **CodeEditor, imlecin satırındaki işareti çizmiyor (2026-11-04).**
    *   Ne: Bir `CodeRangeMark`, imleç onun satırına gelince görünmez oluyor: altındaki dalgalı çizgi de satır numarasının yanındaki nokta da gidiyor; satır yalnızca imlecin satırının dolgusuyla çiziliyor. İmleç başka satıra geçince işaret geri geliyor. Belge tersini söylüyor: işaret çizilir, iletisi de "while the caret is in it" gösterilir. Invalid panolarının üçü de imleci ikinci problemin üstünde (10. satır) ve o satırı işaretli çiziyor; Formatico'nun önizlemelerinde 10. satır bu yüzden işaretsiz. Masaüstünde (JVM, `PantrixTestTheme`) üç satırlık bir JSON'da görüldü: imleç başta iken 3. satır işaretli, imleç 3. satırdayken işaretsiz. Odak editörde değilken iletinin `CodeEditor_<tagKey>.Note_<number>` düğümü de yok; odakla gösterilip gösterilmediği doğrulanacak.
    *   Nerede: MCP `get_component PantrixCodeEditor` ("Marks", `rangeMarks`: "a wavy line under each in the colour of its tone, a dot before its line, and its message while the caret is in it"); `get_api CodeRangeMark`.
    *   Öneri: imlecin satırında da işaretin çizgisi ve noktası; imlecin satırının dolgusu işaretin altında kalsın. Bir ekran resmi testi: imleç işaretin içindeyken çizgi ve nokta çiziliyor.
    *   Durum: açık. Formatico'da geçici bir çözüm yok; telefonun problem kartı imlecin problemini ayrıca söylüyor.
