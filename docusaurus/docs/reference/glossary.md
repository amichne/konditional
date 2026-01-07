# Glossary

## Atomic swap

An update model where configuration is replaced in a single atomic step, so readers observe either the old snapshot or the new snapshot, never a
partially-applied mix.

Learn more: [Atomic evaluation snapshots](/fundamentals/evaluation-semantics), [Theory: Atomicity Guarantees](/theory/atomicity-guarantees)

## Allowlist

A set of `StableId`s that bypass a rule's ramp-up check after the rule matches by criteria. Allowlists do not force a rule to match; they only bypass the
rollout gate.

Learn more: [Ramp-up allowlisting](/how-to-guides/rolling-out-gradually)

## AxisValue

A custom targeting value along a named axis (environment, tenant, region, etc.). Axis values provide a stable string `id` used for rule matching and snapshot
serialization.

Learn more: [Axis targeting](/core/rules), [API Reference: Core Types](/core/types)

## Bucketing

Deterministically assigning a context (via `StableId`) to a bucket in a fixed space so percentage ramp-ups are stable and reproducible. Konditional derives the
bucket from `SHA-256("$salt:$flagKey:${stableIdHex}")` and reduces it into `[0, 10_000)`.

Learn more: [Deterministic ramp-up bucketing](/how-to-guides/rolling-out-gradually), [Theory: Determinism Proofs](/theory/determinism-proofs)

## Configuration

The in-memory snapshot used for evaluation: a map of registered features to their effective definitions (defaults, rules, salt, active state), plus optional
metadata.

Learn more: [Configuration lifecycle](/fundamentals/configuration-lifecycle)

## ConfigurationPatch

An incremental update to a `Configuration` that can add/update flag definitions and remove keys, producing a new `Configuration` snapshot when applied.

