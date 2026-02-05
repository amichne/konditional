---
title: Reference Index (Code Pointers)
---

# Reference Index (Direct File Pointers)

This section is intentionally “boring”: it is a set of stable pointers you can use to validate behavior claims and
answer “what actually happens?” by reading the source.

Core types:

- `konditional-core/src/main/kotlin/io/amichne/konditional/core/Namespace.kt` — namespace definition + property delegates
- `konditional-core/src/main/kotlin/io/amichne/konditional/core/features/Feature.kt` — `Feature<T, C, M>` typed handle
- `konditional-core/src/main/kotlin/io/amichne/konditional/core/FlagDefinition.kt` — evaluation algorithm + ramp-up gating

Evaluation & results:

- `konditional-core/src/main/kotlin/io/amichne/konditional/api/FeatureEvaluation.kt` — `evaluate` / `explain` decision flow
- `konditional-core/src/main/kotlin/io/amichne/konditional/api/EvaluationResult.kt` — `EvaluationResult<T>` shape (decision types)
- `konditional-core/src/main/kotlin/io/amichne/konditional/api/RampUpBucketing.kt` — deterministic bucketing helpers

Rules:

- `konditional-core/src/main/kotlin/io/amichne/konditional/rules/Rule.kt` — rule model + base targeting
- `konditional-core/src/main/kotlin/io/amichne/konditional/rules/ConditionalValue.kt` — `rule → value` pairing

Context & targeting:

- `konditional-core/src/main/kotlin/io/amichne/konditional/context/Context.kt` — context capabilities (mixins)
- `konditional-core/src/main/kotlin/io/amichne/konditional/context/axis/Axis.kt` — axis definition + helpers
- `konditional-core/src/main/kotlin/io/amichne/konditional/context/axis/AxisValue.kt` — stable axis value contract
- `konditional-core/src/main/kotlin/io/amichne/konditional/context/RampUp.kt` — typed ramp-up percentage
- `konditional-core/src/main/kotlin/io/amichne/konditional/core/id/StableId.kt` — stable identity (hex-backed)
- `konditional-core/src/main/kotlin/io/amichne/konditional/core/id/HexId.kt` — hex identity type

DSL:

- `konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/FlagScope.kt` — flag authoring DSL surface
- `konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/RuleScope.kt` — rule authoring DSL surface
- `konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/RuleSet.kt` — composition primitives

Registry:

- `konditional-core/src/main/kotlin/io/amichne/konditional/core/registry/NamespaceRegistry.kt` — consumer-safe registry surface
- `konditional-core/src/main/kotlin/io/amichne/konditional/core/instance/ConfigurationView.kt` — flags + metadata view
- `konditional-core/src/main/kotlin/io/amichne/konditional/core/ops/RegistryHooks.kt` — logging + metrics hooks

Structured values & parsing:

- `konditional-core/src/main/kotlin/io/amichne/konditional/core/types/Konstrained.kt` — schema-backed value contract
- `konditional-core/src/main/kotlin/io/amichne/konditional/core/result/ParseResult.kt` — result type for parsing boundaries
- `konditional-core/src/main/kotlin/io/amichne/konditional/core/result/ParseError.kt` — typed parse failure model
