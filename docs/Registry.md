# Registry: Taxonomy-Based Flag Organization

This document explains Konditional's registry system, which provides compile-time and runtime isolation for feature flags through taxonomies.

---

## Overview

Konditional organizes feature flags using **taxonomies** - isolated namespaces that provide:

- **Compile-time isolation**: Features are type-bound to their taxonomy
- **Runtime isolation**: Each taxonomy has its own flag registry
- **Governance**: All taxonomies enumerated in one sealed hierarchy
- **Direct operations**: Taxonomies implement `ModuleRegistry`, eliminating `.registry` access

```kotlin
// Features are bound to specific taxonomies
object PaymentFeatures : FeatureContainer<Taxonomy.Domain.Payments>(
    Taxonomy.Domain.Payments
) {
    val APPLE_PAY by boolean(default = false)
}

// Evaluate using the bound taxonomy
val isEnabled = context.evaluate(PaymentFeatures.APPLE_PAY)

// Or query the taxonomy directly
val definition = Taxonomy.Domain.Payments.featureFlag(PaymentFeatures.APPLE_PAY)
```

---

## Taxonomy System

### What Is a Taxonomy?

A `Taxonomy` is a sealed class hierarchy that defines organizational boundaries for feature flags.

**Key characteristics:**

- **Unique ID**: Each taxonomy has a string identifier
- **Isolated registry**: Each taxonomy maintains its own registry instance
- **ModuleRegistry operations**: Taxonomies provide direct access to registry methods
- **Sealed hierarchy**: All taxonomies must be defined in the sealed class

### Taxonomy Types

Konditional provides two taxonomy categories:

#### Global Taxonomy

The `Taxonomy.Global` taxonomy contains shared flags accessible to all teams:

```kotlin
data object Global : Taxonomy("global")
```

**Use for:**
- System-wide kill switches
- Maintenance mode flags
- Cross-cutting feature toggles
- Common infrastructure flags

**Example:**

```kotlin
object CoreFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val KILL_SWITCH by boolean(default = false)
    val MAINTENANCE_MODE by boolean(default = false)
    val DEBUG_LOGGING by boolean(default = false)
}
```

#### Domain Taxonomies

Domain taxonomies provide isolated namespaces for functional areas:

```kotlin
sealed class Domain(id: String) : Taxonomy(id) {
    data object Authentication : Domain("auth")
    data object Payments : Domain("payments")
    data object Messaging : Domain("messaging")
    data object Search : Domain("search")
    data object Recommendations : Domain("recommendations")
}
```

**Each domain taxonomy provides:**

- Independent registry instance (runtime isolation)
- Type-bound features (compile-time isolation)
- Independent serialization/deployment
- No cross-taxonomy flag access

**Example:**

```kotlin
object AuthFeatures : FeatureContainer<Taxonomy.Domain.Authentication>(
    Taxonomy.Domain.Authentication
) {
    val SOCIAL_LOGIN by boolean(default = false)
    val TWO_FACTOR_AUTH by boolean(default = true)
    val BIOMETRIC_AUTH by boolean(default = false)
}

object PaymentFeatures : FeatureContainer<Taxonomy.Domain.Payments>(
    Taxonomy.Domain.Payments
) {
    val APPLE_PAY by boolean(default = false)
    val GOOGLE_PAY by boolean(default = false)
    val CRYPTO_PAYMENTS by boolean(default = false)
}
```

### Adding New Taxonomies

To add a new domain taxonomy, add an object to the `Domain` sealed class:

```kotlin
sealed class Domain(id: String) : Taxonomy(id) {
    // ... existing domains ...

    data object Analytics : Domain("analytics")
    data object Notifications : Domain("notifications")
}
```

**The sealed hierarchy ensures:**

- No taxonomy ID collisions at compile time
- Exhaustive when-expressions
- IDE autocomplete for all taxonomies
- Central visibility of all modules

---

## ModuleRegistry Interface

`ModuleRegistry` defines the contract for managing feature flag configurations:

```kotlin
interface ModuleRegistry {
    fun load(config: Konfig)
    fun konfig(): Konfig
    fun <S, T, C, M> featureFlag(key: Feature<S, T, C, M>): FlagDefinition<S, T, C, M>?
    fun allFlags(): Map<Feature<*, *, *, *>, FlagDefinition<*, *, *, *>>
}
```

### Core Operations

#### Loading Configuration

Load a complete flag configuration snapshot:

