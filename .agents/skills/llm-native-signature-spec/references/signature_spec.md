# Signature Spec (Cascading + Referential)

## Purpose
Generate a compressed, high-density `signatures/` tree that mirrors repository layout and enables fast "read/no-read" decisions before opening full source files.

## Output Contract
- Root output folder: `signatures/` at repository root.
- Path mirror rule: `signatures/<relative/source/path>/<filename>.<ext>.sig`.
- Registry file: `signatures/INDEX.sig` with one line per `.sig` file.

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

## Drift Prevention
- Regenerate all signatures when source files change (CI or pre-commit).
- Keep output deterministic: stable sort order + normalized spacing.
- Never hand-edit generated `signatures/` output.
- Keep parser intentionally conservative; false negatives are acceptable if format stability is preserved.
