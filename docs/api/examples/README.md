# Konditional Examples

**Time to complete: 20-30 minutes**

This directory contains complete, runnable examples showing you how to migrate from traditional feature flags to Konditional's type-safe approach.

```shell
gh repo clone https://github.com/amichne/konditional-examples.git
```

---

## Learning Path

Follow these examples in order for the best learning experience:

### 1. [Basic Usage](01-BasicUsage.kt)

**Time: 5-8 minutes**

Your first steps with Konditional - learn the fundamentals by migrating simple boolean toggles.

??? example "What you'll learn"
    - Defining type-safe feature flags with `Conditional<S, C>`
    - Creating evaluation `Context` for targeting
    - Platform-specific targeting rules
    - Version-based rollouts
    - Configuration flags (non-boolean values)
    - Multiple rules with specificity ordering

**Migration focus**: Replace `getBoolean()` and `getString()` calls with type-safe flags.

---

### 2. [Rollouts](02-Rollouts.kt)

**Time: 8-10 minutes**

Master gradual rollouts and percentage-based distribution with deterministic bucketing.

??? example "What you'll learn"
    - Gradual percentage rollouts (10%, 25%, 50%, 100%)
    - Phased rollout strategies
    - Platform-specific rollout percentages
    - **Deterministic bucketing** with StableId (no more random distributions!)
    - Multi-variant A/B testing
    - Salt-based re-bucketing for new experiments

**Migration focus**: Replace manual modulo-based rollouts with deterministic `Rollout`.

---

### 3. [Custom Context](03-CustomContext.kt)

**Time: 8-10 minutes**

Extend `Context` with your domain-specific properties for advanced targeting.

??? example "What you'll learn"
    - Extending Context with custom properties (premium status, subscription tiers, etc.)
    - Enterprise contexts with complex business logic
    - Creating reusable `Evaluable` classes
    - Combining standard targeting (platform, locale) with custom logic
    - Context hierarchies and polymorphism

**Migration focus**: Replace map-based context with structured, type-safe context extensions.

---

### 4. [Serialization](04-Serialization.kt)

**Time: 8-10 minutes**

Integrate with remote configuration systems using type-safe JSON serialization.

??? example "What you'll learn"
    - Serializing and deserializing flag configurations
    - Type-safe error handling with `ParseResult`
    - Patch-based incremental updates
    - Remote configuration loading patterns
    - Graceful degradation with fallback configs
    - Configuration versioning and rollback strategies
    - Hot reloading flags without app restart

**Migration focus**: Replace manual JSON parsing with built-in type-safe serialization.

---

## Running the Examples

Each example file is a standalone Kotlin file with a `main()` function.

### Using IntelliJ IDEA / Android Studio

1. Open the example file
2. Click the green play button next to `main()`
3. View output in the Run window

### Using Command Line

```bash
# Using Kotlin compiler
kotlinc 01-BasicUsage.kt -include-runtime -d basic-usage.jar
java -jar basic-usage.jar

# Or with Gradle (if in a project)
./gradlew run
```

## Common Migration Patterns

### Before & After: Simple Boolean Toggle

<details>
<summary><strong>Traditional approach (click to expand)</strong></summary>

```kotlin
// Scattered definitions
const val FEATURE_KEY = "new_checkout"

// Usage site 1
if (featureManager.isEnabled(FEATURE_KEY)) {
    showNewCheckout()
}

// Usage site 2 - typo risk!
if (featureManager.isEnabled("new_chekout")) {  // ❌ Typo!
    showNewCheckout()
}
```
</details>

<details>
<summary><strong>Konditional approach (click to expand)</strong></summary>

```kotlin
// Define once
val NEW_CHECKOUT: Conditional<Boolean, Context> = Conditional("new_checkout")

// Configure once
ConfigBuilder.config {
    NEW_CHECKOUT with {
        default(value = false)
        rule { platforms(Platform.IOS) } implies true
    }
}

// Use everywhere - compile-time safe!
if (context.evaluate(NEW_CHECKOUT)) {
    showNewCheckout()
}

// Typos caught at compile time
// if (context.evaluate(NEW_CHEKOUT)) { }  // ❌ Won't compile!
```
</details>

### Before & After: Percentage Rollout

<details>
<summary><strong>Traditional approach (inconsistent)</strong></summary>

```kotlin
// Manual bucketing - different results each session
val userId = user.id.hashCode()
val inRollout = (userId % 100) < 25  // 25% rollout

// Problem: Same user gets different results across sessions
```
</details>

<details>
<summary><strong>Konditional approach (deterministic)</strong></summary>

