# Konditional — Agent Instructions (AGENTS.md)

You are an agent working inside the **Konditional** repository. Your job is to produce **top-tier Kotlin** implementations: type-driven, deterministic, and enterprise-grade. Favor compile-time guarantees over runtime checks. Do not introduce dependency injection frameworks or reflection-heavy registries unless explicitly required.

## Repository map (expected)
Top-level modules/directories you should assume exist and prefer:
- `konditional-core/` — core algebra, types, evaluation semantics
- `konditional-runtime/` — runtime registry, lifecycle, snapshot/atomic updates
- `konditional-serialization/` — JSON boundary, codecs, parse results
- `konditional-observability/` — explainability, tracing, shadowing/mismatch reporting
- `openapi/` — OpenAPI artifacts and generation inputs/outputs
- `openfeature/` — OpenFeature integration surface
- `kontracts/` — Kontract DSL / schema tooling (OpenAPI generation, etc.)
- `build-logic/` — Gradle plugins/conventions used by the build
- `detekt-rules/` — static analysis rules
- `llm-docs/` — design documents and invariants you must obey
- `docusaurus/` — documentation site

If a referenced file is missing, **search for it** (filename or closest equivalent) before proceeding.

---

## Common workflows and commands

Use these commands when you need established project workflows. Keep the scope
small and choose the narrowest command that matches the task.

- Build and test: `make build`, `make test`, `make detekt`, `make check`.
- Documentation site: `make docs-build`, `make docs-serve`, `make docs-clean`.
- Publish flow (interactive): `make publish`.
- Publish flow (non-interactive): run `make publish-plan` with
  `PUBLISH_TARGET=release` and `VERSION_CHOICE=patch`.
- Publish targets: `make publish-run-local`, `make publish-run-snapshot`,
  `make publish-run-release`, `make publish-run-github`,
  `./scripts/publish.sh {local|snapshot|release|github}`.
- Publish validation: `make publish-validate-local`, `make
  publish-validate-snapshot`, `make publish-validate-release`, `make
  publish-validate-github`.
- Version bump helpers: `./scripts/bump-version.sh {none|patch|minor|major}
  [--snapshot]`.
- Release prep shortcut: `./scripts/prepare-release.sh [version-choice]`.
- Signature artifacts: `./scripts/generate-signatures.sh`,
  `./scripts/check-signatures-drift.sh`.

---

## Module architecture and dependencies

**Layered structure (bottom-up):**

1. **`konditional-core`** — Pure evaluation semantics, core types (Namespace, Feature, Context), no I/O
   - Exports: `io.amichne.konditional.{api,context,core,rules,values}.*`
   - No dependencies on `runtime` or `serialization`
   
2. **`konditional-serialization`** — JSON codecs via Moshi, `ParseResult<T>` boundary type
   - Custom adapters: `ValueClassAdapterFactory` wraps `@JvmInline value class` types
   - Deterministic output: maps sorted by keys, explicit ordering
   
3. **`konditional-runtime`** — Atomic registry, snapshot loader, lifecycle (`load`, `rollback`, `dump`)
   - `InMemoryNamespaceRegistry` uses `AtomicReference<Configuration>` for linearizable updates
   - Side-effecting operations: `Namespace.update(Configuration)`, `Namespace.rollback(steps)`, `Namespace.dump()`
   
4. **`konditional-observability`** — Explain/trace APIs, shadow evaluation, mismatch detection
   
5. **`konditional-otel`** — OpenTelemetry bridge for metrics/spans

6. **`openfeature`** — OpenFeature provider integration

7. **`konditional-http-server`** — Ktor-based HTTP control plane

**Critical boundary:** `:konditional-core` must never depend on `:runtime` or `:serialization`. Runtime/serialization import core, not the reverse.

---

## Gradle conventions (build-logic plugins)

