#!/usr/bin/env bash
# Puts Formatico up from this machine: Gradle builds the web app and its server, Docker makes the stack's two images,
# and they run as one stack, here or on the host through Coolify (Faz 22 of docs/PLAN.md).
#
#   deploy/deploy.sh local [--no-gradle]   the stack on this machine at http://localhost:4005, checked (check.sh)
#   deploy/deploy.sh stop                  stops it and takes its containers away; the images and the shares stay
#   deploy/deploy.sh web [options]         the images to GHCR, Coolify puts them up, the host checked
#   deploy/deploy.sh desktop [options]     the macOS app in a DMG, signed with the Developer ID and notarized by Apple
#   deploy/deploy.sh android [--no-gradle] the Android app's APK, signed with the release key, checked
#   deploy/deploy.sh release [--dry-run]   a draft release on GitHub, with the DMG and the APK
#   deploy/deploy.sh publish               the draft published; the Windows MSI may come with it or later (windows.sh)
#
# The version is formatico.version in gradle.properties. Options: --dry-run builds both images for both architectures
# and sends nothing, to GHCR or Coolify; for desktop it signs the DMG and sends nothing to Apple, and for release it
# prints the notes and makes nothing; --no-gradle takes what Gradle made last; --no-coolify pushes the images and
# leaves Coolify alone, as the first push does before Formatico's resource is made. FORMATICO_PORT gives the local
# stack another port. The release's files are written to build/release, each with the commit it was built from.
#
# Coolify's address, its API token and Formatico's resource are read from ~/.gradle/gradle.properties: CoolifyUrl,
# CoolifyToken, CoolifyFormaticoUuid. The token never leaves that file but for the request's header. The steps, the
# one-time setup in Coolify among them, are in docs/DEPLOY_GUIDE.md.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEPLOY="$ROOT/deploy"
WEB="$ROOT/app/webApp/build/dist/wasmJs/productionExecutable"
JAR="$ROOT/server/build/libs/server.jar"
REGISTRY="ghcr.io/develappsancho"
REPOSITORY="develappsancho/formatico-cmp"
GRADLE_PROPERTIES="$HOME/.gradle/gradle.properties"
OUT="$ROOT/build/release"
# The release's files keep their names from one version to the next, so that a link to the latest release's file
# (releases/latest/download/<name>) always gives the newest. deploy/windows.sh names the MSI the same way.
DMG_NAME="Formatico-macOS.dmg"
APK_NAME="Formatico-Android.apk"
MSI_NAME="Formatico-Windows.msi"
export FORMATICO_PORT="${FORMATICO_PORT:-4005}"

fail() {
    echo "deploy: $*" >&2
    exit 1
}

step() {
    echo "== $*"
}

# The images are named by the commit they are built from, marked when the tree holds changes.
revision() {
    local commit
    commit="$(git -C "$ROOT" rev-parse --short HEAD)"
    if [ -n "$(git -C "$ROOT" status --porcelain)" ]; then echo "$commit-dirty"; else echo "$commit"; fi
}

# Formatico's version, formatico.version in gradle.properties: what the app says it is, and the images' tag.
formatico_version() {
    sed -n 's/^formatico\.version=//p' "$ROOT/gradle.properties" | tr -d '\r'
}

# A key of ~/.gradle/gradle.properties, or nothing.
gradle_property() {
    [ -f "$GRADLE_PROPERTIES" ] || return 0
    sed -n "s/^[[:space:]]*$1[[:space:]]*=[[:space:]]*//p" "$GRADLE_PROPERTIES" | tail -n 1 | tr -d '\r' | sed 's/[[:space:]]*$//'
}

need_docker() {
    docker info > /dev/null 2>&1 || fail "Docker is not running: start Docker Desktop, then run this again"
}

