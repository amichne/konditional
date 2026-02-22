# Namespace Isolation

Namespaces are the primary blast-radius boundary in Konditional. Isolation is
structural in type shape, identifier shape, and runtime lifecycle.

## Isolation claim

Operations on one namespace do not mutate state for another namespace.

## Mechanism stack

### 1. Namespace-scoped identifiers

`FeatureId` includes namespace seed and feature key. This prevents key
collisions between domains with similar feature names.

### 2. Namespace-bound feature types

`Feature<T, C, M>` carries namespace type `M`, enabling compile-time
namespace-constrained APIs.

### 3. Namespace-owned lifecycle

`load`, `rollback`, and kill-switch semantics are resolved within the owning
namespace registry.

## Boundary behavior

Use namespace-scoped loader/codec calls to keep configuration boundaries local.

```kotlin
val authResult = NamespaceSnapshotLoader(Auth).load(authJson)
val paymentsResult = NamespaceSnapshotLoader(Payments).load(paymentsJson)
```

A parse failure in one namespace does not imply mutation of any other namespace.

## Operational guidance

Use separate namespaces when ownership, update cadence, or failure tolerance
differ meaningfully. Avoid over-segmenting a single cohesive domain.

## Related

- [Core primitives](/learn/core-primitives)
- [Configuration lifecycle](/learn/configuration-lifecycle)
- [How-to: namespace isolation](/how-to-guides/namespace-isolation)

## Claim ledger

| claim_id | claim_statement | claim_kind | status |
| --- | --- | --- | --- |
| TH-005-C1 | Feature identifiers include namespace scope to prevent cross-domain key collisions. | guarantee | supported |
| TH-005-C2 | Each namespace owns independent runtime state and lifecycle operations. | mechanism | supported |
| TH-005-C3 | Snapshot loader rejects mismatched namespace payloads instead of leaking updates across boundaries. | failure_mode | supported |
| TH-005-C4 | Namespace-bound feature types enable compile-time isolation of API surfaces. | guarantee | supported |
| TH-005-C5 | Kill-switch and rollback commands are scoped to the owning namespace state. | mechanism | supported |
| TH-005-C6 | Namespace-scoped feature identifiers remain stable at JSON boundaries. | boundary | supported |
