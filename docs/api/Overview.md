# Konditional API Overview

Konditional is a type-safe, composable feature flag library for Kotlin that follows Parse Don't Validate principles. It enables context-driven conditional logic with compile-time safety and runtime flexibility.

## Core Concepts

### 1. Conditionals

A `Conditional<S, C>` is a typed feature flag key that identifies a flag in the registry.

- `S`: The value type the flag returns (Boolean, String, custom types, etc.)
- `C`: The context type used for evaluation

```kotlin
val ENABLE_FEATURE: Conditional<Boolean, MyContext> =
    Conditional("enable_feature")
```

### 2. Context

`Context` provides the evaluation dimensions for feature flags:

- **locale**: Application locale (AppLocale)
- **platform**: Deployment platform (Platform)
- **appVersion**: Semantic version (Version)
- **stableId**: Unique identifier for deterministic bucketing (StableId)

Extend `Context` to add custom targeting dimensions.

### 3. Feature Flags

`FeatureFlag<S, C>` defines the behavior of a flag:

- Default value when no rules match
- List of conditional values (rule + target value pairs)
- Active/inactive state
- Salt for deterministic hashing

### 4. Rules

`Rule<C>` defines matching criteria:

- **Base matching**: locale, platform, version range
- **Extension matching**: custom logic via `Evaluable<C>`
- **Rollout**: percentage-based gradual deployment
- **Specificity**: precedence when multiple rules match

### 5. Registry

`FlagRegistry` manages flag configurations:

- Thread-safe singleton registry (default)
- Load complete snapshots
- Apply incremental patches
- Update individual flags

## Architecture

```mermaid
graph TD
    A[Conditional Key] --> B[FlagRegistry]
    B --> C[FeatureFlag]
    C --> D[ConditionalValue]
    D --> E[Rule]
    D --> F[Target Value]
    E --> G[BaseEvaluable]
    E --> H[Extension Evaluable]
    I[Context] --> C
    I --> E

    style A fill:#e1f5ff
    style B fill:#fff4e1
    style C fill:#ffe1f5
    style D fill:#e1ffe1
    style E fill:#f5e1ff
```

## Evaluation Flow

```mermaid
sequenceDiagram
    participant Client
    participant Context
    participant Registry
    participant Flag
    participant Rules

    Client->>Context: evaluate(conditional)
    Context->>Registry: featureFlag(conditional)
    Registry->>Flag: Found
    Context->>Flag: evaluate(context)
    Flag->>Rules: Find matching rules
    Rules->>Rules: Check specificity
    Rules->>Rules: Check rollout
    Flag->>Client: Return value
```

## Type Safety

Konditional enforces type safety at compile time:

```kotlin
// Compile error: Type mismatch
val BOOLEAN_FLAG: Conditional<Boolean, MyContext> = Conditional("flag")
val stringValue: String = context.evaluate(BOOLEAN_FLAG) // Won't compile!

// Correct usage
val boolValue: Boolean = context.evaluate(BOOLEAN_FLAG) // Type-safe
```

## Parse Don't Validate

Konditional follows functional programming principles:

- Refined types encode invariants (Rollout, StableId, Version)
- Result types for fallible operations (EvaluationResult, ParseResult)
- Illegal states are unrepresentable
- No redundant validation in domain logic

## Key Features

### Thread-Safe Registry

The singleton `FlagRegistry` uses atomic operations for safe concurrent access.

### Composable Rules

Rules compose base targeting with custom extension logic:

```kotlin
Rule(
    locales = setOf(AppLocale.EN_US),
    platforms = setOf(Platform.IOS),
    extension = object : Evaluable<MyContext>() {
        override fun matches(context: MyContext) = context.isPremium
        override fun specificity() = 1
    }
)
```

### Deterministic Rollouts

Rollout percentages use consistent hashing with stable IDs for deterministic bucketing.

### JSON Serialization

Built-in serialization for remote configuration:

```kotlin
val serializer = SnapshotSerializer.default
val json = serializer.serialize(konfig)
val result = serializer.deserialize(json) // ParseResult<Konfig>
```

### DSL Builders

Type-safe DSL for defining flags:

```kotlin
ConfigBuilder.config {
    MY_FLAG with {
        default(value = false)
        rule {
            platforms(Platform.IOS)
            rollout = Rollout.of(50.0)
        } implies true
    }
}
```

## Next Steps

- [Core API](Core.md) - Conditional evaluation and results
- [Context System](Context.md) - Context types and extension
- [Flag Registry](Flags.md) - Registry operations and lifecycle
- [Rules System](Rules.md) - Rule composition and evaluation
- [Builder DSL](Builders.md) - Declarative flag configuration
- [Serialization](Serialization.md) - JSON serialization and deserialization
- [Examples](examples/) - Complete working examples
