#!/usr/bin/env bash
# Version/template consistency check. Default reports and exits 0; --strict fails on inconsistency.
set -u
cd "$(dirname "$0")/.."

MODE="${1:-}"
V="$(grep -E '^mod_version=' gradle.properties | head -1 | cut -d= -f2-)"
FAIL=0

echo "gradle.properties mod_version = ${V}"
echo "---"

check_file() {
    local f="$1" min="$2" n
    if [ ! -f "$f" ]; then
        echo "MISSING FILE: $f"
        FAIL=1
        return
    fi
    n="$(grep -cF "$V" "$f" || true)"
    if [ "$n" -lt "$min" ]; then
        echo "INCONSISTENT: $f contains current version ${n} time(s), expected >= ${min}"
        FAIL=1
    else
        echo "OK: $f (${n})"
    fi
}

check_file README.md 1
check_file CHANGELOG.md 1

echo "--- metadata placeholders"
template="src/main/templates/META-INF/neoforge.mods.toml"
if [ ! -f "$template" ]; then
    echo "MISSING FILE: $template"
    FAIL=1
else
    while IFS= read -r key; do
        if ! grep -qE "^${key}=" gradle.properties; then
            echo "UNKNOWN TEMPLATE PROPERTY: $key"
            FAIL=1
        else
            echo "OK: $key"
        fi
    done < <(grep -oE '\$\{[A-Za-z_][A-Za-z0-9_]*\}' "$template" | sed -E 's/^\$\{//;s/\}$//' | sort -u)
fi

echo "---"
if [ "$FAIL" -ne 0 ]; then
    echo "Result: inconsistent. Fix before merge/release."
    [ "$MODE" = "--strict" ] && exit 1
else
    echo "Result: consistent."
fi
exit 0