```kotlin
// Create a Konfig (typically from JSON deserialization)
val konfig = TaxonomySnapshotSerializer.deserialize<Taxonomy.Domain.Payments>(jsonString)

// Load into taxonomy's registry
when (konfig) {
    is ParseResult.Success -> Taxonomy.Domain.Payments.load(konfig.value)
    is ParseResult.Failure -> logger.error("Failed to load: ${konfig.error}")
}
```

**Properties:**

- **Atomic**: Entire configuration replaces current state in one operation
- **Thread-safe**: Backed by `AtomicReference` for lock-free updates
- **Consistent**: Readers see complete old OR new config, never partial state

#### Retrieving Current State

Get the current configuration snapshot:

```kotlin
val currentConfig = Taxonomy.Domain.Payments.konfig()

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
val definition = Taxonomy.Domain.Payments.featureFlag(PaymentFeatures.APPLE_PAY)

if (definition != null) {
    println("Default: ${definition.defaultValue}")
    println("Active: ${definition.isActive}")
    println("Rules: ${definition.values.size}")
}
```

**Returns:** `FlagDefinition<S, T, C, M>?` - null if flag not found

#### Querying All Flags

Retrieve all flags from a taxonomy:

```kotlin
val allFlags = Taxonomy.Domain.Payments.allFlags()

allFlags.forEach { (feature, definition) ->
    println("${feature.key}: ${definition.defaultValue}")
}
```

### Factory Function

Create new registry instances for testing:

```kotlin
companion object {
    operator fun invoke(konfig: Konfig = Konfig(emptyMap())): ModuleRegistry
}
```

**Usage:**

```kotlin
@Test
fun `test feature behavior`() {
    // Create isolated registry for testing
    val testRegistry = ModuleRegistry()

    // Note: Direct registry usage is for advanced cases
    // Prefer using Taxonomy instances in production
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
val konfig = Taxonomy.Domain.Payments.konfig()

// Serialize to JSON
val json = TaxonomySnapshotSerializer.serialize(konfig)

// Save to file or send over network
File("payment-config.json").writeText(json)
```

#### Importing Configuration

```kotlin
// Read from file
val json = File("payment-config.json").readText()

// Deserialize
val result = TaxonomySnapshotSerializer.deserialize<Taxonomy.Domain.Payments>(json)

when (result) {
    is ParseResult.Success -> {
        // Load into registry
        Taxonomy.Domain.Payments.load(result.value)
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
val snapshot = Taxonomy.Domain.Payments.konfig()

// Evaluate multiple flags against same snapshot
// (even if registry is updated concurrently)
val applePayDef = snapshot.flags[PaymentFeatures.APPLE_PAY]
val googlePayDef = snapshot.flags[PaymentFeatures.GOOGLE_PAY]

val applePayEnabled = applePayDef?.evaluate(context)
val googlePayEnabled = googlePayDef?.evaluate(context)
```

---

## Taxonomy-Based Isolation

### Compile-Time Isolation

Features are type-bound to their taxonomy, preventing cross-taxonomy usage:

```kotlin
object AuthFeatures : FeatureContainer<Taxonomy.Domain.Authentication>(
    Taxonomy.Domain.Authentication
) {
    val SOCIAL_LOGIN by boolean(default = false)
}

object PaymentFeatures : FeatureContainer<Taxonomy.Domain.Payments>(
    Taxonomy.Domain.Payments
) {
    val APPLE_PAY by boolean(default = false)
}

// ✓ Correct: Feature bound to Authentication taxonomy
val authEnabled = context.evaluate(AuthFeatures.SOCIAL_LOGIN)

// ✗ Won't compile: SOCIAL_LOGIN belongs to Authentication, not Payments
Taxonomy.Domain.Payments.featureFlag(AuthFeatures.SOCIAL_LOGIN)  // Type error
```

**Benefits:**

- Impossible to query flags from wrong taxonomy
- Refactoring is safe (types guide changes)
- Clear ownership boundaries
- No runtime taxonomy checks needed

### Runtime Isolation

Each taxonomy has its own `ModuleRegistry` instance:

```kotlin
// Authentication taxonomy has its own registry
Taxonomy.Domain.Authentication.load(authConfig)

// Payments taxonomy has its own registry
Taxonomy.Domain.Payments.load(paymentsConfig)

// Registries are independent - no shared state
```

**Benefits:**

- Independent deployment of taxonomy configurations
- No key collisions between taxonomies
- Team autonomy (each team owns their taxonomy)
- Isolated testing (load different configs per taxonomy)

