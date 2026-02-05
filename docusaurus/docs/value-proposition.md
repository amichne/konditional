---
title: Value Proposition
---

# Konditional Value Proposition

Konditional is a **compile-time control plane** for flags and configuration:

- Flags are **properties, not strings**.
- Values are **typed** at the call site.
- Evaluation semantics are **centralized, deterministic, and explainable**.
- Configuration is treated as a **validated trust boundary**, not “best-effort input”.

:::danger The bet
If you cannot trust your configuration and experimentation engine with absolute certainty, you do not have real control
in production.
:::

---

## String configurations: failure modes

### Typo drift + silent failures

**Problem:** string keys live outside the type system, so the compiler cannot prove that call sites match the
configuration surface.

**Consequence:** typos silently route traffic to defaults, and you only discover drift after metrics or revenue move.

**Konditional response:**

- `Namespace` binds features to a concrete, typed container.
- `Feature.evaluate` returns `T` at the call site.

:::note Outcome
Key usage and return types are enforced by the compiler instead of by convention.
:::

### Type coercion at runtime

**Problem:** configuration arrives as untyped data, forcing runtime coercion or ad-hoc defaults.

**Consequence:** a malformed value can either crash evaluation or silently change behavior.

**Konditional response:**

- Parsing is an explicit boundary that yields `ParseResult` with a structured `ParseError` on failure.
- Loaded state is a read-only `ConfigurationView`, so runtime sees validated configuration, not raw input.

---

## Boolean flag systems: the boolean matrix explosion

### Variants encoded as control flow

**Problem:** multiple booleans encode variants as nested conditionals.

**Consequence:** testing grows combinatorially and intent is scattered across call sites.

**Konditional response:**

- `Feature<T, C, M>` supports non‑Boolean values.
- `Rule` defines value selection with one set of semantics.
- `FlagScope` and `RuleScope` centralize how rules are authored so targeting logic is expressed once and consumed
  everywhere.

### Debugging ambiguity

**Problem:** a true/false result does not tell you why a decision was made.

**Consequence:** debugging rollouts becomes guesswork and postmortems lack traceability.

**Konditional response:**

- `Feature.explain` returns a structured decision record.
- `Feature.evaluateWithReason` provides a compatible explanation path for existing callers.

---

## Deterministic targeting and rollouts

### Stable rollout math

**Problem:** gradual rollouts are hard to reproduce without deterministic bucketing.

**Consequence:** the same user can fall into different buckets across environments or time.

**Konditional response:**

- `RampUpBucketing` computes stable buckets from `StableId` and `HexId`, making rollout assignment reproducible for the
  same user, flag, and salt.

### Targeting axes

**Problem:** real segmentation relies on more dimensions than locale or platform.

**Consequence:** teams add ad-hoc fields and inconsistent targeting rules across services.

**Konditional response:**

- `Context` carries `AxisValues`.
- Each `Axis` defines a stable dimension so rules can match on typed, discoverable targeting data.

---

## Comparison

| Aspect | String-keyed systems | Boolean-only systems | Konditional |
|---|---|---|---|
| Key binding | Runtime string lookup | Enum/constant names, still detached from values | `Namespace` + `Feature<T, C, M>` bind keys to types |
| Value types | Runtime coercion | Boolean only | `Feature<T, C, M>` carries non-Boolean values |
| Rule semantics | SDK-specific or per-team | Per-team conditionals | `Rule` authored via `FlagScope` and `RuleScope` |
| Debuggability | Opaque decision path | Opaque decision path | `Feature.explain` and `Feature.evaluateWithReason` |
| Rollout determinism | SDK-dependent | Reimplemented per team | `RampUpBucketing` with `StableId` and `HexId` |
| Targeting dimensions | Ad-hoc fields | Ad-hoc fields | `Context` + `Axis` + `AxisValues` |
| Config boundary | Implicit parsing | Implicit parsing | `ParseResult` / `ParseError` + `ConfigurationView` |

---

## When it fits

- You need compile-time correctness for flag definitions and evaluation.
- You need deterministic evaluation and reproducible rollouts.
- You want a single, centralized rule model instead of per-team conditionals.

## When it does not

- You require fully dynamic flags with no static definitions.
- You prioritize a hosted dashboard over compile-time guarantees.

---

## Start here

Start with a `Namespace` and evaluate via `Feature.evaluate`:

- [Quick Start](/quick-start)
- [Core Concepts](/core-concepts)
- [API Reference](/api-reference)
