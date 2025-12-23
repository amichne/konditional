# Namespace Isolation

Why namespaces prevent collisions, how they enforce separation, and when to use multiple namespaces.

---

## The Problem: Global Shared State

Without isolation, all flags share a single global registry:

```kotlin
// ✗ Global registry (all flags mixed together)
object GlobalFlags {
    val darkMode = flag("dark_mode")
    val paymentProcessing = flag("payment_processing")
    val analyticsEnabled = flag("analytics_enabled")
}
```

**Issues:**
1. **Name collisions** — Two teams pick the same flag name
2. **Coupled lifecycle** — Updating one domain's config affects others
3. **Blast radius** — Configuration error in one domain breaks all domains
4. **No governance** — Can't enforce team boundaries

---

## Konditional's Solution: Namespace Isolation

Each namespace has its own registry, configuration lifecycle, and independence guarantees:

```kotlin
sealed class AppDomain(id: String) : Namespace(id) {
    data object Auth : AppDomain("auth") {
        val socialLogin by boolean<Context>(default = false)
        val twoFactorAuth by boolean<Context>(default = true)
    }

    data object Payments : AppDomain("payments") {
        val applePay by boolean<Context>(default = false)
        val stripeIntegration by boolean<Context>(default = true)
    }
}
```

**Guarantees:**
1. **Separate registries** — `Auth` and `Payments` have independent `NamespaceRegistry` instances
2. **Independent lifecycle** — Load/rollback/disable operations are scoped to one namespace
3. **Failure isolation** — Parse error in `Auth` config doesn't affect `Payments`
4. **No name collisions** — `Auth.socialLogin` and `Payments.socialLogin` can coexist

---

## Mechanism 1: Separate NamespaceRegistry Instances

### Registry Storage

```kotlin
object NamespaceRegistryFactory {
    private val registries: MutableMap<String, NamespaceRegistry> = mutableMapOf()

    fun getOrCreate(namespaceId: String): NamespaceRegistry {
        return registries.getOrPut(namespaceId) {
            InMemoryNamespaceRegistry(namespaceId)
        }
    }
}
```

**Key insight:** Each namespace ID maps to a unique `NamespaceRegistry` instance.

### Namespace Construction

```kotlin
abstract class Namespace(val id: String) {
    val registry: NamespaceRegistry = NamespaceRegistryFactory.getOrCreate(id)
}
```

**Guarantee:** Two namespaces with different IDs have different registries (no shared state).

---

## Mechanism 2: Feature Key Scoping

Features are keyed by `(namespaceId, featureKey)`:

```kotlin
data class FeatureKey(val value: String) {
    companion object {
        fun from(namespaceId: String, propertyName: String): FeatureKey {
            return FeatureKey("feature::$namespaceId::$propertyName")
        }
    }
}
```

**Example:**
- `Auth.socialLogin` → `"feature::auth::socialLogin"`
- `Payments.socialLogin` → `"feature::payments::socialLogin"`

**Guarantee:** Features with the same property name but different namespaces have different keys (no collisions).

---

## Mechanism 3: Type-Bound Containers

Features are parameterized by their namespace type:

```kotlin
interface Feature<out T : Any, in C : Context, M : Namespace>
```

**Example:**

```kotlin
object Auth : Namespace("auth") {
    val socialLogin: Feature<Boolean, Context, Auth> by boolean(default = false)
}

object Payments : Namespace("payments") {
    val socialLogin: Feature<Boolean, Context, Payments> by boolean(default = false)
}
```

**Type safety:** `Auth.socialLogin` and `Payments.socialLogin` are different types (compiler prevents mixing them).

---

## Independent Lifecycle Operations

### Load

```kotlin
val authConfig = SnapshotSerializer.fromJson(authJson).value
val paymentConfig = SnapshotSerializer.fromJson(paymentJson).value

Auth.load(authConfig)      // Only affects Auth registry
Payments.load(paymentConfig)  // Only affects Payments registry
```

### Rollback

```kotlin
Auth.rollback(steps = 1)      // Only affects Auth
Payments.rollback(steps = 1)  // Only affects Payments
```

### Kill-Switch

```kotlin
Auth.disableAll()  // Only Auth evaluations return defaults
// Payments evaluations continue normally
```

---

## Failure Isolation

### Parse Error in One Namespace

