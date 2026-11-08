# Formatico

Formatico formats, validates, explores and converts structured text: JSON and its kin, XML, YAML, TOML, CSV and
properties. It is a Compose Multiplatform app for Android, iOS, the desktop and the web, built with
[Pantrix UI](https://pantrixui.com).

## Modules

| Module | What it holds |
| :--- | :--- |
| [`app/shared`](app/shared) | The app every platform draws: `App()`, in Formatico's theme, with its bar, Koin and the navigation3 back stack |
| [`app/androidApp`](app/androidApp), [`app/desktopApp`](app/desktopApp), [`app/webApp`](app/webApp), [`app/iosApp`](app/iosApp) | Each platform's entry point |
| [`core/format`](core/format) | Pure Kotlin: the formats, their lossless parsers, format detection, problems with their line and column, JSON formatted, minified and with its keys sorted, YAML read through snakeyaml-engine-kmp and written by Formatico's own printer, CSV and TSV read and written by Formatico's own reader and printer, JSONPath |
| [`core/schema`](core/schema) | A document validated against a JSON Schema, drafts 4 to 2020-12, by optimumcode's json-schema-validator: its errors as Formatico's problems, each with its path and the keyword it fails, an `anyOf` or a `oneOf` none of whose schemas matched as one; why a schema cannot be used (a draft not known, a `$ref` to nothing, not a schema of its draft) |
| [`core/base`](core/base) | The bases every feature builds on: `MviViewModel`, `MviScreen` |
| [`core/storage`](core/storage) | What is kept on the device: the small values in KSafe, the documents' texts encrypted (AES-GCM, KSafe's key) as files, and in a browser in IndexedDB |
| [`core/testing`](core/testing) | What the tests share: waiting while a browser runs, a preview's screenshot, an open menu's |
| [`core/share`](core/share) | Pure Kotlin: a document in a link (`#d=`, zlib in Base64), an encrypted link (`#s=` and its key, `&k=`), the envelope a document is encrypted in, and what the app and the server agree on (`ShareServer`) |
| [`core/network`](core/network) | What the app reaches on the network, on Ktor's client (OkHttp, `NSURLSession`, Java's, `fetch`): Formatico's server, the encrypted shares (AES-GCM on the device, a key of each share's own), the documents at web addresses |
| [`core/navigation`](core/navigation) | A browser tab's history and address kept with the app's screens (adapted from navigation3-browser, MIT) |
| [`core/designsystem`](core/designsystem) | Formatico's look over Pantrix UI: `FormaticoPreset` (Teal), Geist and Geist Mono, the syntax colours, `FormaticoTheme` |
| [`feature/start`](feature/start) | The Start screen: paste, open, drop, recent documents and samples |
| [`feature/workspace`](feature/workspace) | The open documents and their workspace: the toolbar, the tree with its value previews, the table of an array or a CSV (sorting, filtering, columns, a column summed up, saved as CSV), the graph of its objects and arrays as cards and links (Pantrix's `PantrixGraph`), the status bar; a document's types (Kotlin, TypeScript, Swift, Go, JSON Schema) and its validation against a schema |
| [`server`](server) | Formatico's server, Spring Boot on WebFlux: the encrypted shares it keeps and cannot read, and web addresses fetched for a browser, public ones alone |
| [`deploy`](deploy) | The stack: nginx with the web app and the server beside it, in Docker; `deploy/deploy.sh local` runs it on this machine at `localhost:4005`, `deploy/check.sh` checks it |
| [`build-logic`](build-logic) | The convention plugins, as Kotlin classes: `formatico.kmp.library`, `formatico.kmp.compose`, `formatico.feature`, `formatico.quality`, `formatico.web.application` |

## Setting up

Pantrix UI comes from GitHub Packages, which asks for a token even for a public package. Put these two lines in
`~/.gradle/gradle.properties`, never in the project; the token needs `read:packages`:

```properties
GitHubPackagesUsername=<your GitHub user name>
GitHubPackagesPassword=<a classic personal access token with read:packages>
```

Claude Code gets Pantrix UI's skill and MCP server from the plugin declared in `.claude/settings.json`; a new session
loads it.

## Running

- Android: `./gradlew :app:androidApp:assembleDebug`
- Desktop: `./gradlew :app:desktopApp:run`
- Web, Wasm: `./gradlew :app:webApp:wasmJsBrowserDevelopmentRun`
- Web, JavaScript: `./gradlew :app:webApp:jsBrowserDevelopmentRun`
- Web, both builds and the loader that picks one: `./gradlew :app:webApp:wasmJsBrowserDistribution`
- iOS: open [`app/iosApp`](app/iosApp) in Xcode and run it.
- The server: `FORMATICO_SHARES_DIRECTORY=build/shares ./gradlew :server:bootRun`, at `localhost:8080`, its shares
  in a folder it can write; a desktop app run against it takes `-Dformatico.server=http://localhost:8080`.
- The whole stack, the web app behind nginx and the server: `deploy/deploy.sh local` (Docker), at `localhost:4005`.

## Checking

- Tests: `./gradlew jvmTest` (and `iosSimulatorArm64Test`, `wasmJsBrowserTest`)
- Detekt, with Pantrix UI's rules: `./gradlew detekt`
- Kotlinter: `./gradlew lintKotlin`, and `./gradlew formatKotlin` to fix the form
- Characters that cannot be seen, in the Kotlin sources: `./gradlew checkInvisibleCharacters` (part of `check`)
- Screenshots on the desktop (Roborazzi): `./gradlew verifyRoborazziJvm` compares against each module's
  `screenshots/`, `./gradlew recordRoborazziJvm` writes them again. A module's are its previews, every case
  ([`feature/start/screenshots`](feature/start/screenshots), [`feature/workspace/screenshots`](feature/workspace/screenshots));
  [`app/shared/screenshots`](app/shared/screenshots) has the app on every board of the canvas at its three widths,
  light, dark and in Turkish (`-tr`), and its bar's previews. Each board is drawn with Pantrix's diagnostics on and
  has to find nothing
- The boards as iOS and Android draw them: `./gradlew :app:shared:verifyRoborazziIosSimulatorArm64` (the simulator,
  [`screenshots/ios`](app/shared/screenshots/ios)) and `./gradlew :app:shared:verifyRoborazziAndroidHostTest`
  (Robolectric, [`screenshots/android`](app/shared/screenshots/android)); `record…` writes them again
- Accessibility: `BoardAccessibilityTest` takes the app to every board at every width on the JVM, iOS and Wasm, and
  asserts that Pantrix's diagnostics found nothing (a control without a name, words too faint, a touch target
  squeezed, a tag on two nodes)
