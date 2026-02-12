# Konditional — Agent Instructions (AGENTS.md)

You are an agent working inside the **Konditional** repository. Your job is to produce **top-tier Kotlin** implementations: type-driven, deterministic, and enterprise-grade. Favor compile-time guarantees over runtime checks. Do not introduce dependency injection frameworks or reflection-heavy registries unless explicitly required.

## Repository map (expected)
Top-level modules/directories you should assume exist and prefer:
- `konditional-core/` — core algebra, types, evaluation semantics
- `konditional-runtime/` — runtime registry, lifecycle, snapshot/atomic updates
- `konditional-serialization/` — JSON boundary, codecs, parse results
- `konditional-observability/` — explainability, tracing, shadowing/mismatch reporting
- `konditional-spec/` — specifications, contracts, conformance fixtures
- `openapi/` — OpenAPI artifacts and generation inputs/outputs
- `openfeature/` — OpenFeature integration surface
- `kontracts/` — Kontract DSL / schema tooling (OpenAPI generation, etc.)
- `build-logic/` — Gradle plugins/conventions used by the build
- `detekt-rules/` — static analysis rules
- `llm-docs/` — design documents and invariants you must obey
- `docusaurus/` — documentation site

If a referenced file is missing, **search for it** (filename or closest equivalent) before proceeding.

---

## Hard invariants (do not violate)

### 1) Kotlin-first, type-safe by default
- Model domain constraints using Kotlin types: `sealed` ADTs, `value class` identifiers, delegating wrappers, variance, `inline` + `reified` generics where helpful.
- Prefer exhaustive `when` over open hierarchies.
- Avoid nullability; model absence explicitly.

### 2) Parse, don’t validate (boundary discipline)
- Treat all external inputs (JSON, OpenAPI payloads, HTTP bodies, files) as **untrusted**.
- Decode into a **trusted typed model** that cannot represent invalid state.
- No exceptions for control flow at boundaries: return typed error ADTs.

### 3) Determinism by construction
- Same inputs must yield same outputs.
- No ambient time, randomness, global state, or unstable iteration order in core evaluation.
- Any ordering must be explicitly stabilized with deterministic tie-breakers.

### 4) Atomic snapshot state
- Readers must never observe partial updates.
- Prefer immutable snapshots swapped atomically (e.g., `AtomicReference<Snapshot>`).
- Updates must be linearizable: either old snapshot or new snapshot.

### 5) Namespace isolation and blast radius control
- Keep operations scoped: namespace → feature → flag/rule (as applicable).
- Avoid global registries that mix concerns or allow accidental cross-namespace mutation.

### 6) Migration/shadowing without behavior drift
- Support dual-run/shadow evaluation and mismatch reporting without changing baseline results.
- Observability must not alter evaluation semantics.

---

## Required reading (repo-relative)
Treat these as source-of-truth for constraints and terminology:
- [`docusaurus/docs/theory/type-safety-boundaries.md`](docusaurus/docs/theory/type-safety-boundaries.md)
- [`docusaurus/docs/theory/namespace-isolation.md`](docusaurus/docs/theory/namespace-isolation.md)
- [`docusaurus/docs/theory/determinism-proofs.md`](docusaurus/docs/theory/determinism-proofs.md)
- [`docusaurus/docs/theory/parse-dont-validate.md`](docusaurus/docs/theory/parse-dont-validate.md)
- [`docusaurus/docs/theory/atomicity-guarantees.md`](docusaurus/docs/theory/atomicity-guarantees.md)
- [`docusaurus/docs/theory/migration-and-shadowing.md`](docusaurus/docs/theory/migration-and-shadowing.md)
- [`.signatures/INDEX.sig`](.signatures/INDEX.sig)

Schema/contract inputs (often needed for boundary work):
- `openapi/` (OpenAPI specs/artifacts)
- `openapi.json` or `openapi/*.json` (if present)
- `konditional-serialization/` (codecs)
- `kontracts/` (OpenAPI/spec generation DSL)

---

## What “world-class Kotlin” means here (quality bar)

### Public API discipline
- Small, opinionated, stable surface. Hide internals aggressively (`internal`, sealed boundaries, package scoping).
- Every public type/function must have KDoc describing:
    - invariants
    - determinism assumptions
    - boundary expectations
    - error semantics

### Total, explicit error handling
- Prefer `sealed interface Error` + `sealed interface Result` patterns.
- No `null`, no `Throwable` propagation across module boundaries unless explicitly defined as part of the API contract.

### Testing requirements (non-negotiable)
For any meaningful change, add tests that prove invariants:
- **Determinism tests**: same inputs → same output; ordering stabilized.
- **Boundary tests**: decoding failures produce typed errors with precise paths/fields.
- **Atomicity tests**: concurrent read/write never yields partial state; readers observe whole snapshots.
- **Namespace isolation tests**: operations cannot leak across namespaces.
- **Golden fixtures**: JSON fixtures for serialization/openapi shapes when relevant.

Prefer property-based tests when they increase confidence (ordering, bucketing, reducers).

---

## Agent workflow (how you should operate)

### Step 0 — Locate invariants and the right module
Before writing code:
1) Identify which modules are affected (`konditional-core`, `runtime`, `serialization`, `observability`, `spec`).
2) Read the relevant `llm-docs/*` files for invariants involved.
3) Search for existing patterns/types; reuse them rather than inventing parallel structures.

### Step 1 — Write an “Assertability Plan” in your head
Do not output it unless asked, but you must follow it:
- List invariants to preserve.
- List tests to add proving each invariant.
- Identify boundary points and ensure parse/don’t-validate is respected.

### Step 2 — Implement in vertical slices
1) Core types / ADTs (pure)
2) Pure evaluation semantics (deterministic)
3) Runtime snapshot/registry (atomic)
4) Serialization boundary (typed parsing + errors)
5) Observability/shadow/migration hooks

### Step 3 — Prove it
- Add tests before declaring completion.
- Keep hot paths allocation- and complexity-aware (but don’t micro-optimize prematurely).

---

## Kotlin style constraints (strong opinions)
- Prefer:
    - `sealed interface` for domain sums
    - `@JvmInline value class` for identifiers and stable keys
    - `data class` for immutable product types
    - `object` singletons only for stateless values (no mutable caches)
- Avoid:
    - reflection-based type registries
    - global mutable singletons
    - DI frameworks
    - `Map<String, Any>` style payload models in core logic

---

## Completion checklist (must be true before you stop)
- Code compiles.
- Tests added and pass.
- Public API documented (KDoc).
- Determinism, atomicity, isolation, and boundary constraints are preserved.
- No new “stringly typed” identifiers or ad-hoc parsing in core modules.
- Any new JSON shape is backed by fixtures and (where applicable) OpenAPI/schema alignment.

```xml
<!-- Optional structural prompt markers (kept literal via fenced block) -->
<kotlin_mastery_rules enabled="true" />
```
