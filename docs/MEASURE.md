# Büyük belgelerin ölçümü

Faz 14'ün ölçümü (2026-11-06): sahibinin hedefi masaüstünde 50 MB, telefonda ve web'de 10 MB (CLAUDE.md). Bir MB
Formatico'nun saydığı gibi 1.000.000 bayt; dosyanın sınırı da öyle.

## Nasıl ölçülür

```bash
./gradlew :feature:workspace:jvmTest --tests '*LargeDocumentsMeasure*' -Pformatico.measure
```

`LargeDocumentsMeasure` (`feature/workspace/src/jvmTest`) yalnızca `-Pformatico.measure` verilince çalışır;
`-Pformatico.measure.sizes=10` ve `-Pformatico.measure.formats=csv,json` daraltır. Örnekler `LargeSamples`'ın
(`commonTest`): siparişler (JSON, XML, YAML, TOML), bir günlüğün olayları (NDJSON) ve bir tablonun satırları (CSV),
her koşuda aynı. Her adımın süresi ve okumanın ve ağacın tuttuğu bellek `feature/workspace/build/measure/large-documents.md`'ye
yazılır. Karşılaştırmada öteki belge aynıdır, ortasındaki bir ad değişmiştir.

## JVM

MacBook (Apple silikon, 128 GB), JDK 21, yığın 8 GB. Süreler milisaniye; "MB" okumanın ya da ağacın, metnin üstüne
tuttuğu bellek. Ağaç: belge açılırken açılan dallar ve kurulan satırlar. Seçim: ortadaki bir satır seçilince satırların
yeniden kurulması. Sorgu: `$..id`, XML'de `//order`. Convert: JSON YAML'a, öteki her biçim JSON'a.

### 10 MB

| Biçim | Okuma (ms / MB) | Ağaç (ms / MB, satır) | Seçim | Tablo | Summary | Format | Minify | Sorgu | Convert | Anlamla karşılaştırma | Metinle |
| :--- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| JSON | 434 / 135 | 28 / 12 (26.821) | 7 | 16 | 287 | 282 | 254 | 83 | 388 | 994 | 304 |
| NDJSON | 430 / 159 | 26 / 47 (112.040) | 20 | 21 | 121 | 255 | 248 | 14 | 185 | 761 | 204 |
| CSV | 621 / 320 | 53 / 81 (193.768) | 24 | 13 | 341 | 145 | 0 | 92 | 301 | 1506 | 234 |
| XML | 538 / 160 | 10 / 19 (43.389) | 0 | 10 | 145 | 174 | 213 | 442 | 120 | 1051 | 216 |
| YAML | 1031 / 163 | 18 / 21 (49.789) | 4 | 5 | 114 | 868 | 815 | 73 | 260 | 1324 | 302 |
| TOML | 545 / 157 | 0 / 0 (3) | 0 | 5 | 158 | 385 | 314 | 48 | 153 | 856 | 248 |

### 50 MB

| Biçim | Okuma (ms / MB) | Ağaç (ms / MB, satır) | Seçim | Tablo | Summary | Format | Minify | Sorgu | Convert | Anlamla karşılaştırma | Metinle |
| :--- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| JSON | 1856 / 674 | 35 / 57 (134.093) | 12 | 53 | 554 | 1373 | 981 | 134 | 1528 | 4570 | 1180 |
| NDJSON | 2062 / 802 | 135 / 237 (560.191) | 70 | 36 | 474 | 1116 | 1180 | 42 | 636 | 3589 | 1021 |
| CSV | 3162 / 1592 | 204 / 404 (960.321) | 95 | 73 | 1406 | 477 | 0 | 386 | 1521 | 8237 | 1139 |
| XML | 1695 / 800 | 53 / 94 (216.234) | 0 | 42 | 435 | 879 | 770 | 1981 | 667 | 5281 | 1117 |
| YAML | 4014 / 811 | 56 / 106 (247.977) | 32 | 27 | 560 | 3728 | 4362 | 164 | 722 | 5438 | 1250 |
| TOML | 2448 / 785 | 0 / 0 (3) | 0 | 24 | 465 | 1646 | 1763 | 265 | 696 | 5196 | 1282 |

