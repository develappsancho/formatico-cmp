# Yayın rehberi

Formatico'nun web uygulaması ve sunucusu böyle yayına alınır: önce bu makinede Docker Compose ile, sonra Coolify'da.
Düzen pantrixui.com'un sitesininkiyle aynıdır; neyin nerede çalıştığı ve neden böyle kurulduğu PLAN.md'nin Faz 16'sında,
"Barındırma" maddesindedir. Android'in APK'sı, macOS'un DMG'si ve Windows'un MSI'ı deponun GitHub'daki sürümünde
yayımlanır (Bölüm 6–9); MSI, bir Windows makinesinde yapılır.

## 1. Özet

| Komut | Ne yapar |
| :--- | :--- |
| `deploy/deploy.sh local` | Web uygulamasını ve sunucuyu derler, iki imajı yapar, yığını `http://localhost:4005`'te kaldırır ve denetler |
| `deploy/deploy.sh local --no-gradle` | Aynısını Gradle'ın son çıktısıyla yapar: yalnızca `deploy/` değiştiğinde |
| `deploy/deploy.sh stop` | Yığını durdurur ve konteynerlerini kaldırır; imajlar ve paylaşımların diski kalır |
| `deploy/check.sh <adres>` | Denetimleri bir adreste çalıştırır: lokalde ya da canlıda |
| `deploy/deploy.sh web` | İki imajı iki mimari için GHCR'ye gönderir, Coolify'a yayını başlatır ve canlı adresi denetler |
| `deploy/deploy.sh desktop` | macOS uygulamasını derler, Developer ID ile imzalar, Apple'ın onayından geçirir: `build/release/Formatico-macOS.dmg` |
| `deploy/deploy.sh android` | Android uygulamasını yayın anahtarıyla imzalı derler, imzasını denetler: `build/release/Formatico-Android.apk` |
| `deploy/deploy.sh release` | GitHub'da taslak bir sürüm açar, DMG'yi ve APK'yı koyar; taslağı yalnızca deponun ortakları görür |
| `deploy/windows.sh` | Windows'ta, Git Bash'te: MSI'ı derler ve sürüme koyar, taslağa ya da yayımlanmışa (Bölüm 8) |
| `deploy/deploy.sh publish` | DMG ve APK taslaktaysa sürümü yayımlar, son sürüm yapar; MSI sonra da gelebilir |

`web`'in seçenekleri:

*   `--dry-run` her şeyi derler, iki imajı iki mimari için yapar ve GHCR'ye ya da Coolify'a hiçbir şey göndermez.
*   `--no-gradle` Gradle'ın son çıktısını alır.
*   `--no-coolify` imajları gönderir ve Coolify'a dokunmaz; ilk gönderim böyle yapılır (Bölüm 5.1).

`desktop`'ta `--dry-run` DMG'yi imzalar ve Apple'a göndermez; `release`'te notları yazar ve hiçbir şey açmaz.

Sürüm, `gradle.properties`'teki `formatico.version`'dır (`1.0.0`). Uygulama onu Ayarlar'ın About satırında yazar;
imajların etiketi, sürümün adı (`v1.0.0`) ve üç uygulamanın sürümü de odur.

## 2. Gerekenler

