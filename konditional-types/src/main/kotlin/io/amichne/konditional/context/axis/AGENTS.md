# Directory Scope

This file scopes `konditional-types/src/main/kotlin/io/amichne/konditional/context/axis/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Typed custom-axis model: axis ids, grouped values, enum-backed axis values, and explicit-id escape hatches.

## Local Files
- `Axes.kt`
- `Axis.kt`
- `AxisKey.kt`
- `AxisValue.kt`
- `KonditionalExplicitId.kt`

## Rules
- Axis identity must stay deterministic; renames must not silently change targeting semantics without an explicit opt-in id.
- Preserve enum-backed typing and immutable grouped-value semantics.
