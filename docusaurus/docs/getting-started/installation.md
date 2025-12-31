# Installation

Add Konditional to your Kotlin project via Gradle or Maven.

## Gradle (Kotlin DSL)

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.amichne:konditional:0.0.1")
}
```

## Gradle (Groovy DSL)

```groovy
// build.gradle
dependencies {
    implementation 'io.amichne:konditional:0.0.1'
}
```

## Maven

```xml
<dependency>
    <groupId>io.amichne</groupId>
    <artifactId>konditional</artifactId>
    <version>0.0.1</version>
</dependency>
```

---

## Requirements

- **Kotlin 2.2+**
- **JVM 21+**
- **Kotlin reflection** — Konditional depends on `kotlin-reflect` for custom structured value decoding at the JSON boundary

---

## Next Steps

- [Your First Flag](/getting-started/your-first-flag) — Define and evaluate your first feature flag
- [Loading from JSON](/getting-started/loading-from-json) — Add runtime configuration
