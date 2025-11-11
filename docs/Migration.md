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
- ✅ No null check needed
- ✅ Type mismatch impossible
- ✅ Platform logic moved to configuration
- ✅ IDE auto-complete for flag name

---

### Example 2: String Configuration

**Before:**
```kotlin
class ApiClient {
    private val config: ConfigService

    fun getEndpoint(): String {
        val env = System.getenv("ENV") ?: "prod"
        return when (env) {
            "dev" -> config.getString("api_endpoint_dev") ?: "https://dev.api"
            "staging" -> config.getString("api_endpoint_staging") ?: "https://staging.api"
            else -> config.getString("api_endpoint_prod") ?: "https://api.prod"
        }
    }
}
```

**Issues:**
- 3 separate flag names
- Environment logic in application code
- 3 null checks
- Easy to typo flag names

**After:**
```kotlin
// 1. Define the flag
enum class ApiConfig(override val key: String) : Conditional<String, Context> {
    ENDPOINT("api_endpoint")
}

// 2. Configure with environment-based rules
config {
    ApiConfig.ENDPOINT with {
        default("https://api.prod.example.com")

        rule {
            extension {
                object : Evaluable<Context>() {
                    override fun matches(context: Context): Boolean =
                        System.getenv("ENV") == "dev"
                    override fun specificity(): Int = 1
                }
            }
        }.implies("https://dev.api.example.com")

        rule {
            extension {
                object : Evaluable<Context>() {
                    override fun matches(context: Context): Boolean =
                        System.getenv("ENV") == "staging"
                    override fun specificity(): Int = 1
                }
            }
        }.implies("https://staging.api.example.com")
    }
}

// 3. Use it
class ApiClient(private val context: Context) {
    fun getEndpoint(): String =
        context.evaluate(ApiConfig.ENDPOINT)  // Environment handled automatically
}
```

**Benefits:**
- ✅ Single flag name
- ✅ Environment logic centralized
- ✅ Zero null checks
- ✅ Testable by mocking context, not environment variables

---

### Example 3: Complex Type (Data Class)

**Before:**
```kotlin
class ThemeManager {
    private val config: ConfigService

    fun getTheme(): ThemeConfig {
        val primaryColor = config.getString("theme_primary_color") ?: "#FFFFFF"
        val secondaryColor = config.getString("theme_secondary_color") ?: "#000000"
        val fontSize = config.getInt("theme_font_size") ?: 14
        val darkMode = config.getBoolean("theme_dark_mode") ?: false

        return ThemeConfig(primaryColor, secondaryColor, fontSize, darkMode)
    }
}
```

**Issues:**
- 4 separate flags for one logical concept
- 4 null checks
- No guarantee all theme parts are consistent
- Hard to A/B test complete themes

**After:**
```kotlin
// 1. Define the type
data class ThemeConfig(
    val primaryColor: String,
    val secondaryColor: String,
    val fontSize: Int,
    val darkMode: Boolean
)

// 2. Define single flag for entire theme
enum class Theme(override val key: String) : Conditional<ThemeConfig, Context> {
    APP_THEME("app_theme")
}

// 3. Configure complete themes atomically
config {
    Theme.APP_THEME with {
        default(
            ThemeConfig(
                primaryColor = "#FFFFFF",
                secondaryColor = "#000000",
                fontSize = 14,
                darkMode = false
            )
        )

        // Entire theme for iOS
        rule {
            platforms(Platform.IOS)
        }.implies(
            ThemeConfig(
                primaryColor = "#F5F5F5",
                secondaryColor = "#1E1E1E",
                fontSize = 16,
                darkMode = true
            )
        )

        // Gradual rollout of new theme
        rule {
            platforms(Platform.ANDROID)
            rollout = Rollout.of(25.0)  // 25% of Android users
        }.implies(
            ThemeConfig(
                primaryColor = "#E0E0E0",
                secondaryColor = "#2C2C2C",
                fontSize = 15,
                darkMode = true
            )
        )
    }
}

// 4. Use it
class ThemeManager(private val context: Context) {
    fun getTheme(): ThemeConfig =
        context.evaluate(Theme.APP_THEME)  // Atomic, type-safe, non-null
}
```

**Benefits:**
- ✅ Atomic theme updates
- ✅ Single flag for related config
- ✅ A/B testing entire themes
- ✅ Guaranteed consistency

---

### Example 4: Context-Dependent Flag

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
- ✅ Business logic declarative
- ✅ Context requirements explicit
- ✅ Type-safe tier enum
- ✅ Single source of truth

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
- ✅ SHA-256 based bucketing (deterministic)
- ✅ Independent buckets per flag (no correlation)
- ✅ StableId ensures consistency across sessions/platforms
- ✅ Change percentage in config, no code changes

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

