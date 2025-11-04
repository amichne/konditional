---
title: Context Polymorphism
description: Create custom context types tailored to your domain while maintaining type safety
---

# Context Polymorphism

One of Konditional's most powerful features is context polymorphism - the ability to define custom context types tailored to your domain, while maintaining full type safety throughout the system.

## Why Custom Contexts?

The base `Context` interface provides common properties like `locale`, `platform`, `appVersion`, and `stableId`. However, your application may need to make decisions based on:

- **Organization/Tenant ID** for multi-tenant applications
- **User roles or permissions** for enterprise features
- **Subscription tier** for freemium models
- **Experiment groups** for A/B testing
- **Device capabilities** for hardware-specific features
- **Session attributes** for personalization
- **Any domain-specific data** your application needs

## The Context Interface

```kotlin
interface Context {
    val locale: AppLocale
    val platform: Platform
    val appVersion: Version
    val stableId: StableId
}
```

This interface is intentionally minimal - it defines only what's needed for the base rule system. You extend it with your own properties.

## Creating a Custom Context

### Example: Enterprise Context

```kotlin
data class EnterpriseContext(
    // Required: Base Context properties
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,

    // Custom: Enterprise-specific properties
    val organizationId: String,
    val subscriptionTier: SubscriptionTier,
    val userRole: UserRole,
    val featuresEnabled: Set<String>,
) : Context

enum class SubscriptionTier {
    FREE, BASIC, PREMIUM, ENTERPRISE
}

enum class UserRole {
    VIEWER, EDITOR, ADMIN, OWNER
}
```

### Example: Experiment Context

```kotlin
data class ExperimentContext(
    // Required: Base Context properties
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,

    // Custom: Experiment-specific properties
    val experimentGroups: Set<String>,
    val sessionId: String,
    val cohortId: String,
    val firstSeen: Instant,
) : Context
```

## Using Custom Contexts with Flags

Once you have a custom context, you use it as the type parameter in your `Conditional` definitions:

```kotlin
enum class EnterpriseFeatures(
    override val key: String
) : Conditional<Boolean, EnterpriseContext> {  // Note: EnterpriseContext, not Context
    ADVANCED_ANALYTICS("advanced_analytics"),
    BULK_EXPORT("bulk_export"),
    API_ACCESS("api_access"),
    CUSTOM_BRANDING("custom_branding"),
    ;

}
```

### Configuration

Configure rules using base Context properties (the custom context implements them):

```kotlin
config {
    EnterpriseFeatures.ADVANCED_ANALYTICS with {
        default(false)

        // Base properties work fine
        rule {
            platforms(Platform.WEB)
            versions {
                min(2, 0)
            }
        } implies true
    }

    EnterpriseFeatures.API_ACCESS with {
        default(false)

        // Full rollout for web users on v2.0+
        rule {
            platforms(Platform.WEB)
            versions {
                min(2, 0)
            }
        } implies true
    }
}
```

### Evaluation

Evaluate with your custom context:

```kotlin
val enterpriseCtx = EnterpriseContext(
    locale = AppLocale.EN_US,
    platform = Platform.WEB,
    appVersion = Version(2, 5, 0),
    stableId = StableId.of("user-123"),
    organizationId = "org-456",
    subscriptionTier = SubscriptionTier.ENTERPRISE,
    userRole = UserRole.ADMIN,
    featuresEnabled = setOf("beta-features"),
)

// Type-safe: returns Boolean
val hasAnalytics = enterpriseCtx.evaluate(EnterpriseFeatures.ADVANCED_ANALYTICS)
```

## Extending Rules for Custom Contexts

While the base `Rule<C>` works with any context type, you can create custom rule types that leverage your context's specific properties:

```kotlin
data class EnterpriseRule(
    val Rule: Rule<EnterpriseContext>,
    val minSubscriptionTier: SubscriptionTier? = null,
    val requiredRole: UserRole? = null,
    val requiredFeatures: Set<String> = emptySet(),
) {
    fun matches(context: EnterpriseContext): Boolean {
        // First check base rule (platform, locale, version)
        if (!Rule.matches(context)) return false

        // Then check enterprise-specific requirements
        if (minSubscriptionTier != null && context.subscriptionTier.ordinal < minSubscriptionTier.ordinal) {
            return false
        }

        if (requiredRole != null && context.userRole.ordinal < requiredRole.ordinal) {
            return false
        }

        if (requiredFeatures.isNotEmpty() && !context.featuresEnabled.containsAll(requiredFeatures)) {
            return false
        }

        return true
    }
}
```

Usage:

```kotlin
val enterpriseOnlyRule = EnterpriseRule(
    Rule = Rule(
        rollout = Rollout.MAX,
        locales = emptySet(),
        platforms = setOf(Platform.WEB),
        versionRange = Unbounded,
    ),
    minSubscriptionTier = SubscriptionTier.ENTERPRISE,
    requiredRole = UserRole.ADMIN,
)

if (enterpriseOnlyRule.matches(enterpriseCtx)) {
    // User meets all criteria
}
```

## Multiple Context Types in the Same Application

You can use multiple context types for different purposes:

```kotlin
// Standard features use base Context
enum class StandardFeatures(
    override val key: String
) : Conditional<Boolean, Context> {
    DARK_MODE("dark_mode"),
    NEW_UI("new_ui"),
    ;

}

// Enterprise features use EnterpriseContext
enum class EnterpriseFeatures(
    override val key: String
) : Conditional<Boolean, EnterpriseContext> {
    ADVANCED_ANALYTICS("advanced_analytics"),
    ;

}

// Experiment features use ExperimentContext
enum class ExperimentFeatures(
    override val key: String
) : Conditional<String, ExperimentContext> {  // Note: String value type
    HOMEPAGE_VARIANT("homepage_variant"),
    ;

}
```

