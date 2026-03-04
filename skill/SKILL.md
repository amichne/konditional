---
name: konditional
description: >
  Use this skill when integrating Konditional into a codebase — whether you are
  a new user defining your first typed namespace or an enterprise team migrating
  from string-key flag systems. Covers namespace declaration, context modeling,
  evaluation, remote config loading, gradual rollout, shadow migration, and
  OpenFeature interoperability.
---

# Konditional skill (integration and adoption)

This skill is the canonical guide for adopting Konditional in this repository.
Use it for first-time integrations, greenfield namespace design, and enterprise
migration from legacy string-key or config-map systems.

## Primary outcomes

- Define typed namespaces and evaluate flags with compile-time safety.
- Load remote configuration through a boundary-safe snapshot loader.
- Migrate from string-key systems without behavior drift using dual-read and shadowing.
- Keep observability side-channel only; baseline semantics are never altered.

## Trigger when

- An engineer is adding Konditional to a service for the first time.
- A team is defining namespaces and typed feature surfaces for a new domain.
- You are replacing string keys, config maps, or untyped SDK wrappers with typed APIs.
- Integrations require runtime snapshot loading, rollback, and kill-switch safety.
- You need OpenFeature interoperability with typed context mapping.
- You are planning phased rollout with mismatch observability and rollback drills.

## Execution mode stack (optimized)

Apply these modes in order. The first matching mode controls the next action.

1. **Kotlin mastery mode (default for Kotlin code changes)**
   - Design for consumers first, not one-off call sites.
   - Enforce correctness at compile time with sealed hierarchies, value classes,
     variance, constrained constructors, and reified generics when needed.
   - Prefer immutable models, expression-oriented Kotlin, and Kotlin-native
     constructs over Java-isms.
   - Treat all boundary inputs as untrusted; parse into trusted models and return
     typed error ADTs instead of using exceptions for control flow.
   - Keep core semantics deterministic: no ambient time/randomness/global mutable
     state, and no unstable ordering without explicit deterministic tie-breakers.
   - For concurrent state, require atomic immutable snapshots so readers observe
     either old or new state, never partial updates.
   - Keep blast radius scoped by namespace/module/feature boundaries. Avoid
     global mutable registries and stringly typed core models.
   - For migrations, support shadow/dual-run mismatch reporting without changing
     baseline behavior.
   - When coroutines are involved: define `CoroutineContext`, preserve
     structured concurrency, and make exception propagation explicit.
   - For ambiguous requirements: present at least two valid interpretations,
     compare trade-offs, then choose one and justify it.

2. **Architecture review mode (enterprise readiness and risk discovery)**
   - Use critical review framing: identify high-impact weaknesses, not style nits.
   - Evaluate at minimum:
     - type safety and compile-time guarantees
     - API surface design coherence and extensibility
     - enterprise concerns (observability, stateless scale, compatibility)
     - maintainability and complexity scaling for large teams
   - Classify findings by severity: `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`.
   - For every finding, include issue, impact, and concrete recommendation.

3. **IntelliJ semantic mode (when symbol identity matters)**
   - Check IDE indexing availability first with `ide_index_status`.
   - If available, prefer semantic tools:
     - `ide_find_definition` for symbol origin
     - `ide_find_references` for usage impact
     - `ide_find_implementations` and `ide_type_hierarchy` for abstraction flow
     - `ide_refactor_rename` for safe renames
     - `ide_diagnostics` after non-trivial edits
   - Use `rg`/`rg --files` only for non-symbol text discovery or fallback.

4. **Gradle/LSP/debug mode (tooling setup, validation, or failures)**
   - Use `./gradlew` when present. Do not use system Gradle.
   - Invoke the `kotlin-jvm-lsp-gradle-debug` workflow before executing Gradle
     verification loops or debugger attach workflows.
   - Baseline toolchain:
     - `java -version`
     - `./gradlew -v`
     - `./gradlew tasks --all`
   - Stabilize baseline before deeper work:
     - `./gradlew clean test`
   - Validate debug attach path:
     - `./gradlew run --debug-jvm` or `./gradlew test --debug-jvm`
     - Attach JDWP client to `localhost:5005` unless overridden
   - If broken, use fixed triage:
     - `./gradlew --stop`
     - `./gradlew clean build --refresh-dependencies`
     - capture exact failing command, stack trace, and minimal repro module
   - LSP backend selection must be explicit:
     - prefer `Kotlin/kotlin-lsp`
     - use `fwcd/kotlin-language-server` only with stated trade-offs

## Enterprise adoption artifact contract

For mature enterprise adoption requests, produce these artifacts explicitly:

1. **Adoption architecture brief**
   - Namespace ownership model and blast-radius boundaries.
   - Module-level integration map (`core`, `runtime`, `serialization`,
     `observability`, `openfeature`).
2. **Migration specification**
   - Deterministic inventory mapping `legacy_key -> namespace.feature`.
   - Baseline/candidate dual-run plan with promotion and rollback gates.
3. **Boundary and error model**
   - Untrusted input inventory and parser boundary definitions.
   - Typed parse-failure handling and last-known-good snapshot strategy.
4. **Verification matrix**
   - Determinism tests, boundary failure tests, isolation tests, and
     atomicity tests where concurrency applies.
5. **Operations and observability plan**
   - Shadow mismatch telemetry, SLO-aligned alert thresholds, and kill-switch
     drill cadence.

## Current contracts (signature-backed)

- **Namespace-first definitions**: define flags directly on
  `object : Namespace("id")` via delegated properties (`boolean`, `string`,
  `integer`, `double`, `enum`, `custom`). [CLM-NS-001]
- **Typed context model**: use `Context` plus mix-ins (`LocaleContext`,
  `PlatformContext`, `VersionContext`, `StableIdContext`) and custom subtype
  extensions for business fields. [CLM-CTX-001]
- **Deterministic rule DSL**: use `rule(value)`, `enable`, `disable`, `anyOf`,
  `axis`, `extension`, `versions`, `rampUp`, `allowlist`, and `ruleSet`.
  [CLM-DSL-001]
- **Namespace isolation by construction**: each namespace owns its registry and
  axis handles remain explicit and deterministic by stable axis id.
  [CLM-ISO-001]
- **Runtime lifecycle in runtime module**: `Namespace.load`,
  `Namespace.rollback`, `history`, and `historyMetadata` live in
  `:konditional-runtime`. [CLM-RT-001]
- **Boundary-safe snapshot loading**: `NamespaceSnapshotLoader` decodes with
  compiled schema, returns typed parse failures, and keeps namespace context in
  boundary errors. [CLM-BND-002]
- **Typed parse failure model**: boundary failures are typed `ParseError`
  variants and introspectable through `parseErrorOrNull()`. [CLM-BND-001]
- **Shadow migration safety**: `evaluateWithShadow` returns baseline value and
  reports mismatch side-channel only. [CLM-SHD-001]

See `skill/resources/evidence-map.md` for exact signature/source/test links.

## New user / first integration workflow

Use this path when adding Konditional to a service that has no prior flag system.

1. **Add dependencies**
   ```kotlin
   // build.gradle.kts
   dependencies {
       implementation("io.amichne:konditional-core:0.1.0")
       implementation("io.amichne:konditional-runtime:0.1.0")
       implementation("io.amichne:konditional-serialization:0.1.0")
   }
   ```

2. **Declare a namespace**

   Group flags by team or domain ownership. One namespace = one blast-radius boundary.
   ```kotlin
   object AppFlags : Namespace("app") {
       val darkMode   by boolean<Context>(default = false)
       val checkoutV2 by boolean<Context>(default = false)
   }
   ```

3. **Model your context**

   Extend `Context` with the fields your rules will target. Keep it a data class.
   ```kotlin
   data class AppContext(
       override val stableId: StableId?,
       val platform: String,
       val region: String,
   ) : Context, StableIdContext
   ```

4. **Evaluate a flag**

   Evaluation is a pure function: same context → same result.
   ```kotlin
   val ctx = AppContext(stableId = StableId("user-123"), platform = "ios", region = "us")
   val showDarkMode: Boolean = AppFlags.darkMode.evaluate(ctx)
   ```

5. **Load remote configuration safely**

   Parse at the boundary. Keep last-known-good on failure.
   ```kotlin
   import io.amichne.konditional.serialization.snapshot.NamespaceSnapshotLoader
   import io.amichne.konditional.core.result.parseErrorOrNull

   val result = NamespaceSnapshotLoader(AppFlags).load(remoteJson)
   result.onFailure { failure ->
       logger.error("config rejected: ${failure.parseErrorOrNull()?.message}")
       // Last-known-good snapshot remains active
   }
   ```

6. **Add a gradual rollout rule**

   Use `rampUp` for deterministic percentage-based rollout tied to `stableId`.
   ```kotlin
   object AppFlags : Namespace("app") {
       val checkoutV2 by boolean<AppContext>(default = false) {
           rule(true) { rampUp(percentage = 10) }
       }
   }
   ```

7. **Write integration tests**

   Test determinism, isolation, and boundary failures before shipping.
   ```kotlin
   @Test fun `same context always gets same bucket`() {
       val ctx = AppContext(stableId = StableId("abc"), platform = "ios", region = "us")
       repeat(100) {
           assertEquals(AppFlags.checkoutV2.evaluate(ctx), AppFlags.checkoutV2.evaluate(ctx))
       }
   }
   ```

