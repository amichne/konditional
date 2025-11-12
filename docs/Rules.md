# Rules & Evaluables

Rules define targeting criteria and rollout strategies for feature flags. The Konditional rule system is built on a composable architecture that separates standard targeting (platform, locale, version) from custom domain logic through the `Evaluable` abstraction.

## Rule Fundamentals

A **Rule** specifies conditions that must be met for a particular value to be returned. Rules combine:

- Standard targeting criteria (locale, platform, version)
- Custom evaluation logic through extensions
- Rollout percentage for gradual deployment
- Optional documentation notes

```kotlin
rule {
    platforms(Platform.IOS, Platform.ANDROID)
    locales(AppLocale.EN_US, AppLocale.EN_CA)
    versions {
        min(2, 0, 0)
        max(3, 0, 0)
    }
    rollout = Rollout.of(50.0)
    note("Mobile-only feature, 50% gradual rollout")
}.implies(true)
```

## Basic Targeting

### Platform Targeting

Target specific platforms where your application runs:

```kotlin
rule {
    platforms(Platform.IOS, Platform.ANDROID)
}.implies(mobileValue)

rule {
    platforms(Platform.WEB)
}.implies(webValue)

rule {
    platforms(Platform.SERVER)
}.implies(backendValue)
```

Available platforms:
- `Platform.IOS`
- `Platform.ANDROID`
- `Platform.WEB`
- `Platform.DESKTOP`
- `Platform.SERVER`

### Locale Targeting

Target users based on language and region:

```kotlin
rule {
    locales(AppLocale.EN_US, AppLocale.EN_CA, AppLocale.EN_GB)
}.implies(englishValue)

rule {
    locales(AppLocale.FR_FR, AppLocale.FR_CA)
}.implies(frenchValue)

rule {
    locales(AppLocale.ES_ES, AppLocale.ES_MX)
}.implies(spanishValue)
```

### Version Targeting

Target specific version ranges using semantic versioning:

```kotlin
// Minimum version only
rule {
    versions {
        min(2, 0, 0)  // Version 2.0.0 or higher
    }
}.implies(newFeatureValue)

// Maximum version only
rule {
    versions {
        max(2, 0, 0)  // Version 2.0.0 or lower
    }
}.implies(legacyValue)

// Version range
rule {
    versions {
        min(1, 5, 0)  // >= 1.5.0
        max(2, 0, 0)  // <= 2.0.0
    }
}.implies(transitionValue)

// Exact version
rule {
    versions {
        min(2, 1, 3)
        max(2, 1, 3)
    }
}.implies(specificVersionValue)
```

### Combined Targeting

Combine multiple criteria - all must match:

```kotlin
rule {
    platforms(Platform.IOS, Platform.ANDROID)
    locales(AppLocale.EN_US)
    versions {
        min(2, 0, 0)
    }
}.implies(mobileEnglishV2Value)
```

## Rollouts

Rollouts enable gradual feature deployment to a percentage of users who match the rule criteria.

### Basic Rollout

```kotlin
rule {
    platforms(Platform.IOS)
    rollout = Rollout.of(25.0)  // 25% of iOS users
}.implies(true)
```

### Rollout Characteristics

**Deterministic**: The same user (identified by `stableId`) always gets the same rollout assignment.

```kotlin
val user1 = Context(
    locale = AppLocale.EN_US,
    platform = Platform.IOS,
    appVersion = Version(2, 0, 0),
    stableId = StableId.of("user-123")
)

// This user will always get the same result
val result1 = user1.evaluateSafe(feature)  // Deterministic
val result2 = user1.evaluateSafe(feature)  // Same as result1
```

**Independent**: Each flag has its own bucketing space. A user in the 25% rollout for one feature is independent of their assignment in another feature.

**Stable**: Changing a flag's configuration does not affect rollout assignments unless you change the salt.

### Rollout Strategies

#### Gradual Rollout

