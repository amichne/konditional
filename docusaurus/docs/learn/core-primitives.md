# Core concepts

This page gives you the minimum shared vocabulary for reading and writing
Konditional code.

## Read this page when

- You are onboarding to the DSL.
- You are reviewing code that mixes core, runtime, and serialization terms.
- You need a quick map before diving into API details.

## Concepts in scope

- **Namespace**: ownership boundary for features and lifecycle operations.
- **Feature**: typed value definition with default and ordered rules.
- **Context**: runtime inputs (`locale`, `platform`, `appVersion`, `stableId`).
- **Rule**: criteria plus value; criteria within one rule are conjunctive.
- **Specificity**: deterministic precedence score for rule ordering.
- **Stable bucketing**: deterministic ramp-up assignment from stable identity.
- **Snapshot**: immutable configuration state read by evaluators.

### Context {#context}

`Context` is the runtime input contract used by rule matching and ramp-up
bucketing.

### Feature {#feature}

A `Feature<T, C, M>` binds value type, context type, and namespace ownership.

### Namespace {#namespace}

`Namespace` is the operational boundary for state, loading, rollback, and
kill-switch operations.

### StableId: deterministic ramp-ups {#stableid-deterministic-ramp-ups}

`StableId` is the deterministic cohort key used for ramp-up bucketing.

## Practical boundaries

- Compile-time typing covers static declarations and `evaluate(...)` return types.
- Untrusted JSON must cross a parse boundary before becoming a snapshot.
- Runtime operations (`load`, `rollback`, `disableAll`) are namespace-scoped.

## Related pages

- [konditional-core](/core)
- [Evaluation model](/learn/evaluation-model)
- [Configuration lifecycle](/learn/configuration-lifecycle)
- [Type safety boundaries](/theory/type-safety-boundaries)

## Next steps

1. Learn matching order in [Evaluation model](/learn/evaluation-model).
2. Learn parse flow in [Configuration lifecycle](/learn/configuration-lifecycle).
3. Learn namespace governance in [Namespace isolation](/theory/namespace-isolation).
