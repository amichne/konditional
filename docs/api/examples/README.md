# Konditional Examples

This directory contains complete, runnable examples demonstrating how to use the Konditional library.

## Example Files

### 01-BasicUsage.kt

Demonstrates fundamental feature flag usage:

- Defining boolean feature flags
- Configuring flags with default values
- Platform-specific targeting
- Version-based rollouts
- Configuration flags (non-boolean values)
- Multiple rules with specificity

**Key Concepts**: Conditionals, Context, basic rules, evaluation

### 02-Rollouts.kt

Shows gradual rollout and percentage-based distribution:

- Gradual percentage rollouts (10%, 25%, 50%, 100%)
- Phased rollout strategies
- Platform-specific rollout percentages
- Deterministic bucketing with StableId
- Multi-variant A/B testing
- Salt-based re-bucketing

**Key Concepts**: Rollout, deterministic hashing, A/B testing

### 03-CustomContext.kt

Explores context extension and custom targeting:

- Extending Context with domain-specific properties
- Enterprise contexts with subscription tiers
- Reusable Evaluable classes
- Combining standard and custom targeting
- Context hierarchies and polymorphism

**Key Concepts**: Context extension, Evaluable, custom targeting logic

### 04-Serialization.kt

Covers JSON serialization and remote configuration:

- Basic serialization/deserialization
- Error handling with ParseResult
- Patch serialization for incremental updates
- Remote configuration loading
- Graceful degradation with fallback configs
- Configuration versioning and rollback
- Hot reloading without restart

**Key Concepts**: SnapshotSerializer, ParseResult, Konfig, KonfigPatch

## Running the Examples

Each example file is a standalone Kotlin file with a `main()` function. To run:

```bash
# Using Kotlin compiler
kotlinc 01-BasicUsage.kt -include-runtime -d basic-usage.jar
java -jar basic-usage.jar

# Or with your IDE
# Open the file and run the main() function
```

## Example Organization

Examples are organized from simple to complex:

1. **Basic Usage** - Start here if you're new to Konditional
2. **Rollouts** - Learn gradual deployment and A/B testing
3. **Custom Context** - Extend the library for your domain
4. **Serialization** - Integrate with remote configuration

## Common Patterns Demonstrated

### Simple Feature Toggle

```kotlin
val FEATURE: Conditional<Boolean, Context> = Conditional("feature")

ConfigBuilder.config {
    FEATURE with {
        default(value = false)
    }
}

val isEnabled = context.evaluate(FEATURE)
```

### Platform-Specific Feature

```kotlin
ConfigBuilder.config {
    FEATURE with {
        default(value = false)
        rule {
            platforms(Platform.IOS)
        } implies true
    }
}
```

### Gradual Rollout

```kotlin
ConfigBuilder.config {
    FEATURE with {
        default(value = false)
        rule {
            rollout = Rollout.of(25.0)  // 25% of users
        } implies true
    }
}
```

### Custom Targeting

```kotlin
data class UserContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val isPremium: Boolean
) : Context

ConfigBuilder.config {
    FEATURE with {
        default(value = false)
        rule {
            extension {
                object : Evaluable<UserContext>() {
                    override fun matches(context: UserContext) = context.isPremium
                    override fun specificity() = 1
                }
            }
        } implies true
    }
}
```

### Remote Configuration

```kotlin
val json = fetchFromServer()

when (val result = SnapshotSerializer.default.deserialize(json)) {
    is ParseResult.Success -> {
        FlagRegistry.load(result.value)
        println("Configuration loaded")
    }
    is ParseResult.Failure -> {
        println("Error: ${result.error}")
    }
}
```

## Next Steps

After reviewing these examples:

1. Read the [API documentation](../) for detailed reference
2. Check the [test suite](../../../src/test/) for more examples
3. Build your own feature flags for your application

## Questions?

- Review the [Overview](../Overview.md) for high-level concepts
- Check the specific documentation for each subsystem:
  - [Core API](../Core.md)
  - [Context System](../Context.md)
  - [Rules System](../Rules.md)
  - [Builder DSL](../Builders.md)
  - [Serialization](../Serialization.md)