Increase rollout percentage over time:

```kotlin
// Phase 1: 10%
config {
    MyFeature.NEW_CHECKOUT with {
        default(false)
        rule {
            rollout = Rollout.of(10.0)
        }.implies(true)
    }
}

// Phase 2: 50%
config {
    MyFeature.NEW_CHECKOUT with {
        default(false)
        rule {
            rollout = Rollout.of(50.0)
        }.implies(true)
    }
}

// Phase 3: 100%
config {
    MyFeature.NEW_CHECKOUT with {
        default(false)
        rule {
            rollout = Rollout.MAX  // or Rollout.of(100.0)
        }.implies(true)
    }
}
```

#### Canary Deployment

Test with a small percentage before wider rollout:

```kotlin
config {
    MyFeature.RISKY_FEATURE with {
        default(false)

        // Canary: 1% of production users
        rule {
            rollout = Rollout.of(1.0)
            note("Canary deployment - monitoring for issues")
        }.implies(true)
    }
}
```

#### Segmented Rollout

Different rollout percentages for different segments:

```kotlin
config {
    MyFeature.BETA_FEATURE with {
        default(false)

        // 100% rollout for internal users
        rule {
            extension {
                object : Evaluable<EnterpriseContext>() {
                    override fun matches(context: EnterpriseContext) =
                        context.organizationId == "internal"
                    override fun specificity() = 1
                }
            }
            rollout = Rollout.MAX
        }.implies(true)

        // 25% rollout for enterprise customers
        rule {
            extension {
                object : Evaluable<EnterpriseContext>() {
                    override fun matches(context: EnterpriseContext) =
                        context.subscriptionTier == SubscriptionTier.ENTERPRISE
                    override fun specificity() = 1
                }
            }
            rollout = Rollout.of(25.0)
        }.implies(true)
    }
}
```

### Rollout Salt

The salt affects hash-based bucketing. Changing the salt redistributes users across rollout buckets:

```kotlin
config {
    MyFeature.EXPERIMENT with {
        default(false)
        salt("v1")  // Initial salt

        rule {
            rollout = Rollout.of(50.0)
        }.implies(true)
    }
}

// Change salt to redistribute users
config {
    MyFeature.EXPERIMENT with {
        default(false)
        salt("v2")  // Different salt = different buckets

        rule {
            rollout = Rollout.of(50.0)
        }.implies(true)
    }
}
```

Use cases for changing salt:
- Reset an A/B test with fresh user assignments
- Fix biased rollout distributions
- Run a new experiment on the same feature

## Rule Evaluation Order

Rules are evaluated in order of **specificity** (highest first). When multiple rules match, the first matching rule (by specificity) determines the value.

### Specificity Calculation

Specificity is the sum of specified constraints:

```kotlin
// Specificity = 0 (no constraints)
rule {
    rollout = Rollout.MAX
}.implies(value)

// Specificity = 1 (one constraint)
rule {
    platforms(Platform.IOS)
}.implies(value)

// Specificity = 2 (two constraints)
rule {
    platforms(Platform.IOS)
    locales(AppLocale.EN_US)
}.implies(value)

// Specificity = 3 (three constraints)
rule {
    platforms(Platform.IOS)
    locales(AppLocale.EN_US)
    versions { min(2, 0, 0) }
}.implies(value)

// Custom extensions add to specificity
rule {
    platforms(Platform.IOS)  // +1
    extension {  // +1 (from extension's specificity())
        object : Evaluable<Context>() {
            override fun matches(context: Context) = /* custom logic */
            override fun specificity() = 1
        }
    }
}.implies(value)  // Total specificity = 2
```

### Evaluation Example

