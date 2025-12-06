# Konditional Architecture Overview

## System Purpose

Konditional is a compile-time verified feature flag library for Kotlin. Core philosophy: "If it compiles, it works" - making runtime configuration errors impossible through type system constraints.

## Design Principles

1. **Parse, Don't Validate**: Use sealed types (`ParseResult`, `EvaluationResult`) instead of exceptions
2. **Type Safety First**: Extensive use of generics to encode invariants at compile time
3. **Impossible States**: Type system makes illegal states unrepresentable
4. **Zero Dependencies**: Only Moshi for JSON serialization (optional)
5. **Thread Safety**: Lock-free reads via AtomicReference, deterministic evaluation
6. **Ergonomic DSL**: Property delegation + builder pattern for natural API

## Core Components

### 1. Feature System
**Location**: `src/main/kotlin/io/amichne/konditional/core/features/`

```
Feature<T : Any, M : Namespace>                    // 📖 Public - Identity of a flag
├── BooleanFeature<M>                             // 📖 Boolean flags
├── StringFeature<M>                              // 📖 String flags
├── IntFeature<M>                                 // 📖 Integer flags
├── DoubleFeature<M>                              // 📖 Double flags
├── EnumFeature<E : Enum<E>, M>                   // 📖 Enum flags
└── DataClassFeature<T : DataClassWithSchema, M>  // 📖 Structured config
```

**Type Parameters**:
- `T` - The value type (Boolean, String, Int, Double, Enum, DataClass)
- `M` - The namespace (phantom type for compile-time isolation)

### 2. Flag Definition System
**Location**: `src/main/kotlin/io/amichne/konditional/core/FlagDefinition.kt`

```kotlin
FlagDefinition<T : Any, C : Context, M : Namespace>  // 🔒 Internal evaluation model
```

**Type Parameters**:
- `T` - Value type
- `C` - Context type for evaluation (allows custom contexts)
- `M` - Namespace

**Key Properties**:
- `defaultValue: T` - Returned when no rules match or inactive
- `feature: Feature<T, M>` - The feature this defines
- `values: List<ConditionalValue<T, C, M>>` - Targeting rules (sorted by specificity)
- `isActive: Boolean` - Kill switch
- `salt: String` - For deterministic bucketing

### 3. Namespace System
**Location**: `src/main/kotlin/io/amichne/konditional/core/Namespace.kt`

```kotlin
sealed class Namespace(
    val id: String,
    internal val registry: NamespaceRegistry
) : NamespaceRegistry by registry
```

**Purpose**: Provides compile-time and runtime isolation between teams

**Predefined Namespaces**:
- `Namespace.Global` - Shared flags
- `Namespace.Authentication` - Auth flags
- `Namespace.Payments` - Payment flags
- `Namespace.Messaging` - Messaging flags
- `Namespace.Search` - Search flags
- `Namespace.Recommendations` - Recommendation flags

**Key Insight**: Each namespace has its own `InMemoryNamespaceRegistry` instance, preventing cross-namespace configuration errors.

### 4. FeatureContainer (Primary User API)
**Location**: `src/main/kotlin/io/amichne/konditional/core/features/FeatureContainer.kt`

```kotlin
abstract class FeatureContainer<M : Namespace>(
    internal val namespace: M
) : FeatureAware<M>
```

**Usage Pattern**:
```kotlin
object PaymentFeatures : FeatureContainer<Namespace.Payments>(Namespace.Payments) {
    val APPLE_PAY by boolean(default = false) {
        rule { platforms(Platform.IOS) } returns true
    }
}
```

**Key Methods**:
- `boolean(default, flagScope)` - Create boolean feature
- `string(default, stringScope)` - Create string feature
- `integer(default, integerScope)` - Create int feature
- `double(default, decimalScope)` - Create double feature
- `enum(default, enumScope)` - Create enum feature
- `dataClass(default, dataClassScope)` - Create data class feature
- `allFeatures()` - Get all registered features

### 5. Evaluation System
**Location**: `src/main/kotlin/io/amichne/konditional/core/result/utils/ContextEvaluationUtils.kt`

