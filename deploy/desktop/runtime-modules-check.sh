#!/usr/bin/env bash
# Checks that the Java runtime a desktop app is packed with holds every module the app's jars use, as `jdeps` reads
# them: Compose packs only the modules app/desktopApp/build.gradle.kts names, and a module left out ends the app at its
# start, as java.net.http ended 1.0.0's (2026-11-12). deploy/deploy.sh desktop runs it on the Mac, deploy/windows.sh on
# Windows, on the app Gradle made before it is packed.
#
#   deploy/desktop/runtime-modules-check.sh <the app's jars folder> <its runtime's release file>
set -euo pipefail

# No apostrophe in the messages: macOS's bash 3.2 reads one inside "${…}" as a quote.
jars="${1:?the folder of the jars of the app}"
release="${2:?the release file of its runtime}"
command -v jdeps > /dev/null || { echo "jdeps is not on the PATH: it comes with the JDK, in its bin" >&2; exit 1; }
[ -f "$release" ] || { echo "No runtime's release file at $release" >&2; exit 1; }

needed="$(jdeps --print-module-deps --ignore-missing-deps --multi-release 21 "$jars"/*.jar | tr -d '\r' | tr ',' '\n' | sort -u)"
held="$(sed -n 's/^MODULES="\(.*\)"/\1/p' "$release" | tr -d '\r' | tr ' ' '\n' | sort -u)"
missing="$(comm -23 <(echo "$needed") <(echo "$held"))"
if [ -n "$missing" ]; then
    echo "The app's runtime lacks modules its jars use: $(echo $missing). Name them in app/desktopApp/build.gradle.kts, nativeDistributions' modules(...)." >&2
    exit 1
fi
echo "The runtime holds every module the app's jars use: $(echo $needed | tr ' ' ',')"
