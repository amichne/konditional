# Directory Scope

This file scopes `konditional-engine/src/main/kotlin/io/amichne/konditional/core/features/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Typed feature abstractions and per-value-type implementations.

## Local Files
- `BooleanFeature.kt`
- `DoubleFeature.kt`
- `EnumFeature.kt`
- `Feature.kt`
- `IntFeature.kt`
- `KotlinClassFeature.kt`
- `StringFeature.kt`

## Rules
- Preserve the generic relationship between feature value type, context type, and owning namespace.
- New feature kinds belong here only if they remain first-class and compile-time safe across the DSL and runtime.
