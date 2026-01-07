# Installation

Konditional Core is a single dependency.

Replace `VERSION` with the latest published version.

## Gradle (Kotlin DSL)

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.amichne:konditional-core:VERSION")
}
```

## Gradle (Groovy DSL)

```groovy
// build.gradle
dependencies {
    implementation 'io.amichne:konditional-core:VERSION'
}
```

## Maven

```xml
<dependency>
  <groupId>io.amichne</groupId>
  <artifactId>konditional-core</artifactId>
  <version>VERSION</version>
</dependency>
```

That is enough to define features and evaluate them in code. If you need remote configuration, JSON serialization, or
observability utilities, see the module docs:

- [Runtime](/runtime/index)
- [Serialization](/serialization/index)
- [Observability](/observability/index)
