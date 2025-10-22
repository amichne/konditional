---
title: 'Step 3: Create Configuration'
description: Build your first flag configuration using ConfigBuilder
---


## Overview

In this step, you'll create a flag configuration using `ConfigBuilder`. This configuration will define your feature flags with their default values and targeting rules.

::: tip
**Time estimate:** 15-20 minutes

**Goal:** Create a `Flags.Snapshot` that can be serialized to JSON
:::

## Your First Configuration

Let's assume you have these feature flags defined:

```kotlin title="FeatureFlags.kt"
enum class FeatureFlags(override val key: String) : Conditional<Boolean, Context> {
    DARK_MODE("dark_mode"),
    NEW_ONBOARDING("new_onboarding"),
    COMPACT_CARDS("compact_cards"),
}
```

### Simple Configuration

Start with a basic configuration using default values only:

```kotlin
import io.amichne.konditional.builders.ConfigBuilder

val snapshot = ConfigBuilder.buildSnapshot {
    FeatureFlags.DARK_MODE with {
        default(false)
    }

    FeatureFlags.NEW_ONBOARDING with {
        default(false)
    }

    FeatureFlags.COMPACT_CARDS with {
        default(true)
    }
}
```

This creates a snapshot where:
- `DARK_MODE` defaults to `false`
- `NEW_ONBOARDING` defaults to `false`
- `COMPACT_CARDS` defaults to `true`

No targeting rules yet - all users see the same values.

## Adding Targeting Rules

Now let's add rules to target specific users:

### Example 1: Platform-Based Targeting

Enable dark mode only on iOS:

```kotlin
import io.amichne.konditional.context.Platform

val snapshot = ConfigBuilder.buildSnapshot {
    FeatureFlags.DARK_MODE with {
        default(false)

        boundary {
            platforms(Platform.IOS)
        }.implies(true)
    }
}
```

### Example 2: Locale-Based Targeting

Enable new onboarding for US users:

```kotlin
import io.amichne.konditional.context.AppLocale

val snapshot = ConfigBuilder.buildSnapshot {
    FeatureFlags.NEW_ONBOARDING with {
        default(false)

        boundary {
            locales(AppLocale.EN_US, AppLocale.ES_US)
        }.implies(true)
    }
}
```

### Example 3: Gradual Rollout

Roll out to 25% of users using rampUp:

```kotlin
import io.amichne.konditional.context.RampUp

val snapshot = ConfigBuilder.buildSnapshot {
    FeatureFlags.COMPACT_CARDS with {
        default(false)

        boundary {
            rampUp = RampUp.of(25.0) // 25% of users
        }.implies(true)
    }
}
```

### Example 4: Version-Based Targeting

Enable for users on version 2.0.0 or higher:

```kotlin
val snapshot = ConfigBuilder.buildSnapshot {
    FeatureFlags.DARK_MODE with {
        default(false)

        boundary {
            versions {
                min(2, 0, 0)
            }
        }.implies(true)
    }
}
```

## Combining Criteria

Rules can combine multiple targeting criteria:

```kotlin
val snapshot = ConfigBuilder.buildSnapshot {
    FeatureFlags.NEW_ONBOARDING with {
        default(false)

        // Rule 1: 50% rollout for US iOS users on v2.0+
        boundary {
            rampUp = RampUp.of(50.0)
            locales(AppLocale.EN_US)
            platforms(Platform.IOS)
            versions {
                min(2, 0, 0)
            }
        }.implies(true)

        // Rule 2: 100% for all Android users on v2.1+
        boundary {
            platforms(Platform.ANDROID)
            versions {
                min(2, 1, 0)
            }
        }.implies(true)
    }
}
```

::: note
Rules are evaluated in order of **specificity** (most specific first). More specific rules override less specific ones.
:::

## Real-World Configuration

Here's a complete example for a production app:

```kotlin title="createProductionConfig.kt"
import io.amichne.konditional.builders.ConfigBuilder
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.RampUp

fun createProductionConfig(): Flags.Snapshot {
    return ConfigBuilder.buildSnapshot {
        // Dark Mode: Enabled for everyone
        FeatureFlags.DARK_MODE with {
            default(true)
        }

        // New Onboarding: Gradual rollout
        FeatureFlags.NEW_ONBOARDING with {
            default(false)

            // 10% rollout for US users
            boundary {
                rampUp = RampUp.of(10.0)
                locales(AppLocale.EN_US)
            }.implies(true)

            // 5% rollout for other English locales
            boundary {
                rampUp = RampUp.of(5.0)
                locales(AppLocale.EN_CA)
            }.implies(true)
        }

        // Compact Cards: Platform-specific
        FeatureFlags.COMPACT_CARDS with {
            default(false)

            // Enable for all mobile platforms
            boundary {
                platforms(Platform.IOS, Platform.ANDROID)
            }.implies(true)

            // But disable for old versions
            boundary {
                versions {
                    max(1, 9, 9)
                }
            }.implies(false)
        }

        // Premium Feature: Version and platform gated
        FeatureFlags.PREMIUM_FEATURE with {
            default(false)

            boundary {
                platforms(Platform.IOS)
                versions {
                    min(2, 0, 0)
                }
            }.implies(true)
        }
    }
}
```