```kotlin
// Primary API (never throws)
fun <T, C, M> C.evaluateSafe(key: Feature<T, M>): EvaluationResult<T>

// Convenience APIs
fun <T, C, M> C.evaluateOrNull(key: Feature<T, M>): T?
fun <T, C, M> C.evaluateOrDefault(key: Feature<T, M>, default: T): T
fun <T, C, M> C.evaluateOrThrow(key: Feature<T, M>): T  // ⚠️ Throws
```

**Evaluation Algorithm** (FlagDefinition.kt:72-84):
1. If `!isActive`, return `defaultValue`
2. Find first matching rule (sorted by specificity descending)
3. Check rollout eligibility via SHA-256 bucketing
4. Return rule value or `defaultValue`

### 6. Rule System
**Location**: `src/main/kotlin/io/amichne/konditional/rules/`

```kotlin
Rule<C : Context>(
    val rollout: Rampup,
    internal val baseEvaluable: BaseEvaluable<C>,
    val extension: Evaluable<C>
)
```

**Matching**:
```kotlin
fun matches(context: C): Boolean =
    baseEvaluable.matches(context) && extension.matches(context)
```

**Specificity** (determines evaluation order):
```kotlin
fun specificity(): Int =
    baseEvaluable.specificity() + extension.specificity()
```

**BaseEvaluable Components**:
- Locale matching: +1 specificity per locale
- Platform matching: +1 specificity per platform
- Version range matching: +1 specificity if present

### 7. Configuration & Registry
**Location**: `src/main/kotlin/io/amichne/konditional/core/`

```kotlin
Configuration(
    val flags: Map<Feature<*, *>, FlagDefinition<*, *, *>>
)

interface NamespaceRegistry {
    fun load(config: Configuration)
    val configuration: Configuration
    fun <T, C, M> flag(key: Feature<T, M>): FlagDefinition<T, C, M>?
    fun allFlags(): Map<Feature<*, *>, FlagDefinition<*, *, *>>
}
```

**InMemoryNamespaceRegistry**:
- Thread-safe via `AtomicReference<Configuration>`
- Lock-free reads
- CAS-based updates
- Test override support

## Data Flow

### 1. Feature Definition Flow
```
FeatureContainer.boolean()
  → ContainerFeaturePropertyDelegate (captures property name)
  → lazy { BooleanFeature(name, namespace) }
  → FlagBuilder(feature).apply(configScope)
  → FlagBuilder.build() → FlagDefinition
  → namespace.updateDefinition(flagDefinition)
  → InMemoryNamespaceRegistry.updateDefinition()
  → AtomicReference.updateAndGet { ... }
```

### 2. Evaluation Flow
```
context.evaluateSafe(feature)
  → feature.namespace.flag(feature) → FlagDefinition?
  → flagDefinition.evaluate(context)
  → if !isActive: return defaultValue
  → conditionalValues.firstOrNull {
      it.rule.matches(context) && isInEligibleSegment(...)
    }?.value ?: defaultValue
```

### 3. Rule Matching Flow
```
rule.matches(context)
  → baseEvaluable.matches(context)
      - Check locale in set
      - Check platform in set
      - Check version in range
  → extension.matches(context)
      - Custom business logic
  → return base AND extension
```

## Type Parameter Architecture (Current - Post Simplification)

### Feature Layer (2 parameters)
```kotlin
Feature<T : Any, M : Namespace>
```
- `T`: Value type (what the flag returns)
- `M`: Namespace (phantom type for isolation)

### Definition Layer (3 parameters)
```kotlin
FlagDefinition<T : Any, C : Context, M : Namespace>
```
- `T`: Value type
- `C`: Context type (enables custom context types)
- `M`: Namespace

### DSL Layer (3 parameters)
```kotlin
FlagScope<T : Any, C : Context, M : Namespace>
FlagBuilder<T : Any, C : Context, M : Namespace>
```
- `T`: Value type
- `C`: Context type for rules
- `M`: Namespace