### Before: Mocking Config Service

```kotlin
@Test
fun `test dark mode enabled`() {
    val mockConfig = mock<ConfigService>()
    whenever(mockConfig.getBoolean("dark_mode")).thenReturn(true)

    val activity = SettingsActivity(mockConfig)
    assertTrue(activity.isDarkModeEnabled())
}
```

**Issues:**
- Mock setup boilerplate
- String literal duplication
- Mocking framework dependency

### After: Simple Context Objects

```kotlin
@Test
fun `test dark mode enabled on iOS`() {
    val context = TestContext(platform = Platform.IOS)

    val enabled = context.evaluate(Features.DARK_MODE)

    assertTrue(enabled)
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

**Benefits:**
- ✅ No mocking needed
- ✅ Simple data class construction
- ✅ Type-safe test setup
- ✅ Reusable factories

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
- ✅ Type-safe deserialization
- ✅ Atomic config updates
- ✅ Patch support for incremental changes
- ✅ Parse errors caught before applying

---

## Common Migration Patterns

### Pattern 1: Feature Flag → Conditional Boolean

```kotlin
// Before
if (config.getBoolean("new_feature") == true) { }

// After
if (context.evaluate(Features.NEW_FEATURE)) { }
```

### Pattern 2: Environment Config → Custom Evaluable

```kotlin
// Before
val endpoint = when (System.getenv("ENV")) {
    "dev" -> config.getString("dev_endpoint")
    else -> config.getString("prod_endpoint")
}

// After
enum class ApiConfig(override val key: String) : Conditional<String, Context> {
    ENDPOINT("api_endpoint")
}

config {
    ApiConfig.ENDPOINT with {
        default("https://api.prod")
        rule {
            extension {
                object : Evaluable<Context>() {
                    override fun matches(context: Context) =
                        System.getenv("ENV") == "dev"
                    override fun specificity() = 1
                }
            }
        }.implies("https://api.dev")
    }
}
```

### Pattern 3: User Segmentation → Context Extensions

```kotlin
// Before
fun isPremiumFeatureEnabled(user: User): Boolean {
    val enabled = config.getBoolean("premium_feature") ?: false
    return enabled && user.tier == "premium"
}

// After
data class AppContext(
    // ... base fields
    val subscriptionTier: SubscriptionTier
) : Context

enum class PremiumFeatures(override val key: String)
    : Conditional<Boolean, AppContext> {
    ADVANCED_ANALYTICS("premium_feature")
}

config {
    PremiumFeatures.ADVANCED_ANALYTICS with {
        default(false)
        rule {
            extension {
                object : Evaluable<AppContext>() {
                    override fun matches(context: AppContext) =
                        context.subscriptionTier == SubscriptionTier.PREMIUM
                    override fun specificity() = 1
                }
            }
        }.implies(true)
    }
}

// Usage
val enabled = context.evaluate(PremiumFeatures.ADVANCED_ANALYTICS)
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

## Troubleshooting

### "I need different context for different flags"

Use multiple context types:

```kotlin
// Basic features use base context
enum class BasicFeatures(override val key: String)
    : Conditional<Boolean, Context> {
    DARK_MODE("dark_mode")
}

// Enterprise features require more context
enum class EnterpriseFeatures(override val key: String)
    : Conditional<Boolean, EnterpriseContext> {
    BULK_EXPORT("bulk_export")
}
```

### "My flag depends on runtime state"

Use custom Evaluable extensions:

```kotlin
config {
    Features.PEAK_HOURS_MODE with {
        default(false)
        rule {
            extension {
                object : Evaluable<Context>() {
                    override fun matches(context: Context): Boolean {
                        val hour = LocalTime.now().hour
                        return hour in 9..17  // 9 AM - 5 PM
                    }
                    override fun specificity() = 1
                }
            }
        }.implies(true)
    }
}
```

### "I need to update config at runtime"

Use atomic registry updates:

```kotlin
// Update entire config
FlagRegistry.load(newSnapshot)

// Or apply incremental patch
SnapshotSerializer.default.applyPatchJson(currentSnapshot, patchJson)
```

---

## Next Steps

1. **[Quick Start Guide](./QuickStart.md)** - Get your first flag running
2. **[Error Prevention Reference](./ErrorPrevention.md)** - See all eliminated error classes
3. **[Context Guide](./Context.md)** - Design your context types
4. **[Builders Guide](./Builders.md)** - Master the configuration DSL

**Remember**: Migration can be gradual. Start with your most error-prone flags first!
