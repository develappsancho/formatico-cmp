#!/usr/bin/env bash
# Checks a Formatico stack at its address: the app's page, the server's health, a share kept, read back and deleted,
# and a fetch of an address the server must never reach.
#
#   deploy/check.sh http://localhost:4005
set -euo pipefail

# No apostrophe in the message: macOS's bash 3.2 reads one inside "${…}" as a quote and the script with it.
BASE="${1:?give the address of the stack, as http://localhost:4005}"
BASE="${BASE%/}"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

fail() {
    echo "check: $*" >&2
    exit 1
}

ok() {
    echo "  ok  $*"
}

status() {
    curl -s -o /dev/null -w '%{http_code}' "$@"
}

curl -fsS "$BASE/" | grep -q "Formatico" || fail "$BASE/ is not Formatico's page"
ok "the app's page"

[ "$(curl -fsS "$BASE/api/health")" = ok ] || fail "the server is not up at $BASE/api/health"
ok "the server's health"

# Bytes no text holds, as an encrypted share's are, sent as the web app sends them: from the stack's own origin, which
# the server must let in.
head -c 4096 /dev/urandom > "$WORK/share"
curl -fsS -X POST -H "Origin: $BASE" -H 'Content-Type: application/octet-stream' --data-binary @"$WORK/share" \
    "$BASE/api/shares?days=1" > "$WORK/kept" || fail "the server did not keep a share"
id="$(python3 -c 'import json, sys; print(json.load(open(sys.argv[1]))["id"])' "$WORK/kept")"
token="$(python3 -c 'import json, sys; print(json.load(open(sys.argv[1]))["deleteToken"])' "$WORK/kept")"
ok "a share kept, as $id"

curl -fsS "$BASE/api/shares/$id" -o "$WORK/given"
cmp -s "$WORK/share" "$WORK/given" || fail "the share given back is not the one kept"
ok "the share given back, as it was"

[ "$(status -X DELETE -H 'X-Delete-Token: not its token' "$BASE/api/shares/$id")" = 403 ] || fail "another token deleted the share"
[ "$(status -X DELETE -H "Origin: $BASE" -H "X-Delete-Token: $token" "$BASE/api/shares/$id")" = 204 ] ||
    fail "its token did not delete the share"
[ "$(status "$BASE/api/shares/$id")" = 404 ] || fail "the share deleted is still given"
ok "the share deleted with its token alone"

[ "$(status "$BASE/api/fetch?url=http%3A%2F%2F127.0.0.1%2F")" = 400 ] || fail "the server fetched its own machine"
[ "$(status "$BASE/api/fetch?url=http%3A%2F%2F169.254.169.254%2F")" = 400 ] || fail "the server fetched the cloud's metadata"
ok "no fetch of the server's own network"

echo "All checks passed at $BASE"
