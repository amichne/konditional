# Migration Guide: From String-Based to Type-Safe Configuration

This guide walks you through migrating your existing string-based configuration system to Konditional's type-safe approach.

## Migration Strategy

### Phase 1: Run in Parallel (Recommended)
Keep your existing system running while gradually migrating flags to Konditional. This allows:
- Safe rollback if issues arise
- Team learns the new system incrementally
- Production confidence builds gradually

### Phase 2: Full Migration
Once confident, deprecate the old system and complete the migration.

---

## Step 1: Identify Your Current Flags

### Audit Your String-Based Config

Create an inventory of your current flags:

```kotlin
// Example current system
interface ConfigService {
    fun getBoolean(key: String): Boolean?
    fun getString(key: String): String?
    fun getInt(key: String): Int?
}

// Current usage scattered across codebase:
config.getBoolean("dark_mode")
config.getString("api_endpoint")
config.getInt("max_retries")
config.getBoolean("premium_export")
config.getString("theme_color")
```

**Action**: Search your codebase for all `config.get*` calls and create a spreadsheet:

| Flag Name | Type | Default Value | Used In | Business Logic |
|-----------|------|---------------|---------|----------------|
| `dark_mode` | Boolean | `false` | SettingsActivity | Platform-dependent |
| `api_endpoint` | String | `"https://api.prod"` | ApiClient | Environment-dependent |
| `max_retries` | Int | `3` | NetworkService | None |
| `premium_export` | Boolean | `false` | ExportManager | Subscription-dependent |

---

## Step 2: Set Up Konditional

### Add Dependency

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.amichne:konditional:1.0.0")
}
```

### Create Base Context

Define what information your flags need for evaluation:

```kotlin
// Before: Context is implicit, scattered
config.getBoolean("premium_export")  // How does it know user tier?

// After: Context is explicit
data class AppContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,  // For rollout bucketing
    val userId: String,
    val subscriptionTier: SubscriptionTier
) : Context

enum class SubscriptionTier {
    FREE, PROFESSIONAL, ENTERPRISE
}
```

**Key Insight**: Making context explicit immediately reveals hidden dependencies in your current system.

---

## Step 3: Migrate Flag-by-Flag

### Example 1: Simple Boolean Flag

**Before:**
```kotlin
class SettingsActivity {
    private val config: ConfigService

    fun isDarkModeEnabled(): Boolean {
        return config.getBoolean("dark_mode") ?: false
    }
}
```

**After:**
```kotlin
// 1. Define the flag
enum class Features(override val key: String) : Conditional<Boolean, Context> {
    DARK_MODE("dark_mode")
}

// 2. Configure it
config {
    Features.DARK_MODE with {
        default(false)  // No more null handling!

        rule {
            platforms(Platform.IOS)  // Platform-specific behavior
        }.implies(true)
    }
}

// 3. Use it
class SettingsActivity(private val context: Context) {
    fun isDarkModeEnabled(): Boolean =
        context.evaluate(Features.DARK_MODE)  // Type-safe, non-null
}
```

**Migration Checklist:**
-  No null check needed
-  Type mismatch impossible
-  Platform logic moved to configuration
-  IDE auto-complete for flag name

---

### Example 2: Context-Dependent Flag with Custom Context

**Before:**
```kotlin
class ExportManager {
    private val config: ConfigService
    private val user: User

    fun canExport(): Boolean {
        val baseEnabled = config.getBoolean("export_enabled") ?: false
        val premiumOnly = config.getBoolean("export_premium_only") ?: true

        // Business logic scattered
        return baseEnabled && (!premiumOnly || user.tier == "premium")
    }
}
```

**Issues:**
- Business logic mixed with config
- Unclear what context is needed
- Multiple flag lookups for one decision
- String-based tier comparison

**After:**
```kotlin
// 1. Define context with business domain
data class AppContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val subscriptionTier: SubscriptionTier
) : Context

enum class SubscriptionTier {
    FREE, PROFESSIONAL, ENTERPRISE
}

// 2. Define flag with specific context
enum class Features(override val key: String) : Conditional<Boolean, AppContext> {
    DATA_EXPORT("export_enabled")
}

