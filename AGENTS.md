# Konditional — Agent Navigation Guide

This file is intentionally optimized for **progressive disclosure**: start shallow, go deeper only when needed.

Your objective in this repository is to produce **type-safe, deterministic Kotlin changes** that preserve atomicity and namespace isolation.

---

## 0) Quick orientation (read this first)

Konditional is a Kotlin feature-flag platform with strong invariants:
- Parse untrusted input into trusted typed models.
- Keep evaluation deterministic.
- Keep runtime updates atomic.
- Keep namespace operations isolated.

If you only remember one rule: **do not trade away compile-time guarantees for convenience**.

---

## 1) What this repository contains

Top-level areas and their purpose:
- `konditional-core/` → pure domain model + evaluation semantics (no I/O)
- `konditional-serialization/` → JSON boundary + Moshi adapters + parse results
- `konditional-runtime/` → snapshot lifecycle + registry atomicity
- `konditional-observability/` → explainability, tracing, shadow/mismatch reporting
- `konditional-otel/` → OpenTelemetry integration
- `konditional-http-server/` → Ktor control plane
- `openfeature/` → OpenFeature provider surface
- `kontracts/` + `openapi/` → contracts/spec generation and artifacts
- `docusaurus/` → documentation site (theory + guides)
- `.agents/` → reusable agent skills, scripts, and references distributed with the repo

Hard boundary: **`konditional-core` must never depend on runtime or serialization**.

---

## 2) How to traverse the repo (recommended path)

Use this path before making non-trivial edits:

1. **Anchor on intent**
   - Find the owning module from the map above.
2. **Load invariants first**
   - Read `docusaurus/docs/theory/*.md` relevant to your change.
3. **Find existing patterns**
   - Reuse local ADTs, loaders, adapters, registry patterns.
4. **Implement smallest vertical slice**
   - Types → semantics → runtime boundary → observability hooks.
5. **Prove behavior**
   - Add tests for determinism, boundary errors, atomicity, and isolation where relevant.

For agent-specific acceleration, inspect `.agents/skills/*/SKILL.md` and only load references/scripts needed for your task.

---

## 3) Progressive disclosure playbook

### Level A — Fast path (small edits)
Use when change is localized and low-risk.
- Read owning module README/docs + nearby tests.
- Apply minimal patch.
- Run narrow test target first.

### Level B — Standard path (most tasks)
Use for any behavioral or API touching change.
- Read relevant theory docs under `docusaurus/docs/theory/`.
- Check dependency direction constraints.
- Add/update tests proving invariants.

### Level C — Deep path (cross-module or boundary changes)
Use when touching parsing, runtime snapshots, migration/shadowing, or contracts.
- Validate impacts across core/serialization/runtime/observability.
- Ensure no behavior drift in baseline evaluation.
- Add fixtures or contract artifacts when JSON/OpenAPI shapes change.

---

## 4) Non-negotiable engineering rules

1. **Kotlin-first type safety**
   - Prefer `sealed interface`, `@JvmInline value class`, immutable `data class`.
2. **Parse, don’t validate**
   - External input must return typed boundary failures (no exception-driven control flow).
3. **Determinism by construction**
   - Stabilize ordering; avoid ambient time/randomness in evaluation paths.
4. **Atomic snapshots**
   - Readers must see old-or-new snapshots only, never partial updates.
5. **Namespace isolation**
   - Scope operations per namespace; avoid cross-namespace mutation surfaces.
6. **Observability is non-invasive**
   - Explain/shadow paths must not alter evaluation semantics.

---

## 5) Commands you should prefer

- Build/test/check: `make build`, `make test`, `make detekt`, `make check`
- Docs site: `make docs-build`, `make docs-serve`, `make docs-clean`
- Publish helpers: `make publish-plan`, `./scripts/publish.sh ...`

Choose the **narrowest command** that validates your change.

---

## 6) Testing expectations

For meaningful behavior changes, prove the relevant invariants:
- determinism (repeat evaluations)
- boundary parsing failures (typed errors)
- atomicity under concurrent load/read
- namespace isolation

Use test fixtures (`java-test-fixtures`) for shared helpers where appropriate.

---

## 7) Where to look when stuck

1. `docusaurus/docs/theory/` for invariants and proofs.
2. `docusaurus/docs/reference/` for canonical API/snapshot notes.
3. Module tests in the owning package for executable examples.
4. `.agents/skills/` for task-specific scripts and reference playbooks.

If a referenced file is missing, search for closest equivalent before introducing a new pattern.

---

## 8) Quality bar for completion

Before stopping:
- Code compiles.
- Relevant tests pass.
- Public API changes include KDoc and explicit error semantics.
- Dependency boundaries remain valid.
- No new stringly-typed identifiers in core paths.