# A release is made from what the repository holds: no change left out of a commit, and the commit on GitHub.
need_published_tree() {
    [ -z "$(git -C "$ROOT" status --porcelain)" ] || fail "the tree has changes that are not committed"
    local head remote
    head="$(git -C "$ROOT" rev-parse HEAD)"
    remote="$(git -C "$ROOT" ls-remote origin refs/heads/main | cut -f1)"
    [ "$head" = "$remote" ] || fail "main on GitHub is not this commit: push it first"
}

# GitHub's CLI, signed in as the account the images belong to, which may write packages.
need_github() {
    command -v gh > /dev/null || fail "GitHub's CLI, gh, is not installed"
    local login scopes
    login="$(gh api user --jq .login 2> /dev/null)" || fail "gh is not signed in: gh auth login"
    [ "$login" = "${REGISTRY#ghcr.io/}" ] || fail "gh is signed in as $login; switch to ${REGISTRY#ghcr.io/}: gh auth switch"
    scopes="$(gh api -i user 2> /dev/null | tr -d '\r' | sed -n 's/^[Xx]-[Oo][Aa]uth-[Ss]copes: //p')"
    [[ ",${scopes// /}," == *",write:packages,"* ]] || fail "gh may not write packages: gh auth refresh -h github.com -s write:packages"
}

# Gradle builds the web app's distribution, its JavaScript fallback in it, and the server's jar.
gradle_build() {
    step "Gradle: the web app's distribution, its JavaScript fallback in it, and the server's jar"
    "$ROOT/gradlew" -p "$ROOT" --console=plain -q :app:webApp:wasmJsBrowserDistribution :app:webApp:copyJsApp :server:bootJar
}

need_built() {
    [ -f "$WEB/index.html" ] || fail "no web distribution in $WEB: run without --no-gradle"
    [ -f "$JAR" ] || fail "no server jar at $JAR: run without --no-gradle"
}

# --- Coolify ---------------------------------------------------------------------------------------------------------

COOLIFY_HEADER=""

need_coolify() {
    COOLIFY_URL="$(gradle_property CoolifyUrl)"
    COOLIFY_URL="${COOLIFY_URL%/}"
    COOLIFY_UUID="$(gradle_property CoolifyFormaticoUuid)"
    local token
    token="$(gradle_property CoolifyToken)"
    [ -n "$COOLIFY_URL" ] || fail "CoolifyUrl is not in $GRADLE_PROPERTIES"
    [ -n "$token" ] || fail "CoolifyToken is not in $GRADLE_PROPERTIES"
    [ -n "$COOLIFY_UUID" ] || fail "CoolifyFormaticoUuid is not in $GRADLE_PROPERTIES: Formatico's resource in Coolify"
    # The token goes to curl in a file of its own, so that it is in no command's arguments.
    COOLIFY_HEADER="$(mktemp)"
    chmod 600 "$COOLIFY_HEADER"
    trap 'rm -f "$COOLIFY_HEADER"' EXIT
    printf 'Authorization: Bearer %s\n' "$token" > "$COOLIFY_HEADER"
}

coolify() {
    curl -fsS --max-time 30 -H "@$COOLIFY_HEADER" -H 'Accept: application/json' "$COOLIFY_URL/api/v1/$1"
}

# What changes something in Coolify is asked for by POST: a deploy has been one since Coolify 4.5.
coolify_post() {
    curl -fsS --max-time 30 -X POST -H "@$COOLIFY_HEADER" -H 'Accept: application/json' "$COOLIFY_URL/api/v1/$1"
}

json() {
    python3 -c "import json, sys; data = json.load(sys.stdin); print($1)"
}

# Asks Coolify to put Formatico's resource up again, with the images just pushed, and waits until it is done.
coolify_deploy() {
    local answer deployment status last=""
    answer="$(coolify_post "deploy?uuid=$COOLIFY_UUID&force=false")" || fail "Coolify did not start the deploy"
    deployment="$(json "data['deployments'][0]['deployment_uuid']" <<< "$answer")"
    echo "Coolify's deployment $deployment"
    for _ in $(seq 1 120); do
        status="$(coolify "deployments/$deployment" | json "data.get('status', '')")" || status="unknown"
        if [ "$status" != "$last" ]; then echo "  $status"; last="$status"; fi
        case "$status" in
            finished) return 0 ;;
            failed | cancelled*) fail "Coolify's deployment $deployment ended $status: its log is in Coolify" ;;
        esac
        sleep 10
    done
    fail "Coolify's deployment $deployment did not end within 20 minutes"
}

