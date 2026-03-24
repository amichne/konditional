# Directory Scope

This file scopes `konditional-engine/src/main/kotlin/io/amichne/konditional/core/ops/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Observational hooks for logging, metrics, and registry lifecycle integration.

## Local Files
- `KonditionalLogger.kt`
- `Metrics.kt`
- `MetricsCollector.kt`
- `RegistryHooks.kt`

## Rules
- Observability must remain non-invasive: no hook here may alter evaluation outcomes.
- Default no-op implementations should stay allocation-light and safe for library consumers.
