# Formatico

Formatico formats, validates, explores and converts structured text: JSON and its kin, XML, YAML, TOML, CSV and
properties. It is a Compose Multiplatform app for Android, iOS, desktop and the web (`formatico.pantrixui.com`),
built with Pantrix UI. Application id and iOS bundle id: `com.developersancho.formatico`.

## How I want you to work

You are my eyes on the code. If something catches your attention while reading code or logs — a bug,
a risk, a smell, something surprising — flag it, even if it's unrelated to the task at hand. I will
never see it unless you tell me.

## Why it is built the way it is

Formatico is also a test of Pantrix UI: it is built the way a developer who found pantrixui.com would build it.

- **Learn Pantrix UI only from what an outsider has:** pantrixui.com (its Installation, Docs and For AI pages, the
  guides and the component pages), the Claude Code plugin and skill the site describes, the MCP server, and the
  packages (the jars and source jars Gradle resolves): the published ones, and for now the local build below.
- **Until the release before production, Pantrix's local build stands in for the published one** (the owner's
  decision, 2026-11-03). Fixes for the findings go into it as they come; the real release is made once, before
  production.
  - **Packages:** 0.1.2-SNAPSHOT from `mavenLocal`, which `settings.gradle.kts` reads for `com.pantrixui` SNAPSHOTs
    only; GitHub Packages stays listed for the release.
  - **Plugin:** `pantrix-ui@pantrix-ui-local` from `~/.pantrix/marketplace`, installed with `--scope local`, in place
    of the disabled `pantrix-ui@pantrix-ui`. Its MCP server is the jar in the plugin's folder, run with `java` over
    stdio, not `pantrixui.com/mcp`; its `get_changelog` says under Unreleased what is new, which the site does not
    show yet.
  - **A new local build:** reload the plugins (`/reload-plugins`, or a new session) and sync Gradle again; a SNAPSHOT
    in `mavenLocal` is read as it is, with no cache in the way.
  - **A finding that blocks the work** is marked `tıkıyor` in `docs/PANTRIX_FINDINGS.md`; it is fixed first and the
    local package rebuilt.
  - **A workaround in Formatico's code** says in a comment which finding it is for, and is removed when the fix
    lands.
  - **Before production:** the version goes back to the released one and the `mavenLocal` block is removed;
    `claude plugin enable pantrix-ui@pantrix-ui --scope local` and `claude plugin marketplace remove pantrix-ui-local`
    are run; Formatico is built again on every target with the package from GitHub Packages.
- **Never read the library's or the site's repositories** on this machine (`../pantrix-ui-cmp`, `../pantrix-ui-site`)
  or anything of theirs that is not published, beyond the local packages and plugin above. One exception (the
  owner's decision, 2026-11-06): for Formatico's hosting beside the site, the site's `deploy/` and `docs/deploy/` may
  be read, and nothing else of it.
- **Every gap goes into `docs/PANTRIX_FINDINGS.md`**, as its header says: what was looked for, where on the site, what
  was missing, what is proposed. The library and the site are fixed in another session, with the owner's yes; this
  project never edits them.
- A missing component is a finding too, not a reason to build one inside Formatico. Where the design asks for what
  Pantrix cannot draw, say so in the findings and in the report.

## The owner's decisions (2026-11-02)

- **v1 scope:** one workspace for every format (JSON, JSONC, JSON5, NDJSON, XML, YAML, TOML, CSV/TSV,
  properties/INI): detect the format, format and minify, validate with line and column and repair, text, tree and
  table views, JSONPath and XPath queries, convert between formats, semantic diff, tabs, local history, a command
  palette with shortcuts, drag and drop, the clipboard, English and Turkish, dark theme, adaptive layouts. In the
  tree, values are previewed (dates, colours, URLs, images); the text and the tree keep their selection in step.
  From the "düzen" design (2026-11-04): a search in the tree, opening it to a level, copying a value's path as
  JSONPath, JSON Pointer or jq, each column's type in the table, saving or copying the table (CSV, Markdown), and the
  Summary of a document, opened from the status bar's size.
- **Large files:** 50 MB on desktop, 10 MB on phones and the web, measured.
- **On the device (2026-11-04):** the documents' texts are encrypted, AES-GCM with a key KSafe keeps
  (cryptography-kotlin); should it cause trouble, they go back to plain text.
- **v2:** share links, type generation (Kotlin, TypeScript, Swift, Go), a graph view, JSON Schema. In-app AI with the
  user's own key was dropped (the owner's decision, 2026-11-09).
- **Parsers:** our own lossless ones in a pure Kotlin `core:format` module (comments, the spelling of numbers and the
  order of keys kept), except YAML, which is parsed by `snakeyaml-engine-kmp` and printed by our own printer.
