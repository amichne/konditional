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
- `konditional-types/` → foundational shared types: contexts, identifiers, parse-result ADTs, `Konstrained` contracts (no I/O, no downstream deps)
- `konditional-engine/` → typed namespace definition, deterministic evaluation, atomic registry state, explainability hooks
- `konditional-json/` → JSON boundary: Moshi codecs and schema-aware parsing layered on engine types
- `kontracts/` → contracts/spec artifacts
- `smoke-test/` → integration smoke tests
- `docs/` → documentation site (MkDocs/Zensical)
- `skill/` → agent skill definition (`skill/SKILL.md`)

Hard boundaries:
- **`konditional-types` is downstream of nothing** in the main stack — no deps on engine or JSON.
- **`konditional-engine` depends only on `:konditional-types` and `:kontracts`** — no JSON-specific parsing concerns.
- **`konditional-json` is the parse boundary** — failed decode must never mutate live namespace state.

---

## 2) How to traverse the repo (recommended path)

Use this path before making non-trivial edits:

1. **Anchor on intent**
   - Find the owning module from the map above.
2. **Load invariants first**
   - Read `docs/*.md` relevant to your change.
3. **Find existing patterns**
   - Reuse local ADTs, loaders, adapters, registry patterns.
4. **Implement smallest vertical slice**
   - Types → semantics → engine boundary → JSON parsing.
5. **Prove behavior**
   - Add tests for determinism, boundary errors, atomicity, and isolation where relevant.

For agent-specific acceleration, inspect `skill/SKILL.md`.

---

## 3) Progressive disclosure playbook

### Level A — Fast path (small edits)
Use when change is localized and low-risk.
- Read owning module README/docs + nearby tests.
- Apply minimal patch.
- Run narrow test target first.

### Level B — Standard path (most tasks)
Use for any behavioral or API touching change.
- Read relevant docs under `docs/`.
- Check dependency direction constraints.
- Add/update tests proving invariants.

### Level C — Deep path (cross-module or boundary changes)
Use when touching parsing, engine evaluation, or contracts.
- Validate impacts across `konditional-types` / `konditional-engine` / `konditional-json`.
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
- Compile-only loops: `make compile`, `make compile-test`
- Clean/rebuild loops: `make clean`, `make rebuild`, `make full-clean`
- Docs site: `make docs-install`, `make docs-build`, `make docs-serve`, `make docs-clean`
- Publish planning + validation: `make publish-plan`, `make validate-publish`
- Publish execution: `make publish-local`, `make publish-snapshot`, `make publish-release`, `make publish-github`
- Version helpers: `make publish-version-{none|snapshot|patch|minor|major}`
- Legacy publish entrypoint (still available): `./scripts/publish.sh ...`

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

1. `docs/` for invariants, quickstart, and reference notes.
2. Module tests in the owning package for executable examples.
3. `skill/SKILL.md` for task-specific integration playbook.

If a referenced file is missing, search for closest equivalent before introducing a new pattern.

---

## 8) Quality bar for completion

Before stopping:
- Code compiles.
- Relevant tests pass.
- Public API changes include KDoc and explicit error semantics.
- Dependency boundaries remain valid.
- No new stringly-typed identifiers in core paths.
