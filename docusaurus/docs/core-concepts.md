---
title: Core Concepts (What Each Type Prevents)
---

# Core Concepts (Definitions + Why They Exist)

Konditional’s core types exist to eliminate entire classes of feature-flag failure modes by making misuse impossible (or
at least obvious) at compile time.

## Namespace

**Definition:** A `Namespace` is the unit of **compile-time ownership** and **runtime isolation**.

**Why it exists:**

- Prevents cross-team collisions: a feature is bound to the namespace type.
- Creates an isolation boundary for configuration: each namespace has an isolated `NamespaceRegistry`.
- Enables “single object = registry handle” ergonomics: `Namespace` delegates `NamespaceRegistry`, so the namespace is
  also where you call registry operations (like kill-switching).

**Guarantee / Mechanism / Boundary**

- **Guarantee (compile-time):** Features are type-bound to the namespace they are defined in.
- **Mechanism:** `Feature<T, C, out M : Namespace>` carries the namespace in its type.
- **Boundary (runtime):** A namespace’s registry state (configuration + kill-switch + hooks) can change results.

## Feature

**Definition:** A `Feature<T, C, M>` is the typed handle your application code evaluates at call sites.

**Why it exists:**

- Keeps call sites stable: app code depends on `Feature` APIs, not on config formats.
- Makes “value type” and “context type” explicit: you cannot evaluate a feature with an incompatible context type.

**Guarantee / Mechanism / Boundary**

- **Guarantee (compile-time):** The evaluated value is `T` (no “stringly typed” flag values).
- **Mechanism:** `Feature` is generic in `T` and `C`, and is created via namespace property delegation.
- **Boundary (runtime):** The effective value comes from a `FlagDefinition` stored in the registry configuration.

## FlagDefinition

**Definition:** A `FlagDefinition<T, C, M>` is the runtime shape of a feature: default value, active state, rule set,
rollout salt, and evaluation algorithm.

**Why it exists:**

- Separates “what the flag is” (feature identity) from “how it currently evaluates” (definition loaded into registry).
- Makes evaluation deterministic and explainable.

**Guarantee / Mechanism / Boundary**

- **Guarantee (runtime):** Given the same inputs, evaluation is deterministic.
- **Mechanism:** Rules are evaluated in a stable precedence order; bucketing uses stable identifiers + salt.
- **Boundary:** If configuration parsing rejects an update, the previously active definition remains effective.

Next:

- [Evaluation Flow](evaluation-flow)
- [Rules](rules)

