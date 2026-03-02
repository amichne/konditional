---
name: sig-reachability
description: |
  Run a dead-code surface scan over .signatures/ and report types with 0 or low
  inbound references from other main-source sig files. Produces three tiers:
  ORPHANED (no refs anywhere), TEST-ONLY (referenced only from tests), and
  LOW USAGE (1–2 main refs). API surface modules (openfeature, kontracts,
  konditional-http-server, konditional-otel) are reported separately since their
  public types are designed to be consumed externally.

  Use when: conducting architecture reviews, API analysis refactoring modules, removing features, after axis/flag cleanup,
  or any time you suspect accumulated dead code.
---

# sig-reachability

Run the full analysis and print the report to stdout:

```bash
python3 "$CLAUDE_PROJECT_DIR/.claude/hooks/sig_reachability.py" \
  --sig-dir "$CLAUDE_PROJECT_DIR/.signatures" \
  --report-file -
```

Or write to the persistent report file (also written automatically by the hook):

```bash
python3 "$CLAUDE_PROJECT_DIR/.claude/hooks/sig_reachability.py" \
  --sig-dir "$CLAUDE_PROJECT_DIR/.signatures" \
  --report-file "$CLAUDE_PROJECT_DIR/.claude/sig-reachability-report.html"

open "$CLAUDE_PROJECT_DIR/.claude/sig-reachability-report.html"
```

## Suppressing known noise — exclusions file

Edit `.claude/sig-reachability-exclusions.txt` to silence entries you know are
fine until you touch them next:

```
# exclude a specific file (auto-removed on next edit of that file)
konditional-core/src/main/kotlin/io/amichne/konditional/context/Context.kt

# exclude an entire package subtree (sticky — remove manually)
io.amichne.konditional.internal.legacy
```

**File entries** (`/` present or ends with `.kt`) are automatically removed from
the exclusions file the moment Claude Code edits that file, so the next report
will include it again with fresh counts.

**Package entries** (dotted identifier, no `/`) are sticky and must be removed
manually.

## How to act on results

**ORPHANED** — Highest confidence. Verify with IDE find-usages before deleting;
the scanner only sees signatures, not runtime reflection or string-based lookups.

**TEST-ONLY** — The type serves tests only. Decide whether to move it to
`java-test-fixtures` or delete it if the test coverage it enables is no longer
needed.

**LOW USAGE (1–2 refs)** — Review the referencing files. If the sole reference
is a factory/adapter that can be inlined, the type may be redundant.

**API SURFACE** — These are expected to be empty internally. Only investigate
if the whole module is being deprecated.

## Caveats

- Reflection-based usage (e.g., Moshi adapter by class name) is invisible to
  the scanner. Always confirm with IDE find-usages before deleting.
- Same-package references that don't appear in `imports=` are caught via raw
  content scan, but this is heuristic — a short FQCN component matching a
  common word could produce false negatives.
- After regenerating `.signatures/` (via llm-native-signature-spec), re-run
  this scan to get fresh results.
