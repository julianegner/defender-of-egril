#!/usr/bin/env bash
# Fails when production Kotlin files exceed MAX_LINES, unless they are listed in
# the allowlist with a line count that they have not grown beyond.
set -euo pipefail

MAX_LINES=1000
ALLOWLIST=".github/file-length-allowlist.txt"

declare -A allowed=()
if [ -f "$ALLOWLIST" ]; then
	while read -r count path; do
		[ -z "${path:-}" ] && continue
		[[ "$count" == \#* ]] && continue
		allowed["$path"]="$count"
	done <"$ALLOWLIST"
fi

failures=0
checked=()

while read -r path; do
	case "$path" in
	*Test.kt | */commonTest/* | */desktopTest/* | */androidTest/* | */iosTest/* | */wasmJsTest/* | */jvmTest/*) continue ;;
	esac
	lines=$(wc -l <"$path")
	checked+=("$path")
	if [ "$lines" -gt "$MAX_LINES" ]; then
		if [ -z "${allowed[$path]:-}" ]; then
			echo "::error file=$path::$lines lines exceeds $MAX_LINES; split the file or add it to $ALLOWLIST with a justification"
			failures=$((failures + 1))
		elif [ "$lines" -gt "${allowed[$path]}" ]; then
			echo "::error file=$path::grew from ${allowed[$path]} to $lines lines; split the file instead of growing it"
			failures=$((failures + 1))
		fi
	fi
done < <(git ls-files '*.kt')

for path in "${!allowed[@]}"; do
	if [ ! -f "$path" ]; then
		echo "::error file=$ALLOWLIST::$path no longer exists; remove it from the allowlist"
		failures=$((failures + 1))
		continue
	fi
	lines=$(wc -l <"$path")
	if [ "$lines" -le "$MAX_LINES" ]; then
		echo "::error file=$ALLOWLIST::$path is now $lines lines (<= $MAX_LINES); remove it from the allowlist"
		failures=$((failures + 1))
	fi
done

if [ "$failures" -gt 0 ]; then
	echo "File length check failed with $failures problem(s)."
	exit 1
fi
echo "File length check passed (${#checked[@]} production Kotlin files checked, limit $MAX_LINES lines)."