// 3. Business logic in configuration
config {
    Features.DATA_EXPORT with {
        default(false)

        // Full rollout for enterprise
        rule {
            extension {
                object : Evaluable<AppContext>() {
                    override fun matches(context: AppContext): Boolean =
                        context.subscriptionTier == SubscriptionTier.ENTERPRISE
                    override fun specificity(): Int = 1
                }
            }
        }.implies(true)

        // Gradual rollout for professional
        rule {
            extension {
                object : Evaluable<AppContext>() {
                    override fun matches(context: AppContext): Boolean =
                        context.subscriptionTier == SubscriptionTier.PROFESSIONAL
                    override fun specificity(): Int = 1
                }
            }
            rollout = Rollout.of(50.0)  // 50% of professional users
        }.implies(true)
    }
}

// 4. Use it
class ExportManager(private val context: AppContext) {
    fun canExport(): Boolean =
        context.evaluate(Features.DATA_EXPORT)  // Business logic handled
}
```

**Benefits:**
-  Business logic declarative
-  Context requirements explicit
-  Type-safe tier enum
-  Single source of truth

---

## Step 4: Handle Gradual Rollouts

### Before: Manual Bucketing

```kotlin
// Scattered rollout logic
fun isNewCheckoutEnabled(userId: String): Boolean {
    val enabled = config.getBoolean("new_checkout") ?: false
    if (!enabled) return false

    // Manual bucketing logic
    val hash = userId.hashCode()
    val bucket = (hash % 100).absoluteValue
    return bucket < 25  // 25% rollout
}
```

**Issues:**
- Bucketing logic duplicated
- Hard to change rollout percentage
- Not deterministic across platforms

### After: Built-In Rollouts

```kotlin
enum class Features(override val key: String) : Conditional<Boolean, Context> {
    NEW_CHECKOUT("new_checkout")
}

config {
    Features.NEW_CHECKOUT with {
        default(false)

        rule {
            rollout = Rollout.of(25.0)  // 25% of users
        }.implies(true)
    }
}

// Usage: Automatic bucketing via context.stableId
val enabled = context.evaluate(Features.NEW_CHECKOUT)
```

**Rollout Features:**
-  SHA-256 based bucketing (deterministic)
-  Independent buckets per flag (no correlation)
-  StableId ensures consistency across sessions/platforms
-  Change percentage in config, no code changes

---

## Step 5: Organize by Domain

### Before: Flat Flag Namespace

```kotlin
// All flags in one place
config.getBoolean("dark_mode")
config.getBoolean("premium_export")
config.getString("api_endpoint")
config.getBoolean("ios_new_checkout")
```

### After: Organized Enums

```kotlin
// Group by domain
enum class UiFeatures(override val key: String) : Conditional<Boolean, Context> {
    DARK_MODE("dark_mode"),
    NEW_CHECKOUT("new_checkout")
}

enum class ExportFeatures(override val key: String)
    : Conditional<Boolean, AppContext> {
    PREMIUM_EXPORT("premium_export")
}

enum class ApiConfig(override val key: String) : Conditional<String, Context> {
    ENDPOINT("api_endpoint")
}

// Clear namespacing in usage
context.evaluate(UiFeatures.DARK_MODE)
context.evaluate(ExportFeatures.PREMIUM_EXPORT)
context.evaluate(ApiConfig.ENDPOINT)
```

---

## Step 6: Testing Strategy

Testing becomes significantly simpler - no mocking frameworks needed, just plain data class construction:

```kotlin
@Test
fun `test dark mode enabled on iOS`() {
    val context = TestContext(platform = Platform.IOS)
    assertTrue(context.evaluate(Features.DARK_MODE))
}

@Test
fun `test premium export for enterprise users`() {
    val context = TestContext(subscriptionTier = SubscriptionTier.ENTERPRISE)
    assertTrue(context.evaluate(Features.DATA_EXPORT))
}

