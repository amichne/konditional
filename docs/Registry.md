# Registry: Namespace-Based Flag Organization

This document explains Konditional's registry system, which provides compile-time and runtime isolation for feature flags through taxonomies.

---

## Overview

Konditional organizes feature flags using **taxonomies** - isolated namespaces that provide:

- **Compile-time isolation**: Features are type-bound to their namespace
- **Runtime isolation**: Each namespace has its own flag registry
- **Governance**: All taxonomies enumerated in one sealed hierarchy
- **Direct operations**: Taxonomies implement `ModuleRegistry`, eliminating `.registry` access

```kotlin
// Features are bound to specific taxonomies
object PaymentFeatures : FeatureContainer<Namespace.Payments>(
    Namespace.Payments
) {
    val APPLE_PAY by boolean(default = false)
}

// Evaluate using the bound namespace
val isEnabled = context.evaluate(PaymentFeatures.APPLE_PAY)

// Or query the namespace directly
val definition = Namespace.Payments.featureFlag(PaymentFeatures.APPLE_PAY)
```

---

## Namespace System

### What Is a Namespace?

A `Namespace` is a sealed class hierarchy that defines organizational boundaries for feature flags.

**Key characteristics:**

- **Unique ID**: Each namespace has a string identifier
- **Isolated registry**: Each namespace maintains its own registry instance
- **ModuleRegistry operations**: Taxonomies provide direct access to registry methods
- **Sealed hierarchy**: All taxonomies must be defined in the sealed class

### Namespace Types

Konditional provides two namespace categories:

#### Global Namespace

The `Namespace.Global` namespace contains shared flags accessible to all teams:

```kotlin
data object Global : Namespace("global")
```

**Use for:**
- System-wide kill switches
- Maintenance mode flags
- Cross-cutting feature toggles
- Common infrastructure flags

**Example:**

```kotlin
object CoreFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val KILL_SWITCH by boolean(default = false)
    val MAINTENANCE_MODE by boolean(default = false)
    val DEBUG_LOGGING by boolean(default = false)
}
```

#### Domain Taxonomies

Domain taxonomies provide isolated namespaces for functional areas:

```kotlin
sealed class Domain(id: String) : Namespace(id) {
    data object Authentication : Domain("auth")
    data object Payments : Domain("payments")
    data object Messaging : Domain("messaging")
    data object Search : Domain("search")
    data object Recommendations : Domain("recommendations")
}
```

**Each domain namespace provides:**

- Independent registry instance (runtime isolation)
- Type-bound features (compile-time isolation)
- Independent serialization/deployment
- No cross-namespace flag access

**Example:**

```kotlin
object AuthFeatures : FeatureContainer<Namespace.Authentication>(
    Namespace.Authentication
) {
    val SOCIAL_LOGIN by boolean(default = false)
    val TWO_FACTOR_AUTH by boolean(default = true)
    val BIOMETRIC_AUTH by boolean(default = false)
}

object PaymentFeatures : FeatureContainer<Namespace.Payments>(
    Namespace.Payments
) {
    val APPLE_PAY by boolean(default = false)
    val GOOGLE_PAY by boolean(default = false)
    val CRYPTO_PAYMENTS by boolean(default = false)
}
```

### Adding New Taxonomies

To add a new domain namespace, add an object to the `Domain` sealed class:

```kotlin
sealed class Domain(id: String) : Namespace(id) {
    // ... existing domains ...

    data object Analytics : Domain("analytics")
    data object Notifications : Domain("notifications")
}
```

**The sealed hierarchy ensures:**

- No namespace ID collisions at compile time
- Exhaustive when-expressions
- IDE autocomplete for all taxonomies
- Central visibility of all modules

---

## ModuleRegistry Interface

`ModuleRegistry` defines the contract for managing feature flag configurations:

```kotlin
interface ModuleRegistry {
    fun load(config: Konfig)
    fun configuration(): Konfig
    fun <S, T, C, M> featureFlag(key: Feature<S, T, C, M>): FlagDefinition<S, T, C, M>?
    fun allFlags(): Map<Feature<*, *, *, *>, FlagDefinition<*, *, *, *>>
}
```