### Governance Through Sealed Hierarchy

All taxonomies must be defined in the sealed class:

```kotlin
sealed class Domain(id: String) : Taxonomy(id) {
    data object Authentication : Domain("auth")
    data object Payments : Domain("payments")
    // All domains visible here
}
```

**Benefits:**

- Central registry of all taxonomies
- No duplicate taxonomy IDs
- Exhaustive when-expressions
- Clear organizational structure

---

## Thread Safety Guarantees

**Lock-free reads:**

```kotlin
// Multiple threads can read simultaneously without contention
val def1 = taxonomy.featureFlag(feature)  // Thread 1
val def2 = taxonomy.featureFlag(feature)  // Thread 2 (concurrent)
```

**Atomic updates:**

```kotlin
// Update atomically replaces entire config
taxonomy.load(newKonfig)  // All flags update together
```

**Read-write safety:**

```kotlin
// Thread A: Reading
val definition = taxonomy.featureFlag(feature)
val result = definition?.evaluate(context)

// Thread B: Updating (concurrent)
taxonomy.load(newKonfig)

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

## Registry Operations Through Taxonomy

Since `Taxonomy` implements `ModuleRegistry` via delegation, you can call registry methods directly:

### Loading Configuration

```kotlin
// Old style (verbose)
Taxonomy.Domain.Payments.registry.load(konfig)

// New style (direct)
Taxonomy.Domain.Payments.load(konfig)
```

### Querying State

```kotlin
// Get current snapshot
val snapshot = Taxonomy.Domain.Payments.konfig()

// Query specific flag
val definition = Taxonomy.Domain.Payments.featureFlag(PaymentFeatures.APPLE_PAY)

