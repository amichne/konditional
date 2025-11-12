# Architecture

This document explains Konditional's internal architecture, design principles, and how the various components work
together.

## Design Principles

### Type Safety First

Every API surface uses strong typing to prevent runtime errors:

- Generic type parameters enforce value type constraints
- Context polymorphism ensures features get appropriate evaluation contexts
- EncodableValue types make unsupported types unrepresentable
- No string-based lookups or unchecked casts

### Parse, Don't Validate

APIs return explicit result types rather than throwing exceptions:

- `EvaluationResult<T>` for flag evaluation
- `ParseResult<T>` for deserialization
- Fold functions for ergonomic error handling

### Deterministic Behavior

Same inputs always produce same outputs:

- SHA-256 based bucketing for rollouts
- Stable evaluation order (by specificity)
- Independent flag bucketing spaces
- No global mutable state (except atomic registry)

### Composition Over Inheritance

Components compose through interfaces:

- Rules compose BaseEvaluable + extension Evaluable
- Features compose key + registry + value type
- Builders use sealed interfaces with internal implementations

## Core Components

### Feature

Entry point for defining flags:

```
Feature<S : EncodableValue<T>, T : Any, C : Context, M : Module>
  |
  +-- key: String
  +-- registry: FlagRegistry
  +-- update(definition)
```

Features are typically implemented as enum members or object declarations.

### FlagDefinition

Internal representation of configured flags:

```
FlagDefinition<S, T, C, M>
  |
  +-- feature: Feature<S, T, C, M>
  +-- defaultValue: T
  +-- values: List<ConditionalValue<S, T, C, M>>
  +-- isActive: Boolean
  +-- salt: String
  |
  +-- evaluate(context: C): T
```

FlagDefinition handles:

- Rule evaluation and specificity ordering
- Rollout bucketing via SHA-256 hashing
- Fallback to default value

### Context

Evaluation environment:

```
Context
  |
  +-- locale: AppLocale
  +-- platform: Platform
  +-- appVersion: Version
  +-- stableId: StableId
```

Can be extended with custom fields for domain-specific targeting.

### Rule

Targeting criteria:

```
Rule<C : Context>
  |
  +-- rollout: Rollout
  +-- note: String?
  +-- baseEvaluable: BaseEvaluable<C>
  +-- extension: Evaluable<C>
  |
  +-- matches(context: C): Boolean
  +-- specificity(): Int
```

Rules compose:

- Base targeting (locale, platform, version)
- Custom extension logic
- Both must match for rule to match

### Evaluable

Composable evaluation abstraction:

```
Evaluable<C : Context>
  |
  +-- matches(context: C): Boolean (default: true)
  +-- specificity(): Int (default: 0)

BaseEvaluable<C>
  |
  +-- locales: Set<AppLocale>
  +-- platforms: Set<Platform>
  +-- versionRange: VersionRange
```

Evaluables can be composed to create complex targeting logic.

### FlagRegistry

Configuration storage:

```
FlagRegistry
  |
  +-- load(konfig: Konfig)
  +-- update(patch: KonfigPatch)
  +-- update(definition: FlagDefinition<S, T, C, M>)
  +-- konfig(): Konfig
  +-- featureFlag(key: Feature<S, T, C, M>): FlagDefinition<S, T, C, M>?
  +-- allFlags(): Map<Feature<*, *, *>, FlagDefinition<*, *, *, *>>
```

Default implementation uses `AtomicReference<Konfig>` for thread-safe updates.

## Data Flow

### Configuration Flow

```
DSL (config { })
  -> ConfigBuilder
  -> Konfig (immutable snapshot)
  -> FlagRegistry (AtomicReference)
```

1. DSL builds intermediate builder structure
2. Builder creates immutable Konfig
3. Konfig loaded into registry atomically

### Evaluation Flow

```
Context.evaluateSafe(feature)
  -> Registry.featureFlag(feature)
  -> FlagDefinition.evaluate(context)
  -> ConditionalValue matching (by specificity)
  -> Rule.matches(context) && isInEligibleSegment()
  -> Return matched value or default
```

