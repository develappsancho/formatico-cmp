# Yayın rehberi

Formatico'nun web uygulaması ve sunucusu böyle yayına alınır: önce bu makinede Docker Compose ile, sonra Coolify'da.
Düzen pantrixui.com'un sitesininkiyle aynıdır; neyin nerede çalıştığı ve neden böyle kurulduğu PLAN.md'nin Faz 16'sında,
"Barındırma" maddesindedir. Android'in APK'sı ve macOS'un DMG'si deponun GitHub'daki sürümünde yayımlanır (Bölüm 6
ve 7).

## 1. Özet

| Komut | Ne yapar |
| :--- | :--- |
| `deploy/deploy.sh local` | Web uygulamasını ve sunucuyu derler, iki imajı yapar, yığını `http://localhost:4005`'te kaldırır ve denetler |
| `deploy/deploy.sh local --no-gradle` | Aynısını Gradle'ın son çıktısıyla yapar: yalnızca `deploy/` değiştiğinde |
| `deploy/deploy.sh stop` | Yığını durdurur ve konteynerlerini kaldırır; imajlar ve paylaşımların diski kalır |
| `deploy/check.sh <adres>` | Denetimleri bir adreste çalıştırır: lokalde ya da canlıda |
| `deploy/deploy.sh web` | İki imajı iki mimari için GHCR'ye gönderir, Coolify'a yayını başlatır ve canlı adresi denetler |

`web`'in seçenekleri:

*   `--dry-run` her şeyi derler, iki imajı iki mimari için yapar ve GHCR'ye ya da Coolify'a hiçbir şey göndermez.
*   `--no-gradle` Gradle'ın son çıktısını alır.
*   `--no-coolify` imajları gönderir ve Coolify'a dokunmaz; ilk gönderim böyle yapılır (Bölüm 5.1).

Sürüm, `gradle.properties`'teki `formatico.version`'dır (`1.0.0`). Uygulama onu Ayarlar'ın About satırında yazar;
imajların etiketi de odur.

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
| Sunucunun sağlığı | `/api/health` `ok` diyor |
| Paylaşım | Şifreli bir paylaşım kaydediliyor, olduğu gibi geri veriliyor; başka bir anahtarla silinemiyor, kendi anahtarıyla siliniyor, sonra yok |
| Vekil | Sunucu kendi makinesine (`127.0.0.1`) ve bulutun bilgi adresine (`169.254.169.254`) gitmiyor: 400 |

Herhangi bir adreste çalışır:

```bash
deploy/check.sh https://formatico.pantrixui.com
```

## 5. Coolify'da yayın

İmajlar bu makinede derlenir ve GHCR'ye gönderilir. Coolify depodaki `deploy/compose.yaml`'ı okur, imajları çeker ve
yığını kaldırır; orada hiçbir şey derlenmez. Önlerinde Coolify'ın Traefik'i durur: HTTPS ve alan adı.

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

İmzalı APK:

```bash
./gradlew :app:androidApp:assembleRelease
```

Çıktı `app/androidApp/build/outputs/apk/release/androidApp-release.apk`'tır. `local.properties`'te `storeFile`
yoksa APK imzasız çıkar (`androidApp-release-unsigned.apk`). `versionCode` sürümden hesaplanır: `1.0.0` → `10000`.

İmzayı denetlemek (Android SDK'nın `build-tools`'unda):

```bash
apksigner verify --print-certs app/androidApp/build/outputs/apk/release/androidApp-release.apk
```

## 7. macOS'un DMG'si ve GitHub'daki sürüm

Henüz betikte yok: `deploy.sh desktop` eklenecek. Uygulamayı Developer ID ile imzalayacak, Apple'ın onayından
(notarization) geçirecek ve DMG'yi sürümün GitHub sayfasına koyacak. Bu bölüm o zaman yazılacak.

GitHub'da bir sürüm, sahibinin onayıyla yayımlanır.