# Coolify calls a deployment finished once its containers have started, and its proxy can answer 503 for a while
# after, until the new stack takes requests: the checks wait for the app's page, two minutes at most.
wait_for_app() {
    local address="$1" code=""
    for _ in $(seq 1 24); do
        code="$(curl -s -o /dev/null -w '%{http_code}' "$address/")" || code="000"
        [ "$code" = 200 ] && return 0
        sleep 5
    done
    echo "  $address/ still answers $code after two minutes"
}

# The address Formatico's resource is served at in Coolify: formatico-web's first domain. A Docker Compose resource
# keeps its domains by service, in docker_compose_domains, not in fqdn.
coolify_address() {
    coolify "applications/$COOLIFY_UUID" | python3 -c '
import json, sys
data = json.load(sys.stdin)
domains = data.get("docker_compose_domains") or {}
if isinstance(domains, str):
    domains = json.loads(domains or "{}")
domain = (domains.get("formatico-web") or {}).get("domain") or data.get("fqdn") or ""
print(domain.split(",")[0].strip().rstrip("/"))'
}

# --- Commands --------------------------------------------------------------------------------------------------------

# Compose on this machine: the stack with the parts that build its images here and publish its port.
compose_local() {
    docker compose -f "$DEPLOY/compose.yaml" -f "$DEPLOY/compose.local.yaml" "$@"
}

local_up() {
    local gradle=true
    for argument in "$@"; do
        case "$argument" in
            --no-gradle) gradle=false ;;
            *) fail "unknown option: $argument" ;;
        esac
    done
    need_docker
    if $gradle; then gradle_build; fi
    need_built

    FORMATICO_TAG="$(revision)"
    export FORMATICO_TAG
    step "Docker: the images of $FORMATICO_TAG, and the stack"
    compose_local up --build --detach --wait --remove-orphans
    compose_local images

    step "The checks"
    "$DEPLOY/check.sh" "http://localhost:$FORMATICO_PORT"
    echo "Formatico is at http://localhost:$FORMATICO_PORT"
}