1. Lookup flag definition in registry
2. Iterate through conditional values (sorted by specificity)
3. Check if rule matches context
4. Check if context is in rollout bucket
5. Return value from first matching rule, or default

### Specificity Ordering

Rules are evaluated in specificity order:

```
Rules sorted by:
  1. specificity() DESC
  2. note ASC (tie-breaker)

Specificity calculation:
  baseEvaluable.specificity() + extension.specificity()

BaseEvaluable specificity:
  (locales not empty ? 1 : 0) +
  (platforms not empty ? 1 : 0) +
  (versionRange has bounds ? 1 : 0)
```

More specific rules are evaluated first.

### Bucketing Algorithm

Rollout bucketing uses SHA-256:

```kotlin
fun isInEligibleSegment(
    flagKey: String,
    id: HexId,
    salt: String,
    rollout: Rollout
): Boolean {
    if (rollout <= 0.0) return false
    if (rollout >= 100.0) return true

    val bucket = stableBucket(flagKey, id, salt)
    return bucket < (rollout.value * 100).toInt()
}

fun stableBucket(
    flagKey: String,
    id: HexId,
    salt: String
): Int {
    val hash = SHA256("$salt:$flagKey:${id.id}")
    val first4Bytes = hash[0..3]
    return (first4Bytes as Int) % 10_000
}
```

Properties:

- Deterministic: Same inputs always hash to same bucket
- Independent: Each flag has separate bucketing space
- Stable: Changing salt redistributes buckets
- Range: 0-9999 (0.01% granularity)

## Thread Safety

### Lock-Free Reads

Flag evaluation requires no locks:

```kotlin
class SingletonFlagRegistry : FlagRegistry {
    private val konfigRef = AtomicReference<Konfig>(Konfig.EMPTY)

    override fun featureFlag(key: Feature<S, T, C, M>): FlagDefinition<S, T, C, M>? {
        return konfigRef.get().flags[key]  // No lock needed
    }
}
```

Reads use `AtomicReference.get()`, which is lock-free.

### Atomic Updates

Configuration updates are atomic:

```kotlin
override fun load(config: Konfig) {
    konfigRef.set(config)  // Atomic swap
}
```

New configuration replaces old atomically. Concurrent evaluations see either old or new, never partial state.

### Immutable Data

All configuration data is immutable:

```kotlin
data class Konfig(
    val flags: Map<Feature<*, *, *>, FlagDefinition<*, *, *, *>>
)  // Map is immutable

data class FlagDefinition<S, T, C, M>(
    val defaultValue: T,  // Immutable
    val values: List<ConditionalValue<S, T, C, M>>,  // List is immutable
    // ...
)
```

Immutability ensures thread safety without locks.

## Builder DSL Architecture

### Scope Hierarchy

```
ConfigScope (sealed interface)
  -> ConfigBuilder (internal implementation)

FlagScope<S, T, C, M> (sealed interface)
  -> FlagBuilder<S, T, C, M> (internal implementation)

RuleScope<C> (sealed interface)
  -> RuleBuilder<C> (internal implementation)
```

Sealed interfaces hide implementation, preventing direct instantiation.

### Type State

Builders use typestate pattern:

```kotlin
interface FlagScope<S, T, C, M> {
    fun rule(build: RuleScope<C>.() -> Unit): Rule<C>
    infix fun Rule<C>.implies(value: T)  // Rule must be associated with value
}
```

The `implies` extension ensures rules are always associated with values.

### DSL Markers

`@FeatureFlagDsl` prevents accidental scope nesting:

```kotlin
@DslMarker
annotation class FeatureFlagDsl

@FeatureFlagDsl
interface ConfigScope

@FeatureFlagDsl
interface FlagScope<S, T, C, M>

@FeatureFlagDsl
interface RuleScope<C>
```

This prevents constructions like `rule { rule { } }`.

## Serialization Architecture

### Type Adapters

Custom Moshi adapters handle domain types:

```
SnapshotSerializer
  |
  +-- SerializableSnapshot (DTO)
  |     |
  |     +-- List<SerializableFlag>
  |           |
  |           +-- SerializableRule
  |                 |
  |                 +-- VersionRange (polymorphic)
  |
  +-- FlagValueAdapter (handles EncodableValue)
  +-- VersionRangeAdapter (handles VersionRange subtypes)
```

Domain models convert to/from serializable DTOs.

### Polymorphic Serialization

VersionRange uses polymorphic JSON:

```json
{
  "type": "MIN_AND_MAX_BOUND",
  "min": {
    ...
  },
  "max": {
    ...
  }
}
```

Moshi's `PolymorphicJsonAdapterFactory` handles type discrimination.

### Parse-Don't-Validate

Deserialization returns `ParseResult`:

```kotlin
sealed interface ParseResult<out T> {
    data class Success<T>(val value: T)
    data class Failure(val error: ParseError)
}
```

Forces explicit error handling, no exceptions.

## Extension Points

### Custom Contexts

Extend `Context` interface:

```kotlin
interface Context {
    val locale: AppLocale
    val platform: Platform
    val appVersion: Version
    val stableId: StableId
}

data class CustomContext(
    override val locale: AppLocale,
    // ... standard properties
    val customField: CustomType  // Extension
) : Context
```

### Custom Evaluables

Extend `Evaluable` for custom targeting:

```kotlin
abstract class Evaluable<C : Context> {
    open fun matches(context: C): Boolean = true
    open fun specificity(): Int = 0
}

class CustomEvaluable : Evaluable<CustomContext>() {
    override fun matches(context: CustomContext): Boolean {
        // Custom logic
    }

    override fun specificity(): Int = 1
}
```

### Custom Registries

Implement `FlagRegistry` for custom storage:

```kotlin
interface FlagRegistry {
    fun load(config: Konfig)
    fun update(patch: KonfigPatch)
    fun update(definition: FlagDefinition<S, T, C, M>)
    fun konfig(): Konfig
    fun featureFlag(key: Feature<S, T, C, M>): FlagDefinition<S, T, C, M>?
    fun allFlags(): Map<Feature<*, *, *>, FlagDefinition<*, *, *, *>>
}

class DatabaseBackedRegistry(private val db: Database) : FlagRegistry {
    // Implement using database storage
}
```

## Performance Characteristics

### Evaluation

- **Lookup**: O(1) hash map lookup
- **Matching**: O(n) where n = number of rules (typically small, <10)
- **Bucketing**: O(1) hash computation
- **Memory**: Immutable snapshots, no per-request allocation

### Updates

- **Load**: O(1) atomic swap
- **Patch**: O(n) where n = number of flags in patch

### Serialization

- **Serialize**: O(n) where n = total flags
- **Deserialize**: O(n) where n = total flags
- **Parse**: Single-pass parsing via Moshi

## Testing Architecture

### Test Registries

Create isolated registries for tests:

```kotlin

@Test
fun `test feature evaluation`() {
    val testRegistry = FlagRegistry.create()

    config(testRegistry) {
        MyFeature.FLAG with { default(false) }
    }

    val context = Context(TODO())
    val result = context.evaluateSafe(MyFeature.FLAG, testRegistry)

    assertTrue(result is EvaluationResult.Success)
}
```

### Test Contexts

Create test context factories:

```kotlin
object TestContexts {
    fun ios(version: Version = Version(2, 0, 0)): Context =
        Context(
            locale = AppLocale.EN_US,
            platform = Platform.IOS,
            appVersion = version,
            stableId = StableId.of("test-user")
        )

    fun android(): Context = // ...
    fun web(): Context = // ...
}
```

## Best Practices

### Immutability

Keep all domain models immutable for thread safety.

### Composition

Prefer composition over inheritance for extensibility.

### Explicit Results

Use result types instead of exceptions for expected errors.

### Type Safety

Leverage generics and sealed types to make invalid states unrepresentable.

### Lock-Free

Design for lock-free concurrent access where possible.

## Next Steps

- **[Overview](index.md)**: Back to API overview
- **[Context](Context.md)**: Deep dive into contexts
- **[Rules](Rules.md)**: Understand rule evaluation
