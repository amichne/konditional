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
  axis catalog; typed axis inference remains namespace-scoped. [CLM-ISO-001]
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

1. Start with `signatures/INDEX.sig`.
2. Open only relevant `*.sig` files for target modules and symbols.
3. Use IntelliJ semantic symbol lookup for definitions/references before
   broad text search.
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
- Avoid DI frameworks, reflection registries, and stringly-typed core models.
- Keep observability side-channel only; never alter baseline semantics.

## Resources

- **Enterprise and API samples:**
  - `skill/resources/konditional_samples.kt`
- **Claim-to-signature/source/test map:**
  - `skill/resources/evidence-map.md`
- **OpenAI prompt/tooling best practices for skill authoring:**
  - `skill/resources/openai_prompting_best_practices.md`

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
