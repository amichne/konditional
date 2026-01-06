# Module Split Status (`:core` → split modules)

Source of truth: `prompt.md`

## Goal

- Fully replace legacy `:core` with split modules (`:konditional-core`, `:konditional-runtime`, `:konditional-serialization`, `:konditional-observability`) and delete `:core` from build logic, source tree, docs, and published coordinates.

## Hard Rules (Circular Dependency Policy)

- `:konditional-core` contains only abstractions + minimal consumer DSL/public surface.
- `:konditional-core` must not depend on serialization/runtime/observability implementations.
- `:konditional-runtime`, `:konditional-serialization`, `:konditional-observability` depend “inwards” on `:konditional-core`.
- No `-Xfriend-paths` as a long-term solution.
- Use `@RequiresOptIn` + internal API packages for cross-module implementation contracts.

## Phases + Exit Criteria

### Phase 0 — Baseline snapshot

- [ ] `make check` passes on baseline (before refactor).
- [ ] Module inventory performed (`settings.gradle.kts`, module `build.gradle.kts` files).

### Phase 1 — Contracts first (core public surface)

- [x] `:konditional-core` declares minimal public API allowlist (enforced in build).
- [x] `@KonditionalInternalApi` opt-in exists in `:konditional-core`.

### Phase 2 — Break circular dependencies

- [x] `NamespaceRegistry` exists as interface-only in `:konditional-core`.
- [x] Metrics / hooks contracts exist in `:konditional-core` and reference only primitives/core types.
- [x] Configuration “view” contracts exist in `:konditional-core` and are consumed by evaluation.

### Phase 3 — Move implementations into split modules

- [x] Registry implementations moved to `:konditional-runtime`.
- [x] Serialization codecs + configuration models moved to `:konditional-serialization`.
- [x] Observability/shadow utilities moved to `:konditional-observability`.

### Phase 4 — Remove `-Xfriend-paths`

- [x] `konditional-serialization/build.gradle.kts` has no `-Xfriend-paths`.
- [x] `konditional-observability/build.gradle.kts` has no `-Xfriend-paths`.

### Phase 5 — Gradle + dependents consolidation

- [x] Downstream modules depend on the split modules (no `:core`).
- [x] CI/Makefile does not reference `:core`.

### Phase 6 — Tests migrated

- [x] Tests are owned by the correct modules.
- [x] No reliance on `:core:test` or `:core:testFixtures`.

### Phase 7 — Docs + API surface updated

- [ ] Docs updated to new dependency coordinates and operations.
- [ ] Migration guide exists (`:core` → split modules).

### Phase 8 — Remove legacy `:core`

- [x] `include("core")` removed from `settings.gradle.kts`.
- [x] `core/` directory deleted.
- [x] No `project(":core")` references remain.
- [ ] `make check` passes.
- [ ] `llm-docs/scripts/extract-llm-context.sh` executed and outputs updated.

