# Architecture Overview

This document describes the high-level architecture of Konditional and how its core components work together to provide type-safe, deterministic feature flag evaluation.

## Core Concepts

### 1. Conditional<S, C>

`Conditional<S : Any, C : Context>` is the central abstraction representing a feature flag or configuration value.

- **S**: The value type returned when evaluating this conditional (e.g., `Boolean`, `String`, custom types)
- **C**: The context type used for evaluation (e.g., `Context`, `EnterpriseContext`)

```kotlin
interface Conditional<S : Any, C : Context> {
    val key: String
    fun with(build: FlagBuilder<S, C>.() -> Unit)
    fun update(condition: Condition<S, C>)
}
```

Typically implemented as an enum for convenience:

```kotlin
enum class Features(override val key: String) : Conditional<Boolean, Context> {
    DARK_MODE("dark_mode"),
    NEW_UI("new_ui"),
    ;

    override fun with(build: FlagBuilder<Boolean, Context>.() -> Unit) =
        update(FlagBuilder(this).apply(build).build())
}
```

### 2. Context

`Context` defines what information is available for rule evaluation. The base interface provides:

- `locale`: Application locale
- `platform`: Platform (iOS, Android, Web)
- `appVersion`: Semantic version
- `stableId`: Unique identifier for deterministic bucketing

You can extend this interface with your own fields:

```kotlin
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val organizationId: String,
    val subscriptionTier: SubscriptionTier,
) : Context
```

### 3. Condition<S, C>

`Condition<S : Any, C : Context>` contains the evaluation logic for a conditional:

- **key**: Reference to the `Conditional<S, C>` this condition evaluates
- **bounds**: List of `Surjection<S, C>` (rule → value mappings)
- **defaultValue**: Value returned when no rules match
- **fallbackValue**: Reserved for future use
- **salt**: String used in hash function for bucketing independence

```kotlin
data class Condition<S : Any, C : Context>(
    val key: Conditional<S, C>,
    val bounds: List<Surjection<S, C>>,
    val defaultValue: S,
    val fallbackValue: S,
    val salt: String = "v1",
)
```

**Evaluation Logic**:
1. Sort surjections by rule specificity (descending)
2. Find first rule that matches the context
3. Check if user is in the eligible bucket for that rule's ramp-up percentage
4. Return the associated value, or default if no match

### 4. Rule<C>

`Rule<C : Context>` defines matching criteria and ramp-up percentage:

```kotlin
data class Rule<C : Context>(
    val rampUp: RampUp,
    val locales: Set<AppLocale> = emptySet(),
    val platforms: Set<Platform> = emptySet(),
    val versionRange: VersionRange = Unbounded,
    val note: String? = null,
)
```

**Matching**: A rule matches if ALL specified constraints are satisfied:
- If `locales` is empty, any locale matches; otherwise context.locale must be in the set
- If `platforms` is empty, any platform matches; otherwise context.platform must be in the set
- If `versionRange` is unbounded, any version matches; otherwise context.appVersion must be in range

**Specificity**: Calculated as:
```kotlin
(if (locales.isNotEmpty()) 1 else 0) +
(if (platforms.isNotEmpty()) 1 else 0) +
(if (versionRange.hasBounds()) 1 else 0)
```

Ranges from 0 (no constraints) to 3 (all constraints specified).

### 5. Surjection<S, C>

`Surjection<S : Any, C : Context>` maps a rule to its output value:

```kotlin
data class Surjection<S : Any, C : Context>(
    val rule: Rule<C>,
    val value: S
)
```

Created using the `implies` infix operator in the DSL:

```kotlin
boundary {
    platforms(Platform.IOS)
    rampUp = RampUp.of(50.0)
} implies true
```

### 6. Flags Singleton

`Flags` is the central registry holding all configured conditions:

```kotlin
object Flags {
    class FlagEntry<S : Any, C : Context>(
        val condition: Condition<S, C>
    )

    data class Snapshot internal constructor(
        val flags: Map<Conditional<*, *>, FlagEntry<*, *>>
    )

    fun load(config: Snapshot)
    fun <S : Any, C : Context> update(condition: Condition<S, C>)
    fun <S : Any, C : Context> C.evaluate(key: Conditional<S, C>): S
    fun <C : Context> C.evaluate(): Map<Conditional<*, *>, Any?>
}
```

**Key features**:
- **Atomic updates**: `Snapshot` is replaced atomically using `AtomicReference`
- **Lock-free reads**: Evaluation reads from a stable snapshot
- **FlagEntry wrapper**: Maintains type safety between `Conditional<S, C>` and `Condition<S, C>`