Ağaçta arama ("grace"), 10 MB: CSV 76 ms (32.295 eşleşme), JSON 25 ms (4.470).

**Ağacın kaynağı (2026-11-11).** Pantrix'in C partisiyle Workspace'in ağacı bir kaynaktan okuyor (`TreeSource`,
`DocumentSource`): ağaç bir dalın kaç çocuğu olduğunu ve sıradaki çocuğunu satır görünmek üzereyken istiyor. Ölçüm
artık bir ekranın satırlarını kaynağa ağacın sorduğu gibi soruyor (en çok 60 satır): her biçimde, 10 ve 50 MB'ta,
açılışta 0–12 ms, tutulan bellek ölçülemeyecek kadar az, 60 satır kuruluyor; en kalabalık dal açıkken de 61–63 satır.
Yukarıdaki tablolarda "Ağaç" sütunu kaynaktan önceki hâl: 50 MB'lık CSV'de 960.321 satır, 404 MB. "Seçim" sütunu da
kalktı: bir değer seçilince hiçbir satır yeniden kurulmuyor. Okuma değişmedi (50 MB CSV 3.577 ms / 1.592 MB).

### Düzeltmelerden önce (10 MB)

| | JSON | NDJSON | CSV | XML | YAML | TOML |
| :--- | ---: | ---: | ---: | ---: | ---: | ---: |
| Okumanın belleği (MB) | 182 | 199 | 408 | 220 | 234 | 210 |
| Ağacın belleği (MB) | 441 | 91 | 157 | 36 | 41 | — |
| Sorgu (ms) | 98 | 35 | 115 | dakikalar | 57 | 44 |

Önceki ağaç kapalı dallar dahil her düğümün satırını kuruyordu; NDJSON'dan sonrası, kapalı dalları kurmayan ağaçla ama
paylaşılan biçimlerden önce ölçüldü. 50 MB'ta CSV'nin okuması 2.028 MB'tı (şimdi 1.592), ağacı 780 MB (şimdi 404).

## Cihazlar

Bütün cihazlar yayın yapısıyla, 10 MB'lık iki örnekle: siparişler (JSON, 9.999.200 bayt) ve tablonun satırları (CSV,
9.999.018 bayt, 192.671 satır). Süre dosya seçilince ya da uygulama açılınca başlıyor, ağaç ya da tablo görününce
bitiyor; ekran görüntüsüyle ölçüldü, yarım saniye kadar şaşar.

