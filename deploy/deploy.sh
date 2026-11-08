#!/usr/bin/env bash
# Puts Formatico up from this machine: Gradle builds the web app and its server, Docker makes the stack's two images,
# and they run as one stack, here or on the host through Coolify (Faz 22 of docs/PLAN.md).
#
#   deploy/deploy.sh local [--no-gradle]   the stack on this machine at http://localhost:4005, checked (check.sh)
#   deploy/deploy.sh stop                  stops it and takes its containers away; the images and the shares stay
#   deploy/deploy.sh web [options]         the images to GHCR, Coolify puts them up, the host checked
#
# The version is formatico.version in gradle.properties. Options: --dry-run builds both images for both architectures
# and sends nothing, to GHCR or Coolify; --no-gradle takes what Gradle made last; --no-coolify pushes the images and
# leaves Coolify alone, as the first push does before Formatico's resource is made. FORMATICO_PORT gives the local
# stack another port.
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

case "${1:-}" in
    local) shift; local_up "$@" ;;
    stop) need_docker; compose_local down --remove-orphans ;;
    web) shift; web "$@" ;;
    *) awk 'NR > 1 && !/^#/ { exit } NR > 1 { sub(/^# ?/, ""); print }' "$0"; exit 2 ;;
esac
