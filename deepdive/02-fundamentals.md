# Engineering Deep Dive: Fundamentals

**Navigate**: [← Previous: Introduction](01-introduction.md) | [Next: Type System →](03-type-system.md)

---

## The Three Core Concepts

Konditional is built on three fundamental abstractions:

1. **Features** - What you're evaluating (the flag itself)
2. **Contexts** - When/where evaluation happens (the runtime state)
3. **Namespaces** - How features are organized (isolation boundaries)

Understanding these three concepts is essential to understanding everything else in the system.

---

## Part 1: Features

### What Is a Feature?

A **Feature** is a type-safe identifier for a configurable value. It's analogous to an enum constant, but with richer type information.

```kotlin
sealed interface Feature<S : EncodableValue<T>, T : Any, C : Context, M : Namespace> {
    val key: String
    val namespace: M
}
```

Let's break this down:

#### The Key
```kotlin
val key: String
```

The unique identifier for this feature. Used for:
- Serialization (JSON field name)
- Registry lookup
- Debugging and logging

With `FeatureContainer`, the key is automatically derived from the property name:
```kotlin
object AppFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val DARK_MODE by boolean(default = false)
    //  ^^^^^^^^^
    //  This becomes the key
}

println(AppFeatures.DARK_MODE.key)  // "DARK_MODE"
```

#### The Namespace
```kotlin
val namespace: M
```

Every feature belongs to exactly one namespace. This provides:
- **Isolation**: Different teams can't step on each other's flags
- **Organization**: Related features grouped together
- **Type safety**: Compiler enforces namespace boundaries

### Creating Features

There are two patterns for creating features:

#### Pattern 1: FeatureContainer (Recommended)
```kotlin
object PaymentFeatures : FeatureContainer<Namespace.Payments>(Namespace.Payments) {
    // Boolean feature
    val APPLE_PAY by boolean(default = false)

    // String feature
    val API_ENDPOINT by string(default = "https://api.prod.com")

    // Numeric features
    val MAX_RETRIES by int(default = 3)
    val TIMEOUT_SEC by double(default = 30.0)
}
```

**Benefits**:
- Automatic key derivation from property names
- Mixed types in one container
- IDE autocomplete
- Minimal boilerplate

