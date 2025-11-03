---
title: Quick Start
description: Get up and running with Konditional in minutes
---

# Quick Start Guide

This guide will help you get started with Konditional in your Kotlin project.

## Installation

Add the Konditional dependency to your project:

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.amichne:konditional-core:VERSION")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'io.amichne:konditional-core:VERSION'
}
```

### Maven

```xml
<dependency>
    <groupId>io.amichne</groupId>
    <artifactId>konditional-core</artifactId>
    <version>VERSION</version>
</dependency>
```

## Basic Usage

### 1. Define Your Feature Flags

Create an enum implementing `Conditional<S, C>` where `S` is the value type and `C` is the context type:

```kotlin
import io.amichne.konditional.core.Conditional
import io.amichne.konditional.builders.FlagBuilder
import io.amichne.konditional.context.Context

enum class Features(override val key: String) : Conditional<Boolean, Context> {
    DARK_MODE("dark_mode"),
    NEW_CHECKOUT("new_checkout"),
    ANALYTICS("analytics"),
    ;

    override fun with(build: FlagBuilder<Boolean, Context>.() -> Unit) =
        update(FlagBuilder(this).apply(build).build())
}
```

### 2. Configure Your Flags

Use the DSL to configure flag behavior:

```kotlin
import io.amichne.konditional.builders.ConfigBuilder.Companion.config
import io.amichne.konditional.context.Platform

config {
    Features.DARK_MODE with {
        default(false)  // Default value when no rules match

        // Enable for iOS users
        rule {
            platforms(Platform.IOS)
        } implies true
    }

    Features.NEW_CHECKOUT with {
        default(false)

        // Enable for version 2.0+
        rule {
            versions {
                min(2, 0)
            }
        } implies true
    }
}
```

### 3. Create a Context and Evaluate

Create an evaluation context and check flag values:

```kotlin
import io.amichne.konditional.core.evaluate
import io.amichne.konditional.context.*
import io.amichne.konditional.core.StableId

// Create a context with user/app information
val context = Context(
    locale = AppLocale.EN_US,
    platform = Platform.IOS,
    appVersion = Version(2, 1, 0),
    stableId = StableId.of("user-123")
)

// Evaluate flags (uses SingletonFlagRegistry by default)
val isDarkModeEnabled = context.evaluate(Features.DARK_MODE)  // true (iOS)
val isNewCheckout = context.evaluate(Features.NEW_CHECKOUT)   // true (v2.0+)

if (isDarkModeEnabled) {
    applyDarkTheme()
}
```

## Common Patterns

### String-Based Flags

For configuration values like API endpoints or theme names:

```kotlin
enum class Config(override val key: String) : Conditional<String, Context> {
    API_ENDPOINT("api_endpoint"),
    THEME_NAME("theme_name"),
    ;

    override fun with(build: FlagBuilder<String, Context>.() -> Unit) =
        update(FlagBuilder(this).apply(build).build())
}

config {
    Config.API_ENDPOINT with {
        default("https://api.prod.example.com")

        rule {
            platforms(Platform.WEB)
        } implies "https://api.staging.example.com"
    }
}

val endpoint = context.evaluate(Config.API_ENDPOINT)
```

### Integer-Based Flags

For numeric limits and thresholds:

```kotlin
enum class Limits(override val key: String) : Conditional<Int, Context> {
    MAX_CONNECTIONS("max_connections"),
    TIMEOUT_SECONDS("timeout_seconds"),
    ;

    override fun with(build: FlagBuilder<Int, Context>.() -> Unit) =
        update(FlagBuilder(this).apply(build).build())
}

config {
    Limits.MAX_CONNECTIONS with {
        default(10)

        rule {
            platforms(Platform.WEB)
        } implies 50
    }
}

val maxConnections = context.evaluate(Limits.MAX_CONNECTIONS)
```

### Gradual Rollouts

Use rollout percentages for gradual feature releases:

```kotlin
import io.amichne.konditional.context.Rollout

config {
    Features.NEW_CHECKOUT with {
        default(false)

        // 25% rollout to iOS users
        rule {
            platforms(Platform.IOS)
            rollout = Rollout.of(25.0)
        } implies true
    }
}
```

### Complex Targeting

Combine multiple criteria:

```kotlin
config {
    Features.ANALYTICS with {
        default(false)

        // Enable for US iOS users on v2.0+ with 50% rollout
        rule {
            locales(AppLocale.EN_US)
            platforms(Platform.IOS)
            versions {
                min(2, 0)
            }
            rollout = Rollout.of(50.0)
        } implies true
    }
}
```

## Next Steps

- Learn about [Architecture](/advanced/architecture/) to understand how Konditional works
- Explore [Conditional Types](/advanced/conditional-types/) for using different value types
- Read about [Context Polymorphism](/advanced/context-polymorphism/) to create custom contexts
- Learn about [Serialization](/serialization/overview/) to manage flags remotely

## Example Project

For complete working examples, check out the test suite in the [GitHub repository](https://github.com/amichne/konditional).