// Get all flags
val allFlags = Taxonomy.Domain.Payments.allFlags()
```

### Example: Loading from Remote Config

```kotlin
suspend fun loadRemoteConfig(taxonomy: Taxonomy.Domain.Payments) {
    try {
        // Fetch from remote
        val json = httpClient.get("https://config.example.com/payments")

        // Deserialize
        val result = TaxonomySnapshotSerializer.deserialize<Taxonomy.Domain.Payments>(json)

        when (result) {
            is ParseResult.Success -> {
                // Load directly into taxonomy
                taxonomy.load(result.value)
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

## Choosing the Right Taxonomy

### Use `Taxonomy.Global` for:

**System-wide flags:**
```kotlin
object SystemFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val KILL_SWITCH by boolean(default = false)
    val MAINTENANCE_MODE by boolean(default = false)
    val RATE_LIMITING by boolean(default = true)
}
```

**Infrastructure flags:**
```kotlin
object InfraFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val NEW_DATABASE by boolean(default = false)
    val CIRCUIT_BREAKER by boolean(default = true)
    val DEBUG_LOGGING by boolean(default = false)
}
```

**Cross-cutting concerns:**
```kotlin
object MonitoringFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val METRICS_ENABLED by boolean(default = true)
    val TRACING_ENABLED by boolean(default = false)
    val PROFILING_ENABLED by boolean(default = false)
}
```

### Use `Taxonomy.Domain.*` for:

**Team-owned features:**
```kotlin
// Authentication team owns this taxonomy
object AuthFeatures : FeatureContainer<Taxonomy.Domain.Authentication>(
    Taxonomy.Domain.Authentication
) {
    val SOCIAL_LOGIN by boolean(default = false)
    val TWO_FACTOR_AUTH by boolean(default = true)
    val PASSWORDLESS_LOGIN by boolean(default = false)
}
```

**Domain-specific experiments:**
```kotlin
// Payments team owns this taxonomy
object PaymentExperiments : FeatureContainer<Taxonomy.Domain.Payments>(
    Taxonomy.Domain.Payments
) {
    val NEW_CHECKOUT_FLOW by boolean(default = false)
    val ONE_CLICK_PURCHASE by boolean(default = false)
    val SAVED_PAYMENT_METHODS by boolean(default = true)
}
```

**Isolated deployments:**
```kotlin
// Search team can deploy independently
object SearchFeatures : FeatureContainer<Taxonomy.Domain.Search>(
    Taxonomy.Domain.Search
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
object PaymentFeatures : FeatureContainer<Taxonomy.Domain.Payments>(
    Taxonomy.Domain.Payments
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

### Do: Use Taxonomy Instances Directly

```kotlin
// ✓ Good: Taxonomy implements ModuleRegistry
Taxonomy.Domain.Payments.load(konfig)
val snapshot = Taxonomy.Domain.Payments.konfig()

// ✗ Verbose: Accessing internal registry
Taxonomy.Domain.Payments.registry.load(konfig)
```

### Do: Organize by Domain

```kotlin
// ✓ Good: Clear ownership
object AuthFeatures : FeatureContainer<Taxonomy.Domain.Authentication>(
    Taxonomy.Domain.Authentication
) { ... }

object PaymentFeatures : FeatureContainer<Taxonomy.Domain.Payments>(
    Taxonomy.Domain.Payments
) { ... }
```

### Do: Use Global Sparingly

```kotlin
// ✓ Good: True system-wide concern
object SystemFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val KILL_SWITCH by boolean(default = false)
}

// ✗ Bad: Team feature in Global
object FeatureFlags : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
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
    // (Advanced usage - prefer using Taxonomy in production)
}
```

### Don't: Mix Taxonomies

```kotlin
// ✗ Bad: Won't compile - type mismatch
Taxonomy.Domain.Payments.featureFlag(AuthFeatures.SOCIAL_LOGIN)
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

Here's a complete example showing taxonomy-based organization:

```kotlin
// 1. Define domain-specific features
object PaymentFeatures : FeatureContainer<Taxonomy.Domain.Payments>(
    Taxonomy.Domain.Payments
) {
    val APPLE_PAY by boolean(default = false) {
        rule {
            platforms(Platform.IOS)
            versions { min(2, 0, 0) }
        } implies true
    }

    val GOOGLE_PAY by boolean(default = false) {
        rule {
            platforms(Platform.ANDROID)
        } implies true
    }

    val CRYPTO_PAYMENTS by boolean(default = false) {
        rule {
            rollout = Rollout.of(10.0)  // 10% rollout
        } implies true
    }
}

object AuthFeatures : FeatureContainer<Taxonomy.Domain.Authentication>(
    Taxonomy.Domain.Authentication
) {
    val SOCIAL_LOGIN by boolean(default = false)
    val TWO_FACTOR_AUTH by boolean(default = true)
    val BIOMETRIC_AUTH by boolean(default = false)
}

// 2. Load configurations independently
suspend fun loadConfigurations() {
    // Load payments config
    val paymentsJson = httpClient.get("https://config.example.com/payments")
    val paymentsResult = TaxonomySnapshotSerializer
        .deserialize<Taxonomy.Domain.Payments>(paymentsJson)

    when (paymentsResult) {
        is ParseResult.Success -> {
            Taxonomy.Domain.Payments.load(paymentsResult.value)
        }
        is ParseResult.Failure -> {
            logger.error("Failed to load payments config")
        }
    }

    // Load auth config independently
    val authJson = httpClient.get("https://config.example.com/auth")
    val authResult = TaxonomySnapshotSerializer
        .deserialize<Taxonomy.Domain.Authentication>(authJson)

    when (authResult) {
        is ParseResult.Success -> {
            Taxonomy.Domain.Authentication.load(authResult.value)
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
    val paymentsSnapshot = Taxonomy.Domain.Payments.konfig()
    val paymentsJson = TaxonomySnapshotSerializer.serialize(paymentsSnapshot)
    File("payments.json").writeText(paymentsJson)

    // Export auth config
    val authSnapshot = Taxonomy.Domain.Authentication.konfig()
    val authJson = TaxonomySnapshotSerializer.serialize(authSnapshot)
    File("auth.json").writeText(authJson)
}

// 5. Inspect registry state
fun debugRegistries() {
    // Inspect payments flags
    println("=== Payments Flags ===")
    Taxonomy.Domain.Payments.allFlags().forEach { (feature, definition) ->
        println("${feature.key}: active=${definition.isActive}, default=${definition.defaultValue}")
    }

    // Inspect auth flags
    println("=== Auth Flags ===")
    Taxonomy.Domain.Authentication.allFlags().forEach { (feature, definition) ->
        println("${feature.key}: active=${definition.isActive}, default=${definition.defaultValue}")
    }
}
```

---

## Summary

| Concept              | Purpose                                                  |
|----------------------|----------------------------------------------------------|
| **Taxonomy**         | Organizational namespace with isolated registry          |
| **Taxonomy.Global**  | System-wide flags (kill switches, maintenance)           |
| **Taxonomy.Domain**  | Team-owned flags (domain-specific features)              |
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