# Formatico's images for the host, both architectures, pushed to GHCR; Coolify puts them up and the host is checked.
web() {
    local dry=false gradle=true ask_coolify=true
    for argument in "$@"; do
        case "$argument" in
            --dry-run) dry=true ;;
            --no-gradle) gradle=false ;;
            --no-coolify) ask_coolify=false ;;
            *) fail "unknown option: $argument" ;;
        esac
    done
    local version
    version="$(formatico_version)"
    [[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || fail "formatico.version in gradle.properties is no version: \"$version\""
    need_docker
    if ! $dry; then
        need_published_tree
        need_github
        if $ask_coolify; then need_coolify; fi
    fi
    if $gradle; then gradle_build; fi
    need_built

    local commit
    commit="$(revision)"
    local output=(--push)
    if $dry; then
        # Built for both architectures, and kept nowhere: what a push would send.
        output=(--output type=cacheonly)
    else
        step "GHCR: signing in as ${REGISTRY#ghcr.io/}"
        gh auth token | docker login ghcr.io -u "${REGISTRY#ghcr.io/}" --password-stdin > /dev/null
    fi
    local image
    for image in web server; do
        step "The $image image, $version ($commit), for linux/amd64 and linux/arm64"
        local context=(--build-context "app=$WEB")
        [ "$image" = server ] && context=(--build-context "jar=$(dirname "$JAR")")
        # Both architectures make an index, and GHCR reads the repository a package belongs to from the index's
        # annotations: the Dockerfile's labels are each architecture's.
        docker buildx build --platform linux/amd64,linux/arm64 \
            -f "$DEPLOY/$image/Dockerfile" "${context[@]}" --build-arg "REVISION=$commit" \
            --annotation "index:org.opencontainers.image.source=https://github.com/$REPOSITORY" \
            --annotation "index:org.opencontainers.image.licenses=Apache-2.0" \
            -t "$REGISTRY/formatico-$image:$version" -t "$REGISTRY/formatico-$image:$commit" \
            -t "$REGISTRY/formatico-$image:latest" \
            "${output[@]}" "$DEPLOY/$image"
    done
    if $dry; then
        echo "Dry run: nothing pushed, and Coolify not asked."
        return
    fi
    if ! $ask_coolify; then
        echo "Pushed, and Coolify not asked (--no-coolify)."
        return
    fi

    step "Coolify: Formatico's resource, with the images just pushed"
    coolify_deploy
    local address
    address="$(coolify_address)"
    [ -n "$address" ] || fail "Formatico's resource in Coolify has no domain on formatico-web"
    wait_for_app "$address"
    step "The checks, at $address"
    "$DEPLOY/check.sh" "$address"
}

# --- The release's files: the DMG, the APK, and the release on GitHub ------------------------------------------------

# formatico.version, or the run stops where it is no version.
checked_version() {
    local version
    version="$(formatico_version)"
    [[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || fail "formatico.version in gradle.properties is no version: \"$version\""
    echo "$version"
}

# A file's size, in MB as GitHub shows them, and its SHA-256.
describe() {
    local sha size
    sha="$(shasum -a 256 "$1" | cut -d' ' -f1)"
    size="$(stat -f %z "$1" | awk '{ printf "%.1f MB", $1 / 1000000 }')"
    echo "$(basename "$1"): $size, SHA-256 $sha"
}

# The newest build-tools of the Android SDK: ANDROID_HOME's, local.properties' sdk.dir, or Android Studio's own.
android_build_tools() {
    local sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}" tools
    if [ -z "$sdk" ] && [ -f "$ROOT/local.properties" ]; then
        sdk="$(sed -n 's/^[[:space:]]*sdk\.dir[[:space:]]*=[[:space:]]*//p' "$ROOT/local.properties" | tr -d '\r')"
    fi
    [ -n "$sdk" ] || sdk="$HOME/Library/Android/sdk"
    tools="$(ls -d "$sdk"/build-tools/* 2> /dev/null | sort -V | tail -n 1)"
    [ -x "$tools/apksigner" ] || fail "no apksigner in the Android SDK's build-tools, in $sdk"
    echo "$tools"
}

# The macOS app, built by Gradle, signed with the Developer ID and notarized by Apple, as build/release's DMG. A dry run
# signs it and sends nothing to Apple: such a DMG is no release's, and Gatekeeper would stop it.
desktop() {
    local dry=false gradle=true
    for argument in "$@"; do
        case "$argument" in
            --dry-run) dry=true ;;
            --no-gradle) gradle=false ;;
            *) fail "unknown option: $argument" ;;
        esac
    done
    [ "$(uname)" = Darwin ] || fail "the macOS app is built on a Mac"
    local version
    version="$(checked_version)"
    if $gradle; then
        step "Gradle: the desktop app, $version"
        "$ROOT/gradlew" -p "$ROOT" --console=plain -q :app:desktopApp:createDistributable
    fi
    local built="$ROOT/app/desktopApp/build/compose/binaries/main/app/Formatico.app"
    [ -d "$built" ] || fail "no app at $built: run without --no-gradle"
    local built_version
    built_version="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleShortVersionString' "$built/Contents/Info.plist")"
    [ "$built_version" = "$version" ] || fail "the app is $built_version, not $version: run without --no-gradle"

    step "The DMG: signed with the Developer ID$($dry || echo ', and notarized by Apple')"
    local work="$OUT/desktop"
    rm -rf "$work"
    mkdir -p "$work"
    rm -f "$OUT/$DMG_NAME.commit"
    ditto "$built" "$work/Formatico.app"
    local notarization=()
    $dry && notarization=(--no-notarize)
    "$DEPLOY/desktop/macos-dmg.sh" "$work/Formatico.app" "$OUT/$DMG_NAME" ${notarization[@]+"${notarization[@]}"}
    rm -rf "$work"
    $dry || revision > "$OUT/$DMG_NAME.commit"
    describe "$OUT/$DMG_NAME"
}