- The words in English and in Turkish: each feature keeps them in Kotlin, in its own `strings` package (the workspace
  one per screen), an interface and an object of it for each language; the theme's language chooses which
  (`inTheLanguage`). The compiler keeps the two languages' words and their values together, and each module's
  `StringsTest` that no Turkish word is still the English one
- In a browser: `./gradlew :app:webTest:test` builds the web distribution, serves it on this machine and drives this
  machine's Chrome through its DevTools (`-Pformatico.chrome=<path>` for another): the loader's choice of build, the
  tab's history and address, shared links, a reload, a phone's width, Turkish
- The types Types writes, by each language's compiler: `./gradlew :core:format:jvmTest -Pformatico.go=<go>
  -Pformatico.tsc=<tsc> -Pformatico.swiftc=/usr/bin/swiftc` (`TypesCompileTest`). Go's decode the documents with no
  field left over and write them back as they were, TypeScript's take them as they are (`--strict`), Swift's decode
  and write them; a compiler not given is skipped. Kotlin's are compiled in `feature:workspace`'s tests, on every
  target, and read their document with kotlinx.serialization
- Previews: every screen and component has them beside it (`StartPreviews.kt`, `WorkspacePreviews.kt`,
  `AppPreviews.kt`), each case in the light theme and the dark, a screen at each board's width
- The app icons, from the canvas's icon board: `python3 tools/icons/generate.py` (fontTools and Pillow; iconutil
  on macOS) writes them into each app's folders
- On a device (Maestro, installed apart): install the debug build, then
  `maestro --device <id> test -e APP_ID=com.developersancho.formatico -e SHOTS=<device> app/maestro/flows`.
  A flow for each board, on a phone's layout. Each takes its screen's picture under `SHOTS`, in Maestro's output for
  the run (`~/.maestro/tests/<run>/<flow>/takeScreenshot/`, or `--test-output-dir`). `--include-tags smoke` runs four

## Documents

- [`docs/PANTRIX_FINDINGS.md`](docs/PANTRIX_FINDINGS.md): every gap met while learning Pantrix UI from its site.
- [`docs/OPEN_ITEMS.md`](docs/OPEN_ITEMS.md): what is open in Formatico itself.
- [`docs/PLAN.md`](docs/PLAN.md): the phases, in order.