```kotlin
ConfigBuilder.config {
    BETA_FEATURE with {
        default(value = false)
        rule {
            rollout = Rollout.of(25.0)  // Deterministic SHA-256 bucketing
        } implies true
    }
}

// Same user ALWAYS gets the same result
val enabled = context.evaluate(BETA_FEATURE)
```
</details>

### Before & After: Custom Targeting

<details>
<summary><strong>Traditional approach (map-based)</strong></summary>

```kotlin
// Untyped context map
val context = mapOf(
    "userId" to user.id,
    "isPremium" to user.isPremium,
    "accountAge" to user.accountAge
)

// Manual logic scattered throughout codebase
fun isFeatureEnabled(): Boolean {
    val isPremium = context["isPremium"] as? Boolean ?: false
    val accountAge = context["accountAge"] as? Int ?: 0
    return isPremium && accountAge > 30
}
```
</details>

<details>
<summary><strong>Konditional approach (type-safe)</strong></summary>

```kotlin
// Type-safe context extension
data class UserContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val isPremium: Boolean,
    val accountAge: Int
) : Context

// Declarative rules
ConfigBuilder.config {
    PREMIUM_FEATURE with {
        default(value = false)
        rule {
            extension {
                object : Evaluable<UserContext>() {
                    override fun matches(ctx: UserContext) =
                        ctx.isPremium && ctx.accountAge > 30
                    override fun specificity() = 2
                }
            }
        } implies true
    }
}
```
</details>

---

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

## Migration Checklist

After completing these examples, you should be able to:

- [ ] Replace string-based flags with type-safe `Conditional<S, C>`
- [ ] Create evaluation contexts with proper targeting dimensions
- [ ] Configure flags using the builder DSL
- [ ] Implement gradual rollouts with deterministic bucketing
- [ ] Extend `Context` for domain-specific targeting
- [ ] Serialize and deserialize flag configurations
- [ ] Handle errors gracefully with `ParseResult` and `EvaluationResult`

## Next Steps

### For Quick Integration

1. **[Builder DSL](../Builders.md)** (10 min) - Master the configuration syntax
2. **[Core API](../Core.md)** (10 min) - Understand Conditionals and evaluation
3. **Start migrating!** - Pick one flag and convert it

### For Deep Understanding

1. **[API Overview](../Overview.md)** (10 min) - Architecture and design principles
2. **[Context System](../Context.md)** (8 min) - Targeting dimensions explained
3. **[Rules System](../Rules.md)** (8 min) - Advanced rule composition
4. **[Serialization](../Serialization.md)** (8 min) - Remote config integration

### For Production Deployment

1. Review the [test suite](../../../src/test/) for testing patterns
2. Set up remote configuration with your backend
3. Plan your rollout strategy (start with low-risk flags)
4. Monitor and iterate

## Common Questions

<details>
<summary><strong>Q: Can I gradually migrate, or must I convert everything at once?</strong></summary>

**A:** You can migrate gradually! Konditional flags can coexist with your existing flag system. Start with one or two flags, gain confidence, then migrate more.

```kotlin
// Both can coexist
val newFeature = context.evaluate(NEW_FEATURE)        // Konditional
val oldFeature = legacyFlags.getBoolean("old_feature", false)  // Legacy
```
</details>

<details>
<summary><strong>Q: How do I handle flags that don't fit the standard Context?</strong></summary>

**A:** Extend `Context` with custom properties! See [03-CustomContext.kt](03-CustomContext.kt) for examples.

```kotlin
data class MyContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val customProperty: String  // Your custom targeting
) : Context
```
</details>

<details>
<summary><strong>Q: What if my rollout percentages need to change frequently?</strong></summary>

**A:** Use remote configuration! See [04-Serialization.kt](04-Serialization.kt) to learn how to load flag configs from your backend without app updates.
</details>

<details>
<summary><strong>Q: How do I ensure deterministic bucketing for A/B tests?</strong></summary>

**A:** Use `StableId` with a consistent user identifier:

```kotlin
val context = Context(
    // ... other fields
    stableId = StableId.of(userId)  // Same user ID = same bucket
)
```

Konditional uses SHA-256 hashing to ensure the same user always gets the same variant.
</details>

## Additional Resources

- [GitHub Repository](https://github.com/amichne/konditional) - Source code and issues
- [API Reference](../Overview.md) - Complete API documentation
- [Parse, Don't Validate](https://lexi-lambda.github.io/blog/2019/11/05/parse-don-t-validate/) - Design philosophy

---

**Estimated total time to migrate your first production flag: 30-45 minutes** 
