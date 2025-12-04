# Migration Guide

Switch from string-based feature flags to compile-time safety. This guide maps concepts and shows adoption patterns.

---

## Concept Mapping

### String-Based Systems → Konditional

| Common Pattern (LaunchDarkly/Statsig/Custom)  | Konditional                       | Key Difference                          |
|-----------------------------------------------|-----------------------------------|-----------------------------------------|
| `getFlag("flag-name")` or `client.boolVariation("flag", false)` | `feature { Features.FLAG }` | Compile-time property vs runtime string |
| Context with `Map<String, Any>` attributes    | `Context` data class              | Typed fields vs HashMap                 |
| Rules in dashboard or config files            | `rule { }` DSL (code)             | Version-controlled in code              |
| Percentage rollouts                           | `rollout { 50.0 }`                | Same concept, local computation         |
| Segments/audiences/conditions                 | Custom `extension { }` logic      | Type-safe predicates                    |
| Projects/environments/namespaces              | `Namespace`                       | Compile-time isolated                   |
| Flag variations/treatments                    | `rule {...} returns value`        | Type-safe values                        |

### Specific Service Mappings

**LaunchDarkly:**
- `LDClient.boolVariation()` → `feature { Feature }`
- `LDContext` → `Context` data class
- Segments → `extension { Evaluable.factory { ... } }`

**Statsig:**
- `statsig.getConfig()` → `feature { Feature }`
- Dynamic Config → String/Int/Double features with rules
- Feature Gates → Boolean features

**Custom String-Based:**
- `featureFlags["flag"]` → `feature { Features.FLAG }`
- String keys → Property delegation
- Type casting → Compiler-enforced types

---

## Code Comparison

### Boolean Flag

**String-Based (LaunchDarkly example):**

```kotlin
val client = LDClient(sdkKey)
val context = LDContext.builder("user-123")
    .set("platform", "ios")
    .build()

val enabled = client.boolVariation("dark-mode", context, false)
```

**String-Based (Statsig example):**

```kotlin
val statsig = Statsig.initialize(sdkKey)
val user = StatsigUser("user-123")
    .setCustom(mapOf("platform" to "ios"))

val enabled = statsig.checkGate(user, "dark_mode")
```

**Konditional:**

```kotlin
object Features : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val DARK_MODE by boolean(default = false)
}

val context = Context(
    locale = AppLocale.UNITED_STATES,
    platform = Platform.IOS,
    appVersion = Version.parse("1.0.0"),
    stableId = StableId.of("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6")
)

val enabled = feature { Features.DARK_MODE }
```

**Key differences:**

- No SDK key or network needed (offline-first)
- StableId requires hex format (deterministic bucketing)
- Property access with IDE autocomplete
- Type safety: compiler knows `enabled` is `Boolean`

### String Configuration

**String-Based (Custom implementation):**

```kotlin
val config = ConfigManager.getInstance()
val endpoint = when (config.getString("api-endpoint-variant")) {
    "ios" -> "https://api-ios.example.com"
    "android" -> "https://api-android.example.com"
    else -> "https://api.example.com"
}
```

**String-Based (Statsig Dynamic Config):**

```kotlin
val config = statsig.getConfig(user, "api_config")
val endpoint = config.getString("endpoint", "https://api.example.com")
```

**Konditional:**

```kotlin
object Config : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val API_ENDPOINT by string(default = "https://api.example.com") {
        rule { platforms(Platform.IOS) } returns "https://api-ios.example.com"
        rule { platforms(Platform.ANDROID) } returns "https://api-android.example.com"
    }
}

val endpoint = feature { Config.API_ENDPOINT }  // Type: String, never null
```

**Key differences:**

- No treatment-to-value mapping (direct type-safe values)
- Rules defined in code (version-controlled)
- Returns actual config value, not treatment name

### Custom Attributes / Context Fields

**String-Based (LaunchDarkly example):**

```kotlin
val context = LDContext.builder("user-123")
    .set("tier", "enterprise")  // String value, no validation
    .set("organization", "acme-corp")
    .build()

// In dashboard: target users where tier == "enterprise"
```

**String-Based (Statsig example):**

```kotlin
val user = StatsigUser("user-123")
    .setCustom(mapOf(
        "tier" to "enterprise",  // String value, no validation
        "organization" to "acme-corp"
    ))
```

**Konditional:**

```kotlin
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val subscriptionTier: SubscriptionTier,  // Typed, not string
    val organizationId: String
) : Context

object PremiumFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val ADVANCED_ANALYTICS by boolean(default = false) {
        rule {
            extension {
                Evaluable.factory { ctx: EnterpriseContext ->
                    ctx.subscriptionTier == SubscriptionTier.ENTERPRISE
                }
            }
        } returns true
    }
}

val ctx = EnterpriseContext(..., subscriptionTier = SubscriptionTier.ENTERPRISE, ...)
val enabled = feature { PremiumFeatures.ADVANCED_ANALYTICS }
```

**Key differences:**

- Type-safe enum `SubscriptionTier` vs string "enterprise"
- Custom fields defined as properties (compiler-validated)
- Business logic in code (testable, refactorable)

---

## Adoption Patterns

### Pattern 1: Gradual (Recommended)

Run Konditional alongside your existing system, migrate flag-by-flag.

**Steps:**

1. Add Konditional dependency
2. Define one flag in Konditional (mirror existing config)
3. Evaluate both systems, log differences
4. Once confident, switch to Konditional for that flag
5. Repeat for remaining flags
6. Remove old system dependency

