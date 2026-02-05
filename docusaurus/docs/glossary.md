---
title: Glossary
---

# Glossary

Definitions for Konditional’s core vocabulary, with links to the “next useful page” in this documentation set.

:::tip Reading strategy
Use this page to disambiguate terms quickly, then jump to the linked pages for mechanics and examples.
:::

---

## Atomic swap

An update model where configuration is replaced in a single atomic step, so readers observe either the old snapshot or
the new snapshot, never a partially-applied mix.

Learn more:

- [Registry & Configuration](/registry-and-configuration) (atomic snapshot refresh)

## Allowlist

A set of `StableId`s that bypass a rule's ramp-up check after the rule matches by criteria. Allowlists do not force a
rule to match; they only bypass the rollout gate.

Learn more:

- [Rule Model](/rules) (allowlist semantics)
- [Rollouts & Bucketing](/rollouts-and-bucketing)

## AxisValue

A custom targeting value along a named axis (environment, tenant, region, etc.). Axis values provide a stable string
`id` used for rule matching and snapshot serialization.

Learn more:

- [Context & Axes](/context-and-axes)

## Bucketing

Deterministically assigning a context (via `StableId`) to a bucket in a fixed space so percentage ramp-ups are stable
and reproducible.

Learn more:

- [Rollouts & Bucketing](/rollouts-and-bucketing)
- [API Reference](/api-reference) (`RampUpBucketing`)

## Configuration

The in-memory snapshot used for evaluation: a map of registered features to their effective definitions (defaults,
rules, salt, active state), plus optional metadata.

Learn more:

- [Registry & Configuration](/registry-and-configuration)

## ConfigurationPatch

An incremental update that can add/update flag definitions and remove keys, producing a new configuration snapshot when
applied.

Learn more:

- [API Reference](/api-reference) (`ConfigurationSnapshotCodec.applyPatchJson`)
- [Parsing & Errors](/parsing-and-errors)

## Context

The runtime inputs to evaluation (for example: locale, platform, app version, stable ID, axes).

Learn more:

- [Context & Axes](/context-and-axes)

## Determinism

- **Guarantee:** given the same active configuration snapshot and the same context, evaluation produces the same value.
- **Mechanism:** stable rule precedence (specificity ordering) and deterministic rollout bucketing.
- **Boundary:** determinism assumes stable `StableId`, stable salt, and a stable configuration snapshot.

Learn more:

- [Evaluation Flow](/evaluation-flow)
- [Rollouts & Bucketing](/rollouts-and-bucketing)

## Discriminated union

A JSON encoding pattern for polymorphic types (like Kotlin sealed classes) where a `type` field identifies which
variant is being represented.

Learn more:

- [API Reference](/api-reference) (snapshot codecs and boundary parsing)

## Extension predicate

A custom, typed predicate attached to a rule via `extension { ... }`. Extensions let you express domain-specific
targeting beyond built-in criteria (platform/locale/version/axes) and contribute to a rule's specificity.

Learn more:

- [Rule Model](/rules)
- [Evaluation Flow](/evaluation-flow) (specificity)

## Feature

A typed configuration value (boolean/string/int/enum/custom) declared as a delegated property on a `Namespace`.
Features always have a required default, so evaluation is total.

Learn more:

- [Core Concepts](/core-concepts)
- [Quick Start](/quick-start)

## Kill-switch

A namespace-scoped override that disables all rules in a registry, causing evaluations to return declared defaults for
that namespace (without changing feature definitions).

Learn more:

- [Registry & Configuration](/registry-and-configuration)

## Namespace

An isolation boundary with its own registry and independent configuration lifecycle (load/rollback/disable).

Learn more:

- [Core Concepts](/core-concepts)

## ParseResult

An explicit boundary type used for JSON parsing and patch application: `Success(value)` or `Failure(error)`.

Learn more:

- [Parsing & Errors](/parsing-and-errors)
- [API Reference](/api-reference)

## Ramp-up

A percentage rollout gate applied after a rule matches by criteria. Ramp-ups use deterministic bucketing so increasing a
percentage only adds users (for a stable `(stableId, flagKey, salt)`).

Learn more:

- [Rollouts & Bucketing](/rollouts-and-bucketing)

## Rollback

Restoring a prior configuration snapshot from bounded history maintained by the registry.

Learn more:

- [API Reference](/api-reference) (`Namespace.rollback`)
- [Registry & Configuration](/registry-and-configuration)

## Rule

A typed mapping from criteria to a concrete value: if all criteria match the context (AND semantics) and the context
passes rollout/allowlist gates, the rule's value is returned.

Learn more:

- [Rule Model](/rules)

## Salt

A per-feature string included in the bucketing input. Changing the salt intentionally redistributes bucket assignments
for that feature.

Learn more:

- [Rollouts & Bucketing](/rollouts-and-bucketing)

## Shadow evaluation

Evaluating a feature against a baseline registry (returned value) while also evaluating against a candidate registry
for comparison telemetry.

Learn more:

- [Observability & Debugging](/observability-and-debugging)
- [Recipes](/recipes)

## Snapshot

A serialized JSON representation of configuration state used as a storage/transport format. Snapshots are parsed at a
validated trust boundary before they can be loaded into a namespace.

Learn more:

- [API Reference](/api-reference) (`ConfigurationSnapshotCodec`, `NamespaceSnapshotLoader`)
- [Parsing & Errors](/parsing-and-errors)

## Specificity

A rule precedence metric: more specific rules are evaluated first.

Learn more:

- [Evaluation Flow](/evaluation-flow)

## StableId

A stable identifier used for deterministic bucketing and allowlists.

Learn more:

- [Rollouts & Bucketing](/rollouts-and-bucketing)

## Total evaluation

- **Guarantee:** evaluating a registered feature returns a value for every context.
- **Mechanism:** a default is required, and it is returned when no rule produces a value (or when a registry/flag is
  inactive/disabled).
- **Boundary:** totality assumes the feature is registered and evaluation reads from a valid configuration snapshot.

Learn more:

- [Evaluation Flow](/evaluation-flow)

## Trust boundary

The point where untrusted input (typically JSON) enters the system. Konditional treats JSON configuration as a trust
boundary: it must be validated into domain types before it can be loaded and influence evaluation.

Learn more:

- [Parsing & Errors](/parsing-and-errors)

## VersionRange

A version constraint used in rules (min/max/unbounded) to target contexts by semantic app version.

Learn more:

- [Rule Model](/rules)