Learn more: [Incremental updates via patches](/fundamentals/configuration-lifecycle#incremental-updates-via-patches)

## Context

The runtime inputs to evaluation (for example: locale, platform, app version, and stable ID). Rules match against context, and evaluation requires an explicit
context value.

Learn more: [Context primitive](/fundamentals/core-primitives#context)

## Determinism

- **Guarantee**: Given the same active configuration snapshot and the same context, evaluation produces the same value.
- **Mechanism**: Stable rule precedence (specificity ordering + tie-break by definition order) and deterministic rollout bucketing.
- **Boundary**: Determinism assumes a stable `StableId`, a stable salt, and a stable configuration snapshot.

Learn more: [Deterministic evaluation](/fundamentals/evaluation-semantics#2-deterministic-same-inputs--same-outputs)

## Discriminated Union

A JSON encoding pattern for polymorphic types (like Kotlin sealed classes) where a `type` field identifies which variant is being represented. In OpenAPI
schemas, discriminators map type values to schema references for proper client codegen.

Examples in Konditional: `FlagValue` (BOOLEAN/STRING/INT/DOUBLE/ENUM/DATA_CLASS) and `VersionRange` (UNBOUNDED/MIN_BOUND/MAX_BOUND/MIN_AND_MAX_BOUND).

Learn
more: [Persistence format: Value encoding](/serialization/persistence-format#value-encoding-defaultvalue--rule-value), [Kontracts: Discriminated Unions](/kontracts/schema-dsl#discriminated-unions-sealed-classes)

## Extension predicate

A custom, typed predicate attached to a rule via `extension { ... }`. Extensions let you express domain-specific targeting beyond built-in criteria (
platform/locale/version/axes) and contribute to a rule's specificity.

Learn more: [Custom predicates via extension](/how-to-guides/custom-business-logic), [Core Types](/core/types)

## Feature

A typed configuration value (boolean/string/int/enum/custom) declared as a delegated property on a `Namespace`. Features always have a required default, so
evaluation is non-null.

Learn more: [Feature primitive](/fundamentals/core-primitives#feature)

## Kill-switch

A namespace-scoped override that disables all rules in a registry, causing evaluations to return declared defaults for that namespace (without changing feature
definitions).

Learn more: [Emergency kill switch](/fundamentals/evaluation-semantics), [API Reference: Namespace Operations](/runtime/operations)

## Namespace

An isolation boundary with its own registry and independent configuration lifecycle (load/rollback/disable). Namespaces prevent unrelated domains from sharing
configuration state.

Learn more: [Namespace primitive](/fundamentals/core-primitives#namespace)

## ParseResult

An explicit boundary type used for JSON parsing and patch application: `Success(value)` or `Failure(error)`. Parse failures return structured errors instead of
throwing, so invalid remote input can be rejected before it affects evaluation.

Learn
more: [ParseResult boundary](/fundamentals/type-safety), [API Reference: Serialization](/serialization/reference), [Theory: Parse Don't Validate](/theory/parse-dont-validate)

## Ramp-up

A percentage rollout gate applied after a rule matches by criteria. Ramp-ups use deterministic bucketing so increasing a percentage only adds users (for a
stable `(stableId, flagKey, salt)`).

Learn more: [Percentage ramp-up](/how-to-guides/rolling-out-gradually)

## Rollback

Restoring a prior configuration snapshot from a bounded history maintained by the registry. Rollbacks are namespace-scoped and revert the active configuration
without changing code-defined features.

Learn more: [Rollback support](/fundamentals/configuration-lifecycle), [API Reference: Namespace Operations](/runtime/operations)

## Rule

A typed mapping from criteria to a concrete value: if all criteria match the context (AND semantics) and the context passes rollout/allowlist gates, the rule's
value is returned.

Learn more: [AND semantics inside a rule](/core/rules)

## Salt

A per-feature string included in the bucketing input. Changing the salt intentionally redistributes bucket assignments for that feature (useful when re-running
experiments).

Learn more: [Bucketing input and salt](/how-to-guides/rolling-out-gradually)

## Shadow evaluation

Evaluating a feature against a baseline registry (returned value) while also evaluating against a candidate registry for comparison telemetry. This supports
migrations by surfacing mismatches without changing behavior.

Learn more: [Shadow evaluation](/observability/shadow-evaluation), [Theory: Migration and Shadowing](/theory/migration-and-shadowing)

## Snapshot

A serialized JSON representation of configuration state used as a storage/transport format. Snapshots are parsed at a validated trust boundary before they can
be loaded into a namespace.

Learn more: [Persistence & storage format](/serialization/persistence-format)

## Specificity

A rule precedence metric: more specific rules are evaluated first. In Konditional, total specificity is the sum of base targeting criteria (
platforms/locales/version bounds/axes) plus extension specificity.

Learn more: [Specificity ordering](/fundamentals/evaluation-semantics)

## StableId

A stable identifier used for deterministic bucketing and allowlists. `StableId.of(input)` normalizes a non-blank string into a hex id; `StableId.fromHex(hex)`
uses a precomputed canonical hex id.

Learn more: [StableId](/fundamentals/core-primitives#stableid-deterministic-ramp-ups)

## Total evaluation

- **Guarantee**: Evaluating a registered feature returns a value for every context.
- **Mechanism**: A default is required, and it is returned when no rule produces a value (or when a registry/flag is inactive/disabled).
- **Boundary**: Totality assumes the feature is registered and evaluation reads from a valid configuration snapshot.

Learn more: [Total evaluation](/fundamentals/evaluation-semantics#1-total-always-returns-a-value)

## Trust boundary

The point where untrusted input (typically JSON) enters the system. Konditional treats JSON configuration as a trust boundary: it must be validated into domain
types (`ParseResult.Success`) before it can be loaded and influence evaluation.

Learn more: [Trust boundaries](/fundamentals/type-safety)

## VersionRange

A version constraint used in rules (min/max/unbounded) to target contexts by semantic app version. Version ranges participate in rule matching and contribute to
specificity when bounds are present.

Learn more: [Version ranges](/core/rules), [API Reference: Core Types](/core/types)
