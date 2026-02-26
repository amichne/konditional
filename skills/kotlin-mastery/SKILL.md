---
name: kotlin-mastery
description: >
  Produce type-driven, deterministic, test-proven Kotlin with strict boundary discipline.
  Use when writing code that handles untrusted input, domain modeling, data transformation,
  concurrent state, or API boundaries where correctness and compile-time safety are critical.
  Prefer over kotlin-architect when parse-don't-validate, determinism invariants, or
  typed error ADTs are the primary concern.
---

# Kotlin Mastery

Generate Kotlin that favors compile-time guarantees over runtime checks. No DI frameworks
or reflection-heavy registries unless explicitly required.

## Hard Invariants

These rules are non-negotiable and apply to all output.

### Type Safety First

- Model domain constraints using Kotlin types: sealed ADTs, value classes for identifiers,
  delegation, variance, inline + reified generics where helpful.
- Prefer exhaustive `when` expressions; avoid catch-all `else` when the domain can be fully modeled.
- Avoid nullability in core logic — model absence explicitly (e.g., `Option`/`sealed`).

### Parse, Don't Validate (Boundary Discipline)

- All external inputs (JSON, network payloads, files, DB rows, env vars) are untrusted.
- Decode/parse into a trusted typed model that **cannot represent invalid state**.
- Do not use exceptions for control flow at boundaries — return typed error ADTs.

### Determinism by Construction

- Same inputs must yield same outputs.
- No ambient time, randomness, global state, or unstable iteration order in core logic.
- Any ordering must be explicit, stable, and have deterministic tie-breakers.

### Atomic Snapshot State *(when concurrency is relevant)*

- Readers must never observe partial updates.
- Prefer immutable snapshots swapped atomically (`AtomicReference<Snapshot>`), or confinement
  to a single owner thread/dispatcher with explicit ownership transfer.
- Updates must be linearizable: readers see either the old snapshot or the new one — never in-between.

### Isolation Boundaries

- Keep operations scoped: subsystem → module → feature → entity.
- No global mutable singletons or "god registries" that mix concerns and expand blast radius.

### Safe Migration / Shadowing *(when evolving systems)*

- Support dual-run / shadow evaluation and mismatch reporting without changing baseline results.
- Observability must not alter core semantics.

## Quality Standards

### Public API Discipline

- Keep the public API small, opinionated, and stable; hide internals aggressively
  (`internal`, sealed boundaries, package scoping).
- Every public type/function must have KDoc documenting: invariants, guarantees,
  determinism assumptions, boundary expectations, error semantics, and performance notes.

### Error Handling

- Prefer `sealed Error` + `sealed Result` (or `Either`-style) patterns.
- No null returns in core logic.
- No unchecked exceptions crossing module boundaries unless explicitly part of the contract.

### Performance Posture

- Be performance-aware without premature micro-optimization.
- Keep hot paths allocation-conscious and predictable.
- Prefer stable data structures and deterministic behavior over clever tricks.

## Kotlin Design Defaults

**Prefer:**
- `sealed interface` / `sealed class` for domain sums
- `@JvmInline value class` for IDs, keys, normalized tokens, stable identifiers
- Immutable `data class` product models with `val` properties
- `by` delegation for behavioral composition
- `in`/`out` variance to encode substitutability at compile time
- `inline + reified` for type witnesses when it reduces runtime branching
- Explicit constructors/factories over DI frameworks

**Avoid (unless explicitly required):**
- Reflection-based registries or discovery
- Runtime type checks for compatibility expressible via generics/variance
- `Map<String, Any>` or stringly typed models in core logic
- Global mutable singletons (especially caches) without strong rationale and tests
- Exception-first boundary behavior

## Workflow

Work in vertical slices, in order:

1. **Determine boundaries** — Identify untrusted inputs, invariants to enforce by types,
   and concurrency/atomicity requirements.
2. **Assertability plan** *(internal)* — List invariants to preserve, tests that prove each,
   the trusted domain model, and the boundary parser.
3. **Implement slices:**
   - Core types / ADTs (pure)
   - Pure semantics (deterministic)
   - Boundary parsing + typed errors
   - Runtime integration (state, IO, concurrency) — **invoke `kotlin-jvm-lsp-gradle-debug`
     before writing runtime integration code** to confirm the Gradle baseline is stable and
     the build/test loop is validated. Do not assume the environment is healthy.
   - Observability hooks that do **not** change semantics
4. **Prove correctness** — Add tests before declaring completion. Prefer property-based tests
   where determinism, ordering, reducers, or bucketing matter.
   - **Invoke `kotlin-jvm-lsp-gradle-debug`** to execute and validate the test suite
     (`./gradlew clean test`). Never self-report tests as passing without running them.

## Testing Requirements

**Always include:**
- Unit tests for core logic and ADT exhaustiveness expectations.
- Boundary tests for parse failures with field/path specificity and error typing.

**Include when applicable:**
- Determinism tests (same inputs → same outputs).
- Concurrency/atomicity smoke tests (no partial reads).
- Golden fixtures for serialization shapes.
- Compatibility tests for migrations/shadowing.

## Runtime Tooling Rules

These rules govern all interactions with the JVM build and test environment.

- **Always delegate to `kotlin-jvm-lsp-gradle-debug`** before executing any Gradle command,
  attaching a debugger, or diagnosing build/classpath failures. Do not reinvent its workflow.
- **Stabilize the Gradle baseline first.** If `./gradlew clean test` does not pass before
  implementation begins, surface the failure immediately — do not proceed with new code.
- **Do not report compilation success from static analysis alone.** Compilation must be
  confirmed via `./gradlew build` or equivalent Gradle task.
- **Debug attach must use the `kotlin-jvm-lsp-gradle-debug` workflow** (`--debug-jvm` flag,
  JDWP on `localhost:5005`) unless the build defines a different port.
- **On tooling failure**, follow the recovery sequence defined in
  `skills/kotlin-jvm-lsp-gradle-debug/references/command-playbook.md` before escalating.

## Completion Gate

Do not declare completion until all are true:

- [ ] Code compiles — confirmed via `./gradlew build` (not inferred).
- [ ] Tests added and pass — confirmed via `./gradlew clean test` output.
- [ ] Public API has KDoc.
- [ ] Invariants preserved: type safety, determinism, boundary discipline.
- [ ] No new stringly typed identifiers or ad-hoc parsing in core modules.
- [ ] Any new external shape is backed by fixtures and typed parsing.