# The Android app, built by Gradle and signed with the release key local.properties names, as build/release's APK.
android() {
    local gradle=true
    for argument in "$@"; do
        case "$argument" in
            --no-gradle) gradle=false ;;
            *) fail "unknown option: $argument" ;;
        esac
    done
    local version
    version="$(checked_version)"
    if $gradle; then
        step "Gradle: the Android app, $version, signed with the release key"
        "$ROOT/gradlew" -p "$ROOT" --console=plain -q :app:androidApp:assembleRelease
    fi
    # Without the key's lines in local.properties, Gradle writes androidApp-release-unsigned.apk instead.
    local apk="$ROOT/app/androidApp/build/outputs/apk/release/androidApp-release.apk"
    [ -f "$apk" ] || fail "no signed APK at $apk: the release key is not in local.properties (docs/DEPLOY_GUIDE.md, 6)"
    local tools built_version
    tools="$(android_build_tools)"
    built_version="$("$tools/aapt2" dump badging "$apk" | sed -n "s/.*versionName='\([^']*\)'.*/\1/p" | head -n 1)"
    [ "$built_version" = "$version" ] || fail "the APK is $built_version, not $version: run without --no-gradle"
    step "The APK's signature"
    "$tools/apksigner" verify "$apk" || fail "the APK's signature does not hold"
    mkdir -p "$OUT"
    cp "$apk" "$OUT/$APK_NAME"
    revision > "$OUT/$APK_NAME.commit"
    describe "$OUT/$APK_NAME"
}

# A file of build/release, built from this very commit, or the run stops with the command that makes it.
need_release_file() {
    local name="$1" command="$2" made
    [ -f "$OUT/$name" ] || fail "no $name in $OUT: deploy/deploy.sh $command"
    made="$(cat "$OUT/$name.commit" 2> /dev/null || echo none)"
    [ "$made" = "$(revision)" ] || fail "$name was built from $made, not from $(revision): deploy/deploy.sh $command"
}

# GitHub's CLI, signed in as an account that may make the repository's releases.
need_github_releases() {
    command -v gh > /dev/null || fail "GitHub's CLI, gh, is not installed"
    [ "$(gh api "repos/$REPOSITORY" --jq .permissions.push 2> /dev/null)" = true ] ||
        fail "gh's account may not write to $REPOSITORY: gh auth status"
}

# The release's notes: what Formatico is, and each file's platform and SHA-256. deploy/windows.sh adds the MSI's.
release_notes() {
    local version="$1" dmg_sha apk_sha certificate fence='```'
    dmg_sha="$(shasum -a 256 "$OUT/$DMG_NAME" | cut -d' ' -f1)"
    apk_sha="$(shasum -a 256 "$OUT/$APK_NAME" | cut -d' ' -f1)"
    certificate="$("$(android_build_tools)/apksigner" verify --print-certs "$OUT/$APK_NAME" |
        sed -n 's/.*certificate SHA-256 digest: //p' | head -n 1)"
    printf '%s\n' \
        "Formatico $version formats, validates, explores and converts structured text: JSON and its kin, XML, YAML, TOML, CSV and properties. It runs on the web too, with nothing to install: https://formatico.pantrixui.com" \
        "" \
        "### macOS" \
        "" \
        "\`$DMG_NAME\`, for Macs with Apple silicon: signed with a Developer ID and notarized by Apple. Its SHA-256:" \
        "" \
        "$fence" "$dmg_sha" "$fence" \
        "" \
        "### Android" \
        "" \
        "\`$APK_NAME\`, for Android 11 or later: Android asks once to allow installing apps from where it was downloaded. Its SHA-256, and its signing certificate's:" \
        "" \
        "$fence" "$apk_sha" "$certificate" "$fence"
}

