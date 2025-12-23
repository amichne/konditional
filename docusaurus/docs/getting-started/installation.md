# Installation

Add Konditional to your Kotlin project via Gradle or Maven.

## Gradle (Kotlin DSL)

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.amichne:konditional:0.0.1")
}
```

## Gradle (Groovy DSL)

```groovy
// build.gradle
dependencies {
    implementation 'io.github.amichne:konditional:0.0.1'
}
```

## Maven

```xml
<dependency>
    <groupId>io.github.amichne</groupId>
    <artifactId>konditional</artifactId>
    <version>0.0.1</version>
</dependency>
```

---

## Requirements

- **Kotlin 1.9+**
- **JVM 11+** (or compatible Kotlin/Native, Kotlin/JS target)
- **Kotlin reflection** — Konditional bundles `kotlin-reflect` for custom data class deserialization

---

## Multiplatform Support

Konditional is a Kotlin Multiplatform library. It supports:

- **JVM** (Android, server-side)
- **Kotlin/JS** (browser, Node.js)
- **Kotlin/Native** (iOS, macOS, Linux, Windows)

Platform-specific configuration may be required for certain targets. See the [migration guide](/migration) for details on integrating with existing codebases.

---

## Next Steps

- [Your First Flag](/getting-started/your-first-flag) — Define and evaluate your first feature flag
- [Loading from JSON](/getting-started/loading-from-json) — Add runtime configuration
