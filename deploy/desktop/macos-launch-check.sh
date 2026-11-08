#!/usr/bin/env bash
# Opens Formatico's macOS app and checks it comes up: its main window on screen, and nothing thrown. deploy/deploy.sh
# desktop runs it on the app it packs, before the DMG; it is how 1.0.0, which stopped at its start on a module its Java
# runtime lacked, would have been caught (2026-11-12). A process that is still running says nothing: the launcher's
# error dialog keeps it running.
#
#   deploy/desktop/macos-launch-check.sh <Formatico.app> [seconds]
#
# The app runs with a home of its own, so that what it keeps on this machine (its history, its settings) is not
# touched, and keeps its keys in that home rather than in the login keychain: an app that reads the installed one's
# keychain items is asked for the login password (2026-11-12). It is closed again after the check.
set -euo pipefail

app="${1:?the app to open}"
wait_for="${2:-20}"
binary="$app/Contents/MacOS/Formatico"
[ -x "$binary" ] || { echo "No app at $binary" >&2; exit 1; }

work="$(mktemp -d)"
log="$work/app.log"
pid=""
cleanup() {
    if [ -n "$pid" ]; then
        kill "$pid" 2> /dev/null || true
        wait "$pid" 2> /dev/null || true
    fi
    rm -rf "$work"
}
trap cleanup EXIT

mkdir -p "$work/home"
JAVA_TOOL_OPTIONS="-Duser.home=$work/home -Dksafe.jvm.keyVault=software" "$binary" > "$log" 2>&1 &
pid=$!
sleep "$wait_for"

# The windows the app has, as width x height: the window server tells them by the process, with no permission asked.
# Those in every Space: with a full-screen app in front, the new window opens in another Space (2026-11-12).
windows="$(osascript -l JavaScript - "$pid" << 'EOF'
ObjC.import('CoreGraphics');
function run(argv) {
    const pid = parseInt(argv[0]);
    const all = ObjC.castRefToObject($.CGWindowListCopyWindowInfo($.kCGWindowListOptionAll, 0));
    const sizes = [];
    for (let i = 0; i < all.count; i++) {
        const window = all.objectAtIndex(i);
        if (window.objectForKey('kCGWindowOwnerPID').intValue !== pid) continue;
        const bounds = window.objectForKey('kCGWindowBounds');
        sizes.push(bounds.objectForKey('Width').intValue + 'x' + bounds.objectForKey('Height').intValue);
    }
    return sizes.join(' ');
}
EOF
)"

running=true
kill -0 "$pid" 2> /dev/null || running=false
thrown="$(grep -v '^Picked up JAVA_TOOL_OPTIONS' "$log" | grep -E 'Exception|Error|error:' | head -n 5 || true)"
# The main window is a screen's worth; the launcher's error dialog is a few hundred points wide.
main="$(tr ' ' '\n' <<< "$windows" | awk -F x '$1 >= 800 && $2 >= 500 { print; exit }')"

echo "Formatico's windows: ${windows:-none}"
if ! $running || [ -z "$main" ] || [ -n "$thrown" ]; then
    echo "Formatico did not come up:" >&2
    $running || echo "  it ended" >&2
    [ -n "$main" ] || echo "  no main window, only: ${windows:-none}" >&2
    [ -n "$thrown" ] && echo "$thrown" | sed 's/^/  /' >&2
    exit 1
fi
echo "Formatico came up: its window is $main, and nothing was thrown."
