# Konditional

Generic, deterministic feature evaluation for any non-null type.  
Flags are fully type-safe and extensible.  
Evaluation depends on a **flexible context** to drive precise, strongly-typed rules
Thread-safe, dependency-free, and deterministic.

---

## Core concepts

### Generic flags  
A `Flag<T : Any>` may return any non-null value type (`Boolean`, `Enum`, `String`, numeric, or domain-specific).  
All rule matching and deterministic bucketing operate generically, with no special casing by type.

### Context-based evaluation
A `Contextual` implementation (or the provided `Context`)defines what information the system can read
(e.g. `locale`, `platform`, `version`, `stableId`, or any domain data). Evaluation of rules are 
dynamic along unconstrained axis of this interface, while retaining bucketing sematics via
the `StableId` and `HexId`, which federate guarantable creation.
Consumers can implement their own context sources (API request, device info, session attributes) to fulfill dynamic needs.

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
3. Evaluate the list of `Rule`'s using the current `Context`.
4. Resolve the most specific matching rule, checking coverage and eligibility.  
5. Return the resolved value of type `T` the selected `Rule` maps onto (via `Surjection`).

All reads are lock-free. Updates atomically replace the snapshot.

> Scoped, short-lived test scaffolding coming soon!

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

🚧  **Coming soon!**  🚧

> This will be overhauled in the immedaite future, with aspirations of generifying, and allowing extension by users seamlessly. Rules will likely have a generic bound, which will specify the supported `Contextual` types. This means `Surjection` will also be bound by this type, though API surface shouldn't functionally be different.

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
enum class CustomFlags : FeatureFlag<Boolean> {
    SHOW_NEW_UI
}

// Evaluate with context
val context = Context(
    locale = AppLocale.EN_US,
    platform = Platform.IOS,
    appVersion = Version(7, 10, 1),
    stableId = StableId.of("00000000000000000000000000000000")
)
context.evaluate() // -> Map<FeatureFlag<*>, Any?>
context.evaluate(CustomFlags.SHOW_NEW_UI) // -> Boolean
```

---

## DSL example

```kotlin
enum class DomainProvidedEnum : FeatureFlag<Boolean> {
    CUSTOM_CASE,
    USE_LIGHTWEIGHT_HOME
}

config {
    DomainProvidedEnum.CUSTOM_CASE withRules {
        default(value = false)
        rule {
            platforms(Platform.IOS)
            version {
                leftBound(7, 10, 0)
            }
            note("US iOS staged rollout")
            rampUp = 50.0
        } gives true
        rule {
            locales(AppLocale.HI_IN)
            note("IN Hindi full")
        } gives true
    }
    DomainProvidedEnum.USE_LIGHTWEIGHT_HOME withRules {
        default(value = true, coverage = 100.0)
        rule {
            platforms(Platform.ANDROID)
            version {
                rightBound(6, 4, 99)
            }
            note("Android legacy off")
        } gives false
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
- `Context` — extension point for environment data
    - _Changes incoming!_
- `tests/` — deterministic behavior validation  

---

All tests confirm correctness of this generic, context-flexible feature evaluation engine.