**Example dual evaluation:**

```kotlin
// Wrapper to compare both systems
fun isEnabled(flagName: String, context: Context): Boolean {
    // Existing system (LaunchDarkly example)
    val oldResult = featureFlagClient.getBoolean(flagName, false)

    // Konditional
    val newResult = when (flagName) {
        "dark_mode" -> feature { Features.DARK_MODE }
        else -> null
    }

    if (newResult != null && oldResult != newResult) {
        logger.warn("Mismatch for $flagName: Old=$oldResult, New=$newResult")
    }

    return oldResult  // Use old system until validated
}
```

### Pattern 2: Big-Bang (Faster, Higher Risk)

Migrate all flags at once.

**Steps:**

1. Export all existing flags (from dashboard, config files, or database)
2. Define equivalent Konditional features
3. Test thoroughly in staging
4. Deploy and monitor closely
5. Roll back if issues arise

**Use when:** You have comprehensive tests and can tolerate brief outages.

### Pattern 3: New Features Only

Keep existing flags in your current system, use Konditional for new flags.

**Steps:**

1. Add Konditional for new features
2. Gradually migrate old flags as time permits
3. Eventually deprecate old system

**Use when:** Migration isn't urgent, want to learn Konditional gradually.

---

## Common Migration Challenges

### Challenge 1: StableId Format

**String-based systems:** User IDs can be any string (`"user-123"`, `"alice@example.com"`)

**Konditional:** `StableId` must be valid hexadecimal (32+ characters)

**Solution:** Hash your existing IDs:

```kotlin
fun userIdToStableId(userId: String): StableId {
    val hash = MessageDigest.getInstance("SHA-256")
        .digest(userId.toByteArray())
        .joinToString("") { "%02x".format(it) }
    return StableId.of(hash)
}

val stableId = userIdToStableId("user-123")
```

### Challenge 2: Dynamic Attributes

**String-based systems:** Attributes set at runtime (`context.set("key", value)` or `Map<String, Any>`)

**Konditional:** Context fields must be defined at compile time

**Solution:** Create custom context with all possible fields:

```kotlin
data class AppContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val subscriptionTier: SubscriptionTier?,  // Nullable if optional
    val betaTester: Boolean,
    val organizationId: String?
) : Context
```

### Challenge 3: Remote Configuration

**String-based systems:** Flags configured via dashboard/API, updated instantly

**Konditional:** Flags defined in code, rules updated via UI or JSON

**Solution:** Use UI with RBAC or JSON serialization for remote updates:

```kotlin
// Define flags in code with defaults
object Features : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val DARK_MODE by boolean(default = false)
}

// Load remote rules (doesn't change code, just rule configuration)
val remoteJson = fetchFromServer("/flags.json")
when (val result = SnapshotSerializer.fromJson(remoteJson)) {
    is ParseResult.Success -> Namespace.Global.load(result.value)
    is ParseResult.Failure -> logger.error("Failed to load config")
}
```

### Challenge 4: Rollout Redistribution

**Issue:** Users in 50% rollout in your old system won't match 50% in Konditional (different bucketing algorithms)

**Solution:** Accept redistribution or use salt to align:

- **Accept:** Users may see different experience temporarily (most teams do this)
- **Align:** Adjust Konditional salt until distribution matches (trial-and-error, not recommended)

Most teams accept redistribution since rollouts are temporary anyway.

---

## Why Migrate?

### 1. Eliminate Runtime Errors

**String-based:**

```kotlin
getFlag("dark-mod")  // Typo! Returns null or default silently
```

**Konditional:**

```kotlin
feature { Features.DARK_MOD }  // Compile error: unresolved reference
```

### 2. Reduce Infrastructure Costs

**SaaS systems (LaunchDarkly/Statsig):** Monthly fees scale with MAU/seat count

**Konditional:** Zero infrastructure cost (runs locally)

### 3. Improve Performance

**String-based:** Network latency or cache lookup overhead, type casting

**Konditional:** Zero network calls, O(n) local evaluation where n < 10, zero allocation

### 4. Version Control Everything

**Dashboard-based systems:** Flag rules live in UI (audit log separate from code)

**Konditional:** Flags defined in code (Git history + UI with RBAC for rule updates)

### 5. Type Safety

**String-based:** `getInt("flag")` vs `getString("flag")` — must remember type, can typo

**Konditional:** Compiler knows the type, IDE autocomplete works, typos impossible

---

## Decision Matrix

| Factor                   | Stick with Current System                              | Migrate to Konditional             |
|--------------------------|--------------------------------------------------------|------------------------------------|
| **Budget**               | SaaS cost not a concern                                | Want to eliminate SaaS fees        |
| **Tech stack**           | Multi-language (Java, Go, Python, Ruby, etc.)          | Kotlin/JVM only                    |
| **Type safety priority** | Low (runtime errors acceptable)                        | High (critical for correctness)    |
| **Offline support**      | Not needed (always connected)                          | Essential (mobile, edge computing) |
| **Integration effort**   | Already integrated, working well                       | Willing to invest migration time   |
| **Control & Compliance** | Prefer SaaS management                                 | Need self-hosted with RBAC control |

---

## Next Steps

**Ready to migrate?** Start with [Getting Started](01-getting-started.md) to run your first Konditional flag.

**Need feature parity?** See [Targeting & Rollouts](04-targeting-rollouts.md) for advanced rules matching most feature flag systems.

**Want to understand the model?** See [Core Concepts](03-core-concepts.md) for deep dive into Features, Context, and
Namespaces.