*   **Docker Desktop**, açık. `docker compose` ve iki mimari için derleyen `buildx` onunla gelir.
*   **JDK** ve Gradle'ın kendisi (`./gradlew`).
*   **GitHub Packages'ın anahtarı:** Pantrix UI GitHub Packages'tan gelir. `~/.gradle/gradle.properties`'te
    `GitHubPackagesUsername` ve `read:packages` yetkili bir token olarak `GitHubPackagesPassword` (README, "Setting
    up").
*   **Canlıya yayın için** (Bölüm 5):
    *   GitHub'ın komut satırı aracı `gh`, `develappsancho` hesabıyla açık ve `write:packages` izniyle.
    *   Coolify'ın adresi, API anahtarı ve Formatico'nun kaynağının UUID'si `~/.gradle/gradle.properties`'te:
        `CoolifyUrl`, `CoolifyToken`, `CoolifyFormaticoUuid`.
    *   `python3`: betik Coolify'ın yanıtlarını onunla okur; macOS'ta vardır.

## 3. Lokalde yayın

```bash
deploy/deploy.sh local
```

Komut dört şey yapar:

1.  **Gradle:** `:app:webApp:wasmJsBrowserDistribution` web uygulamasının üretim çıktısını yazar
    (`app/webApp/build/dist/wasmJs/productionExecutable`). `:app:webApp:copyJsApp` aynı klasöre JavaScript sürümünü
    ekler: Wasm'ı çalıştıramayan bir tarayıcı için. `:server:bootJar` sunucunun jar'ını yapar
    (`server/build/libs/server.jar`). İlk derleme birkaç dakika sürer.
2.  **Docker:** İki imajı yapar. Adları, derlendikleri commit'in kısa karmasıdır; ağaçta commit'lenmemiş bir değişiklik
    varsa sonuna `-dirty` eklenir: `ghcr.io/develappsancho/formatico-web:1658d25-dirty`.
3.  **Compose:** Yığını kaldırır ve iki konteyner de sağlıklı (`healthy`) olana kadar bekler.
4.  **Denetim:** `check.sh`'ı `http://localhost:4005`'te çalıştırır (Bölüm 4).

Sonunda Formatico `http://localhost:4005`'tedir; yalnızca bu makineden açılır. Başka bir port için:

```bash
FORMATICO_PORT=4100 deploy/deploy.sh local
```

Lokal yığının sunucusu, tarayıcının o porttan gelen isteklerini kabul eder (`compose.local.yaml`,
`FORMATICO_ALLOWEDORIGINS`). Masaüstü uygulaması bu yığına `-Dformatico.server=http://localhost:4005` ile bağlanır.

Konteynerlerin günlükleri:

```bash
docker compose -f deploy/compose.yaml -f deploy/compose.local.yaml logs -f
```

Durdurmak:

```bash
deploy/deploy.sh stop
```

## 4. Denetimler

`check.sh`, yığını bir tarayıcının ulaştığı gibi dener. Her denetim bir satır yazar; biri başarısız olursa betik de
başarısız döner.

| Denetim | Denenen |
| :--- | :--- |
| Uygulamanın sayfası | Ana sayfa Formatico'nunki |
| Başlıklar | Sayfada `default-src 'none'` ile başlayan içerik güvenliği politikası; API'nin yanıtlarında da politika ve `nosniff` (`deploy/web/headers.conf`) |
| HTTPS'e bağlılık | HTTPS'te sayfa ve API `Strict-Transport-Security` gönderiyor; düz HTTP'de göndermiyor, ama önündeki vekil `X-Forwarded-Proto: https` derse gönderiyor |
| Sunucunun sağlığı | `/api/health` `ok` diyor |
| Paylaşım | Şifreli bir paylaşım kaydediliyor, olduğu gibi geri veriliyor; başka bir anahtarla silinemiyor, kendi anahtarıyla siliniyor, sonra yok |
| Vekil | Sunucu kendi makinesine (`127.0.0.1`) ve bulutun bilgi adresine (`169.254.169.254`) gitmiyor: 400 |

Herhangi bir adreste çalışır:

```bash
deploy/check.sh https://formatico.pantrixui.com
```

## 5. Coolify'da yayın

İmajlar bu makinede derlenir ve GHCR'ye gönderilir. Coolify depodaki `deploy/compose.yaml`'ı okur, imajları çeker ve
yığını kaldırır; orada hiçbir şey derlenmez. Önlerinde Coolify'ın Traefik'i durur: HTTPS ve alan adı. Traefik isteğin
HTTPS ile geldiğini `X-Forwarded-Proto`'da söyler; nginx o zaman `Strict-Transport-Security: max-age=31536000`
gönderir, tarayıcı da bir yıl boyunca adrese yalnızca HTTPS ile gider (`deploy/web/default.conf`).

### 5.1 Bir kerelik kurulum

1.  **GHCR'ye gönderme izni.** İmajlar `gh` hangi hesapla açıksa onun adına gider: `develappsancho`. İzin bir kez
    eklenir; sitenin yayınından zaten varsa bu adım atlanır:

    ```bash
    gh auth refresh -h github.com -s write:packages
    ```

2.  **Coolify'ın API anahtarı.** Sitenin yayınından `CoolifyUrl` ve `CoolifyToken` zaten
    `~/.gradle/gradle.properties`'teyse aynıları kullanılır. Değilse Coolify'da **Keys & Tokens** → **API Tokens**'tan
    `read` ve `deploy` yetkili bir anahtar yaratılır ve yazılır; depoya girmez:

    ```properties
    CoolifyUrl=https://coolify.ornek-alan-adi.com
    CoolifyToken=…
    ```

3.  **İlk gönderim.** Coolify'daki kaynak, imajlar GHCR'de olmadan yayımlayamaz. İlk gönderim Coolify'a dokunmadan
    yapılır. Betik, son commit'in GitHub'da olmasını şart koşar:

    ```bash
    deploy/deploy.sh web --no-coolify
    ```

4.  **Paketleri herkese açık yapmak.** İki paket, `formatico-web` ve `formatico-server`, gizli olarak açılır. İmajların
    dizinindeki `org.opencontainers.image.source` bilgisiyle depoya kendiliğinden bağlanırlar. Her biri için GitHub'da
    paketin sayfası → **Package settings** → **Danger Zone** → **Change visibility** → **Public**. Yoksa Coolify
    imajları çekemez.
5.  **Coolify'da kaynak.** **Projects**'te projenin ortamı → **+ New** → **Public Repository**:

    | Alan | Değer |
    | :--- | :--- |
    | Repository URL | `https://github.com/develappsancho/formatico-cmp` |
    | Branch | `main` |
    | Build Pack | Docker Compose |
    | Base Directory | `/` |
    | Docker Compose Location | `/deploy/compose.yaml` |
    | `formatico-web`'in alan adı | `https://formatico.pantrixui.com` |
    | `formatico-server`'ın alan adı | Boş: ona yalnızca nginx ulaşır |
    | Connect To Predefined Network | **Kapalı** |

    Ortam değişkeni gerekmez: sunucu, tarayıcının yalnızca `https://formatico.pantrixui.com`'dan gelen isteklerini
    kabul eder (`formatico.allowed-origins`). "Connect To Predefined Network" açılırsa yığın, Coolify'ın başka
    kaynaklarla paylaştığı ağa da bağlanır. nginx sunucuya yığının kendi ağından ulaşır; ortak ağa gerek yoktur.

    **Save** ile kaydedilir; ilk yayını betik başlatır.

6.  **Kaynağın UUID'si.** Panelde kaynağın adresinin son parçasıdır: `…/application/<uuid>`. Kaynağın adının
    sonundaki kimlik UUID değildir: ikisi ayrı dizilerdir. UUID de `~/.gradle/gradle.properties`'e yazılır:

    ```properties
    CoolifyFormaticoUuid=…
    ```

7.  **DNS.** `formatico.pantrixui.com`'un A kaydı açık (2026-11-06). Sertifikayı Traefik ilk yayında Let's Encrypt'ten
    alır.

### 5.2 Her yayın

Bir yayın, GitHub'daki bir commit'ten yapılır. Ağaçta commit'lenmemiş bir değişiklik varsa ya da yerel `main`
GitHub'daki `main` değilse betik başlamaz.

1.  Yeni bir sürümse `gradle.properties`'te `formatico.version` artırılır. Aynı sürümle gönderilen imajlar o sürümün
    etiketinin üstüne yazılır.
2.  Commit ve push.
3.  Yayın:

    ```bash
    deploy/deploy.sh web
    ```

`web` şunları yapar:

1.  Web uygulamasını ve sunucuyu Gradle'la derler.
2.  GHCR'ye `gh`'nin anahtarıyla girer. İki imajı `linux/amd64` ve `linux/arm64` için yapar ve üç etiketle gönderir:
    sürüm (`1.0.0`), commit (`1658d25`) ve `latest`. Coolify'ın makinesi kendi mimarisininkini çeker. İmajların
    dizininde deponun adresi ve lisans (`Apache-2.0`) yazar.
3.  Coolify'a yayını POST ile sorar, her 10 saniyede durumunu okur ve bitmesini bekler, en çok 20 dakika.
4.  Ana sayfa 200 dönene dek bekler, en çok iki dakika: Coolify yayını konteynerler başlayınca bitmiş sayar, Traefik
    yeni yığın istek alana dek bir süre 503 dönebilir. Sonra `check.sh`'ı canlı adreste çalıştırır.

Önce denemek: her şey derlenir, hiçbir yere bir şey gönderilmez.

```bash
deploy/deploy.sh web --dry-run
```

Canlıyı yeniden denetlemek:

```bash
deploy/check.sh https://formatico.pantrixui.com
```

### 5.3 Bir sürüme dönmek

Compose dosyası imajları `${FORMATICO_TAG:-latest}` ile çeker. Coolify'da kaynağın ortam değişkenlerine
`FORMATICO_TAG=1.0.0` (ya da bir commit'in kısa karması) eklenip yeniden yayımlanınca o sürüm çalışır. Değişken
silinince yığın yine `latest`'e döner. Paylaşımlar `shares` diskinde durur; bir sürüme dönmek onlara dokunmaz.

### 5.4 Alan adı değişirse

1.  Coolify'da `formatico-web`'in alan adı.
2.  Sunucu: Coolify'da `FORMATICO_ALLOWEDORIGINS=https://yeni-alan-adi.com`, ya da `application.properties`'te
    `formatico.allowed-origins`.
3.  Uygulamalar: bağlantıların ve sunucunun adresi `ShareLinks.SITE`'tadır (`core/share`). Web kendi adresini
    kullanır; Android, iOS ve masaüstü yeniden derlenip yayımlanır.

### 5.5 Sorun giderme

| Belirti | Neden ve çözüm |
| :--- | :--- |
| `Docker is not running` | Docker Desktop'ı açın, komutu yeniden çalıştırın |
| `port is already allocated` | 4005'i başka bir şey kullanıyor: `FORMATICO_PORT=4100 deploy/deploy.sh local` |
| `the tree has changes that are not committed` | Ağaçta commit'lenmemiş bir değişiklik var |
| `main on GitHub is not this commit` | Son commit push edilmemiş |
| `gh is signed in as …` | `gh` başka bir hesapla açık: `gh auth switch` |
| `gh may not write packages` | 5.1'deki izin eksik: `gh auth refresh -h github.com -s write:packages` |
| `CoolifyFormaticoUuid is not in …` | 5.1, adım 6 yapılmamış |
| Coolify'ın `401` ya da `403` yanıtı | Anahtar yanlış ya da `deploy` yetkisi yok |
| Coolify'ın `404` yanıtı | `CoolifyFormaticoUuid` kaynağın adındaki kimlik; adresin son parçası olmalı |
| Coolify'ın `405` yanıtı | Coolify 4.5'ten beri yayın POST ile istenir; betiğin eski bir kopyası GET gönderiyor |
| Coolify'ın `503` yanıtı ("This site is not available at the moment"), panel de alan adından açılmıyor | Vekilin panel için kullandığı dosya (`/data/coolify/proxy/dynamic/coolify.yaml`) yok. Panele SSH tüneliyle girin (`ssh -L 8000:localhost:8000 …`, sonra `http://localhost:8000`); Settings'te Domain'i yeniden yazıp Save'e basın. Dosya geri gelir, vekil kendisi okur (2026-11-13) |
| Coolify imajı çekemiyor | Paket hâlâ gizli (5.1, adım 4) |
| `… has no domain on formatico-web` | Kaynakta `formatico-web`'in alan adı boş (5.1, adım 5) |
| `… still answers 503 after two minutes` | Yığın istek almıyor: Coolify'da kaynağın günlüklerine bakın |
| Tarayıcıda paylaşım CORS hatası veriyor | Sunucu o adresi tanımıyor: `FORMATICO_ALLOWEDORIGINS` (5.4) |
| Yeni bir değişiklik görünmüyor | `--no-gradle` eski çıktıyı kullanır; komutu onsuz çalıştırın |
| Eski imajlar yer kaplıyor | `docker image ls 'ghcr.io/develappsancho/formatico-*'` ile bakın, `docker image rm` ile silin |

## 6. Android'in APK'sı

APK, sahibinin yayın anahtarıyla imzalanır. Anahtarın dosyası ve parolaları depoya girmez: `local.properties`'te
dört satırdır.

| Anahtar | Değer |
| :--- | :--- |
| `storeFile` | Anahtar dosyasının deponun köküne göre yolu, ör. `signing/…jks` |
| `storePassword` | Dosyanın parolası |
| `keyAlias` | Anahtarın takma adı |
| `keyPassword` | Anahtarın parolası |

`.gitignore`, `signing/`, `*.jks` ve `*.keystore`'u yok sayar. Yok sayma yalnızca izlenmeyen dosyalara işler: bir kez
`git add` edilmiş bir anahtar dosyası commit'ten önce `git rm --cached <dosya>` ile çıkarılır.

İmzalı APK, `build/release/Formatico-Android.apk` olarak:

```bash
deploy/deploy.sh android
```

Betik `:app:androidApp:assembleRelease`'i çalıştırır, APK'nın sürümüne ve imzasına bakar (`aapt2`, `apksigner`,
Android SDK'nın `build-tools`'undan) ve hangi commit'ten derlendiğini yanına yazar. `local.properties`'te
`storeFile` yoksa Gradle APK'yı imzasız yapar (`androidApp-release-unsigned.apk`) ve betik durur. `versionCode`
sürümden hesaplanır: `1.0.0` → `10000`. Android 11 ve sonrası içindir (`minSdk` 30).

İmzanın sertifikası elle:

```bash
apksigner verify --print-certs build/release/Formatico-Android.apk
```

## 7. macOS'un DMG'si

```bash
deploy/deploy.sh desktop
```

Gerekenler, bu Mac'te bir kez:

*   Anahtar zincirinde ekibin (`FN6T94F922`) **Developer ID Application** sertifikası.
*   Apple'ın onayı için bilgiler, anahtar zincirinde `pantrix-notary` adıyla; sitenin yayını da aynısını kullanır.
    Yoksa: `xcrun notarytool store-credentials pantrix-notary`.

Betik şunları yapar (`deploy/desktop/macos-dmg.sh`, sitenin betiğinin Formatico'ya uyarlanmışı):

1.  Gradle `:app:desktopApp:createDistributable` ile `Formatico.app`'i yapar; sürümüne bakar. Uygulamanın Java ortamı
    yalnızca `app/desktopApp/build.gradle.kts`'in saydığı modülleri tutar: `deploy/desktop/runtime-modules-check.sh`,
    `jdeps`'in jar'larda bulduğu her modülün ortamda olduğuna bakar, eksik olanı sayıp durur.
2.  Uygulamayı sertifikanın karmasıyla imzalar: jar'ların içindeki yerel kütüphaneler, Java'nın çalışma ortamının
    kütüphaneleri ve çalıştırılabilirleri, ortamın kendisi, sonra uygulama, JVM'in üç hakkıyla
    (`deploy/desktop/macos-entitlements.plist`). Compose'un kendi imzası sertifikayı adıyla arar; adında ASCII dışı
    bir harf olunca bulamaz.
3.  DMG'yi yapar ve imzalar, Apple'a onaya gönderir (birkaç dakika), onayı DMG'ye ekler ve Gatekeeper'a sorar.
4.  DMG'yi bağlar ve içindeki uygulamayı açar (`deploy/desktop/macos-launch-check.sh`), kendine ait boş bir ev
    klasörüyle, bu Mac'te tutulan geçmişe ve ayarlara dokunmadan; anahtarlarını da anahtar zincirinde değil o
    klasörde tutar (`-Dksafe.jvm.keyVault=software`), kurulu uygulamanın anahtar zinciri kayıtlarına dokunmaz. Ana
    penceresi ekranda ve günlüğünde bir istisna yoksa geçer. Yalnızca bir hata penceresi açılırsa ya da bir istisna yazılırsa DMG'yi siler ve durur. Ayakta kalan
    bir süreç yetmez: başlatıcının hata penceresi de süreci ayakta tutar.
5.  `build/release/Formatico-macOS.dmg`'yi, boyutunu, SHA-256'sını ve commit'ini yazar.

1.0.0 bu iki denetim olmadan çıktı ve açılışta `java.net.http`'yi bulamadığı için durdu (2026-11-12): ortamında Ktor'un,
KSafe'in ve kotlinx.coroutines'in kullandığı dört modül yoktu.

Kurulu uygulama verisini `~/Library/Application Support/Formatico`'da, anahtarlarını anahtar zincirinde
`eu.anifantakis.ksafe.com.developersancho.formatico` adıyla tutar. Koddan çalıştırma (Gradle'ın `run`'ı, IDE) kendi
yerini kullanır: `Formatico Development` klasörü ve `….formatico.development` adı. Anahtar zinciri bir kaydı onu yapan
uygulamaya sormadan verir, başkasından giriş parolasını ister; 1.0.2'ye kadar ikisi de KSafe'in ortak adını
(`eu.anifantakis.ksafe.shared`) kullanıyordu ve koddan çalıştırmanın yaptığı kayıtlar kurulu uygulamaya her açılışta
parola sorduruyordu (2026-11-12). 1.0.2, eksik bir anahtarı ortak addan bir kez okuyup kendi adına yazar: eski
kurulumun geçmişi kalır, bunun için bir kez parola sorulur.

DMG Apple silikonlu Mac'ler içindir: içindeki Java ortamı bu Mac'inkidir (arm64). `--dry-run` imzalar, Apple'a
göndermez; böyle bir DMG sürüme konmaz (Gatekeeper durdurur) ve `release` onu kabul etmez.

## 8. Windows'un MSI'ı

Compose bir MSI'ı yalnızca Windows'ta yapar. MSI, deponun ortağı olan bir arkadaşın Windows makinesinde, sahibinin
Mac'te açtığı sürümün commit'inden yapılır ve o sürüme konur: sürüm taslakken ya da yayımlandıktan sonra, arkadaşın
makinesi hangi gün hazırsa. Sürüm MSI'ı beklemez; indirme menüsü Windows'u MSI sürüme konduğunda gösterir. MSI
imzasızdır: Windows ilk açılışta "Windows
bilgisayarınızı korudu" der (SmartScreen); **Ek bilgi** → **Yine de çalıştır**. Kurulum yalnızca kullanıcı içindir,
yönetici izni istemez; bir sonraki sürüm aynı yükseltme koduyla (`app/desktopApp/build.gradle.kts`, `upgradeUuid`)
eskisinin yerine kurulur.

### 8.1 Bir kerelik: sahibi

GitHub'da depo → **Settings** → **Collaborators** → **Add people**: arkadaşın hesabı, **Write** yetkisiyle. Taslak
sürümü yalnızca ortaklar görür; bir sürüme, taslak ya da yayımlanmış, yalnızca yazma yetkisi olan dosya koyabilir.

Deponun ayarlarında **Immutable releases** kapalı kalır (bugün kapalı). Açılırsa yayımlanmış bir sürüme dosya
eklenemez: MSI ancak sürüm taslakken gelebilir, betik de bunu söyleyip durur.

### 8.2 Bir kerelik: arkadaşın makinesi

0.  **Davet:** GitHub'ın e-postasındaki ya da `github.com/notifications`'daki daveti kabul edin; kabul edilmeden
    hesap depoya yazamaz.
1.  **Araçlar** (PowerShell'de):

    ```powershell
    winget install Git.Git
    winget install EclipseAdoptium.Temurin.21.JDK
    winget install GitHub.cli
    ```

    Git, Git Bash'i getirir; betik orada çalışır. JDK Gradle'ı başlatmak içindir: derlemenin JDK'sını (Azul Zulu 21)
    ve MSI'ın araçlarını (WiX) Gradle ilk derlemede kendisi indirir. Android SDK ya da Xcode gerekmez (Android
    SDK'sız bir kopyada masaüstü derlemesi denendi, 2026-11-13). Kurulumdan sonra Git Bash'i kapatıp yeniden açın:
    yeni araçlar ancak öyle bulunur.
2.  **GitHub'a giriş** (Git Bash'te): `gh auth login`, ortak yapılan hesapla.
3.  **Pantrix UI'ın paketleri:** GitHub Packages, herkese açık bir paket için bile bir token ister. GitHub'da
    **Settings** → **Developer settings** → **Personal access tokens** → **Tokens (classic)** → yalnızca
    `read:packages` yetkili bir token. Kullanıcının klasöründeki `.gradle\gradle.properties`'e (Git Bash'te
    `~/.gradle/gradle.properties`) iki satır; depoya girmez:

    ```properties
    GitHubPackagesUsername=<GitHub kullanıcı adı>
    GitHubPackagesPassword=<read:packages yetkili token>
    ```

4.  **Depo** (Git Bash'te), kısa bir klasöre: Windows'un yol sınırı (260 karakter) derlemenin derin klasörlerine
    takılmasın.

    ```bash
    git config --global core.longpaths true
    mkdir -p /c/src && cd /c/src
    git clone https://github.com/develappsancho/formatico-cmp.git
    ```

`.gitattributes` betiklerin ve Gradle'ın sarmalayıcısının satır sonlarını Windows'ta da LF tutar; yoksa Git for Windows
onları CRLF yazar ve bash çalıştıramaz.

### 8.3 Her sürüm: arkadaş

Sahibi sürümü açtıktan sonra (Bölüm 9), taslakken ya da yayımlandıktan sonra, istenen gün, Git Bash'te deponun
kökünde:

```bash
git fetch origin --tags
git checkout v<sürüm>
deploy/windows.sh
```

Betik önce bakar: Windows mı, `formatico.version`, Java, iki `GitHubPackages` satırı, temiz ağaç, `gh`'nin hesabının
depoya yazabilmesi, `v<sürüm>` sürümü. Ağaç sürümün commit'inde değilse durur ve hangi commit'e geçileceğini söyler
(`git checkout <commit>`): taslağın hedefi, yayımlanmış sürümde etiketin commit'i. Sonra Gradle
`:app:desktopApp:packageMsi` ile MSI'ı yapar, uygulamanın Java ortamının jar'ların kullandığı her modülü tuttuğuna bakar
(`deploy/desktop/runtime-modules-check.sh`, Mac'tekinin aynısı) (ilk derleme JDK'yı ve WiX'i indirdiği için uzun sürer),
`build/release/Formatico-Windows.msi` olarak sürüme koyar ve notların Windows bölümünü SHA-256'sıyla yazar. Yeniden
çalıştırılırsa MSI'ı ve bölümü yenisiyle değiştirir.

`deploy/windows.sh --dry-run` MSI'ı bulunulan commit'ten yapar ve hiçbir yere göndermez: makinenin hazır olduğunu
denemek için.

## 9. GitHub'daki sürüm

Bir sürüm GitHub'daki bir commit'ten, sırayla yapılır:

1.  **Sahibi, Mac'te:** commit ve push, sonra

    ```bash
    deploy/deploy.sh desktop
    deploy/deploy.sh android
    deploy/deploy.sh release
    ```

    `release`, iki dosyanın bu commit'ten derlendiğine bakar ve GitHub'da `v<sürüm>` adıyla **taslak** bir sürüm açar:
    hedefi bu commit, dosyaları DMG ve APK, notları her dosyanın platformu ve SHA-256'sı. Taslak zaten varsa
    dosyalarını ve notlarını yeniler; sürüm yayımlanmışsa durur: yeni bir sürüm yeni bir `formatico.version` ister.
    `deploy/deploy.sh release --dry-run` yalnızca notları yazar.
2.  **Sahibi:** sürüm, sahibinin onayıyla yayımlanır:

    ```bash
    deploy/deploy.sh publish
    ```

    Taslakta DMG ya da APK yoksa durur; ikisi varsa sürümü yayımlar ve son sürüm yapar. MSI henüz yoksa bunu söyler ve
    yine yayımlar.
3.  **Arkadaş, Windows'ta, istediği gün:** `deploy/windows.sh` (Bölüm 8.3). 2'den önce de olabilir, sonra da: MSI
    taslağa ya da yayımlanmış sürüme konur.

Dosyaların adı her sürümde aynıdır; bu yüzden `releases/latest/download/<ad>` hep son sürümün dosyasını verir:

*   `https://github.com/develappsancho/formatico-cmp/releases/latest/download/Formatico-macOS.dmg`
*   `https://github.com/develappsancho/formatico-cmp/releases/latest/download/Formatico-Android.apk`
*   `https://github.com/develappsancho/formatico-cmp/releases/latest/download/Formatico-Windows.msi`

### 9.1 Sorun giderme

| Belirti | Neden ve çözüm |
| :--- | :--- |
| `The keychain has no Developer ID Application certificate` | Sertifika bu Mac'in anahtar zincirinde değil |
| `Apple did not notarize the DMG` | Betik Apple'ın günlüğünü yazar; çoğu zaman imzasız kalmış bir kütüphane |
| `no signed APK` | `local.properties`'te anahtarın satırları yok (Bölüm 6) |
| `… was built from …, not from …` | Dosya başka bir commit'ten: `desktop` ya da `android` yeniden |
| `gh's account may not write` | `gh` başka bir hesapla açık, ya da hesap deponun ortağı değil (8.1) |
| `there is no release v… on GitHub yet` | Sürüm henüz açılmadı: sahibinin `release`'i |
| `is no draft release` (`publish`) | Taslak açılmadı, ya da sürüm zaten yayımlandı |
| `is immutable` | Deponun ayarlarında Immutable releases açık (8.1): MSI, sürüm taslakken konur |
| `this is not the release's commit` | Arkadaşın ağacı başka bir commit'te: betiğin söylediği `git checkout` |
| `GitHubPackagesPassword is not in …` | 8.2, adım 3 |
| Windows'ta `bash\r: No such file or directory` | Depo `.gitattributes`'tan önce klonlanmış: yeniden klonlamak |
| `The app's runtime lacks modules its jars use: …` | Bir kütüphane yeni bir Java modülü kullanıyor: `app/desktopApp/build.gradle.kts`'te `modules(...)`'a eklemek |
| `Formatico did not come up` | DMG'deki uygulama açılmadı: betik pencerelerini ve günlükteki istisnayı yazar; DMG tutulmaz |
| `jdeps is not on the PATH` | JDK'nın `bin`'i PATH'te değil (Windows'ta Temurin'in kurulumu ekler; Git Bash yeniden açılır) |
