---
title: 'Step 1: Add Dependencies'
description: Add Moshi serialization dependencies to your project
---


## Overview

The first step in integrating Konditional serialization is adding the Moshi JSON library to your project. Moshi provides the serialization infrastructure that Konditional uses.

::: tip
**Time estimate:** 5 minutes

**Prerequisites:** Gradle-based Kotlin project with Konditional already added
:::

## Add Moshi to build.gradle.kts

Open your `build.gradle.kts` file and add the Moshi dependencies:

```kotlin title="build.gradle.kts" ins={2-4}
dependencies {
    // Moshi for JSON serialization
    implementation("com.squareup.moshi:moshi:1.15.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    implementation("com.squareup.moshi:moshi-adapters:1.15.0")

    // Your existing dependencies
    implementation("io.amichne:konditional:1.0.0")
    // ...
}
```

### Why These Dependencies?

| Dependency | Purpose |
|------------|---------|
| `moshi` | Core JSON parsing and serialization |
| `moshi-kotlin` | Kotlin-specific adapters (data classes, default values) |
| `moshi-adapters` | Standard adapters for common types |

::: note
The Konditional serialization package already includes adapters for all Konditional types. You don't need to write custom adapters.
:::

## Sync Your Project

After adding the dependencies, sync your Gradle project:

```bash
./gradlew build
```

## Verify Installation

Create a simple test to verify Moshi is working:

```kotlin title="src/test/kotlin/VerifyMoshiTest.kt"
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlin.test.Test
import kotlin.test.assertEquals

class VerifyMoshiTest {
    @Test
    fun `moshi can serialize simple objects`() {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        data class Person(val name: String, val age: Int)

        val adapter = moshi.adapter(Person::class.java)
        val person = Person("Alice", 30)

        val json = adapter.toJson(person)
        val restored = adapter.fromJson(json)

        assertEquals(person, restored)
    }
}
```

Run the test:

```bash
./gradlew test --tests VerifyMoshiTest
```

If the test passes, Moshi is correctly installed!

## Project Structure

Your project should now look like this:


```

- build.gradle.kts
- src/
  - main/kotlin/
    - your/package/
      - FeatureFlags.kt (your existing flags)
  - test/kotlin/
    - VerifyMoshiTest.kt

```


## What's Next?

Now that Moshi is installed, you need to register your feature flags so they can be deserialized.

<div style="display: flex; justify-content: space-between; margin-top: 2rem;">
  <div></div>
  <a href="/serialization/steps/step-02-register/" style="text-decoration: none;">
    <strong>Next: Step 2 - Register Flags →</strong>
  </a>
</div>

## Troubleshooting

### Build Fails with "Could not resolve dependency"

**Problem:** Gradle can't download Moshi

**Solution:** Check your repository configuration:

```kotlin title="build.gradle.kts"
repositories {
    mavenCentral() // Moshi is on Maven Central
}
```

### "Unresolved reference: Moshi"

**Problem:** IDE hasn't picked up the new dependency

**Solution:**
1. Click "Sync Project with Gradle Files" in your IDE
2. Or run `./gradlew --refresh-dependencies`

### Version Conflicts

**Problem:** Multiple versions of Moshi in the dependency tree

**Solution:** Force a specific version:

```kotlin title="build.gradle.kts"
configurations.all {
    resolutionStrategy {
        force("com.squareup.moshi:moshi:1.15.0")
    }
}
```