### Core Operations

#### Loading Configuration

Load a complete flag configuration snapshot:

```kotlin
// Create a Konfig (typically from JSON deserialization)
val configuration = NamespaceSnapshotSerializer.deserialize<Namespace.Payments>(jsonString)

// Load into namespace's registry
when (configuration) {
    is ParseResult.Success -> Namespace.Payments.load(configuration.value)
    is ParseResult.Failure -> logger.error("Failed to load: ${configuration.error}")
}
```

**Properties:**

- **Atomic**: Entire configuration replaces current state in one operation
- **Thread-safe**: Backed by `AtomicReference` for lock-free updates
- **Consistent**: Readers see complete old OR new config, never partial state

#### Retrieving Current State

Get the current configuration snapshot:

```kotlin
val currentConfig = Namespace.Payments.configuration()

// Inspect flags
currentConfig.flags.forEach { (feature, definition) ->
    println("${feature.key}: default=${definition.defaultValue}, active=${definition.isActive}")
}
```

**Use cases:**

- Serialization to JSON
- State inspection
- Debugging
- Configuration export

#### Querying Individual Flags

Retrieve a specific flag definition:

```kotlin
val definition = Namespace.Payments.featureFlag(PaymentFeatures.APPLE_PAY)

if (definition != null) {
    println("Default: ${definition.defaultValue}")
    println("Active: ${definition.isActive}")
    println("Rules: ${definition.values.size}")
}
```

**Returns:** `FlagDefinition<S, T, C, M>?` - null if flag not found

#### Querying All Flags

Retrieve all flags from a namespace:

```kotlin
val allFlags = Namespace.Payments.allFlags()

allFlags.forEach { (feature, definition) ->
    println("${feature.key}: ${definition.defaultValue}")
}
```

### Factory Function

Create new registry instances for testing:

```kotlin
companion object {
    operator fun invoke(configuration: Konfig = Konfig(emptyMap())): ModuleRegistry
}
```

**Usage:**

```kotlin
@Test
fun `test feature behavior`() {
    // Create isolated registry for testing
    val testRegistry = ModuleRegistry()

    // Note: Direct registry usage is for advanced cases
    // Prefer using Namespace instances in production
}
```

---

## Konfig: Immutable Configuration Snapshot

`Konfig` is an immutable data class representing a complete registry state at a point in time.

### Characteristics

**Immutable:**
- Cannot be modified after creation
- Thread-safe for concurrent reads
- Safe to share across threads

**Snapshot Semantics:**
- Represents state at a point in time
- Remains consistent even if registry is updated
- Enables consistent multi-flag evaluations

### Usage Patterns

#### Exporting Configuration

```kotlin
// Get current state
val configuration = Namespace.Payments.configuration()

// Serialize to JSON
val json = NamespaceSnapshotSerializer.serialize(configuration)

// Save to file or send over network
File("payment-config.json").writeText(json)
```

#### Importing Configuration

```kotlin
// Read from file
val json = File("payment-config.json").readText()

// Deserialize
val result = NamespaceSnapshotSerializer.deserialize<Namespace.Payments>(json)

when (result) {
    is ParseResult.Success -> {
        // Load into registry
        Namespace.Payments.load(result.value)
        println("Loaded ${result.value.flags.size} flags")
    }
    is ParseResult.Failure -> {
        logger.error("Parse error: ${result.error}")
    }
}
```

#### Consistent Multi-Flag Evaluation

```kotlin
// Get snapshot
val snapshot = Namespace.Payments.configuration()

// Evaluate multiple flags against same snapshot
// (even if registry is updated concurrently)
val applePayDef = snapshot.flags[PaymentFeatures.APPLE_PAY]
val googlePayDef = snapshot.flags[PaymentFeatures.GOOGLE_PAY]

val applePayEnabled = applePayDef?.evaluate(context)
val googlePayEnabled = googlePayDef?.evaluate(context)
```

---

## Namespace-Based Isolation

### Compile-Time Isolation

