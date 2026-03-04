# Context-Driven Evaluation: Migration Guide

This guide covers how to adopt the new types and APIs introduced in the
Context-Driven Evaluation Refactor (ADR-0001, deliverables D2–D7).
All changes are additive and backward-compatible unless noted.

---

## D2 — Typed context and axis identifiers

### New types

| Type | Package | Purpose |
|---|---|---|
| `ContextKey` | `io.amichne.konditional.context` | Inline value class wrapping a non-blank string key for context dimensions |
| `AxisKey` | `io.amichne.konditional.context.axis` | Inline value class for the stable axis identifier |
| `ExternalSnapshotRef` | `io.amichne.konditional.core.external` | Sealed type representing a validated, versioned external config reference |

### What changed

`Axis` now exposes a `key: AxisKey` computed property derived from its `id`:

```kotlin
val env = Axis.of<Environment>()
val key: AxisKey = env.key  // AxisKey(id = env.id)
```

### Migration

No call-site changes required. `AxisKey` and `ContextKey` are new surfaces that
callers can use when building structured context maps or logging. Adopt them at
your own pace.

---

## D3 — Predicate registry

### New types

| Type | Purpose |
|---|---|
| `PredicateRef` | Sealed discriminated union: `BuiltIn(id)` or `Registered(namespaceId, id)` |
| `PredicateRegistry<C>` | Namespace-scoped registry for `Targeting.Custom<C>` predicates |
| `InMemoryPredicateRegistry<C>` | Default in-memory implementation |
| `ParseError.UnknownPredicate` | Typed error when a `PredicateRef` cannot be resolved |

### Usage

```kotlin
val registry = InMemoryPredicateRegistry<MyContext>(namespaceId = "my-namespace")

val ref = PredicateRef.Registered(namespaceId = "my-namespace", id = "is-beta-user")
registry.register(ref) { ctx -> ctx.isBetaUser }

val predicate = registry.resolve(ref).getOrThrow()
```

### Error handling

Resolving an unregistered ref returns a typed failure:

```kotlin
registry.resolve(ref).fold(
    onSuccess = { it },
    onFailure = { err ->
        // err is KonditionalBoundaryFailure wrapping ParseError.UnknownPredicate
    }
)
```

---

## D4 — Structured parse error variants

Three new `ParseError` subtypes provide fine-grained failure information at JSON
serialization boundaries:

| Variant | Fields | Meaning |
|---|---|---|
| `ParseError.UnknownField` | `path`, `message` | A field present in input JSON is not recognised by the schema |
| `ParseError.MissingRequired` | `path`, `message` | A required field is absent from the input |
| `ParseError.InvalidValue` | `path`, `reason`, `message` | A field value is present but semantically invalid |

These are produced by the serialization layer when strict validation is enabled.
No call-site changes are needed; existing `when` exhaustiveness checks in
consuming code will require a branch for each new variant.

**Update sealed exhaustiveness checks** if you pattern-match on `ParseError`:

```kotlin
when (error) {
    is ParseError.InvalidHexId -> ...
    is ParseError.InvalidRollout -> ...
    is ParseError.InvalidVersion -> ...
    is ParseError.FeatureNotFound -> ...
    is ParseError.FlagNotFound -> ...
    is ParseError.InvalidSnapshot -> ...
    is ParseError.InvalidJson -> ...
    is ParseError.UnknownPredicate -> ...       // D3
    is ParseError.UnversionedExternalRef -> ... // D6
    is ParseError.UnknownField -> ...           // D4
    is ParseError.MissingRequired -> ...        // D4
    is ParseError.InvalidValue -> ...           // D4
}
```

---

## D5 — `NamespaceSnapshot` as the unit of atomic exchange

`InMemoryNamespaceRegistry` now stores `NamespaceSnapshot` internally instead
of a raw `Configuration`. Externally the `configuration: ConfigurationView`
property is unchanged, but a new property is now available:

```kotlin
val snapshot: NamespaceSnapshot = registry.currentSnapshot
val version: String? = snapshot.version  // delegates to Configuration.metadata.version
```

`NamespaceSnapshot.empty` is a pre-allocated sentinel for initial/unloaded state.

### Migration

No breaking changes. Adopt `currentSnapshot` where you need the whole
snapshot (e.g., for version-aware logging or conditional rollback logic).

---

## D6 — External snapshot backstop registry

`ExternalSnapshotRef` (from D2) is now fully integrated with a typed registry:

| Type | Purpose |
|---|---|
| `ExternalRefRegistry` | Namespace-scoped registry for validated external config refs |
| `InMemoryExternalRefRegistry` | Default in-memory implementation |
| `ParseError.UnversionedExternalRef` | Typed error for blank id or version |

### Safe construction

Use the boundary-safe factory instead of the constructor when handling raw strings:

```kotlin
// Preferred — returns typed error on blank id or version
ExternalSnapshotRef.parse(id = rawId, version = rawVersion)
    .fold(onSuccess = { use(it) }, onFailure = { handleError(it) })

// Direct — throws on invalid input; use only for trusted, validated strings
ExternalSnapshotRef.versioned(id = "config-service", version = "v42")
```

---

## D7 — Namespace-scoped shadow evaluation

### New `ShadowOptions` field

`enabledForNamespaces: Set<String>?` restricts shadow evaluation to a named
subset of namespaces. When `null` (the default) all namespaces participate.

```kotlin
// Opt only "payments" and "checkout" into shadow evaluation
ShadowOptions.of(enabledForNamespaces = setOf("payments", "checkout"))
```

### New `ShadowMismatch` fields

| Field | Type | Meaning |
|---|---|---|
| `namespaceId` | `String` | Namespace that produced the mismatch |
| `detectedAtEpochMillis` | `Long` | Wall-clock timestamp at detection time |

**Update any call sites** that destructure or copy `ShadowMismatch` by named
fields — the new fields are required constructor parameters.

```kotlin
onMismatch = { mismatch ->
    log.warn(
        "Shadow mismatch ns={} key={} at={} kinds={}",
        mismatch.namespaceId,
        mismatch.featureKey,
        mismatch.detectedAtEpochMillis,
        mismatch.kinds,
    )
}
```

---

## Invariant checklist

After migration, verify:

- [ ] All `when (parseError)` exhaustiveness checks include D3/D4/D6 variants.
- [ ] `ShadowMismatch` consumers compile with the two new fields.
- [ ] `ShadowOptions.of(...)` call sites compile (new optional `enabledForNamespaces` param has default `null`).
- [ ] All baseline evaluation paths still return the correct value (shadow path is non-invasive).