## Enterprise migration workflow

Use this path when replacing a legacy string-key or config-map flag system.

1. **Discovery and inventory**
   - Build a deterministic inventory of existing keys, call sites, defaults, and
     owners.
   - Prefer signatures first, then scoped text search in app repos.
   - Produce a migration table keyed by `legacy_key -> namespace.feature`.
2. **Namespace and ownership mapping**
   - Group flags by team/domain blast radius into namespace boundaries.
   - Keep one rollback/kill-switch control plane per namespace.
3. **Type conversion**
   - Replace string lookups with delegated typed features.
   - Move untyped context maps into typed `Context` subtypes and axes.
4. **Adapter phase (no behavior drift)**
   - Add a dual-read adapter that compares legacy value vs Konditional value.
   - Keep legacy value as baseline until mismatch rate is below rollout target.
5. **Runtime boundary hardening**
   - Load snapshots with `NamespaceSnapshotLoader` and typed error handling.
   - Preserve last-known-good snapshot on failures.
6. **Shadow and promotion**
   - Use `evaluateWithShadow` and emit structured mismatch telemetry.
   - Promote candidate only after deterministic gates pass.
7. **Integration finalization**
   - Add OpenFeature provider/mappers for external clients.
   - Run rollback and kill-switch drills before production cutover.

## Token-efficient execution workflow

1. Start with `.signatures/INDEX.sig`.
2. Open only relevant `*.sig` files for target modules and symbols.
3. For symbol-aware work, gate on `ide_index_status` and prefer IntelliJ MCP
   semantic tools before broad text search.
4. Read source only for symbols already selected from signatures.
5. Reuse existing docs samples and tests before introducing new patterns.
6. Keep all guidance scoped to one module boundary at a time:
   - `konditional-core`
   - `konditional-runtime`
   - `konditional-serialization`
   - `konditional-observability`
   - `openfeature`

## Response contract for this skill

- For new users: start with namespace declaration and evaluate() before introducing
  remote config or shadowing.
- For migrations: produce adoption guidance in phases, not one-shot rewrites.
- Always include: boundary assumptions, rollback plan, and mismatch strategy.
- Prefer compile-ready snippets using current imports and APIs.
- Report exact commands executed and outcomes when validating behavior.
- For tooling/debug tasks, include: environment summary, selected LSP backend and
  debug adapter rationale, status (`working`, `partially working`, or `blocked`),
  and next concrete command.
- For architecture review tasks, structure output as:
  - `Executive Summary`
  - `Critical Findings` (severity-tagged with issue, impact, recommendation)
  - `Complexity Reduction Opportunities`
  - `Strengths`
  - `Next Steps`
- Avoid DI frameworks, reflection registries, and stringly-typed core models.
- Keep observability side-channel only; never alter baseline semantics.

## Completion gate (mandatory)

Do not declare completion until all are true:

1. For code changes, compilation is confirmed via `./gradlew build` or a
   narrower equivalent module build command.
2. Tests are added for new behavior and cover determinism, boundary errors,
   and migration/rollout behavior when applicable.
3. Test success is confirmed via executed Gradle tests (`./gradlew clean test`
   or narrower module-scoped equivalent) and reported.
4. Public API additions/changes include KDoc documenting invariants and error
   semantics.
5. Behavior claims are validated by executed commands/tests.
6. Verification commands and outcomes are reported clearly.
7. For tooling workflows, at least one build/test command and one debug attach
   path are validated.
8. For enterprise architecture-review requests, findings are severity-classified
   and include concrete remediation recommendations.

## Resources

- **Enterprise and API samples:**
  - `skill/resources/konditional_samples.kt`
- **Claim-to-signature/source/test map:**
  - `skill/resources/evidence-map.md`

## Quick usage template

**New user integration:**

1. Declare a namespace with typed delegated properties.
2. Define a context data class extending `Context` with your targeting fields.
3. Call `feature.evaluate(ctx)` and confirm the return type matches your declaration.
4. Load a JSON snapshot via `NamespaceSnapshotLoader` and handle `Result` explicitly.
5. Add a `rampUp` rule and write a determinism test.

**Enterprise migration:**

1. Identify target modules and invariants.
2. Build a legacy inventory and namespace mapping table.
3. Propose a dual-read and shadow rollout slice with explicit rollback gates.
4. Add boundary-safe loading with typed parse error handling for untrusted inputs.
5. Add tests for determinism, namespace isolation, boundary failures, and
   migration mismatch reporting.
