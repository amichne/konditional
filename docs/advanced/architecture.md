---
title: Architecture
description: High-level architecture of Konditional and how its core components work together
---

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
    fun update(condition: FlagDefinition<S, C>)
}
```

Typically implemented as an enum for convenience:

```kotlin
enum class Features(override val key: String) : Conditional<Boolean, Context> {
    DARK_MODE("dark_mode"),
    NEW_UI("new_ui"),
    ;

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

### 3. FlagDefinition<S, C>

`FlagDefinition<S : Any, C : Context>` contains the evaluation logic for a conditional:

- **key**: Reference to the `Conditional<S, C>` this condition evaluates
- **bounds**: List of `TargetedValue<S, C>` (rule → value mappings)
- **defaultValue**: Value returned when no rules match
- **fallbackValue**: Reserved for future use
- **salt**: String used in hash function for bucketing independence

```kotlin
data class FlagDefinition<S : Any, C : Context>(
    val key: Conditional<S, C>,
    val bounds: List<TargetedValue<S, C>>,
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

### 4. Evaluable<C> (Base Abstraction)

`Evaluable<C : Context>` is the foundation for composable rule evaluation:

```kotlin
abstract class Evaluable<C : Context> {
    internal open fun matches(context: C): Boolean = true
    internal open fun specificity(): Int = 0
}
```

**Purpose**: Provides a composable abstraction for rule evaluation logic that can be combined and extended.

**Key Features**:
- **Composability**: Multiple Evaluables can be composed together
- **Default behavior**: Returns true (matches all) with 0 specificity
- **Extension point**: Subclasses override to add custom matching logic

### 5. UserClientEvaluator<C>

`UserClientEvaluator<C : Context>` encapsulates standard client targeting logic:

```kotlin
data class UserClientEvaluator<C : Context>(
    val locales: Set<AppLocale> = emptySet(),
    val platforms: Set<Platform> = emptySet(),
    val versionRange: VersionRange = Unbounded,
) : Evaluable<C>()
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

### 6. Rule<C> (Composable Implementation)

`Rule<C : Context>` composes standard targeting with extensible evaluation:

```kotlin
data class Rule<C : Context>(
    val rollout: Rollout = Rollout.of(100.0),
    val note: String? = null,
    val userClientEvaluator: UserClientEvaluator<C> = UserClientEvaluator(),
    val extension: Evaluable<C> = object : Evaluable<C>() {},
) : Evaluable<C>()
```

**Composition Architecture**:
- **userClientEvaluator**: Handles standard locale, platform, and version targeting
- **extension**: Allows custom domain-specific evaluation logic
- Both must match for the rule to match: `userClientEvaluator.matches(context) && extension.matches(context)`
- Total specificity is the sum: `userClientEvaluator.specificity() + extension.specificity()`

**Convenience Constructor**: For backward compatibility, there's a secondary constructor that accepts individual parameters:
```kotlin
Rule(
    rollout = Rollout.of(50.0),
    locales = setOf(AppLocale.EN_US),
    platforms = setOf(Platform.IOS),
    versionRange = LeftBound(Version(2, 0, 0))
)
```

This creates a `UserClientEvaluator` internally with the specified constraints.

### 7. TargetedValue<S, C>

`TargetedValue<S : Any, C : Context>` maps a rule to its output value:

```kotlin
data class TargetedValue<S : Any, C : Context>(
    val rule: Rule<C>,
    val value: S
)
```

Created using the `implies` infix operator in the DSL:

```kotlin
rule {
    platforms(Platform.IOS)
    rollout = Rollout.of(50.0)
} implies true
```

### 8. FlagRegistry Interface and SingletonFlagRegistry

`FlagRegistry` is the abstraction for managing feature flag configurations, with `SingletonFlagRegistry` as the default implementation:

```kotlin
interface FlagRegistry {
    fun load(config: Snapshot)
    fun applyPatch(patch: SnapshotPatch)
    fun <S : Any, C : Context> update(definition: FlagDefinition<S, C>)
    fun getCurrentSnapshot(): Snapshot
    fun <S : Any, C : Context> getFlag(key: Conditional<S, C>): ContextualFeatureFlag<S, C>?
    fun getAllFlags(): Map<Conditional<*, *>, ContextualFeatureFlag<*, *>>
}

object SingletonFlagRegistry : FlagRegistry {
    // Thread-safe singleton implementation using AtomicReference
}

// Extension functions for evaluation
fun <S : Any, C : Context> C.evaluate(
    key: Conditional<S, C>,
    registry: FlagRegistry = SingletonFlagRegistry
): S

fun <C : Context> C.evaluate(
    registry: FlagRegistry = SingletonFlagRegistry
): Map<Conditional<*, *>, Any?>
```

**Key features**:
- **Abstraction**: `FlagRegistry` interface allows custom implementations
- **Atomic updates**: `Snapshot` is replaced atomically using `AtomicReference`
- **Lock-free reads**: Evaluation reads from a stable snapshot
- **Incremental updates**: Support for `SnapshotPatch` for efficient partial updates
- **Type safety**: Maintains type safety between `Conditional<S, C>` and `FlagDefinition<S, C>`

## Evaluation Flow

```
1. Application calls: context.evaluate(Features.DARK_MODE)
                                    ↓
2. Extension function uses SingletonFlagRegistry by default
                                    ↓
3. Registry retrieves flag definition for Features.DARK_MODE from current snapshot
                                    ↓
4. Flag definition.evaluate(context) is called
                                    ↓
5. Definition sorts targeted values by rule specificity (most specific first)
                                    ↓
6. For each targeted value (in order):
   a. Check if rule.matches(context)
   b. If matches, check if user is in eligible bucket
   c. If eligible, return the value
                                    ↓
7. If no rule matches, return defaultValue
```

## Deterministic Bucketing

Each surjection has a `rollout` percentage (0-100%). To determine if a user is eligible:

1. Compute `bucket = SHA-256("$salt:$flagKey:$stableId") mod 10000`
2. User is eligible if `bucket < (rollout * 100)`

**Properties**:
- **Deterministic**: Same `stableId` + `flagKey` + `salt` always produces same bucket
- **Independent**: Different `flagKey` values produce independent buckets (no correlation)
- **Granular**: 10,000 buckets allows 0.01% precision in ramp-up percentages

## Composable Architecture

Konditional's architecture is built on composition rather than inheritance. The key abstraction is `Evaluable<C>`, which provides two core operations:

- **matches(context: C)**: Determines if a context satisfies the criteria
- **specificity()**: Returns a numeric value for precedence ordering

This simple interface enables powerful composition patterns:

### Composition in Rule

The `Rule<C>` class demonstrates composition by combining two Evaluables:

```kotlin
data class Rule<C : Context>(
    val userClientEvaluator: UserClientEvaluator<C>,
    val extension: Evaluable<C>
) : Evaluable<C>() {
    override fun matches(context: C): Boolean =
        userClientEvaluator.matches(context) && extension.matches(context)

    override fun specificity(): Int =
        userClientEvaluator.specificity() + extension.specificity()
}
```

This design provides:
- **Separation of concerns**: Standard targeting is separate from custom logic
- **Reusability**: Custom Evaluables can be reused across multiple rules
- **Predictable precedence**: Specificity values compose additively
- **Extension without modification**: Add custom logic without changing Rule class

### Benefits of Composable Design

1. **Testability**: Each Evaluable can be tested independently
2. **Flexibility**: Mix and match different evaluation strategies
3. **Type safety**: Composition preserves type parameters throughout
4. **Clear semantics**: AND composition for matching, SUM composition for specificity

## Type Safety Architecture

The generic type parameters `<S : Any, C : Context>` flow through the entire system:

```
Conditional<S, C>
    ↓
FlagDefinition<S, C>
    ↓
TargetedValue<S, C>
    ↓
Rule<C> extends Evaluable<C>
    ↓
UserClientEvaluator<C> + extension: Evaluable<C>

FlagEntry<S, C> wraps FlagDefinition<S, C>
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
- TargetedValue<S, C> instances (from rule { } implies value)
- Default value
- Fallback value
    ↓ builds
FlagDefinition<S, C>
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

Konditional is designed for extension through its composable architecture:

1. **Custom Contexts**: Implement `Context` interface with your fields
2. **Custom Value Types**: Use any `S : Any` type in `Conditional<S, C>`
3. **Custom Evaluables**: Extend `Evaluable<C>` to create reusable evaluation logic
4. **Rule Extensions**: Use the `extension` parameter in `Rule<C>` to add custom logic
5. **Custom Builders**: Extend builders to add domain-specific DSL methods

### Example: Custom Evaluable

```kotlin
class SubscriptionTierEvaluator<C : EnterpriseContext>(
    val requiredTier: SubscriptionTier
) : Evaluable<C>() {
    override fun matches(context: C): Boolean =
        context.subscriptionTier >= requiredTier

    override fun specificity(): Int = 1
}
```

### Example: Using Custom Evaluable in Rules

```kotlin
// Compose custom evaluable with standard targeting
Rule(
    rollout = Rollout.of(100.0),
    locales = setOf(AppLocale.EN_US),
    platforms = setOf(Platform.IOS),
    extension = SubscriptionTierEvaluator(SubscriptionTier.ENTERPRISE)
)

// Or create more complex compositions
Rule(
    rollout = Rollout.of(50.0),
    extension = object : Evaluable<EnterpriseContext>() {
        override fun matches(context: EnterpriseContext): Boolean {
            return context.subscriptionTier >= SubscriptionTier.PREMIUM &&
                   context.userRole in setOf(UserRole.ADMIN, UserRole.OWNER)
        }
        override fun specificity(): Int = 2
    }
)
```

This composable design allows you to build reusable evaluation logic that can be mixed and matched across different rules.

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
- **Composability**: `Evaluable<C>` abstraction enables flexible composition of evaluation logic
- **Determinism**: SHA-256 based bucketing with same inputs → same outputs
- **Thread safety**: Lock-free reads with atomic updates
- **Extensibility**: Generic parameters allow custom contexts, value types, and evaluation strategies
- **Separation of concerns**: Standard targeting separated from custom logic through composition
- **Performance**: Simple evaluation with no synchronization on read path