### Key Insight
The `C` parameter was removed from `Feature` because:
1. Features themselves don't evaluate - FlagDefinitions do
2. Evaluation can be polymorphic (any `C : Context` works)
3. Rules within a flag can still be context-specific
4. Simplifies user-facing API significantly

## Thread Safety Model

### Lock-Free Reads
```kotlin
private val current = AtomicReference(Configuration(emptyMap()))

override val configuration: Configuration
    get() = current.get()  // No locks!
```

### Atomic Updates
```kotlin
internal fun updateDefinition(definition: FlagDefinition<T, C, *>) {
    current.updateAndGet { currentSnapshot ->
        val mutableFlags = currentSnapshot.flags.toMutableMap()
        mutableFlags[definition.feature] = definition
        Configuration(mutableFlags)  // New immutable instance
    }
}
```

### Deterministic Bucketing (Thread-Safe)
```kotlin
private fun stableBucket(flagKey: String, id: HexId, salt: String): Int {
    val digest = MessageDigest.getInstance("SHA-256")  // New instance per call
    // ... SHA-256 hash → 4 bytes → int → mod 10,000
}
```

## Error Handling Philosophy

### No Exceptions in Normal Flow
All operations return typed results:
```kotlin
sealed interface EvaluationResult<out S> {
    data class Success<S>(val value: S)
    data class FlagNotFound(val key: String)
    data class EvaluationError(val key: String, val error: Throwable)
}

sealed interface ParseResult<out T> {
    data class Success<T>(val value: T)
    data class Failure(val error: ParseError)
}
```

### Internal Error Types (Not Exposed)
```kotlin
sealed class ParseError {
    data class InvalidJson(val message: String)
    data class InvalidSnapshot(val message: String)
    data class TypeMismatch(...)
    data class UnsupportedType(...)
}
```

## Key Files Reference

| File | Purpose | Visibility |
|------|---------|-----------|
| `Feature.kt` | Base abstraction | 📖 Public |
| `FlagDefinition.kt` | Evaluation model | 🔒 Internal |
| `FeatureContainer.kt` | Main user API | 📖 Public |
| `Namespace.kt` | Isolation boundary | 📖 Public |
| `NamespaceRegistry.kt` | Configuration management | 📖 Public interface |
| `InMemoryNamespaceRegistry.kt` | Registry implementation | 🔒 Internal |
| `Configuration.kt` | Immutable snapshot | 📖 Public |
| `FlagBuilder.kt` | DSL implementation | 🔒 Internal |
| `Rule.kt` | Targeting logic | 📖 Public |
| `Context.kt` | Evaluation context | 📖 Public |

## Module Structure

```
io.amichne.konditional/
├── context/              # Evaluation context (Context, Platform, Locale, Version)
├── core/
│   ├── features/        # Feature definitions and containers
│   ├── dsl/            # DSL interfaces (FlagScope, RuleScope)
│   ├── types/          # Type encoding system (EncodableValue hierarchy)
│   ├── registry/       # Thread-safe flag storage
│   ├── instance/       # Configuration snapshots
│   └── result/         # Result types (ParseResult, EvaluationResult)
├── rules/              # Rule evaluation and matching
│   ├── evaluable/      # Composable evaluation logic
│   └── versions/       # Version range constraints
├── serialization/      # JSON serialization with Moshi
└── internal/           # Internal implementation details
    ├── builders/       # DSL implementation (FlagBuilder, RuleBuilder)
    └── serialization/  # Serialization adapters and models
```

## Compile-Time Guarantees

1. **Type-bound features**: Cannot evaluate a Payments feature with Authentication namespace
2. **No null values**: All defaults required, no nullable types
3. **Exhaustive sealing**: All result types sealed, forcing exhaustive when-expressions
4. **Evidence-based encoding**: Only supported types can be encoded
5. **Namespace isolation**: Cannot mix flags from different namespaces

## Runtime Guarantees

1. **Deterministic evaluation**: Same context + same flag = same result
2. **Thread-safe reads**: No locks on evaluation path
3. **Atomic updates**: Configuration changes are atomic
4. **No class cast exceptions**: Type erasure handled with safe casts
5. **Platform-stable hashing**: SHA-256 bucketing works across all platforms
