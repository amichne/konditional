---
name: intellij-index-ide-integration
description: Use IntelliJ semantic MCP navigation and refactoring tools for symbol-level code work when the `intellij-index` MCP server is available, including finding references, definitions, implementations, type hierarchies, safe renames, and diagnostics; trigger this skill when requests involve navigating or editing code symbols in IDE-indexed projects.
---

# IntelliJ Index IDE Integration

## Overview

Use IntelliJ-backed MCP tools as the default path for symbol-aware code operations whenever the `intellij-index` MCP server is present. Prefer these tools over text search because they use IDE semantic indexing.

## MCP Availability Gate

1. Check MCP server availability before applying this workflow.
2. Apply this skill only when `intellij-index` is present.
3. If `intellij-index` is unavailable, fall back to standard repository search/edit workflows.

## Preferred Tool Mapping

For code-symbol tasks, use these tools first:

- **Find references**: `ide_find_references`
- **Go to definition**: `ide_find_definition`
- **Rename symbols**: `ide_refactor_rename`
- **Type hierarchy**: `ide_type_hierarchy`
- **Find implementations**: `ide_find_implementations`
- **Diagnostics**: `ide_diagnostics`

## Working Rules

- Start symbol navigation with IntelliJ MCP tools, not `grep`/`rg`, when symbol identity matters.
- Use text search only for non-symbol text discovery (for example logs, plain config values, or TODO strings).
- For renames, always prefer `ide_refactor_rename` to ensure project-wide, type-aware updates.
- Run `ide_diagnostics` after meaningful code edits when applicable to catch semantic issues early.

## Quick Task Playbooks

### Locate a symbol and usages

1. Resolve the symbol location with `ide_find_definition`.
2. Collect impact with `ide_find_references`.
3. Use results to scope edits.

### Safely rename a symbol

1. Confirm target symbol with `ide_find_definition`.
2. Execute `ide_refactor_rename`.
3. Verify follow-on issues with `ide_diagnostics`.

### Understand abstractions

1. Inspect inheritance/parent-child relations with `ide_type_hierarchy`.
2. Enumerate concrete implementations with `ide_find_implementations`.
3. Use both outputs to decide where behavior changes belong.
