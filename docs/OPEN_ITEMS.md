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
*   [ ] **Koin ve navigation3 henüz bağlı değil (2026-11-02).**
    *   Ne: Yığının ikisi de sahibinin kararı; ama tuvalde ekran panosu yok, ilk özellik modülü de yok. Boş bir Koin modülü ve tek hedefli bir gezinti, sınanamayan kod olurdu.
    *   Öneri: ilk özellik (Start ya da Workspace) panosu çizilince bir `formatico.kmp.feature` convention eklentisi: `core:base`, `core:designsystem`, `koin-compose-viewmodel`, `navigation3-ui`. Sürümler bugün Koin 4.2.2, navigation3 1.2.0.
    *   Durum: açık.
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
