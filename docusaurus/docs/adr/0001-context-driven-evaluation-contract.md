---
title: "ADR-0001: Context-Driven Evaluation Contract"
sidebar_position: 1
---

# ADR-0001: Context-Driven Evaluation Contract

**Status:** Accepted
**Date:** 2026-03-03
**Milestone:** [Context-Driven Evaluation Refactor](https://github.com/amichne/konditional/milestone/1)

---

## Context

Konditional's evaluation model accepts a `Context` and a configuration `Namespace` to produce a typed value for any feature flag. As the platform has matured, two unsafe patterns have emerged:

1. **Ambient external reads** — some evaluation paths consult external data sources (remote configs, databases) at evaluation time without version pinning, breaking determinism.
2. **Stringly-typed identifiers** — axis and context field lookups pass raw `String` keys at call sites, losing compile-time safety and enabling silent runtime mislookups.

This ADR defines the canonical evaluation contract that eliminates both patterns.

---

## Decision

### 1. Default evaluation contract: context + registered axes only

The default evaluation path **must** rely exclusively on:

- Fields present in the typed `Context` passed to `evaluate()`
- Axis values from the `Axes` attached to that `Context`
- Configuration loaded into the `NamespaceRegistry` as an immutable `NamespaceSnapshot`

**Any access to ambient external state (HTTP calls, database reads, global shared maps) at evaluation time is prohibited** unless explicitly opted into via the Quarantined External Backstop (see §3).

**Enforcement mechanism:** The `NamespaceRegistry.load()` path validates that any `ExternalSnapshotRef` present in a configuration carries a non-blank `version` field. Unversioned references are rejected with `ParseError.UnversionedExternalRef` at load time, before they can reach the evaluation path.

### 2. Typed identifiers in core evaluation paths

The following identifiers must be typed value classes in `konditional-core`:

| Raw form | Typed replacement |
|----------|------------------|
| `String` axis ID | `AxisKey` (`@JvmInline value class`) |
| `String` context field key | `ContextKey` (`@JvmInline value class`) |
| External dependency ref | `ExternalSnapshotRef` (sealed interface) |
| Named predicate ref | `PredicateRef` (sealed interface) |

**No new stringly-typed identifiers** may be introduced in `konditional-core` evaluation paths. Existing `String`-typed public API surface is deprecated and scheduled for removal in `v0.5.0`.

### 3. Quarantined External Backstop

Consuming a configuration from an external source is permitted **only** when all of the following conditions hold:

1. **Opt-in is explicit:** the namespace registers an `ExternalSnapshotRef` in its configuration.
2. **Version is pinned:** `ExternalSnapshotRef.version` is a non-blank, immutable version string (e.g., a content hash or monotonic version token).
3. **Resolver is deterministic:** resolving the same `(id, version)` pair must always return the same content.
4. **Scope is namespace-local:** an external ref registered in namespace A is invisible to namespace B.

Violations at registration time produce a typed `ParseError` (not a thrown exception). Violations at evaluation time cannot occur because they are rejected at load time.

### 4. Predicate registration

Named predicates must be registered via a typed `PredicateRegistry` scoped to a namespace. Ad-hoc `Predicate` instances (the legacy `fun interface Predicate<C>`) are deprecated. Resolution order within the registry is deterministic and stable (definition order, then lexicographic by id).

---

## Invariants

All four invariants below are non-negotiable and enforced at the type or API boundary:

### Determinism

> Same `Context` + same `NamespaceSnapshot` → same evaluation result, always.

**Enforcement:** Evaluation reads only from the immutable `NamespaceSnapshot` and the passed `Context`. No `System.currentTimeMillis()`, `Random`, or ambient I/O in the evaluation path.

### Atomic Snapshots

> Readers observe either the previous complete snapshot or the new complete snapshot — never partial state.

**Enforcement:** `InMemoryNamespaceRegistry` uses a single `AtomicReference<NamespaceSnapshot>` per namespace. Writes use `getAndSet` under a write lock to maintain history; reads use `get()` with no lock.

### Namespace Isolation

> Configuration, predicates, and external refs in namespace A are invisible to namespace B at evaluation time.

**Enforcement:** `NamespaceRegistry` is scoped by `namespaceId`. `PredicateRegistry` is scoped by `namespaceId`. Cross-namespace lookups are not provided by the API surface.

### Typed Boundary Errors

> External input failures produce typed `ParseError` values — not thrown exceptions.

**Enforcement:** All Moshi adapters and snapshot loaders wrap failure paths in `Result<T, ParseError>`. `KonditionalBoundaryFailure` wraps `ParseError` for interop with exception-expecting consumers but must not be constructed outside the boundary layer.

---

## Consequences

### Breaking changes

- `Axis.id: String` is supplemented by `Axis.key: AxisKey`; callers using `id` should migrate to `key`.
- `Predicate<C>` (the legacy fun interface) is deprecated in favour of `Targeting.Custom<C>` and `PredicateRef`-based registration.
- External snapshot loading without a `version` field will fail at load time with a typed error.

### Non-breaking additions

- `ContextKey`, `AxisKey`, `ExternalSnapshotRef`, `PredicateRef`, `PredicateRegistry` are additive.
- `NamespaceSnapshot` formalises the existing `AtomicReference<Configuration>` pattern; no behavioural change.
- `ParseError` gains three new variants (`UnknownField`, `MissingRequired`, `InvalidValue`) and one enforcement variant (`UnversionedExternalRef`, `UnknownPredicate`).

### Migration path

See the [Migration Guide](../guides/context-driven-evaluation-migration.md) for step-by-step instructions.

---

## Alternatives considered

### Allow unversioned external reads with explicit annotation

Rejected. Annotations are erased at runtime and provide no enforcement. A required field in a typed ADT is the only guarantee.

### Introduce a global predicate registry

Rejected. Global shared state violates namespace isolation and makes it impossible to reason about which namespace owns which predicate.

### Keep stringly-typed axis IDs

Rejected. String IDs cannot be checked at compile time. A `@JvmInline value class` has identical runtime cost with full compile-time safety.

---

## References

- [Determinism Proofs](../theory/determinism-proofs.md)
- [Atomic Snapshot Guarantees](../theory/atomicity-guarantees.md)
- [Namespace Isolation](../theory/namespace-isolation.md)
- [Parse, Don't Validate](../theory/parse-dont-validate.md)
- [Migration and Shadowing](../theory/migration-and-shadowing.md)