Features are type-bound to their namespace, preventing cross-namespace usage:

```kotlin
object AuthFeatures : FeatureContainer<Namespace.Authentication>(
    Namespace.Authentication
) {
    val SOCIAL_LOGIN by boolean(default = false)
}

object PaymentFeatures : FeatureContainer<Namespace.Payments>(
    Namespace.Payments
) {
    val APPLE_PAY by boolean(default = false)
}

// ✓ Correct: Feature bound to Authentication namespace
val authEnabled = context.evaluate(AuthFeatures.SOCIAL_LOGIN)

// ✗ Won't compile: SOCIAL_LOGIN belongs to Authentication, not Payments
Namespace.Payments.featureFlag(AuthFeatures.SOCIAL_LOGIN)  // Type error
```

**Benefits:**

- Impossible to query flags from wrong namespace
- Refactoring is safe (types guide changes)
- Clear ownership boundaries
- No runtime namespace checks needed

### Runtime Isolation

Each namespace has its own `ModuleRegistry` instance:

```kotlin
// Authentication namespace has its own registry
Namespace.Authentication.load(authConfig)

// Payments namespace has its own registry
Namespace.Payments.load(paymentsConfig)

// Registries are independent - no shared state
```

**Benefits:**

- Independent deployment of namespace configurations
- No key collisions between taxonomies
- Team autonomy (each team owns their namespace)
- Isolated testing (load different configs per namespace)

### Governance Through Sealed Hierarchy

All taxonomies must be defined in the sealed class:

```kotlin
sealed class Domain(id: String) : Namespace(id) {
    data object Authentication : Domain("auth")
    data object Payments : Domain("payments")
    // All domains visible here
}
```

**Benefits:**

- Central registry of all taxonomies
- No duplicate namespace IDs
- Exhaustive when-expressions
- Clear organizational structure

---

## Thread Safety Guarantees

**Lock-free reads:**

```kotlin
// Multiple threads can read simultaneously without contention
val def1 = namespace.featureFlag(feature)  // Thread 1
val def2 = namespace.featureFlag(feature)  // Thread 2 (concurrent)
```

**Atomic updates:**

```kotlin
// Update atomically replaces entire config
namespace.load(newKonfig)  // All flags update together
```

**Read-write safety:**

```kotlin
// Thread A: Reading
val definition = namespace.featureFlag(feature)
val result = definition?.evaluate(context)

// Thread B: Updating (concurrent)
namespace.load(newKonfig)

// Thread A's reference remains valid
// Sees consistent snapshot (old or new, never mixed)
```

### Performance Characteristics

| Operation          | Latency        | Throughput      |
|--------------------|----------------|-----------------|
| Read flag          | ~5-10 ns       | 100M+ ops/sec   |
| Read (contended)   | ~5-10 ns       | Linear scaling  |
| Load configuration | ~1-10 µs       | Doesn't block reads |

**Key advantage:** Lock-free reads scale linearly with CPU cores.

---

## Registry Operations Through Namespace

Since `Namespace` implements `ModuleRegistry` via delegation, you can call registry methods directly:

### Loading Configuration

```kotlin
// Old style (verbose)
Namespace.Payments.registry.load(configuration)

// New style (direct)
Namespace.Payments.load(configuration)
```

### Querying State

```kotlin
// Get current snapshot
val snapshot = Namespace.Payments.configuration()

// Query specific flag
val definition = Namespace.Payments.featureFlag(PaymentFeatures.APPLE_PAY)

// Get all flags
val allFlags = Namespace.Payments.allFlags()
```

### Example: Loading from Remote Config

```kotlin
suspend fun loadRemoteConfig(namespace: Namespace.Payments) {
    try {
        // Fetch from remote
        val json = httpClient.get("https://config.example.com/payments")

        // Deserialize
        val result = NamespaceSnapshotSerializer.deserialize<Namespace.Payments>(json)

        when (result) {
            is ParseResult.Success -> {
                // Load directly into namespace
                namespace.load(result.value)
                logger.info("Loaded ${result.value.flags.size} payment flags")
            }
            is ParseResult.Failure -> {
                logger.error("Failed to parse: ${result.error}")
            }
        }
    } catch (e: Exception) {
        logger.error("Failed to fetch remote config", e)
    }
}
```