- **Editor:** `PantrixCodeEditor` is the workspace's text pane (in the local package since 2026-11-03).
- **No backend in v1:** a small document is shared compressed in the link's fragment. v2 brings a Spring Boot server
  for end-to-end encrypted links and a URL proxy.
- **Navigation:** Compose's navigation3.
- **Stack:** Compose Multiplatform 1.12.2, Kotlin 2.4.21, Pantrix UI 0.1.2-SNAPSHOT from `mavenLocal` until the
  release before production (from GitHub Packages then), MVI with feature modules and Koin, detekt with
  `com.pantrixui:detekt-rules`, Kotlinter, tests in `commonTest` with Pantrix's testing module, Roborazzi, Maestro and
  browser tests, the web in Wasm with a JavaScript fallback.
- **Build logic:** convention plugins are Kotlin classes (`Plugin<Project>`), never precompiled `.gradle.kts` script
  plugins.
- **MVI:** the base view model, the base screen and every other base live in one module.
- **Every screen and every component comes with its previews and its tests** (the owner's rule, 2026-11-04):
  - **Previews:** one `@Preview` per case, fed by a `PreviewParameterProvider` that lists every state the screen or
    component can be in (empty, filled, loading, error, each format it shows), at the boards' widths and in light and
    dark.
  - **Screenshot tests (Roborazzi):** the same cases, recorded and verified.
  - **Compose UI tests:** what each case shows and what pressing it does, in `commonTest`, on every target.
  - **Unit tests:** its view model, its state and every function it is built from.
  - Work that adds a screen or a component without these is not done.

## Design

- **The screens are designed here, before their code,** on the Claude Design canvas
  https://claude.ai/artifact/PKpxmvKfeisZ4bgPTWVdZp, with the "Pantrix UI" design system as an outsider finds it (its
  README first). Each screen is drawn at 1440, 834 and 390 wide. The design is matched one to one in code.
- **The boards to draw:** Start; Workspace (the editor and the tree, the toolbar); an invalid document (the marks, the
  list of problems, repair); Table; Query; Convert; Compare; Summary; the command palette; Settings; the share
  dialog. The tree's value previews and the text and tree kept in step are shown on them.
- The canvas already holds the identity boards (A is the pick). It reads the design system's `tokens.css`;
  `formatico-options.css` keeps only Formatico's brand, typefaces and syntax colours, and the editor's pane look until
  the design system's `CodeEditor` takes an app's style (a finding). The boards draw the editor with the design
  system's `CodeEditor` (since 2026-11-09).
- **Identity: Teal with Geist and Geist Mono**, the owner's pick, as Formatico's own `DesignPreset`:
  - Brand role, light: `#0F766E`, on it `#FFFFFF`, container `#CCFBF1`, on the container `#134E4A`, focus ring
    `#0D9488`.
  - Brand role, dark: `#2DD4BF`, on it `#042F2E`, container `#134E4A`, on the container `#CCFBF1`, focus ring
    `#2DD4BF`.
  - Syntax, light: key `#115E59`, string `#B45309`, number `#7C3AED`, `true`/`false`/`null` `#BE123C`, punctuation
    `#475569`, comment `#64748B`, current line `#F0FDFA`.
  - Syntax, dark: key `#5EEAD4`, string `#FCD34D`, number `#C4B5FD`, `true`/`false`/`null` `#FDA4AF`, punctuation
    `#CBD5E1`, comment `#94A3B8`, current line `#13262B`.
  - Geist for the interface, Geist Mono for every text the user brings; both are under the OFL.

## Working here

- **The owner commits.** Never run a git command that writes (`init`, `add`, `commit`, `restore`, `reset`, `stash`,
  `checkout`, `mv`, `tag`, `push`); read-only git is fine. The repository is a local git repository on `master`, with
  no remote yet.
- **Every hand-over ends with one commit message:** one line, `[TYPE][Summary]` (`DEV`, `FIX`, `TEST`, `DOCS`, `CI`,
  `RELEASE`), English, imperative, no body and no trailers, with the new files to add and a commit by path.
- **Reports to the owner are in Turkish:** what was done, what was verified and on which targets, what was not, which
  findings were added. Code, KDoc and commit messages are in English.
- When the owner only asks a question, answer it; do not start work.
- Never print or copy the values in `local.properties`, a token or a key. A download needs the owner's yes, with the
  file, its source and its size.
- Never `./gradlew --stop`: it stops the IDE's daemon too.
- What catches the eye in Formatico itself and is outside the work at hand goes into `docs/OPEN_ITEMS.md`; what is
  Pantrix's goes into `docs/PANTRIX_FINDINGS.md`.
- Devices: the iPhone 18 Pro Max simulator stays booted; the Android emulator is the Pixel 7 Pro.
