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

### 1. Define a Flag

Create a simple boolean feature flag:

```kotlin
import io.amichne.konditional.core.Flag

val myFeatureFlag = Flag(
    id = "my-feature",
    defaultValue = false
)
```

### 2. Evaluate the Flag

Use the flag in your code:

```kotlin
if (myFeatureFlag.evaluate()) {
    // New feature code
    println("Feature enabled!")
} else {
    // Legacy code
    println("Feature disabled")
}
```

### 3. Add Conditions

Make flags conditional based on context:

```kotlin
import io.amichne.konditional.core.*

val betaFeatureFlag = Flag(
    id = "beta-feature",
    defaultValue = false,
    rules = listOf(
        Rule(
            condition = VersionCondition(
                range = VersionRange.from("2.0.0")
            ),
            value = true
        )
    )
)

// Evaluate with context
val context = EvaluationContext(version = Version("2.1.0"))
val isEnabled = betaFeatureFlag.evaluate(context)
```

## Common Patterns

### String-Based Flags

```kotlin
val themeFlag = Flag(
    id = "app-theme",
    defaultValue = "light"
)
```

### Integer-Based Flags

```kotlin
val maxItemsFlag = Flag(
    id = "max-items",
    defaultValue = 10
)
```

### Complex Rules

```kotlin
val premiumFeatureFlag = Flag(
    id = "premium-feature",
    defaultValue = false,
    rules = listOf(
        Rule(
            condition = AndCondition(
                VersionCondition(range = VersionRange.from("1.5.0")),
                UserAttributeCondition(attribute = "subscription", value = "premium")
            ),
            value = true
        )
    )
)
```

## Next Steps

- Learn about [Serialization](/serialization/overview/) to manage flags remotely
- Explore [Advanced Topics](/advanced/patch-updates/) for complex use cases
- Check out the complete [API Reference](/serialization/api/)

## Example Project

For a complete working example, check out the [examples directory](https://github.com/amichne/konditional/tree/main/examples) in the GitHub repository.