- `konditional.kotlin-library` — Base Kotlin/JVM setup
- `konditional.publishing` — Maven Central publishing config
- `konditional.detekt` — Static analysis with project-wide baseline
- `konditional.junit-platform` — JUnit 5 test config
- `konditional.core-api-boundary` — Enforces package export rules (see `konditional-core/build.gradle.kts`)

**Test fixtures:** Use `java-test-fixtures` plugin to share test utilities across modules (e.g., `testFixtures(project(":konditional-core"))`).

---

## Beads Is Persistent Memory (Optional)

If `bd` (Beads) is available locally, treat it as the canonical working memory for planning, status, and handoff. Do not rely on chat transcript memory for project state.

### Session start
```bash
bd ready --limit 50
bd list --status in_progress
```

### During execution
```bash
# Capture new work or discoveries
bd create "<clear task title>"

# Claim/track active work
bd update <issue-id> --status in_progress

# Append implementation notes, decisions, and blockers
bd update <issue-id> --append-notes "<what changed and why>"
```

### Session close
```bash
# Close completed issues and sync to git-native JSONL
bd update <issue-id> --status closed --notes "<verification + outcome>"
bd sync
```

**Note:** `bd` is not universally available. If missing, track work via git commits and PR descriptions.

---

## IntelliJ-Native MCP Navigation (idea-first)

For symbol-aware operations, use IntelliJ MCP tools first. Plain text search (`rg`) is fallback only when symbol identity is irrelevant.

### MCP server checks
 
If `idea` is missing in a local setup:

```bash
codex mcp add idea --url http://127.0.0.1:64343/stream
```

### Preferred symbol operations (when exposed by the idea MCP)
- Definition lookup
- Reference search
- Implementation search
- Type hierarchy
- Safe rename refactor
- IDE diagnostics after non-trivial edits

Fallback rule:
- Use `rg`/`rg --files` for plain-text discovery, logs, and non-symbol searches only.

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

### Theory documentation (docusaurus/docs/theory/)
- [`type-safety-boundaries.md`](docusaurus/docs/theory/type-safety-boundaries.md) — Parse/don't-validate, typed error boundaries
- [`namespace-isolation.md`](docusaurus/docs/theory/namespace-isolation.md) — Namespace-scoped state, blast radius control
- [`determinism-proofs.md`](docusaurus/docs/theory/determinism-proofs.md) — SHA-256 bucketing, stable rule ordering
- [`parse-dont-validate.md`](docusaurus/docs/theory/parse-dont-validate.md) — `ParseResult<T>` pattern, no exceptions at boundaries
- [`atomicity-guarantees.md`](docusaurus/docs/theory/atomicity-guarantees.md) — `AtomicReference<Configuration>`, linearizability
- [`migration-and-shadowing.md`](docusaurus/docs/theory/migration-and-shadowing.md) — Dual-eval, mismatch detection
- [`claims-registry.md`](docusaurus/docs/theory/claims-registry.md) — Claim IDs and verification index

### Schema/contract inputs (often needed for boundary work)
- `openapi/` (OpenAPI specs/artifacts)
- `konditional-serialization/` (Moshi codecs, snapshot format)
- `kontracts/` (Type-safe JSON Schema DSL for OpenAPI generation)

---

## Concrete type patterns (critical examples)

### Boundary result type (`ParseResult<T>`)
All external input parsing returns a sealed interface with typed errors:

```kotlin
sealed interface ParseResult<out T> {
    data class Success<T>(val value: T) : ParseResult<T>
    data class Failure(val error: ParseError) : ParseResult<Nothing>
}
```

For Kotlin `Result` compatibility, wrap errors in `KonditionalBoundaryFailure`:
```kotlin
fun <T> parseFailure(error: ParseError): Result<T> = 
    Result.failure(KonditionalBoundaryFailure(error))

fun Throwable.parseErrorOrNull(): ParseError? = 
    (this as? KonditionalBoundaryFailure)?.parseError
```