## Environment-Specific Configurations

Create different configs for different environments:


=== "Tab"

  
=== "Development"

    ```kotlin

      title="developmentConfig.kt"
    fun createDevelopmentConfig() = ConfigBuilder.buildSnapshot {
        // Everything enabled for development
        FeatureFlags.DARK_MODE with { default(true) }
        FeatureFlags.NEW_ONBOARDING with { default(true) }
        FeatureFlags.COMPACT_CARDS with { default(true) }
        FeatureFlags.DEBUG_MENU with { default(true) }
    }
    ```
  

  
=== "Staging"

    ```kotlin title="stagingConfig.kt"
    fun createStagingConfig() = ConfigBuilder.buildSnapshot {
        // Production-like with some overrides
        FeatureFlags.DARK_MODE with { default(true) }

        FeatureFlags.NEW_ONBOARDING with {
            default(false)
            boundary {
                rampUp = RampUp.of(50.0) // Higher rollout for testing
            }.implies(true)
        }

        FeatureFlags.COMPACT_CARDS with { default(true) }
        FeatureFlags.DEBUG_MENU with { default(true) } // Debug tools available
    }
    ```
  

  
=== "Production"

    ```kotlin title="productionConfig.kt"
    fun createProductionConfig() = ConfigBuilder.buildSnapshot {
        // Conservative rollouts
        FeatureFlags.DARK_MODE with { default(true) }

        FeatureFlags.NEW_ONBOARDING with {
            default(false)
            boundary {
                rampUp = RampUp.of(10.0) // Careful rollout
                locales(AppLocale.EN_US)
            }.implies(true)
        }

        FeatureFlags.COMPACT_CARDS with { default(false) }
        FeatureFlags.DEBUG_MENU with { default(false) } // Disabled
    }
    ```
  


## Configuration Best Practices

### 1. Document Your Rules

Use the `note` field to explain why a rule exists:

```kotlin
boundary {
    rampUp = RampUp.of(50.0)
    locales(AppLocale.EN_US)
    platforms(Platform.IOS)
    note = "JIRA-123: Gradual rollout for US iOS users to test performance"
}.implies(true)
```

### 2. Use Salt for Experiment Rebucketing

Change the salt when you want to rebucket users:

```kotlin
FeatureFlags.NEW_EXPERIMENT with {
    default(false)
    salt = "v2" // Changed from "v1" to rebucket users

    boundary {
        rampUp = RampUp.of(50.0)
    }.implies(true)
}
```

### 3. Keep Defaults Conservative

Default to `false` for new features:

```kotlin
FeatureFlags.RISKY_NEW_FEATURE with {
    default(false) // Safe default
    // Add rules to selectively enable
}
```

### 4. Test Your Configuration

Before serializing, test that it behaves as expected:

```kotlin
fun testConfiguration() {
    val snapshot = createProductionConfig()
    Flags.load(snapshot)

    // Test various contexts
    val usIosContext = Context(
        AppLocale.EN_US,
        Platform.IOS,
        Version.of(2, 0, 0),
        StableId.of("test-user-1")
    )

    with(Flags) {
        val darkMode = usIosContext.evaluate(FeatureFlags.DARK_MODE)
        val onboarding = usIosContext.evaluate(FeatureFlags.NEW_ONBOARDING)

        println("Dark mode: $darkMode")
        println("New onboarding: $onboarding")
    }
}
```

## What's Next?

Now that you have a configuration, you'll serialize it to JSON in the next step.

<div style="display: flex; justify-content: space-between; margin-top: 2rem;">
  <a href="/serialization/steps/step-02-register/" style="text-decoration: none;">
    <strong>← Previous: Step 2 - Register Flags</strong>
  </a>
  <a href="/serialization/steps/step-04-serialize/" style="text-decoration: none;">
    <strong>Next: Step 4 - Serialize →</strong>
  </a>
</div>
