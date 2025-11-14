# Real-World Comparison: Your Actual Code

## Current Implementation (From your codebase)

### EnterpriseTestFeatures.kt

```kotlin
/**
 * Feature flags for enterprise contexts.
 */
enum class EnterpriseFeatures(
    override val key: String,
) : BooleanFeature<EnterpriseContext, Taxonomy.Core> {
    /** Advanced analytics feature */
    ADVANCED_ANALYTICS("advanced_analytics"),

    /** Custom branding feature */
    CUSTOM_BRANDING("custom_branding"),

    /** API access feature */
    API_ACCESS("api_access");

    override val module: Taxonomy.Core = Taxonomy.Core  // ❌ BOILERPLATE
}

/**
 * Feature flags for experiment contexts.
 */
enum class ExperimentFeatures(
    override val key: String,
) : StringFeature<ExperimentContext, Taxonomy.Core> {
    /** Homepage variant */
    HOMEPAGE_VARIANT("homepage_variant"),

    /** Onboarding style */
    ONBOARDING_STYLE("onboarding_style");

    override val module: Taxonomy.Core = Taxonomy.Core  // ❌ BOILERPLATE
}
```

### Problems with Current Approach

1. ❌ **Boilerplate**: `override val module = Taxonomy.Core` repeated in EVERY enum
2. ❌ **Type separation**: Boolean features in one enum, String features in another
3. ❌ **No enumeration**: Can't get list of all enterprise-related features
4. ❌ **Manual tracking**: Need to manually maintain lists for testing/validation

---

## FeatureContainer Implementation

### EnterpriseTestFeatures.kt (Refactored)

```kotlin
/**
 * All enterprise-related features in one container.
 * Supports Boolean, String, Int, and custom types.
 */
object EnterpriseFeatures : FeatureContainer<EnterpriseContext, Taxonomy.Core>(
    Taxonomy.Core  // ✅ Module declared ONCE
) {
    // Boolean features
    val ADVANCED_ANALYTICS by boolean("advanced_analytics")
    val CUSTOM_BRANDING by boolean("custom_branding")
    val API_ACCESS by boolean("api_access")

    // String features (mixed with boolean - not possible with enums!)
    val HOMEPAGE_VARIANT by string("homepage_variant")
    val ONBOARDING_STYLE by string("onboarding_style")

    // Additional types (examples)
    val MAX_API_CALLS by int("max_api_calls")
    val BRANDING_CONFIG by jsonObject<BrandingConfig>("branding_config")
}

data class BrandingConfig(
    val primaryColor: String,
    val logoUrl: String,
    val companyName: String
)
```

---

## Side-by-Side Comparison

<table>
<tr>
<th>Current (Enums)</th>
<th>FeatureContainer</th>
</tr>
<tr>
<td>

```kotlin
// 2 separate enums
enum class EnterpriseFeatures(
    override val key: String,
) : BooleanFeature<
    EnterpriseContext,
    Taxonomy.Core
> {
    ADVANCED_ANALYTICS("advanced_analytics"),
    CUSTOM_BRANDING("custom_branding"),
    API_ACCESS("api_access");

    override val module = Taxonomy.Core
}

enum class ExperimentFeatures(
    override val key: String,
) : StringFeature<
    ExperimentContext,
    Taxonomy.Core
> {
    HOMEPAGE_VARIANT("homepage_variant"),
    ONBOARDING_STYLE("onboarding_style");

    override val module = Taxonomy.Core
}

// Lines: 26
// Modules declared: 2 times
// Enums needed: 2
// Can enumerate: ❌ Manually
```

</td>
<td>

```kotlin
// 1 unified container
object EnterpriseFeatures : FeatureContainer<
    EnterpriseContext,
    Taxonomy.Core
>(Taxonomy.Core) {

    val ADVANCED_ANALYTICS by boolean("advanced_analytics")
    val CUSTOM_BRANDING by boolean("custom_branding")
    val API_ACCESS by boolean("api_access")
    val HOMEPAGE_VARIANT by string("homepage_variant")
    val ONBOARDING_STYLE by string("onboarding_style")
}

// Lines: 11
// Module declared: 1 time
// Containers needed: 1
// Can enumerate: ✅ allFeatures()


```

</td>
</tr>
</table>

**Metrics:**
- 📉 **58% fewer lines** (26 → 11)
- 📉 **50% less boilerplate** (2 module overrides → 1 declaration)
- ✅ **Mixed types** in single container
- ✅ **Auto-enumeration** via `EnterpriseFeatures.allFeatures()`

---

## Real-World Use Cases (Your Code)