### Value class identifiers
All domain identifiers use `@JvmInline value class` for type safety:
```kotlin
@JvmInline value class FeatureId(val value: String)
@JvmInline value class StableId(val value: String)
```

Custom Moshi adapter: `ValueClassAdapterFactory` handles serialization automatically.

### Atomic state updates
Runtime registry uses `AtomicReference<Configuration>` for linearizable snapshots:
```kotlin
// konditional-runtime/InMemoryNamespaceRegistry.kt
private val configRef = AtomicReference<Configuration>(initial)

// Atomic swap on load
fun load(configuration: Configuration) {
    configRef.set(configuration)
}

// Lock-free read
val configuration: ConfigurationView
    get() = configRef.get()
```

### Namespace delegation pattern
Features register themselves via Kotlin property delegation:
```kotlin
object AppFlags : Namespace("app") {
    val darkMode by boolean<Context>(default = false) {
        rule(true) { platforms(Platform.IOS) }
    }
}
```

The `by` operator registers the feature in the namespace registry at initialization.

### Test fixtures sharing
Modules use `java-test-fixtures` plugin to share test utilities:
```kotlin
// konditional-core/build.gradle.kts
plugins {
    `java-test-fixtures`
}

// konditional-runtime/build.gradle.kts
dependencies {
    testImplementation(testFixtures(project(":konditional-core")))
}
```

Place shared test utilities in `src/testFixtures/kotlin/`.

---

## What "world-class Kotlin" means here (quality bar)

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
1) Identify which modules are affected. **Module responsibility map:**
   - **Core types, evaluation logic, context traits** → `konditional-core`
   - **JSON codecs, snapshot format, Moshi adapters** → `konditional-serialization`
   - **Registry lifecycle, `load`/`rollback`/`dump` operations** → `konditional-runtime`
   - **Explain API, shadow evaluation, tracing** → `konditional-observability`
   - **HTTP endpoints, Ktor server** → `konditional-http-server`
   - **OpenFeature provider bridge** → `openfeature`
   - **OpenTelemetry metrics/spans** → `konditional-otel`
   - **Type-safe JSON Schema DSL** → `kontracts`
   - **OpenAPI artifacts** → `openapi`
2) Read the relevant theory docs (`docusaurus/docs/theory/*.md`) for invariants involved.
3) Search for existing patterns/types in the target module; reuse rather than inventing parallel structures.
4) **Check dependency boundaries:** Core must never import runtime/serialization. Serialization/runtime import core.

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
- **Test categories required:**
  - Determinism: same context → same result (1000 iterations)
  - Boundary: malformed JSON → typed `ParseError`
  - Atomicity: concurrent load/evaluate never sees partial state
  - Namespace isolation: operations don't leak across namespaces
- Keep hot paths allocation- and complexity-aware (but don't micro-optimize prematurely).

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

## Landing the Plane (Session Completion)

**When ending a work session**, you MUST complete ALL steps below. Work is NOT complete until `git push` succeeds.

**MANDATORY WORKFLOW:**

1. **File issues for remaining work** - Create issues for anything that needs follow-up
2. **Run quality gates** (if code changed) - Tests, linters, builds
3. **Update issue status** - Close finished work, update in-progress items
4. **PUSH TO REMOTE** - This is MANDATORY:
````bash
   git pull --rebase
   bd sync
   git push
   git status  # MUST show "up to date with origin"
````
5. **Clean up** - Clear stashes, prune remote branches
6. **Verify** - All changes committed AND pushed
7. **Hand off** - Provide context for next session

**CRITICAL RULES:**
- Work is NOT complete until `git push` succeeds
- NEVER stop before pushing - that leaves work stranded locally
- NEVER say "ready to push when you are" - YOU must push
- If push fails, resolve and retry until it succeeds

Always use the OpenAI developer documentation MCP server (`openaiDeveloperDocs`) for OpenAI API, Codex, Apps SDK, Agents SDK, and model/tooling questions. Use web fallback only when MCP yields no meaningful results.