**How it works** (we'll dive deeper in Chapter 3):
```kotlin
// The `by` keyword uses Kotlin's property delegation
val APPLE_PAY by boolean(default = false)
//            ^^
//            Property delegation - registers feature on first access
```

#### Pattern 2: Enum (Alternative)
```kotlin
enum class PaymentFeatures(override val key: String)
    : BooleanFeature<Context, Namespace.Payments> {

    APPLE_PAY("apple_pay"),
    GOOGLE_PAY("google_pay");

    override val namespace = Namespace.Payments
}
```

**Benefits**:
- Exhaustive when expressions
- Explicit key strings (stable across refactors)
- Familiar enum patterns

**Trade-offs**:
- All features must be same type
- More boilerplate
- Need separate enums for Boolean, String, Int, Double

### Features Are Just Identifiers

Important: **Features themselves don't hold configuration**. They're just type-safe identifiers.

The actual configuration lives in `FlagDefinition`:

```kotlin
// Feature: Just an identifier
val DARK_MODE: BooleanFeature<Context, Namespace.Global>

// FlagDefinition: Holds configuration (default, rules, etc.)
val definition: FlagDefinition<BoolEncodeable, Boolean, Context, Namespace.Global> =
    FlagDefinition(
        feature = DARK_MODE,
        defaultValue = false,
        values = listOf(/* rules */),
        isActive = true,
        salt = "v1"
    )
```

We'll explore `FlagDefinition` in detail in Chapter 4 (Evaluation Engine).

---

## Part 2: Contexts

### What Is a Context?

A **Context** represents the runtime state during flag evaluation. It answers: "Who is evaluating this feature, and under what conditions?"

```kotlin
interface Context {
    val locale: AppLocale
    val platform: Platform
    val appVersion: Version
    val stableId: StableId
}
```

Every evaluation requires a context:
```kotlin
val context = Context(
    locale = AppLocale.EN_US,
    platform = Platform.IOS,
    appVersion = Version.parse("2.1.0"),
    stableId = StableId.of("user-123")
)

val enabled = context.evaluate(AppFeatures.DARK_MODE)
//            ^^^^^^^
//            Context is the subject performing evaluation
```

### The Four Standard Fields

#### 1. locale: AppLocale
```kotlin
val locale: AppLocale
```

The user's language and region. Used for targeting specific markets:
```kotlin
rule {
    locales(AppLocale.EN_US, AppLocale.EN_CA)
} returns true
```

**Available locales**: `EN_US`, `EN_CA`, `EN_GB`, `FR_FR`, `DE_DE`, `ES_US`, `ES_ES`, `IT_IT`, `PT_BR`, `JA_JP`, `ZH_CN`, `KO_KR`, `HI_IN`, `AR_SA`, `RU_RU`, `NL_NL`, `SV_SE`, `PL_PL`, `TR_TR`, `TH_TH`

#### 2. platform: Platform
```kotlin
val platform: Platform
```

The runtime platform. Used for platform-specific features:
```kotlin
rule {
    platforms(Platform.IOS, Platform.ANDROID)
} returns mobileValue
```

**Available platforms**: `IOS`, `ANDROID`, `WEB`, `DESKTOP`, `SERVER`

#### 3. appVersion: Version
```kotlin
val appVersion: Version
```

Semantic version of the application. Used for version-based targeting:
```kotlin
rule {
    versions {
        min(2, 0, 0)  // >= 2.0.0
    }
} returns newFeatureValue
```

**Version format**: `major.minor.patch` (e.g., `Version(2, 1, 3)`)

#### 4. stableId: StableId
```kotlin
val stableId: StableId
```

A stable identifier for bucketing. Must be:
- **Persistent**: Same value across sessions
- **Unique**: Different for each user/entity
- **Minimum 32 hex characters**: Ensures sufficient entropy

```kotlin
// Good: User ID (persistent, unique)
StableId.of("a1b2c3d4e5f6...")  // 32+ hex chars

// Good: Device ID (persistent, unique)
StableId.of(deviceUUID.replace("-", ""))

// Bad: Session ID (not persistent)
// Bad: Random value (changes each time)
```

The stable ID is used for **deterministic bucketing** in rollouts. Same ID always gets same bucket.

### Creating Contexts

**Basic context**:
```kotlin
val context = Context(
    locale = AppLocale.EN_US,
    platform = Platform.IOS,
    appVersion = Version.parse("2.1.0"),
    stableId = StableId.of("user-abc123...")
)
```

**From user data**:
```kotlin
fun createContextForUser(user: User): Context = Context(
    locale = AppLocale.valueOf(user.language),
    platform = detectPlatform(),
    appVersion = BuildConfig.VERSION,
    stableId = StableId.of(user.id.toHexString())
)
```

### Custom Contexts

You can extend `Context` with additional fields for domain-specific targeting:

```kotlin
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    // Custom fields
    val organizationId: String,
    val subscriptionTier: SubscriptionTier,
    val seatCount: Int
) : Context

enum class SubscriptionTier {
    STARTER, PROFESSIONAL, ENTERPRISE
}
```

Then use in features:
```kotlin
val ADVANCED_ANALYTICS by boolean<EnterpriseContext>(default = false) {
//                                 ^^^^^^^^^^^^^^^^^^
//                                 Requires EnterpriseContext
    rule {
        extension {
            Evaluable.factory { ctx: EnterpriseContext ->
                ctx.subscriptionTier == SubscriptionTier.ENTERPRISE
            }
        }
    } returns true
}
```

**Compiler enforcement**:
```kotlin
val basicContext: Context = Context(...)
val enterpriseContext: EnterpriseContext = EnterpriseContext(...)

// ✓ Works
enterpriseContext.evaluate(EnterpriseFeatures.ADVANCED_ANALYTICS)

// ✗ Compile error: Context is not EnterpriseContext
basicContext.evaluate(EnterpriseFeatures.ADVANCED_ANALYTICS)
```

---

## Part 3: Namespaces

### What Is a Namespace?

A **Namespace** (also called a "Module" in some contexts) provides **isolation boundaries** for features. Each namespace has its own independent registry.

```kotlin
sealed class Namespace(val id: String) {
    data object Global : Namespace("global")
    data object Payments : Namespace("payments")
    data object Authentication : Namespace("authentication")
    data object Messaging : Namespace("messaging")
}
```

### Why Namespaces?

**Problem**: Without namespaces, all features share a single global registry:

```kotlin
// Without namespaces
object CoreFeatures {
    val DARK_MODE by boolean(default = false)
}

object PaymentFeatures {
    val DARK_MODE by boolean(default = false)  // ❌ Key collision!
}
```

**Solution**: Namespaces provide isolation:

```kotlin
// With namespaces
object CoreFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val DARK_MODE by boolean(default = false)  // Registered in Global
}

object PaymentFeatures : FeatureContainer<Namespace.Payments>(Namespace.Payments) {
    val DARK_MODE by boolean(default = false)  // Registered in Payments ✓
}
```

Each namespace maintains its own registry, so no collision.

### Namespace Architecture

Each namespace has:

#### 1. Registry
Stores feature flag definitions:
```kotlin
interface NamespaceRegistry {
    fun <S : EncodableValue<T>, T : Any, C : Context, M : Namespace>
        register(definition: FlagDefinition<S, T, C, M>)

    fun configuration(): Konfig
    fun load(konfig: Konfig)
}
```

#### 2. Configuration State
Current active configuration:
```kotlin
val currentConfig = Namespace.Payments.configuration()
```

#### 3. Load/Update Mechanism
Atomic configuration updates:
```kotlin
val newConfig = Konfig(/* ... */)
Namespace.Payments.load(newConfig)  // Atomic swap
```

### Creating Namespaces

**Built-in namespaces**:
```kotlin
Namespace.Global
Namespace.Payments
Namespace.Authentication
Namespace.Messaging
```

**Custom namespaces**:
```kotlin
sealed class Namespace(val id: String) {
    data object Global : Namespace("global")
    data object Payments : Namespace("payments")

    // Add your own
    data object MyDomain : Namespace("my_domain")
}
```

Then use with features:
```kotlin
object MyFeatures : FeatureContainer<Namespace.MyDomain>(Namespace.MyDomain) {
    val MY_FLAG by boolean(default = false)
}
```

### Namespace Benefits

#### 1. Team Isolation
```kotlin
// Payments team manages their namespace
object PaymentFeatures : FeatureContainer<Namespace.Payments>(Namespace.Payments) {
    val APPLE_PAY by boolean(default = false)
}

// Auth team manages theirs
object AuthFeatures : FeatureContainer<Namespace.Authentication>(Namespace.Authentication) {
    val SSO_ENABLED by boolean(default = false)
}
```

No cross-contamination. Each team deploys independently.

#### 2. Independent Deployment
```kotlin
// Update payments configuration without affecting auth
val paymentsConfig = loadPaymentsConfigFromServer()
Namespace.Payments.load(paymentsConfig)

// Auth configuration unchanged
```

#### 3. Reduced Blast Radius
```kotlin
// Serialization error in payments doesn't affect auth
when (val result = SnapshotSerializer.fromJson(paymentsJson)) {
    is ParseResult.Failure -> {
        // Payments config invalid, but auth still works
        logError("Payments config failed: ${result.error}")
    }
}
```

#### 4. Clear Ownership
```kotlin
// Namespace → Team mapping
Namespace.Payments → Payments Team
Namespace.Authentication → Auth Team
Namespace.Messaging → Messaging Team
```

---

## How The Three Work Together

### The Flow

```
1. Define Features (in a Namespace)
   ↓
2. Create Context (with runtime state)
   ↓
3. Evaluate Feature (Context evaluates Feature using Namespace's registry)
   ↓
4. Get Result (type-safe value)
```

### Complete Example

```kotlin
// 1. Define features in a namespace
object PaymentFeatures : FeatureContainer<Namespace.Payments>(Namespace.Payments) {
    val APPLE_PAY by boolean(default = false) {
        rule {
            platforms(Platform.IOS)
            versions { min(2, 0, 0) }
            rollout { 50.0 }
        } returns true
    }
}

// 2. Create context from runtime state
fun createContext(user: User): Context = Context(
    locale = AppLocale.valueOf(user.language),
    platform = Platform.IOS,
    appVersion = Version.parse("2.1.0"),
    stableId = StableId.of(user.id.toHexString())
)

// 3. Evaluate
fun processPayment(user: User) {
    val context = createContext(user)

    // Context evaluates Feature using Namespace.Payments registry
    val applePayEnabled = context.evaluate(PaymentFeatures.APPLE_PAY)
    //                    ^^^^^^^         ^^^^^^^^^^^^^^^^^^^^^^^^^^
    //                    Context         Feature (in Namespace.Payments)

    // 4. Get type-safe result
    if (applePayEnabled) {  // applePayEnabled: Boolean (guaranteed)
        showApplePayOption()
    }
}
```

### Under the Hood

Here's what happens during `context.evaluate(PaymentFeatures.APPLE_PAY)`:

```kotlin
// Simplified implementation
fun <S : EncodableValue<T>, T : Any, C : Context, M : Namespace>
    Context.evaluate(feature: Feature<S, T, C, M>): T {

    // 1. Look up FlagDefinition in feature's namespace
    val definition = feature.namespace.registry.lookup(feature.key)

    // 2. Evaluate the definition with this context
    return definition.evaluate(this)
}
```

We'll explore the evaluation process in detail in Chapter 4.

---

## Putting It All Together

### Key Relationships

```
Feature
  ↓ has a
Namespace ← maintains → Registry
                           ↓ stores
                       FlagDefinitions
                           ↓ evaluated with
                        Context
                           ↓ produces
                        Value (T)
```

### Type Flow

```kotlin
// Feature with type parameters
Feature<S, T, C, M>
   ↓
   S: EncodableValue<T>  - Serialization wrapper
   T: Any                - Actual value type (Boolean, String, Int, Double)
   C: Context            - Evaluation context type
   M: Namespace          - Namespace this feature belongs to

// FlagDefinition with same type parameters
FlagDefinition<S, T, C, M>(
    feature: Feature<S, T, C, M>,
    defaultValue: T,
    values: List<ConditionalValue<S, T, C, M>>,
    // ...
)

// Evaluation preserves types
Context.evaluate(Feature<S, T, C, M>): T
//               ^^^^^^^^^^^^^^^^^^^^^^  ↑
//               Input type             Output type matches T
```

The type system ensures end-to-end type safety. We'll dive deep into this in Chapter 3.

---

## Review: Core Concepts

### Features
- Type-safe identifiers for configurable values
- Bound to a specific namespace at compile time
- Don't hold configuration themselves (that's FlagDefinition's job)
- Created via `FeatureContainer` or enum pattern

### Contexts
- Represent runtime state during evaluation
- Standard fields: locale, platform, appVersion, stableId
- Can be extended with custom fields for business logic
- Type parameter constrains which features can be evaluated

### Namespaces
- Provide isolation boundaries between feature sets
- Each has independent registry
- Enable team ownership and independent deployment
- Reduce blast radius of configuration errors

### How They Relate
```kotlin
Context + Feature (in Namespace) → Value

// More precisely:
C.evaluate(Feature<S, T, C, M>) → T
  where M contains FlagDefinition<S, T, C, M>
```

---

## Next Steps

Now that you understand the fundamental building blocks, we can explore:

**Next chapter**: [Type System](03-type-system.md)
- How generic constraints enforce safety
- Why `EncodableValue` exists
- How property delegation works
- Type parameter variance and bounds

The type system is what makes compile-time guarantees possible. Let's see how.

---

**Navigate**: [← Previous: Introduction](01-introduction.md) | [Next: Type System →](03-type-system.md)
