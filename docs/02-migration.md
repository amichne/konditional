# Migration Guide

Switch from LaunchDarkly/Split with confidence. This guide maps concepts and shows adoption patterns.

---

## Concept Mapping

### LaunchDarkly → Konditional

| LaunchDarkly                          | Konditional                       | Key Difference                          |
|---------------------------------------|-----------------------------------|-----------------------------------------|
| `client.boolVariation("flag", false)` | `context.evaluate(Features.FLAG)` | Compile-time property vs runtime string |
| `LDContext` with attributes           | `Context` data class              | Typed fields vs HashMap                 |
| Targeting rules (dashboard)           | `rule { }` DSL (code)             | Version-controlled vs UI-only           |
| Rollout percentage                    | `rollout { 50.0 }`                | Same concept, local computation         |
| Segments                              | Custom `extension { }` logic      | Type-safe predicates                    |
| Projects/Environments                 | `Namespace`                       | Compile-time isolated                   |
| Flag variations                       | `rule {...} returns value`        | Type-safe values                        |

### Split → Konditional

| Split                         | Konditional                                 | Key Difference                          |
|-------------------------------|---------------------------------------------|-----------------------------------------|
| `client.getTreatment("flag")` | `context.evaluate(Features.FLAG)`           | Compile-time property vs runtime string |
| `SplitClient` with attributes | `Context` data class                        | Typed fields vs key-value pairs         |
| Split definitions (dashboard) | `rule { }` DSL (code)                       | Version-controlled in code              |
| Traffic allocation            | `rollout { }`                               | Local SHA-256 bucketing                 |
| Conditions/matchers           | `platforms()`, `locales()`, `extension { }` | Type-safe targeting                     |
| Treatments                    | Rule return values                          | Type-safe (not just strings)            |

---

## Code Comparison

### Boolean Flag

**LaunchDarkly:**

```kotlin
val client = LDClient(sdkKey)
val context = LDContext.builder("user-123")
    .set("platform", "ios")
    .build()

val enabled = client.boolVariation("dark-mode", context, false)
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

val enabled = context.evaluateOrDefault(Features.DARK_MODE, false)
```

**Key differences:**

- No SDK key needed (offline-first)
- StableId requires hex format (deterministic bucketing)
- Property access with IDE autocomplete
- Type safety: compiler knows `enabled` is `Boolean`

### String Configuration

**Split:**

```kotlin
val client = SplitFactoryBuilder()
    .setApiKey(apiKey)
    .build()
    .client()

val treatment = client.getTreatment("api-endpoint", emptyMap())
val endpoint = when (treatment) {
    "ios" -> "https://api-ios.example.com"
    "android" -> "https://api-android.example.com"
    else -> "https://api.example.com"
}
```

**Konditional:**

```kotlin
object Config : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val API_ENDPOINT by string(default = "https://api.example.com") {
        rule { platforms(Platform.IOS) } returns "https://api-ios.example.com"
        rule { platforms(Platform.ANDROID) } returns "https://api-android.example.com"
    }
}

val endpoint = context.evaluate(Config.API_ENDPOINT)  // Type: String, never null
```

**Key differences:**

- No treatment-to-value mapping (direct type-safe values)
- Rules defined in code (version-controlled)
- Returns actual config value, not treatment name

### Custom Attributes / Context Fields

**LaunchDarkly:**

```kotlin
val context = LDContext.builder("user-123")
    .set("tier", "enterprise")
    .set("organization", "acme-corp")
    .build()

// In LaunchDarkly dashboard: target users where tier == "enterprise"
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
val enabled = ctx.evaluate(PremiumFeatures.ADVANCED_ANALYTICS)
```

**Key differences:**

- Type-safe enum `SubscriptionTier` vs string "enterprise"
- Custom fields defined as properties (compiler-validated)
- Business logic in code (testable, refactorable)

---

## Adoption Patterns

### Pattern 1: Gradual (Recommended)

Run Konditional alongside LaunchDarkly/Split, migrate flag-by-flag.

**Steps:**

1. Add Konditional dependency
2. Define one flag in Konditional (mirror LaunchDarkly config)
3. Evaluate both systems, log differences
4. Once confident, switch to Konditional for that flag
5. Repeat for remaining flags
6. Remove LaunchDarkly/Split dependency

**Example dual evaluation:**