---

## Choosing the Right Namespace

### Use `Namespace.Global` for:

**System-wide flags:**
```kotlin
object SystemFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val KILL_SWITCH by boolean(default = false)
    val MAINTENANCE_MODE by boolean(default = false)
    val RATE_LIMITING by boolean(default = true)
}
```

**Infrastructure flags:**
```kotlin
object InfraFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val NEW_DATABASE by boolean(default = false)
    val CIRCUIT_BREAKER by boolean(default = true)
    val DEBUG_LOGGING by boolean(default = false)
}
```

**Cross-cutting concerns:**
```kotlin
object MonitoringFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val METRICS_ENABLED by boolean(default = true)
    val TRACING_ENABLED by boolean(default = false)
    val PROFILING_ENABLED by boolean(default = false)
}
```

### Use `Namespace.*` for:

**Team-owned features:**
```kotlin
// Authentication team owns this namespace
object AuthFeatures : FeatureContainer<Namespace.Authentication>(
    Namespace.Authentication
) {
    val SOCIAL_LOGIN by boolean(default = false)
    val TWO_FACTOR_AUTH by boolean(default = true)
    val PASSWORDLESS_LOGIN by boolean(default = false)
}
```

**Domain-specific experiments:**
```kotlin
// Payments team owns this namespace
object PaymentExperiments : FeatureContainer<Namespace.Payments>(
    Namespace.Payments
) {
    val NEW_CHECKOUT_FLOW by boolean(default = false)
    val ONE_CLICK_PURCHASE by boolean(default = false)
    val SAVED_PAYMENT_METHODS by boolean(default = true)
}
```

**Isolated deployments:**
```kotlin
// Search team can deploy independently
object SearchFeatures : FeatureContainer<Namespace.Search>(
    Namespace.Search
) {
    val FUZZY_SEARCH by boolean(default = false)
    val AUTOCOMPLETE by boolean(default = true)
    val PERSONALIZED_RESULTS by boolean(default = false)
}
```

### Decision Matrix

| Aspect              | Global                          | Domain                            |
|---------------------|---------------------------------|-----------------------------------|
| **Ownership**       | Central team / SRE              | Feature team                      |
| **Scope**           | System-wide                     | Domain-specific                   |
| **Deployment**      | Coordinated                     | Independent                       |
| **Governance**      | Centralized                     | Decentralized                     |
| **Key collisions**  | Shared namespace (use prefixes) | Isolated namespace                |
| **Examples**        | Kill switches, maintenance      | Team features, experiments        |

---

## FeatureRegistry: Deserialization Support

`FeatureRegistry` is a separate registry for mapping string keys to `Feature` instances during deserialization:

```kotlin
object FeatureRegistry {
    fun <S, T, C> register(conditional: Feature<S, T, C, *>)
    fun get(key: String): ParseResult<Feature<*, *, *, *>>
    fun contains(key: String): Boolean
    fun clear()
}
```

### Why It Exists

When deserializing flags from JSON, we need to reconstruct `Feature` references:

```json
{
  "flags": [
    {
      "key": "apple_pay",
      "default": false
    }
  ]
}
```

The deserializer needs to find `PaymentFeatures.APPLE_PAY` from the string `"apple_pay"`.

### Registration

**Register individual features:**

```kotlin
FeatureRegistry.register(PaymentFeatures.APPLE_PAY)
FeatureRegistry.register(PaymentFeatures.GOOGLE_PAY)
```

**Register all enum members:**

```kotlin
// If using enum-based features
PaymentFeatures.values().forEach { FeatureRegistry.register(it) }
```

**Register FeatureContainer features:**

```kotlin
// FeatureContainer features auto-register when defined
object PaymentFeatures : FeatureContainer<Namespace.Payments>(
    Namespace.Payments
) {
    // These auto-register during initialization
    val APPLE_PAY by boolean(default = false)
    val GOOGLE_PAY by boolean(default = false)
}
```

