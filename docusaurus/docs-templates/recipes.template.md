---
toc_min_heading_level: 2
toc_max_heading_level: 5
---

<!--
Generated file: do not edit recipes.md directly.
Source: docusaurus/docs-templates/recipes.template.md + konditional-observability/src/docsSamples/kotlin/io/amichne/konditional/docsamples/RecipesSamples.kt
-->

# Recipes: Best-Practice Patterns

Practical patterns for real-world feature control using only Konditional building blocks. Each recipe highlights a
supported solution area and makes the guarantee boundaries explicit.

Covered solution areas:

- Typed features (booleans, enums, structured values)
- Deterministic rollouts and salting
- Axes and custom context targeting
- Remote configuration (snapshot/patch boundary + rollback)
- Shadow evaluation for safe migrations
- Namespace isolation and kill-switch
- Observability hooks (logging + metrics)

---

## Typed Variants Instead of Boolean Explosion {#recipe-1-typed-variants-instead-of-boolean-explosion}

When you have multiple rollout variants, model them as a typed value (enum or string) rather than composing booleans.

{{recipe-1-typed-variants}}

- **Guarantee**: Variant values are compile-time correct and exhaustively handled.
- **Mechanism**: Enum-typed feature delegates (`enum<...>`) and Kotlin `when` exhaustiveness.
- **Boundary**: Remote JSON can only select enum constants already compiled into the binary.

---

## Deterministic Ramp-Up with Resettable Salt {#recipe-2-deterministic-ramp-up-with-resettable-salt}

Gradually roll out a feature without reshuffling users; use `salt(...)` when you need a clean resample.

{{recipe-2-rampup}}

To restart the experiment with a fresh sample:

{{recipe-2-reset}}

- **Guarantee**: Same `(stableId, flagKey, salt)` always yields the same bucket.
- **Mechanism**: SHA-256 deterministic bucketing in `RampUpBucketing`.
- **Boundary**: Changing `salt` intentionally redistributes buckets.

---

## Runtime-Configurable Segments via Axes {#recipe-3-runtime-configurable-segments-via-axes}

Use axes for segment targeting you want to update via JSON (without redeploying predicates).

{{recipe-3-axes}}

- **Guarantee**: Segment targeting is type-safe and serializable.
- **Mechanism**: Axis IDs are stored in JSON; `axis(...)` evaluates against `Context.axisValues`.
- **Boundary**: Axis IDs must remain stable across builds and obfuscation.

---

## Business Logic Targeting with Custom Context + Extension {#recipe-4-business-logic-targeting-with-custom-context-extension}

Use strongly-typed extensions for domain logic that should not be remotely mutable.

{{recipe-4-extension}}

- **Guarantee**: Extension predicates are type-safe and enforced at compile time.
- **Mechanism**: `Feature<T, EnterpriseContext>` makes the extension receiver strongly typed.
- **Boundary**: Extension logic is not serialized; only its rule parameters (e.g., ramp-up) can be updated remotely.

---

## Structured Values with Schema Validation {#recipe-5-structured-values-with-schema-validation}

Use `custom<T>` for structured configuration that must be validated at the JSON boundary.

{{recipe-5-structured}}

- **Guarantee**: Invalid structured config is rejected before it reaches evaluation.
- **Mechanism**: Kontracts schema validation at `ConfigurationSnapshotCodec.decode(...)`.
- **Boundary**: Semantic correctness of field values (e.g., "appropriate backoff") remains a human responsibility.

---

## Safe Remote Config Loading + Rollback {#recipe-6-safe-remote-config-loading-rollback}

Use `ParseResult` to enforce a hard boundary at the JSON parse step, and roll back on bad updates.

{{recipe-6-load}}

If a later update causes issues:

{{recipe-6-rollback}}

- **Guarantee**: Invalid config never becomes active; swaps are atomic.
- **Mechanism**: `ParseResult` boundary + `Namespace.load(...)` atomic swap.
- **Boundary**: A valid config can still be logically wrong; rollback is the safe escape hatch.

---

## Controlled Migrations with Shadow Evaluation {#recipe-7-controlled-migrations-with-shadow-evaluation}

Compare a candidate configuration to baseline behavior without changing production outputs.

{{recipe-7-shadow}}

- **Guarantee**: Production behavior stays pinned to baseline while candidate is evaluated.
- **Mechanism**: `evaluateWithShadow(...)` evaluates baseline + candidate but returns baseline value.
- **Boundary**: Shadow evaluation is inline and adds extra work to the hot path; sample if needed.

---

## Namespace Isolation + Kill-Switch {#recipe-8-namespace-isolation-kill-switch}

Use separate namespaces for independent lifecycles, and a scoped kill-switch for emergencies.

{{recipe-8-namespace}}

- **Guarantee**: Disabling a namespace only affects that namespace.
- **Mechanism**: Each `Namespace` has an isolated registry and kill-switch.
- **Boundary**: `disableAll()` returns defaults; it does not modify feature definitions or remote config state.

---

## Lightweight Observability Hooks {#recipe-9-lightweight-observability-hooks}

Attach logging and metrics without depending on a specific vendor SDK.

{{recipe-9-observability}}

- **Guarantee**: Hooks receive evaluation and lifecycle signals with consistent payloads.
- **Mechanism**: `RegistryHooks` are invoked inside the runtime's evaluation and load paths.
- **Boundary**: Hooks run on the hot path; keep them non-blocking.

---

## Next Steps

- [Rules & Targeting: Rule Composition](/rules-and-targeting/rule-composition)
- [Rules & Targeting: Rollout Strategies](/rules-and-targeting/rollout-strategies)
- [Fundamentals: Configuration Lifecycle](/learn/configuration-lifecycle)
- [Advanced: Shadow Evaluation](/advanced/shadow-evaluation)
- [API Reference: Observability](/api-reference/observability)
