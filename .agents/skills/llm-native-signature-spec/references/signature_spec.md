# Signature Spec (Navigation Contract)

## Purpose
Define the canonical navigation contract for `signatures/`, so agents can
derive accurate repository context with minimal token use and no context drift.

## Core guarantees
1. Accuracy first: navigation conclusions must be signature-backed.
2. JVM symbol fidelity: signature entries map 1:1 to concrete JVM symbols used
   for reasoning.
3. Primary navigation: signatures are the default path for repository traversal.
4. Subagent driver: signatures define subagent scope and traversal order.
5. Sole wide-context layer: global repository understanding must come from
   signatures, not broad source crawling.

## Artifact contract
- Root output folder: `signatures/` at repository root.
- Path mirror rule: `signatures/<relative/source/path>/<filename>.<ext>.sig`.
- Registry file: `signatures/INDEX.sig` with one line per `.sig` file.
- Registry order: lexicographically sorted, one canonical path per line.

## Per-File Schema (`*.sig`)
Emit machine-friendly key-value sections in this exact order:
1. `file=<relative path>`
2. `package=<package | <default>>`
3. `imports=<comma-separated imports>` (omit if none)
4. `type=<fqcn>|kind=<class|interface|...>|decl=<normalized declaration>` (repeat per type)
5. `fields:` followed by `- <normalized field signature>` (omit block if none)
6. `methods:` followed by `- <normalized method signature>` (omit block if none)

## Referential Semantics
- FQCN is mandatory for JVM types whenever package is available.
- Preserve import graph as a dependency hint (`imports=`).
- For files with no top-level type, emit `types=<none>`.
- Prefer normalized whitespace to keep diffs stable.

## Required traversal semantics
Use signature artifacts as the only wide-context repository layer.

1. Begin with `INDEX.sig` to discover possible locations.
2. Narrow by path/module/package and symbol relevance before opening source.
3. Read `.sig` files in deterministic order (candidate rank, then
   lexicographic tie-break).
4. Build a symbol-scoped working set, then assign subagents from that scope.
5. Escalate to source only for targeted symbol verification or body-level
   behavior details.

This traversal discipline is mandatory for token efficiency, determinism, and
context integrity.

## Non-compliance
Any broad context discovery path that bypasses signatures is invalid and
introduces context rot risk.