// Reusable test context factory
fun TestContext(
    locale: AppLocale = AppLocale.EN_US,
    platform: Platform = Platform.ANDROID,
    appVersion: Version = Version(1, 0, 0),
    stableId: StableId = StableId.of("test-user"),
    subscriptionTier: SubscriptionTier = SubscriptionTier.FREE
) = AppContext(locale, platform, appVersion, stableId, subscriptionTier)
```

**Key benefits**: No mocks, type-safe setup, reusable factories, simple data class construction

---

## Step 7: Configuration Management

### Loading from JSON

If you currently load config from a remote server:

```kotlin
// Before: String key-value pairs
val json = """
{
  "dark_mode": true,
  "api_endpoint": "https://api.prod",
  "max_retries": 5
}
"""
```

**After: Type-safe snapshots**

```kotlin
// 1. Define flags
enum class Features(override val key: String) : Conditional<Boolean, Context> {
    DARK_MODE("dark_mode")
}

// 2. Export current config
val snapshot = buildSnapshot {
    Features.DARK_MODE with {
        default(false)
    }
}

val json = SnapshotSerializer.default.serialize(snapshot)

// 3. Load from JSON
val result = SnapshotSerializer.default.deserialize(json)
when (result) {
    is ParseResult.Success -> FlagRegistry.load(result.value)
    is ParseResult.Failure -> logger.error("Config parse error: ${result.error}")
}

// 4. Apply patches without full reload
val patchJson = """
{
  "flags": [
    {
      "key": "dark_mode",
      "rules": [...],
      "default": true
    }
  ]
}
"""
SnapshotSerializer.default.applyPatchJson(currentConfig, patchJson)
```

**Benefits:**
-  Type-safe deserialization
-  Atomic config updates
-  Patch support for incremental changes
-  Parse errors caught before applying

---

## Common Migration Patterns

### Feature Flags
```kotlin
// Before: Nullable boolean checks
if (config.getBoolean("new_feature") == true) { }

// After: Type-safe evaluation
if (context.evaluate(Features.NEW_FEATURE)) { }
```

### Environment-Based Configuration
```kotlin
// Before: Environment logic in code
val endpoint = when (System.getenv("ENV")) {
    "dev" -> config.getString("dev_endpoint") ?: "https://dev.api"
    else -> config.getString("prod_endpoint") ?: "https://api.prod"
}

// After: Environment logic in configuration
config {
    ApiConfig.ENDPOINT with {
        default("https://api.prod")
        rule {
            extension { /* environment check */ }
        }.implies("https://api.dev")
    }
}
```

### User Segmentation
```kotlin
// Before: Business logic mixed with config
fun canExport(): Boolean {
    val enabled = config.getBoolean("export") ?: false
    return enabled && user.tier == "premium"
}

// After: Business logic in configuration
data class AppContext(..., val subscriptionTier: SubscriptionTier) : Context

config {
    Features.DATA_EXPORT with {
        default(false)
        rule {
            extension { context.subscriptionTier == SubscriptionTier.PREMIUM }
        }.implies(true)
    }
}
```

---

## Migration Checklist

### For Each Flag:

- [ ] Identify type (Boolean, String, Int, custom)
- [ ] Determine context requirements
- [ ] Create enum definition
- [ ] Define default value
- [ ] Add rules for special cases
- [ ] Update call sites to use `context.evaluate()`
- [ ] Remove null handling code
- [ ] Update tests to use simple context objects
- [ ] Verify no typos in flag names (IDE will show)
- [ ] Remove old string-based lookup

### For Your System:

- [ ] Define base Context interface
- [ ] Create context factory functions
- [ ] Set up flag registration at app start
- [ ] Configure remote config deserialization (if needed)
- [ ] Create test context factories
- [ ] Update documentation
- [ ] Train team on new patterns
- [ ] Set deprecation timeline for old system

---

## Troubleshooting Common Scenarios

**Different context per flag**: Define multiple context types - basic features use `Context`, domain-specific features use custom contexts like `EnterpriseContext`.

**Runtime-dependent flags**: Use custom `Evaluable` extensions that check runtime state (time of day, system resources, etc.).

**Dynamic config updates**: Use `FlagRegistry.load(newSnapshot)` for full updates or `SnapshotSerializer.default.applyPatchJson()` for incremental patches.

---

## Next Steps

1. **[Quick Start Guide](./QuickStart.md)** - Get your first flag running
2. **[Error Prevention Reference](./ErrorPrevention.md)** - See all eliminated error classes
3. **[Context Guide](./Context.md)** - Design your context types
4. **[Builders Guide](./Builders.md)** - Master the configuration DSL

**Remember**: Migration can be gradual. Start with your most error-prone flags first!