| | Android | iOS | Web |
| :--- | :--- | :--- | :--- |
| Cihaz | Emülatör, Pixel 7 Pro (arm64, katlanabilir), `largeHeap` | iPhone 18 Pro Max simülatörü | Chrome (uygulamanın tarayıcı bölmesi), Wasm |
| JSON | Soğuk açılışta 5,1 s; yığın en çok 386 MB, sonra 159 MB | Dosyalar'dan 4 s; bellek 1,0 GB | Sekmesine geçince 2,0 s; JS yığını 279 MB |
| CSV | Soğuk açılışta 9,8 s (önce 13 s); yığın en çok 394 MB | Soğuk açılışta 5,9 s (önce 48 s); bellek 1,35 GB | Yeniden yüklenince 10 s (önce 35 s); JS yığını 1,1 GB |
| Seçim | Ağaçta bir değer 0,3 s içinde seçili; 194 bin satırlık CSV ağacında yarım saniye (satırları koruyan `RowMaker`'dan önce) | — | — |

*   **Android:** emülatörün yığın sınırı 192 MB; 10 MB'lık JSON okunurken yığın 370 MB'a çıkıyor, bu yüzden uygulama
    `android:largeHeap="true"` istiyor (emülatörde 576 MB). Debug yapısında JSON 8–10 s sürüyor. CSV'nin ilk ölçümünde
    arka sekmede 10 MB'lık JSON'un okunmuşu da tutuluyordu: yığın 493 MB'a, sınıra 80 MB kalana dek çıktı. Okunmuşların
    sınırı (`readingsBudget`) artık yalnızca birini tutuyor. CSV kapatılıp JSON'a geçilince uygulama bir kez bellek
    yetmezliğiyle kapandı: geçmiş, kaydettiği her metni uygulama kapanana dek bellekte tutuyordu. O düzeltildikten sonra
    aynı adımda yığın en çok 547 MB; kapanan belgenin durumu okuma sürerken bir süre bırakılmıyor (`OPEN_ITEMS.md`).
*   **iOS:** belleğin hemen hepsi Kotlin/Native'in yığını ("App-Specific Tag 7"): nesnelerin işaretçileri sekiz bayt,
    metinler hep iki baytlık; aynı okuma JVM'dekinin iki katı kadar. CSV'nin 48 saniyesi okumanın değil tablonun
    sütunlarınındı: her hücre sayı diye okunuyordu (`statsOf`), Kotlin/Native'de sayı olmayan bir metni okumak bir istisna
    kurup yakalıyor. Şimdi yalnızca sayıların sütunu okunuyor.
*   **Web:** Wasm'da arka plan iş parçacığı yok (`Dispatchers.Default` ana iş parçacığı); okuma sürerken sayfa donuyor,
    en uzun donma 2,3 s. Dosya, sayfanın seçicisine bu sekmede verilerek açıldı (uygulamanın tarayıcı bölmesi işletim
    sisteminin dosya seçicisini süremiyor).
*   **Masaüstü:** 50 MB'lık bir CSV okunmuşuyla ve ağacıyla 2 GB'ı geçtiği için uygulama makinenin belleğinin yarısını
    alıyor (`-XX:MaxRAMPercentage=50`); JVM'in varsayılanı dörtte biri, 8 GB'lık bir makinede 2 GB. Uygulamanın bütünüyle
    ölçümü aşağıda, "Uygulamanın bütünü".

## Uygulamanın bütünü (Faz 21, 2026-11-10)

Faz 14'ün JVM ölçümü okumayı ve ağacın satırlarını ölçüyordu; bu ölçüm ekranın bütününü: Start'ın bırakma alanına
dosya verilir, belge okunup durum çubuğu "Valid JSON" ya da "Valid CSV" diyene kadar geçen süre ve yol boyunca tutulan
bellek. Örnekler Faz 14'ünkiler, sınırın hemen altında: JSON 9.998.824 ve 49.998.891 bayt, CSV 9.998.703 ve 49.997.437
bayt.

```bash
./gradlew :feature:workspace:jvmTest --tests '*LargeDocumentsMeasure.samples' -Pformatico.measure.samples=$PWD/build/measure/samples -Pformatico.measure.formats=json,csv
```

```bash
./gradlew :app:shared:jvmTest --tests '*AppLargeDocumentsMeasure*' -Pformatico.measure -Pformatico.measure.samples=$PWD/build/measure/samples
```

```bash
./gradlew :app:webTest:test --tests '*WebLargeDocumentsMeasure*' -Pformatico.measure -Pformatico.measure.samples=$PWD/build/measure/samples
```

Makine: Apple M5 Max, 128 GB, JDK 21, Chrome 157 (başsız).

### Masaüstü: 1440×900'lük pencerede

`AppLargeDocumentsMeasure` (`app/shared/src/jvmTest`) uygulamanın kendisini (`App`) masaüstünün penceresinde,
1440×900'de ve masaüstü platformunda çiziyor; Compose'un test penceresi ekran dışında çizer. Testin yığını 4 GB: masaüstü
uygulamasının 8 GB'lık bir makinede aldığı (`-XX:MaxRAMPercentage=50`). Süre bırakma alanına basıldığında, dosya elde
başlıyor; bellek çöp toplanmadan en çok ve belge görününce toplanmış hâliyle.

Pantrix'in C partisiyle (2026-11-11), ağaç kaynaktan okurken (`TreeSource`):

| Dosya | Görünene dek | Yığın en çok | Yığın görününce |
| :--- | ---: | ---: | ---: |
| JSON, 10 MB | 0,6 s | 936 MB | 229 MB |
| CSV, 10 MB | 2,1 s | 1.026 MB | 415 MB |
| JSON, 50 MB | 2,0 s | 2.769 MB | 1.049 MB |
| CSV, 50 MB | 6,4 s | 3.292 MB | 1.969 MB |

İlk ölçüm (2026-11-10): JSON 10 MB 0,7 s, 697 / 242 MB; CSV 10 MB 1,9 s, 1.006 / 416 MB; CSV 50 MB 5,9 s,
3.351 / 1.969 MB; JSON 50 MB açılmıyordu. "Yığın en çok" çöp toplanmadan okunduğu için koşudan koşuya oynar.

*   **JSON, 50 MB:** açılıyor. Pantrix'in kod editörü bir milyonu aşkın satırlık metinde çöküyordu: satırları parçalara
    bölerken sınırı `Int`'te hesaplıyordu (`PANTRIX_FINDINGS.md`, "Kod editörü bir milyonu aşkın satırlık bir metinde
    çöküyor", 2026-11-11'de kapandı). Örnek 1.741.784 satır.
*   **CSV, 50 MB:** 4 GB'ın 3,3 GB'ına çıkıyor; açıldıktan sonra 2 GB. 8 GB'lık bir makinede sığıyor ama payı az.
*   **Denenmeyen:** işletim sisteminin gerçek penceresi ve dosya seçicisi. Uygulama bir dosyayı yalnızca kendi
    denetimleriyle (seçici, bırakma, yapıştırma) alıyor; bunları bu makinede sürmek sahibinin klavyesini ve panosunu
    kullanmak demek. Ölçülen, aynı uygulamanın aynı JVM'de ve aynı yığında, aynı boyda çizdiği ekran.

### Web: JavaScript yedeği

`WebLargeDocumentsMeasure` (`app/webTest`) yayın dağıtımını, sunucunun başlıklarıyla, makinenin Chrome'unda açıyor. JS
yedeği yükleyiciye `WebAssembly.validate`'in `false` demesiyle seçiliyor (eski bir tarayıcıya verilen cevap); dosya
sayfanın dosya seçicisine DevTools'la veriliyor. Bellek V8'in yığını: Wasm'ın nesneleri de onda (çöp toplaması
tarayıcının), Skia'nın kendi belleği değil.