```kotlin
// Wrapper to compare both systems
fun isEnabled(flagName: String, context: Context): Boolean {
    val ldResult = ldClient.boolVariation(flagName, context.toLD(), false)

    val kdResult = when (flagName) {
        "dark_mode" -> context.evaluateOrDefault(Features.DARK_MODE, false)
        else -> null
    }

    if (kdResult != null && ldResult != kdResult) {
        logger.warn("Mismatch for $flagName: LD=$ldResult, KD=$kdResult")
    }

    return ldResult  // Use LaunchDarkly until validated
}
```

### Pattern 2: Big-Bang (Faster, Higher Risk)

Migrate all flags at once.

**Steps:**

1. Export all LaunchDarkly/Split flags
2. Define equivalent Konditional features
3. Test thoroughly in staging
4. Deploy and monitor closely
5. Roll back if issues arise

**Use when:** You have comprehensive tests and can tolerate brief outages.

### Pattern 3: New Features Only

Keep existing flags in LaunchDarkly/Split, use Konditional for new flags.

**Steps:**

1. Add Konditional for new features
2. Gradually migrate old flags as time permits
3. Eventually deprecate LaunchDarkly/Split

**Use when:** Migration isn't urgent, want to learn Konditional gradually.

---

## Common Migration Challenges

### Challenge 1: StableId Format

**LaunchDarkly/Split:** User IDs can be any string (`"user-123"`, `"alice@example.com"`)

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

**LaunchDarkly/Split:** Attributes set at runtime (`context.set("key", value)`)

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

**LaunchDarkly/Split:** Flags configured via dashboard, updated instantly

**Konditional:** Flags defined in code, but can load rules from JSON

**Solution:** Use serialization for remote updates:

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

**Issue:** Users in 50% rollout in LaunchDarkly won't match 50% in Konditional (different bucketing)

**Solution:** Accept redistribution or use salt to align:

- Accept: Users may see different experience temporarily
- Align: Adjust Konditional salt until distribution matches (trial-and-error)

Most teams accept redistribution since rollouts are temporary.

---

## Why Migrate?

### 1. Eliminate Runtime Errors

**LaunchDarkly/Split:**

```kotlin
client.boolVariation("dark-mod", false)  // Typo! Returns default silently
```

**Konditional:**

```kotlin
context.evaluate(Features.DARK_MOD)  // Compile error: unresolved reference
```

### 2. Reduce Infrastructure Costs

**LaunchDarkly/Split:** Monthly SaaS fees scale with MAU (monthly active users)

**Konditional:** Zero infrastructure cost (runs locally)

### 3. Improve Performance

**LaunchDarkly/Split:** Network latency (even with caching)

**Konditional:** Zero network calls, O(n) local evaluation where n < 10

### 4. Version Control Everything

**LaunchDarkly/Split:** Flag rules live in dashboard (hard to audit/review)

**Konditional:** Everything in code (Git history, code review, rollback)

### 5. Type Safety

**LaunchDarkly/Split:** `client.intVariation()` vs `client.stringVariation()` — must remember type

**Konditional:** Compiler knows the type, autocomplete works

---

## Decision Matrix

| Factor                   | Stick with LaunchDarkly/Split           | Migrate to Konditional             |
|--------------------------|-----------------------------------------|------------------------------------|
| **Team size**            | Large teams, need UI for non-engineers  | Engineering-first teams            |
| **Audit requirements**   | Need LaunchDarkly audit log             | Git provides audit trail           |
| **Budget**               | Cost not a concern                      | Want to eliminate SaaS fees        |
| **Update frequency**     | Need instant flag updates in production | Can deploy code to update flags    |
| **Tech stack**           | Multi-language (Java, Go, Python, etc.) | Kotlin/JVM only                    |
| **Type safety priority** | Low                                     | High (critical for correctness)    |
| **Offline support**      | Not needed                              | Essential (mobile, edge computing) |

---

## Next Steps

**Ready to migrate?** Start with [Getting Started](01-getting-started.md) to run your first Konditional flag.

**Need feature parity?** See [Targeting & Rollouts](04-targeting-rollouts.md) for advanced rules matching LaunchDarkly's
capabilities.

**Want to understand the model?** See [Core Concepts](03-core-concepts.md) for deep dive into Features, Context, and
Namespaces.