### Lookup During Deserialization

```kotlin
// Deserializer looks up feature by key
when (val result = FeatureRegistry.get("apple_pay")) {
    is ParseResult.Success -> {
        val feature = result.value
        // Use feature to build FlagDefinition
    }
    is ParseResult.Failure -> {
        logger.error("Feature not found: ${result.error}")
    }
}
```

### Thread Safety Warning

**FeatureRegistry is NOT thread-safe:**

```kotlin
// ✓ Good: Register during initialization
fun main() {
    // Register all features before concurrent access
    PaymentFeatures.values().forEach { FeatureRegistry.register(it) }

    // Now safe to deserialize concurrently
    launch { deserializeConfig1() }
    launch { deserializeConfig2() }
}

// ✗ Bad: Concurrent registration
launch { FeatureRegistry.register(feature1) }  // Race condition!
launch { FeatureRegistry.register(feature2) }
```

**Best practice:** Complete all registration during application startup before any concurrent access.

### Testing

Clear registry between tests:

```kotlin
@BeforeEach
fun setup() {
    FeatureRegistry.clear()
    PaymentFeatures.values().forEach { FeatureRegistry.register(it) }
}
```

---

## Best Practices

### Do: Use Namespace Instances Directly

```kotlin
// ✓ Good: Namespace implements ModuleRegistry
Namespace.Payments.load(configuration)
val snapshot = Namespace.Payments.configuration()

// ✗ Verbose: Accessing internal registry
Namespace.Payments.registry.load(configuration)
```

### Do: Organize by Domain

```kotlin
// ✓ Good: Clear ownership
object AuthFeatures : FeatureContainer<Namespace.Authentication>(
    Namespace.Authentication
) { ... }

object PaymentFeatures : FeatureContainer<Namespace.Payments>(
    Namespace.Payments
) { ... }
```

### Do: Use Global Sparingly

```kotlin
// ✓ Good: True system-wide concern
object SystemFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val KILL_SWITCH by boolean(default = false)
}

// ✗ Bad: Team feature in Global
object FeatureFlags : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val NEW_CHECKOUT by boolean(default = false)  // Should be in Payments domain
}
```

### Do: Register Features Before Deserialization

```kotlin
// ✓ Good: Register during initialization
object AppInitializer {
    fun init() {
        // Register all features
        PaymentFeatures.APPLE_PAY  // Access triggers registration
        PaymentFeatures.GOOGLE_PAY

        // Now safe to deserialize
        loadConfigs()
    }
}
```

### Do: Use Isolated Registries for Testing

```kotlin
// ✓ Good: Test isolation
@Test
fun `test payment feature`() {
    val testRegistry = ModuleRegistry()

    // Configure test registry
    // (Advanced usage - prefer using Namespace in production)
}
```

### Don't: Mix Taxonomies

```kotlin
// ✗ Bad: Won't compile - type mismatch
Namespace.Payments.featureFlag(AuthFeatures.SOCIAL_LOGIN)
```

### Don't: Modify FeatureRegistry Concurrently

```kotlin
// ✗ Bad: Race condition
launch { FeatureRegistry.register(feature1) }
launch { FeatureRegistry.register(feature2) }

// ✓ Good: Register sequentially during init
fun init() {
    FeatureRegistry.register(feature1)
    FeatureRegistry.register(feature2)
}
```

---

## Complete Example

Here's a complete example showing namespace-based organization:

