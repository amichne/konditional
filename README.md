# Konditional

Generic, deterministic feature evaluation for any non-null type.  
Flags are fully type-safe and extensible.  
Evaluation depends on a **malleable context**, abstracted behind a **facade interface** for clean integration.  
Thread-safe, dependency-free, and deterministic.

---

## Core concepts

### Generic flags  
A `Flag<T : Any>` may return any non-null value type (`Boolean`, `Enum`, `String`, numeric, or domain-specific).  
All rule matching and deterministic bucketing operate generically, with no special casing by type.

### Malleable context  
`Context` is not fixed.  
A `ContextFacade` defines what information the system can read (e.g. `locale`, `platform`, `version`, `stableId`, or any domain data).  
Evaluation of rules depends only on this interface, not on a concrete implementation.  
Consumers can substitute their own context sources (API request, device info, session attributes).

### Deterministic bucketing  
Each evaluation uses `SHA-256("$salt:$flagKey:$stableId")` to derive a stable bucket `[0, 9999]`.  
This ensures repeatable behavior across sessions for the same stable ID and flag.  
Flag independence is guaranteed because the flag key contributes to the hash input.

### Rule-based resolution  
Each flag defines an ordered list of `Surjection<T>` entries.  
Each `Surjection` links a `Rule` and an output value.  
The first matching rule that includes the user (via its coverage) determines the value.  
If none match, eligibility is rechecked for the default value; otherwise fallback applies.

---

## Evaluation flow

1. Read the current snapshot from `Flags`.  
2. Locate the `Flag<T>` by key or type.  
3. Evaluate the `ContextFacade`.  
4. Resolve the most specific matching rule, checking coverage and eligibility.  
5. Return the resolved value of type `T`.

All reads are lock-free. Updates atomically replace the snapshot.

---

## Rule system

Rules combine optional constraints:

- `Locale`
- `Platform`
- `VersionRange`
- Arbitrary domain predicates via custom rule types

Each rule defines:

- `coveragePct`: what fraction of stable buckets are included  
- `value`: what to return when matched  

Default and fallback values apply when no rules match or eligibility fails.

---

## Version handling

Semantic version ranges use precise bounds:

- Unbounded, left-bounded, right-bounded, or fully bounded  
- Inclusive/exclusive endpoints validated by tests  
- Comparison logic based on parsed `major.minor.patch` semantics

---

## Type safety and determinism

- `Flag<T>` preserves type identity through evaluation.  
- Bucketing and rule matching are pure and deterministic.  
- Context variability is isolated behind the `ContextFacade`.  
- Tests guarantee no collisions or cross-flag interference.

---

## Usage

```kotlin
// Define a typed flag
val showNewUI = Flag("show_new_ui", defaultValue = false)

// Evaluate with context
val ctx = ContextFacade {
    locale = "en_US"
    platform = "ios"
    version = "7.12.3"
    stableId = "a1b2c3..."
}

val result: Boolean = showNewUI.evaluate(ctx)
```

---

## DSL example

```kotlin
FeatureFlags.boolean("enable_search") {
  default(false)
  rule {
    locale("en_US")
    versionRange { atLeast("7.0.0") }
    coverage(50.0)
    returns(true)
  }
}
```

---

## Test coverage

All tests validate:

- Deterministic hashing and bucket assignment  
- Generic type safety across flag types  
- Rule ordering and specificity  
- Coverage boundary correctness  
- Version range comparison logic  
- Default and fallback eligibility behavior  
- Independence between flags

All current tests pass, confirming deterministic evaluation for arbitrary non-null types.

---

## Design principles

- **Generic by default** — Any non-null return type supported.  
- **Context-agnostic** — Context is provided via an interface, not fixed structure.  
- **Deterministic and pure** — Same inputs always yield the same output.  
- **Zero dependencies** — No DI framework or reflection.  
- **Safe concurrency** — Atomic snapshot updates.  

---

## Implementation map

- `Flag<T>` — core generic flag logic  
- `Flags` — snapshot registry and entrypoint  
- `Surjection<T>` — rule/value pair  
- `Rule` — match logic and coverage  
- `VersionRange` — semantic version constraints  
- `ContextFacade` — extension point for environment data  
- `tests/` — deterministic behavior validation  

---

All tests confirm correctness of this generic, context-flexible feature evaluation engine.