### Use Case 1: Testing All Enterprise Features

```kotlin
// ❌ Current approach - must manually track
@Test
fun `test all enterprise features are configured`() {
    val booleanFeatures = EnterpriseFeatures.values().toList()
    val stringFeatures = ExperimentFeatures.values().toList()
    // Must remember to add every new enum...

    booleanFeatures.forEach { /* test */ }
    stringFeatures.forEach { /* test */ }
}

// ✅ FeatureContainer - automatic
@Test
fun `test all enterprise features are configured`() {
    EnterpriseFeatures.allFeatures().forEach { feature ->
        // Automatically includes ALL features, always up to date
        assertDoesNotThrow {
            context.evaluateSafe(feature as Feature<*, Any, EnterpriseContext, Taxonomy.Core>)
        }
    }
}
```

### Use Case 2: Validating Configuration

```kotlin
// ❌ Current approach - manual tracking
fun validateEnterpriseConfig(config: Map<String, Any>) {
    val expectedKeys = listOf(
        "advanced_analytics",
        "custom_branding",
        "api_access",
        "homepage_variant",
        "onboarding_style"
        // Easy to forget to update this list!
    )

    val missing = expectedKeys.filter { it !in config }
    // ...
}

// ✅ FeatureContainer - automatic
fun validateEnterpriseConfig(config: Map<String, Any>) {
    val expectedKeys = EnterpriseFeatures.allFeatures().map { it.key }
    val missing = expectedKeys.filter { it !in config }
    // Always accurate, never stale
}
```

### Use Case 3: Feature Inventory for Documentation

```kotlin
// ❌ Current approach - must manually list enums
fun generateDocs() {
    println("Enterprise Features:")
    EnterpriseFeatures.values().forEach { println("- ${it.key}") }
    println("Experiment Features:")
    ExperimentFeatures.values().forEach { println("- ${it.key}") }
    // Must update this when adding new enums
}

// ✅ FeatureContainer - automatic
fun generateDocs() {
    println("Enterprise Features (${EnterpriseFeatures.allFeatures().size} total):")
    EnterpriseFeatures.allFeatures().forEach { feature ->
        println("- ${feature.key} (${feature::class.simpleName})")
    }
    // Automatically includes all features with their types
}
```

---

## Migration Path for Your Codebase

### Step 1: Identify Conversion Candidates

From your codebase, these would benefit most:

```kotlin
// Good candidates (simple boolean enums):
✅ EnterpriseFeatures (3 boolean features)
✅ TestJsonObjectFeatures (if can mix with other types)
✅ TestThemeFeatures (if can mix with other types)
✅ TestListFeatures (if can mix with other types)

// Consider for future:
⚠️ Any new feature sets you create
⚠️ Features where you need validation/enumeration
```

### Step 2: Gradual Migration

```kotlin
// Keep existing enums, add FeatureContainer alongside
@Deprecated("Use EnterpriseFeatures container",
    ReplaceWith("EnterpriseFeatures.ADVANCED_ANALYTICS"))
enum class EnterpriseFeatures_OLD { ... }

object EnterpriseFeatures : FeatureContainer<...> { ... }

// Update call sites gradually
context.evaluate(EnterpriseFeatures_OLD.API_ACCESS)  // Old
context.evaluate(EnterpriseFeatures.API_ACCESS)      // New
```

### Step 3: Leverage New Capabilities

Once migrated, add these value-adds:

```kotlin
// Validation in tests
EnterpriseFeatures.allFeatures().forEach { /* validate */ }

// Configuration auditing
fun auditMissingConfig() =
    EnterpriseFeatures.allFeatures()
        .filter { !registry.hasFlag(it.key) }

// Documentation generation
fun exportFeatureList() =
    EnterpriseFeatures.allFeatures()
        .map { FeatureDoc(it.key, it.module, it::class.simpleName) }
```

---

## Bottom Line

**FeatureContainer gives you:**
1. ✅ 58% less code for your actual use case
2. ✅ Zero boilerplate (1 module declaration vs 2+ overrides)
3. ✅ Mixed types (boolean + string in one container)
4. ✅ Complete enumeration (automatic, always accurate)
5. ✅ Better testing (iterate all features)
6. ✅ Better validation (detect missing configs)

**Ready to use?** All core pieces are implemented:
- `src/main/kotlin/io/amichne/konditional/core/FeatureContainer.kt`
- `src/test/kotlin/io/amichne/konditional/core/FeatureContainerTest.kt`
- `src/test/kotlin/io/amichne/konditional/core/FeatureContainerExample.kt`

**Next:** Review files, run tests, try with your features!
