#!/usr/bin/env bash
# Formatico's Windows installer: built on a Windows machine, as Compose makes an MSI on Windows alone, and added to the
# release on GitHub that deploy/deploy.sh release made on the Mac, a draft or published already: on any day after it.
# Run in Git Bash, at the repository's root.
#
#   deploy/windows.sh              the MSI built from the release's commit, added to the release with its SHA-256
#   deploy/windows.sh --dry-run    the MSI built from this commit, and sent nowhere
#
# What the machine needs (docs/DEPLOY_GUIDE.md, 7): Git for Windows (its Git Bash runs this), a JDK of 17 or later to
# start Gradle, which then fetches the JDK 21 the build runs on and WiX, the MSI's tools, by itself; GitHub's CLI, gh,
# signed in as a collaborator who may write to the repository; and a GitHub token that reads packages, in Gradle's
# gradle.properties, for Pantrix UI's packages. No Android SDK, no Xcode: the desktop app needs neither.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
REPOSITORY="develappsancho/formatico-cmp"
MSI_NAME="Formatico-Windows.msi"
OUT="$ROOT/build/release"

fail() {
    echo "windows: $*" >&2
    exit 1
}

step() {
    echo "== $*"
}

dry=false
gradle=true
for argument in "$@"; do
    case "$argument" in
        --dry-run) dry=true ;;
        --no-gradle) gradle=false ;;
        *) fail "unknown option: $argument (deploy/windows.sh [--dry-run] [--no-gradle])" ;;
    esac
done

# --- What the machine has --------------------------------------------------------------------------------------------

case "$(uname -s)" in
    MINGW* | MSYS* | CYGWIN*) ;;
    *) fail "this is run on Windows, in Git Bash: Compose makes an MSI on Windows alone" ;;
esac

version="$(sed -n 's/^formatico\.version=//p' "$ROOT/gradle.properties" | tr -d '\r')"
[[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || fail "formatico.version in gradle.properties is no version: \"$version\""
tag="v$version"

command -v java > /dev/null || [ -n "${JAVA_HOME:-}" ] ||
    fail "no Java to start Gradle: install a JDK of 17 or later (Temurin or Zulu), then open Git Bash again"

# Gradle's own folder: GRADLE_USER_HOME, or .gradle in the user's folder, which Git Bash names as $HOME.
gradle_home="${GRADLE_USER_HOME:-$HOME/.gradle}"
command -v cygpath > /dev/null && gradle_home="$(cygpath -u "$gradle_home")"
properties="$gradle_home/gradle.properties"
for key in GitHubPackagesUsername GitHubPackagesPassword; do
    grep -q "^[[:space:]]*$key[[:space:]]*=" "$properties" 2> /dev/null ||
        fail "$key is not in $properties: Pantrix UI's packages come from GitHub Packages (docs/DEPLOY_GUIDE.md, 7)"
done

[ -z "$(git -C "$ROOT" status --porcelain)" ] || fail "the tree has changes: the MSI is built from the release's commit alone"

if ! $dry; then
    command -v gh > /dev/null || fail "GitHub's CLI, gh, is not installed: winget install GitHub.cli"
    gh auth status > /dev/null 2>&1 || fail "gh is not signed in: gh auth login"
    [ "$(gh api "repos/$REPOSITORY" --jq .permissions.push 2> /dev/null)" = true ] ||
        fail "gh's account may not write to $REPOSITORY: the owner adds it as a collaborator"
    # The release, a draft or published already: the MSI may come on any day after it, as the Windows machine allows.
    release="$(gh release view "$tag" -R "$REPOSITORY" --json isDraft,isImmutable,targetCommitish \
        --jq '[.isDraft, .isImmutable, .targetCommitish] | @tsv' 2> /dev/null | tr -d '\r')" ||
        fail "there is no release $tag on GitHub yet: the owner makes it first (deploy/deploy.sh release)"
    IFS=$'\t' read -r draft immutable target <<< "$release"
    [ "$immutable" != true ] ||
        fail "$tag is immutable: no file is added to it once it is out (the repository's Settings, Immutable releases)"
    # A draft names the commit it is made from; a published release is its tag's commit, which may have been given
    # as a branch's name instead.
    if ! [[ "$target" =~ ^[0-9a-f]{40}$ ]]; then
        # An annotated tag is listed twice, the tag and, as `^{}`, its commit: the commit, where it is there. The
        # pattern ends in `*`, or ls-remote leaves the `^{}` line out.
        target="$(git -C "$ROOT" ls-remote --tags origin "$tag*" | awk -v ref="refs/tags/$tag" '
            $2 == ref "^{}" { peeled = $1 } $2 == ref { plain = $1 } END { print (peeled != "" ? peeled : plain) }')"
        [ -n "$target" ] || fail "the commit of $tag could not be found on GitHub"
    fi
    [ "$(git -C "$ROOT" rev-parse HEAD)" = "$target" ] ||
        fail "this is not the release's commit: git fetch origin && git checkout $target, then run this again"
    $draft && kind="draft release" || kind="release"
fi

# --- The MSI ---------------------------------------------------------------------------------------------------------

if $gradle; then
    step "Gradle: Formatico $version's MSI (Gradle fetches its JDK and WiX the first time)"
    "$ROOT/gradlew" -p "$ROOT" --console=plain :app:desktopApp:packageMsi
fi
built="$ROOT/app/desktopApp/build/compose/binaries/main/msi/Formatico-$version.msi"
[ -f "$built" ] || fail "no MSI at $built: run without --no-gradle"
mkdir -p "$OUT"
cp "$built" "$OUT/$MSI_NAME"
msi="$OUT/$MSI_NAME"
sha="$(sha256sum "$msi" | cut -d' ' -f1)"
size="$(wc -c < "$msi" | awk '{ printf "%.1f MB", $1 / 1000000 }')"
case "$(uname -m)" in
    x86_64 | amd64) machines="64-bit Windows 10 or later on Intel and AMD (x64)" ;;
    aarch64 | arm64) machines="Windows 11 on Arm" ;;
    *) machines="Windows 10 or later" ;;
esac
echo "$MSI_NAME: $size, SHA-256 $sha, for $machines"

if $dry; then
    echo "Dry run: the MSI is at $msi, and sent nowhere."
    exit 0
fi

# --- The release -----------------------------------------------------------------------------------------------------

step "GitHub: $MSI_NAME on the $kind $tag"
gh release upload "$tag" "$msi" --clobber -R "$REPOSITORY"

# The notes keep what the Mac wrote, the macOS and Android sections; the Windows section is written again, whole.
notes="$(mktemp)"
trap 'rm -f "$notes"' EXIT
gh release view "$tag" -R "$REPOSITORY" --json body --jq .body | tr -d '\r' |
    awk '/^### Windows$/ { skip = 1; next } /^### / { skip = 0 }
        !skip { line[++count] = $0; if ($0 != "") last = count }
        END { for (i = 1; i <= last; i++) print line[i] }' > "$notes"
fence='```'
printf '%s\n' "" "### Windows" "" \
    "\`$MSI_NAME\`, for $machines. It is not signed: where Windows says it protected the PC (SmartScreen), More info, then Run anyway. It installs for the user alone, with no administrator. Its SHA-256:" \
    "" "$fence" "$sha" "$fence" >> "$notes"
gh release edit "$tag" -R "$REPOSITORY" --notes-file "$notes"
gh release view "$tag" -R "$REPOSITORY" --json url --jq .url
if $draft; then echo "Done: the owner publishes the release (deploy/deploy.sh publish)."; else echo "Done: the release has its MSI."; fi