## Evaluation Flow

```
1. Application calls: context.evaluate(Features.DARK_MODE)
                                    ↓
2. Flags retrieves FlagEntry for Features.DARK_MODE from current snapshot
                                    ↓
3. FlagEntry.condition.evaluate(context) is called
                                    ↓
4. Condition sorts surjections by rule specificity (most specific first)
                                    ↓
5. For each surjection (in order):
   a. Check if surjection.rule.matches(context)
   b. If matches, check if user is in eligible bucket
   c. If eligible, return surjection.value
                                    ↓
6. If no surjection matches, return defaultValue
```

## Deterministic Bucketing

Each surjection has a `rampUp` percentage (0-100%). To determine if a user is eligible:

1. Compute `bucket = SHA-256("$salt:$flagKey:$stableId") mod 10000`
2. User is eligible if `bucket < (rampUp * 100)`

**Properties**:
- **Deterministic**: Same `stableId` + `flagKey` + `salt` always produces same bucket
- **Independent**: Different `flagKey` values produce independent buckets (no correlation)
- **Granular**: 10,000 buckets allows 0.01% precision in ramp-up percentages

## Type Safety Architecture

The generic type parameters `<S : Any, C : Context>` flow through the entire system:

```
Conditional<S, C>
    ↓
Condition<S, C>
    ↓
Surjection<S, C>
    ↓
Rule<C>

FlagEntry<S, C> wraps Condition<S, C>
    ↓
Map<Conditional<*, *>, FlagEntry<*, *>> stores all flags
    ↓
Retrieval casts FlagEntry<*, *> to FlagEntry<S, C>
    ↓
FlagEntry.evaluate(context: C): S maintains type safety
```

The `FlagEntry` wrapper is crucial: it ensures that when we retrieve a flag by key, the associated condition has matching type parameters. While an unchecked cast is still needed (due to type erasure), the wrapper makes it structurally safe - the types are guaranteed to match if the key matches.

## Thread Safety

**Reads** (evaluation):
- Lock-free: read from `AtomicReference<Snapshot>`
- Snapshot is immutable once created
- Multiple threads can evaluate concurrently

**Writes** (configuration updates):
- `AtomicReference.set()` provides atomic snapshot replacement
- Writers never block readers
- Later writes win if concurrent

## DSL Architecture

The configuration DSL is built with type-safe builders:

```kotlin
ConfigBuilder
    ↓ creates
FlagBuilder<S, C>
    ↓ creates
RuleBuilder<C>
    ↓ creates
Rule<C>

FlagBuilder combines:
- Surjection<S, C> instances (from boundary { } implies value)
- Default value
- Fallback value
    ↓ builds
Condition<S, C>
    ↓ wrapped in
FlagEntry<S, C>
    ↓ added to
Snapshot
```

The DSL ensures:
- Type parameters match throughout construction
- Required fields (default value) are provided
- Rules and values have compatible types

## Extension Points

Konditional is designed for extension:

1. **Custom Contexts**: Implement `Context` interface with your fields
2. **Custom Value Types**: Use any `S : Any` type in `Conditional<S, C>`
3. **Custom Rules**: Wrap or extend `Rule<C>` with additional logic
4. **Custom Builders**: Extend builders to add domain-specific DSL methods

Example custom rule:

```kotlin
data class EnterpriseRule<C : EnterpriseContext>(
    val baseRule: Rule<C>,
    val requiredTier: SubscriptionTier?,
    val requiredRole: UserRole?
) {
    fun matches(context: C): Boolean {
        if (!baseRule.matches(context)) return false
        if (requiredTier != null && context.subscriptionTier < requiredTier) return false
        if (requiredRole != null && context.userRole < requiredRole) return false
        return true
    }
}
```

## Performance Characteristics

- **Evaluation**: O(n) where n = number of surjections for a flag
  - Typically very small (1-10 rules per flag)
  - Sorted once when condition is created
  - First match wins (early exit)

- **Memory**: O(f × r) where f = number of flags, r = average rules per flag
  - Snapshots are immutable and shared
  - No per-request allocation

- **Bucketing**: O(1) hash computation
  - SHA-256 with small input
  - No synchronization required

## Summary

Konditional's architecture prioritizes:
- **Type safety**: Generics flow through the entire system
- **Determinism**: SHA-256 based bucketing with same inputs → same outputs
- **Thread safety**: Lock-free reads with atomic updates
- **Extensibility**: Generic parameters allow custom contexts and value types
- **Performance**: Simple evaluation with no synchronization on read path