```kotlin
val authJson = """{ "invalid": true }"""
val paymentJson = """{ "valid": "config" }"""

when (val result = Auth.fromJson(authJson)) {
    is ParseResult.Failure -> {
        // Auth parse failed
        logger.error("Auth config parse failed: ${result.error}")
        // Auth uses last-known-good config
    }
}

when (val result = Payments.fromJson(paymentJson)) {
    is ParseResult.Success -> {
        // Payments config loaded successfully
        // Payments is unaffected by Auth parse failure
    }
}
```

**Guarantee:** Parse failures in one namespace don't affect other namespaces.

---

## When to Use Multiple Namespaces

### Use Case 1: Team Ownership

```kotlin
sealed class TeamDomain(id: String) : Namespace(id) {
    data object Recommendations : TeamDomain("recommendations") {
        val COLLABORATIVE_FILTERING by boolean<Context>(default = true)
        val CONTENT_BASED by boolean<Context>(default = false)
    }

    data object Search : TeamDomain("search") {
        val FUZZY_MATCHING by boolean<Context>(default = true)
        val AUTOCOMPLETE by boolean<Context>(default = true)
    }
}
```

**Benefits:**
- Recommendations team owns `recommendations` namespace
- Search team owns `search` namespace
- No coordination required for config updates

### Use Case 2: Different Update Frequencies

```kotlin
object ExperimentFlags : Namespace("experiments") {
    // Updated frequently (daily experiments)
}

object InfrastructureFlags : Namespace("infrastructure") {
    // Updated rarely (circuit breakers, kill switches)
}
```

**Benefits:**
- Experiment config changes don't risk infrastructure stability
- Infrastructure config has higher review standards

### Use Case 3: Failure Isolation

```kotlin
object CriticalPath : Namespace("critical") {
    val PAYMENT_ENABLED by boolean<Context>(default = true)
}

object Analytics : Namespace("analytics") {
    val TRACKING_ENABLED by boolean<Context>(default = false)
}
```

**Benefits:**
- Analytics config error doesn't affect payment processing
- Critical path config has higher SLA

---

## Anti-Pattern: Over-Segmentation

**Don't:**

```kotlin
object AuthSocialLogin : Namespace("auth-social-login")
object AuthTwoFactor : Namespace("auth-two-factor")
object AuthPasswordReset : Namespace("auth-password-reset")
```

**Issues:**
- Too many namespaces increase complexity
- No real benefit to isolation (all owned by Auth team)

**Better:**

```kotlin
object Auth : Namespace("auth") {
    val socialLogin by boolean<Context>(default = false)
    val twoFactorAuth by boolean<Context>(default = true)
    val passwordReset by boolean<Context>(default = true)
}
```

---

## Namespace Governance Patterns

### Pattern 1: Sealed Hierarchy

```kotlin
sealed class AppDomain(id: String) : Namespace(id) {
    data object Auth : AppDomain("auth")
    data object Payments : AppDomain("payments")
    data object Analytics : AppDomain("analytics")
}
```

**Benefits:**
- All namespaces are discoverable (sealed = exhaustive)
- Compiler prevents unknown namespaces

### Pattern 2: Team Ownership via Package Structure

```
com.example.teams.auth.AuthFeatures : Namespace("auth")
com.example.teams.payments.PaymentFeatures : Namespace("payments")
com.example.teams.analytics.AnalyticsFeatures : Namespace("analytics")
```

**Benefits:**
- Package structure mirrors team structure
- Code ownership via CODEOWNERS file

---

## Formal Properties

| Property | Mechanism | Guarantee |
|----------|-----------|-----------|
| **No name collisions** | Feature keys scoped by namespace ID | `Auth.socialLogin` ≠ `Payments.socialLogin` |
| **Separate state** | Different `NamespaceRegistry` instances | Updating Auth doesn't affect Payments |
| **Independent lifecycle** | Operations scoped to namespace | `Auth.load(...)` only affects Auth |
| **Failure isolation** | Parse errors scoped to namespace | Auth parse failure doesn't break Payments |
| **Type safety** | Feature parameterized by namespace type | Compiler prevents mixing namespaces |

---

## Next Steps

- [Fundamentals: Core Primitives](/fundamentals/core-primitives) — Namespace primitive
- [Advanced: Multiple Namespaces](/advanced/multiple-namespaces) — Practical patterns
- [API Reference: Namespace Operations](/api-reference/namespace-operations) — Lifecycle API
