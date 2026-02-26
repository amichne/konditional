# Kotlin JVM Tooling Matrix

## LSP Backends

### Option A: Kotlin Official LSP (`Kotlin/kotlin-lsp`)
- Status: experimental, pre-alpha.
- Strength: official direction from Kotlin team.
- Constraint: currently focused on JVM-only Kotlin Gradle projects out of the box.
- Note: implementation is IntelliJ-based; use when this coupling is acceptable.

### Option B: FWCD Kotlin Language Server (`fwcd/kotlin-language-server`)
- Status: deprecated in favor of official LSP.
- Strength: open-source and editor-agnostic.
- Constraint: older ecosystem and potential compatibility gaps on newer Kotlin setups.
- Use when strict open-source preference outweighs deprecation risk.

## Debug Backends

### Option A: JVM JDWP Attach
- Start target with `--debug-jvm` and attach to port `5005` by default.
- Works with most editors and DAP clients.

### Option B: FWCD Kotlin Debug Adapter (`fwcd/kotlin-debug-adapter`)
- Status: community adapter.
- Strength: editor-agnostic Kotlin/JVM debugging via DAP.
- Pair well with fwcd language server workflows.

## Selection Rules

1. Prefer project stability over novelty for production work.
2. Prefer editor-agnostic components when the user asks for decoupling.
3. State tradeoffs explicitly:
- official but IntelliJ-coupled,
- open-source but deprecated,
- or hybrid stack.