```kotlin
config {
    MyFeature.THEME with {
        default("light")

        // Specificity = 2, evaluated first
        rule {
            platforms(Platform.IOS)
            locales(AppLocale.EN_US)
        }.implies("dark-us")

        // Specificity = 1, evaluated second
        rule {
            platforms(Platform.IOS)
        }.implies("dark-ios")

        // Specificity = 1, evaluated third (tie broken by note alphabetically)
        rule {
            locales(AppLocale.EN_US)
        }.implies("light-us")
    }
}

// Context: iOS + EN_US
// Matches both rule 1 (specificity 2) and rule 2 (specificity 1)
// Returns "dark-us" (highest specificity wins)
val context1 = Context(
    platform = Platform.IOS,
    locale = AppLocale.EN_US,
    // ...
)
context1.evaluateSafe(MyFeature.THEME)  // "dark-us"

// Context: iOS + FR_FR
// Matches only rule 2 (specificity 1)
// Returns "dark-ios"
val context2 = Context(
    platform = Platform.IOS,
    locale = AppLocale.FR_FR,
    // ...
)
context2.evaluateSafe(MyFeature.THEME)  // "dark-ios"
```

## Custom Extensions

Extensions allow domain-specific targeting beyond standard criteria using the `Evaluable` abstraction.

### Evaluable Interface

```kotlin
abstract class Evaluable<C : Context> {
    open fun matches(context: C): Boolean = true
    open fun specificity(): Int = 0
}
```

### Basic Extension

```kotlin
rule {
    extension {
        object : Evaluable<EnterpriseContext>() {
            override fun matches(context: EnterpriseContext): Boolean =
                context.subscriptionTier == SubscriptionTier.ENTERPRISE

            override fun specificity(): Int = 1
        }
    }
}.implies(enterpriseValue)
```

### Complex Extensions

Combine multiple conditions:

```kotlin
rule {
    platforms(Platform.WEB)
    extension {
        object : Evaluable<EnterpriseContext>() {
            override fun matches(context: EnterpriseContext): Boolean {
                val isPremium = context.subscriptionTier in setOf(
                    SubscriptionTier.PROFESSIONAL,
                    SubscriptionTier.ENTERPRISE
                )
                val isAdmin = context.userRole in setOf(
                    UserRole.ADMIN,
                    UserRole.OWNER
                )
                return isPremium && isAdmin
            }

            override fun specificity(): Int = 2  // Two conditions checked
        }
    }
}.implies(premiumAdminValue)
```

### Reusable Extensions

Define extension classes for reuse:

```kotlin
class SubscriptionTierEvaluable(
    private val allowedTiers: Set<SubscriptionTier>
) : Evaluable<EnterpriseContext>() {
    override fun matches(context: EnterpriseContext): Boolean =
        context.subscriptionTier in allowedTiers

    override fun specificity(): Int = 1
}

class UserRoleEvaluable(
    private val allowedRoles: Set<UserRole>
) : Evaluable<EnterpriseContext>() {
    override fun matches(context: EnterpriseContext): Boolean =
        context.userRole in allowedRoles

    override fun specificity(): Int = 1
}

// Use in rules
config {
    MyFeature.ADMIN_PANEL with {
        default(false)

        rule {
            extension {
                UserRoleEvaluable(setOf(UserRole.ADMIN, UserRole.OWNER))
            }
        }.implies(true)
    }

    MyFeature.PREMIUM_FEATURES with {
        default(false)

        rule {
            extension {
                SubscriptionTierEvaluable(setOf(
                    SubscriptionTier.PROFESSIONAL,
                    SubscriptionTier.ENTERPRISE
                ))
            }
        }.implies(true)
    }
}
```

## Rule Composition

Rules compose base targeting with custom extensions. Both must match for the rule to match.

```kotlin
rule {
    // Base targeting (BaseEvaluable)
    platforms(Platform.WEB)
    locales(AppLocale.EN_US)

    // Custom extension
    extension {
        object : Evaluable<EnterpriseContext>() {
            override fun matches(context: EnterpriseContext) =
                context.organizationId == "enterprise-123"
            override fun specificity() = 1
        }
    }

    // Rollout (checked after matching)
    rollout = Rollout.of(50.0)
}.implies(value)
```