| Yapı | Dosya | Görünene dek | JS yığını en çok | JS yığını görününce |
| :--- | :--- | ---: | ---: | ---: |
| JavaScript | JSON, 10 MB | 2,4 s | 767 MB | 205 MB |
| JavaScript | CSV, 10 MB | 3,0 s | 746 MB | 344 MB |
| Wasm | JSON, 10 MB | 3,1 s | 975 MB | 259 MB |
| Wasm | CSV, 10 MB | 5,0 s | 958 MB | 473 MB |

*   **JS yedeği hedefi karşılıyor:** 10 MB'lık JSON 2,4 s'de, CSV 3 s'de açılıyor; JS yığını en çok 770 MB.
*   **Wasm burada JS'ten yavaş:** başsız Chrome'da Wasm yapısı her iki örnekte de daha uzun sürdü ve daha çok tuttu.
    Faz 14'ün Wasm ölçümü (uygulamanın tarayıcı bölmesinde, ekran görüntüleriyle: JSON 2,0 s, CSV 10 s) başka bir
    yoldan alındı; iki tablo birbiriyle karşılaştırılmıyor. Okuma iki yapıda da ana iş parçacığında, sayfa o sürece donuyor
    (`OPEN_ITEMS.md`, "Büyük belgelerde ölçülüp bırakılanlar").

