# Memory Leak Scan

The memory leak scan checks non-stale branches for retained game state and heap growth.

## Components

- `frontend/composeApp/src/desktopTest/kotlin/de/egril/defender/memory/MemoryLeakTest.kt`: the detector. It is skipped unless `MEMORY_LEAK_TEST=true`.
- `.github/workflows/memory-leak-scan.yml`: runs monthly (1st, 06:00 UTC) and on demand.
- `.github/scripts/memory_leak_report.py`: aggregates results and builds the coding-agent prompts.

## Checks

- **RETENTION**: a released `GameState` must be garbage collected after several `System.gc()` attempts.
- **GROWTH**: the post-GC heap slope over repeated games must stay below `MEMORY_LEAK_MAX_GROWTH_KB` (default 256).

## Run locally

```bash
cd frontend
MEMORY_LEAK_TEST=true ./gradlew :composeApp:desktopTest --tests "de.egril.defender.memory.MemoryLeakTest" --offline
cat composeApp/build/memory-leak-report/result.txt
```

Optional variables: `MEMORY_LEAK_ITERATIONS` (default 40) and `MEMORY_LEAK_MAX_GROWTH_KB` (default 256).

## Workflow inputs

- `stale_days` (default 90): branches whose last commit is older than this are skipped.
- `max_branches` (default 30): upper bound on scanned branches.

Branches that cannot compile the harness or lack `frontend/composeApp` are reported as `ERROR`, not as leaks.

When leaks are found, the workflow opens an issue containing a copy-paste prompt for each leaking branch. The issue is not assigned to Copilot.
