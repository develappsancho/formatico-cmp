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
*   **Masaüstü:** 50 MB yalnızca JVM ölçümüyle; masaüstü uygulamasının penceresinde 50 MB'lık bir dosya açılmadı. 50
    MB'lık bir CSV okunmuşuyla ve ağacıyla 2 GB'ı geçtiği için uygulama makinenin belleğinin yarısını alıyor
    (`-XX:MaxRAMPercentage=50`); JVM'in varsayılanı dörtte biri, 8 GB'lık bir makinede 2 GB.
