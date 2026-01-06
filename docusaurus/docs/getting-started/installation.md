# Installation

Add Konditional to your Kotlin project via Gradle or Maven.

## Gradle (Kotlin DSL)

```kotlin
// build.gradle.kts
dependencies {
    // Core API + DSL (compile-time surface)
    implementation("io.amichne:konditional-core:0.0.1")

    // Default runtime registry + lifecycle operations (Namespace.load/rollback/history)
    implementation("io.amichne:konditional-runtime:0.0.1")

    // Optional: JSON snapshot/patch codecs + configuration model
    implementation("io.amichne:konditional-serialization:0.0.1")

    // Optional: shadow evaluation + observability utilities
    implementation("io.amichne:konditional-observability:0.0.1")
}
```

## Gradle (Groovy DSL)

```groovy
// build.gradle
dependencies {
    implementation 'io.amichne:konditional-core:0.0.1'
    implementation 'io.amichne:konditional-runtime:0.0.1'
    implementation 'io.amichne:konditional-serialization:0.0.1'
    implementation 'io.amichne:konditional-observability:0.0.1'
}
```

## Maven

```xml
<dependency>
    <groupId>io.amichne</groupId>
    <artifactId>konditional-core</artifactId>
    <version>0.0.1</version>
</dependency>
<dependency>
    <groupId>io.amichne</groupId>
    <artifactId>konditional-runtime</artifactId>
    <version>0.0.1</version>
</dependency>
<dependency>
    <groupId>io.amichne</groupId>
    <artifactId>konditional-serialization</artifactId>
    <version>0.0.1</version>
</dependency>
<dependency>
    <groupId>io.amichne</groupId>
    <artifactId>konditional-observability</artifactId>
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
