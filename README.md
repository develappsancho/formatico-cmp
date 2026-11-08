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
| [`core/base`](core/base) | The bases every feature builds on: `MviViewModel`, `MviScreen` |
| [`core/designsystem`](core/designsystem) | Formatico's look over Pantrix UI: `FormaticoPreset` (Teal), Geist and Geist Mono, the syntax colours, `FormaticoTheme`, Formatico's own icons |
| [`feature/start`](feature/start) | The Start screen: paste, open, drop, recent documents and samples |
| [`feature/workspace`](feature/workspace) | The open documents and their workspace: the toolbar, the tree with its value previews, the table of an array or a CSV (sorting, filtering, columns, a column summed up, saved as CSV), the status bar |
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

## Checking

- Tests: `./gradlew jvmTest` (and `iosSimulatorArm64Test`, `wasmJsBrowserTest`)
- Detekt, with Pantrix UI's rules: `./gradlew detekt`
- Kotlinter: `./gradlew lintKotlin`, and `./gradlew formatKotlin` to fix the form
- Characters that cannot be seen, in the Kotlin sources: `./gradlew checkInvisibleCharacters` (part of `check`)
- The boards' screens on the desktop (Roborazzi): `./gradlew :app:shared:verifyRoborazziJvm` compares against
  [`app/shared/screenshots`](app/shared/screenshots), `./gradlew :app:shared:recordRoborazziJvm` writes them again
- The app icons, from the canvas's icon board: `python3 tools/icons/generate.py` (fontTools and Pillow; iconutil
  on macOS) writes them into each app's folders
- On a device (Maestro, installed apart): install the debug build, then
  `maestro --device <id> test -e APP_ID=com.developersancho.formatico app/maestro/flows`

## Documents

- [`docs/PANTRIX_FINDINGS.md`](docs/PANTRIX_FINDINGS.md): every gap met while learning Pantrix UI from its site.
- [`docs/OPEN_ITEMS.md`](docs/OPEN_ITEMS.md): what is open in Formatico itself.
- [`docs/PLAN.md`](docs/PLAN.md): the phases, in order.