```kotlin
// 1. Define domain-specific features
object PaymentFeatures : FeatureContainer<Namespace.Payments>(
    Namespace.Payments
) {
    val APPLE_PAY by boolean(default = false) {
        rule {
            platforms(Platform.IOS)
            versions { min(2, 0, 0) }
        } returns true
    }

    val GOOGLE_PAY by boolean(default = false) {
        rule {
            platforms(Platform.ANDROID)
        } returns true
    }

    val CRYPTO_PAYMENTS by boolean(default = false) {
        rule {
            rollout = Rollout.of(10.0)  // 10% rollout
        } returns true
    }
}

object AuthFeatures : FeatureContainer<Namespace.Authentication>(
    Namespace.Authentication
) {
    val SOCIAL_LOGIN by boolean(default = false)
    val TWO_FACTOR_AUTH by boolean(default = true)
    val BIOMETRIC_AUTH by boolean(default = false)
}

// 2. Load configurations independently
suspend fun loadConfigurations() {
    // Load payments config
    val paymentsJson = httpClient.get("https://config.example.com/payments")
    val paymentsResult = NamespaceSnapshotSerializer
        .deserialize<Namespace.Payments>(paymentsJson)

    when (paymentsResult) {
        is ParseResult.Success -> {
            Namespace.Payments.load(paymentsResult.value)
        }
        is ParseResult.Failure -> {
            logger.error("Failed to load payments config")
        }
    }

    // Load auth config independently
    val authJson = httpClient.get("https://config.example.com/auth")
    val authResult = NamespaceSnapshotSerializer
        .deserialize<Namespace.Authentication>(authJson)

    when (authResult) {
        is ParseResult.Success -> {
            Namespace.Authentication.load(authResult.value)
        }
        is ParseResult.Failure -> {
            logger.error("Failed to load auth config")
        }
    }
}

// 3. Evaluate features
fun processPayment(context: Context) {
    val applePayEnabled = context.evaluateOrDefault(
        PaymentFeatures.APPLE_PAY,
        default = false
    )

    if (applePayEnabled) {
        showApplePayButton()
    }
}

fun handleLogin(context: Context) {
    val socialLoginEnabled = context.evaluateOrDefault(
        AuthFeatures.SOCIAL_LOGIN,
        default = false
    )

    if (socialLoginEnabled) {
        showSocialLoginOptions()
    }
}

// 4. Export configurations
fun exportConfigs() {
    // Export payments config
    val paymentsSnapshot = Namespace.Payments.configuration()
    val paymentsJson = NamespaceSnapshotSerializer.serialize(paymentsSnapshot)
    File("payments.json").writeText(paymentsJson)

    // Export auth config
    val authSnapshot = Namespace.Authentication.configuration()
    val authJson = NamespaceSnapshotSerializer.serialize(authSnapshot)
    File("auth.json").writeText(authJson)
}

// 5. Inspect registry state
fun debugRegistries() {
    // Inspect payments flags
    println("=== Payments Flags ===")
    Namespace.Payments.allFlags().forEach { (feature, definition) ->
        println("${feature.key}: active=${definition.isActive}, default=${definition.defaultValue}")
    }

    // Inspect auth flags
    println("=== Auth Flags ===")
    Namespace.Authentication.allFlags().forEach { (feature, definition) ->
        println("${feature.key}: active=${definition.isActive}, default=${definition.defaultValue}")
    }
}
```

---

## Summary

| Concept              | Purpose                                                  |
|----------------------|----------------------------------------------------------|
| **Namespace**         | Organizational namespace with isolated registry          |
| **Namespace.Global**  | System-wide flags (kill switches, maintenance)           |
| **Namespace**  | Team-owned flags (domain-specific features)              |
| **ModuleRegistry**   | Interface for loading, querying flag configurations      |
| **Konfig**           | Immutable snapshot of all flags at a point in time       |
| **InMemoryModuleRegistry** | Thread-safe registry implementation (AtomicReference) |
| **FeatureRegistry**  | Maps string keys to Feature instances (deserialization)  |

**Core Principle:** Taxonomies provide compile-time and runtime isolation, enabling independent team ownership while maintaining system-wide type safety.

---

## Next Steps

- **[QuickStart](./QuickStart.md)** - Get started with feature flags in 5 minutes
- **[Features](./Features.md)** - Define features using FeatureContainer
- **[Evaluation](./Evaluation.md)** - Understand flag evaluation mechanics
- **[Serialization](./Serialization.md)** - Export/import configurations as JSON
- **[Configuration](./Configuration.md)** - DSL reference for building configurations
- **[Context](./Context.md)** - Custom evaluation contexts