Evaluation order:
1. Base targeting matches (platform, locale, version)
2. Extension matches (custom logic)
3. Rollout eligibility (hash-based bucketing)

All three must succeed for the rule to select its value.

## Rule Notes

Add documentation to rules for clarity:

```kotlin
rule {
    platforms(Platform.IOS)
    rollout = Rollout.of(25.0)
    note("Gradual rollout to iOS users - Phase 1 of mobile launch")
}.implies(true)

rule {
    extension {
        object : Evaluable<EnterpriseContext>() {
            override fun matches(context: EnterpriseContext) =
                context.organizationId in setOf("beta-tester-1", "beta-tester-2")
            override fun specificity() = 1
        }
    }
    note("Beta testing with specific partner organizations")
}.implies(betaValue)
```

Notes are useful for:
- Explaining complex targeting logic
- Tracking rollout phases
- Documenting business decisions
- Debugging evaluation behavior

## Empty Rule Semantics

Empty constraints match everything:

```kotlin
// Matches all contexts (no constraints)
rule {
    rollout = Rollout.MAX
}.implies(defaultValue)

// Matches all platforms (locales empty)
rule {
    locales(AppLocale.EN_US)
}.implies(englishValue)

// Matches all versions (versionRange unbounded)
rule {
    platforms(Platform.IOS)
}.implies(iosValue)
```

This "match all" semantic is useful for default rules or broad targeting.

## Best Practices

### Order Rules by Specificity

While Konditional handles this automatically, thinking in terms of specificity helps design clearer rules:

```kotlin
config {
    MyFeature.VALUE with {
        default("default")

        // Most specific: platform + locale + version
        rule {
            platforms(Platform.IOS)
            locales(AppLocale.EN_US)
            versions { min(2, 0, 0) }
        }.implies("specific")

        // Medium specific: platform + locale
        rule {
            platforms(Platform.IOS)
            locales(AppLocale.EN_US)
        }.implies("medium")

        // Least specific: platform only
        rule {
            platforms(Platform.IOS)
        }.implies("broad")
    }
}
```

### Use Extensions for Domain Logic

Keep standard targeting for platform/locale/version and use extensions for business logic:

```kotlin
// Good: Separation of concerns
rule {
    platforms(Platform.WEB)  // Standard targeting
    extension {  // Domain logic
        SubscriptionTierEvaluable(setOf(SubscriptionTier.ENTERPRISE))
    }
}.implies(value)

// Avoid: Mixing concerns in one place would require custom rule types
```

### Document Complex Rules

Use notes for rules with non-obvious logic:

```kotlin
rule {
    extension {
        object : Evaluable<Context>() {
            override fun matches(context: Context) = /* complex logic */
            override fun specificity() = 3
        }
    }
    rollout = Rollout.of(15.0)
    note("Targeting high-value users for premium feature test - approved by PM on 2024-01-15")
}.implies(premiumValue)
```

### Test Rule Evaluation

Create unit tests for complex rule logic:

```kotlin
@Test
fun `enterprise users get premium features`() {
    val context = EnterpriseContext(
        locale = AppLocale.EN_US,
        platform = Platform.WEB,
        appVersion = Version(2, 0, 0),
        stableId = StableId.of("test-user"),
        subscriptionTier = SubscriptionTier.ENTERPRISE,
        // ...
    )

    val result = context.evaluateSafe(MyFeature.PREMIUM_FEATURE)
    assertTrue(result is EvaluationResult.Success && result.value == true)
}
```

## Next Steps

- **[Flags](Flags.md)**: Learn about feature flag registration
- **[Builders](Builders.md)**: Master the rule DSL
- **[Context](Context.md)**: Understand custom context extensions
- **[Overview](index.md)**: Back to API overview
