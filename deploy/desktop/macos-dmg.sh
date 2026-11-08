#!/usr/bin/env bash
# Signs Formatico's desktop app with the keychain's Developer ID, packs it in a DMG, and has Apple notarize the DMG.
# deploy/deploy.sh desktop runs it; it is Pantrix UI's site's deploy/desktop/macos-dmg.sh, made Formatico's.
#
#   deploy/desktop/macos-dmg.sh <Formatico.app> <out.dmg> [--no-notarize]
#
# The app is the one `./gradlew :app:desktopApp:createDistributable` writes, which Compose signs ad hoc. Compose's own
# signing finds the certificate by its name and hands that name to codesign, which does not find a name with letters
# beyond ASCII; so the app is signed here, by the certificate's hash, as Compose would sign it: the native libraries
# inside the jars, every library and executable of the runtime, the runtime, then the app with the JVM's
# entitlements. The notarization takes the keychain profile `pantrix-notary` (`xcrun notarytool store-credentials`),
# so no password passes through here.
set -euo pipefail

app="$(cd "$(dirname "${1:?the app to sign}")" && pwd)/$(basename "$1")"
dmg="$(cd "$(dirname "${2:?the DMG to write}")" && pwd)/$(basename "$2")"
notarize=true
[[ "${3:-}" == "--no-notarize" ]] && notarize=false

team="FN6T94F922"
profile="pantrix-notary"
here="$(cd "$(dirname "$0")" && pwd)"
entitlements="$here/macos-entitlements.plist"

identity="$(security find-identity -v -p codesigning |
    awk -v team="($team)" '/Developer ID Application/ && index($0, team) { print $2; exit }')"
if [[ -z "$identity" ]]; then
    echo "The keychain has no Developer ID Application certificate of the team $team." >&2
    exit 1
fi

sign() {
    codesign --force --timestamp --options runtime --sign "$identity" "$@"
}

is_mach_o() {
    file -b "$1" | grep -q "Mach-O"
}

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

echo "Signing the native libraries inside the jars"
for jar in "$app"/Contents/app/*.jar; do
    natives="$(unzip -Z1 "$jar" | grep -E '\.(dylib|jnilib)$' || true)"
    [[ -z "$natives" ]] && continue
    folder="$work/jars/$(basename "$jar")"
    mkdir -p "$folder"
    while IFS= read -r native; do
        unzip -q -o "$jar" "$native" -d "$folder"
        sign "$folder/$native"
        (cd "$folder" && zip -q "$jar" "$native")
    done <<< "$natives"
done

echo "Signing the runtime's and the app's libraries and executables"
find "$app/Contents/runtime" "$app/Contents/app" -type f -print0 | while IFS= read -r -d '' file; do
    if is_mach_o "$file"; then sign "$file"; fi
done
sign "$app/Contents/runtime"
sign --entitlements "$entitlements" "$app"
codesign --verify --deep --strict "$app"

echo "Packing $(basename "$dmg")"
stage="$work/stage"
mkdir -p "$stage"
ditto "$app" "$stage/$(basename "$app")"
ln -s /Applications "$stage/Applications"
rm -f "$dmg"
hdiutil create -quiet -volname "$(basename "$app" .app)" -srcfolder "$stage" -format UDZO "$dmg"
codesign --force --timestamp --sign "$identity" "$dmg"

if ! $notarize; then
    echo "Not notarized: $dmg"
    exit 0
fi

echo "Notarizing $(basename "$dmg"), which takes some minutes"
result="$work/notarization.plist"
xcrun notarytool submit "$dmg" --keychain-profile "$profile" --wait --output-format plist > "$result"
status="$(/usr/libexec/PlistBuddy -c "Print :status" "$result")"
if [[ "$status" != "Accepted" ]]; then
    id="$(/usr/libexec/PlistBuddy -c "Print :id" "$result")"
    echo "Apple did not notarize the DMG: $status. Its log:" >&2
    xcrun notarytool log "$id" --keychain-profile "$profile" >&2
    exit 1
fi
xcrun stapler staple -q "$dmg"
spctl --assess --type open --context context:primary-signature "$dmg"
echo "Signed, notarized and stapled: $dmg"
