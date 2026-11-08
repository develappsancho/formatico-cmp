#!/usr/bin/env bash
# Puts Formatico up from this machine: Gradle builds the web app and its server, Docker makes the stack's two images,
# and they run as one stack, here. Publishing them (GHCR, then Coolify) comes with the
# release, Faz 22 of docs/PLAN.md.
#
#   deploy/deploy.sh local [--no-gradle]   the stack on this machine at http://localhost:4005, checked (check.sh)
#   deploy/deploy.sh stop                  stops it and takes its containers away; the images and the shares stay
#
# --no-gradle takes what Gradle made last. FORMATICO_PORT gives the stack another port.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEPLOY="$ROOT/deploy"
WEB="$ROOT/app/webApp/build/dist/wasmJs/productionExecutable"
JAR="$ROOT/server/build/libs/server.jar"
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

need_docker() {
    docker info > /dev/null 2>&1 || fail "Docker is not running: start Docker Desktop, then run this again"
}

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
    if $gradle; then
        step "Gradle: the web app's distribution, its JavaScript fallback in it, and the server's jar"
        "$ROOT/gradlew" -p "$ROOT" --console=plain -q :app:webApp:wasmJsBrowserDistribution :app:webApp:copyJsApp :server:bootJar
    fi
    [ -f "$WEB/index.html" ] || fail "no web distribution in $WEB: run without --no-gradle"
    [ -f "$JAR" ] || fail "no server jar at $JAR: run without --no-gradle"

    FORMATICO_TAG="$(revision)"
    export FORMATICO_TAG
    step "Docker: the images of $FORMATICO_TAG, and the stack"
    compose_local up --build --detach --wait --remove-orphans
    compose_local images

    step "The checks"
    "$DEPLOY/check.sh" "http://localhost:$FORMATICO_PORT"
    echo "Formatico is at http://localhost:$FORMATICO_PORT"
}

case "${1:-}" in
    local) shift; local_up "$@" ;;
    stop) need_docker; compose_local down --remove-orphans ;;
    *) awk 'NR > 1 && !/^#/ { exit } NR > 1 { sub(/^# ?/, ""); print }' "$0"; exit 2 ;;
esac