# A draft release v<version> on GitHub, at this commit, with the DMG and the APK. A draft is seen by the repository's
# collaborators alone, and `publish` makes it public. The Windows MSI is added on a Windows machine (deploy/windows.sh),
# to the draft or, later, to the release once it is out. A published release is not made again: a new one takes a new
# formatico.version.
release() {
    local dry=false
    for argument in "$@"; do
        case "$argument" in
            --dry-run) dry=true ;;
            *) fail "unknown option: $argument" ;;
        esac
    done
    local version
    version="$(checked_version)"
    $dry || need_published_tree
    need_release_file "$DMG_NAME" desktop
    need_release_file "$APK_NAME" android
    release_notes "$version" > "$OUT/notes.md"
    if $dry; then
        echo "Dry run: the release v$version is not made. Its notes:"
        cat "$OUT/notes.md"
        return
    fi
    need_github_releases
    local tag="v$version" draft
    draft="$(gh release view "$tag" -R "$REPOSITORY" --json isDraft --jq .isDraft 2> /dev/null || echo none)"
    case "$draft" in
        none)
            step "GitHub: the draft release $tag of $REPOSITORY"
            gh release create "$tag" "$OUT/$DMG_NAME" "$OUT/$APK_NAME" -R "$REPOSITORY" --draft \
                --target "$(git -C "$ROOT" rev-parse HEAD)" --title "Formatico $version" --notes-file "$OUT/notes.md"
            ;;
        true)
            step "GitHub: the draft release $tag, its DMG, its APK and its notes again"
            gh release upload "$tag" "$OUT/$DMG_NAME" "$OUT/$APK_NAME" --clobber -R "$REPOSITORY"
            gh release edit "$tag" --notes-file "$OUT/notes.md" -R "$REPOSITORY"
            ;;
        *) fail "$tag is published already: a new release takes a new formatico.version" ;;
    esac
    gh release view "$tag" -R "$REPOSITORY" --json url --jq .url
    echo "A draft: deploy/deploy.sh publish makes it public; deploy/windows.sh adds the MSI, before or after."
}

# The draft release made public and the latest, once it holds the DMG and the APK. The MSI need not be there yet:
# deploy/windows.sh adds it to the published release on whatever day the Windows machine builds it.
publish() {
    [ $# -eq 0 ] || fail "unknown option: $1"
    local version
    version="$(checked_version)"
    need_github_releases
    local tag="v$version" assets name
    [ "$(gh release view "$tag" -R "$REPOSITORY" --json isDraft --jq .isDraft 2> /dev/null)" = true ] ||
        fail "$tag is no draft release on GitHub: deploy/deploy.sh release"
    assets="$(gh release view "$tag" -R "$REPOSITORY" --json assets --jq '.assets[].name')"
    for name in "$DMG_NAME" "$APK_NAME"; do
        grep -qx "$name" <<< "$assets" || fail "$tag has no $name yet"
    done
    grep -qx "$MSI_NAME" <<< "$assets" ||
        echo "$tag has no $MSI_NAME yet: it is published without it, and deploy/windows.sh adds it on Windows later."
    step "GitHub: $tag published, as the latest release"
    gh release edit "$tag" -R "$REPOSITORY" --draft=false --latest
    gh release view "$tag" -R "$REPOSITORY" --json url --jq .url
}

case "${1:-}" in
    local) shift; local_up "$@" ;;
    stop) need_docker; compose_local down --remove-orphans ;;
    web) shift; web "$@" ;;
    desktop) shift; desktop "$@" ;;
    android) shift; android "$@" ;;
    release) shift; release "$@" ;;
    publish) shift; publish "$@" ;;
    *) awk 'NR > 1 && !/^#/ { exit } NR > 1 { sub(/^# ?/, ""); print }' "$0"; exit 2 ;;
esac
