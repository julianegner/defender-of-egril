#!/usr/bin/env python3
"""Aggregate per-branch memory leak results into a summary and coding-agent prompts.

Usage: memory_leak_report.py <results_dir> <issue_body_out> <summary_out>

Writes has_leaks=true|false to $GITHUB_OUTPUT. The issue body is only written
when at least one branch reports LEAKS_FOUND.
"""
import os
import pathlib
import sys

MAX_ISSUE_BODY_CHARS = 65000

PROMPT_TEMPLATE = """Fix the memory leaks found by the monthly memory leak scan on branch `{branch}`.

Scan summary:
{findings}

Steps:
1. Check out branch `{branch}`.
2. Reproduce with: `cd frontend && MEMORY_LEAK_TEST=true ./gradlew :composeApp:desktopTest --tests "de.egril.defender.memory.MemoryLeakTest" --offline`
   The report is written to `frontend/composeApp/build/memory-leak-report/result.txt`.
3. Find what keeps the released game state or the growing heap reachable. Typical causes: global or companion-object collections, listeners or callbacks never removed, static caches without eviction, coroutine scopes or timers that outlive a game.
4. Fix the root cause in production code. Do NOT weaken `MemoryLeakTest`, raise `MEMORY_LEAK_MAX_GROWTH_KB`, or skip the test.
5. Re-run the memory leak test until it reports `STATUS: CLEAN`.
6. Run the full desktop test suite: `cd frontend && ./gradlew :composeApp:desktopTest --offline`
7. Commit the fix to `{branch}` with a descriptive message and report what was changed.
"""


def parse_result(path: pathlib.Path):
    data = {"STATUS": "ERROR", "BRANCH": path.parent.name, "INFO": [], "FINDING": []}
    for raw in path.read_text(encoding="utf-8", errors="replace").splitlines():
        line = raw.strip()
        if ":" not in line:
            continue
        key, _, value = line.partition(":")
        key = key.strip()
        value = value.strip()
        if key == "STATUS":
            data["STATUS"] = value
        elif key == "BRANCH":
            data["BRANCH"] = value
        elif key == "INFO":
            data["INFO"].append(value)
        elif key == "FINDING":
            data["FINDING"].append(value)
    return data


def load_results(results_dir: pathlib.Path):
    results = []
    seen_dirs = set()
    for path in sorted(results_dir.rglob("result.txt")):
        seen_dirs.add(path.parent.name)
        results.append(parse_result(path))
    # A matrix job that failed before writing a report still needs a row.
    for branch_dir in sorted(p for p in results_dir.iterdir() if p.is_dir()):
        if branch_dir.name not in seen_dirs:
            results.append({"STATUS": "ERROR", "BRANCH": branch_dir.name,
                            "INFO": ["no report produced"], "FINDING": []})
    return results


def build_summary(results):
    lines = ["| Branch | Status | Findings |", "|---|---|---|"]
    for r in results:
        lines.append(f"| `{r['BRANCH']}` | {r['STATUS']} | {len(r['FINDING'])} |")
    return "\n".join(lines) + "\n"


def build_issue_body(results):
    leaking = [r for r in results if r["STATUS"] == "LEAKS_FOUND"]
    errored = [r for r in results if r["STATUS"] not in ("CLEAN", "LEAKS_FOUND")]
    parts = [
        "The scheduled memory leak scan found leaks on the following branches.",
        "",
        build_summary(results),
    ]
    if errored:
        parts.append("Branches that could not be scanned (not treated as leaks):")
        for r in errored:
            parts.append(f"- `{r['BRANCH']}`: {r['STATUS']} ({'; '.join(r['INFO']) or 'see workflow log'})")
        parts.append("")
    parts.append("## Coding agent prompts")
    parts.append("")
    for r in leaking:
        findings = "\n".join(f"- {f}" for f in r["FINDING"]) or "- (no details)"
        parts.append(f"### Branch `{r['BRANCH']}`")
        parts.append("")
        parts.append("```text")
        parts.append(PROMPT_TEMPLATE.format(branch=r["BRANCH"], findings=findings).rstrip())
        parts.append("```")
        parts.append("")
    body = "\n".join(parts)
    if len(body) > MAX_ISSUE_BODY_CHARS:
        body = body[:MAX_ISSUE_BODY_CHARS] + "\n\n_Output truncated; see the workflow artifacts for the full prompts._\n"
    return body


def write_github_output(name: str, value: str):
    target = os.environ.get("GITHUB_OUTPUT")
    if target:
        with open(target, "a", encoding="utf-8") as fh:
            fh.write(f"{name}={value}\n")
    else:
        print(f"{name}={value}")


def main():
    if len(sys.argv) != 4:
        print(__doc__, file=sys.stderr)
        return 2
    results_dir = pathlib.Path(sys.argv[1])
    issue_body_out = pathlib.Path(sys.argv[2])
    summary_out = pathlib.Path(sys.argv[3])

    results = load_results(results_dir) if results_dir.is_dir() else []
    summary_out.write_text(build_summary(results), encoding="utf-8")

    has_leaks = any(r["STATUS"] == "LEAKS_FOUND" for r in results)
    if has_leaks:
        issue_body_out.write_text(build_issue_body(results), encoding="utf-8")
    write_github_output("has_leaks", "true" if has_leaks else "false")
    return 0


if __name__ == "__main__":
    sys.exit(main())