Each set of flags operates independently with its own context type.

## Context Creation Patterns

### From User Session

```kotlin
fun createEnterpriseContext(session: UserSession): EnterpriseContext {
    return EnterpriseContext(
        locale = session.user.preferredLocale,
        platform = session.deviceInfo.platform,
        appVersion = session.appVersion,
        stableId = StableId.of(session.user.id),
        organizationId = session.organization.id,
        subscriptionTier = session.organization.subscriptionTier,
        userRole = session.user.roleIn(session.organization),
        featuresEnabled = session.organization.enabledFeatures,
    )
}
```

### From API Request

```kotlin
fun createContextFromRequest(request: HttpServletRequest): EnterpriseContext {
    val user = authenticateUser(request)
    val org = user.organization

    return EnterpriseContext(
        locale = parseLocale(request.getHeader("Accept-Language")),
        platform = parsePlatform(request.getHeader("User-Agent")),
        appVersion = parseVersion(request.getHeader("X-App-Version")),
        stableId = StableId.of(user.id),
        organizationId = org.id,
        subscriptionTier = org.tier,
        userRole = user.role,
        featuresEnabled = org.features,
    )
}
```

### From GraphQL Context

```kotlin
data class GraphQLContext(
    val user: User,
    val organization: Organization,
    // ... other GraphQL context fields
) {
    fun toEnterpriseContext(): EnterpriseContext {
        return EnterpriseContext(
            locale = user.locale,
            platform = user.lastSeenPlatform,
            appVersion = user.lastSeenAppVersion,
            stableId = StableId.of(user.id),
            organizationId = organization.id,
            subscriptionTier = organization.tier,
            userRole = user.role,
            featuresEnabled = organization.features,
        )
    }
}

// In GraphQL resolver:
val enterpriseCtx = graphQLContext.toEnterpriseContext()
val hasAccess = enterpriseCtx.evaluate(EnterpriseFeatures.API_ACCESS)
```

## Inheritance and Context Hierarchies

You can create context hierarchies:

```kotlin
// Base authenticated context
interface AuthenticatedContext : Context {
    val userId: String
    val sessionId: String
}

// Enterprise adds organization info
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    override val userId: String,
    override val sessionId: String,
    val organizationId: String,
    val subscriptionTier: SubscriptionTier,
) : AuthenticatedContext

// Free user doesn't have organization
data class FreeUserContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    override val userId: String,
    override val sessionId: String,
) : AuthenticatedContext
```

## Best Practices

### 1. Keep Contexts Immutable

Use `data class` and `val` properties:

```kotlin
data class MyContext(
    override val locale: AppLocale,  // val, not var
    val customField: String,
) : Context
```

### 2. Use Stable IDs Correctly

The `stableId` should be:
- **Consistent** for the same user across sessions
- **Unique** per user
- **Non-PII** if possible (hashed user ID, device ID, etc.)

```kotlin
// Good: Stable, unique identifier
stableId = StableId.of(user.hashedId)

// Bad: Session ID changes every session
stableId = StableId.of(session.id)

// Bad: Not unique per user
stableId = StableId.of(user.organization.id)
```

### 3. Keep Contexts Lightweight

Don't include heavy objects or computed values:

```kotlin
// Good: Only data needed for evaluation
data class MyContext(
    override val stableId: StableId,
    val subscriptionTier: SubscriptionTier,
    // ...
) : Context

// Bad: Includes heavy objects
data class MyContext(
    override val stableId: StableId,
    val user: User,  // May contain lots of data
    val organization: Organization,  // May trigger database queries
) : Context
```

### 4. Document Custom Fields

Add KDoc to explain the purpose of custom fields:

```kotlin
data class EnterpriseContext(
    // Base Context properties
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,

    /**
     * Unique identifier for the user's organization.
     * Used for organization-specific feature rollouts.
     */
    val organizationId: String,

    /**
     * Current subscription tier of the organization.
     * Used to gate premium features.
     */
    val subscriptionTier: SubscriptionTier,
) : Context
```

### 5. Test with Multiple Context Types

Write tests that verify flags work correctly with your custom contexts:

```kotlin
@Test
fun `enterprise features evaluate correctly with EnterpriseContext`() {
    config {
        EnterpriseFeatures.ADVANCED_ANALYTICS with {
            default(false)
            rule {
                platforms(Platform.WEB)
            } implies true
        }
    }

    val ctx = EnterpriseContext(
        locale = AppLocale.EN_US,
        platform = Platform.WEB,
        appVersion = Version(1, 0, 0),
        stableId = StableId.of("test-user"),
        organizationId = "org-123",
        subscriptionTier = SubscriptionTier.ENTERPRISE,
        userRole = UserRole.ADMIN,
        featuresEnabled = emptySet(),
    )

    assertTrue(ctx.evaluate(EnterpriseFeatures.ADVANCED_ANALYTICS))
}
```

## Summary

Context polymorphism in Konditional allows you to:

- **Extend the base Context** with domain-specific fields
- **Maintain type safety** throughout the system
- **Create custom rules** that leverage your context's properties
- **Use multiple context types** in the same application
- **Adapt to your domain** without modifying the framework

This makes Konditional flexible enough to handle any use case while keeping the type safety guarantees that make it powerful.
